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
package com.codename1.ui.chart;

import com.codename1.ui.Graphics;

/// One data series. A series owns its data, its colour and how it paints itself; there is
/// no separate renderer object to keep in step with it.
///
/// Changing data through a series' own methods animates the chart from the old values to
/// the new ones. Batch several changes inside `Chart.update(Runnable)` to animate them as
/// one transition.
public abstract class Series {
    private String name;
    private Integer color;
    private boolean visible = true;
    private boolean secondaryAxis;
    private boolean showValues;
    private ValueFormatter valueFormatter;
    Chart chart;
    /// 0 while hidden, 1 while shown; animated when the user toggles the series in the legend.
    float visibility = 1;
    /// visibility when the current transition started
    float visFrom = 1;

    /// Creates a named series.
    ///
    /// #### Parameters
    ///
    /// - `name`: shown in the legend and the tooltip
    protected Series(String name) {
        this.name = name;
    }

    /// The legend name.
    public String getName() {
        return name;
    }

    /// Renames the series.
    public Series setName(String name) {
        this.name = name;
        repaintChart();
        return this;
    }

    /// The explicit colour, or the theme colour for this series' position.
    public int getColor() {
        if (color != null) {
            return color.intValue();
        }
        if (chart != null) {
            return chart.getTheme().getSeriesColor(chart.indexOf(this));
        }
        return 0x0072B2;
    }

    /// Fixes the series colour instead of taking it from the theme palette.
    public Series setColor(int rgb) {
        color = Integer.valueOf(rgb);
        repaintChart();
        return this;
    }

    /// False while the user has hidden it from the legend or the app hid it.
    public boolean isVisible() {
        return visible;
    }

    /// Shows or hides the series with an animated transition.
    public Series setVisible(boolean visible) {
        if (this.visible != visible) {
            if (chart != null) {
                chart.beginChange();
            }
            this.visible = visible;
            if (chart != null) {
                chart.visibilityChanged(this);
            } else {
                visibility = visible ? 1 : 0;
                visFrom = visibility;
            }
        }
        return this;
    }

    /// True when the series is measured against the right hand value axis.
    public boolean isSecondaryAxis() {
        return secondaryAxis;
    }

    /// Plots the series against the secondary (right) value axis.
    public Series setSecondaryAxis(boolean secondaryAxis) {
        if (chart != null) {
            // snapshot first, or the transition replays whatever an earlier one captured
            chart.beginChange();
        }
        this.secondaryAxis = secondaryAxis;
        dataChanged();
        return this;
    }

    /// True when value labels are drawn next to the data. Labels that would collide are
    /// left out rather than drawn over each other.
    public boolean isShowValues() {
        return showValues;
    }

    /// Draws value labels on the data.
    public Series setShowValues(boolean showValues) {
        this.showValues = showValues;
        repaintChart();
        return this;
    }

    /// Formatter for value labels and the tooltip, or null to use the value axis'.
    public ValueFormatter getValueFormatter() {
        return valueFormatter;
    }

    /// Sets how values print in labels and tooltips.
    public Series setValueFormatter(ValueFormatter valueFormatter) {
        this.valueFormatter = valueFormatter;
        repaintChart();
        return this;
    }

    /// The chart this series is in, or null.
    public Chart getChart() {
        return chart;
    }

    /// True for series drawn against x and y axes; false for pies, treemaps and other
    /// series that fill the plot themselves.
    public abstract boolean isCartesian();

    /// The extent of the data along x, `{min, max}`, or null when empty or not cartesian.
    public double[] getXBounds() {
        return null;
    }

    /// The extent along x of the data at or below `maxX`, `{min, max}`, or null when none is.
    /// A category axis with no labels asks for this to leave a far outlier out of its bands.
    ///
    /// #### Parameters
    ///
    /// - `maxX`: the largest x that counts
    ///
    /// #### Returns
    ///
    /// the bounds of the points that count, or null
    public double[] getXBoundsWithin(double maxX) {
        double[] b = getXBounds();
        return b != null && b[1] <= maxX ? b : null;
    }

    /// The extent of the data along y, `{min, max}`, or null when empty or not cartesian.
    public double[] getYBounds() {
        return null;
    }

    /// Paints the series.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the current frame: graphics, plot area, axes and animation progress
    public abstract void paint(ChartContext ctx);

