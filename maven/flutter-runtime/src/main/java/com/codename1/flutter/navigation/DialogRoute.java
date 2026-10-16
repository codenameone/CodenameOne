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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A modal route that displays a dialog above the current page — Flutter's
 * {@code DialogRoute<T>}. Pushed onto the {@link Navigator}; the {@code builder}
 * produces the dialog content lazily when the route is shown. This pass records
 * the builder, barrier appearance and settings for API shape; the actual modal
 * presentation is handled by the navigation layer.
 *
 * @param <T> the value the route completes with when popped
 */
public class DialogRoute<T> extends Route<T> {

    private BuildContext context;
    private Funcs.Func1<BuildContext, Widget> builder;
    private Object settings;
    private Color barrierColor;
    private boolean barrierDismissible = true;
    private String barrierLabel;
    private boolean useSafeArea = true;

    public void context(BuildContext v) {
        this.context = v;
    }

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    public void settings(Object v) {
        this.settings = v;
    }

    public void barrierColor(Color v) {
        this.barrierColor = v;
    }

    public void barrierDismissible(boolean v) {
        this.barrierDismissible = v;
    }

    public void barrierLabel(String v) {
        this.barrierLabel = v;
    }

    public void useSafeArea(boolean v) {
        this.useSafeArea = v;
    }

    public void themes(Object v) {
    }

    public void anchorPoint(Object v) {
    }

    public void traversalEdgeBehavior(Object v) {
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }
}
