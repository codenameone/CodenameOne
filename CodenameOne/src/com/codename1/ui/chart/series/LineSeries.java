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
import com.codename1.ui.Stroke;
import com.codename1.ui.chart.ChartContext;
import com.codename1.ui.chart.layout.LabelLayout;
import com.codename1.ui.geom.GeneralPath;

/// A line, spline, step or area series.
///
/// Splines are monotone cubic curves: they pass through every point and never overshoot
/// between two of them, so a smoothed line cannot invent a peak the data does not have.
/// Dense data is reduced to the minimum and maximum of each pixel column before drawing,
/// which keeps every spike while painting a constant number of segments.
public class LineSeries extends XYSeries {
    /// Straight segments between points.
    public static final int STRAIGHT = 0;
    /// A monotone cubic curve through the points.
    public static final int SPLINE = 1;
    /// A horizontal run to each point's x, then a vertical step.
    public static final int STEP = 2;

    private int mode = STRAIGHT;
    private boolean fill;
    private int fillAlpha = 64;
    private float lineWidthMm = 0.5f;
    private boolean markers;
    private boolean dashed;

    /// Creates an empty line series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the legend name
    public LineSeries(String name) {
        super(name);
    }

    /// `STRAIGHT`, `SPLINE` or `STEP`.
    public LineSeries setMode(int mode) {
        this.mode = mode;
        repaintChart();
        return this;
    }

    /// The drawing mode.
    public int getMode() {
        return mode;
    }

    /// Fills the area under the line, turning it into an area series; stacked areas need a
    /// shared `setStackGroup`.
    public LineSeries setFilled(boolean fill) {
        this.fill = fill;
        repaintChart();
        return this;
    }

    /// True for an area series.
    public boolean isFilled() {
        return fill;
    }

    /// Opacity of the area fill, 0 to 255.
    public LineSeries setFillAlpha(int alpha) {
        fillAlpha = Math.max(0, Math.min(255, alpha));
        repaintChart();
        return this;
    }

    /// Line thickness in millimetres.
    public LineSeries setLineWidth(float mm) {
        lineWidthMm = mm;
        repaintChart();
        return this;
    }

    /// Draws a dot at every point (skipped automatically when points are too dense to tell apart).
    public LineSeries setMarkers(boolean markers) {
        this.markers = markers;
        repaintChart();
        return this;
    }

    /// Draws a dashed line, for targets and forecasts.
    public LineSeries setDashed(boolean dashed) {
        this.dashed = dashed;
        repaintChart();
        return this;
    }

