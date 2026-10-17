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

/// A C# array of two or more dimensions: `int[,]`, `string[,,]`.
///
/// The JVM has no such thing. Its `int[][]` is an array of arrays, which
/// costs an object per row and two loads per element; .NET lays a
/// rectangular array out as one block, and so does this. Each element kind
/// has a final subclass holding one flat array in row-major order -- the
/// last index varies fastest, which is the order .NET stores, enumerates and
/// initializes them in -- and this class holds the dimensions.
///
/// Every index is checked against its own dimension before the flat index
/// is computed. That is not the array's own bounds check done twice:
/// `a[0, 5]` of a 3 by 4 array is flat index 5, a perfectly good element of
/// the block, and C# says it is an `IndexOutOfRangeException`.
public abstract class MdArray {
    private final int[] lengths;
    private final int d0;
    private final int d1;
    private final int d2;

    /// `lengths` is kept, not copied: the factories make it.
    MdArray(int[] lengths) {
        this.lengths = lengths;
        d0 = lengths[0];
        d1 = lengths[1];
        d2 = lengths.length > 2 ? lengths[2] : 1;
    }

    private static int checked(long total) {
        if (total > Integer.MAX_VALUE) {
            throw new OverflowException();
        }
        return (int) total;
    }

    private static int dimension(int length) {
        if (length < 0) {
            throw new OverflowException();
        }
        return length;
    }

    /// The number of elements of an `a` by `b` array. A negative length is
    /// an `OverflowException`, as it is on .NET.
    public static int size(int a, int b) {
        return checked((long) dimension(a) * dimension(b));
    }

    public static int size(int a, int b, int c) {
        return checked((long) size(a, b) * dimension(c));
    }

    public static int size(int[] lengths) {
        if (lengths.length < 2) {
            throw new ArgumentException("An array of this kind has two or more dimensions.");
        }
        int total = 1;
        for (int length : lengths) {
            total = checked((long) total * dimension(length));
        }
        return total;
    }

    static int[] copyOf(int[] lengths) {
        int[] copy = new int[lengths.length];
        System.arraycopy(lengths, 0, copy, 0, lengths.length);
        return copy;
    }

    /// The same dimensions, for a copy of the array.
    final int[] lengths() {
        return copyOf(lengths);
    }

    /// The flat index of `[i, j]`.
    public final int index(int i, int j) {
        if (i < 0 || i >= d0 || j < 0 || j >= d1) {
            throw new IndexOutOfRangeException();
        }
        return i * d1 + j;
    }

    /// The flat index of `[i, j, k]`.
    public final int index(int i, int j, int k) {
        if (i < 0 || i >= d0 || j < 0 || j >= d1 || k < 0 || k >= d2) {
            throw new IndexOutOfRangeException();
        }
        return (i * d1 + j) * d2 + k;
    }

    /// The flat index of an element of an array of any rank.
    public final int index(int[] indices) {
        if (indices.length != lengths.length) {
            throw new ArgumentException("Only as many indices as the array has dimensions.");
        }
        int flat = 0;
        for (int d = 0; d < lengths.length; d++) {
            int i = indices[d];
            if (i < 0 || i >= lengths[d]) {
                throw new IndexOutOfRangeException();
            }
            flat = flat * lengths[d] + i;
        }
        return flat;
    }

    /// `Array.Rank`.
    public final int rank() {
        return lengths.length;
    }

    /// `Array.GetLength(dimension)`.
    public final int length(int dimension) {
        if (dimension < 0 || dimension >= lengths.length) {
            throw new IndexOutOfRangeException();
        }
        return lengths[dimension];
    }

    /// `Array.Length`: the elements of every dimension together.
    public final int length() {
        int total = 1;
        for (int length : lengths) {
            total *= length;
        }
        return total;
    }

    /// `Array.Clone()`: the same dimensions over a copy of the elements.
    abstract MdArray copy();

    /// `Array.Clear`, on flat indices that have been checked.
    abstract void clear(int from, int count);

    /// The element at a flat index, boxed.
    abstract Object boxed(int flat);
}
