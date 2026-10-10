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

/// The methods of `System.Object`, which is `java.lang.Object` here. A
/// class written in C# overrides `ToString`, `Equals` and `GetHashCode` as
/// the Java methods of the same meaning, so these mostly forward; what they
/// add is the .NET spelling of the values that are JDK boxes.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Object_ {
    private Object_() {
    }

    public static String ToString(Object o) {
        if (o instanceof Boolean) {
            return Boolean_.ToString(((Boolean) o).booleanValue());
        }
        if (o instanceof Float) {
            return Single_.ToString(((Float) o).floatValue());
        }
        if (o instanceof Double) {
            return Double_.ToString(((Double) o).doubleValue());
        }
        if (o == null) {
            throw new NullReferenceException();
        }
        return o.toString();
    }

    /// Both `a.Equals(b)` and the static `Object.Equals(a, b)`.
    public static boolean Equals(Object a, Object b) {
        return Interop.areEqual(a, b);
    }

    public static boolean ReferenceEquals(Object a, Object b) {
        return a == b; // NOPMD CompareObjectsWithEquals
    }

    public static int GetHashCode(Object o) {
        if (o == null) {
            throw new NullReferenceException();
        }
        return Interop.hash(o);
    }
}
