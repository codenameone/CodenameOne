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
package com.codenameone.examples.pulse;

/// Days, counted in UTC.
///
/// The charts and the earnings are by day, and a day has to mean the same on
/// SQLite, PostgreSQL and MySQL, whose date functions agree on nothing. So the
/// tables hold milliseconds since the epoch, and the arithmetic is done here.
/// UTC throughout: a service in one city would shift by that city's offset.
public final class Days {
    public static final long MILLIS = 86400000L;

    private Days() {
    }

    /// The number of the day `millis` falls in: whole days since the epoch.
    public static long number(long millis) {
        return millis / MILLIS;
    }

    /// The first millisecond of the day `millis` falls in.
    public static long start(long millis) {
        return millis - millis % MILLIS;
    }

    /// The stretch of time from `from` to `to` cut at each midnight: the
    /// number of each day it touches followed by how many of its milliseconds
    /// fall in that day, day after day. Empty when `to` is not after `from`.
    ///
    /// Time on line is credited to the days it was spent in. Credited whole
    /// to the day it ended in, a courier on line across midnight had the
    /// minutes before it counted as the next day's.
    public static long[] split(long from, long to) {
        if (to <= from) {
            return new long[0];
        }
        long[] out = new long[(int) (number(to - 1) - number(from) + 1) * 2];
        long at = from;
        for (int iter = 0; iter < out.length; iter += 2) {
            long end = Math.min(to, start(at) + MILLIS);
            out[iter] = number(at);
            out[iter + 1] = end - at;
            at = end;
        }
        return out;
    }

    /// The day `millis` falls in, as `yyyy-MM-dd`.
    public static String iso(long millis) {
        // Days since the epoch to a civil date, by the usual era arithmetic:
        // shift the year to start in March so the leap day is its last day.
        long z = number(millis) + 719468L;
        long era = z / 146097L;
        long dayOfEra = z - era * 146097L;
        long yearOfEra = (dayOfEra - dayOfEra / 1460L + dayOfEra / 36524L - dayOfEra / 146096L)
                / 365L;
        long dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L);
        long shifted = (5L * dayOfYear + 2L) / 153L;
        long day = dayOfYear - (153L * shifted + 2L) / 5L + 1L;
        long month = shifted < 10L ? shifted + 3L : shifted - 9L;
        long year = yearOfEra + era * 400L + (month <= 2L ? 1L : 0L);
        StringBuilder out = new StringBuilder(10);
        out.append(year).append('-');
        if (month < 10L) {
            out.append('0');
        }
        out.append(month).append('-');
        if (day < 10L) {
            out.append('0');
        }
        return out.append(day).toString();
    }

    /// Whether `text` is a date written `yyyy-MM-dd`, with a month and a day
    /// that could exist.
    public static boolean isIso(String text) {
        if (text == null || text.length() != 10 || text.charAt(4) != '-' || text.charAt(7) != '-') {
            return false;
        }
        for (int iter = 0; iter < 10; iter++) {
            char c = text.charAt(iter);
            if (iter != 4 && iter != 7 && (c < '0' || c > '9')) {
                return false;
            }
        }
        int year = 0;
        for (int iter = 0; iter < 4; iter++) {
            year = year * 10 + (text.charAt(iter) - '0');
        }
        int month = (text.charAt(5) - '0') * 10 + (text.charAt(6) - '0');
        int day = (text.charAt(8) - '0') * 10 + (text.charAt(9) - '0');
        // The day is held to its own month: these strings are compared as
        // text and never parsed, so a 31st of February accepted here would be
        // stored, and approved, as somebody's date of birth.
        return month >= 1 && month <= 12 && day >= 1 && day <= length(year, month);
    }

    // How many days a month has, by the Gregorian rule for leap years.
    private static int length(int year, int month) {
        if (month == 2) {
            boolean leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
            return leap ? 29 : 28;
        }
        return month == 4 || month == 6 || month == 9 || month == 11 ? 30 : 31;
    }
}
