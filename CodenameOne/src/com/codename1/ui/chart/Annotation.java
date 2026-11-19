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
package com.codename1.ui.chart;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.chart.layout.LabelLayout;
import com.codename1.ui.geom.GeneralPath;

/// A reference line, shaded band or label laid over a cartesian chart: a target, a
/// threshold, a weekend, a release date.
///
/// ```java
/// chart.addAnnotation(Annotation.valueLine(100, "Target"));
/// chart.addAnnotation(Annotation.xBand(start, end, "Outage").setColor(0xEF5350));
/// ```
public final class Annotation {
    private static final int VALUE_LINE = 0;
    private static final int VALUE_BAND = 1;
    private static final int X_LINE = 2;
    private static final int X_BAND = 3;

    private final int kind;
    private final double from;
    private final double to;
    private final String label;
    private Integer color;
    private boolean behind;
    private boolean dashed = true;
    private boolean includeInRange = true;

    private Annotation(int kind, double from, double to, String label) {
        this.kind = kind;
        this.from = Math.min(from, to);
        this.to = Math.max(from, to);
        this.label = label;
        this.behind = kind == VALUE_BAND || kind == X_BAND;
    }

    /// A line across the plot at a value.
    public static Annotation valueLine(double value, String label) {
        return new Annotation(VALUE_LINE, value, value, label);
    }

    /// A shaded band between two values.
    public static Annotation valueBand(double from, double to, String label) {
        return new Annotation(VALUE_BAND, from, to, label);
    }

    /// A line across the plot at an x position (a date, a category index).
    public static Annotation xLine(double x, String label) {
        return new Annotation(X_LINE, x, x, label);
    }

    /// A shaded band between two x positions.
    public static Annotation xBand(double from, double to, String label) {
        return new Annotation(X_BAND, from, to, label);
    }

    /// Sets the colour; the theme's axis colour by default.
    public Annotation setColor(int rgb) {
        color = Integer.valueOf(rgb);
        return this;
    }

    /// Draws a line solid instead of dashed.
    public Annotation setDashed(boolean dashed) {
        this.dashed = dashed;
        return this;
    }

    /// Draws behind the series instead of over them; bands default to behind.
    public Annotation setBehindSeries(boolean behind) {
        this.behind = behind;
        return this;
    }

    /// Whether a value line or band widens the value axis so it is always visible.
    public Annotation setIncludeInRange(boolean include) {
        includeInRange = include;
        return this;
    }

    private boolean secondaryAxis;

    /// Measures a value line or band against the chart's secondary y axis, for a threshold
    /// that belongs to a series drawn on that axis.
    public Annotation setSecondaryAxis(boolean secondary) {
        this.secondaryAxis = secondary;
        return this;
    }

    boolean isSecondaryAxis() {
        return secondaryAxis;
    }

    boolean isBehindSeries() {
        return behind;
    }

    boolean isOnValueAxis() {
        return includeInRange && (kind == VALUE_LINE || kind == VALUE_BAND) && finite(from) && finite(to);
    }

    double getValueExtent() {
        return from;
    }

    double getValueExtentMax() {
        return to;
    }

    private String labelText;
    private int labelX;
    private int labelY;

    private static boolean finite(double v) {
        return v > Double.NEGATIVE_INFINITY && v < Double.POSITIVE_INFINITY;
    }

