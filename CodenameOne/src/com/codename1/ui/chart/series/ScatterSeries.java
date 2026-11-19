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
import java.util.Arrays;

/// Unconnected points. Giving the points sizes turns it into a bubble chart: the bubble's
/// *area*, not its radius, is proportional to the size value, so big values do not look
/// quadratically bigger than they are.
public class ScatterSeries extends XYSeries {
    // the bubble size is aux channel 0, so slide(), removeRow(), setMaxPoints() and the
    // transitions keep it aligned with its point; NaN is "no size", drawn as a plain dot
    private boolean bubbles;
    private float pointMm = 1.6f;
    private float maxBubbleMm = 9f;
    private int fillAlpha = 200;

    /// Creates an empty scatter series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the legend name
    public ScatterSeries(String name) {
        super(name, 1);
    }

    /// Sets a size per point of the points the series holds now, making this a bubble series;
    /// null for plain points. A point past the end of `sizes`, or with a NaN size, is drawn as a
    /// plain dot. Sizes move with their points as points are added, removed or slide out of a
    /// `setMaxPoints` window; use `setData(double[], double[], double[])` or
    /// `add(double, double, double)` to give new points theirs.
    public ScatterSeries setSizes(double... sizes) {
        for (int i = 0; i < count; i++) {
            aux[0][i] = sizes != null && i < sizes.length ? sizes[i] : Double.NaN;
            markExplicit(i);
        }
        setBubbles(sizes != null);
        repaintChart();
        return this;
    }

    /// Replaces all points with bubbles of the given sizes.
    ///
    /// #### Parameters
    ///
    /// - `x`: x values, or null for 0, 1, 2...
    ///
    /// - `y`: the values
    ///
    /// - `sizes`: one size per point, NaN for a plain dot; null for plain points, as
    ///   `setSizes(null)` does
    public ScatterSeries setData(double[] x, double[] y, double[] sizes) {
        double[] channel = sizes;
        if (channel == null) {
            channel = new double[y.length];
            for (int i = 0; i < channel.length; i++) {
                channel[i] = Double.NaN;
            }
        }
        setRows(x, y, new double[][]{channel});
        setBubbles(sizes != null);
        return this;
    }

    /// Appends a bubble, sliding the window when a maximum is set.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x value
    ///
    /// - `y`: the value
    ///
    /// - `size`: the bubble size, NaN for a plain dot
    public ScatterSeries add(double x, double y, double size) {
        addRow(x, y, new double[]{size});
        setBubbles(true);
        return this;
    }

    /// The size of point `i`, NaN when it has none.
    public double getSize(int i) {
        return aux[0][i];
    }

    private void setBubbles(boolean b) {
        bubbles = b;
        fillAlpha = b ? 150 : 200;
    }

    @Override
    protected boolean isAuxRelative(int channel) {
        return false;
    }

    @Override
    protected double auxDefault(int channel, double y) {
        return Double.NaN;
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
        return diameterOf(ctx, aux[0][i], maxSize);
    }

    private int diameterOf(ChartContext ctx, double s, double maxSize) {
        if (!bubbles || !(maxSize > 0) || isGap(s)) { //NOPMD LogicInversion - also true for a NaN max
            return Math.max(3, ctx.mm(pointMm));
        }
        // clamped: a size mid-transition can exceed the settled maximum
        double f = Math.min(1, Math.sqrt(Math.max(0, s) / maxSize));
        return Math.max(3, (int) (ctx.mm(maxBubbleMm) * f));
    }

    private double maxSize() {
        double m = 0;
        if (bubbles) {
            double[] sz = aux[0];
            for (int i = 0; i < count; i++) {
                // a NaN or infinite size is a plain dot; Math.max would carry a NaN into every
                // bubble, and an infinite maximum would shrink every other bubble to a dot
                if (!isGap(sz[i]) && sz[i] > m) {
                    m = sz[i];
                }
            }
        }
        return m;
    }

    // one bit per plot pixel, reused across frames, for the dense-scatter dedupe in paint
    private int[] drawnCells;

    @Override
    public void paint(ChartContext ctx) {
        Graphics g = ctx.getGraphics();
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction() * fillAlpha / 255));
        g.setColor(getColor());
        double ms = maxSize();
        double xmin = ctx.getXAxis().getVisibleMin();
        double xmax = ctx.getXAxis().getVisibleMax();
        // a dense plain scatter draws many markers onto the same pixel; past twice the plot
        // width, a marker whose center pixel already holds one of the same size is skipped.
        // Bubbles differ in size per point, so they are always drawn.
        int pw = ctx.getPlotWidth();
        int ph = ctx.getPlotHeight();
        boolean dedupe = !bubbles && count > 2 * pw && pw > 0 && ph > 0;
        if (dedupe) {
            int words = (pw * ph + 31) / 32;
            if (drawnCells == null || drawnCells.length < words) {
                drawnCells = new int[words];
            } else {
                Arrays.fill(drawnCells, 0, words, 0);
            }
        }
        int px0 = ctx.getPlotX();
        int py0 = ctx.getPlotY();
        for (int i = 0; i < count; i++) {
            if (isGap(xs[i]) || xs[i] < xmin || xs[i] > xmax) {
                continue;
            }
            double v = animatedY(ctx, i);
            if (isGap(v)) {
                continue;
            }
            // the size channel animates per point like the value: a bubble grows from nothing only
            // on the series' entry, and a change elsewhere leaves every other bubble's size alone
            int d = diameterOf(ctx, bubbles ? animatedAux(ctx, 0, i) : aux[0][i], ms);
            int sx = (int) ctx.screenX(this, xs[i], v);
            int sy = (int) ctx.screenY(this, xs[i], v);
            if (dedupe) {
                int cx = sx - px0;
                int cy = sy - py0;
                // a point on the plot's far edge lands one past it; it shares the edge cell
                if (cx == pw) {
                    cx--;
                }
                if (cy == ph) {
                    cy--;
                }
                if (cx >= 0 && cx < pw && cy >= 0 && cy < ph) {
                    int cell = cy * pw + cx;
                    int bit = 1 << (cell & 31);
                    if ((drawnCells[cell >> 5] & bit) != 0) {
                        continue;
                    }
                    drawnCells[cell >> 5] |= bit;
                }
            }
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
            if (isGap(v) || isGap(xs[i])) {
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
        // points are not index aligned with other series; the shared tooltip gives them rows
        // only on a chart of nothing but scatter
        return -1;
    }
}
