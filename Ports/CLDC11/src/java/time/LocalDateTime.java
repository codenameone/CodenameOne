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

import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;

/// A date and a time of day with no zone, such as 2007-12-03T10:15:30.
public final class LocalDateTime implements ChronoLocalDateTime<LocalDate> {
    /// The earliest date and time.
    public static final LocalDateTime MIN = new LocalDateTime(LocalDate.MIN, LocalTime.MIN);
    /// The latest date and time.
    public static final LocalDateTime MAX = new LocalDateTime(LocalDate.MAX, LocalTime.MAX);

    private static final long NANOS_PER_SECOND = TimeArithmetic.NANOS_PER_SECOND;
    private static final long NANOS_PER_MINUTE = 60L * NANOS_PER_SECOND;
    private static final long NANOS_PER_HOUR = 60L * NANOS_PER_MINUTE;
    private static final long NANOS_PER_DAY = TimeArithmetic.NANOS_PER_DAY;

    private final LocalDate date;
    private final LocalTime time;

    private LocalDateTime(LocalDate date, LocalTime time) {
        this.date = date;
        this.time = time;
    }

    private LocalDateTime with(LocalDate newDate, LocalTime newTime) {
        if (date == newDate && time == newTime) {
            return this;
        }
        return new LocalDateTime(newDate, newTime);
    }

    public static LocalDateTime now() {
        return now(Clock.systemDefaultZone());
    }

    /// The date and time now in `zone`.
    public static LocalDateTime now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static LocalDateTime now(Clock clock) {
        return ofInstant(clock.instant(), clock.getZone());
    }

