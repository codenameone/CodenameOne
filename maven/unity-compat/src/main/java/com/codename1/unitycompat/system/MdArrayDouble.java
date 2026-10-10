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

/// A C# array of two or more dimensions whose elements are `double`: one flat
/// `double[]` in row-major order under the dimensions [MdArray] keeps.
@SuppressWarnings("PMD.MethodNamingConventions") // $-names are the protocol translated code calls
public final class MdArrayDouble extends MdArray {
    /// The elements. Translated code indexes it directly where it holds a
    /// reference to an element (`ref a[i, j]`), having had the index checked.
    public final double[] data; // NOPMD the element store itself: a copy would be another array

    private MdArrayDouble(int[] lengths, double[] data) {
        super(lengths);
        this.data = data;
    }

    /// `new T[a, b]`.
    public static MdArrayDouble $new(int a, int b) {
        return new MdArrayDouble(new int[] {a, b}, new double[size(a, b)]);
    }

    /// `new T[a, b, c]`.
    public static MdArrayDouble $new(int a, int b, int c) {
        return new MdArrayDouble(new int[] {a, b, c}, new double[size(a, b, c)]);
    }

    /// An array of any rank; the lengths are copied.
    public static MdArrayDouble $new(int[] lengths) {
        return new MdArrayDouble(copyOf(lengths), new double[size(lengths)]);
    }

    public static double get(MdArrayDouble a, int i, int j) {
        return a.data[a.index(i, j)];
    }

    public static double get(MdArrayDouble a, int i, int j, int k) {
        return a.data[a.index(i, j, k)];
    }

    public static double get(MdArrayDouble a, int[] indices) {
        return a.data[a.index(indices)];
    }

    public static void set(MdArrayDouble a, int i, int j, double value) {
        a.data[a.index(i, j)] = value;
    }

    public static void set(MdArrayDouble a, int i, int j, int k, double value) {
        a.data[a.index(i, j, k)] = value;
    }

    public static void set(MdArrayDouble a, int[] indices, double value) {
        a.data[a.index(indices)] = value;
    }

    @Override
    MdArray copy() {
        double[] to = new double[data.length];
        System.arraycopy(data, 0, to, 0, data.length);
        return new MdArrayDouble(lengths(), to);
    }

    @Override
    void clear(int from, int count) {
        for (int i = from; i < from + count; i++) {
            data[i] = 0d;
        }
    }

    @Override
    Object boxed(int flat) {
        return Double.valueOf(data[flat]);
    }
}
