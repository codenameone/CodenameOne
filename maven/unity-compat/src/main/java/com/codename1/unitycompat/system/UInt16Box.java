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

/// A boxed `ushort`: what the value is once C# puts it in an `object` or
/// hands it to erased generic code -- a `List<ushort>`, a format argument.
/// A `ushort` and a `char` are both a Java `char`, and
/// one JDK box for both made the number print as a character and answer
/// `is char`. A `char` keeps the JDK box, so nothing about it changed; the translator names this class wherever it boxes, unboxes or
/// tests the unsigned one (`Names.boxClass`).
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class UInt16Box implements IComparable {
    // The values a program boxes most are small, as the JDK's own boxes
    // assume: a box for each is made once.
    private static final UInt16Box[] SMALL = small();

    /// The bits of the value, in the Java type the unboxed value has.
    public final char value;

    private UInt16Box(char value) {
        this.value = value;
    }

    private static UInt16Box[] small() {
        UInt16Box[] all = new UInt16Box[256];
        for (int i = 0; i < all.length; i++) {
            all[i] = new UInt16Box((char) i);
        }
        return all;
    }

    public static UInt16Box valueOf(char value) {
        return value < 256 ? SMALL[value] : new UInt16Box(value);
    }

    public char charValue() {
        return value;
    }

    /// `IComparable.CompareTo`: after null, and only against the same type.
    @Override
    public int CompareTo(Object other) {
        if (other == null) {
            return 1;
        }
        if (other instanceof UInt16Box) {
            return UInt16_.CompareTo(value, ((UInt16Box) other).value);
        }
        throw new ArgumentException("Object must be of type UInt16.");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UInt16Box && ((UInt16Box) o).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return UInt16_.ToString(value);
    }
}
