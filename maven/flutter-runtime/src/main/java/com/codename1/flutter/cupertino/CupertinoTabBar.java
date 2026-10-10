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
import com.codename1.flutter.material.BottomNavigationBarItem;
import com.codename1.flutter.widgets.Container;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * The iOS bottom tab bar — Flutter's {@code CupertinoTabBar}. Used to
 * configure a {@link CupertinoTabScaffold}; the bar itself composes an empty
 * {@link Container} placeholder this pass (the tab items are captured).
 */
public class CupertinoTabBar extends StatelessWidget {

    private DartList<BottomNavigationBarItem> items;
    private Funcs.VoidFunc1<Long> onTap;

    public void items(DartList<BottomNavigationBarItem> v) {
        this.items = v;
    }

    public void onTap(Funcs.VoidFunc1<Long> v) {
        this.onTap = v;
    }

    public void currentIndex(long v) {
    }

    public void backgroundColor(Color v) {
    }

    public void activeColor(Color v) {
    }

    public void inactiveColor(Color v) {
    }

    public void iconSize(double v) {
    }

    public void border(Object v) {
    }

    public DartList<BottomNavigationBarItem> getItems() {
        return items;
    }

    @Override
    public Widget build(BuildContext context) {
        return new Container();
    }
}
