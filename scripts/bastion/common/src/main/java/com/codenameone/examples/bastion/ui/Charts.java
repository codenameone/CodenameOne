/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.ui;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.MultipleCategorySeries;
import com.codename1.charts.models.RangeCategorySeries;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.views.BarChart;
import com.codename1.charts.views.DoughnutChart;
import com.codename1.charts.views.LineChart;
import com.codename1.charts.views.RangeBarChart;
import com.codename1.charts.util.NumberFormat;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import java.util.Date;

/// The app's charts, drawn by `com.codename1.charts`.
///
/// Nothing here names a colour: each is the foreground of a style in
/// `theme.css` (`BnChartLine`, `BnChartIn`, `BnChartOut`, `BnChartGrid`,
/// `BnChartText`, `BnChartSlice1`..`BnChartSlice5`), so dark mode and a
/// re-skin reach the charts the way they reach everything else.
///
/// Time runs left to right in every language, Hebrew included: a chart is a
/// picture of a time line, not a line of text, and the frame around it (the
/// title, the legend rows) is what mirrors.
public final class Charts {
    private static final float HEIGHT_MM = 34f;

    private Charts() {
    }

    /// Axis numbers the way a statement writes them in a margin: 950,
    /// 12.5k, 3.2M.
    private static final NumberFormat COMPACT = new NumberFormat() {
        @Override
        public String format(double value) {
            return compact(value);
        }
    };

    private static void money(XYMultipleSeriesRenderer renderer, final String prefix) {
        if (prefix == null || prefix.length() == 0) {
            return;
        }
        renderer.setYLabelFormat(new NumberFormat() {
            @Override
            public String format(double value) {
                return value < 0 ? "-" + prefix + compact(-value) : prefix + compact(value);
            }
        }, 0);
    }

    static String compact(double value) {
        double size = Math.abs(value);
        if (size >= 1000000) {
            return trim(value / 1000000) + "M";
        }
        if (size >= 1000) {
            return trim(value / 1000) + "k";
        }
        if (size >= 100 || value == Math.floor(value)) {
            return String.valueOf((long) Math.floor(value + 0.5));
        }
        if (size >= 1 || size == 0) {
            return trim(value);
        }
        // Volumes of a coin are fractions: keep two significant digits.
        double scale = 1;
        while (size * scale < 10) {
            scale *= 10;
        }
        return String.valueOf(Math.floor(value * scale + 0.5) / scale);
    }

    private static String trim(double value) {
        long tenths = (long) Math.floor(value * 10 + 0.5);
        return tenths % 10 == 0 ? String.valueOf(tenths / 10)
                : (tenths / 10) + "." + Math.abs(tenths % 10);
    }

    /// The colour of style `uiid`, opaque, as the chart library wants it.
    static int color(String uiid) {
        Style style = UIManager.getInstance().getComponentStyle(uiid);
        return 0xff000000 | style.getFgColor();
    }

    private static int text() {
        return CN.convertToPixels(2.3f);
    }

    /// A line of `values` (one a day), filled below, with the first and the
    /// last day written under it.
    ///
    /// @param name what a test finds the chart by
    public static Component line(String name, long[] days, double[] values) {
        return line(name, days, values, "");
    }

