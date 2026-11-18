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

/// Traded volume per period, each bar coloured by whether its period closed up or down.
///
/// Usually shown in its own chart under the prices, linked with `Chart.syncWith` so both pan
/// and zoom together.
///
/// ```java
/// VolumeSeries volume = new VolumeSeries("Volume", prices, volumes);
/// ```
public class VolumeSeries extends BarSeries {
    private final CandlestickSeries prices;

    /// Creates the series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    ///
    /// - `prices`: the candles that decide each bar's colour, or null for the series colour
    ///
    /// - `volume`: the volume of each period, parallel to the prices
    public VolumeSeries(String name, CandlestickSeries prices, double[] volume) {
        super(name);
        this.prices = prices;
        double[] x = null;
        if (prices != null && prices.size() == volume.length) {
            x = new double[volume.length];
            for (int i = 0; i < x.length; i++) {
                x[i] = prices.getX(i);
            }
        }
        setData(x, volume);
        setCornerRadius(0);
        setBarFraction(0.7f);
    }

    @Override
    protected int colorAt(ChartContext ctx, int i) {
        if (prices == null || i >= prices.size()) {
            return getColor();
        }
        int c = prices.getClose(i) >= prices.getOpen(i) ? ctx.getTheme().getPositiveColor() : ctx.getTheme().getNegativeColor();
        // volume reads as context, not as the subject: tone it down towards the background
        return blend(c, ctx.getTheme().getGridColor(), 0.35f);
    }

    static int blend(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xff) * (1 - t) + ((b >> 16) & 0xff) * t);
        int g = (int) (((a >> 8) & 0xff) * (1 - t) + ((b >> 8) & 0xff) * t);
        int bl = (int) ((a & 0xff) * (1 - t) + (b & 0xff) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
