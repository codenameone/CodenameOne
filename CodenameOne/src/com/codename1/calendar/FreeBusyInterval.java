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

/// Busy interval returned by a scheduling/free-busy query.
public final class FreeBusyInterval {

    private final Instant startTime;

    private final Instant endTime;

    private final CalendarEvent.Availability availability;

    public FreeBusyInterval(Instant startTime, Instant endTime, CalendarEvent.Availability availability) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime required");
        }
        if (endTime.compareTo(startTime) < 0) {
            throw new IllegalArgumentException("endTime");
        }
        this.startTime = startTime;
        this.endTime = endTime;
        this.availability = availability == null ? CalendarEvent.Availability.BUSY : availability;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public CalendarEvent.Availability getAvailability() {
        return availability;
    }
}
