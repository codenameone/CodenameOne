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
package com.codename1.ui.chart.axis;

import com.codename1.l10n.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;

/// A time axis that skips the gaps between sessions: weekends, holidays, overnight hours.
///
/// Series on this axis use the **index** of each bar as their x value (0, 1, 2...) and the axis
/// maps every index to its timestamp for labelling, the way trading charts do. Each bar is one
/// slot wide no matter how much time separates it from the previous one, so a Monday candle sits
/// right next to Friday's.
///
/// ```java
/// SessionTimeAxis axis = new SessionTimeAxis(Axis.BOTTOM, timestamps);
/// chart.setXAxis(axis);
/// chart.addSeries(new CandlestickSeries("ACME").setOhlc(null, open, high, low, close));
/// ```
public class SessionTimeAxis extends Axis {
    private long[] times;
    private int lastField = Calendar.DAY_OF_MONTH;

    /// Creates the axis.
    ///
    /// #### Parameters
    ///
    /// - `position`: one of the `Axis` position constants
    ///
    /// - `times`: the timestamp of each bar in milliseconds, ascending
    public SessionTimeAxis(int position, long[] times) {
        super(position);
        setTimes(times);
    }

    /// Replaces the timestamps.
    ///
    /// #### Parameters
    ///
    /// - `times`: the timestamp of each bar in milliseconds, ascending
    ///
    /// #### Returns
    ///
    /// this axis
    public SessionTimeAxis setTimes(long[] times) {
        int n = times == null ? 0 : times.length;
        this.times = new long[n];
        if (n > 0) {
            System.arraycopy(times, 0, this.times, 0, n);
        }
        return this;
    }

    /// The timestamp of bar `index`, extrapolated past either end.
    ///
    /// #### Parameters
    ///
    /// - `index`: the bar index
    ///
    /// #### Returns
    ///
    /// milliseconds since the epoch
    public long timeAt(double index) {
        int n = times.length;
        if (n == 0) {
            return 0;
        }
        int i = (int) Math.rint(index);
        if (i < 0) {
            return times[0];
        }
        if (i >= n) {
            return times[n - 1];
        }
        return times[i];
    }

    @Override
    public void setDataBounds(double min, double max) {
        // bars are padded by half a slot; keep the full set of indices visible
        super.setDataBounds(Math.min(min, -0.5), Math.max(max, times.length - 0.5));
    }

    @Override
    protected double[] adjustBounds(double min, double max) {
        return new double[]{min, max};
    }

    @Override
    public double[] ticks(int maxCount) {
        int n = times.length;
        if (n == 0) {
            return new double[0];
        }
        int lo = Math.max(0, (int) Math.ceil(getVisibleMin()));
        int hi = Math.min(n - 1, (int) Math.floor(getVisibleMax()));
        if (hi < lo) {
            return new double[0];
        }
        int[] field = new int[1];
        // gaps make calendar time longer than the bars it covers, so ask for a few more ticks
        double[] t = TimeAxis.timeTicks(times[lo], times[hi], maxCount + 2, field);
        lastField = field[0];
        double[] tmp = new double[t.length + 1];
        int count = 0;
        int last = -1;
        int minGap = Math.max(1, (hi - lo + 1) / Math.max(1, maxCount + 1));
        for (double tick : t) {
            int idx = firstAtOrAfter((long) tick, lo, hi);
            if (idx < 0 || idx == last || last >= 0 && idx - last < minGap) {
                continue;
            }
            tmp[count++] = idx;
            last = idx;
        }
        double[] out = new double[count];
        System.arraycopy(tmp, 0, out, 0, count);
        return out;
    }

    private int firstAtOrAfter(long t, int lo, int hi) {
        int a = lo;
        int b = hi + 1;
        while (a < b) {
            int m = (a + b) >>> 1;
            if (times[m] < t) {
                a = m + 1;
            } else {
                b = m;
            }
        }
        return a > hi ? -1 : a;
    }

    private SimpleDateFormat formatter;
    private String formatterPattern;

    @Override
    protected String defaultFormat(double value, double step) {
        String pattern = step <= 0 ? "MMM d, yyyy" : TimeAxis.patternFor(lastField);
        if (formatter == null || !pattern.equals(formatterPattern)) {
            // one formatter per pattern, not one per label per frame
            formatter = new SimpleDateFormat(pattern);
            formatterPattern = pattern;
        }
        return formatter.format(new Date(timeAt(value)));
    }
}
