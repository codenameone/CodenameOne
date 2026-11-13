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
    private Double minimumSize;
    private com.codename1.flutter.rendering.Size minimumSizeWH;
    private com.codename1.flutter.rendering.Size maximumSize;
    private Color disabledForegroundColor;
    private Color disabledBackgroundColor;

    /**
     * The full {@code styleFrom}: the flat overrides plus a minimum and maximum size and
     * the colours used while the button is disabled.
     */
    public static ButtonStyle styleFrom(Color foregroundColor, Color backgroundColor, Color shadowColor,
                                        Double elevation, TextStyle textStyle, EdgeInsets padding,
                                        Object side, Object shape, Object alignment, Object tapTargetSize,
                                        Object visualDensity, com.codename1.flutter.rendering.Size minimumSize,
                                        com.codename1.flutter.rendering.Size maximumSize,
                                        Color disabledForegroundColor, Color disabledBackgroundColor) {
        ButtonStyle s = styleFrom(foregroundColor, backgroundColor, shadowColor, elevation, textStyle, padding,
                side, shape, alignment, tapTargetSize, visualDensity);
        s.minimumSizeWH = minimumSize;
        s.maximumSize = maximumSize;
        s.disabledForegroundColor = disabledForegroundColor;
        s.disabledBackgroundColor = disabledBackgroundColor;
        return s;
    }

    /** The two-dimensional minimum from {@code styleFrom(minimumSize:)}, or null. */
    public com.codename1.flutter.rendering.Size getMinimumSizeWH() {
        return minimumSizeWH;
    }

    /** The maximum from {@code styleFrom(maximumSize:)}, or null. */
    public com.codename1.flutter.rendering.Size getMaximumSize() {
        return maximumSize;
    }

    public Color getDisabledForegroundColor() {
        return disabledForegroundColor;
    }

    public Color getDisabledBackgroundColor() {
        return disabledBackgroundColor;
    }

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

    /**
     * A SQUARE minimum interactive size in logical pixels, or null when the role's own
     * default applies. Not part of {@link #styleFrom} (Flutter's own real
     * {@code minimumSize} is a {@code Size}, width and height separately, and no caller
     * in this runtime needs an asymmetric one) -- CupertinoButton sets it directly on the
     * built style, after composing onto {@link ElevatedButton}/{@link TextButton}, for
     * Flutter's {@code kMinInteractiveDimensionCupertino} (44).
     */
    public Double getMinimumSize() {
        return minimumSize;
    }

    /** @see #getMinimumSize() */
    public void minimumSize(Double v) {
        this.minimumSize = v;
    }

    /**
     * Flutter's {@code ButtonStyle.merge}: this style, with every property it
     * leaves null taken from {@code other}. A button's own style merged over its
     * theme's ({@code ElevatedButtonThemeData.style} and friends).
     */
    public ButtonStyle merge(ButtonStyle other) {
        if (other == null) {
            return this;
        }
        ButtonStyle r = new ButtonStyle();
        r.foregroundColor = foregroundColor != null ? foregroundColor : other.foregroundColor;
        r.backgroundColor = backgroundColor != null ? backgroundColor : other.backgroundColor;
        r.shadowColor = shadowColor != null ? shadowColor : other.shadowColor;
        r.elevation = elevation != null ? elevation : other.elevation;
        r.textStyle = textStyle != null ? textStyle : other.textStyle;
        r.padding = padding != null ? padding : other.padding;
        r.side = side != null ? side : other.side;
        r.shape = shape != null ? shape : other.shape;
        r.alignment = alignment != null ? alignment : other.alignment;
        r.tapTargetSize = tapTargetSize != null ? tapTargetSize : other.tapTargetSize;
        r.visualDensity = visualDensity != null ? visualDensity : other.visualDensity;
        r.minimumSize = minimumSize != null ? minimumSize : other.minimumSize;
        r.minimumSizeWH = minimumSizeWH != null ? minimumSizeWH : other.minimumSizeWH;
        r.maximumSize = maximumSize != null ? maximumSize : other.maximumSize;
        r.disabledForegroundColor = disabledForegroundColor != null ? disabledForegroundColor : other.disabledForegroundColor;
        r.disabledBackgroundColor = disabledBackgroundColor != null ? disabledBackgroundColor : other.disabledBackgroundColor;
        return r;
    }
}
