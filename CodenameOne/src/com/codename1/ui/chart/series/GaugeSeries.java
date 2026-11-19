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
import com.codename1.ui.chart.Series;
import com.codename1.ui.chart.layout.LabelLayout;
import com.codename1.ui.chart.layout.Shapes;

/// A gauge: one value on a curved scale, with optional coloured bands such as
/// green / amber / red zones. The needle or the filled arc animates to each new value.
///
/// ```java
/// GaugeSeries cpu = new GaugeSeries("CPU", 0, 100).setValue(72);
/// cpu.addBand(0, 60, 0x009E73).addBand(60, 85, 0xE69F00).addBand(85, 100, 0xD55E00);
/// ```
public class GaugeSeries extends Series {
    private double min;
    private double max;
    private double value;
    private double from = Double.NaN;
    private double sweep = 240;
    private boolean needle;
    private float thickness = 0.22f;
    private final java.util.ArrayList<double[]> bands = new java.util.ArrayList<double[]>();
    private String unit = "";
    private float lastCx;
    // the value font, derived once per (base font, size) rather than on every frame
    private Font bigBase;
    private float bigSize;
    private Font bigCached;
    private float lastCy;
    private float lastR;

    /// Creates a gauge.
    ///
    /// #### Parameters
    ///
    /// - `name`: the label under the value
    ///
    /// - `min`: the start of the scale
    ///
    /// - `max`: the end of the scale
    public GaugeSeries(String name, double min, double max) {
        super(name);
        this.min = min;
        this.max = max;
        this.value = min;
    }

    @Override
    public boolean isCartesian() {
        return false;
    }

    /// Sets the value, animated from the current one.
    ///
    /// #### Parameters
    ///
    /// - `v`: the new value, clamped to the scale
    ///
    /// #### Returns
    ///
    /// this series
    public GaugeSeries setValue(double v) {
        if (getChart() != null) {
            getChart().beginChange();
        }
        value = v;
        dataChanged();
        return this;
    }

    /// The current value.
    public double getValue() {
        return value;
    }

    /// Changes the scale.
    ///
    /// #### Parameters
    ///
    /// - `min`: the start of the scale
    ///
    /// - `max`: the end of the scale
    ///
    /// #### Returns
    ///
    /// this series
    public GaugeSeries setRange(double min, double max) {
        this.min = min;
        this.max = max;
        repaintChart();
        return this;
    }

    /// Adds a coloured zone to the scale.
    ///
    /// #### Parameters
    ///
    /// - `from`: where the zone starts
    ///
    /// - `to`: where the zone ends
    ///
    /// - `rgb`: its colour
    ///
    /// #### Returns
    ///
    /// this series
    public GaugeSeries addBand(double from, double to, int rgb) {
        bands.add(new double[]{from, to, rgb});
        repaintChart();
        return this;
    }

    /// The angle the scale spans; 180 is a semicircle, 240 the default, 360 a ring.
    ///
    /// #### Parameters
    ///
    /// - `deg`: the sweep in degrees
    ///
    /// #### Returns
    ///
    /// this series
    public GaugeSeries setSweep(double deg) {
        sweep = Math.max(30, Math.min(360, deg));
        repaintChart();
        return this;
    }

    /// Shows a needle over the bands instead of a filled arc.
    ///
    /// #### Parameters
    ///
    /// - `n`: true for a needle
    ///
    /// #### Returns
    ///
    /// this series
    public GaugeSeries setNeedle(boolean n) {
        needle = n;
        repaintChart();
        return this;
    }

    /// Text appended to the value, such as "%" or " km/h".
    ///
    /// #### Parameters
    ///
    /// - `unit`: the suffix
    ///
    /// #### Returns
    ///
    /// this series
    public GaugeSeries setUnit(String unit) {
        this.unit = unit == null ? "" : unit;
        repaintChart();
        return this;
    }

    @Override
    protected void captureTransitionStart() {
        ChartContext ctx = getChart() == null ? null : getChart().getLastContext();
        from = ctx == null ? min : shown(ctx);
    }

    @Override
    protected void captureEntryStart() {
        from = min;
    }

    private double shown(ChartContext ctx) {
        double t = ctx.getProgress();
        // an infinite reading is no reading, like NaN: clamping it would show a full gauge
        double target = isGap(value) ? Double.NaN : Math.max(min, Math.min(max, value));
        if (t >= 1 || isGap(from)) {
            return target;
        }
        return lerp(from, target, t);
    }

    private double angleOf(double v) {
        double span = max - min;
        double f = span == 0 ? 0 : (v - min) / span;
        return -sweep / 2 + sweep * f;
    }

