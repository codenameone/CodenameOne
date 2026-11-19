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

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.chart.ChartContext;
import com.codename1.ui.chart.ChartPoint;
import com.codename1.ui.chart.Series;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.axis.Ticks;
import com.codename1.ui.chart.layout.LabelLayout;
import com.codename1.ui.chart.layout.Shapes;
import com.codename1.ui.geom.GeneralPath;

import java.util.List;

/// A radar (spider) chart: one polygon per series over a shared set of spokes. All radar series
/// in a chart share the spokes of the first one and one radial scale.
///
/// ```java
/// String[] skills = {"Speed", "Power", "Range", "Defense", "Stamina"};
/// chart.addSeries(new RadarSeries("Alice", skills).setValues(8, 6, 7, 5, 9));
/// chart.addSeries(new RadarSeries("Bob", skills).setValues(5, 9, 4, 8, 6));
/// ```
public class RadarSeries extends Series {
    private String[] spokes;
    private double[] values = new double[0];
    private double[] from;
    private float fillAlpha = 0.2f;
    private float lastCx;
    private float lastCy;
    private float lastR;
    private double lastMax = 1;

    /// Creates a radar series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    ///
    /// - `spokes`: the name of each spoke; may be null on all but the first radar series
    public RadarSeries(String name, String... spokes) {
        super(name);
        this.spokes = spokes;
    }

    @Override
    public boolean isCartesian() {
        return false;
    }

    /// Replaces the values, one per spoke, animated.
    ///
    /// #### Parameters
    ///
    /// - `v`: the values
    ///
    /// #### Returns
    ///
    /// this series
    public RadarSeries setValues(double... v) {
        if (getChart() != null) {
            getChart().beginChange();
        }
        values = new double[v.length];
        System.arraycopy(v, 0, values, 0, v.length);
        dataChanged();
        return this;
    }

    /// The value on spoke `i`.
    public double getValue(int i) {
        return values[i];
    }

    /// The opacity of the polygon's fill.
    ///
    /// #### Parameters
    ///
    /// - `a`: 0 to 1
    ///
    /// #### Returns
    ///
    /// this series
    public RadarSeries setFillAlpha(float a) {
        fillAlpha = a;
        repaintChart();
        return this;
    }

    private String[] spokes() {
        if (spokes != null && spokes.length > 0) {
            return spokes;
        }
        for (Series s : radars()) {
            RadarSeries r = (RadarSeries) s;
            if (r.spokes != null && r.spokes.length > 0) {
                return r.spokes;
            }
        }
        return new String[values.length];
    }

    private java.util.ArrayList<Series> radars() {
        java.util.ArrayList<Series> out = new java.util.ArrayList<Series>();
        Chart c = getChart();
        if (c == null) {
            out.add(this);
            return out;
        }
        List<Series> all = c.getSeries();
        for (Series s : all) {
            if (s instanceof RadarSeries) {
                out.add(s);
            }
        }
        return out;
    }

    private double shown(ChartContext ctx, int i) {
        double t = ctx.getProgress();
        double target = values[i];
        if (t >= 1 || from == null || i >= from.length) {
            return target;
        }
        return lerp(from[i], target, t);
    }

