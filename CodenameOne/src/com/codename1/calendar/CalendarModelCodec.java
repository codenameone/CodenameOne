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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Lossless map encoding used by the offline cache and provider fixtures.
public final class CalendarModelCodec {

    private CalendarModelCodec() {
    }

    public static Map<String, Object> encodeEvent(CalendarEvent e) {
        Map<String, Object> m = common(e.getId(), e.getCalendarId(), e.getSourceId(), e.getVersion(), e.getTitle(), e.getDescription(), e.getLocation());
        m.put("codecVersion", Integer.valueOf(1));
        put(m, "url", e.getUrl());
        put(m, "recurringEventId", e.getRecurringEventId());
        put(m, "start", encodeDateTime(e.getStart()));
        put(m, "end", encodeDateTime(e.getEnd()));
        put(m, "status", e.getStatus().name());
        put(m, "availability", e.getAvailability().name());
        put(m, "privacy", e.getPrivacy().name());
        put(m, "recurrence", encodeRecurrence(e.getRecurrence()));
        List<Object> attendees = new ArrayList<Object>();
        for (CalendarAttendee a : e.getAttendees()) {
            attendees.add(encodeAttendee(a));
        }
        m.put("attendees", attendees);
        List<Object> alarms = new ArrayList<Object>();
        for (CalendarAlarm a : e.getAlarms()) {
            alarms.add(encodeAlarm(a));
        }
        m.put("alarms", alarms);
        List<Object> attachments = new ArrayList<Object>();
        for (CalendarAttachment a : e.getAttachments()) {
            attachments.add(encodeAttachment(a));
        }
        m.put("attachments", attachments);
        put(m, "conference", encodeConference(e.getConference()));
        m.put("providerData", new HashMap<String, String>(e.getProviderData()));
        return m;
    }

    public static CalendarEvent decodeEvent(Map m) {
        CalendarEvent e = new CalendarEvent().setId(s(m, "id")).setCalendarId(s(m, "calendarId")).setSourceId(s(m, "sourceId")).setVersion(s(m, "version")).setTitle(s(m, "title")).setDescription(s(m, "description")).setLocation(s(m, "location")).setUrl(s(m, "url")).setRecurringEventId(s(m, "recurringEventId")).setStart(decodeDateTime(map(m, "start"))).setEnd(decodeDateTime(map(m, "end")));
        e.setStatus(eventStatus(s(m, "status")));
        e.setAvailability(eventAvailability(s(m, "availability")));
        e.setPrivacy(eventPrivacy(s(m, "privacy")));
        e.setRecurrence(decodeRecurrence(map(m, "recurrence")));
        for (Object v : list(m, "attendees")) {
            if (v instanceof Map) {
                e.addAttendee(decodeAttendee((Map) v));
            }
        }
        for (Object v : list(m, "alarms")) {
            if (v instanceof Map) {
                e.addAlarm(decodeAlarm((Map) v));
            }
        }
        for (Object v : list(m, "attachments")) {
            if (v instanceof Map) {
                e.addAttachment(decodeAttachment((Map) v));
            }
        }
        e.setConference(decodeConference(map(m, "conference")));
        copyProvider(m, e, null);
        return e;
    }

    public static Map<String, Object> encodeTask(CalendarTask t) {
        Map<String, Object> m = common(t.getId(), t.getCalendarId(), t.getSourceId(), t.getVersion(), t.getTitle(), t.getDescription(), t.getLocation());
        m.put("codecVersion", Integer.valueOf(1));
        put(m, "start", encodeDateTime(t.getStart()));
        put(m, "due", encodeDateTime(t.getDue()));
        put(m, "recurrence", encodeRecurrence(t.getRecurrence()));
        m.put("completed", Boolean.valueOf(t.isCompleted()));
        put(m, "completionTime", epochMillis(t.getCompletionTime()));
        m.put("priority", Integer.valueOf(t.getPriority()));
        List<Object> alarms = new ArrayList<Object>();
        for (CalendarAlarm a : t.getAlarms()) {
            alarms.add(encodeAlarm(a));
        }
        m.put("alarms", alarms);
        List<Object> attachments = new ArrayList<Object>();
        for (CalendarAttachment a : t.getAttachments()) {
            attachments.add(encodeAttachment(a));
        }
        m.put("attachments", attachments);
        m.put("providerData", new HashMap<String, String>(t.getProviderData()));
        return m;
    }

