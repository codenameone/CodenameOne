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

import com.codename1.ui.chart.indicator.Indicators;
import com.codename1.ui.chart.series.BoxPlotSeries;
import com.codename1.ui.chart.series.CandlestickSeries;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Values computed by the financial indicators and statistical series, checked against
/// hand-computed results.
public class ChartMathTest {
    private static final double[] V = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    @Test
    public void simpleMovingAverage() {
        double[] s = Indicators.sma(V, 3);
        assertTrue(Double.isNaN(s[0]) && Double.isNaN(s[1]));
        assertEquals(2, s[2], 1e-9);
        assertEquals(9, s[9], 1e-9);
    }

    @Test
    public void exponentialMovingAverageTracksALinearSeries() {
        double[] e = Indicators.ema(V, 3);
        // seeded with the SMA, then lags a straight line by a constant
        assertEquals(2, e[2], 1e-9);
        for (int i = 4; i < V.length; i++) {
            assertEquals(1, V[i] - e[i], 1e-6);
        }
    }

    @Test
    public void bollingerBandsAreSymmetric() {
        double[][] b = Indicators.bollinger(V, 4, 2);
        for (int i = 3; i < V.length; i++) {
            assertEquals(b[1][i] - b[0][i], b[0][i] - b[2][i], 1e-9);
            assertTrue(b[1][i] > b[0][i]);
        }
    }

    @Test
    public void rsiOfAnAlwaysRisingSeriesIs100() {
        double[] r = Indicators.rsi(V, 5);
        assertEquals(100, r[9], 1e-9);
        double[] down = new double[V.length];
        for (int i = 0; i < V.length; i++) {
            down[i] = -V[i];
        }
        assertEquals(0, Indicators.rsi(down, 5)[9], 1e-9);
    }

    @Test
    public void macdHistogramIsMacdMinusSignal() {
        double[] v = new double[60];
        for (int i = 0; i < v.length; i++) {
            v[i] = 50 + 10 * Math.sin(i / 5.0);
        }
        double[][] m = Indicators.macd(v, 12, 26, 9);
        for (int i = 40; i < v.length; i++) {
            assertEquals(m[0][i] - m[1][i], m[2][i], 1e-9);
        }
    }

    @Test
    public void quantilesInterpolate() {
        double[] s = {1, 2, 3, 4};
        assertEquals(1, BoxPlotSeries.quantile(s, 0), 1e-9);
        assertEquals(2.5, BoxPlotSeries.quantile(s, 0.5), 1e-9);
        assertEquals(4, BoxPlotSeries.quantile(s, 1), 1e-9);
    }

    @Test
    public void heikinAshiFollowsItsDefinition() {
        CandlestickSeries c = new CandlestickSeries("t");
        c.setOhlc(null, new double[]{10, 11, 12}, new double[]{12, 13, 14}, new double[]{9, 10, 11}, new double[]{11, 12, 13});
        double[][] ha = c.heikinAshi();
        // close = (o + h + l + c) / 4; open = average of the previous HA open and close
        assertEquals((10 + 12 + 9 + 11) / 4.0, ha[3][0], 1e-9);
        assertEquals((ha[0][0] + ha[3][0]) / 2, ha[0][1], 1e-9);
        assertTrue(ha[1][1] >= Math.max(ha[0][1], ha[3][1]));
        assertTrue(ha[2][1] <= Math.min(ha[0][1], ha[3][1]));
    }
}
