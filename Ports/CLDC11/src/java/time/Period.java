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

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// An amount of calendar time in years, months and days, such as
/// "2 years, 3 months and 4 days". The three are kept apart and never
/// converted into one another except by [#normalized].
public final class Period implements TemporalAmount {
    public static final Period ZERO = new Period(0, 0, 0);

    private final int years;
    private final int months;
    private final int days;

    private Period(int years, int months, int days) {
        this.years = years;
        this.months = months;
        this.days = days;
    }

    private static Period create(int years, int months, int days) {
        if ((years | months | days) == 0) {
            return ZERO;
        }
        return new Period(years, months, days);
    }

    public static Period of(int years, int months, int days) {
        return create(years, months, days);
    }

    public static Period ofDays(int days) {
        return create(0, 0, days);
    }

    public static Period ofWeeks(int weeks) {
        return create(0, 0, TimeArithmetic.toIntExact(TimeArithmetic.multiplyExact((long) weeks, 7L)));
    }

    public static Period ofMonths(int months) {
        return create(0, months, 0);
    }

    public static Period ofYears(int years) {
        return create(years, 0, 0);
    }

    /// The years, months and days from `startDateInclusive` to
    /// `endDateExclusive`.
    public static Period between(LocalDate startDateInclusive, LocalDate endDateExclusive) {
        return startDateInclusive.until(endDateExclusive);
    }

    /// The period of `amount`, which must be in years, months and days.
    public static Period from(TemporalAmount amount) {
        if (amount instanceof Period) {
            return (Period) amount;
        }
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        int years = 0;
        int months = 0;
        int days = 0;
        List<TemporalUnit> units = amount.getUnits();
        for (int i = 0; i < units.size(); i++) {
            TemporalUnit unit = units.get(i);
            long value = amount.get(unit);
            if (unit == ChronoUnit.YEARS) {
                years = TimeArithmetic.toIntExact(value);
            } else if (unit == ChronoUnit.MONTHS) {
                months = TimeArithmetic.toIntExact(value);
            } else if (unit == ChronoUnit.DAYS) {
                days = TimeArithmetic.toIntExact(value);
            } else {
                throw new DateTimeException("Unit must be Years, Months or Days, but was " + unit);
            }
        }
        return create(years, months, days);
    }

