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
    /// A NaN value is a gap. Gaps never take part in the triangle areas or bucket averages. A
    /// bucket that contains one keeps its first NaN index as a gap marker, so the gap survives
    /// downsampling rather than being bridged, and also keeps its best real point, so a sparse
    /// NaN does not cost the bucket its peak. The result therefore has `threshold` indices when
    /// the data has no gaps, and never more than `2 * threshold - 2`.
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
        // one slot per bucket, plus a gap marker for each bucket that holds a NaN
        int[] out = new int[2 * threshold - 2];
        int outLen = 1;
        double every = (n - 2) / (double) (threshold - 2);
        out[0] = 0;
        // the anchor is the last kept point that has a value; a NaN never anchors a triangle
        double ax = 0;
        double ay = 0;
        for (int j = 0; j < n; j++) {
            if (!isGap(x, y, j)) {
                ax = xAt(x, j);
                ay = y[j];
                break;
            }
        }
        for (int i = 0; i < threshold - 2; i++) {
            // the average of the next bucket is the third corner of the triangle; NaN gaps are
            // left out of it
            int avgStart = (int) Math.floor((i + 1) * every) + 1;
            int avgEnd = Math.min(n, (int) Math.floor((i + 2) * every) + 1);
            double avgX = 0;
            double avgY = 0;
            int len = 0;
            for (int j = avgStart; j < avgEnd; j++) {
                if (!isGap(x, y, j)) {
                    avgX += xAt(x, j);
                    avgY += y[j];
                    len++;
                }
            }
            if (len > 0) {
                avgX /= len;
                avgY /= len;
            } else {
                avgX = ax;
                avgY = ay;
            }
            int start = (int) Math.floor(i * every) + 1;
            int end = (int) Math.floor((i + 1) * every) + 1;
            double best = -1;
            int pick = -1;
            int gap = -1;
            for (int j = start; j < end; j++) {
                if (isGap(x, y, j)) {
                    if (gap < 0) {
                        gap = j;
                    }
                    continue;
                }
                double area = Math.abs((ax - avgX) * (y[j] - ay) - (ax - xAt(x, j)) * (avgY - ay));
                if (area != area) {
                    // an overflow (infinity minus infinity); counted as the smallest area, so it
                    // still picks a point rather than none but never wins over a real extreme -
                    // as `best` a NaN would fail every later comparison and keep this point
                    area = -1;
                }
                if (area > best || pick < 0) {
                    best = area;
                    pick = j;
                }
            }
            if (pick < 0) {
                // nothing but gap in this bucket; an empty bucket adds nothing
                if (gap >= 0) {
                    out[outLen++] = gap;
                }
                continue;
            }
            ax = xAt(x, pick);
            ay = y[pick];
            if (gap >= 0 && gap < pick) {
                // a bucket holding a gap keeps it, so a line drawn from the result still breaks there
                out[outLen++] = gap;
                gap = -1;
            }
            out[outLen++] = pick;
            if (gap >= 0) {
                out[outLen++] = gap;
            }
        }
        out[outLen++] = n - 1;
        if (outLen == out.length) {
            return out;
        }
        int[] trimmed = new int[outLen];
        System.arraycopy(out, 0, trimmed, 0, outLen);
        return trimmed;
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

    /// A NaN or infinite y, or x, is a gap.
    private static boolean isGap(double[] x, double[] y, int j) {
        double v = y[j];
        if (v != v || Double.isInfinite(v)) {
            return true;
        }
        if (x == null) {
            return false;
        }
        double xv = x[j];
        return xv != xv || Double.isInfinite(xv);
    }

    private static double xAt(double[] x, int i) {
        return x == null ? i : x[i];
    }
}
