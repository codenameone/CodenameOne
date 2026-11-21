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
import com.codename1.ui.chart.ChartContext;
import com.codename1.ui.chart.ChartPoint;

/// Values with an uncertainty interval, drawn as a marker with whiskers. Layer it over a bar or
/// line series of the same values to add error bars to them.
///
/// ```java
/// double[] mean = {5, 7, 6}, low = {4, 6, 4.5}, high = {6, 8.5, 7};
/// new ErrorBarSeries("Mean").setIntervals(null, mean, low, high);
/// ```
///
/// ![Error bars](https://www.codenameone.com/developer-guide/img/generated/chart-error-bars.png)
public class ErrorBarSeries extends XYSeries {
    private boolean markers = true;

    /// Creates an empty series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    public ErrorBarSeries(String name) {
        super(name, 2);
    }

    /// Replaces all values.
    ///
    /// #### Parameters
    ///
    /// - `x`: x values, or null for 0, 1, 2...
    ///
    /// - `value`: the central values
    ///
    /// - `low`: the lower end of each interval
    ///
    /// - `high`: the upper end of each interval
    ///
    /// #### Returns
    ///
    /// this series
    public ErrorBarSeries setIntervals(double[] x, double[] value, double[] low, double[] high) {
        setRows(x, value, new double[][]{low, high});
        return this;
    }

    /// Shows or hides the marker at the central value, for overlaying on bars.
    ///
    /// #### Parameters
    ///
    /// - `m`: true to draw markers
    ///
    /// #### Returns
    ///
    /// this series
    public ErrorBarSeries setMarkers(boolean m) {
        markers = m;
        repaintChart();
        return this;
    }

    /// The lower end of interval `i`.
    public double getLow(int i) {
        return getAux(0, i);
    }

    /// The upper end of interval `i`.
    public double getHigh(int i) {
        return getAux(1, i);
    }

    @Override
    public double[] getYBounds() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < size(); i++) {
            double[] v = {getLow(i), getHigh(i), getY(i)};
            for (int k = 0; k < 3; k++) {
                if (!isGap(v[k])) {
                    min = Math.min(min, v[k]);
                    max = Math.max(max, v[k]);
                }
            }
        }
        return min > max ? null : new double[]{min, max};
    }

    @Override
    public void paint(ChartContext ctx) {
        int[] r = visibleRange(ctx);
        Graphics g = ctx.getGraphics();
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction()));
        g.setColor(getColor());
        // 15% of the spacing, but never wider than 4mm: zoomed in, the spacing spans the plot
        int cap = Math.min(ctx.mm(4), Math.max(ctx.mm(1.2f), Math.round(ctx.xLength(getMinXSpacing()) * 0.15f)));
        int dot = ctx.mm(1.4f);
        boolean rot = ctx.isRotated();
        for (int i = r[0]; i <= r[1]; i++) {
            // whiskers and marker move from where they were when the last transition started, so a
            // change elsewhere leaves this one still; only a first appearance grows out of the value
            double v = animatedY(ctx, i);
            if (isGap(v) || isGap(getX(i))) {
                continue;
            }
            // a NaN end has no whisker: it would round to pixel 0, so that side collapses onto
            // the value and gets no cap
            double low = getLow(i);
            double high = getHigh(i);
            boolean hasLo = !isGap(low);
            boolean hasHi = !isGap(high);
            double lo = hasLo ? animatedAux(ctx, 0, i) : v;
            double hi = hasHi ? animatedAux(ctx, 1, i) : v;
            int x0 = Math.round(ctx.screenX(this, getX(i), lo));
            int y0 = Math.round(ctx.screenY(this, getX(i), lo));
            int x1 = Math.round(ctx.screenX(this, getX(i), hi));
            int y1 = Math.round(ctx.screenY(this, getX(i), hi));
            if (hasLo || hasHi) {
                g.drawLine(x0, y0, x1, y1);
            }
            if (hasLo) {
                drawCap(g, rot, x0, y0, cap);
            }
            if (hasHi) {
                drawCap(g, rot, x1, y1, cap);
            }
            if (markers) {
                int cx = Math.round(ctx.screenX(this, getX(i), v));
                int cy = Math.round(ctx.screenY(this, getX(i), v));
                g.fillArc(cx - dot / 2, cy - dot / 2, dot, dot, 0, 360);
            }
        }
        g.setAlpha(alpha);
    }

    private static void drawCap(Graphics g, boolean rot, int x, int y, int cap) {
        if (rot) {
            g.drawLine(x, y - cap / 2, x, y + cap / 2);
        } else {
            g.drawLine(x - cap / 2, y, x + cap / 2, y);
        }
    }

    @Override
    public String formatTooltip(ChartContext ctx, ChartPoint p) {
        int i = p.getIndex();
        String n = getName() == null ? "" : getName() + ": ";
        return n + formatValue(ctx, getY(i)) + " (" + formatValue(ctx, getLow(i)) + " - " + formatValue(ctx, getHigh(i)) + ")";
    }

    @Override
    protected String tableValue(int i) {
        return formatValue(null, getY(i)) + " (" + formatValue(null, getLow(i)) + " - " + formatValue(null, getHigh(i)) + ")";
    }
}
