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
import com.codename1.ui.chart.Series;
import com.codename1.ui.chart.layout.LabelLayout;

import java.util.List;

/// Vertical columns, or horizontal bars when the chart is rotated. Several bar series side
/// by side form groups; bar series sharing a stack group stack instead.
public class BarSeries extends XYSeries {
    private float barFraction = 0.75f;
    private float cornerMm = 0.6f;

    /// Creates an empty bar series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the legend name
    public BarSeries(String name) {
        super(name);
    }

    /// Creates a bar series that keeps extra values per bar, for range and waterfall bars.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    ///
    /// - `auxChannels`: number of extra values per bar
    protected BarSeries(String name, int auxChannels) {
        super(name, auxChannels);
    }

    /// The share of each category band the bar group fills, 0.1 to 1.
    public BarSeries setBarFraction(float f) {
        barFraction = Math.max(0.1f, Math.min(1, f));
        repaintChart();
        return this;
    }

    /// The radius of the bar's outer corners in millimetres; 0 for square bars.
    public BarSeries setCornerRadius(float mm) {
        cornerMm = mm;
        repaintChart();
        return this;
    }

    @Override
    public double[] getYBounds() {
        double[] b = super.getYBounds();
        if (b == null) {
            return null;
        }
        // bars grow from zero, so zero must be visible
        return new double[]{Math.min(0, b[0]), Math.max(0, b[1])};
    }

    @Override
    public double[] getXBounds() {
        double[] b = super.getXBounds();
        if (b == null) {
            return null;
        }
        double half = getMinXSpacing() / 2;
        return new double[]{b[0] - half, b[1] + half};
    }

