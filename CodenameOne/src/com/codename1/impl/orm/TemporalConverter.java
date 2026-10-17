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
package com.codename1.impl.orm;

import com.codename1.orm.session.AttributeConverter;

import java.util.Date;

/// The storage of a `@Temporal` field: a Date as UTC ISO 8601 text, which every
/// engine accepts for its native date and timestamp columns and SQLite stores
/// as is.
///
/// Text rather than a driver-level date type for the same reason the portable
/// default is a number: the backend's wire clients hand every value over as
/// text, and the client's SQLite has no date type at all. What makes the text
/// unambiguous is the format and the zone -- the backend pins its connections
/// to UTC and ISO output, so the server sends what this parses, and what this
/// writes is never re-interpreted in a session zone.
///
/// Internal ORM runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public class TemporalConverter implements AttributeConverter<Date, String> {
    private static final long DAY = 86400000L;
    private final boolean day;

    TemporalConverter(boolean day) {
        this.day = day;
    }

    /// `@Temporal(TemporalType.TIMESTAMP)`.
    /// @hidden
    public static final class Timestamp extends TemporalConverter {
        /// Creates the converter.
        public Timestamp() {
            super(false);
        }
    }

    /// `@Temporal(TemporalType.DATE)`.
    /// @hidden
    public static final class Day extends TemporalConverter {
        /// Creates the converter.
        public Day() {
            super(true);
        }
    }

    @Override
    public String toDatabase(Date value) {
        return value == null ? null : format(value.getTime(), day);
    }

    @Override
    public Date fromDatabase(String value) {
        if (value == null) {
            return null;
        }
        long millis = parse(value);
        // A DATE column read through a time-of-day value -- SQLite text, or a
        // native column the mapping did not expect -- still answers a day.
        return new Date(day ? floorDiv(millis, DAY) * DAY : millis);
    }

    /// `2026-10-16 09:30:00.000` in UTC, or `2026-10-16` for a day. Fixed width,
    /// so on SQLite, where it is compared as text, text order is time order.
    static String format(long millis, boolean dayOnly) {
        long days = floorDiv(millis, DAY);
        long rest = millis - days * DAY;
        long[] civil = civil(days);
        if (civil[0] < 0 || civil[0] > 9999) {
            throw new IllegalArgumentException("A @Temporal value must fall in the years 0000 to 9999: " + millis);
        }
        StringBuilder out = new StringBuilder(23);
        pad(out, civil[0], 4).append('-');
        pad(out, civil[1], 2).append('-');
        pad(out, civil[2], 2);
        if (!dayOnly) {
            out.append(' ');
            pad(out, rest / 3600000L, 2).append(':');
            pad(out, rest / 60000L % 60, 2).append(':');
            pad(out, rest / 1000L % 60, 2).append('.');
            pad(out, rest % 1000L, 3);
        }
        return out.toString();
    }

    /// Reads `YYYY-MM-DD`, optionally followed by a space or `T`, `HH:MM`,
    /// optional seconds and fraction, and an optional `Z` or numeric offset --
    /// the forms PostgreSQL (DateStyle ISO), MySQL and ISO 8601 text produce.
    /// Digits past the millisecond are dropped, toward the earlier instant.
    /// Anything else is refused rather than guessed at: a zero date, `infinity`,
    /// a BC year or an epoch number in a column the mapping says is native.
    static long parse(String text) {
        Cursor c = new Cursor(text.trim());
        long year = c.digits(4);
        c.expect('-');
        long month = c.digits(2);
        c.expect('-');
        long dayOfMonth = c.digits(2);
        if (month < 1 || month > 12 || dayOfMonth < 1 || dayOfMonth > daysInMonth(year, month)) {
            throw c.invalid();
        }
        long millis = 0;
        if (c.more() && (c.peek() == ' ' || c.peek() == 'T')) {
            c.next();
            long hour = c.digits(2);
            c.expect(':');
            long minute = c.digits(2);
            long second = 0;
            long fraction = 0;
            if (c.more() && c.peek() == ':') {
                c.next();
                second = c.digits(2);
                if (c.more() && c.peek() == '.') {
                    c.next();
                    int count = 0;
                    while (c.more() && c.peek() >= '0' && c.peek() <= '9') {
                        if (count < 3) {
                            fraction = fraction * 10 + (c.next() - '0');
                        } else {
                            c.next();
                        }
                        count++;
                    }
                    if (count == 0) {
                        throw c.invalid();
                    }
                    for (; count < 3; count++) {
                        fraction *= 10;
                    }
                }
            }
            if (hour > 23 || minute > 59 || second > 59) {
                throw c.invalid();
            }
            millis = ((hour * 60 + minute) * 60 + second) * 1000 + fraction;
        }
        if (c.more()) {
            char sign = c.next();
            if (sign == '+' || sign == '-') {
                long offsetHours = c.digits(2);
                long offsetMinutes = 0;
                long offsetSeconds = 0;
                if (c.more() && c.peek() == ':') {
                    c.next();
                }
                if (c.more()) {
                    offsetMinutes = c.digits(2);
                    if (c.more() && c.peek() == ':') {
                        c.next();
                    }
                    if (c.more()) {
                        offsetSeconds = c.digits(2);
                    }
                }
                // The range java.time accepts. Beyond it the text is malformed,
                // and applying it would read a plausible but wrong instant.
                long offset = (offsetHours * 60 + offsetMinutes) * 60 + offsetSeconds;
                if (offsetMinutes > 59 || offsetSeconds > 59 || offset > 18 * 3600) {
                    throw c.invalid();
                }
                millis -= (sign == '+' ? offset : -offset) * 1000;
            } else if (sign != 'Z' && sign != 'z') {
                throw c.invalid();
            }
        }
        if (c.more()) {
            throw c.invalid();
        }
        return days(year, month, dayOfMonth) * DAY + millis;
    }

    private static final class Cursor {
        private final String text;
        private int at;

        Cursor(String text) {
            this.text = text;
        }

        boolean more() {
            return at < text.length();
        }

        char peek() {
            return text.charAt(at);
        }

        char next() {
            return text.charAt(at++);
        }

        long digits(int count) {
            long value = 0;
            for (int i = 0; i < count; i++) {
                if (!more() || peek() < '0' || peek() > '9') {
                    throw invalid();
                }
                value = value * 10 + (next() - '0');
            }
            return value;
        }

        void expect(char ch) {
            if (!more() || next() != ch) {
                throw invalid();
            }
        }

        IllegalArgumentException invalid() {
            return new IllegalArgumentException("Not a @Temporal value: \"" + text + "\"");
        }
    }

    private static long daysInMonth(long year, long month) {
        if (month == 2) {
            return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0) ? 29 : 28;
        }
        return month == 4 || month == 6 || month == 9 || month == 11 ? 30 : 31;
    }

    /// Days since 1970-01-01 of a proleptic Gregorian date (Howard Hinnant's
    /// days_from_civil).
    static long days(long year, long month, long dayOfMonth) {
        long y = month <= 2 ? year - 1 : year;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + dayOfMonth - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    /// {year, month, day} of a day count since 1970-01-01 (civil_from_days).
    static long[] civil(long days) {
        long z = days + 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long dayOfMonth = doy - (153 * mp + 2) / 5 + 1;
        long month = mp < 10 ? mp + 3 : mp - 9;
        return new long[] {month <= 2 ? yoe + era * 400 + 1 : yoe + era * 400, month, dayOfMonth};
    }

    private static long floorDiv(long value, long divisor) {
        long quotient = value / divisor;
        return value % divisor != 0 && (value < 0) != (divisor < 0) ? quotient - 1 : quotient;
    }

    private static StringBuilder pad(StringBuilder out, long value, int width) {
        String digits = String.valueOf(value);
        for (int i = digits.length(); i < width; i++) {
            out.append('0');
        }
        return out.append(digits);
    }
}
