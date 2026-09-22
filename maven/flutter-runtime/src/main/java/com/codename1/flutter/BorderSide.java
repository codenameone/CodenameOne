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
package com.codename1.flutter;

/**
 * A side of a border: its color, width and line style — Flutter's
 * {@code BorderSide}.
 */
public final class BorderSide {

    public static final BorderSide none = makeNone();

    private Color color = new Color(0xFF000000L);
    private double width = 1.0;
    private BorderStyle style = BorderStyle.solid;

    /// Where the stroke sits relative to the path it follows: -1 fully inside, 0 centred
    /// on it, 1 fully outside. Codename One strokes centred, so these are carried for the
    /// geometry pass rather than honoured today - but a border naming one has to compile.
    public static final double strokeAlignInside = -1.0;
    public static final double strokeAlignCenter = 0.0;
    public static final double strokeAlignOutside = 1.0;

    private Double strokeAlign;

    public void strokeAlign(double v) {
        this.strokeAlign = v;
    }

    public Double getStrokeAlign() {
        return strokeAlign;
    }

    public BorderSide() {
    }

    private static BorderSide makeNone() {
        BorderSide b = new BorderSide();
        b.width = 0.0;
        b.style = BorderStyle.none;
        return b;
    }

    public Color color() {
        return color;
    }

    public void color(Color v) {
        this.color = v;
    }

    public double width() {
        return width;
    }

    public void width(double v) {
        this.width = v;
    }

    public BorderStyle style() {
        return style;
    }

    public void style(BorderStyle v) {
        this.style = v == null ? BorderStyle.solid : v;
    }

    /**
     * Dart's {@code BorderSide.lerp(a, b, t)}: linear interpolation between two
     * sides. Widths interpolate; the color and style are taken from the side the
     * blend is closest to. Deferred rendering does not read the result, so a
     * simple threshold blend is sufficient.
     */
    public static BorderSide lerp(BorderSide a, BorderSide b, double t) {
        if (a == null) return b;
        if (b == null) return a;
        BorderSide r = new BorderSide();
        r.width = a.width + (b.width - a.width) * t;
        BorderSide dominant = t < 0.5 ? a : b;
        r.color = dominant.color;
        r.style = dominant.style;
        return r;
    }

    /**
     * Dart's {@code BorderSide.toPaint()}: a stroking {@link Paint} for this
     * side (its color at its width, or a hairline fill when the style is none).
     */
    public Paint toPaint() {
        Paint p = new Paint();
        p.color(color);
        p.strokeWidth(width);
        p.style(PaintingStyle.stroke);
        return p;
    }

    /** Flutter's default {@code BorderSide()}: opaque black, one logical pixel. */
    public static BorderSide solidBlack() {
        BorderSide s = new BorderSide();
        s.color(new Color(0xFF000000L));
        s.width(1);
        return s;
    }
}
