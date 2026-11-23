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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.chart.Annotation;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.Sparkline;
import com.codename1.ui.chart.TreeNode;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.axis.LogAxis;
import com.codename1.ui.chart.axis.NumberAxis;
import com.codename1.ui.chart.axis.SessionTimeAxis;
import com.codename1.ui.chart.indicator.Indicators;
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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// The chart snippets the developer guide's Charts section includes. Each method returns
/// what its snippet builds, and `ChartFigures` photographs that return value, so the picture
/// next to every listing in the guide is drawn by the listing itself.
public class ModernChartSnippets {

    public Form firstChart() {
        // tag::modern-chart-first[]
        Form form = new Form("Revenue", new BorderLayout());
        Chart chart = new Chart()
                .setTitle("Revenue")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Q1", "Q2", "Q3", "Q4"));
        chart.addSeries(new BarSeries("2025").setData(null, new double[]{120, 180, 150, 210}));
        chart.addSeries(new LineSeries("Target").setDashed(true).setData(null, new double[]{140, 160, 170, 190}));
        form.add(BorderLayout.CENTER, chart);
        // end::modern-chart-first[]
        return form;
    }

    /// The chart the anatomy figure labels: a title, a legend, both axes with titles, grid
    /// lines, a target annotation and a selected point showing its tooltip.
    public Chart anatomy() {
        // tag::chart-anatomy[]
        Chart chart = new Chart().setTitle("Monthly sales")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Jan", "Feb", "Mar", "Apr", "May", "Jun"));
        chart.getXAxis().setTitle("Month");
        chart.getYAxis().setTitle("Units");
        BarSeries sales = new BarSeries("Sales");
        sales.setData(null, new double[]{42, 55, 48, 63, 71, 66});
        chart.addSeries(sales);
        chart.addSeries(new LineSeries("Forecast").setMarkers(true).setData(null, new double[]{45, 50, 54, 58, 62, 67}));
        chart.addAnnotation(Annotation.valueLine(60, "Target"));
        // end::chart-anatomy[]
        return chart;
    }

    public Chart line() {
        // tag::chart-line[]
        Chart chart = new Chart().setTitle("Daily visits")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
        chart.addSeries(new LineSeries("Web").setMarkers(true).setData(null, new double[]{120, 190, 150, 240, 210, 90, 70}));
        chart.addSeries(new LineSeries("App").setMarkers(true).setData(null, new double[]{80, 110, 140, 130, 170, 160, 150}));
        // end::chart-line[]
        return chart;
    }

