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

import java.util.Arrays;

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

    /**
     * Dart's s * n operator -- repeat. A count of zero OR LESS is the empty string, not
     * an error: measured on the Dart 3.9 VM, {@code 'x' * -1} is {@code ''}.
     */
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

    /**
     * Dart's {@code lastIndexOf(pattern, start)}: the last match starting at or before
     * {@code start}, which must lie in 0..length. Java clamps an out-of-range start
     * instead of throwing, so it is range-checked as {@link #indexOf(String, String, long)} is.
     */
    public static long lastIndexOf(String s, String other, long start) {
        RangeError.checkValueInInterval(start, 0, s.length(), "start");
        return s.lastIndexOf(other, (int) start);
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
        return lastIndexOf(s, pattern, s.length());
    }

    /** Dart's {@code lastIndexOf(Pattern, start)} for a RegExp: searching back from start. */
    public static long lastIndexOf(String s, RegExp pattern, long start) {
        RangeError.checkValueInInterval(start, 0, s.length(), "start");
        for (int i = (int) start; i >= 0; i--) {
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
     * Dart's {@code contains(pattern, startIndex)}: a match that starts at or after
     * {@code startIndex}, which must lie in 0..length. The start used to be dropped, so
     * {@code 'abc'.contains('a', 1)} searched from 0 and answered true.
     */
    public static boolean contains(String s, RegExp pattern, long startIndex) {
        RangeError.checkValueInInterval(startIndex, 0, s.length(), "startIndex");
        return pattern.matchFrom(s, (int) startIndex) != null;
    }

    /** {@link #contains(String, RegExp, long)} for a String pattern. */
    public static boolean contains(String s, String other, long startIndex) {
        RangeError.checkValueInInterval(startIndex, 0, s.length(), "startIndex");
        return s.indexOf(other, (int) startIndex) >= 0;
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

    /**
     * Dart's String.trim: strips Dart's whitespace -- Unicode spaces and line
     * separators included, a non-breaking space among them. Java's trim only strips
     * characters at or below U+0020, so text pasted with non-breaking spaces stayed
     * padded after a trim.
     */
    public static String trim(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && isDartWhitespace(s.charAt(start))) {
            start++;
        }
        while (end > start && isDartWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return start == 0 && end == s.length() ? s : s.substring(start, end);
    }

    /** The code units Dart's trim treats as whitespace. */
    static boolean isDartWhitespace(char c) {
        if (c <= 0x20) {
            return c == 0x20 || (c >= 0x09 && c <= 0x0D);
        }
        return c == 0x85 || c == 0xA0 || c == 0x1680 || (c >= 0x2000 && c <= 0x200A)
                || c == 0x2028 || c == 0x2029 || c == 0x202F || c == 0x205F
                || c == 0x3000 || c == 0xFEFF;
    }

    /** Dart's {@code startsWith(pattern, index)}: whether the pattern occurs at index. */
    public static boolean startsWith(String s, String pattern, long index) {
        RangeError.checkValueInInterval(index, 0, s.length(), "index");
        return s.startsWith(pattern, (int) index);
    }

    public static boolean contains(String s, String other) {
        return s.contains(other);
    }

    public static String replaceAll(String s, String from, String to) {
        // Dart's replaceAll on a String pattern is literal, like Java's replace.
        return s.replace(from, to);
    }

    public static String replaceFirst(String s, String from, String to) {
        return replaceFirst(s, from, to, 0);
    }

    /** Dart's {@code replaceFirst(from, to, startIndex)}: the first match at or after the start. */
    public static String replaceFirst(String s, String from, String to, long startIndex) {
        RangeError.checkValueInInterval(startIndex, 0, s.length(), "startIndex");
        int i = s.indexOf(from, (int) startIndex);
        if (i < 0) {
            return s;
        }
        return s.substring(0, i) + to + s.substring(i + from.length());
    }

    /** {@link #replaceFirst(String, String, String, long)} for a RegExp pattern. */
    public static String replaceFirst(String s, RegExp pattern, String to, long startIndex) {
        RangeError.checkValueInInterval(startIndex, 0, s.length(), "startIndex");
        RegExpMatch m = pattern.matchFrom(s, (int) startIndex);
        if (m == null) {
            return s;
        }
        return s.substring(0, (int) m.start()) + to + s.substring((int) m.end());
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
        // Dart's whitespace, not Java's: String.trim leaves U+00A0, U+2003, U+3000 and
        // the rest in place, and 123 between two U+00A0 failed to parse.
        String t = trim(s);
        boolean negative = false;
        if (t.startsWith("-") || t.startsWith("+")) {
            negative = t.charAt(0) == '-';
            t = t.substring(1);
        }
        int r = radix;
        boolean hexPrefix = false;
        if (r < 0) {
            if (t.length() > 2 && t.charAt(0) == '0' && (t.charAt(1) == 'x' || t.charAt(1) == 'X')) {
                t = t.substring(2);
                r = 16;
                hexPrefix = true;
            } else {
                r = 10;
            }
        }
        if (t.isEmpty()) {
            return null;
        }
        for (int i = 0; i < t.length(); i++) {
            if (asciiDigit(t.charAt(i), r) < 0) {
                return null;   // also rejects a second sign, which parseLong would take
            }
        }
        if (hexPrefix && !negative) {
            // Dart reads an unsigned 0x literal up to 0xffffffffffffffff as the
            // two's-complement bits (0xffffffffffffffff is -1); a '-' keeps the signed
            // range, which parseLong below already enforces. Hand-folded because
            // Long.parseUnsignedLong is not in the device class library.
            int start = 0;
            while (start < t.length() - 1 && t.charAt(start) == '0') {
                start++;
            }
            if (t.length() - start > 16) {
                return null;
            }
            long bits = 0;
            for (int i = start; i < t.length(); i++) {
                bits = (bits << 4) | asciiDigit(t.charAt(i), 16);
            }
            return Long.valueOf(bits);
        }
        try {
            return Long.valueOf(Long.parseLong(negative ? "-" + t : t, r));
        } catch (NumberFormatException e) {
            return null;   // out of range
        }
    }

    /**
     * The value of an ASCII digit or letter in {@code radix}, or -1. Dart's int.parse
     * takes only these; Character.digit also takes every Unicode decimal digit, so
     * Arabic-Indic three parsed as 3 (and Long.parseLong would take it too).
     */
    private static int asciiDigit(char c, int radix) {
        int d;
        if (c >= '0' && c <= '9') {
            d = c - '0';
        } else if (c >= 'a' && c <= 'z') {
            d = c - 'a' + 10;
        } else if (c >= 'A' && c <= 'Z') {
            d = c - 'A' + 10;
        } else {
            return -1;
        }
        return d < radix ? d : -1;
    }

    /**
     * Unconditional (locale- and context-free) multi-character upper mappings from
     * Unicode's SpecialCasing.txt, keyed by code point and sorted for binary search.
     * A per-UTF-16-unit {@code Character.toUpperCase(char)} can only ever answer one
     * output unit, so it leaves these alone; measured on the Dart 3.9 VM,
     * {@code 'strasse-with-sharp-s'.toUpperCase()} (U+00DF) answers "...SS", and the
     * ligatures (U+FB00 etc.) expand to their letters the same way.
     */
    private static final int[] UPPER_SPECIAL_CP = {
        0x00DF, 0x0149, 0x01F0, 0x0390, 0x03B0, 0x0587, 0x1E96, 0x1E97,
        0x1E98, 0x1E99, 0x1E9A, 0x1F50, 0x1F52, 0x1F54, 0x1F56, 0x1F80,
        0x1F81, 0x1F82, 0x1F83, 0x1F84, 0x1F85, 0x1F86, 0x1F87, 0x1F88,
        0x1F89, 0x1F8A, 0x1F8B, 0x1F8C, 0x1F8D, 0x1F8E, 0x1F8F, 0x1F90,
        0x1F91, 0x1F92, 0x1F93, 0x1F94, 0x1F95, 0x1F96, 0x1F97, 0x1F98,
        0x1F99, 0x1F9A, 0x1F9B, 0x1F9C, 0x1F9D, 0x1F9E, 0x1F9F, 0x1FA0,
        0x1FA1, 0x1FA2, 0x1FA3, 0x1FA4, 0x1FA5, 0x1FA6, 0x1FA7, 0x1FA8,
        0x1FA9, 0x1FAA, 0x1FAB, 0x1FAC, 0x1FAD, 0x1FAE, 0x1FAF, 0x1FB2,
        0x1FB3, 0x1FB4, 0x1FB6, 0x1FB7, 0x1FBC, 0x1FC2, 0x1FC3, 0x1FC4,
        0x1FC6, 0x1FC7, 0x1FCC, 0x1FD2, 0x1FD3, 0x1FD6, 0x1FD7, 0x1FE2,
        0x1FE3, 0x1FE4, 0x1FE6, 0x1FE7, 0x1FF2, 0x1FF3, 0x1FF4, 0x1FF6,
        0x1FF7, 0x1FFC, 0xFB00, 0xFB01, 0xFB02, 0xFB03, 0xFB04, 0xFB05,
        0xFB06, 0xFB13, 0xFB14, 0xFB15, 0xFB16, 0xFB17
    };

    /** The multi-character upper mapping for the matching entry in {@link #UPPER_SPECIAL_CP}. */
    private static final String[] UPPER_SPECIAL_MAP = {
        "SS", "\u02BCN", "J\u030C", "\u0399\u0308\u0301",
        "\u03A5\u0308\u0301", "\u0535\u0552", "H\u0331", "T\u0308",
        "W\u030A", "Y\u030A", "A\u02BE", "\u03A5\u0313",
        "\u03A5\u0313\u0300", "\u03A5\u0313\u0301", "\u03A5\u0313\u0342", "\u1F08\u0399",
        "\u1F09\u0399", "\u1F0A\u0399", "\u1F0B\u0399", "\u1F0C\u0399",
        "\u1F0D\u0399", "\u1F0E\u0399", "\u1F0F\u0399", "\u1F08\u0399",
        "\u1F09\u0399", "\u1F0A\u0399", "\u1F0B\u0399", "\u1F0C\u0399",
        "\u1F0D\u0399", "\u1F0E\u0399", "\u1F0F\u0399", "\u1F28\u0399",
        "\u1F29\u0399", "\u1F2A\u0399", "\u1F2B\u0399", "\u1F2C\u0399",
        "\u1F2D\u0399", "\u1F2E\u0399", "\u1F2F\u0399", "\u1F28\u0399",
        "\u1F29\u0399", "\u1F2A\u0399", "\u1F2B\u0399", "\u1F2C\u0399",
        "\u1F2D\u0399", "\u1F2E\u0399", "\u1F2F\u0399", "\u1F68\u0399",
        "\u1F69\u0399", "\u1F6A\u0399", "\u1F6B\u0399", "\u1F6C\u0399",
        "\u1F6D\u0399", "\u1F6E\u0399", "\u1F6F\u0399", "\u1F68\u0399",
        "\u1F69\u0399", "\u1F6A\u0399", "\u1F6B\u0399", "\u1F6C\u0399",
        "\u1F6D\u0399", "\u1F6E\u0399", "\u1F6F\u0399", "\u1FBA\u0399",
        "\u0391\u0399", "\u0386\u0399", "\u0391\u0342", "\u0391\u0342\u0399",
        "\u0391\u0399", "\u1FCA\u0399", "\u0397\u0399", "\u0389\u0399",
        "\u0397\u0342", "\u0397\u0342\u0399", "\u0397\u0399", "\u0399\u0308\u0300",
        "\u0399\u0308\u0301", "\u0399\u0342", "\u0399\u0308\u0342", "\u03A5\u0308\u0300",
        "\u03A5\u0308\u0301", "\u03A1\u0313", "\u03A5\u0342", "\u03A5\u0308\u0342",
        "\u1FFA\u0399", "\u03A9\u0399", "\u038F\u0399", "\u03A9\u0342",
        "\u03A9\u0342\u0399", "\u03A9\u0399", "FF", "FI",
        "FL", "FFI", "FFL", "ST",
        "ST", "\u0544\u0546", "\u0544\u0535", "\u0544\u053B",
        "\u054E\u0546", "\u0544\u053D"
    };

    /**
     * Capital sigma (Greek, U+03A3). Its simple lowercase is U+03C3 (medial), but
     * Unicode's Final_Sigma condition -- the one context-sensitive rule the
     * unconditional default algorithm still applies -- lowercases a sigma at the
     * end of a word to U+03C2 (final) instead.
     */
    private static final int CAPITAL_SIGMA = 0x03A3;
    private static final String MEDIAL_SIGMA = "\u03C3";
    private static final String FINAL_SIGMA = "\u03C2";

    /**
     * Capital I with dot above (U+0130): the one unconditional entry whose
     * LOWER mapping (not just upper) is multi-character -- "i" plus a combining
     * dot above -- rather than plain "i".
     */
    private static final int CAPITAL_I_WITH_DOT_ABOVE = 0x0130;
    private static final String LOWER_I_WITH_DOT_ABOVE = "i\u0307";

    private static boolean isAscii(String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    /** The multi-character upper mapping for {@code cp}, or null when it has none. */
    private static String specialUpper(int cp) {
        int idx = Arrays.binarySearch(UPPER_SPECIAL_CP, cp);
        return idx >= 0 ? UPPER_SPECIAL_MAP[idx] : null;
    }

    /**
     * Unicode's Final_Sigma: {@code index} (a char index) names a capital sigma;
     * true when it is preceded -- skipping case-ignorable characters (combining
     * marks, the apostrophe/quote/dot marks Unicode treats as part of a word) --
     * by a cased letter, and followed by no cased letter before the next
     * non-case-ignorable character or the end of the string.
     */
    private static boolean isFinalSigma(String s, int index) {
        boolean sawCasedBefore = false;
        int i = index;
        while (i > 0) {
            int cp = Character.codePointBefore(s, i);
            i -= Character.charCount(cp);
            if (isCaseIgnorable(cp)) {
                continue;
            }
            sawCasedBefore = isCased(cp);
            break;
        }
        if (!sawCasedBefore) {
            return false;
        }
        int len = s.length();
        int j = index + 1;
        while (j < len) {
            int cp = Character.codePointAt(s, j);
            if (isCaseIgnorable(cp)) {
                j += Character.charCount(cp);
                continue;
            }
            return !isCased(cp);
        }
        return true;
    }

    /**
     * A "cased" code point for Final_Sigma: upper, lower, or title case.
     * {@code Character.isTitleCase(int)} has no device-runtime implementation
     * (vm/JavaAPI and Ports/CLDC11 define no such method, unlike the
     * category constants below), so title case is tested through
     * {@code getType} instead -- it covers the same handful of digraphs
     * ("Dz", "Lj", ...) and nothing calls into a missing native.
     */
    private static boolean isCased(int cp) {
        return Character.isUpperCase(cp) || Character.isLowerCase(cp)
                || Character.getType(cp) == Character.TITLECASE_LETTER;
    }

    /** Unicode's Case_Ignorable, approximated: combining marks, format/modifier
     * characters, and the handful of punctuation marks (apostrophe, middle dot,
     * curly quotes, ...) that Unicode's word-break rules keep inside a word.
     */
    private static boolean isCaseIgnorable(int cp) {
        switch (Character.getType(cp)) {
            case Character.NON_SPACING_MARK:
            case Character.ENCLOSING_MARK:
            case Character.COMBINING_SPACING_MARK:
            case Character.FORMAT:
            case Character.MODIFIER_LETTER:
            case Character.MODIFIER_SYMBOL:
                return true;
            default:
                break;
        }
        switch (cp) {
            case 0x0027: // APOSTROPHE
            case 0x00B7: // MIDDLE DOT
            case 0x0387: // GREEK ANO TELEIA
            case 0x05F4: // HEBREW PUNCTUATION GERSHAYIM
            case 0x2018: // LEFT SINGLE QUOTATION MARK
            case 0x2019: // RIGHT SINGLE QUOTATION MARK
            case 0x2024: // ONE DOT LEADER
            case 0x2027: // HYPHENATION POINT
            case 0xFE13:
            case 0xFE52:
            case 0xFE55:
            case 0xFF07:
            case 0xFF0E:
            case 0xFF1A:
                return true;
            default:
                return false;
        }
    }

    private static String asciiToUpperCase(String s) {
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

    private static String asciiToLowerCase(String s) {
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

    /**
     * Dart's String.toUpperCase: Unicode's default full case mapping, locale
     * independent, one code point at a time -- never Java's locale-sensitive
     * {@code String.toUpperCase()} (which also folds i to a dotted capital under
     * Turkish), and never a per-UTF-16-unit {@code Character.toUpperCase(char)}
     * loop, which both garbles every supplementary-plane letter (Deseret, Osage,
     * Adlam, ...) by mapping each surrogate half on its own, and cannot answer
     * the SpecialCasing.txt expansions in {@link #UPPER_SPECIAL_CP} at all -- it
     * has nowhere to put a second output character. The ASCII fast path below
     * is exact: no ASCII character needs either kind of special handling.
     */
    public static String toUpperCase(String s) {
        if (isAscii(s)) {
            return asciiToUpperCase(s);
        }
        int len = s.length();
        StringBuilder out = new StringBuilder(len);
        for (int i = 0; i < len; ) {
            int cp = Character.codePointAt(s, i);
            i += Character.charCount(cp);
            String special = specialUpper(cp);
            if (special != null) {
                out.append(special);
            } else {
                out.append(Character.toChars(Character.toUpperCase(cp)));
            }
        }
        return out.toString();
    }

    /**
     * Dart's String.toLowerCase: as {@link #toUpperCase}, plus Unicode's one
     * context-sensitive default-algorithm rule, Final_Sigma (see
     * {@link #isFinalSigma}), and the single unconditional entry whose LOWER
     * mapping is itself multi-character, capital I with dot above (U+0130).
     */
    public static String toLowerCase(String s) {
        if (isAscii(s)) {
            return asciiToLowerCase(s);
        }
        int len = s.length();
        StringBuilder out = new StringBuilder(len);
        for (int i = 0; i < len; ) {
            int cp = Character.codePointAt(s, i);
            int charCount = Character.charCount(cp);
            if (cp == CAPITAL_SIGMA) {
                out.append(isFinalSigma(s, i) ? FINAL_SIGMA : MEDIAL_SIGMA);
            } else if (cp == CAPITAL_I_WITH_DOT_ABOVE) {
                out.append(LOWER_I_WITH_DOT_ABOVE);
            } else {
                out.append(Character.toChars(Character.toLowerCase(cp)));
            }
            i += charCount;
        }
        return out.toString();
    }

    /** Dart's double.parse. */
    public static double parseDouble(String s) {
        Double d = tryParseDouble(s);
        if (d == null) {
            throw new dart.core.FormatException("Invalid double: " + s);
        }
        return d.doubleValue();
    }

    /**
     * Dart's double.tryParse: null for anything outside Dart's grammar -- an optional
     * sign, then NaN, Infinity, or decimal digits with an optional fraction and
     * exponent, with surrounding whitespace allowed. Java's parser also accepts a d or f
     * suffix and hexadecimal floats, so '1d' and '0x1.0p0' parsed to 1.0 where Dart
     * answers null.
     */
    public static Double tryParseDouble(String s) {
        if (s == null) {
            return null;
        }
        String t = trim(s);
        if (!isDartDouble(t)) {
            return null;
        }
        try {
            return Double.valueOf(Double.parseDouble(t));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isDartDouble(String t) {
        int i = 0;
        int n = t.length();
        if (i < n && (t.charAt(i) == '+' || t.charAt(i) == '-')) {
            i++;
        }
        String rest = t.substring(i);
        if (rest.equals("NaN") || rest.equals("Infinity")) {
            return true;
        }
        int digits = 0;
        while (i < n && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
            i++;
            digits++;
        }
        if (i < n && t.charAt(i) == '.') {
            i++;
            while (i < n && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
                i++;
                digits++;
            }
        }
        if (digits == 0) {
            return false;
        }
        if (i < n && (t.charAt(i) == 'e' || t.charAt(i) == 'E')) {
            i++;
            if (i < n && (t.charAt(i) == '+' || t.charAt(i) == '-')) {
                i++;
            }
            int exp = 0;
            while (i < n && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
                i++;
                exp++;
            }
            if (exp == 0) {
                return false;
            }
        }
        return i == n;
    }

    public static long compareTo(String a, String b) {
        int r = a.compareTo(b);
        return r < 0 ? -1 : (r > 0 ? 1 : 0);
    }
}
