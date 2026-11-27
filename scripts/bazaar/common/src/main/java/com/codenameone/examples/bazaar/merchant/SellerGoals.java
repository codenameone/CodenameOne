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
package com.codenameone.examples.bazaar.merchant;

/// The seller's goals, streak and milestones, worked out from sales by day.
/// Pure arithmetic with no UI, so it is tested without a display.
///
/// `daily` is cents per day, oldest first, the last entry today.
final class SellerGoals {
    /// No milestone.
    static final String NONE = "";
    /// The first sale of the day.
    static final String FIRST_SALE = "first";
    /// Today beat every earlier day of the window.
    static final String RECORD_DAY = "record";
    /// The order count crossed a multiple of 100.
    static final String HUNDREDS = "hundreds";

    private SellerGoals() {
    }

    /// A daily goal a little above the usual day: the average of the days
    /// before today with a sale, plus a fifth, rounded up to a round amount
    /// ($5 steps under $100, $10 under $1000, $50 above). A store with no
    /// history gets $100.
    static long dailyGoal(long[] daily) {
        long sum = 0;
        int days = 0;
        for (int iter = 0; iter < daily.length - 1; iter++) {
            if (daily[iter] > 0) {
                sum += daily[iter];
                days++;
            }
        }
        if (days == 0) {
            return 10000;
        }
        long target = sum / days * 6 / 5;
        long step = target < 10000 ? 500 : target < 100000 ? 1000 : 5000;
        return Math.max(step, (target + step - 1) / step * step);
    }

    /// The week's goal: seven days of [#dailyGoal].
    static long weeklyGoal(long[] daily) {
        return dailyGoal(daily) * 7;
    }

    /// Sales of the last seven days, today included.
    static long week(long[] daily) {
        long sum = 0;
        for (int iter = Math.max(0, daily.length - 7); iter < daily.length; iter++) {
            sum += daily[iter];
        }
        return sum;
    }

    /// `value` of `goal` in basis points, never above 10000.
    static int progressBp(long value, long goal) {
        if (goal <= 0 || value <= 0) {
            return 0;
        }
        return (int) Math.min(10000L, value * 10000L / goal);
    }

    /// Days in a row with a sale, ending today -- or yesterday when today has
    /// no sale yet, so the streak does not read zero every morning.
    static int streak(long[] daily) {
        int end = daily.length - 1;
        if (end >= 0 && daily[end] <= 0) {
            end--;
        }
        int n = 0;
        for (int iter = end; iter >= 0 && daily[iter] > 0; iter--) {
            n++;
        }
        return n;
    }

    /// The milestone today has reached, the most notable first: a crossed
    /// hundred of orders, a record day, the first sale of the day.
    ///
    /// @param ordersBefore the store's order count before the newest orders
    /// @param ordersNow the count now
    static String milestone(long[] daily, int todayOrders, int ordersBefore, int ordersNow) {
        if (ordersNow >= 100 && ordersNow / 100 > ordersBefore / 100) {
            return HUNDREDS;
        }
        int last = daily.length - 1;
        if (last >= 1 && daily[last] > 0) {
            long best = 0;
            int withSales = 0;
            for (int iter = 0; iter < last; iter++) {
                best = Math.max(best, daily[iter]);
                if (daily[iter] > 0) {
                    withSales++;
                }
            }
            if (withSales >= 3 && daily[last] > best) {
                return RECORD_DAY;
            }
        }
        if (todayOrders >= 1) {
            return FIRST_SALE;
        }
        return NONE;
    }

    /// The value an animated counter shows `elapsed` of `duration` millis in:
    /// an ease-out from 0, exactly `target` at the end.
    static long counted(long target, long elapsed, long duration) {
        if (duration <= 0 || elapsed >= duration) {
            return target;
        }
        if (elapsed <= 0) {
            return 0;
        }
        double t = elapsed / (double) duration;
        double eased = 1 - (1 - t) * (1 - t) * (1 - t);
        return Math.round(target * eased);
    }
}
