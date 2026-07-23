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
package com.codename1.impl.javase;

import com.codename1.calendar.CalendarAttendee;
import com.codename1.calendar.CalendarDateTime;
import com.codename1.calendar.CalendarEvent;
import com.codename1.calendar.CalendarMutationScope;
import com.codename1.calendar.CalendarPage;
import com.codename1.calendar.CalendarQuery;
import com.codename1.calendar.CalendarTask;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSECalendarSourceTest {

    @Test
    void boundedQueryFiltersAllDayEvents() {
        JavaSECalendarSource source = new JavaSECalendarSource();
        source.saveEvent(allDayEvent("outside", LocalDate.of(2026, 6, 1)), CalendarMutationScope.ALL);
        source.saveEvent(allDayEvent("inside", LocalDate.of(2026, 7, 15)), CalendarMutationScope.ALL);

        CalendarPage<CalendarEvent> result = source.queryEvents(new CalendarQuery()
                .setStartTime(Instant.parse("2026-07-01T00:00:00Z"))
                .setEndTime(Instant.parse("2026-07-31T23:59:59Z"))).get();

        assertEquals(1, result.getItems().size());
        assertEquals("inside", result.getItems().get(0).getTitle());
    }

    @Test
    void eventsAreDetachedFromSimulatorStorage() {
        CalendarAttendee attendee = new CalendarAttendee().setName("Original").setSelf(true);
        CalendarEvent input = new CalendarEvent().setTitle("Stored").addAttendee(attendee);
        JavaSECalendarSource source = new JavaSECalendarSource();

        CalendarEvent saved = source.saveEvent(input, CalendarMutationScope.ALL).get();
        input.setTitle("Changed input");
        attendee.setName("Changed input attendee");
        saved.setTitle("Changed result");
        saved.getAttendees().get(0).setName("Changed result attendee");

        CalendarEvent queried = source.queryEvents(null).get().getItems().get(0);
        assertEquals("Stored", queried.getTitle());
        assertEquals("Original", queried.getAttendees().get(0).getName());

        queried.setTitle("Changed query");
        queried.getAttendees().get(0).setName("Changed query attendee");
        CalendarEvent reread = source.getEvent(saved.getCalendarId(), saved.getId()).get();
        assertEquals("Stored", reread.getTitle());
        assertEquals("Original", reread.getAttendees().get(0).getName());
    }

    @Test
    void tasksAreDetachedFromSimulatorStorage() {
        CalendarTask input = new CalendarTask().setTitle("Stored");
        JavaSECalendarSource source = new JavaSECalendarSource();

        CalendarTask saved = source.saveTask(input, CalendarMutationScope.ALL).get();
        input.setTitle("Changed input");
        saved.setTitle("Changed result");

        CalendarTask queried = source.queryTasks(null).get().getItems().get(0);
        assertEquals("Stored", queried.getTitle());

        queried.setTitle("Changed query");
        assertEquals("Stored", source.getTask(saved.getCalendarId(), saved.getId()).get().getTitle());
    }

    private static CalendarEvent allDayEvent(String title, LocalDate date) {
        return new CalendarEvent().setTitle(title)
                .setStart(CalendarDateTime.allDay(date))
                .setEnd(CalendarDateTime.allDay(date.plusDays(1)));
    }
}
