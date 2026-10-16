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

import com.codename1.flutter.BorderRadius;
import com.codename1.flutter.Color;
import com.codename1.flutter.ShapeBorder;
import com.codename1.flutter.widgets.GestureDetector;

/**
 * The material tap-target with ink feedback — Flutter's {@code InkResponse}
 * (the superclass of {@link InkWell}). M2 renders it exactly like a
 * {@link GestureDetector} (transparent overlay, no ripple); the ink splash and
 * highlight are retained as configuration for a later milestone.
 */
public class InkResponse extends GestureDetector {

    private Color splashColor;
    private Color highlightColor;
    private Color focusColor;
    private Color hoverColor;
    private ShapeBorder customBorder;
    private BorderRadius borderRadius;
    private Double radius;
    private Boolean containedInkWell;

    public void splashColor(Color v) {
        this.splashColor = v;
    }

    public void highlightColor(Color v) {
        this.highlightColor = v;
    }

    public void focusColor(Color v) {
        this.focusColor = v;
    }

    public void hoverColor(Color v) {
        this.hoverColor = v;
    }

    public void customBorder(ShapeBorder v) {
        this.customBorder = v;
    }

    public void borderRadius(BorderRadius v) {
        this.borderRadius = v;
    }

    public void radius(double v) {
        this.radius = v;
    }

    public void containedInkWell(boolean v) {
        this.containedInkWell = v;
    }

    public Color getSplashColor() {
        return splashColor;
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    public BorderRadius getBorderRadius() {
        return borderRadius;
    }

    /** The explicit splash radius in logical pixels, or null to size it to the box. */
    public Double getRadius() {
        return radius;
    }
}
