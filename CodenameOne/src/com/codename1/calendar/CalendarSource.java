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

import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Base contract shared by local stores and online providers.
public abstract class CalendarSource {

    private final String id;

    private final String displayName;

    private final List<CalendarChangeListener> listeners = new ArrayList<CalendarChangeListener>();

    protected CalendarSource(String id, String displayName) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id required");
        }
        this.id = id;
        this.displayName = displayName == null ? id : displayName;
    }

    public final String getId() {
        return id;
    }

    public final String getDisplayName() {
        return displayName;
    }

    public boolean isAvailable() {
        return !getCapabilities().asSet().isEmpty();
    }

    public CalendarCapabilities getCapabilities() {
        return CalendarCapabilities.none();
    }

    public CalendarAuthorizationStatus getAuthorizationStatus(CalendarAccess access) {
        return CalendarAuthorizationStatus.DENIED;
    }

    public AsyncResource<CalendarAuthorizationStatus> requestAuthorization(CalendarAccess access) {
        return unavailable("Calendar authorization is unavailable");
    }

    public AsyncResource<CalendarPage<CalendarInfo>> listCalendars(CalendarInfo.ContentType type, String pageToken) {
        return unsupported("Listing calendars");
    }

    public AsyncResource<CalendarInfo> saveCalendar(CalendarInfo calendar) {
        return unsupported("Saving calendars");
    }

    public AsyncResource<Boolean> deleteCalendar(String calendarId) {
        return unsupported("Deleting calendars");
    }

    public AsyncResource<CalendarPage<CalendarEvent>> queryEvents(CalendarQuery query) {
        return unsupported("Querying events");
    }

    public AsyncResource<CalendarEvent> getEvent(String calendarId, String eventId) {
        return unsupported("Reading events");
    }

    public AsyncResource<CalendarEvent> saveEvent(CalendarEvent event, CalendarMutationScope scope) {
        return unsupported("Saving events");
    }

    public AsyncResource<Boolean> deleteEvent(String calendarId, String eventId, CalendarMutationScope scope, String version) {
        return unsupported("Deleting events");
    }

    public AsyncResource<CalendarEvent> respondToEvent(String calendarId, String eventId, CalendarAttendee.Response response, String comment) {
        return unsupported("Responding to events");
    }

    public AsyncResource<List<FreeBusyInterval>> queryFreeBusy(List<String> calendarIds, Instant startTime, Instant endTime) {
        return unsupported("Free/busy");
    }

    public AsyncResource<CalendarPage<CalendarTask>> queryTasks(CalendarQuery query) {
        return unsupported("Querying tasks");
    }

    public AsyncResource<CalendarTask> getTask(String calendarId, String taskId) {
        return unsupported("Reading tasks");
    }

    public AsyncResource<CalendarTask> saveTask(CalendarTask task, CalendarMutationScope scope) {
        return unsupported("Saving tasks");
    }

    public AsyncResource<Boolean> deleteTask(String calendarId, String taskId, CalendarMutationScope scope, String version) {
        return unsupported("Deleting tasks");
    }

    public final synchronized void addChangeListener(CalendarChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public final synchronized void removeChangeListener(CalendarChangeListener listener) {
        listeners.remove(listener);
    }

    protected final void fireChange(final CalendarChange change) {
        final List<CalendarChangeListener> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<CalendarChangeListener>(listeners);
        }
        Runnable notify = new Runnable() {

            @Override
            public void run() {
                for (CalendarChangeListener listener : snapshot) {
                    listener.calendarChanged(change);
                }
            }
        };
        if (Display.isInitialized() && !Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(notify);
        } else {
            notify.run();
        }
    }

    protected static <T> AsyncResource<T> unsupported(String operation) {
        AsyncResource<T> out = new AsyncResource<T>();
        out.error(new CalendarException(CalendarError.NOT_SUPPORTED, operation + " is not supported by this source"));
        return out;
    }

    protected static <T> AsyncResource<T> unavailable(String message) {
        AsyncResource<T> out = new AsyncResource<T>();
        out.error(new CalendarException(CalendarError.NOT_AVAILABLE, message));
        return out;
    }

    protected static <T> AsyncResource<T> completed(T value) {
        AsyncResource<T> out = new AsyncResource<T>();
        out.complete(value);
        return out;
    }
}
