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

import com.codename1.io.Util;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Microsoft Graph calendar and Microsoft To Do source. No client secret or
/// refresh token is stored by this class.
public class MicrosoftCalendarSource extends OAuthCalendarSource {

    public static final String SCOPE_CALENDARS = "Calendars.ReadWrite";

    public static final String SCOPE_TASKS = "Tasks.ReadWrite";

    private static final String GRAPH = "https://graph.microsoft.com/v1.0";

    public MicrosoftCalendarSource(CalendarTokenProvider tokens) {
        this(tokens, null);
    }

    public MicrosoftCalendarSource(CalendarTokenProvider tokens, CalendarHttpTransport transport) {
        super("microsoft", "Microsoft 365", tokens, transport, new String[] { SCOPE_CALENDARS, SCOPE_TASKS, "offline_access" });
    }

    @Override
    public CalendarCapabilities getCapabilities() {
        return CalendarCapabilities.of(CalendarCapability.READ_CALENDARS, CalendarCapability.MANAGE_CALENDARS, CalendarCapability.READ_EVENTS, CalendarCapability.WRITE_EVENTS, CalendarCapability.DELETE_EVENTS, CalendarCapability.READ_TASKS, CalendarCapability.WRITE_TASKS, CalendarCapability.DELETE_TASKS, CalendarCapability.RECURRENCE, CalendarCapability.ATTENDEES_READ, CalendarCapability.ATTENDEES_WRITE, CalendarCapability.RESPOND_TO_INVITATIONS, CalendarCapability.ALARMS, CalendarCapability.FREE_BUSY, CalendarCapability.ATTACHMENTS, CalendarCapability.CONFERENCING, CalendarCapability.OFFLINE_MUTATIONS);
    }

