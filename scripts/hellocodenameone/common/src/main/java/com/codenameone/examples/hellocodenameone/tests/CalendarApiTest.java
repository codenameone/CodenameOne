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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.calendar.CalendarDateTime;
import com.codename1.calendar.CalendarAlarm;
import com.codename1.calendar.CalendarEvent;
import com.codename1.calendar.CalendarRecurrenceRule;
import com.codename1.calendar.ICalendarCodec;
import com.codename1.calendar.LocalCalendarSource;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Portable calendar smoke coverage; never requests permission or writes user data. */
public final class CalendarApiTest extends BaseTest {
    public boolean shouldTakeScreenshot(){return false;}
    public boolean runTest(){try{CalendarEvent source=new CalendarEvent().setId("cn1-calendar-smoke").setTitle("Calendar smoke").setStart(CalendarDateTime.allDay(LocalDate.of(2026,7,19))).setEnd(CalendarDateTime.allDay(LocalDate.of(2026,7,20))).setRecurrence(new CalendarRecurrenceRule().setFrequency(CalendarRecurrenceRule.Frequency.YEARLY)).addAlarm(new CalendarAlarm().setTimeBefore(Duration.ofMinutes(15)));CalendarEvent decoded=ICalendarCodec.readEvent(ICalendarCodec.writeEvent(source));assertEqual("cn1-calendar-smoke",decoded.getId());assertTrue(decoded.getStart().isAllDay(),"All-day event lost its date-only semantics");assertEqual(LocalDate.of(2026,7,19),decoded.getStart().getDate());assertEqual(Duration.ofMinutes(15),decoded.getAlarms().get(0).getTimeBefore());assertEqual(CalendarRecurrenceRule.Frequency.YEARLY,decoded.getRecurrence().getFrequency());Instant instant=Instant.ofEpochMilli(1784455200000L);CalendarEvent timed=new CalendarEvent().setStart(CalendarDateTime.instant(instant,ZoneId.of("Asia/Jerusalem")));CalendarEvent timedDecoded=ICalendarCodec.readEvent(ICalendarCodec.writeEvent(timed));assertEqual(instant.toEpochMilli(),timedDecoded.getStart().getDateTime().toInstant().toEpochMilli());System.out.println("CN1SS:CALENDAR_DIAG capabilities="+LocalCalendarSource.getInstance().getCapabilities().asSet());done();return true;}catch(Throwable error){fail("Calendar API test failed: "+error);return false;}}
}
