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
package com.codename1.ui.chart.series;

import com.codename1.ui.Graphics;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.ChartContext;
import com.codename1.ui.chart.ChartPoint;
import com.codename1.ui.chart.Series;

import java.util.HashMap;

/// Base of series made of named parts that fill the plot: pie slices, funnel stages.
///
/// Each part gets its own legend entry and colour from the theme palette, and can be hidden on
/// its own by tapping its legend entry; the others animate to fill the gap.
public abstract class CategoricalSeries extends Series {
    String[] labels = new String[0];
    double[] values = new double[0];
    boolean[] hidden = new boolean[0];
    int[] colors;
    private double[] from;
    private boolean entry;

    /// Creates an empty series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    protected CategoricalSeries(String name) {
        super(name);
    }

    @Override
    public boolean isCartesian() {
        return false;
    }

    /// Replaces all parts.
    ///
    /// #### Parameters
    ///
    /// - `labels`: the name of each part
    ///
    /// - `values`: the value of each part, parallel to the labels
    ///
    /// #### Returns
    ///
    /// this series
    public CategoricalSeries setData(String[] labels, double[] values) {
        begin();
        int n = values.length;
        String[] oldLabels = this.labels;
        boolean[] oldHidden = hidden;
        this.labels = new String[n];
        this.values = new double[n];
        for (int i = 0; i < n; i++) {
            this.labels[i] = labels != null && i < labels.length ? labels[i] : String.valueOf(i + 1);
            this.values[i] = values[i];
        }
        hidden = carryHidden(oldLabels, oldHidden, this.labels);
        dataChanged();
        return this;
    }

    // counts the lookups carryHidden makes, so a test can hold it linear
    int hiddenLookups;

    // a part hidden from the legend stays hidden when new data still names it, wherever it moved.
    // Labels may repeat, so the k-th part of a name takes the state of the k-th old part of that
    // name; an unlabelled part keeps its index's state. Linear: each old index is chained to the
    // next old index of the same name, and a cursor per name walks that chain
    private boolean[] carryHidden(String[] oldLabels, boolean[] oldHidden, String[] labels) {
        int n = labels.length;
        boolean[] h = new boolean[n];
        boolean any = false;
        for (boolean b : oldHidden) {
            if (b) {
                any = true;
                break;
            }
        }
        if (!any) {
            return h;
        }
        if (oldLabels == null) {
            for (int i = 0; i < n && i < oldHidden.length; i++) {
                h[i] = oldHidden[i];
            }
            return h;
        }
        int m = Math.min(oldLabels.length, oldHidden.length);
        int[] next = new int[m];
        HashMap<String, Integer> cursor = new HashMap<String, Integer>();
        HashMap<String, Integer> tail = new HashMap<String, Integer>();
        for (int k = 0; k < m; k++) {
            next[k] = -1;
            String l = oldLabels[k];
            if (l == null) {
                continue;
            }
            hiddenLookups++;
            Integer t = tail.get(l);
            if (t == null) {
                cursor.put(l, Integer.valueOf(k));
            } else {
                next[t.intValue()] = k;
            }
            tail.put(l, Integer.valueOf(k));
        }
        for (int i = 0; i < n; i++) {
            String l = labels[i];
            if (l == null) {
                h[i] = i < oldHidden.length && oldHidden[i];
                continue;
            }
            hiddenLookups++;
            Integer c = cursor.get(l);
            if (c != null) {
                int k = c.intValue();
                if (k >= 0) {
                    h[i] = oldHidden[k];
                    cursor.put(l, Integer.valueOf(next[k]));
                }
            }
        }
        return h;
    }

    /// Changes the value of one part, animated.
    ///
    /// #### Parameters
    ///
    /// - `i`: the part index
    ///
    /// - `value`: the new value
    ///
    /// #### Returns
    ///
    /// this series
    public CategoricalSeries setValue(int i, double value) {
        begin();
        values[i] = value;
        dataChanged();
        return this;
    }

    /// Fixes the colour of each part instead of using the theme palette.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: one colour per part
    ///
    /// #### Returns
    ///
    /// this series
    public CategoricalSeries setColors(int... rgb) {
        colors = new int[rgb.length];
        System.arraycopy(rgb, 0, colors, 0, rgb.length);
        repaintChart();
        return this;
    }

    /// The number of parts.
    public int size() {
        return values.length;
    }

    /// The name of part `i`.
    public String getLabel(int i) {
        return labels[i];
    }

    /// The value of part `i`.
    public double getValue(int i) {
        return values[i];
    }

    /// The colour of part `i`.
    public int getColor(int i) {
        if (colors != null && i < colors.length) {
            return colors[i];
        }
        Chart c = getChart();
        return c == null ? 0x0072B2 : c.getTheme().getSeriesColor(i);
    }

    /// True if part `i` is shown.
    public boolean isShown(int i) {
        return i < hidden.length && !hidden[i];
    }

    private void begin() {
        Chart c = getChart();
        if (c != null) {
            c.beginChange();
        }
    }

    /// The value of part `i` in the current frame of an animation; hidden parts shrink to 0.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the paint context
    ///
    /// - `i`: the part index
    ///
    /// #### Returns
    ///
    /// the displayed value
    protected double displayValue(ChartContext ctx, int i) {
        double v = values[i];
        // NaN and negative parts draw as nothing; a NaN must not poison the total
        double target = hidden[i] || !(v > 0 && v < Double.POSITIVE_INFINITY) ? 0 : v; //NOPMD LogicInversion - NaN, infinities and negatives count as nothing
        double t = ctx == null ? 1 : ctx.getProgress();
        if (t >= 1 || from == null || i >= from.length) {
            return target;
        }
        return lerp(from[i], target, t);
    }

    /// True while the first appearance animation runs.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the paint context
    ///
    /// #### Returns
    ///
    /// true during the entry animation
    protected boolean isEntering(ChartContext ctx) {
        if (entry && ctx.getProgress() >= 1) {
            entry = false;
        }
        return entry;
    }

    /// The sum of the displayed values.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the paint context
    ///
    /// #### Returns
    ///
    /// the total
    protected double displayTotal(ChartContext ctx) {
        double s = 0;
        for (int i = 0; i < values.length; i++) {
            s += displayValue(ctx, i);
        }
        return s;
    }

    /// The sum of the visible target values, for percentages.
    public double getTotal() {
        double s = 0;
        for (int i = 0; i < values.length; i++) {
            // the same parts displayValue draws: an infinite part would make every share 0%
            if (!hidden[i] && values[i] > 0 && values[i] < Double.POSITIVE_INFINITY) {
                s += values[i];
            }
        }
        return s;
    }

    // A share is taken of values scaled by the largest shown one, so parts near Double.MAX_VALUE
    // neither overflow the total nor answer NaN. One rule for every percentage this series
    // prints, so a slice label and its tooltip always agree.
    private boolean countsForShare(int i) {
        double v = values[i];
        return !hidden[i] && v > 0 && v < Double.POSITIVE_INFINITY;
    }

    /// The largest value a percentage is taken of: shown, positive and finite; 0 when none.
    ///
    /// #### Returns
    ///
    /// the largest shown value
    protected double largestShown() {
        double maxPart = 0;
        for (int k = 0; k < values.length; k++) {
            if (countsForShare(k) && values[k] > maxPart) {
                maxPart = values[k];
            }
        }
        return maxPart;
    }

    /// The total of the shown values, each divided by `maxPart`.
    ///
    /// #### Parameters
    ///
    /// - `maxPart`: the value of `largestShown()`
    ///
    /// #### Returns
    ///
    /// the scaled total
    protected double scaledShownTotal(double maxPart) {
        double t = 0;
        if (maxPart > 0) {
            for (int k = 0; k < values.length; k++) {
                if (countsForShare(k)) {
                    t += values[k] / maxPart;
                }
            }
        }
        return t;
    }

    /// The percentage part `i` is of the shown parts, given the base from `largestShown()` and
    /// `scaledShownTotal()`; negative when the part has no share to print (hidden, NaN,
    /// negative or infinite).
    ///
    /// #### Parameters
    ///
    /// - `i`: the part index
    ///
    /// - `maxPart`: the largest shown value
    ///
    /// - `scaledTotal`: the total scaled by `maxPart`
    ///
    /// #### Returns
    ///
    /// the percentage, or a negative number
    protected double percentOf(int i, double maxPart, double scaledTotal) {
        double v = values[i];
        if (maxPart <= 0 || scaledTotal <= 0 || hidden[i] || !(v >= 0) || Double.isInfinite(v)) { //NOPMD LogicInversion - NaN is no share
            return -1;
        }
        return v / maxPart / scaledTotal * 100;
    }

    @Override
    protected void captureTransitionStart() {
        ChartContext ctx = getChart() == null ? null : getChart().getLastContext();
        double[] f = new double[values.length];
        for (int i = 0; i < f.length; i++) {
            f[i] = ctx == null ? 0 : displayValue(ctx, i);
        }
        from = f;
        entry = false;
    }

    @Override
    protected void captureEntryStart() {
        from = null;
        entry = true;
    }

    @Override
    public int getLegendEntryCount() {
        return values.length;
    }

    @Override
    public String getLegendEntryName(int i) {
        return labels[i];
    }

    @Override
    public void paintLegendEntrySymbol(Graphics g, int i, int x, int y, int size) {
        g.setColor(getColor(i));
        g.fillRoundRect(x, y, size, size, size / 3, size / 3);
    }

    @Override
    public boolean isLegendEntryVisible(int i) {
        return isShown(i);
    }

    @Override
    public void toggleLegendEntry(int i) {
        begin();
        hidden[i] = !hidden[i];
        dataChanged();
    }

    @Override
    public int getPointCount() {
        return values.length;
    }

    @Override
    public String formatTooltip(ChartContext ctx, ChartPoint p) {
        int i = p.getIndex();
        double share = percentOf(i, largestShown(), scaledShownTotal(largestShown()));
        String pct = share >= 0 ? " (" + com.codename1.ui.chart.axis.Axis.formatNumber(share, 1) + "%)" : "";
        return labels[i] + ": " + formatValue(ctx, values[i]) + pct;
    }

    /// A point describing part `i` at a screen position.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the paint context
    ///
    /// - `i`: the part index
    ///
    /// - `sx`: absolute screen x of the part's anchor
    ///
    /// - `sy`: absolute screen y of the part's anchor
    ///
    /// #### Returns
    ///
    /// the point
    protected ChartPoint point(ChartContext ctx, int i, float sx, float sy) {
        return new ChartPoint(this, i, i, values[i], Math.round(sx) - ctx.getOriginX(), Math.round(sy) - ctx.getOriginY(), labels[i]);
    }

    @Override
    public void collectTableRows(java.util.List<String[]> rows) {
        for (int i = 0; i < values.length; i++) {
            rows.add(new String[]{getName(), labels[i], formatValue(null, values[i])});
        }
    }
}