    @Override
    public void paint(ChartContext ctx) {
        int[] r = visibleRange(ctx);
        if (r[1] < r[0]) {
            return;
        }
        Graphics g = ctx.getGraphics();
        int alpha = g.getAlpha();
        float vis = getVisibilityFraction();
        int color = getColor();
        // reduce to screen points, splitting at gaps; buffers are kept between frames
        int n = r[1] - r[0] + 1;
        ensureBuffers(n);
        float[] px = bufX;
        float[] py = bufY;
        float[] pb = bufB;
        boolean[] gap = bufGap;
        int m = 0;
        boolean dense = n > ctx.getPlotWidth() * 2 && !ctx.isRotated();
        boolean needBase = getStackGroup() != null || fill;
        // dense data keeps, per pixel column, the first, lowest, highest and last point in
        // their own order (M4), which draws the same pixels as every point would
        float col = Float.NaN;
        int first = -1;
        int lo = -1;
        int hi = -1;
        int last = -1;
        float loY = 0;
        float hiY = 0;
        for (int i = r[0]; i <= r[1] + 1; i++) {
            boolean end = i > r[1];
            double v = end ? Double.NaN : animatedY(ctx, i);
            if (v != v) {
                if (dense && first >= 0) {
                    m = flushColumn(ctx, m, first, lo, hi, last, needBase);
                    first = -1;
                    col = Float.NaN;
                }
                if (m > 0) {
                    gap[m - 1] = true;
                }
                continue;
            }
            if (!dense) {
                m = emit(ctx, m, i, needBase);
                continue;
            }
            double b = animatedBase(ctx, i);
            float sx = ctx.screenX(this, xs[i], b + v);
            float sy = ctx.screenY(this, xs[i], b + v);
            float c = (float) Math.floor(sx);
            if (c != col) {
                if (first >= 0) {
                    m = flushColumn(ctx, m, first, lo, hi, last, needBase);
                }
                col = c;
                first = i;
                lo = i;
                hi = i;
                loY = sy;
                hiY = sy;
            } else if (sy < loY) {
                loY = sy;
                lo = i;
            } else if (sy > hiY) {
                hiY = sy;
                hi = i;
            }
            last = i;
        }
        if (m == 0) {
            return;
        }
        g.setAlpha((int) (alpha * vis));
        if (fill) {
            g.setColor(color);
            int fa = g.getAlpha();
            g.setAlpha(fa * fillAlpha / 255);
            int start = 0;
            for (int i = 0; i < m; i++) {
                if (gap[i] || i == m - 1) {
                    g.fillShape(areaPath(px, py, pb, start, i));
                    start = i + 1;
                }
            }
            g.setAlpha(fa);
        }
        float w = Math.max(1, ctx.mm(lineWidthMm));
        if (stroke == null || stroke.getLineWidth() != w) {
            stroke = new Stroke(w, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4);
        }
        g.setColor(color);
        int start = 0;
        for (int i = 0; i < m; i++) {
            if (gap[i] || i == m - 1) {
                if (i > start) {
                    GeneralPath p = path;
                    p.reset();
                    appendLine(p, px, py, start, i, true);
                    if (dashed) {
                        drawDashed(g, px, py, start, i, w, ctx.mm(2));
                    } else {
                        g.drawShape(p, stroke);
                    }
                }
                start = i + 1;
            }
        }
        if (markers && m <= ctx.getPlotWidth() / Math.max(1, ctx.mm(2.5f))) {
            int d = Math.max(4, ctx.mm(1.6f));
            for (int i = 0; i < m; i++) {
                g.setColor(ctx.getChart().getStyle().getBgColor());
                g.fillArc((int) px[i] - d / 2 - 1, (int) py[i] - d / 2 - 1, d + 2, d + 2, 0, 360);
                g.setColor(color);
                g.fillArc((int) px[i] - d / 2, (int) py[i] - d / 2, d, d, 0, 360);
            }
        }
        if (isShowValues()) {
            paintValueLabels(ctx, r);
        }
        g.setAlpha(alpha);
    }

    private float[] bufX = new float[0];
    private float[] bufY = new float[0];
    private float[] bufB = new float[0];
    private boolean[] bufGap = new boolean[0];
    private Stroke stroke;
    private final GeneralPath path = new GeneralPath();

    private void ensureBuffers(int n) {
        if (bufX.length < n) {
            bufX = new float[n];
            bufY = new float[n];
            bufB = new float[n];
            bufGap = new boolean[n];
        } else {
            for (int i = 0; i < n; i++) {
                bufGap[i] = false;
            }
        }
    }

    private int emit(ChartContext ctx, int m, int i, boolean needBase) {
        double v = animatedY(ctx, i);
        double b = animatedBase(ctx, i);
        bufX[m] = ctx.screenX(this, xs[i], b + v);
        bufY[m] = ctx.screenY(this, xs[i], b + v);
        bufB[m] = needBase ? baseline(ctx, i, b) : 0;
        return m + 1;
    }

    private int flushColumn(ChartContext ctx, int m, int first, int lo, int hi, int last, boolean needBase) {
        m = emit(ctx, m, first, needBase);
        int a = Math.min(lo, hi);
        int b = Math.max(lo, hi);
        if (a != first) {
            m = emit(ctx, m, a, needBase);
        }
        if (b != first && b != a) {
            m = emit(ctx, m, b, needBase);
        }
        if (last != first && last != a && last != b) {
            m = emit(ctx, m, last, needBase);
        }
        return m;
    }

    private float baseline(ChartContext ctx, int i, double base) {
        double b = getStackGroup() != null ? base : Math.max(ctx.getValueAxis(this).getVisibleMin(), Math.min(0, ctx.getValueAxis(this).getVisibleMax()));
        if (getStackGroup() == null && ctx.getValueAxis(this).getVisibleMin() > 0) {
            b = ctx.getValueAxis(this).getVisibleMin();
        }
        return ctx.isRotated() ? ctx.screenX(this, xs[i], b) : ctx.screenY(this, xs[i], b);
    }

