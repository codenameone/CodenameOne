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
package com.codename1.compat.jdk;

import java.time.DateTimeException;

/// `java.time.Month`: the twelve months of the ISO calendar.
public enum Month {
    JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;

    private static final Month[] ALL = values();

    /// The month numbered `month`, 1 for January to 12 for December.
    public static Month of(int month) {
        if (month < 1 || month > 12) {
            throw new DateTimeException("Invalid value for MonthOfYear: " + month);
        }
        return ALL[month - 1];
    }

    /// 1 for January to 12 for December.
    public int getValue() {
        return ordinal() + 1;
    }

    /// The month `months` later, around the year.
    public Month plus(long months) {
        int amount = (int) (months % 12);
        return ALL[(ordinal() + amount + 12) % 12];
    }

    /// The month `months` earlier, around the year.
    public Month minus(long months) {
        return plus(-(months % 12));
    }

    /// The number of days of this month in a leap year or another.
    public int length(boolean leapYear) {
        // Not a switch: one over an enum compiles to a class that catches
        // NoSuchFieldError, which a device does not have.
        if (this == FEBRUARY) {
            return leapYear ? 29 : 28;
        }
        if (this == APRIL || this == JUNE || this == SEPTEMBER || this == NOVEMBER) {
            return 30;
        }
        return 31;
    }

    /// The fewest days this month can have.
    public int minLength() {
        return length(false);
    }

    /// The most days this month can have.
    public int maxLength() {
        return length(true);
    }

    /// The day of the year this month starts on, 1 for January.
    public int firstDayOfYear(boolean leapYear) {
        int day = 1;
        for (int i = 0; i < ordinal(); i++) {
            day += ALL[i].length(leapYear);
        }
        return day;
    }

    /// The first month of the quarter this month is in.
    public Month firstMonthOfQuarter() {
        return ALL[ordinal() / 3 * 3];
    }
}
