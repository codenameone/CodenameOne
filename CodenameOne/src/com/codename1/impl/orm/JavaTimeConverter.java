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
package com.codename1.impl.orm;

import com.codename1.orm.session.AttributeConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/// The storage of `java.time.Instant`, `LocalDate` and `LocalDateTime` fields.
///
/// The defaults follow `java.util.Date`'s: a number in an integer column, which
/// sorts, compares and indexes the same on every engine. An Instant or a
/// LocalDateTime is milliseconds since 1970-01-01T00:00 -- UTC for the Instant,
/// the wall clock itself for the LocalDateTime, which has no zone to convert
/// through -- and a LocalDate is the day count since 1970-01-01. So an Instant
/// reads a column a `long createdAt` or a `Date` wrote, unchanged.
///
/// `@Temporal` opts into a native column instead, through the same UTC ISO text
/// `TemporalConverter` writes: `TIMESTAMP` for an Instant or a LocalDateTime,
/// `DATE` for a LocalDate.
///
/// Precision is the millisecond, as for a Date: nanoseconds below it are dropped
/// toward the earlier instant on write.
///
/// Internal ORM runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class JavaTimeConverter {
    private static final long DAY = 86400000L;

    private JavaTimeConverter() {
    }

    /// An Instant as epoch milliseconds, the default.
    /// @hidden
    public static final class InstantMillis implements AttributeConverter<Instant, Long> {
        @Override
        public Long toDatabase(Instant value) {
            return value == null ? null : Long.valueOf(value.toEpochMilli());
        }

        @Override
        public Instant fromDatabase(Long value) {
            return value == null ? null : Instant.ofEpochMilli(value.longValue());
        }
    }

    /// An Instant in a native timestamp column, `@Temporal(TemporalType.TIMESTAMP)`.
    /// @hidden
    public static final class InstantTimestamp implements AttributeConverter<Instant, String> {
        private final TemporalConverter text = new TemporalConverter.Timestamp();

        @Override
        public String toDatabase(Instant value) {
            return value == null ? null : text.toDatabase(new java.util.Date(value.toEpochMilli()));
        }

        @Override
        public Instant fromDatabase(String value) {
            return value == null ? null : Instant.ofEpochMilli(TemporalConverter.parse(value));
        }
    }

    /// A LocalDate as the day count since 1970-01-01, the default.
    /// @hidden
    public static final class LocalDateEpochDay implements AttributeConverter<LocalDate, Long> {
        @Override
        public Long toDatabase(LocalDate value) {
            return value == null ? null : Long.valueOf(value.toEpochDay());
        }

        @Override
        public LocalDate fromDatabase(Long value) {
            return value == null ? null : LocalDate.ofEpochDay(value.longValue());
        }
    }

    /// A LocalDate in a native date column, `@Temporal(TemporalType.DATE)`.
    /// @hidden
    public static final class LocalDateDay implements AttributeConverter<LocalDate, String> {
        private final TemporalConverter text = new TemporalConverter.Day();

        @Override
        public String toDatabase(LocalDate value) {
            return value == null ? null : text.toDatabase(new java.util.Date(value.toEpochDay() * DAY));
        }

        @Override
        public LocalDate fromDatabase(String value) {
            return value == null ? null : LocalDate.ofEpochDay(floorDiv(TemporalConverter.parse(value), DAY));
        }
    }

    /// A LocalDateTime's wall clock as milliseconds since 1970-01-01T00:00, the
    /// default.
    /// @hidden
    public static final class LocalDateTimeMillis implements AttributeConverter<LocalDateTime, Long> {
        @Override
        public Long toDatabase(LocalDateTime value) {
            return value == null ? null : Long.valueOf(millis(value));
        }

        @Override
        public LocalDateTime fromDatabase(Long value) {
            return value == null ? null : wallClock(value.longValue());
        }
    }

    /// A LocalDateTime in a native timestamp column,
    /// `@Temporal(TemporalType.TIMESTAMP)`. The wall clock is written as is; the
    /// backend's UTC session means no zone ever shifts it.
    /// @hidden
    public static final class LocalDateTimeTimestamp implements AttributeConverter<LocalDateTime, String> {
        private final TemporalConverter text = new TemporalConverter.Timestamp();

        @Override
        public String toDatabase(LocalDateTime value) {
            return value == null ? null : text.toDatabase(new java.util.Date(millis(value)));
        }

        @Override
        public LocalDateTime fromDatabase(String value) {
            return value == null ? null : wallClock(TemporalConverter.parse(value));
        }
    }

    /// Milliseconds of a wall clock, refusing what a long cannot hold: a
    /// LocalDateTime reaches the year 999999999, and `LocalDateTime.MAX` as a
    /// "no bound" value would otherwise wrap to a plausible wrong time. An
    /// Instant needs no such check; its toEpochMilli throws by itself.
    static long millis(LocalDateTime value) {
        long days = value.toLocalDate().toEpochDay();
        if (days > Long.MAX_VALUE / DAY - 1 || days < Long.MIN_VALUE / DAY + 1) {
            throw new IllegalArgumentException("A LocalDateTime does not fit in epoch milliseconds: " + value);
        }
        return days * DAY + value.toLocalTime().toNanoOfDay() / 1000000L;
    }

    static LocalDateTime wallClock(long millis) {
        long days = floorDiv(millis, DAY);
        return LocalDateTime.of(LocalDate.ofEpochDay(days), LocalTime.ofNanoOfDay((millis - days * DAY) * 1000000L));
    }

    private static long floorDiv(long value, long divisor) {
        long quotient = value / divisor;
        return value % divisor != 0 && (value < 0) != (divisor < 0) ? quotient - 1 : quotient;
    }
}