    /// Reads `PnYnMnWnD`: each section optional but at least one present,
    /// each number signed, the letters in either case, and a leading sign
    /// negating the whole. Weeks are added to the days.
    public static Period parse(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        int pos = 0;
        boolean negate = false;
        if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
            negate = s.charAt(pos) == '-';
            pos++;
        }
        if (pos >= s.length() || (s.charAt(pos) != 'P' && s.charAt(pos) != 'p')) {
            throw unparsablePeriod(s, null);
        }
        pos++;
        String order = "YMWD";
        long[] values = new long[4];
        int next = 0;
        boolean any = false;
        try {
            while (pos < s.length()) {
                int start = pos;
                if (s.charAt(pos) == '+' || s.charAt(pos) == '-') {
                    pos++;
                }
                int digits = pos;
                while (pos < s.length() && s.charAt(pos) >= '0' && s.charAt(pos) <= '9') {
                    pos++;
                }
                if (pos == digits || pos >= s.length()) {
                    throw unparsablePeriod(s, null);
                }
                char letter = Character.toUpperCase(s.charAt(pos));
                int slot = order.indexOf(letter, next);
                if (slot < 0) {
                    throw unparsablePeriod(s, null);
                }
                values[slot] = Integer.parseInt(s.substring(s.charAt(start) == '+' ? start + 1 : start, pos));
                next = slot + 1;
                any = true;
                pos++;
            }
        } catch (NumberFormatException ex) {
            throw unparsablePeriod(s, ex);
        }
        if (!any) {
            throw unparsablePeriod(s, null);
        }
        if (negate) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == Integer.MIN_VALUE) {
                    throw unparsablePeriod(s, new ArithmeticException("integer overflow"));
                }
                values[i] = -values[i];
            }
        }
        // Weeks join the days outside the reading, so an overflow there is
        // the arithmetic's own exception, as the JDK's is.
        int days = TimeArithmetic.toIntExact(values[3] + TimeArithmetic.toIntExact(values[2] * 7));
        return create((int) values[0], (int) values[1], days);
    }

    private static java.time.format.DateTimeParseException unparsablePeriod(String text, Throwable cause) {
        return new java.time.format.DateTimeParseException("Text cannot be parsed to a Period", text, 0, cause);
    }

    @Override
    public long get(TemporalUnit unit) {
        if (unit == ChronoUnit.YEARS) {
            return years;
        }
        if (unit == ChronoUnit.MONTHS) {
            return months;
        }
        if (unit == ChronoUnit.DAYS) {
            return days;
        }
        throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    @Override
    public List<TemporalUnit> getUnits() {
        ArrayList<TemporalUnit> units = new ArrayList<TemporalUnit>(3);
        units.add(ChronoUnit.YEARS);
        units.add(ChronoUnit.MONTHS);
        units.add(ChronoUnit.DAYS);
        return Collections.unmodifiableList(units);
    }

    public int getYears() {
        return years;
    }

    public int getMonths() {
        return months;
    }

    public int getDays() {
        return days;
    }

    public boolean isZero() {
        return this == ZERO || (years | months | days) == 0;
    }

    public boolean isNegative() {
        return years < 0 || months < 0 || days < 0;
    }

    public Period withYears(int years) {
        return years == this.years ? this : create(years, months, days);
    }

    public Period withMonths(int months) {
        return months == this.months ? this : create(years, months, days);
    }

    public Period withDays(int days) {
        return days == this.days ? this : create(years, months, days);
    }

    /// This period plus `amountToAdd`, which must be in years, months and
    /// days; each unit is added to its own.
    public Period plus(TemporalAmount amountToAdd) {
        Period other = from(amountToAdd);
        return create(TimeArithmetic.toIntExact(TimeArithmetic.addExact((long) years, (long) other.years)),
                TimeArithmetic.toIntExact(TimeArithmetic.addExact((long) months, (long) other.months)),
                TimeArithmetic.toIntExact(TimeArithmetic.addExact((long) days, (long) other.days)));
    }

    public Period minus(TemporalAmount amountToSubtract) {
        Period other = from(amountToSubtract);
        return create(TimeArithmetic.toIntExact(TimeArithmetic.subtractExact((long) years, (long) other.years)),
                TimeArithmetic.toIntExact(TimeArithmetic.subtractExact((long) months, (long) other.months)),
                TimeArithmetic.toIntExact(TimeArithmetic.subtractExact((long) days, (long) other.days)));
    }

    public Period plusYears(long yearsToAdd) {
        return yearsToAdd == 0 ? this
                : create(TimeArithmetic.toIntExact(TimeArithmetic.addExact((long) years, yearsToAdd)), months, days);
    }

    public Period plusMonths(long monthsToAdd) {
        return monthsToAdd == 0 ? this
                : create(years, TimeArithmetic.toIntExact(TimeArithmetic.addExact((long) months, monthsToAdd)), days);
    }

    public Period plusDays(long daysToAdd) {
        return daysToAdd == 0 ? this
                : create(years, months, TimeArithmetic.toIntExact(TimeArithmetic.addExact((long) days, daysToAdd)));
    }

    public Period minusYears(long yearsToSubtract) {
        return yearsToSubtract == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1)
                : plusYears(-yearsToSubtract);
    }

    public Period minusMonths(long monthsToSubtract) {
        return monthsToSubtract == Long.MIN_VALUE ? plusMonths(Long.MAX_VALUE).plusMonths(1)
                : plusMonths(-monthsToSubtract);
    }

    public Period minusDays(long daysToSubtract) {
        return daysToSubtract == Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1)
                : plusDays(-daysToSubtract);
    }

    public Period multipliedBy(int scalar) {
        if (this == ZERO || scalar == 1) {
            return this;
        }
        return create(TimeArithmetic.toIntExact(TimeArithmetic.multiplyExact((long) years, (long) scalar)),
                TimeArithmetic.toIntExact(TimeArithmetic.multiplyExact((long) months, (long) scalar)),
                TimeArithmetic.toIntExact(TimeArithmetic.multiplyExact((long) days, (long) scalar)));
    }

    public Period negated() {
        return multipliedBy(-1);
    }

    /// The years and months rebalanced so that the months lie within
    /// -11 to 11 with the sign of the years; the days are left as they are.
    public Period normalized() {
        long totalMonths = toTotalMonths();
        long splitYears = totalMonths / 12;
        int splitMonths = (int) (totalMonths % 12);
        if (splitYears == years && splitMonths == months) {
            return this;
        }
        return create(TimeArithmetic.toIntExact(splitYears), splitMonths, days);
    }

    public long toTotalMonths() {
        return years * 12L + months;
    }

    /// Adds the years and months together, then the days, as the JDK does:
    /// a date at the end of a month stays valid in between.
    @Override
    public Temporal addTo(Temporal temporal) {
        if (months == 0) {
            if (years != 0) {
                temporal = temporal.plus(years, ChronoUnit.YEARS);
            }
        } else {
            long totalMonths = toTotalMonths();
            if (totalMonths != 0) {
                temporal = temporal.plus(totalMonths, ChronoUnit.MONTHS);
            }
        }
        if (days != 0) {
            temporal = temporal.plus(days, ChronoUnit.DAYS);
        }
        return temporal;
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        if (months == 0) {
            if (years != 0) {
                temporal = temporal.minus(years, ChronoUnit.YEARS);
            }
        } else {
            long totalMonths = toTotalMonths();
            if (totalMonths != 0) {
                temporal = temporal.minus(totalMonths, ChronoUnit.MONTHS);
            }
        }
        if (days != 0) {
            temporal = temporal.minus(days, ChronoUnit.DAYS);
        }
        return temporal;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Period)) {
            return false;
        }
        Period other = (Period) obj;
        return years == other.years && months == other.months && days == other.days;
    }

    public int hashCode() {
        return years + ((months << 8) | (months >>> 24)) + ((days << 16) | (days >>> 16));
    }

    public String toString() {
        if (this == ZERO || (years | months | days) == 0) {
            return "P0D";
        }
        StringBuilder sb = new StringBuilder("P");
        if (years != 0) {
            sb.append(years).append('Y');
        }
        if (months != 0) {
            sb.append(months).append('M');
        }
        if (days != 0) {
            sb.append(days).append('D');
        }
        return sb.toString();
    }
}
