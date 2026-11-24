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

import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.TreeNode;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.BoxPlotSeries;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.ErrorBarSeries;
import com.codename1.ui.chart.series.FlameGraphSeries;
import com.codename1.ui.chart.series.FunnelSeries;
import com.codename1.ui.chart.series.GaugeSeries;
import com.codename1.ui.chart.series.HeatmapSeries;
import com.codename1.ui.chart.series.HistogramSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.RangeBarSeries;
import com.codename1.ui.chart.series.SankeySeries;
import com.codename1.ui.chart.series.ScatterSeries;
import com.codename1.ui.chart.series.SunburstSeries;
import com.codename1.ui.chart.series.TreemapSeries;
import com.codename1.ui.chart.series.VolumeSeries;
import com.codename1.ui.chart.series.WaterfallSeries;

/// One small chart of every series type, with fixed data, shared by the still captures that
/// group several types and by the filmstrips that animate every type. Each method fills the
/// chart it is given, so a caller decides whether it animates.
final class Chart2Gallery {
    static final String[] Q = {"Q1", "Q2", "Q3", "Q4"};
    static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri"};

    private Chart2Gallery() {
    }

    static Chart line(Chart c) {
        c.setTitle("Line").setXAxis(new CategoryAxis(Axis.BOTTOM, DAYS));
        c.addSeries(new LineSeries("Visits").setMarkers(true).setData(null, new double[]{12, 19, 15, 24, 21}));
        return c;
    }

    static Chart spline(Chart c) {
        c.setTitle("Spline").setXAxis(new CategoryAxis(Axis.BOTTOM, DAYS));
        c.addSeries(new LineSeries("Temp").setMode(LineSeries.SPLINE).setData(null, new double[]{14, 22, 18, 27, 20}));
        return c;
    }

    static Chart step(Chart c) {
        c.setTitle("Step").setXAxis(new CategoryAxis(Axis.BOTTOM, DAYS));
        c.addSeries(new LineSeries("Price").setMode(LineSeries.STEP).setData(null, new double[]{5, 5, 8, 6, 9}));
        return c;
    }

    static Chart stackedArea(Chart c) {
        c.setTitle("Stacked area").setXAxis(new CategoryAxis(Axis.BOTTOM, DAYS));
        c.addSeries(new LineSeries("Web").setFilled(true).setStackGroup("s").setData(null, new double[]{10, 14, 12, 18, 16}));
        c.addSeries(new LineSeries("App").setFilled(true).setStackGroup("s").setData(null, new double[]{6, 8, 11, 9, 13}));
        return c;
    }

    static Chart groupedBars(Chart c) {
        c.setTitle("Grouped bars").setXAxis(new CategoryAxis(Axis.BOTTOM, Q));
        c.addSeries(new BarSeries("2024").setData(null, new double[]{120, 150, 140, 190}));
        c.addSeries(new BarSeries("2025").setData(null, new double[]{135, 170, 165, 230}));
        return c;
    }

    static Chart stackedBars(Chart c) {
        c.setTitle("Stacked bars").setXAxis(new CategoryAxis(Axis.BOTTOM, Q));
        c.addSeries(new BarSeries("Online").setStackGroup("s").setData(null, new double[]{60, 80, 75, 110}));
        c.addSeries(new BarSeries("Store").setStackGroup("s").setData(null, new double[]{50, 45, 60, 70}));
        return c;
    }

    static Chart horizontalBars(Chart c) {
        c.setTitle("Horizontal bars").setXAxis(new CategoryAxis(Axis.BOTTOM, "Java", "Kotlin", "Swift", "Dart"));
        c.setRotated(true);
        c.addSeries(new BarSeries("Devs").setData(null, new double[]{82, 47, 39, 21}));
        return c;
    }

