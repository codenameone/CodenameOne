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
 * Material {@code AppBarTheme}: write-once app-bar styling. Named Dart
 * constructor parameters map to setter methods; unset values stay null.
 */
public class AppBarTheme {

    private Color backgroundColor;
    private Color foregroundColor;
    private Color color;
    private Color shadowColor;
    private Color surfaceTintColor;
    private Double elevation;
    private Double scrolledUnderElevation;
    private IconThemeData iconTheme;
    private IconThemeData actionsIconTheme;
    private TextStyle titleTextStyle;
    private TextStyle toolbarTextStyle;
    private Boolean centerTitle;
    private Double titleSpacing;
    private Double toolbarHeight;
    private Object systemOverlayStyle;
    private Object shape;

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void foregroundColor(Color v) {
        this.foregroundColor = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void shadowColor(Color v) {
        this.shadowColor = v;
    }

    public void surfaceTintColor(Color v) {
        this.surfaceTintColor = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void scrolledUnderElevation(double v) {
        this.scrolledUnderElevation = v;
    }

    public void iconTheme(IconThemeData v) {
        this.iconTheme = v;
    }

    public void actionsIconTheme(IconThemeData v) {
        this.actionsIconTheme = v;
    }

    public void titleTextStyle(TextStyle v) {
        this.titleTextStyle = v;
    }

    public void toolbarTextStyle(TextStyle v) {
        this.toolbarTextStyle = v;
    }

    public void centerTitle(boolean v) {
        this.centerTitle = v;
    }

    public void titleSpacing(double v) {
        this.titleSpacing = v;
    }

    public void toolbarHeight(double v) {
        this.toolbarHeight = v;
    }

    public void systemOverlayStyle(Object v) {
        this.systemOverlayStyle = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    /**
     * The bar's fill. {@code color} is Flutter's older name for the same slot and
     * apps still use it — the gallery themes every demo page's bar with
     * {@code AppBarTheme(color: colorScheme.primary)}, so reading only
     * {@code backgroundColor} left every demo bar the default grey instead of
     * Material purple.
     */
    public Color backgroundColor() {
        return backgroundColor != null ? backgroundColor : color;
    }

    public Double elevation() {
        return elevation;
    }

    public IconThemeData iconTheme() {
        return iconTheme;
    }

    /** {@code AppBarTheme.actionsIconTheme}, falling back to the bar's icon theme. */
    public IconThemeData actionsIconTheme() {
        return actionsIconTheme != null ? actionsIconTheme : iconTheme;
    }

    public Color foregroundColor() {
        return foregroundColor;
    }

    public TextStyle titleTextStyle() {
        return titleTextStyle;
    }

    public TextStyle toolbarTextStyle() {
        return toolbarTextStyle;
    }

    public Object shape() {
        return shape;
    }

    public Boolean centerTitle() {
        return centerTitle;
    }
}
