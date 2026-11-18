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
package com.codenameone.examples.hellocodenameone.tests.charts;

import com.codename1.ui.Component;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.SessionTimeAxis;
import com.codename1.ui.chart.indicator.Indicators;
import com.codename1.ui.chart.series.CandlestickSeries;

/// Forty candlesticks on a session time axis that skips weekends, with a 10-period SMA.
public class Chart2CandlesScreenshotTest extends AbstractChart2ScreenshotTest {

    @Override
    protected Component buildChart() {
        Chart c = staticChart();
        int n = 40;
        long[] t = new long[n];
        double[] o = new double[n];
        double[] h = new double[n];
        double[] l = new double[n];
        double[] cl = new double[n];
        double p = 100;
        for (int i = 0; i < n; i++) {
            // two trading weeks per row of ten, weekends skipped by the axis
            t[i] = 1735776000000L + (i / 5 * 7 + i % 5) * 86400000L;
            double d = ((i * 37) % 11 - 5) * 0.6;
            o[i] = p;
            cl[i] = p + d;
            h[i] = Math.max(o[i], cl[i]) + 0.8 + (i % 3) * 0.3;
            l[i] = Math.min(o[i], cl[i]) - 0.7 - (i % 4) * 0.2;
            p = cl[i];
        }
        CandlestickSeries cs = new CandlestickSeries("ACME");
        cs.setOhlc(null, o, h, l, cl);
        c.setTitle("ACME").setXAxis(new SessionTimeAxis(Axis.BOTTOM, t));
        c.addSeries(cs);
        c.addSeries(Indicators.line("SMA 10", cs, Indicators.sma(cl, 10)));
        return c;
    }

    @Override
    protected String screenshotName() {
        return "chart2-candles";
    }
}
