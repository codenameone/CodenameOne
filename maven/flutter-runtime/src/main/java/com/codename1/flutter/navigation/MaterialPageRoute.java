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
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A route whose page is produced by a {@code WidgetBuilder}. Pushed with
 * {@link Navigator#push}; the builder runs lazily when the route's element
 * tree mounts, receiving a BuildContext inside the NEW page's tree.
 *
 * <p>The type parameter {@code T} is the route's result type (the value a
 * {@code Navigator.pop(result)} returns). It is phantom in this runtime but
 * lets transpiled {@code MaterialPageRoute<T>} subclasses and type arguments
 * resolve.</p>
 *
 * @param <T> the route's pop-result type
 */
public class MaterialPageRoute<T> extends Route<T> {

    private Funcs.Func1<BuildContext, Widget> builder;
    private Object maintainState;
    private Object fullscreenDialog;

    public void builder(Funcs.Func1<BuildContext, Widget> v) {
        this.builder = v;
    }

    /** Flutter's {@code maintainState} — whether the route stays mounted when covered. */
    public void maintainState(Object v) {
        this.maintainState = v;
    }

    /** Flutter's {@code fullscreenDialog} — whether the route is a full-screen modal. */
    public void fullscreenDialog(Object v) {
        this.fullscreenDialog = v;
    }

    @Override
    public boolean isFullscreenDialog() {
        return Boolean.TRUE.equals(fullscreenDialog);
    }

    public Funcs.Func1<BuildContext, Widget> getBuilder() {
        return builder;
    }

    @Override
    public Widget buildPage(BuildContext context) {
        Funcs.Func1<BuildContext, Widget> b = getBuilder();
        return b == null ? null : b.call(context);
    }
}
