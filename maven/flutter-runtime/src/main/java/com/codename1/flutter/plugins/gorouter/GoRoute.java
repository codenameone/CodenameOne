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
package com.codename1.flutter.plugins.gorouter;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * go_router's {@code GoRoute}: a path pattern ({@code /pets/:id}, or relative to its
 * parent when it has no leading slash) and the builder of the page shown for it.
 */
public class GoRoute extends RouteBase {
    private String path;
    private String name;
    private Funcs.Func2<BuildContext, GoRouterState, Widget> builder;
    private Funcs.Func2<BuildContext, GoRouterState, Object> pageBuilder;
    private Funcs.Func2<BuildContext, GoRouterState, Object> redirect;

    public GoRoute() {
    }

    public void path(String v) {
        this.path = v;
    }

    public String path() {
        return path;
    }

    public void name(String v) {
        this.name = v;
    }

    public String name() {
        return name;
    }

    public void builder(Funcs.Func2<BuildContext, GoRouterState, Widget> v) {
        this.builder = v;
    }

    /**
     * A builder of a {@code Page}. Codename One has no page objects of its own, so the
     * page's child is what gets shown -- see {@link GoRouter#buildPage}.
     */
    public void pageBuilder(Funcs.Func2<BuildContext, GoRouterState, Object> v) {
        this.pageBuilder = v;
    }

    public void redirect(Funcs.Func2<BuildContext, GoRouterState, Object> v) {
        this.redirect = v;
    }

    Funcs.Func2<BuildContext, GoRouterState, Widget> builder() {
        return builder;
    }

    Funcs.Func2<BuildContext, GoRouterState, Object> pageBuilder() {
        return pageBuilder;
    }

    Funcs.Func2<BuildContext, GoRouterState, Object> redirectFunc() {
        return redirect;
    }
}
