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

import com.codename1.util.regex.RE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/// The members of `String`, `StringBuilder`, `StringBuffer` and `Character`
/// a desktop application names and the device's classes do not have.
///
/// The build's remap step redirects each such call here; an instance method
/// arrives with its receiver as the first argument.
///
/// The character classes are answered without the Unicode tables a JDK
/// carries: exactly for ASCII and Latin-1, by script block beyond that. The
/// `int` overloads take a code point and answer for the basic plane only.
public final class JdkStrings {

    private JdkStrings() {
    }

    // ---- String ----

    public static String join(CharSequence delimiter, CharSequence... elements) {
        if (delimiter == null || elements == null) {
            throw new NullPointerException();
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                out.append(delimiter);
            }
            out.append(String.valueOf(elements[i]));
        }
        return out.toString();
    }

    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        if (delimiter == null || elements == null) {
            throw new NullPointerException();
        }
        StringBuilder out = new StringBuilder();
        boolean first = true;
        Iterator<? extends CharSequence> it = elements.iterator();
        while (it.hasNext()) {
            if (!first) {
                out.append(delimiter);
            }
            first = false;
            out.append(String.valueOf(it.next()));
        }
        return out.toString();
    }

    public static String valueOf(char[] data) {
        return new String(data);
    }

    /// The locale is not consulted: the device formats one way.
    public static String format(Locale locale, String format, Object... args) {
        return String.format(format, args);
    }

    /// Whether the whole of `s` matches `regex`, in the regular expression
    /// dialect of `com.codename1.util.regex.RE`.
    public static boolean matches(String s, String regex) {
        if (regex == null) {
            throw new NullPointerException();
        }
        RE re = new RE("^(" + regex + ")$");
        return re.match(s) && re.getParenStart(0) == 0 && re.getParenEnd(0) == s.length();
    }

    /// Folds character by character, which no device locale can change.
    public static String toLowerCase(String s, Locale locale) {
        int n = s.length();
        StringBuilder out = null;
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            char l = Character.toLowerCase(c);
            if (l != c && out == null) {
                out = new StringBuilder(n);
                out.append(s.substring(0, i));
            }
            if (out != null) {
                out.append(l);
            }
        }
        return out == null ? s : out.toString();
    }

    public static String formatted(String s, Object... args) {
        return String.format(s, args);
    }

    public static boolean isBlank(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /// Unlike `trim`, which drops everything up to the space character,
    /// this drops white space, wherever in Unicode it is.
    public static String strip(String s) {
        return stripTrailing(stripLeading(s));
    }

    public static String stripLeading(String s) {
        int start = 0;
        while (start < s.length() && Character.isWhitespace(s.charAt(start))) {
            start++;
        }
        return start == 0 ? s : s.substring(start);
    }

    public static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return end == s.length() ? s : s.substring(0, end);
    }

    public static String repeat(String s, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count is negative: " + count);
        }
        if (count == 1) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            out.append(s);
        }
        return out.toString();
    }

    /// The UTF-16 units of `s`, each as an `int`.
    public static IntStream chars(CharSequence s) {
        if (s == null) {
            throw new NullPointerException();
        }
        // Through flatMap, so that the length is read when the stream runs:
        // a StringBuilder may grow between the call and the traversal.
        return Stream.of(s).flatMapToInt(text -> IntStream.range(0, text.length()).map(i -> text.charAt(i)));
    }

    /// The lines of `s`, without their terminators: a line feed, a carriage
    /// return, or the two together. A terminator at the very end starts no
    /// empty last line.
    public static Stream<String> lines(String s) {
        List<String> out = new ArrayList<String>();
        int n = s.length();
        int start = 0;
        while (start < n) {
            int end = start;
            while (end < n && s.charAt(end) != '\n' && s.charAt(end) != '\r') {
                end++;
            }
            out.add(s.substring(start, end));
            if (end < n && s.charAt(end) == '\r' && end + 1 < n && s.charAt(end + 1) == '\n') {
                end++;
            }
            start = end + 1;
        }
        return JdkCollections.stream(out);
    }

    public static String[] split(String s, String regex) {
        return split(s, regex, 0);
    }

    /// Splits `s` around the matches of `regex`, by the JDK's rules for
    /// `limit`: positive is the most pieces there may be, zero drops empty
    /// pieces from the end, negative keeps them.
    ///
    /// A separator with nothing special in it -- a comma, an escaped dot or
    /// pipe -- is searched for as plain text. Anything else goes to
    /// `com.codename1.util.regex.RE`, whose dialect is the common core of
    /// the JDK's and not all of it.
    public static String[] split(String s, String regex, int limit) {
        String literal = literal(regex);
        RE re = literal == null ? new RE(portable(regex)) : null;
        List<String> pieces = new ArrayList<String>();
        int n = s.length();
        int index = 0;
        int from = 0;
        boolean matchedAny = false;
        while (from <= n) {
            int start;
            int end;
            if (literal != null) {
                start = literal.length() == 0 ? from : s.indexOf(literal, from);
                if (start < 0 || start > n) {
                    break;
                }
                end = start + literal.length();
            } else {
                if (!re.match(s, from)) {
                    break;
                }
                start = re.getParenStart(0);
                end = re.getParenEnd(0);
            }
            // After an empty match the search moves on by one character,
            // or it would find the same place again.
            from = end > start ? end : end + 1;
            if (limit > 0 && pieces.size() == limit - 1) {
                break;
            }
            if (start == end && start == 0) {
                // An empty match at the very start separates nothing.
                continue;
            }
            matchedAny = true;
            pieces.add(s.substring(index, start));
            index = end;
        }
        if (!matchedAny) {
            return new String[] {s};
        }
        pieces.add(s.substring(index));
        int size = pieces.size();
        if (limit == 0) {
            while (size > 0 && pieces.get(size - 1).length() == 0) {
                size--;
            }
        }
        String[] out = new String[size];
        for (int i = 0; i < size; i++) {
            out[i] = pieces.get(i);
        }
        return out;
    }

    /// `regex` as the device's engine reads it, where that differs from the
    /// JDK for the same text: a hyphen that closes a character class
    /// (`[+*-]`) is a hyphen to the JDK and an open-ended range to the
    /// engine, so it is escaped.
    private static String portable(String regex) {
        if (regex.indexOf("-]") < 0) {
            return regex;
        }
        StringBuilder out = new StringBuilder(regex.length() + 2);
        boolean inClass = false;
        int n = regex.length();
        for (int i = 0; i < n; i++) {
            char c = regex.charAt(i);
            if (c == '\\' && i + 1 < n) {
                out.append(c).append(regex.charAt(++i));
                continue;
            }
            if (c == '[') {
                inClass = true;
            } else if (c == ']') {
                inClass = false;
            } else if (c == '-' && inClass && i + 1 < n && regex.charAt(i + 1) == ']') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    /// The text `regex` matches when it can only match one: no character in
    /// it is special, apart from a backslash in front of a punctuation
    /// character, which stands for that character. Null when it is a
    /// pattern.
    private static String literal(String regex) {
        StringBuilder out = new StringBuilder(regex.length());
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '\\') {
                if (i + 1 >= regex.length()) {
                    return null;
                }
                char escaped = regex.charAt(++i);
                if (escaped < 128 && !isAsciiLetterOrDigit(escaped)) {
                    out.append(escaped);
                    continue;
                }
                return null;
            }
            if (".$|()[]{}^?*+".indexOf(c) >= 0) {
                return null;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static boolean isAsciiLetterOrDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    // ---- StringBuilder and StringBuffer ----

    public static int indexOf(StringBuilder sb, String str) {
        return sb.toString().indexOf(str);
    }

    public static int indexOf(StringBuilder sb, String str, int from) {
        return sb.toString().indexOf(str, from);
    }

    public static int lastIndexOf(StringBuilder sb, String str) {
        return sb.toString().lastIndexOf(str);
    }

    public static int lastIndexOf(StringBuilder sb, String str, int from) {
        return sb.toString().lastIndexOf(str, from);
    }

    public static String substring(StringBuilder sb, int start) {
        return sb.toString().substring(start);
    }

    public static String substring(StringBuilder sb, int start, int end) {
        return sb.toString().substring(start, end);
    }

    public static StringBuilder replace(StringBuilder sb, int start, int end, String str) {
        String s = sb.toString();
        sb.setLength(0);
        sb.append(replaced(s, start, end, str));
        return sb;
    }

    public static StringBuilder insert(StringBuilder sb, int offset, char[] str) {
        return sb.insert(offset, new String(str));
    }

    public static int indexOf(StringBuffer sb, String str) {
        return sb.toString().indexOf(str);
    }

    public static int indexOf(StringBuffer sb, String str, int from) {
        return sb.toString().indexOf(str, from);
    }

    public static int lastIndexOf(StringBuffer sb, String str) {
        return sb.toString().lastIndexOf(str);
    }

    public static int lastIndexOf(StringBuffer sb, String str, int from) {
        return sb.toString().lastIndexOf(str, from);
    }

    public static String substring(StringBuffer sb, int start) {
        return sb.toString().substring(start);
    }

    public static String substring(StringBuffer sb, int start, int end) {
        return sb.toString().substring(start, end);
    }

    public static StringBuffer replace(StringBuffer sb, int start, int end, String str) {
        String s = sb.toString();
        sb.setLength(0);
        sb.append(replaced(s, start, end, str));
        return sb;
    }

    private static String replaced(String s, int start, int end, String str) {
        if (start < 0 || start > s.length() || start > end) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (str == null) {
            throw new NullPointerException();
        }
        int to = end > s.length() ? s.length() : end;
        return s.substring(0, start) + str + s.substring(to);
    }

    // ---- Character ----

    public static String toString(char c) {
        return String.valueOf(c);
    }

    public static boolean isLetter(char c) {
        if (c < 0x80) {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
        }
        if (c < 0x100) {
            return c == 0xaa || c == 0xb5 || c == 0xba || c >= 0xc0 && c != 0xd7 && c != 0xf7;
        }
        return c <= 0x24f // Latin Extended
                || c >= 0x370 && c <= 0x3ff && c != 0x37e && c != 0x387 // Greek
                || c >= 0x400 && c <= 0x481 || c >= 0x48a && c <= 0x52f // Cyrillic
                || c >= 0x531 && c <= 0x556 || c >= 0x561 && c <= 0x587 // Armenian
                || c >= 0x5d0 && c <= 0x5ea // Hebrew
                || c >= 0x620 && c <= 0x64a || c >= 0x671 && c <= 0x6d3 // Arabic
                || c >= 0x904 && c <= 0x939 // Devanagari
                || c >= 0xe01 && c <= 0xe30 // Thai
                || c >= 0x10a0 && c <= 0x10ff && c != 0x10fb // Georgian
                || c >= 0x1e00 && c <= 0x1fff // Latin and Greek Extended Additional
                || c >= 0x3041 && c <= 0x3096 || c >= 0x30a1 && c <= 0x30fa // Kana
                || c >= 0x3400 && c <= 0x4dbf || c >= 0x4e00 && c <= 0x9fff // CJK
                || c >= 0xac00 && c <= 0xd7a3; // Hangul
    }

    public static boolean isLetter(int codePoint) {
        return codePoint >= 0 && codePoint <= 0xffff && isLetter((char) codePoint);
    }

    public static boolean isAlphabetic(int codePoint) {
        return isLetter(codePoint);
    }

    public static boolean isLetterOrDigit(char c) {
        return isLetter(c) || Character.isDigit(c);
    }

    public static boolean isLetterOrDigit(int codePoint) {
        return codePoint >= 0 && codePoint <= 0xffff && isLetterOrDigit((char) codePoint);
    }

    public static boolean isDigit(int codePoint) {
        return codePoint >= 0 && codePoint <= 0xffff && Character.isDigit((char) codePoint);
    }

    public static boolean isLowerCase(int codePoint) {
        return codePoint >= 0 && codePoint <= 0xffff && Character.isLowerCase((char) codePoint);
    }

    public static boolean isUpperCase(int codePoint) {
        return codePoint >= 0 && codePoint <= 0xffff && Character.isUpperCase((char) codePoint);
    }

    public static boolean isSpaceChar(int codePoint) {
        return codePoint >= 0 && codePoint <= 0xffff && Character.isSpaceChar((char) codePoint);
    }

    public static boolean isISOControl(char c) {
        return c <= 0x1f || c >= 0x7f && c <= 0x9f;
    }

    public static boolean isISOControl(int codePoint) {
        return codePoint >= 0 && codePoint <= 0x1f || codePoint >= 0x7f && codePoint <= 0x9f;
    }

    public static int toLowerCase(int codePoint) {
        return codePoint >= 0 && codePoint <= 0xffff ? Character.toLowerCase((char) codePoint) : codePoint;
    }

    public static int toUpperCase(int codePoint) {
        return codePoint >= 0 && codePoint <= 0xffff ? Character.toUpperCase((char) codePoint) : codePoint;
    }

    public static char forDigit(int digit, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX || digit < 0 || digit >= radix) {
            return '\0';
        }
        return (char) (digit < 10 ? '0' + digit : 'a' + digit - 10);
    }

    public static int digit(int codePoint, int radix) {
        return codePoint >= 0 && codePoint <= 0xffff ? Character.digit((char) codePoint, radix) : -1;
    }

    /// The value of a digit, or of a Latin letter read as a digit of base
    /// 36, or -1.
    public static int getNumericValue(char c) {
        return Character.digit(c, Character.MAX_RADIX);
    }

    public static int getNumericValue(int codePoint) {
        return digit(codePoint, Character.MAX_RADIX);
    }
}
