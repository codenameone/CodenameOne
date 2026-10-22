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
package java.time.chrono;

import java.time.temporal.Temporal;

/// A date of a calendar. The ISO calendar's `LocalDate` is the one there is.
public interface ChronoLocalDate extends Temporal, Comparable<ChronoLocalDate> {
    /// The days since 1970-01-01.
    long toEpochDay();

    /// Whether the year of this date is a leap year.
    boolean isLeapYear();

    /// The days in the month of this date.
    int lengthOfMonth();

    /// The days in the year of this date.
    default int lengthOfYear() {
        return isLeapYear() ? 366 : 365;
    }

    /// Whether this date comes after `other`.
    default boolean isAfter(ChronoLocalDate other) {
        return toEpochDay() > other.toEpochDay();
    }

    /// Whether this date comes before `other`.
    default boolean isBefore(ChronoLocalDate other) {
        return toEpochDay() < other.toEpochDay();
    }

    /// Whether this date is the same day as `other`.
    default boolean isEqual(ChronoLocalDate other) {
        return toEpochDay() == other.toEpochDay();
    }
}
