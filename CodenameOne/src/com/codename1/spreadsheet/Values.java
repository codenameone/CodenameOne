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

import com.codename1.util.MathUtil;

/// The conversions between the kinds of value a cell holds, as a spreadsheet makes them:
/// TRUE is 1 in arithmetic, text that reads as a number is that number, and an empty cell is
/// zero or the empty text depending on who asks.
final class Values {
    private static final double[] POW10 = {
        1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18,
        1e19, 1e20, 1e21, 1e22
    };

    private Values() {
    }

    /// Ten to a power that may be negative.
    static double pow10signed(int e) {
        return e >= 0 ? pow10(e) : 1 / pow10(-e);
    }

    static double pow2(int e) {
        double r = 1;
        for (int i = 0; i < e; i++) {
            r *= 2;
        }
        return r;
    }

    static double pow10(int e) {
        if (e >= 0 && e < POW10.length) {
            return POW10[e];
        }
        if (e < 0 && -e < POW10.length) {
            return 1 / POW10[-e];
        }
        return MathUtil.pow(10, e);
    }

    /// The number a value stands for in arithmetic. Throws the error the value is, or
    /// `#VALUE!` for text that is not a number.
    static double num(Object v) {
        if (v instanceof Double) {
            return ((Double) v).doubleValue();
        }
        if (v == null || v == Evaluator.MISSING) { //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
            return 0;
        }
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue() ? 1 : 0;
        }
        if (v instanceof String) {
            Double d = parseNumber((String) v);
            if (d == null) {
                throw EvalError.VALUE;
            }
            return d.doubleValue();
        }
        if (v instanceof CellError) {
            throw EvalError.of((CellError) v);
        }
        throw EvalError.VALUE;
    }

    static String text(Object v) {
        if (v instanceof String) {
            return (String) v;
        }
        if (v == null || v == Evaluator.MISSING) { //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
            return "";
        }
        if (v instanceof Double) {
            return general(((Double) v).doubleValue());
        }
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue() ? "TRUE" : "FALSE";
        }
        if (v instanceof CellError) {
            throw EvalError.of((CellError) v);
        }
        throw EvalError.VALUE;
    }

    static boolean bool(Object v) {
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        if (v == null || v == Evaluator.MISSING) { //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
            return false;
        }
        if (v instanceof Double) {
            return ((Double) v).doubleValue() != 0;
        }
        if (v instanceof String) {
            String s = (String) v;
            if ("TRUE".equalsIgnoreCase(s)) {
                return true;
            }
            if ("FALSE".equalsIgnoreCase(s)) {
                return false;
            }
            throw EvalError.VALUE;
        }
        if (v instanceof CellError) {
            throw EvalError.of((CellError) v);
        }
        throw EvalError.VALUE;
    }

    /// Reads text as a number the way a cell does when it is typed in: `12`, `-1.5e3`, `45%`,
    /// `1,234.5`, an ISO date (`2024-03-01`, optionally with a time) or a time (`13:45`,
    /// `13:45:10`). Answers null for anything else. Dates use the 1900 system.
    static Double parseNumber(String raw) {
        String s = raw.trim();
        int n = s.length();
        if (n == 0) {
            return null;
        }
        boolean percent = false;
        if (s.charAt(n - 1) == '%') {
            percent = true;
            s = s.substring(0, n - 1).trim();
            n = s.length();
            if (n == 0) {
                return null;
            }
        }
        int i = 0;
        char c = s.charAt(0);
        if (c == '+' || c == '-') {
            i++;
        }
        int digits = 0;
        boolean grouping = false;
        while (i < n) {
            c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                digits++;
            } else if (c == ',' && digits > 0 && isThreeDigits(s, i + 1)) {
                grouping = true;
            } else {
                break;
            }
            i++;
        }
        if (i < n && s.charAt(i) == '.') {
            i++;
            while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                i++;
                digits++;
            }
        }
        if (digits > 0 && i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) {
                j++;
            }
            int expDigits = 0;
            while (j < n && s.charAt(j) >= '0' && s.charAt(j) <= '9') {
                j++;
                expDigits++;
            }
            if (expDigits > 0 && expDigits < 4) {
                i = j;
            }
        }
        if (digits > 0 && i == n) {
            if (grouping) {
                StringBuilder sb = new StringBuilder(n);
                for (int k = 0; k < n; k++) {
                    if (s.charAt(k) != ',') {
                        sb.append(s.charAt(k));
                    }
                }
                s = sb.toString();
            }
            if (!isDecimal(s)) {
                return null;
            }
            try {
                double d = Double.parseDouble(s);
                return Double.valueOf(percent ? d / 100 : d);
            } catch (NumberFormatException err) {
                return null;
            }
        }
        if (percent) {
            return null;
        }
        return parseDateTime(s);
    }

    private static boolean isThreeDigits(String s, int from) {
        if (from + 3 > s.length()) {
            return false;
        }
        for (int i = from; i < from + 3; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return from + 3 == s.length() || s.charAt(from + 3) < '0' || s.charAt(from + 3) > '9';
    }

    /// Whether text names a day, and not only a time of day or a number.
    static boolean isDateText(String text) {
        String s = text.trim();
        return namedDate(s, new int[1]) >= 0 || s.length() >= 8 && s.indexOf('-') == 4 && parseDateTime(s) != null;
    }

    /// `yyyy-mm-dd`, `yyyy-mm-ddThh:mm[:ss]`, `yyyy-mm-dd hh:mm[:ss]`, `hh:mm[:ss[.fff]]`.
    /// The day is numbered in the 1900 system.
    static Double parseDateTime(String s) {
        int n = s.length();
        double day = 0;
        int i = 0;
        boolean hasDate = false;
        int[] named = {0};
        double byName = namedDate(s, named);
        if (byName >= 0) {
            day = byName;
            i = named[0];
            hasDate = true;
            if (i == n) {
                return Double.valueOf(day);
            }
            if (s.charAt(i) != ' ') {
                return null;
            }
            i++;
        } else if (n >= 8 && s.indexOf('-') == 4) {
            int[] pos = {0};
            int y = digits(s, pos, 4);
            if (y < 0 || !eat(s, pos, '-')) {
                return null;
            }
            int m = digits(s, pos, 2);
            if (m < 1 || m > 12 || !eat(s, pos, '-')) {
                return null;
            }
            int d = digits(s, pos, 2);
            if (d < 1 || d > DateUtil.daysInMonth(y, m) || y < 1900) {
                return null;
            }
            day = DateUtil.serial(y, m, d, false);
            i = pos[0];
            hasDate = true;
            if (i == n) {
                return Double.valueOf(day);
            }
            if (s.charAt(i) != 'T' && s.charAt(i) != ' ') {
                return null;
            }
            i++;
        }
        int[] pos = {i};
        int h = digits(s, pos, 2);
        if (h < 0 || !eat(s, pos, ':')) {
            return null;
        }
        int min = digits(s, pos, 2);
        if (min < 0 || min > 59) {
            return null;
        }
        double sec = 0;
        if (eat(s, pos, ':')) {
            int whole = digits(s, pos, 2);
            if (whole < 0 || whole > 59) {
                return null;
            }
            sec = whole;
            if (eat(s, pos, '.')) {
                int start = pos[0];
                int frac = digits(s, pos, 9);
                if (frac < 0) {
                    return null;
                }
                sec += frac / pow10(pos[0] - start);
            }
        }
        // a trailing Z or offset is accepted and ignored: the cell holds wall-clock time
        if (pos[0] < n && hasDate && s.charAt(pos[0]) == 'Z') {
            pos[0]++;
        }
        // "6:35 AM", "6:35pm", "6:35 p": the hour is then one of a twelve hour clock
        int meridiem = pos[0] < n && s.charAt(pos[0]) == ' ' ? pos[0] + 1 : pos[0];
        if (meridiem < n && n - meridiem <= 2 && (n - meridiem == 1 || s.charAt(n - 1) == 'M' || s.charAt(n - 1) == 'm')) {
            char half = s.charAt(meridiem);
            boolean pm = half == 'P' || half == 'p';
            if (!pm && half != 'A' && half != 'a' || h > 12) {
                return null;
            }
            h = h == 12 ? 0 : h;
            if (pm) {
                h += 12;
            }
            pos[0] = n;
        }
        if (pos[0] != n || h > 23 && hasDate) {
            return null;
        }
        return Double.valueOf(day + (h * 3600 + min * 60 + sec) / 86400);
    }

    private static final String[] MONTHS = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST",
        "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};

    private static int month(String s, int[] pos) {
        int i = pos[0];
        int start = i;
        while (i < s.length() && (s.charAt(i) >= 'A' && s.charAt(i) <= 'Z' || s.charAt(i) >= 'a' && s.charAt(i) <= 'z')) {
            i++;
        }
        int len = i - start;
        for (int m = 0; m < 12 && len >= 3; m++) {
            if ((len == 3 || len == MONTHS[m].length()) && s.regionMatches(true, start, MONTHS[m], 0, len)) {
                pos[0] = i;
                return m + 1;
            }
        }
        return -1;
    }

    /// The day number of a date that names its month in English, `2-Jan-2020`,
    /// `2 January 2020` or `Jan 2, 2020`, and -1 for anything else. `pos` is left after it.
    ///
    /// A date of digits and slashes is not read, here or anywhere: 1/2/2020 is the first of
    /// February to half the world and the second of January to the other half, and a file
    /// does not say which it was written for. `yyyy-mm-dd` and a named month mean one day.
    static double namedDate(String s, int[] pos) {
        if (s.length() < 8) {
            return -1;
        }
        int d;
        int m;
        if (s.charAt(0) >= '0' && s.charAt(0) <= '9') {
            d = digits(s, pos, 2);
            if (!eat(s, pos, '-') && !eat(s, pos, ' ')) {
                return -1;
            }
            m = month(s, pos);
            if (m < 0 || !eat(s, pos, '-') && !eat(s, pos, ' ')) {
                return -1;
            }
        } else {
            m = month(s, pos);
            if (m < 0 || !eat(s, pos, ' ')) {
                return -1;
            }
            d = digits(s, pos, 2);
            if (d < 0 || !eat(s, pos, ',')) {
                return -1;
            }
            eat(s, pos, ' ');
        }
        int start = pos[0];
        int y = digits(s, pos, 4);
        if (pos[0] - start != 4 || y < 1900 || d < 1 || d > DateUtil.daysInMonth(y, m)) {
            return -1;
        }
        return DateUtil.serial(y, m, d, false);
    }

    private static boolean eat(String s, int[] pos, char c) {
        if (pos[0] < s.length() && s.charAt(pos[0]) == c) {
            pos[0]++;
            return true;
        }
        return false;
    }

    /// Reads up to `max` digits, answers -1 when there is none.
    private static int digits(String s, int[] pos, int max) {
        int i = pos[0];
        int start = i;
        int v = 0;
        while (i < s.length() && i - start < max && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            v = v * 10 + (s.charAt(i) - '0');
            i++;
        }
        if (i == start) {
            return -1;
        }
        pos[0] = i;
        return v;
    }

    /// A number as the General format writes it: at most 15 significant digits, no trailing
    /// zeros, scientific notation only for the very large and the very small. The digits are
    /// produced here, not by `Double.toString`: 0.1+0.2 has to read `0.3`, and the same on
    /// every platform.
    static String general(double d) {
        if (d != 0 && Math.abs(d) < 1e-300) {
            // Scaling the digits out of a number this small overflows a long, and multiplying
            // it into range loses the few bits it has. Its shortest decimal form is exact
            // enough: the mantissa goes through General for the rounding, the exponent as is.
            String text = Double.toString(d);
            int e = text.indexOf('E');
            if (e > 0) {
                double mantissa = Double.parseDouble(text.substring(0, e));
                int exponent = Integer.parseInt(text.substring(e + 1));
                String digits = general(mantissa);
                if (Math.abs(Double.parseDouble(digits)) >= 10) {
                    // 9.999999999999999 rounds to 10 in fifteen digits
                    digits = general(mantissa / 10);
                    exponent++;
                }
                return digits + "E" + exponent;
            }
        }
        if (d == 0) {
            return "0";
        }
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return CellError.NUM.toString();
        }
        StringBuilder sb = new StringBuilder(24);
        if (d < 0) {
            sb.append('-');
            d = -d;
        }
        if (d < 1e15 && d == Math.floor(d)) {
            sb.append((long) d);
            return sb.toString();
        }
        int[] exp = new int[1];
        long mant = significand(d, 15, exp);
        int e = exp[0];
        String digits = Long.toString(mant);
        int last = digits.length();
        while (last > 1 && digits.charAt(last - 1) == '0') {
            last--;
        }
        if (e >= 15 || e < -9) {
            sb.append(digits.charAt(0));
            if (last > 1) {
                sb.append('.').append(digits.substring(1, last));
            }
            sb.append('E').append(e < 0 ? '-' : '+');
            int abs = Math.abs(e);
            if (abs < 10) {
                sb.append('0');
            }
            sb.append(abs);
            return sb.toString();
        }
        if (e < 0) {
            sb.append("0.");
            for (int i = -1; i > e; i--) {
                sb.append('0');
            }
            sb.append(digits.substring(0, last));
            return sb.toString();
        }
        if (last <= e + 1) {
            sb.append(digits.substring(0, e + 1));
        } else {
            sb.append(digits.substring(0, e + 1)).append('.').append(digits.substring(e + 1, last));
        }
        return sb.toString();
    }

    /// The first `count` significant decimal digits of a positive number as an integer, and
    /// in `exp[0]` the power of ten of its first digit.
    static long significand(double d, int count, int[] exp) {
        int e = (int) Math.floor(MathUtil.log10(d));
        long mant = scaled(d, count - 1 - e);
        long limit = (long) pow10(count);
        if (mant >= limit) {
            e++;
            mant = scaled(d, count - 1 - e);
        } else if (mant < limit / 10) {
            e--;
            mant = scaled(d, count - 1 - e);
        }
        exp[0] = e;
        return mant;
    }

    private static long scaled(double d, int power) {
        double v;
        if (power >= 0) {
            v = power <= 22 ? d * POW10[power] : d * POW10[22] * pow10(power - 22);
        } else {
            v = -power <= 22 ? d / POW10[-power] : d / POW10[22] / pow10(-power - 22);
        }
        return (long) Math.floor(v + 0.5);
    }

    /// Rounds half away from zero at a decimal place, the rounding of ROUND and of number
    /// formats. Scaling by a power of ten first would turn 1.005 into 100.49999999999999;
    /// the nudge by one part in 2^50 undoes exactly that representation error.
    static double round(double d, int places) {
        if (d == 0 || Double.isNaN(d) || Double.isInfinite(d)) {
            return d;
        }
        if (places > 15) {
            return d;
        }
        double abs = Math.abs(d);
        double scaled;
        if (places >= 0) {
            scaled = abs * pow10(places);
        } else {
            scaled = abs / pow10(-places);
        }
        if (scaled >= 9.007199254740992E15) {
            return d;
        }
        // The nudge is a share of the number, and for a number of fifteen digits that share
        // would be a half by itself: 999999999999999 is whole and stays what it is.
        double r = scaled == Math.floor(scaled) ? scaled : Math.floor(scaled + Math.min(scaled * 8.9e-16, 0.1) + 0.5);
        double result = places >= 0 ? r / pow10(places) : r * pow10(-places);
        return d < 0 ? -result : result;
    }

    /// Compares two values for sorting and for the comparison operators: numbers sort before
    /// text, text before logical values, and text ignores case.
    static int compare(Object a, Object b) {
        if (a == Evaluator.MISSING) { //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
            a = null;
        }
        if (b == Evaluator.MISSING) { //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
            b = null;
        }
        if (a == null && b == null) {
            return 0;
        }
        // an empty cell is the zero of whatever it is compared with
        if (a == null) {
            a = b instanceof String ? (Object) "" : b instanceof Boolean ? (Object) Boolean.FALSE : (Object) Double.valueOf(0);
        }
        if (b == null) {
            b = a instanceof String ? (Object) "" : a instanceof Boolean ? (Object) Boolean.FALSE : (Object) Double.valueOf(0);
        }
        int ra = rank(a);
        int rb = rank(b);
        if (ra != rb) {
            return ra < rb ? -1 : 1;
        }
        if (a instanceof Double) {
            double x = ((Double) a).doubleValue();
            double y = ((Double) b).doubleValue();
            // Two numbers that agree in their fifteen digits are the same number to a
            // spreadsheet: 0.1+0.2=0.3 is true there, and it shows both as 0.3.
            if (Math.abs(x - y) <= Math.max(Math.abs(x), Math.abs(y)) * 1e-15) {
                return 0;
            }
            return x < y ? -1 : 1;
        }
        if (a instanceof String) {
            return compareText((String) a, (String) b);
        }
        if (a instanceof Boolean) {
            boolean x = ((Boolean) a).booleanValue();
            boolean y = ((Boolean) b).booleanValue();
            return x == y ? 0 : x ? 1 : -1;
        }
        return 0;
    }

    private static int rank(Object v) {
        if (v instanceof Double) {
            return 0;
        }
        if (v instanceof String) {
            return 1;
        }
        if (v instanceof Boolean) {
            return 2;
        }
        return 3;
    }

    static int compareText(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            char x = Character.toUpperCase(a.charAt(i));
            char y = Character.toUpperCase(b.charAt(i));
            if (x != y) {
                return x < y ? -1 : 1;
            }
        }
        return a.length() == b.length() ? 0 : a.length() < b.length() ? -1 : 1;
    }

    static boolean equalsIgnoreCase(Object a, Object b) {
        return compare(a, b) == 0;
    }

    /// Whether text is a number in decimal notation: a sign, digits with at most one point,
    /// and an exponent. `Double.parseDouble` also takes `0x1p3`, `1d` and `NaN`, which no
    /// spreadsheet writes as a number, and a device's class library may read them otherwise.
    static boolean isDecimal(String s) {
        int n = s.length();
        int i = 0;
        if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            i++;
        }
        int digits = 0;
        boolean point = false;
        for (; i < n; i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                digits++;
            } else if (c == '.' && !point) {
                point = true;
            } else {
                break;
            }
        }
        if (digits == 0) {
            return false;
        }
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            i++;
            if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                i++;
            }
            int exponent = 0;
            for (; i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9'; i++) {
                exponent++;
            }
            if (exponent == 0) {
                return false;
            }
        }
        return i == n;
    }
}
