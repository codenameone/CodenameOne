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

import com.codename1.calendar.CalendarDate;
import com.codename1.calendar.CalendarDateTime;
import com.codename1.calendar.CalendarEvent;
import com.codename1.calendar.CalendarRecurrenceRule;
import com.codename1.calendar.ICalendarCodec;
import com.codename1.calendar.LocalCalendarSource;

/** Portable calendar smoke coverage; never requests permission or writes user data. */
public final class CalendarApiTest extends BaseTest {
    public boolean shouldTakeScreenshot(){return false;}
    public boolean runTest(){try{CalendarEvent source=new CalendarEvent().setId("cn1-calendar-smoke").setTitle("Calendar smoke").setStart(CalendarDateTime.allDay(new CalendarDate(2026,7,19))).setEnd(CalendarDateTime.allDay(new CalendarDate(2026,7,20))).setRecurrence(new CalendarRecurrenceRule().setFrequency(CalendarRecurrenceRule.Frequency.YEARLY));CalendarEvent decoded=ICalendarCodec.readEvent(ICalendarCodec.writeEvent(source));assertEqual("cn1-calendar-smoke",decoded.getId());assertTrue(decoded.getStart().isAllDay(),"All-day event lost its date-only semantics");assertEqual(CalendarRecurrenceRule.Frequency.YEARLY,decoded.getRecurrence().getFrequency());System.out.println("CN1SS:CALENDAR_DIAG capabilities="+LocalCalendarSource.getInstance().getCapabilities().asSet());done();return true;}catch(Throwable error){fail("Calendar API test failed: "+error);return false;}}
}
