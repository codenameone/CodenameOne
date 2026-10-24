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
package com.codename1.desktopcompat.org.jdesktop.swingx.calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.org.jdesktop.swingx.calendar.DateSelectionModel.SelectionMode;
import com.codename1.desktopcompat.org.jdesktop.swingx.event.DateSelectionEvent;
import com.codename1.desktopcompat.org.jdesktop.swingx.event.DateSelectionEvent.EventType;
import com.codename1.desktopcompat.org.jdesktop.swingx.event.DateSelectionListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import org.junit.Test;

/// The selection models on their own: no component and no display.
public class DateSelectionModelTest {

    private static Date at(int day, int hour) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 2024);
        c.set(Calendar.MONTH, Calendar.MARCH);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static Date day(int day) {
        return at(day, 0);
    }

    private static List<EventType> listen(DateSelectionModel m) {
        final List<EventType> seen = new ArrayList<EventType>();
        m.addDateSelectionListener(new DateSelectionListener() {
            @Override
            public void valueChanged(DateSelectionEvent ev) {
                seen.add(ev.getEventType());
            }
        });
        return seen;
    }

    @Test
    public void daySelectionReadsADateAsItsDay() {
        DaySelectionModel m = new DaySelectionModel();
        List<EventType> seen = listen(m);
        assertTrue(m.isSelectionEmpty());
        assertNull(m.getFirstSelectionDate());
        m.setSelectionInterval(at(10, 15), at(10, 15));
        assertEquals(day(10), m.getFirstSelectionDate());
        assertEquals(day(10), m.getNormalizedDate(at(10, 23)));
        assertTrue(m.isSelected(at(10, 8)));
        assertFalse(m.isSelected(day(11)));
        assertEquals(1, seen.size());
        assertEquals(EventType.DATES_SET, seen.get(0));
        // The same day again changes nothing and tells nobody.
        m.setSelectionInterval(day(10), day(10));
        assertEquals(1, seen.size());
        m.clearSelection();
        assertTrue(m.isSelectionEmpty());
        assertEquals(EventType.SELECTION_CLEARED, seen.get(1));
    }

    @Test
    public void theModeDecidesWhatAnIntervalSelects() {
        DaySelectionModel m = new DaySelectionModel();
        assertEquals(SelectionMode.SINGLE_SELECTION, m.getSelectionMode());
        m.setSelectionInterval(day(5), day(8));
        assertEquals(1, m.getSelection().size());
        assertEquals(day(5), m.getFirstSelectionDate());

        m.setSelectionMode(SelectionMode.SINGLE_INTERVAL_SELECTION);
        assertTrue("changing the mode clears the selection", m.isSelectionEmpty());
        m.setSelectionInterval(day(5), day(8));
        assertEquals(4, m.getSelection().size());
        assertEquals(day(5), m.getFirstSelectionDate());
        assertEquals(day(8), m.getLastSelectionDate());
        m.addSelectionInterval(day(20), day(21));
        assertEquals("one interval at a time", 2, m.getSelection().size());
        assertEquals(day(20), m.getFirstSelectionDate());

        m.setSelectionMode(SelectionMode.MULTIPLE_INTERVAL_SELECTION);
        m.setSelectionInterval(day(5), day(8));
        m.addSelectionInterval(day(20), day(21));
        assertEquals(6, m.getSelection().size());
        m.removeSelectionInterval(day(6), day(7));
        assertEquals(4, m.getSelection().size());
        assertTrue(m.isSelected(day(5)));
        assertFalse(m.isSelected(day(6)));
        assertTrue(m.isSelected(day(21)));
        // An interval that ends before it starts selects nothing.
        m.setSelectionInterval(day(9), day(3));
        assertEquals(4, m.getSelection().size());
    }

    @Test
    public void unselectableDatesAndBoundsAreLeftOut() {
        DaySelectionModel m = new DaySelectionModel();
        m.setSelectionMode(SelectionMode.SINGLE_INTERVAL_SELECTION);
        TreeSet<Date> no = new TreeSet<Date>();
        no.add(at(11, 9));
        m.setUnselectableDates(no);
        assertTrue(m.isUnselectableDate(day(11)));
        assertEquals(1, m.getUnselectableDates().size());
        assertEquals(day(11), m.getUnselectableDates().first());
        m.setSelectionInterval(day(10), day(12));
        assertEquals(2, m.getSelection().size());
        assertFalse(m.isSelected(day(11)));

        List<EventType> seen = listen(m);
        m.setLowerBound(day(11));
        assertEquals(day(11), m.getLowerBound());
        assertTrue(m.isUnselectableDate(day(10)));
        assertFalse("the day before the bound left the selection", m.isSelected(day(10)));
        assertTrue(m.isSelected(day(12)));
        assertTrue(seen.contains(EventType.LOWER_BOUND_CHANGED));
        m.setUpperBound(day(20));
        assertEquals(day(20), m.getUpperBound());
        assertTrue(m.isUnselectableDate(day(21)));
        assertFalse(m.isUnselectableDate(day(20)));
        assertTrue("the bound itself stays selectable", m.isSelected(day(12)));
        assertTrue(seen.contains(EventType.UPPER_BOUND_CHANGED));
        m.setSelectionInterval(day(19), day(23));
        assertEquals(2, m.getSelection().size());
        assertEquals(day(20), m.getLastSelectionDate());
    }

    @Test
    public void adjustingAndTheCalendarAreAnnounced() {
        DaySelectionModel m = new DaySelectionModel();
        List<EventType> seen = listen(m);
        assertFalse(m.isAdjusting());
        m.setAdjusting(true);
        assertTrue(m.isAdjusting());
        m.setAdjusting(true);
        m.setAdjusting(false);
        assertEquals(2, seen.size());
        assertEquals(EventType.ADJUSTING_STARTED, seen.get(0));
        assertEquals(EventType.ADJUSTING_STOPPED, seen.get(1));

        assertEquals(Calendar.SUNDAY, m.getFirstDayOfWeek());
        m.setFirstDayOfWeek(Calendar.MONDAY);
        assertEquals(Calendar.MONDAY, m.getFirstDayOfWeek());
        assertEquals(EventType.CALENDAR_CHANGED, seen.get(2));
        // Every call answers a calendar of its own.
        Calendar a = m.getCalendar();
        a.setTime(day(3));
        Calendar b = m.getCalendar();
        b.setTime(day(9));
        assertEquals(3, a.get(Calendar.DAY_OF_MONTH));
        assertEquals(1, m.getDateSelectionListeners().size());
    }

    @Test
    public void theDefaultModelKeepsTheInstant() {
        DefaultDateSelectionModel m = new DefaultDateSelectionModel();
        m.setSelectionInterval(at(10, 15), at(10, 15));
        assertEquals(at(10, 15), m.getFirstSelectionDate());
        assertTrue(m.isSelected(at(10, 15)));
        assertFalse(m.isSelected(day(10)));
        assertEquals(at(10, 15), m.getNormalizedDate(at(10, 15)));
        m.setSelectionMode(SelectionMode.SINGLE_INTERVAL_SELECTION);
        m.setSelectionInterval(at(10, 15), at(12, 15));
        assertEquals(3, m.getSelection().size());
        assertEquals(at(12, 15), m.getLastSelectionDate());
    }

    @Test
    public void theCalendarHelpersFindTheEdgesOfADay() {
        Calendar c = Calendar.getInstance();
        assertEquals(day(10), CalendarUtils.startOfDay(c, at(10, 17)));
        assertEquals(day(11).getTime() - 1, CalendarUtils.endOfDay(c, at(10, 17)).getTime());
        c.setTime(at(10, 17));
        assertTrue(CalendarUtils.isSameDay(c, at(10, 23)));
        assertFalse(CalendarUtils.isSameDay(c, at(11, 1)));
        assertEquals(at(10, 17), c.getTime());
        CalendarUtils.startOfMonth(c);
        assertEquals(day(1), c.getTime());
        assertTrue(CalendarUtils.isStartOfMonth(c));
        CalendarUtils.endOfMonth(c);
        assertEquals(31, c.get(Calendar.DAY_OF_MONTH));
        assertTrue(CalendarUtils.isEndOfMonth(c));
        assertTrue(CalendarUtils.isEndOfDay(c));
        assertEquals(29, CalendarUtils.daysInMonth(2024, Calendar.FEBRUARY));
        assertEquals(28, CalendarUtils.daysInMonth(2100, Calendar.FEBRUARY));
        assertTrue(CalendarUtils.areEqual(null, null));
        assertFalse(CalendarUtils.areEqual(day(1), null));
        assertTrue(CalendarUtils.areEqual(day(1), at(1, 0)));
    }
}
