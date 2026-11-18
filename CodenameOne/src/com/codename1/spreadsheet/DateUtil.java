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
package com.codename1.spreadsheet;

import java.util.Calendar;
import java.util.Date;

/// Converts between the day numbers spreadsheets store dates as and calendar dates.
///
/// A date in a spreadsheet is a number of days, with the time of day as its fraction. In the
/// usual system day 1 is the first of January 1900 and day 60 is the 29th of February 1900,
/// a day that never was: the first spreadsheets treated 1900 as a leap year and every file
/// since keeps the numbering. The other system counts from the first of January 1904.
///
/// The arithmetic is done on plain integers: the device class library has no
/// `GregorianCalendar`.
final class DateUtil {
    private static final int EPOCH_1900 = 25569;
    private static final int EPOCH_1904 = 24107;

    private DateUtil() {
    }

    static long floorDiv(long a, long b) {
        long q = a / b;
        if ((a % b != 0) && ((a < 0) != (b < 0))) {
            q--;
        }
        return q;
    }

    /// Days from the first of January 1970 to a date of the Gregorian calendar.
    static long daysFromCivil(long y, int m, int d) {
        if (m <= 2) {
            y--;
        }
        long era = floorDiv(y, 400);
        long yoe = y - era * 400;
        long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    /// The year, month (1 to 12) and day of a number of days from the first of January 1970.
    static int[] civilFromDays(long z) {
        z += 719468;
        long era = floorDiv(z, 146097);
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        int d = (int) (doy - (153 * mp + 2) / 5 + 1);
        int m = (int) (mp < 10 ? mp + 3 : mp - 9);
        if (m <= 2) {
            y++;
        }
        return new int[] {(int) y, m, d};
    }

    /// The day number of a date. The month and the day may be out of range, as the DATE
    /// function allows: month 14 is February of the next year, day 0 the last day of the
    /// month before.
    static double serial(long year, long month, long day, boolean date1904) {
        long y = year + floorDiv(month - 1, 12);
        int m = (int) (month - 1 - floorDiv(month - 1, 12) * 12) + 1;
        long first = daysFromCivil(y, m, 1);
        if (date1904) {
            return first + day - 1 + EPOCH_1904;
        }
        // The first of the month is numbered, then the days are counted on from it in the
        // spreadsheet's own numbering: DATE(1900,2,29) and DATE(1900,3,0) are both day 60.
        long s = first + EPOCH_1900;
        if (s < 61) {
            s--;
        }
        return s + day - 1;
    }

    /// The year, month and day of a day number. Day 60 of the 1900 system answers the 29th
    /// of February 1900, as a spreadsheet does.
    static int[] fields(double serial, boolean date1904) {
        long n = (long) Math.floor(serial);
        if (date1904) {
            return civilFromDays(n - EPOCH_1904);
        }
        if (n == 60) {
            return new int[] {1900, 2, 29};
        }
        if (n < 60) {
            return civilFromDays(n - EPOCH_1900 + 1);
        }
        return civilFromDays(n - EPOCH_1900);
    }

    /// The second of the day, 0 to 86399, of a day number, rounded the way a spreadsheet
    /// displays it.
    static int secondOfDay(double serial) {
        double frac = serial - Math.floor(serial);
        int seconds = (int) Math.floor(frac * 86400 + 0.5);
        return seconds >= 86400 ? 86399 : seconds;
    }

    /// Day of the week, 1 for Sunday to 7 for Saturday.
    static int weekday(double serial, boolean date1904) {
        long n = (long) Math.floor(serial);
        // Days 1 to 59 of the 1900 system are not corrected for the phantom leap day here, on
        // purpose: a spreadsheet says the first of January 1900 was a Sunday, and it was not.
        long days = n - (date1904 ? EPOCH_1904 : EPOCH_1900);
        // the first of January 1970 was a Thursday
        long w = days + 4;
        return (int) (w - floorDiv(w, 7) * 7) + 1;
    }

    static boolean isLeapYear(long y) {
        return y % 4 == 0 && (y % 100 != 0 || y % 400 == 0);
    }

    static int daysInMonth(long y, int m) {
        switch (m) {
            case 2:
                return isLeapYear(y) ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    /// The moment a day number stands for, read as local time: a spreadsheet date has no
    /// time zone, and "the third of March at nine" means nine on the wall clock.
    static Date toDate(double serial, boolean date1904) {
        int[] f = fields(serial, date1904);
        double frac = serial - Math.floor(serial);
        long millis = (long) Math.floor(frac * 86400000d + 0.5);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, f[0]);
        cal.set(Calendar.MONTH, f[1] - 1);
        // the phantom 29th of February 1900 lands on the first of March
        cal.set(Calendar.DAY_OF_MONTH, f[2]);
        cal.set(Calendar.HOUR_OF_DAY, (int) (millis / 3600000));
        cal.set(Calendar.MINUTE, (int) (millis / 60000 % 60));
        cal.set(Calendar.SECOND, (int) (millis / 1000 % 60));
        cal.set(Calendar.MILLISECOND, (int) (millis % 1000));
        return cal.getTime();
    }

    /// The day number of a moment, read as local time.
    static double toSerial(Date date, boolean date1904) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        double day = serial(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), date1904);
        double millis = cal.get(Calendar.HOUR_OF_DAY) * 3600000d + cal.get(Calendar.MINUTE) * 60000d
                + cal.get(Calendar.SECOND) * 1000d + cal.get(Calendar.MILLISECOND);
        return day + millis / 86400000d;
    }
}
