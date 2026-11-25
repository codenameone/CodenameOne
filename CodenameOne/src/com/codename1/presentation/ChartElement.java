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
package com.codename1.presentation;

import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.CategoryAxis;
import com.codename1.ui.chart.axis.NumberAxis;
import com.codename1.ui.chart.series.BarSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.PieSeries;
import com.codename1.ui.chart.series.RadarSeries;
import com.codename1.ui.chart.series.ScatterSeries;

/// A chart on a slide: its data -- categories, named series of values, colors -- and its
/// kind. It is drawn by `com.codename1.ui.chart.Chart`, so a slide's chart looks like and
/// animates like any other chart in the application, and `toChart` gives the live,
/// interactive component for a slide show that wants one.
///
/// PPTX charts (`c:chartSpace`) and ODF charts are read into this model; a kind or option the
/// model lacks falls back to the nearest kind.
public final class ChartElement extends SlideElement {
    /// Vertical bars (OOXML's column chart).
    public static final String COLUMN = "column";

    /// Horizontal bars.
    public static final String BAR = "bar";

    /// Lines.
    public static final String LINE = "line";

    /// Filled areas under lines.
    public static final String AREA = "area";

    /// A pie of the first series.
    public static final String PIE = "pie";

    /// A pie with a hole.
    public static final String DOUGHNUT = "doughnut";

    /// Points: the categories, read as numbers, are x and every series is y; see
    /// [#xValues()].
    public static final String SCATTER = "scatter";

    /// A radar (spider) chart.
    public static final String RADAR = "radar";

    /// The kind, one of the constants.
    public final String kind;

    /// The title, or null.
    public final String title;

    /// True when bars or areas of different series stack.
    public final boolean stacked;

    /// True when a legend is shown.
    public final boolean legend;

    private final String[] categories;
    private final String[] seriesNames;
    private final double[][] values;
    private final int[] colors;

    /// A chart.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `kind`: one of the kind constants
    ///
    /// - `x`: left edge in points
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `categories`: the category labels
    ///
    /// - `seriesNames`: one name per series
    ///
    /// - `values`: one row of values per series, one value per category
    public ChartElement(String id, String kind, float x, float y, float width, float height,
            String[] categories, String[] seriesNames, double[][] values) {
        this(id, null, kind, x, y, width, height, 0, null, false, true, categories,
                seriesNames, values, null);
    }

    /// A chart with every property.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `name`: a name, or null
    ///
    /// - `kind`: one of the kind constants
    ///
    /// - `x`: left edge
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `rotation`: degrees clockwise
    ///
    /// - `title`: the title, or null
    ///
    /// - `stacked`: whether series stack
    ///
    /// - `legend`: whether a legend shows
    ///
    /// - `categories`: the category labels
    ///
    /// - `seriesNames`: the series names
    ///
    /// - `values`: one row per series
    ///
    /// - `colors`: one 0xRRGGBB per series, or null for the chart theme's
    public ChartElement(String id, String name, String kind, float x, float y, float width,
            float height, float rotation, String title, boolean stacked, boolean legend,
            String[] categories, String[] seriesNames, double[][] values, int[] colors) {
        super(id, name, x, y, width, height, rotation, false, false);
        this.kind = kind(kind);
        this.title = title;
        this.stacked = stacked;
        this.legend = legend;
        this.categories = categories == null ? new String[0] : copy(categories);
        int n = values == null ? 0 : values.length;
        this.values = new double[n][];
        for (int i = 0; i < n; i++) {
            double[] row = values[i] == null ? new double[0] : values[i];
            this.values[i] = new double[row.length];
            System.arraycopy(row, 0, this.values[i], 0, row.length);
        }
        this.seriesNames = new String[n];
        for (int i = 0; i < n; i++) {
            this.seriesNames[i] = seriesNames != null && i < seriesNames.length
                    && seriesNames[i] != null ? seriesNames[i] : "Series " + (i + 1);
        }
        if (colors == null) {
            this.colors = null;
        } else {
            this.colors = new int[colors.length];
            System.arraycopy(colors, 0, this.colors, 0, colors.length);
        }
    }

    private static String[] copy(String[] a) {
        String[] c = new String[a.length];
        for (int i = 0; i < a.length; i++) {
            c[i] = a[i] == null ? "" : a[i];
        }
        return c;
    }

    private static String kind(String k) {
        String[] all = {COLUMN, BAR, LINE, AREA, PIE, DOUGHNUT, SCATTER, RADAR};
        for (String s : all) {
            if (s.equals(k)) {
                return s;
            }
        }
        return COLUMN;
    }

