/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.firebaseui;

import com.codename1.flutter.BuildContext;

import dart.runtime.Funcs;

/**
 * firebase_ui_auth's {@code AuthStateChangeAction<T>}. Generics are erased, so the
 * transpiler hands the action its {@code T} ({@link #providedType}) and the action runs only
 * for a state of that type: an action for {@code SignedIn} is never called with a
 * {@code UserCreated}, whose callback would read it as the wrong class. Written without a
 * type argument it runs for every state.
 */
public class AuthStateChangeAction<T> {
    private final Funcs.VoidFunc2<BuildContext, T> callback;
    private Class<?> type;

    public AuthStateChangeAction(Funcs.VoidFunc2<BuildContext, T> callback) {
        this.callback = callback;
    }

    public AuthStateChangeAction<T> providedType(Class<?> t) {
        type = t;
        return this;
    }

    @SuppressWarnings("unchecked")
    void fire(BuildContext context, AuthState state) {
        if (callback != null && (type == null || type.isInstance(state))) {
            callback.call(context, (T) state);
        }
    }

    static void fireAll(Iterable<?> actions, BuildContext context, AuthState state) {
        if (actions == null) {
            return;
        }
        for (Object a : actions) {
            if (a instanceof AuthStateChangeAction) {
                ((AuthStateChangeAction<?>) a).fire(context, state);
            }
        }
    }
}
