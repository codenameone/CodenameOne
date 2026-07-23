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

import com.codename1.util.AsyncResource;
import com.codename1.ui.Display;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalendarIntegrationTest {
    @Test
    void calendarModelUsesJavaTimeValuesEndToEnd() {
        Instant completion = Instant.ofEpochMilli(1784458800123L);
        ZoneId zone = ZoneId.of("Asia/Jerusalem");
        ZonedDateTime timed = ZonedDateTime.ofInstant(completion, zone);
        Duration reminder = Duration.ofMinutes(30);
        CalendarTask source = new CalendarTask()
                .setStart(CalendarDateTime.timed(timed))
                .setDue(CalendarDateTime.allDay(LocalDate.of(2026, 7, 31)))
                .setCompletionTime(completion)
                .addAlarm(new CalendarAlarm().setTimeBefore(reminder));

        CalendarTask decoded = CalendarModelCodec.decodeTask(CalendarModelCodec.encodeTask(source));

        assertEquals(timed, decoded.getStart().getDateTime());
        assertEquals(LocalDate.of(2026, 7, 31), decoded.getDue().getDate());
        assertEquals(completion, decoded.getCompletionTime());
        assertEquals(reminder, decoded.getAlarms().get(0).getTimeBefore());
        assertEquals(zone, new CalendarInfo().setTimeZone(zone).getTimeZone());
        assertEquals(completion, new CalendarQuery().setStartTime(completion).getStartTime());
        assertEquals(completion, new FreeBusyInterval(completion, completion, null).getStartTime());
        assertThrows(IllegalArgumentException.class,
                () -> new CalendarAlarm().setTimeBefore(Duration.ofSeconds(-1)));
    }

    @Test
    void iCalendarRoundTripPreservesPortableEventData() throws Exception {
        CalendarRecurrenceRule recurrence = new CalendarRecurrenceRule().setFrequency(CalendarRecurrenceRule.Frequency.WEEKLY)
                .setInterval(2).setCount(Integer.valueOf(8)).addDayOfWeek(1).addDayOfWeek(3);
        CalendarEvent event = new CalendarEvent().setId("event-1").setTitle("Planning, review")
                .setDescription("Line one\nLine two").setLocation("Room; 4")
                .setStart(CalendarDateTime.instant(Instant.ofEpochMilli(1784455200000L),ZoneId.of("Asia/Jerusalem")))
                .setEnd(CalendarDateTime.instant(Instant.ofEpochMilli(1784458800000L),ZoneId.of("Asia/Jerusalem")))
                .setRecurrence(recurrence).setPrivacy(CalendarEvent.Privacy.PRIVATE)
                .addAttendee(new CalendarAttendee().setName("Ada Lovelace").setEmail("ada@example.com").setRole(CalendarAttendee.Role.OPTIONAL).setResponse(CalendarAttendee.Response.ACCEPTED))
                .addAlarm(new CalendarAlarm().setTimeBefore(Duration.ofMinutes(15)))
                .addAttachment(new CalendarAttachment().setUri("https://example.com/spec.pdf").setMimeType("application/pdf"))
                .setConference(new CalendarConference().setJoinUrl("https://meet.example.com/abc"))
                .putProviderData("X-CN1-TEST","preserved");

        String encoded = ICalendarCodec.writeEvent(event);
        CalendarEvent decoded = ICalendarCodec.readEvent(encoded);

        assertEquals("event-1",decoded.getId());
        assertEquals("Planning, review",decoded.getTitle());
        assertEquals("Line one\nLine two",decoded.getDescription());
        assertEquals("Asia/Jerusalem",decoded.getStart().getDateTime().getZone().getId());
        assertEquals(CalendarEvent.Privacy.PRIVATE,decoded.getPrivacy());
        assertEquals(2,decoded.getRecurrence().getInterval());
        assertEquals(2,decoded.getRecurrence().getDaysOfWeek().size());
        assertEquals(CalendarAttendee.Role.OPTIONAL,decoded.getAttendees().get(0).getRole());
        assertEquals(Duration.ofMinutes(15),decoded.getAlarms().get(0).getTimeBefore());
        assertEquals("https://meet.example.com/abc",decoded.getConference().getJoinUrl());
        assertEquals("preserved",decoded.getProviderData().get("X-CN1-TEST"));
        assertTrue(encoded.contains("\r\n ") || encoded.contains("Planning\\, review"));
    }

    @Test
    void allDayTaskRoundTripDoesNotInventATimeZone() throws Exception {
        CalendarTask task=new CalendarTask().setId("todo-1").setTitle("Ship").setDue(CalendarDateTime.allDay(LocalDate.of(2026,7,31))).setCompleted(true).setPriority(3);
        CalendarTask decoded=ICalendarCodec.readTask(ICalendarCodec.writeTask(task));
        assertTrue(decoded.getDue().isAllDay());
        assertEquals(LocalDate.of(2026,7,31),decoded.getDue().getDate());
        assertTrue(decoded.isCompleted());
        assertEquals(3,decoded.getPriority());
    }

    @Test
    void portableDateCodecHandlesFractionsAndOffsets() {
        assertEquals(Instant.ofEpochMilli(123L), CalendarDateUtil.parseDateTime("1970-01-01T02:30:00.123+02:30", ZoneId.of("UTC")));
        assertEquals("19700101T000000", CalendarDateUtil.formatBasic(Instant.ofEpochMilli(0L), ZoneId.of("UTC")));
        assertEquals("1970-01-01T00:00:00.000", CalendarDateUtil.formatIso(Instant.ofEpochMilli(0L), ZoneId.of("UTC"), true));
    }

    @Test
    void googleSourceRefreshesOnceAfterUnauthorizedAndReturnsDeltaToken() {
        RecordingTokens tokens=new RecordingTokens();
        RecordingTransport transport=new RecordingTransport();
        transport.add(401,"",null);
        transport.add(200,"{\"items\":[{\"id\":\"e1\",\"summary\":\"Review\",\"start\":{\"date\":\"2026-07-20\"},\"end\":{\"date\":\"2026-07-21\"}}],\"nextSyncToken\":\"sync-2\"}",null);
        GoogleCalendarSource source=new GoogleCalendarSource(tokens,transport);
        CalendarPage<CalendarEvent> page=source.queryEvents(new CalendarQuery().setCalendarId("primary")).get();
        assertEquals(2,tokens.calls);
        assertTrue(tokens.lastForceRefresh);
        assertEquals("sync-2",page.getSyncToken());
        assertEquals("Review",page.getItems().get(0).getTitle());
        assertEquals("Bearer refreshed",transport.requests.get(1).getHeaders().get("Authorization"));
    }

    @Test
    void providerHttpErrorsDoNotExposeResponseBodies() {
        RecordingTransport transport = new RecordingTransport();
        transport.add(400, "{\"error\":\"private-user-data\"}", null);
        GoogleCalendarSource source = new GoogleCalendarSource(new RecordingTokens(), transport);

        AsyncResource.AsyncExecutionException error = assertThrows(AsyncResource.AsyncExecutionException.class,
                () -> source.queryEvents(new CalendarQuery().setCalendarId("primary")).get());

        assertTrue(error.getCause() instanceof CalendarException);
        assertEquals("Calendar provider returned HTTP 400", error.getCause().getMessage());
    }

    @Test
    void microsoftSourceReturnsDeltaLinkAndPortableRecurrence() {
        RecordingTokens tokens=new RecordingTokens();
        RecordingTransport transport=new RecordingTransport();
        transport.add(200,"{\"value\":[{\"id\":\"e2\",\"subject\":\"Standup\",\"start\":{\"dateTime\":\"2026-07-20T09:00:00\",\"timeZone\":\"UTC\"},\"end\":{\"dateTime\":\"2026-07-20T09:30:00\",\"timeZone\":\"UTC\"},\"recurrence\":{\"pattern\":{\"type\":\"weekly\",\"interval\":1,\"daysOfWeek\":[\"monday\"]},\"range\":{\"type\":\"numbered\",\"startDate\":\"2026-07-20\",\"numberOfOccurrences\":3}}}],\"@odata.deltaLink\":\"https://graph.microsoft.com/delta/2\"}",null);
        MicrosoftCalendarSource source=new MicrosoftCalendarSource(tokens,transport);

        CalendarPage<CalendarEvent> page=source.queryEvents(new CalendarQuery().setCalendarId("work")).get();

        assertEquals("https://graph.microsoft.com/delta/2",page.getSyncToken());
        assertEquals("Standup",page.getItems().get(0).getTitle());
        assertEquals(CalendarRecurrenceRule.Frequency.WEEKLY,page.getItems().get(0).getRecurrence().getFrequency());
        assertEquals(Integer.valueOf(3),page.getItems().get(0).getRecurrence().getCount());
    }

    @Test
    void localChangeListenersCanBeAddedAndRemoved() {
        final NotifyingSource source=new NotifyingSource();
        final List<CalendarChange> changes=new ArrayList<CalendarChange>();
        CalendarChangeListener listener=new CalendarChangeListener(){
            public void calendarChanged(CalendarChange change){changes.add(change);}
        };
        source.addChangeListener(listener);
        Display.getInstance().callSeriallyAndWait(new Runnable(){public void run(){source.emit();}});
        source.removeChangeListener(listener);
        Display.getInstance().callSeriallyAndWait(new Runnable(){public void run(){source.emit();}});

        assertEquals(1,changes.size());
        assertEquals(CalendarChange.ChangeType.RESET,changes.get(0).getChangeType());
        assertEquals(CalendarChange.EntityType.EVENT,changes.get(0).getEntityType());
    }

    @Test
    void calDavDigestRetriesChallengeAndParsesEvent() {
        RecordingTransport transport=new RecordingTransport();
        Map<String,String>challenge=new HashMap<String,String>();
        challenge.put("WWW-Authenticate","Digest realm=\"calendar\", nonce=\"abc123\", qop=\"auth\"");
        transport.add(401,"",challenge);
        transport.add(207,"<?xml version=\"1.0\"?><d:multistatus xmlns:d=\"DAV:\" xmlns:c=\"urn:ietf:params:xml:ns:caldav\"><d:sync-token>token-9</d:sync-token><d:response><d:href>/cal/e1.ics</d:href><d:propstat><d:prop><d:getetag>\"v1\"</d:getetag><c:calendar-data>BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nUID:e1\r\nSUMMARY:CalDAV event\r\nDTSTART;VALUE=DATE:20260720\r\nDTEND;VALUE=DATE:20260721\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n</c:calendar-data></d:prop></d:propstat></d:response></d:multistatus>",null);
        CalDavCalendarSource source=new CalDavCalendarSource("work","Work","https://dav.example.com/cal/",CalDavAuthentication.digest("user","pass"),transport);
        CalendarPage<CalendarEvent> page=source.queryEvents(new CalendarQuery().setCalendarId("https://dav.example.com/cal/")).get();
        assertEquals("token-9",page.getSyncToken());
        assertEquals("CalDAV event",page.getItems().get(0).getTitle());
        String authorization=transport.requests.get(1).getHeaders().get("Authorization");
        assertTrue(authorization.startsWith("Digest "));
        assertTrue(authorization.contains("qop=auth"));
    }

    @Test
    void offlineQueueAppliesMutationsOnlyWhenSyncIsCalled() throws Exception {
        MemoryCalendarCache cache=new MemoryCalendarCache();
        InMemorySource source=new InMemorySource();
        CalendarSyncEngine engine=new CalendarSyncEngine(source,cache);
        assertThrows(IllegalArgumentException.class,
                () -> engine.queueEventSave(null, CalendarMutationScope.ALL));
        assertThrows(IllegalArgumentException.class,
                () -> engine.queueEventSave(new CalendarEvent(), CalendarMutationScope.ALL));
        assertThrows(IllegalArgumentException.class,
                () -> engine.queueTaskSave(null, CalendarMutationScope.ALL));
        assertThrows(IllegalArgumentException.class,
                () -> engine.queueTaskSave(new CalendarTask(), CalendarMutationScope.ALL));
        assertThrows(IllegalArgumentException.class,
                () -> engine.queueEventDelete(null, "event", null, CalendarMutationScope.ALL));
        assertThrows(IllegalArgumentException.class,
                () -> engine.queueEventDelete("work", null, null, CalendarMutationScope.ALL));
        assertThrows(IllegalArgumentException.class,
                () -> engine.queueTaskDelete(null, "task", null, CalendarMutationScope.ALL));
        assertThrows(IllegalArgumentException.class,
                () -> engine.queueTaskDelete("work", null, null, CalendarMutationScope.ALL));
        engine.queueEventSave(new CalendarEvent().setCalendarId("work").setTitle("Offline"),CalendarMutationScope.ALL);
        assertEquals(0,source.saved);
        assertEquals(1,engine.getPendingCount());
        CalendarSyncResult result=engine.sync().get();
        assertEquals(1,source.saved);
        assertEquals(1,result.getAppliedCount());
        assertEquals(0,result.getRemainingCount());
    }

    private static final class RecordingTokens implements CalendarTokenProvider {
        int calls; boolean lastForceRefresh;
        public AsyncResource<CalendarAuthToken>getToken(String[]scopes,boolean forceRefresh){calls++;lastForceRefresh=forceRefresh;return complete(new CalendarAuthToken(forceRefresh?"refreshed":"initial",null,null));}
    }
    private static final class RecordingTransport implements CalendarHttpTransport {
        final List<CalendarHttpRequest>requests=new ArrayList<CalendarHttpRequest>();final List<CalendarHttpResponse>responses=new ArrayList<CalendarHttpResponse>();
        void add(int code,String body,Map<String,String>headers){responses.add(new CalendarHttpResponse(code,body,headers));}
        public AsyncResource<CalendarHttpResponse>execute(CalendarHttpRequest request){requests.add(request);return complete(responses.remove(0));}
    }
    private static final class InMemorySource extends CalendarSource {
        int saved; InMemorySource(){super("memory","Memory");}
        public AsyncResource<CalendarEvent>saveEvent(CalendarEvent event,CalendarMutationScope scope){saved++;event.setId("saved-"+saved);return complete(event);}
    }
    private static final class NotifyingSource extends CalendarSource {
        NotifyingSource(){super("notifying","Notifying");}
        void emit(){fireChange(new CalendarChange(getId(),null,null,CalendarChange.EntityType.EVENT,CalendarChange.ChangeType.RESET));}
    }
    private static <T>AsyncResource<T>complete(T value){AsyncResource<T>out=new AsyncResource<T>();out.complete(value);return out;}
}
