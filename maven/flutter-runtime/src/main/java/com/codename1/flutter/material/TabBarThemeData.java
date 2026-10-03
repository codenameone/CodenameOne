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
 * Material {@code TabBarThemeData}: the Material-3 rename of {@link TabBarTheme};
 * same write-once tab-bar styling shape.
 */
public class TabBarThemeData {

    private Color indicatorColor;
    private Color labelColor;
    private Color unselectedLabelColor;
    private TextStyle labelStyle;
    private TextStyle unselectedLabelStyle;
    private Object indicator;
    private Object indicatorSize;
    private Object labelPadding;
    private Object overlayColor;
    private Object dividerColor;

    public void indicatorColor(Color v) {
        this.indicatorColor = v;
    }

    public void labelColor(Color v) {
        this.labelColor = v;
    }

    public void unselectedLabelColor(Color v) {
        this.unselectedLabelColor = v;
    }

    public void labelStyle(TextStyle v) {
        this.labelStyle = v;
    }

    public void unselectedLabelStyle(TextStyle v) {
        this.unselectedLabelStyle = v;
    }

    public void indicator(Object v) {
        this.indicator = v;
    }

    public void indicatorSize(Object v) {
        this.indicatorSize = v;
    }

    public void labelPadding(Object v) {
        this.labelPadding = v;
    }

    public void overlayColor(Object v) {
        this.overlayColor = v;
    }

    public void dividerColor(Object v) {
        this.dividerColor = v;
    }

    public Color labelColor() {
        return labelColor;
    }
}
