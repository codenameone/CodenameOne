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

/// A month of a year, such as `2024-02`, with no day and no zone.
public final class YearMonth implements Comparable<YearMonth>, java.io.Serializable {

    private final int year;
    private final int month;

    private YearMonth(int year, int month) {
        this.year = year;
        this.month = month;
    }

    public static YearMonth of(int year, int month) {
        if (month < 1 || month > 12) {
            throw new DateTimeException("Invalid value for MonthOfYear: " + month);
        }
        if (year < -999999999 || year > 999999999) {
            throw new DateTimeException("Invalid value for Year: " + year);
        }
        return new YearMonth(year, month);
    }

    public static YearMonth of(int year, Month month) {
        return of(year, month.getValue());
    }

    public static YearMonth now() {
        return from(LocalDate.now());
    }

    public static YearMonth now(ZoneId zone) {
        return from(LocalDate.now(zone));
    }

    public static YearMonth now(Clock clock) {
        return from(LocalDate.now(clock));
    }

    /// The month of `date`.
    public static YearMonth from(LocalDate date) {
        return new YearMonth(date.getYear(), date.getMonthValue());
    }

    /// Parses `uuuu-MM`, with an optional sign on the year.
    public static YearMonth parse(CharSequence text) {
        String s = text.toString();
        int dash = s.lastIndexOf('-');
        if (dash <= 0 || s.length() - dash != 3) {
            throw new java.time.format.DateTimeParseException("Text '" + s + "' could not be parsed", s, 0);
        }
        String y = s.substring(0, dash);
        String m = s.substring(dash + 1);
        int start = y.charAt(0) == '+' || y.charAt(0) == '-' ? 1 : 0;
        // As the JDK's formatter: more than four year digits need a sign,
        // and a '+' is only allowed on more than four.
        int yearDigits = y.length() - start;
        boolean plus = y.charAt(0) == '+';
        if (yearDigits < 4 || (yearDigits > 4 && start == 0) || (plus && yearDigits == 4)
                || !digits(y, start) || !digits(m, 0)) {
            throw new java.time.format.DateTimeParseException("Text '" + s + "' could not be parsed", s, 0);
        }
        if (y.charAt(0) == '+') {
            y = y.substring(1);
        }
        try {
            return of(Integer.parseInt(y), Integer.parseInt(m));
        } catch (NumberFormatException e) {
            throw new java.time.format.DateTimeParseException("Text '" + s + "' could not be parsed", s, 0);
        } catch (DateTimeException e) {
            throw new java.time.format.DateTimeParseException("Text '" + s + "' could not be parsed: "
                    + e.getMessage(), s, 0);
        }
    }

    private static boolean digits(String s, int from) {
        if (from >= s.length()) {
            return false;
        }
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public int getYear() {
        return year;
    }

    public int getMonthValue() {
        return month;
    }

    public Month getMonth() {
        return Month.of(month);
    }

    public boolean isLeapYear() {
        return (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    public int lengthOfMonth() {
        return LocalDate.of(year, month, 1).lengthOfMonth();
    }

    public int lengthOfYear() {
        return isLeapYear() ? 366 : 365;
    }

    public boolean isValidDay(int dayOfMonth) {
        return dayOfMonth >= 1 && dayOfMonth <= lengthOfMonth();
    }

    public YearMonth withYear(int year) {
        return of(year, month);
    }

    public YearMonth withMonth(int month) {
        return of(year, month);
    }

    public YearMonth plusMonths(long monthsToAdd) {
        if (monthsToAdd == 0) {
            return this;
        }
        long total = year * 12L + (month - 1) + monthsToAdd;
        long y = Math.floorDiv(total, 12L);
        if (y < -999999999L || y > 999999999L) {
            throw new DateTimeException("Invalid value for Year: " + y);
        }
        return new YearMonth((int) y, (int) Math.floorMod(total, 12L) + 1);
    }

    public YearMonth minusMonths(long monthsToSubtract) {
        if (monthsToSubtract == Long.MIN_VALUE) {
            return plusMonths(Long.MAX_VALUE).plusMonths(1);
        }
        return plusMonths(-monthsToSubtract);
    }

    public YearMonth plusYears(long yearsToAdd) {
        if (yearsToAdd == 0) {
            return this;
        }
        long y = year + yearsToAdd;
        if (y < -999999999L || y > 999999999L) {
            throw new DateTimeException("Invalid value for Year: " + y);
        }
        return new YearMonth((int) y, month);
    }

    public YearMonth minusYears(long yearsToSubtract) {
        if (yearsToSubtract == Long.MIN_VALUE) {
            return plusYears(Long.MAX_VALUE).plusYears(1);
        }
        return plusYears(-yearsToSubtract);
    }

    public LocalDate atDay(int dayOfMonth) {
        return LocalDate.of(year, month, dayOfMonth);
    }

    public LocalDate atEndOfMonth() {
        return LocalDate.of(year, month, lengthOfMonth());
    }

    public boolean isBefore(YearMonth other) {
        return compareTo(other) < 0;
    }

    public boolean isAfter(YearMonth other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(YearMonth other) {
        int cmp = year - other.year;
        if (cmp == 0) {
            cmp = month - other.month;
        }
        return cmp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof YearMonth)) {
            return false;
        }
        YearMonth o = (YearMonth) obj;
        return year == o.year && month == o.month;
    }

    @Override
    public int hashCode() {
        return year ^ (month << 27);
    }

    /// The JDK's text, `uuuu-MM`. Unlike `LocalDate`, the JDK puts no `+`
    /// on a year past 9999 here, so such a text does not parse back.
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(10);
        int absYear = year < 0 ? -year : year;
        if (absYear < 1000) {
            if (year < 0) {
                buf.append('-');
            }
            buf.append(Integer.toString(absYear + 10000).substring(1));
        } else {
            buf.append(year);
        }
        buf.append(month < 10 ? "-0" : "-").append(month);
        return buf.toString();
    }
}
