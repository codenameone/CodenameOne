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

/// A reporting period, `[from, to)` in milliseconds cut on UTC midnights as
/// the server cuts its days, and the period before it that the server
/// compares with: as long, and ending where this one starts.
///
/// Nothing here draws or translates, so the arithmetic is tested alone.
final class Period {
    static final long DAY = 86400000L;
    /// The longest period the server reports on.
    static final int MAX_DAYS = 400;

    static final String TODAY = "today";
    static final String YESTERDAY = "yesterday";
    static final String LAST_7 = "7d";
    static final String LAST_30 = "30d";
    static final String LAST_90 = "90d";
    static final String MONTH = "month";
    static final String YEAR = "year";
    /// The presets, in the order they are offered.
    static final String[] PRESETS = {TODAY, YESTERDAY, LAST_7, LAST_30, LAST_90, MONTH, YEAR};
    /// What each preset is called, untranslated.
    static final String[] LABELS = {"Today", "Yesterday", "Last 7 days", "Last 30 days",
        "Last 90 days", "This month", "This year"};

    final long from;
    final long to;

    Period(long from, long to) {
        this.from = from;
        this.to = Math.max(to, from + DAY);
    }

    /// The UTC midnight `millis` falls after.
    static long start(long millis) {
        return millis - ((millis % DAY) + DAY) % DAY;
    }

    /// The preset `code` as of `now`; an unknown code is the last 30 days.
    static Period preset(String code, long now) {
        long today = start(now);
        long tomorrow = today + DAY;
        if (TODAY.equals(code)) {
            return new Period(today, tomorrow);
        }
        if (YESTERDAY.equals(code)) {
            return new Period(today - DAY, today);
        }
        if (LAST_7.equals(code)) {
            return new Period(tomorrow - 7 * DAY, tomorrow);
        }
        if (LAST_90.equals(code)) {
            return new Period(tomorrow - 90 * DAY, tomorrow);
        }
        if (MONTH.equals(code)) {
            int[] ymd = civil(today / DAY);
            return new Period(days(ymd[0], ymd[1], 1) * DAY, tomorrow);
        }
        if (YEAR.equals(code)) {
            int[] ymd = civil(today / DAY);
            return new Period(days(ymd[0], 1, 1) * DAY, tomorrow);
        }
        return new Period(tomorrow - 30 * DAY, tomorrow);
    }

    /// A period of whole days from the day of `fromMillis` through the day of
    /// `toMillis`, inclusive, swapped when given backwards and capped at
    /// [#MAX_DAYS].
    static Period custom(long fromMillis, long toMillis) {
        long a = start(Math.min(fromMillis, toMillis));
        long b = start(Math.max(fromMillis, toMillis)) + DAY;
        if (b - a > MAX_DAYS * DAY) {
            a = b - MAX_DAYS * DAY;
        }
        return new Period(a, b);
    }

    /// The period the server compares this one with.
    Period previous() {
        return new Period(from - (to - from), from);
    }

    int days() {
        long d = (to - from + DAY - 1) / DAY;
        return (int) Math.max(1, d);
    }

    String fromParam() {
        return Long.toString(from);
    }

    String toParam() {
        return Long.toString(to);
    }

    // ------------------------------------------------------------------ deltas

    /// The change from `before` to `now` in basis points of `before`;
    /// `Integer.MIN_VALUE` when there is nothing to compare with.
    static int deltaBp(long now, long before) {
        if (before == 0) {
            return now == 0 ? 0 : Integer.MIN_VALUE;
        }
        long bp = (now - before) * 10000L / Math.abs(before);
        return (int) Math.max(-10000000L, Math.min(10000000L, bp));
    }

    /// A change as people read it: "+12.5%", "-3%", "0%", or "New" when the
    /// period before had none.
    static String delta(long now, long before) {
        int bp = deltaBp(now, before);
        if (bp == Integer.MIN_VALUE) {
            return "New";
        }
        return (bp > 0 ? "+" : "") + percent(bp);
    }

    /// Basis points as a percentage with at most one decimal: 1250 is
    /// "12.5%", -300 is "-3%".
    static String percent(int bp) {
        boolean negative = bp < 0;
        long tenths = (Math.abs((long) bp) + 5) / 10;
        String whole = Long.toString(tenths / 10);
        long frac = tenths % 10;
        return (negative && tenths != 0 ? "-" : "") + whole + (frac == 0 ? "" : "." + frac) + "%";
    }

    /// Whether a change is good news: up, except for refunds and discounts,
    /// where down is.
    static boolean good(long now, long before, boolean lowerIsBetter) {
        return lowerIsBetter ? now <= before : now >= before;
    }

    // ------------------------------------------------------------ the calendar

    /// Year, month (1-12) and day of the `epochDay`-th day after 1970-01-01.
    static int[] civil(long epochDay) {
        long z = epochDay + 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        return new int[] {(int) (m <= 2 ? y + 1 : y), (int) m, (int) d};
    }

    /// The epoch day of a date.
    static long days(int year, int month, int day) {
        long y = month <= 2 ? year - 1 : year;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long mp = month > 2 ? month - 3 : month + 9;
        long doy = (153 * mp + 2) / 5 + day - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    /// `yyyy-MM-dd` of the day `millis` falls in.
    static String iso(long millis) {
        int[] ymd = civil(start(millis) / DAY);
        return ymd[0] + "-" + (ymd[1] < 10 ? "0" : "") + ymd[1] + "-" + (ymd[2] < 10 ? "0" : "")
                + ymd[2];
    }
}
