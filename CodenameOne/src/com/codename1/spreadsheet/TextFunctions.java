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

/// The text functions.
final class TextFunctions {
    static final String SCALAR_NAMES = "CHAR CLEAN CODE DOLLAR EXACT FIND FIXED LEFT LEN LOWER MID NUMBERVALUE PROPER"
            + " REPLACE REPT RIGHT SEARCH SUBSTITUTE TEXT TEXTAFTER TEXTBEFORE TRIM UNICHAR UNICODE UPPER VALUE";

    static final String RANGE_NAMES = "CONCAT CONCATENATE TEXTJOIN";

    /// The longest text a cell holds.
    private static final int MAX_TEXT = 32767;

    private TextFunctions() {
    }

    private static Double d(double v) {
        return Double.valueOf(v);
    }

    private static int count(Object[] a, int i, int fallback) {
        int n = Evaluator.integer(a, i, fallback);
        if (n < 0) {
            throw EvalError.VALUE;
        }
        return n;
    }

    static Object call(String name, Evaluator ev, Object[] a) {
        switch (Functions.index(SWITCH_1, name)) {
            case 0: // CHAR
            case 1: { // UNICHAR
                int code = Evaluator.integer(a, 0);
                if (code < 1 || code > ("CHAR".equals(name) ? 255 : 0x10ffff)) {
                    throw EvalError.VALUE;
                }
                StringBuilder sb = new StringBuilder();
                if (code > 0xffff) {
                    int v = code - 0x10000;
                    sb.append((char) (0xd800 + (v >> 10))).append((char) (0xdc00 + (v & 0x3ff)));
                } else {
                    sb.append((char) code);
                }
                return sb.toString();
            }
            case 2: // CODE
            case 3: { // UNICODE
                String s = Evaluator.text(a, 0);
                if (s.length() == 0) {
                    throw EvalError.VALUE;
                }
                char c = s.charAt(0);
                if (c >= 0xd800 && c < 0xdc00 && s.length() > 1 && "UNICODE".equals(name)) {
                    return d(0x10000 + ((c - 0xd800) << 10) + (s.charAt(1) - 0xdc00));
                }
                return d(c);
            }
            case 4: { // CLEAN
                String s = Evaluator.text(a, 0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < s.length(); i++) {
                    if (s.charAt(i) >= ' ') {
                        sb.append(s.charAt(i));
                    }
                }
                return sb.toString();
            }
            case 5: { // DOLLAR
                double x = Evaluator.num(a, 0);
                int places = Evaluator.integer(a, 1, 2);
                String body = "$" + fixed(Math.abs(x), places, true);
                return x < 0 ? "-" + body : body;
            }
            case 6: { // FIXED
                double x = Evaluator.num(a, 0);
                String body = fixed(Math.abs(x), Evaluator.integer(a, 1, 2), !Evaluator.bool(a, 2, false));
                return x < 0 && Values.round(x, Evaluator.integer(a, 1, 2)) != 0 ? "-" + body : body;
            }
            case 7: // EXACT
                return Boolean.valueOf(Evaluator.text(a, 0).equals(Evaluator.text(a, 1)));
            case 8: // FIND
            case 9: { // SEARCH
                String needle = Evaluator.text(a, 0);
                String hay = Evaluator.text(a, 1);
                int start = Evaluator.integer(a, 2, 1);
                if (start < 1 || start > hay.length() + 1) {
                    throw EvalError.VALUE;
                }
                int at = "FIND".equals(name) ? hay.indexOf(needle, start - 1) : search(needle, hay, start - 1);
                if (at < 0) {
                    throw EvalError.VALUE;
                }
                return d(at + 1);
            }
            case 10: { // LEFT
                String s = Evaluator.text(a, 0);
                return s.substring(0, Math.min(s.length(), count(a, 1, 1)));
            }
            case 11: { // RIGHT
                String s = Evaluator.text(a, 0);
                return s.substring(s.length() - Math.min(s.length(), count(a, 1, 1)));
            }
            case 12: { // MID
                String s = Evaluator.text(a, 0);
                int start = Evaluator.integer(a, 1);
                int n = count(a, 2, 0);
                if (start < 1) {
                    throw EvalError.VALUE;
                }
                if (start > s.length()) {
                    return "";
                }
                return s.substring(start - 1, (int) Math.min((long) s.length(), (long) start - 1 + n));
            }
            case 13: // LEN
                return d(Evaluator.text(a, 0).length());
            case 14: // LOWER
                return Evaluator.text(a, 0).toLowerCase();
            case 15: // UPPER
                return Evaluator.text(a, 0).toUpperCase();
            case 16: { // PROPER
                String s = Evaluator.text(a, 0);
                StringBuilder sb = new StringBuilder(s.length());
                boolean first = true;
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    boolean letter = Character.toUpperCase(c) != Character.toLowerCase(c);
                    sb.append(letter ? first ? Character.toUpperCase(c) : Character.toLowerCase(c) : c);
                    first = !letter;
                }
                return sb.toString();
            }
            case 17: { // REPLACE
                String s = Evaluator.text(a, 0);
                int start = Evaluator.integer(a, 1);
                int n = count(a, 2, 0);
                if (start < 1) {
                    throw EvalError.VALUE;
                }
                int from = Math.min(s.length(), start - 1);
                int to = (int) Math.min((long) s.length(), (long) from + n);
                return s.substring(0, from) + Evaluator.text(a, 3) + s.substring(to);
            }
            case 18: { // REPT
                String s = Evaluator.text(a, 0);
                int n = count(a, 1, 0);
                if ((long) s.length() * n > MAX_TEXT) {
                    throw EvalError.VALUE;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < n; i++) {
                    sb.append(s);
                }
                return sb.toString();
            }
            case 19: // SUBSTITUTE
                return substitute(a);
            case 20: { // TEXT
                Object v = Evaluator.arg(a, 0);
                return CellFormat.format(Evaluator.text(a, 1), v == Evaluator.MISSING ? null : v, ev.workbook.isDate1904()); //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
            }
            case 21: // TEXTAFTER
            case 22: // TEXTBEFORE
                return textAround(name, a);
            case 23: { // TRIM
                String s = Evaluator.text(a, 0);
                StringBuilder sb = new StringBuilder();
                boolean pending = false;
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if (c == ' ') {
                        pending = sb.length() > 0;
                    } else {
                        if (pending) {
                            sb.append(' ');
                            pending = false;
                        }
                        sb.append(c);
                    }
                }
                return sb.toString();
            }
            case 24: { // VALUE
                Object v = Evaluator.arg(a, 0);
                if (v instanceof Double) {
                    return v;
                }
                if (!(v instanceof String)) {
                    throw EvalError.VALUE;
                }
                Double parsed = Values.parseNumber((String) v);
                if (parsed == null) {
                    throw EvalError.VALUE;
                }
                return parsed;
            }
            case 25: // NUMBERVALUE
                return numberValue(a);
            case 26: { // CONCATENATE
                StringBuilder sb = new StringBuilder();
                for (Object argItem : a) {
                    append(sb, ev.scalar(argItem));
                }
                return sb.toString();
            }
            case 27: { // CONCAT
                StringBuilder sb = new StringBuilder();
                for (Object argItem : a) {
                    Object[][] m = ev.matrix(argItem);
                    for (Object[] line : m) {
                        for (Object value : line) {
                            append(sb, value);
                        }
                    }
                }
                return sb.toString();
            }
            case 28: { // TEXTJOIN
                if (a.length < 3) {
                    throw EvalError.VALUE;
                }
                Object[][] delimiters = ev.matrix(a[0]);
                boolean ignoreEmpty = Values.bool(ev.scalar(a[1]));
                StringBuilder sb = new StringBuilder();
                int joined = 0;
                for (int i = 2; i < a.length; i++) {
                    Object[][] m = ev.matrix(a[i]);
                    for (Object[] line : m) {
                        for (Object v : line) {
                            if (v instanceof CellError) {
                                return v;
                            }
                            String s = v == null ? "" : Values.text(v);
                            if (ignoreEmpty && s.length() == 0) {
                                continue;
                            }
                            if (joined > 0) {
                                int count = delimiters.length * Evaluator.width(delimiters);
                                if (count > 0) {
                                    int k = (joined - 1) % count;
                                    int w = Evaluator.width(delimiters);
                                    Object delimiter = delimiters[k / w][k % w];
                                    sb.append(delimiter == null ? "" : Values.text(delimiter));
                                }
                            }
                            sb.append(s);
                            joined++;
                            if (sb.length() > MAX_TEXT) {
                                throw EvalError.VALUE;
                            }
                        }
                    }
                }
                return sb.toString();
            }
            default:
                return CellError.NAME;
        }
    }

    private static void append(StringBuilder sb, Object v) {
        if (v instanceof CellError) {
            throw EvalError.of((CellError) v);
        }
        if (v != null && v != Evaluator.MISSING) { //NOPMD CompareObjectsWithEquals - the marker of an omitted argument is one object
            sb.append(Values.text(v));
        }
    }

    /// Where a pattern with wildcards first matches at or after a position, ignoring case.
    private static int search(String needle, String hay, int from) {
        String pattern = needle + "*";
        for (int i = from; i <= hay.length(); i++) {
            if (Criteria.wildcardMatch(pattern, 0, hay, i)) {
                return i;
            }
        }
        return -1;
    }

    /// A number with a fixed count of decimals, which may be negative to round to tens.
    static String fixed(double abs, int places, boolean grouping) {
        if (places > 127) {
            throw EvalError.VALUE;
        }
        double rounded = Values.round(abs, places);
        int decimals = Math.max(0, places);
        StringBuilder digits = new StringBuilder();
        if (rounded >= 1e15) {
            digits.append(Values.general(Math.floor(rounded)));
            decimals = 0;
        } else {
            long whole = (long) Math.floor(rounded);
            digits.append(whole);
            if (decimals > 0) {
                int shown = Math.min(decimals, 15);
                long fraction = (long) Math.floor((rounded - whole) * Values.pow10(shown) + 0.5);
                if (fraction >= (long) Values.pow10(shown)) {
                    fraction = (long) Values.pow10(shown) - 1;
                }
                String f = Long.toString(fraction);
                StringBuilder tail = new StringBuilder(".");
                for (int i = f.length(); i < shown; i++) {
                    tail.append('0');
                }
                tail.append(f);
                for (int i = shown; i < decimals; i++) {
                    tail.append('0');
                }
                if (grouping) {
                    group(digits);
                }
                return digits.append(tail).toString();
            }
        }
        if (grouping) {
            group(digits);
        }
        return digits.toString();
    }

    private static void group(StringBuilder digits) {
        for (int i = digits.length() - 3; i > 0; i -= 3) {
            digits.insert(i, ',');
        }
    }

    private static Object substitute(Object[] a) {
        String s = Evaluator.text(a, 0);
        String from = Evaluator.text(a, 1);
        String to = Evaluator.text(a, 2);
        if (from.length() == 0) {
            return s;
        }
        int instance = 0;
        if (Evaluator.has(a, 3)) {
            instance = Evaluator.integer(a, 3);
            if (instance < 1) {
                throw EvalError.VALUE;
            }
        }
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        int seen = 0;
        while (true) {
            int at = s.indexOf(from, pos);
            if (at < 0) {
                break;
            }
            seen++;
            if (instance == 0 || seen == instance) {
                sb.append(s.substring(pos, at)).append(to);
            } else {
                sb.append(s.substring(pos, at + from.length()));
            }
            pos = at + from.length();
            if (seen == instance) {
                break;
            }
        }
        sb.append(s.substring(pos));
        if (sb.length() > MAX_TEXT) {
            throw EvalError.VALUE;
        }
        return sb.toString();
    }

    private static Object textAround(String name, Object[] a) {
        String s = Evaluator.text(a, 0);
        String delimiter = Evaluator.text(a, 1);
        int instance = Evaluator.integer(a, 2, 1);
        boolean ignoreCase = Evaluator.num(a, 3, 0) != 0;
        boolean matchEnd = Evaluator.num(a, 4, 0) != 0;
        if (instance == 0 || delimiter.length() == 0) {
            throw EvalError.VALUE;
        }
        String hay = ignoreCase ? s.toLowerCase() : s;
        String needle = ignoreCase ? delimiter.toLowerCase() : delimiter;
        int at = -1;
        if (instance > 0) {
            int pos = 0;
            for (int i = 0; i < instance; i++) {
                at = hay.indexOf(needle, pos);
                if (at < 0) {
                    break;
                }
                pos = at + needle.length();
            }
            if (at < 0 && matchEnd) {
                at = s.length();
            }
        } else {
            int pos = hay.length();
            for (int i = 0; i < -instance; i++) {
                at = pos - needle.length() < 0 ? -1 : hay.lastIndexOf(needle, pos - needle.length());
                if (at < 0) {
                    break;
                }
                pos = at;
            }
            if (at < 0 && matchEnd) {
                return "TEXTAFTER".equals(name) ? s : "";
            }
        }
        if (at < 0) {
            if (Evaluator.has(a, 5)) {
                return a[5];
            }
            throw EvalError.NA;
        }
        if ("TEXTBEFORE".equals(name)) {
            return s.substring(0, at);
        }
        return at >= s.length() ? "" : s.substring(at + delimiter.length());
    }

    private static Object numberValue(Object[] a) {
        String s = Evaluator.text(a, 0);
        String decimal = Evaluator.has(a, 1) ? Evaluator.text(a, 1) : ".";
        String group = Evaluator.has(a, 2) ? Evaluator.text(a, 2) : ",";
        if (decimal.length() == 0 || group.length() == 0) {
            throw EvalError.VALUE;
        }
        char dec = decimal.charAt(0);
        char grp = group.charAt(0);
        StringBuilder sb = new StringBuilder();
        int percents = 0;
        boolean seenDecimal = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                continue;
            }
            if (c == dec) {
                if (seenDecimal) {
                    throw EvalError.VALUE;
                }
                seenDecimal = true;
                sb.append('.');
            } else if (c == grp) {
                if (seenDecimal) {
                    throw EvalError.VALUE;
                }
            } else if (c == '%') {
                percents++;
            } else {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            return d(0);
        }
        double v;
        try {
            v = Double.parseDouble(sb.toString());
        } catch (NumberFormatException e) {
            throw EvalError.VALUE;
        }
        for (int i = 0; i < percents; i++) {
            v /= 100;
        }
        return d(v);
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "CHAR", "UNICHAR", "CODE", "UNICODE", "CLEAN", "DOLLAR", "FIXED", "EXACT", "FIND",
            "SEARCH", "LEFT", "RIGHT", "MID", "LEN", "LOWER", "UPPER", "PROPER", "REPLACE", "REPT",
            "SUBSTITUTE", "TEXT", "TEXTAFTER", "TEXTBEFORE", "TRIM", "VALUE", "NUMBERVALUE",
            "CONCATENATE", "CONCAT", "TEXTJOIN");
}
