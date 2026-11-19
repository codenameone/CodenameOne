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
package com.codename1.ui.chart.indicator;

import com.codename1.ui.chart.series.CandlestickSeries;
import com.codename1.ui.chart.series.LineSeries;
import com.codename1.ui.chart.series.XYSeries;

/// Technical indicators computed from price arrays.
///
/// Every method returns arrays as long as its input. Values before the indicator has enough
/// history are `NaN`, which line series draw as a gap, so an indicator lines up with its prices
/// index for index.
///
/// ```java
/// double[] close = ...;
/// chart.addSeries(Indicators.line("SMA 20", prices, Indicators.sma(close, 20)));
/// double[][] bb = Indicators.bollinger(close, 20, 2);
/// ```
public final class Indicators {
    private Indicators() {
    }

    /// The simple moving average.
    ///
    /// #### Parameters
    ///
    /// - `v`: the values
    ///
    /// - `period`: the window length
    ///
    /// #### Returns
    ///
    /// the average of each window, `NaN` for the first `period - 1` values
    public static double[] sma(double[] v, int period) {
        checkPeriod(period);
        double[] out = new double[v.length];
        double sum = 0;
        // gap (non-finite) values in the window; while there is one the average is NaN, and it recovers once the
        // gap leaves the window because the gap was never added to the sum
        int gaps = 0;
        for (int i = 0; i < v.length; i++) {
            double x = v[i];
            if (!gap(x)) {
                sum += x;
            } else {
                gaps++;
            }
            if (i >= period) {
                double old = v[i - period];
                if (!gap(old)) {
                    sum -= old;
                } else {
                    gaps--;
                }
            }
            out[i] = i >= period - 1 && gaps == 0 ? sum / period : Double.NaN;
        }
        return out;
    }

    /// The exponential moving average, seeded with the simple average of the first window.
    ///
    /// #### Parameters
    ///
    /// - `v`: the values
    ///
    /// - `period`: the window length
    ///
    /// #### Returns
    ///
    /// the average, `NaN` for the first `period - 1` values
    public static double[] ema(double[] v, int period) {
        checkPeriod(period);
        double[] out = new double[v.length];
        double k = 2.0 / (period + 1);
        double prev = Double.NaN;
        double seed = 0;
        int seen = 0;
        for (int i = 0; i < v.length; i++) {
            double x = v[i];
            if (gap(x)) {
                // a gap (NaN or infinite) is NaN like every other indicator's; the average resumes from where it was
                out[i] = Double.NaN;
                continue;
            }
            if (seen < period) {
                seed += x;
                seen++;
                if (seen == period) {
                    prev = seed / period;
                }
                out[i] = prev;
                continue;
            }
            prev = x * k + prev * (1 - k);
            out[i] = prev;
        }
        return out;
    }

