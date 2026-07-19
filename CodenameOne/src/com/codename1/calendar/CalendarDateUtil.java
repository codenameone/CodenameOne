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
package com.codename1.calendar;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

final class CalendarDateUtil {
    private CalendarDateUtil() {}

    static String[] split(String value, char delimiter) {
        int count = 1;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) == delimiter) count++;
        String[] out = new String[count];
        int start = 0, index = 0;
        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == delimiter) {
                out[index++] = value.substring(start, i);
                start = i + 1;
            }
        }
        return out;
    }

    static String formatBasic(long value, String zone) {
        Calendar calendar = calendar(value, zone);
        return pad(calendar.get(Calendar.YEAR), 4)
                + pad(calendar.get(Calendar.MONTH) + 1, 2)
                + pad(calendar.get(Calendar.DAY_OF_MONTH), 2) + "T"
                + pad(calendar.get(Calendar.HOUR_OF_DAY), 2)
                + pad(calendar.get(Calendar.MINUTE), 2)
                + pad(calendar.get(Calendar.SECOND), 2);
    }

    static String formatIso(long value, String zone, boolean milliseconds) {
        Calendar calendar = calendar(value, zone);
        String out = pad(calendar.get(Calendar.YEAR), 4) + "-"
                + pad(calendar.get(Calendar.MONTH) + 1, 2) + "-"
                + pad(calendar.get(Calendar.DAY_OF_MONTH), 2) + "T"
                + pad(calendar.get(Calendar.HOUR_OF_DAY), 2) + ":"
                + pad(calendar.get(Calendar.MINUTE), 2) + ":"
                + pad(calendar.get(Calendar.SECOND), 2);
        return milliseconds ? out + "." + pad(calendar.get(Calendar.MILLISECOND), 3) : out;
    }

    static long parseDateTime(String value, String defaultZone) {
        boolean compact = value.length() > 4 && value.charAt(4) != '-';
        int year = number(value, 0, 4);
        int month = number(value, compact ? 4 : 5, compact ? 6 : 7);
        int day = number(value, compact ? 6 : 8, compact ? 8 : 10);
        int hour = number(value, compact ? 9 : 11, compact ? 11 : 13);
        int minute = number(value, compact ? 11 : 14, compact ? 13 : 16);
        int second = number(value, compact ? 13 : 17, compact ? 15 : 19);
        int position = compact ? 15 : 19;
        int millis = 0;
        if (position < value.length() && value.charAt(position) == '.') {
            int start = ++position, end = start;
            while (end < value.length() && Character.isDigit(value.charAt(end))) end++;
            int digits = Math.min(3, end - start);
            for (int i = 0; i < digits; i++) millis = millis * 10 + value.charAt(start + i) - '0';
            if (digits == 1) millis *= 100;
            else if (digits == 2) millis *= 10;
            position = end;
        }
        String zone = defaultZone == null ? "UTC" : defaultZone;
        if (position < value.length()) {
            char suffix = value.charAt(position);
            if (suffix == 'Z' || suffix == 'z') {
                zone = "UTC";
            } else if (suffix == '+' || suffix == '-') {
                String offset = value.substring(position);
                if (offset.length() == 5) offset = offset.substring(0, 3) + ":" + offset.substring(3);
                zone = "GMT" + offset;
            }
        }
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zone));
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, millis);
        return calendar.getTime().getTime();
    }

    static CalendarDate dateFor(long value, String zone) {
        Calendar calendar = calendar(value, zone);
        return new CalendarDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    static long allDayMillis(CalendarDate date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, date.getYear());
        calendar.set(Calendar.MONTH, date.getMonth() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, date.getDay());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime();
    }

    private static Calendar calendar(long value, String zone) {
        Calendar out = Calendar.getInstance(TimeZone.getTimeZone(zone == null ? "UTC" : zone));
        out.setTime(new Date(value));
        return out;
    }

    private static int number(String value, int start, int end) {
        if (start < 0 || end > value.length() || start >= end) throw new IllegalArgumentException("Invalid date: " + value);
        return Integer.parseInt(value.substring(start, end));
    }

    private static String pad(int value, int width) {
        String out = String.valueOf(value);
        while (out.length() < width) out = "0" + out;
        return out;
    }
}
