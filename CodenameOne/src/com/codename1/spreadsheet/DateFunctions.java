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

import java.util.Date;

/// The date and time functions. A date is a day number, and the time of day its fraction.
final class DateFunctions {
    static final String SCALAR_NAMES = "DATE DATEDIF DATEVALUE DAY DAYS DAYS360 EDATE EOMONTH HOUR ISOWEEKNUM MINUTE"
            + " MONTH NOW SECOND TIME TIMEVALUE TODAY WEEKDAY WEEKNUM YEAR YEARFRAC";

    static final String RANGE_NAMES = "NETWORKDAYS NETWORKDAYS.INTL WORKDAY WORKDAY.INTL";

    /// The weekend patterns NETWORKDAYS.INTL names by number, Monday first.
    private static final String[] WEEKENDS = {"0000011", "1000001", "1100000", "0110000", "0011000", "0001100",
        "0000110"};

    private DateFunctions() {
    }

    private static Double d(double v) {
        return Double.valueOf(v);
    }

    private static double date(Object[] a, int i) {
        double v = Evaluator.num(a, i);
        if (v < 0 || v >= 2958466) {
            throw EvalError.NUM;
        }
        return v;
    }

    private static final int DAYS_TO_1904 = 1462;
    private static final double LAST_DAY = 2958465;

    /// Text that names a day is read in the 1900 numbering; a workbook that counts from 1904
    /// has to have it in its own, or `YEAR("2024-03-15")` answers 2028. Only the date
    /// functions do this: text added to a number in such a workbook is still read in the
    /// 1900 numbering.
    private static Object[] in1904(Object[] a) {
        Object[] out = a;
        for (int i = 0; i < a.length; i++) {
            if (a[i] instanceof String && Values.isDateText((String) a[i])) {
                Double d = Values.parseDateTime(((String) a[i]).trim());
                if (d != null) {
                    if (out == a) { //NOPMD CompareObjectsWithEquals - whether the copy was made yet
                        out = new Object[a.length];
                        System.arraycopy(a, 0, out, 0, a.length);
                    }
                    out[i] = Double.valueOf(d.doubleValue() - DAYS_TO_1904);
                }
            }
        }
        return out;
    }

