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
package com.codename1.backend;

/**
 * The one date format HTTP/1.1 requires on the wire ("Sun, 06 Nov 1994 08:49:37
 * GMT"), formatted and parsed from epoch milliseconds directly.
 *
 * Done arithmetically rather than through Calendar and TimeZone: the format is
 * fixed and always GMT, and going through a calendar would make a header depend on
 * the process's default time zone, which is how Last-Modified ends up hours off on
 * a machine that is not in UTC.
 */
public final class Http1Date {
    private static final String[] DAYS = {"Thu", "Fri", "Sat", "Sun", "Mon", "Tue", "Wed"};
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private Http1Date() {
    }

    // JavaAPI's Math has no floorDiv/floorMod, and integer division in Java
    // truncates toward zero -- which for a pre-1970 timestamp gives the wrong day.
    private static long floorDiv(long x, long y) {
        long q = x / y;
        if((x % y != 0) && ((x < 0) != (y < 0))) {
            q--;
        }
        return q;
    }

    private static long floorMod(long x, long y) {
        return x - floorDiv(x, y) * y;
    }

    public static String format(long millis) {
        long seconds = floorDiv(millis, 1000L);
        long days = floorDiv(seconds, 86400L);
        int secondOfDay = (int)floorMod(seconds, 86400L);
        // 1970-01-01 was a Thursday, which is why DAYS starts there.
        int dayOfWeek = (int)floorMod(days, 7L);
        int[] civil = civilFromDays(days);
        StringBuilder out = new StringBuilder(29);
        out.append(DAYS[dayOfWeek]).append(", ");
        two(out, civil[2]).append(' ').append(MONTHS[civil[1] - 1]).append(' ');
        out.append(civil[0]).append(' ');
        two(out, secondOfDay / 3600).append(':');
        two(out, (secondOfDay / 60) % 60).append(':');
        two(out, secondOfDay % 60).append(" GMT");
        return out.toString();
    }

    /** Epoch millis, or -1 when the value is not a date this understands. */
    public static long parse(String value) {
        if(value == null) {
            return -1;
        }
        String v = value.trim();
        // "Sun, 06 Nov 1994 08:49:37 GMT" -- the only form a modern server must
        // emit. The two obsolete RFC 850 / asctime forms are not accepted; a client
        // sending one gets a full response rather than a wrong 304.
        // The WHOLE shape, not the length and one comma. Every field below is
        // read by fixed offset and then handed to daysFromCivil, which NORMALISES
        // whatever it is given: "Sun, 99 Nov 9999 99:99:99 BAD" was accepted and
        // turned into a date far in the future, and StaticFiles then read that as
        // "newer than the file" and answered 304 -- a conditional request served
        // no content because its date was nonsense. A malformed date has to be
        // no date at all.
        if(v.length() != 29 || v.charAt(3) != ',' || v.charAt(4) != ' '
                || v.charAt(7) != ' ' || v.charAt(11) != ' ' || v.charAt(16) != ' '
                || v.charAt(19) != ':' || v.charAt(22) != ':' || v.charAt(25) != ' '
                || !"GMT".equals(v.substring(26))) {
            return -1;
        }
        try {
            int day = Integer.parseInt(v.substring(5, 7).trim());
            String monthName = v.substring(8, 11);
            int month = -1;
            for(int iter = 0 ; iter < MONTHS.length ; iter++) {
                if(MONTHS[iter].equals(monthName)) {
                    month = iter + 1;
                    break;
                }
            }
            if(month < 0) {
                return -1;
            }
            int year = Integer.parseInt(v.substring(12, 16).trim());
            int hour = Integer.parseInt(v.substring(17, 19).trim());
            int minute = Integer.parseInt(v.substring(20, 22).trim());
            int second = Integer.parseInt(v.substring(23, 25).trim());
            // Ranges, for the same reason: daysFromCivil answers for day 99 as
            // readily as for day 9, and the answer is a different date than the
            // one written. A second of 60 is allowed because a leap second is
            // spelled that way.
            if(day < 1 || day > daysInMonth(year, month) || hour > 23 || minute > 59
                    || second > 60 || year < 1) {
                return -1;
            }
            long days = daysFromCivil(year, month, day);
            return ((days * 86400L) + hour * 3600L + minute * 60L + second) * 1000L;
        } catch (NumberFormatException err) {
            return -1;
        } catch (IndexOutOfBoundsException err) {
            return -1;
        }
    }

    /** Days in a month, so a date that does not exist is not silently moved. */
    private static int daysInMonth(int year, int month) {
        switch(month) {
            case 2:
                boolean leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
                return leap ? 29 : 28;
            case 4: case 6: case 9: case 11:
                return 30;
            default:
                return 31;
        }
    }

    private static StringBuilder two(StringBuilder out, int value) {
        if(value < 10) {
            out.append('0');
        }
        return out.append(value);
    }

    /*
     * Howard Hinnant's civil-date algorithms: exact for every date in range, with
     * no leap-year special cases to get wrong. The shift moves the epoch to
     * 0000-03-01 so February -- the only month whose length varies -- lands at the
     * end of the year and drops out of the arithmetic.
     */
    private static int[] civilFromDays(long days) {
        long z = days + 719468L;
        long era = floorDiv(z, 146097L);
        long doe = z - era * 146097L;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400L;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        return new int[]{(int)(m <= 2 ? y + 1 : y), (int)m, (int)d};
    }

    private static long daysFromCivil(int year, int month, int day) {
        long y = year - (month <= 2 ? 1 : 0);
        long era = floorDiv(y, 400L);
        long yoe = y - era * 400L;
        long mp = month > 2 ? month - 3 : month + 9;
        long doy = (153 * mp + 2) / 5 + day - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097L + doe - 719468L;
    }
}
