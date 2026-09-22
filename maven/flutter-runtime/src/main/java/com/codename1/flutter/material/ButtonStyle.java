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
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.TextStyle;

/**
 * The visual overrides a material button applies on top of its theme
 * defaults. Only produced by the buttons' {@code styleFrom} factory (see
 * {@link ElevatedButton#styleFrom}, {@link TextButton#styleFrom},
 * {@link OutlinedButton#styleFrom}) and consumed opaquely through the buttons'
 * {@code style:} parameter.
 *
 * <p>The visual properties the render layer models — foreground/background
 * color, padding, elevation — are captured as flat fields; the geometric
 * overrides (side, shape, alignment, tap-target size, visual density) are held
 * opaquely for API shape in this pass.</p>
 */
public class ButtonStyle {

    private Color foregroundColor;
    private Color backgroundColor;
    private Color shadowColor;
    private Double elevation;
    private TextStyle textStyle;
    private EdgeInsets padding;
    private Object side;
    private Object shape;
    private Object alignment;
    private Object tapTargetSize;
    private Object visualDensity;

    /**
     * Builds a ButtonStyle from the flat overrides. Parameter order matches the
     * {@code styleFrom} Dart stub shared by the three button classes.
     */
    public static ButtonStyle styleFrom(Color foregroundColor, Color backgroundColor, Color shadowColor,
                                        Double elevation, TextStyle textStyle, EdgeInsets padding,
                                        Object side, Object shape, Object alignment, Object tapTargetSize,
                                        Object visualDensity) {
        ButtonStyle s = new ButtonStyle();
        s.foregroundColor = foregroundColor;
        s.backgroundColor = backgroundColor;
        s.shadowColor = shadowColor;
        s.elevation = elevation;
        s.textStyle = textStyle;
        s.padding = padding;
        s.side = side;
        s.shape = shape;
        s.alignment = alignment;
        s.tapTargetSize = tapTargetSize;
        s.visualDensity = visualDensity;
        return s;
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getShadowColor() {
        return shadowColor;
    }

    public Double getElevation() {
        return elevation;
    }

    public TextStyle getTextStyle() {
        return textStyle;
    }

    public EdgeInsets getPadding() {
        return padding;
    }

    /** The requested outline shape, or null when the role's default applies. */
    public Object getShape() {
        return shape;
    }
}
