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

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;

/// A date and time with a fixed offset from UTC, such as
/// 2007-12-03T10:15:30+01:00.
public final class OffsetDateTime implements Temporal, Comparable<OffsetDateTime> {
    /// The earliest date and time, at the largest offset.
    public static final OffsetDateTime MIN = new OffsetDateTime(LocalDateTime.MIN, ZoneOffset.MAX);

    /// The latest date and time, at the smallest offset.
    public static final OffsetDateTime MAX = new OffsetDateTime(LocalDateTime.MAX, ZoneOffset.MIN);

    private final LocalDateTime dateTime;
    private final ZoneOffset offset;

    private OffsetDateTime(LocalDateTime dateTime, ZoneOffset offset) {
        if (dateTime == null) {
            throw new NullPointerException("dateTime");
        }
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        this.dateTime = dateTime;
        this.offset = offset;
    }

    public static OffsetDateTime now() {
        return now(Clock.systemDefaultZone());
    }

    public static OffsetDateTime now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static OffsetDateTime now(Clock clock) {
        return ofInstant(clock.instant(), clock.getZone());
    }

    public static OffsetDateTime of(LocalDate date, LocalTime time, ZoneOffset offset) {
        return new OffsetDateTime(LocalDateTime.of(date, time), offset);
    }

    public static OffsetDateTime of(LocalDateTime dateTime, ZoneOffset offset) {
        return new OffsetDateTime(dateTime, offset);
    }

