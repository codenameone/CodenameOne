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

/// Times typed as `YYYY-MM-DD HH:MM` in UTC, the way the publishing schedule
/// is entered. Plain arithmetic on the civil calendar, so it reads the same on
/// every device whatever its time zone.
final class UtcTimes {
    private static final long MINUTE = 60000L;
    private static final long DAY = 86400000L;

    private UtcTimes() {
    }

    /// `millis` as `YYYY-MM-DD HH:MM`, empty for 0 or less.
    static String format(long millis) {
        if (millis <= 0) {
            return "";
        }
        long days = millis / DAY;
        long minutes = (millis % DAY) / MINUTE;
        // Days since 1970-01-01 to a civil date (H. Hinnant's algorithm).
        long z = days + 719468;
        long era = z / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        long y = yoe + era * 400 + (m <= 2 ? 1 : 0);
        StringBuilder b = new StringBuilder();
        b.append(y).append('-');
        two(b, m).append('-');
        two(b, d).append(' ');
        two(b, minutes / 60).append(':');
        two(b, minutes % 60);
        return b.toString();
    }

    private static StringBuilder two(StringBuilder b, long n) {
        if (n < 10) {
            b.append('0');
        }
        return b.append(n);
    }

    /// `YYYY-MM-DD HH:MM` (or `YYYY-MM-DDTHH:MM`, or a date alone for
    /// midnight) as milliseconds; 0 for empty text and -1 for anything else.
    static long parse(String text) {
        String t = text == null ? "" : text.trim();
        if (t.length() == 0) {
            return 0;
        }
        if (t.length() != 10 && t.length() != 16) {
            return -1;
        }
        long y = digits(t, 0, 4);
        long m = digits(t, 5, 2);
        long d = digits(t, 8, 2);
        long h = 0;
        long min = 0;
        if (t.charAt(4) != '-' || t.charAt(7) != '-') {
            return -1;
        }
        if (t.length() == 16) {
            char sep = t.charAt(10);
            if (sep != ' ' && sep != 'T' || t.charAt(13) != ':') {
                return -1;
            }
            h = digits(t, 11, 2);
            min = digits(t, 14, 2);
        }
        if (y < 1970 || m < 1 || m > 12 || d < 1 || d > monthDays(y, m) || h < 0 || h > 23
                || min < 0 || min > 59) {
            return -1;
        }
        // A civil date to days since 1970-01-01.
        long yy = m <= 2 ? y - 1 : y;
        long era = yy / 400;
        long yoe = yy - era * 400;
        long doy = (153 * (m > 2 ? m - 3 : m + 9) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        long days = era * 146097 + doe - 719468;
        return days * DAY + (h * 60 + min) * MINUTE;
    }

    private static long monthDays(long y, long m) {
        if (m == 2) {
            return y % 4 == 0 && (y % 100 != 0 || y % 400 == 0) ? 29 : 28;
        }
        return m == 4 || m == 6 || m == 9 || m == 11 ? 30 : 31;
    }

    /// The number in `count` digits at `at`, -1 when one is not a digit.
    private static long digits(String t, int at, int count) {
        long out = 0;
        for (int iter = at; iter < at + count; iter++) {
            char c = t.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }
}