    /// Bollinger bands: a simple moving average with bands a number of standard deviations away.
    ///
    /// #### Parameters
    ///
    /// - `v`: the values
    ///
    /// - `period`: the window length, 20 is customary
    ///
    /// - `deviations`: the band width in standard deviations, 2 is customary
    ///
    /// #### Returns
    ///
    /// `{middle, upper, lower}`
    public static double[][] bollinger(double[] v, int period, double deviations) {
        checkPeriod(period);
        double[] mid = sma(v, period);
        double[] up = new double[v.length];
        double[] lo = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            if (gap(mid[i])) {
                up[i] = Double.NaN;
                lo[i] = Double.NaN;
                continue;
            }
            double ss = 0;
            for (int j = i - period + 1; j <= i; j++) {
                double d = v[j] - mid[i];
                ss += d * d;
            }
            double sd = Math.sqrt(ss / period);
            up[i] = mid[i] + deviations * sd;
            lo[i] = mid[i] - deviations * sd;
        }
        return new double[][]{mid, up, lo};
    }

    /// The relative strength index with Wilder smoothing, between 0 and 100.
    ///
    /// #### Parameters
    ///
    /// - `v`: the closing prices
    ///
    /// - `period`: the window length, 14 is customary
    ///
    /// #### Returns
    ///
    /// the index, `NaN` for the first `period` values
    public static double[] rsi(double[] v, int period) {
        checkPeriod(period);
        double[] out = new double[v.length];
        double gain = 0;
        double loss = 0;
        // consecutive valid changes since the start or the last gap; a gap restarts the seeding, so
        // the output is NaN until `period` changes past it are known, never a blend across it
        int valid = 0;
        for (int i = 0; i < v.length; i++) {
            double ch = i == 0 ? Double.NaN : v[i] - v[i - 1];
            if (gap(ch)) {
                out[i] = Double.NaN;
                valid = 0;
                gain = 0;
                loss = 0;
                continue;
            }
            double g = ch > 0 ? ch : 0;
            double l = ch < 0 ? -ch : 0;
            valid++;
            if (valid <= period) {
                gain += g;
                loss += l;
                if (valid < period) {
                    out[i] = Double.NaN;
                    continue;
                }
                gain /= period;
                loss /= period;
            } else {
                gain = (gain * (period - 1) + g) / period;
                loss = (loss * (period - 1) + l) / period;
            }
            if (loss == 0) {
                // a flat market has neither strength nor weakness: the neutral midpoint, not 100
                out[i] = gain == 0 ? 50 : 100;
            } else {
                out[i] = 100 - 100 / (1 + gain / loss);
            }
        }
        return out;
    }

    /// Moving average convergence divergence.
    ///
    /// #### Parameters
    ///
    /// - `v`: the closing prices
    ///
    /// - `fast`: the fast EMA period, 12 is customary
    ///
    /// - `slow`: the slow EMA period, 26 is customary
    ///
    /// - `signal`: the signal EMA period, 9 is customary
    ///
    /// #### Returns
    ///
    /// `{macd, signal, histogram}`
    public static double[][] macd(double[] v, int fast, int slow, int signal) {
        checkPeriod(fast);
        checkPeriod(slow);
        checkPeriod(signal);
        double[] f = ema(v, fast);
        double[] s = ema(v, slow);
        double[] m = new double[v.length];
        int first = -1;
        for (int i = 0; i < v.length; i++) {
            m[i] = f[i] - s[i];
            if (first < 0 && !gap(m[i])) {
                first = i;
            }
        }
        double[] sig = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            sig[i] = Double.NaN;
        }
        if (first >= 0) {
            double[] tail = new double[v.length - first];
            System.arraycopy(m, first, tail, 0, tail.length);
            double[] e = ema(tail, signal);
            System.arraycopy(e, 0, sig, first, e.length);
        }
        double[] hist = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            hist[i] = m[i] - sig[i];
        }
        return new double[][]{m, sig, hist};
    }

    /// The closing prices of a candlestick series.
    ///
    /// #### Parameters
    ///
    /// - `prices`: the series
    ///
    /// #### Returns
    ///
    /// a new array of closes
    public static double[] closes(CandlestickSeries prices) {
        double[] out = new double[prices.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = prices.getClose(i);
        }
        return out;
    }

    /// A line series of indicator values that shares the x values of `source`.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    ///
    /// - `source`: the series whose x values to reuse
    ///
    /// - `values`: the indicator values, parallel to the source
    ///
    /// #### Returns
    ///
    /// a new line series
    public static LineSeries line(String name, XYSeries source, double[] values) {
        double[] x = new double[values.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i < source.size() ? source.getX(i) : i;
        }
        LineSeries s = new LineSeries(name);
        s.setData(x, values);
        return s;
    }

    /// True for a value that is a gap: NaN or infinite. An infinity added to a running sum would
    /// poison every later output, so it is skipped exactly as NaN is.
    private static boolean gap(double v) {
        return v != v || Double.isInfinite(v);
    }

    private static void checkPeriod(int period) {
        if (period < 1) {
            throw new IllegalArgumentException("An indicator period must be at least 1, got " + period);
        }
    }
}
