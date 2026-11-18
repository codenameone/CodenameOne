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
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.chart.Annotation;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.Sparkline;
import com.codename1.ui.chart.TreeNode;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.axis.SessionTimeAxis;
import com.codename1.ui.chart.indicator.Indicators;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.BoxPlotSeries;
import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.FunnelSeries;
import com.codename1.ui.chart.series.GaugeSeries;
import com.codename1.ui.chart.series.HeatmapSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.SankeySeries;
import com.codename1.ui.chart.series.SunburstSeries;
import com.codename1.ui.chart.series.TreemapSeries;
import com.codename1.ui.chart.series.WaterfallSeries;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// Screenshots of the `com.codename1.ui.chart` package, one per chart family. Data is fixed
/// (no random numbers) and animation is off, so every capture is deterministic.
public abstract class ModernChartScreenshotTest extends BaseTest {
    private final String name;

    protected ModernChartScreenshotTest(String name) {
        this.name = name;
    }

    @Override
    public boolean runTest() throws Exception {
        String shot = "chart2-" + name;
        Form form = createForm(shot, new BorderLayout(), shot);
        form.add(BorderLayout.CENTER, build());
        form.show();
        return true;
    }

    private Component build() {
        if ("sparklines".equals(name)) {
            return sparklines();
        }
        Chart c = new Chart();
        c.setAnimationDuration(0);
        if ("mixed".equals(name)) {
            c.setTitle("Revenue vs target").setXAxis(new CategoryAxis(Axis.BOTTOM, "Q1", "Q2", "Q3", "Q4"));
            c.addSeries(new BarSeries("2024").setData(null, new double[]{120, 150, 140, 190}));
            c.addSeries(new BarSeries("2025").setData(null, new double[]{135, 170, 165, 230}));
            c.addSeries(new LineSeries("Target").setDashed(true).setData(null, new double[]{130, 160, 170, 200}));
            c.addAnnotation(Annotation.valueLine(180, "Stretch"));
        } else if ("dense-labels".equals(name)) {
            String[] cats = new String[24];
            double[] v = new double[24];
            for (int i = 0; i < cats.length; i++) {
                cats[i] = "Department number " + (i + 1);
                v[i] = 1000 + (i * 7919 % 97) * 1234.5;
            }
            c.setTitle("A very long chart title that has to be shortened to fit on a phone screen");
            c.setXAxis(new CategoryAxis(Axis.BOTTOM, cats));
            BarSeries spend = new BarSeries("Spend in a currency with a long name");
            spend.setData(null, v);
            spend.setShowValues(true);
            c.addSeries(spend);
        } else if ("candles".equals(name)) {
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
        } else if ("waterfall".equals(name)) {
            c.setTitle("Profit").setXAxis(new CategoryAxis(Axis.BOTTOM, "Start", "Sales", "Returns", "Costs", "Tax", "Net"));
            c.addSeries(new WaterfallSeries("P&L").setSteps(new double[]{500, 220, -60, -180, -90, 0},
                    new boolean[]{false, false, false, false, false, true}).setShowValues(true));
        } else if ("boxplot".equals(name)) {
            BoxPlotSeries bp = new BoxPlotSeries("Latency");
            for (int d = 0; d < 4; d++) {
                bp.addBox(d, 10 + d, 20 + d * 2, 28 + d * 3, 35 + d * 3, 50 + d * 5);
            }
            c.setTitle("Latency (ms)").setXAxis(new CategoryAxis(Axis.BOTTOM, "Mon", "Tue", "Wed", "Thu"));
            c.addSeries(bp);
        } else if ("heatmap".equals(name)) {
            double[][] m = new double[5][7];
            for (int r = 0; r < 5; r++) {
                for (int k = 0; k < 7; k++) {
                    m[r][k] = (r * 13 + k * 7) % 40 + 5;
                }
            }
            c.setTitle("Visits").setXAxis(new CategoryAxis(Axis.BOTTOM, "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
                    .setYAxis(new CategoryAxis(Axis.LEFT, "Night", "Morning", "Noon", "Afternoon", "Evening"));
            c.addSeries(new HeatmapSeries("Visits").setMatrix(m).setShowValues(true));
        } else if ("pie".equals(name)) {
            c.setTitle("Mobile OS share").setLegendPosition(Chart.LEGEND_NONE);
            c.addSeries(new PieSeries("Share").setData(new String[]{"Android", "iOS", "Samsung", "KaiOS", "Other"},
                    new double[]{70.1, 28.5, 0.6, 0.3, 0.5}));
        } else if ("doughnut".equals(name)) {
            c.setTitle("Budget");
            c.addSeries(new PieSeries("Budget").setInnerRadius(0.6f).setCenterText("3,000")
                    .setLabelMode(PieSeries.LABELS_INSIDE)
                    .setData(new String[]{"Rent", "Food", "Transport", "Fun", "Savings"}, new double[]{1200, 600, 250, 300, 650}));
        } else if ("gauge".equals(name)) {
            c.addSeries(new GaugeSeries("CPU", 0, 100).setValue(72).setUnit("%")
                    .addBand(0, 60, 0x009E73).addBand(60, 85, 0xE69F00).addBand(85, 100, 0xD55E00));
        } else if ("radar".equals(name)) {
            c.setTitle("Players");
            c.addSeries(new RadarSeries("Alice", "Speed", "Power", "Range", "Defense", "Stamina", "Agility").setValues(8, 6, 7, 5, 9, 7));
            c.addSeries(new RadarSeries("Bob").setValues(5, 9, 4, 8, 6, 3));
        } else if ("treemap".equals(name) || "sunburst".equals(name)) {
            TreeNode root = portfolio();
            if ("treemap".equals(name)) {
                c.setTitle("Portfolio").addSeries(new TreemapSeries("Portfolio", root));
            } else {
                c.setTitle("Portfolio").addSeries(new SunburstSeries("Portfolio", root));
            }
        } else if ("sankey".equals(name)) {
            c.setTitle("Energy flow");
            c.addSeries(new SankeySeries("Energy").addLink("Solar", "Grid", 40).addLink("Wind", "Grid", 30)
                    .addLink("Gas", "Grid", 60).addLink("Gas", "Industry", 20).addLink("Grid", "Homes", 55)
                    .addLink("Grid", "Industry", 45).addLink("Grid", "Losses", 30));
        } else {
            c.setTitle("Signup funnel");
            c.addSeries(new FunnelSeries("Funnel").setData(new String[]{"Visited", "Viewed pricing", "Started trial", "Paid"},
                    new double[]{12000, 4200, 900, 210}));
        }
        return c;
    }

    private static TreeNode portfolio() {
        TreeNode root = new TreeNode("Portfolio");
        TreeNode tech = root.add(new TreeNode("Tech"));
        tech.add(new TreeNode("ACME", 120));
        tech.add(new TreeNode("Globex", 80));
        tech.add(new TreeNode("Initech", 35));
        TreeNode energy = root.add(new TreeNode("Energy"));
        energy.add(new TreeNode("Solar", 60));
        energy.add(new TreeNode("Wind", 40));
        TreeNode health = root.add(new TreeNode("Health"));
        health.add(new TreeNode("Pharma", 50));
        health.add(new TreeNode("Devices", 20));
        root.add(new TreeNode("Cash", 30));
        return root;
    }

    private static Container sparklines() {
        Container list = new Container(BoxLayout.y());
        String[] names = {"ACME", "Globex", "Initech", "Hooli"};
        int[] types = {Sparkline.LINE, Sparkline.AREA, Sparkline.BARS, Sparkline.LINE};
        for (int i = 0; i < names.length; i++) {
            double[] v = new double[20];
            for (int k = 0; k < v.length; k++) {
                v[k] = ((k * (i + 3) * 17) % 23) - (i == 2 ? 11 : 0);
            }
            Container row = new Container(new BorderLayout());
            row.add(BorderLayout.CENTER, new Label(names[i]));
            row.add(BorderLayout.EAST, new Sparkline(v).setType(types[i]).setShowExtremes(i == 3));
            list.add(row);
        }
        return list;
    }
    /// The `mixed` case.
    public static final class Chart2MixedScreenshotTest extends ModernChartScreenshotTest {
        public Chart2MixedScreenshotTest() {
            super("mixed");
        }
    }

    /// The `dense-labels` case.
    public static final class Chart2DenseLabelsScreenshotTest extends ModernChartScreenshotTest {
        public Chart2DenseLabelsScreenshotTest() {
            super("dense-labels");
        }
    }

    /// The `candles` case.
    public static final class Chart2CandlesScreenshotTest extends ModernChartScreenshotTest {
        public Chart2CandlesScreenshotTest() {
            super("candles");
        }
    }

    /// The `waterfall` case.
    public static final class Chart2WaterfallScreenshotTest extends ModernChartScreenshotTest {
        public Chart2WaterfallScreenshotTest() {
            super("waterfall");
        }
    }

    /// The `boxplot` case.
    public static final class Chart2BoxplotScreenshotTest extends ModernChartScreenshotTest {
        public Chart2BoxplotScreenshotTest() {
            super("boxplot");
        }
    }

    /// The `heatmap` case.
    public static final class Chart2HeatmapScreenshotTest extends ModernChartScreenshotTest {
        public Chart2HeatmapScreenshotTest() {
            super("heatmap");
        }
    }

    /// The `pie` case.
    public static final class Chart2PieScreenshotTest extends ModernChartScreenshotTest {
        public Chart2PieScreenshotTest() {
            super("pie");
        }
    }

    /// The `doughnut` case.
    public static final class Chart2DoughnutScreenshotTest extends ModernChartScreenshotTest {
        public Chart2DoughnutScreenshotTest() {
            super("doughnut");
        }
    }

    /// The `gauge` case.
    public static final class Chart2GaugeScreenshotTest extends ModernChartScreenshotTest {
        public Chart2GaugeScreenshotTest() {
            super("gauge");
        }
    }

    /// The `radar` case.
    public static final class Chart2RadarScreenshotTest extends ModernChartScreenshotTest {
        public Chart2RadarScreenshotTest() {
            super("radar");
        }
    }

    /// The `treemap` case.
    public static final class Chart2TreemapScreenshotTest extends ModernChartScreenshotTest {
        public Chart2TreemapScreenshotTest() {
            super("treemap");
        }
    }

    /// The `sunburst` case.
    public static final class Chart2SunburstScreenshotTest extends ModernChartScreenshotTest {
        public Chart2SunburstScreenshotTest() {
            super("sunburst");
        }
    }

    /// The `sankey` case.
    public static final class Chart2SankeyScreenshotTest extends ModernChartScreenshotTest {
        public Chart2SankeyScreenshotTest() {
            super("sankey");
        }
    }

    /// The `funnel` case.
    public static final class Chart2FunnelScreenshotTest extends ModernChartScreenshotTest {
        public Chart2FunnelScreenshotTest() {
            super("funnel");
        }
    }

    /// The `sparklines` case.
    public static final class Chart2SparklinesScreenshotTest extends ModernChartScreenshotTest {
        public Chart2SparklinesScreenshotTest() {
            super("sparklines");
        }
    }
}
