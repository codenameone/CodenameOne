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
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// January 2026 in UTC: the 5th is a Monday, the 9th a Friday.
class WorkCalendarTest {
    static final ZoneId UTC = ZoneId.of("UTC");
    static final long H = TimeMath.HOUR;

    static long at(int day, int hour) {
        return at(UTC, 2026, 1, day, hour, 0);
    }

    static long at(ZoneId zone, int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli();
    }

    @Test
    void dayOfWeekIsIso() {
        assertEquals(1, TimeMath.dayOfWeek(TimeMath.epochDay(at(5, 12), UTC)));
        assertEquals(7, TimeMath.dayOfWeek(TimeMath.epochDay(at(4, 12), UTC)));
        assertEquals(4, TimeMath.dayOfWeek(0));
        assertEquals(3, TimeMath.dayOfWeek(-1));
        long sunday = TimeMath.epochDay(at(4, 0), UTC);
        assertEquals(sunday, TimeMath.startOfWeek(TimeMath.epochDay(at(7, 0), UTC), 7));
        assertEquals(sunday + 1, TimeMath.startOfWeek(TimeMath.epochDay(at(7, 0), UTC), 1));
    }

    @Test
    void workSkipsTheNightAndTheWeekend() {
        WorkCalendar cal = new WorkCalendar(UTC);
        assertEquals(at(12, 11), cal.addWorkingTime(at(9, 15), 4 * H));
        assertEquals(at(9, 15), cal.subtractWorkingTime(at(12, 11), 4 * H));
        assertEquals(4 * H, cal.workingTimeBetween(at(9, 15), at(12, 11)));
        assertEquals(-4 * H, cal.workingTimeBetween(at(12, 11), at(9, 15)));
        assertEquals(8 * H, cal.getMillisPerDay());
    }

    @Test
    void workThatFillsADayEndsAtItsClose() {
        WorkCalendar cal = new WorkCalendar(UTC);
        assertEquals(at(5, 17), cal.addWorkingTime(at(5, 9), 8 * H));
        // The next thing starts the morning after, and a finish found from
        // that morning is the evening before.
        assertEquals(at(6, 9), cal.nextWorkingTime(at(5, 17)));
        assertEquals(at(5, 17), cal.previousWorkingTime(at(6, 9)));
        assertEquals(at(5, 17), cal.previousWorkingTime(at(5, 17)));
        assertEquals(at(5, 11), cal.nextWorkingTime(at(5, 11)));
        assertEquals(at(5, 9), cal.nextWorkingTime(at(3, 13)));
        assertEquals(at(9, 17), cal.previousWorkingTime(at(11, 13)));
    }

    @Test
    void holidaysAndExceptionsOverrideTheWeek() {
        WorkCalendar cal = new WorkCalendar(UTC);
        cal.addHoliday(TimeMath.epochDay(at(6, 0), UTC));
        assertEquals(at(7, 17), cal.addWorkingTime(at(5, 9), 16 * H));
        assertFalse(cal.isWorkingDay(TimeMath.epochDay(at(6, 0), UTC)));
        // A Saturday that is worked in the morning.
        cal.setException(TimeMath.epochDay(at(10, 0), UTC), 9 * 60, 13 * 60);
        assertEquals(at(10, 11), cal.addWorkingTime(at(9, 17), 2 * H));
        assertTrue(cal.isWorkingTime(at(10, 10)));
        assertFalse(cal.isWorkingTime(at(10, 13)));
        cal.removeException(TimeMath.epochDay(at(10, 0), UTC));
        assertEquals(at(12, 11), cal.addWorkingTime(at(9, 17), 2 * H));
    }

    @Test
    void aLunchBreakIsNotWorked() {
        WorkCalendar cal = new WorkCalendar(UTC).setWorkingHours(8 * 60, 12 * 60, 13 * 60, 17 * 60);
        assertEquals(at(5, 14), cal.addWorkingTime(at(5, 11), 2 * H));
        assertEquals(at(5, 11), cal.subtractWorkingTime(at(5, 14), 2 * H));
        assertEquals(at(5, 13), cal.nextWorkingTime(at(5, 12)));
        // Friday is a half day.
        cal.setWorkingHours(5, new int[]{8 * 60, 12 * 60});
        assertEquals(at(12, 9), cal.addWorkingTime(at(9, 11), 2 * H));
    }

    @Test
    void aDayThatLosesAnHourIsStillADayOfWork() {
        ZoneId ny = ZoneId.of("America/New_York");
        // 2026-03-08 is 23 hours long in New York.
        WorkCalendar always = WorkCalendar.continuous(ny);
        long satNoon = at(ny, 2026, 3, 7, 12, 0);
        assertEquals(at(ny, 2026, 3, 8, 13, 0), always.addWorkingTime(satNoon, 24 * H));
        assertEquals(23 * H, always.workingTimeBetween(at(ny, 2026, 3, 8, 0, 0), at(ny, 2026, 3, 9, 0, 0)));

        WorkCalendar weekend = new WorkCalendar(ny).setWorkingDay(7, true);
        assertEquals(at(ny, 2026, 3, 8, 17, 0), weekend.addWorkingTime(at(ny, 2026, 3, 6, 17, 0), 8 * H));
        assertEquals(at(ny, 2026, 3, 9, 9, 0), weekend.nextWorkingTime(at(ny, 2026, 3, 8, 17, 0)));
    }

    @Test
    void aCalendarThatNeverWorksSaysSo() {
        final WorkCalendar cal = new WorkCalendar(UTC);
        for (int d = 1; d <= 7; d++) {
            cal.setWorkingDay(d, false);
        }
        assertThrows(IllegalStateException.class, () -> cal.addWorkingTime(at(5, 9), H));
        assertThrows(IllegalStateException.class, () -> cal.subtractWorkingTime(at(5, 9), H));
        assertThrows(IllegalArgumentException.class, () -> new WorkCalendar(UTC).setWorkingHours(600, 500));
        assertThrows(IllegalArgumentException.class, () -> new WorkCalendar(UTC).setWorkingHours(600));
    }
}
