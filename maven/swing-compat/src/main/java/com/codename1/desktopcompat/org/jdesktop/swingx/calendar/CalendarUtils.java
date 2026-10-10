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
package com.codename1.desktopcompat.org.jdesktop.swingx.calendar;

import java.util.Calendar;
import java.util.Date;

/// Static helpers that move a calendar to the start or end of a day,
/// month, year or decade and compare dates by day.
///
/// Only what the device's calendar can do is here: it has `get`, `set`,
/// `add`, `getTime` and `setTime` and nothing else, so the helpers that
/// need the calendar's first day of the week (`startOfWeek`, `endOfWeek`),
/// its daylight saving rules or its field ranges are absent. A month's
/// length is computed by the Gregorian rule.
public class CalendarUtils {

    public static final int ONE_MINUTE = 60 * 1000;
    public static final int ONE_HOUR = 60 * ONE_MINUTE;
    public static final int THREE_HOURS = 3 * ONE_HOUR;
    public static final int ONE_DAY = 24 * ONE_HOUR;
    /// A pseudo calendar field: the decade.
    public static final int DECADE = 5467;
    /// A pseudo calendar field: the year within the decade.
    public static final int YEAR_IN_DECADE = 5468;

    public CalendarUtils() {
    }

    /// The number of days of a month, `month` counted from zero.
    static int daysInMonth(int year, int month) {
        switch (month) {
            case Calendar.FEBRUARY:
                boolean leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
                return leap ? 29 : 28;
            case Calendar.APRIL:
            case Calendar.JUNE:
            case Calendar.SEPTEMBER:
            case Calendar.NOVEMBER:
                return 30;
            default:
                return 31;
        }
    }

    public static boolean isStartOfDay(Calendar calendar) {
        return calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) == 0
                && calendar.get(Calendar.SECOND) == 0 && calendar.get(Calendar.MILLISECOND) == 0;
    }

    public static boolean isEndOfDay(Calendar calendar) {
        return calendar.get(Calendar.HOUR_OF_DAY) == 23 && calendar.get(Calendar.MINUTE) == 59
                && calendar.get(Calendar.SECOND) == 59 && calendar.get(Calendar.MILLISECOND) == 999;
    }

    public static boolean isStartOfMonth(Calendar calendar) {
        return calendar.get(Calendar.DAY_OF_MONTH) == 1 && isStartOfDay(calendar);
    }

    public static boolean isEndOfMonth(Calendar calendar) {
        int last = daysInMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
        return calendar.get(Calendar.DAY_OF_MONTH) == last && isEndOfDay(calendar);
    }

    /// Moves the calendar to the first millisecond of the first year of
    /// its decade.
    public static void startOfDecade(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        calendar.set(Calendar.YEAR, year - year % 10);
        startOfYear(calendar);
    }

    public static Date startOfDecade(Calendar calendar, Date date) {
        calendar.setTime(date);
        startOfDecade(calendar);
        return calendar.getTime();
    }

    public static boolean isStartOfDecade(Calendar calendar) {
        return calendar.get(Calendar.YEAR) % 10 == 0 && isStartOfYear(calendar);
    }

    /// Moves the calendar to the first millisecond of its year.
    public static void startOfYear(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        startOfDay(calendar);
    }

    public static Date startOfYear(Calendar calendar, Date date) {
        calendar.setTime(date);
        startOfYear(calendar);
        return calendar.getTime();
    }

    public static boolean isStartOfYear(Calendar calendar) {
        return calendar.get(Calendar.MONTH) == Calendar.JANUARY && isStartOfMonth(calendar);
    }

    /// Moves the calendar to the first millisecond of its month.
    public static void startOfMonth(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        startOfDay(calendar);
    }

    /// Moves the calendar to the last millisecond of its month.
    public static void endOfMonth(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, daysInMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)));
        endOfDay(calendar);
    }

    /// The first millisecond of the day of `date`; the calendar is left
    /// there.
    public static Date startOfDay(Calendar calendar, Date date) {
        calendar.setTime(date);
        startOfDay(calendar);
        return calendar.getTime();
    }

    /// The last millisecond of the day of `date`; the calendar is left
    /// there.
    public static Date endOfDay(Calendar calendar, Date date) {
        calendar.setTime(date);
        endOfDay(calendar);
        return calendar.getTime();
    }

    public static void startOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // Reading the time settles the fields, as the device's calendar
        // computes lazily.
        calendar.getTime();
    }

    public static void endOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        calendar.getTime();
    }

    /// Whether both are `null` or both name the same instant.
    public static boolean areEqual(Date current, Date date) {
        if (current == null) {
            return date == null;
        }
        return date != null && current.getTime() == date.getTime();
    }

    /// Whether the calendar's time and `date` fall on the same day. The
    /// calendar keeps its time.
    public static boolean isSameDay(Calendar today, Date now) {
        Date kept = today.getTime();
        try {
            int y = today.get(Calendar.YEAR);
            int m = today.get(Calendar.MONTH);
            int d = today.get(Calendar.DAY_OF_MONTH);
            today.setTime(now);
            return y == today.get(Calendar.YEAR) && m == today.get(Calendar.MONTH)
                    && d == today.get(Calendar.DAY_OF_MONTH);
        } finally {
            today.setTime(kept);
        }
    }
}
