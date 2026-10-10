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

/// The methods of `System.Single`, which is the primitive `float` here and so
/// cannot carry them itself. The value comes first.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Single_ {
    private Single_() {
    }

    public static String ToString(float v) {
        return Interop.format(v);
    }

    public static boolean Equals(float v, float o) {
        return v == o || (v != v && o != o);
    }

    public static boolean Equals(float v, Object other) {
        if (!(other instanceof Float)) {
            return false;
        }
        float o = ((Float) other).floatValue();
        return v == o || (v != v && o != o);
    }

    public static int GetHashCode(float v) {
        // Zero and negative zero are equal, so they hash alike.
        return v == 0f ? 0 : Float.floatToIntBits(v);
    }

    public static int CompareTo(float v, float o) {
        return (v < o ? -1 : v > o ? 1 : v == o ? 0 : v != v ? (o != o ? 0 : -1) : 1);
    }
}
