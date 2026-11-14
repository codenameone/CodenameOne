/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.luau;

/// UTC calendar arithmetic for `os.time` and `os.date`. Games run on many
/// devices at once, so dates are always UTC rather than device local time.
final class DateMath {
    private DateMath() {
    }

    private static final String[] DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July",
        "August", "September", "October", "November", "December"};

    static long daysFromCivil(long y, int m, int d) {
        y -= m <= 2 ? 1 : 0;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153L * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    static long toEpoch(int y, int mo, int d, int h, int mi, int s) {
        // normalise month overflow the way mktime does
        int my = (mo - 1) / 12;
        int mm = (mo - 1) % 12;
        if (mm < 0) {
            mm += 12;
            my--;
        }
        return daysFromCivil(y + my, mm + 1, 1) * 86400L + (d - 1) * 86400L + h * 3600L + mi * 60L + s;
    }

    /// {year, month, day, hour, min, sec, wday (1=Sunday), yday}
    static int[] civil(long t) {
        long days = Math.floorDiv(t, 86400L);
        long secs = t - days * 86400L;
        long z = days + 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        int d = (int) (doy - (153 * mp + 2) / 5 + 1);
        int m = (int) (mp < 10 ? mp + 3 : mp - 9);
        if (m <= 2) {
            y++;
        }
        int wday = (int) Math.floorMod(days + 4, 7L) + 1;
        int yday = (int) (days - daysFromCivil(y, 1, 1)) + 1;
        return new int[] {(int) y, m, d, (int) (secs / 3600), (int) (secs / 60 % 60), (int) (secs % 60), wday, yday};
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : Integer.toString(v);
    }

    static Object format(String fmt, long t) {
        String f = fmt;
        if (f.startsWith("!")) {
            f = f.substring(1);
        }
        int[] c = civil(t);
        if (f.startsWith("*t")) {
            LuaTable r = new LuaTable();
            r.rawset("year", Interp.num(c[0]));
            r.rawset("month", Interp.num(c[1]));
            r.rawset("day", Interp.num(c[2]));
            r.rawset("hour", Interp.num(c[3]));
            r.rawset("min", Interp.num(c[4]));
            r.rawset("sec", Interp.num(c[5]));
            r.rawset("wday", Interp.num(c[6]));
            r.rawset("yday", Interp.num(c[7]));
            r.rawset("isdst", Boolean.FALSE);
            return r;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < f.length(); i++) {
            char ch = f.charAt(i);
            if (ch != '%' || i + 1 >= f.length()) {
                b.append(ch);
                continue;
            }
            char s = f.charAt(++i);
            switch (s) {
                case 'Y':
                    b.append(c[0]);
                    break;
                case 'y':
                    b.append(two(c[0] % 100));
                    break;
                case 'm':
                    b.append(two(c[1]));
                    break;
                case 'd':
                    b.append(two(c[2]));
                    break;
                case 'H':
                    b.append(two(c[3]));
                    break;
                case 'I':
                    b.append(two(c[3] % 12 == 0 ? 12 : c[3] % 12));
                    break;
                case 'M':
                    b.append(two(c[4]));
                    break;
                case 'S':
                    b.append(two(c[5]));
                    break;
                case 'p':
                    b.append(c[3] < 12 ? "AM" : "PM");
                    break;
                case 'A':
                    b.append(DAYS[c[6] - 1]);
                    break;
                case 'a':
                    b.append(DAYS[c[6] - 1].substring(0, 3));
                    break;
                case 'B':
                    b.append(MONTHS[c[1] - 1]);
                    break;
                case 'b':
                    b.append(MONTHS[c[1] - 1].substring(0, 3));
                    break;
                case 'j': {
                    String v = Integer.toString(c[7]);
                    while (v.length() < 3) {
                        v = "0" + v;
                    }
                    b.append(v);
                    break;
                }
                case 'c':
                    b.append(DAYS[c[6] - 1].substring(0, 3)).append(' ').append(MONTHS[c[1] - 1].substring(0, 3)).append(' ')
                            .append(two(c[2])).append(' ').append(two(c[3])).append(':').append(two(c[4]))
                            .append(':').append(two(c[5])).append(' ').append(c[0]);
                    break;
                case 'x':
                    b.append(two(c[1])).append('/').append(two(c[2])).append('/').append(two(c[0] % 100));
                    break;
                case 'X':
                    b.append(two(c[3])).append(':').append(two(c[4])).append(':').append(two(c[5]));
                    break;
                case '%':
                    b.append('%');
                    break;
                default:
                    throw LuaError.rt("invalid conversion specifier '%" + s + "'");
            }
        }
        return b.toString();
    }
}
