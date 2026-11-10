/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package dart.async;

import dart.runtime.DartRuntime;

import java.util.Iterator;
import java.util.NoSuchElementException;

/// A `sync*` body lowered to a state machine (the transpiler's AsyncLowering), driven as
/// the iterator Dart's generator is: every `moveNext` runs the body from where it last
/// stopped to its next `yield`, so the body's side effects interleave with the consumer's,
/// an endless generator stops being run once the consumer stops asking, and a throw
/// reaches the `moveNext` that runs into it -- after the elements before it were seen.
/// `yield*` hands the inner iterable's elements over one at a time, as Dart does.
/// One frame is one iteration: the iterable makes a new frame for every `iterator`.
/// Abandoning the iterator (an early `break`) runs no `finally` of the body, which is
/// what the Dart VM does too.
public class SyncStarFrame<T> extends LambdaFrame<Object> implements Iterator<T> {
    private T current;
    private boolean ready;
    private boolean done;
    private Iterator<?> inner;

    public SyncStarFrame() {
    }

    /// Always false: nothing cancels a sync* body; the lowered resume code asks the
    /// async* question.
    public final boolean cancelled() {
        return false;
    }

    public final void yield$(T v, int resume) {
        state = resume;
        current = v;
        ready = true;
    }

    public final void yieldAll(Object source, int resume) {
        state = resume;
        Iterable<?> it = (Iterable<?>) source;
        if (it == null) {
            throw new NullPointerException("yield* of null");
        }
        inner = it.iterator();
    }

    @Override
    public void complete(Object v) {
        done = true;
    }

    @Override
    public void fail(Throwable t) {
        done = true;
        throw DartRuntime.rethrow(t);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final boolean hasNext() {
        while (!ready && !done) {
            if (inner != null) {
                Iterator<?> in = inner;
                boolean more;
                try {
                    more = in.hasNext();
                } catch (Throwable t) {
                    inner = null;
                    done = true;
                    throw DartRuntime.rethrow(t);
                }
                if (more) {
                    current = (T) in.next();
                    ready = true;
                    return true;
                }
                inner = null;
            }
            try {
                step();
            } catch (Throwable t) {
                done = true;
                inner = null;
                throw DartRuntime.rethrow(t);
            }
        }
        return ready;
    }

    @Override
    public final T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        ready = false;
        T v = current;
        current = null;
        return v;
    }
}
