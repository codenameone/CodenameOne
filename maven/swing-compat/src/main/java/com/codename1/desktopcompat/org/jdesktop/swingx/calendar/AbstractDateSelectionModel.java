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

import com.codename1.desktopcompat.org.jdesktop.swingx.event.DateSelectionEvent;
import com.codename1.desktopcompat.org.jdesktop.swingx.event.DateSelectionEvent.EventType;
import com.codename1.desktopcompat.org.jdesktop.swingx.event.DateSelectionListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeSet;

/// The part of a date selection model that does not depend on how the
/// selection is stored: the calendar, the bounds, the adjusting flag and
/// the listeners.
///
/// ## What differs from SwingX
///
///  - The first day of the week and the minimal days in the first week are
///    kept by the model; the calendar answered by [#getCalendar()] does
///    not carry them, because the device's calendar has no such
///    properties. The first day of the week starts as Sunday whatever the
///    locale.
///  - The locale is recorded and reported, nothing more.
///  - The `listenerMap` field and the `EMPTY_DATES` constant are absent.
public abstract class AbstractDateSelectionModel implements DateSelectionModel {

    protected boolean adjusting;
    protected Calendar calendar;
    protected Date upperBound;
    protected Date lowerBound;
    protected Locale locale;

    private int firstDayOfWeek = Calendar.SUNDAY;
    private int minimalDaysInFirstWeek = 1;
    private final ArrayList<DateSelectionListener> listeners = new ArrayList<DateSelectionListener>();

    public AbstractDateSelectionModel() {
        this(null);
    }

    public AbstractDateSelectionModel(Locale locale) {
        this.locale = locale != null ? locale : Locale.getDefault();
        this.calendar = Calendar.getInstance();
    }

    @Override
    public Calendar getCalendar() {
        Calendar c = Calendar.getInstance(calendar.getTimeZone());
        c.setTime(calendar.getTime());
        return c;
    }

    @Override
    public int getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    /// Sets the day a week starts with, one of the `Calendar` day
    /// constants. Anything else is ignored.
    @Override
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        if (firstDayOfWeek < Calendar.SUNDAY || firstDayOfWeek > Calendar.SATURDAY
                || firstDayOfWeek == this.firstDayOfWeek) {
            return;
        }
        this.firstDayOfWeek = firstDayOfWeek;
        fireValueChanged(EventType.CALENDAR_CHANGED);
    }

    @Override
    public int getMinimalDaysInFirstWeek() {
        return minimalDaysInFirstWeek;
    }

    @Override
    public void setMinimalDaysInFirstWeek(int minimalDays) {
        if (minimalDays == minimalDaysInFirstWeek) {
            return;
        }
        minimalDaysInFirstWeek = minimalDays;
        fireValueChanged(EventType.CALENDAR_CHANGED);
    }

    @Override
    public TimeZone getTimeZone() {
        return calendar.getTimeZone();
    }

    /// Changes the time zone dates are read in. What was selected, the
    /// bounds and the unselectable dates meant days of the old zone and
    /// are dropped.
    @Override
    public void setTimeZone(TimeZone timeZone) {
        TimeZone old = calendar.getTimeZone();
        if (timeZone == null || timeZone.equals(old)) {
            return;
        }
        calendar.setTimeZone(timeZone);
        adjustDatesToTimeZone(old);
        fireValueChanged(EventType.CALENDAR_CHANGED);
    }

    /// Called when the time zone changed; `oldTimeZone` is the one before.
    protected void adjustDatesToTimeZone(TimeZone oldTimeZone) {
        clearSelection();
        setLowerBound(null);
        setUpperBound(null);
        setUnselectableDates(new TreeSet<Date>());
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public void setLocale(Locale locale) {
        Locale l = locale != null ? locale : Locale.getDefault();
        if (l.equals(this.locale)) {
            return;
        }
        this.locale = l;
        fireValueChanged(EventType.CALENDAR_CHANGED);
    }

    /// The first millisecond of the day `date` falls on.
    protected Date startOfDay(Date date) {
        return CalendarUtils.startOfDay(calendar, date);
    }

    /// The last millisecond of the day `date` falls on.
    protected Date endOfDay(Date date) {
        return CalendarUtils.endOfDay(calendar, date);
    }

    protected boolean isSameDay(Date selected, Date compare) {
        return startOfDay(selected).equals(startOfDay(compare));
    }

    @Override
    public Date getUpperBound() {
        return copy(upperBound);
    }

    @Override
    public void setUpperBound(Date upperBound) {
        Date bound = upperBound != null ? getNormalizedDate(upperBound) : null;
        if (CalendarUtils.areEqual(bound, this.upperBound)) {
            return;
        }
        this.upperBound = bound;
        dropOutside();
        fireValueChanged(EventType.UPPER_BOUND_CHANGED);
    }

    @Override
    public Date getLowerBound() {
        return copy(lowerBound);
    }

    @Override
    public void setLowerBound(Date lowerBound) {
        Date bound = lowerBound != null ? getNormalizedDate(lowerBound) : null;
        if (CalendarUtils.areEqual(bound, this.lowerBound)) {
            return;
        }
        this.lowerBound = bound;
        dropOutside();
        fireValueChanged(EventType.LOWER_BOUND_CHANGED);
    }

    /// Unselects the dates the bounds no longer allow.
    private void dropOutside() {
        if (isSelectionEmpty()) {
            return;
        }
        Iterator<Date> it = getSelection().iterator();
        while (it.hasNext()) {
            Date d = it.next();
            boolean above = upperBound != null && d.getTime() > upperBound.getTime();
            boolean below = lowerBound != null && d.getTime() < lowerBound.getTime();
            if (above || below) {
                removeSelectionInterval(d, d);
            }
        }
    }

    private static Date copy(Date d) {
        return d == null ? null : new Date(d.getTime());
    }

    @Override
    public boolean isAdjusting() {
        return adjusting;
    }

    @Override
    public void setAdjusting(boolean adjusting) {
        if (adjusting == this.adjusting) {
            return;
        }
        this.adjusting = adjusting;
        fireValueChanged(adjusting ? EventType.ADJUSTING_STARTED : EventType.ADJUSTING_STOPPED);
    }

    @Override
    public void addDateSelectionListener(DateSelectionListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    @Override
    public void removeDateSelectionListener(DateSelectionListener l) {
        listeners.remove(l);
    }

    public List<DateSelectionListener> getDateSelectionListeners() {
        return new ArrayList<DateSelectionListener>(listeners);
    }

    protected void fireValueChanged(EventType eventType) {
        if (listeners.isEmpty()) {
            return;
        }
        DateSelectionListener[] all = listeners.toArray(new DateSelectionListener[listeners.size()]);
        DateSelectionEvent e = new DateSelectionEvent(this, eventType, isAdjusting());
        for (int i = 0; i < all.length; i++) {
            all[i].valueChanged(e);
        }
    }
}
