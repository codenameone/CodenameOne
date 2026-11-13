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

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/// The hours work happens in: a weekly pattern, and the days that differ from
/// it.
///
/// A duration in a project is working time. Two days of work that start on a
/// Friday afternoon end on Tuesday, and this is the class that says so. A
/// `Scheduler` reads the same object to shade the hours nobody works.
///
/// The default is Monday to Friday, 09:00 to 17:00, in the device's zone.
public final class WorkCalendar {
    private static final int[] NONE = new int[0];
    // A stretch of days with no working time longer than this is a calendar
    // that never works again, not a holiday.
    private static final int MAX_IDLE_DAYS = 3700;

    private final ZoneId zone;
    private final boolean[] workingDays = new boolean[8];
    private int[] hours = new int[]{9 * 60, 17 * 60};
    private final int[][] dayHours = new int[8][];
    private final Map<Long, int[]> exceptions = new HashMap<Long, int[]>();

    /// A Monday to Friday, 09:00 to 17:00 calendar in the device's zone.
    public WorkCalendar() {
        this(ZoneId.systemDefault());
    }

    /// A Monday to Friday, 09:00 to 17:00 calendar in `zone`.
    public WorkCalendar(ZoneId zone) {
        if (zone == null) {
            throw new IllegalArgumentException("zone");
        }
        this.zone = zone;
        for (int d = 1; d <= 5; d++) {
            workingDays[d] = true;
        }
    }

    /// A calendar in which every minute of every day is working time, for
    /// durations that are elapsed time.
    public static WorkCalendar continuous(ZoneId zone) {
        WorkCalendar c = new WorkCalendar(zone);
        for (int d = 1; d <= 7; d++) {
            c.workingDays[d] = true;
        }
        c.hours = new int[]{0, TimeMath.MINUTES_PER_DAY};
        return c;
    }

    public ZoneId getZone() {
        return zone;
    }

    /// Whether a day of the week is worked. `isoDay` is 1 for Monday to 7 for
    /// Sunday.
    public WorkCalendar setWorkingDay(int isoDay, boolean working) {
        checkDay(isoDay);
        workingDays[isoDay] = working;
        return this;
    }

    public boolean isWorkingWeekday(int isoDay) {
        checkDay(isoDay);
        return workingDays[isoDay];
    }

    /// The hours of every working day that has none of its own, as pairs of
    /// minutes since midnight: `setWorkingHours(8 * 60, 12 * 60, 13 * 60, 17 * 60)`
    /// is a day with a lunch break. An end of `1440` is midnight.
    public WorkCalendar setWorkingHours(int... startEndMinutes) {
        hours = checked(startEndMinutes);
        return this;
    }

    /// The hours of one day of the week, which also makes it a working day.
    public WorkCalendar setWorkingHours(int isoDay, int[] startEndMinutes) {
        checkDay(isoDay);
        int[] own = checked(startEndMinutes);
        // No hours is a day off and not a day with hours of its own, so the
        // day takes the default hours if it is made a working day again.
        dayHours[isoDay] = own.length > 0 ? own : null;
        workingDays[isoDay] = own.length > 0;
        return this;
    }

    /// Makes one date a day off, whatever the week says.
    public WorkCalendar addHoliday(long epochDay) {
        exceptions.put(Long.valueOf(epochDay), NONE);
        return this;
    }

    /// Gives one date hours of its own, whatever the week says.
    public WorkCalendar setException(long epochDay, int... startEndMinutes) {
        exceptions.put(Long.valueOf(epochDay), checked(startEndMinutes));
        return this;
    }

    /// Returns a date to the weekly pattern.
    public WorkCalendar removeException(long epochDay) {
        exceptions.remove(Long.valueOf(epochDay));
        return this;
    }

    /// The working hours of a date, as pairs of minutes since midnight. Empty
    /// for a day off. The array is the calendar's own: read it, do not change it.
    public int[] getWorkingHours(long epochDay) {
        if (!exceptions.isEmpty()) {
            int[] ex = exceptions.get(Long.valueOf(epochDay));
            if (ex != null) {
                return ex;
            }
        }
        int dow = TimeMath.dayOfWeek(epochDay);
        if (!workingDays[dow]) {
            return NONE;
        }
        return dayHours[dow] != null ? dayHours[dow] : hours;
    }

    public boolean isWorkingDay(long epochDay) {
        return getWorkingHours(epochDay).length > 0;
    }

