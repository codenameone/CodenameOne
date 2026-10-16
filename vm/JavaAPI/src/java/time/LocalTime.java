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

/// A time of day with no date and no zone, such as 10:15:30.
public final class LocalTime implements Temporal, Comparable<LocalTime> {
    /// 00:00.
    public static final LocalTime MIDNIGHT = new LocalTime(0, 0, 0, 0);
    /// 00:00, the earliest time of day.
    public static final LocalTime MIN = MIDNIGHT;
    /// 12:00.
    public static final LocalTime NOON = new LocalTime(12, 0, 0, 0);
    /// 23:59:59.999999999, the latest time of day.
    public static final LocalTime MAX = new LocalTime(23, 59, 59, 999999999);

    private static final long NANOS_PER_SECOND = TimeArithmetic.NANOS_PER_SECOND;
    private static final long NANOS_PER_MINUTE = 60L * NANOS_PER_SECOND;
    private static final long NANOS_PER_HOUR = 60L * NANOS_PER_MINUTE;
    private static final long NANOS_PER_DAY = TimeArithmetic.NANOS_PER_DAY;

    private final int hour;
    private final int minute;
    private final int second;
    private final int nano;

    private LocalTime(int hour, int minute, int second, int nano) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.nano = nano;
    }

    private static LocalTime create(int hour, int minute, int second, int nano) {
        if ((minute | second | nano) == 0 && hour == 0) {
            return MIDNIGHT;
        }
        return new LocalTime(hour, minute, second, nano);
    }

    public static LocalTime now() {
        return now(Clock.systemDefaultZone());
    }

    /// The time of day now in `zone`.
    public static LocalTime now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static LocalTime now(Clock clock) {
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone()).toLocalTime();
    }

    public static LocalTime of(int hour, int minute) {
        return of(hour, minute, 0, 0);
    }

    public static LocalTime of(int hour, int minute, int second) {
        return of(hour, minute, second, 0);
    }

    public static LocalTime of(int hour, int minute, int second, int nanoOfSecond) {
        TimeArithmetic.check("HourOfDay", 0, 23, hour);
        TimeArithmetic.check("MinuteOfHour", 0, 59, minute);
        TimeArithmetic.check("SecondOfMinute", 0, 59, second);
        TimeArithmetic.check("NanoOfSecond", 0, 999999999, nanoOfSecond);
        return create(hour, minute, second, nanoOfSecond);
    }

    public static LocalTime ofSecondOfDay(long secondOfDay) {
        TimeArithmetic.check("SecondOfDay", 0, 86399, secondOfDay);
        int hours = (int) (secondOfDay / 3600);
        secondOfDay -= hours * 3600L;
        int minutes = (int) (secondOfDay / 60);
        secondOfDay -= minutes * 60L;
        return create(hours, minutes, (int) secondOfDay, 0);
    }

    public static LocalTime ofNanoOfDay(long nanoOfDay) {
        TimeArithmetic.check("NanoOfDay", 0, NANOS_PER_DAY - 1, nanoOfDay);
        int hours = (int) (nanoOfDay / NANOS_PER_HOUR);
        nanoOfDay -= hours * NANOS_PER_HOUR;
        int minutes = (int) (nanoOfDay / NANOS_PER_MINUTE);
        nanoOfDay -= minutes * NANOS_PER_MINUTE;
        int seconds = (int) (nanoOfDay / NANOS_PER_SECOND);
        nanoOfDay -= seconds * NANOS_PER_SECOND;
        return create(hours, minutes, seconds, (int) nanoOfDay);
    }

    /// The time of day of `temporal`.
    public static LocalTime from(TemporalAccessor temporal) {
        if (temporal instanceof LocalTime) {
            return (LocalTime) temporal;
        }
        if (temporal instanceof LocalDateTime) {
            return ((LocalDateTime) temporal).toLocalTime();
        }
        if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toLocalTime();
        }
        if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).toLocalTime();
        }
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        throw new DateTimeException("Unable to obtain LocalTime from TemporalAccessor: " + temporal
                + " of type " + temporal.getClass().getName());
    }

    public static LocalTime parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_LOCAL_TIME);
    }

    public static LocalTime parse(CharSequence text, DateTimeFormatter formatter) {
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

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public int getNano() {
        return nano;
    }

    public int toSecondOfDay() {
        return hour * 3600 + minute * 60 + second;
    }

    public long toNanoOfDay() {
        return hour * NANOS_PER_HOUR + minute * NANOS_PER_MINUTE + second * NANOS_PER_SECOND + nano;
    }

    /// This time of day on `date`.
    public LocalDateTime atDate(LocalDate date) {
        return LocalDateTime.of(date, this);
    }

    public LocalTime withHour(int hour) {
        if (this.hour == hour) {
            return this;
        }
        TimeArithmetic.check("HourOfDay", 0, 23, hour);
        return create(hour, minute, second, nano);
    }

    public LocalTime withMinute(int minute) {
        if (this.minute == minute) {
            return this;
        }
        TimeArithmetic.check("MinuteOfHour", 0, 59, minute);
        return create(hour, minute, second, nano);
    }

    public LocalTime withSecond(int second) {
        if (this.second == second) {
            return this;
        }
        TimeArithmetic.check("SecondOfMinute", 0, 59, second);
        return create(hour, minute, second, nano);
    }

    public LocalTime withNano(int nanoOfSecond) {
        if (this.nano == nanoOfSecond) {
            return this;
        }
        TimeArithmetic.check("NanoOfSecond", 0, 999999999, nanoOfSecond);
        return create(hour, minute, second, nanoOfSecond);
    }

    /// This time with everything smaller than `unit` set to zero. The unit
    /// has to divide a day.
    public LocalTime truncatedTo(TemporalUnit unit) {
        if (unit == ChronoUnit.NANOS) {
            return this;
        }
        long dur = TimeArithmetic.truncationNanos(unit);
        long nod = toNanoOfDay();
        return ofNanoOfDay((nod / dur) * dur);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isTimeBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public LocalTime plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            if (u == ChronoUnit.NANOS) {
                return plusNanos(amountToAdd);
            }
            if (u == ChronoUnit.MICROS) {
                return plusNanos((amountToAdd % 86400000000L) * 1000);
            }
            if (u == ChronoUnit.MILLIS) {
                return plusNanos((amountToAdd % 86400000L) * 1000000);
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
                return plusHours((amountToAdd % 2) * 12);
            }
            throw TimeArithmetic.unsupported(unit);
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.addTo(this, amountToAdd);
    }

    @Override
    public LocalTime minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit);
    }

    @Override
    public LocalTime plus(TemporalAmount amount) {
        return (LocalTime) amount.addTo(this);
    }

    @Override
    public LocalTime minus(TemporalAmount amount) {
        return (LocalTime) amount.subtractFrom(this);
    }

    public LocalTime plusHours(long hoursToAdd) {
        if (hoursToAdd == 0) {
            return this;
        }
        int newHour = ((int) (hoursToAdd % 24) + hour + 24) % 24;
        return create(newHour, minute, second, nano);
    }

    public LocalTime plusMinutes(long minutesToAdd) {
        if (minutesToAdd == 0) {
            return this;
        }
        int mofd = hour * 60 + minute;
        int newMofd = ((int) (minutesToAdd % 1440) + mofd + 1440) % 1440;
        if (mofd == newMofd) {
            return this;
        }
        return create(newMofd / 60, newMofd % 60, second, nano);
    }

    public LocalTime plusSeconds(long secondstoAdd) {
        if (secondstoAdd == 0) {
            return this;
        }
        int sofd = toSecondOfDay();
        int newSofd = ((int) (secondstoAdd % 86400) + sofd + 86400) % 86400;
        if (sofd == newSofd) {
            return this;
        }
        return create(newSofd / 3600, (newSofd / 60) % 60, newSofd % 60, nano);
    }

    public LocalTime plusNanos(long nanosToAdd) {
        if (nanosToAdd == 0) {
            return this;
        }
        long nofd = toNanoOfDay();
        long newNofd = ((nanosToAdd % NANOS_PER_DAY) + nofd + NANOS_PER_DAY) % NANOS_PER_DAY;
        if (nofd == newNofd) {
            return this;
        }
        return ofNanoOfDay(newNofd);
    }

    public LocalTime minusHours(long hoursToSubtract) {
        return plusHours(-(hoursToSubtract % 24));
    }

    public LocalTime minusMinutes(long minutesToSubtract) {
        return plusMinutes(-(minutesToSubtract % 1440));
    }

    public LocalTime minusSeconds(long secondsToSubtract) {
        return plusSeconds(-(secondsToSubtract % 86400));
    }

    public LocalTime minusNanos(long nanosToSubtract) {
        return plusNanos(-(nanosToSubtract % NANOS_PER_DAY));
    }

    /// The whole units of `unit` from this time to `endExclusive`, within
    /// one day.
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        LocalTime end = from(endExclusive);
        if (unit instanceof ChronoUnit) {
            long nanosUntil = end.toNanoOfDay() - toNanoOfDay();
            ChronoUnit u = (ChronoUnit) unit;
            if (u == ChronoUnit.NANOS) {
                return nanosUntil;
            }
            if (u == ChronoUnit.MICROS) {
                return nanosUntil / 1000;
            }
            if (u == ChronoUnit.MILLIS) {
                return nanosUntil / 1000000;
            }
            if (u == ChronoUnit.SECONDS) {
                return nanosUntil / NANOS_PER_SECOND;
            }
            if (u == ChronoUnit.MINUTES) {
                return nanosUntil / NANOS_PER_MINUTE;
            }
            if (u == ChronoUnit.HOURS) {
                return nanosUntil / NANOS_PER_HOUR;
            }
            if (u == ChronoUnit.HALF_DAYS) {
                return nanosUntil / (12 * NANOS_PER_HOUR);
            }
            throw TimeArithmetic.unsupported(unit);
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.between(this, end);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public boolean isAfter(LocalTime other) {
        return compareTo(other) > 0;
    }

    public boolean isBefore(LocalTime other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(LocalTime other) {
        long a = toNanoOfDay();
        long b = other.toNanoOfDay();
        return a < b ? -1 : a > b ? 1 : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LocalTime)) {
            return false;
        }
        LocalTime other = (LocalTime) obj;
        return hour == other.hour && minute == other.minute && second == other.second && nano == other.nano;
    }

    @Override
    public int hashCode() {
        long nod = toNanoOfDay();
        return (int) (nod ^ (nod >>> 32));
    }

    /// The JDK's text: `HH:mm`, then `:ss` when there are seconds or a
    /// fraction, then the fraction in as many groups of three digits as it
    /// needs.
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(18);
        buf.append(hour < 10 ? "0" : "").append(hour).append(minute < 10 ? ":0" : ":").append(minute);
        if (second > 0 || nano > 0) {
            buf.append(second < 10 ? ":0" : ":").append(second);
            if (nano > 0) {
                buf.append('.');
                if (nano % 1000000 == 0) {
                    buf.append(Integer.toString((nano / 1000000) + 1000).substring(1));
                } else if (nano % 1000 == 0) {
                    buf.append(Integer.toString((nano / 1000) + 1000000).substring(1));
                } else {
                    buf.append(Integer.toString(nano + 1000000000).substring(1));
                }
            }
        }
        return buf.toString();
    }
}