    public static OffsetDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second,
            int nanoOfSecond, ZoneOffset offset) {
        return new OffsetDateTime(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond),
                offset);
    }

    public static OffsetDateTime ofInstant(Instant instant, ZoneId zone) {
        if (instant == null) {
            throw new NullPointerException("instant");
        }
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        ZoneOffset offset = DateTimeSupport.offsetFromInstant(instant, zone);
        return new OffsetDateTime(LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset),
                offset);
    }

    /// The date, time and offset of `temporal`.
    public static OffsetDateTime from(TemporalAccessor temporal) {
        if (temporal instanceof OffsetDateTime) {
            return (OffsetDateTime) temporal;
        }
        if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toOffsetDateTime();
        }
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        throw new DateTimeException("Unable to obtain OffsetDateTime from TemporalAccessor: " + temporal
                + " of type " + temporal.getClass().getName());
    }

    public static OffsetDateTime parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static OffsetDateTime parse(CharSequence text, DateTimeFormatter formatter) {
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

    public LocalDateTime toLocalDateTime() {
        return dateTime;
    }

    public LocalDate toLocalDate() {
        return dateTime.toLocalDate();
    }

    public LocalTime toLocalTime() {
        return dateTime.toLocalTime();
    }

    public ZoneOffset getOffset() {
        return offset;
    }

    public Instant toInstant() {
        return dateTime.toInstant(offset);
    }

    public long toEpochSecond() {
        return dateTime.toEpochSecond(offset);
    }

    /// This date and time, as a moment in `offset`'s own zone.
    public ZonedDateTime toZonedDateTime() {
        return ZonedDateTime.of(dateTime, offset);
    }

    /// The same moment in `zone`.
    public ZonedDateTime atZoneSameInstant(ZoneId zone) {
        return ZonedDateTime.ofInstant(dateTime, offset, zone);
    }

    /// The same moment at another offset.
    public OffsetDateTime withOffsetSameInstant(ZoneOffset offset) {
        if (offset.equals(this.offset)) {
            return this;
        }
        int difference = offset.getTotalSeconds() - this.offset.getTotalSeconds();
        return new OffsetDateTime(dateTime.plusSeconds(difference), offset);
    }

    /// The same wall clock time at another offset.
    public OffsetDateTime withOffsetSameLocal(ZoneOffset offset) {
        return offset.equals(this.offset) ? this : new OffsetDateTime(dateTime, offset);
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

    private OffsetDateTime with(LocalDateTime newDateTime) {
        return newDateTime == dateTime ? this : new OffsetDateTime(newDateTime, offset);
    }

    public OffsetDateTime withYear(int year) {
        return with(dateTime.withYear(year));
    }

    public OffsetDateTime withMonth(int month) {
        return with(dateTime.withMonth(month));
    }

    public OffsetDateTime withDayOfMonth(int dayOfMonth) {
        return with(dateTime.withDayOfMonth(dayOfMonth));
    }

    public OffsetDateTime withDayOfYear(int dayOfYear) {
        return with(dateTime.withDayOfYear(dayOfYear));
    }

    public OffsetDateTime withHour(int hour) {
        return with(dateTime.withHour(hour));
    }

    public OffsetDateTime withMinute(int minute) {
        return with(dateTime.withMinute(minute));
    }

    public OffsetDateTime withSecond(int second) {
        return with(dateTime.withSecond(second));
    }

    public OffsetDateTime withNano(int nanoOfSecond) {
        return with(dateTime.withNano(nanoOfSecond));
    }

    public OffsetDateTime truncatedTo(TemporalUnit unit) {
        return with(dateTime.truncatedTo(unit));
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public OffsetDateTime plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return with(dateTime.plus(amountToAdd, unit));
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.addTo(this, amountToAdd);
    }

    @Override
    public OffsetDateTime minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit);
    }

    @Override
    public OffsetDateTime plus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amountToAdd");
        }
        return (OffsetDateTime) amount.addTo(this);
    }

    @Override
    public OffsetDateTime minus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amountToSubtract");
        }
        return (OffsetDateTime) amount.subtractFrom(this);
    }

    public OffsetDateTime plusYears(long years) {
        return with(dateTime.plusYears(years));
    }

    public OffsetDateTime plusMonths(long months) {
        return with(dateTime.plusMonths(months));
    }

    public OffsetDateTime plusWeeks(long weeks) {
        return with(dateTime.plusWeeks(weeks));
    }

    public OffsetDateTime plusDays(long days) {
        return with(dateTime.plusDays(days));
    }

    public OffsetDateTime plusHours(long hours) {
        return with(dateTime.plusHours(hours));
    }

    public OffsetDateTime plusMinutes(long minutes) {
        return with(dateTime.plusMinutes(minutes));
    }

    public OffsetDateTime plusSeconds(long seconds) {
        return with(dateTime.plusSeconds(seconds));
    }

    public OffsetDateTime plusNanos(long nanos) {
        return with(dateTime.plusNanos(nanos));
    }

    public OffsetDateTime minusYears(long years) {
        return with(dateTime.minusYears(years));
    }

    public OffsetDateTime minusMonths(long months) {
        return with(dateTime.minusMonths(months));
    }

    public OffsetDateTime minusWeeks(long weeks) {
        return with(dateTime.minusWeeks(weeks));
    }

    public OffsetDateTime minusDays(long days) {
        return with(dateTime.minusDays(days));
    }

    public OffsetDateTime minusHours(long hours) {
        return with(dateTime.minusHours(hours));
    }

    public OffsetDateTime minusMinutes(long minutes) {
        return with(dateTime.minusMinutes(minutes));
    }

    public OffsetDateTime minusSeconds(long seconds) {
        return with(dateTime.minusSeconds(seconds));
    }

    public OffsetDateTime minusNanos(long nanos) {
        return with(dateTime.minusNanos(nanos));
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        OffsetDateTime end = from(endExclusive);
        if (unit instanceof ChronoUnit) {
            end = end.withOffsetSameInstant(offset);
            return dateTime.until(end.dateTime, unit);
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.between(this, end);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public boolean isAfter(OffsetDateTime other) {
        long a = toEpochSecond();
        long b = other.toEpochSecond();
        return a > b || (a == b && getNano() > other.getNano());
    }

    public boolean isBefore(OffsetDateTime other) {
        long a = toEpochSecond();
        long b = other.toEpochSecond();
        return a < b || (a == b && getNano() < other.getNano());
    }

    public boolean isEqual(OffsetDateTime other) {
        return toEpochSecond() == other.toEpochSecond() && getNano() == other.getNano();
    }

    /// Orders by moment, and moments that are equal by wall clock time.
    public int compareTo(OffsetDateTime other) {
        if (offset.equals(other.offset)) {
            return dateTime.compareTo(other.dateTime);
        }
        long a = toEpochSecond();
        long b = other.toEpochSecond();
        int cmp = a < b ? -1 : (a > b ? 1 : 0);
        if (cmp == 0) {
            cmp = getNano() - other.getNano();
            if (cmp == 0) {
                cmp = dateTime.compareTo(other.dateTime);
            }
        }
        return cmp;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OffsetDateTime)) {
            return false;
        }
        OffsetDateTime other = (OffsetDateTime) obj;
        return dateTime.equals(other.dateTime) && offset.equals(other.offset);
    }

    public int hashCode() {
        return dateTime.hashCode() ^ offset.hashCode();
    }

    public String toString() {
        return dateTime.toString() + offset.toString();
    }
}
