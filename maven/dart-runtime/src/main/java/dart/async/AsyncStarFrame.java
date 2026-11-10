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
import dart.runtime.Funcs;

/// The frame of an {@code async*} body: the async state machine of {@link LambdaFrame}
/// with yield as one more suspension point, driving the controller behind the stream it
/// returns. Nothing blocks and no thread is started; every resume runs from a microtask.
///
/// Dart's rules, which the emitted body follows by resuming through the states below:
///
/// - The body starts in a microtask once the stream is listened to.
/// - {@code yield v} delivers v and suspends; the body resumes in a microtask, or, while
///   the subscription is paused, only when it is resumed.
/// - {@code yield* s} forwards every event of s (pausing and cancelling s with the
///   subscription) and resumes when s is done.
/// - Cancelling resumes a body suspended at a yield, where {@link #cancelled()} answers
///   true and the emitted code returns through the enclosing finally blocks.
/// - The body's end closes the stream; an error it throws is delivered first.
public class AsyncStarFrame<T> extends LambdaFrame<Object> {
    private final StreamController<T> controller = new StreamController<T>();
    private boolean cancelled;
    private boolean parked;
    private StreamSubscription<T> delegate;

    public AsyncStarFrame() {
        controller.sync(true);
        controller.onListen(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                Microtasks.schedule(AsyncStarFrame.this);
            }
        });
        controller.onPause(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (delegate != null) {
                    delegate.pause();
                }
            }
        });
        controller.onResume(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (delegate != null) {
                    delegate.resume();
                } else {
                    wake();
                }
            }
        });
        controller.onCancel(new Funcs.Func0<Object>() {
            @Override
            public Object call() {
                cancelled = true;
                StreamSubscription<T> d = delegate;
                if (d != null) {
                    delegate = null;
                    d.cancel();
                    parked = true;
                }
                wake();
                // cancel() completes once the body has run its finally blocks.
                return isFinished() ? null : future;
            }
        });
    }

    /// The stream the body feeds.
    public final Stream<T> stream() {
        return controller.stream();
    }

    /// True once the listener cancelled: a body resumed at a yield returns.
    public final boolean cancelled() {
        return cancelled;
    }

    private void wake() {
        if (parked) {
            parked = false;
            Microtasks.schedule(this);
        }
    }

    /// {@code yield v}: delivers v, then suspends the body until {@code resume}.
    public final void yield$(T v, int resume) {
        state = resume;
        $v = null;
        $e = null;
        if (!cancelled) {
            controller.add(v);
        }
        parked = true;
        if (cancelled || !controller.isPaused()) {
            wake();
        }
    }

    /// {@code yield* source}: forwards source's events, resuming at {@code resume} once
    /// it is done (or the subscription is cancelled).
    @SuppressWarnings("unchecked")
    public final void yieldAll(Object source, int resume) {
        state = resume;
        $v = null;
        $e = null;
        if (cancelled || source == null) {
            parked = true;
            wake();
            return;
        }
        // done[0]: the source ended, possibly inside listen() itself.
        final boolean[] done = new boolean[1];
        StreamSubscription<T> s = ((Stream<T>) source).listen(new Funcs.VoidFunc1<T>() {
            @Override
            public void call(T v) {
                controller.add(v);
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                controller.addError(e);
            }
        }, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (!done[0]) {
                    done[0] = true;
                    delegate = null;
                    parked = true;
                    wake();
                }
            }
        });
        if (!done[0]) {
            delegate = s;
            if (controller.isPaused()) {
                s.pause();
            }
        }
    }

    @Override
    public void complete(Object v) {
        if (isFinished()) {
            return;
        }
        super.complete(null);
        controller.close();
    }

    @Override
    public void fail(Throwable t) {
        if (isFinished()) {
            return;
        }
        super.complete(null);
        if (!cancelled) {
            controller.addError(DartRuntime.caught(t));
        }
        controller.close();
    }
}
