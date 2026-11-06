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

/// One activation of a Dart {@code async} body, lowered to a state machine: the
/// continuation-passing form of {@code await}. The body is {@link #step()}, a switch on
/// {@link #state}; an await stores the state to resume at, calls
/// {@link #await(Object, int)} and returns. Nothing blocks and no thread is started:
/// the frame resumes from the microtask queue (on the EDT when a Display is live), with
/// {@link Zone#current} set back to the zone the frame was created in.
///
/// On resume the awaited result is in {@link #$v}, or its error in {@link #$e} (which the
/// body rethrows, or routes to a catch). Anything {@code step()} throws fails
/// {@link #future}; {@link #complete(Object)} is the body's {@code return}.
public abstract class AsyncFrame<T> implements Runnable {
    /// The state {@link #step()} resumes at; 0 on entry.
    public int state;
    /// The value the last await produced.
    public Object $v;
    /// The error the last await produced, or null.
    public Throwable $e;
    /// The zone the frame was created in, current again on every resume.
    protected final Zone zone = Zone.current;
    /// What the async call returns.
    public final Future<T> future = new Future<T>();
    private boolean finished;

    protected AsyncFrame() {
    }

    /// Runs the body from {@link #state} until it awaits, returns or throws.
    protected abstract void step() throws Throwable;

    /// One activation: step() in the frame's zone, a throw failing the frame.
    @Override
    public final void run() {
        Zone previous = Zone.current;
        Zone.current = zone;
        try {
            step();
        } catch (Throwable t) {
            fail(t);
        } finally {
            Zone.current = previous;
        }
    }

    /// Runs the body synchronously up to its first await and answers the future, as a
    /// call to a Dart async function does.
    public final Future<T> start() {
        run();
        return future;
    }

    /// {@code await v}: the body sets nothing else and returns right after this call.
    /// The frame always yields, even when v is a completed future or not a future at
    /// all; it resumes at {@code resume} from a microtask.
    public final void await(Object v, int resume) {
        state = resume;
        suspended = true;
        $v = null;
        $e = null;
        final Future<Object> f = Future.awaitable(v);
        f.driveOnce();
        f.markHandled();
        // A listener on a future that has completed is already queued as a microtask
        // (see Future.onComplete); one that completes later queues the resume, so the
        // body never continues inside whatever code completed the future.
        final boolean queued = f.completedNow();
        f.onComplete(new Runnable() {
            @Override
            public void run() {
                Throwable err = f.completedError();
                if (err != null) {
                    $e = err;
                } else {
                    $v = f.completedValue();
                }
                if (queued || f.resumeInline) {
                    AsyncFrame.this.run();
                } else {
                    Microtasks.schedule(AsyncFrame.this);
                }
            }
        });
    }

    /// The awaited value, or the awaited error thrown.
    public final Object value() throws Throwable {
        Throwable e = $e;
        if (e != null) {
            $e = null;
            throw e;
        }
        return $v;
    }

    /// {@code return v}: completes the future (adopting v when it is itself a future).
    public void complete(T v) {
        if (finished) {
            return;
        }
        finished = true;
        future.complete(v);
    }

    /// Set at the first await: a failure after it is reported to the zone at once.
    private boolean suspended;

    /// An uncaught throw out of the body.
    public void fail(Throwable t) {
        if (finished) {
            throw new IllegalStateException("async frame failed after it finished", t);
        }
        finished = true;
        future.completeError(t, suspended);
    }

    final void markFinished() {
        finished = true;
    }

    /// Whether the body has returned or failed.
    protected final boolean isFinished() {
        return finished;
    }
}
