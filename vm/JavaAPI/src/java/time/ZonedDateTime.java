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
package java.time;

import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;

/// A date and a time of day in a time zone, such as
/// 2007-12-03T10:15:30+01:00[Europe/Paris].
///
/// Adding a date-based amount moves the wall clock and keeps the offset where
/// the zone allows it; adding a time-based amount moves the instant. Across a
/// change of offset the two differ, as they do in the JDK.
public final class ZonedDateTime implements ChronoZonedDateTime<LocalDate> {
    private final LocalDateTime dateTime;
    private final ZoneId zone;
    private final ZoneOffset offset;

    private ZonedDateTime(LocalDateTime dateTime, ZoneOffset offset, ZoneId zone) {
        this.dateTime = dateTime;
        this.offset = offset;
        this.zone = zone;
    }

    public static ZonedDateTime now() {
        return now(Clock.systemDefaultZone());
    }

    /// The date and time now in `zone`.
    public static ZonedDateTime now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static ZonedDateTime now(Clock clock) {
        return ofInstant(clock.instant(), clock.getZone());
    }

    public static ZonedDateTime of(LocalDate date, LocalTime time, ZoneId zone) {
        return of(LocalDateTime.of(date, time), zone);
    }

    public static ZonedDateTime of(LocalDateTime localDateTime, ZoneId zone) {
        return ofLocal(localDateTime, zone, null);
    }

