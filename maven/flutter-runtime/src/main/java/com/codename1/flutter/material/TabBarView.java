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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.DartList;

/**
 * The page view paired with a {@link TabBar} — Flutter's {@code TabBarView}.
 * Shows the child at the {@link TabController}'s current index (index 0 when no
 * controller is attached). The horizontal swipe transition between pages is
 * deferred; the selected page renders.
 */
public class TabBarView extends StatelessWidget {

    private DartList<Widget> children;
    private TabController controller;

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public void controller(TabController v) {
        this.controller = v;
    }

    public void physics(Object v) {
    }

    public void dragStartBehavior(Object v) {
    }

    public void viewportFraction(double v) {
    }

    public void clipBehavior(Clip v) {
    }

    @Override
    public Widget build(BuildContext context) {
        if (children == null || children.size() == 0) {
            return new SizedBox();
        }
        int idx = controller != null ? (int) controller.index() : 0;
        if (idx < 0 || idx >= children.size()) {
            idx = 0;
        }
        return children.get(idx);
    }
}
