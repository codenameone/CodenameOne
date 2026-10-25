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
package com.codename1.mapping;

import java.util.Date;

/// Conversions the generated `@Mapped` mappers share: the lenient readers for
/// `@JsonFormat` fields, ISO-8601 dates computed by arithmetic, and the path
/// prefixing of a nested failure. Called only from generated code.
///
/// Nothing here consults a locale or a time zone: neither is guaranteed on a
/// device, and a wire format must not depend on the phone's settings. Dates are
/// always written in UTC.
public final class JsonValues {
    private JsonValues() {
    }

    /// A JSON number, or a string holding one, as a `long`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the value is neither
    public static long toLong(Object v) {
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (v instanceof String) {
            String s = ((String) v).trim();
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return (long) parseDouble(s);
            }
        }
        throw new IllegalArgumentException("expected a number");
    }

    /// A JSON whole number in `[min, max]`, for an array element of an
    /// integral type: a fraction or a value out of range is refused rather
    /// than truncated, as the server's codec refuses it.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the value is not such a number
    public static long toIntegral(Object v, long min, long max) {
        long l;
        if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            // max + 1.0, not max: (double) Long.MAX_VALUE rounds up to 2^63,
            // which would pass a plain d > max and then clamp in the cast.
            if (d != Math.floor(d) || d < min || d >= max + 1.0) {
                throw new IllegalArgumentException("expected a whole number in range, found " + v);
            }
            l = (long) d;
        } else if (v instanceof String) {
            String s = ((String) v).trim();
            try {
                l = Long.parseLong(s);
            } catch (NumberFormatException e) {
                return toIntegral(Double.valueOf(parseDouble(s)), min, max);
            }
        } else if (v instanceof Number) {
            l = ((Number) v).longValue();
        } else {
            throw new IllegalArgumentException("expected a number");
        }
        if (l < min || l > max) {
            throw new IllegalArgumentException("expected a whole number in range, found " + v);
        }
        return l;
    }

    /// `json` read by `mapper`, which must be a JSON object.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `json` is not an object
    public static Object object(Mapper mapper, Object json) {
        if (!(json instanceof java.util.Map) || mapper == null) {
            throw new IllegalArgumentException("expected a JSON object");
        }
        return mapper.fromMap((java.util.Map) json);
    }

    /// A JSON number, or a string holding one, as a `double`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the value is neither
    public static double toDouble(Object v) {
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v instanceof String) {
            return parseDouble(((String) v).trim());
        }
        throw new IllegalArgumentException("expected a number");
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("expected a number, found \"" + s + "\"", e);
        }
    }

    /// A JSON date in either shape: epoch milliseconds (a number or a numeric
    /// string) or an ISO-8601 string -- a date alone, or a date and time with
    /// optional fraction and `Z` or numeric offset.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the value is neither
    public static Date toDate(Object v) {
        if (v instanceof Number) {
            return new Date(((Number) v).longValue());
        }
        if (v instanceof String) {
            String s = ((String) v).trim();
            if (s.length() > 0 && s.indexOf('-', 1) < 0) {
                return new Date(toLong(s));
            }
            return new Date(parseIso(s));
        }
        throw new IllegalArgumentException("expected a date");
    }

    /// Formats `d` as `yyyy-MM-ddTHH:mm:ss.SSSZ` in UTC.
    public static String formatIso(Date d) {
        long millis = d.getTime();
        long days = floorDiv(millis, 86400000L);
        long ms = millis - days * 86400000L;
        // Civil-from-days (Howard Hinnant's algorithm), proleptic Gregorian.
        long z = days + 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long day = doy - (153 * mp + 2) / 5 + 1;
        long month = mp < 10 ? mp + 3 : mp - 9;
        if (month <= 2) {
            y++;
        }
        StringBuilder b = new StringBuilder(24);
        pad(b, y, 4);
        b.append('-');
        pad(b, month, 2);
        b.append('-');
        pad(b, day, 2);
        b.append('T');
        pad(b, ms / 3600000L, 2);
        b.append(':');
        pad(b, (ms / 60000L) % 60, 2);
        b.append(':');
        pad(b, (ms / 1000L) % 60, 2);
        b.append('.');
        pad(b, ms % 1000L, 3);
        b.append('Z');
        return b.toString();
    }

    private static void pad(StringBuilder b, long v, int width) {
        if (v < 0) {
            b.append('-');
            v = -v;
        }
        String s = String.valueOf(v);
        for (int i = s.length(); i < width; i++) {
            b.append('0');
        }
        b.append(s);
    }

    private static long floorDiv(long a, long b) {
        long q = a / b;
        if ((a % b != 0) && ((a < 0) != (b < 0))) {
            q--;
        }
        return q;
    }

    /// Days in `month` (1-12) of the proleptic Gregorian `year`.
    private static int daysIn(long year, int month) {
        if (month == 2) {
            boolean leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
            return leap ? 29 : 28;
        }
        return month == 4 || month == 6 || month == 9 || month == 11 ? 30 : 31;
    }

    /// Parses an ISO-8601 date or date-time to epoch milliseconds. A value with
    /// no offset is read as UTC.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `s` is not ISO-8601
    public static long parseIso(String s) {
        try {
            int len = s.length();
            int p = 0;
            boolean negYear = false;
            if (len > 0 && (s.charAt(0) == '-' || s.charAt(0) == '+')) {
                negYear = s.charAt(0) == '-';
                p = 1;
            }
            int dash = s.indexOf('-', p);
            long year = Long.parseLong(s.substring(p, dash));
            if (negYear) {
                year = -year;
            }
            int month = Integer.parseInt(s.substring(dash + 1, dash + 3));
            int day = Integer.parseInt(s.substring(dash + 4, dash + 6));
            if (s.charAt(dash + 3) != '-' || month < 1 || month > 12 || day < 1
                    || day > daysIn(year, month)) {
                throw new IllegalArgumentException();
            }
            p = dash + 6;
            long ms = 0;
            long offsetMs = 0;
            if (p < len) {
                char t = s.charAt(p);
                if (t != 'T' && t != 't' && t != ' ') {
                    throw new IllegalArgumentException();
                }
                p++;
                int hh = Integer.parseInt(s.substring(p, p + 2));
                if (s.charAt(p + 2) != ':') {
                    throw new IllegalArgumentException();
                }
                int mm = Integer.parseInt(s.substring(p + 3, p + 5));
                p += 5;
                int ss = 0;
                int frac = 0;
                if (p < len && s.charAt(p) == ':') {
                    ss = Integer.parseInt(s.substring(p + 1, p + 3));
                    p += 3;
                    if (p < len && (s.charAt(p) == '.' || s.charAt(p) == ',')) {
                        p++;
                        int digits = 0;
                        while (p < len && Character.isDigit(s.charAt(p))) {
                            if (digits < 3) {
                                frac = frac * 10 + (s.charAt(p) - '0');
                            }
                            digits++;
                            p++;
                        }
                        if (digits == 0) {
                            throw new IllegalArgumentException();
                        }
                        for (int i = digits; i < 3; i++) {
                            frac *= 10;
                        }
                    }
                }
                // The same bounds the server's reader holds: no 24:00, no leap second.
                if (hh > 23 || mm > 59 || ss > 59) {
                    throw new IllegalArgumentException();
                }
                ms = hh * 3600000L + mm * 60000L + ss * 1000L + frac;
                if (p < len) {
                    char z = s.charAt(p);
                    if ((z == 'Z' || z == 'z') && p + 1 == len) {
                        p++;
                    } else if (z == '+' || z == '-') {
                        String off = s.substring(p + 1);
                        int oh;
                        int om = 0;
                        if (off.length() == 2) {
                            oh = Integer.parseInt(off);
                        } else if (off.length() == 4) {
                            oh = Integer.parseInt(off.substring(0, 2));
                            om = Integer.parseInt(off.substring(2));
                        } else if (off.length() == 5 && off.charAt(2) == ':') {
                            oh = Integer.parseInt(off.substring(0, 2));
                            om = Integer.parseInt(off.substring(3));
                        } else {
                            throw new IllegalArgumentException();
                        }
                        if (oh < 0 || oh > 23 || om < 0 || om > 59) {
                            throw new IllegalArgumentException();
                        }
                        offsetMs = (oh * 60L + om) * 60000L;
                        if (z == '-') {
                            offsetMs = -offsetMs;
                        }
                        p = len;
                    }
                }
                if (p != len) {
                    throw new IllegalArgumentException();
                }
            }
            return daysFromCivil(year, month, day) * 86400000L + ms - offsetMs;
        } catch (RuntimeException e) {
            // NumberFormatException and StringIndexOutOfBoundsException are
            // thrown explicitly by the parsers, never by a cast.
            throw new IllegalArgumentException("expected an ISO-8601 date, found \"" + s + "\"", e);
        }
    }

    private static long daysFromCivil(long y, int m, int d) {
        if (m <= 2) {
            y--;
        }
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153L * (m > 2 ? m - 3 : m + 9) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    /// Prefixes a nested failure with the location it happened at. A message
    /// that already starts with a `$` path is joined to it, so a failure three
    /// levels down reads `$.order.items[2].due: required member is missing`.
    public static IllegalArgumentException at(String path, IllegalArgumentException e) {
        String msg = e.getMessage();
        if (msg != null && msg.startsWith("$")) {
            return new IllegalArgumentException(path + msg.substring(1));
        }
        return new IllegalArgumentException(path + ": " + (msg == null ? "invalid value" : msg));
    }
}