    /// `{a0, a1, horizontal ? 1 : 0}`: the line or band's pixel extent, or null off screen.
    private int[] extent(ChartContext ctx) {
        boolean valueKind = kind == VALUE_LINE || kind == VALUE_BAND;
        boolean horizontalLine = valueKind != ctx.isRotated();
        com.codename1.ui.chart.axis.Axis axis = ctx.getXAxis();
        if (valueKind) {
            com.codename1.ui.chart.axis.Axis second = ctx.getChart().getSecondaryYAxis();
            axis = secondaryAxis && second != null ? second : ctx.getChart().getYAxis();
        }
        if (!finite(from) || !finite(to)) {
            // NaN normalizes to NaN, which every comparison above lets through to the plot's bottom edge
            return null;
        }
        if (axis instanceof com.codename1.ui.chart.axis.LogAxis && Math.max(from, to) <= 0) {
            // a log axis clamps zero and below to an edge-hugging 1e-300 instead of answering NaN,
            // so a line (or a band) lying wholly at or below zero is off the axis and skipped
            return null;
        }
        double n0 = axis.toNormalized(from);
        double n1 = axis.toNormalized(to);
        if (Double.isNaN(n0) || Double.isNaN(n1)) {
            // a custom axis may answer NaN for a value it cannot place: no position on the plot
            return null;
        }
        if (n1 < 0 || n0 > 1) {
            return null;
        }
        n0 = Math.max(0, n0);
        n1 = Math.min(1, n1);
        int px = ctx.getPlotX();
        int py = ctx.getPlotY();
        int pw = ctx.getPlotWidth();
        int ph = ctx.getPlotHeight();
        boolean topDown = ctx.getChart().isTopDown(axis);
        if (horizontalLine) {
            if (topDown) {
                return new int[]{py + (int) Math.round(n0 * ph), py + (int) Math.round(n1 * ph), 1};
            }
            return new int[]{py + ph - (int) Math.round(n1 * ph), py + ph - (int) Math.round(n0 * ph), 1};
        }
        return new int[]{px + (int) Math.round(n0 * pw), px + (int) Math.round(n1 * pw), 0};
    }

    /// Finds a free place for the label, trying both sides of the line; a label with no free
    /// place is left out rather than drawn over another.
    void layoutLabel(ChartContext ctx) {
        labelText = null;
        int[] e = label == null || label.length() == 0 ? null : extent(ctx);
        if (e == null) {
            return;
        }
        Font f = ctx.getFont();
        int pad = ctx.mm(1);
        int px = ctx.getPlotX();
        int py = ctx.getPlotY();
        int pw = ctx.getPlotWidth();
        String t = LabelLayout.ellipsize(f, label, pw / 2);
        int tw = f.stringWidth(t);
        int fh = f.getHeight();
        int[][] candidates;
        if (e[2] == 1) {
            candidates = new int[][]{
                {px + pw - tw - pad, e[0] - fh - pad / 2}, {px + pw - tw - pad, e[1] + pad / 2},
                {px + pad, e[0] - fh - pad / 2}, {px + pad, e[1] + pad / 2}
            };
        } else {
            candidates = new int[][]{
                {e[1] + pad, py + pad}, {e[0] - pad - tw, py + pad},
                {e[1] + pad, py + pad + fh}, {e[0] - pad - tw, py + pad + fh}
            };
        }
        for (int[] c : candidates) {
            if (ctx.getLabels().placeAt(c[0], c[1], tw, fh)) {
                labelText = t;
                labelX = c[0];
                labelY = c[1];
                return;
            }
        }
    }

    void paint(ChartContext ctx) {
        Graphics g = ctx.getGraphics();
        int c = color != null ? color.intValue() : ctx.getTheme().getAxisColor();
        int[] e = extent(ctx);
        if (e == null) {
            return;
        }
        boolean horizontalLine = e[2] == 1;
        int a0 = e[0];
        int a1 = e[1];
        int px = ctx.getPlotX();
        int py = ctx.getPlotY();
        int pw = ctx.getPlotWidth();
        int ph = ctx.getPlotHeight();
        int alpha = g.getAlpha();
        g.setColor(c);
        if (kind == VALUE_BAND || kind == X_BAND) {
            g.setAlpha(alpha * 40 / 255);
            if (horizontalLine) {
                g.fillRect(px, a0, pw, Math.max(1, a1 - a0));
            } else {
                g.fillRect(a0, py, Math.max(1, a1 - a0), ph);
            }
            g.setAlpha(alpha);
        } else {
            float w = Math.max(1, ctx.mm(0.35f));
            Stroke s = new Stroke(w, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 4);
            // at least a pixel: mm() rounds to 0 on a low density, and a 0 step never ends the loop
            int dash = Math.max(1, dashed ? ctx.mm(1.5f) : Math.max(pw, ph));
            int len = horizontalLine ? pw : ph;
            for (int o = 0; o < len; o += dash * 2) {
                GeneralPath p = new GeneralPath();
                int end = Math.min(len, o + dash);
                if (horizontalLine) {
                    p.moveTo(px + o, a0);
                    p.lineTo(px + end, a0);
                } else {
                    p.moveTo(a0, py + o);
                    p.lineTo(a0, py + end);
                }
                g.drawShape(p, s);
            }
        }
        if (labelText != null) {
            g.setColor(c);
            g.drawString(labelText, labelX, labelY);
        }
    }
}
