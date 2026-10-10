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

import com.codename1.flutter.Color;
import com.codename1.flutter.TextStyle;

/**
 * Theming values for descendant {@link NavigationRail}s — Flutter's
 * {@code NavigationRailThemeData}. Reached both as a constructed theme value and
 * via {@code Theme.of(context).navigationRailTheme}; the reply study reads
 * {@link #unselectedLabelTextStyle()} for its folder-section colors.
 */
public class NavigationRailThemeData {

    private Color backgroundColor;
    private double elevation;
    private TextStyle unselectedLabelTextStyle;
    private TextStyle selectedLabelTextStyle;
    private IconThemeData unselectedIconTheme;
    private IconThemeData selectedIconTheme;
    private double groupAlignment;
    private NavigationRailLabelType labelType;
    private boolean useIndicator;
    private Color indicatorColor;
    private Object indicatorShape;
    private double minWidth;
    private double minExtendedWidth;

    public void backgroundColor(Color v) { this.backgroundColor = v; }
    public void elevation(double v) { this.elevation = v; }
    public void unselectedLabelTextStyle(TextStyle v) { this.unselectedLabelTextStyle = v; }
    public void selectedLabelTextStyle(TextStyle v) { this.selectedLabelTextStyle = v; }
    public void unselectedIconTheme(IconThemeData v) { this.unselectedIconTheme = v; }
    public void selectedIconTheme(IconThemeData v) { this.selectedIconTheme = v; }
    public void groupAlignment(double v) { this.groupAlignment = v; }
    public void labelType(NavigationRailLabelType v) { this.labelType = v; }
    public void useIndicator(boolean v) { this.useIndicator = v; }
    public void indicatorColor(Color v) { this.indicatorColor = v; }
    public void indicatorShape(Object v) { this.indicatorShape = v; }
    public void minWidth(double v) { this.minWidth = v; }
    public void minExtendedWidth(double v) { this.minExtendedWidth = v; }

    public Color backgroundColor() { return backgroundColor; }
    public double elevation() { return elevation; }
    public TextStyle unselectedLabelTextStyle() { return unselectedLabelTextStyle; }
    public TextStyle selectedLabelTextStyle() { return selectedLabelTextStyle; }
    public IconThemeData unselectedIconTheme() { return unselectedIconTheme; }
    public IconThemeData selectedIconTheme() { return selectedIconTheme; }
}
