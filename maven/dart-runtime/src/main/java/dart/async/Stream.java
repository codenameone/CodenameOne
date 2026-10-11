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

import java.util.ArrayList;
import java.util.List;

/**
 * Dart's {@code Stream<T>} -- the minimal subset needed to deliver platform
 * events ({@code EventChannel.receiveBroadcastStream}) and to feed a
 * {@link StreamController}.
 *
 * <p>{@link #listen}'s {@code onData} is concretely typed
 * {@link Funcs.VoidFunc1}, matching real Dart's own {@code void
 * onData(T event)?}: outside a handful of names the emitter special-cases
 * by name ({@code Future.then/catchError/whenComplete}), a Dart closure
 * literal is emitted as a plain, untyped Java lambda with no wrapping, so
 * whatever parameter it is passed to MUST be a genuine functional
 * interface -- a parameter typed plain {@code Object} is not one, and the
 * generated call fails to compile. {@code onError} and {@code onDone}
 * stay {@code Object} (real Dart types {@code onError} as the untyped
 * {@code Function?}) and are dispatched with {@code instanceof} across
 * {@link Funcs.VoidFunc0}/{@link Funcs.Func0}/
 * {@link Funcs.VoidFunc1}/{@link Funcs.Func1} -- never a blind cast, per
 * this runtime's no-ClassCastException rule -- which covers every
 * hand-written Java caller; a transpiled Dart closure passed for either of
 * those two specifically would need the same concrete typing {@code
 * onData} has, which is not provided here.</p>
 *
 * <p><b>Not implemented</b> (kept out deliberately, not by oversight): a
 * single-subscription stream that buffers events before anyone listens --
 * every stream here behaves as broadcast, so a {@code listen()} with no
 * prior listener misses nothing only because {@link #onFirstListener()}
 * runs synchronously before this call returns; {@code sync: true}
 * zero-latency delivery semantics (events always reach listeners
 * immediately and synchronously here, which is closer to {@code sync:
 * true} than to buffered async dispatch, but is not distinguished from
 * it); {@code Stream.fromIterable}/{@code periodic}/{@code generators};
 * {@code asyncMap}/{@code asyncExpand}/{@code transform}/{@code distinct}/
 * {@code take}/{@code skip}/{@code timeout}/{@code expand}; and multiple
 * independent "listen sessions" are not distinguished from multiple
 * listeners on one session -- {@link #listen} always adds to the same
 * listener set.</p>
 */
public class Stream<T> {

    private final List<Sub> subs = new ArrayList<Sub>();
    private boolean closed;

    /** Holds one subscriber's handlers and paused state. */
    private static final class Sub {
        Object onData;
        Object onError;
        Object onDone;
        boolean cancelOnError;
        boolean paused;
        boolean cancelled;
    }

    /**
     * Runs once, synchronously, when the first subscriber attaches (0 -> 1
     * live subscriptions). The default does nothing; a stream backing an
     * event channel overrides this to call the platform
     * {@code StreamHandler.onListen}.
     */
    protected void onFirstListener() {
    }

    /**
     * Runs once, synchronously, when the last subscription cancels (1 -> 0
     * live subscriptions). The default does nothing; a stream backing an
     * event channel overrides this to call the platform
     * {@code StreamHandler.onCancel}.
     */
    protected void onNoListeners() {
    }

    public final StreamSubscription<T> listen(Funcs.VoidFunc1<T> onData) {
        return listen(onData, null, null, null);
    }

    public final StreamSubscription<T> listen(Funcs.VoidFunc1<T> onData, Object onError) {
        return listen(onData, onError, null, null);
    }

    public final StreamSubscription<T> listen(Funcs.VoidFunc1<T> onData, Object onError, Object onDone) {
        return listen(onData, onError, onDone, null);
    }