    /// Finds the data point nearest a screen position.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the frame the series was last painted in
    ///
    /// - `x`: chart relative x
    ///
    /// - `y`: chart relative y
    ///
    /// - `radius`: how far from the point a touch may land and still hit it
    ///
    /// #### Returns
    ///
    /// the point, or null
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        return null;
    }

    /// The number of points along x, for a shared tooltip; -1 when not index aligned.
    public int getPointCount() {
        return -1;
    }

    /// The number of points `Chart.setSelection` and arrow-key stepping can reach by index.
    /// The same as `getPointCount()`, except for a series whose points are not index aligned
    /// with other series (scatter), which still has its own points to select.
    public int getSelectablePointCount() {
        return getPointCount();
    }

    /// True when `getPointCount()` counts points a paint laid out (treemap, sunburst, Sankey)
    /// rather than the model's points, so an index past it may exist after the next paint.
    protected boolean isPointCountFromLayout() {
        return false;
    }

    /// The point at `index` for a shared tooltip, or null.
    public ChartPoint getPoint(ChartContext ctx, int index) {
        return null;
    }

    /// Paints the legend swatch.
    ///
    /// #### Parameters
    ///
    /// - `g`: graphics
    ///
    /// - `x`: swatch left
    ///
    /// - `y`: swatch top
    ///
    /// - `size`: swatch size
    public void paintLegendSymbol(Graphics g, int x, int y, int size) {
        g.setColor(getColor());
        g.fillRoundRect(x, y, size, size, size / 3, size / 3);
    }

    /// Entries this series contributes to the legend; a single series by default, one per
    /// slice for a pie.
    public int getLegendEntryCount() {
        return 1;
    }

    /// The name of legend entry `i`.
    public String getLegendEntryName(int i) {
        return name;
    }

    /// Paints the swatch of legend entry `i`.
    public void paintLegendEntrySymbol(Graphics g, int i, int x, int y, int size) {
        paintLegendSymbol(g, x, y, size);
    }

    /// True if entry `i` is visible; a pie slice can be hidden on its own.
    public boolean isLegendEntryVisible(int i) {
        return visible;
    }

    /// Toggles legend entry `i`.
    public void toggleLegendEntry(int i) {
        setVisible(!visible);
    }

    /// Records the values currently on screen as the start of the next transition. Called
    /// by the chart before data changes; a subclass with animated data overrides it.
    protected void captureTransitionStart() {
    }

    /// Records a zero baseline as the start of the entry animation.
    protected void captureEntryStart() {
    }

    /// Called by the chart when a transition has finished, so a subclass can drop the start
    /// values it captured; a later change that forgot to snapshot then shows its new values
    /// instead of replaying a transition from long gone data.
    protected void transitionEnded() {
    }

    /// Formats a value for a label or tooltip.
    public String formatValue(ChartContext ctx, double value) {
        if (valueFormatter != null) {
            return valueFormatter.format(value, 0);
        }
        if (ctx != null && isCartesian()) {
            return ctx.getValueAxis(this).format(value, 0);
        }
        return com.codename1.ui.chart.axis.Axis.formatNumber(value, value == Math.rint(value) ? 0 : 2);
    }

    /// The tooltip row for a point: the series or point name and its formatted value. Series with
    /// several values per point (open/high/low/close, ranges, box plots) override this.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the last paint context
    ///
    /// - `p`: the point
    ///
    /// #### Returns
    ///
    /// one line of text
    public String formatTooltip(ChartContext ctx, ChartPoint p) {
        String n = p.getLabel() != null ? p.getLabel() : getName();
        return (n == null || n.length() == 0 ? "" : n + ": ") + formatValue(ctx, p.getY());
    }

    /// Adds this series' data to an accessible table, one `{series, item, value}` row per point.
    /// Used by `Chart.toTable()` and the chart's accessibility text.
    ///
    /// #### Parameters
    ///
    /// - `rows`: the list to append rows to
    public void collectTableRows(java.util.List<String[]> rows) {
    }

    /// Called when a point of this series is tapped, before the chart selects it. A series that
    /// navigates on tap (drilling into a treemap node) returns true to stop the selection.
    ///
    /// #### Parameters
    ///
    /// - `p`: the tapped point
    ///
    /// #### Returns
    ///
    /// true if the tap was consumed
    public boolean handleTap(ChartPoint p) {
        return false;
    }

    /// Called by the chart when this series is added to it, and again when the chart comes
    /// back on screen (its `initComponent`); a series may be told more than once. A series
    /// that listens to a model outliving it (a `TreeNode` tree) registers here, so the model
    /// does not keep a discarded chart reachable: a chart taken off screen, or a series
    /// removed from its chart, stops listening.
    protected void attached() {
    }

    /// Called by the chart when this series is removed from it, or the chart leaves the screen
    /// (its `deinitialize`); a series may be told more than once. Undoes `attached()`.
    protected void detached() {
    }

    /// Tells the chart this series' data changed, which animates the new values in.
    protected void dataChanged() {
        if (chart != null) {
            chart.seriesDataChanged(this);
        }
    }

    /// Repaints without a transition.
    protected void repaintChart() {
        if (chart != null) {
            chart.repaint();
        }
    }

    /// The current shown fraction, 0 hidden to 1 shown, animated while the user toggles it.
    protected float getVisibilityFraction() {
        return visibility;
    }

    /// True for a value that cannot be plotted: NaN or either infinity. Such a point is a gap,
    /// drawn as nothing and kept out of every axis bound.
    protected static boolean isGap(double v) {
        return v != v || Double.isInfinite(v);
    }

    /// Interpolates for transitions; a start that is a gap (NaN or infinite) means the value
    /// appears without a tween, since every in-between value of an infinite start is NaN or
    /// infinite and the point would vanish for the whole transition.
    protected static double lerp(double a, double b, double t) {
        if (isGap(a)) {
            return b;
        }
        if (isGap(b)) {
            return b;
        }
        return a + (b - a) * t;
    }
}
