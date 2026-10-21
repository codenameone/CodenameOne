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

import java.util.ArrayList;
import java.util.List;

import dart.async.Future;
import dart.runtime.Dyn;
import dart.runtime.Funcs;

/// bloc's `Bloc<Event, State>`: events go in through [#add], and the handler
/// registered for that event type with [#on] emits the states that come out.
///
/// The Dart `on<E>` type argument is erased in Java, so the transpiler passes it
/// as `type` (see the stub's `witness` parameter). A handler registered without
/// one -- `on<E>` inside a generic class -- sees every event.
public abstract class Bloc<E, S> extends BlocBase<S> {

    /// `Bloc.observer`: the app-wide [BlocObserver].
    public static BlocObserver observer = new BlocObserver();

    private final List<Class<?>> types = new ArrayList<Class<?>>();
    private final List<Object> handlers = new ArrayList<Object>();

    protected Bloc(S initialState) {
        super(initialState);
    }

    /// `on<T>(handler)`. The handler answers whatever its body does -- a Future for an
    /// async one -- so it is typed to return Object; Emitter.call answers null so an arrow
    /// handler that only emits fits too.
    public <T> void on(Funcs.Func2<T, Emitter<S>, Object> handler, Class<T> type) {
        types.add(type);
        handlers.add(handler);
    }

    public <T> void on(Funcs.Func2<T, Emitter<S>, Object> handler, Object transformer, Class<T> type) {
        on(handler, type);
    }

    public void add(E event) {
        if (isClosed()) {
            throw new dart.core.StateError("Cannot add new events after calling close");
        }
        onEvent(event);
        boolean handled = false;
        for (int i = 0; i < handlers.size(); i++) {
            Class<?> t = types.get(i);
            if (t != null && !t.isInstance(event)) {
                continue;
            }
            handled = true;
            final Emitter<S> emitter = new Emitter<S>(this, event);
            Object result;
            try {
                result = Dyn.invoke(handlers.get(i), event, emitter);
            } catch (RuntimeException err) {
                emitter.complete();
                onError(err, null);
                throw err;
            }
            if (result instanceof Future) {
                // A handler that fails later reaches onError (and the observer) as a
                // synchronous throw does; nothing else would ever see that future's error.
                @SuppressWarnings("unchecked")
                Future<Object> pending = (Future<Object>) result;
                pending.then(new dart.runtime.Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object v) {
                        emitter.complete();
                    }
                }, new dart.runtime.Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object err) {
                        emitter.complete();
                        onError(err, null);
                        // Bloc notifies onError and then rethrows into the zone. Throwing
                        // here would only fail the future then() returns, which nobody
                        // holds, so report it as uncaught the way the zone would.
                        reportUncaught(dart.runtime.DartRuntime.asError(err));
                    }
                });
            } else {
                emitter.complete();
            }
        }
        if (!handled) {
            dart.core.StateError err = new dart.core.StateError("add(" + event + ") was called without a registered "
                    + "event handler. Make sure to register a handler via on<"
                    + (event == null ? "?" : event.getClass().getSimpleName()) + ">((event, emit) {...})");
            onError(err, null);
            // Bloc notifies onError and then throws the StateError at the caller of add.
            throw err;
        }
    }

    /**
     * Logs an asynchronous handler failure that nothing awaits. Logging needs an
     * installed implementation, which a headless test lacks, and reporting must
     * never throw.
     */
    private static void reportUncaught(Throwable t) {
        try {
            com.codename1.io.Log.e(t);
        } catch (Throwable loggingFailed) {
            // Nothing to log through; nothing more to do.
        }
    }

    public void onEvent(E event) {
        observer.onEvent(this, event);
    }

    public void onTransition(Transition<E, S> transition) {
        observer.onTransition(this, transition);
    }

    @SuppressWarnings("unchecked")
    void emitTransition(Object event, S next) {
        if (isClosed()) {
            return;
        }
        if (!dart.runtime.DartRuntime.eq(next, state())) {
            onTransition(new Transition<E, S>(state(), (E) event, next));
        }
        emit(next);
    }
}
