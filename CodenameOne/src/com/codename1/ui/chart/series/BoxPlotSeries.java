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

/// Box and whisker plots: the median, the interquartile box and the whiskers of each group.
///
/// Give it the five numbers directly or let it compute them from raw samples:
///
/// ```java
/// BoxPlotSeries s = new BoxPlotSeries("Latency");
/// s.addSamples(0, mondaySamples);
/// s.addSamples(1, tuesdaySamples);
/// ```
public class BoxPlotSeries extends XYSeries {
    private static final int MIN = 0;
    private static final int Q1 = 1;
    private static final int Q3 = 2;
    private static final int MAX = 3;
    private float boxFraction = 0.5f;
    // outliers of boxes built from samples, as {x, outlier...} rows; keyed by x so a slide cannot
    // pair a box with another box's outliers
    private final java.util.ArrayList<double[]> outliers = new java.util.ArrayList<double[]>();

    /// Creates an empty series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    public BoxPlotSeries(String name) {
        super(name, 4);
    }

    /// Appends a box from its five numbers.
    ///
    /// #### Parameters
    ///
    /// - `x`: the group's x value
    ///
    /// - `min`: the lower whisker
    ///
    /// - `q1`: the first quartile
    ///
    /// - `median`: the median
    ///
    /// - `q3`: the third quartile
    ///
    /// - `max`: the upper whisker
    ///
    /// #### Returns
    ///
    /// this series
    public BoxPlotSeries addBox(double x, double min, double q1, double median, double q3, double max) {
        removeOutliers(x);
        addRow(x, median, new double[]{min, q1, q3, max});
        return this;
    }

    /// Appends a box computed from raw samples. The whiskers reach the furthest samples within
    /// 1.5 times the interquartile range of the box (Tukey's rule) and every sample beyond them is
    /// an outlier, drawn as a dot and returned by `getOutliers(int)`.
    ///
    /// #### Parameters
    ///
    /// - `x`: the group's x value
    ///
    /// - `samples`: the values; the array is not modified
    ///
    /// #### Returns
    ///
    /// this series
    public BoxPlotSeries addSamples(double x, double[] samples) {
        // NaN has no place in an order statistic, and sort does not order it on every platform
        int n = 0;
        for (double v : samples) {
            if (v == v) {
                n++;
            }
        }
        double[] s = new double[n];
        n = 0;
        for (double v : samples) {
            if (v == v) {
                s[n++] = v;
            }
        }
        java.util.Arrays.sort(s);
        if (s.length == 0) {
            return this;
        }
        double q1 = quantile(s, 0.25);
        double q3 = quantile(s, 0.75);
        double fence = 1.5 * (q3 - q1);
        double lo = q1 - fence;
        double hi = q3 + fence;
        double wMin = q1;
        double wMax = q3;
        int out = 0;
        for (double v : s) {
            if (v < lo || v > hi) {
                out++;
            } else {
                wMin = Math.min(wMin, v);
                wMax = Math.max(wMax, v);
            }
        }
        removeOutliers(x);
        if (out > 0) {
            double[] row = new double[out + 1];
            row[0] = x;
            int k = 1;
            for (double v : s) {
                if (v < lo || v > hi) {
                    row[k++] = v;
                }
            }
            outliers.add(row);
        }
        addRow(x, quantile(s, 0.5), new double[]{wMin, q1, q3, wMax});
        return this;
    }

    /// The outliers of box `i`: the samples `addSamples` placed beyond the whiskers. A box given
    /// by `addBox` has none.
    ///
    /// #### Parameters
    ///
    /// - `i`: the box index
    ///
    /// #### Returns
    ///
    /// a new array, empty when there are none
    public double[] getOutliers(int i) {
        double x = getX(i);
        for (int k = 0; k < outliers.size(); k++) {
            double[] row = outliers.get(k);
            if (row[0] == x) {
                double[] o = new double[row.length - 1];
                System.arraycopy(row, 1, o, 0, o.length);
                return o;
            }
        }
        return new double[0];
    }

    /// The lower whisker of box `i`.
    ///
    /// #### Parameters
    ///
    /// - `i`: the box index
    ///
    /// #### Returns
    ///
    /// the lower whisker's value
    public double getMin(int i) {
        return getAux(MIN, i);
    }

    /// The upper whisker of box `i`.
    ///
    /// #### Parameters
    ///
    /// - `i`: the box index
    ///
    /// #### Returns
    ///
    /// the upper whisker's value
    public double getMax(int i) {
        return getAux(MAX, i);
    }

    private void removeOutliers(double x) {
        for (int k = outliers.size() - 1; k >= 0; k--) {
            if (outliers.get(k)[0] == x) {
                outliers.remove(k);
            }
        }
    }

    /// Forgets the outliers of boxes that are no longer in the series, such as one a slide dropped.
    @Override
    protected void dataChanged() {
        for (int k = outliers.size() - 1; k >= 0; k--) {
            if (indexOfX(outliers.get(k)[0]) < 0) {
                outliers.remove(k);
            }
        }
        super.dataChanged();
    }

    /// Linear interpolated quantile of sorted data.
    ///
    /// #### Parameters
    ///
    /// - `sorted`: ascending values
    ///
    /// - `q`: the quantile, 0 to 1
    ///
    /// #### Returns
    ///
    /// the quantile
    public static double quantile(double[] sorted, double q) {
        if (sorted.length == 0) {
            return Double.NaN;
        }
        if (sorted.length == 1) {
            return sorted[0];
        }
        double pos = q * (sorted.length - 1);
        int i = (int) Math.floor(pos);
        double f = pos - i;
        if (i + 1 >= sorted.length) {
            return sorted[sorted.length - 1];
        }
        return sorted[i] + (sorted[i + 1] - sorted[i]) * f;
    }

    /// The fraction of each slot a box takes.
    ///
    /// #### Parameters
    ///
    /// - `f`: 0.1 to 1
    ///
    /// #### Returns
    ///
    /// this series
    public BoxPlotSeries setBoxFraction(float f) {
        boxFraction = Math.max(0.1f, Math.min(1, f));
        repaintChart();
        return this;
    }

    @Override
    public double[] getYBounds() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < size(); i++) {
            min = Math.min(min, getAux(MIN, i));
            max = Math.max(max, getAux(MAX, i));
            for (double o : getOutliers(i)) {
                min = Math.min(min, o);
                max = Math.max(max, o);
            }
        }
        return min > max ? null : new double[]{min, max};
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

    @Override
    public void paint(ChartContext ctx) {
        int[] r = visibleRange(ctx);
        Graphics g = ctx.getGraphics();
        int alpha = g.getAlpha();
        float vis = getVisibilityFraction();
        float t = ctx.getProgress();
        float slot = ctx.xLength(getMinXSpacing());
        float half = slot * boxFraction / 2;
        boolean rot = ctx.isRotated();
        int color = getColor();
        for (int i = r[0]; i <= r[1]; i++) {
            double med = getY(i);
            double[] v = new double[4];
            for (int k = 0; k < 4; k++) {
                v[k] = med + (getAux(k, i) - med) * t;
            }
            float c = rot ? ctx.screenY(this, getX(i), med) : ctx.screenX(this, getX(i), med);
            float pMin = pos(ctx, i, v[MIN], rot);
            float pQ1 = pos(ctx, i, v[Q1], rot);
            float pMed = pos(ctx, i, med, rot);
            float pQ3 = pos(ctx, i, v[Q3], rot);
            float pMax = pos(ctx, i, v[MAX], rot);
            g.setColor(color);
            g.setAlpha((int) (alpha * vis * 0.3f));
            rect(g, rot, c - half, Math.min(pQ1, pQ3), half * 2, Math.abs(pQ3 - pQ1));
            g.setAlpha((int) (alpha * vis));
            outline(g, rot, c - half, Math.min(pQ1, pQ3), half * 2, Math.abs(pQ3 - pQ1));
            line(g, rot, c, pMin, c, pQ1);
            line(g, rot, c, pMax, c, pQ3);
            line(g, rot, c - half / 2, pMin, c + half / 2, pMin);
            line(g, rot, c - half / 2, pMax, c + half / 2, pMax);
            rect(g, rot, c - half, pMed - 1, half * 2, Math.max(2, ctx.mm(0.4f)));
            if (t >= 1) {
                int d = Math.max(3, ctx.mm(1));
                for (double o : getOutliers(i)) {
                    float po = pos(ctx, i, o, rot);
                    int cx = Math.round(rot ? po : c);
                    int cy = Math.round(rot ? c : po);
                    g.drawArc(cx - d / 2, cy - d / 2, d, d, 0, 360);
                }
            }
        }
        g.setAlpha(alpha);
    }

    private float pos(ChartContext ctx, int i, double v, boolean rot) {
        return rot ? ctx.screenX(this, getX(i), v) : ctx.screenY(this, getX(i), v);
    }

    private static void rect(Graphics g, boolean rot, float a, float b, float w, float h) {
        if (rot) {
            g.fillRect(Math.round(b), Math.round(a), Math.max(1, Math.round(h)), Math.max(1, Math.round(w)));
        } else {
            g.fillRect(Math.round(a), Math.round(b), Math.max(1, Math.round(w)), Math.max(1, Math.round(h)));
        }
    }

    private static void outline(Graphics g, boolean rot, float a, float b, float w, float h) {
        if (rot) {
            g.drawRect(Math.round(b), Math.round(a), Math.max(1, Math.round(h)), Math.max(1, Math.round(w)));
        } else {
            g.drawRect(Math.round(a), Math.round(b), Math.max(1, Math.round(w)), Math.max(1, Math.round(h)));
        }
    }

    private static void line(Graphics g, boolean rot, float a0, float b0, float a1, float b1) {
        if (rot) {
            g.drawLine(Math.round(b0), Math.round(a0), Math.round(b1), Math.round(a1));
        } else {
            g.drawLine(Math.round(a0), Math.round(b0), Math.round(a1), Math.round(b1));
        }
    }

    @Override
    public String formatTooltip(ChartContext ctx, ChartPoint p) {
        int i = p.getIndex();
        return "min " + formatValue(ctx, getAux(MIN, i)) + "  Q1 " + formatValue(ctx, getAux(Q1, i))
                + "  med " + formatValue(ctx, getY(i)) + "  Q3 " + formatValue(ctx, getAux(Q3, i))
                + "  max " + formatValue(ctx, getAux(MAX, i));
    }

    @Override
    protected String tableValue(int i) {
        return formatValue(null, getAux(MIN, i)) + " / " + formatValue(null, getAux(Q1, i)) + " / " + formatValue(null, getY(i))
                + " / " + formatValue(null, getAux(Q3, i)) + " / " + formatValue(null, getAux(MAX, i));
    }
}
