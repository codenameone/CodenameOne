/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.backend;

/// UTC civil-date arithmetic on the proleptic Gregorian calendar, shared by the
/// backend's hand-written date formats: log timestamps, HTTP dates, plist
/// dates, AWS timestamps and X.509 times.
///
/// Hand-written rather than java.time on purpose: the translated runtime's date
/// parsing is locale-aware, and the log path must not allocate. Nothing here
/// parses or validates a format; each caller keeps its own separators and
/// error behaviour and takes only the arithmetic from this class.
public final class CivilTime {
    private CivilTime() {
    }

    /// Floor division. JavaAPI's Math has no floorDiv, and Java's `/` truncates
    /// toward zero, which gives the wrong day before 1970.
    public static long floorDiv(long x, long y) {
        long q = x / y;
        if (x % y != 0 && (x < 0) != (y < 0)) {
            q--;
        }
        return q;
    }

    /// Whether `y` is a Gregorian leap year.
    public static boolean isLeap(long y) {
        return y % 4 == 0 && (y % 100 != 0 || y % 400 == 0);
    }

    /// The length of month `m` (1 to 12) of year `y`.
    public static int daysInMonth(long y, int m) {
        switch (m) {
            case 2:
                return isLeap(y) ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    /// Days since 1970-01-01 of the date `y-m-d` (Howard Hinnant's
    /// days_from_civil). It does not validate: day 31 of February is
    /// normalised into March, so a caller checks `daysInMonth` first.
    public static long daysFromCivil(long y, int m, int d) {
        long yy = m <= 2 ? y - 1 : y;
        long era = floorDiv(yy, 400L);
        long yoe = yy - era * 400L;
        long mp = m > 2 ? m - 3 : m + 9;
        long doy = (153 * mp + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097L + doe - 719468L;
    }

    /// Writes the civil date of `days` since 1970-01-01 into `out` as year,
    /// month (1 to 12) and day (1 to 31), so a caller can reuse one array.
    public static void civilFromDays(long days, int[] out) {
        long z = days + 719468L;
        long era = floorDiv(z, 146097L);
        long doe = z - era * 146097L;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long m = mp < 10 ? mp + 3 : mp - 9;
        out[0] = (int) (yoe + era * 400L + (m <= 2 ? 1 : 0));
        out[1] = (int) m;
        out[2] = (int) (doy - (153 * mp + 2) / 5 + 1);
    }

    /// Appends the date of `days` since 1970-01-01 as year (four digits at
    /// least), month and day (two digits each), with `separator` between the
    /// fields or nothing when it is 0. Allocates nothing.
    public static StringBuilder appendDate(StringBuilder out, long days, char separator) {
        long z = days + 719468L;
        long era = floorDiv(z, 146097L);
        long doe = z - era * 146097L;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long m = mp < 10 ? mp + 3 : mp - 9;
        pad(out, yoe + era * 400L + (m <= 2 ? 1 : 0), 4);
        if (separator != 0) {
            out.append(separator);
        }
        pad(out, m, 2);
        if (separator != 0) {
            out.append(separator);
        }
        return pad(out, doy - (153 * mp + 2) / 5 + 1, 2);
    }

    /// Appends `value` in at least `width` digits, zero-filled on the left,
    /// without allocating. A negative value is a `-` followed by its padded
    /// magnitude (`-0044`), never zeros in front of the sign.
    public static StringBuilder pad(StringBuilder out, long value, int width) {
        if (value == Long.MIN_VALUE) {
            return out.append("-9223372036854775808");
        }
        long v = value;
        if (v < 0) {
            out.append('-');
            v = -v;
        }
        int len = 1;
        long div = 1;
        while (v / div >= 10) {
            div *= 10;
            len++;
        }
        for (int i = len; i < width; i++) {
            out.append('0');
        }
        for (; div > 0; div /= 10) {
            out.append((char) ('0' + (v / div) % 10));
        }
        return out;
    }
}
