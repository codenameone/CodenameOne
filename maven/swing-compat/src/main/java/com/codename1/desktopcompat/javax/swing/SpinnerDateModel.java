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
package com.codename1.desktopcompat.javax.swing;

import java.util.Calendar;
import java.util.Date;

/// A spinner model over dates: a value, an optional earliest and latest
/// date, and the calendar field one step changes, such as
/// `Calendar.DAY_OF_MONTH`.
///
/// Stepping is `Calendar.add` on the default calendar, so a field the
/// platform's calendar cannot add to does not step. A bound that is a date
/// is compared by its time.
@SuppressWarnings("rawtypes")
public class SpinnerDateModel extends AbstractSpinnerModel {

    private static final int LAST_FIELD = Calendar.MILLISECOND;

    private Comparable start;
    private Comparable end;
    private final Calendar value;
    private int calendarField;

    public SpinnerDateModel(Date value, Comparable start, Comparable end, int calendarField) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (!validField(calendarField)) {
            throw new IllegalArgumentException("invalid calendarField");
        }
        if (!((start == null || compare(start, value) <= 0) && (end == null || compare(end, value) >= 0))) {
            throw new IllegalArgumentException("(start <= value <= end) is false");
        }
        this.value = Calendar.getInstance();
        this.start = start;
        this.end = end;
        this.calendarField = calendarField;
        this.value.setTime(value);
    }

    public SpinnerDateModel() {
        this(new Date(), null, null, Calendar.DAY_OF_MONTH);
    }

    /// The fields run from the era, zero, to the millisecond.
    private static boolean validField(int field) {
        return field >= 0 && field <= LAST_FIELD;
    }

    @SuppressWarnings("unchecked")
    private static int compare(Comparable bound, Date d) {
        if (bound instanceof Date) {
            long x = ((Date) bound).getTime();
            long y = d.getTime();
            return x < y ? -1 : x == y ? 0 : 1;
        }
        return bound.compareTo(d);
    }

    private static boolean same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    public void setStart(Comparable start) {
        if (!same(start, this.start)) {
            this.start = start;
            fireStateChanged();
        }
    }

    public Comparable getStart() {
        return start;
    }

    public void setEnd(Comparable end) {
        if (!same(end, this.end)) {
            this.end = end;
            fireStateChanged();
        }
    }

    public Comparable getEnd() {
        return end;
    }

    public void setCalendarField(int calendarField) {
        if (!validField(calendarField)) {
            throw new IllegalArgumentException("invalid calendarField");
        }
        if (calendarField != this.calendarField) {
            this.calendarField = calendarField;
            fireStateChanged();
        }
    }

    public int getCalendarField() {
        return calendarField;
    }

    private Date step(int dir) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(value.getTime());
        cal.add(calendarField, dir);
        return cal.getTime();
    }

    @Override
    public Object getNextValue() {
        Date next = step(1);
        return end == null || compare(end, next) >= 0 ? next : null;
    }

    @Override
    public Object getPreviousValue() {
        Date prev = step(-1);
        return start == null || compare(start, prev) <= 0 ? prev : null;
    }

    public Date getDate() {
        return value.getTime();
    }

    @Override
    public Object getValue() {
        return value.getTime();
    }

    @Override
    public void setValue(Object value) {
        if (!(value instanceof Date)) {
            throw new IllegalArgumentException("illegal value");
        }
        if (!value.equals(this.value.getTime())) {
            this.value.setTime((Date) value);
            fireStateChanged();
        }
    }
}
