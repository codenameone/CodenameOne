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
package com.codename1.backend.aws;

/**
 * The ISO basic timestamp AWS signs with: yyyyMMdd'T'HHmmss'Z', always UTC.
 *
 * Written out by hand rather than through SimpleDateFormat because the translated
 * runtime's date formatting is locale-aware and this format must not be -- an
 * Arabic-Indic digit or a locale that renders the year differently produces a
 * signature the service cannot reproduce, and the only symptom is a 403.
 */
final class Clock {
    private static final int[] DAYS_IN_MONTH =
            {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private Clock() {
    }

    static String timestamp() {
        return timestamp(System.currentTimeMillis());
    }

    static String timestamp(long millis) {
        long seconds = millis / 1000L;
        if(millis < 0 && (millis % 1000L) != 0) {
            seconds--; // floor, so a pre-epoch instant does not round toward zero
        }
        long days = floorDiv(seconds, 86400L);
        int secondOfDay = (int)(seconds - days * 86400L);

        int year = 1970;
        while(true) {
            int length = isLeap(year) ? 366 : 365;
            if(days >= length) {
                days -= length;
                year++;
            } else if(days < 0) {
                year--;
                days += isLeap(year) ? 366 : 365;
            } else {
                break;
            }
        }
        int month = 0;
        while(true) {
            int length = DAYS_IN_MONTH[month] + (month == 1 && isLeap(year) ? 1 : 0);
            if(days < length) {
                break;
            }
            days -= length;
            month++;
        }

        StringBuilder out = new StringBuilder(16);
        pad(out, year, 4);
        pad(out, month + 1, 2);
        pad(out, (int)days + 1, 2);
        out.append('T');
        pad(out, secondOfDay / 3600, 2);
        pad(out, (secondOfDay / 60) % 60, 2);
        pad(out, secondOfDay % 60, 2);
        out.append('Z');
        return out.toString();
    }

    private static long floorDiv(long value, long divisor) {
        long q = value / divisor;
        if((value % divisor != 0) && ((value < 0) != (divisor < 0))) {
            q--;
        }
        return q;
    }

    private static boolean isLeap(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }

    private static void pad(StringBuilder out, int value, int width) {
        String text = String.valueOf(value);
        for(int iter = text.length() ; iter < width ; iter++) {
            out.append('0');
        }
        out.append(text);
    }
}
