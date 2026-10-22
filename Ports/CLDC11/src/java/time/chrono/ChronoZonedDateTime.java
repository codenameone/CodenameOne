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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;

/// A date and a time of day in a time zone.
public interface ChronoZonedDateTime<D extends ChronoLocalDate>
        extends Temporal, Comparable<ChronoZonedDateTime<?>> {
    /// The date and time on the wall clock.
    ChronoLocalDateTime<D> toLocalDateTime();

    /// The offset from UTC.
    ZoneOffset getOffset();

    /// The zone.
    ZoneId getZone();

    /// The date.
    default D toLocalDate() {
        return toLocalDateTime().toLocalDate();
    }

    /// The time of day.
    default LocalTime toLocalTime() {
        return toLocalDateTime().toLocalTime();
    }

    /// The seconds since 1970-01-01T00:00Z.
    default long toEpochSecond() {
        return toLocalDateTime().toEpochSecond(getOffset());
    }

    /// The moment this is.
    default Instant toInstant() {
        return Instant.ofEpochSecond(toEpochSecond(), toLocalTime().getNano());
    }

    /// Whether this moment comes after `other`.
    default boolean isAfter(ChronoZonedDateTime<?> other) {
        long thisSecond = toEpochSecond();
        long otherSecond = other.toEpochSecond();
        return thisSecond > otherSecond
                || (thisSecond == otherSecond && toLocalTime().getNano() > other.toLocalTime().getNano());
    }

    /// Whether this moment comes before `other`.
    default boolean isBefore(ChronoZonedDateTime<?> other) {
        long thisSecond = toEpochSecond();
        long otherSecond = other.toEpochSecond();
        return thisSecond < otherSecond
                || (thisSecond == otherSecond && toLocalTime().getNano() < other.toLocalTime().getNano());
    }

    /// Whether this is the same moment as `other`.
    default boolean isEqual(ChronoZonedDateTime<?> other) {
        return toEpochSecond() == other.toEpochSecond()
                && toLocalTime().getNano() == other.toLocalTime().getNano();
    }
}
