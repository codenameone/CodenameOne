/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.ops;

/// Demand forecasting and stock suggestions in integer arithmetic.
///
/// Demand is units per day in hundredths, so 0.25 a day is 25. The base rate
/// is the moving average of the last [#WINDOW] days of sales; a weekday's
/// factor (basis points, 10000 = average) is how that weekday sold against
/// the average, which is the weekly seasonality. A day of the forecast is the
/// base times its weekday's factor.
///
/// Days are indexed as whole days since the epoch; weekday is `(day + 3) % 7`
/// with 0 = Monday, since 1970-01-01 was a Thursday.
public final class Forecast {
    /// Days of history the moving average reads.
    public static final int WINDOW = 28;
    /// Days of cover a reorder aims for.
    public static final int TARGET_DAYS = 30;
    /// Below this cover a location is short.
    public static final int LOW_DAYS = 14;
    /// Above this cover a location has stock to spare.
    public static final int HIGH_DAYS = 60;
    /// The cover reported when nothing sells.
    public static final int NO_DEMAND = 9999;

    private Forecast() {
    }

    /// Monday = 0 .. Sunday = 6 for a day index.
    public static int weekday(long day) {
        long w = (day + 3) % 7;
        return (int) (w < 0 ? w + 7 : w);
    }

    /// The base rate in hundredths per day: the mean of `units`.
    public static long baseX100(int[] units) {
        if (units == null || units.length == 0) {
            return 0;
        }
        long sum = 0;
        for (int iter = 0; iter < units.length; iter++) {
            sum += Math.max(0, units[iter]);
        }
        return sum * 100 / units.length;
    }

    /// Each weekday's factor in basis points; `units[i]` sold on day
    /// `lastDay - units.length + 1 + i`. A weekday with no history, or no
    /// sales at all, is 10000.
    public static int[] weekdayFactors(int[] units, long lastDay) {
        int[] factors = new int[7];
        long[] sums = new long[7];
        int[] counts = new int[7];
        long total = 0;
        int n = units == null ? 0 : units.length;
        for (int iter = 0; iter < n; iter++) {
            int wd = weekday(lastDay - n + 1 + iter);
            sums[wd] += Math.max(0, units[iter]);
            counts[wd]++;
            total += Math.max(0, units[iter]);
        }
        for (int wd = 0; wd < 7; wd++) {
            if (total == 0 || counts[wd] == 0) {
                factors[wd] = 10000;
            } else {
                // (sum/count) / (total/n), scaled to basis points.
                factors[wd] = (int) (sums[wd] * n * 10000L / ((long) counts[wd] * total));
            }
        }
        return factors;
    }

    /// Units forecast for the `days` after `lastDay`, in hundredths.
    public static long forecastX100(int[] units, long lastDay, int days) {
        long base = baseX100(units);
        int[] f = weekdayFactors(units, lastDay);
        long out = 0;
        for (int d = 1; d <= days; d++) {
            out += base * f[weekday(lastDay + d)] / 10000;
        }
        return out;
    }

    /// Days the available stock lasts at `dailyX100`; [#NO_DEMAND] when
    /// nothing sells.
    public static int daysOfCover(long available, long dailyX100) {
        if (dailyX100 <= 0) {
            return NO_DEMAND;
        }
        long d = Math.max(0, available) * 100 / dailyX100;
        return (int) Math.min(NO_DEMAND, d);
    }

    /// Units to order so that available plus incoming covers [#TARGET_DAYS];
    /// 0 when the location is not short.
    public static int reorderQuantity(long available, long incoming, long dailyX100) {
        if (dailyX100 <= 0 || daysOfCover(available + incoming, dailyX100) >= LOW_DAYS) {
            return 0;
        }
        long want = (dailyX100 * TARGET_DAYS + 99) / 100;
        return (int) Math.max(0, want - Math.max(0, available) - Math.max(0, incoming));
    }

    /// One move of stock between two locations; see [#rebalance].
    public static final class Move {
        public final int from;
        public final int to;
        public final int quantity;

        public Move(int from, int to, int quantity) {
            this.from = from;
            this.to = to;
            this.quantity = quantity;
        }
    }

    /// Moves from locations with more than [#HIGH_DAYS] of cover to those
    /// under [#LOW_DAYS], largest need first; a donor keeps [#HIGH_DAYS]
    /// (or everything, if nothing sells there it keeps nothing back beyond a
    /// zero cover) and a taker is brought up to [#TARGET_DAYS].
    public static java.util.List<Move> rebalance(long[] available, long[] dailyX100) {
        java.util.List<Move> out = new java.util.ArrayList<Move>();
        int n = available == null ? 0 : available.length;
        long[] spare = new long[n];
        long[] need = new long[n];
        for (int iter = 0; iter < n; iter++) {
            long a = Math.max(0, available[iter]);
            long d = dailyX100[iter];
            if (daysOfCover(a, d) > HIGH_DAYS) {
                long keep = d <= 0 ? 0 : (d * HIGH_DAYS + 99) / 100;
                spare[iter] = Math.max(0, a - keep);
            } else if (d > 0 && daysOfCover(a, d) < LOW_DAYS) {
                need[iter] = Math.max(0, (d * TARGET_DAYS + 99) / 100 - a);
            }
        }
        while (true) {
            int taker = -1;
            int giver = -1;
            for (int iter = 0; iter < n; iter++) {
                if (need[iter] > 0 && (taker < 0 || need[iter] > need[taker])) {
                    taker = iter;
                }
                if (spare[iter] > 0 && (giver < 0 || spare[iter] > spare[giver])) {
                    giver = iter;
                }
            }
            if (taker < 0 || giver < 0) {
                return out;
            }
            long q = Math.min(need[taker], spare[giver]);
            out.add(new Move(giver, taker, (int) q));
            need[taker] -= q;
            spare[giver] -= q;
        }
    }
}