    static Object call(String name, Evaluator ev, Object[] a) {
        boolean d1904 = ev.workbook.isDate1904();
        if (d1904 && !"DATEVALUE".equals(name) && !"TIMEVALUE".equals(name)) {
            a = in1904(a);
        }
        switch (Functions.index(SWITCH_1, name)) {
            case 0: { // DATE
                double year = Math.floor(Evaluator.num(a, 0));
                if (year < 0 || year > 9999) {
                    throw EvalError.NUM;
                }
                if (year < 1900) {
                    year += 1900;
                }
                double s = DateUtil.serial((long) year, (long) Math.floor(Evaluator.num(a, 1)),
                        (long) Math.floor(Evaluator.num(a, 2)), d1904);
                if (s < 0 || s >= 2958466) {
                    throw EvalError.NUM;
                }
                return d(s);
            }
            case 1: // DATEVALUE
            case 2: { // TIMEVALUE
                Double v = Values.parseDateTime(Evaluator.text(a, 0).trim());
                if (v == null) {
                    throw EvalError.VALUE;
                }
                double s = v.doubleValue();
                if (d1904 && Values.isDateText(Evaluator.text(a, 0))) {
                    s -= DAYS_TO_1904;
                }
                return d("DATEVALUE".equals(name) ? Math.floor(s) : s - Math.floor(s));
            }
            case 3: // YEAR
                return d(DateUtil.fields(date(a, 0), d1904)[0]);
            case 4: // MONTH
                return d(DateUtil.fields(date(a, 0), d1904)[1]);
            case 5: // DAY
                return d(DateUtil.fields(date(a, 0), d1904)[2]);
            case 6: // HOUR
                return d(Math.floor(DateUtil.secondOfDay(date(a, 0)) / 3600d));
            case 7: // MINUTE
                return d(DateUtil.secondOfDay(date(a, 0)) / 60 % 60);
            case 8: // SECOND
                return d(DateUtil.secondOfDay(date(a, 0)) % 60);
            case 9: { // TIME
                double seconds = Math.floor(Evaluator.num(a, 0)) * 3600 + Math.floor(Evaluator.num(a, 1)) * 60
                        + Math.floor(Evaluator.num(a, 2));
                if (seconds < 0) {
                    throw EvalError.NUM;
                }
                double days = seconds / 86400;
                return d(days - Math.floor(days));
            }
            case 10: // NOW
                return d(DateUtil.toSerial(new Date(), d1904));
            case 11: // TODAY
                return d(Math.floor(DateUtil.toSerial(new Date(), d1904)));
            case 12: // DAYS
                return d(Math.floor(date(a, 0)) - Math.floor(date(a, 1)));
            case 13: { // DAYS360
                int[] s = DateUtil.fields(date(a, 0), d1904);
                int[] e = DateUtil.fields(date(a, 1), d1904);
                return d(days360(s, e, Evaluator.bool(a, 2, false)));
            }
            case 14: // EDATE
            case 15: { // EOMONTH
                int[] f = DateUtil.fields(date(a, 0), d1904);
                long months = (long) Evaluator.num(a, 1);
                if ("EOMONTH".equals(name)) {
                    return d(DateUtil.serial(f[0], f[1] + months + 1, 0, d1904));
                }
                long index = (long) f[0] * 12 + f[1] - 1 + months;
                long y = DateUtil.floorDiv(index, 12);
                int m = (int) (index - y * 12) + 1;
                double result = DateUtil.serial(y, m, Math.min(f[2], DateUtil.daysInMonth(y, m)), d1904);
                if (result < 0) {
                    throw EvalError.NUM;
                }
                return d(result);
            }
            case 16: { // WEEKDAY
                int w = DateUtil.weekday(date(a, 0), d1904);
                int type = Evaluator.integer(a, 1, 1);
                if (type == 1 || type == 17) {
                    return d(w);
                }
                if (type == 2 || type == 11) {
                    return d((w + 5) % 7 + 1);
                }
                if (type == 3) {
                    return d((w + 5) % 7);
                }
                if (type >= 12 && type <= 16) {
                    // 12 starts the week on Tuesday, 16 on Saturday
                    return d((w + 16 - type) % 7 + 1);
                }
                throw EvalError.NUM;
            }
            case 17: { // WEEKNUM
                double s = Math.floor(date(a, 0));
                int type = Evaluator.integer(a, 1, 1);
                if (type == 21) {
                    return d(isoWeek(s, d1904));
                }
                int first;
                if (type == 1 || type == 17) {
                    first = 1;
                } else if (type == 2 || type == 11) {
                    first = 2;
                } else if (type >= 12 && type <= 16) {
                    first = type - 9;
                } else {
                    throw EvalError.NUM;
                }
                int year = DateUtil.fields(s, d1904)[0];
                double jan1 = DateUtil.serial(year, 1, 1, d1904);
                int offset = (DateUtil.weekday(jan1, d1904) - first + 7) % 7;
                return d(Math.floor((s - jan1 + offset) / 7) + 1);
            }
            case 18: // ISOWEEKNUM
                return d(isoWeek(Math.floor(date(a, 0)), d1904));
            case 19: // YEARFRAC
                return d(yearFrac(Math.floor(date(a, 0)), Math.floor(date(a, 1)), Evaluator.integer(a, 2, 0), d1904));
            case 20: // DATEDIF
                return dateDif(Math.floor(date(a, 0)), Math.floor(date(a, 1)), Evaluator.text(a, 2), d1904);
            case 21: // NETWORKDAYS
            case 22: // NETWORKDAYS.INTL
            case 23: // WORKDAY
            case 24: // WORKDAY.INTL
                return workdays(name, ev, a, d1904);
            default:
                return CellError.NAME;
        }
    }

