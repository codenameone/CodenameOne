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

/// Open/high/low/close prices drawn as candlesticks, OHLC bars or Heikin-Ashi candles.
///
/// A rising period (close at or above open) is drawn in the theme's positive colour and a falling
/// one in its negative colour. Pair it with a `SessionTimeAxis` to skip weekends, or with a
/// `TimeAxis` to keep calendar spacing.
///
/// ```java
/// Chart chart = new Chart();
/// long day = 24L * 60 * 60 * 1000;
/// long[] timestamps = {0, day, 4 * day};
/// double[] open = {10, 11, 12}, high = {12, 13, 14}, low = {9, 10, 11}, close = {11, 12, 13};
/// CandlestickSeries prices = new CandlestickSeries("ACME");
/// prices.setOhlc(null, open, high, low, close);
/// chart.setXAxis(new SessionTimeAxis(Axis.BOTTOM, timestamps));
/// chart.addSeries(prices);
/// ```
public class CandlestickSeries extends XYSeries {
    /// Filled bodies with wicks.
    public static final int CANDLES = 0;
    /// A vertical high-low line with open and close ticks.
    public static final int OHLC_BARS = 1;
    /// Candles of Heikin-Ashi averaged prices, which smooth out noise to show the trend.
    public static final int HEIKIN_ASHI = 2;

    private static final int OPEN = 0;
    private static final int HIGH = 1;
    private static final int LOW = 2;

    private int style = CANDLES;
    private float bodyFraction = 0.7f;
    private Integer upColor;
    private Integer downColor;
    private double[][] ha;

