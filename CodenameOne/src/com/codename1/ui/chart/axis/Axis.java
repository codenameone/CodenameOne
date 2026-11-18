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
package com.codename1.ui.chart.axis;

import com.codename1.l10n.L10NManager;
import com.codename1.ui.chart.ValueFormatter;

/// Base of every chart axis. An axis owns two ranges: the **data range** the chart computes
/// from its series, and the **visible range** the user pans and zooms inside it.
///
/// Labels are never laid out by hand: the chart asks the axis for as many ticks as the
/// measured label size allows, so labels on a small screen thin out instead of overlapping.
public abstract class Axis {
    /// Axis drawn below the plot.
    public static final int BOTTOM = 0;
    /// Axis drawn left of the plot.
    public static final int LEFT = 1;
    /// Axis drawn right of the plot.
    public static final int RIGHT = 2;
    /// Axis drawn above the plot.
    public static final int TOP = 3;

    private int position;
    private String title;
    private boolean visible = true;
    private boolean gridVisible = true;
    private ValueFormatter formatter;
    private Double fixedMin;
    private Double fixedMax;
    private boolean includeZero;
    private double maxZoom = 1000;
    private boolean zoomEnabled = true;

    double dataMin;
    double dataMax = 1;
    double visibleMin;
    double visibleMax = 1;
    private boolean viewportSet;

    /// Creates an axis at the given position.
    ///
    /// #### Parameters
    ///
    /// - `position`: one of `BOTTOM`, `LEFT`, `RIGHT` or `TOP`
    protected Axis(int position) {
        this.position = position;
    }

    /// The side of the plot this axis is drawn on.
    public int getPosition() {
        return position;
    }

    /// Moves the axis to another side.
    public Axis setPosition(int position) {
        this.position = position;
        return this;
    }

    /// True for an axis drawn left or right of the plot, which maps values vertically.
    public boolean isVertical() {
        return position == LEFT || position == RIGHT;
    }

    /// The axis title, or null.
    public String getTitle() {
        return title;
    }

    /// Sets the title drawn alongside the axis.
    public Axis setTitle(String title) {
        this.title = title;
        return this;
    }

    /// Whether the axis line and labels are drawn.
    public boolean isVisible() {
        return visible;
    }

    /// Hides or shows the axis; a hidden axis still maps values.
    public Axis setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /// Whether grid lines are drawn at the ticks.
    public boolean isGridVisible() {
        return gridVisible;
    }

    /// Enables or disables grid lines at the ticks.
    public Axis setGridVisible(boolean gridVisible) {
        this.gridVisible = gridVisible;
        return this;
    }

    /// The formatter, or null for the axis' default.
    public ValueFormatter getFormatter() {
        return formatter;
    }

    /// Replaces how labels are formatted.
    public Axis setFormatter(ValueFormatter formatter) {
        this.formatter = formatter;
        return this;
    }

    /// Pins the bounds instead of deriving them from the data; pass null to restore automatic.
    public Axis setRange(Double min, Double max) {
        this.fixedMin = min;
        this.fixedMax = max;
        return this;
    }

    /// True when `setRange` pinned the low end, so `adjustBounds` must keep it as given.
    protected boolean isMinFixed() {
        return fixedMin != null;
    }

    /// True when `setRange` pinned the high end, so `adjustBounds` must keep it as given.
    protected boolean isMaxFixed() {
        return fixedMax != null;
    }

    /// Forces zero into the automatic range, as a bar chart's value axis needs.
    public Axis setIncludeZero(boolean includeZero) {
        this.includeZero = includeZero;
        return this;
    }

    /// True when zero is forced into the range.
    public boolean isIncludeZero() {
        return includeZero;
    }

    /// The largest magnification pan and zoom may reach, relative to the full data range.
    public Axis setMaxZoom(double maxZoom) {
        this.maxZoom = Math.max(1, maxZoom);
        return this;
    }

    /// Whether gestures may zoom and pan this axis.
    public boolean isZoomEnabled() {
        return zoomEnabled;
    }

    /// Allows or blocks zooming and panning along this axis.
    public Axis setZoomEnabled(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
        return this;
    }

