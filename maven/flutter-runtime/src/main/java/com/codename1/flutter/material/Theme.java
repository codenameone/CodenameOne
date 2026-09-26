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

import com.codename1.flutter.Brightness;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Applies a {@link ThemeData} to a subtree — Flutter's {@code Theme} widget —
 * and provides the static {@link #of(BuildContext)} lookup. As a widget it
 * simply renders its {@code child}; the {@code data} it carries is what a
 * descendant's {@code Theme.of(context)} resolves. When no {@code Theme}
 * ancestor is present, {@link #of} falls back to the nearest
 * {@link MaterialApp}'s effective theme (and a default {@link ThemeData} when
 * there is none).
 */
public class Theme extends com.codename1.flutter.widgets.InheritedWidget {

    private ThemeData data;

    public Theme() {
    }

    public void data(ThemeData v) {
        this.data = v;
    }

    public ThemeData getData() {
        return data;
    }

    @Override
    public boolean updateShouldNotify(com.codename1.flutter.widgets.InheritedWidget oldWidget) {
        return !(oldWidget instanceof Theme) || ((Theme) oldWidget).data != data;
    }

    public static ThemeData of(BuildContext context) {
        // An INHERITED lookup, so it is a hash lookup rather than a walk to the
        // root, and so a widget that reads the theme is rebuilt when the theme
        // changes. Theme.of is called by most themed widgets on every build;
        // as an ancestor search it was one of the hottest paths in the runtime.
        Theme t = context == null
                ? null
                : context.maybeDependOnInheritedWidgetOfExactType(Theme.class);
        if (t != null && t.data != null) {
            return t.data;
        }
        MaterialApp app = context == null
                ? null
                : context.findAncestorWidgetOfExactType(MaterialApp.class);
        if (app != null) {
            return app.effectiveTheme();
        }
        return new ThemeData();
    }

    /**
     * The brightness of the effective theme ({@code Theme.brightnessOf}).
     */
    public static Brightness brightnessOf(BuildContext context) {
        return of(context).brightness();
    }
}
