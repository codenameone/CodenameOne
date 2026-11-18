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
        return addOhlc(x, y, y, y, y);
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
        aux[HIGH][i] = Math.max(aux[HIGH][i], price);
        aux[LOW][i] = Math.min(aux[LOW][i], price);
        ha = null;
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
            double hc = (o + h + l + c) / 4;
            double prev = i == 0 ? Double.NaN : (out[0][i - 1] + out[3][i - 1]) / 2;
            // after a gap the average starts over, so one missing period does not blank the rest
            double ho = prev == prev ? prev : (o + c) / 2;
            out[0][i] = ho;
            out[1][i] = Math.max(h, Math.max(ho, hc));
            out[2][i] = Math.min(l, Math.min(ho, hc));
            out[3][i] = hc;
        }
        return out;
    }

    // one buffer refilled per call: paint and hit testing read a row and are done with it
    private final double[] rowBuf = new double[4];

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
            if (l == l) {
                min = Math.min(min, l);
            }
            if (h == h) {
                max = Math.max(max, h);
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
        float t = ctx.getProgress();
        float slot = ctx.xLength(getMinXSpacing());
        int body = Math.max(1, Math.round(slot * bodyFraction));
        int wick = Math.max(1, Math.min(ctx.mm(0.25f), body / 4));
        for (int i = r[0]; i <= r[1]; i++) {
            double[] v = row(i);
            if (v[3] != v[3]) {
                continue;
            }
            // grow from the close outward while animating
            double o = v[3] + (v[0] - v[3]) * t;
            double h = v[3] + (v[1] - v[3]) * t;
            double l = v[3] + (v[2] - v[3]) * t;
            boolean rising = v[3] >= v[0];
            g.setColor(rising ? up(ctx) : down(ctx));
            int cx = Math.round(ctx.screenX(this, getX(i), v[3]));
            int yh = Math.round(ctx.screenY(this, getX(i), h));
            int yl = Math.round(ctx.screenY(this, getX(i), l));
            int yo = Math.round(ctx.screenY(this, getX(i), o));
            int yc = Math.round(ctx.screenY(this, getX(i), v[3]));
            if (style == OHLC_BARS) {
                g.fillRect(cx - wick / 2, yh, wick, Math.max(1, yl - yh));
                g.fillRect(cx - body / 2, yo - wick / 2, body / 2, wick);
                g.fillRect(cx, yc - wick / 2, body / 2, wick);
                continue;
            }
            g.fillRect(cx - wick / 2, yh, wick, Math.max(1, yl - yh));
            int top = Math.min(yo, yc);
            int hgt = Math.max(1, Math.abs(yo - yc));
            g.fillRect(cx - body / 2, top, body, hgt);
        }
        g.setAlpha(alpha);
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        int[] r = visibleRange(ctx);
        float slot = ctx.xLength(getMinXSpacing());
        int ax = x + ctx.getOriginX();
        int ay = y + ctx.getOriginY();
        for (int i = r[0]; i <= r[1]; i++) {
            float cx = ctx.screenX(this, getX(i), getY(i));
            if (Math.abs(ax - cx) > Math.max(slot / 2, radius / 2f)) {
                continue;
            }
            float yh = ctx.screenY(this, getX(i), getHigh(i));
            float yl = ctx.screenY(this, getX(i), getLow(i));
            if (ay >= yh - radius && ay <= yl + radius) {
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
        g.setColor(getChart() == null ? 0x009E73 : getChart().getTheme().getPositiveColor());
        g.fillRect(x, y + size / 4, size / 2, size / 2);
        g.setColor(getChart() == null ? 0xD55E00 : getChart().getTheme().getNegativeColor());
        g.fillRect(x + size / 2, y, size / 2, size);
    }

    @Override
    protected String tableValue(int i) {
        return "O " + formatValue(null, getOpen(i)) + " H " + formatValue(null, getHigh(i))
                + " L " + formatValue(null, getLow(i)) + " C " + formatValue(null, getClose(i));
    }
}