    public Chart spline() {
        // tag::chart-spline[]
        Chart chart = new Chart().setTitle("Temperature")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "6:00", "9:00", "12:00", "15:00", "18:00", "21:00"));
        chart.addSeries(new LineSeries("Today").setMode(LineSeries.SPLINE).setData(null, new double[]{11, 15, 22, 24, 19, 14}));
        // end::chart-spline[]
        return chart;
    }

    public Chart step() {
        // tag::chart-step[]
        Chart chart = new Chart().setTitle("Plan price")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "2021", "2022", "2023", "2024", "2025"));
        chart.addSeries(new LineSeries("Price").setMode(LineSeries.STEP).setData(null, new double[]{9, 9, 12, 12, 15}));
        // end::chart-step[]
        return chart;
    }

    public Chart stackedArea() {
        // tag::chart-stacked-area[]
        Chart chart = new Chart().setTitle("Traffic by source")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Mon", "Tue", "Wed", "Thu", "Fri"));
        chart.addSeries(new LineSeries("Search").setFilled(true).setStackGroup("traffic")
                .setData(null, new double[]{40, 52, 48, 60, 58}));
        chart.addSeries(new LineSeries("Social").setFilled(true).setStackGroup("traffic")
                .setData(null, new double[]{20, 25, 32, 28, 36}));
        chart.addSeries(new LineSeries("Direct").setFilled(true).setStackGroup("traffic")
                .setData(null, new double[]{15, 14, 18, 20, 22}));
        // end::chart-stacked-area[]
        return chart;
    }

    public Chart groupedBars() {
        // tag::chart-grouped-bars[]
        Chart chart = new Chart().setTitle("Revenue")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Q1", "Q2", "Q3", "Q4"));
        chart.addSeries(new BarSeries("2024").setData(null, new double[]{120, 150, 140, 190}));
        chart.addSeries(new BarSeries("2025").setData(null, new double[]{135, 170, 165, 230}));
        // end::chart-grouped-bars[]
        return chart;
    }

    public Chart stackedBars() {
        // tag::chart-stacked-bars[]
        Chart chart = new Chart().setTitle("Sales by channel")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Q1", "Q2", "Q3", "Q4"));
        chart.addSeries(new BarSeries("Online").setStackGroup("sales").setData(null, new double[]{60, 80, 75, 110}));
        chart.addSeries(new BarSeries("Stores").setStackGroup("sales").setData(null, new double[]{50, 45, 60, 70}));
        // end::chart-stacked-bars[]
        return chart;
    }

    public Chart horizontalBars() {
        // tag::chart-horizontal-bars[]
        Chart chart = new Chart().setTitle("Developers")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Java", "Kotlin", "Swift", "Dart", "C#"));
        chart.setRotated(true);
        chart.addSeries(new BarSeries("Thousands").setData(null, new double[]{82, 47, 39, 21, 33}));
        // end::chart-horizontal-bars[]
        return chart;
    }

    public Chart scatter() {
        // tag::chart-scatter[]
        Chart chart = new Chart().setTitle("Height and weight");
        chart.getXAxis().setTitle("cm");
        chart.getYAxis().setTitle("kg");
        chart.addSeries(new ScatterSeries("People").setData(
                new double[]{158, 162, 165, 170, 172, 175, 178, 181, 185, 190},
                new double[]{52, 58, 61, 66, 70, 69, 77, 80, 84, 90}, null));
        // end::chart-scatter[]
        return chart;
    }

    public Chart bubble() {
        // tag::chart-bubble[]
        Chart chart = new Chart().setTitle("Markets");
        chart.getXAxis().setTitle("Growth %");
        chart.getYAxis().setTitle("Margin %");
        // the third array is each bubble's size
        chart.addSeries(new ScatterSeries("Regions").setData(
                new double[]{2, 4, 6, 8, 10},
                new double[]{12, 18, 9, 15, 22},
                new double[]{30, 90, 45, 120, 20}));
        // end::chart-bubble[]
        return chart;
    }

    public Chart rangeBars() {
        // tag::chart-range-bars[]
        Chart chart = new Chart().setTitle("Temperature range")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Mon", "Tue", "Wed", "Thu", "Fri"));
        chart.addSeries(new RangeBarSeries("Low to high").setRanges(null,
                new double[]{8, 10, 7, 12, 9}, new double[]{17, 21, 15, 24, 19}));
        // end::chart-range-bars[]
        return chart;
    }

    public Chart errorBars() {
        // tag::chart-error-bars[]
        Chart chart = new Chart().setTitle("Test scores")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Group A", "Group B", "Group C", "Group D"));
        // the value, then the low and high end of each interval
        chart.addSeries(new ErrorBarSeries("Mean").setIntervals(null,
                new double[]{72, 81, 65, 77}, new double[]{66, 75, 58, 70}, new double[]{78, 88, 71, 83}));
        // end::chart-error-bars[]
        return chart;
    }

    public Chart boxPlot() {
        // tag::chart-box-plot[]
        Chart chart = new Chart().setTitle("Response time (ms)")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Mon", "Tue", "Wed", "Thu"));
        BoxPlotSeries latency = new BoxPlotSeries("Latency");
        // x, minimum, lower quartile, median, upper quartile, maximum
        latency.addBox(0, 10, 20, 28, 35, 50);
        latency.addBox(1, 12, 24, 31, 40, 62);
        latency.addBox(2, 9, 18, 25, 31, 44);
        latency.addBox(3, 14, 27, 36, 45, 70);
        chart.addSeries(latency);
        // end::chart-box-plot[]
        return chart;
    }

    public Chart histogram() {
        // tag::chart-histogram[]
        double[] samples = new double[200];
        for (int i = 0; i < samples.length; i++) {
            // a made up, roughly bell shaped spread of response times
            samples[i] = 40 + (i * 37) % 23 + (i * 11) % 17 + (i * 7) % 13;
        }
        Chart chart = new Chart().setTitle("Response times");
        chart.addSeries(new HistogramSeries("Requests").setSamples(samples, 12));
        // end::chart-histogram[]
        return chart;
    }

    public Chart waterfall() {
        // tag::chart-waterfall[]
        Chart chart = new Chart().setTitle("Profit")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Start", "Sales", "Returns", "Costs", "Tax", "Net"));
        // each step adds to the running total; the last one is a total bar
        chart.addSeries(new WaterfallSeries("P&L").setSteps(new double[]{500, 220, -60, -180, -90, 0},
                new boolean[]{false, false, false, false, false, true}).setShowValues(true));
        // end::chart-waterfall[]
        return chart;
    }

    public Chart heatmap() {
        // tag::chart-heatmap[]
        double[][] visits = {
            {5, 9, 12, 8, 6},
            {22, 30, 28, 35, 25},
            {40, 38, 45, 42, 30},
            {18, 25, 20, 28, 33}
        };
        Chart chart = new Chart().setTitle("Visits")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Mon", "Tue", "Wed", "Thu", "Fri"))
                .setYAxis(new CategoryAxis(Axis.LEFT, "Night", "Morning", "Noon", "Evening"));
        chart.addSeries(new HeatmapSeries("Visits").setMatrix(visits).setShowValues(true));
        // end::chart-heatmap[]
        return chart;
    }

    public Chart candlestick() {
        // tag::chart-candlestick[]
        double[] open = {100, 102, 101, 104, 103, 106, 108, 107, 105, 109, 111, 110};
        double[] close = {102, 101, 104, 103, 106, 108, 107, 105, 109, 111, 110, 113};
        double[] high = new double[open.length];
        double[] low = new double[open.length];
        for (int i = 0; i < open.length; i++) {
            high[i] = Math.max(open[i], close[i]) + 1.5;
            low[i] = Math.min(open[i], close[i]) - 1.2;
        }
        CandlestickSeries prices = new CandlestickSeries("ACME");
        prices.setOhlc(null, open, high, low, close);
        Chart chart = new Chart().setTitle("ACME");
        chart.addSeries(prices);
        chart.addSeries(Indicators.line("SMA 4", prices, Indicators.sma(close, 4)));
        // end::chart-candlestick[]
        return chart;
    }

    public Chart ohlc() {
        Chart chart = candlestick();
        ((CandlestickSeries) chart.getSeries().get(0)).setStyle(CandlestickSeries.OHLC_BARS);
        chart.setTitle("OHLC bars");
        return chart;
    }

    public Chart heikinAshi() {
        Chart chart = candlestick();
        CandlestickSeries prices = (CandlestickSeries) chart.getSeries().get(0);
        // tag::chart-heikin-ashi[]
        prices.setStyle(CandlestickSeries.HEIKIN_ASHI);
        // end::chart-heikin-ashi[]
        chart.setTitle("Heikin-Ashi");
        return chart;
    }

    public Container financial() {
        // tag::modern-chart-financial[]
        long day = 24L * 60 * 60 * 1000;
        long[] times = {0, day, 2 * day, 3 * day, 6 * day};
        double[] open = {10, 11, 12, 11, 13};
        double[] high = {12, 13, 14, 13, 15};
        double[] low = {9, 10, 11, 10, 12};
        double[] close = {11, 12, 11, 13, 14};
        double[] vol = {1200, 1800, 900, 1500, 2100};
        CandlestickSeries prices = new CandlestickSeries("ACME");
        prices.setOhlc(null, open, high, low, close);
        Chart main = new Chart().setXAxis(new SessionTimeAxis(Axis.BOTTOM, times));
        main.addSeries(prices);
        main.addSeries(Indicators.line("SMA 3", prices, Indicators.sma(close, 3)));

        Chart volume = new Chart().setXAxis(new SessionTimeAxis(Axis.BOTTOM, times));
        volume.addSeries(new VolumeSeries("Volume", prices, vol));
        main.setTooltipMode(Chart.TOOLTIP_SHARED);
        volume.setTooltipMode(Chart.TOOLTIP_SHARED);
        main.syncWith(volume);
        // end::modern-chart-financial[]
        return BoxLayout.encloseY(main, volume);
    }

    public Chart pie() {
        // tag::chart-pie[]
        Chart chart = new Chart().setTitle("Mobile OS share");
        chart.addSeries(new PieSeries("Share").setData(
                new String[]{"Android", "iOS", "Samsung", "KaiOS", "Other"},
                new double[]{70.1, 28.5, 0.6, 0.3, 0.5}));
        // end::chart-pie[]
        return chart;
    }

    public Chart doughnut() {
        // tag::chart-doughnut[]
        Chart chart = new Chart().setTitle("Monthly budget");
        chart.addSeries(new PieSeries("Budget").setInnerRadius(0.6f).setCenterText("3,000")
                .setLabelMode(PieSeries.LABELS_INSIDE)
                .setData(new String[]{"Rent", "Food", "Transport", "Fun", "Savings"},
                        new double[]{1200, 600, 250, 300, 650}));
        // end::chart-doughnut[]
        return chart;
    }

    public Chart funnel() {
        // tag::chart-funnel[]
        Chart chart = new Chart().setTitle("Signup funnel");
        chart.addSeries(new FunnelSeries("Funnel").setData(
                new String[]{"Visited", "Viewed pricing", "Started trial", "Paid"},
                new double[]{12000, 4200, 900, 210}));
        // end::chart-funnel[]
        return chart;
    }

    public Chart treemap() {
        // tag::chart-treemap[]
        TreeNode root = new TreeNode("Portfolio");
        TreeNode tech = root.add(new TreeNode("Tech"));
        tech.add(new TreeNode("ACME", 120));
        tech.add(new TreeNode("Globex", 80));
        tech.add(new TreeNode("Initech", 35));
        TreeNode energy = root.add(new TreeNode("Energy"));
        energy.add(new TreeNode("Solar", 60));
        energy.add(new TreeNode("Wind", 40));
        root.add(new TreeNode("Cash", 30));
        Chart chart = new Chart().setTitle("Portfolio");
        chart.addSeries(new TreemapSeries("Holdings", root));
        // end::chart-treemap[]
        return chart;
    }

    public Chart sunburst() {
        // tag::chart-sunburst[]
        TreeNode root = new TreeNode("Disk");
        TreeNode media = root.add(new TreeNode("Media"));
        media.add(new TreeNode("Photos", 48));
        media.add(new TreeNode("Videos", 72));
        media.add(new TreeNode("Music", 20));
        TreeNode apps = root.add(new TreeNode("Apps"));
        apps.add(new TreeNode("Games", 35));
        apps.add(new TreeNode("Tools", 12));
        root.add(new TreeNode("System", 25));
        Chart chart = new Chart().setTitle("Storage (GB)");
        chart.addSeries(new SunburstSeries("Storage", root));
        // end::chart-sunburst[]
        return chart;
    }

    public Chart flameGraph() {
        // tag::chart-flame-graph[]
        TreeNode main = new TreeNode("main");
        TreeNode request = main.add(new TreeNode("handleRequest"));
        TreeNode parse = request.add(new TreeNode("parseJson"));
        parse.add(new TreeNode("readToken", 14));
        parse.add(new TreeNode("self", 6));
        TreeNode render = request.add(new TreeNode("render"));
        TreeNode layout = render.add(new TreeNode("layout"));
        layout.add(new TreeNode("measureText", 18));
        layout.add(new TreeNode("self", 7));
        TreeNode paint = render.add(new TreeNode("paint"));
        paint.add(new TreeNode("fillRect", 12));
        paint.add(new TreeNode("drawString", 16));
        request.add(new TreeNode("log", 5));
        TreeNode gc = main.add(new TreeNode("gc"));
        gc.add(new TreeNode("mark", 9));
        gc.add(new TreeNode("sweep", 6));
        main.add(new TreeNode("idle", 7));
        Chart chart = new Chart().setTitle("CPU time (ms)").setLegendPosition(Chart.LEGEND_NONE);
        chart.addSeries(new FlameGraphSeries("CPU", main));
        // end::chart-flame-graph[]
        return chart;
    }

    public Chart icicle() {
        Chart chart = flameGraph();
        // tag::chart-icicle[]
        FlameGraphSeries cpu = (FlameGraphSeries) chart.getSeries().get(0);
        cpu.setInverted(true);
        // end::chart-icicle[]
        return chart;
    }

    public Chart sankey() {
        // tag::modern-chart-sankey[]
        Chart chart = new Chart().setTitle("Energy flow");
        SankeySeries flow = new SankeySeries("Energy");
        flow.addLink("Solar", "Grid", 40).addLink("Wind", "Grid", 30).addLink("Gas", "Grid", 60)
                .addLink("Gas", "Industry", 20).addLink("Grid", "Homes", 55)
                .addLink("Grid", "Industry", 45).addLink("Grid", "Losses", 30);
        chart.addSeries(flow);
        // end::modern-chart-sankey[]
        return chart;
    }

    public Chart gauge() {
        // tag::chart-gauge[]
        Chart chart = new Chart().setTitle("CPU load");
        chart.addSeries(new GaugeSeries("CPU", 0, 100).setValue(72).setUnit("%")
                .addBand(0, 60, 0x009E73).addBand(60, 85, 0xE69F00).addBand(85, 100, 0xD55E00));
        // end::chart-gauge[]
        return chart;
    }

    public Chart radar() {
        // tag::chart-radar[]
        Chart chart = new Chart().setTitle("Players");
        chart.addSeries(new RadarSeries("Alice", "Speed", "Power", "Range", "Defense", "Stamina", "Agility")
                .setValues(8, 6, 7, 5, 9, 7));
        chart.addSeries(new RadarSeries("Bob").setValues(5, 9, 4, 8, 6, 3));
        // end::chart-radar[]
        return chart;
    }

    public Container sparklines() {
        // tag::chart-sparkline[]
        Container list = new Container(BoxLayout.y());
        String[] names = {"ACME", "Globex", "Initech"};
        int[] types = {Sparkline.LINE, Sparkline.AREA, Sparkline.BARS};
        for (int i = 0; i < names.length; i++) {
            double[] values = new double[20];
            for (int k = 0; k < values.length; k++) {
                values[k] = (k * (i + 3) * 17) % 23;
            }
            Container row = new Container(new BorderLayout());
            row.add(BorderLayout.CENTER, new Label(names[i]));
            row.add(BorderLayout.EAST, new Sparkline(values).setType(types[i]));
            list.add(row);
        }
        // end::chart-sparkline[]
        return list;
    }

    public Chart secondaryAxis() {
        // tag::chart-secondary-axis[]
        Chart chart = new Chart().setTitle("Orders and conversion")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "Jan", "Feb", "Mar", "Apr", "May", "Jun"));
        chart.setSecondaryYAxis(new NumberAxis(Axis.RIGHT).setTitle("%"));
        chart.getYAxis().setTitle("Orders");
        chart.addSeries(new BarSeries("Orders").setData(null, new double[]{320, 410, 380, 460, 520, 490}));
        chart.addSeries(new LineSeries("Conversion").setMarkers(true)
                .setData(null, new double[]{2.1, 2.6, 2.4, 3.0, 3.4, 3.1}).setSecondaryAxis(true));
        // end::chart-secondary-axis[]
        return chart;
    }

    public Chart logAxis() {
        // tag::chart-log-axis[]
        Chart chart = new Chart().setTitle("Users")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "2019", "2020", "2021", "2022", "2023", "2024", "2025"))
                .setYAxis(new LogAxis(Axis.LEFT));
        chart.addSeries(new LineSeries("Users").setMarkers(true)
                .setData(null, new double[]{40, 300, 2500, 18000, 90000, 600000, 2400000}));
        // end::chart-log-axis[]
        return chart;
    }

    public Chart annotations() {
        // tag::chart-annotations[]
        Chart chart = new Chart().setTitle("Server load")
                .setXAxis(new CategoryAxis(Axis.BOTTOM, "0h", "4h", "8h", "12h", "16h", "20h"));
        chart.addSeries(new LineSeries("CPU %").setData(null, new double[]{22, 18, 55, 78, 64, 35}));
        chart.addAnnotation(Annotation.valueLine(70, "Alert"));
        chart.addAnnotation(Annotation.valueBand(40, 60, "Normal"));
        chart.addAnnotation(Annotation.xBand(2, 3, "Deploy"));
        // end::chart-annotations[]
        return chart;
    }

    public Chart denseLabels() {
        // tag::chart-dense-labels[]
        String[] cities = {"Amsterdam", "Barcelona", "Copenhagen", "Dusseldorf", "Edinburgh", "Frankfurt",
            "Gothenburg", "Helsinki", "Istanbul", "Johannesburg", "Krakow", "Lisbon"};
        double[] values = {42, 55, 48, 63, 71, 66, 80, 77, 69, 85, 92, 88};
        Chart chart = new Chart().setTitle("Visitors").setXAxis(new CategoryAxis(Axis.BOTTOM, cities));
        chart.addSeries(new BarSeries("Thousands").setData(null, values).setShowValues(true));
        // end::chart-dense-labels[]
        return chart;
    }

    public Chart legendRight() {
        Chart chart = line();
        // tag::chart-legend-right[]
        chart.setLegendPosition(Chart.LEGEND_RIGHT);
        // end::chart-legend-right[]
        return chart;
    }

    public Chart loading() {
        Chart chart = groupedBars();
        // tag::chart-loading[]
        chart.setLoading("Loading...");
        // end::chart-loading[]
        return chart;
    }
}