    @Override
    public AsyncResource<CalendarPage<CalendarInfo>> listCalendars(final CalendarInfo.ContentType type, String pageToken) {
        final AsyncResource<CalendarPage<CalendarInfo>> out = new AsyncResource<CalendarPage<CalendarInfo>>();
        if (invalidPageToken(pageToken)) {
            return fail(out, "Invalid page token");
        }
        String url = pageToken != null ? pageToken : (type == CalendarInfo.ContentType.TASKS ? GRAPH + "/me/todo/lists" : GRAPH + "/me/calendars");
        json("GET", url, null, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> root) {
                List<CalendarInfo> items = new ArrayList<CalendarInfo>();
                for (Map<String, Object> m : maps(root.get("value"))) {
                    items.add(calendar(m, type));
                }
                out.complete(new CalendarPage<CalendarInfo>(items, string(root, "@odata.nextLink"), null));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarInfo> saveCalendar(final CalendarInfo calendar) {
        final AsyncResource<CalendarInfo> out = new AsyncResource<CalendarInfo>();
        boolean task = calendar.getContentType() == CalendarInfo.ContentType.TASKS;
        Map<String, Object> body = new HashMap<String, Object>();
        body.put(task ? "displayName" : "name", calendar.getName());
        String base = task ? GRAPH + "/me/todo/lists" : GRAPH + "/me/calendars";
        String url = calendar.getId() == null ? base : base + "/" + e(calendar.getId());
        final CalendarInfo.ContentType type = calendar.getContentType();
        json(calendar.getId() == null ? "POST" : "PATCH", url, body, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> m) {
                out.complete(calendar(m, type));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<Boolean> deleteCalendar(String calendarId) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        json("DELETE", GRAPH + "/me/calendars/" + e(calendarId), null, null).ready(done(out)).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<Boolean> deleteCalendar(CalendarInfo calendar) {
        if (calendar == null || calendar.getId() == null) {
            return fail(new AsyncResource<Boolean>(), "calendar and id required");
        }
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        String url = calendar.getContentType() == CalendarInfo.ContentType.TASKS
                ? GRAPH + "/me/todo/lists/" + e(calendar.getId())
                : GRAPH + "/me/calendars/" + e(calendar.getId());
        json("DELETE", url, null, null).ready(done(out)).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarPage<CalendarEvent>> queryEvents(CalendarQuery query) {
        final AsyncResource<CalendarPage<CalendarEvent>> out = new AsyncResource<CalendarPage<CalendarEvent>>();
        if (query == null || query.getCalendarId() == null) {
            return fail(out, "calendarId required");
        }
        String url;
        if (query.getPageToken() != null) {
            if (invalidPageToken(query.getPageToken())) {
                return fail(out, "Invalid page token");
            }
            url = query.getPageToken();
        } else {
            if (query.getSyncToken() != null) {
                return fail(out, "Microsoft Graph v1.0 does not support delta tokens for arbitrary calendars");
            }
            boolean range = query.getStartTime() != null || query.getEndTime() != null;
            StringBuilder b = new StringBuilder(GRAPH).append("/me/calendars/")
                    .append(e(query.getCalendarId())).append(range ? "/calendarView?" : "/events?$top=" + query.getPageSize());
            if (range) {
                long now = System.currentTimeMillis();
                Instant start = query.getStartTime() == null ? Instant.ofEpochMilli(now - 315360000000L) : query.getStartTime();
                Instant end = query.getEndTime() == null ? Instant.ofEpochMilli(now + 315360000000L) : query.getEndTime();
                b.append("startDateTime=").append(e(iso(start))).append("&endDateTime=").append(e(iso(end)))
                        .append("&$top=").append(query.getPageSize());
            }
            url = b.toString();
        }
        final String calendarId = query.getCalendarId();
        json("GET", url, null, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> root) {
                List<CalendarEvent> items = new ArrayList<CalendarEvent>();
                try {
                    for (Map<String, Object> m : maps(root.get("value"))) {
                        items.add(event(m, calendarId));
                    }
                } catch (CalendarException ex) {
                    out.error(ex);
                    return;
                }
                out.complete(new CalendarPage<CalendarEvent>(items, string(root, "@odata.nextLink"), null));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarEvent> getEvent(final String calendarId, String eventId) {
        final AsyncResource<CalendarEvent> out = new AsyncResource<CalendarEvent>();
        json("GET", GRAPH + "/me/calendars/" + e(calendarId) + "/events/" + e(eventId), null, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> m) {
                try {
                    out.complete(event(m, calendarId));
                } catch (CalendarException ex) {
                    out.error(ex);
                }
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarEvent> saveEvent(final CalendarEvent event, CalendarMutationScope scope) {
        final AsyncResource<CalendarEvent> out = new AsyncResource<CalendarEvent>();
        if (event == null || event.getCalendarId() == null) {
            return fail(out, "event and calendarId required");
        }
        String base = GRAPH + "/me/calendars/" + e(event.getCalendarId()) + "/events";
        String url = event.getId() == null ? base : base + "/" + e(event.getId());
        json(event.getId() == null ? "POST" : "PATCH", url, eventMap(event), version(event.getVersion())).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> m) {
                try {
                    CalendarEvent saved = event(m, event.getCalendarId());
                    out.complete(saved);
                    fireChange(new CalendarChange(getId(), saved.getCalendarId(), saved.getId(), CalendarChange.EntityType.EVENT, event.getId() == null ? CalendarChange.ChangeType.CREATED : CalendarChange.ChangeType.UPDATED));
                } catch (CalendarException ex) {
                    out.error(ex);
                }
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<Boolean> deleteEvent(final String calendarId, final String eventId, CalendarMutationScope scope, String version) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        json("DELETE", GRAPH + "/me/calendars/" + e(calendarId) + "/events/" + e(eventId), null, version(version)).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> x) {
                out.complete(Boolean.TRUE);
                fireChange(new CalendarChange(getId(), calendarId, eventId, CalendarChange.EntityType.EVENT, CalendarChange.ChangeType.DELETED));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarEvent> respondToEvent(final String calendarId, final String eventId, CalendarAttendee.Response response, String comment) {
        final AsyncResource<CalendarEvent> out = new AsyncResource<CalendarEvent>();
        if (response != CalendarAttendee.Response.ACCEPTED
                && response != CalendarAttendee.Response.DECLINED
                && response != CalendarAttendee.Response.TENTATIVE) {
            return fail(out, "response must be ACCEPTED, DECLINED, or TENTATIVE");
        }
        String action = response == CalendarAttendee.Response.ACCEPTED ? "accept" : response == CalendarAttendee.Response.DECLINED ? "decline" : "tentativelyAccept";
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("comment", comment);
        body.put("sendResponse", Boolean.TRUE);
        json("POST", GRAPH + "/me/events/" + e(eventId) + "/" + action, body, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> x) {
                getEvent(calendarId, eventId).ready(new SuccessCallback<CalendarEvent>() {

                    @Override
                    public void onSucess(CalendarEvent event) {
                        out.complete(event);
                    }
                }).except(error(out));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<List<FreeBusyInterval>> queryFreeBusy(List<String> ids, Instant start, Instant end) {
        final AsyncResource<List<FreeBusyInterval>> out = new AsyncResource<List<FreeBusyInterval>>();
        if (ids == null || start == null || end == null) {
            return fail(out, "calendar ids, start, and end required");
        }
        collectFreeBusy(ids, 0, start, end, null, new ArrayList<FreeBusyInterval>(), out);
        return out;
    }

    private void collectFreeBusy(final List<String> ids, final int index, final Instant start,
            final Instant end, String pageToken, final List<FreeBusyInterval> result,
            final AsyncResource<List<FreeBusyInterval>> out) {
        if (index >= ids.size()) {
            out.complete(result);
            return;
        }
        CalendarQuery query = new CalendarQuery().setCalendarId(ids.get(index)).setStartTime(start)
                .setEndTime(end).setPageSize(1000).setPageToken(pageToken);
        queryEvents(query).ready(new SuccessCallback<CalendarPage<CalendarEvent>>() {
            @Override
            public void onSucess(CalendarPage<CalendarEvent> page) {
                for (CalendarEvent event : page.getItems()) {
                    if (event.getAvailability() != CalendarEvent.Availability.FREE
                            && event.getStart() != null && event.getEnd() != null
                            && !event.getStart().isAllDay() && !event.getEnd().isAllDay()) {
                        result.add(new FreeBusyInterval(event.getStart().getDateTime().toInstant(),
                                event.getEnd().getDateTime().toInstant(), event.getAvailability()));
                    }
                }
                if (page.getNextPageToken() != null) {
                    collectFreeBusy(ids, index, start, end, page.getNextPageToken(), result, out);
                } else {
                    collectFreeBusy(ids, index + 1, start, end, null, result, out);
                }
            }
        }).except(error(out));
    }

    @Override
    public AsyncResource<CalendarPage<CalendarTask>> queryTasks(CalendarQuery query) {
        final AsyncResource<CalendarPage<CalendarTask>> out = new AsyncResource<CalendarPage<CalendarTask>>();
        if (query == null || query.getCalendarId() == null) {
            return fail(out, "calendarId required");
        }
        if (invalidPageToken(query.getPageToken())) {
            return fail(out, "Invalid page token");
        }
        String url = query.getPageToken() != null ? query.getPageToken() : GRAPH + "/me/todo/lists/" + e(query.getCalendarId()) + "/tasks?$top=" + query.getPageSize();
        final String list = query.getCalendarId();
        json("GET", url, null, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> root) {
                List<CalendarTask> items = new ArrayList<CalendarTask>();
                try {
                    for (Map<String, Object> m : maps(root.get("value"))) {
                        items.add(task(m, list));
                    }
                } catch (CalendarException ex) {
                    out.error(ex);
                    return;
                }
                out.complete(new CalendarPage<CalendarTask>(items, string(root, "@odata.nextLink"), null));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarTask> getTask(final String list, String id) {
        final AsyncResource<CalendarTask> out = new AsyncResource<CalendarTask>();
        json("GET", GRAPH + "/me/todo/lists/" + e(list) + "/tasks/" + e(id), null, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> m) {
                try {
                    out.complete(task(m, list));
                } catch (CalendarException ex) {
                    out.error(ex);
                }
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarTask> saveTask(final CalendarTask task, CalendarMutationScope scope) {
        final AsyncResource<CalendarTask> out = new AsyncResource<CalendarTask>();
        if (task == null || task.getCalendarId() == null) {
            return fail(out, "task and calendarId required");
        }
        String base = GRAPH + "/me/todo/lists/" + e(task.getCalendarId()) + "/tasks";
        String url = task.getId() == null ? base : base + "/" + e(task.getId());
        json(task.getId() == null ? "POST" : "PATCH", url, taskMap(task), version(task.getVersion())).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> m) {
                try {
                    CalendarTask saved = task(m, task.getCalendarId());
                    out.complete(saved);
                    fireChange(new CalendarChange(getId(), saved.getCalendarId(), saved.getId(), CalendarChange.EntityType.TASK, task.getId() == null ? CalendarChange.ChangeType.CREATED : CalendarChange.ChangeType.UPDATED));
                } catch (CalendarException ex) {
                    out.error(ex);
                }
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<Boolean> deleteTask(final String list, final String id, CalendarMutationScope scope, String version) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        json("DELETE", GRAPH + "/me/todo/lists/" + e(list) + "/tasks/" + e(id), null, version(version)).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> x) {
                out.complete(Boolean.TRUE);
                fireChange(new CalendarChange(getId(), list, id, CalendarChange.EntityType.TASK, CalendarChange.ChangeType.DELETED));
            }
        }).except(error(out));
        return out;
    }

    private CalendarInfo calendar(Map<String, Object> m, CalendarInfo.ContentType type) {
        boolean taskList = type == CalendarInfo.ContentType.TASKS;
        return new CalendarInfo().setId(string(m, "id")).setSourceId(getId()).setName(string(m, taskList ? "displayName" : "name")).setOwner(string(map(m.get("owner")), "address")).setReadOnly(taskList ? !bool(m, "isOwner") : !bool(m, "canEdit")).setPrimary(bool(m, "isDefaultCalendar")).setContentType(type).setCapabilities(getCapabilities());
    }

    private CalendarEvent event(Map<String, Object> m, String calendarId) throws CalendarException {
        CalendarEvent out = new CalendarEvent().setId(string(m, "id")).setCalendarId(calendarId).setSourceId(getId()).setVersion(string(m, "@odata.etag")).setTitle(string(m, "subject")).setDescription(string(map(m.get("body")), "content")).setLocation(string(map(m.get("location")), "displayName")).setUrl(string(m, "webLink")).setStart(graphDate(map(m.get("start")))).setEnd(graphDate(map(m.get("end")))).setRecurringEventId(string(m, "seriesMasterId"));
        out.setRecurrence(readGraphRecurrence(map(m.get("recurrence"))));
        if (bool(m, "isCancelled")) {
            out.setStatus(CalendarEvent.Status.CANCELED);
        }
        if (bool(m, "isAllDay")) {
            out.setStart(toAllDay(out.getStart())).setEnd(toAllDay(out.getEnd()));
        }
        String show = string(m, "showAs");
        if ("free".equals(show)) {
            out.setAvailability(CalendarEvent.Availability.FREE);
        } else if ("tentative".equals(show)) {
            out.setAvailability(CalendarEvent.Availability.TENTATIVE);
        } else if ("oof".equals(show)) {
            out.setAvailability(CalendarEvent.Availability.OUT_OF_OFFICE);
        }
        for (Map<String, Object> a : maps(m.get("attendees"))) {
            Map<String, Object> email = map(a.get("emailAddress"));
            CalendarAttendee attendee = new CalendarAttendee().setName(string(email, "name")).setEmail(string(email, "address"));
            if ("optional".equals(string(a, "type"))) {
                attendee.setRole(CalendarAttendee.Role.OPTIONAL);
            } else if ("resource".equals(string(a, "type"))) {
                attendee.setRole(CalendarAttendee.Role.RESOURCE);
            }
            Map<String, Object> status = map(a.get("status"));
            String response = string(status, "response");
            attendee.setResponse(attendeeResponse(response));
            out.addAttendee(attendee);
        }
        if (bool(m, "isReminderOn")) {
            Integer minutes = integer(m, "reminderMinutesBeforeStart");
            if (minutes != null) {
                out.addAlarm(new CalendarAlarm().setTimeBefore(Duration.ofMinutes(minutes.intValue())));
            }
        }
        Map<String, Object> meeting = map(m.get("onlineMeeting"));
        if (!meeting.isEmpty()) {
            out.setConference(new CalendarConference().setProvider(string(m, "onlineMeetingProvider")).setJoinUrl(string(meeting, "joinUrl")));
        }
        return out;
    }

    private Map<String, Object> eventMap(CalendarEvent e) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("subject", e.getTitle());
        if (e.getRecurrence() != null) {
            m.put("recurrence", writeGraphRecurrence(e));
        }
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("contentType", "text");
        body.put("content", e.getDescription());
        m.put("body", body);
        Map<String, Object> location = new HashMap<String, Object>();
        location.put("displayName", e.getLocation());
        m.put("location", location);
        m.put("start", dateMap(e.getStart()));
        m.put("end", dateMap(e.getEnd()));
        m.put("isAllDay", Boolean.valueOf(e.getStart() != null && e.getStart().isAllDay()));
        m.put("showAs", e.getAvailability() == CalendarEvent.Availability.FREE ? "free" : e.getAvailability() == CalendarEvent.Availability.TENTATIVE ? "tentative" : e.getAvailability() == CalendarEvent.Availability.OUT_OF_OFFICE ? "oof" : "busy");
        List<Map<String, Object>> attendees = new ArrayList<Map<String, Object>>();
        for (CalendarAttendee a : e.getAttendees()) {
            Map<String, Object> x = new HashMap<String, Object>();
            Map<String, Object> address = new HashMap<String, Object>();
            address.put("address", a.getEmail());
            address.put("name", a.getName());
            x.put("emailAddress", address);
            x.put("type", a.getRole() == CalendarAttendee.Role.OPTIONAL ? "optional" : a.getRole() == CalendarAttendee.Role.RESOURCE ? "resource" : "required");
            attendees.add(x);
        }
        m.put("attendees", attendees);
        if (!e.getAlarms().isEmpty() && e.getAlarms().get(0).getTimeBefore() != null && e.getAlarms().get(0).getTimeBefore().toMillis() % 60000L == 0L) {
            m.put("isReminderOn", Boolean.TRUE);
            m.put("reminderMinutesBeforeStart", Long.valueOf(e.getAlarms().get(0).getTimeBefore().toMillis() / 60000L));
        }
        if (e.getConference() != null && e.getConference().isCreateRequested()) {
            m.put("isOnlineMeeting", Boolean.TRUE);
            m.put("onlineMeetingProvider", e.getConference().getProvider() == null ? "teamsForBusiness" : e.getConference().getProvider());
        }
        if (e.getId() == null && e.getProviderData().get("cn1.mutationId") != null) {
            m.put("transactionId", e.getProviderData().get("cn1.mutationId"));
        }
        return m;
    }

    private CalendarTask task(Map<String, Object> m, String list) throws CalendarException {
        CalendarTask out = new CalendarTask().setId(string(m, "id")).setCalendarId(list).setSourceId(getId()).setVersion(string(m, "@odata.etag")).setTitle(string(m, "title")).setDescription(string(map(m.get("body")), "content")).setCompleted("completed".equals(string(m, "status"))).setPriority("high".equals(string(m, "importance")) ? 1 : "low".equals(string(m, "importance")) ? 9 : 5);
        if (m.get("startDateTime") instanceof Map) {
            out.setStart(graphDate(map(m.get("startDateTime"))));
        }
        if (m.get("dueDateTime") instanceof Map) {
            out.setDue(graphDate(map(m.get("dueDateTime"))));
        }
        if (m.get("completedDateTime") instanceof Map) {
            CalendarDateTime completed = graphDate(map(m.get("completedDateTime")));
            if (completed != null) {
                out.setCompletionTime(completed.getDateTime().toInstant());
            }
        }
        if (bool(m, "isReminderOn") && m.get("reminderDateTime") instanceof Map) {
            CalendarDateTime reminder = graphDate(map(m.get("reminderDateTime")));
            if (reminder != null) {
                out.addAlarm(new CalendarAlarm().setAbsoluteTime(reminder.getDateTime().toInstant()));
            }
        }
        return out;
    }

    private Map<String, Object> taskMap(CalendarTask t) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("title", t.getTitle());
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("contentType", "text");
        body.put("content", t.getDescription());
        m.put("body", body);
        m.put("status", t.isCompleted() ? "completed" : "notStarted");
        m.put("importance", t.getPriority() > 0 && t.getPriority() < 5 ? "high" : t.getPriority() > 5 ? "low" : "normal");
        if (t.getStart() != null) {
            m.put("startDateTime", dateMap(t.getStart()));
        }
        if (t.getDue() != null) {
            m.put("dueDateTime", dateMap(t.getDue()));
        }
        if (!t.getAlarms().isEmpty() && t.getAlarms().get(0).getAbsoluteTime() != null) {
            m.put("isReminderOn", Boolean.TRUE);
            m.put("reminderDateTime", dateMap(CalendarDateTime.instant(t.getAlarms().get(0).getAbsoluteTime(), ZoneId.of("UTC"))));
        }
        return m;
    }

    private static CalendarDateTime graphDate(Map<String, Object> m) throws CalendarException {
        String date = string(m, "dateTime");
        String zone = string(m, "timeZone");
        if (date == null) {
            return null;
        }
        String timeZone = zone == null ? "UTC" : zone;
        try {
            ZoneId zoneId = CalendarDateUtil.zoneId(timeZone);
            return CalendarDateTime.instant(CalendarDateUtil.parseDateTime(date, zoneId), zoneId);
        } catch (IllegalArgumentException ex) {
            throw new CalendarException(CalendarError.MALFORMED_RESPONSE, "Invalid Graph date: " + date, ex);
        }
    }

    private static Map<String, Object> dateMap(CalendarDateTime d) {
        Map<String, Object> m = new HashMap<String, Object>();
        if (d == null) {
            return m;
        }
        Instant time = d.isAllDay() ? CalendarDateUtil.allDayInstant(d.getDate()) : d.getDateTime().toInstant();
        ZoneId zone = d.isAllDay() ? ZoneId.of("UTC") : d.getDateTime().getZone();
        m.put("dateTime", CalendarDateUtil.formatIso(time, zone, false));
        m.put("timeZone", zone.getId());
        return m;
    }

    private static CalendarDateTime toAllDay(CalendarDateTime d) {
        if (d == null) {
            return null;
        }
        return CalendarDateTime.allDay(d.getDateTime().toLocalDateTime().toLocalDate());
    }

    private static String iso(Instant time) {
        return CalendarDateUtil.formatIso(time, ZoneId.of("UTC"), false) + "Z";
    }

    private static String e(String value) {
        return Util.encodeUrl(value == null ? "" : value);
    }

    private static Map<String, String> version(String v) {
        if (v == null) {
            return null;
        }
        Map<String, String> m = new HashMap<String, String>();
        m.put("If-Match", v);
        return m;
    }

    private static String string(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        return v == null ? null : String.valueOf(v);
    }

    private static boolean bool(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        return Boolean.TRUE.equals(v) || "true".equals(String.valueOf(v));
    }

    private static Integer integer(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Number ? Integer.valueOf(((Number) v).intValue()) : v == null ? null : Integer.valueOf(String.valueOf(v));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object v) {
        return v instanceof Map ? (Map<String, Object>) v : new HashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object v) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        if (v instanceof List) {
            for (Object x : (List<Object>) v) {
                if (x instanceof Map) {
                    out.add((Map<String, Object>) x);
                }
            }
        }
        return out;
    }

    private static String camelEnum(String v) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (Character.isUpperCase(c)) {
                b.append('_').append(c);
            } else {
                b.append(Character.toUpperCase(c));
            }
        }
        String out = b.toString();
        return "NOT_STARTED".equals(out) ? "NONE" : "TENTATIVELY_ACCEPTED".equals(out) ? "TENTATIVE" : out;
    }

    private static CalendarAttendee.Response attendeeResponse(String value) {
        String name = value == null ? null : camelEnum(value);
        for (CalendarAttendee.Response candidate : CalendarAttendee.Response.values()) {
            if (candidate.name().equals(name)) {
                return candidate;
            }
        }
        return CalendarAttendee.Response.NONE;
    }

    private static <T> SuccessCallback<Throwable> error(final AsyncResource<T> out) {
        return new SuccessCallback<Throwable>() {

            @Override
            public void onSucess(Throwable error) {
                out.error(error);
            }
        };
    }

    private static SuccessCallback<Map<String, Object>> done(final AsyncResource<Boolean> out) {
        return new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> x) {
                out.complete(Boolean.TRUE);
            }
        };
    }

    private static <T> AsyncResource<T> fail(AsyncResource<T> out, String message) {
        out.error(new CalendarException(CalendarError.INVALID_ARGUMENT, message));
        return out;
    }

    private static boolean invalidPageToken(String pageToken) {
        // Page tokens are @odata.nextLink URLs that are sent with a Bearer
        // token attached; only Graph URLs may ever be dispatched.
        return pageToken != null && !pageToken.startsWith(GRAPH + "/");
    }

    private static CalendarRecurrenceRule readGraphRecurrence(Map<String, Object> recurrence)
            throws CalendarException {
        if (recurrence.isEmpty()) {
            return null;
        }
        Map<String, Object> pattern = map(recurrence.get("pattern"));
        Map<String, Object> range = map(recurrence.get("range"));
        String type = string(pattern, "type");
        CalendarRecurrenceRule.Frequency frequency = "daily".equals(type) ? CalendarRecurrenceRule.Frequency.DAILY : "weekly".equals(type) ? CalendarRecurrenceRule.Frequency.WEEKLY : containsIgnoreCase(type, "yearly") ? CalendarRecurrenceRule.Frequency.YEARLY : CalendarRecurrenceRule.Frequency.MONTHLY;
        Integer interval = integer(pattern, "interval");
        CalendarRecurrenceRule out = new CalendarRecurrenceRule().setFrequency(frequency).setInterval(Math.max(1, interval == null ? 1 : interval.intValue()));
        for (Object day : list(pattern.get("daysOfWeek"))) {
            out.addDayOfWeek(graphDay(String.valueOf(day)));
        }
        Integer day = integer(pattern, "dayOfMonth");
        Integer month = integer(pattern, "month");
        if (day != null) {
            out.addDayOfMonth(day.intValue());
        }
        if (month != null) {
            out.addMonth(month.intValue());
        }
        Integer count = integer(range, "numberOfOccurrences");
        if (count != null) {
            out.setCount(count);
        }
        String end = string(range, "endDate");
        if (end != null) {
            try {
                out.setUntil(CalendarDateTime.allDay(CalendarDateUtil.parseDate(end)));
            } catch (IllegalArgumentException ex) {
                throw new CalendarException(CalendarError.MALFORMED_RESPONSE,
                        "Invalid Graph recurrence date: " + end, ex);
            }
        }
        return out;
    }

    private static Map<String, Object> writeGraphRecurrence(CalendarEvent event) {
        CalendarRecurrenceRule rule = event.getRecurrence();
        Map<String, Object> out = new HashMap<String, Object>();
        Map<String, Object> pattern = new HashMap<String, Object>();
        Map<String, Object> range = new HashMap<String, Object>();
        String type = rule.getFrequency() == CalendarRecurrenceRule.Frequency.DAILY ? "daily" : rule.getFrequency() == CalendarRecurrenceRule.Frequency.WEEKLY ? "weekly" : rule.getFrequency() == CalendarRecurrenceRule.Frequency.YEARLY ? "absoluteYearly" : "absoluteMonthly";
        pattern.put("type", type);
        pattern.put("interval", Integer.valueOf(rule.getInterval()));
        List<String> days = new ArrayList<String>();
        for (Integer day : rule.getDaysOfWeek()) {
            days.add(graphDay(day.intValue()));
        }
        if (!days.isEmpty()) {
            pattern.put("daysOfWeek", days);
        }
        if (!rule.getDaysOfMonth().isEmpty()) {
            pattern.put("dayOfMonth", rule.getDaysOfMonth().get(0));
        }
        if (!rule.getMonths().isEmpty()) {
            pattern.put("month", rule.getMonths().get(0));
        }
        LocalDate start = event.getStart() != null && event.getStart().isAllDay() ? event.getStart().getDate() : dateFor(event.getStart());
        range.put("startDate", start == null ? "1970-01-01" : start.toString());
        if (rule.getCount() != null) {
            range.put("type", "numbered");
            range.put("numberOfOccurrences", rule.getCount());
        } else if (rule.getUntil() != null) {
            range.put("type", "endDate");
            range.put("endDate", rule.getUntil().isAllDay() ? rule.getUntil().getDate().toString() : dateFor(rule.getUntil()).toString());
        } else {
            range.put("type", "noEnd");
        }
        out.put("pattern", pattern);
        out.put("range", range);
        return out;
    }

    private static LocalDate dateFor(CalendarDateTime value) {
        return value == null ? null : value.getDateTime().toLocalDateTime().toLocalDate();
    }

    private static int graphDay(String value) throws CalendarException {
        String[] days = { "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" };
        for (int i = 0; i < days.length; i++) {
            if (days[i].equalsIgnoreCase(value)) {
                return i + 1;
            }
        }
        throw new CalendarException(CalendarError.MALFORMED_RESPONSE,
                "Invalid Graph recurrence weekday: " + value);
    }

    private static String graphDay(int value) {
        return new String[] { "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" }[value - 1];
    }

    private static boolean containsIgnoreCase(String value, String target) {
        if (value == null) {
            return false;
        }
        int limit = value.length() - target.length();
        for (int i = 0; i <= limit; i++) {
            if (value.regionMatches(true, i, target, 0, target.length())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return value instanceof List ? (List<Object>) value : new ArrayList<Object>();
    }
}
