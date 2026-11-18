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

/// Unconnected points. Giving the points sizes turns it into a bubble chart: the bubble's
/// *area*, not its radius, is proportional to the size value, so big values do not look
/// quadratically bigger than they are.
public class ScatterSeries extends XYSeries {
    private double[] sizes;
    private float pointMm = 1.6f;
    private float maxBubbleMm = 9f;
    private int fillAlpha = 200;

    /// Creates an empty scatter series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the legend name
    public ScatterSeries(String name) {
        super(name);
    }

    /// Sets a size per point, making this a bubble series; null for plain points.
    public ScatterSeries setSizes(double... sizes) {
        this.sizes = sizes;
        if (sizes != null) {
            fillAlpha = 150;
        }
        repaintChart();
        return this;
    }

    /// Diameter of plain points in millimetres.
    public ScatterSeries setPointSize(float mm) {
        pointMm = mm;
        repaintChart();
        return this;
    }

    /// Diameter of the largest bubble in millimetres.
    public ScatterSeries setMaxBubbleSize(float mm) {
        maxBubbleMm = mm;
        repaintChart();
        return this;
    }

    private int diameter(ChartContext ctx, int i, double maxSize) {
        if (sizes == null || i >= sizes.length || maxSize <= 0) {
            return Math.max(3, ctx.mm(pointMm));
        }
        double f = Math.sqrt(Math.max(0, sizes[i]) / maxSize);
        return Math.max(3, (int) (ctx.mm(maxBubbleMm) * f));
    }

    private double maxSize() {
        double m = 0;
        if (sizes != null) {
            for (double s : sizes) {
                m = Math.max(m, s);
            }
        }
        return m;
    }

    @Override
    public void paint(ChartContext ctx) {
        Graphics g = ctx.getGraphics();
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction() * fillAlpha / 255));
        g.setColor(getColor());
        double ms = maxSize();
        double xmin = ctx.getXAxis().getVisibleMin();
        double xmax = ctx.getXAxis().getVisibleMax();
        for (int i = 0; i < count; i++) {
            if (xs[i] < xmin || xs[i] > xmax) {
                continue;
            }
            double v = animatedY(ctx, i);
            if (v != v) {
                continue;
            }
            int d = (int) (diameter(ctx, i, ms) * Math.min(1, ctx.getProgress() * 1.5f + (sizes == null ? 1 : 0)));
            int sx = (int) ctx.screenX(this, xs[i], v);
            int sy = (int) ctx.screenY(this, xs[i], v);
            g.fillArc(sx - d / 2, sy - d / 2, d, d, 0, 360);
        }
        g.setAlpha(alpha);
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        double ms = maxSize();
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            double v = getY(i);
            if (v != v) {
                continue;
            }
            int r = Math.max(radius, diameter(ctx, i, ms) / 2);
            double dx = ctx.screenX(this, xs[i], v) - ctx.getOriginX() - x;
            double dy = ctx.screenY(this, xs[i], v) - ctx.getOriginY() - y;
            double d = dx * dx + dy * dy;
            if (d <= (double) r * r && d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best < 0 ? null : getPoint(ctx, best);
    }

    @Override
    public int getPointCount() {
        // points are not index aligned with other series, so no shared tooltip row
        return -1;
    }
}
