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
package com.codenameone.examples.wayline;

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
        int month = (text.charAt(5) - '0') * 10 + (text.charAt(6) - '0');
        int day = (text.charAt(8) - '0') * 10 + (text.charAt(9) - '0');
        return month >= 1 && month <= 12 && day >= 1 && day <= 31;
    }
}
