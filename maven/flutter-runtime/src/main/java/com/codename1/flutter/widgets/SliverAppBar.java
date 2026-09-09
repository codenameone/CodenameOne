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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * An app bar that integrates into a {@link CustomScrollView} and can expand,
 * float, pin or snap as the user scrolls — Flutter's {@code SliverAppBar}. This
 * milestone renders the {@code title} (with {@code flexibleSpace} preferred when
 * present); the scroll-driven collapse/expand behavior is deferred.
 */
public class SliverAppBar extends StatelessWidget {

    private Widget title;
    private Widget leading;
    private DartList<Widget> actions;
    private Widget flexibleSpace;
    private Color backgroundColor;

    public void title(Widget v) {
        this.title = v;
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public void flexibleSpace(Widget v) {
        this.flexibleSpace = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void pinned(boolean v) {
    }

    public void floating(boolean v) {
    }

    public void snap(boolean v) {
    }

    public void expandedHeight(double v) {
    }

    public void automaticallyImplyLeading(boolean v) {
    }

    public void bottom(Widget v) {
    }

    public void centerTitle(boolean v) {
    }

    public void elevation(double v) {
    }

    @Override
    public Widget build(BuildContext context) {
        if (flexibleSpace != null) {
            return flexibleSpace;
        }
        if (title != null) {
            return title;
        }
        return new SizedBox();
    }
}
