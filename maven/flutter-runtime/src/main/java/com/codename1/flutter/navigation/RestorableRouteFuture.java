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
package com.codename1.flutter.navigation;

import com.codename1.flutter.RestorableProperty;

import dart.runtime.Funcs;

/**
 * A restorable object that can imperatively push a route and complete with its
 * result ({@code RestorableRouteFuture<T>} in Flutter). The gallery constructs it
 * with an {@code onPresent} callback (which pushes a route on a
 * {@link NavigatorState}) and an optional {@code onComplete} callback, then calls
 * {@link #present(Object)} from button handlers.
 *
 * <p>Route restoration is not persisted; this holds the callbacks and drives them
 * within a single session. Because the surrounding {@code Navigator} static
 * helpers (owned by the navigation category) are needed to actually resolve a
 * {@link NavigatorState}, this is a minimal, API-complete implementation.</p>
 *
 * @param <T> the result type produced when the pushed route completes
 */
public class RestorableRouteFuture<T> extends RestorableProperty<Object> {

    private Funcs.Func2<NavigatorState, Object, String> onPresent;
    private Funcs.VoidFunc1<T> onComplete;
    private boolean present;
    private String route;

    public RestorableRouteFuture() {
    }

    /** Named constructor parameter {@code onPresent:} — pushes the route. */
    public void onPresent(Funcs.Func2<NavigatorState, Object, String> callback) {
        this.onPresent = callback;
    }

    /** Named constructor parameter {@code onComplete:} — receives the result. */
    public void onComplete(Funcs.VoidFunc1<T> callback) {
        this.onComplete = callback;
    }

    /**
     * Presents the route: {@code onPresent} pushes it on the navigator of the State
     * that registered this property (Flutter's default navigatorFinder), and the
     * route's result, when it pops, goes to {@code onComplete}. This used to set a
     * flag only, so the route or dialog never opened.
     */
    public void present(Object arguments) {
        NavigatorState nav = Navigator.of(ownerContext(), Boolean.FALSE);
        String id = onPresent == null ? null : onPresent.call(nav, arguments);
        this.route = id;
        this.present = true;
        notifyListeners();
        dart.async.Future<Object> result = Navigator.claimRestorableResult(id);
        if (result != null) {
            result.then(new Funcs.VoidFunc1<Object>() {
                @Override
                public void call(Object value) {
                    complete(value);
                }
            });
        }
    }

    public boolean isPresent() {
        return present;
    }

    /** The restoration id of the presented route while it is showing, else null. */
    public String route() {
        return present ? route : null;
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    /** Deliver a route result to the onComplete callback (single-session use). */
    @SuppressWarnings("unchecked")
    void complete(Object result) {
        this.present = false;
        this.route = null;
        if (onComplete != null) {
            onComplete.call((T) result);
        }
        notifyListeners();
    }
}
