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

    void complete() {
        done = true;
    }

    /// `await emit.forEach(stream, onData: (d) => state)`: emits what onData maps
    /// each event to, until the stream ends.
    public <T> Future<Object> forEach(Stream<T> stream, final Object onData, final Object onError) {
        return stream.forEach(new Funcs.VoidFunc1<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public void call(T value) {
                Emitter.this.call((S) Dyn.invoke(onData, value));
            }
        });
    }

    /// `await emit.onEach(stream, onData: (d) { ... })`.
    public <T> Future<Object> onEach(Stream<T> stream, final Object onData, final Object onError) {
        return stream.forEach(new Funcs.VoidFunc1<T>() {
            @Override
            public void call(T value) {
                Dyn.invoke(onData, value);
            }
        });
    }
}
