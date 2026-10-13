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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

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
        String resolved = resolved(temporal);
        if (resolved != null && resolved.indexOf(':') > 0) {
            return LocalTime.parse(resolved.substring(resolved.indexOf('T') + 1));
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
        String resolved = resolved(temporal);
        if (resolved != null && resolved.indexOf('-', 1) > 0) {
            int t = resolved.indexOf('T');
            return LocalDate.parse(t < 0 ? resolved : resolved.substring(0, t));
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
        String resolved = resolved(temporal);
        if (resolved != null && resolved.indexOf('T') > 0) {
            return LocalDateTime.parse(resolved);
        }
        throw unobtainable("LocalDateTime", temporal);
    }

    /// What a formatter of the desktop's own class library parsed, as ISO
    /// text: a date, a time, or both around a `T`. `null` for anything
    /// else.
    ///
    /// The device's formatter answers a date or a time itself. Under the
    /// simulator and in a desktop build the formatter is the JDK's, which
    /// answers a holder of its own instead, and the interface it is seen
    /// through has no member on the device to ask it anything with. Its
    /// text is the one face both class libraries have, and it ends in
    /// `resolved to` followed by what was parsed.
    private static String resolved(TemporalAccessor temporal) {
        String text = String.valueOf(temporal);
        String mark = " resolved to ";
        int at = text.lastIndexOf(mark);
        if (at < 0) {
            return null;
        }
        String out = text.substring(at + mark.length()).trim();
        return out.length() == 0 ? null : out;
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

    /// Replaced whole, never changed in place: formatters are made and used
    /// on whatever thread an application formats a date on, and a reader
    /// must not meet a table another thread is half way through pruning.
    private static final AtomicReference<Known[]> KNOWN = new AtomicReference<Known[]>(new Known[0]);
    private static final String ISO_DATE_PATTERN = "yyyy-MM-dd";
    private static final String ISO_TIME_PATTERN = "HH:mm:ss";
    private static final String ISO_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private static Known known(DateTimeFormatter formatter) {
        Known[] all = KNOWN.get();
        for (int i = all.length - 1; i >= 0; i--) {
            if (all[i].formatter.get() == formatter) {
                return all[i];
            }
        }
        return null;
    }

    private static DateTimeFormatter remember(DateTimeFormatter formatter, String pattern, Locale locale,
            DateTimeFormatter base, ZoneId zone) {
        Known added = new Known(formatter, pattern, locale, base, zone);
        for (;;) {
            Known[] all = KNOWN.get();
            // The copy drops the entries whose formatter is gone.
            ArrayList<Known> kept = new ArrayList<Known>(all.length + 1);
            for (Known k : all) {
                if (k.formatter.get() != null) {
                    kept.add(k);
                }
            }
            kept.add(added);
            if (KNOWN.compareAndSet(all, kept.toArray(new Known[kept.size()]))) {
                return formatter;
            }
        }
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
    /// does. Made from one of the `ISO_` constants it is a handle that
    /// [#format] reads the constant and the zone from, so the offset, the
    /// `Z` and the fraction of a second are the constant's own.
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
        if (k != null && temporal instanceof LocalDate) {
            // The device's formatter of a pattern formats an instant or a
            // date WITH a time and throws for anything else, so a date
            // alone goes to it at midnight and a time alone on the first
            // day of the epoch. The fields that were made up are the ones
            // the pattern may not name, as on the JDK.
            requireFields(k.pattern, DATE_FIELDS, temporal);
            return formatter.format(LocalDateTime.of((LocalDate) temporal, LocalTime.MIDNIGHT));
        }
        if (k != null && temporal instanceof LocalTime) {
            requireFields(k.pattern, TIME_FIELDS, temporal);
            return formatter.format(LocalDateTime.of(LocalDate.ofEpochDay(0), (LocalTime) temporal));
        }
        if (k == null || k.zone == null) {
            return formatter.format(temporal);
        }
        if (k.base != null && known(k.base) == null) {
            // Made from an ISO_ constant, which no pattern stands for.
            String iso = formatIso(k.base, k.zone, temporal);
            if (iso != null) {
                return iso;
            }
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

    /// What the `ISO_` constant `iso` prints for `temporal` seen in `zone`,
    /// or null when `iso` is not one of the constants or `temporal` is not
    /// a point in time. Each constant formats one class and no other, so
    /// the value is turned into that class first.
    private static String formatIso(DateTimeFormatter iso, ZoneId zone, TemporalAccessor temporal) {
        Instant at;
        LocalDateTime local;
        if (temporal instanceof Instant) {
            at = (Instant) temporal;
            local = ZonedDateTime.ofInstant(at, zone).toLocalDateTime();
        } else if (temporal instanceof ZonedDateTime) {
            at = ((ZonedDateTime) temporal).toInstant();
            local = ZonedDateTime.ofInstant(at, zone).toLocalDateTime();
        } else if (temporal instanceof OffsetDateTime) {
            at = ((OffsetDateTime) temporal).toInstant();
            local = ZonedDateTime.ofInstant(at, zone).toLocalDateTime();
        } else if (temporal instanceof LocalDateTime) {
            local = (LocalDateTime) temporal;
            at = ZonedDateTime.of(local, zone).toInstant();
        } else {
            return null;
        }
        if (iso == DateTimeFormatter.ISO_INSTANT) {
            return iso.format(at);
        }
        if (iso == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
            return iso.format(OffsetDateTime.ofInstant(at, zone));
        }
        if (iso == DateTimeFormatter.ISO_ZONED_DATE_TIME) {
            return iso.format(ZonedDateTime.ofInstant(at, zone));
        }
        if (iso == DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
            return iso.format(local);
        }
        if (iso == DateTimeFormatter.ISO_LOCAL_DATE) {
            return iso.format(local.toLocalDate());
        }
        if (iso == DateTimeFormatter.ISO_LOCAL_TIME) {
            return iso.format(local.toLocalTime());
        }
        return null;
    }

    /// The pattern letters a date alone can answer, and those a time alone
    /// can: the JDK's, less the ones it cannot print for a local value.
    private static final String DATE_FIELDS = "GuyDMLdQqYwWEecFg";
    private static final String TIME_FIELDS = "ahKkHmsSAnNB";

    /// Throws what the JDK throws when `pattern` names a field `temporal`
    /// does not have: a letter outside quotes that is not one of
    /// `allowed`.
    private static void requireFields(String pattern, String allowed, TemporalAccessor temporal) {
        boolean quoted = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
            } else if (!quoted && ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) && allowed.indexOf(c) < 0) {
                throw new DateTimeException("Unsupported field: " + c + " of pattern " + pattern + " for "
                        + temporal);
            }
        }
    }

    /// `date.format(formatter)`.
    public static String format(LocalDate date, DateTimeFormatter formatter) {
        return format(formatter, date);
    }

    /// `time.format(formatter)`.
    public static String format(LocalTime time, DateTimeFormatter formatter) {
        return format(formatter, time);
    }

    /// `dateTime.format(formatter)`, honouring the zone of [#withZone].
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return format(formatter, dateTime);
    }

    /// `LocalDate.parse(text, formatter)`. The device's formatter of a
    /// pattern parses into a date with a time, whatever was asked for.
    public static LocalDate parseLocalDate(CharSequence text, DateTimeFormatter formatter) {
        if (text == null || formatter == null) {
            throw new NullPointerException();
        }
        return localDateFrom(formatter.parse(text));
    }

    /// `LocalTime.parse(text, formatter)`; see [#parseLocalDate].
    public static LocalTime parseLocalTime(CharSequence text, DateTimeFormatter formatter) {
        if (text == null || formatter == null) {
            throw new NullPointerException();
        }
        return localTimeFrom(formatter.parse(text));
    }

    /// `zoned.format(formatter)`, honouring the zone of [#withZone].
    public static String format(ZonedDateTime zoned, DateTimeFormatter formatter) {
        return format(formatter, zoned);
    }

    /// `offset.format(formatter)`, honouring the zone of [#withZone].
    public static String format(OffsetDateTime offset, DateTimeFormatter formatter) {
        return format(formatter, offset);
    }

    /// `ZonedDateTime.parse(text, formatter)` with the zone of [#withZone].
    ///
    /// Text that names no offset is a date and time of that zone. Text that
    /// does name one is a moment, and the override zone is only where that
    /// moment is then shown: `2020-01-01T12:00:00+02:00` read by
    /// `ISO_OFFSET_DATE_TIME.withZone(UTC)` is `10:00Z`, not noon. (Java 8
    /// answered noon; that was a defect of its parser and every later
    /// release answers the moment.)
    public static ZonedDateTime parseZoned(CharSequence text, DateTimeFormatter formatter) {
        Known k = known(formatter);
        if (k == null || k.zone == null) {
            return ZonedDateTime.parse(text, formatter);
        }
        if (k.base != null && known(k.base) == null) {
            // Made from an ISO_ constant, whose own parser reads the offset,
            // the zone and the fraction of a second; the pattern standing
            // in for it here reads none of them.
            ZonedDateTime iso = parseIso(text, k.base, k.zone);
            if (iso != null) {
                return iso;
            }
        }
        TemporalAccessor parsed = formatter.parse(text);
        if (parsed instanceof LocalDateTime) {
            // The device's formatter: it has already applied an offset it
            // read, and answers the moment as a date and time at UTC.
            LocalDateTime local = (LocalDateTime) parsed;
            if (readsOffset(k.pattern)) {
                return ZonedDateTime.ofInstant(local.toInstant(ZoneOffset.UTC), k.zone);
            }
            return ZonedDateTime.of(local, k.zone);
        }
        LocalDateTime local = localDateTimeFrom(parsed);
        ZoneOffset offset = offsetOf(parsed);
        if (offset != null) {
            return ZonedDateTime.ofInstant(local.toInstant(offset), k.zone);
        }
        return ZonedDateTime.of(local, k.zone);
    }

    private static ZonedDateTime parseIso(CharSequence text, DateTimeFormatter iso, ZoneId zone) {
        if (iso == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
            return ZonedDateTime.ofInstant(OffsetDateTime.parse(text, iso).toInstant(), zone);
        }
        if (iso == DateTimeFormatter.ISO_ZONED_DATE_TIME) {
            ZonedDateTime read = ZonedDateTime.parse(text, iso);
            // A region the text names is kept; a bare offset is not a zone
            // of its own, so the override takes its place.
            return read.getZone() instanceof ZoneOffset ? ZonedDateTime.ofInstant(read.toInstant(), zone) : read;
        }
        if (iso == DateTimeFormatter.ISO_INSTANT) {
            return ZonedDateTime.ofInstant(Instant.parse(text), zone);
        }
        if (iso == DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
            return ZonedDateTime.of(LocalDateTime.parse(text, iso), zone);
        }
        return null;
    }

    /// Whether `pattern` has an offset letter (`X`, `x`, `Z` or `O`) outside
    /// its quoted text.
    private static boolean readsOffset(String pattern) {
        if (pattern == null) {
            return false;
        }
        boolean quoted = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
            } else if (!quoted && (c == 'X' || c == 'x' || c == 'Z' || c == 'O')) {
                return true;
            }
        }
        return false;
    }

    /// The offset a formatter of the desktop's own class library read, or
    /// null when it read none. As with [#resolved], its text is the one
    /// face of what it parsed that both class libraries have.
    private static ZoneOffset offsetOf(TemporalAccessor parsed) {
        String text = String.valueOf(parsed);
        String mark = "OffsetSeconds=";
        int at = text.indexOf(mark);
        if (at < 0) {
            return null;
        }
        int from = at + mark.length();
        int to = from;
        while (to < text.length() && (text.charAt(to) == '-' || (text.charAt(to) >= '0' && text.charAt(to) <= '9'))) {
            to++;
        }
        if (to == from) {
            return null;
        }
        int seconds;
        try {
            seconds = Integer.parseInt(text.substring(from, to));
        } catch (NumberFormatException e) {
            return null;
        }
        return ZoneOffset.ofTotalSeconds(seconds);
    }
}