    public static LocalDateTime of(LocalDate date, LocalTime time) {
        if (date == null) {
            throw new NullPointerException("date");
        }
        if (time == null) {
            throw new NullPointerException("time");
        }
        return new LocalDateTime(date, time);
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute));
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute, second));
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second, int nano) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute, second, nano));
    }

    public static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute));
    }

    public static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute, int second) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute, second));
    }

    public static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute, int second,
            int nano) {
        return of(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute, second, nano));
    }

    public static LocalDateTime ofInstant(Instant instant, ZoneId zone) {
        return DateTimeSupport.localDateTimeFromInstant(instant, zone);
    }

    /// The date and time `epochSecond` seconds and `nanoOfSecond` nanoseconds
    /// after 1970-01-01T00:00 at `offset`.
    public static LocalDateTime ofEpochSecond(long epochSecond, int nanoOfSecond, ZoneOffset offset) {
        TimeArithmetic.check("NanoOfSecond", 0, 999999999, nanoOfSecond);
        long localSecond = epochSecond + offset.getTotalSeconds();
        long localEpochDay = TimeArithmetic.floorDiv(localSecond, TimeArithmetic.SECONDS_PER_DAY);
        int secsOfDay = (int) TimeArithmetic.floorMod(localSecond, TimeArithmetic.SECONDS_PER_DAY);
        LocalDate date = LocalDate.ofEpochDay(localEpochDay);
        LocalTime time = LocalTime.ofNanoOfDay(secsOfDay * NANOS_PER_SECOND + nanoOfSecond);
        return new LocalDateTime(date, time);
    }

    /// The date and time of `temporal`.
    public static LocalDateTime from(TemporalAccessor temporal) {
        if (temporal instanceof LocalDateTime) {
            return (LocalDateTime) temporal;
        }
        if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toLocalDateTime();
        }
        if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).toLocalDateTime();
        }
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        throw new DateTimeException("Unable to obtain LocalDateTime from TemporalAccessor: " + temporal
                + " of type " + temporal.getClass().getName());
    }

    public static LocalDateTime parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static LocalDateTime parse(CharSequence text, DateTimeFormatter formatter) {
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
    public LocalDate toLocalDate() {
        return date;
    }

    @Override
    public LocalTime toLocalTime() {
        return time;
    }

    public int getYear() {
        return date.getYear();
    }

    public int getMonthValue() {
        return date.getMonthValue();
    }

    public Month getMonth() {
        return date.getMonth();
    }

    public int getDayOfMonth() {
        return date.getDayOfMonth();
    }

    public int getDayOfYear() {
        return date.getDayOfYear();
    }

    public DayOfWeek getDayOfWeek() {
        return date.getDayOfWeek();
    }

    public int getHour() {
        return time.getHour();
    }

    public int getMinute() {
        return time.getMinute();
    }

    public int getSecond() {
        return time.getSecond();
    }

    public int getNano() {
        return time.getNano();
    }

    public LocalDateTime withYear(int year) {
        return with(date.withYear(year), time);
    }

    public LocalDateTime withMonth(int month) {
        return with(date.withMonth(month), time);
    }

    public LocalDateTime withDayOfMonth(int dayOfMonth) {
        return with(date.withDayOfMonth(dayOfMonth), time);
    }

    public LocalDateTime withDayOfYear(int dayOfYear) {
        return with(date.withDayOfYear(dayOfYear), time);
    }

    public LocalDateTime withHour(int hour) {
        return with(date, time.withHour(hour));
    }

    public LocalDateTime withMinute(int minute) {
        return with(date, time.withMinute(minute));
    }

    public LocalDateTime withSecond(int second) {
        return with(date, time.withSecond(second));
    }

    public LocalDateTime withNano(int nanoOfSecond) {
        return with(date, time.withNano(nanoOfSecond));
    }

    /// This value with everything smaller than `unit` set to zero. The unit
    /// has to divide a day.
    public LocalDateTime truncatedTo(TemporalUnit unit) {
        return with(date, time.truncatedTo(unit));
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public LocalDateTime plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            if (u == ChronoUnit.NANOS) {
                return plusNanos(amountToAdd);
            }
            if (u == ChronoUnit.MICROS) {
                return plusDays(amountToAdd / 86400000000L).plusNanos((amountToAdd % 86400000000L) * 1000);
            }
            if (u == ChronoUnit.MILLIS) {
                return plusDays(amountToAdd / 86400000L).plusNanos((amountToAdd % 86400000L) * 1000000);
            }
            if (u == ChronoUnit.SECONDS) {
                return plusSeconds(amountToAdd);
            }
            if (u == ChronoUnit.MINUTES) {
                return plusMinutes(amountToAdd);
            }
            if (u == ChronoUnit.HOURS) {
                return plusHours(amountToAdd);
            }
            if (u == ChronoUnit.HALF_DAYS) {
                // The JDK's own arithmetic, 256 half days to a step.
                return plusDays(amountToAdd / 256).plusHours((amountToAdd % 256) * 12);
            }
            return with(date.plus(amountToAdd, unit), time);
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.addTo(this, amountToAdd);
    }

    @Override
    public LocalDateTime minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit);
    }

    @Override
    public LocalDateTime plus(TemporalAmount amount) {
        if (amount instanceof Period) {
            Period periodToAdd = (Period) amount;
            return with(date.plus(periodToAdd), time);
        }
        if (amount == null) {
            throw new NullPointerException("amountToAdd");
        }
        return (LocalDateTime) amount.addTo(this);
    }

    @Override
    public LocalDateTime minus(TemporalAmount amount) {
        if (amount instanceof Period) {
            Period periodToSubtract = (Period) amount;
            return with(date.minus(periodToSubtract), time);
        }
        if (amount == null) {
            throw new NullPointerException("amountToSubtract");
        }
        return (LocalDateTime) amount.subtractFrom(this);
    }

    public LocalDateTime plusYears(long years) {
        return with(date.plusYears(years), time);
    }

    public LocalDateTime plusMonths(long months) {
        return with(date.plusMonths(months), time);
    }

    public LocalDateTime plusWeeks(long weeks) {
        return with(date.plusWeeks(weeks), time);
    }

    public LocalDateTime plusDays(long days) {
        return with(date.plusDays(days), time);
    }

    public LocalDateTime plusHours(long hours) {
        return plusWithOverflow(date, hours, 0, 0, 0, 1);
    }

    public LocalDateTime plusMinutes(long minutes) {
        return plusWithOverflow(date, 0, minutes, 0, 0, 1);
    }

    public LocalDateTime plusSeconds(long seconds) {
        return plusWithOverflow(date, 0, 0, seconds, 0, 1);
    }

    public LocalDateTime plusNanos(long nanos) {
        return plusWithOverflow(date, 0, 0, 0, nanos, 1);
    }

    public LocalDateTime minusYears(long years) {
        return years == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-years);
    }

    public LocalDateTime minusMonths(long months) {
        return months == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1) : plusMonths(-months);
    }

    public LocalDateTime minusWeeks(long weeks) {
        return weeks == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1) : plusWeeks(-weeks);
    }

    public LocalDateTime minusDays(long days) {
        return days == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-days);
    }

    public LocalDateTime minusHours(long hours) {
        return plusWithOverflow(date, hours, 0, 0, 0, -1);
    }

    public LocalDateTime minusMinutes(long minutes) {
        return plusWithOverflow(date, 0, minutes, 0, 0, -1);
    }

    public LocalDateTime minusSeconds(long seconds) {
        return plusWithOverflow(date, 0, 0, seconds, 0, -1);
    }

    public LocalDateTime minusNanos(long nanos) {
        return plusWithOverflow(date, 0, 0, 0, nanos, -1);
    }

    /// The time added a unit at a time, each taken apart into whole days and
    /// the rest first, so no step overflows while the result fits.
    private LocalDateTime plusWithOverflow(LocalDate newDate, long hours, long minutes, long seconds, long nanos,
            int sign) {
        if ((hours | minutes | seconds | nanos) == 0) {
            return with(newDate, time);
        }
        long totDays = nanos / NANOS_PER_DAY + seconds / 86400L + minutes / 1440L + hours / 24L;
        totDays *= sign;
        long totNanos = nanos % NANOS_PER_DAY + (seconds % 86400L) * NANOS_PER_SECOND
                + (minutes % 1440L) * NANOS_PER_MINUTE + (hours % 24L) * NANOS_PER_HOUR;
        long curNoD = time.toNanoOfDay();
        totNanos = totNanos * sign + curNoD;
        totDays += TimeArithmetic.floorDiv(totNanos, NANOS_PER_DAY);
        long newNoD = TimeArithmetic.floorMod(totNanos, NANOS_PER_DAY);
        LocalTime newTime = newNoD == curNoD ? time : LocalTime.ofNanoOfDay(newNoD);
        return with(newDate.plusDays(totDays), newTime);
    }

    /// The whole units of `unit` from this value to `endExclusive`.
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        LocalDateTime end = from(endExclusive);
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            if (u.isTimeBased()) {
                long amount = end.date.toEpochDay() - date.toEpochDay();
                if (amount == 0) {
                    return time.until(end.time, unit);
                }
                long timePart = end.time.toNanoOfDay() - time.toNanoOfDay();
                if (amount > 0) {
                    amount--;
                    timePart += NANOS_PER_DAY;
                } else {
                    amount++;
                    timePart -= NANOS_PER_DAY;
                }
                if (u == ChronoUnit.NANOS) {
                    amount = TimeArithmetic.multiplyExact(amount, NANOS_PER_DAY);
                } else if (u == ChronoUnit.MICROS) {
                    amount = TimeArithmetic.multiplyExact(amount, 86400000000L);
                    timePart = timePart / 1000;
                } else if (u == ChronoUnit.MILLIS) {
                    amount = TimeArithmetic.multiplyExact(amount, 86400000L);
                    timePart = timePart / 1000000;
                } else if (u == ChronoUnit.SECONDS) {
                    amount = TimeArithmetic.multiplyExact(amount, 86400L);
                    timePart = timePart / NANOS_PER_SECOND;
                } else if (u == ChronoUnit.MINUTES) {
                    amount = TimeArithmetic.multiplyExact(amount, 1440L);
                    timePart = timePart / NANOS_PER_MINUTE;
                } else if (u == ChronoUnit.HOURS) {
                    amount = TimeArithmetic.multiplyExact(amount, 24L);
                    timePart = timePart / NANOS_PER_HOUR;
                } else {
                    amount = TimeArithmetic.multiplyExact(amount, 2L);
                    timePart = timePart / (NANOS_PER_HOUR * 12);
                }
                return TimeArithmetic.addExact(amount, timePart);
            }
            LocalDate endDate = end.date;
            if (endDate.isAfter(date) && end.time.isBefore(time)) {
                endDate = endDate.minusDays(1);
            } else if (endDate.isBefore(date) && end.time.isAfter(time)) {
                endDate = endDate.plusDays(1);
            }
            return date.until(endDate, unit);
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.between(this, end);
    }

    /// This date and time in `zone`.
    public ZonedDateTime atZone(ZoneId zone) {
        return ZonedDateTime.of(this, zone);
    }

    /// This date and time at `offset`.
    public OffsetDateTime atOffset(ZoneOffset offset) {
        return OffsetDateTime.of(this, offset);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    private int compareTo0(LocalDateTime other) {
        int cmp = date.compareTo(other.date);
        if (cmp == 0) {
            cmp = time.compareTo(other.time);
        }
        return cmp;
    }

    @Override
    public int compareTo(ChronoLocalDateTime<?> other) {
        if (other instanceof LocalDateTime) {
            return compareTo0((LocalDateTime) other);
        }
        int cmp = toLocalDate().compareTo(other.toLocalDate());
        if (cmp == 0) {
            cmp = toLocalTime().compareTo(other.toLocalTime());
        }
        return cmp;
    }

    @Override
    public boolean isAfter(ChronoLocalDateTime<?> other) {
        if (other instanceof LocalDateTime) {
            return compareTo0((LocalDateTime) other) > 0;
        }
        return compareTo(other) > 0;
    }

    @Override
    public boolean isBefore(ChronoLocalDateTime<?> other) {
        if (other instanceof LocalDateTime) {
            return compareTo0((LocalDateTime) other) < 0;
        }
        return compareTo(other) < 0;
    }

    @Override
    public boolean isEqual(ChronoLocalDateTime<?> other) {
        if (other instanceof LocalDateTime) {
            return compareTo0((LocalDateTime) other) == 0;
        }
        return compareTo(other) == 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LocalDateTime)) {
            return false;
        }
        LocalDateTime other = (LocalDateTime) obj;
        return date.equals(other.date) && time.equals(other.time);
    }

    @Override
    public int hashCode() {
        return date.hashCode() ^ time.hashCode();
    }

    @Override
    public String toString() {
        return date.toString() + 'T' + time.toString();
    }
}
