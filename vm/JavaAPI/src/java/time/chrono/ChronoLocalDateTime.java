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

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;

/// A date and a time of day of a calendar, with no zone.
public interface ChronoLocalDateTime<D extends ChronoLocalDate>
        extends Temporal, Comparable<ChronoLocalDateTime<?>> {
    /// The date.
    D toLocalDate();

    /// The time of day.
    LocalTime toLocalTime();

    /// The seconds since 1970-01-01T00:00Z of this date and time at `offset`.
    default long toEpochSecond(ZoneOffset offset) {
        long epochDay = toLocalDate().toEpochDay();
        long secs = epochDay * 86400L + toLocalTime().toSecondOfDay();
        return secs - offset.getTotalSeconds();
    }

    /// The moment this date and time is at `offset`.
    default Instant toInstant(ZoneOffset offset) {
        return Instant.ofEpochSecond(toEpochSecond(offset), toLocalTime().getNano());
    }

    /// Whether this comes after `other` on the local time line.
    default boolean isAfter(ChronoLocalDateTime<?> other) {
        long thisDay = toLocalDate().toEpochDay();
        long otherDay = other.toLocalDate().toEpochDay();
        return thisDay > otherDay
                || (thisDay == otherDay && toLocalTime().toNanoOfDay() > other.toLocalTime().toNanoOfDay());
    }

    /// Whether this comes before `other` on the local time line.
    default boolean isBefore(ChronoLocalDateTime<?> other) {
        long thisDay = toLocalDate().toEpochDay();
        long otherDay = other.toLocalDate().toEpochDay();
        return thisDay < otherDay
                || (thisDay == otherDay && toLocalTime().toNanoOfDay() < other.toLocalTime().toNanoOfDay());
    }

    /// Whether this is the same date and time as `other`.
    default boolean isEqual(ChronoLocalDateTime<?> other) {
        return toLocalTime().toNanoOfDay() == other.toLocalTime().toNanoOfDay()
                && toLocalDate().toEpochDay() == other.toLocalDate().toEpochDay();
    }
}
