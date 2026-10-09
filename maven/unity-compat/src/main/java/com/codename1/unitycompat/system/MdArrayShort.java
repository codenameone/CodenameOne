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

/// A C# array of two or more dimensions whose elements are `short`: one flat
/// `short[]` in row-major order under the dimensions [MdArray] keeps.
@SuppressWarnings("PMD.MethodNamingConventions") // $-names are the protocol translated code calls
public final class MdArrayShort extends MdArray {
    /// The elements. Translated code indexes it directly where it holds a
    /// reference to an element (`ref a[i, j]`), having had the index checked.
    public final short[] data; // NOPMD the element store itself: a copy would be another array

    private MdArrayShort(int[] lengths, short[] data) {
        super(lengths);
        this.data = data;
    }

    /// `new T[a, b]`.
    public static MdArrayShort $new(int a, int b) {
        return new MdArrayShort(new int[] {a, b}, new short[size(a, b)]);
    }

    /// `new T[a, b, c]`.
    public static MdArrayShort $new(int a, int b, int c) {
        return new MdArrayShort(new int[] {a, b, c}, new short[size(a, b, c)]);
    }

    /// An array of any rank; the lengths are copied.
    public static MdArrayShort $new(int[] lengths) {
        return new MdArrayShort(copyOf(lengths), new short[size(lengths)]);
    }

    public static short get(MdArrayShort a, int i, int j) {
        return a.data[a.index(i, j)];
    }

    public static short get(MdArrayShort a, int i, int j, int k) {
        return a.data[a.index(i, j, k)];
    }

    public static short get(MdArrayShort a, int[] indices) {
        return a.data[a.index(indices)];
    }

    public static void set(MdArrayShort a, int i, int j, short value) {
        a.data[a.index(i, j)] = value;
    }

    public static void set(MdArrayShort a, int i, int j, int k, short value) {
        a.data[a.index(i, j, k)] = value;
    }

    public static void set(MdArrayShort a, int[] indices, short value) {
        a.data[a.index(indices)] = value;
    }

    @Override
    MdArray copy() {
        short[] to = new short[data.length];
        System.arraycopy(data, 0, to, 0, data.length);
        return new MdArrayShort(lengths(), to);
    }

    @Override
    void clear(int from, int count) {
        for (int i = from; i < from + count; i++) {
            data[i] = 0;
        }
    }

    @Override
    Object boxed(int flat) {
        return Short.valueOf(data[flat]);
    }
}
