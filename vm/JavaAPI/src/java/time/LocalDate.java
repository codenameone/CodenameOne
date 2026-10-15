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

import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.stream.Stream;

/// A date of the ISO calendar with no time and no zone, such as 2007-12-03.
public final class LocalDate implements ChronoLocalDate {
    /// -999999999-01-01, the earliest date.
    public static final LocalDate MIN = new LocalDate(-999999999, 1, 1);
    /// +999999999-12-31, the latest date.
    public static final LocalDate MAX = new LocalDate(999999999, 12, 31);
    /// 1970-01-01.
    public static final LocalDate EPOCH = new LocalDate(1970, 1, 1);

    private static final int YEAR_LIMIT = 999999999;
    private static final long DAYS_PER_CYCLE = 146097L;
    private static final long DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L);

    private final int year;
    private final short month;
    private final short day;

    private LocalDate(int year, int month, int dayOfMonth) {
        this.year = year;
        this.month = (short) month;
        this.day = (short) dayOfMonth;
    }

    private static boolean leap(long year) {
        return ((year & 3) == 0) && ((year % 100) != 0 || (year % 400) == 0);
    }

    private static int monthLength(int year, int month) {
        return Month.of(month).length(leap(year));
    }

    private static LocalDate create(int year, int month, int dayOfMonth) {
        if (dayOfMonth > 28) {
            int dom = monthLength(year, month);
            if (dayOfMonth > dom) {
                if (dayOfMonth == 29) {
                    throw new DateTimeException("Invalid date 'February 29' as '" + year + "' is not a leap year");
                }
                throw new DateTimeException("Invalid date '" + Month.of(month).name() + " " + dayOfMonth + "'");
            }
        }
        return new LocalDate(year, month, dayOfMonth);
    }

    /// The date, with the day brought back to the last one the month has.
    private static LocalDate resolvePreviousValid(int year, int month, int day) {
        int last = monthLength(year, month);
        return new LocalDate(year, month, day > last ? last : day);
    }

    private static int checkYear(long year) {
        return (int) TimeArithmetic.check("Year", -YEAR_LIMIT, YEAR_LIMIT, year);
    }

    public static LocalDate now() {
        return now(Clock.systemDefaultZone());
    }

    /// The date today in `zone`.
    public static LocalDate now(ZoneId zone) {
        return now(Clock.system(zone));
    }

    public static LocalDate now(Clock clock) {
        return ofInstant(clock.instant(), clock.getZone());
    }

    public static LocalDate of(int year, int month, int dayOfMonth) {
        checkYear(year);
        TimeArithmetic.check("MonthOfYear", 1, 12, month);
        TimeArithmetic.check("DayOfMonth", 1, 31, dayOfMonth);
        return create(year, month, dayOfMonth);
    }

    public static LocalDate of(int year, Month month, int dayOfMonth) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return of(year, month.getValue(), dayOfMonth);
    }

    /// The `dayOfYear`th day of `year`.
    public static LocalDate ofYearDay(int year, int dayOfYear) {
        checkYear(year);
        TimeArithmetic.check("DayOfYear", 1, 366, dayOfYear);
        boolean leap = leap(year);
        if (dayOfYear == 366 && !leap) {
            throw new DateTimeException("Invalid date 'DayOfYear 366' as '" + year + "' is not a leap year");
        }
        Month moy = Month.of((dayOfYear - 1) / 31 + 1);
        int monthEnd = moy.firstDayOfYear(leap) + moy.length(leap) - 1;
        if (dayOfYear > monthEnd) {
            moy = moy.plus(1);
        }
        int dom = dayOfYear - moy.firstDayOfYear(leap) + 1;
        return new LocalDate(year, moy.getValue(), dom);
    }

    /// The date `instant` falls on in `zone`.
    public static LocalDate ofInstant(Instant instant, ZoneId zone) {
        return LocalDateTime.ofInstant(instant, zone).toLocalDate();
    }

    public static LocalDate ofEpochDay(long epochDay) {
        TimeArithmetic.check("EpochDay", -365243219162L, 365241780471L, epochDay);
        long zeroDay = epochDay + DAYS_0000_TO_1970;
        // find the march-based year
        zeroDay -= 60;
        long adjust = 0;
        if (zeroDay < 0) {
            long adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1;
            adjust = adjustCycles * 400;
            zeroDay += -adjustCycles * DAYS_PER_CYCLE;
        }
        long yearEst = (400 * zeroDay + 591) / DAYS_PER_CYCLE;
        long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        if (doyEst < 0) {
            yearEst--;
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        }
        yearEst += adjust;
        int marchDoy0 = (int) doyEst;
        int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
        int month = (marchMonth0 + 2) % 12 + 1;
        int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
        yearEst += marchMonth0 / 10;
        return new LocalDate(checkYear(yearEst), month, dom);
    }

    /// The date of `temporal`.
    public static LocalDate from(TemporalAccessor temporal) {
        if (temporal instanceof LocalDate) {
            return (LocalDate) temporal;
        }
        if (temporal instanceof LocalDateTime) {
            return ((LocalDateTime) temporal).toLocalDate();
        }
        if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toLocalDate();
        }
        if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).toLocalDate();
        }
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        throw new DateTimeException("Unable to obtain LocalDate from TemporalAccessor: " + temporal
                + " of type " + temporal.getClass().getName());
    }

    public static LocalDate parse(CharSequence text) {
        return parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static LocalDate parse(CharSequence text, DateTimeFormatter formatter) {
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

    public int getYear() {
        return year;
    }

    public int getMonthValue() {
        return month;
    }

    /// The month as a `Month`.
    public Month getMonth() {
        return Month.of(month);
    }

    public int getDayOfMonth() {
        return day;
    }

    /// The day of the year, 1 for January 1st.
    public int getDayOfYear() {
        return getMonth().firstDayOfYear(isLeapYear()) + day - 1;
    }

    /// The day of the week.
    public DayOfWeek getDayOfWeek() {
        int dow0 = (int) TimeArithmetic.floorMod(toEpochDay() + 3, 7);
        return DayOfWeek.of(dow0 + 1);
    }

    @Override
    public boolean isLeapYear() {
        return leap(year);
    }

    @Override
    public int lengthOfMonth() {
        return monthLength(year, month);
    }

    @Override
    public int lengthOfYear() {
        return isLeapYear() ? 366 : 365;
    }

    @Override
    public long toEpochDay() {
        long y = year;
        long m = month;
        long total = 0;
        total += 365 * y;
        if (y >= 0) {
            total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
        } else {
            total -= y / -4 - y / -100 + y / -400;
        }
        total += ((367 * m - 362) / 12);
        total += day - 1;
        if (m > 2) {
            total--;
            if (!isLeapYear()) {
                total--;
            }
        }
        return total - DAYS_0000_TO_1970;
    }

    private long getProlepticMonth() {
        return (year * 12L + month - 1);
    }

    public LocalDate withYear(int year) {
        if (this.year == year) {
            return this;
        }
        checkYear(year);
        return resolvePreviousValid(year, month, day);
    }

    public LocalDate withMonth(int month) {
        if (this.month == month) {
            return this;
        }
        TimeArithmetic.check("MonthOfYear", 1, 12, month);
        return resolvePreviousValid(year, month, day);
    }

    public LocalDate withDayOfMonth(int dayOfMonth) {
        if (this.day == dayOfMonth) {
            return this;
        }
        return of(year, month, dayOfMonth);
    }

    public LocalDate withDayOfYear(int dayOfYear) {
        if (getDayOfYear() == dayOfYear) {
            return this;
        }
        return ofYearDay(year, dayOfYear);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isDateBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    @Override
    public LocalDate plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            if (u == ChronoUnit.DAYS) {
                return plusDays(amountToAdd);
            }
            if (u == ChronoUnit.WEEKS) {
                return plusWeeks(amountToAdd);
            }
            if (u == ChronoUnit.MONTHS) {
                return plusMonths(amountToAdd);
            }
            if (u == ChronoUnit.YEARS) {
                return plusYears(amountToAdd);
            }
            if (u == ChronoUnit.DECADES) {
                return plusYears(TimeArithmetic.multiplyExact(amountToAdd, 10));
            }
            if (u == ChronoUnit.CENTURIES) {
                return plusYears(TimeArithmetic.multiplyExact(amountToAdd, 100));
            }
            if (u == ChronoUnit.MILLENNIA) {
                return plusYears(TimeArithmetic.multiplyExact(amountToAdd, 1000));
            }
            if (u == ChronoUnit.ERAS) {
                // The two eras of the ISO calendar: year 1 and later, and before.
                long era = TimeArithmetic.addExact(year >= 1 ? 1 : 0, amountToAdd);
                TimeArithmetic.check("Era", 0, 1, era);
                return amountToAdd == 0 ? this : resolvePreviousValid(1 - year, month, day);
            }
            throw TimeArithmetic.unsupported(unit);
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.addTo(this, amountToAdd);
    }

    @Override
    public LocalDate minus(long amountToSubtract, TemporalUnit unit) {
        return amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit);
    }

    @Override
    public LocalDate plus(TemporalAmount amount) {
        return (LocalDate) amount.addTo(this);
    }

    @Override
    public LocalDate minus(TemporalAmount amount) {
        return (LocalDate) amount.subtractFrom(this);
    }

    public LocalDate plusDays(long daysToAdd) {
        if (daysToAdd == 0) {
            return this;
        }
        return ofEpochDay(TimeArithmetic.addExact(toEpochDay(), daysToAdd));
    }

    public LocalDate plusWeeks(long weeksToAdd) {
        return plusDays(TimeArithmetic.multiplyExact(weeksToAdd, 7));
    }

    public LocalDate plusMonths(long monthsToAdd) {
        if (monthsToAdd == 0) {
            return this;
        }
        long monthCount = year * 12L + (month - 1);
        long calcMonths = monthCount + monthsToAdd;
        int newYear = checkYear(TimeArithmetic.floorDiv(calcMonths, 12));
        int newMonth = (int) TimeArithmetic.floorMod(calcMonths, 12) + 1;
        return resolvePreviousValid(newYear, newMonth, day);
    }

    public LocalDate plusYears(long yearsToAdd) {
        if (yearsToAdd == 0) {
            return this;
        }
        int newYear = checkYear(year + yearsToAdd);
        return resolvePreviousValid(newYear, month, day);
    }

    public LocalDate minusDays(long daysToSubtract) {
        return daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract);
    }

    public LocalDate minusWeeks(long weeksToSubtract) {
        return weeksToSubtract == Long.MIN_VALUE ? plusWeeks(Long.MAX_VALUE).plusWeeks(1)
                : plusWeeks(-weeksToSubtract);
    }

    public LocalDate minusMonths(long monthsToSubtract) {
        return monthsToSubtract == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1)
                : plusMonths(-monthsToSubtract);
    }

    public LocalDate minusYears(long yearsToSubtract) {
        return yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1)
                : plusYears(-yearsToSubtract);
    }

    /// The whole units of `unit` from this date to the date of `endExclusive`.
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        LocalDate end = from(endExclusive);
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            if (u == ChronoUnit.DAYS) {
                return end.toEpochDay() - toEpochDay();
            }
            if (u == ChronoUnit.WEEKS) {
                return (end.toEpochDay() - toEpochDay()) / 7;
            }
            if (u == ChronoUnit.MONTHS) {
                return monthsUntil(end);
            }
            if (u == ChronoUnit.YEARS) {
                return monthsUntil(end) / 12;
            }
            if (u == ChronoUnit.DECADES) {
                return monthsUntil(end) / 120;
            }
            if (u == ChronoUnit.CENTURIES) {
                return monthsUntil(end) / 1200;
            }
            if (u == ChronoUnit.MILLENNIA) {
                return monthsUntil(end) / 12000;
            }
            if (u == ChronoUnit.ERAS) {
                return (end.year >= 1 ? 1 : 0) - (year >= 1 ? 1 : 0);
            }
            throw TimeArithmetic.unsupported(unit);
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return unit.between(this, end);
    }

    long monthsUntil(LocalDate end) {
        long packed1 = getProlepticMonth() * 32L + day;
        long packed2 = end.getProlepticMonth() * 32L + end.day;
        return (packed2 - packed1) / 32;
    }

    /// The years, months and days from this date to `endDateExclusive`.
    public Period until(ChronoLocalDate endDateExclusive) {
        LocalDate end = from(endDateExclusive);
        long totalMonths = end.getProlepticMonth() - getProlepticMonth();
        int days = end.day - day;
        if (totalMonths > 0 && days < 0) {
            totalMonths--;
            LocalDate calcDate = plusMonths(totalMonths);
            days = (int) (end.toEpochDay() - calcDate.toEpochDay());
        } else if (totalMonths < 0 && days > 0) {
            totalMonths++;
            days -= end.lengthOfMonth();
        }
        long years = totalMonths / 12;
        int months = (int) (totalMonths % 12);
        return Period.of(TimeArithmetic.toIntExact(years), months, days);
    }

    /// The dates from this one up to and not including `endExclusive`, a day
    /// apart.
    public Stream<LocalDate> datesUntil(LocalDate endExclusive) {
        long end = endExclusive.toEpochDay();
        long start = toEpochDay();
        if (end < start) {
            throw new IllegalArgumentException(endExclusive + " < " + this);
        }
        // Made in full: the class library has no lazy stream source yet.
        LocalDate[] all = new LocalDate[TimeArithmetic.toIntExact(end - start)];
        for (int i = 0; i < all.length; i++) {
            all[i] = ofEpochDay(start + i);
        }
        return Stream.of(all);
    }

    public LocalDateTime atTime(int hour, int minute) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute));
    }

    public LocalDateTime atTime(int hour, int minute, int second) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute, second));
    }

    public LocalDateTime atTime(int hour, int minute, int second, int nano) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute, second, nano));
    }

    public LocalDateTime atTime(LocalTime time) {
        return LocalDateTime.of(this, time);
    }

    /// Midnight at the start of this date.
    public LocalDateTime atStartOfDay() {
        return LocalDateTime.of(this, LocalTime.MIDNIGHT);
    }

    public ZonedDateTime atStartOfDay(ZoneId zone) {
        return ZonedDateTime.of(this, LocalTime.MIDNIGHT, zone);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    private int compareTo0(LocalDate otherDate) {
        int cmp = year - otherDate.year;
        if (cmp == 0) {
            cmp = month - otherDate.month;
            if (cmp == 0) {
                cmp = day - otherDate.day;
            }
        }
        return cmp;
    }

    @Override
    public int compareTo(ChronoLocalDate other) {
        if (other instanceof LocalDate) {
            return compareTo0((LocalDate) other);
        }
        long a = toEpochDay();
        long b = other.toEpochDay();
        return a < b ? -1 : a > b ? 1 : 0;
    }

    @Override
    public boolean isAfter(ChronoLocalDate other) {
        if (other instanceof LocalDate) {
            return compareTo0((LocalDate) other) > 0;
        }
        return toEpochDay() > other.toEpochDay();
    }

    @Override
    public boolean isBefore(ChronoLocalDate other) {
        if (other instanceof LocalDate) {
            return compareTo0((LocalDate) other) < 0;
        }
        return toEpochDay() < other.toEpochDay();
    }

    @Override
    public boolean isEqual(ChronoLocalDate other) {
        if (other instanceof LocalDate) {
            return compareTo0((LocalDate) other) == 0;
        }
        return toEpochDay() == other.toEpochDay();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof LocalDate && compareTo0((LocalDate) obj) == 0;
    }

    @Override
    public int hashCode() {
        return (year & 0xFFFFF800) ^ ((year << 11) + (month << 6) + day);
    }

    /// The JDK's text, `uuuu-MM-dd`, with a sign on a year past 9999.
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(12);
        int absYear = year < 0 ? -year : year;
        if (absYear < 1000) {
            if (year < 0) {
                buf.append('-');
            }
            String digits = Integer.toString(absYear + 10000);
            buf.append(digits.substring(1));
        } else {
            if (year > 9999) {
                buf.append('+');
            }
            buf.append(year);
        }
        return buf.append(month < 10 ? "-0" : "-").append((int) month).append(day < 10 ? "-0" : "-")
                .append((int) day).toString();
    }
}
