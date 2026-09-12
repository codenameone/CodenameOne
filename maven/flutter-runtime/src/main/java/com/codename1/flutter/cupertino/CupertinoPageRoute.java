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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Animation;
import com.codename1.flutter.navigation.Route;

import dart.runtime.Funcs;

/**
 * A route that presents its page with the iOS slide-in transition — Flutter's
 * {@code CupertinoPageRoute}. The transition itself is not animated this pass;
 * the route carries the page {@code builder}, optional {@code settings} and
 * {@code title}. Subclassable (a demo overrides {@link #buildTransitions} to
 * disable the animation).
 *
 * @param <T> the value type the route completes with when popped
 */
public class CupertinoPageRoute<T> extends Route<T> {

    private Funcs.Func1<BuildContext, Widget> builder;
    private Object settings;
    private String title;

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public void settings(Object v) {
        this.settings = v;
    }

    public void title(String v) {
        this.title = v;
    }

    public void maintainState(boolean v) {
    }

    public void fullscreenDialog(boolean v) {
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Wraps the page in its transition; the default is the identity (no
     * animation) — overridable by subclasses.
     */
    public Widget buildTransitions(BuildContext context, Animation<Double> animation,
                                   Animation<Double> secondaryAnimation, Widget child) {
        return child;
    }

    @Override
    public Widget buildPage(BuildContext context) {
        Funcs.Func1<BuildContext, Widget> b = getBuilder();
        return b == null ? null : b.call(context);
    }
}
