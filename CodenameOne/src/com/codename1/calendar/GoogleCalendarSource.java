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

/// Google Calendar and Google Tasks implementation. The application supplies
/// OAuth tokens and chooses the scopes granted to its own OAuth client.
public class GoogleCalendarSource extends OAuthCalendarSource {

    public static final String SCOPE_CALENDAR = "https://www.googleapis.com/auth/calendar";

    public static final String SCOPE_TASKS = "https://www.googleapis.com/auth/tasks";

    private static final String CALENDAR_API = "https://www.googleapis.com/calendar/v3";

    private static final String TASKS_API = "https://tasks.googleapis.com/tasks/v1";

    public GoogleCalendarSource(CalendarTokenProvider tokens) {
        this(tokens, null);
    }

    public GoogleCalendarSource(CalendarTokenProvider tokens, CalendarHttpTransport transport) {
        super("google", "Google Calendar", tokens, transport, new String[] { SCOPE_CALENDAR, SCOPE_TASKS });
    }

    @Override
    public CalendarCapabilities getCapabilities() {
        return CalendarCapabilities.of(CalendarCapability.READ_CALENDARS, CalendarCapability.MANAGE_CALENDARS, CalendarCapability.READ_EVENTS, CalendarCapability.WRITE_EVENTS, CalendarCapability.DELETE_EVENTS, CalendarCapability.READ_TASKS, CalendarCapability.WRITE_TASKS, CalendarCapability.DELETE_TASKS, CalendarCapability.RECURRENCE, CalendarCapability.ATTENDEES_READ, CalendarCapability.ATTENDEES_WRITE, CalendarCapability.ALARMS, CalendarCapability.FREE_BUSY, CalendarCapability.ATTACHMENTS, CalendarCapability.CONFERENCING, CalendarCapability.DELTA_SYNC, CalendarCapability.OFFLINE_MUTATIONS);
    }

