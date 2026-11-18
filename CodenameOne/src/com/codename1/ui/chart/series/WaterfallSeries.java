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

/// A waterfall (bridge) chart: each bar starts where the previous one ended, so a sequence of
/// gains and losses reads as a running total. Total bars restart from zero and show the sum.
///
/// ```java
/// new WaterfallSeries("Profit").setSteps(new double[]{120, -30, -25, 40, 0}, new boolean[]{false, false, false, false, true});
/// ```
public class WaterfallSeries extends BarSeries {
    private boolean[] totals = new boolean[0];
    private boolean connectors = true;

    /// Creates an empty series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    public WaterfallSeries(String name) {
        super(name, 1);
        setBarFraction(0.6f);
    }

    /// Replaces all steps.
    ///
    /// #### Parameters
    ///
    /// - `deltas`: the change at each step; ignored for total bars
    ///
    /// - `isTotal`: which bars show the running total instead of a change, or null for none
    ///
    /// #### Returns
    ///
    /// this series
    public WaterfallSeries setSteps(double[] deltas, boolean[] isTotal) {
        int n = deltas.length;
        totals = new boolean[n];
        double[] y = new double[n];
        double[] base = new double[n];
        for (int i = 0; i < n; i++) {
            totals[i] = isTotal != null && i < isTotal.length && isTotal[i];
            y[i] = deltas[i];
        }
        // the bases and totals are filled in by dataChanged, which every change goes through
        setRows(null, y, new double[][]{base});
        return this;
    }

    /// Recomputes every base and total from the deltas, so `set`, `add` and a slide keep the bridge
    /// joined. A NaN delta is a gap: its bar is not drawn and the running total carries past it.
    @Override
    protected void dataChanged() {
        double run = 0;
        double[] base = aux[0];
        for (int i = 0; i < count; i++) {
            if (isTotal(i)) {
                base[i] = 0;
                ys[i] = run;
            } else {
                base[i] = run;
                double d = ys[i];
                if (d == d) {
                    run += d;
                }
            }
        }
        super.dataChanged();
    }

    /// Shows or hides the thin lines joining each bar's end to the next bar's start.
    ///
    /// #### Parameters
    ///
    /// - `c`: true to draw connectors
    ///
    /// #### Returns
    ///
    /// this series
    public WaterfallSeries setConnectors(boolean c) {
        connectors = c;
        repaintChart();
        return this;
    }

    /// True when bar `i` is a total.
    public boolean isTotal(int i) {
        return i < totals.length && totals[i];
    }

    @Override
    public double getBase(int i) {
        return getAux(0, i);
    }

    @Override
    public double[] getYBounds() {
        double min = 0;
        double max = 0;
        for (int i = 0; i < size(); i++) {
            double v = getY(i);
            if (v != v) {
                continue;
            }
            double a = getBase(i);
            double b = a + v;
            min = Math.min(min, Math.min(a, b));
            max = Math.max(max, Math.max(a, b));
        }
        return size() == 0 ? null : new double[]{min, max};
    }

    @Override
    protected int colorAt(ChartContext ctx, int i) {
        if (isTotal(i)) {
            return getColor();
        }
        return getY(i) >= 0 ? ctx.getTheme().getPositiveColor() : ctx.getTheme().getNegativeColor();
    }

    @Override
    public void paint(ChartContext ctx) {
        if (connectors && ctx.getProgress() >= 1) {
            Graphics g = ctx.getGraphics();
            g.setColor(ctx.getTheme().getAxisColor());
            for (int i = 0; i + 1 < size(); i++) {
                double d = getY(i);
                double end = getBase(i) + (d == d ? d : 0);
                float x0 = ctx.screenX(this, getX(i), end);
                float y0 = ctx.screenY(this, getX(i), end);
                float x1 = ctx.screenX(this, getX(i + 1), end);
                float y1 = ctx.screenY(this, getX(i + 1), end);
                g.drawLine(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1));
            }
        }
        super.paint(ctx);
    }

    @Override
    public String formatTooltip(ChartContext ctx, ChartPoint p) {
        int i = p.getIndex();
        double v = getY(i);
        String sign = !isTotal(i) && v > 0 ? "+" : "";
        String n = p.getLabel() != null ? p.getLabel() : getName();
        return (n == null ? "" : n + ": ") + sign + formatValue(ctx, v);
    }
}
