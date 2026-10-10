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
///
/// A value reaches these methods as an [EnumBox], which knows its enum, or
/// as a boxed integer. .NET is particular about which it takes where, and
/// so is this: `IsDefined`, `HasFlag` and `CompareTo` refuse a value of
/// another enum, where `GetName` reads the number out of anything.
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

    private static boolean isInteger(Object value) {
        return value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte
                || value instanceof Character;
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
        if (value instanceof Character) {
            // A `ushort` is a `char` here, and boxes as one.
            return ((Character) value).charValue();
        }
        if (value == null) {
            throw new ArgumentNullException();
        }
        throw new ArgumentException(
                "The value passed in must be an enum base or an underlying type for an enum, such as an Int32.");
    }

    /// True if a boxed integer is of the type the enum is stored as, as
    /// far as that survives here: an `int` and a `uint` are both an
    /// `Integer`, and so on for each width.
    private static boolean isUnderlying(Type t, Object value) {
        switch (t.underlying) {
            case Type.I1:
            case Type.U1:
                return value instanceof Byte;
            case Type.I2:
                return value instanceof Short;
            case Type.U2:
                return value instanceof Character;
            case Type.I8:
            case Type.U8:
                return value instanceof Long;
            default:
                return value instanceof Integer;
        }
    }

    private static ArgumentException otherEnum(Object passed, Type t) {
        return new ArgumentException("Object must be the same type as the enum. The type passed in was '"
                + (passed instanceof EnumBox ? ((EnumBox) passed).type.get_FullName() : "an integer")
                + "'; the enum type was '" + t.get_FullName() + "'.");
    }

    /// True for the name of a member, or for a value some member has. The
    /// value has to be of this enum or of the integer type under it:
    /// a member of another enum is an `ArgumentException`, as in .NET, and
    /// not an answer about whichever member has the same number.
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
        if (value instanceof EnumBox) {
            if (((EnumBox) value).type != t) { // NOPMD CompareObjectsWithEquals
                throw otherEnum(value, t);
            }
        } else if (!isInteger(value)) {
            throw new InvalidOperationException("Unknown enum type.");
        } else if (!isUnderlying(t, value)) {
            throw new ArgumentException(
                    "Enum underlying type and the object must be same type or object must be a String.");
        }
        return t.indexOf(t.held(number(value))) >= 0;
    }

    /// The name of the member with this number, or null. Unlike its
    /// neighbours this one does not ask whose value it was handed: .NET's
    /// `Enum.GetName(typeof(A), B.x)` reads the number out of `B.x` and
    /// answers with the member of `A` that has it, and takes an integer of
    /// any width the same way. Only something that is neither an enum nor
    /// an integer is refused.
    public static String GetName(Type enumType, Object value) {
        Type t = enumType(enumType);
        int at = t.indexOf(t.held(number(value)));
        return at < 0 ? null : t.names[at];
    }

    /// The names, in the order of their values: see [#GetValues].
    public static String[] GetNames(Type enumType) {
        Type t = enumType(enumType);
        String[] out = new String[t.names.length];
        System.arraycopy(t.names, 0, out, 0, out.length);
        return out;
    }

    /// The values, in .NET's order: ascending as unsigned numbers, so the
    /// negative members of a signed enum come last. The enum's [Type]
    /// keeps them that way.
    ///
    /// The result is an array of the integer type the enum is stored as,
    /// because an `E[]` is that array here and `(E[]) Enum.GetValues(...)`
    /// is a checked cast: a `long[]` for an enum over `long` or `ulong`,
    /// which an `int[]` would both fail the cast and cut in half, a
    /// `byte[]`, `short[]` or `char[]` for the narrow ones. The elements are
    /// therefore the bare numbers and not boxed enums, so one read out as
    /// an `object` prints as its number.
    public static Object GetValues(Type enumType) {
        Type t = enumType(enumType);
        long[] values = t.values;
        int n = values.length;
        switch (t.underlying) {
            case Type.I1:
            case Type.U1: {
                byte[] out = new byte[n];
                for (int i = 0; i < n; i++) {
                    out[i] = (byte) values[i];
                }
                return out;
            }
            case Type.I2: {
                short[] out = new short[n];
                for (int i = 0; i < n; i++) {
                    out[i] = (short) values[i];
                }
                return out;
            }
            case Type.U2: {
                char[] out = new char[n];
                for (int i = 0; i < n; i++) {
                    out[i] = (char) values[i];
                }
                return out;
            }
            case Type.I8:
            case Type.U8: {
                long[] out = new long[n];
                System.arraycopy(values, 0, out, 0, n);
                return out;
            }
            default: {
                int[] out = new int[n];
                for (int i = 0; i < n; i++) {
                    out[i] = (int) values[i];
                }
                return out;
            }
        }
    }

    public static Object Parse(Type enumType, String value) {
        return Parse(enumType, value, false);
    }

    public static Object Parse(Type enumType, String value, boolean ignoreCase) {
        Type t = enumType(enumType);
        if (value == null) {
            throw new ArgumentNullException();
        }
        return parse(t, value, ignoreCase, true);
    }

    /// `Enum.TryParse(Type, string, out object)`. The `out object` is an
    /// array and an index, like every reference to something that is not a
    /// struct. Text that names no member, or a number the enum cannot
    /// hold, is false and a null result, never an exception.
    public static boolean TryParse(Type enumType, String value, Object[] result, int at) {
        return TryParse(enumType, value, false, result, at);
    }

    public static boolean TryParse(Type enumType, String value, boolean ignoreCase, Object[] result, int at) {
        Type t = enumType(enumType);
        Object parsed = value == null ? null : parse(t, value, ignoreCase, false);
        result[at] = parsed;
        return parsed != null;
    }

    // The limits of the integer types an enum can be stored as, in the
    // order of the constants in `Type`.
    private static final long[] SMALLEST = {0, -128, 0, -32768, 0, Integer.MIN_VALUE, 0, Long.MIN_VALUE, 0};
    private static final long[] LARGEST = {0, 127, 255, 32767, 65535, Integer.MAX_VALUE, 0xFFFFFFFFL, Long.MAX_VALUE, -1};

    /// What `Parse` and `TryParse` share. Text that starts as a number is
    /// the number, of the enum's integer type. Anything else is one member
    /// name or several with commas between -- `"Read, Write"` -- each with
    /// white space around it allowed, and the result is their values or'ed
    /// together, for a flags enum or not. A failure is an exception when
    /// `throwing` and null otherwise.
    private static Object parse(Type t, String value, boolean ignoreCase, boolean throwing) {
        String text = String_.Trim(value);
        int n = text.length();
        if (n == 0) {
            if (throwing) {
                throw new ArgumentException("Must specify valid information for parsing in the string.");
            }
            return null;
        }
        char c = text.charAt(0);
        // The ASCII digits only: a digit of another script is `char.IsDigit`
        // and still no number to parse, so such a text is looked up as a name.
        if (Char_.isAsciiDigit(c) || c == '-' || c == '+') {
            // Not `Long.parseLong`: digits read by hand need no exception
            // to say that the text was a name after all, and know which of
            // a `ulong`'s twenty digits is one too many.
            boolean negative = c == '-';
            int at = c == '-' || c == '+' ? 1 : 0;
            boolean number = at < n;
            boolean overflow = false;
            boolean unsigned64 = t.underlying == Type.U8;
            long magnitude = 0;
            for (; at < n && number; at++) {
                int digit = text.charAt(at) - '0';
                if (digit < 0 || digit > 9) {
                    number = false;
                } else if (!overflow) {
                    // Within 2^64 - 1, which ends in 5 after 1844674407370955161.
                    int against = Interop.compareUnsigned(magnitude, 1844674407370955161L);
                    if (against > 0 || (against == 0 && digit > 5)) {
                        overflow = true;
                    } else {
                        magnitude = magnitude * 10 + digit;
                    }
                }
            }
            if (number) {
                long parsed;
                if (overflow) {
                    parsed = 0;
                } else if (unsigned64) {
                    overflow = negative && magnitude != 0;
                    parsed = magnitude;
                } else if (negative) {
                    // Down to -2^63, whose magnitude is that same bit pattern.
                    overflow = magnitude < 0 && magnitude != Long.MIN_VALUE;
                    parsed = -magnitude;
                    overflow = overflow || parsed < SMALLEST[t.underlying];
                } else {
                    overflow = magnitude < 0 || magnitude > LARGEST[t.underlying];
                    parsed = magnitude;
                }
                if (!overflow) {
                    return new EnumBox(t, t.held(parsed));
                }
                if (throwing) {
                    throw new OverflowException("Value was either too large or too small for the enum's type.");
                }
                return null;
            }
            // Not a number: it is looked for as a name, and not found.
        }
        long result = 0;
        int start = 0;
        boolean found = true;
        while (found && start <= n) {
            int comma = text.indexOf(',', start);
            int end = comma < 0 ? n : comma;
            String name = String_.Trim(text.substring(start, end));
            found = false;
            for (int i = 0; i < t.names.length && !found; i++) {
                // Per character, with no locale: a member name is not prose.
                if (ignoreCase ? t.names[i].equalsIgnoreCase(name) : t.names[i].equals(name)) {
                    result |= t.values[i];
                    found = true;
                }
            }
            start = end + 1;
        }
        if (found) {
            return new EnumBox(t, t.held(result));
        }
        if (throwing) {
            throw new ArgumentException("Requested value '" + value + "' was not found.");
        }
        return null;
    }

    public static Object ToObject(Type enumType, int value) {
        Type t = enumType(enumType);
        return new EnumBox(t, t.held(value));
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

    /// The enum a boxed value is of, or null for a boxed integer: an enum
    /// declared outside the translated assemblies has no [Type] here and
    /// boxes as the integer under it.
    private static Type typeOf(Object value) {
        return value instanceof EnumBox ? ((EnumBox) value).type : null;
    }

    public static boolean HasFlag(Object value, Object flag) {
        return hasFlag(typeOf(value), number(value), flag);
    }

    public static int CompareTo(Object value, Object other) {
        return compare(typeOf(value), number(value), other);
    }

    /// `HasFlag` for a value of the enum `t`. The flag has to be of the
    /// same enum: .NET throws for any other, where the numbers alone would
    /// answer for a member that merely shares its bits.
    static boolean hasFlag(Type t, long value, Object flag) {
        if (flag == null) {
            throw new ArgumentNullException();
        }
        if (typeOf(flag) != t) { // NOPMD CompareObjectsWithEquals
            throw new ArgumentException("The argument type is not the same as the enum type"
                    + (t == null ? "." : " '" + t.get_FullName() + "'."));
        }
        long f = number(flag);
        long v = t == null ? value : t.held(value);
        return (v & f) == f;
    }

    /// `CompareTo` for a value of the enum `t`: 1 against null, an
    /// `ArgumentException` against anything that is not of the same enum,
    /// and otherwise the order of the two numbers in the enum's integer
    /// type -- unsigned for an enum over `uint` or `ulong`.
    static int compare(Type t, long value, Object other) {
        if (other == null) {
            return 1;
        }
        if (typeOf(other) != t || (t == null && !isInteger(other))) { // NOPMD CompareObjectsWithEquals
            if (t == null) {
                throw new ArgumentException("Object must be the same type as the enum.");
            }
            throw otherEnum(other, t);
        }
        long a = t == null ? value : t.held(value);
        long b = number(other);
        if (t != null && (t.underlying == Type.U4 || t.underlying == Type.U8)) {
            return Interop.compareUnsigned(a, b);
        }
        return a < b ? -1 : a > b ? 1 : 0;
    }
}
