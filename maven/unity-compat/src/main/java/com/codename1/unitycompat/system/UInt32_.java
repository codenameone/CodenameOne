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

/// The methods of `System.UInt32`, which is the primitive `int` here and so
/// cannot carry them itself. The value comes first.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class UInt32_ {
    private UInt32_() {
    }

    public static String ToString(int v) {
        return Long.toString(v & 0xFFFFFFFFL);
    }

    public static boolean Equals(int v, int o) {
        return v == o;
    }

    public static boolean Equals(int v, Object other) {
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
        return Interop.compareUnsigned(v, o);
    }
}
