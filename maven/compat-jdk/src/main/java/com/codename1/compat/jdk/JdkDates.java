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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/// Members of `java.time` and `java.util.Date` the device's classes lack,
/// as static methods that take the receiver first.
///
/// Everything here is arithmetic over what the device's classes do have --
/// the epoch day of a date, the nanosecond of a time, the second of an
/// instant -- and follows the JDK's rules for it: a month added to the 31st
/// lands on the last day of a shorter month, a count of months between two
/// dates looks at the day of the month, a count of hours between two
/// moments drops the fraction towards zero.
public final class JdkDates {
    private static final long NANOS_PER_SECOND = 1000000000L;
    private static final long SECONDS_PER_DAY = 86400L;
    private static final long NANOS_PER_DAY = SECONDS_PER_DAY * NANOS_PER_SECOND;

    /// `Duration.ZERO`.
    public static final Duration ZERO = Duration.ofSeconds(0);

    private JdkDates() {
    }

    private static long floorDiv(long a, long b) {
        long q = a / b;
        return (a % b != 0 && ((a < 0) != (b < 0))) ? q - 1 : q;
    }

    private static long floorMod(long a, long b) {
        return a - floorDiv(a, b) * b;
    }

    private static long multiplyExact(long a, long b) {
        long r = a * b;
        long absA = a < 0 ? -a : a;
        long absB = b < 0 ? -b : b;
        if (((absA | absB) >>> 31) != 0 && (b != 0 && (r / b != a) || a == Long.MIN_VALUE && b == -1)) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    private static long addExact(long a, long b) {
        long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    private static DateTimeException unsupported(Object unit) {
        return new DateTimeException("Unsupported unit: " + unit);
    }

    private static ChronoUnit chrono(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return (ChronoUnit) unit;
        }
        throw unsupported(unit);
    }

    // ---------------------------------------------------------------
    // Instant
    // ---------------------------------------------------------------

    private static Instant instant(long seconds, long nanos) {
        return Instant.ofEpochSecond(seconds + floorDiv(nanos, NANOS_PER_SECOND), floorMod(nanos, NANOS_PER_SECOND));
    }

    public static ZonedDateTime atZone(Instant instant, ZoneId zone) {
        return ZonedDateTime.ofInstant(instant, zone);
    }

    public static boolean isBefore(Instant instant, Instant other) {
        return instant.compareTo(other) < 0;
    }

    public static boolean isAfter(Instant instant, Instant other) {
        return instant.compareTo(other) > 0;
    }

    public static Instant plusNanos(Instant instant, long nanos) {
        return instant(instant.getEpochSecond() + nanos / NANOS_PER_SECOND,
                instant.getNano() + nanos % NANOS_PER_SECOND);
    }

    public static Instant minusNanos(Instant instant, long nanos) {
        return instant(instant.getEpochSecond() - nanos / NANOS_PER_SECOND,
                instant.getNano() - nanos % NANOS_PER_SECOND);
    }

    public static Instant plus(Instant instant, Duration amount) {
        return instant(instant.getEpochSecond() + amount.getSeconds(), (long) instant.getNano() + amount.getNano());
    }

    public static Instant minus(Instant instant, Duration amount) {
        return instant(instant.getEpochSecond() - amount.getSeconds(), (long) instant.getNano() - amount.getNano());
    }

    public static Instant plus(Instant instant, long amount, TemporalUnit unit) {
        ChronoUnit u = chrono(unit);
        if (u.compareTo(ChronoUnit.DAYS) > 0) {
            throw unsupported(u);
        }
        if (u.seconds() == 0) {
            long per = NANOS_PER_SECOND / u.nanos();
            return instant(instant.getEpochSecond() + amount / per, instant.getNano() + amount % per * u.nanos());
        }
        return instant(instant.getEpochSecond() + amount * u.seconds(), instant.getNano());
    }

    public static Instant minus(Instant instant, long amount, TemporalUnit unit) {
        return plus(instant, -amount, unit);
    }

    // ---------------------------------------------------------------
    // java.util.Date
    // ---------------------------------------------------------------

    public static Instant toInstant(Date date) {
        return Instant.ofEpochMilli(date.getTime());
    }

    /// `Date.from(Instant)`.
    public static Date from(Instant instant) {
        return new Date(instant.toEpochMilli());
    }

    public static boolean after(Date date, Date when) {
        return date.getTime() > when.getTime();
    }

    public static boolean before(Date date, Date when) {
        return date.getTime() < when.getTime();
    }

    private static int field(Date date, int field) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(field);
    }

