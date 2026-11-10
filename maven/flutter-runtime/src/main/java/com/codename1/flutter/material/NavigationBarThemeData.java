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
import com.codename1.flutter.widgets.Kit;
import dart.core.DartList;
import dart.runtime.Funcs;
import com.codename1.flutter.Color;
/** Flutter's {@code NavigationBarThemeData}, read by {@link NavigationBar}. */
public class NavigationBarThemeData {
    private Color backgroundColor;
    private Color indicatorColor;
    private Double height;
    private Object labelTextStyle;
    private Object labelBehavior;

    public void backgroundColor(Color v) { this.backgroundColor = v; }
    public void indicatorColor(Color v) { this.indicatorColor = v; }
    public void height(Double v) { this.height = v; }
    public void labelTextStyle(Object v) { this.labelTextStyle = v; }
    public void labelBehavior(Object v) { this.labelBehavior = v; }
    public void elevation(Double v) { }
    public void iconTheme(Object v) { }
    public void surfaceTintColor(Object v) { }
    public void shadowColor(Object v) { }
    public void indicatorShape(Object v) { }

    public Color getBackgroundColor() { return backgroundColor; }
    public Color getIndicatorColor() { return indicatorColor; }
    public Double getHeight() { return height; }
    public Object getLabelTextStyle() { return labelTextStyle; }
    public Object getLabelBehavior() { return labelBehavior; }
}
