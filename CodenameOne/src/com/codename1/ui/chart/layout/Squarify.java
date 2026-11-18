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
package com.codename1.ui.chart.layout;

/// The squarified treemap layout of Bruls, Huizing and van Wijk: rectangles with areas
/// proportional to values and aspect ratios as close to square as the greedy row filling
/// allows, which keeps small cells readable.
public final class Squarify {
    private Squarify() {
    }

    /// Lays out values in a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `values`: the values, not necessarily sorted; negative values count as 0
    ///
    /// - `x`: the rectangle's left
    ///
    /// - `y`: the rectangle's top
    ///
    /// - `w`: the rectangle's width
    ///
    /// - `h`: the rectangle's height
    ///
    /// #### Returns
    ///
    /// one `{x, y, w, h}` per value, in the order given
    public static float[][] layout(double[] values, float x, float y, float w, float h) {
        int n = values.length;
        float[][] out = new float[n][4];
        double total = 0;
        for (int i = 0; i < n; i++) {
            total += pos(values[i]);
        }
        if (total <= 0 || w <= 0 || h <= 0) {
            for (int i = 0; i < n; i++) {
                out[i] = new float[]{x, y, 0, 0};
            }
            return out;
        }
        // biggest first
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        for (int a = 1; a < n; a++) {
            for (int b = a; b > 0 && values[order[b]] > values[order[b - 1]]; b--) {
                int t = order[b];
                order[b] = order[b - 1];
                order[b - 1] = t;
            }
        }
        double scale = (double) w * h / total;
        double[] area = new double[n];
        for (int i = 0; i < n; i++) {
            area[i] = pos(values[order[i]]) * scale;
        }
        double rx = x;
        double ry = y;
        double rw = w;
        double rh = h;
        int start = 0;
        while (start < n) {
            double side = Math.min(rw, rh);
            int end = start;
            double sum = area[start];
            double worst = worst(area, start, end, sum, side);
            while (end + 1 < n) {
                double s2 = sum + area[end + 1];
                double w2 = worst(area, start, end + 1, s2, side);
                if (w2 > worst) {
                    break;
                }
                end++;
                sum = s2;
                worst = w2;
            }
            // lay the row along the short side
            if (rw >= rh) {
                double cw = rh > 0 ? sum / rh : 0;
                double cy = ry;
                for (int k = start; k <= end; k++) {
                    double ch = cw > 0 ? area[k] / cw : 0;
                    out[order[k]] = new float[]{(float) rx, (float) cy, (float) cw, (float) ch};
                    cy += ch;
                }
                rx += cw;
                rw -= cw;
            } else {
                double rhh = rw > 0 ? sum / rw : 0;
                double cx = rx;
                for (int k = start; k <= end; k++) {
                    double cw = rhh > 0 ? area[k] / rhh : 0;
                    out[order[k]] = new float[]{(float) cx, (float) ry, (float) cw, (float) rhh};
                    cx += cw;
                }
                ry += rhh;
                rh -= rhh;
            }
            start = end + 1;
        }
        return out;
    }

    private static double worst(double[] area, int from, int to, double sum, double side) {
        if (sum <= 0 || side <= 0) {
            return Double.MAX_VALUE;
        }
        double max = 0;
        double min = Double.MAX_VALUE;
        for (int i = from; i <= to; i++) {
            max = Math.max(max, area[i]);
            min = Math.min(min, area[i]);
        }
        if (min <= 0) {
            return Double.MAX_VALUE;
        }
        double s2 = sum * sum;
        double side2 = side * side;
        return Math.max(side2 * max / s2, s2 / (side2 * min));
    }

    // NaN and negative values count as nothing; Math.max(0, NaN) is NaN and would poison the sums
    private static double pos(double v) {
        return !(v > 0) ? 0 : v; //NOPMD LogicInversion - the negated comparison is also true for NaN
    }
}
