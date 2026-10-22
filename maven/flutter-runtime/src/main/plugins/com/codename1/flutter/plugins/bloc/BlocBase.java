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

import com.codename1.flutter.foundation.ChangeNotifier;

import dart.async.Future;
import dart.async.Stream;
import dart.async.StreamController;
import dart.core.StateError;
import dart.runtime.DartRuntime;

/// bloc's `BlocBase<State>`: the current state, the stream of later states, and
/// `emit`. It is also a [ChangeNotifier], so the provider machinery the runtime
/// already has -- `context.watch`, `context.select`, rebuilding readers -- works on a
/// bloc unchanged, the same way `flutter_bloc`'s providers sit on `provider`.
public abstract class BlocBase<S> implements ChangeNotifier {

    private S state;
    private boolean emitted;
    private boolean closed;
    private final StreamController<S> controller = StreamController.broadcast();

    protected BlocBase(S initialState) {
        this.state = initialState;
        Bloc.observer.onCreate(this);
    }

    public S state() {
        return state;
    }

    public Stream<S> stream() {
        return controller.stream();
    }

    public boolean isClosed() {
        return closed;
    }

    /// Replaces the state and tells everyone. Like bloc, an `==` state is
    /// dropped once something has been emitted, and emitting after `close`
    /// is an error.
    public void emit(S next) {
        if (closed) {
            throw new StateError("Cannot emit new states after calling close");
        }
        if (emitted && DartRuntime.eq(next, state)) {
            return;
        }
        onChange(new Change<S>(state, next));
        state = next;
        emitted = true;
        controller.add(next);
        notifyListeners();
    }

    public void onChange(Change<S> change) {
        Bloc.observer.onChange(this, change);
    }

    public void addError(Object error, Object stackTrace) {
        onError(error, stackTrace);
    }

    public void addError(Object error) {
        onError(error, null);
    }

    public void onError(Object error, Object stackTrace) {
        Bloc.observer.onError(this, error, stackTrace);
    }

    public Future<Object> close() {
        if (closed) {
            return Future.value(null);
        }
        closed = true;
        Bloc.observer.onClose(this);
        return controller.close();
    }
}
