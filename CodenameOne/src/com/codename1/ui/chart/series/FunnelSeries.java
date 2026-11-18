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
import com.codename1.ui.geom.GeneralPath;

/// A funnel: stages of a process as centred bands whose widths are proportional to their
/// values, each labelled with its value and the share of the first stage that reached it.
///
/// ```java
/// new FunnelSeries("Signup").setData(new String[]{"Visited", "Signed up", "Paid"}, new double[]{1000, 300, 45});
/// ```
public class FunnelSeries extends CategoricalSeries {
    private float[] lastTop = new float[0];
    private float[] lastH = new float[0];
    private float lastCx;
    private float lastScale;

    /// Creates an empty funnel.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    public FunnelSeries(String name) {
        super(name);
    }

    @Override
    public void paint(ChartContext ctx) {
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        int fh = f.getHeight();
        int n = size();
        int shown = 0;
        double max = 0;
        for (int i = 0; i < n; i++) {
            double v = displayValue(ctx, i);
            if (isShown(i)) {
                shown++;
            }
            max = Math.max(max, v);
        }
        lastTop = new float[n];
        lastH = new float[n];
        if (shown == 0 || max <= 0) {
            return;
        }
        int gap = ctx.mm(0.6f);
        float bandH = (ctx.getPlotHeight() - gap * (shown - 1)) / (float) shown;
        float cx = ctx.getPlotX() + ctx.getPlotWidth() / 2f;
        float maxW = ctx.getPlotWidth();
        lastCx = cx;
        lastScale = (float) (maxW / max);
        float t = isEntering(ctx) ? ctx.getProgress() : 1;
        float y = ctx.getPlotY();
        double first = -1;
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction()));
        for (int i = 0; i < n; i++) {
            if (!isShown(i)) {
                lastTop[i] = y;
                continue;
            }
            double v = displayValue(ctx, i) * t;
            double next = v;
            for (int j = i + 1; j < n; j++) {
                if (isShown(j)) {
                    next = displayValue(ctx, j) * t;
                    break;
                }
            }
            float w0 = (float) (v / max * maxW);
            float w1 = (float) (next / max * maxW);
            GeneralPath p = new GeneralPath();
            p.moveTo(cx - w0 / 2, y);
            p.lineTo(cx + w0 / 2, y);
            p.lineTo(cx + w1 / 2, y + bandH);
            p.lineTo(cx - w1 / 2, y + bandH);
            p.closePath();
            g.setColor(getColor(i));
            g.fillShape(p);
            lastTop[i] = y;
            lastH[i] = bandH;
            if (first < 0) {
                first = values[i];
            }
            if (t >= 1 && bandH >= fh) {
                String pct = first > 0 ? "  " + Axis.formatNumber(values[i] * 100 / first, 0) + "%" : "";
                String text = LabelLayout.ellipsize(f, labels[i] + "  " + formatValue(ctx, values[i]) + pct, (int) maxW);
                int tw = f.stringWidth(text);
                int tx = Math.round(cx - tw / 2f);
                int ty = Math.round(y + (bandH - fh) / 2);
                boolean inside = tw + ctx.mm(2) < Math.min(w0, w1);
                g.setColor(inside ? BarSeries.contrast(getColor(i)) : ctx.getTheme().getTextColor());
                if (ctx.getLabels().placeAt(tx, ty, tw, fh)) {
                    g.drawString(text, tx, ty);
                }
            }
            y += bandH + gap;
        }
        g.setAlpha(alpha);
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        float ay = y + ctx.getOriginY();
        float ax = x + ctx.getOriginX();
        for (int i = 0; i < lastTop.length; i++) {
            if (lastH[i] > 0 && ay >= lastTop[i] && ay <= lastTop[i] + lastH[i]
                    && Math.abs(ax - lastCx) <= Math.max(values[i] * lastScale / 2, radius)) {
                return getPoint(ctx, i);
            }
        }
        return null;
    }

    @Override
    public ChartPoint getPoint(ChartContext ctx, int i) {
        if (i < 0 || i >= lastTop.length || lastH[i] <= 0) {
            return null;
        }
        return point(ctx, i, lastCx, lastTop[i] + lastH[i] / 2);
    }
}