    /// Called by the chart with the extent of the data on this axis.
    ///
    /// #### Parameters
    ///
    /// - `min`: smallest value of any visible series
    ///
    /// - `max`: largest value of any visible series
    public void setDataBounds(double min, double max) {
        // the last guard against a series or caller that hands over NaN or infinity: such an
        // end carries no extent, so it collapses onto the other end (or 0..1 when neither is
        // usable) and the empty-range padding below takes over
        boolean minOk = min == min && !Double.isInfinite(min);
        boolean maxOk = max == max && !Double.isInfinite(max);
        if (!minOk || !maxOk) {
            if (minOk) {
                max = min;
            } else if (maxOk) {
                min = max;
            } else {
                min = 0;
                max = 1;
            }
        }
        if (includeZero) {
            min = Math.min(min, 0);
            max = Math.max(max, 0);
        }
        if (fixedMin != null) {
            min = fixedMin.doubleValue();
        }
        if (fixedMax != null) {
            max = fixedMax.doubleValue();
        }
        if (fixedMin != null && fixedMax != null && min > max) {
            // both ends given in the wrong order: take them as a range
            double t = min;
            min = max;
            max = t;
        }
        if (!(max > min)) { //NOPMD LogicInversion - the negated comparison is also true for NaN
            // a fixed bound beyond the data keeps its side; only the free side moves
            double anchor = fixedMin != null ? min : max;
            double pad = anchor == 0 ? 1 : Math.abs(anchor) * 0.1;
            if (fixedMin != null && fixedMax == null) {
                max = min + pad;
            } else if (fixedMax != null && fixedMin == null) {
                min = max - pad;
            } else {
                min -= pad;
                max += pad;
            }
        }
        double[] adj = adjustBounds(min, max);
        dataMin = adj[0];
        dataMax = adj[1];
        if (!viewportSet) {
            visibleMin = dataMin;
            visibleMax = dataMax;
        } else {
            // new data bounds (a setMaxPoints window sliding) re-clamp a zoomed viewport here,
            // so it cannot be left hanging past the data
            clampViewport();
        }
    }

    /// Lets a subclass pad or round the automatic bounds; the default rounds a numeric range
    /// outward to the nearest nice step so the outermost grid lines sit on the plot edge.
    ///
    /// #### Parameters
    ///
    /// - `min`: the raw minimum
    ///
    /// - `max`: the raw maximum
    ///
    /// #### Returns
    ///
    /// `{min, max}` to use
    protected double[] adjustBounds(double min, double max) {
        if (fixedMin != null && fixedMax != null) {
            return new double[]{min, max};
        }
        double step = Ticks.niceStep((max - min) / 5);
        double lo = fixedMin != null ? min : Math.floor(min / step) * step;
        double hi = fixedMax != null ? max : Math.ceil(max / step) * step;
        return new double[]{lo, hi};
    }

    /// The low end of the data range.
    public double getDataMin() {
        return dataMin;
    }

    /// The high end of the data range.
    public double getDataMax() {
        return dataMax;
    }

    /// The low end of what is on screen.
    public double getVisibleMin() {
        return visibleMin;
    }

    /// The high end of what is on screen.
    public double getVisibleMax() {
        return visibleMax;
    }

    /// Shows exactly `[min, max]`, clamped to the data range and the zoom limit. Showing the
    /// whole data range clears the zoom, so later data changes rescale the axis again.
    public void setVisibleRange(double min, double max) {
        visibleMin = min;
        visibleMax = max;
        viewportSet = true;
        clampViewport();
    }

    /// Sets what is drawn for one frame of an animated transition, without clamping and
    /// without marking the axis as zoomed; the next data update replaces it.
    public void setDisplayRange(double min, double max) {
        visibleMin = min;
        visibleMax = max;
    }

    /// Returns to the full data range.
    public void resetViewport() {
        viewportSet = false;
        visibleMin = dataMin;
        visibleMax = dataMax;
    }

    /// True when the user has zoomed in, so the visible range is narrower than the data.
    public boolean isZoomed() {
        return viewportSet;
    }

    /// Zooms around a point.
    ///
    /// #### Parameters
    ///
    /// - `factor`: above 1 magnifies, below 1 shrinks
    ///
    /// - `anchor`: the normalized position (0..1 along the axis) that stays put
    public void zoom(double factor, double anchor) {
        if (!zoomEnabled || !(factor > 0)) { //NOPMD LogicInversion - the negated comparison is also true for NaN
            return;
        }
        double a = forward(visibleMin);
        double b = forward(visibleMax);
        double at = a + anchor * (b - a);
        double na = at - (at - a) / factor;
        double nb = na + (b - a) / factor;
        setVisibleRange(inverse(na), inverse(nb));
    }