    /// The year less 1900, as `Date` has always answered.
    public static int getYear(Date date) {
        return field(date, Calendar.YEAR) - 1900;
    }

    /// The month, 0 for January.
    public static int getMonth(Date date) {
        return field(date, Calendar.MONTH);
    }

    /// The day of the month.
    public static int getDate(Date date) {
        return field(date, Calendar.DAY_OF_MONTH);
    }

    /// The day of the week, 0 for Sunday.
    public static int getDay(Date date) {
        return field(date, Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
    }

    public static int getHours(Date date) {
        return field(date, Calendar.HOUR_OF_DAY);
    }

    public static int getMinutes(Date date) {
        return field(date, Calendar.MINUTE);
    }

    public static int getSeconds(Date date) {
        return field(date, Calendar.SECOND);
    }

    // ---------------------------------------------------------------
    // LocalDate
    // ---------------------------------------------------------------

    private static int monthLength(int year, int month) {
        return Month.of(month).length(LocalDate.of(year, 1, 1).isLeapYear());
    }

    /// The date of `year`, `month` and `day`, with the day brought back to
    /// the last one the month has.
    private static LocalDate valid(int year, int month, int day) {
        int last = monthLength(year, month);
        return LocalDate.of(year, month, day > last ? last : day);
    }

    public static DayOfWeek getDayOfWeek(LocalDate date) {
        return DayOfWeek.of((int) floorMod(date.toEpochDay() + 3, 7) + 1);
    }

    public static Month getMonth(LocalDate date) {
        return Month.of(date.getMonthValue());
    }

    public static int getDayOfYear(LocalDate date) {
        return (int) (date.toEpochDay() - LocalDate.of(date.getYear(), 1, 1).toEpochDay()) + 1;
    }

    public static int lengthOfYear(LocalDate date) {
        return date.isLeapYear() ? 366 : 365;
    }

    public static LocalDateTime atStartOfDay(LocalDate date) {
        return date.atTime(0, 0);
    }

    public static LocalDate withDayOfMonth(LocalDate date, int day) {
        return LocalDate.of(date.getYear(), date.getMonthValue(), day);
    }

    public static LocalDate withMonth(LocalDate date, int month) {
        Month.of(month);
        return valid(date.getYear(), month, date.getDayOfMonth());
    }

    public static LocalDate withYear(LocalDate date, int year) {
        return valid(year, date.getMonthValue(), date.getDayOfMonth());
    }

    public static LocalDate withDayOfYear(LocalDate date, int dayOfYear) {
        if (dayOfYear < 1 || dayOfYear > lengthOfYear(date)) {
            throw new DateTimeException("Invalid value for DayOfYear: " + dayOfYear);
        }
        return LocalDate.ofEpochDay(LocalDate.of(date.getYear(), 1, 1).toEpochDay() + dayOfYear - 1);
    }

    /// `LocalDate.of(int, Month, int)`.
    public static LocalDate localDateOf(int year, Month month, int day) {
        return LocalDate.of(year, month.getValue(), day);
    }

    /// `LocalDate.ofInstant(Instant, ZoneId)`.
    public static LocalDate localDateOfInstant(Instant instant, ZoneId zone) {
        return LocalDateTime.ofInstant(instant, zone).toLocalDate();
    }

    /// `LocalDate.now(ZoneId)`.
    public static LocalDate localDateNow(ZoneId zone) {
        return localDateOfInstant(Instant.now(), zone);
    }

    public static LocalDate plus(LocalDate date, long amount, TemporalUnit unit) {
        ChronoUnit u = chrono(unit);
        if (u == ChronoUnit.DAYS) {
            return date.plusDays(amount);
        }
        if (u == ChronoUnit.WEEKS) {
            return date.plusDays(amount * 7);
        }
        if (u == ChronoUnit.MONTHS) {
            return date.plusMonths(amount);
        }
        if (u == ChronoUnit.YEARS) {
            return date.plusYears(amount);
        }
        if (u == ChronoUnit.DECADES) {
            return date.plusYears(amount * 10);
        }
        if (u == ChronoUnit.CENTURIES) {
            return date.plusYears(amount * 100);
        }
        if (u == ChronoUnit.MILLENNIA) {
            return date.plusYears(amount * 1000);
        }
        if (u == ChronoUnit.ERAS) {
            // The two eras of the ISO calendar: year 1 and later, and before.
            long era = addExact(date.getYear() >= 1 ? 1 : 0, amount);
            if (era < 0 || era > 1) {
                throw new DateTimeException("Invalid value for Era (valid values 0 - 1): " + era);
            }
            return amount == 0 ? date : valid(1 - date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        }
        throw unsupported(u);
    }

    public static LocalDate minus(LocalDate date, long amount, TemporalUnit unit) {
        return plus(date, -amount, unit);
    }

    /// The dates from `date` up to and not including `end`, a day apart.
    public static Stream<LocalDate> datesUntil(LocalDate date, LocalDate end) {
        long from = date.toEpochDay();
        long to = end.toEpochDay();
        if (to < from) {
            throw new IllegalArgumentException(end + " < " + date);
        }
        List<LocalDate> dates = new ArrayList<LocalDate>();
        for (long day = from; day < to; day++) {
            dates.add(LocalDate.ofEpochDay(day));
        }
        return JdkCollections.stream(dates);
    }

    private static long prolepticMonth(LocalDate date) {
        return date.getYear() * 12L + date.getMonthValue() - 1;
    }

    private static long monthsUntil(LocalDate start, LocalDate end) {
        long packed1 = prolepticMonth(start) * 32L + start.getDayOfMonth();
        long packed2 = prolepticMonth(end) * 32L + end.getDayOfMonth();
        return (packed2 - packed1) / 32;
    }

    private static long dateUntil(LocalDate start, LocalDate end, ChronoUnit unit) {
        if (unit == ChronoUnit.DAYS) {
            return end.toEpochDay() - start.toEpochDay();
        }
        if (unit == ChronoUnit.WEEKS) {
            return (end.toEpochDay() - start.toEpochDay()) / 7;
        }
        if (unit == ChronoUnit.MONTHS) {
            return monthsUntil(start, end);
        }
        if (unit == ChronoUnit.YEARS) {
            return monthsUntil(start, end) / 12;
        }
        if (unit == ChronoUnit.DECADES) {
            return monthsUntil(start, end) / 120;
        }
        if (unit == ChronoUnit.CENTURIES) {
            return monthsUntil(start, end) / 1200;
        }
        if (unit == ChronoUnit.MILLENNIA) {
            return monthsUntil(start, end) / 12000;
        }
        if (unit == ChronoUnit.ERAS) {
            return (end.getYear() >= 1 ? 1 : 0) - (start.getYear() >= 1 ? 1 : 0);
        }
        throw unsupported(unit);
    }

    // ---------------------------------------------------------------
    // LocalDateTime
    // ---------------------------------------------------------------

    public static DayOfWeek getDayOfWeek(LocalDateTime value) {
        return getDayOfWeek(value.toLocalDate());
    }

    public static Month getMonth(LocalDateTime value) {
        return Month.of(value.getMonthValue());
    }

    public static int getDayOfYear(LocalDateTime value) {
        return getDayOfYear(value.toLocalDate());
    }

    public static LocalDateTime minusDays(LocalDateTime value, long days) {
        return value.plusDays(-days);
    }

    public static LocalDateTime minusHours(LocalDateTime value, long hours) {
        return value.plusHours(-hours);
    }

    public static LocalDateTime minusMinutes(LocalDateTime value, long minutes) {
        return value.plusMinutes(-minutes);
    }

    public static LocalDateTime minusSeconds(LocalDateTime value, long seconds) {
        return value.plusSeconds(-seconds);
    }

    public static LocalDateTime plusWeeks(LocalDateTime value, long weeks) {
        return value.plusDays(weeks * 7);
    }

    public static LocalDateTime minusWeeks(LocalDateTime value, long weeks) {
        return value.plusDays(-weeks * 7);
    }

    public static LocalDateTime plusMonths(LocalDateTime value, long months) {
        return LocalDateTime.of(value.toLocalDate().plusMonths(months), value.toLocalTime());
    }

    public static LocalDateTime minusMonths(LocalDateTime value, long months) {
        return plusMonths(value, -months);
    }

    public static LocalDateTime plusYears(LocalDateTime value, long years) {
        return LocalDateTime.of(value.toLocalDate().plusYears(years), value.toLocalTime());
    }

    public static LocalDateTime minusYears(LocalDateTime value, long years) {
        return plusYears(value, -years);
    }

    public static LocalDateTime plusNanos(LocalDateTime value, long nanos) {
        long total = value.toLocalTime().toNanoOfDay() + nanos % NANOS_PER_DAY;
        long days = nanos / NANOS_PER_DAY + floorDiv(total, NANOS_PER_DAY);
        return LocalDateTime.of(value.toLocalDate().plusDays(days),
                LocalTime.ofNanoOfDay(floorMod(total, NANOS_PER_DAY)));
    }

    public static LocalDateTime minusNanos(LocalDateTime value, long nanos) {
        return plusNanos(value, -nanos);
    }

    public static LocalDateTime plus(LocalDateTime value, long amount, TemporalUnit unit) {
        ChronoUnit u = chrono(unit);
        if (u.isDateBased()) {
            return LocalDateTime.of(plus(value.toLocalDate(), amount, u), value.toLocalTime());
        }
        if (u == ChronoUnit.FOREVER) {
            throw unsupported(u);
        }
        if (u.seconds() == 0) {
            long perDay = NANOS_PER_DAY / u.nanos();
            return plusNanos(value.plusDays(amount / perDay), amount % perDay * u.nanos());
        }
        long perDay = SECONDS_PER_DAY / u.seconds();
        return value.plusDays(amount / perDay).plusSeconds(amount % perDay * u.seconds());
    }

    public static LocalDateTime minus(LocalDateTime value, long amount, TemporalUnit unit) {
        return plus(value, -amount, unit);
    }

    public static LocalDateTime withHour(LocalDateTime value, int hour) {
        LocalTime t = value.toLocalTime();
        return LocalDateTime.of(value.toLocalDate(), LocalTime.of(hour, t.getMinute(), t.getSecond(), t.getNano()));
    }

    public static LocalDateTime withMinute(LocalDateTime value, int minute) {
        LocalTime t = value.toLocalTime();
        return LocalDateTime.of(value.toLocalDate(), LocalTime.of(t.getHour(), minute, t.getSecond(), t.getNano()));
    }

    public static LocalDateTime withSecond(LocalDateTime value, int second) {
        LocalTime t = value.toLocalTime();
        return LocalDateTime.of(value.toLocalDate(), LocalTime.of(t.getHour(), t.getMinute(), second, t.getNano()));
    }

    public static LocalDateTime withNano(LocalDateTime value, int nano) {
        LocalTime t = value.toLocalTime();
        return LocalDateTime.of(value.toLocalDate(), LocalTime.of(t.getHour(), t.getMinute(), t.getSecond(), nano));
    }

    /// The value with everything smaller than `unit` set to zero. The unit
    /// has to divide a day.
    public static LocalDateTime truncatedTo(LocalDateTime value, TemporalUnit unit) {
        ChronoUnit u = chrono(unit);
        if (u.compareTo(ChronoUnit.DAYS) > 0) {
            throw new DateTimeException("Unit is too large to be used for truncation");
        }
        long length = u.seconds() * NANOS_PER_SECOND + u.nanos();
        long nanoOfDay = value.toLocalTime().toNanoOfDay();
        return LocalDateTime.of(value.toLocalDate(), LocalTime.ofNanoOfDay(nanoOfDay - nanoOfDay % length));
    }

    public static ZonedDateTime atZone(LocalDateTime value, ZoneId zone) {
        return ZonedDateTime.of(value, zone);
    }

    public static long toEpochSecond(LocalDateTime value, ZoneOffset offset) {
        return value.toInstant(offset).getEpochSecond();
    }

    private static LocalDateTime dateTime(TemporalAccessor value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof ZonedDateTime) {
            return ((ZonedDateTime) value).toLocalDateTime();
        }
        if (value instanceof OffsetDateTime) {
            return ((OffsetDateTime) value).toLocalDateTime();
        }
        throw new DateTimeException("Unable to obtain LocalDateTime from " + value);
    }

    private static LocalDate date(TemporalAccessor value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        return dateTime(value).toLocalDate();
    }

    private static LocalTime time(TemporalAccessor value) {
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        }
        return dateTime(value).toLocalTime();
    }