    public static CalendarTask decodeTask(Map m) {
        CalendarTask t = new CalendarTask().setId(s(m, "id")).setCalendarId(s(m, "calendarId")).setSourceId(s(m, "sourceId")).setVersion(s(m, "version")).setTitle(s(m, "title")).setDescription(s(m, "description")).setLocation(s(m, "location")).setStart(decodeDateTime(map(m, "start"))).setDue(decodeDateTime(map(m, "due"))).setRecurrence(decodeRecurrence(map(m, "recurrence"))).setCompleted(bool(m, "completed")).setCompletionTime(instantObj(m, "completionTime")).setPriority(integer(m, "priority", 0));
        for (Object v : list(m, "alarms")) {
            if (v instanceof Map) {
                t.addAlarm(decodeAlarm((Map) v));
            }
        }
        for (Object v : list(m, "attachments")) {
            if (v instanceof Map) {
                t.addAttachment(decodeAttachment((Map) v));
            }
        }
        copyProvider(m, null, t);
        return t;
    }

    public static Map<String, Object> encodeDateTime(CalendarDateTime d) {
        if (d == null) {
            return null;
        }
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("allDay", Boolean.valueOf(d.isAllDay()));
        if (d.isAllDay()) {
            m.put("year", d.getDate().getYear());
            m.put("month", d.getDate().getMonthValue());
            m.put("day", d.getDate().getDayOfMonth());
        } else {
            m.put("timestamp", Long.valueOf(d.getDateTime().toInstant().toEpochMilli()));
            m.put("timeZoneId", d.getDateTime().getZone().getId());
        }
        return m;
    }

    public static CalendarDateTime decodeDateTime(Map m) {
        if (m == null) {
            return null;
        }
        if (bool(m, "allDay")) {
            return CalendarDateTime.allDay(LocalDate.of(integer(m, "year", 1970), integer(m, "month", 1), integer(m, "day", 1)));
        }
        String zone = s(m, "timeZoneId") == null ? "UTC" : s(m, "timeZoneId");
        return CalendarDateTime.timed(ZonedDateTime.ofInstant(Instant.ofEpochMilli(lng(m, "timestamp", 0L)), ZoneId.of(zone)));
    }

    private static Map<String, Object> common(String id, String cal, String src, String ver, String title, String desc, String loc) {
        Map<String, Object> m = new HashMap<String, Object>();
        put(m, "id", id);
        put(m, "calendarId", cal);
        put(m, "sourceId", src);
        put(m, "version", ver);
        put(m, "title", title);
        put(m, "description", desc);
        put(m, "location", loc);
        return m;
    }

    private static Map<String, Object> encodeRecurrence(CalendarRecurrenceRule r) {
        if (r == null) {
            return null;
        }
        Map<String, Object> m = new HashMap<String, Object>();
        put(m, "frequency", r.getFrequency() == null ? null : r.getFrequency().name());
        m.put("interval", r.getInterval());
        put(m, "count", r.getCount());
        put(m, "until", encodeDateTime(r.getUntil()));
        m.put("daysOfWeek", new ArrayList<Integer>(r.getDaysOfWeek()));
        m.put("daysOfMonth", new ArrayList<Integer>(r.getDaysOfMonth()));
        m.put("months", new ArrayList<Integer>(r.getMonths()));
        return m;
    }

