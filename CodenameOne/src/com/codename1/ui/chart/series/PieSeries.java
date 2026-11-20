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
import com.codename1.ui.chart.ChartContext;
import com.codename1.ui.chart.ChartPoint;
import com.codename1.ui.chart.axis.Axis;
import com.codename1.ui.chart.layout.LabelLayout;
import com.codename1.ui.chart.layout.Shapes;

/// A pie or doughnut chart.
///
/// Labels sit outside the pie on leader lines and are spread vertically so that no two overlap;
/// when the chart is too small for them, or a slice is too thin to point at, the label is left
/// out and the legend and tooltip carry the name instead.
///
/// ```java
/// Chart chart = new Chart();
/// PieSeries pie = new PieSeries("Share");
/// pie.setData(new String[]{"Android", "iOS", "Other"}, new double[]{71, 28, 1});
/// pie.setInnerRadius(0.6f).setCenterText("2.1B");
/// chart.addSeries(pie);
/// ```
public class PieSeries extends CategoricalSeries {
    /// Reused by every slice of every frame, so painting allocates no path.
    private final com.codename1.ui.geom.GeneralPath sectorPath = new com.codename1.ui.geom.GeneralPath();

    /// Names and percentages outside the pie on leader lines.
    public static final int LABELS_OUTSIDE = 0;
    /// Percentages inside slices large enough to hold them.
    public static final int LABELS_INSIDE = 1;
    /// No labels; the legend and tooltip name the slices.
    public static final int LABELS_NONE = 2;

    private float innerFraction;
    private double startAngle;
    private int labelMode = LABELS_OUTSIDE;
    private String centerText;
    private float padAngle = 0.6f;

    private float lastCx;
    private float lastCy;
    private float lastR;
    private double[] lastStart = new double[0];
    private double[] lastSweep = new double[0];
    private int lastCount;
    // the outside labels' work arrays, reused by every paint
    private int[] labelIdx = new int[0];
    private float[] labelWant = new float[0];
    private boolean[] labelDrop = new boolean[0];
    private float[] labelY = new float[0];

