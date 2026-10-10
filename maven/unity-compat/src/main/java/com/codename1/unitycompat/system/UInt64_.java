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

/// The methods of `System.UInt64`, which is the primitive `long` here and so
/// cannot carry them itself. The value comes first.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class UInt64_ {
    private UInt64_() {
    }

    public static String ToString(long v) {
        return Interop.unsignedToString(v);
    }

    public static boolean Equals(long v, long o) {
        return v == o;
    }

    public static boolean Equals(long v, Object other) {
        if (!(other instanceof Long)) {
            return false;
        }
        long o = ((Long) other).longValue();
        return v == o;
    }

    public static int GetHashCode(long v) {
        return (int) (v ^ (v >>> 32));
    }

    public static int CompareTo(long v, long o) {
        return Interop.compareUnsigned(v, o);
    }
}
