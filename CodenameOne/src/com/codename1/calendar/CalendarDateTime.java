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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/// Either a zoned date-time or an all-day date.
public final class CalendarDateTime {

    private final ZonedDateTime dateTime;

    private final LocalDate date;

    private CalendarDateTime(ZonedDateTime dateTime, LocalDate date) {
        this.dateTime = dateTime;
        this.date = date;
    }

    public static CalendarDateTime timed(ZonedDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("dateTime required");
        }
        return new CalendarDateTime(dateTime, null);
    }

    public static CalendarDateTime instant(Instant instant, ZoneId zone) {
        if (instant == null || zone == null) {
            throw new IllegalArgumentException("instant and zone required");
        }
        return timed(ZonedDateTime.ofInstant(instant, zone));
    }

    public static CalendarDateTime allDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date required");
        }
        return new CalendarDateTime(null, date);
    }

    public boolean isAllDay() {
        return date != null;
    }

    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    public LocalDate getDate() {
        return date;
    }
}
