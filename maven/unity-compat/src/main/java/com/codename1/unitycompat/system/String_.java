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
package com.codename1.unitycompat.system;

/// The methods of `System.String`, which is `java.lang.String` here. The
/// string comes first. Comparisons are ordinal and case changes are per
/// character: translated code has no culture.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class String_ {
    public static final String Empty = "";

    private String_() {
    }

    public static String New(char[] chars) {
        return new String(chars);
    }

    public static String New(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String text(Object o) {
        return o == null ? "" : Object_.ToString(o);
    }

    private static String text(String s) {
        return s == null ? "" : s;
    }

    public static String Concat(String a, String b) {
        return text(a).concat(text(b));
    }

    public static String Concat(String a, String b, String c) {
        return new StringBuilder().append(text(a)).append(text(b)).append(text(c)).toString();
    }

    public static String Concat(String a, String b, String c, String d) {
        return new StringBuilder().append(text(a)).append(text(b)).append(text(c)).append(text(d)).toString();
    }

    public static String Concat(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) { // NOPMD ForLoopCanBeForeach
            sb.append(text(parts[i]));
        }
        return sb.toString();
    }

    public static String Concat(Object a) {
        return text(a);
    }

    public static String Concat(Object a, Object b) {
        return text(a).concat(text(b));
    }

    public static String Concat(Object a, Object b, Object c) {
        return new StringBuilder().append(text(a)).append(text(b)).append(text(c)).toString();
    }

    public static String Concat(Object[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) { // NOPMD ForLoopCanBeForeach
            sb.append(text(parts[i]));
        }
        return sb.toString();
    }

    public static String Format(String format, Object a) {
        return Format(format, new Object[] {a});
    }

    public static String Format(String format, Object a, Object b) {
        return Format(format, new Object[] {a, b});
    }

    public static String Format(String format, Object a, Object b, Object c) {
        return Format(format, new Object[] {a, b, c});
    }

    /// Composite formatting with plain `{n}` items and `{{` `}}` escapes.
    /// Alignment and format strings are not implemented and are rejected
    /// rather than ignored.
    public static String Format(String format, Object[] args) {
        StringBuilder sb = new StringBuilder();
        int n = format.length();
        for (int i = 0; i < n; i++) {
            char c = format.charAt(i);
            if (c == '{') {
                if (i + 1 < n && format.charAt(i + 1) == '{') {
                    sb.append('{');
                    i++;
                    continue;
                }
                int close = format.indexOf('}', i);
                if (close < 0) {
                    throw new FormatException();
                }
                int index = 0;
                for (int k = i + 1; k < close; k++) {
                    char d = format.charAt(k);
                    if (d < '0' || d > '9') {
                        throw new FormatException("Format item " + format.substring(i, close + 1)
                                + " uses alignment or a format string, which is not supported.");
                    }
                    index = index * 10 + (d - '0');
                }
                if (close == i + 1 || index >= args.length) {
                    throw new FormatException();
                }
                sb.append(text(args[index]));
                i = close;
            } else if (c == '}') {
                if (i + 1 < n && format.charAt(i + 1) == '}') {
                    i++;
                }
                sb.append('}');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String Join(String separator, String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(text(separator));
            }
            sb.append(text(parts[i]));
        }
        return sb.toString();
    }

    // `Split`. An overload that takes `StringSplitOptions` carries the
    // enum's name: the enum is an `int` here, and `Split(char[], int)` is a
    // different method that would otherwise be this one.

    public static String[] Split(String s, char[] separators) {
        return split(s, separators, null, Integer.MAX_VALUE, 0);
    }

    public static String[] Split(String s, char[] separators, int count) {
        return split(s, separators, null, count, 0);
    }

    public static String[] Split$StringSplitOptions(String s, char[] separators, int options) {
        return split(s, separators, null, Integer.MAX_VALUE, options);
    }

    public static String[] Split$StringSplitOptions(String s, char[] separators, int count, int options) {
        return split(s, separators, null, count, options);
    }

    public static String[] Split$StringSplitOptions(String s, char separator, int options) {
        return split(s, new char[] {separator}, null, Integer.MAX_VALUE, options);
    }

    public static String[] Split$StringSplitOptions(String s, char separator, int count, int options) {
        return split(s, new char[] {separator}, null, count, options);
    }

    /// One string to split at: a null or empty one splits nowhere.
    public static String[] Split$StringSplitOptions(String s, String separator, int options) {
        return Split$StringSplitOptions(s, separator, Integer.MAX_VALUE, options);
    }

    public static String[] Split$StringSplitOptions(String s, String separator, int count, int options) {
        return split(s, null, new String[] {separator == null ? "" : separator}, count, options);
    }

    public static String[] Split$StringSplitOptions(String s, String[] separators, int options) {
        return Split$StringSplitOptions(s, separators, Integer.MAX_VALUE, options);
    }

    /// Several strings to split at: none at all means white space.
    public static String[] Split$StringSplitOptions(String s, String[] separators, int count, int options) {
        if (separators == null || separators.length == 0) {
            return split(s, null, null, count, options);
        }
        return split(s, null, separators, count, options);
    }

    /// Length of the separator that starts at `at`, or 0.
    private static int separatorAt(String s, int at, char[] chars, String[] strings) {
        if (strings != null) {
            for (String candidate : strings) {
                if (candidate != null && candidate.length() > 0 && s.startsWith(candidate, at)) {
                    return candidate.length();
                }
            }
            return 0;
        }
        char c = s.charAt(at);
        if (chars == null || chars.length == 0) {
            return Char_.IsWhiteSpace(c) ? 1 : 0;
        }
        for (char separator : chars) {
            if (separator == c) {
                return 1;
            }
        }
        return 0;
    }

    private static String[] split(String s, char[] chars, String[] strings, int count, int options) {
        if (count < 0) {
            throw new ArgumentOutOfRangeException();
        }
        if ((options & ~1) != 0) {
            throw new ArgumentException("Illegal enum value: " + options + ".");
        }
        boolean omitEmpty = (options & 1) != 0;
        int length = s.length();
        if (count == 0 || (omitEmpty && length == 0)) {
            return new String[0];
        }
        if (count == 1) {
            return new String[] {s};
        }
        java.util.ArrayList<String> parts = new java.util.ArrayList<String>();
        int start = 0;
        int at = 0;
        while (at < length && parts.size() < count - 1) {
            int n = separatorAt(s, at, chars, strings);
            if (n == 0) {
                at++;
                continue;
            }
            if (!omitEmpty || at > start) {
                parts.add(s.substring(start, at));
            }
            at += n;
            start = at;
        }
        if (omitEmpty) {
            // The last part starts after whatever separators come first, and
            // keeps the ones inside it: it is there because the count ran out.
            while (start < length) {
                int n = separatorAt(s, start, chars, strings);
                if (n == 0) {
                    break;
                }
                start += n;
            }
            if (start < length) {
                parts.add(s.substring(start));
            }
        } else {
            parts.add(s.substring(start));
        }
        String[] result = new String[parts.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = parts.get(i);
        }
        return result;
    }

    public static boolean IsNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean op_Equality(String a, String b) {
        return a == b || (a != null && a.equals(b)); // NOPMD CompareObjectsWithEquals
    }

    public static boolean op_Inequality(String a, String b) {
        return !op_Equality(a, b);
    }

    /// Both `a.Equals(b)` and the static `string.Equals(a, b)`.
    public static boolean Equals(String a, String b) {
        return op_Equality(a, b);
    }

    public static boolean Equals(String a, Object b) {
        return a.equals(b);
    }

    public static int GetHashCode(String s) {
        return s.hashCode();
    }

    public static String ToString(String s) {
        return s.toString();
    }

    public static int CompareTo(String a, String b) {
        if (b == null) {
            return 1;
        }
        int c = a.compareTo(b);
        return c < 0 ? -1 : c > 0 ? 1 : 0;
    }

    public static int get_Length(String s) {
        return s.length();
    }

    public static char get_Chars(String s, int index) {
        return s.charAt(index);
    }

    public static String Substring(String s, int start) {
        return s.substring(start);
    }

    /// The second argument is a length in .NET, not an end index.
    public static String Substring(String s, int start, int length) {
        return s.substring(start, start + length);
    }

    public static int IndexOf(String s, String value) {
        return s.indexOf(value);
    }

    public static int IndexOf(String s, char value) {
        return s.indexOf(value);
    }

    public static boolean Contains(String s, String value) {
        return s.indexOf(value) >= 0;
    }

    public static boolean StartsWith(String s, String value) {
        return s.startsWith(value);
    }

    public static boolean EndsWith(String s, String value) {
        return s.endsWith(value);
    }

    public static String Trim(String s) {
        return s.trim();
    }

    public static String Replace(String s, String from, String to) {
        if (from.length() == 0) {
            throw new ArgumentException();
        }
        StringBuilder sb = new StringBuilder();
        int at = 0;
        while (true) {
            int next = s.indexOf(from, at);
            if (next < 0) {
                return sb.append(s.substring(at)).toString();
            }
            sb.append(s.substring(at, next)).append(text(to));
            at = next + from.length();
        }
    }

    public static String ToUpperInvariant(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            sb.append(Character.toUpperCase(s.charAt(i)));
        }
        return sb.toString();
    }

    public static String ToLowerInvariant(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            sb.append(Character.toLowerCase(s.charAt(i)));
        }
        return sb.toString();
    }
}