    /// The same line, its axis written as money: `prefix` is the currency
    /// sign, as in $8k.
    public static Component line(String name, long[] days, double[] values, String prefix) {
        XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("");
        for (int iter = 0; iter < values.length; iter++) {
            series.add(iter, values[iter]);
        }
        data.addSeries(series);
        XYMultipleSeriesRenderer renderer = frame();
        XYSeriesRenderer one = new XYSeriesRenderer();
        int brand = color("BnChartLine");
        one.setColor(brand);
        one.setLineWidth(CN.convertToPixels(0.5f));
        XYSeriesRenderer.FillOutsideLine fill = new XYSeriesRenderer.FillOutsideLine(
                XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
        fill.setColor((brand & 0x00ffffff) | 0x33000000);
        one.addFillOutsideLine(fill);
        renderer.addSeriesRenderer(one);
        dayLabels(renderer, days);
        range(renderer, values, values, false);
        money(renderer, prefix);
        return box(name, new ChartComponent(new LineChart(data, renderer)));
    }

    /// Money in and money out, a pair of bars a day.
    public static Component inOut(String name, long[] days, double[] in, double[] out) {
        XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
        XYSeries ins = new XYSeries(Lang.tr("Money in"));
        XYSeries outs = new XYSeries(Lang.tr("Money out"));
        for (int iter = 0; iter < days.length; iter++) {
            ins.add(iter, in[iter]);
            outs.add(iter, out[iter]);
        }
        data.addSeries(ins);
        data.addSeries(outs);
        XYMultipleSeriesRenderer renderer = frame();
        renderer.addSeriesRenderer(bar("BnChartIn"));
        renderer.addSeriesRenderer(bar("BnChartOut"));
        renderer.setBarSpacing(0.35);
        dayLabels(renderer, days);
        range(renderer, in, out, true);
        Container box = box(name, new ChartComponent(new BarChart(data, renderer,
                BarChart.Type.DEFAULT)));
        box.add(BorderLayout.NORTH, legend(new String[] {"Money in", "Money out"},
                new String[] {"BnChartIn", "BnChartOut"}));
        return box;
    }

    /// Bars of `values`, one series, in `uiid`'s colour.
    public static Component bars(String name, long[] days, double[] values, String uiid) {
        return bars(name, days, values, uiid, "");
    }

    /// Bars with a money axis; see [#line(String, long\[\], double\[\], String)].
    public static Component bars(String name, long[] days, double[] values, String uiid,
            String prefix) {
        return bars(name, days, values, uiid, prefix, 0.6f);
    }

    /// Bars `scale` times the standard chart height (a strip under the
    /// candles is 0.6, a panel of its own is taller).
    public static Component bars(String name, long[] days, double[] values, String uiid,
            String prefix, float scale) {
        XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("");
        for (int iter = 0; iter < values.length; iter++) {
            series.add(iter, values[iter]);
        }
        data.addSeries(series);
        XYMultipleSeriesRenderer renderer = frame();
        renderer.addSeriesRenderer(bar(uiid));
        renderer.setBarSpacing(0.25);
        dayLabels(renderer, days);
        range(renderer, values, values, true);
        money(renderer, prefix);
        return box(name, new ChartComponent(new BarChart(data, renderer, BarChart.Type.DEFAULT)),
                scale);
    }

    /// Candles: a body from the open to the close, rising in `BnChartIn`'s
    /// colour and falling in `BnChartOut`'s, over a pale bar from the low to
    /// the high. The chart library draws ranges, not wicks; a range one
    /// shade lighter behind the body reads the same.
    public static Component candles(String name, long[] starts, double[] open, double[] high,
            double[] low, double[] close) {
        RangeCategorySeries range = new RangeCategorySeries("range");
        RangeCategorySeries up = new RangeCategorySeries("up");
        RangeCategorySeries down = new RangeCategorySeries("down");
        for (int iter = 0; iter < open.length; iter++) {
            range.add(low[iter], high[iter]);
            boolean rising = close[iter] >= open[iter];
            double top = Math.max(open[iter], close[iter]);
            double bottom = Math.min(open[iter], close[iter]);
            if (top - bottom < (high[iter] - low[iter]) / 50) {
                // A doji still shows as a line.
                top = bottom + Math.max((high[iter] - low[iter]) / 50, 1e-9);
            }
            if (rising) {
                up.add(bottom, top);
                down.add(bottom, bottom);
            } else {
                up.add(bottom, bottom);
                down.add(bottom, top);
            }
        }
        XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
        data.addSeries(range.toXYSeries());
        data.addSeries(up.toXYSeries());
        data.addSeries(down.toXYSeries());
        XYMultipleSeriesRenderer renderer = frame();
        // The hour's whole range, quiet behind the open-close body.
        XYSeriesRenderer shadow = bar("BnChartText");
        renderer.addSeriesRenderer(shadow);
        renderer.addSeriesRenderer(bar("BnChartIn"));
        renderer.addSeriesRenderer(bar("BnChartOut"));
        renderer.setBarSpacing(0.3);
        renderer.setXAxisMin(0.5);
        renderer.setXAxisMax(open.length + 0.5);
        hourLabels(renderer, starts);
        range(renderer, low, high, false);
        return box(name, new ChartComponent(new RangeBarChart(data, renderer,
                BarChart.Type.STACKED)));
    }

    /// A ring of `values`, one slice each, with a legend of `labels` and the
    /// share of each beside it.
    public static Component donut(String name, String[] labels, double[] values,
            String[] shares) {
        MultipleCategorySeries series = new MultipleCategorySeries("");
        series.add("", labels, values);
        DefaultRenderer renderer = new DefaultRenderer();
        String[] uiids = new String[labels.length];
        for (int iter = 0; iter < labels.length; iter++) {
            uiids[iter] = "BnChartSlice" + (iter % 5 + 1);
            SimpleSeriesRenderer slice = new SimpleSeriesRenderer();
            slice.setColor(color(uiids[iter]));
            renderer.addSeriesRenderer(slice);
        }
        renderer.setShowLabels(false);
        renderer.setShowLegend(false);
        renderer.setPanEnabled(false);
        renderer.setZoomEnabled(false);
        renderer.setInScroll(true);
        renderer.setBackgroundColor(0);
        renderer.setApplyBackgroundColor(false);
        renderer.setMargins(new int[] {0, 0, 0, 0});
        ChartComponent chart = new ChartComponent(new DoughnutChart(series, renderer));
        chart.setFocusable(false);
        chart.setName(name);
        final int size = CN.convertToPixels(26f);
        chart.setPreferredSize(new Dimension(size, size));
        chart.setUIID("BnChart");
        Container legend = new Container(BoxLayout.y());
        for (int iter = 0; iter < labels.length; iter++) {
            Container row = new Container(new BorderLayout());
            Label swatch = new Label(" ", "BnSwatch");
            swatch.getAllStyles().setBgColor(color(uiids[iter]) & 0xffffff);
            swatch.getAllStyles().setBgTransparency(255);
            row.add(BorderLayout.WEST, swatch);
            row.add(BorderLayout.CENTER, new Label(labels[iter], "BnLegend"));
            row.add(BorderLayout.EAST, new Label(shares[iter], "BnLegendValue"));
            legend.add(row);
        }
        Container box = new Container(new BorderLayout(), "BnChartBox");
        box.add(BorderLayout.WEST, chart);
        box.add(BorderLayout.CENTER, legend);
        return box;
    }

    /// A legend of coloured squares, one per series, in a row.
    static Container legend(String[] labels, String[] uiids) {
        Container row = new Container(BoxLayout.x());
        for (int iter = 0; iter < labels.length; iter++) {
            Label swatch = new Label(" ", "BnSwatch");
            swatch.getAllStyles().setBgColor(color(uiids[iter]) & 0xffffff);
            swatch.getAllStyles().setBgTransparency(255);
            row.add(swatch);
            row.add(new Label(Lang.tr(labels[iter]), "BnLegend"));
        }
        return row;
    }

    private static XYSeriesRenderer bar(String uiid) {
        XYSeriesRenderer one = new XYSeriesRenderer();
        one.setColor(color(uiid));
        return one;
    }

    /// The axes, grid and text every XY chart here shares.
    private static XYMultipleSeriesRenderer frame() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        int grid = color("BnChartGrid");
        int label = color("BnChartText");
        renderer.setAxesColor(grid);
        renderer.setGridColor(grid);
        renderer.setShowGridY(true);
        renderer.setShowGridX(false);
        renderer.setLabelsColor(label);
        renderer.setXLabelsColor(label);
        renderer.setYLabelsColor(0, label);
        renderer.setLabelsTextSize(text());
        renderer.setShowLegend(false);
        renderer.setPanEnabled(false, false);
        renderer.setZoomEnabled(false, false);
        renderer.setInScroll(true);
        // The margins are painted opaque whatever their alpha, so they and
        // the plot take the box's own background and the chart has no frame.
        int paper = 0xff000000
                | UIManager.getInstance().getComponentStyle("BnChartBox").getBgColor();
        renderer.setApplyBackgroundColor(true);
        renderer.setBackgroundColor(paper);
        renderer.setMarginsColor(paper);
        renderer.setXLabels(0);
        renderer.setYLabels(4);
        renderer.setYLabelsAlign(Component.RIGHT);
        renderer.setYLabelsPadding(CN.convertToPixels(1f));
        int m = CN.convertToPixels(1f);
        // The library already keeps room under the axis for its labels (and an
        // axis title this chart has none of): no bottom margin on top of it.
        renderer.setAxisTitleTextSize(0);
        renderer.setMargins(new int[] {m * 2, CN.convertToPixels(11f), 0, m * 5});
        renderer.setPointSize(0);
        renderer.setYLabelFormat(COMPACT, 0);
        return renderer;
    }