    private static CalendarRecurrenceRule decodeRecurrence(Map m) {
        if (m == null) {
            return null;
        }
        CalendarRecurrenceRule r = new CalendarRecurrenceRule().setInterval(integer(m, "interval", 1)).setCount(intObj(m, "count")).setUntil(decodeDateTime(map(m, "until")));
        r.setFrequency(frequency(s(m, "frequency")));
        for (Object v : list(m, "daysOfWeek")) {
            r.addDayOfWeek(((Number) v).intValue());
        }
        for (Object v : list(m, "daysOfMonth")) {
            r.addDayOfMonth(((Number) v).intValue());
        }
        for (Object v : list(m, "months")) {
            r.addMonth(((Number) v).intValue());
        }
        return r;
    }

    private static Map<String, Object> encodeAttendee(CalendarAttendee a) {
        Map<String, Object> m = new HashMap<String, Object>();
        put(m, "name", a.getName());
        put(m, "email", a.getEmail());
        put(m, "uri", a.getUri());
        m.put("role", a.getRole().name());
        m.put("response", a.getResponse().name());
        m.put("organizer", a.isOrganizer());
        m.put("self", a.isSelf());
        return m;
    }

    private static CalendarAttendee decodeAttendee(Map m) {
        return new CalendarAttendee().setName(s(m, "name")).setEmail(s(m, "email")).setUri(s(m, "uri")).setOrganizer(bool(m, "organizer")).setSelf(bool(m, "self")).setRole(attendeeRole(s(m, "role"))).setResponse(attendeeResponse(s(m, "response")));
    }

    private static Map<String, Object> encodeAlarm(CalendarAlarm a) {
        Map<String, Object> m = new HashMap<String, Object>();
        put(m, "timeBeforeMillis", a.getTimeBefore() == null ? null : Long.valueOf(a.getTimeBefore().toMillis()));
        put(m, "absoluteTime", epochMillis(a.getAbsoluteTime()));
        m.put("method", a.getMethod().name());
        return m;
    }

    private static CalendarAlarm decodeAlarm(Map m) {
        CalendarAlarm a = new CalendarAlarm();
        if (m.get("timeBeforeMillis") != null) {
            a.setTimeBefore(Duration.ofMillis(lng(m, "timeBeforeMillis", 0L)));
        } else {
            a.setAbsoluteTime(instantObj(m, "absoluteTime"));
        }
        a.setMethod(alarmMethod(s(m, "method")));
        return a;
    }

    private static Map<String, Object> encodeAttachment(CalendarAttachment a) {
        Map<String, Object> m = new HashMap<String, Object>();
        put(m, "id", a.getId());
        put(m, "name", a.getName());
        put(m, "mimeType", a.getMimeType());
        put(m, "uri", a.getUri());
        m.put("size", Long.valueOf(a.getSize()));
        put(m, "content", a.getContent());
        return m;
    }

    private static CalendarAttachment decodeAttachment(Map m) {
        return new CalendarAttachment().setId(s(m, "id")).setName(s(m, "name")).setMimeType(s(m, "mimeType")).setUri(s(m, "uri")).setSize(lng(m, "size", -1)).setContent((byte[]) m.get("content"));
    }

    private static Map<String, Object> encodeConference(CalendarConference c) {
        if (c == null) {
            return null;
        }
        Map<String, Object> m = new HashMap<String, Object>();
        put(m, "provider", c.getProvider());
        put(m, "id", c.getId());
        put(m, "joinUrl", c.getJoinUrl());
        m.put("createRequested", c.isCreateRequested());
        m.put("phoneNumbers", new ArrayList<String>(c.getPhoneNumbers()));
        return m;
    }

    private static CalendarConference decodeConference(Map m) {
        if (m == null) {
            return null;
        }
        CalendarConference c = new CalendarConference().setProvider(s(m, "provider")).setId(s(m, "id")).setJoinUrl(s(m, "joinUrl")).setCreateRequested(bool(m, "createRequested"));
        for (Object v : list(m, "phoneNumbers")) {
            c.addPhoneNumber(String.valueOf(v));
        }
        return c;
    }