    /// Pans by a fraction of the visible span; positive moves toward larger values.
    public void pan(double normalizedDelta) {
        if (!zoomEnabled) {
            return;
        }
        double a = forward(visibleMin);
        double b = forward(visibleMax);
        double d = normalizedDelta * (b - a);
        setVisibleRange(inverse(a + d), inverse(b + d));
    }

    private void clampViewport() {
        double lo0 = forward(dataMin);
        double hi0 = forward(dataMax);
        double full = hi0 - lo0;
        double minSpan = full / maxZoom;
        double span = Math.min(full, Math.max(minSpan, forward(visibleMax) - forward(visibleMin)));
        double lo = forward(visibleMin);
        if (lo < lo0) {
            lo = lo0;
        }
        if (lo + span > hi0) {
            lo = hi0 - span;
        }
        if (span >= full * 0.9999) {
            viewportSet = false;
            visibleMin = dataMin;
            visibleMax = dataMax;
            return;
        }
        visibleMin = inverse(lo);
        visibleMax = inverse(lo + span);
    }

    /// Maps a value into the space the axis is linear in; the identity here, `log10` for a
    /// logarithmic axis. Zoom, pan and normalization all work in this space.
    protected double forward(double value) {
        return value;
    }

    /// The inverse of `forward`.
    protected double inverse(double t) {
        return t;
    }

    /// Maps a value to 0..1 across the visible range (outside 0..1 when off screen).
    public double toNormalized(double value) {
        double a = forward(visibleMin);
        double b = forward(visibleMax);
        return b == a ? 0 : (forward(value) - a) / (b - a);
    }

    /// The inverse of `toNormalized`.
    public double fromNormalized(double n) {
        double a = forward(visibleMin);
        double b = forward(visibleMax);
        return inverse(a + n * (b - a));
    }

    /// Ticks for the visible range.
    ///
    /// #### Parameters
    ///
    /// - `maxCount`: the most ticks the measured label size allows
    ///
    /// #### Returns
    ///
    /// ascending tick values
    public double[] ticks(int maxCount) {
        return Ticks.linear(visibleMin, visibleMax, maxCount);
    }

    /// Formats a tick or value with the custom formatter if set, else the default.
    ///
    /// #### Parameters
    ///
    /// - `value`: the value
    ///
    /// - `step`: neighbouring tick spacing, or 0
    ///
    /// #### Returns
    ///
    /// the label
    public String format(double value, double step) {
        if (formatter != null) {
            return formatter.format(value, step);
        }
        return defaultFormat(value, step);
    }

    /// The axis' built in formatting; numbers are grouped and localized.
    protected String defaultFormat(double value, double step) {
        int decimals = step > 0 ? Ticks.decimalsFor(step) : (value == Math.rint(value) ? 0 : 2);
        double abs = Math.abs(value);
        if (step > 0 && abs >= 1e6 && step >= 1e5) {
            return compact(value, step);
        }
        return formatNumber(value, decimals);
    }

    /// Formats `value` with exactly `decimals` decimal places through the localization manager.
    /// Whole numbers print without a decimal point, which the double overload of
    /// `L10NManager.format` does not guarantee.
    ///
    /// #### Parameters
    ///
    /// - `value`: the number
    ///
    /// - `decimals`: decimal places, 0 or more
    ///
    /// #### Returns
    ///
    /// the localized text
    public static String formatNumber(double value, int decimals) {
        if (value != value) {
            return "";
        }
        if (decimals <= 0) {
            double r = Math.rint(value);
            if (r == 0) {
                r = 0;
            }
            if (Math.abs(r) < Integer.MAX_VALUE) {
                return L10NManager.getInstance().format((int) r);
            }
            return Long.toString((long) r);
        }
        return L10NManager.getInstance().format(value, decimals);
    }

    static String compact(double value, double step) {
        double abs = Math.abs(value);
        String suffix;
        double div;
        if (abs >= 1e12) {
            suffix = "T";
            div = 1e12;
        } else if (abs >= 1e9) {
            suffix = "B";
            div = 1e9;
        } else {
            suffix = "M";
            div = 1e6;
        }
        int dec = Ticks.decimalsFor(step / div);
        return formatNumber(value / div, Math.min(dec, 2)) + suffix;
    }
}
