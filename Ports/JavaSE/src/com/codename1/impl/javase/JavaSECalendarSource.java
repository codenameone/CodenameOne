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

import com.codename1.calendar.CalendarAccess;
import com.codename1.calendar.CalendarAttendee;
import com.codename1.calendar.CalendarAuthorizationStatus;
import com.codename1.calendar.CalendarCapabilities;
import com.codename1.calendar.CalendarCapability;
import com.codename1.calendar.CalendarChange;
import com.codename1.calendar.CalendarEvent;
import com.codename1.calendar.CalendarInfo;
import com.codename1.calendar.CalendarMutationScope;
import com.codename1.calendar.CalendarPage;
import com.codename1.calendar.CalendarQuery;
import com.codename1.calendar.CalendarTask;
import com.codename1.calendar.FreeBusyInterval;
import com.codename1.calendar.LocalCalendarSource;
import com.codename1.util.AsyncResource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic in-memory calendar used by the simulator and desktop run. */
final class JavaSECalendarSource extends LocalCalendarSource {
    private final Map<String,CalendarInfo> calendars = new LinkedHashMap<String,CalendarInfo>();
    private final Map<String,CalendarEvent> events = new LinkedHashMap<String,CalendarEvent>();
    private final Map<String,CalendarTask> tasks = new LinkedHashMap<String,CalendarTask>();
    private int nextId = 1;
    private final CalendarCapabilities capabilities = CalendarCapabilities.of(
            CalendarCapability.READ_CALENDARS, CalendarCapability.MANAGE_CALENDARS,
            CalendarCapability.READ_EVENTS, CalendarCapability.WRITE_EVENTS, CalendarCapability.DELETE_EVENTS,
            CalendarCapability.READ_TASKS, CalendarCapability.WRITE_TASKS, CalendarCapability.DELETE_TASKS,
            CalendarCapability.RECURRENCE, CalendarCapability.ATTENDEES_READ,
            CalendarCapability.ATTENDEES_WRITE, CalendarCapability.RESPOND_TO_INVITATIONS,
            CalendarCapability.ALARMS, CalendarCapability.FREE_BUSY, CalendarCapability.ATTACHMENTS,
            CalendarCapability.CONFERENCING, CalendarCapability.LOCAL_CHANGE_LISTENER,
            CalendarCapability.OFFLINE_MUTATIONS);