    /// Whether `millis` is inside working hours.
    public boolean isWorkingTime(long millis) {
        long day = TimeMath.epochDay(millis, zone);
        int[] iv = getWorkingHours(day);
        for (int i = 0; i < iv.length; i += 2) {
            if (millis >= TimeMath.at(day, iv[i], zone) && millis < TimeMath.at(day, iv[i + 1], zone)) {
                return true;
            }
        }
        return false;
    }

    /// The first working instant at or after `millis`.
    public long nextWorkingTime(long millis) {
        return addWorkingTime(millis, 0);
    }

    /// The last instant at or before `millis` that work can end at: `millis`
    /// itself when it is inside working hours or closes them.
    public long previousWorkingTime(long millis) {
        return subtractWorkingTime(millis, 0);
    }

    /// The instant `duration` milliseconds of working time after `from`. A
    /// duration that ends exactly as a working period closes ends there, not
    /// at the opening of the next one. A negative duration goes backwards.
    public long addWorkingTime(long from, long duration) {
        if (duration < 0) {
            return subtractWorkingTime(from, -duration);
        }
        long day = TimeMath.epochDay(from, zone);
        long remaining = duration;
        long cursor = from;
        int idle = 0;
        while (true) {
            int[] iv = getWorkingHours(day);
            idle = iv.length == 0 ? idle + 1 : 0;
            if (idle > MAX_IDLE_DAYS) {
                throw new IllegalStateException("The calendar has no working time");
            }
            for (int i = 0; i < iv.length; i += 2) {
                long e = TimeMath.at(day, iv[i + 1], zone);
                if (e <= cursor) {
                    continue;
                }
                long begin = Math.max(TimeMath.at(day, iv[i], zone), cursor);
                long avail = e - begin;
                if (remaining <= avail) {
                    return begin + remaining;
                }
                remaining -= avail;
                cursor = e;
            }
            day++;
        }
    }

    /// The instant `duration` milliseconds of working time before `from`.
    public long subtractWorkingTime(long from, long duration) {
        if (duration < 0) {
            return addWorkingTime(from, -duration);
        }
        long day = TimeMath.epochDay(from, zone);
        long remaining = duration;
        long cursor = from;
        int idle = 0;
        while (true) {
            int[] iv = getWorkingHours(day);
            idle = iv.length == 0 ? idle + 1 : 0;
            if (idle > MAX_IDLE_DAYS) {
                throw new IllegalStateException("The calendar has no working time");
            }
            for (int i = iv.length - 2; i >= 0; i -= 2) {
                long s = TimeMath.at(day, iv[i], zone);
                if (s >= cursor) {
                    continue;
                }
                long end = Math.min(TimeMath.at(day, iv[i + 1], zone), cursor);
                long avail = end - s;
                if (remaining <= avail) {
                    return end - remaining;
                }
                remaining -= avail;
                cursor = s;
            }
            day--;
        }
    }

    /// The working time between two instants, negative when `to` is before
    /// `from`.
    public long workingTimeBetween(long from, long to) {
        if (to < from) {
            return -workingTimeBetween(to, from);
        }
        long first = TimeMath.epochDay(from, zone);
        long last = TimeMath.epochDay(to, zone);
        long total = 0;
        for (long day = first; day <= last; day++) {
            int[] iv = getWorkingHours(day);
            for (int i = 0; i < iv.length; i += 2) {
                long s = Math.max(TimeMath.at(day, iv[i], zone), from);
                long e = Math.min(TimeMath.at(day, iv[i + 1], zone), to);
                if (e > s) {
                    total += e - s;
                }
            }
        }
        return total;
    }

    /// The working time in a full day of the weekly pattern, which is what "a
    /// day" of duration means in this calendar.
    public long getMillisPerDay() {
        long total = 0;
        for (int i = 0; i < hours.length; i += 2) {
            total += (hours[i + 1] - hours[i]) * TimeMath.MINUTE;
        }
        return total;
    }

    private static void checkDay(int isoDay) {
        if (isoDay < 1 || isoDay > 7) {
            throw new IllegalArgumentException("isoDay " + isoDay);
        }
    }

    private static int[] checked(int[] v) {
        if (v == null || v.length % 2 != 0) {
            throw new IllegalArgumentException("Working hours are pairs of start and end minutes");
        }
        int[] out = new int[v.length];
        int last = 0;
        for (int i = 0; i < v.length; i += 2) {
            if (v[i] < last || v[i + 1] <= v[i] || v[i + 1] > TimeMath.MINUTES_PER_DAY) {
                throw new IllegalArgumentException("Working hours must be ascending and inside one day");
            }
            out[i] = v[i];
            out[i + 1] = v[i + 1];
            last = v[i + 1];
        }
        return out;
    }
}
