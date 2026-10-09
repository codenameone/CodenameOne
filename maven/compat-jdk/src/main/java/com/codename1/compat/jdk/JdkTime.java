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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/// The members of the `java.time` classes a desktop application names and
/// the device's classes do not have.
///
/// The build's remap step redirects each such call here; an instance method
/// arrives with its receiver as the first argument.
///
/// The device has one calendar and no `java.time.chrono`, so where the JDK
/// takes a `ChronoLocalDate` these take the one interface the device's
/// `LocalDate` implements, `TemporalAccessor` -- the build passes the
/// argument as it is -- and accept a `LocalDate` and nothing else.
public final class JdkTime {

    private static final long NANOS_PER_DAY = 86400000000000L;

    private JdkTime() {
    }

    // ---- LocalTime ----

    public static LocalTime plusNanos(LocalTime time, long nanos) {
        long shift = nanos % NANOS_PER_DAY;
        if (shift == 0) {
            return time;
        }
        return LocalTime.ofNanoOfDay((time.toNanoOfDay() + shift + NANOS_PER_DAY) % NANOS_PER_DAY);
    }

    public static LocalTime minusNanos(LocalTime time, long nanos) {
        return plusNanos(time, -(nanos % NANOS_PER_DAY));
    }

    public static LocalTime minusSeconds(LocalTime time, long seconds) {
        return time.plusSeconds(-(seconds % 86400L));
    }

    public static LocalTime minusMinutes(LocalTime time, long minutes) {
        return time.plusMinutes(-(minutes % 1440L));
    }

    public static LocalTime minusHours(LocalTime time, long hours) {
        return time.plusHours(-(hours % 24L));
    }

    public static boolean isBefore(LocalTime time, LocalTime other) {
        return time.toNanoOfDay() < other.toNanoOfDay();
    }

    public static boolean isAfter(LocalTime time, LocalTime other) {
        return time.toNanoOfDay() > other.toNanoOfDay();
    }

    public static LocalTime localTimeFrom(TemporalAccessor temporal) {
        if (temporal instanceof LocalTime) {
            return (LocalTime) temporal;
        }
        if (temporal instanceof LocalDateTime) {
            return ((LocalDateTime) temporal).toLocalTime();
        }
        if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toLocalDateTime().toLocalTime();
        }
        if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).toLocalDateTime().toLocalTime();
        }
        throw unobtainable("LocalTime", temporal);
    }

    // ---- LocalDate ----

    private static long epochDay(TemporalAccessor date) {
        if (date instanceof LocalDate) {
            return ((LocalDate) date).toEpochDay();
        }
        if (date == null) {
            throw new NullPointerException();
        }
        throw new DateTimeException("Only java.time.LocalDate is supported as a date");
    }

    public static boolean isBefore(LocalDate date, TemporalAccessor other) {
        return date.toEpochDay() < epochDay(other);
    }

    public static boolean isAfter(LocalDate date, TemporalAccessor other) {
        return date.toEpochDay() > epochDay(other);
    }

    public static boolean isEqual(LocalDate date, TemporalAccessor other) {
        return date.toEpochDay() == epochDay(other);
    }

    /// The sign is the JDK's; the magnitude is 1, where the JDK answers a
    /// difference of years, months or days.
    public static int compareTo(LocalDate date, TemporalAccessor other) {
        return Long.compare(date.toEpochDay(), epochDay(other));
    }

    public static LocalDate minusMonths(LocalDate date, long months) {
        return date.plusMonths(-months);
    }

    public static LocalDate minusYears(LocalDate date, long years) {
        return date.plusYears(-years);
    }

    public static LocalDate plusWeeks(LocalDate date, long weeks) {
        return date.plusDays(weeks * 7L);
    }

    public static LocalDate minusWeeks(LocalDate date, long weeks) {
        return date.plusDays(-weeks * 7L);
    }

    public static LocalDate localDateFrom(TemporalAccessor temporal) {
        if (temporal instanceof LocalDate) {
            return (LocalDate) temporal;
        }
        if (temporal instanceof LocalDateTime) {
            return ((LocalDateTime) temporal).toLocalDate();
        }
        if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toLocalDateTime().toLocalDate();
        }
        if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).toLocalDateTime().toLocalDate();
        }
        throw unobtainable("LocalDate", temporal);
    }

    // ---- LocalDateTime ----

    public static LocalDateTime localDateTimeFrom(TemporalAccessor temporal) {
        if (temporal instanceof LocalDateTime) {
            return (LocalDateTime) temporal;
        }
        if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toLocalDateTime();
        }
        if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).toLocalDateTime();
        }
        throw unobtainable("LocalDateTime", temporal);
    }

    private static DateTimeException unobtainable(String type, TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        return new DateTimeException("Unable to obtain " + type + " from TemporalAccessor: " + temporal);
    }

    // ---- DateTimeFormatter ----

    /// `formatter.parse(text, LocalDate::from)`: parses, and hands what was
    /// parsed to `query`. The device's formatter parses into the date and
    /// time classes themselves, which is what the three `from` methods
    /// above read.
    public static <T> T parse(DateTimeFormatter formatter, CharSequence text, TemporalQuery<T> query) {
        if (text == null || query == null) {
            throw new NullPointerException();
        }
        return query.queryFrom(formatter.parse(text));
    }
}