    /// Creates an empty series.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    public CandlestickSeries(String name) {
        super(name, 3);
    }

    /// Replaces all prices.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x value of each period (a time, or an index for `SessionTimeAxis`); null for 0, 1, 2...
    ///
    /// - `open`: opening prices
    ///
    /// - `high`: highest prices
    ///
    /// - `low`: lowest prices
    ///
    /// - `close`: closing prices
    ///
    /// #### Returns
    ///
    /// this series
    public CandlestickSeries setOhlc(double[] x, double[] open, double[] high, double[] low, double[] close) {
        ha = null;
        setRows(x, close, new double[][]{open, high, low});
        return this;
    }

    /// Appends one period, for streaming quotes.
    ///
    /// #### Parameters
    ///
    /// - `x`: the period's x value
    ///
    /// - `open`: the opening price
    ///
    /// - `high`: the highest price
    ///
    /// - `low`: the lowest price
    ///
    /// - `close`: the closing price
    ///
    /// #### Returns
    ///
    /// this series
    public CandlestickSeries addOhlc(double x, double open, double high, double low, double close) {
        ha = null;
        addRow(x, close, new double[]{open, high, low});
        return this;
    }

    @Override
    public XYSeries add(double x, double y) {
        // a candle of one price: its open, high and low are derived from it, so set() moves them
        ha = null;
        return super.add(x, y);
    }

    /// Updates the last period in place, as a live tick does to the current candle.
    ///
    /// #### Parameters
    ///
    /// - `price`: the latest traded price
    ///
    /// #### Returns
    ///
    /// this series
    public CandlestickSeries updateLast(double price) {
        int i = size() - 1;
        if (i < 0) {
            return this;
        }
        // snapshot before anything changes, so the widened wick tweens with the body; only a
        // live tick widens high and low, set() writes the close alone
        if (getChart() != null) {
            getChart().beginChange();
        }
        // a live candle holds real prices now: set() below must not re-derive its open, high or low
        markExplicit(i);
        if (!isGap(price)) {
            if (!(aux[HIGH][i] >= price)) { //NOPMD LogicInversion - also true for a NaN high
                aux[HIGH][i] = price;
            }
            if (!(aux[LOW][i] <= price)) { //NOPMD LogicInversion - also true for a NaN low
                aux[LOW][i] = price;
            }
        }
        set(i, price);
        return this;
    }

    /// Chooses candles, OHLC bars or Heikin-Ashi candles.
    ///
    /// #### Parameters
    ///
    /// - `style`: `CANDLES`, `OHLC_BARS` or `HEIKIN_ASHI`
    ///
    /// #### Returns
    ///
    /// this series
    public CandlestickSeries setStyle(int style) {
        this.style = style;
        repaintChart();
        return this;
    }

    /// The drawing style.
    public int getStyle() {
        return style;
    }

    /// The fraction of each slot a body takes, 0.1 to 1.
    ///
    /// #### Parameters
    ///
    /// - `f`: the fraction
    ///
    /// #### Returns
    ///
    /// this series
    public CandlestickSeries setBodyFraction(float f) {
        bodyFraction = Math.max(0.1f, Math.min(1, f));
        repaintChart();
        return this;
    }

    /// Overrides the theme's rising and falling colours.
    ///
    /// #### Parameters
    ///
    /// - `up`: colour of rising periods
    ///
    /// - `down`: colour of falling periods
    ///
    /// #### Returns
    ///
    /// this series
    public CandlestickSeries setColors(int up, int down) {
        upColor = Integer.valueOf(up);
        downColor = Integer.valueOf(down);
        repaintChart();
        return this;
    }

    /// Opening price of period `i`.
    public double getOpen(int i) {
        return getAux(OPEN, i);
    }

    /// Highest price of period `i`.
    public double getHigh(int i) {
        return getAux(HIGH, i);
    }

    /// Lowest price of period `i`.
    public double getLow(int i) {
        return getAux(LOW, i);
    }

    /// Closing price of period `i`.
    public double getClose(int i) {
        return getY(i);
    }

    /// Computes Heikin-Ashi values, `{open, high, low, close}` arrays.
    ///
    /// #### Returns
    ///
    /// the averaged prices for every period
    public double[][] heikinAshi() {
        int n = size();
        double[][] out = new double[4][n];
        for (int i = 0; i < n; i++) {
            double o = getOpen(i);
            double h = getHigh(i);
            double l = getLow(i);
            double c = getClose(i);
            if (isGap(o) || isGap(h) || isGap(l) || isGap(c)) {
                // a candle with any price missing is a gap, and the next one starts the average over
                out[0][i] = Double.NaN;
                out[1][i] = Double.NaN;
                out[2][i] = Double.NaN;
                out[3][i] = Double.NaN;
                continue;
            }
            double hc = (o + h + l + c) / 4;
            double prev = i == 0 ? Double.NaN : (out[0][i - 1] + out[3][i - 1]) / 2;
            // after a gap the average starts over, so one missing period does not blank the rest
            double ho = isGap(prev) ? (o + c) / 2 : prev;
            out[0][i] = ho;
            out[1][i] = Math.max(h, Math.max(ho, hc));
            out[2][i] = Math.min(l, Math.min(ho, hc));
            out[3][i] = hc;
        }
        return out;
    }

    // one buffer refilled per call: paint and hit testing read a row and are done with it
    private final double[] rowBuf = new double[4];

    /// Drops the cached Heikin-Ashi values: they depend on every candle, so any change to the data
    /// (an add, a slide, a `set`, an `updateLast`) makes them stale, not only a change of length.
    @Override
    protected void dataChanged() {
        ha = null;
        super.dataChanged();
    }

    private double[] row(int i) {
        double[] r = rowBuf;
        if (style == HEIKIN_ASHI) {
            if (ha == null || ha[0].length != size()) {
                ha = heikinAshi();
            }
            r[0] = ha[0][i];
            r[1] = ha[1][i];
            r[2] = ha[2][i];
            r[3] = ha[3][i];
            return r;
        }
        r[0] = getOpen(i);
        r[1] = getHigh(i);
        r[2] = getLow(i);
        r[3] = getClose(i);
        return r;
    }

    @Override
    public double[] getYBounds() {
        int n = size();
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double l = getLow(i);
            double h = getHigh(i);
            double c = getY(i);
            // a close set outside an explicit high/low is still drawn, so it is kept on the axis
            if (!isGap(c)) {
                min = Math.min(min, c);
                max = Math.max(max, c);
            }
            if (!isGap(l)) {
                min = Math.min(min, l);
            }
            if (!isGap(h)) {
                max = Math.max(max, h);
            }
            if (style == HEIKIN_ASHI) {
                // the averaged candle can reach past the raw high and low, and it is what is drawn
                double[] v = row(i);
                if (!isGap(v[1])) {
                    max = Math.max(max, v[1]);
                    min = Math.min(min, v[1]);
                }
                if (!isGap(v[2])) {
                    min = Math.min(min, v[2]);
                    max = Math.max(max, v[2]);
                }
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

    // a candle missing any price, or its x, is not drawn: a NaN would map to pixel 0 and an
    // infinity to a coordinate millions of pixels off screen
    private boolean isGap(int i, double[] v) {
        return isGap(getX(i)) || isGap(v[0]) || isGap(v[1]) || isGap(v[2]) || isGap(v[3]);
    }

    private int up(ChartContext ctx) {
        return upColor != null ? upColor.intValue() : ctx.getTheme().getPositiveColor();
    }

    private int down(ChartContext ctx) {
        return downColor != null ? downColor.intValue() : ctx.getTheme().getNegativeColor();
    }

    @Override
    public void paint(ChartContext ctx) {
        int[] r = visibleRange(ctx);
        Graphics g = ctx.getGraphics();
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction()));
        float slot = ctx.xLength(getMinXSpacing());
        int body = Math.max(1, Math.round(slot * bodyFraction));
        int wick = Math.max(1, Math.min(ctx.mm(0.25f), body / 4));
        boolean rot = ctx.isRotated();
        boolean ha = style == HEIKIN_ASHI;
        for (int i = r[0]; i <= r[1]; i++) {
            double[] v = row(i);
            if (isGap(i, v)) {
                continue;
            }
            // each candle moves from where it was when the last transition started, so a change to
            // one candle (updateLast, a slide) leaves the rest still; only a first appearance grows.
            // A Heikin-Ashi row is derived from every earlier candle, so it takes the raw candle's
            // animated offset per price rather than being re-derived (and reallocated) each frame
            double[] pr = paintedRow(ctx, i, v, ha);
            double o = pr[0];
            double h = pr[1];
            double l = pr[2];
            double cl = pr[3];
            boolean rising = v[3] >= v[0];
            g.setColor(rising ? up(ctx) : down(ctx));
            double x = getX(i);
            // the category position runs along x, or along y when the chart is rotated
            int cx = Math.round(rot ? ctx.screenY(this, x, cl) : ctx.screenX(this, x, cl));
            int ph = Math.round(pos(ctx, x, h, rot));
            int pl = Math.round(pos(ctx, x, l, rot));
            int po = Math.round(pos(ctx, x, o, rot));
            int pc = Math.round(pos(ctx, x, cl, rot));
            int wTop = Math.min(ph, pl);
            int wLen = Math.max(1, Math.abs(pl - ph));
            rect(g, rot, cx - wick / 2, wTop, wick, wLen);
            if (style == OHLC_BARS) {
                // open tick on the left (or top), close tick on the right (or bottom)
                rect(g, rot, cx - body / 2, po - wick / 2, body / 2, wick);
                rect(g, rot, cx, pc - wick / 2, body / 2, wick);
                continue;
            }
            rect(g, rot, cx - body / 2, Math.min(po, pc), body, Math.max(1, Math.abs(po - pc)));
        }
        g.setAlpha(alpha);
    }

    // the row as painted this frame: each price at its animated value. A Heikin-Ashi row is derived
    // from every earlier candle, so it takes the raw candle's animated offset per price rather than
    // being re-derived (and reallocated) each frame
    private double[] paintedRow(ChartContext ctx, int i, double[] v, boolean ha) {
        double[] out = paintBuf;
        double ac = animatedY(ctx, i);
        double ao = animatedAux(ctx, OPEN, i);
        double ah = animatedAux(ctx, HIGH, i);
        double al = animatedAux(ctx, LOW, i);
        out[0] = ha ? v[0] + ao - getOpen(i) : ao;
        out[1] = ha ? v[1] + ah - getHigh(i) : ah;
        out[2] = ha ? v[2] + al - getLow(i) : al;
        out[3] = ha ? v[3] + ac - getClose(i) : ac;
        return out;
    }

    private final double[] paintBuf = new double[4];

    // a value's pixel along the value axis: y normally, x when rotated
    private float pos(ChartContext ctx, double x, double v, boolean rot) {
        return rot ? ctx.screenX(this, x, v) : ctx.screenY(this, x, v);
    }

    // a rectangle given as (category position, value position, category extent, value extent)
    private static void rect(Graphics g, boolean rot, int a, int b, int w, int h) {
        if (rot) {
            g.fillRect(b, a, h, w);
        } else {
            g.fillRect(a, b, w, h);
        }
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        int[] r = visibleRange(ctx);
        float slot = ctx.xLength(getMinXSpacing());
        int ax = x + ctx.getOriginX();
        int ay = y + ctx.getOriginY();
        for (int i = r[0]; i <= r[1]; i++) {
            // the drawn row: Heikin-Ashi candles span their averaged high and low, not the raw ones,
            // and mid transition each price is where it is painted, not where it is going
            double[] raw = row(i);
            if (isGap(i, raw)) {
                continue;
            }
            double[] v = paintedRow(ctx, i, raw, style == HEIKIN_ASHI);
            boolean rot = ctx.isRotated();
            float cx = rot ? ctx.screenY(this, getX(i), v[3]) : ctx.screenX(this, getX(i), v[3]);
            if (Math.abs((rot ? ay : ax) - cx) > Math.max(slot / 2, radius / 2f)) {
                continue;
            }
            float ph = pos(ctx, getX(i), v[1], rot);
            float pl = pos(ctx, getX(i), v[2], rot);
            float pv = rot ? ax : ay;
            if (pv >= Math.min(ph, pl) - radius && pv <= Math.max(ph, pl) + radius) {
                return getPoint(ctx, i);
            }
        }
        return null;
    }

    @Override
    public String formatTooltip(ChartContext ctx, ChartPoint p) {
        int i = p.getIndex();
        double[] v = row(i);
        return "O " + formatValue(ctx, v[0]) + "  H " + formatValue(ctx, v[1])
                + "  L " + formatValue(ctx, v[2]) + "  C " + formatValue(ctx, v[3]);
    }

    @Override
    public void paintLegendSymbol(Graphics g, int x, int y, int size) {
        g.setColor(upColor != null ? upColor.intValue()
                : getChart() == null ? 0x009E73 : getChart().getTheme().getPositiveColor());
        g.fillRect(x, y + size / 4, size / 2, size / 2);
        g.setColor(downColor != null ? downColor.intValue()
                : getChart() == null ? 0xD55E00 : getChart().getTheme().getNegativeColor());
        g.fillRect(x + size / 2, y, size / 2, size);
    }

    @Override
    protected String tableValue(int i) {
        return "O " + formatValue(null, getOpen(i)) + " H " + formatValue(null, getHigh(i))
                + " L " + formatValue(null, getLow(i)) + " C " + formatValue(null, getClose(i));
    }
}
