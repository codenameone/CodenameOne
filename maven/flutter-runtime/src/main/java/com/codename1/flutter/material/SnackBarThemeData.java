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
 * Material {@code SnackBarThemeData}: write-once snack-bar styling. Named Dart
 * constructor parameters map to setter methods; unset values stay null.
 */
public class SnackBarThemeData {

    private Color backgroundColor;
    private Color actionTextColor;
    private Color disabledActionTextColor;
    private TextStyle contentTextStyle;
    private Double elevation;
    private Object shape;
    private SnackBarBehavior behavior;
    private Double width;
    private Object insetPadding;
    private Boolean showCloseIcon;
    private Color closeIconColor;

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void actionTextColor(Color v) {
        this.actionTextColor = v;
    }

    public void disabledActionTextColor(Color v) {
        this.disabledActionTextColor = v;
    }

    public void contentTextStyle(TextStyle v) {
        this.contentTextStyle = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public void behavior(SnackBarBehavior v) {
        this.behavior = v;
    }

    public void width(double v) {
        this.width = v;
    }

    public void insetPadding(Object v) {
        this.insetPadding = v;
    }

    public void showCloseIcon(boolean v) {
        this.showCloseIcon = v;
    }

    public void closeIconColor(Color v) {
        this.closeIconColor = v;
    }

    public Color backgroundColor() {
        return backgroundColor;
    }

    public SnackBarBehavior behavior() {
        return behavior;
    }
}
