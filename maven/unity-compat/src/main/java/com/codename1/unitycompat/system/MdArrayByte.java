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

/// A C# array of two or more dimensions whose elements are `sbyte` or `byte`: one flat
/// `byte[]` in row-major order under the dimensions [MdArray] keeps.
@SuppressWarnings("PMD.MethodNamingConventions") // $-names are the protocol translated code calls
public final class MdArrayByte extends MdArray {
    /// The elements. Translated code indexes it directly where it holds a
    /// reference to an element (`ref a[i, j]`), having had the index checked.
    public final byte[] data; // NOPMD the element store itself: a copy would be another array

    private MdArrayByte(int[] lengths, byte[] data) {
        super(lengths);
        this.data = data;
    }

    /// `new T[a, b]`.
    public static MdArrayByte $new(int a, int b) {
        return new MdArrayByte(new int[] {a, b}, new byte[size(a, b)]);
    }

    /// `new T[a, b, c]`.
    public static MdArrayByte $new(int a, int b, int c) {
        return new MdArrayByte(new int[] {a, b, c}, new byte[size(a, b, c)]);
    }

    /// An array of any rank; the lengths are copied.
    public static MdArrayByte $new(int[] lengths) {
        return new MdArrayByte(copyOf(lengths), new byte[size(lengths)]);
    }

    public static byte get(MdArrayByte a, int i, int j) {
        return a.data[a.index(i, j)];
    }

    public static byte get(MdArrayByte a, int i, int j, int k) {
        return a.data[a.index(i, j, k)];
    }

    public static byte get(MdArrayByte a, int[] indices) {
        return a.data[a.index(indices)];
    }

    public static void set(MdArrayByte a, int i, int j, byte value) {
        a.data[a.index(i, j)] = value;
    }

    public static void set(MdArrayByte a, int i, int j, int k, byte value) {
        a.data[a.index(i, j, k)] = value;
    }

    public static void set(MdArrayByte a, int[] indices, byte value) {
        a.data[a.index(indices)] = value;
    }

    @Override
    MdArray copy() {
        byte[] to = new byte[data.length];
        System.arraycopy(data, 0, to, 0, data.length);
        return new MdArrayByte(lengths(), to);
    }

    @Override
    void clear(int from, int count) {
        for (int i = from; i < from + count; i++) {
            data[i] = 0;
        }
    }

    @Override
    Object boxed(int flat) {
        return Byte.valueOf(data[flat]);
    }
}