    /// `{start, width, total}`: this series' place among the side by side bar groups, in
    /// slot weights. Every series weighs its visibility fraction (a stack group the largest of its
    /// members'), so a series fading in or out narrows its slot smoothly, and every series of the
    /// chart computes the same layout during the fade.
    private float[] slot() {
        List<Series> all = getChart().getSeries();
        java.util.ArrayList<Object> keys = new java.util.ArrayList<Object>();
        float[] w = new float[all.size()];
        int mine = -1;
        for (Series s : all) {
            if (!(s instanceof BarSeries)) {
                continue;
            }
            BarSeries bs = (BarSeries) s;
            float vis = bs.getVisibilityFraction();
            if (vis <= 0 && s != this) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                continue;
            }
            String g = bs.getStackGroup();
            Object key = g == null ? s : g;
            int idx = keys.indexOf(key);
            if (idx < 0) {
                idx = keys.size();
                keys.add(key);
            }
            w[idx] = Math.max(w[idx], Math.max(0, vis));
            if (s == this) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                mine = idx;
            }
        }
        float total = 0;
        float start = 0;
        for (int k = 0; k < keys.size(); k++) {
            if (k == mine) {
                start = total;
            }
            total += w[k];
        }
        if (mine < 0 || !(total > 0)) { //NOPMD LogicInversion - also rejects NaN
            return new float[]{0, 1, 1};
        }
        return new float[]{start, w[mine], total};
    }

    private float[] barRect(ChartContext ctx, int i, double base, double v, float[] slot) {
        // one slot is the closest spacing of the data: a category band, a day on a time axis
        float band = ctx.xLength(getMinXSpacing()) * barFraction;
        float each = band * slot[1] / slot[2];
        float offset = -band / 2 + band * slot[0] / slot[2];
        float inset = slot[1] < slot[2] ? Math.max(0, Math.min(each * 0.08f, ctx.mm(0.4f))) : 0;
        if (ctx.isRotated()) {
            float cy = ctx.screenY(this, xs[i], 0);
            float x0 = ctx.screenX(this, xs[i], base);
            float x1 = ctx.screenX(this, xs[i], base + v);
            return new float[]{Math.min(x0, x1), cy + offset + inset, Math.abs(x1 - x0), each - 2 * inset};
        }
        float cx = ctx.screenX(this, xs[i], 0);
        float y0 = ctx.screenY(this, xs[i], base);
        float y1 = ctx.screenY(this, xs[i], base + v);
        return new float[]{cx + offset + inset, Math.min(y0, y1), each - 2 * inset, Math.abs(y1 - y0)};
    }

    @Override
    public void paint(ChartContext ctx) {
        int[] r = visibleRange(ctx);
        Graphics g = ctx.getGraphics();
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction()));
        float[] slot = slot();
        int radius = ctx.mm(cornerMm);
        boolean top = getStackGroup() == null || isTopOfStack();
        for (int i = r[0]; i <= r[1]; i++) {
            double v = animatedY(ctx, i);
            if (v != v) {
                continue;
            }
            float[] b = barRect(ctx, i, animatedBase(ctx, i), v, slot);
            int x = Math.round(b[0]);
            int y = Math.round(b[1]);
            int w = Math.max(1, Math.round(b[2]));
            int h = Math.round(b[3]);
            if (h <= 0 && !ctx.isRotated() || w <= 0) {
                continue;
            }
            g.setColor(colorAt(ctx, i));
            int rr = top ? Math.min(radius, Math.min(w, Math.max(h, 1)) / 2) : 0;
            if (rr > 1) {
                // round only the outer end: paint a rounded rect and square off the base half
                g.fillRoundRect(x, y, w, Math.max(h, 1), rr * 2, rr * 2);
                if (ctx.isRotated()) {
                    boolean neg = v < 0;
                    g.fillRect(neg ? x + w - rr : x, y, rr, Math.max(h, 1));
                } else {
                    boolean neg = v < 0;
                    g.fillRect(x, neg ? y : y + h - rr, w, rr);
                }
            } else {
                g.fillRect(x, y, w, Math.max(h, 1));
            }
        }
        if (isShowValues()) {
            paintLabels(ctx, r, slot);
        }
        g.setAlpha(alpha);
    }

    private boolean isTopOfStack() {
        List<Series> all = getChart().getSeries();
        boolean after = false;
        for (Series s : all) {
            if (s == this) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
                after = true;
                continue;
            }
            if (after && s instanceof BarSeries && s.isVisible() && getStackGroup().equals(((BarSeries) s).getStackGroup())) {
                return false;
            }
        }
        return true;
    }

    private void paintLabels(ChartContext ctx, int[] r, float[] slot) {
        Graphics g = ctx.getGraphics();
        g.setFont(ctx.getFont());
        int fh = ctx.getFont().getHeight();
        LabelLayout ll = ctx.getLabels();
        int pad = ctx.mm(0.8f);
        for (int i = r[0]; i <= r[1]; i++) {
            double v = getY(i);
            if (v != v) {
                continue;
            }
            float[] b = barRect(ctx, i, animatedBase(ctx, i), animatedY(ctx, i), slot);
            String s = formatValue(ctx, v);
            int w = ctx.getFont().stringWidth(s);
            int x;
            int y;
            boolean inside;
            if (ctx.isRotated()) {
                inside = getStackGroup() != null;
                x = inside ? (int) (b[0] + (b[2] - w) / 2) : (v < 0 ? (int) b[0] - pad - w : (int) (b[0] + b[2]) + pad);
                y = (int) (b[1] + (b[3] - fh) / 2);
            } else {
                inside = getStackGroup() != null;
                x = (int) (b[0] + (b[2] - w) / 2);
                y = inside ? (int) (b[1] + (b[3] - fh) / 2) : (v < 0 ? (int) (b[1] + b[3]) + pad : (int) b[1] - pad - fh);
            }
            if (inside && (ctx.isRotated() ? b[2] < w + pad : b[3] < fh + pad)) {
                continue;
            }
            if (ll.placeAt(x, y, w, fh)) {
                g.setColor(inside ? contrast(colorAt(ctx, i)) : ctx.getTheme().getTextColor());
                g.drawString(s, x, y);
            }
        }
    }

    /// The fill colour of bar `i`; the series colour unless a subclass colours bars by value.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: the paint context
    ///
    /// - `i`: the bar index
    ///
    /// #### Returns
    ///
    /// an RGB colour
    protected int colorAt(ChartContext ctx, int i) {
        return getColor();
    }

    static int contrast(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int gr = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return r * 299 + gr * 587 + b * 114 > 150000 ? 0x111111 : 0xffffff;
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        int[] r = visibleRange(ctx);
        float[] slot = slot();
        int ax = x + ctx.getOriginX();
        int ay = y + ctx.getOriginY();
        for (int i = r[0]; i <= r[1]; i++) {
            double v = getY(i);
            if (v != v) {
                continue;
            }
            float[] b = barRect(ctx, i, getBase(i), v, slot);
            float minExtent = radius / 2f;
            float bx = b[0];
            float by = b[1];
            float bw = b[2];
            float bh = b[3];
            if (ctx.isRotated() && bw < minExtent) {
                bw = minExtent;
            } else if (!ctx.isRotated() && bh < minExtent) {
                by -= minExtent - bh;
                bh = minExtent;
            }
            if (ax >= bx && ax <= bx + bw && ay >= by && ay <= by + bh) {
                return getPoint(ctx, i);
            }
        }
        return null;
    }
}
