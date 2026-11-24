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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.ChartTheme;
import com.codename1.ui.chart.Sparkline;
import com.codename1.ui.chart.ValueFormatter;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.axis.NumberAxis;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.FunnelSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.WaterfallSeries;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.bazaar.ui.Lang;

/// The merchant screens' charts, on `com.codename1.ui.chart` and coloured from
/// the `MeChart*` UIIDs so they follow the theme and dark mode: sales over time
/// with the period before dashed behind it, bars, a donut of shares, the
/// conversion funnel, the profit and loss waterfall, the dashboard sparklines
/// and a meter for a share. Money is long cents everywhere and is written by
/// `MerchantState.money`.
final class MCharts {
    private static final float HEIGHT_MM = 40f;

    /// Cents written as the store's money.
    static final ValueFormatter MONEY = (value, step) ->
            MerchantState.money((long) Math.floor(value + 0.5));

    /// Cents written short for an axis: 1.2k.
    static final ValueFormatter MONEY_AXIS = (value, step) -> compact(value / 100.0);

    /// A count written short.
    static final ValueFormatter COUNT = (value, step) -> compact(value);

    private MCharts() {
    }

    static int color(String uiid) {
        Style style = UIManager.getInstance().getComponentStyle(uiid);
        return style.getFgColor() & 0xffffff;
    }

    /// The chart theme from the app's own UIIDs: labels in `MeChartText`, grid in
    /// `MeChartGrid`, series in the `MeChartSlice` colours, the tooltip in the
    /// inverse of the surface. Built whenever a chart is, so a light or dark
    /// switch, which rebuilds the screens, is followed.
    static ChartTheme theme() {
        int text = color("MeChartText");
        int r = (text >> 16) & 0xff;
        int g = (text >> 8) & 0xff;
        int b = text & 0xff;
        boolean dark = r * 299 + g * 587 + b * 114 > 128000;
        int paper = UIManager.getInstance().getComponentStyle("MeChartBox").getBgColor() & 0xffffff;
        int ink = color("MeLegend");
        return new ChartTheme(dark)
                .setPalette(color("MeChartSlice1"), color("MeChartSlice2"),
                        color("MeChartSlice3"), color("MeChartSlice4"))
                .setTextColor(ink)
                .setSecondaryTextColor(text)
                .setLegendTextColor(text)
                .setGridColor(color("MeChartGrid"))
                .setAxisColor(color("MeChartGrid"))
                .setTooltipBackground(ink)
                .setTooltipText(paper)
                .setPositiveColor(color("MeChartSlice2"))
                .setNegativeColor(color("MeChartSlice4"));
    }

    /// A short number: 1.2k, 3.4M.
    static String compact(double value) {
        double size = Math.abs(value);
        if (size >= 1000000) {
            return trim(value / 1000000) + "M";
        }
        if (size >= 1000) {
            return trim(value / 1000) + "k";
        }
        return String.valueOf((long) Math.floor(value + 0.5));
    }

    private static String trim(double value) {
        long tenths = (long) Math.floor(value * 10 + 0.5);
        return tenths % 10 == 0 ? String.valueOf(tenths / 10)
                : (tenths / 10) + "." + Math.abs(tenths % 10);
    }

    /// `2026-11-20` as `11-20`; anything else as it is.
    static String shortDay(String label) {
        if (label != null && label.length() == 10 && label.charAt(4) == '-') {
            return label.substring(5);
        }
        return label == null ? "" : label;
    }

    private static String[] shortDays(String[] labels) {
        String[] out = new String[labels.length];
        for (int iter = 0; iter < labels.length; iter++) {
            out[iter] = shortDay(labels[iter]);
        }
        return out;
    }