    private static Instant instantOf(TemporalAccessor value) {
        if (value instanceof Instant) {
            return (Instant) value;
        }
        if (value instanceof ZonedDateTime) {
            return ((ZonedDateTime) value).toInstant();
        }
        if (value instanceof OffsetDateTime) {
            return ((OffsetDateTime) value).toInstant();
        }
        throw new DateTimeException("Unable to obtain Instant from " + value);
    }

    /// The order of two dates. Not `compareTo`: the JDK declares it over
    /// `ChronoLocalDate` and a device over `LocalDate`, so a call compiled
    /// here names a method the device does not have.
    private static int compare(LocalDate a, LocalDate b) {
        long x = a.toEpochDay();
        long y = b.toEpochDay();
        return x < y ? -1 : x > y ? 1 : 0;
    }

    /// The order of two date-times, for the reason [#compare(LocalDate, LocalDate)]
    /// gives.
    private static int compare(LocalDateTime a, LocalDateTime b) {
        int dates = compare(a.toLocalDate(), b.toLocalDate());
        return dates != 0 ? dates : a.toLocalTime().compareTo(b.toLocalTime());
    }

    /// `LocalDateTime.isBefore(ChronoLocalDateTime)`.
    public static boolean isBefore(LocalDateTime value, TemporalAccessor other) {
        return compare(value, dateTime(other)) < 0;
    }

