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

/// The members of `System.Array`, for the arrays that have no class of
/// their own to put them on: a JVM array for one dimension, an [MdArray]
/// for more. Which one an `Array` is, is only known when it gets here.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Array_ {
    private Array_() {
    }

    private static int vectorLength(Object array) {
        if (array instanceof Object[]) {
            return ((Object[]) array).length;
        }
        if (array instanceof int[]) {
            return ((int[]) array).length;
        }
        if (array instanceof float[]) {
            return ((float[]) array).length;
        }
        if (array instanceof boolean[]) {
            return ((boolean[]) array).length;
        }
        if (array instanceof char[]) {
            return ((char[]) array).length;
        }
        if (array instanceof byte[]) {
            return ((byte[]) array).length;
        }
        if (array instanceof short[]) {
            return ((short[]) array).length;
        }
        if (array instanceof long[]) {
            return ((long[]) array).length;
        }
        if (array instanceof double[]) {
            return ((double[]) array).length;
        }
        if (array == null) {
            throw new NullReferenceException();
        }
        throw new InvalidCastException();
    }

    public static int get_Length(Object array) {
        return array instanceof MdArray ? ((MdArray) array).length() : vectorLength(array);
    }

    public static int get_Rank(Object array) {
        if (array instanceof MdArray) {
            return ((MdArray) array).rank();
        }
        vectorLength(array);
        return 1;
    }

    public static int GetLength(Object array, int dimension) {
        if (array instanceof MdArray) {
            return ((MdArray) array).length(dimension);
        }
        int n = vectorLength(array);
        if (dimension != 0) {
            throw new IndexOutOfRangeException();
        }
        return n;
    }

    /// Always 0: C# cannot declare an array that starts anywhere else.
    public static int GetLowerBound(Object array, int dimension) {
        GetLength(array, dimension);
        return 0;
    }

    public static int GetUpperBound(Object array, int dimension) {
        return GetLength(array, dimension) - 1;
    }

    /// `Array.Clone()` of anything but a one-dimensional array of structs,
    /// which goes to [#$cloneStructs]: a shallow copy.
    public static Object Clone(Object array) {
        if (array instanceof MdArray) {
            return ((MdArray) array).copy();
        }
        if (array instanceof Object[]) {
            return ((Object[]) array).clone();
        }
        if (array instanceof int[]) {
            return ((int[]) array).clone();
        }
        if (array instanceof float[]) {
            return ((float[]) array).clone();
        }
        if (array instanceof boolean[]) {
            return ((boolean[]) array).clone();
        }
        if (array instanceof char[]) {
            return ((char[]) array).clone();
        }
        if (array instanceof byte[]) {
            return ((byte[]) array).clone();
        }
        if (array instanceof short[]) {
            return ((short[]) array).clone();
        }
        if (array instanceof long[]) {
            return ((long[]) array).clone();
        }
        if (array instanceof double[]) {
            return ((double[]) array).clone();
        }
        vectorLength(array);
        return null;
    }

    /// `Array.Clone()` of a one-dimensional array the translator knows to
    /// hold structs: each element is a value, so each is copied.
    public static Object $cloneStructs(Object array) {
        Object[] to = ((Object[]) array).clone();
        for (int i = 0; i < to.length; i++) {
            to[i] = ((Struct) to[i]).$copyValue();
        }
        return to;
    }

    private static void range(int length, int index, int count) {
        if (index < 0 || count < 0 || index > length - count) {
            throw new IndexOutOfRangeException();
        }
    }

    /// `Array.Clear(array, index, length)`: the elements go back to their
    /// default. An array of more dimensions is cleared by flat index, which
    /// is how .NET counts here too.
    public static void Clear(Object array, int index, int count) {
        if (array instanceof MdArray) {
            MdArray md = (MdArray) array;
            range(md.length(), index, count);
            md.clear(index, count);
            return;
        }
        range(vectorLength(array), index, count);
        int end = index + count;
        if (array instanceof Object[]) {
            Object[] a = (Object[]) array;
            for (int i = index; i < end; i++) {
                a[i] = null;
            }
        } else if (array instanceof int[]) {
            int[] a = (int[]) array;
            for (int i = index; i < end; i++) {
                a[i] = 0;
            }
        } else if (array instanceof float[]) {
            float[] a = (float[]) array;
            for (int i = index; i < end; i++) {
                a[i] = 0f;
            }
        } else if (array instanceof boolean[]) {
            boolean[] a = (boolean[]) array;
            for (int i = index; i < end; i++) {
                a[i] = false;
            }
        } else if (array instanceof char[]) {
            char[] a = (char[]) array;
            for (int i = index; i < end; i++) {
                a[i] = '\0';
            }
        } else if (array instanceof byte[]) {
            byte[] a = (byte[]) array;
            for (int i = index; i < end; i++) {
                a[i] = 0;
            }
        } else if (array instanceof short[]) {
            short[] a = (short[]) array;
            for (int i = index; i < end; i++) {
                a[i] = 0;
            }
        } else if (array instanceof long[]) {
            long[] a = (long[]) array;
            for (int i = index; i < end; i++) {
                a[i] = 0L;
            }
        } else {
            double[] a = (double[]) array;
            for (int i = index; i < end; i++) {
                a[i] = 0d;
            }
        }
    }

    /// `Array.Clear` of a one-dimensional array the translator knows to hold
    /// structs: each element is reset where it is.
    public static void $clearStructs(Object array, int index, int count) {
        Object[] a = (Object[]) array;
        range(a.length, index, count);
        for (int i = index; i < index + count; i++) {
            ((Struct) a[i]).$clear();
        }
    }
}