    @Override
    public void paint(ChartContext ctx) {
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        int fh = f.getHeight();
        float w = ctx.getPlotWidth();
        float h = ctx.getPlotHeight();
        // the arc's lowest point decides how much height a given radius needs
        double lowest = -Math.cos(Math.toRadians(sweep / 2));
        // The value text is drawn up to 2.5x the font (capped by the radius, so this is an upper
        // bound) with the name one line under it; the radius is the largest that leaves room for
        // the arc's own lowest point AND for that text below the centre.
        float bigCap = f.isTTFNativeFont() ? fh * 2.5f : fh;
        float nameH = getName() != null ? fh : 0;
        float rArc = h / (float) (1 + Math.max(lowest, 0));
        float rText = needle ? (h - bigCap - nameH) / 1.25f : h - bigCap / 2f - nameH;
        float r = Math.min(w / 2f, Math.min(rArc, rText)) - ctx.mm(1);
        r = Math.max(8, r);
        float cx = ctx.getPlotX() + w / 2f;
        float cy = ctx.getPlotY() + r + ctx.mm(0.5f);
        lastCx = cx;
        lastCy = cy;
        lastR = r;
        float th = r * thickness;
        double start = -sweep / 2;
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction()));
        g.setColor(ctx.getTheme().getGridColor());
        g.fillShape(Shapes.sector(cx, cy, r, r - th, start, sweep));
        double v = shown(ctx);
        // NaN is "no reading": the track and bands are drawn, the value arc and needle are not
        boolean hasValue = !isGap(v);
        if (bands.isEmpty() || !needle) {
            if (!bands.isEmpty()) {
                for (int i = 0; i < bands.size(); i++) {
                    double[] b = bands.get(i);
                    g.setColor((int) b[2]);
                    g.setAlpha((int) (alpha * 0.25f));
                    double a0 = angleOf(Math.max(min, b[0]));
                    double a1 = angleOf(Math.min(max, b[1]));
                    if (a1 > a0) {
                        g.fillShape(Shapes.sector(cx, cy, r, r - th, a0, a1 - a0));
                    }
                }
                g.setAlpha(alpha);
            }
            if (hasValue) {
                g.setColor(colorFor(v));
                g.fillShape(Shapes.sector(cx, cy, r, r - th, start, angleOf(v) - start));
            }
        } else {
            for (int i = 0; i < bands.size(); i++) {
                double[] b = bands.get(i);
                g.setColor((int) b[2]);
                double a0 = angleOf(Math.max(min, b[0]));
                double a1 = angleOf(Math.min(max, b[1]));
                // a band wholly outside [min, max] clamps to an empty or reversed sweep
                if (a1 > a0) {
                    g.fillShape(Shapes.sector(cx, cy, r, r - th, a0, a1 - a0));
                }
            }
        }
        if (needle && hasValue) {
            double a = angleOf(v);
            float nw = Math.max(2, ctx.mm(0.8f));
            com.codename1.ui.geom.GeneralPath p = new com.codename1.ui.geom.GeneralPath();
            p.moveTo(Shapes.px(cx, r - th / 2, a), Shapes.py(cy, r - th / 2, a));
            p.lineTo(Shapes.px(cx, nw, a + 90), Shapes.py(cy, nw, a + 90));
            p.lineTo(Shapes.px(cx, nw, a - 90), Shapes.py(cy, nw, a - 90));
            p.closePath();
            g.setColor(ctx.getTheme().getTextColor());
            g.fillShape(p);
            g.fillArc(Math.round(cx - nw * 1.5f), Math.round(cy - nw * 1.5f), Math.round(nw * 3), Math.round(nw * 3), 0, 360);
        }
        String text = hasValue ? formatValue(ctx, needle ? value : v) + unit : "-";
        Font big = f;
        if (f.isTTFNativeFont()) {
            big = bigFont(f, Math.min(r * 0.45f, f.getPixelSize() * 2.5f));
        }
        g.setColor(ctx.getTheme().getTextColor());
        int ty = needle ? Math.round(cy + r * 0.25f) : Math.round(cy - big.getHeight() / 2f);
        g.setFont(big);
        String vt = LabelLayout.ellipsize(big, text, (int) (2 * (r - th) * 0.85f));
        g.drawString(vt, Math.round(cx - big.stringWidth(vt) / 2f), ty);
        g.setFont(f);
        if (getName() != null) {
            g.setColor(ctx.getTheme().getSecondaryTextColor());
            String nt = LabelLayout.ellipsize(f, getName(), (int) (2 * (r - th) * 0.85f));
            g.drawString(nt, Math.round(cx - f.stringWidth(nt) / 2f), ty + big.getHeight());
        }
        g.setAlpha(alpha);
    }

    private int colorFor(double v) {
        for (int i = 0; i < bands.size(); i++) {
            double[] b = bands.get(i);
            if (v >= b[0] && v <= b[1]) {
                return (int) b[2];
            }
        }
        return getColor();
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        float dx = x + ctx.getOriginX() - lastCx;
        float dy = y + ctx.getOriginY() - lastCy;
        if (dx * dx + dy * dy <= lastR * lastR) {
            return getPoint(ctx, 0);
        }
        return null;
    }

    @Override
    public int getPointCount() {
        return 1;
    }

    @Override
    public ChartPoint getPoint(ChartContext ctx, int index) {
        return new ChartPoint(this, 0, 0, value, Math.round(lastCx) - ctx.getOriginX(), Math.round(lastCy) - ctx.getOriginY(), null);
    }

    @Override
    public void collectTableRows(java.util.List<String[]> rows) {
        rows.add(new String[]{getName(), formatValue(null, min) + " - " + formatValue(null, max), formatValue(null, value) + unit});
    }

    private Font bigFont(Font f, float size) {
        if (bigCached == null || bigBase != f || Float.floatToIntBits(bigSize) != Float.floatToIntBits(size)) { //NOPMD CompareObjectsWithEquals - a different font instance is a different base
            bigBase = f;
            bigSize = size;
            bigCached = f.derive(size, Font.STYLE_BOLD);
        }
        return bigCached;
    }
}