    static Chart histogram(Chart c) {
        double[] samples = new double[60];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = 40 + ((i * 37) % 23) + ((i * 11) % 17);
        }
        c.setTitle("Histogram");
        c.addSeries(new HistogramSeries("Response ms").setSamples(samples, 8));
        return c;
    }

    static Chart waterfall(Chart c) {
        c.setTitle("Waterfall").setXAxis(new CategoryAxis(Axis.BOTTOM, "Start", "Sales", "Costs", "Net"));
        c.addSeries(new WaterfallSeries("P&L").setSteps(new double[]{500, 220, -180, 0},
                new boolean[]{false, false, false, true}));
        return c;
    }

    static Chart scatter(Chart c) {
        c.setTitle("Scatter");
        c.addSeries(new ScatterSeries("Trees").setData(new double[]{2, 3, 5, 6, 8, 9, 11, 12},
                new double[]{3, 5, 4, 7, 8, 7, 10, 12}, null));
        return c;
    }

    static Chart bubble(Chart c) {
        c.setTitle("Bubble");
        c.addSeries(new ScatterSeries("Cities").setData(new double[]{2, 4, 6, 8, 10},
                new double[]{5, 9, 4, 8, 6}, new double[]{3, 9, 5, 12, 2}));
        return c;
    }

    static Chart rangeBars(Chart c) {
        c.setTitle("Range bars").setXAxis(new CategoryAxis(Axis.BOTTOM, DAYS));
        c.addSeries(new RangeBarSeries("Temp").setRanges(null, new double[]{8, 10, 7, 12, 9},
                new double[]{17, 21, 15, 24, 19}));
        return c;
    }

    static Chart errorBars(Chart c) {
        c.setTitle("Error bars").setXAxis(new CategoryAxis(Axis.BOTTOM, "A", "B", "C", "D"));
        c.addSeries(new ErrorBarSeries("Mean").setIntervals(null, new double[]{12, 18, 9, 15},
                new double[]{10, 15, 7, 11}, new double[]{14, 22, 12, 18}));
        return c;
    }

    static Chart boxPlot(Chart c) {
        BoxPlotSeries bp = new BoxPlotSeries("Latency");
        for (int d = 0; d < 4; d++) {
            bp.addBox(d, 10 + d, 20 + d * 2, 28 + d * 3, 35 + d * 3, 50 + d * 5);
        }
        c.setTitle("Box plot").setXAxis(new CategoryAxis(Axis.BOTTOM, "Mon", "Tue", "Wed", "Thu"));
        c.addSeries(bp);
        return c;
    }

    static Chart heatmap(Chart c) {
        double[][] m = new double[4][5];
        for (int r = 0; r < 4; r++) {
            for (int k = 0; k < 5; k++) {
                m[r][k] = (r * 13 + k * 7) % 40 + 5;
            }
        }
        c.setTitle("Heatmap").setXAxis(new CategoryAxis(Axis.BOTTOM, DAYS))
                .setYAxis(new CategoryAxis(Axis.LEFT, "Night", "Morning", "Noon", "Evening"));
        c.addSeries(new HeatmapSeries("Visits").setMatrix(m));
        return c;
    }

    static CandlestickSeries prices(String name) {
        int n = 16;
        double[] o = new double[n];
        double[] h = new double[n];
        double[] l = new double[n];
        double[] cl = new double[n];
        double p = 100;
        for (int i = 0; i < n; i++) {
            double d = ((i * 37) % 11 - 5) * 0.6;
            o[i] = p;
            cl[i] = p + d;
            h[i] = Math.max(o[i], cl[i]) + 0.8 + (i % 3) * 0.3;
            l[i] = Math.min(o[i], cl[i]) - 0.7 - (i % 4) * 0.2;
            p = cl[i];
        }
        return new CandlestickSeries(name).setOhlc(null, o, h, l, cl);
    }

    static Chart candles(Chart c) {
        c.setTitle("Candlestick");
        c.addSeries(prices("ACME"));
        return c;
    }

    static Chart ohlc(Chart c) {
        c.setTitle("OHLC bars");
        c.addSeries(prices("ACME").setStyle(CandlestickSeries.OHLC_BARS));
        return c;
    }

    static Chart heikinAshi(Chart c) {
        c.setTitle("Heikin-Ashi");
        c.addSeries(prices("ACME").setStyle(CandlestickSeries.HEIKIN_ASHI));
        return c;
    }

    static Chart volume(Chart c) {
        double[] v = new double[16];
        for (int i = 0; i < v.length; i++) {
            v[i] = 200 + (i * 53) % 170;
        }
        c.setTitle("Volume");
        c.addSeries(new VolumeSeries("Volume", prices("ACME"), v));
        return c;
    }

    static Chart pie(Chart c) {
        c.setTitle("Pie");
        c.addSeries(new PieSeries("Share").setData(new String[]{"Android", "iOS", "Other"}, new double[]{70, 28, 2}));
        return c;
    }

    static Chart doughnut(Chart c) {
        c.setTitle("Doughnut");
        c.addSeries(new PieSeries("Budget").setInnerRadius(0.6f).setCenterText("3,000")
                .setData(new String[]{"Rent", "Food", "Fun", "Savings"}, new double[]{1200, 600, 300, 900}));
        return c;
    }

    static Chart funnel(Chart c) {
        c.setTitle("Funnel");
        c.addSeries(new FunnelSeries("Funnel").setData(new String[]{"Visited", "Trial", "Paid"},
                new double[]{12000, 2900, 610}));
        return c;
    }

    static Chart gauge(Chart c) {
        c.setTitle("Gauge");
        c.addSeries(new GaugeSeries("CPU", 0, 100).setValue(72).setUnit("%")
                .addBand(0, 60, 0x009E73).addBand(60, 85, 0xE69F00).addBand(85, 100, 0xD55E00));
        return c;
    }

    static Chart radar(Chart c) {
        c.setTitle("Radar");
        c.addSeries(new RadarSeries("Alice", "Speed", "Power", "Range", "Defense", "Stamina").setValues(8, 6, 7, 5, 9));
        c.addSeries(new RadarSeries("Bob").setValues(5, 9, 4, 8, 6));
        return c;
    }

    static Chart treemap(Chart c) {
        c.setTitle("Treemap");
        c.addSeries(new TreemapSeries("Portfolio", AbstractChart2ScreenshotTest.portfolio()));
        return c;
    }

    static Chart sunburst(Chart c) {
        c.setTitle("Sunburst");
        c.addSeries(new SunburstSeries("Portfolio", AbstractChart2ScreenshotTest.portfolio()));
        return c;
    }

    static Chart sankey(Chart c) {
        c.setTitle("Sankey");
        c.addSeries(new SankeySeries("Energy").addLink("Solar", "Grid", 40).addLink("Gas", "Grid", 60)
                .addLink("Grid", "Homes", 55).addLink("Grid", "Industry", 45));
        return c;
    }

    /// A small CPU profile: a request handler that spends most of its time rendering.
    static TreeNode profile() {
        TreeNode main = new TreeNode("main");
        TreeNode handle = main.add(new TreeNode("handleRequest"));
        TreeNode parse = handle.add(new TreeNode("parseJson"));
        parse.add(new TreeNode("readToken", 14));
        parse.add(new TreeNode("self", 6));
        TreeNode render = handle.add(new TreeNode("render"));
        TreeNode layout = render.add(new TreeNode("layout"));
        layout.add(new TreeNode("measureText", 18));
        layout.add(new TreeNode("self", 7));
        TreeNode paint = render.add(new TreeNode("paint"));
        paint.add(new TreeNode("fillRect", 12));
        paint.add(new TreeNode("drawString", 16));
        handle.add(new TreeNode("log", 5));
        TreeNode gc = main.add(new TreeNode("gc"));
        gc.add(new TreeNode("mark", 9));
        gc.add(new TreeNode("sweep", 6));
        main.add(new TreeNode("idle", 7));
        return main;
    }

    static Chart flameGraph(Chart c) {
        c.setTitle("Flame graph").setLegendPosition(Chart.LEGEND_NONE);
        c.addSeries(new FlameGraphSeries("CPU", profile()));
        return c;
    }

    static Chart icicle(Chart c) {
        c.setTitle("Icicle").setLegendPosition(Chart.LEGEND_NONE);
        c.addSeries(new FlameGraphSeries("CPU", profile()).setInverted(true));
        return c;
    }
}
