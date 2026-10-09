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
/// say.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Type {
    private static final HashMap byClass = new HashMap();

    private final String fullName;
    private final Class type;
    final String[] names;
    final long[] values;
    final boolean flags;

    private Type(String fullName, Class type, String[] names, long[] values, boolean flags) {
        this.fullName = fullName;
        this.type = type;
        this.names = names;
        this.values = values;
        this.flags = flags;
    }

    /// The type of a class, struct or primitive.
    public static Type $of(Class type, String fullName) {
        Type t = (Type) byClass.get(type);
        if (t == null) {
            t = new Type(fullName, type, null, null, false);
            byClass.put(type, t);
        }
        return t;
    }

    /// The type of an enum, with its members in declaration order.
    public static Type $enum(String fullName, String[] names, long[] values, boolean flags) {
        return new Type(fullName, null, names, values, flags);
    }

    /// A boxed value of this enum.
    public static Object $box(long value, Type type) {
        return new EnumBox(type, value);
    }

    public static String $name(int value, Type type) {
        return type.nameOf(value);
    }

    public static String $name(long value, Type type) {
        return type.nameOf(value);
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
    String nameOf(long value) {
        int at = indexOf(value);
        if (at >= 0) {
            return names[at];
        }
        if (flags && value != 0 && values != null) {
            long rest = value;
            String out = null;
            // From the last member down, as .NET does, so that a member
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
        return Long.toString(value);
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
