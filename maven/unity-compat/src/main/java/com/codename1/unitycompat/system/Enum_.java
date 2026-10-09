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

/// The methods of `System.Enum`. An enum's members come from its
/// [Type], which the translator builds from the declaration.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Enum_ {
    private Enum_() {
    }

    private static Type enumType(Type type) {
        if (type == null) {
            throw new ArgumentNullException();
        }
        if (type.names == null) {
            throw new ArgumentException("Type provided must be an Enum.");
        }
        return type;
    }

    /// The number in a boxed enum or a boxed integer.
    static long number(Object value) {
        if (value instanceof EnumBox) {
            return ((EnumBox) value).value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        }
        if (value instanceof Long) {
            return ((Long) value).longValue();
        }
        if (value instanceof Short) {
            return ((Short) value).shortValue();
        }
        if (value instanceof Byte) {
            return ((Byte) value).byteValue();
        }
        throw new ArgumentException("The value is not an enum or an integer.");
    }

    /// True for the name of a member, or for a value some member has.
    public static boolean IsDefined(Type enumType, Object value) {
        Type t = enumType(enumType);
        if (value == null) {
            throw new ArgumentNullException();
        }
        if (value instanceof String) {
            for (int i = 0; i < t.names.length; i++) {
                if (t.names[i].equals(value)) {
                    return true;
                }
            }
            return false;
        }
        return t.indexOf(number(value)) >= 0;
    }

    public static String GetName(Type enumType, Object value) {
        Type t = enumType(enumType);
        int at = t.indexOf(number(value));
        return at < 0 ? null : t.names[at];
    }

    public static String[] GetNames(Type enumType) {
        Type t = enumType(enumType);
        String[] out = new String[t.names.length];
        System.arraycopy(t.names, 0, out, 0, out.length);
        return out;
    }

    /// The values, as the `int[]` an `(E[])` cast of the result expects of
    /// an enum over `int`, in ascending order as .NET gives them.
    public static Object GetValues(Type enumType) {
        Type t = enumType(enumType);
        int[] out = new int[t.values.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = (int) t.values[i];
        }
        for (int i = 1; i < out.length; i++) {
            int v = out[i];
            int j = i - 1;
            while (j >= 0 && out[j] > v) {
                out[j + 1] = out[j];
                j--;
            }
            out[j + 1] = v;
        }
        return out;
    }

    public static Object Parse(Type enumType, String value) {
        return Parse(enumType, value, false);
    }

    public static Object Parse(Type enumType, String value, boolean ignoreCase) {
        Type t = enumType(enumType);
        if (value == null) {
            throw new ArgumentNullException();
        }
        String wanted = value.trim();
        for (int i = 0; i < t.names.length; i++) {
            if (ignoreCase ? t.names[i].equalsIgnoreCase(wanted) : t.names[i].equals(wanted)) {
                return new EnumBox(t, t.values[i]);
            }
        }
        try {
            return new EnumBox(t, Long.parseLong(wanted));
        } catch (NumberFormatException e) {
            throw new ArgumentException("Requested value '" + value + "' was not found."); // NOPMD PreserveStackTrace
        }
    }

    public static Object ToObject(Type enumType, int value) {
        return new EnumBox(enumType(enumType), value);
    }

    // What follows is called on a boxed enum: an `Enum` variable.

    public static String ToString(Object value) {
        return value.toString();
    }

    public static boolean Equals(Object value, Object other) {
        return value.equals(other);
    }

    public static int GetHashCode(Object value) {
        return value.hashCode();
    }

    public static boolean HasFlag(Object value, Object flag) {
        long f = number(flag);
        return (number(value) & f) == f;
    }

    public static int CompareTo(Object value, Object other) {
        long a = number(value);
        long b = number(other);
        return a < b ? -1 : a > b ? 1 : 0;
    }
}
