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
package com.codename1.flutter.plugins.bloc;

import dart.async.Future;
import dart.async.Stream;
import dart.runtime.Dyn;
import dart.runtime.Funcs;

/// bloc's `Emitter<State>`, handed to every event handler. Dart calls it as a
/// function (`emit(state)`), which reaches [#call]; it answers null rather than
/// void so that an arrow handler `(e, emit) => emit(s)` still fits the
/// value-returning handler type.
public class Emitter<S> {
    private final Bloc<?, S> bloc;
    private final Object event;
    private boolean done;

    Emitter(Bloc<?, S> bloc, Object event) {
        this.bloc = bloc;
        this.event = event;
    }

    public Object call(S state) {
        if (!done) {
            bloc.emitTransition(event, state);
        }
        return null;
    }

    public boolean isDone() {
        return done;
    }

    /// Finishes this emitter: later emits are ignored, and a forEach/onEach
    /// subscription is cancelled and its future completed, as bloc does on close.
    void complete() {
        dart.async.StreamSubscription<?> s;
        dart.async.Completer<Object> r;
        synchronized (this) {
            done = true;
            s = activeSub;
            r = activeResult;
            activeSub = null;
            activeResult = null;
        }
        bloc.emitterDone(this);
        if (s != null) {
            s.cancel();
        }
        if (r != null && !r.isCompleted()) {
            r.complete(null);
        }
    }

    private dart.async.StreamSubscription<?> activeSub;
    private dart.async.Completer<Object> activeResult;

    /// `await emit.forEach(stream, onData: (d) => state, onError: (e, st) => state)`:
    /// emits what onData maps each event to, until the stream ends. A source error
    /// goes to onError, whose state is emitted, as Bloc does; with no onError the
    /// error fails the handler.
    public <T> Future<Object> forEach(Stream<T> stream, final Funcs.Func1<T, S> onData, final Object onError) {
        return drive(stream, onData, onError, true);
    }

    /// `await emit.onEach(stream, onData: (d) { ... }, onError: (e, st) { ... })`.
    public <T> Future<Object> onEach(Stream<T> stream, final Funcs.VoidFunc1<T> onData, final Object onError) {
        return drive(stream, onData, onError, false);
    }

    @SuppressWarnings("unchecked")
    private <T> Future<Object> drive(Stream<T> stream, final Object onData, final Object onError,
            final boolean emitResults) {
        final dart.async.Completer<Object> result = new dart.async.Completer<Object>();
        final dart.async.StreamSubscription<?>[] sub = new dart.async.StreamSubscription<?>[1];
        sub[0] = stream.listen(new Funcs.VoidFunc1<T>() {
            @Override
            public void call(T value) {
                try {
                    Object v = Dyn.invoke(onData, value);
                    if (emitResults) {
                        Emitter.this.call((S) v);
                    }
                } catch (RuntimeException t) {
                    fail(sub[0], result, t);
                }
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object err) {
                if (onError == null) {
                    fail(sub[0], result, err);
                    return;
                }
                try {
                    Object e = err instanceof Throwable ? dart.runtime.DartRuntime.caught((Throwable) err) : err;
                    Object v = invokeOnError(onError, e);
                    if (emitResults) {
                        Emitter.this.call((S) v);
                    }
                } catch (RuntimeException t) {
                    fail(sub[0], result, t);
                }
            }
        }, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                if (!result.isCompleted()) {
                    result.complete(null);
                }
            }
        }, Boolean.FALSE);
        boolean already;
        synchronized (this) {
            already = done;
            if (!already) {
                activeSub = sub[0];
                activeResult = result;
            }
        }
        if (already) {
            sub[0].cancel();
            if (!result.isCompleted()) {
                result.complete(null);
            }
        }
        return result.future();
    }

    private void fail(dart.async.StreamSubscription<?> sub, dart.async.Completer<Object> result, Object err) {
        if (sub != null) {
            sub.cancel();
        }
        if (!result.isCompleted()) {
            result.completeError(err);
        }
    }

    private static Object invokeOnError(Object handler, Object err) {
        if (handler instanceof Funcs.Func2 || handler instanceof Funcs.VoidFunc2) {
            return Dyn.invoke(handler, err, null);
        }
        return Dyn.invoke(handler, err);
    }
}
