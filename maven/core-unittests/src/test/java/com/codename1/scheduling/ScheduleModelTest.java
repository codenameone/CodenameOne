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

import com.codename1.calendar.CalendarDateTime;
import com.codename1.calendar.CalendarEvent;
import com.codename1.calendar.CalendarException;
import com.codename1.calendar.CalendarError;
import com.codename1.calendar.CalendarInfo;
import com.codename1.calendar.CalendarMutationScope;
import com.codename1.calendar.CalendarPage;
import com.codename1.calendar.CalendarQuery;
import com.codename1.calendar.CalendarSource;
import com.codename1.calendar.FreeBusyInterval;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.codename1.scheduling.WorkCalendarTest.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The models a scheduler reads: the one in memory, and the one over a
/// calendar source. January 2026 in UTC.
class ScheduleModelTest extends UITestBase {
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static List<ScheduleItem> items(ScheduleModel m, long from, long to) {
        List<ScheduleItem> out = new ArrayList<ScheduleItem>();
        m.getItems(from, to, out);
        return out;
    }

    private static final class Counter implements ScheduleChangeListener {
        int count;

        @Override
        public void scheduleChanged(ScheduleModel model) {
            count++;
        }
    }

    @FormTest
    void theMemoryModelAnswersARangeAndTellsItsListeners() {
        DefaultScheduleModel m = new DefaultScheduleModel();
        Counter c = new Counter();
        m.addChangeListener(c);
        ScheduleItem a = new ScheduleItem("a", "A", at(5, 9), at(5, 10));
        ScheduleItem b = new ScheduleItem("b", "B", at(6, 9), at(6, 10));
        m.addItem(a).addItem(b);
        assertEquals(2, c.count);
        assertEquals(1, items(m, at(5, 0), at(6, 0)).size());
        assertSame(a, items(m, at(5, 0), at(6, 0)).get(0));
        assertTrue(items(m, at(5, 10), at(6, 9)).isEmpty(), "an item ends before its end and starts at its start");
        assertEquals(2, items(m, at(5, 0), at(7, 0)).size());
        assertSame(b, m.getItem("b"));

        assertTrue(m.updateItem(a, at(6, 12), at(6, 13), "ann"));
        assertEquals(at(6, 12), a.getStart());
        assertEquals("ann", a.getResourceId());
        assertEquals(3, c.count);

        m.setCreationEnabled(false);
        assertNull(m.createItem(at(7, 9), at(7, 10), null), "a model can refuse new items");
        m.setCreationEnabled(true).setDefaultDuration(30 * TimeMath.MINUTE);
        ScheduleItem made = m.createItem(at(7, 9), at(7, 9), "ann");
        assertNotNull(made);
        assertEquals(at(7, 9) + 30 * TimeMath.MINUTE, made.getEnd());
        assertEquals("ann", made.getResourceId());
        assertEquals(3, m.getAllItems().size());

        assertTrue(m.removeItem(made));
        assertFalse(m.removeItem(made));
        m.removeChangeListener(c);
        int before = c.count;
        m.clearItems();
        assertEquals(before, c.count);
    }

    /// A source that keeps its events in a list, answers two to a page, and
    /// fails its next save when told to.
    private static final class ListSource extends CalendarSource {
        final List<CalendarEvent> events = new ArrayList<CalendarEvent>();
        final List<CalendarQuery> queries = new ArrayList<CalendarQuery>();
        final List<CalendarMutationScope> scopes = new ArrayList<CalendarMutationScope>();
        final List<String> deleted = new ArrayList<String>();
        boolean failSave;
        boolean failDelete;
        int saves;

        ListSource() {
            super("list", "List");
        }

        @Override
        public AsyncResource<CalendarPage<CalendarEvent>> queryEvents(CalendarQuery q) {
            queries.add(q);
            List<CalendarEvent> mine = new ArrayList<CalendarEvent>();
            for (CalendarEvent e : events) {
                if (q.getCalendarId().equals(e.getCalendarId())) {
                    mine.add(e);
                }
            }
            int from = q.getPageToken() == null ? 0 : Integer.parseInt(q.getPageToken());
            int to = Math.min(mine.size(), from + 2);
            AsyncResource<CalendarPage<CalendarEvent>> out = new AsyncResource<CalendarPage<CalendarEvent>>();
            out.complete(new CalendarPage<CalendarEvent>(new ArrayList<CalendarEvent>(mine.subList(from, to)),
                    to < mine.size() ? String.valueOf(to) : null, null));
            return out;
        }

