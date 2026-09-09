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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.Duration;

/**
 * Creates a {@link TabController} and shares it with descendant {@link TabBar} /
 * {@link TabBarView} widgets — Flutter's {@code DefaultTabController}. This
 * milestone renders the subtree ({@code child}); the descendant tab widgets
 * currently default to index 0 rather than resolving the inherited controller,
 * so {@link #of} returns a fresh controller of the configured length.
 */
public class DefaultTabController extends StatelessWidget {

    private long length;
    private long initialIndex;
    private Widget child;

    public void length(long v) {
        this.length = v;
    }

    public void initialIndex(long v) {
        this.initialIndex = v;
    }

    public void animationDuration(Duration v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public static TabController of(BuildContext context) {
        TabController c = new TabController();
        c.length(1);
        return c;
    }

    @Override
    public Widget build(BuildContext context) {
        return child != null ? child : new SizedBox();
    }
}
