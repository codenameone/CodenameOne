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
package com.codenameone.developerguide.calendar;

import com.codename1.calendar.CalDavAuthentication;
import com.codename1.calendar.CalDavCalendarSource;
import com.codename1.calendar.CalendarAccess;
import com.codename1.calendar.CalendarAlarm;
import com.codename1.calendar.CalendarAttendee;
import com.codename1.calendar.CalendarAuthorizationStatus;
import com.codename1.calendar.CalendarCapabilities;
import com.codename1.calendar.CalendarCapability;
import com.codename1.calendar.CalendarConflict;
import com.codename1.calendar.CalendarDateTime;
import com.codename1.calendar.CalendarEvent;
import com.codename1.calendar.CalendarException;
import com.codename1.calendar.CalendarManager;
import com.codename1.calendar.CalendarMutationScope;
import com.codename1.calendar.CalendarQuery;
import com.codename1.calendar.CalendarRecurrenceRule;
import com.codename1.calendar.CalendarSource;
import com.codename1.calendar.CalendarSyncEngine;
import com.codename1.calendar.GoogleCalendarSource;
import com.codename1.calendar.ICalendarCodec;
import com.codename1.calendar.LocalCalendarSource;
import com.codename1.calendar.OidcCalendarTokenProvider;
import com.codename1.calendar.StorageCalendarCache;
import com.codename1.io.Log;
import com.codename1.io.oidc.OidcClient;

public class CalendarIntegrationSnippets {
    public void discoverCapabilities() {
        // tag::calendar-capabilities[]
        LocalCalendarSource local = LocalCalendarSource.getInstance();
        CalendarCapabilities capabilities = local.getCapabilities();

        if (capabilities.supports(CalendarCapability.READ_EVENTS)) {
            local.requestAuthorization(CalendarAccess.EVENTS_READ_ONLY).ready(status -> {
                if (status == CalendarAuthorizationStatus.FULL) {
                    local.queryEvents(new CalendarQuery()
                            .setCalendarId("primary")
                            .setStartTime(System.currentTimeMillis()))
                         .ready(page -> page.getItems().forEach(System.out::println));
                }
            });
        }
        // end::calendar-capabilities[]
    }

    public void createEvent(LocalCalendarSource local, String calendarId, long startMillis, long endMillis) {
        // tag::calendar-create-event[]
        CalendarEvent event = new CalendarEvent()
                .setCalendarId(calendarId)
                .setTitle("Architecture review")
                .setStart(CalendarDateTime.instant(startMillis, "Europe/Paris"))
                .setEnd(CalendarDateTime.instant(endMillis, "Europe/Paris"))
                .setRecurrence(new CalendarRecurrenceRule()
                        .setFrequency(CalendarRecurrenceRule.Frequency.WEEKLY)
                        .addDayOfWeek(2))
                .addAttendee(new CalendarAttendee()
                        .setName("Ari")
                        .setEmail("ari@example.com"))
                .addAlarm(new CalendarAlarm().setMinutesBefore(15));

        local.saveEvent(event, CalendarMutationScope.ALL).ready(saved -> {
            Log.p("Created " + saved.getId());
        });
        // end::calendar-create-event[]
    }

    public void configureGoogle(String clientId, String redirectUri) {
        // tag::calendar-google-oidc[]
        OidcClient.discover("https://accounts.google.com").ready(oidc -> {
            oidc.setClientId(clientId)
                .setRedirectUri(redirectUri)
                .setScopes(GoogleCalendarSource.SCOPE_CALENDAR,
                           GoogleCalendarSource.SCOPE_TASKS)
                .authorize().ready(tokens -> {
                    CalendarSource google = new GoogleCalendarSource(
                            new OidcCalendarTokenProvider(oidc, tokens));
                    CalendarManager.getInstance().registerSource(google);
                });
        });
        // end::calendar-google-oidc[]
    }

    public CalendarSource configureCalDav(String calendarHomeUrl, String username, String password) {
        // tag::calendar-caldav[]
        CalendarSource caldav = new CalDavCalendarSource(
                "work", "Work CalDAV", calendarHomeUrl,
                CalDavAuthentication.digest(username, password));
        // end::calendar-caldav[]
        return caldav;
    }

    public CalendarEvent importExport(CalendarEvent event) throws CalendarException {
        // tag::calendar-import-export[]
        String ics = ICalendarCodec.writeEvent(event);
        CalendarEvent imported = ICalendarCodec.readEvent(ics);
        // end::calendar-import-export[]
        return imported;
    }

    public CalendarSyncEngine sync(CalendarSource google, CalendarEvent event) throws CalendarException {
        // tag::calendar-sync[]
        CalendarSyncEngine sync = new CalendarSyncEngine(
                google, new StorageCalendarCache("google-account-1"));
        sync.queueEventSave(event, CalendarMutationScope.ALL);
        sync.sync().ready(result -> {
            for (CalendarConflict conflict : result.getConflicts()) {
                // Present both versions, then call resolveConflict(...).
            }
        });
        // end::calendar-sync[]
        return sync;
    }
}