    /// The y axis from zero (or the lowest value, for a price) to a little
    /// over the highest.
    private static void range(XYMultipleSeriesRenderer renderer, double[] a, double[] b,
            boolean fromZero) {
        double lo = Double.MAX_VALUE;
        double hi = -Double.MAX_VALUE;
        for (int iter = 0; iter < a.length; iter++) {
            lo = Math.min(lo, Math.min(a[iter], b[iter]));
            hi = Math.max(hi, Math.max(a[iter], b[iter]));
        }
        if (a.length == 0) {
            lo = 0;
            hi = 1;
        }
        if (fromZero || lo > 0 && lo < (hi - lo) / 3) {
            lo = Math.min(0, lo);
        }
        double pad = Math.max((hi - lo) * 0.08, Math.abs(hi) * 0.01 + 1e-9);
        renderer.setYAxisMin(fromZero && lo >= 0 ? 0 : lo - pad);
        renderer.setYAxisMax(hi + pad);
    }

    private static void dayLabels(XYMultipleSeriesRenderer renderer, long[] days) {
        if (days.length == 0) {
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("MMM d");
        int[] at = {0, days.length / 2, days.length - 1};
        for (int iter = 0; iter < at.length; iter++) {
            renderer.addTextLabel(at[iter], format.format(new Date(days[at[iter]])));
        }
        renderer.setXAxisMin(-0.5);
        renderer.setXAxisMax(days.length - 0.5);
    }

    private static void hourLabels(XYMultipleSeriesRenderer renderer, long[] starts) {
        if (starts.length == 0) {
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        int[] at = {0, starts.length / 2, starts.length - 1};
        for (int iter = 0; iter < at.length; iter++) {
            renderer.addTextLabel(at[iter] + 1, format.format(new Date(starts[at[iter]])));
        }
    }

    private static Container box(String name, ChartComponent chart) {
        return box(name, chart, 1f);
    }

    private static Container box(String name, ChartComponent chart, float scale) {
        chart.setName(name);
        chart.setFocusable(false);
        chart.setUIID("BnChart");
        chart.setPreferredH(CN.convertToPixels(HEIGHT_MM * scale));
        Container box = new Container(new BorderLayout(), "BnChartBox");
        // Mirrored by nothing: see the class comment.
        box.setRTL(false);
        chart.setRTL(false);
        box.add(BorderLayout.CENTER, chart);
        return box;
    }
}
