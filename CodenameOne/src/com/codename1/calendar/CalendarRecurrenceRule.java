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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Portable RFC-5545-style recurrence rule.
public class CalendarRecurrenceRule {

    public enum Frequency {

        DAILY, WEEKLY, MONTHLY, YEARLY
    }

    private Frequency frequency;

    private int interval = 1;

    private Integer count;

    private CalendarDateTime until;

    private final List<Integer> daysOfWeek = new ArrayList<Integer>();

    private final List<Integer> daysOfMonth = new ArrayList<Integer>();

    private final List<Integer> months = new ArrayList<Integer>();

    public Frequency getFrequency() {
        return frequency;
    }

    public CalendarRecurrenceRule setFrequency(Frequency v) {
        frequency = v;
        return this;
    }

    public int getInterval() {
        return interval;
    }

    public CalendarRecurrenceRule setInterval(int v) {
        if (v < 1) {
            throw new IllegalArgumentException("interval");
        }
        interval = v;
        return this;
    }

    public Integer getCount() {
        return count;
    }

    public CalendarRecurrenceRule setCount(Integer v) {
        count = v;
        return this;
    }

    public CalendarDateTime getUntil() {
        return until;
    }

    public CalendarRecurrenceRule setUntil(CalendarDateTime v) {
        until = v;
        return this;
    }

    public CalendarRecurrenceRule addDayOfWeek(int v) {
        if (v < 1 || v > 7) {
            throw new IllegalArgumentException("dayOfWeek");
        }
        daysOfWeek.add(v);
        return this;
    }

    public CalendarRecurrenceRule addDayOfMonth(int v) {
        if (v == 0 || v < -31 || v > 31) {
            throw new IllegalArgumentException("dayOfMonth");
        }
        daysOfMonth.add(v);
        return this;
    }

    public CalendarRecurrenceRule addMonth(int v) {
        if (v < 1 || v > 12) {
            throw new IllegalArgumentException("month");
        }
        months.add(v);
        return this;
    }

    public List<Integer> getDaysOfWeek() {
        return Collections.unmodifiableList(daysOfWeek);
    }

    public List<Integer> getDaysOfMonth() {
        return Collections.unmodifiableList(daysOfMonth);
    }

    public List<Integer> getMonths() {
        return Collections.unmodifiableList(months);
    }
}
