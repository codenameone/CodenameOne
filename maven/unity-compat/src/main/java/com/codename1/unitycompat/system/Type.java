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

import java.util.HashMap;

/// `System.Type`, as far as translated code can use one: `typeof(T)`
/// compared, printed, handed to `GetComponent` or to `Enum`.
///
/// There is no reflection behind it. The type of a class is its Java class
/// and its .NET name, made on first use and kept, so that two `typeof` of
/// one type are the same object. The type of an enum is built by the class
/// the translator writes for that enum, and carries its members' names and
/// values, which is the one thing about an enum the integer under it cannot
/// say, and the integer type it is stored as.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Type {
    // The integer type under an enum.
    static final int I1 = 1;
    static final int U1 = 2;
    static final int I2 = 3;
    static final int U2 = 4;
    static final int I4 = 5;
    static final int U4 = 6;
    static final int I8 = 7;
    static final int U8 = 8;

    /// The first type asked for with each Java class: nearly always the
    /// only one, so `typeof` is one lookup.
    private static final HashMap byClass = new HashMap();
    /// The types that share a Java class with an earlier one, by .NET name.
    private static final HashMap byName = new HashMap();

    private final String fullName;
    private final Class type;
    final String[] names;
    final long[] values;
    final boolean flags;
    /// One of [#I1] to [#U8] for an enum, 0 for anything else.
    final int underlying;

    private Type(String fullName, Class type, String[] names, long[] values, boolean flags, int underlying) {
        this.fullName = fullName;
        this.type = type;
        this.names = names;
        this.values = values;
        this.flags = flags;
        this.underlying = underlying;
    }

    /// The type of a class, struct or primitive.
    ///
    /// A type is its .NET name, not its Java class: `int` and `uint` are
    /// both an `Integer` here, as are `long` and `ulong`, `byte` and
    /// `sbyte`, and `char` and `ushort`, and `typeof(int) == typeof(uint)`
    /// is false. So the class alone finds a type only when the name agrees;
    /// a second name for a class gets a type of its own, kept by that name.
    ///
    /// What erasure leaves the same stays the same: `List<int>` and
    /// `List<string>` are one class and arrive under one name, so their
    /// types are one object.
    public static Type $of(Class type, String fullName) {
        Type t = (Type) byClass.get(type);
        if (t == null) {
            t = new Type(fullName, type, null, null, false, 0);
            byClass.put(type, t);
            return t;
        }
        if (t.fullName.equals(fullName)) {
            return t;
        }
        // The name decides the class -- the translator derives both from
        // the one C# type -- so the name is all the key this needs.
        t = (Type) byName.get(fullName);
        if (t == null) {
            t = new Type(fullName, type, null, null, false, 0);
            byName.put(fullName, t);
        }
        return t;
    }

    private static int underlyingOf(String name) {
        if ("System.Int32".equals(name)) {
            return I4;
        }
        if ("System.Byte".equals(name)) {
            return U1;
        }
        if ("System.SByte".equals(name)) {
            return I1;
        }
        if ("System.Int16".equals(name)) {
            return I2;
        }
        if ("System.UInt16".equals(name)) {
            return U2;
        }
        if ("System.UInt32".equals(name)) {
            return U4;
        }
        if ("System.Int64".equals(name)) {
            return I8;
        }
        if ("System.UInt64".equals(name)) {
            return U8;
        }
        throw new ArgumentException("An enum cannot be stored as " + name + ".");
    }

    /// The type of an enum. The members arrive in declaration order with
    /// the values the metadata has, and `underlying` is the .NET name of
    /// the integer type the enum is stored as, `System.Int32` unless the
    /// declaration says otherwise.
    ///
    /// They are kept the way .NET hands them out, which is sorted by value
    /// taken as an unsigned number -- `{All = -1, None = 0, One = 1}` is
    /// None, One, All -- since `GetValues`, `GetNames` and the names a flags
    /// value prints as all follow that order. Members with one value keep
    /// the order they were declared in.
    public static Type $enum(String fullName, String[] names, long[] values, boolean flags, String underlying) {
        Type t = new Type(fullName, null, names, values, flags, underlyingOf(underlying));
        for (int i = 0; i < values.length; i++) {
            values[i] = t.held(values[i]);
        }
        for (int i = 1; i < values.length; i++) {
            long v = values[i];
            String n = names[i];
            int j = i - 1;
            while (j >= 0 && Interop.compareUnsigned(values[j], v) > 0) {
                values[j + 1] = values[j];
                names[j + 1] = names[j];
                j--;
            }
            values[j + 1] = v;
            names[j + 1] = n;
        }
        return t;
    }

    /// A value of this enum as translated code holds it once widened to a
    /// `long`, which is how every value here is kept and compared: an
    /// `int` and a `uint` are both a JVM `int` and widen with their sign, a
    /// `byte` and a `ushort` are kept without one. The metadata instead
    /// writes a `uint` member of 4000000000 as that positive number, and
    /// without this it would never equal the value a script boxes.
    long held(long value) {
        switch (underlying) {
            case I1:
                return (byte) value;
            case U1:
                return value & 0xFFL;
            case I2:
                return (short) value;
            case U2:
                return value & 0xFFFFL;
            case I4:
            case U4:
                return (int) value;
            default:
                return value;
        }
    }

    /// A boxed value of this enum.
    public static Object $box(long value, Type type) {
        return new EnumBox(type, type.held(value));
    }

    public static String $name(int value, Type type) {
        return type.nameOf(value);
    }

    public static String $name(long value, Type type) {
        return type.nameOf(value);
    }

    // What follows is a method of `System.Enum` called on a variable of the
    // enum's own type, where the value is a bare integer: the translator
    // sends the enum's type after the arguments, as it does for `ToString`,
    // because `Enum` refuses a value of another enum and the integer alone
    // does not say which enum it is.

    public static boolean $hasFlag(int value, Object flag, Type type) {
        return Enum_.hasFlag(type, value, flag);
    }

    public static boolean $hasFlag(long value, Object flag, Type type) {
        return Enum_.hasFlag(type, value, flag);
    }

    public static int $compareTo(int value, Object other, Type type) {
        return Enum_.compare(type, value, other);
    }

    public static int $compareTo(long value, Object other, Type type) {
        return Enum_.compare(type, value, other);
    }

    public static boolean $equals(int value, Object other, Type type) {
        return other instanceof EnumBox && ((EnumBox) other).type == type // NOPMD CompareObjectsWithEquals
                && ((EnumBox) other).value == type.held(value);
    }

    public static boolean $equals(long value, Object other, Type type) {
        return other instanceof EnumBox && ((EnumBox) other).type == type // NOPMD CompareObjectsWithEquals
                && ((EnumBox) other).value == type.held(value);
    }

    /// The Java class, or null for an enum, which has none.
    public Class $class() {
        return type;
    }

    int indexOf(long value) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == value) {
                    return i;
                }
            }
        }
        return -1;
    }

    /// What `ToString` gives for a value of this enum: the name of the
    /// member with that value; for a flags enum with no such member, the
    /// names of the members that make it up; otherwise the number.
    String nameOf(long given) {
        long value = held(given);
        int at = indexOf(value);
        if (at >= 0) {
            return names[at];
        }
        if (flags && value != 0 && values != null) {
            long rest = value;
            String out = null;
            // From the largest member down, as .NET does, so that a member
            // covering several bits wins over the bits it is made of.
            for (int i = values.length - 1; i >= 0 && rest != 0; i--) {
                long v = values[i];
                if (v != 0 && (rest & v) == v) {
                    rest -= v;
                    out = out == null ? names[i] : names[i] + ", " + out;
                }
            }
            if (rest == 0 && out != null) {
                return out;
            }
        }
        // The number, as the enum's own integer type prints it.
        if (underlying == U4) {
            return Long.toString(value & 0xFFFFFFFFL);
        }
        return underlying == U8 ? Interop.unsignedToString(value) : Long.toString(value);
    }

    public String get_Name() {
        int dot = fullName.lastIndexOf('.');
        int plus = fullName.lastIndexOf('+');
        return fullName.substring((plus > dot ? plus : dot) + 1);
    }

    public String get_FullName() {
        return fullName;
    }

    public boolean get_IsEnum() {
        return names != null;
    }

    public static boolean op_Equality(Type a, Type b) {
        return a == b; // NOPMD CompareObjectsWithEquals
    }

    public static boolean op_Inequality(Type a, Type b) {
        return a != b; // NOPMD CompareObjectsWithEquals
    }

    @Override
    public String toString() {
        return fullName;
    }
}