        @Override
        public AsyncResource<List<FreeBusyInterval>> queryFreeBusy(List<String> ids, Instant from, Instant to) {
            List<FreeBusyInterval> list = new ArrayList<FreeBusyInterval>();
            list.add(new FreeBusyInterval(Instant.ofEpochMilli(at(5, 13)), Instant.ofEpochMilli(at(5, 14)),
                    CalendarEvent.Availability.BUSY));
            list.add(new FreeBusyInterval(Instant.ofEpochMilli(at(5, 15)), Instant.ofEpochMilli(at(5, 16)),
                    CalendarEvent.Availability.FREE));
            AsyncResource<List<FreeBusyInterval>> out = new AsyncResource<List<FreeBusyInterval>>();
            out.complete(list);
            return out;
        }

        @Override
        public AsyncResource<CalendarEvent> saveEvent(CalendarEvent event, CalendarMutationScope scope) {
            saves++;
            scopes.add(scope);
            AsyncResource<CalendarEvent> out = new AsyncResource<CalendarEvent>();
            if (failSave) {
                out.error(new CalendarException(CalendarError.CONFLICT, "refused"));
            } else {
                if (event.getId() == null) {
                    event.setId("saved-" + saves);
                    events.add(event);
                }
                out.complete(event);
            }
            return out;
        }

        @Override
        public AsyncResource<Boolean> deleteEvent(String calendarId, String eventId, CalendarMutationScope scope,
                                                  String version) {
            AsyncResource<Boolean> out = new AsyncResource<Boolean>();
            if (failDelete) {
                out.error(new CalendarException(CalendarError.CONFLICT, "refused"));
            } else {
                deleted.add(eventId);
                out.complete(Boolean.TRUE);
            }
            return out;
        }
    }

    private static CalendarEvent event(String calendar, String id, long start, long end) {
        return new CalendarEvent().setCalendarId(calendar).setId(id).setTitle(id)
                .setStart(CalendarDateTime.instant(Instant.ofEpochMilli(start), UTC))
                .setEnd(CalendarDateTime.instant(Instant.ofEpochMilli(end), UTC));
    }

    private static ScheduleItem find(List<ScheduleItem> list, String id) {
        for (ScheduleItem i : list) {
            if (i.getId().equals(id)) {
                return i;
            }
        }
        return null;
    }

    @FormTest
    void theCalendarModelLoadsEveryPageOfEveryCalendar() {
        ListSource source = new ListSource();
        source.events.add(event("work", "a", at(5, 9), at(5, 10)));
        source.events.add(event("work", "b", at(5, 11), at(5, 12)));
        source.events.add(event("work", "c", at(6, 9), at(6, 10)));
        source.events.add(event("work", "gone", at(6, 11), at(6, 12)).setStatus(CalendarEvent.Status.CANCELED));
        source.events.add(new CalendarEvent().setCalendarId("home").setId("trip").setTitle("Trip")
                .setStart(CalendarDateTime.allDay(LocalDate.of(2026, 1, 6)))
                .setEnd(CalendarDateTime.allDay(LocalDate.of(2026, 1, 8))));
        source.events.add(event("home", "run", at(7, 7), at(7, 8)).setRecurringEventId("series"));

        CalendarScheduleModel m = new CalendarScheduleModel(source, UTC);
        Counter c = new Counter();
        m.addChangeListener(c);
        m.addCalendar(new CalendarInfo().setId("work").setName("Work").setColor(0xff112233));
        m.addCalendar(new CalendarInfo().setId("home").setName("Home").setReadOnly(true));
        m.addFreeBusyCalendar(new CalendarInfo().setId("boss").setName("Boss"));
        assertTrue(source.queries.isEmpty(), "nothing is asked for before a range is shown");
        m.setVisibleRange(at(5, 0), at(8, 0));
        flushSerialCalls();
        assertTrue(c.count > 0);
        assertNull(m.getLastError());

        assertEquals(3, m.getResources().size());
        assertEquals("Work", m.getResources().get(0).getName());
        assertEquals(0x112233, m.getResources().get(0).getColor());
        CalendarQuery first = source.queries.get(0);
        assertTrue(first.getStartTime().toEpochMilli() < at(5, 0), "more than the range is loaded");
        assertTrue(first.getEndTime().toEpochMilli() > at(8, 0));

        List<ScheduleItem> all = items(m, at(5, 0), at(8, 0));
        assertNotNull(find(all, "a"));
        assertNotNull(find(all, "b"));
        assertNotNull(find(all, "c"), "the second page");
        assertNull(find(all, "gone"), "a cancelled event is not shown");
        ScheduleItem trip = find(all, "trip");
        assertTrue(trip.isAllDay());
        assertEquals(at(6, 0), trip.getStart());
        assertEquals(at(8, 0), trip.getEnd());
        assertFalse(trip.isMovable(), "a read only calendar");
        ScheduleItem run = find(all, "run@" + at(7, 7));
        assertNotNull(run, "an occurrence is told from the others of its series by its start");
        assertTrue(run.isRecurring());
        ScheduleItem busy = null;
        for (ScheduleItem i : all) {
            if (i.getKind() == ScheduleItem.Kind.BLOCKED) {
                assertNull(busy, "free time is not an item");
                busy = i;
            }
        }
        assertNotNull(busy);
        assertEquals("boss", busy.getResourceId());
        assertEquals(at(5, 13), busy.getStart());
        assertNull(CalendarScheduleModel.eventOf(busy));

        // Inside what is loaded nothing is asked again.
        int asked = source.queries.size();
        m.setVisibleRange(at(6, 0), at(7, 0));
        flushSerialCalls();
        assertEquals(asked, source.queries.size());
        m.setVisibleRange(at(20, 0), at(23, 0));
        flushSerialCalls();
        assertTrue(source.queries.size() > asked);

        m.removeCalendar("boss");
        assertEquals(2, m.getResources().size());
        m.dispose();
    }