    /// Creates an empty pie.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    public PieSeries(String name) {
        super(name);
    }

    /// Makes it a doughnut.
    ///
    /// #### Parameters
    ///
    /// - `fraction`: the hole's radius as a fraction of the outer radius, 0 for a full pie
    ///
    /// #### Returns
    ///
    /// this series
    public PieSeries setInnerRadius(float fraction) {
        innerFraction = Math.max(0, Math.min(0.95f, fraction));
        repaintChart();
        return this;
    }

    /// Where the first slice starts.
    ///
    /// #### Parameters
    ///
    /// - `deg`: degrees clockwise from twelve o'clock
    ///
    /// #### Returns
    ///
    /// this series
    public PieSeries setStartAngle(double deg) {
        startAngle = deg;
        repaintChart();
        return this;
    }

    /// Chooses where labels go.
    ///
    /// #### Parameters
    ///
    /// - `mode`: `LABELS_OUTSIDE`, `LABELS_INSIDE` or `LABELS_NONE`
    ///
    /// #### Returns
    ///
    /// this series
    public PieSeries setLabelMode(int mode) {
        labelMode = mode;
        repaintChart();
        return this;
    }

    /// Text shown in the hole of a doughnut, usually the total.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text, or null
    ///
    /// #### Returns
    ///
    /// this series
    public PieSeries setCenterText(String text) {
        centerText = text;
        repaintChart();
        return this;
    }

    /// The gap between slices.
    ///
    /// #### Parameters
    ///
    /// - `deg`: the gap in degrees, 0 for none
    ///
    /// #### Returns
    ///
    /// this series
    public PieSeries setPadAngle(float deg) {
        padAngle = Math.max(0, deg);
        repaintChart();
        return this;
    }

    // Percentage of slice i, taken like the sweep: of values scaled by the largest finite one, so
    // slices near Double.MAX_VALUE neither overflow the total nor answer NaN. Negative when the
    // slice has no share to print.
    private double percentOf(int i) {
        return percentOf(i, pctMaxPart, pctScaledTotal);
    }

    // the base of every label's percentage, taken once per paint so each label is a division
    // rather than two passes over every slice
    private double pctMaxPart;
    private double pctScaledTotal;
    // how many times the slices were scanned for that base; read by tests to hold it to one per paint
    int percentBaseScans;

    // the smallest sweep, in degrees, that gets an outside label
    private static final double OUTSIDE_LABEL_SWEEP = 0.5;
    // outside labels formatted and measured for the pie's radius; read by tests
    int labelMeasures;

    private void computePercentBase() {
        pctMaxPart = largestShown();
        pctScaledTotal = scaledShownTotal(pctMaxPart);
        percentBaseScans++;
    }

    private String labelText(int i) {
        double p = percentOf(i);
        String pct = p >= 0 ? Axis.formatNumber(p, p < 10 ? 1 : 0) + "%" : "";
        if (labelMode == LABELS_INSIDE) {
            return pct;
        }
        return labels[i] + " " + pct;
    }

    @Override
    public void paint(ChartContext ctx) {
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        int fh = f.getHeight();
        int gap = ctx.mm(1.5f);
        int n = size();
        computePercentBase();
        float cx = ctx.getPlotX() + ctx.getPlotWidth() / 2f;
        float cy = ctx.getPlotY() + ctx.getPlotHeight() / 2f;
        int minDim = Math.min(ctx.getPlotWidth(), ctx.getPlotHeight());
        float r = minDim / 2f - ctx.mm(1);
        // Shares are taken of values scaled by the largest one, so two slices near
        // Double.MAX_VALUE neither overflow the total to infinity nor give an infinite sweep.
        double maxPart = 0;
        for (int i = 0; i < n; i++) {
            double v = displayValue(ctx, i);
            if (v > maxPart && v < Double.POSITIVE_INFINITY) {
                maxPart = v;
            }
        }
        double total = 0;
        if (maxPart > 0) {
            for (int i = 0; i < n; i++) {
                double v = displayValue(ctx, i);
                if (v > 0 && v < Double.POSITIVE_INFINITY) {
                    total += v / maxPart;
                }
            }
        }
        boolean outside = labelMode == LABELS_OUTSIDE;
        int maxLabel = 0;
        if (outside) {
            for (int i = 0; i < n; i++) {
                // only a slice paintOutsideLabels can label is worth formatting and measuring
                double v = displayValue(ctx, i);
                if (total > 0 && v > 0 && v < Double.POSITIVE_INFINITY && 360 * (v / maxPart / total) > OUTSIDE_LABEL_SWEEP) {
                    labelMeasures++;
                    maxLabel = Math.max(maxLabel, f.stringWidth(labelText(i)));
                }
            }
            maxLabel = Math.min(maxLabel, (int) (ctx.getPlotWidth() * 0.3f));
            float rr = Math.min(ctx.getPlotHeight() / 2f - fh, ctx.getPlotWidth() / 2f - maxLabel - gap * 3);
            if (rr < minDim * 0.22f) {
                // too cramped for outside labels: give the pie the room and rely on legend/tooltip
                outside = false;
            } else {
                r = rr;
            }
        }
        r = Math.max(4, r);
        double sweepScale = isEntering(ctx) ? ctx.getProgress() : 1;
        ChartPoint sel = getChart() == null ? null : getChart().getSelection();
        int selIndex = sel != null && sel.getSeries() == this ? sel.getIndex() : -1; //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
        lastCx = cx;
        lastCy = cy;
        lastR = r;
        // grown only when the slice count grows; lastCount bounds what the hit tests read
        if (lastStart.length < n) {
            lastStart = new double[n];
            lastSweep = new double[n];
        }
        lastCount = n;
        double a = startAngle;
        float inner = r * innerFraction;
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction()));
        for (int i = 0; i < n; i++) {
            double v = displayValue(ctx, i);
            double sweep = total > 0 && v > 0 && v < Double.POSITIVE_INFINITY ? 360 * (v / maxPart / total) * sweepScale : 0;
            if (!(sweep >= 0 && sweep <= 360)) { //NOPMD LogicInversion - NaN and infinite sweeps draw as nothing
                sweep = 0;
            }
            lastStart[i] = a;
            lastSweep[i] = sweep;
            if (sweep > 0.05) {
                double pad = n > 1 && sweep > padAngle * 2 ? padAngle / 2 : 0;
                float ox = 0;
                float oy = 0;
                if (i == selIndex) {
                    double mid = a + sweep / 2;
                    ox = Shapes.px(0, ctx.mm(1.5f), mid);
                    oy = Shapes.py(0, ctx.mm(1.5f), mid);
                }
                g.setColor(getColor(i));
                g.fillShape(Shapes.sector(sectorPath, cx + ox, cy + oy, r, inner, a + pad, sweep - pad * 2));
            }
            a += sweep;
        }
        if (centerText != null && inner > fh) {
            g.setColor(ctx.getTheme().getTextColor());
            String t = LabelLayout.ellipsize(f, centerText, (int) (inner * 1.6f));
            g.setFont(f);
            g.drawString(t, (int) (cx - f.stringWidth(t) / 2f), (int) (cy - fh / 2f));
        }
        if (sweepScale >= 1 && labelMode != LABELS_NONE) {
            if (outside) {
                paintOutsideLabels(ctx, cx, cy, r, maxLabel, gap);
            } else {
                paintInsideLabels(ctx, cx, cy, r, inner);
            }
        }
        g.setAlpha(alpha);
    }

    private void paintInsideLabels(ChartContext ctx, float cx, float cy, float r, float inner) {
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        int fh = f.getHeight();
        float mr = (r + inner) / 2;
        if (inner <= 0) {
            mr = r * 0.62f;
        }
        for (int i = 0; i < size(); i++) {
            double sweep = lastSweep[i];
            double p = percentOf(i);
            if (sweep <= 0 || p < 0) {
                continue;
            }
            String t = Axis.formatNumber(p, 0) + "%";
            int w = f.stringWidth(t);
            double mid = lastStart[i] + sweep / 2;
            // the arc length at the label radius must hold the text
            double arc = Math.toRadians(sweep) * mr;
            if (arc < w + ctx.mm(1) || (r - inner) < fh && inner > 0) {
                continue;
            }
            int x = Math.round(Shapes.px(cx, mr, mid) - w / 2f);
            int y = Math.round(Shapes.py(cy, mr, mid) - fh / 2f);
            if (ctx.getLabels().placeAt(x, y, w, fh)) {
                g.setColor(BarSeries.contrast(getColor(i)));
                g.drawString(t, x, y);
            }
        }
    }

    private void paintOutsideLabels(ChartContext ctx, float cx, float cy, float r, int maxLabel, int gap) {
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        int fh = f.getHeight();
        int n = size();
        int top = ctx.getPlotY();
        int bottom = ctx.getPlotY() + ctx.getPlotHeight();
        for (int side = 0; side < 2; side++) {
            if (labelIdx.length < n) {
                labelIdx = new int[n];
                labelWant = new float[n];
                labelDrop = new boolean[n];
                labelY = new float[n];
            }
            int[] idx = labelIdx;
            float[] want = labelWant;
            int m = 0;
            for (int i = 0; i < n; i++) {
                double sweep = lastSweep[i];
                if (sweep <= OUTSIDE_LABEL_SWEEP) {
                    continue;
                }
                double mid = lastStart[i] + sweep / 2;
                boolean right = Math.sin(Math.toRadians(mid)) >= 0;
                if (right != (side == 0)) {
                    continue;
                }
                idx[m] = i;
                want[m] = Shapes.py(cy, r + gap, mid) - fh / 2f;
                m++;
            }
            // one pixel more than the label layout's own gap so resolved labels are never rejected
            float[] y = resolve(idx, want, m, fh + 3, top, bottom);
            for (int k = 0; k < m; k++) {
                // NaN here is resolve()'s own "label rejected" marker, not data: NaN-only is right
                if (y[k] != y[k]) {
                    continue;
                }
                int i = idx[k];
                double mid = lastStart[i] + lastSweep[i] / 2;
                float ax = Shapes.px(cx, r, mid);
                float ay = Shapes.py(cy, r, mid);
                float ex = Shapes.px(cx, r + gap, mid);
                float ly = y[k] + fh / 2f;
                float tx = side == 0 ? cx + r + gap * 2 : cx - r - gap * 2;
                String t = LabelLayout.ellipsize(f, labelText(i), maxLabel);
                int w = f.stringWidth(t);
                int lx = side == 0 ? Math.round(tx + gap / 2f) : Math.round(tx - gap / 2f - w);
                if (!ctx.getLabels().placeAt(lx, Math.round(y[k]), w, fh)) {
                    continue;
                }
                g.setColor(ctx.getTheme().getSecondaryTextColor());
                g.drawLine(Math.round(ax), Math.round(ay), Math.round(ex), Math.round(ly));
                g.drawLine(Math.round(ex), Math.round(ly), Math.round(tx), Math.round(ly));
                g.setColor(ctx.getTheme().getTextColor());
                g.drawString(t, lx, Math.round(y[k]));
            }
        }
    }

    /// Spreads label tops so none overlap, dropping the smallest slices' labels when the column
    /// cannot hold them all. Dropped labels are `NaN`.
    private float[] resolve(int[] idx, float[] want, int m, int fh, int top, int bottom) {
        // sort by wanted position
        for (int a = 1; a < m; a++) {
            for (int b = a; b > 0 && want[b] < want[b - 1]; b--) {
                float tw = want[b];
                want[b] = want[b - 1];
                want[b - 1] = tw;
                int ti = idx[b];
                idx[b] = idx[b - 1];
                idx[b - 1] = ti;
            }
        }
        boolean[] drop = labelDrop;
        for (int k = 0; k < m; k++) {
            drop[k] = false;
        }
        int capacity = Math.max(0, (bottom - top) / fh);
        int kept = m;
        while (kept > capacity) {
            int worst = -1;
            for (int k = 0; k < m; k++) {
                if (!drop[k] && (worst < 0 || values[idx[k]] < values[idx[worst]])) {
                    worst = k;
                }
            }
            drop[worst] = true;
            kept--;
        }
        float[] y = labelY;
        float prev = top - fh;
        for (int k = 0; k < m; k++) {
            if (drop[k]) {
                y[k] = Float.NaN;
                continue;
            }
            y[k] = Math.max(Math.max(want[k], top), prev + fh);
            prev = y[k];
        }
        float limit = bottom - fh + 3;
        for (int k = m - 1; k >= 0; k--) {
            if (drop[k]) {
                continue;
            }
            if (y[k] > limit) {
                y[k] = limit;
            }
            limit = y[k] - fh;
        }
        return y;
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        float ax = x + ctx.getOriginX() - lastCx;
        float ay = y + ctx.getOriginY() - lastCy;
        double d = Math.sqrt(ax * ax + ay * ay);
        if (d > lastR + radius / 2f || d < lastR * innerFraction - radius / 2f) {
            return null;
        }
        double deg = Math.toDegrees(com.codename1.util.MathUtil.atan2(ax, -ay));
        for (int i = 0; i < lastCount; i++) {
            double rel = deg - lastStart[i];
            rel = rel - 360 * Math.floor(rel / 360);
            if (lastSweep[i] > 0 && rel <= lastSweep[i]) {
                return getPoint(ctx, i);
            }
        }
        return null;
    }

    @Override
    public ChartPoint getPoint(ChartContext ctx, int i) {
        if (i < 0 || i >= lastCount) {
            return null;
        }
        double mid = lastStart[i] + lastSweep[i] / 2;
        return point(ctx, i, Shapes.px(lastCx, lastR * 0.8f, mid), Shapes.py(lastCy, lastR * 0.8f, mid));
    }
}
