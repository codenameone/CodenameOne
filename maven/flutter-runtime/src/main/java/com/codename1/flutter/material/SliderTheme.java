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
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.InheritedWidget;

/**
 * Establishes an ambient {@link SliderThemeData} for its subtree — Flutter's
 * {@code SliderTheme}. Descendant {@code Slider}/{@code RangeSlider} widgets read
 * {@code SliderTheme.of(context)} for their visual configuration.
 *
 * <p>It is a real {@link InheritedWidget}: {@code of} used to return a fresh default no
 * matter what the tree said, so any slider styling in the app was silently discarded.</p>
 */
public class SliderTheme extends InheritedWidget {

    private SliderThemeData data;

    public void data(SliderThemeData v) {
        this.data = v;
    }

    public SliderThemeData getData() {
        return data;
    }

    /** Dart's {@code SliderTheme.of(context)}: the nearest enclosing slider theme. */
    public static SliderThemeData of(BuildContext context) {
        SliderTheme t = context == null ? null
                : context.dependOnInheritedWidgetOfExactType(SliderTheme.class);
        if (t != null && t.data != null) {
            return t.data;
        }
        return new SliderThemeData();
    }

    @Override
    public boolean updateShouldNotify(InheritedWidget oldWidget) {
        return !(oldWidget instanceof SliderTheme) || ((SliderTheme) oldWidget).data != data;
    }
}