    static double days360(int[] s, int[] e, boolean european) {
        int sd = s[2];
        int ed = e[2];
        int sm = s[1];
        int em = e[1];
        int sy = s[0];
        int ey = e[0];
        if (european) {
            sd = Math.min(sd, 30);
            ed = Math.min(ed, 30);
        } else {
            boolean startIsLast = sd == DateUtil.daysInMonth(sy, sm);
            if (startIsLast) {
                sd = 30;
            }
            // Only the 31st is moved. The rule is written as "the last day of a month", but
            // spreadsheets leave the end of February where it is, and files hold their answer.
            if (ed == 31) {
                if (sd < 30) {
                    ed = 1;
                    em++;
                } else {
                    ed = 30;
                }
            }
        }
        return (ey - sy) * 360d + (em - sm) * 30d + (ed - sd);
    }

    private static double isoWeek(double serial, boolean d1904) {
        // the week belongs to the year of its Thursday
        int monday0 = (DateUtil.weekday(serial, d1904) + 5) % 7;
        double thursday = serial - monday0 + 3;
        int year = DateUtil.fields(thursday, d1904)[0];
        double jan1 = DateUtil.serial(year, 1, 1, d1904);
        return Math.floor((thursday - jan1) / 7) + 1;
    }

    static double yearFrac(double start, double end, int basis, boolean d1904) {
        if (start > end) {
            double t = start;
            start = end;
            end = t;
        }
        int[] s = DateUtil.fields(start, d1904);
        int[] e = DateUtil.fields(end, d1904);
        switch (basis) {
            case 0:
                return days360(s, e, false) / 360;
            case 1: {
                double days = end - start;
                if (s[0] == e[0] || s[0] + 1 == e[0] && (e[1] < s[1] || e[1] == s[1] && e[2] <= s[2])) {
                    // no more than a year apart: a leap day inside the span makes it a 366 day year
                    boolean leap = false;
                    if (s[0] == e[0]) {
                        leap = DateUtil.isLeapYear(s[0]);
                    } else {
                        if (DateUtil.isLeapYear(s[0]) && (s[1] < 2 || s[1] == 2 && s[2] <= 29)) {
                            leap = true;
                        }
                        if (DateUtil.isLeapYear(e[0]) && (e[1] > 2 || e[1] == 2 && e[2] == 29)) {
                            leap = true;
                        }
                    }
                    return days / (leap ? 366 : 365);
                }
                double total = 0;
                for (int y = s[0]; y <= e[0]; y++) {
                    total += DateUtil.isLeapYear(y) ? 366 : 365;
                }
                return days / (total / (e[0] - s[0] + 1));
            }
            case 2:
                return (end - start) / 360;
            case 3:
                return (end - start) / 365;
            case 4:
                return days360(s, e, true) / 360;
            default:
                throw EvalError.NUM;
        }
    }

    private static Object dateDif(double start, double end, String unit, boolean d1904) {
        if (start > end) {
            throw EvalError.NUM;
        }
        int[] s = DateUtil.fields(start, d1904);
        int[] e = DateUtil.fields(end, d1904);
        int months = (e[0] - s[0]) * 12 + e[1] - s[1];
        if (e[2] < s[2]) {
            months--;
        }
        String u = FormulaParser.asciiUpper(unit);
        switch (Functions.index(SWITCH_2, u)) {
            case 0: // D
                return d(end - start);
            case 1: // M
                return d(months);
            case 2: // Y
                return d(Math.floor(months / 12d));
            case 3: // YM
                return d(months % 12);
            case 4: { // MD
                if (e[2] >= s[2]) {
                    return d(e[2] - s[2]);
                }
                // counted from the start day in the month before the end
                return d(end - DateUtil.serial(e[0], e[1] - 1, s[2], d1904));
            }
            case 5: { // YD
                double anniversary = DateUtil.serial(e[0], s[1], s[2], d1904);
                if (anniversary > end) {
                    anniversary = DateUtil.serial(e[0] - 1, s[1], s[2], d1904);
                }
                return d(end - anniversary);
            }
            default:
                throw EvalError.NUM;
        }
    }

