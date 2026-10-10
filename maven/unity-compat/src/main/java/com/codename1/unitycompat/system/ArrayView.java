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

import com.codename1.unitycompat.system.collections.generic.IEnumerable_1;
import com.codename1.unitycompat.system.collections.generic.IEnumerator_1;

/// An array seen as the collection interfaces it implements on .NET. The
/// translator wraps an array in one where C# passes it to a parameter of
/// such a type -- `keys.Any(...)`, `new List<int>(numbers)` -- because a
/// JVM array implements nothing. The view reads the array it was made
/// from, so a change to an element shows through, as it would on .NET.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class ArrayView implements IEnumerable_1 {
    private final Object array;
    private final int length;

    private ArrayView(Object array, int length) {
        this.array = array;
        this.length = length;
    }

    /// The view of an array, or null for null.
    public static ArrayView $of(Object array) {
        if (array == null) {
            return null;
        }
        int n;
        if (array instanceof MdArray) {
            // Every element, last index fastest: the order .NET enumerates
            // an array of several dimensions in.
            n = ((MdArray) array).length();
        } else if (array instanceof Object[]) {
            n = ((Object[]) array).length;
        } else if (array instanceof int[]) {
            n = ((int[]) array).length;
        } else if (array instanceof float[]) {
            n = ((float[]) array).length;
        } else if (array instanceof boolean[]) {
            n = ((boolean[]) array).length;
        } else if (array instanceof char[]) {
            n = ((char[]) array).length;
        } else if (array instanceof byte[]) {
            n = ((byte[]) array).length;
        } else if (array instanceof short[]) {
            n = ((short[]) array).length;
        } else if (array instanceof long[]) {
            n = ((long[]) array).length;
        } else if (array instanceof double[]) {
            n = ((double[]) array).length;
        } else {
            throw new InvalidCastException();
        }
        return new ArrayView(array, n);
    }

    public int get_Count() {
        return length;
    }

    public int get_Length() {
        return length;
    }

    /// The element, boxed as the erased code that asked expects it.
    public Object get_Item(int i) {
        if (i < 0 || i >= length) {
            throw new IndexOutOfRangeException();
        }
        if (array instanceof MdArray) {
            return ((MdArray) array).boxed(i);
        }
        if (array instanceof Object[]) {
            return ((Object[]) array)[i];
        }
        if (array instanceof int[]) {
            return Integer.valueOf(((int[]) array)[i]);
        }
        if (array instanceof float[]) {
            return Float.valueOf(((float[]) array)[i]);
        }
        if (array instanceof boolean[]) {
            return ((boolean[]) array)[i] ? Boolean.TRUE : Boolean.FALSE;
        }
        if (array instanceof char[]) {
            return Character.valueOf(((char[]) array)[i]);
        }
        if (array instanceof byte[]) {
            return Byte.valueOf(((byte[]) array)[i]);
        }
        if (array instanceof short[]) {
            return Short.valueOf(((short[]) array)[i]);
        }
        if (array instanceof long[]) {
            return Long.valueOf(((long[]) array)[i]);
        }
        return Double.valueOf(((double[]) array)[i]);
    }

    @Override
    public IEnumerator_1 GetEnumerator() {
        return new Walk(this);
    }

    private static final class Walk implements IEnumerator_1 {
        private final ArrayView view;
        private int next;
        private Object current;

        Walk(ArrayView view) {
            this.view = view;
        }

        @Override
        public boolean MoveNext() {
            if (next < view.length) {
                current = view.get_Item(next++);
                return true;
            }
            current = null;
            return false;
        }

        @Override
        public Object get_Current() {
            return current;
        }

        @Override
        public void Reset() {
            next = 0;
            current = null;
        }

        @Override
        public void Dispose() {
        }
    }
}
