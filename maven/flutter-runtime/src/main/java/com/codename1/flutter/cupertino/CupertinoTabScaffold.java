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
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A tabbed iOS page — Flutter's {@code CupertinoTabScaffold}: a bottom
 * {@link CupertinoTabBar} above a body produced per-tab by {@code tabBuilder}.
 * Tab switching is not wired this pass, so it builds and shows the first tab's
 * content (index 0).
 */
public class CupertinoTabScaffold extends StatelessWidget {

    private CupertinoTabBar tabBar;
    private Funcs.Func2<BuildContext, Long, Widget> tabBuilder;

    public void tabBar(CupertinoTabBar v) {
        this.tabBar = v;
    }

    public void tabBuilder(Funcs.Func2<BuildContext, Long, Widget> v) {
        this.tabBuilder = v;
    }

    public void controller(Object v) {
    }

    public void backgroundColor(Color v) {
    }

    public void resizeToAvoidBottomInset(boolean v) {
    }

    public void restorationId(String v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return tabBuilder == null ? null : tabBuilder.call(context, 0L);
    }
}