    @FormTest
    void anEditIsSavedAndPutBackWhenTheSourceRefuses() {
        ListSource source = new ListSource();
        CalendarEvent a = event("work", "a", at(5, 9), at(5, 10));
        source.events.add(a);
        CalendarScheduleModel m = new CalendarScheduleModel(source, UTC);
        m.addCalendar(new CalendarInfo().setId("work").setName("Work"));
        m.addCalendar(new CalendarInfo().setId("home").setName("Home"));
        m.setVisibleRange(at(5, 0), at(6, 0));
        flushSerialCalls();
        ScheduleItem item = find(items(m, at(5, 0), at(6, 0)), "a");
        assertSame(a, CalendarScheduleModel.eventOf(item));

        assertTrue(m.updateItem(item, at(5, 14), at(5, 16), "home"));
        flushSerialCalls();
        assertEquals(1, source.saves);
        assertEquals(CalendarMutationScope.THIS_INSTANCE, source.scopes.get(0));
        assertEquals(at(5, 14), a.getStart().getDateTime().toInstant().toEpochMilli());
        assertEquals(at(5, 16), a.getEnd().getDateTime().toInstant().toEpochMilli());
        assertEquals("home", a.getCalendarId());
        assertEquals("home", item.getResourceId());
        assertNull(m.getLastError());

        source.failSave = true;
        assertTrue(m.updateItem(item, at(5, 17), at(5, 18), "work"));
        flushSerialCalls();
        assertNotNull(m.getLastError());
        assertEquals(at(5, 14), item.getStart(), "the item is where it was");
        assertEquals("home", item.getResourceId());
        assertEquals("home", a.getCalendarId());
        assertEquals(at(5, 14), a.getStart().getDateTime().toInstant().toEpochMilli());
        // A refused save is followed by a load, which is what the source
        // really holds: the event is found again by its id.
        ScheduleItem again = find(items(m, at(5, 0), at(6, 0)), "a");
        assertNotNull(again);
        assertEquals(at(5, 14), again.getStart());
        assertEquals("home", again.getResourceId());

        // A new event that is refused is taken out again.
        assertNull(m.createItem(at(5, 11), at(5, 12), "work"), "creation is off until it is asked for");
        m.setCreationEnabled(true, "New");
        int before = items(m, at(5, 0), at(6, 0)).size();
        ScheduleItem refused = m.createItem(at(5, 11), at(5, 12), "work");
        flushSerialCalls();
        assertNotNull(refused);
        assertEquals(before, items(m, at(5, 0), at(6, 0)).size());

        source.failSave = false;
        ScheduleItem made = m.createItem(at(5, 11), at(5, 12), "work");
        flushSerialCalls();
        assertEquals("New", made.getTitle());
        assertEquals(before + 1, items(m, at(5, 0), at(6, 0)).size());
        assertNotNull(CalendarScheduleModel.eventOf(made).getId());

        source.failDelete = true;
        assertTrue(m.removeItem(item));
        flushSerialCalls();
        assertNotNull(find(items(m, at(5, 0), at(6, 0)), "a"), "a refused delete leaves the item");
        source.failDelete = false;
        assertTrue(m.removeItem(item));
        flushSerialCalls();
        assertNull(find(items(m, at(5, 0), at(6, 0)), "a"));
        assertEquals("a", source.deleted.get(0));
        m.dispose();
    }
}
