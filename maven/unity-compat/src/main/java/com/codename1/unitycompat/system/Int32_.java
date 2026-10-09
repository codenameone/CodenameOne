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

/// The methods of `System.Int32`, which is the primitive `int` here and so
/// cannot carry them itself. The value comes first.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Int32_ {
    private Int32_() {
    }

    public static String ToString(int v) {
        return Integer.toString(v);
    }

    public static boolean Equals(int v, int o) {
        return v == o;
    }

    public static boolean Equals(int v, Object other) {
        if (other instanceof EnumBox) {
            return ((EnumBox) other).value == v;
        }
        if (!(other instanceof Integer)) {
            return false;
        }
        int o = ((Integer) other).intValue();
        return v == o;
    }

    public static int GetHashCode(int v) {
        return v;
    }

    public static int CompareTo(int v, int o) {
        return (v < o ? -1 : v > o ? 1 : 0);
    }

    /// The digits of an integer written the invariant way: white space,
    /// an optional sign, digits, white space. Returns the value in a `long`,
    /// or `Long.MIN_VALUE` for text that is not a number and
    /// `Long.MAX_VALUE` for a number an `int` cannot hold.
    private static long scan(String s) {
        int end = s.length();
        int at = 0;
        while (at < end && Char_.IsWhiteSpace(s.charAt(at))) {
            at++;
        }
        while (end > at && Char_.IsWhiteSpace(s.charAt(end - 1))) {
            end--;
        }
        boolean negative = false;
        if (at < end && (s.charAt(at) == '-' || s.charAt(at) == '+')) {
            negative = s.charAt(at) == '-';
            at++;
        }
        if (at == end) {
            return Long.MIN_VALUE;
        }
        long value = 0;
        boolean overflow = false;
        for (; at < end; at++) {
            char c = s.charAt(at);
            if (c < '0' || c > '9') {
                return Long.MIN_VALUE;
            }
            if (!overflow) {
                value = value * 10 + (c - '0');
                overflow = value > 2147483648L;
            }
        }
        if (negative) {
            value = -value;
        }
        if (overflow || value > Integer.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return value;
    }

    /// `int.Parse(string)`.
    public static int Parse(String s) {
        if (s == null) {
            throw new ArgumentNullException();
        }
        long v = scan(s);
        if (v == Long.MIN_VALUE) {
            throw new FormatException("Input string was not in a correct format.");
        }
        if (v == Long.MAX_VALUE) {
            throw new OverflowException("Value was either too large or too small for an Int32.");
        }
        return (int) v;
    }

    /// `int.TryParse(string, out int)`: the result is zero when the text is
    /// not an integer an `int` holds.
    public static boolean TryParse(String s, int[] result, int at) {
        long v = s == null ? Long.MIN_VALUE : scan(s);
        if (v == Long.MIN_VALUE || v == Long.MAX_VALUE) {
            result[at] = 0;
            return false;
        }
        result[at] = (int) v;
        return true;
    }

    /// `Enum.HasFlag` on an enum over `int`.
    public static boolean HasFlag(int v, Object flag) {
        int f = (int) Enum_.number(flag);
        return (v & f) == f;
    }
}
