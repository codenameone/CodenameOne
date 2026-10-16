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

/**
 * The lowest-level material button — Flutter's {@code RawMaterialButton}.
 * Shares {@link ButtonBase}'s press/child plumbing and adds a raw
 * {@code fillColor}. Used by color-picker swatches and other custom buttons
 * that want the material tap semantics without the higher-level button styles.
 */
public class RawMaterialButton extends ButtonBase {

    private Color fillColor;
    private Double elevation;
    private com.codename1.flutter.EdgeInsets padding;
    private Object shape;

    public void fillColor(Color v) {
        this.fillColor = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void padding(com.codename1.flutter.EdgeInsets v) {
        this.padding = v;
    }

    public void shape(Object v) {
        this.shape = v;
    }

    public Color getFillColor() {
        return fillColor;
    }
}