    /**
     * Dart's {@code listen(onData, {onError, onDone, cancelOnError})},
     * flattened to its canonical positional order (the stub convention for
     * named parameters). {@code cancelOnError} cancels the subscription
     * right after its {@code onError} handler runs for the first error.
     *
     * <p>{@code onData} is concretely typed (real Dart's own signature is
     * {@code void onData(T event)?}, never value-returning), unlike
     * {@code onError}/{@code onDone}: the transpiler emits every closure
     * literal as a bare, untyped Java lambda with no wrapping, so any
     * parameter a Dart closure is passed to must be a genuine functional
     * interface -- a plain {@code Object} parameter is not one, and the
     * generated call fails to compile.</p>
     */
    public final StreamSubscription<T> listen(Funcs.VoidFunc1<T> onData, Object onError, Object onDone, Boolean cancelOnError) {
        Sub sub = new Sub();
        sub.onData = onData;
        sub.onError = onError;
        sub.onDone = onDone;
        sub.cancelOnError = cancelOnError != null && cancelOnError.booleanValue();
        boolean first = subs.isEmpty();
        subs.add(sub);
        if (first) {
            onFirstListener();
        }
        return new Handle(sub);
    }

    /** True while at least one live (not cancelled) subscription exists. */
    protected final boolean hasListeners() {
        return !subs.isEmpty();
    }

    /** Delivers a data event to every live, non-paused subscriber. */
    @SuppressWarnings("unchecked")
    protected final void emit(T value) {
        for (Sub s : new ArrayList<Sub>(subs)) {
            if (!s.cancelled && !s.paused) {
                deliverData(s.onData, value);
            }
        }
    }

    /**
     * Delivers an error to every live, non-paused subscriber; a subscriber
     * whose {@code cancelOnError} was set is then cancelled.
     */
    protected final void emitError(Object error) {
        for (Sub s : new ArrayList<Sub>(subs)) {
            if (s.cancelled || s.paused) {
                continue;
            }
            deliverError(s.onError, error);
            if (s.cancelOnError) {
                cancelSub(s);
            }
        }
    }

    /** Delivers "done" to every live subscriber, then clears the listener set. */
    protected final void emitDone() {
        closed = true;
        List<Sub> snapshot = new ArrayList<Sub>(subs);
        subs.clear();
        for (Sub s : snapshot) {
            if (!s.cancelled) {
                deliverVoid(s.onDone);
            }
        }
    }

    private void cancelSub(Sub s) {
        if (s.cancelled) {
            return;
        }
        s.cancelled = true;
        subs.remove(s);
        if (subs.isEmpty() && !closed) {
            onNoListeners();
        }
    }