    /// The x values of a scatter chart: its categories read as numbers, as PPTX's xVal and
    /// ODF's x range hold them, or 1, 2, 3... where a category is not a number.
    ///
    /// #### Returns
    ///
    /// one x value per category
    public double[] xValues() {
        int n = maxLength();
        double[] xs = new double[n];
        for (int j = 0; j < n; j++) {
            String s = j < categories.length ? categories[j] : null;
            double v = j + 1;
            if (s != null) {
                try {
                    v = Double.parseDouble(s.trim());
                } catch (NumberFormatException e) {
                    v = j + 1;
                }
            }
            xs[j] = Double.isNaN(v) || Double.isInfinite(v) ? j + 1 : v;
        }
        return xs;
    }

    /// The number of categories.
    ///
    /// #### Returns
    ///
    /// the count
    public int categoryCount() {
        return categories.length;
    }

    /// A category label.
    ///
    /// #### Parameters
    ///
    /// - `i`: the index
    ///
    /// #### Returns
    ///
    /// the label
    public String category(int i) {
        return categories[i];
    }

    /// The number of series.
    ///
    /// #### Returns
    ///
    /// the count
    public int seriesCount() {
        return values.length;
    }

    /// A series name.
    ///
    /// #### Parameters
    ///
    /// - `i`: the series
    ///
    /// #### Returns
    ///
    /// the name
    public String seriesName(int i) {
        return seriesNames[i];
    }

    /// A copy of one series' values.
    ///
    /// #### Parameters
    ///
    /// - `i`: the series
    ///
    /// #### Returns
    ///
    /// the values
    public double[] values(int i) {
        double[] c = new double[values[i].length];
        System.arraycopy(values[i], 0, c, 0, c.length);
        return c;
    }

    /// The color of a series, or -1 when the chart theme picks it.
    ///
    /// #### Parameters
    ///
    /// - `i`: the series
    ///
    /// #### Returns
    ///
    /// 0xRRGGBB or -1
    public int seriesColor(int i) {
        return colors != null && i < colors.length && colors[i] >= 0 ? colors[i] & 0xffffff : -1;
    }

    /// This chart with other data.
    ///
    /// #### Parameters
    ///
    /// - `cats`: categories
    ///
    /// - `names`: series names
    ///
    /// - `data`: one row per series
    ///
    /// #### Returns
    ///
    /// the chart
    public ChartElement withData(String[] cats, String[] names, double[][] data) {
        return new ChartElement(id, name, kind, x, y, width, height, rotation, title, stacked,
                legend, cats, names, data, colors);
    }

    /// This chart as another kind.
    ///
    /// #### Parameters
    ///
    /// - `value`: one of the kind constants
    ///
    /// #### Returns
    ///
    /// the chart
    public ChartElement withKind(String value) {
        return new ChartElement(id, name, value, x, y, width, height, rotation, title, stacked,
                legend, categories, seriesNames, values, colors);
    }

    /// This chart with a title.
    ///
    /// #### Parameters
    ///
    /// - `value`: the title, or null
    ///
    /// #### Returns
    ///
    /// the chart
    public ChartElement withTitle(String value) {
        return new ChartElement(id, name, kind, x, y, width, height, rotation, value, stacked,
                legend, categories, seriesNames, values, colors);
    }

    /// This chart stacked or side by side.
    ///
    /// #### Parameters
    ///
    /// - `value`: true to stack
    ///
    /// #### Returns
    ///
    /// the chart
    public ChartElement withStacked(boolean value) {
        return new ChartElement(id, name, kind, x, y, width, height, rotation, title, value,
                legend, categories, seriesNames, values, colors);
    }

    /// This chart with series colors.
    ///
    /// #### Parameters
    ///
    /// - `value`: 0xRRGGBB per series, or null
    ///
    /// #### Returns
    ///
    /// the chart
    public ChartElement withColors(int[] value) {
        return new ChartElement(id, name, kind, x, y, width, height, rotation, title, stacked,
                legend, categories, seriesNames, values, value);
    }

