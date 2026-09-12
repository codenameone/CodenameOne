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
 * A description of a gradient that can produce a {@link Shader} for a given
 * rectangle — Flutter's {@code Gradient}. Concrete subtypes are
 * {@link LinearGradient}, {@link RadialGradient} and {@link SweepGradient}.
 *
 * <p>Only the color list is retained by the Codename One runtime for this
 * milestone; geometry (begin/end/center/stops) is held but not yet painted.</p>
 */
public abstract class Gradient {

    Object begin;
    Object end;
    Object center;
    Object colors;
    Object stops;
    TileMode tileMode = TileMode.clamp;
    Object transform;

    public void begin(Object v) {
        this.begin = v;
    }

    public void end(Object v) {
        this.end = v;
    }

    public void center(Object v) {
        this.center = v;
    }

    public void colors(Object v) {
        this.colors = v;
    }

    public void stops(Object v) {
        this.stops = v;
    }

    public void tileMode(TileMode v) {
        this.tileMode = v == null ? TileMode.clamp : v;
    }

    public void transform(Object v) {
        this.transform = v;
    }

    public Object getColors() {
        return colors;
    }

    public Object getBegin() {
        return begin;
    }

    public Object getEnd() {
        return end;
    }

    /**
     * The gradient's colour ramp as ARGB values, in order, or an empty array when the
     * gradient carries no usable colours. Dart hands the list over as a {@code DartList} of
     * {@link Color}, so it arrives here as an untyped {@link java.util.List}.
     */
    public int[] colorRamp() {
        if (!(colors instanceof java.util.List)) {
            return new int[0];
        }
        java.util.List<?> list = (java.util.List<?>) colors;
        java.util.List<Integer> out = new java.util.ArrayList<Integer>();
        for (Object o : list) {
            if (o instanceof Color) {
                out.add(Integer.valueOf(((Color) o).argb()));
            }
        }
        int[] ramp = new int[out.size()];
        for (int i = 0; i < ramp.length; i++) {
            ramp[i] = out.get(i).intValue();
        }
        return ramp;
    }

    /** Produces a shader painting this gradient over {@code rect}. */
    public Shader createShader(Rect rect, Object textDirection) {
        return new GradientShader(this, rect);
    }

    /** A concrete {@link Shader} bound to a gradient and a rectangle. */
    public static final class GradientShader extends Shader {
        final Gradient gradient;
        final Rect rect;

        GradientShader(Gradient gradient, Rect rect) {
            this.gradient = gradient;
            this.rect = rect;
        }

        public Gradient gradient() {
            return gradient;
        }

        public Rect rect() {
            return rect;
        }
    }
}
