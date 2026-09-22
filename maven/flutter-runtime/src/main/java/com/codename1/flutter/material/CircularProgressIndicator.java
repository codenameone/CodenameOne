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
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

/**
 * A circular material progress indicator — Flutter's {@code
 * CircularProgressIndicator}. A determinate {@code value} or an indeterminate
 * spin are accepted; this pass reserves a square box sized to the default
 * indicator diameter, deferring the arc paint and spin animation.
 */
public class CircularProgressIndicator extends Widget {

    private Double value;
    private Color color;
    private Color backgroundColor;
    private Double strokeWidth;

    public void value(double v) {
        this.value = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void valueColor(Object v) {
    }

    public void strokeWidth(double v) {
        this.strokeWidth = v;
    }

    public void semanticsLabel(String v) {
    }

    public void semanticsValue(String v) {
    }

    public Double getValue() {
        return value;
    }

    /** The stroke width, or null for the Material default. */
    public Double getStrokeWidth() {
        return strokeWidth;
    }

    /** The arc colour, or null to take the theme's primary. */
    public Color getColor() {
        return color;
    }

    /** The track colour behind the arc, or null when there is none. */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new CircularProgressIndicatorRenderElement(this);
    }
}
