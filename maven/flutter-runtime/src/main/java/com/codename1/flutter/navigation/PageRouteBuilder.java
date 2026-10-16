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

import com.codename1.flutter.Color;

import dart.core.Duration;

/**
 * A {@link Route} whose page and transition are supplied by builder callbacks —
 * Flutter's {@code PageRouteBuilder}. {@code pageBuilder} produces the
 * destination widget and {@code transitionsBuilder} wraps it in the animated
 * transition; both receive the {@code (context, animation, secondaryAnimation)}
 * triple. This pass records the callbacks and route configuration; driving the
 * transition animation is deferred.
 *
 * @param <T> the value the route completes with when popped
 */
public class PageRouteBuilder<T> extends Route<T> {

    private RouteSettings settings;
    private Object pageBuilder;
    private Object transitionsBuilder;
    private Duration transitionDuration;
    private Duration reverseTransitionDuration;
    private boolean opaque = true;
    private boolean barrierDismissible;
    private Color barrierColor;
    private String barrierLabel;
    private boolean maintainState = true;
    private boolean fullscreenDialog;

    public void settings(RouteSettings v) {
        this.settings = v;
    }

    public void pageBuilder(dart.runtime.Funcs.Func3<com.codename1.flutter.BuildContext,
            com.codename1.flutter.animation.Animation<Double>,
            com.codename1.flutter.animation.Animation<Double>, com.codename1.flutter.Widget> v) {
        this.pageBuilder = v;
    }

    public void transitionsBuilder(dart.runtime.Funcs.Func4<com.codename1.flutter.BuildContext,
            com.codename1.flutter.animation.Animation<Double>,
            com.codename1.flutter.animation.Animation<Double>,
            com.codename1.flutter.Widget, com.codename1.flutter.Widget> v) {
        this.transitionsBuilder = v;
    }

    public void transitionDuration(Duration v) {
        this.transitionDuration = v;
    }

    public void reverseTransitionDuration(Duration v) {
        this.reverseTransitionDuration = v;
    }

    public void opaque(boolean v) {
        this.opaque = v;
    }

    public void barrierDismissible(boolean v) {
        this.barrierDismissible = v;
    }

    public void barrierColor(Color v) {
        this.barrierColor = v;
    }

    public void barrierLabel(String v) {
        this.barrierLabel = v;
    }

    public void maintainState(boolean v) {
        this.maintainState = v;
    }

    public void fullscreenDialog(boolean v) {
        this.fullscreenDialog = v;
    }

    public RouteSettings getSettings() {
        return settings;
    }

    public Object getPageBuilder() {
        return pageBuilder;
    }

    public Object getTransitionsBuilder() {
        return transitionsBuilder;
    }

    public Duration getTransitionDuration() {
        return transitionDuration;
    }

    @Override
    @SuppressWarnings("unchecked")
    public com.codename1.flutter.Widget buildPage(com.codename1.flutter.BuildContext context) {
        if (!(pageBuilder instanceof dart.runtime.Funcs.Func3)) {
            return null;
        }
        // The two animations a page builder is handed; a route shown without a
        // transition is at its end state.
        return (com.codename1.flutter.Widget) ((dart.runtime.Funcs.Func3<Object, Object, Object, Object>)
                pageBuilder).call(context, null, null);
    }
}
