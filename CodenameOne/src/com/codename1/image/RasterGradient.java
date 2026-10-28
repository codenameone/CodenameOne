/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.image;

/// A linear or radial color gradient for [RasterGraphics#setPaint(RasterPaint)].
///
/// Colors are interpolated per channel, not premultiplied, between stops at
/// increasing fractions from 0 to 1. Before the first stop and after the last
/// one the end colors extend ("pad").
@com.codename1.impl.SharedWithBackend
public final class RasterGradient implements RasterPaint {
    private final boolean radial;
    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;
    private final double radius;
    private final float[] fractions;
    private final int[] colors;

    private RasterGradient(boolean radial, double x1, double y1, double x2, double y2, double radius,
                           float[] fractions, int[] colors) {
        if (fractions.length != colors.length || colors.length == 0) {
            throw new IllegalArgumentException("one color per fraction is required");
        }
        for (int i = 1; i < fractions.length; i++) {
            if (fractions[i] < fractions[i - 1]) {
                throw new IllegalArgumentException("fractions must not decrease");
            }
        }
        this.radial = radial;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.radius = radius;
        this.fractions = new float[fractions.length];
        System.arraycopy(fractions, 0, this.fractions, 0, fractions.length);
        this.colors = new int[colors.length];
        System.arraycopy(colors, 0, this.colors, 0, colors.length);
    }

    /// Creates a gradient along the line from `(x1, y1)` (fraction 0) to
    /// `(x2, y2)` (fraction 1).
    ///
    /// #### Parameters
    ///
    /// - `x1`: start x
    /// - `y1`: start y
    /// - `x2`: end x
    /// - `y2`: end y
    /// - `fractions`: stop positions, 0 to 1, not decreasing
    /// - `colors`: ARGB color of each stop
    ///
    /// #### Returns
    ///
    /// the gradient
    public static RasterGradient linear(double x1, double y1, double x2, double y2, float[] fractions,
                                        int[] colors) {
        return new RasterGradient(false, x1, y1, x2, y2, 0, fractions, colors);
    }

    /// Creates a circular gradient from the center (fraction 0) to the
    /// circle of `radius` (fraction 1).
    ///
    /// #### Parameters
    ///
    /// - `cx`: center x
    /// - `cy`: center y
    /// - `radius`: radius, above zero
    /// - `fractions`: stop positions, 0 to 1, not decreasing
    /// - `colors`: ARGB color of each stop
    ///
    /// #### Returns
    ///
    /// the gradient
    public static RasterGradient radial(double cx, double cy, double radius, float[] fractions, int[] colors) {
        if (radius <= 0) {
            throw new IllegalArgumentException("radius must be positive");
        }
        return new RasterGradient(true, cx, cy, 0, 0, radius, fractions, colors);
    }

    /// Returns the gradient's color at a point.
    ///
    /// #### Parameters
    ///
    /// - `x`: user-space x
    /// - `y`: user-space y
    ///
    /// #### Returns
    ///
    /// the ARGB color
    @Override
    public int getColor(double x, double y) {
        double t;
        if (radial) {
            double dx = x - x1;
            double dy = y - y1;
            t = Math.sqrt(dx * dx + dy * dy) / radius;
        } else {
            double vx = x2 - x1;
            double vy = y2 - y1;
            double len2 = vx * vx + vy * vy;
            t = len2 == 0 ? 0 : ((x - x1) * vx + (y - y1) * vy) / len2;
        }
        return colorAt(t);
    }

    private int colorAt(double t) {
        int n = fractions.length;
        if (t <= fractions[0]) {
            return colors[0];
        }
        if (t >= fractions[n - 1]) {
            return colors[n - 1];
        }
        int i = 1;
        while (fractions[i] < t) {
            i++;
        }
        double span = fractions[i] - fractions[i - 1];
        double f = span <= 0 ? 1 : (t - fractions[i - 1]) / span;
        return lerp(colors[i - 1], colors[i], f);
    }

    static int lerp(int a, int b, double f) {
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int ca = a >>> s & 0xff;
            int cb = b >>> s & 0xff;
            r |= ((int) (ca + (cb - ca) * f + 0.5) & 0xff) << s;
        }
        return r;
    }
}