    @Override
    public AsyncResource<CalendarPage<CalendarInfo>> listCalendars(final CalendarInfo.ContentType type, String pageToken) {
        final AsyncResource<CalendarPage<CalendarInfo>> out = new AsyncResource<CalendarPage<CalendarInfo>>();
        String url = type == CalendarInfo.ContentType.TASKS ? TASKS_API + "/users/@me/lists" : CALENDAR_API + "/users/me/calendarList";
        if (pageToken != null) {
            url += "?pageToken=" + Util.encodeUrl(pageToken);
        }
        json("GET", url, null, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> root) {
                List<CalendarInfo> result = new ArrayList<CalendarInfo>();
                try {
                    for (Map<String, Object> item : maps(root.get("items"))) {
                        result.add(calendar(item, type));
                    }
                } catch (CalendarException ex) {
                    out.error(ex);
                    return;
                }
                out.complete(new CalendarPage<CalendarInfo>(result, string(root, "nextPageToken"), null));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarInfo> saveCalendar(final CalendarInfo calendar) {
        final AsyncResource<CalendarInfo> out = new AsyncResource<CalendarInfo>();
        boolean task = calendar.getContentType() == CalendarInfo.ContentType.TASKS;
        Map<String, Object> body = new HashMap<String, Object>();
        body.put(task ? "title" : "summary", calendar.getName());
        if (!task && calendar.getTimeZone() != null) {
            body.put("timeZone", calendar.getTimeZone().getId());
        }
        String base = task ? TASKS_API + "/users/@me/lists" : CALENDAR_API + "/calendars";
        String method = calendar.getId() == null ? "POST" : "PUT";
        String url = calendar.getId() == null ? base : base + "/" + e(calendar.getId());
        final CalendarInfo.ContentType type = calendar.getContentType();
        json(method, url, body, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> value) {
                try {
                    out.complete(calendar(value, type));
                } catch (CalendarException ex) {
                    out.error(ex);
                }
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<Boolean> deleteCalendar(String calendarId) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        json("DELETE", CALENDAR_API + "/calendars/" + e(calendarId), null, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> ignored) {
                out.complete(Boolean.TRUE);
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<Boolean> deleteCalendar(CalendarInfo calendar) {
        if (calendar == null || calendar.getId() == null) {
            return fail(new AsyncResource<Boolean>(), CalendarError.INVALID_ARGUMENT, "calendar and id required");
        }
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        String url = calendar.getContentType() == CalendarInfo.ContentType.TASKS
                ? TASKS_API + "/users/@me/lists/" + e(calendar.getId())
                : CALENDAR_API + "/calendars/" + e(calendar.getId());
        json("DELETE", url, null, null).ready(new SuccessCallback<Map<String, Object>>() {
            @Override
            public void onSucess(Map<String, Object> ignored) {
                out.complete(Boolean.TRUE);
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarPage<CalendarEvent>> queryEvents(CalendarQuery query) {
        final AsyncResource<CalendarPage<CalendarEvent>> out = new AsyncResource<CalendarPage<CalendarEvent>>();
        if (query == null || query.getCalendarId() == null) {
            return fail(out, CalendarError.INVALID_ARGUMENT, "calendarId required");
        }
        StringBuilder url = new StringBuilder(CALENDAR_API).append("/calendars/").append(e(query.getCalendarId())).append("/events?maxResults=").append(query.getPageSize());
        param(url, "pageToken", query.getPageToken());
        if (query.getSyncToken() != null) {
            param(url, "syncToken", query.getSyncToken());
        } else {
            param(url, "q", query.getText());
            if (query.getStartTime() != null) {
                param(url, "timeMin", iso(query.getStartTime()));
            }
            if (query.getEndTime() != null) {
                param(url, "timeMax", iso(query.getEndTime()));
            }
        }
        param(url, "singleEvents", String.valueOf(query.isExpandRecurrences()));
        param(url, "showDeleted", String.valueOf(query.getSyncToken() != null || query.isIncludeDeleted()));
        final String calendarId = query.getCalendarId();
        json("GET", url.toString(), null, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> root) {
                List<CalendarEvent> result = new ArrayList<CalendarEvent>();
                try {
                    for (Map<String, Object> item : maps(root.get("items"))) {
                        result.add(event(item, calendarId));
                    }
                } catch (CalendarException ex) {
                    out.error(ex);
                    return;
                }
                out.complete(new CalendarPage<CalendarEvent>(result, string(root, "nextPageToken"), string(root, "nextSyncToken")));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarEvent> getEvent(final String calendarId, String eventId) {
        final AsyncResource<CalendarEvent> out = new AsyncResource<CalendarEvent>();
        json("GET", CALENDAR_API + "/calendars/" + e(calendarId) + "/events/" + e(eventId), null, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> value) {
                try {
                    out.complete(event(value, calendarId));
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
            return fail(out, CalendarError.INVALID_ARGUMENT, "event and calendarId required");
        }
        String base = CALENDAR_API + "/calendars/" + e(event.getCalendarId()) + "/events";
        String url = event.getId() == null ? base : base + "/" + e(event.getId());
        if (event.getConference() != null && event.getConference().isCreateRequested()) {
            url += "?conferenceDataVersion=1";
        }
        Map<String, String> headers = version(event.getVersion());
        final boolean create = event.getId() == null;
        final String idempotentId = create ? googleEventId(event.getProviderData().get("cn1.mutationId")) : null;
        Map<String, Object> body = eventMap(event);
        if (idempotentId != null) {
            body.put("id", idempotentId);
        }
        json(create ? "POST" : "PUT", url, body, headers).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> value) {
                try {
                    CalendarEvent saved = event(value, event.getCalendarId());
                    out.complete(saved);
                    fireChange(new CalendarChange(getId(), saved.getCalendarId(), saved.getId(), CalendarChange.EntityType.EVENT, create ? CalendarChange.ChangeType.CREATED : CalendarChange.ChangeType.UPDATED));
                } catch (CalendarException ex) {
                    out.error(ex);
                }
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable error) {
                if (idempotentId != null && error instanceof CalendarException
                        && ((CalendarException) error).getError() == CalendarError.CONFLICT) {
                    getEvent(event.getCalendarId(), idempotentId).ready(new SuccessCallback<CalendarEvent>() {
                        @Override
                        public void onSucess(CalendarEvent existing) {
                            out.complete(existing);
                        }
                    }).except(error(out));
                    return;
                }
                out.error(error);
            }
        });
        return out;
    }

    @Override
    public AsyncResource<Boolean> deleteEvent(final String calendarId, final String eventId, CalendarMutationScope scope, String version) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        json("DELETE", CALENDAR_API + "/calendars/" + e(calendarId) + "/events/" + e(eventId), null, version(version)).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> ignored) {
                out.complete(Boolean.TRUE);
                fireChange(new CalendarChange(getId(), calendarId, eventId, CalendarChange.EntityType.EVENT, CalendarChange.ChangeType.DELETED));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarEvent> respondToEvent(final String calendarId, final String eventId, CalendarAttendee.Response response, String comment) {
        return unsupported("Responding to Google invitations");
    }

    @Override
    public AsyncResource<List<FreeBusyInterval>> queryFreeBusy(List<String> calendarIds, Instant startTime, Instant endTime) {
        final AsyncResource<List<FreeBusyInterval>> out = new AsyncResource<List<FreeBusyInterval>>();
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("timeMin", iso(startTime));
        body.put("timeMax", iso(endTime));
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (String id : calendarIds) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("id", id);
            items.add(m);
        }
        body.put("items", items);
        json("POST", CALENDAR_API + "/freeBusy", body, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> root) {
                List<FreeBusyInterval> result = new ArrayList<FreeBusyInterval>();
                Map<String, Object> calendars = map(root.get("calendars"));
                for (Object object : calendars.values()) {
                    Map<String, Object> cal = map(object);
                    for (Map<String, Object> busy : maps(cal.get("busy"))) {
                        try {
                            result.add(new FreeBusyInterval(parseIso(string(busy, "start")), parseIso(string(busy, "end")), CalendarEvent.Availability.BUSY));
                        } catch (CalendarException ex) {
                            out.error(ex);
                            return;
                        }
                    }
                }
                out.complete(result);
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarPage<CalendarTask>> queryTasks(CalendarQuery query) {
        final AsyncResource<CalendarPage<CalendarTask>> out = new AsyncResource<CalendarPage<CalendarTask>>();
        if (query == null || query.getCalendarId() == null) {
            return fail(out, CalendarError.INVALID_ARGUMENT, "calendarId required");
        }
        StringBuilder url = new StringBuilder(TASKS_API).append("/lists/").append(e(query.getCalendarId())).append("/tasks?maxResults=").append(query.getPageSize());
        param(url, "pageToken", query.getPageToken());
        param(url, "showCompleted", String.valueOf(query.isIncludeDeleted()));
        final String listId = query.getCalendarId();
        json("GET", url.toString(), null, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> root) {
                List<CalendarTask> result = new ArrayList<CalendarTask>();
                try {
                    for (Map<String, Object> item : maps(root.get("items"))) {
                        result.add(task(item, listId));
                    }
                } catch (CalendarException ex) {
                    out.error(ex);
                    return;
                }
                out.complete(new CalendarPage<CalendarTask>(result, string(root, "nextPageToken"), null));
            }
        }).except(error(out));
        return out;
    }

    @Override
    public AsyncResource<CalendarTask> getTask(final String listId, String taskId) {
        final AsyncResource<CalendarTask> out = new AsyncResource<CalendarTask>();
        json("GET", TASKS_API + "/lists/" + e(listId) + "/tasks/" + e(taskId), null, null).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> m) {
                try {
                    out.complete(task(m, listId));
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
            return fail(out, CalendarError.INVALID_ARGUMENT, "task and calendarId required");
        }
        String base = TASKS_API + "/lists/" + e(task.getCalendarId()) + "/tasks";
        String url = task.getId() == null ? base : base + "/" + e(task.getId());
        json(task.getId() == null ? "POST" : "PUT", url, taskMap(task), version(task.getVersion())).ready(new SuccessCallback<Map<String, Object>>() {

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
    public AsyncResource<Boolean> deleteTask(final String listId, final String taskId, CalendarMutationScope scope, String version) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        json("DELETE", TASKS_API + "/lists/" + e(listId) + "/tasks/" + e(taskId), null, version(version)).ready(new SuccessCallback<Map<String, Object>>() {

            @Override
            public void onSucess(Map<String, Object> x) {
                out.complete(Boolean.TRUE);
                fireChange(new CalendarChange(getId(), listId, taskId, CalendarChange.EntityType.TASK, CalendarChange.ChangeType.DELETED));
            }
        }).except(error(out));
        return out;
    }

    private CalendarInfo calendar(Map<String, Object> m, CalendarInfo.ContentType type)
            throws CalendarException {
        String name = string(m, type == CalendarInfo.ContentType.TASKS ? "title" : "summary");
        String zone = string(m, "timeZone");
        try {
            return new CalendarInfo().setId(string(m, "id")).setSourceId(getId()).setName(name).setOwner(string(m, "summaryOverride")).setTimeZone(zone == null ? null : CalendarDateUtil.zoneId(zone)).setPrimary(bool(m, "primary")).setReadOnly("reader".equals(string(m, "accessRole"))).setContentType(type).setCapabilities(getCapabilities());
        } catch (IllegalArgumentException ex) {
            throw new CalendarException(CalendarError.MALFORMED_RESPONSE,
                    "Invalid provider time zone: " + zone, ex);
        }
    }

    private CalendarEvent event(Map<String, Object> m, String calendarId) throws CalendarException {
        CalendarEvent out = new CalendarEvent().setId(string(m, "id")).setCalendarId(calendarId).setSourceId(getId()).setVersion(string(m, "etag")).setTitle(string(m, "summary")).setDescription(string(m, "description")).setLocation(string(m, "location")).setUrl(string(m, "htmlLink")).setRecurringEventId(string(m, "recurringEventId"));
        out.setStart(googleDate(map(m.get("start")))).setEnd(googleDate(map(m.get("end"))));
        List<Object> recurrence = list(m.get("recurrence"));
        if (!recurrence.isEmpty()) {
            out.setRecurrence(ICalendarCodec.readRecurrenceRule(String.valueOf(recurrence.get(0))));
        }
        String status = string(m, "status");
        if ("cancelled".equals(status)) {
            out.setStatus(CalendarEvent.Status.CANCELED);
        } else if ("tentative".equals(status)) {
            out.setStatus(CalendarEvent.Status.TENTATIVE);
        }
        out.setAvailability("transparent".equals(string(m, "transparency")) ? CalendarEvent.Availability.FREE : CalendarEvent.Availability.BUSY);
        for (Map<String, Object> a : maps(m.get("attendees"))) {
            CalendarAttendee attendee = new CalendarAttendee().setName(string(a, "displayName")).setEmail(string(a, "email")).setOrganizer(bool(a, "organizer")).setSelf(bool(a, "self"));
            String r = string(a, "responseStatus");
            attendee.setResponse(attendeeResponse(r));
            out.addAttendee(attendee);
        }
        Map<String, Object> reminders = map(m.get("reminders"));
        for (Map<String, Object> a : maps(reminders.get("overrides"))) {
            Integer minutes = integer(a, "minutes");
            if (minutes == null) {
                continue;
            }
            CalendarAlarm alarm = new CalendarAlarm().setTimeBefore(Duration.ofMinutes(minutes.intValue()));
            String method = string(a, "method");
            if ("email".equals(method)) {
                alarm.setMethod(CalendarAlarm.Method.EMAIL);
            } else if ("sms".equals(method)) {
                alarm.setMethod(CalendarAlarm.Method.AUDIO);
            }
            out.addAlarm(alarm);
        }
        for (Map<String, Object> a : maps(m.get("attachments"))) {
            out.addAttachment(new CalendarAttachment().setUri(string(a, "fileUrl")).setName(string(a, "title")).setMimeType(string(a, "mimeType")));
        }
        Map<String, Object> conference = map(m.get("conferenceData"));
        for (Map<String, Object> entry : maps(conference.get("entryPoints"))) {
            if ("video".equals(string(entry, "entryPointType"))) {
                out.setConference(new CalendarConference().setJoinUrl(string(entry, "uri")).setProvider(string(map(conference.get("conferenceSolution")), "name")));
            }
        }
        return out;
    }

    private Map<String, Object> eventMap(CalendarEvent e) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("summary", e.getTitle());
        if (e.getRecurrence() != null) {
            List<String> r = new ArrayList<String>();
            r.add("RRULE:" + ICalendarCodec.writeRecurrenceRule(e.getRecurrence()));
            m.put("recurrence", r);
        }
        m.put("description", e.getDescription());
        m.put("location", e.getLocation());
        m.put("start", googleDate(e.getStart()));
        m.put("end", googleDate(e.getEnd()));
        m.put("status", e.getStatus() == CalendarEvent.Status.CANCELED ? "cancelled"
                : e.getStatus() == CalendarEvent.Status.TENTATIVE ? "tentative" : "confirmed");
        m.put("transparency", e.getAvailability() == CalendarEvent.Availability.FREE ? "transparent" : "opaque");
        List<Map<String, Object>> attendees = new ArrayList<Map<String, Object>>();
        for (CalendarAttendee a : e.getAttendees()) {
            Map<String, Object> x = new HashMap<String, Object>();
            x.put("email", a.getEmail());
            x.put("displayName", a.getName());
            x.put("optional", Boolean.valueOf(a.getRole() == CalendarAttendee.Role.OPTIONAL));
            x.put("responseStatus", googleAttendeeResponse(a.getResponse()));
            attendees.add(x);
        }
        m.put("attendees", attendees);
        if (!e.getAlarms().isEmpty()) {
            Map<String, Object> r = new HashMap<String, Object>();
            r.put("useDefault", Boolean.FALSE);
            List<Map<String, Object>> overrides = new ArrayList<Map<String, Object>>();
            for (CalendarAlarm a : e.getAlarms()) {
                if (a.getTimeBefore() != null && a.getTimeBefore().toMillis() % 60000L == 0L) {
                    Map<String, Object> x = new HashMap<String, Object>();
                    x.put("minutes", Long.valueOf(a.getTimeBefore().toMillis() / 60000L));
                    x.put("method", a.getMethod() == CalendarAlarm.Method.EMAIL ? "email" : a.getMethod() == CalendarAlarm.Method.AUDIO ? "sms" : "popup");
                    overrides.add(x);
                }
            }
            r.put("overrides", overrides);
            m.put("reminders", r);
        }
        if (e.getConference() != null && e.getConference().isCreateRequested()) {
            Map<String, Object> c = new HashMap<String, Object>();
            Map<String, Object> create = new HashMap<String, Object>();
            create.put("requestId", String.valueOf(System.currentTimeMillis()));
            c.put("createRequest", create);
            m.put("conferenceData", c);
        }
        return m;
    }

    private CalendarTask task(Map<String, Object> m, String listId) throws CalendarException {
        CalendarTask out = new CalendarTask().setId(string(m, "id")).setCalendarId(listId).setSourceId(getId()).setVersion(string(m, "etag")).setTitle(string(m, "title")).setDescription(string(m, "notes")).setCompleted("completed".equals(string(m, "status")));
        String due = string(m, "due");
        String completed = string(m, "completed");
        if (due != null) {
            out.setDue(CalendarDateTime.allDay(
                    java.time.ZonedDateTime.ofInstant(parseIso(due), ZoneId.of("UTC")).toLocalDateTime().toLocalDate()));
        }
        if (completed != null) {
            out.setCompletionTime(parseIso(completed));
        }
        return out;
    }

    private Map<String, Object> taskMap(CalendarTask t) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("title", t.getTitle());
        m.put("notes", t.getDescription());
        m.put("status", t.isCompleted() ? "completed" : "needsAction");
        if (t.getDue() != null) {
            Instant due = t.getDue().isAllDay() ? CalendarDateUtil.allDayInstant(t.getDue().getDate())
                    : t.getDue().getDateTime().toInstant();
            m.put("due", iso(due));
        }
        if (t.isCompleted() && t.getCompletionTime() != null) {
            m.put("completed", iso(t.getCompletionTime()));
        }
        return m;
    }

    private static CalendarDateTime googleDate(Map<String, Object> m) throws CalendarException {
        String date = string(m, "date");
        String dateTime = string(m, "dateTime");
        String zone = string(m, "timeZone");
        try {
            if (date != null) {
                return CalendarDateTime.allDay(CalendarDateUtil.parseDate(date));
            }
            if (dateTime != null) {
                return CalendarDateTime.instant(parseIso(dateTime),
                        CalendarDateUtil.zoneId(zone == null ? "UTC" : zone));
            }
        } catch (IllegalArgumentException ex) {
            throw new CalendarException(CalendarError.MALFORMED_RESPONSE,
                    "Invalid provider date or time zone", ex);
        }
        return null;
    }

    private static Map<String, Object> googleDate(CalendarDateTime value) {
        Map<String, Object> m = new HashMap<String, Object>();
        if (value == null) {
            return m;
        }
        if (value.isAllDay()) {
            m.put("date", value.getDate().toString());
        } else {
            m.put("dateTime", iso(value.getDateTime().toInstant()));
            m.put("timeZone", value.getDateTime().getZone().getId());
        }
        return m;
    }

    private static String iso(Instant time) {
        return CalendarDateUtil.formatIso(time, ZoneId.of("UTC"), true) + "Z";
    }

    private static Instant parseIso(String value) throws CalendarException {
        try {
            return CalendarDateUtil.parseDateTime(value, ZoneId.of("UTC"));
        } catch (IllegalArgumentException ex) {
            throw new CalendarException(CalendarError.MALFORMED_RESPONSE, "Invalid provider date: " + value, ex);
        }
    }

    private static void param(StringBuilder b, String name, String value) {
        if (value != null) {
            b.append('&').append(name).append('=').append(Util.encodeUrl(value));
        }
    }

    private static String e(String v) {
        return Util.encodeUrl(v == null ? "" : v);
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

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object v) {
        return v instanceof List ? (List<Object>) v : new ArrayList<Object>();
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
        return b.toString();
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

    private static String googleAttendeeResponse(CalendarAttendee.Response response) {
        if (response == CalendarAttendee.Response.ACCEPTED) {
            return "accepted";
        }
        if (response == CalendarAttendee.Response.DECLINED) {
            return "declined";
        }
        if (response == CalendarAttendee.Response.TENTATIVE) {
            return "tentative";
        }
        return "needsAction";
    }

    private static String googleEventId(String mutationId) {
        if (mutationId == null) {
            return null;
        }
        long hash = 1469598103934665603L;
        for (int i = 0; i < mutationId.length(); i++) {
            hash ^= mutationId.charAt(i);
            hash *= 1099511628211L;
        }
        return "cn1" + longToHexString(hash);
    }

    private static String longToHexString(long value) {
        int high = (int) (value >>> 32);
        String lowHex = Integer.toHexString((int) value);
        if (high == 0) {
            return lowHex;
        }
        StringBuilder hex = new StringBuilder(Integer.toHexString(high));
        for (int i = lowHex.length(); i < 8; i++) {
            hex.append('0');
        }
        return hex.append(lowHex).toString();
    }

    private static <T> SuccessCallback<Throwable> error(final AsyncResource<T> out) {
        return new SuccessCallback<Throwable>() {

            @Override
            public void onSucess(Throwable error) {
                out.error(error);
            }
        };
    }

    private static <T> AsyncResource<T> fail(AsyncResource<T> out, CalendarError type, String message) {
        out.error(new CalendarException(type, message));
        return out;
    }
}