    /// `LocalDateTime.isAfter(ChronoLocalDateTime)`.
    public static boolean isAfter(LocalDateTime value, TemporalAccessor other) {
        return compare(value, dateTime(other)) > 0;
    }

    /// `LocalDateTime.isEqual(ChronoLocalDateTime)`.
    public static boolean isEqual(LocalDateTime value, TemporalAccessor other) {
        return compare(value, dateTime(other)) == 0;
    }

    // ---------------------------------------------------------------
    // ZonedDateTime
    // ---------------------------------------------------------------

    public static LocalDate toLocalDate(ZonedDateTime value) {
        return value.toLocalDateTime().toLocalDate();
    }

    public static LocalTime toLocalTime(ZonedDateTime value) {
        return value.toLocalDateTime().toLocalTime();
    }

    public static long toEpochSecond(ZonedDateTime value) {
        return value.toInstant().getEpochSecond();
    }

    public static int getYear(ZonedDateTime value) {
        return value.toLocalDateTime().getYear();
    }

    public static int getMonthValue(ZonedDateTime value) {
        return value.toLocalDateTime().getMonthValue();
    }

    public static Month getMonth(ZonedDateTime value) {
        return Month.of(value.toLocalDateTime().getMonthValue());
    }

    public static int getDayOfMonth(ZonedDateTime value) {
        return value.toLocalDateTime().getDayOfMonth();
    }

