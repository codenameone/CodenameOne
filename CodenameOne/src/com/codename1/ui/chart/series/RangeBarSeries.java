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

import com.codename1.ui.chart.ChartContext;
import com.codename1.ui.chart.ChartPoint;

/// Floating bars that span from a start value to an end value: schedules, temperature ranges,
/// price ranges. Combine with `Chart.setRotated(true)` for a timeline.
///
/// ```java
/// new RangeBarSeries("Temperature").setRanges(null, lows, highs);
/// ```
public class RangeBarSeries extends BarSeries {
    /// Creates an empty series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    public RangeBarSeries(String name) {
        // channel 0 is the start, channel 1 the end exactly as given; y keeps the length the
        // bar painting and stacking read, and the end is kept beside it because start + length
        // does not round-trip (0.1 + (0.4 - 0.1) is 0.30000000000000004 for an end of 0.4)
        super(name, 2);
    }

    /// A range added by `add(x, y)` or `setData` starts at zero, so its bar spans `0..y`.
    @Override
    protected double auxDefault(int channel, double y) {
        return channel == 0 ? 0 : y;
    }

    /// Replaces all ranges.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x value of each bar, or null for 0, 1, 2...
    ///
    /// - `start`: where each bar starts
    ///
    /// - `end`: where each bar ends
    ///
    /// #### Returns
    ///
    /// this series
    public RangeBarSeries setRanges(double[] x, double[] start, double[] end) {
        if (start == null || end == null || start.length != end.length) {
            throw new IllegalArgumentException("start and end must have the same length");
        }
        if (x != null && x.length != end.length) {
            throw new IllegalArgumentException("x has " + x.length + " values for " + end.length + " ranges");
        }
        double[] len = new double[end.length];
        for (int i = 0; i < len.length; i++) {
            len[i] = end[i] - start[i];
        }
        setRows(x, len, new double[][]{start, end});
        return this;
    }

    /// Appends one range.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x value
    ///
    /// - `start`: the start value
    ///
    /// - `end`: the end value
    ///
    /// #### Returns
    ///
    /// this series
    public RangeBarSeries addRange(double x, double start, double end) {
        addRow(x, end - start, new double[]{start, end});
        return this;
    }

    /// The start of range `i`.
    public double getStart(int i) {
        return getAux(0, i);
    }

    /// The end of range `i`.
    public double getEnd(int i) {
        double start = getAux(0, i);
        double end = getAux(1, i);
        double len = getY(i);
        // set(i, y) and setData change only the length; the stored end is still this range's
        // end only while the length is the one it was computed from
        if (Double.compare(len, end - start) == 0) {
            return end;
        }
        return start + len;
    }

    @Override
    public double getBase(int i) {
        return getAux(0, i);
    }

    @Override
    public double[] getYBounds() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < size(); i++) {
            double a = getStart(i);
            double b = getEnd(i);
            if (!isGap(a) && !isGap(b)) {
                min = Math.min(min, Math.min(a, b));
                max = Math.max(max, Math.max(a, b));
            }
        }
        return min > max ? null : new double[]{min, max};
    }

    @Override
    public String formatTooltip(ChartContext ctx, ChartPoint p) {
        int i = p.getIndex();
        String n = getName() == null ? "" : getName() + ": ";
        return n + formatValue(ctx, getStart(i)) + " - " + formatValue(ctx, getEnd(i));
    }

    @Override
    protected String tableValue(int i) {
        return formatValue(null, getStart(i)) + " - " + formatValue(null, getEnd(i));
    }
}