    private static Chart chart(String name, float heightMm) {
        final int h = CN.convertToPixels(heightMm);
        Chart chart = new Chart() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(CN.convertToPixels(50f), h);
            }
        };
        chart.setName(name);
        chart.setUIID("MeChart");
        chart.setRTL(false);
        chart.setTheme(theme());
        chart.setZoomEnabled(false);
        chart.setTooltipMode(Chart.TOOLTIP_POINT);
        chart.setLegendPosition(Chart.LEGEND_NONE);
        return chart;
    }

    private static Chart cartesian(String name, String[] labels, ValueFormatter values) {
        Chart chart = chart(name, HEIGHT_MM);
        chart.setXAxis(new CategoryAxis(Axis.BOTTOM, shortDays(labels)).setGridVisible(false));
        Axis y = values == COUNT ? new CountAxis() : new NumberAxis(Axis.LEFT);
        chart.setYAxis(y.setIncludeZero(true).setFormatter(values));
        return chart;
    }

    /// A count axis: only whole-number ticks. A small maximum (2 orders on the
    /// busiest day) made the nice step 0.5, and those halves were rounded to
    /// the same label twice ("2, 2, 1, 1").
    static final class CountAxis extends NumberAxis {
        CountAxis() {
            super(Axis.LEFT);
        }

        @Override
        public double[] ticks(int maxCount) {
            return wholeTicks(super.ticks(maxCount));
        }
    }

    /// `ticks` without the fractional ones, ascending and each value once.
    static double[] wholeTicks(double[] ticks) {
        int n = 0;
        double[] out = new double[ticks.length];
        for (int iter = 0; iter < ticks.length; iter++) {
            double t = ticks[iter];
            if (t == Math.rint(t) && (n == 0 || out[n - 1] != t)) {
                out[n++] = t;
            }
        }
        double[] fit = new double[n];
        System.arraycopy(out, 0, fit, 0, n);
        return fit;
    }

    private static Container box(Component chart) {
        Container box = new Container(new BorderLayout(), "MeChartBox");
        box.setRTL(false);
        box.add(BorderLayout.CENTER, chart);
        return box;
    }

    /// Sales by day in cents, with the period before as a dashed line when given;
    /// a tap shows the day's amount.
    static Component line(String name, String[] labels, double[] now, double[] before) {
        Chart chart = cartesian(name, labels, MONEY_AXIS);
        if (before != null) {
            chart.addSeries(new LineSeries(Lang.tr("Previous period")).setDashed(true)
                    .setLineWidth(0.3f).setData(null, before)
                    .setColor(color("MeChartPrevious")).setValueFormatter(MONEY));
        }
        chart.addSeries(new LineSeries(Lang.tr("This period")).setMode(LineSeries.SPLINE)
                .setFilled(true).setFillAlpha(50).setLineWidth(0.5f).setData(null, now)
                .setColor(color("MeChartLine")).setValueFormatter(MONEY));
        if (before != null) {
            chart.setTooltipMode(Chart.TOOLTIP_SHARED);
            chart.setLegendPosition(Chart.LEGEND_TOP);
        }
        return box(chart);
    }

    /// Bars of `values`, written with `format`; a tap selects a bar and shows it.
    static Component bars(String name, String[] labels, double[] values, ValueFormatter format) {
        Chart chart = cartesian(name, labels, format == MONEY ? MONEY_AXIS : format);
        chart.addSeries(new BarSeries("").setCornerRadius(0.6f).setData(null, values)
                .setColor(color("MeChartBar")).setValueFormatter(format));
        return box(chart);
    }

    /// Shares of a whole in cents as a donut, with the legend beside it carrying
    /// each part's written share.
    static Component donut(String name, String[] labels, double[] values, String[] shares) {
        PieSeries pie = new PieSeries("").setInnerRadius(0.62f).setPadAngle(1.5f)
                .setLabelMode(PieSeries.LABELS_NONE);
        pie.setData(labels, values);
        pie.setValueFormatter(MONEY);
        Chart chart = chart(name, 30f);
        chart.addSeries(pie);
        chart.setPreferredW(CN.convertToPixels(30f));
        // Each part's name wraps on a line of its own with its share under it:
        // a name and a share side by side were both cut to "..." in a card a
        // third of a desktop wide.
        Container legend = new Container(BoxLayout.y());
        for (int iter = 0; iter < labels.length; iter++) {
            Container row = new Container(new BorderLayout(), "MeLegendRow");
            row.add(BorderLayout.WEST, BorderLayout.north(swatch(color("MeChartSlice"
                    + (iter % 4 + 1)))));
            Container words = new Container(BoxLayout.y());
            com.codename1.components.SpanLabel part = new com.codename1.components.SpanLabel(
                    labels[iter]);
            part.setUIID("Container");
            part.setTextUIID("MeLegend");
            words.add(part);
            Label share = new Label(shares[iter], "MeLegendValue");
            share.setShouldLocalize(false);
            words.add(share);
            row.add(BorderLayout.CENTER, words);
            legend.add(row);
        }
        Container box = new Container(new BorderLayout(), "MeChartBox");
        box.add(BorderLayout.WEST, chart);
        box.add(BorderLayout.CENTER, legend);
        return box;
    }

    private static Label swatch(int rgb) {
        Label swatch = new Label(" ", "MeSwatchDot");
        swatch.getAllStyles().setBgColor(rgb);
        swatch.getAllStyles().setBgTransparency(255);
        return swatch;
    }

    /// The conversion funnel: one band per step, narrowing with its count, each
    /// step's count written on it and its share of the first in the tooltip.
    static Component funnel(String name, String[] labels, int[] counts) {
        double[] values = new double[counts.length];
        for (int iter = 0; iter < counts.length; iter++) {
            values[iter] = Math.max(0, counts[iter]);
        }
        FunnelSeries funnel = new FunnelSeries(Lang.tr("Conversion funnel"));
        funnel.setData(labels, values);
        funnel.setShowValues(true);
        funnel.setValueFormatter(COUNT);
        return box(chart(name, 9f * Math.max(3, labels.length)).addSeries(funnel));
    }

    /// The profit and loss statement as a waterfall. `steps` are cents, negative
    /// for what is taken off; a step marked in `totals` shows the running total.
    static Component waterfall(String name, String[] labels, double[] steps, boolean[] totals) {
        Chart chart = cartesian(name, labels, MONEY_AXIS);
        chart.addSeries(new WaterfallSeries(Lang.tr("Profit and loss"))
                .setSteps(steps, totals).setConnectors(true)
                .setColor(color("MeChartBar")).setValueFormatter(MONEY));
        return box(chart);
    }

    /// A dashboard sparkline: a filled trend in the `MeSpark` colour, its last
    /// point marked.
    /// Daily columns, not a line: a fortnight of a small store has days with
    /// no orders, and a line through them is a saw edge that reads as noise.
    static Component sparkline(double[] values, String name) {
        Sparkline spark = new Sparkline(values) {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(CN.convertToPixels(30f), CN.convertToPixels(9f));
            }
        };
        spark.setType(Sparkline.BARS).setColor(color("MeSpark")).setShowLast(true);
        spark.setUIID("MeSpark");
        spark.setName(name);
        return spark;
    }

    /// A thin bar filled to `share` (0 to 1).
    static Component meter(final double share) {
        Component c = new Component() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(CN.convertToPixels(20f), CN.convertToPixels(1.4f));
            }

            @Override
            public void paint(Graphics g) {
                g.setColor(color("MeChartGrid"));
                g.fillRoundRect(getX(), getY(), getWidth(), getHeight(), getHeight(), getHeight());
                g.setColor(color("MeChartBar"));
                int w = (int) (getWidth() * Math.max(0, Math.min(1, share)));
                if (w > 0) {
                    g.fillRoundRect(getX(), getY(), w, getHeight(), getHeight(), getHeight());
                }
            }
        };
        c.setUIID("MeMeter");
        return c;
    }
}
