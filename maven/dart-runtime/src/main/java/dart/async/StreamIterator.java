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

import dart.runtime.Funcs;

import java.util.ArrayDeque;

/**
 * Dart's {@code StreamIterator}, and the lowering target of {@code await for}.
 *
 * <p>{@code await for (x in s) body} becomes a loop over {@link #next()}, which parks
 * the current frame the way {@link Await#await$} does until the stream delivers an
 * event, finishes or fails. Events that arrive while the loop body runs are queued, so
 * none is lost. Leaving the loop by any path calls {@link #cancel()} in a finally block,
 * which cancels the subscription as Dart does on {@code break} or an exception.</p>
 */
public final class StreamIterator<T> {

    private static final Object DONE = new Object();

    private final Stream<T> stream;
    private final ArrayDeque<Object> queue = new ArrayDeque<Object>();
    private StreamSubscription<T> sub;
    private Completer<Boolean> waiting;
    private T current;
    private boolean finished;
    private Object error;
    private boolean hasError;

    public StreamIterator(Stream<T> stream) {
        this.stream = stream;
    }

    public T current() {
        return current;
    }

    /** Dart's {@code moveNext()}: completes with false once the stream is done. */
    public Future<Boolean> moveNext() {
        if (sub == null && !finished) {
            subscribe();
        }
        Completer<Boolean> c = new Completer<Boolean>();
        if (!deliver(c)) {
            waiting = c;
        }
        return c.future();
    }

    /** The blocking step {@code await for} is lowered to. */
    public boolean next() {
        Boolean more = Await.await$(moveNext());
        return more != null && more.booleanValue();
    }

    public Future<Object> cancel() {
        finished = true;
        queue.clear();
        StreamSubscription<T> s = sub;
        sub = null;
        if (s != null) {
            return s.cancel();
        }
        return Future.value(null);
    }

    private void subscribe() {
        sub = stream.listen(new Funcs.VoidFunc1<T>() {
            @Override
            public void call(T v) {
                push(v);
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                if (!hasError) {
                    hasError = true;
                    error = e;
                }
                push(DONE);
            }
        }, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                push(DONE);
            }
        }, Boolean.TRUE);
    }

    private void push(Object event) {
        queue.addLast(event);
        Completer<Boolean> c = waiting;
        if (c != null && deliver(c)) {
            waiting = null;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean deliver(Completer<Boolean> c) {
        if (queue.isEmpty()) {
            if (finished) {
                c.complete(Boolean.FALSE);
                return true;
            }
            return false;
        }
        Object e = queue.removeFirst();
        if (e == DONE) {
            finished = true;
            queue.clear();
            sub = null;
            if (hasError) {
                hasError = false;
                c.completeError(error);
            } else {
                c.complete(Boolean.FALSE);
            }
            return true;
        }
        current = (T) e;
        c.complete(Boolean.TRUE);
        return true;
    }
}
