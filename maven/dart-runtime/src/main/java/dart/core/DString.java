/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

/**
 * Static helpers implementing Dart's String API over java.lang.String
 * (used directly — no wrapper allocation). The transpiler maps Dart String
 * members that have no direct java.lang.String equivalent to these.
 */
public final class DString {

    private DString() {
    }

    public static long length(String s) {
        return s.length();
    }

    public static boolean isEmpty(String s) {
        return s.isEmpty();
    }

    public static boolean isNotEmpty(String s) {
        return !s.isEmpty();
    }

    /** Dart's s[i] — single-character string. */
    public static String idx(String s, long index) {
        RangeError.checkValidIndex(index, s.length());
        return String.valueOf(s.charAt((int) index));
    }

    public static long codeUnitAt(String s, long index) {
        RangeError.checkValidIndex(index, s.length());
        return s.charAt((int) index);
    }

    /** Dart's s * n operator — repeat. */
    public static String repeat(String s, long times) {
        if (times <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (long i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static String substring(String s, long start) {
        RangeError.checkValueInInterval(start, 0, s.length(), "start");
        return s.substring((int) start);
    }

    public static String substring(String s, long start, long end) {
        RangeError.checkValueInInterval(start, 0, s.length(), "start");
        RangeError.checkValueInInterval(end, start, s.length(), "end");
        return s.substring((int) start, (int) end);
    }

    public static String padLeft(String s, long width, String padding) {
        StringBuilder sb = new StringBuilder();
        for (long i = s.length(); i < width; i++) {
            sb.append(padding);
        }
        return sb.append(s).toString();
    }

    public static String padLeft(String s, long width) {
        return padLeft(s, width, " ");
    }

    public static String padRight(String s, long width, String padding) {
        StringBuilder sb = new StringBuilder(s);
        for (long i = s.length(); i < width; i++) {
            sb.append(padding);
        }
        return sb.toString();
    }

    public static String padRight(String s, long width) {
        return padRight(s, width, " ");
    }

    public static DartList<String> split(String s, String pattern) {
        DartList<String> out = new DartList<>();
        if (pattern.isEmpty()) {
            for (int i = 0; i < s.length(); i++) {
                out.add(String.valueOf(s.charAt(i)));
            }
            return out;
        }
        int start = 0;
        int i;
        while ((i = s.indexOf(pattern, start)) >= 0) {
            out.add(s.substring(start, i));
            start = i + pattern.length();
        }
        out.add(s.substring(start));
        return out;
    }

    /**
     * Dart's {@code String.split(Pattern)} for a RegExp, ported from the Dart
     * VM's own algorithm so the edge cases agree: an empty input that the
     * pattern matches splits into no parts at all, an empty match at the
     * current split point is skipped rather than producing an empty part, and a
     * match at the very end ends the split. Dart declares split (and the other
     * Pattern methods below) with a Pattern, so {@code s.split(RegExp(','))} is
     * ordinary source; with only the String overload it generated a call javac
     * rejected.
     */
    public static DartList<String> split(String s, RegExp pattern) {
        DartList<String> out = new DartList<>();
        int length = s.length();
        java.util.Iterator<RegExpMatch> matches = pattern.allMatches(s).iterator();
        if (length == 0 && matches.hasNext()) {
            return out;
        }
        int startIndex = 0;
        int previousIndex = 0;
        while (true) {
            if (startIndex == length || !matches.hasNext()) {
                out.add(s.substring(previousIndex, length));
                break;
            }
            RegExpMatch match = matches.next();
            if (match.start() == length) {
                out.add(s.substring(previousIndex, length));
                break;
            }
            int endIndex = (int) match.end();
            if (startIndex == endIndex && endIndex == previousIndex) {
                ++startIndex;
                continue;
            }
            out.add(s.substring(previousIndex, (int) match.start()));
            startIndex = previousIndex = endIndex;
        }
        return out;
    }

    public static long indexOf(String s, String other) {
        return s.indexOf(other);
    }

    public static long indexOf(String s, String other, long start) {
        // Dart range-checks the start; Java clamps a negative one to 0 and answers
        // -1 past the end, so 'abc'.indexOf('a', -1) found the 'a'.
        RangeError.checkValueInInterval(start, 0, s.length(), "start");
        return s.indexOf(other, (int) start);
    }

    public static long lastIndexOf(String s, String other) {
        return s.lastIndexOf(other);
    }

    /** Dart's {@code indexOf(Pattern)} for a RegExp: where the first match starts. */
    public static long indexOf(String s, RegExp pattern) {
        return indexOf(s, pattern, 0);
    }

    /** Dart's {@code indexOf(Pattern, start)}: the first match found searching from start. */
    public static long indexOf(String s, RegExp pattern, long start) {
        RangeError.checkValueInInterval(start, 0, s.length(), "start");
        RegExpMatch m = pattern.matchFrom(s, (int) start);
        return m == null ? -1 : m.start();
    }

    /**
     * Dart's {@code lastIndexOf(Pattern)} for a RegExp: the greatest position
     * at which the pattern matches starting exactly there, as Dart defines it.
     */
    public static long lastIndexOf(String s, RegExp pattern) {
        for (int i = s.length(); i >= 0; i--) {
            RegExpMatch m = pattern.matchFrom(s, i);
            if (m != null && m.start() == i) {
                return i;
            }
        }
        return -1;
    }

    /** Dart's {@code contains(Pattern)} for a RegExp. */
    public static boolean contains(String s, RegExp pattern) {
        return pattern.hasMatch(s);
    }

    /**
     * Dart's {@code replaceAll(Pattern, String)} for a RegExp. The replacement
     * is literal -- Dart does not expand group references here; that is
     * replaceAllMapped's job -- so every match, empty ones included, is swapped
     * for the same text: {@code 'abc'.replaceAll(RegExp(''), '-')} is -a-b-c-.
     */
    public static String replaceAll(String s, RegExp pattern, String to) {
        StringBuilder sb = new StringBuilder();
        int last = 0;
        for (RegExpMatch m : pattern.allMatches(s)) {
            sb.append(s, last, (int) m.start()).append(to);
            last = (int) m.end();
        }
        return sb.append(s, last, s.length()).toString();
    }

    /** Dart's {@code replaceFirst(Pattern, String)} for a RegExp; the replacement is literal. */
    public static String replaceFirst(String s, RegExp pattern, String to) {
        RegExpMatch m = pattern.firstMatch(s);
        if (m == null) {
            return s;
        }
        return s.substring(0, (int) m.start()) + to + s.substring((int) m.end());
    }

    public static boolean contains(String s, String other) {
        return s.contains(other);
    }

    public static String replaceAll(String s, String from, String to) {
        // Dart's replaceAll on a String pattern is literal, like Java's replace.
        return s.replace(from, to);
    }

    public static String replaceFirst(String s, String from, String to) {
        int i = s.indexOf(from);
        if (i < 0) {
            return s;
        }
        return s.substring(0, i) + to + s.substring(i + from.length());
    }

    /** Dart's int.parse. */
    public static long parseInt(String s) {
        Long v = tryParseInt(s);
        if (v == null) {
            throw new dart.core.FormatException("Invalid radix-10 number: " + s);
        }
        return v.longValue();
    }

    /** Dart's int.parse with {@code radix:}. */
    public static long parseInt(String s, long radix) {
        Long v = tryParseInt(s, radix);
        if (v == null) {
            throw new dart.core.FormatException("Invalid radix-" + radix + " number: " + s);
        }
        return v.longValue();
    }

    /** Dart's int.tryParse: decimal, or hexadecimal with a 0x prefix. */
    public static Long tryParseInt(String s) {
        return parseSigned(s, -1);
    }

    /** Dart's int.tryParse with {@code radix:} -- 2 to 36, no 0x prefix. */
    public static Long tryParseInt(String s, long radix) {
        if (radix < 2 || radix > 36) {
            throw new RangeError("Invalid value: Not in inclusive range 2..36: " + radix);
        }
        return parseSigned(s, (int) radix);
    }

    /**
     * Dart's integer syntax: surrounding whitespace, an optional sign, then digits
     * in {@code radix} -- or, with no radix given, decimal digits or 0x and hex
     * digits. Null when the text is not that.
     */
    private static Long parseSigned(String s, int radix) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        boolean negative = false;
        if (t.startsWith("-") || t.startsWith("+")) {
            negative = t.charAt(0) == '-';
            t = t.substring(1);
        }
        int r = radix;
        if (r < 0) {
            if (t.length() > 2 && t.charAt(0) == '0' && (t.charAt(1) == 'x' || t.charAt(1) == 'X')) {
                t = t.substring(2);
                r = 16;
            } else {
                r = 10;
            }
        }
        if (t.isEmpty()) {
            return null;
        }
        for (int i = 0; i < t.length(); i++) {
            if (Character.digit(t.charAt(i), r) < 0) {
                return null;   // also rejects a second sign, which parseLong would take
            }
        }
        try {
            return Long.valueOf(Long.parseLong(negative ? "-" + t : t, r));
        } catch (NumberFormatException e) {
            return null;   // out of range
        }
    }

    /**
     * Dart's String.toUpperCase: locale independent, one UTF-16 unit at a time, so
     * a sharp s (U+00DF) is left alone, as Dart leaves it. Java's String.toUpperCase follows
     * the default locale, and under Turkish folds i to a dotted capital.
     */
    public static String toUpperCase(String s) {
        char[] out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char u = Character.toUpperCase(c);
            if (u != c) {
                if (out == null) {
                    out = s.toCharArray();
                }
                out[i] = u;
            }
        }
        return out == null ? s : new String(out);
    }

    /** Dart's String.toLowerCase: locale independent, as {@link #toUpperCase}. */
    public static String toLowerCase(String s) {
        char[] out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char l = Character.toLowerCase(c);
            if (l != c) {
                if (out == null) {
                    out = s.toCharArray();
                }
                out[i] = l;
            }
        }
        return out == null ? s : new String(out);
    }

    /** Dart's double.parse. */
    public static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            throw new dart.core.FormatException("Invalid double: " + s);
        }
    }

    /** Dart's double.tryParse. */
    public static Double tryParseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static long compareTo(String a, String b) {
        int r = a.compareTo(b);
        return r < 0 ? -1 : (r > 0 ? 1 : 0);
    }
}
