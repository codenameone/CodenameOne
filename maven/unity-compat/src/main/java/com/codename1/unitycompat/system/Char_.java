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

/// The methods of `System.Char`, which is the primitive `char` here and so
/// cannot carry them itself. The value comes first.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Char_ {
    private Char_() {
    }

    public static String ToString(char v) {
        return String.valueOf(v);
    }

    public static boolean Equals(char v, char o) {
        return v == o;
    }

    public static boolean Equals(char v, Object other) {
        if (!(other instanceof Character)) {
            return false;
        }
        char o = ((Character) other).charValue();
        return v == o;
    }

    public static int GetHashCode(char v) {
        return v;
    }

    /// `char.IsWhiteSpace`: the characters Unicode calls white space,
    /// listed, because the device's class library has no table to ask.
    public static boolean IsWhiteSpace(char c) {
        if (c < 128) {
            return c == ' ' || (c >= 9 && c <= 13);
        }
        return c == 0x85 || c == 0xA0 || c == 0x1680 || (c >= 0x2000 && c <= 0x200A) || c == 0x2028
                || c == 0x2029 || c == 0x202F || c == 0x205F || c == 0x3000;
    }

    /// The zero of every run of decimal digits past ASCII in the basic
    /// plane, ascending: Unicode's category Nd, which is what
    /// `char.IsDigit` asks about, comes in runs of exactly ten. Listed
    /// because the device's class library has no table to ask --
    /// `Character.isDigit` answers false for everything on one of the two
    /// libraries this runs against. A digit outside the basic plane is two
    /// `char`s, and .NET says false for each half as well.
    private static final char[] DIGIT_ZEROS = {
        0x0660, 0x06F0, 0x07C0, 0x0966, 0x09E6, 0x0A66, 0x0AE6, 0x0B66,
        0x0BE6, 0x0C66, 0x0CE6, 0x0D66, 0x0DE6, 0x0E50, 0x0ED0, 0x0F20,
        0x1040, 0x1090, 0x17E0, 0x1810, 0x1946, 0x19D0, 0x1A80, 0x1A90,
        0x1B50, 0x1BB0, 0x1C40, 0x1C50, 0xA620, 0xA8D0, 0xA900, 0xA9D0,
        0xA9F0, 0xAA50, 0xABF0, 0xFF10
    };

    /// `char.IsDigit`: a decimal digit of any script, as .NET has it.
    /// `int.Parse` reads the ASCII ten only, there as here.
    public static boolean IsDigit(char c) {
        if (c < 128) {
            return isAsciiDigit(c);
        }
        return digit(c) >= 0;
    }

    /// The ten digits a number is parsed from, whatever [#IsDigit(char)]
    /// says of the others.
    static boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /// The value of a decimal digit past ASCII, or -1 for anything else.
    private static int digit(char c) {
        int low = 0;
        int high = DIGIT_ZEROS.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int zero = DIGIT_ZEROS[mid];
            if (c < zero) {
                high = mid - 1;
            } else if (c > zero + 9) {
                low = mid + 1;
            } else {
                return c - zero;
            }
        }
        return -1;
    }

    public static int CompareTo(char v, char o) {
        return (v - o);
    }
}