    JavaSECalendarSource() {
        CalendarInfo eventCalendar = new CalendarInfo().setId("sim-events").setSourceId(getId())
                .setName("Simulator Calendar").setPrimary(true).setCapabilities(capabilities);
        CalendarInfo taskCalendar = new CalendarInfo().setId("sim-tasks").setSourceId(getId())
                .setName("Simulator Tasks").setContentType(CalendarInfo.ContentType.TASKS).setCapabilities(capabilities);
        calendars.put(eventCalendar.getId(), eventCalendar);
        calendars.put(taskCalendar.getId(), taskCalendar);
    }
    public boolean isAvailable() { return true; }
    public CalendarCapabilities getCapabilities() { return capabilities; }
    public CalendarAuthorizationStatus getAuthorizationStatus(CalendarAccess access) { return CalendarAuthorizationStatus.FULL; }
    public AsyncResource<CalendarAuthorizationStatus> requestAuthorization(CalendarAccess access) { return completed(CalendarAuthorizationStatus.FULL); }
    public synchronized AsyncResource<CalendarPage<CalendarInfo>> listCalendars(CalendarInfo.ContentType type, String token) {
        List<CalendarInfo> out = new ArrayList<CalendarInfo>();
        for (CalendarInfo c : calendars.values()) if (type == null || type == c.getContentType()) out.add(c);
        return completed(new CalendarPage<CalendarInfo>(out, null, String.valueOf(nextId)));
    }
    public synchronized AsyncResource<CalendarInfo> saveCalendar(CalendarInfo calendar) {
        if (calendar.getId() == null) calendar.setId("sim-cal-" + nextId++);
        calendar.setSourceId(getId()).setCapabilities(capabilities);
        calendars.put(calendar.getId(), calendar);
        fireChange(new CalendarChange(getId(), calendar.getId(), null, CalendarChange.EntityType.CALENDAR, CalendarChange.ChangeType.UPDATED));
        return completed(calendar);
    }
    public synchronized AsyncResource<Boolean> deleteCalendar(String id) {
        boolean removed = calendars.remove(id) != null;
        if (removed) fireChange(new CalendarChange(getId(), id, null, CalendarChange.EntityType.CALENDAR, CalendarChange.ChangeType.DELETED));
        return completed(Boolean.valueOf(removed));
    }
    public synchronized AsyncResource<CalendarPage<CalendarEvent>> queryEvents(CalendarQuery query) {
        List<CalendarEvent> out = new ArrayList<CalendarEvent>();
        for (CalendarEvent e : events.values()) {
            if (query != null && query.getCalendarId() != null && !query.getCalendarId().equals(e.getCalendarId())) continue;
            if (query != null && query.getStartTime() != null && e.getEnd() != null && !e.getEnd().isAllDay()
                    && e.getEnd().getDateTime().toInstant().compareTo(query.getStartTime()) < 0) continue;
            if (query != null && query.getEndTime() != null && e.getStart() != null && !e.getStart().isAllDay()
                    && e.getStart().getDateTime().toInstant().compareTo(query.getEndTime()) > 0) continue;
            out.add(e);
        }
        return completed(new CalendarPage<CalendarEvent>(out, null, String.valueOf(nextId)));
    }
    public synchronized AsyncResource<CalendarEvent> getEvent(String calendarId, String id) { return completed(events.get(id)); }
    public synchronized AsyncResource<CalendarEvent> saveEvent(CalendarEvent event, CalendarMutationScope scope) {
        boolean created = event.getId() == null;
        if (created) event.setId("sim-event-" + nextId++);
        if (event.getCalendarId() == null) event.setCalendarId("sim-events");
        event.setSourceId(getId()).setVersion(String.valueOf(nextId));
        events.put(event.getId(), event);
        fireChange(new CalendarChange(getId(), event.getCalendarId(), event.getId(), CalendarChange.EntityType.EVENT,
                created ? CalendarChange.ChangeType.CREATED : CalendarChange.ChangeType.UPDATED));
        return completed(event);
    }
    public synchronized AsyncResource<Boolean> deleteEvent(String calendarId, String id, CalendarMutationScope scope, String version) {
        boolean removed = events.remove(id) != null;
        if (removed) fireChange(new CalendarChange(getId(), calendarId, id, CalendarChange.EntityType.EVENT, CalendarChange.ChangeType.DELETED));
        return completed(Boolean.valueOf(removed));
    }
    public synchronized AsyncResource<CalendarEvent> respondToEvent(String calendarId, String id, CalendarAttendee.Response response, String comment) {
        CalendarEvent event = events.get(id);
        if (event != null) for (CalendarAttendee a : event.getAttendees()) if (a.isSelf()) a.setResponse(response);
        return completed(event);
    }
    public synchronized AsyncResource<List<FreeBusyInterval>> queryFreeBusy(List<String> ids, Instant start, Instant end) {
        List<FreeBusyInterval> out = new ArrayList<FreeBusyInterval>();
        for (CalendarEvent event : events.values()) if (event.getAvailability() != CalendarEvent.Availability.FREE
                && event.getStart() != null && event.getEnd() != null && !event.getStart().isAllDay() && !event.getEnd().isAllDay()
                && event.getEnd().getDateTime().toInstant().compareTo(start) >= 0
                && event.getStart().getDateTime().toInstant().compareTo(end) <= 0)
            out.add(new FreeBusyInterval(event.getStart().getDateTime().toInstant(),
                    event.getEnd().getDateTime().toInstant(), event.getAvailability()));
        return completed(out);
    }
    public synchronized AsyncResource<CalendarPage<CalendarTask>> queryTasks(CalendarQuery query) {
        List<CalendarTask> out = new ArrayList<CalendarTask>();
        for (CalendarTask task : tasks.values()) if (query == null || query.getCalendarId() == null || query.getCalendarId().equals(task.getCalendarId())) out.add(task);
        return completed(new CalendarPage<CalendarTask>(out, null, String.valueOf(nextId)));
    }
    public synchronized AsyncResource<CalendarTask> getTask(String calendarId, String id) { return completed(tasks.get(id)); }
    public synchronized AsyncResource<CalendarTask> saveTask(CalendarTask task, CalendarMutationScope scope) {
        boolean created = task.getId() == null;
        if (created) task.setId("sim-task-" + nextId++);
        if (task.getCalendarId() == null) task.setCalendarId("sim-tasks");
        task.setSourceId(getId()).setVersion(String.valueOf(nextId));
        tasks.put(task.getId(), task);
        fireChange(new CalendarChange(getId(), task.getCalendarId(), task.getId(), CalendarChange.EntityType.TASK,
                created ? CalendarChange.ChangeType.CREATED : CalendarChange.ChangeType.UPDATED));
        return completed(task);
    }
    public synchronized AsyncResource<Boolean> deleteTask(String calendarId, String id, CalendarMutationScope scope, String version) {
        boolean removed = tasks.remove(id) != null;
        if (removed) fireChange(new CalendarChange(getId(), calendarId, id, CalendarChange.EntityType.TASK, CalendarChange.ChangeType.DELETED));
        return completed(Boolean.valueOf(removed));
    }
}
