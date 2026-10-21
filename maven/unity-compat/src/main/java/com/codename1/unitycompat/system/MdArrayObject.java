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

/// A C# array of two or more dimensions whose elements are references or
/// structs: one flat array in row-major order under the dimensions
/// [MdArray] keeps.
///
/// The flat array is made by the translated code, which knows the element
/// class: a `string[,]` is over a `String[]`, so that `ref a[i, j]` can be
/// handed to a method expecting a reference to a string. An array of
/// structs arrives with an object in every element, as a struct array of
/// one dimension does, and its elements are assigned into, never replaced.
@SuppressWarnings("PMD.MethodNamingConventions") // $-names are the protocol translated code calls
public final class MdArrayObject extends MdArray {
    /// The elements. Translated code indexes it directly where it holds a
    /// reference to an element (`ref a[i, j]`), having had the index checked.
    public final Object[] data; // NOPMD the element store itself: a copy would be another array
    private final boolean structs;

    private MdArrayObject(int[] lengths, Object[] data, boolean structs) {
        super(lengths);
        this.data = data;
        this.structs = structs;
    }

    /// `new T[a, b]` over `data`, which has `a * b` elements.
    public static MdArrayObject $wrap(Object[] data, boolean structs, int a, int b) {
        return new MdArrayObject(new int[] {a, b}, data, structs);
    }

    /// `new T[a, b, c]` over `data`.
    public static MdArrayObject $wrap(Object[] data, boolean structs, int a, int b, int c) {
        return new MdArrayObject(new int[] {a, b, c}, data, structs);
    }

    /// An array of any rank over `data`; the lengths are copied.
    public static MdArrayObject $wrap(Object[] data, boolean structs, int[] lengths) {
        return new MdArrayObject(copyOf(lengths), data, structs);
    }

    public static Object get(MdArrayObject a, int i, int j) {
        return a.data[a.index(i, j)];
    }

    public static Object get(MdArrayObject a, int i, int j, int k) {
        return a.data[a.index(i, j, k)];
    }

    public static Object get(MdArrayObject a, int[] indices) {
        return a.data[a.index(indices)];
    }

    public static void set(MdArrayObject a, int i, int j, Object value) {
        a.data[a.index(i, j)] = value;
    }

    public static void set(MdArrayObject a, int i, int j, int k, Object value) {
        a.data[a.index(i, j, k)] = value;
    }

    public static void set(MdArrayObject a, int[] indices, Object value) {
        a.data[a.index(indices)] = value;
    }

    @Override
    MdArray copy() {
        // An array's own clone is the one way to another array of the same
        // element class without reflection, and the one clone ParparVM has.
        Object[] to = data.clone();
        if (structs) {
            for (int i = 0; i < to.length; i++) {
                to[i] = ((Struct) to[i]).$copyValue();
            }
        }
        return new MdArrayObject(lengths(), to, structs);
    }

    @Override
    void clear(int from, int count) {
        for (int i = from; i < from + count; i++) {
            if (structs) {
                ((Struct) data[i]).$clear();
            } else {
                data[i] = null;
            }
        }
    }

    @Override
    Object boxed(int flat) {
        return structs ? ((Struct) data[flat]).$copyValue() : data[flat];
    }
}
