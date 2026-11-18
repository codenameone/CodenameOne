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
package com.codename1.ui.chart.series;

/// Largest-Triangle-Three-Buckets downsampling (Steinarsson, 2013): picks the points of a long
/// series that keep its visual shape, so a million samples can be handed to a chart as a few
/// thousand.
///
/// A `LineSeries` already draws only the highest and lowest point of every pixel column, so it
/// stays fast without this. Use it to cut memory and the cost of animating, hit testing and
/// tooltips on very large data:
///
/// ```java
/// int[] keep = Downsample.lttb(x, y, 2000);
/// series.setData(Downsample.pick(x, keep), Downsample.pick(y, keep));
/// ```
public final class Downsample {
    private Downsample() {
    }

    /// The indices of the points LTTB keeps, ascending; the first and last are always kept.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x values, ascending; null means the index
    ///
    /// - `y`: the y values
    ///
    /// - `threshold`: how many points to keep, at least 3
    ///
    /// #### Returns
    ///
    /// the kept indices; every index when the series is already that short
    public static int[] lttb(double[] x, double[] y, int threshold) {
        int n = y.length;
        if (threshold >= n || threshold < 3) {
            int[] all = new int[n];
            for (int i = 0; i < n; i++) {
                all[i] = i;
            }
            return all;
        }
        int[] out = new int[threshold];
        double every = (n - 2) / (double) (threshold - 2);
        int a = 0;
        out[0] = 0;
        for (int i = 0; i < threshold - 2; i++) {
            // the average of the next bucket is the third corner of the triangle
            int avgStart = (int) Math.floor((i + 1) * every) + 1;
            int avgEnd = Math.min(n, (int) Math.floor((i + 2) * every) + 1);
            double avgX = 0;
            double avgY = 0;
            for (int j = avgStart; j < avgEnd; j++) {
                avgX += xAt(x, j);
                avgY += y[j];
            }
            int len = Math.max(1, avgEnd - avgStart);
            avgX /= len;
            avgY /= len;
            int start = (int) Math.floor(i * every) + 1;
            int end = (int) Math.floor((i + 1) * every) + 1;
            double ax = xAt(x, a);
            double ay = y[a];
            double best = -1;
            int pick = start;
            for (int j = start; j < end; j++) {
                double area = Math.abs((ax - avgX) * (y[j] - ay) - (ax - xAt(x, j)) * (avgY - ay));
                if (area > best) {
                    best = area;
                    pick = j;
                }
            }
            out[i + 1] = pick;
            a = pick;
        }
        out[threshold - 1] = n - 1;
        return out;
    }

    /// The values at the given indices.
    ///
    /// #### Parameters
    ///
    /// - `values`: the full array; null gives null, for an index based x
    ///
    /// - `indices`: what `lttb` returned
    ///
    /// #### Returns
    ///
    /// a new array of the picked values
    public static double[] pick(double[] values, int[] indices) {
        if (values == null) {
            return null; //NOPMD ReturnEmptyCollectionRatherThanNull - null x means index based x, as in setData
        }
        double[] out = new double[indices.length];
        for (int i = 0; i < indices.length; i++) {
            out[i] = values[indices[i]];
        }
        return out;
    }

    private static double xAt(double[] x, int i) {
        return x == null ? i : x[i];
    }
}
