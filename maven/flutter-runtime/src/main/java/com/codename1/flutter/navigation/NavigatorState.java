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

/**
 * The mutable state of a {@code Navigator} ({@code NavigatorState} in Flutter),
 * as reached via {@code Navigator.of(context)} or a {@code GlobalKey<NavigatorState>}.
 * Only the restoration-related push surface exercised by the gallery is modelled
 * here; each restorable push returns an opaque restoration id (a no-op string in
 * this implementation). The routing itself is handled by {@link Navigator}.
 */
public abstract class NavigatorState {

    /**
     * Push a route created by {@code routeBuilder}, returning a restoration id.
     * Restoration is not persisted, so the returned id is informational only.
     * {@code routeBuilder} is a {@code Route Function(BuildContext, Object?)}.
     */
    public String restorablePush(
            dart.runtime.Funcs.Func2<com.codename1.flutter.BuildContext, Object, Object> routeBuilder,
            Object arguments) {
        return "";
    }

    public String restorablePushNamed(String routeName, Object arguments) {
        return routeName == null ? "" : routeName;
    }

    public void pop(Object result) {
    }

    /**
     * Pop routes until {@code predicate} accepts the top route
     * ({@code NavigatorState.popUntil}). No route stack is kept, so this is a no-op.
     * {@code predicate} is a {@code bool Function(Route)}.
     */
    public void popUntil(dart.runtime.Funcs.Func1<Route<Object>, Boolean> predicate) {
    }

    /** Push a named route ({@code NavigatorState.pushNamed}); completes with its pop result. */
    public dart.async.Future<Object> pushNamed(String routeName, Object arguments) {
        return dart.async.Future.value(null);
    }

    /** Replace the current route ({@code NavigatorState.pushReplacementNamed}). */
    public dart.async.Future<Object> pushReplacementNamed(String routeName, Object arguments, Object result) {
        return dart.async.Future.value(null);
    }

    /** Pop if possible ({@code NavigatorState.maybePop}). */
    public dart.async.Future<Boolean> maybePop(Object result) {
        return dart.async.Future.value(Boolean.FALSE);
    }

    /**
     * Whether the navigator can pop the current route ({@code NavigatorState.canPop}).
     * This minimal model keeps no route stack, so it reports {@code false}.
     */
    public boolean canPop() {
        return false;
    }

    /**
     * Push the given route onto the navigator ({@code NavigatorState.push}); the
     * returned future completes with the route's pop result. Navigator's own states
     * override this; the base answers an already-completed null.
     */
    public dart.async.Future<Object> push(Object route) {
        return dart.async.Future.value(null);
    }
}
