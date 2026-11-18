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
package com.codenameone.developerguide.charts;

import com.codename1.ui.Form;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.axis.SessionTimeAxis;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.indicator.Indicators;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.SankeySeries;
import com.codename1.ui.chart.series.VolumeSeries;
import com.codename1.ui.layouts.BorderLayout;

/// The chart snippets the developer guide's Charts section includes.
public class ModernChartSnippets {

    void firstChart(Form form) {
        // tag::modern-chart-first[]
        Chart chart = new Chart()
                .setTitle("Revenue")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Q1", "Q2", "Q3", "Q4"));
        chart.addSeries(new BarSeries("2025").setData(null, new double[]{120, 180, 150, 210}));
        chart.addSeries(new LineSeries("Target").setDashed(true).setData(null, new double[]{140, 160, 170, 190}));
        form.add(BorderLayout.CENTER, chart);
        // end::modern-chart-first[]
    }

    void financial(long[] times, double[] open, double[] high, double[] low, double[] close, double[] vol) {
        // tag::modern-chart-financial[]
        CandlestickSeries prices = new CandlestickSeries("ACME");
        prices.setOhlc(null, open, high, low, close);
        Chart main = new Chart().setXAxis(new SessionTimeAxis(Axis.BOTTOM, times));
        main.addSeries(prices);
        main.addSeries(Indicators.line("SMA 20", prices, Indicators.sma(close, 20)));

        Chart volume = new Chart().setXAxis(new SessionTimeAxis(Axis.BOTTOM, times));
        volume.addSeries(new VolumeSeries("Volume", prices, vol));
        main.setTooltipMode(Chart.TOOLTIP_SHARED);
        volume.setTooltipMode(Chart.TOOLTIP_SHARED);
        main.syncWith(volume);
        // end::modern-chart-financial[]
    }

    void sankey(Chart chart) {
        // tag::modern-chart-sankey[]
        SankeySeries flow = new SankeySeries("Energy");
        flow.addLink("Solar", "Grid", 40).addLink("Gas", "Grid", 60).addLink("Grid", "Homes", 55);
        chart.addSeries(flow);
        // end::modern-chart-sankey[]
    }
}