    public static int getDayOfYear(ZonedDateTime value) {
        return getDayOfYear(value.toLocalDateTime().toLocalDate());
    }

    public static DayOfWeek getDayOfWeek(ZonedDateTime value) {
        return getDayOfWeek(value.toLocalDateTime().toLocalDate());
    }

    public static int getHour(ZonedDateTime value) {
        return value.toLocalDateTime().getHour();
    }

    public static int getMinute(ZonedDateTime value) {
        return value.toLocalDateTime().getMinute();
    }

    public static int getSecond(ZonedDateTime value) {
        return value.toLocalDateTime().getSecond();
    }

    public static int getNano(ZonedDateTime value) {
        return value.toLocalDateTime().getNano();
    }

    public static ZonedDateTime withZoneSameInstant(ZonedDateTime value, ZoneId zone) {
        return ZonedDateTime.ofInstant(value.toInstant(), zone);
    }

    // A date added to a zoned value moves its wall clock, a time moves
    // the instant: across a change of offset the two differ, as they do in
    // the JDK.

    public static ZonedDateTime plusDays(ZonedDateTime value, long days) {
        return ZonedDateTime.of(value.toLocalDateTime().plusDays(days), value.getZone());
    }

    public static ZonedDateTime minusDays(ZonedDateTime value, long days) {
        return plusDays(value, -days);
    }

    public static ZonedDateTime plusWeeks(ZonedDateTime value, long weeks) {
        return plusDays(value, weeks * 7);
    }

    public static ZonedDateTime minusWeeks(ZonedDateTime value, long weeks) {
        return plusDays(value, -weeks * 7);
    }

    public static ZonedDateTime plusMonths(ZonedDateTime value, long months) {
        return ZonedDateTime.of(plusMonths(value.toLocalDateTime(), months), value.getZone());
    }

    public static ZonedDateTime minusMonths(ZonedDateTime value, long months) {
        return plusMonths(value, -months);
    }

    public static ZonedDateTime plusYears(ZonedDateTime value, long years) {
        return ZonedDateTime.of(plusYears(value.toLocalDateTime(), years), value.getZone());
    }

    public static ZonedDateTime minusYears(ZonedDateTime value, long years) {
        return plusYears(value, -years);
    }

    public static ZonedDateTime plusSeconds(ZonedDateTime value, long seconds) {
        return ZonedDateTime.ofInstant(value.toInstant().plusSeconds(seconds), value.getZone());
    }

    public static ZonedDateTime minusSeconds(ZonedDateTime value, long seconds) {
        return plusSeconds(value, -seconds);
    }

    public static ZonedDateTime plusMinutes(ZonedDateTime value, long minutes) {
        return plusSeconds(value, minutes * 60);
    }

    public static ZonedDateTime minusMinutes(ZonedDateTime value, long minutes) {
        return plusSeconds(value, -minutes * 60);
    }

    public static ZonedDateTime plusHours(ZonedDateTime value, long hours) {
        return plusSeconds(value, hours * 3600);
    }

