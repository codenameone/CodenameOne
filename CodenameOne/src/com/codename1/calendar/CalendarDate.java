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

/// A date without a time zone, used for all-day events and date-only tasks.
public final class CalendarDate {

    private final int year;

    private final int month;

    private final int day;

    public CalendarDate(int year, int month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            throw new IllegalArgumentException("Invalid date");
        }
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    @Override
    public String toString() {
        return pad(year, 4) + "-" + pad(month, 2) + "-" + pad(day, 2);
    }

    private static String pad(int value, int size) {
        String s = String.valueOf(value);
        StringBuilder out = new StringBuilder(size);
        for (int i = s.length(); i < size; i++) {
            out.append('0');
        }
        return out.append(s).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CalendarDate)) {
            return false;
        }
        CalendarDate d = (CalendarDate) o;
        return year == d.year && month == d.month && day == d.day;
    }

    @Override
    public int hashCode() {
        return ((year * 31) + month) * 31 + day;
    }
}
