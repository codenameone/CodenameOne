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

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.MultipleCategorySeries;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.DefaultRenderer;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.util.NumberFormat;
import com.codename1.charts.views.BarChart;
import com.codename1.charts.views.DoughnutChart;
import com.codename1.charts.views.LineChart;
import com.codename1.charts.views.PointStyle;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codenameone.examples.bazaar.ui.Lang;

/// The analytics screen's charts, coloured from the `MeChart*` UIIDs so they
/// follow the theme and dark mode: a line of sales with the period before
/// dashed behind it, bars of orders, a donut of channels, a funnel and a
/// meter for a share.
final class MCharts {
    private static final float HEIGHT_MM = 40f;

    private MCharts() {
    }

    static int color(String uiid) {
        Style style = UIManager.getInstance().getComponentStyle(uiid);
        return 0xff000000 | style.getFgColor();
    }

    private static int paper() {
        return 0xff000000 | UIManager.getInstance().getComponentStyle("MeChartBox").getBgColor();
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

    private static XYMultipleSeriesRenderer frame(String[] labels) {
        XYMultipleSeriesRenderer r = new XYMultipleSeriesRenderer();
        int grid = color("MeChartGrid");
        int text = color("MeChartText");
        r.setAxesColor(grid);
        r.setGridColor(grid);
        r.setShowGridY(true);
        r.setShowGridX(false);
        r.setLabelsColor(text);
        r.setXLabelsColor(text);
        r.setYLabelsColor(0, text);
        r.setLabelsTextSize(CN.convertToPixels(2.3f));
        r.setShowLegend(false);
        r.setPanEnabled(false, false);
        r.setZoomEnabled(false, false);
        r.setInScroll(true);
        r.setApplyBackgroundColor(true);
        r.setBackgroundColor(paper());
        r.setMarginsColor(paper());
        r.setXLabels(0);
        r.setYLabels(4);
        r.setYLabelsAlign(Component.RIGHT);
        r.setYLabelsPadding(CN.convertToPixels(1f));
        r.setAxisTitleTextSize(0);
        int m = CN.convertToPixels(1f);
        r.setMargins(new int[] {m * 2, CN.convertToPixels(10f), 0, m * 4});
        r.setPointSize(0);
        r.setYLabelFormat(new NumberFormat() {
            @Override
            public String format(double value) {
                return compact(value);
            }
        }, 0);
        if (labels.length > 0) {
            int[] at = labels.length < 3 ? new int[] {0, labels.length - 1}
                    : new int[] {0, labels.length / 2, labels.length - 1};
            for (int iter = 0; iter < at.length; iter++) {
                r.addTextLabel(at[iter], shortDay(labels[at[iter]]));
            }
        }
        r.setXAxisMin(-0.5);
        r.setXAxisMax(Math.max(0.5, labels.length - 0.5));
        return r;
    }

    /// `2026-11-20` as `11-20`; anything else as it is.
    static String shortDay(String label) {
        if (label != null && label.length() == 10 && label.charAt(4) == '-') {
            return label.substring(5);
        }
        return label == null ? "" : label;
    }

    private static void range(XYMultipleSeriesRenderer r, double[] a, double[] b) {
        double hi = 1;
        for (int iter = 0; iter < a.length; iter++) {
            hi = Math.max(hi, a[iter]);
        }
        for (int iter = 0; b != null && iter < b.length; iter++) {
            hi = Math.max(hi, b[iter]);
        }
        r.setYAxisMin(0);
        r.setYAxisMax(hi * 1.1);
    }

    /// Sales by day, with the period before as a dashed line when given.
    static Component line(String name, String[] labels, double[] now, double[] before) {
        XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
        XYMultipleSeriesRenderer r = frame(labels);
        if (before != null) {
            XYSeries prev = new XYSeries(Lang.tr("Previous period"));
            for (int iter = 0; iter < before.length; iter++) {
                prev.add(iter, before[iter]);
            }
            data.addSeries(prev);
            XYSeriesRenderer p = new XYSeriesRenderer();
            p.setColor(color("MeChartPrevious"));
            p.setLineWidth(CN.convertToPixels(0.3f));
            p.setPointStyle(PointStyle.POINT);
            r.addSeriesRenderer(p);
        }
        XYSeries series = new XYSeries(Lang.tr("This period"));
        for (int iter = 0; iter < now.length; iter++) {
            series.add(iter, now[iter]);
        }
        data.addSeries(series);
        XYSeriesRenderer one = new XYSeriesRenderer();
        int brand = color("MeChartLine");
        one.setColor(brand);
        one.setLineWidth(CN.convertToPixels(0.5f));
        XYSeriesRenderer.FillOutsideLine fill = new XYSeriesRenderer.FillOutsideLine(
                XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
        fill.setColor((brand & 0x00ffffff) | 0x33000000);
        one.addFillOutsideLine(fill);
        r.addSeriesRenderer(one);
        range(r, now, before);
        Container box = box(name, new ChartComponent(new LineChart(data, r)));
        if (before != null) {
            box.add(BorderLayout.NORTH, legend(new String[] {"This period", "Previous period"},
                    new String[] {"MeChartLine", "MeChartPrevious"}));
        }
        return box;
    }

    /// Orders by day as bars.
    static Component bars(String name, String[] labels, double[] values) {
        XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries("");
        for (int iter = 0; iter < values.length; iter++) {
            series.add(iter, values[iter]);
        }
        data.addSeries(series);
        XYMultipleSeriesRenderer r = frame(labels);
        XYSeriesRenderer bar = new XYSeriesRenderer();
        bar.setColor(color("MeChartBar"));
        r.addSeriesRenderer(bar);
        r.setBarSpacing(0.25);
        range(r, values, null);
        return box(name, new ChartComponent(new BarChart(data, r, BarChart.Type.DEFAULT)));
    }

    /// Shares of a whole as a donut with its legend.
    static Component donut(String name, String[] labels, double[] values, String[] shares) {
        MultipleCategorySeries series = new MultipleCategorySeries("");
        series.add("", labels, values);
        DefaultRenderer renderer = new DefaultRenderer();
        String[] uiids = new String[labels.length];
        for (int iter = 0; iter < labels.length; iter++) {
            uiids[iter] = "MeChartSlice" + (iter % 4 + 1);
            SimpleSeriesRenderer slice = new SimpleSeriesRenderer();
            slice.setColor(color(uiids[iter]));
            renderer.addSeriesRenderer(slice);
        }
        renderer.setShowLabels(false);
        renderer.setShowLegend(false);
        renderer.setPanEnabled(false);
        renderer.setZoomEnabled(false);
        renderer.setInScroll(true);
        renderer.setApplyBackgroundColor(false);
        renderer.setMargins(new int[] {0, 0, 0, 0});
        ChartComponent chart = new ChartComponent(new DoughnutChart(series, renderer));
        chart.setFocusable(false);
        chart.setName(name);
        int size = CN.convertToPixels(28f);
        chart.setPreferredSize(new Dimension(size, size));
        chart.setUIID("MeChart");
        Container legend = new Container(BoxLayout.y());
        for (int iter = 0; iter < labels.length; iter++) {
            Container row = new Container(new BorderLayout());
            row.add(BorderLayout.WEST, swatch(uiids[iter]));
            row.add(BorderLayout.CENTER, new Label(labels[iter], "MeLegend"));
            Label share = new Label(shares[iter], "MeLegendValue");
            share.setShouldLocalize(false);
            row.add(BorderLayout.EAST, share);
            legend.add(row);
        }
        Container box = new Container(new BorderLayout(), "MeChartBox");
        box.add(BorderLayout.WEST, chart);
        box.add(BorderLayout.CENTER, legend);
        return box;
    }

    private static Label swatch(String uiid) {
        Label swatch = new Label(" ", "MeSwatchDot");
        swatch.getAllStyles().setBgColor(color(uiid) & 0xffffff);
        swatch.getAllStyles().setBgTransparency(255);
        return swatch;
    }

    static Container legend(String[] labels, String[] uiids) {
        Container row = new Container(BoxLayout.x());
        for (int iter = 0; iter < labels.length; iter++) {
            row.add(swatch(uiids[iter]));
            row.add(new Label(Lang.tr(labels[iter]), "MeLegend"));
        }
        return row;
    }

    private static Container box(String name, ChartComponent chart) {
        chart.setName(name);
        chart.setFocusable(false);
        chart.setUIID("MeChart");
        chart.setPreferredH(CN.convertToPixels(HEIGHT_MM));
        Container box = new Container(new BorderLayout(), "MeChartBox");
        box.setRTL(false);
        chart.setRTL(false);
        box.add(BorderLayout.CENTER, chart);
        return box;
    }

    /// A funnel: one band per step, as wide as its count against the first,
    /// with the step's name, count and share of the first written on it.
    static Component funnel(final String name, final String[] labels, final int[] counts) {
        Component c = new Component() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(CN.convertToPixels(60f),
                        CN.convertToPixels(9f) * Math.max(1, labels.length));
            }

            @Override
            public void paint(Graphics g) {
                int n = labels.length;
                if (n == 0) {
                    return;
                }
                int band = getHeight() / n;
                int gap = Math.max(1, CN.convertToPixels(0.8f));
                int top = Math.max(1, counts[0]);
                Font font = UIManager.getInstance().getComponentStyle("MeFunnel").getFont();
                g.setFont(font);
                int[] fills = {color("MeChartSlice1"), color("MeChartSlice2"),
                    color("MeChartSlice3"), color("MeChartSlice4")};
                int ink = color("MeFunnel");
                for (int iter = 0; iter < n; iter++) {
                    double share = Math.min(1.0, Math.max(0, counts[iter]) / (double) top);
                    int w = Math.max(CN.convertToPixels(2f), (int) (getWidth() * share));
                    int x = getX() + (getWidth() - w) / 2;
                    int y = getY() + iter * band;
                    g.setColor(fills[iter % fills.length] & 0xffffff);
                    g.setAlpha(255);
                    g.fillRoundRect(x, y, w, band - gap, gap * 2, gap * 2);
                    String pct = counts[0] <= 0 ? "" : "  " + Period.percent(
                            (int) (Math.max(0, counts[iter]) * 10000L / counts[0]));
                    String text = labels[iter] + "  " + counts[iter] + pct;
                    g.setColor(ink & 0xffffff);
                    int tw = font.stringWidth(text);
                    g.drawString(text, getX() + Math.max(0, (getWidth() - tw) / 2),
                            y + (band - gap - font.getHeight()) / 2);
                }
            }
        };
        c.setName(name);
        c.setUIID("MeFunnelBox");
        return c;
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
                g.setColor(color("MeChartGrid") & 0xffffff);
                g.fillRoundRect(getX(), getY(), getWidth(), getHeight(), getHeight(), getHeight());
                g.setColor(color("MeChartBar") & 0xffffff);
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