    /**
     * Dart's {@code Stream<T>.map<R>}: a derived stream that applies
     * {@code transform} to every event. The upstream subscription is only
     * live while the derived stream itself has listeners.
     */
    public final <R> Stream<R> map(final Object transform) {
        final Stream<T> upstream = this;
        return new Stream<R>() {
            private StreamSubscription<T> upstreamSub;

            @Override
            @SuppressWarnings("unchecked")
            protected void onFirstListener() {
                final Stream<R> self = this;
                upstreamSub = upstream.listen(new Funcs.VoidFunc1<T>() {
                    @Override
                    public void call(T v) {
                        Object mapped;
                        try {
                            mapped = invoke1(transform, v);
                        } catch (Throwable t) {
                            self.emitError(t);
                            return;
                        }
                        self.emit((R) mapped);
                    }
                }, new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object e) {
                        self.emitError(e);
                    }
                }, new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        self.emitDone();
                    }
                });
            }

            @Override
            protected void onNoListeners() {
                if (upstreamSub != null) {
                    upstreamSub.cancel();
                    upstreamSub = null;
                }
            }
        };
    }

    /** Dart's {@code Stream<T>.where}: a derived stream of events {@code test} accepts. */
    public final Stream<T> where(final Object test) {
        final Stream<T> upstream = this;
        return new Stream<T>() {
            private StreamSubscription<T> upstreamSub;

            @Override
            protected void onFirstListener() {
                final Stream<T> self = this;
                upstreamSub = upstream.listen(new Funcs.VoidFunc1<T>() {
                    @Override
                    public void call(T v) {
                        boolean keep;
                        try {
                            keep = Boolean.TRUE.equals(invoke1(test, v));
                        } catch (Throwable t) {
                            self.emitError(t);
                            return;
                        }
                        if (keep) {
                            self.emit(v);
                        }
                    }
                }, new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object e) {
                        self.emitError(e);
                    }
                }, new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        self.emitDone();
                    }
                });
            }

            @Override
            protected void onNoListeners() {
                if (upstreamSub != null) {
                    upstreamSub.cancel();
                    upstreamSub = null;
                }
            }
        };
    }

    // ------------------------------------------------------------------
    // Callback dispatch -- instanceof, never a blind cast (see class comment).
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    static void deliverData(Object onData, Object value) {
        if (onData instanceof Funcs.VoidFunc1) {
            ((Funcs.VoidFunc1<Object>) onData).call(value);
        } else if (onData instanceof Funcs.Func1) {
            ((Funcs.Func1<Object, Object>) onData).call(value);
        }
    }

    /** Invokes a one-argument transform/predicate callback and returns its result. */
    @SuppressWarnings("unchecked")
    static Object invoke1(Object fn, Object value) {
        if (fn instanceof Funcs.Func1) {
            return ((Funcs.Func1<Object, Object>) fn).call(value);
        }
        if (fn instanceof Funcs.VoidFunc1) {
            ((Funcs.VoidFunc1<Object>) fn).call(value);
            return null;
        }
        throw new dart.core.ArgumentError("not a one-argument function");
    }

    /**
     * Invokes an error handler of any shape a Dart {@code onError} closure
     * transpiles to: {@code (e)}, {@code (e, stackTrace)}, with or without a
     * return value.
     */
    @SuppressWarnings("unchecked")
    static void deliverError(Object onError, Object error) {
        Object err = error instanceof Throwable
                ? dart.runtime.DartRuntime.caught((Throwable) error) : error;
        if (onError instanceof Funcs.VoidFunc1) {
            ((Funcs.VoidFunc1<Object>) onError).call(err);
        } else if (onError instanceof Funcs.Func1) {
            ((Funcs.Func1<Object, Object>) onError).call(err);
        } else if (onError instanceof Funcs.VoidFunc2) {
            ((Funcs.VoidFunc2<Object, Object>) onError).call(err, null);
        } else if (onError instanceof Funcs.Func2) {
            ((Funcs.Func2<Object, Object, Object>) onError).call(err, null);
        } else if (onError == null) {
            // No handler installed: Dart's default behaviour is to forward the
            // error to the subscription's zone, which for a headless runtime
            // has nowhere else to go but the log.
            com.codename1.io.Log.e(err instanceof Throwable ? (Throwable) err : new RuntimeException(String.valueOf(err)));
        }
    }

    static void deliverVoid(Object cb) {
        if (cb instanceof Funcs.VoidFunc0) {
            ((Funcs.VoidFunc0) cb).call();
        } else if (cb instanceof Funcs.Func0) {
            ((Funcs.Func0<?>) cb).call();
        }
    }

    /** A live subscription's handle: mutates the {@link Sub} it was created with. */
    private final class Handle implements StreamSubscription<T> {
        private final Sub sub;

        Handle(Sub sub) {
            this.sub = sub;
        }

        @Override
        public Future<Object> cancel() {
            cancelSub(sub);
            return Future.value(null);
        }

        @Override
        public void onData(Object handleData) {
            sub.onData = handleData;
        }

        @Override
        public void onError(Object handleError) {
            sub.onError = handleError;
        }

        @Override
        public void onDone(Object handleDone) {
            sub.onDone = handleDone;
        }

        @Override
        public void pause() {
            sub.paused = true;
        }

        @Override
        public void resume() {
            sub.paused = false;
        }

        @Override
        public boolean isPaused() {
            return sub.paused;
        }
    }
}
