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

    public static boolean IsDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public static int CompareTo(char v, char o) {
        return (v - o);
    }
}
