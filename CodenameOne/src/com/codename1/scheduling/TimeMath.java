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
package com.codename1.scheduling;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/// Calendar arithmetic on epoch milliseconds in a time zone.
///
/// The scheduling model keeps every instant as epoch milliseconds, because a
/// view compares and subtracts them for every item it paints. These are the
/// few places where an instant has to become a calendar day and back, and they
/// go through `java.time` so a day that is 23 or 25 hours long is handled by
/// the zone rules and not by adding `24 * 60 * 60 * 1000`.
public final class TimeMath {
    /// Milliseconds in a minute.
    public static final long MINUTE = 60000L;
    /// Milliseconds in an hour.
    public static final long HOUR = 3600000L;
    /// Milliseconds in a day that has no zone transition in it.
    public static final long DAY = 86400000L;
    /// Minutes in a day.
    public static final int MINUTES_PER_DAY = 1440;

    private TimeMath() {
    }

    /// The day `millis` falls on in `zone`, as a count of days since
    /// 1970-01-01.
    public static long epochDay(long millis, ZoneId zone) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zone).toLocalDate().toEpochDay();
    }

    /// The instant the day `epochDay` starts at in `zone`.
    public static long startOfEpochDay(long epochDay, ZoneId zone) {
        return LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /// The start of the day `millis` falls on.
    public static long startOfDay(long millis, ZoneId zone) {
        return startOfEpochDay(epochDay(millis, zone), zone);
    }

    /// The instant of a wall-clock minute on a day. `minuteOfDay` may be
    /// `1440`, which is the start of the following day.
    public static long at(long epochDay, int minuteOfDay, ZoneId zone) {
        if (minuteOfDay >= MINUTES_PER_DAY) {
            return startOfEpochDay(epochDay + 1, zone);
        }
        if (minuteOfDay <= 0) {
            return startOfEpochDay(epochDay, zone);
        }
        return ZonedDateTime.of(LocalDate.ofEpochDay(epochDay),
                LocalTime.of(minuteOfDay / 60, minuteOfDay % 60), zone).toInstant().toEpochMilli();
    }

    /// The wall-clock minute of the day at `millis`.
    public static int minuteOfDay(long millis, ZoneId zone) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zone).toLocalTime().toSecondOfDay() / 60;
    }

    /// The ISO day of the week of a day: 1 is Monday, 7 is Sunday.
    public static int dayOfWeek(long epochDay) {
        // 1970-01-01 was a Thursday.
        long m = (epochDay + 3) % 7;
        if (m < 0) {
            m += 7;
        }
        return (int) m + 1;
    }

    /// The first day of the week `epochDay` is in, when weeks start on
    /// `firstDayOfWeek` (ISO, 1 is Monday).
    public static long startOfWeek(long epochDay, int firstDayOfWeek) {
        int back = dayOfWeek(epochDay) - firstDayOfWeek;
        if (back < 0) {
            back += 7;
        }
        return epochDay - back;
    }

    /// The first day of the month `epochDay` is in.
    public static long startOfMonth(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).withDayOfMonth(1).toEpochDay();
    }

    /// The first day of the month `months` after the one `epochDay` is in.
    public static long plusMonths(long epochDay, int months) {
        return LocalDate.ofEpochDay(epochDay).withDayOfMonth(1).plusMonths(months).toEpochDay();
    }
}