    public static ZonedDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second,
            int nanoOfSecond, ZoneId zone) {
        return ofLocal(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond), zone, null);
    }

    /// `localDateTime` in `zone`. A wall clock time the zone skips is moved
    /// forward by the length of the gap; one it passes twice takes
    /// `preferredOffset` when that is one of its two offsets, and the earlier
    /// offset otherwise.
    public static ZonedDateTime ofLocal(LocalDateTime localDateTime, ZoneId zone, ZoneOffset preferredOffset) {
        if (localDateTime == null) {
            throw new NullPointerException("localDateTime");
        }
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        if (zone instanceof ZoneOffset) {
            return new ZonedDateTime(localDateTime, (ZoneOffset) zone, zone);
        }
        long local = localDateTime.toEpochSecond(ZoneOffset.UTC);
        // The offsets a day either side; a zone changes its offset far less
        // often than that, so these are the offset before any transition
        // touching this wall clock time and the offset after it.
        ZoneOffset before = offsetAt(local - 86400L, zone);
        ZoneOffset after = offsetAt(local + 86400L, zone);
        boolean beforeValid = offsetAt(local - before.getTotalSeconds(), zone).equals(before);
        boolean afterValid = offsetAt(local - after.getTotalSeconds(), zone).equals(after);
        if (before.equals(after) || (beforeValid && !afterValid)) {
            return new ZonedDateTime(localDateTime, before, zone);
        }
        if (afterValid && !beforeValid) {
            return new ZonedDateTime(localDateTime, after, zone);
        }
        if (beforeValid) {
            // An overlap: the wall clock time happens at both offsets.
            ZoneOffset chosen = after.equals(preferredOffset) ? after : before;
            return new ZonedDateTime(localDateTime, chosen, zone);
        }
        // A gap: the wall clock time never happens; move past it.
        LocalDateTime shifted = localDateTime.plusSeconds(after.getTotalSeconds() - before.getTotalSeconds());
        return new ZonedDateTime(shifted, after, zone);
    }

    /// The two offsets of the overlap this wall clock time falls in, earlier
    /// first, or null when it falls in none.
    private ZoneOffset[] overlap() {
        if (zone instanceof ZoneOffset) {
            return null;
        }
        long local = dateTime.toEpochSecond(ZoneOffset.UTC);
        ZoneOffset before = offsetAt(local - 86400L, zone);
        ZoneOffset after = offsetAt(local + 86400L, zone);
        if (before.equals(after) || !offsetAt(local - before.getTotalSeconds(), zone).equals(before)
                || !offsetAt(local - after.getTotalSeconds(), zone).equals(after)) {
            return null;
        }
        return new ZoneOffset[] {before, after};
    }

    /// At a wall clock time the zone passes twice, the first of the two
    /// moments; otherwise this.
    public ZonedDateTime withEarlierOffsetAtOverlap() {
        ZoneOffset[] both = overlap();
        return both == null || both[0].equals(offset) ? this : new ZonedDateTime(dateTime, both[0], zone);
    }

    /// At a wall clock time the zone passes twice, the second of the two
    /// moments; otherwise this.
    public ZonedDateTime withLaterOffsetAtOverlap() {
        ZoneOffset[] both = overlap();
        return both == null || both[1].equals(offset) ? this : new ZonedDateTime(dateTime, both[1], zone);
    }

    private static ZoneOffset offsetAt(long epochSecond, ZoneId zone) {
        return DateTimeSupport.offsetFromInstant(Instant.ofEpochSecond(epochSecond), zone);
    }

    public static ZonedDateTime ofInstant(Instant instant, ZoneId zone) {
        if (instant == null) {
            throw new NullPointerException("instant");
        }
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return create(instant.getEpochSecond(), instant.getNano(), zone);
    }

    /// The moment `localDateTime` is at `offset`, in `zone`.
    public static ZonedDateTime ofInstant(LocalDateTime localDateTime, ZoneOffset offset, ZoneId zone) {
        return create(localDateTime.toEpochSecond(offset), localDateTime.getNano(), zone);
    }

    private static ZonedDateTime create(long epochSecond, int nanoOfSecond, ZoneId zone) {
        Instant instant = Instant.ofEpochSecond(epochSecond, nanoOfSecond);
        ZoneOffset offset = DateTimeSupport.offsetFromInstant(instant, zone);
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(epochSecond, nanoOfSecond, offset);
        return new ZonedDateTime(ldt, offset, zone);
    }

    /// The date and time and zone of `temporal`.
    public static ZonedDateTime from(TemporalAccessor temporal) {
        if (temporal instanceof ZonedDateTime) {
            return (ZonedDateTime) temporal;
        }
        if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).toZonedDateTime();
        }
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        throw new DateTimeException("Unable to obtain ZonedDateTime from TemporalAccessor: " + temporal
                + " of type " + temporal.getClass().getName());
    }

    public static ZonedDateTime parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    public static ZonedDateTime parse(CharSequence text, DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        TemporalAccessor parsed = formatter.parse(text);
        try {
            return from(parsed);
        } catch (DateTimeException ex) {
            throw TimeArithmetic.unparsable(text, ex);
        }
    }

    @Override
    public LocalDateTime toLocalDateTime() {
        return dateTime;
    }

    @Override
    public LocalDate toLocalDate() {
        return dateTime.toLocalDate();
    }

    @Override
    public LocalTime toLocalTime() {
        return dateTime.toLocalTime();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public ZoneOffset getOffset() {
        return offset;
    }

    @Override
    public Instant toInstant() {
        return Instant.ofEpochSecond(toEpochSecond(), dateTime.getNano());
    }

    @Override
    public long toEpochSecond() {
        return dateTime.toEpochSecond(offset);
    }

    /// This date and time with its offset and no zone.
    public OffsetDateTime toOffsetDateTime() {
        return OffsetDateTime.of(dateTime, offset);
    }

    public int getYear() {
        return dateTime.getYear();
    }

    public int getMonthValue() {
        return dateTime.getMonthValue();
    }

    public Month getMonth() {
        return dateTime.getMonth();
    }

    public int getDayOfMonth() {
        return dateTime.getDayOfMonth();
    }

    public int getDayOfYear() {
        return dateTime.getDayOfYear();
    }

    public DayOfWeek getDayOfWeek() {
        return dateTime.getDayOfWeek();
    }

    public int getHour() {
        return dateTime.getHour();
    }

    public int getMinute() {
        return dateTime.getMinute();
    }

    public int getSecond() {
        return dateTime.getSecond();
    }

    public int getNano() {
        return dateTime.getNano();
    }

    /// The same moment in `zone`.
    public ZonedDateTime withZoneSameInstant(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return this.zone.equals(zone) ? this : create(dateTime.toEpochSecond(offset), dateTime.getNano(), zone);
    }

    /// The same wall clock time in `zone`, keeping the offset if it can.
    public ZonedDateTime withZoneSameLocal(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return this.zone.equals(zone) ? this : ofLocal(dateTime, zone, offset);
    }

    private ZonedDateTime resolveLocal(LocalDateTime newDateTime) {
        return ofLocal(newDateTime, zone, offset);
    }

    private ZonedDateTime resolveInstant(LocalDateTime newDateTime) {
        return ofInstant(newDateTime, offset, zone);
    }

    public ZonedDateTime withYear(int year) {
        return resolveLocal(dateTime.withYear(year));
    }

    public ZonedDateTime withMonth(int month) {
        return resolveLocal(dateTime.withMonth(month));
    }

    public ZonedDateTime withDayOfMonth(int dayOfMonth) {
        return resolveLocal(dateTime.withDayOfMonth(dayOfMonth));
    }

    public ZonedDateTime withDayOfYear(int dayOfYear) {
        return resolveLocal(dateTime.withDayOfYear(dayOfYear));
    }

    /// This date and time with its zone replaced by its current offset.
    public ZonedDateTime withFixedOffsetZone() {
        return zone.equals(offset) ? this : new ZonedDateTime(dateTime, offset, offset);
    }

    public ZonedDateTime withHour(int hour) {
        return resolveLocal(dateTime.withHour(hour));
    }

    public ZonedDateTime withMinute(int minute) {
        return resolveLocal(dateTime.withMinute(minute));
    }

    public ZonedDateTime withSecond(int second) {
        return resolveLocal(dateTime.withSecond(second));
    }

    public ZonedDateTime withNano(int nanoOfSecond) {
        return resolveLocal(dateTime.withNano(nanoOfSecond));
    }

    public ZonedDateTime truncatedTo(TemporalUnit unit) {
        return resolveLocal(dateTime.truncatedTo(unit));
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public ZonedDateTime plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            if (unit.isDateBased()) {
                return resolveLocal(dateTime.plus(amountToAdd, unit));
            }
            return resolveInstant(dateTime.plus(amountToAdd, unit));
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.addTo(this, amountToAdd);
    }

    @Override
    public ZonedDateTime minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit);
    }

    @Override
    public ZonedDateTime plus(TemporalAmount amount) {
        if (amount instanceof Period) {
            return resolveLocal(dateTime.plus(amount));
        }
        if (amount == null) {
            throw new NullPointerException("amountToAdd");
        }
        return (ZonedDateTime) amount.addTo(this);
    }

    @Override
    public ZonedDateTime minus(TemporalAmount amount) {
        if (amount instanceof Period) {
            return resolveLocal(dateTime.minus(amount));
        }
        if (amount == null) {
            throw new NullPointerException("amountToSubtract");
        }
        return (ZonedDateTime) amount.subtractFrom(this);
    }

    public ZonedDateTime plusYears(long years) {
        return resolveLocal(dateTime.plusYears(years));
    }

    public ZonedDateTime plusMonths(long months) {
        return resolveLocal(dateTime.plusMonths(months));
    }

    public ZonedDateTime plusWeeks(long weeks) {
        return resolveLocal(dateTime.plusWeeks(weeks));
    }

    public ZonedDateTime plusDays(long days) {
        return resolveLocal(dateTime.plusDays(days));
    }

    public ZonedDateTime plusHours(long hours) {
        return resolveInstant(dateTime.plusHours(hours));
    }

    public ZonedDateTime plusMinutes(long minutes) {
        return resolveInstant(dateTime.plusMinutes(minutes));
    }

    public ZonedDateTime plusSeconds(long seconds) {
        return resolveInstant(dateTime.plusSeconds(seconds));
    }

    public ZonedDateTime plusNanos(long nanos) {
        return resolveInstant(dateTime.plusNanos(nanos));
    }

    public ZonedDateTime minusYears(long years) {
        return years == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-years);
    }

    public ZonedDateTime minusMonths(long months) {
        return months == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1) : plusMonths(-months);
    }

    public ZonedDateTime minusWeeks(long weeks) {
        return weeks == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1) : plusWeeks(-weeks);
    }

    public ZonedDateTime minusDays(long days) {
        return days == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-days);
    }

    public ZonedDateTime minusHours(long hours) {
        return hours == Long.MIN_VALUE ? plusHours(Long.MAX_VALUE).plusHours(1) : plusHours(-hours);
    }

    public ZonedDateTime minusMinutes(long minutes) {
        return minutes == Long.MIN_VALUE ? plusMinutes(Long.MAX_VALUE).plusMinutes(1) : plusMinutes(-minutes);
    }

    public ZonedDateTime minusSeconds(long seconds) {
        return seconds == Long.MIN_VALUE ? plusSeconds(Long.MAX_VALUE).plusSeconds(1) : plusSeconds(-seconds);
    }

    public ZonedDateTime minusNanos(long nanos) {
        return nanos == Long.MIN_VALUE ? plusNanos(Long.MAX_VALUE).plusNanos(1) : plusNanos(-nanos);
    }

    /// The whole units of `unit` from this value to `endExclusive`: date
    /// units on the wall clock of this zone, time units on the time line.
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        ZonedDateTime end = from(endExclusive);
        if (unit instanceof ChronoUnit) {
            if (unit.isDateBased()) {
                end = end.withZoneSameInstant(zone);
                return dateTime.until(end.dateTime, unit);
            }
            LocalDateTime endHere = LocalDateTime.ofEpochSecond(end.toEpochSecond(), end.getNano(), offset);
            return dateTime.until(endHere, unit);
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.between(this, end);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    @Override
    public int compareTo(ChronoZonedDateTime<?> other) {
        long a = toEpochSecond();
        long b = other.toEpochSecond();
        if (a != b) {
            return a < b ? -1 : 1;
        }
        int cmp = toLocalTime().getNano() - other.toLocalTime().getNano();
        if (cmp == 0) {
            cmp = toLocalDateTime().compareTo(other.toLocalDateTime());
            if (cmp == 0) {
                cmp = getZone().getId().compareTo(other.getZone().getId());
            }
        }
        return cmp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ZonedDateTime)) {
            return false;
        }
        ZonedDateTime other = (ZonedDateTime) obj;
        return dateTime.equals(other.dateTime) && offset.equals(other.offset) && zone.equals(other.zone);
    }

    @Override
    public int hashCode() {
        int z = zone.hashCode();
        return dateTime.hashCode() ^ offset.hashCode() ^ ((z << 3) | (z >>> 29));
    }

    /// The JDK's text: the date and time, the offset, and the zone in
    /// brackets when it is a region rather than the offset itself.
    @Override
    public String toString() {
        String str = dateTime.toString() + offset.toString();
        if (!offset.equals(zone)) {
            str += '[' + zone.toString() + ']';
        }
        return str;
    }
}
