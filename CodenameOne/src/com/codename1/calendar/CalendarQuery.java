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

/// Query options shared by event and task listing operations.
public class CalendarQuery {

    private String calendarId;

    private String text;

    private String pageToken;

    private String syncToken;

    private Instant startTime;

    private Instant endTime;

    private int pageSize = 100;

    private boolean expandRecurrences = true;

    private boolean includeDeleted;

    public String getCalendarId() {
        return calendarId;
    }

    public CalendarQuery setCalendarId(String v) {
        calendarId = v;
        return this;
    }

    public String getText() {
        return text;
    }

    public CalendarQuery setText(String v) {
        text = v;
        return this;
    }

    public String getPageToken() {
        return pageToken;
    }

    public CalendarQuery setPageToken(String v) {
        pageToken = v;
        return this;
    }

    public String getSyncToken() {
        return syncToken;
    }

    public CalendarQuery setSyncToken(String v) {
        syncToken = v;
        return this;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public CalendarQuery setStartTime(Instant v) {
        startTime = v;
        return this;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public CalendarQuery setEndTime(Instant v) {
        endTime = v;
        return this;
    }

    public int getPageSize() {
        return pageSize;
    }

    public CalendarQuery setPageSize(int v) {
        if (v < 1) {
            throw new IllegalArgumentException("pageSize");
        }
        pageSize = v;
        return this;
    }

    public boolean isExpandRecurrences() {
        return expandRecurrences;
    }

    public CalendarQuery setExpandRecurrences(boolean v) {
        expandRecurrences = v;
        return this;
    }

    public boolean isIncludeDeleted() {
        return includeDeleted;
    }

    public CalendarQuery setIncludeDeleted(boolean v) {
        includeDeleted = v;
        return this;
    }
}
