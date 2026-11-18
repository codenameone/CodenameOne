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

import com.codename1.util.MathUtil;

/// A histogram: samples counted into equal width bins and drawn as touching bars.
///
/// ```java
/// new HistogramSeries("Response time").setSamples(latencies, 0);
/// ```
public class HistogramSeries extends BarSeries {
    private double binWidth;

    /// Creates an empty histogram.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    public HistogramSeries(String name) {
        super(name);
        setBarFraction(1);
        setCornerRadius(0);
    }

    /// Bins the samples.
    ///
    /// #### Parameters
    ///
    /// - `samples`: the raw values; `NaN` values are ignored
    ///
    /// - `bins`: the number of bins, or 0 or less to choose one with Sturges' rule
    ///
    /// #### Returns
    ///
    /// this series
    public HistogramSeries setSamples(double[] samples, int bins) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int n = 0;
        for (double v : samples) {
            if (!isGap(v)) {
                min = Math.min(min, v);
                max = Math.max(max, v);
                n++;
            }
        }
        if (n == 0) {
            clear();
            return this;
        }
        if (bins <= 0) {
            bins = (int) Math.ceil(MathUtil.log(n) / MathUtil.log(2) + 1);
        }
        // identical samples make one bar, not a run of empty ones
        bins = max > min ? Math.max(1, bins) : 1;
        double width = max > min ? (max - min) / bins : 1;
        double[] x = new double[bins];
        double[] y = new double[bins];
        for (int b = 0; b < bins; b++) {
            x[b] = min + width * (b + 0.5);
        }
        for (double v : samples) {
            if (!isGap(v)) {
                int b = (int) ((v - min) / width);
                y[Math.min(bins - 1, Math.max(0, b))]++;
            }
        }
        binWidth = width;
        setData(x, y);
        return this;
    }

    /// The width of one bin in data units.
    public double getBinWidth() {
        return binWidth;
    }
}
