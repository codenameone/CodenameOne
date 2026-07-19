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

/// Either an instant with an Olson time-zone ID or an all-day date.
public final class CalendarDateTime {
    private final long timestamp;
    private final String timeZoneId;
    private final CalendarDate date;
    private CalendarDateTime(long timestamp, String timeZoneId, CalendarDate date) {
        this.timestamp = timestamp; this.timeZoneId = timeZoneId; this.date = date;
    }
    public static CalendarDateTime instant(long timestamp, String timeZoneId) {
        if (timeZoneId == null || timeZoneId.length() == 0) throw new IllegalArgumentException("timeZoneId required");
        return new CalendarDateTime(timestamp, timeZoneId, null);
    }
    public static CalendarDateTime allDay(CalendarDate date) {
        if (date == null) throw new IllegalArgumentException("date required");
        return new CalendarDateTime(0L, null, date);
    }
    public boolean isAllDay() { return date != null; }
    public long getTimestamp() { return timestamp; }
    public String getTimeZoneId() { return timeZoneId; }
    public CalendarDate getDate() { return date; }
}