    @Override
    protected void captureTransitionStart() {
        ChartContext ctx = getChart() == null ? null : getChart().getLastContext();
        from = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            from[i] = ctx == null ? 0 : shown(ctx, i);
        }
    }

    @Override
    protected void captureEntryStart() {
        from = new double[values.length];
    }

    @Override
    public void paint(ChartContext ctx) {
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        int fh = f.getHeight();
        String[] sp = spokes();
        int n = Math.max(sp.length, values.length);
        if (n < 3) {
            return;
        }
        java.util.ArrayList<Series> rs = radars();
        double max = 0;
        int maxLabel = 0;
        for (int k = 0; k < rs.size(); k++) {
            RadarSeries r = (RadarSeries) rs.get(k);
            // a series still fading out is painted against this scale, so it counts too
            if (!(r.getVisibilityFraction() > 0)) { //NOPMD LogicInversion - also rejects NaN
                continue;
            }
            for (int i = 0; i < r.values.length; i++) {
                // the scale is the target's: rings snap to it while the polygons tween, so a grow-in
                // or a uniform change is not cancelled by a scale that shrinks along with the values
                double rv = pos(r.values[i]);
                if (rv > 0) {
                    max = Math.max(max, rv);
                }
            }
        }
        for (String label : sp) {
            if (label != null) {
                maxLabel = Math.max(maxLabel, f.stringWidth(label));
            }
        }
        maxLabel = Math.min(maxLabel, ctx.getPlotWidth() / 4);
        double[] ticks = Ticks.linear(0, max <= 0 ? 1 : max, 5);
        double step = ticks.length > 1 ? ticks[1] - ticks[0] : max;
        if (!(step > 0) || step == Double.POSITIVE_INFINITY) { //NOPMD LogicInversion - also catches NaN
            // the ring loops below step by this; 0 or NaN would never end them
            step = 1;
        }
        double top = ticks.length > 0 ? ticks[ticks.length - 1] : 1;
        if (top < max) {
            top += step;
        }
        if (!(top > 0) || top == Double.POSITIVE_INFINITY) { //NOPMD LogicInversion - also catches NaN
            top = 1;
        }
        // While values shrink the polygons are still painted at the old, larger values; the scale
        // must hold them or a polygon is drawn far outside the rings. Growth keeps the target's
        // scale (shown values are below it), so a grow-in is not cancelled.
        double shownMax = 0;
        for (int k = 0; k < rs.size(); k++) {
            RadarSeries rr = (RadarSeries) rs.get(k);
            if (!(rr.getVisibilityFraction() > 0)) { //NOPMD LogicInversion - also rejects NaN
                continue;
            }
            for (int i = 0; i < rr.values.length; i++) {
                double sv = pos(rr.shown(ctx, i));
                if (sv > shownMax && sv != Double.POSITIVE_INFINITY) {
                    shownMax = sv;
                }
            }
        }
        top = Math.max(top, shownMax);
        // the ring loops below stop at RINGS whatever the arithmetic says, so no scale can hang the EDT
        if (top / step > MAX_RINGS) {
            step = top / MAX_RINGS;
        }
        float cx = ctx.getPlotX() + ctx.getPlotWidth() / 2f;
        float cy = ctx.getPlotY() + ctx.getPlotHeight() / 2f;
        float r = Math.min(ctx.getPlotWidth() / 2f - maxLabel - ctx.mm(2), ctx.getPlotHeight() / 2f - fh - ctx.mm(1));
        r = Math.max(8, r);
        lastCx = cx;
        lastCy = cy;
        lastR = r;
        lastMax = top;
        // The grid belongs to the first radar that is painted at all: a hidden
        // first radar is never painted, so keying on rs.get(0) lost the grid.
        RadarSeries gridOwner = null;
        for (int k = 0; k < rs.size(); k++) {
            RadarSeries candidate = (RadarSeries) rs.get(k);
            if (candidate.getVisibilityFraction() > 0) {
                gridOwner = candidate;
                break;
            }
        }
        boolean first = gridOwner == this; //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
        if (first) {
            g.setColor(ctx.getTheme().getGridColor());
            for (int rk = 1; rk <= MAX_RINGS && rk * step <= top + step * 1e-6; rk++) {
                double tk = rk * step;
                float rr = (float) (r * tk / top);
                GeneralPath ring = new GeneralPath();
                for (int i = 0; i <= n; i++) {
                    double a = 360.0 * (i % n) / n;
                    if (i == 0) {
                        ring.moveTo(Shapes.px(cx, rr, a), Shapes.py(cy, rr, a));
                    } else {
                        ring.lineTo(Shapes.px(cx, rr, a), Shapes.py(cy, rr, a));
                    }
                }
                g.drawShape(ring, new Stroke(1, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1));
            }
            for (int i = 0; i < n; i++) {
                double a = 360.0 * i / n;
                g.drawLine(Math.round(cx), Math.round(cy), Math.round(Shapes.px(cx, r, a)), Math.round(Shapes.py(cy, r, a)));
            }
            g.setFont(f);
            for (int i = 0; i < n && i < sp.length; i++) {
                if (sp[i] == null) {
                    continue;
                }
                double a = 360.0 * i / n;
                String t = LabelLayout.ellipsize(f, sp[i], maxLabel);
                int w = f.stringWidth(t);
                float lx = Shapes.px(cx, r + ctx.mm(1.5f), a);
                float ly = Shapes.py(cy, r + ctx.mm(1.5f), a);
                double s = Math.sin(Math.toRadians(a));
                double c = Math.cos(Math.toRadians(a));
                int x = Math.abs(s) < 0.2 ? Math.round(lx - w / 2f) : (s > 0 ? Math.round(lx) : Math.round(lx - w));
                int y = Math.abs(c) < 0.2 ? Math.round(ly - fh / 2f) : (c > 0 ? Math.round(ly - fh) : Math.round(ly));
                g.setColor(ctx.getTheme().getSecondaryTextColor());
                ctx.getLabels().reserve(x, y, w, fh);
                g.drawString(t, x, y);
            }
            g.setColor(ctx.getTheme().getSecondaryTextColor());
            for (int rk = 1; rk <= MAX_RINGS && rk * step <= top + step * 1e-6; rk++) {
                double tk = rk * step;
                String t = Axis.formatNumber(tk, Ticks.decimalsFor(step));
                float rr = (float) (r * tk / top);
                int x = Math.round(cx + ctx.mm(0.5f));
                int y = Math.round(cy - rr - fh);
                if (ctx.getLabels().placeAt(x, y, f.stringWidth(t), fh)) {
                    g.drawString(t, x, y);
                }
            }
        }
        GeneralPath poly = new GeneralPath();
        for (int i = 0; i < n; i++) {
            double v = i < values.length ? shown(ctx, i) : 0;
            float rr = (float) (r * pos(v) / top);
            double a = 360.0 * i / n;
            if (i == 0) {
                poly.moveTo(Shapes.px(cx, rr, a), Shapes.py(cy, rr, a));
            } else {
                poly.lineTo(Shapes.px(cx, rr, a), Shapes.py(cy, rr, a));
            }
        }
        poly.closePath();
        int alpha = g.getAlpha();
        float vis = getVisibilityFraction();
        g.setColor(getColor());
        g.setAlpha((int) (alpha * vis * fillAlpha));
        g.fillShape(poly);
        g.setAlpha((int) (alpha * vis));
        g.drawShape(poly, new Stroke(Math.max(1, ctx.mm(0.5f)), Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 1));
        g.setAlpha(alpha);
    }

    @Override
    public int getPointCount() {
        return values.length;
    }

    @Override
    public ChartPoint getPoint(ChartContext ctx, int i) {
        if (i < 0 || i >= values.length) {
            return null;
        }
        int n = Math.max(spokes().length, values.length);
        double a = 360.0 * i / n;
        // the vertex as painted this frame, so a hover mid transition finds what is on screen
        float rr = (float) (lastR * pos(shown(ctx, i)) / lastMax);
        String[] sp = spokes();
        return new ChartPoint(this, i, i, values[i], Math.round(Shapes.px(lastCx, rr, a)) - ctx.getOriginX(),
                Math.round(Shapes.py(lastCy, rr, a)) - ctx.getOriginY(), i < sp.length ? sp[i] : null);
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        ChartPoint best = null;
        long bestD = (long) radius * radius;
        for (int i = 0; i < values.length; i++) {
            ChartPoint p = getPoint(ctx, i);
            long dx = p.getScreenX() - x;
            long dy = p.getScreenY() - y;
            if (dx * dx + dy * dy <= bestD) {
                bestD = dx * dx + dy * dy;
                best = p;
            }
        }
        return best;
    }

    @Override
    public String formatTooltip(ChartContext ctx, ChartPoint p) {
        String spoke = p.getLabel() == null ? "" : " " + p.getLabel();
        return getName() + spoke + ": " + formatValue(ctx, p.getY());
    }

    @Override
    public void collectTableRows(List<String[]> rows) {
        String[] sp = spokes();
        for (int i = 0; i < values.length; i++) {
            rows.add(new String[]{getName(), i < sp.length && sp[i] != null ? sp[i] : String.valueOf(i + 1), formatValue(null, values[i])});
        }
    }

    private static final int MAX_RINGS = 50;

    // NaN, infinite and negative values count as nothing; Math.max(0, NaN) is NaN and would poison
    // the sums, and an infinite value would make the scale (and the ring loops' step) infinite
    private static double pos(double v) {
        return !(v > 0) || v == Double.POSITIVE_INFINITY ? 0 : v; //NOPMD LogicInversion - the negated comparison is also true for NaN
    }
}
