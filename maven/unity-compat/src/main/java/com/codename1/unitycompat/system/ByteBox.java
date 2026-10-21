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

/// A boxed `byte`: what the value is once C# puts it in an `object` or
/// hands it to erased generic code -- a `List<byte>`, a format argument.
/// A `byte` and an `sbyte` are both a Java `byte`, and
/// one JDK box for both made the value print, order and answer `is` as
/// the signed one. The signed type keeps the JDK box, so nothing about it
/// changed; the translator names this class wherever it boxes, unboxes or
/// tests the unsigned one (`Names.boxClass`).
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class ByteBox implements IComparable {
    // The values a program boxes most are small, as the JDK's own boxes
    // assume: a box for each is made once.
    private static final ByteBox[] SMALL = small();

    /// The bits of the value, in the Java type the unboxed value has.
    public final byte value;

    private ByteBox(byte value) {
        this.value = value;
    }

    private static ByteBox[] small() {
        ByteBox[] all = new ByteBox[256];
        for (int i = 0; i < all.length; i++) {
            all[i] = new ByteBox((byte) i);
        }
        return all;
    }

    public static ByteBox valueOf(byte value) {
        return SMALL[value & 0xFF];
    }

    public byte byteValue() {
        return value;
    }

    /// `IComparable.CompareTo`: after null, and only against the same type.
    @Override
    public int CompareTo(Object other) {
        if (other == null) {
            return 1;
        }
        if (other instanceof ByteBox) {
            return Byte_.CompareTo(value, ((ByteBox) other).value);
        }
        throw new ArgumentException("Object must be of type Byte.");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ByteBox && ((ByteBox) o).value == value;
    }

    @Override
    public int hashCode() {
        return value & 0xFF;
    }

    @Override
    public String toString() {
        return Byte_.ToString(value);
    }
}