    public static ZonedDateTime minusHours(ZonedDateTime value, long hours) {
        return plusSeconds(value, -hours * 3600);
    }

    /// `ZonedDateTime.isBefore(ChronoZonedDateTime)`.
    public static boolean isBefore(ZonedDateTime value, TemporalAccessor other) {
        return value.toInstant().compareTo(instantOf(other)) < 0;
    }

    /// `ZonedDateTime.isAfter(ChronoZonedDateTime)`.
    public static boolean isAfter(ZonedDateTime value, TemporalAccessor other) {
        return value.toInstant().compareTo(instantOf(other)) > 0;
    }

    /// `ZonedDateTime.isEqual(ChronoZonedDateTime)`.
    public static boolean isEqual(ZonedDateTime value, TemporalAccessor other) {
        return value.toInstant().compareTo(instantOf(other)) == 0;
    }

    // ---------------------------------------------------------------
    // An amount added: a Duration or a Period
    // ---------------------------------------------------------------

    private static DateTimeException noAmount(Object amount) {
        return new DateTimeException("Unsupported amount: " + amount);
    }

    /// `Instant.plus(TemporalAmount)`.
    public static Instant plusAmount(Instant instant, Object amount) {
        if (amount instanceof Duration) {
            return plus(instant, (Duration) amount);
        }
        if (amount instanceof Period) {
            return plusPeriod(instant, (Period) amount, 1);
        }
        throw noAmount(amount);
    }

    /// `Instant.minus(TemporalAmount)`.
    public static Instant minusAmount(Instant instant, Object amount) {
        if (amount instanceof Duration) {
            return minus(instant, (Duration) amount);
        }
        if (amount instanceof Period) {
            return plusPeriod(instant, (Period) amount, -1);
        }
        throw noAmount(amount);
    }

    /// A moment has days and no months: a period of days alone is added.
    private static Instant plusPeriod(Instant instant, Period period, int sign) {
        if (period.getYears() * 12L + period.getMonths() != 0) {
            throw unsupported(ChronoUnit.MONTHS);
        }
        return plus(instant, (long) sign * period.getDays(), ChronoUnit.DAYS);
    }

    private static LocalDate plusPeriod(LocalDate date, Period period, int sign) {
        long months = period.getYears() * 12L + period.getMonths();
        return date.plusMonths(sign * months).plusDays((long) sign * period.getDays());
    }

    /// `LocalDate.plus(TemporalAmount)`.
    public static LocalDate plusAmount(LocalDate date, Object amount) {
        if (amount instanceof Period) {
            return plusPeriod(date, (Period) amount, 1);
        }
        throw noAmount(amount);
    }

    /// `LocalDate.minus(TemporalAmount)`.
    public static LocalDate minusAmount(LocalDate date, Object amount) {
        if (amount instanceof Period) {
            return plusPeriod(date, (Period) amount, -1);
        }
        throw noAmount(amount);
    }

    private static LocalDateTime plusAmount(LocalDateTime value, Object amount, int sign) {
        if (amount instanceof Period) {
            return LocalDateTime.of(plusPeriod(value.toLocalDate(), (Period) amount, sign), value.toLocalTime());
        }
        if (amount instanceof Duration) {
            Duration d = (Duration) amount;
            long perDay = SECONDS_PER_DAY;
            long seconds = sign * d.getSeconds();
            return plusNanos(value.plusDays(seconds / perDay).plusSeconds(seconds % perDay), (long) sign * d.getNano());
        }
        throw noAmount(amount);
    }

    /// `LocalDateTime.plus(TemporalAmount)`.
    public static LocalDateTime plusAmount(LocalDateTime value, Object amount) {
        return plusAmount(value, amount, 1);
    }

    /// `LocalDateTime.minus(TemporalAmount)`.
    public static LocalDateTime minusAmount(LocalDateTime value, Object amount) {
        return plusAmount(value, amount, -1);
    }

    // ---------------------------------------------------------------
    // Between two values
    // ---------------------------------------------------------------

    /// The whole units between two moments given as seconds and nanoseconds.
    private static long timeUntil(long s1, long n1, long s2, long n2, ChronoUnit unit) {
        long seconds = s2 - s1;
        long nanos = n2 - n1;
        if (seconds > 0 && nanos < 0) {
            seconds--;
            nanos += NANOS_PER_SECOND;
        } else if (seconds < 0 && nanos > 0) {
            seconds++;
            nanos -= NANOS_PER_SECOND;
        }
        if (unit.seconds() == 0) {
            return addExact(multiplyExact(seconds, NANOS_PER_SECOND / unit.nanos()), nanos / unit.nanos());
        }
        return seconds / unit.seconds();
    }