    /// The days of the week that are not worked, Monday first.
    private static boolean[] weekend(Evaluator ev, Object arg) {
        String mask = WEEKENDS[0];
        Object v = arg == Evaluator.MISSING ? null : ev.scalar(arg); //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
        if (v instanceof String) {
            mask = (String) v;
            if (mask.length() != 7 || "1111111".equals(mask)) {
                throw EvalError.VALUE;
            }
        } else if (v != null) {
            int code = (int) Values.num(v);
            if (code >= 1 && code <= 7) {
                mask = WEEKENDS[code - 1];
            } else if (code >= 11 && code <= 17) {
                // one day off: 11 is Sunday, 12 Monday
                int day = (code - 12 + 7) % 7;
                StringBuilder sb = new StringBuilder("0000000");
                sb.setCharAt(day, '1');
                mask = sb.toString();
            } else {
                throw EvalError.NUM;
            }
        }
        boolean[] off = new boolean[7];
        for (int i = 0; i < 7; i++) {
            char c = mask.charAt(i);
            if (c != '0' && c != '1') {
                throw EvalError.VALUE;
            }
            off[i] = c == '1';
        }
        return off;
    }

    private static Object workdays(String name, Evaluator ev, Object[] a, boolean d1904) {
        boolean intl = name.endsWith(".INTL");
        double start = Math.floor(ev.scalarNum(a, 0));
        double second = ev.scalarNum(a, 1);
        // Past the last day there is, the 31st of December 9999, adding one to a day number
        // is lost in rounding and a loop over the days never ends.
        if (start < 0 || start > LAST_DAY || name.startsWith("NETWORKDAYS") && (second < 0 || second > LAST_DAY)) {
            throw EvalError.NUM;
        }
        boolean[] off = weekend(ev, intl ? Evaluator.arg(a, 2) : Evaluator.MISSING);
        DoubleList holidays = new DoubleList();
        int h = intl ? 3 : 2;
        if (Evaluator.has(a, h) && a[h] instanceof Object[][]) {
            // An array written into the formula lists its holidays as text as often as as
            // numbers, {"2026-01-01","2026-12-25"}, and a spreadsheet reads each one as the
            // date it spells. A range is different: text in a cell is skipped there.
            for (Object[] line : (Object[][]) a[h]) {
                for (Object value : line) {
                    if (value != null) {
                        holidays.add(Values.num(value));
                    }
                }
            }
        } else if (Evaluator.has(a, h)) {
            ev.numbers(a[h], holidays, false);
        }
        double[] days = holidays.sorted();
        for (int i = 0; i < days.length; i++) {
            days[i] = Math.floor(days[i]);
        }
        if (name.startsWith("NETWORKDAYS")) {
            double end = Math.floor(second);
            double from = Math.min(start, end);
            double to = Math.max(start, end);
            if (to - from > 3660000) {
                throw EvalError.NUM;
            }
            int count = 0;
            for (double day = from; day <= to; day++) {
                if (isWorked(day, off, days, d1904)) {
                    count++;
                }
            }
            return d(start <= end ? count : -count);
        }
        int remaining = (int) second;
        int step = remaining < 0 ? -1 : 1;
        double day = start;
        int guard = 0;
        while (remaining != 0) {
            day += step;
            if (isWorked(day, off, days, d1904)) {
                remaining -= step;
            }
            guard++;
            if (guard > 4000000 || day < 0) {
                throw EvalError.NUM;
            }
        }
        return d(day);
    }

    private static boolean isWorked(double day, boolean[] off, double[] holidays, boolean d1904) {
        if (off[(DateUtil.weekday(day, d1904) + 5) % 7]) {
            return false;
        }
        for (double holiday : holidays) {
            if (holiday == day) {
                return false;
            }
        }
        return true;
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "DATE", "DATEVALUE", "TIMEVALUE", "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND",
            "TIME", "NOW", "TODAY", "DAYS", "DAYS360", "EDATE", "EOMONTH", "WEEKDAY", "WEEKNUM",
            "ISOWEEKNUM", "YEARFRAC", "DATEDIF", "NETWORKDAYS", "NETWORKDAYS.INTL", "WORKDAY",
            "WORKDAY.INTL");

    private static final java.util.HashMap<String, Integer> SWITCH_2 = Functions.switchTable(
            "D", "M", "Y", "YM", "MD", "YD");
}
