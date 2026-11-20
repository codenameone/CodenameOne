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
/// **Live data.** A streaming series appends with `XYSeries.add(y)`, which gives each new bar the
/// next index, and with `setMaxPoints(n)` drops its oldest bar, so its first index climbs past 0.
/// Follow it by calling `addTime(long)` with each bar's timestamp, and `setMaxTimes(n)` with the
/// same `n`: the axis then remembers the index of its first stored timestamp
/// (`getFirstIndex()`) and labels the climbing indices with the times that belong to them.
///
/// ```java
/// Chart chart = new Chart();
/// long day = 24L * 60 * 60 * 1000;
/// long[] timestamps = {0, day, 4 * day};
/// double[] open = {10, 11, 12}, high = {12, 13, 14}, low = {9, 10, 11}, close = {11, 12, 13};
/// SessionTimeAxis axis = new SessionTimeAxis(Axis.BOTTOM, timestamps);
/// chart.setXAxis(axis);
/// chart.addSeries(new CandlestickSeries("ACME").setOhlc(null, open, high, low, close));
/// ```
public class SessionTimeAxis extends Axis {
    private long[] times;
    private int count;
    private int firstIndex;
    private int maxTimes;
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
    /// An axis does not know the chart it is on, so the chart is not repainted by this call,
    /// as with `CategoryAxis.setCategories`: call `repaint()` on the chart, or change the
    /// series data in the same step, which repaints it.
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
        this.times = new long[Math.max(16, n)];
        if (n > 0) {
            System.arraycopy(times, 0, this.times, 0, n);
        }
        count = n;
        firstIndex = 0;
        trim();
        return this;
    }

    /// Appends the timestamp of the next bar, for a series that grows with `XYSeries.add(y)`.
    ///
    /// An axis does not know the chart it is on, so the chart is not repainted by this call,
    /// as with `CategoryAxis.setCategories`: call `repaint()` on the chart, or change the
    /// series data in the same step, which repaints it.
    ///
    /// #### Parameters
    ///
    /// - `time`: the new bar's timestamp in milliseconds, not before the previous one
    ///
    /// #### Returns
    ///
    /// this axis
    public SessionTimeAxis addTime(long time) {
        if (count == times.length) {
            long[] n = new long[times.length * 2];
            System.arraycopy(times, 0, n, 0, count);
            times = n;
        }
        times[count++] = time;
        trim();
        return this;
    }

    /// Keeps only the newest `n` timestamps, dropping the oldest as `addTime` appends, the way
    /// `XYSeries.setMaxPoints(n)` drops the oldest bar. Each dropped timestamp moves
    /// `getFirstIndex()` up by one, so the remaining bars keep their indices. Like `setTimes`,
    /// this does not repaint the chart.
    ///
    /// #### Parameters
    ///
    /// - `n`: the maximum number of timestamps, or 0 for no limit
    ///
    /// #### Returns
    ///
    /// this axis
    public SessionTimeAxis setMaxTimes(int n) {
        maxTimes = n;
        trim();
        return this;
    }

    /// The bar index of the oldest stored timestamp: 0 until `setMaxTimes` drops one.
    ///
    /// #### Returns
    ///
    /// the index of the first timestamp
    public int getFirstIndex() {
        return firstIndex;
    }

    private void trim() {
        if (maxTimes > 0 && count > maxTimes) {
            int drop = count - maxTimes;
            System.arraycopy(times, drop, times, 0, maxTimes);
            count = maxTimes;
            firstIndex += drop;
        }
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
        int n = count;
        if (n == 0) {
            return 0;
        }
        int i = (int) Math.rint(index) - firstIndex;
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
        super.setDataBounds(Math.min(min, firstIndex - 0.5), Math.max(max, firstIndex + count - 0.5));
    }

    @Override
    protected double[] adjustBounds(double min, double max) {
        return new double[]{min, max};
    }

    @Override
    public double[] ticks(int maxCount) {
        int n = count;
        if (n == 0) {
            return new double[0];
        }
        // clamped in double before the cast: a range zoomed or panned far past the data would
        // saturate the int cast and overflow on subtracting firstIndex, leaving no ticks
        double vlo = Math.ceil(getVisibleMin()) - firstIndex;
        double vhi = Math.floor(getVisibleMax()) - firstIndex;
        int lo = vlo > 0 ? (int) Math.min(n, vlo) : 0;
        int hi = vhi < n - 1 ? (int) Math.max(-1, vhi) : n - 1;
        if (hi < lo) {
            return new double[0];
        }
        int[] field = new int[1];
        // gaps make calendar time longer than the bars it covers, so ask for a few more ticks
        double[] t = TimeAxis.timeTicks(times[lo], times[hi], maxCount + 2, field);
        lastField = field[0];
        double[] tmp = new double[t.length + 1];
        int found = 0;
        int last = -1;
        int minGap = Math.max(1, (hi - lo + 1) / Math.max(1, maxCount + 1));
        for (double tick : t) {
            int idx = firstAtOrAfter((long) tick, lo, hi);
            if (idx < 0 || idx == last || last >= 0 && idx - last < minGap) {
                continue;
            }
            tmp[found++] = idx + firstIndex;
            last = idx;
        }
        double[] out = new double[found];
        System.arraycopy(tmp, 0, out, 0, found);
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
