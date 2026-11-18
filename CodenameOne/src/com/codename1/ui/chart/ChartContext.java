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

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.layout.LabelLayout;

/// Everything a series needs to paint one frame: graphics, the plot rectangle, the axes
/// and how far the current animation has progressed.
public final class ChartContext {
    Graphics graphics;
    Chart chart;
    int plotX;
    int plotY;
    int plotW;
    int plotH;
    float progress = 1;
    Font font;
    LabelLayout labels;
    /// the chart component's x and y in its parent's paint coordinates
    int originX;
    int originY;

    ChartContext() {
    }

    /// The graphics to paint with; translated so the chart's top left is `(originX, originY)`.
    public Graphics getGraphics() {
        return graphics;
    }

    /// The chart being painted.
    public Chart getChart() {
        return chart;
    }

    /// The theme.
    public ChartTheme getTheme() {
        return chart.getTheme();
    }

    /// Left of the plot area, in the parent's paint coordinates (the coordinate space of
    /// `getGraphics()`), not absolute screen coordinates. A pointer's absolute x converts to
    /// this space as `absX - chart.getAbsoluteX() + chart.getX()`.
    public int getPlotX() {
        return plotX;
    }

    /// Top of the plot area, in the parent's paint coordinates. A pointer's absolute y
    /// converts as `absY - chart.getAbsoluteY() + chart.getY()`.
    public int getPlotY() {
        return plotY;
    }

    /// Plot width.
    public int getPlotWidth() {
        return plotW;
    }

    /// Plot height.
    public int getPlotHeight() {
        return plotH;
    }

    /// The chart's x in its parent's paint coordinates (`chart.getX()`); subtract it from a
    /// painted x to get one relative to the chart.
    public int getOriginX() {
        return originX;
    }

    /// The chart's y in its parent's paint coordinates (`chart.getY()`).
    public int getOriginY() {
        return originY;
    }

    /// The eased animation progress, 0 at the start of a transition and 1 when settled.
    public float getProgress() {
        return progress;
    }

    /// The label font.
    public Font getFont() {
        return font;
    }

    /// The shared collision layout: data labels placed through it never overlap each other
    /// or the axis labels.
    public LabelLayout getLabels() {
        return labels;
    }

    /// True when the category (x) axis runs vertically, as in a horizontal bar chart.
    public boolean isRotated() {
        return chart.isRotated();
    }

    /// The x axis.
    public Axis getXAxis() {
        return chart.getXAxis();
    }

    /// The value axis a series is measured against.
    public Axis getValueAxis(Series s) {
        return s.isSecondaryAxis() && chart.getSecondaryYAxis() != null ? chart.getSecondaryYAxis() : chart.getYAxis();
    }

    /// The x of a data point in the parent's paint coordinates (see `getPlotX()`).
    public float screenX(Series s, double x, double y) {
        if (chart.isRotated()) {
            return (float) (plotX + getValueAxis(s).toNormalized(y) * plotW);
        }
        return (float) (plotX + chart.getXAxis().toNormalized(x) * plotW);
    }

    /// The y of a data point in the parent's paint coordinates (see `getPlotY()`).
    public float screenY(Series s, double x, double y) {
        if (chart.isRotated()) {
            return (float) (plotY + chart.getXAxis().toNormalized(x) * plotH);
        }
        return (float) (plotY + plotH - getValueAxis(s).toNormalized(y) * plotH);
    }

    /// The length in pixels of `dx` units along the x axis (a category band, a bar width),
    /// measured from the start of the visible range. On a linear axis that is the same
    /// everywhere; on a non-linear one (a `LogAxis` x) it is the length at the low end only,
    /// so slot widths derived from it stay uniform rather than following each point.
    public float xLength(double dx) {
        Axis a = chart.getXAxis();
        double min = a.getVisibleMin();
        double n = Math.abs(a.toNormalized(min + dx) - a.toNormalized(min));
        if (n != n || n > 1) {
            // NaN (a log axis cannot place a non-positive bound) or wider than the plot
            n = n != n ? 0 : 1;
        }
        return (float) (n * (chart.isRotated() ? plotH : plotW));
    }

    /// Value to a pixel position along the value axis, for baselines.
    public float valuePosition(Series s, double y) {
        double n = getValueAxis(s).toNormalized(y);
        return chart.isRotated() ? (float) (plotX + n * plotW) : (float) (plotY + plotH - n * plotH);
    }

    /// A dimension in pixels from millimetres.
    public int mm(float mm) {
        return Chart.mm(mm);
    }
}
