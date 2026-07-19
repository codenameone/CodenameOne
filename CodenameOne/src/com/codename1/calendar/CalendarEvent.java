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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A portable calendar event. Mutable setters make it convenient to create an
/// event, while sources return detached instances so callers may safely edit.
public class CalendarEvent {
    public enum Status { CONFIRMED, TENTATIVE, CANCELED }
    public enum Availability { BUSY, FREE, TENTATIVE, OUT_OF_OFFICE, WORKING_ELSEWHERE }
    public enum Privacy { DEFAULT, PUBLIC, PRIVATE, CONFIDENTIAL }
    private String id, calendarId, sourceId, version, title, description, location, url, recurringEventId;
    private CalendarDateTime start, end;
    private CalendarRecurrenceRule recurrence;
    private Status status = Status.CONFIRMED;
    private Availability availability = Availability.BUSY;
    private Privacy privacy = Privacy.DEFAULT;
    private CalendarConference conference;
    private final List<CalendarAttendee> attendees = new ArrayList<CalendarAttendee>();
    private final List<CalendarAlarm> alarms = new ArrayList<CalendarAlarm>();
    private final List<CalendarAttachment> attachments = new ArrayList<CalendarAttachment>();
    private final Map<String,String> providerData = new HashMap<String,String>();
    public String getId() { return id; }
    public CalendarEvent setId(String v) { id = v; return this; }
    public String getCalendarId() { return calendarId; }
    public CalendarEvent setCalendarId(String v) { calendarId = v; return this; }
    public String getSourceId() { return sourceId; }
    public CalendarEvent setSourceId(String v) { sourceId = v; return this; }
    public String getVersion() { return version; }
    public CalendarEvent setVersion(String v) { version = v; return this; }
    public String getTitle() { return title; }
    public CalendarEvent setTitle(String v) { title = v; return this; }
    public String getDescription() { return description; }
    public CalendarEvent setDescription(String v) { description = v; return this; }
    public String getLocation() { return location; }
    public CalendarEvent setLocation(String v) { location = v; return this; }
    public String getUrl() { return url; }
    public CalendarEvent setUrl(String v) { url = v; return this; }
    public String getRecurringEventId() { return recurringEventId; }
    public CalendarEvent setRecurringEventId(String v) { recurringEventId = v; return this; }
    public CalendarDateTime getStart() { return start; }
    public CalendarEvent setStart(CalendarDateTime v) { start = v; return this; }
    public CalendarDateTime getEnd() { return end; }
    public CalendarEvent setEnd(CalendarDateTime v) { end = v; return this; }
    public CalendarRecurrenceRule getRecurrence() { return recurrence; }
    public CalendarEvent setRecurrence(CalendarRecurrenceRule v) { recurrence = v; return this; }
    public Status getStatus() { return status; }
    public CalendarEvent setStatus(Status v) { status = v == null ? Status.CONFIRMED : v; return this; }
    public Availability getAvailability() { return availability; }
    public CalendarEvent setAvailability(Availability v) { availability = v == null ? Availability.BUSY : v; return this; }
    public Privacy getPrivacy() { return privacy; }
    public CalendarEvent setPrivacy(Privacy v) { privacy = v == null ? Privacy.DEFAULT : v; return this; }
    public CalendarConference getConference() { return conference; }
    public CalendarEvent setConference(CalendarConference v) { conference = v; return this; }
    public CalendarEvent addAttendee(CalendarAttendee v) { if (v != null) attendees.add(v); return this; }
    public List<CalendarAttendee> getAttendees() { return Collections.unmodifiableList(attendees); }
    public CalendarEvent addAlarm(CalendarAlarm v) { if (v != null) alarms.add(v); return this; }
    public List<CalendarAlarm> getAlarms() { return Collections.unmodifiableList(alarms); }
    public CalendarEvent addAttachment(CalendarAttachment v) { if (v != null) attachments.add(v); return this; }
    public List<CalendarAttachment> getAttachments() { return Collections.unmodifiableList(attachments); }
    public CalendarEvent putProviderData(String k, String v) { if (k != null) providerData.put(k, v); return this; }
    public Map<String,String> getProviderData() { return Collections.unmodifiableMap(providerData); }
}