    private static void copyProvider(Map m, CalendarEvent e, CalendarTask t) {
        Map p = map(m, "providerData");
        if (p != null) {
            for (Object value : p.entrySet()) {
                Map.Entry entry = (Map.Entry) value;
                String key = String.valueOf(entry.getKey());
                String text = entry.getValue() == null ? null : String.valueOf(entry.getValue());
                if (e != null) {
                    e.putProviderData(key, text);
                } else {
                    t.putProviderData(key, text);
                }
            }
        }
    }

    private static CalendarEvent.Status eventStatus(String value) {
        for (CalendarEvent.Status candidate : CalendarEvent.Status.values()) {
            if (candidate.name().equals(value)) {
                return candidate;
            }
        }
        return CalendarEvent.Status.CONFIRMED;
    }

    private static CalendarEvent.Availability eventAvailability(String value) {
        for (CalendarEvent.Availability candidate : CalendarEvent.Availability.values()) {
            if (candidate.name().equals(value)) {
                return candidate;
            }
        }
        return CalendarEvent.Availability.BUSY;
    }

    private static CalendarEvent.Privacy eventPrivacy(String value) {
        for (CalendarEvent.Privacy candidate : CalendarEvent.Privacy.values()) {
            if (candidate.name().equals(value)) {
                return candidate;
            }
        }
        return CalendarEvent.Privacy.DEFAULT;
    }

    private static CalendarRecurrenceRule.Frequency frequency(String value) {
        for (CalendarRecurrenceRule.Frequency candidate : CalendarRecurrenceRule.Frequency.values()) {
            if (candidate.name().equals(value)) {
                return candidate;
            }
        }
        return null;
    }

    private static CalendarAttendee.Role attendeeRole(String value) {
        for (CalendarAttendee.Role candidate : CalendarAttendee.Role.values()) {
            if (candidate.name().equals(value)) {
                return candidate;
            }
        }
        return CalendarAttendee.Role.REQUIRED;
    }

    private static CalendarAttendee.Response attendeeResponse(String value) {
        for (CalendarAttendee.Response candidate : CalendarAttendee.Response.values()) {
            if (candidate.name().equals(value)) {
                return candidate;
            }
        }
        return CalendarAttendee.Response.NONE;
    }

    private static CalendarAlarm.Method alarmMethod(String value) {
        for (CalendarAlarm.Method candidate : CalendarAlarm.Method.values()) {
            if (candidate.name().equals(value)) {
                return candidate;
            }
        }
        return CalendarAlarm.Method.DEFAULT;
    }

    private static void put(Map<String, Object> m, String k, Object v) {
        if (v != null) {
            m.put(k, v);
        }
    }

    private static String s(Map m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }

    private static Map map(Map m, String k) {
        Object v = m.get(k);
        return v instanceof Map ? (Map) v : null;
    }

    private static List list(Map m, String k) {
        Object v = m.get(k);
        return v instanceof List ? (List) v : new ArrayList();
    }

    private static boolean bool(Map m, String k) {
        Object v = m.get(k);
        return Boolean.TRUE.equals(v) || "true".equals(String.valueOf(v));
    }

    private static int integer(Map m, String k, int d) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).intValue() : d;
    }

    private static long lng(Map m, String k, long d) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).longValue() : d;
    }

    private static Integer intObj(Map m, String k) {
        Object v = m.get(k);
        return v instanceof Number ? Integer.valueOf(((Number) v).intValue()) : null;
    }

    private static Long lngObj(Map m, String k) {
        Object v = m.get(k);
        return v instanceof Number ? Long.valueOf(((Number) v).longValue()) : null;
    }

    private static Long epochMillis(Instant value) {
        return value == null ? null : Long.valueOf(value.toEpochMilli());
    }

    private static Instant instantObj(Map m, String k) {
        Long value = lngObj(m, k);
        return value == null ? null : Instant.ofEpochMilli(value.longValue());
    }
}