    /// A live `Chart` component showing this data. Each call makes a new one; a slide show
    /// places it over the slide so the audience gets tooltips and the entrance animation.
    ///
    /// #### Returns
    ///
    /// the chart
    public Chart toChart() {
        Chart c = new Chart();
        if (title != null && title.length() > 0) {
            c.setTitle(title);
        }
        c.setLegendPosition(legend ? Chart.LEGEND_BOTTOM : Chart.LEGEND_NONE);
        if (PIE.equals(kind) || DOUGHNUT.equals(kind)) {
            if (values.length > 0) {
                PieSeries p = new PieSeries(seriesNames[0]);
                p.setData(labels(values[0].length), values[0]);
                if (DOUGHNUT.equals(kind)) {
                    p.setInnerRadius(0.5f);
                }
                if (colors != null && colors.length > 0) {
                    p.setColors(colors);
                }
                c.addSeries(p);
            }
            return c;
        }
        if (RADAR.equals(kind)) {
            for (int i = 0; i < values.length; i++) {
                RadarSeries r = new RadarSeries(seriesNames[i], labels(values[i].length));
                r.setValues(values[i]);
                color(r, i);
                c.addSeries(r);
            }
            return c;
        }
        if (SCATTER.equals(kind)) {
            c.setXAxis(new NumberAxis(Axis.BOTTOM));
            c.setYAxis(new NumberAxis(Axis.LEFT));
            double[] xs = xValues();
            for (int i = 0; i < values.length; i++) {
                int n = Math.min(xs.length, values[i].length);
                double[] px = new double[n];
                double[] py = new double[n];
                System.arraycopy(xs, 0, px, 0, n);
                System.arraycopy(values[i], 0, py, 0, n);
                ScatterSeries s = new ScatterSeries(seriesNames[i]);
                s.setData(px, py, null);
                color(s, i);
                c.addSeries(s);
            }
            return c;
        }
        c.setXAxis(new CategoryAxis(Axis.BOTTOM, labels(maxLength())));
        c.setYAxis(new NumberAxis(Axis.LEFT));
        if (BAR.equals(kind)) {
            c.setRotated(true);
        }
        for (int i = 0; i < values.length; i++) {
            if (LINE.equals(kind) || AREA.equals(kind)) {
                LineSeries l = new LineSeries(seriesNames[i]);
                l.setData(null, values[i]);
                l.setMarkers(LINE.equals(kind));
                if (AREA.equals(kind)) {
                    l.setFilled(true);
                    if (stacked) {
                        l.setStackGroup("stack");
                    }
                }
                color(l, i);
                c.addSeries(l);
            } else {
                BarSeries b = new BarSeries(seriesNames[i]);
                b.setData(null, values[i]);
                if (stacked) {
                    b.setStackGroup("stack");
                }
                color(b, i);
                c.addSeries(b);
            }
        }
        return c;
    }

    private void color(com.codename1.ui.chart.Series s, int i) {
        int col = seriesColor(i);
        if (col >= 0) {
            s.setColor(col);
        }
    }

    private int maxLength() {
        int n = categories.length;
        for (double[] v : values) {
            n = Math.max(n, v.length);
        }
        return n;
    }

    /// The category labels, padded with numbers to `n`.
    private String[] labels(int n) {
        String[] l = new String[Math.max(n, categories.length)];
        for (int i = 0; i < l.length; i++) {
            l[i] = i < categories.length ? categories[i] : String.valueOf(i + 1);
        }
        return l;
    }

    @Override
    public SlideElement withTransform(float nx, float ny, float nw, float nh, float rot,
            boolean fh, boolean fv) {
        return new ChartElement(id, name, kind, nx, ny, nw, nh, rot, title, stacked, legend,
                categories, seriesNames, values, colors);
    }

    @Override
    public SlideElement withId(String value) {
        return new ChartElement(value, name, kind, x, y, width, height, rotation, title,
                stacked, legend, categories, seriesNames, values, colors);
    }

    @Override
    public String plainText() {
        StringBuilder sb = new StringBuilder(title == null ? "" : title);
        for (String name : seriesNames) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(name);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChartElement)) {
            return false;
        }
        ChartElement c = (ChartElement) o;
        if (!sameFrame(c) || !kind.equals(c.kind) || stacked != c.stacked || legend != c.legend
                || (title == null ? c.title != null : !title.equals(c.title))
                || categories.length != c.categories.length || values.length != c.values.length) {
            return false;
        }
        for (int i = 0; i < categories.length; i++) {
            if (!categories[i].equals(c.categories[i])) {
                return false;
            }
        }
        for (int i = 0; i < values.length; i++) {
            if (!seriesNames[i].equals(c.seriesNames[i]) || values[i].length != c.values[i].length
                    || seriesColor(i) != c.seriesColor(i)) {
                return false;
            }
            for (int j = 0; j < values[i].length; j++) {
                if (Double.doubleToLongBits(values[i][j])
                        != Double.doubleToLongBits(c.values[i][j])) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return frameHash() * 31 + kind.hashCode() + values.length;
    }
}
