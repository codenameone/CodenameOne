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
        boolean[] h = new boolean[n];
        for (int i = 0; i < n; i++) {
            this.labels[i] = labels != null && i < labels.length ? labels[i] : String.valueOf(i + 1);
            this.values[i] = values[i];
            h[i] = wasHidden(oldLabels, oldHidden, this.labels[i], i);
        }
        hidden = h;
        dataChanged();
        return this;
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
    // a part hidden from the legend stays hidden when new data still names it, wherever it moved;
    // a label that keeps its index keeps that index's state, otherwise the first old part of that name decides
    private static boolean wasHidden(String[] oldLabels, boolean[] oldHidden, String label, int i) {
        if (oldLabels == null || label == null) {
            return i < oldHidden.length && oldHidden[i];
        }
        if (i < oldLabels.length && label.equals(oldLabels[i])) {
            return i < oldHidden.length && oldHidden[i];
        }
        for (int k = 0; k < oldLabels.length && k < oldHidden.length; k++) {
            if (label.equals(oldLabels[k])) {
                return oldHidden[k];
            }
        }
        return false;
    }

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
        double target = hidden[i] || !(v > 0) ? 0 : v; //NOPMD LogicInversion - the negated comparison is also true for NaN
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
            if (!hidden[i] && values[i] > 0) {
                s += values[i];
            }
        }
        return s;
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
        double total = getTotal();
        String pct = total > 0 ? " (" + com.codename1.ui.chart.axis.Axis.formatNumber(values[i] * 100 / total, 1) + "%)" : "";
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