    private static long dateTimeUntil(LocalDateTime start, LocalDateTime end, ChronoUnit unit) {
        if (unit.isTimeBased()) {
            return timeUntil(start.toLocalDate().toEpochDay() * SECONDS_PER_DAY + start.toLocalTime().toSecondOfDay(),
                    start.getNano(),
                    end.toLocalDate().toEpochDay() * SECONDS_PER_DAY + end.toLocalTime().toSecondOfDay(),
                    end.getNano(), unit);
        }
        LocalDate endDate = end.toLocalDate();
        int dates = compare(endDate, start.toLocalDate());
        int times = end.toLocalTime().compareTo(start.toLocalTime());
        if (dates > 0 && times < 0) {
            endDate = endDate.minusDays(1);
        } else if (dates < 0 && times > 0) {
            endDate = endDate.plusDays(1);
        }
        return dateUntil(start.toLocalDate(), endDate, unit);
    }

    /// `unit.between(start, end)` and `start.until(end, unit)`: the whole
    /// units from one value to another of the same kind.
    public static long until(TemporalAccessor start, TemporalAccessor end, TemporalUnit unit) {
        ChronoUnit u = chrono(unit);
        if (u == ChronoUnit.FOREVER) {
            throw unsupported(u);
        }
        if (start instanceof LocalDate) {
            if (u.isTimeBased()) {
                throw unsupported(u);
            }
            return dateUntil((LocalDate) start, date(end), u);
        }
        if (start instanceof LocalDateTime) {
            return dateTimeUntil((LocalDateTime) start, dateTime(end), u);
        }
        if (start instanceof LocalTime) {
            if (!u.isTimeBased()) {
                throw unsupported(u);
            }
            LocalTime from = (LocalTime) start;
            LocalTime to = time(end);
            return timeUntil(from.toSecondOfDay(), from.getNano(), to.toSecondOfDay(), to.getNano(), u);
        }
        if (start instanceof Instant) {
            if (u.compareTo(ChronoUnit.DAYS) > 0) {
                throw unsupported(u);
            }
            Instant from = (Instant) start;
            Instant to = instantOf(end);
            if (u == ChronoUnit.NANOS || u == ChronoUnit.MICROS) {
                // Counted in nanoseconds first, as the JDK does: more than
                // 292 years apart is an overflow in either unit.
                long nanos = addExact(multiplyExact(to.getEpochSecond() - from.getEpochSecond(), NANOS_PER_SECOND),
                        to.getNano() - from.getNano());
                return u == ChronoUnit.NANOS ? nanos : nanos / 1000;
            }
            if (u == ChronoUnit.MILLIS) {
                // Between the two moments each cut to its millisecond.
                return to.toEpochMilli() - from.toEpochMilli();
            }
            return timeUntil(from.getEpochSecond(), from.getNano(), to.getEpochSecond(), to.getNano(), u);
        }
        if (start instanceof ZonedDateTime) {
            ZonedDateTime from = (ZonedDateTime) start;
            Instant to = instantOf(end);
            if (u.isTimeBased()) {
                Instant at = from.toInstant();
                return timeUntil(at.getEpochSecond(), at.getNano(), to.getEpochSecond(), to.getNano(), u);
            }
            return dateTimeUntil(from.toLocalDateTime(), LocalDateTime.ofInstant(to, from.getZone()), u);
        }
        throw new DateTimeException("Unsupported temporal: " + start);
    }

    // ---------------------------------------------------------------
    // Duration
    // ---------------------------------------------------------------

    private static Duration duration(long seconds, long nanos) {
        return Duration.ofSeconds(seconds + floorDiv(nanos, NANOS_PER_SECOND), floorMod(nanos, NANOS_PER_SECOND));
    }

    private static long totalNanos(Duration d) {
        return d.getSeconds() * NANOS_PER_SECOND + d.getNano();
    }

    /// A value as seconds and nanoseconds on the scale `start` counts in:
    /// the epoch for a moment, the local epoch for a date and time, the day
    /// for a time alone.
    private static long[] moment(TemporalAccessor start, TemporalAccessor value) {
        if (start instanceof Instant || start instanceof ZonedDateTime || start instanceof OffsetDateTime) {
            Instant at = instantOf(value);
            return new long[] {at.getEpochSecond(), at.getNano()};
        }
        if (start instanceof LocalDateTime) {
            LocalDateTime at = dateTime(value);
            return new long[] {at.toLocalDate().toEpochDay() * SECONDS_PER_DAY + at.toLocalTime().toSecondOfDay(),
                at.getNano()};
        }
        if (start instanceof LocalTime) {
            LocalTime at = time(value);
            return new long[] {at.toSecondOfDay(), at.getNano()};
        }
        throw unsupported(ChronoUnit.SECONDS);
    }

