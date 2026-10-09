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

import java.lang.ref.WeakReference;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Locale;

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

    // ---- DateTimeFormatter with an override zone ----
    //
    // The device's formatter is final and holds a pattern and nothing else,
    // so the zone of `withZone` is kept beside it: every formatter made from
    // a pattern is remembered with that pattern, and a zoned one is a second
    // formatter of the same pattern, remembered with its zone. Both tables
    // hold their formatter weakly.

    private static final class Known {
        final WeakReference<DateTimeFormatter> formatter;
        final String pattern;
        final Locale locale;
        /// The formatter this one was made from by `withZone`, or null.
        final DateTimeFormatter base;
        final ZoneId zone;

        Known(DateTimeFormatter formatter, String pattern, Locale locale, DateTimeFormatter base, ZoneId zone) {
            this.formatter = new WeakReference<DateTimeFormatter>(formatter);
            this.pattern = pattern;
            this.locale = locale;
            this.base = base;
            this.zone = zone;
        }
    }

    private static final ArrayList<Known> KNOWN = new ArrayList<Known>();
    private static final String ISO_DATE_PATTERN = "yyyy-MM-dd";
    private static final String ISO_TIME_PATTERN = "HH:mm:ss";
    private static final String ISO_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private static Known known(DateTimeFormatter formatter) {
        for (int i = KNOWN.size() - 1; i >= 0; i--) {
            Known k = KNOWN.get(i);
            DateTimeFormatter f = k.formatter.get();
            if (f == null) {
                KNOWN.remove(i);
            } else if (f == formatter) {
                return k;
            }
        }
        return null;
    }

    private static DateTimeFormatter remember(DateTimeFormatter formatter, String pattern, Locale locale,
            DateTimeFormatter base, ZoneId zone) {
        // The scan drops the entries whose formatter is gone.
        known(null);
        KNOWN.add(new Known(formatter, pattern, locale, base, zone));
        return formatter;
    }

    /// `DateTimeFormatter.ofPattern(pattern)`, remembered with its pattern.
    public static DateTimeFormatter ofPattern(String pattern) {
        return remember(DateTimeFormatter.ofPattern(pattern), pattern, null, null, null);
    }

    /// `DateTimeFormatter.ofPattern(pattern, locale)`, remembered with both.
    public static DateTimeFormatter ofPattern(String pattern, Locale locale) {
        return remember(DateTimeFormatter.ofPattern(pattern, locale), pattern, locale, null, null);
    }

    private static String isoPattern(DateTimeFormatter formatter) {
        if (formatter == DateTimeFormatter.ISO_LOCAL_DATE) {
            return ISO_DATE_PATTERN;
        }
        if (formatter == DateTimeFormatter.ISO_LOCAL_TIME) {
            return ISO_TIME_PATTERN;
        }
        return ISO_DATE_TIME_PATTERN;
    }

    /// `formatter.withZone(zone)`: a formatter that formats an instant, or a
    /// date and time that has a zone or an offset, in `zone`. A null zone
    /// answers a formatter with no override, as the JDK does.
    ///
    /// What comes back formats a local date or time exactly as `formatter`
    /// does. Made from one of the `ISO_` constants it is a formatter of the
    /// constant's pattern without the fraction of a second.
    public static DateTimeFormatter withZone(DateTimeFormatter formatter, ZoneId zone) {
        Known k = known(formatter);
        DateTimeFormatter base = k != null && k.base != null ? k.base : formatter;
        Known origin = k != null && k.base != null ? known(k.base) : k;
        String pattern = origin != null ? origin.pattern : isoPattern(base);
        Locale locale = origin != null ? origin.locale : null;
        if (zone == null) {
            return base;
        }
        if (k != null && zone.equals(k.zone)) {
            return formatter;
        }
        return remember(make(pattern, locale), pattern, locale, base, zone);
    }

    private static DateTimeFormatter make(String pattern, Locale locale) {
        return locale == null ? DateTimeFormatter.ofPattern(pattern) : DateTimeFormatter.ofPattern(pattern, locale);
    }

    /// `formatter.getZone()`: the zone given to [#withZone], or null.
    public static ZoneId getZone(DateTimeFormatter formatter) {
        Known k = known(formatter);
        return k == null ? null : k.zone;
    }

    /// `formatter.format(temporal)`, honouring the zone of [#withZone]: an
    /// instant and a zoned or offset date and time are shown in that zone,
    /// a local date and time as that date and time in the zone, and a date
    /// or a time alone as it is.
    public static String format(DateTimeFormatter formatter, TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        Known k = known(formatter);
        if (k == null || k.zone == null) {
            return formatter.format(temporal);
        }
        if (temporal instanceof Instant) {
            return formatter.format(ZonedDateTime.ofInstant((Instant) temporal, k.zone));
        }
        if (temporal instanceof ZonedDateTime) {
            return formatter.format(ZonedDateTime.ofInstant(((ZonedDateTime) temporal).toInstant(), k.zone));
        }
        if (temporal instanceof OffsetDateTime) {
            return formatter.format(ZonedDateTime.ofInstant(((OffsetDateTime) temporal).toInstant(), k.zone));
        }
        if (temporal instanceof LocalDateTime) {
            return formatter.format(ZonedDateTime.of((LocalDateTime) temporal, k.zone));
        }
        return formatter.format(temporal);
    }

    /// `zoned.format(formatter)`, honouring the zone of [#withZone].
    public static String format(ZonedDateTime zoned, DateTimeFormatter formatter) {
        return format(formatter, zoned);
    }

    /// `offset.format(formatter)`, honouring the zone of [#withZone].
    public static String format(OffsetDateTime offset, DateTimeFormatter formatter) {
        return format(formatter, offset);
    }

    /// `ZonedDateTime.parse(text, formatter)`: with the zone of
    /// [#withZone], the date and time read are those of that zone.
    public static ZonedDateTime parseZoned(CharSequence text, DateTimeFormatter formatter) {
        Known k = known(formatter);
        if (k == null || k.zone == null) {
            return ZonedDateTime.parse(text, formatter);
        }
        return ZonedDateTime.of(localDateTimeFrom(formatter.parse(text)), k.zone);
    }
}
