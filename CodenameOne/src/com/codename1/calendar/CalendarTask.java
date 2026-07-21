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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A task/reminder in a task collection.
public class CalendarTask {

    private String id;

    private String calendarId;

    private String sourceId;

    private String version;

    private String title;

    private String description;

    private String location;

    private CalendarDateTime start;

    private CalendarDateTime due;

    private CalendarRecurrenceRule recurrence;

    private boolean completed;

    private Instant completionTime;

    private int priority;

    private final List<CalendarAlarm> alarms = new ArrayList<CalendarAlarm>();

    private final List<CalendarAttachment> attachments = new ArrayList<CalendarAttachment>();

    private final Map<String, String> providerData = new HashMap<String, String>();

    public String getId() {
        return id;
    }

    public CalendarTask setId(String v) {
        id = v;
        return this;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public CalendarTask setCalendarId(String v) {
        calendarId = v;
        return this;
    }

    public String getSourceId() {
        return sourceId;
    }

    public CalendarTask setSourceId(String v) {
        sourceId = v;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public CalendarTask setVersion(String v) {
        version = v;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public CalendarTask setTitle(String v) {
        title = v;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public CalendarTask setDescription(String v) {
        description = v;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public CalendarTask setLocation(String v) {
        location = v;
        return this;
    }

    public CalendarDateTime getStart() {
        return start;
    }

    public CalendarTask setStart(CalendarDateTime v) {
        start = v;
        return this;
    }

    public CalendarDateTime getDue() {
        return due;
    }

    public CalendarTask setDue(CalendarDateTime v) {
        due = v;
        return this;
    }

    public CalendarRecurrenceRule getRecurrence() {
        return recurrence;
    }

    public CalendarTask setRecurrence(CalendarRecurrenceRule v) {
        recurrence = v;
        return this;
    }

    public boolean isCompleted() {
        return completed;
    }

    public CalendarTask setCompleted(boolean v) {
        completed = v;
        return this;
    }

    public Instant getCompletionTime() {
        return completionTime;
    }

    public CalendarTask setCompletionTime(Instant v) {
        completionTime = v;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public CalendarTask setPriority(int v) {
        priority = v;
        return this;
    }

    public CalendarTask addAlarm(CalendarAlarm v) {
        if (v != null) {
            alarms.add(v);
        }
        return this;
    }

    public List<CalendarAlarm> getAlarms() {
        return Collections.unmodifiableList(alarms);
    }

    public CalendarTask addAttachment(CalendarAttachment v) {
        if (v != null) {
            attachments.add(v);
        }
        return this;
    }

    public List<CalendarAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public CalendarTask putProviderData(String k, String v) {
        if (k != null) {
            providerData.put(k, v);
        }
        return this;
    }

    public Map<String, String> getProviderData() {
        return Collections.unmodifiableMap(providerData);
    }
}
