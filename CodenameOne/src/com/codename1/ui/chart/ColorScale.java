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
package com.codename1.ui.chart;

/// Maps numbers to colours for heatmaps: a sequence of colour stops interpolated linearly.
///
/// ```java
/// ColorScale temp = ColorScale.diverging(0x2166AC, 0xF7F7F7, 0xB2182B);
/// int rgb = temp.colorAt(0.75);
/// ```
public class ColorScale {
    private final double[] positions;
    private final int[] colors;

    /// Creates a scale from stops.
    ///
    /// #### Parameters
    ///
    /// - `positions`: ascending stop positions from 0 to 1
    ///
    /// - `colors`: the colour at each stop
    public ColorScale(double[] positions, int[] colors) {
        if (positions == null || colors == null || positions.length == 0
                || positions.length != colors.length) {
            // interpolation pairs stops by index: a missing or unpaired stop fails here, not
            // as an index error in the first paint
            throw new IllegalArgumentException("positions and colors must be non-empty and of equal length");
        }
        for (int i = 0; i < positions.length; i++) {
            double v = positions[i];
            // interpolation searches the stops in order: NaN, an infinity or a step back finds the wrong pair
            if (!(v > Double.NEGATIVE_INFINITY && v < Double.POSITIVE_INFINITY) || i > 0 && v < positions[i - 1]) { //NOPMD LogicInversion - also true for NaN
                throw new IllegalArgumentException("positions must be finite and non-decreasing");
            }
        }
        this.positions = new double[positions.length];
        this.colors = new int[colors.length];
        System.arraycopy(positions, 0, this.positions, 0, positions.length);
        System.arraycopy(colors, 0, this.colors, 0, colors.length);
    }

    /// A single hue scale from a pale tint to the full colour, for magnitudes.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the colour of the largest values
    ///
    /// #### Returns
    ///
    /// the scale
    public static ColorScale sequential(int rgb) {
        return new ColorScale(new double[]{0, 1}, new int[]{mix(rgb, 0xffffff, 0.88f), rgb});
    }

    /// A two hue scale through a neutral middle, for values above and below a reference.
    ///
    /// #### Parameters
    ///
    /// - `low`: the colour of the smallest values
    ///
    /// - `mid`: the neutral middle colour
    ///
    /// - `high`: the colour of the largest values
    ///
    /// #### Returns
    ///
    /// the scale
    public static ColorScale diverging(int low, int mid, int high) {
        return new ColorScale(new double[]{0, 0.5, 1}, new int[]{low, mid, high});
    }

    /// A perceptually uniform scale (viridis) that stays readable in greyscale and for
    /// colour-blind readers.
    ///
    /// #### Returns
    ///
    /// the scale
    public static ColorScale viridis() {
        return new ColorScale(new double[]{0, 0.25, 0.5, 0.75, 1},
                new int[]{0x440154, 0x3B528B, 0x21918C, 0x5EC962, 0xFDE725});
    }

    /// The colour at a position.
    ///
    /// #### Parameters
    ///
    /// - `f`: 0 to 1; values outside are clamped
    ///
    /// #### Returns
    ///
    /// an RGB colour
    public int colorAt(double f) {
        if (f != f) {
            return colors[0];
        }
        if (f <= positions[0]) {
            return colors[0];
        }
        int n = positions.length;
        if (f >= positions[n - 1]) {
            return colors[n - 1];
        }
        for (int i = 1; i < n; i++) {
            if (f <= positions[i]) {
                double span = positions[i] - positions[i - 1];
                float t = span <= 0 ? 1 : (float) ((f - positions[i - 1]) / span);
                return mix(colors[i - 1], colors[i], t);
            }
        }
        return colors[n - 1];
    }

    static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xff) * (1 - t) + ((b >> 16) & 0xff) * t + 0.5f);
        int g = (int) (((a >> 8) & 0xff) * (1 - t) + ((b >> 8) & 0xff) * t + 0.5f);
        int bl = (int) ((a & 0xff) * (1 - t) + (b & 0xff) * t + 0.5f);
        return (r << 16) | (g << 8) | bl;
    }
}