    private GeneralPath areaPath(float[] px, float[] py, float[] pb, int from, int to) {
        GeneralPath p = new GeneralPath();
        appendLine(p, px, py, from, to, true);
        for (int i = to; i >= from; i--) {
            p.lineTo(px[i], pb[i]);
        }
        p.closePath();
        return p;
    }

    private void appendLine(GeneralPath p, float[] px, float[] py, int from, int to, boolean move) {
        if (move) {
            p.moveTo(px[from], py[from]);
        }
        if (mode == STEP) {
            for (int i = from + 1; i <= to; i++) {
                p.lineTo(px[i], py[i - 1]);
                p.lineTo(px[i], py[i]);
            }
        } else if (mode == SPLINE && to - from >= 2) {
            float[] t = monotoneTangents(px, py, from, to);
            for (int i = from; i < to; i++) {
                float h = (px[i + 1] - px[i]) / 3f;
                p.curveTo(px[i] + h, py[i] + t[i - from] * h, px[i + 1] - h, py[i + 1] - t[i - from + 1] * h,
                        px[i + 1], py[i + 1]);
            }
        } else {
            for (int i = from + 1; i <= to; i++) {
                p.lineTo(px[i], py[i]);
            }
        }
    }

    /// Fritsch-Carlson tangents: slopes are zeroed at local extrema and limited elsewhere so
    /// the curve stays monotone between neighbouring points.
    static float[] monotoneTangents(float[] x, float[] y, int from, int to) {
        int n = to - from + 1;
        float[] d = new float[n - 1];
        for (int i = 0; i < n - 1; i++) {
            float dx = x[from + i + 1] - x[from + i];
            d[i] = dx == 0 ? 0 : (y[from + i + 1] - y[from + i]) / dx;
        }
        float[] t = new float[n];
        t[0] = d[0];
        t[n - 1] = d[n - 2];
        for (int i = 1; i < n - 1; i++) {
            t[i] = d[i - 1] * d[i] <= 0 ? 0 : (d[i - 1] + d[i]) / 2;
        }
        for (int i = 0; i < n - 1; i++) {
            if (d[i] == 0) {
                t[i] = 0;
                t[i + 1] = 0;
                continue;
            }
            float a = t[i] / d[i];
            float b = t[i + 1] / d[i];
            float s = a * a + b * b;
            if (s > 9) {
                float k = (float) (3 / Math.sqrt(s));
                t[i] = k * a * d[i];
                t[i + 1] = k * b * d[i];
            }
        }
        return t;
    }

    private void drawDashed(Graphics g, float[] px, float[] py, int from, int to, float w, int dash) {
        Stroke s = new Stroke(w, Stroke.CAP_BUTT, Stroke.JOIN_ROUND, 4);
        boolean on = true;
        float left = dash;
        for (int i = from; i < to; i++) {
            float x0 = px[i];
            float y0 = py[i];
            float dx = px[i + 1] - x0;
            float dy = py[i + 1] - y0;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float pos = 0;
            while (pos < len) {
                float step = Math.min(left, len - pos);
                if (on) {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(x0 + dx * pos / len, y0 + dy * pos / len);
                    p.lineTo(x0 + dx * (pos + step) / len, y0 + dy * (pos + step) / len);
                    g.drawShape(p, s);
                }
                pos += step;
                left -= step;
                if (left <= 0) {
                    on = !on;
                    left = dash;
                }
            }
        }
    }

    private void paintValueLabels(ChartContext ctx, int[] r) {
        Graphics g = ctx.getGraphics();
        g.setFont(ctx.getFont());
        int h = ctx.getFont().getHeight();
        int[] out = new int[2];
        LabelLayout ll = ctx.getLabels();
        for (int i = r[0]; i <= r[1]; i++) {
            double v = ys[i];
            if (v != v) {
                continue;
            }
            double top = getBase(i) + animatedY(ctx, i);
            String s = formatValue(ctx, v);
            int w = ctx.getFont().stringWidth(s);
            int ax = (int) ctx.screenX(this, xs[i], top);
            int ay = (int) ctx.screenY(this, xs[i], top);
            if (ll.place(ax, ay, w, h, ctx.mm(1), LabelLayout.ABOVE | LabelLayout.BELOW | LabelLayout.RIGHT, out)) {
                g.setColor(ctx.getTheme().getTextColor());
                g.drawString(s, out[0], out[1]);
            }
        }
    }
}