    /// `Duration.between(Temporal, Temporal)`.
    public static Duration between(TemporalAccessor start, TemporalAccessor end) {
        long[] from = moment(start, start);
        long[] to = moment(start, end);
        return duration(to[0] - from[0], to[1] - from[1]);
    }

    /// `Duration.ofNanos(long)`.
    public static Duration ofNanos(long nanos) {
        return duration(0, nanos);
    }

    /// `Duration.of(long, TemporalUnit)`.
    public static Duration of(long amount, TemporalUnit unit) {
        ChronoUnit u = chrono(unit);
        if (u.isDurationEstimated() && u != ChronoUnit.DAYS) {
            throw new DateTimeException("Unit must not have an estimated duration");
        }
        if (u.seconds() == 0) {
            long per = NANOS_PER_SECOND / u.nanos();
            return duration(amount / per, amount % per * u.nanos());
        }
        return Duration.ofSeconds(amount * u.seconds());
    }

    public static long toDays(Duration d) {
        return d.getSeconds() / SECONDS_PER_DAY;
    }

    public static long toHours(Duration d) {
        return d.getSeconds() / 3600;
    }

    public static long toMinutes(Duration d) {
        return d.getSeconds() / 60;
    }

    public static long toSeconds(Duration d) {
        return d.getSeconds();
    }

    public static long toNanos(Duration d) {
        return totalNanos(d);
    }

    public static long toDaysPart(Duration d) {
        return d.getSeconds() / SECONDS_PER_DAY;
    }

    public static int toHoursPart(Duration d) {
        return (int) (toHours(d) % 24);
    }

    public static int toMinutesPart(Duration d) {
        return (int) (toMinutes(d) % 60);
    }

    public static int toSecondsPart(Duration d) {
        return (int) (d.getSeconds() % 60);
    }

    public static int toMillisPart(Duration d) {
        return d.getNano() / 1000000;
    }

    public static int toNanosPart(Duration d) {
        return d.getNano();
    }

    public static boolean isZero(Duration d) {
        return d.getSeconds() == 0 && d.getNano() == 0;
    }

    public static boolean isNegative(Duration d) {
        return d.getSeconds() < 0;
    }

    public static Duration plusDays(Duration d, long days) {
        return duration(d.getSeconds() + days * SECONDS_PER_DAY, d.getNano());
    }

    public static Duration plusHours(Duration d, long hours) {
        return duration(d.getSeconds() + hours * 3600, d.getNano());
    }

    public static Duration plusMinutes(Duration d, long minutes) {
        return duration(d.getSeconds() + minutes * 60, d.getNano());
    }

    public static Duration plusSeconds(Duration d, long seconds) {
        return duration(d.getSeconds() + seconds, d.getNano());
    }

    public static Duration plusMillis(Duration d, long millis) {
        return duration(d.getSeconds() + millis / 1000, d.getNano() + millis % 1000 * 1000000);
    }

    public static Duration plusNanos(Duration d, long nanos) {
        return duration(d.getSeconds() + nanos / NANOS_PER_SECOND, d.getNano() + nanos % NANOS_PER_SECOND);
    }

    public static Duration minusDays(Duration d, long days) {
        return plusDays(d, -days);
    }

    public static Duration minusHours(Duration d, long hours) {
        return plusHours(d, -hours);
    }

    public static Duration minusMinutes(Duration d, long minutes) {
        return plusMinutes(d, -minutes);
    }

    public static Duration minusSeconds(Duration d, long seconds) {
        return plusSeconds(d, -seconds);
    }

    public static Duration minusMillis(Duration d, long millis) {
        return plusMillis(d, -millis);
    }

    public static Duration minusNanos(Duration d, long nanos) {
        return plusNanos(d, -nanos);
    }

    public static Duration negated(Duration d) {
        return duration(-d.getSeconds(), -(long) d.getNano());
    }

    public static Duration abs(Duration d) {
        return isNegative(d) ? negated(d) : d;
    }

    public static Duration multipliedBy(Duration d, long factor) {
        return duration(d.getSeconds() * factor, d.getNano() * factor);
    }

    /// The duration divided, exact for anything shorter than 292 years.
    public static Duration dividedBy(Duration d, long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return duration(0, totalNanos(d) / divisor);
    }
}
