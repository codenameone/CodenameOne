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
package com.codename1.unitycompat.system.collections.generic;

import com.codename1.unitycompat.system.ArgumentNullException;
import com.codename1.unitycompat.system.ArgumentOutOfRangeException;
import com.codename1.unitycompat.system.Interop;
import com.codename1.unitycompat.system.InvalidOperationException;
import com.codename1.unitycompat.system.NotSupportedException;
import com.codename1.unitycompat.system.Struct;

/// `System.Collections.Generic.List<T>`. Elements are `Object`: the
/// translator boxes a primitive and copies a struct on the way in.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class List_1 implements ICollection_1 {
    private Object[] items;
    private int size;
    private int version;

    public List_1() {
        items = new Object[4];
    }

    public List_1(int capacity) {
        items = new Object[capacity < 4 ? 4 : capacity];
    }

    /// `new List<T>(IEnumerable<T>)`: the elements of a collection, in its
    /// order. An array arrives as an [com.codename1.unitycompat.system.ArrayView].
    public List_1(IEnumerable_1 values) {
        if (values == null) {
            throw new ArgumentNullException();
        }
        int n = values instanceof List_1 ? ((List_1) values).size : 0;
        items = new Object[n < 4 ? 4 : n];
        AddRange(values);
    }

    private void check(int index) {
        if (index < 0 || index >= size) {
            throw new ArgumentOutOfRangeException();
        }
    }

    @Override
    public int get_Count() {
        return size;
    }

    public Object get_Item(int index) {
        check(index);
        return items[index];
    }

    public void set_Item(int index, Object value) {
        check(index);
        items[index] = value;
        version++;
    }

    public void Add(Object value) {
        if (size == items.length) {
            Object[] grown = new Object[size * 2];
            System.arraycopy(items, 0, grown, 0, size);
            items = grown;
        }
        items[size++] = value;
        version++;
    }

    public void Insert(int index, Object value) {
        if (index < 0 || index > size) {
            throw new ArgumentOutOfRangeException();
        }
        Add(value);
        System.arraycopy(items, index, items, index + 1, size - 1 - index);
        items[index] = value;
    }

    public int IndexOf(Object value) {
        for (int i = 0; i < size; i++) {
            // Not `equals` alone: 0f and -0f are one value to .NET.
            if (Interop.areEqual(value, items[i])) {
                return i;
            }
        }
        return -1;
    }

    public boolean Contains(Object value) {
        return IndexOf(value) >= 0;
    }

    public void RemoveAt(int index) {
        check(index);
        System.arraycopy(items, index + 1, items, index, size - 1 - index);
        items[--size] = null;
        version++;
    }

    public boolean Remove(Object value) {
        int at = IndexOf(value);
        if (at < 0) {
            return false;
        }
        RemoveAt(at);
        return true;
    }

    /// A copy of the elements. The translator turns the `Object[]` into the
    /// `T[]` the caller declared.
    public Object[] ToArray() {
        Object[] copy = new Object[size];
        System.arraycopy(items, 0, copy, 0, size);
        return copy;
    }

    public void AddRange(IEnumerable_1 values) {
        if (values == null) {
            throw new ArgumentNullException();
        }
        if (values instanceof List_1) {
            // Indexed: a list may be added to itself.
            List_1 other = (List_1) values;
            int n = other.size;
            for (int i = 0; i < n; i++) {
                Add(other.items[i]);
            }
            return;
        }
        // Disposed as `foreach` would: an iterator's `finally` blocks run
        // from there, also when the walk ends early because it threw.
        IEnumerator_1 e = values.GetEnumerator();
        try {
            while (e.MoveNext()) {
                // A struct is copied on its way in: what walks an array
                // hands out the array's own element, which `a[0].x = 1`
                // changes in place, and .NET's list has a value of its own.
                Object v = e.get_Current();
                Add(v instanceof Struct ? ((Struct) v).$copyValue() : v);
            }
        } finally {
            e.Dispose();
        }
    }

    public void Reverse() {
        for (int i = 0, j = size - 1; i < j; i++, j--) {
            Object t = items[i];
            items[i] = items[j];
            items[j] = t;
        }
        version++;
    }

    @Override
    public void Clear() {
        for (int i = 0; i < size; i++) {
            items[i] = null;
        }
        size = 0;
        version++;
    }

    /// The interfaces' `GetEnumerator`: called through a reference that
    /// does not know a struct is coming back, so the result is boxed.
    @Override
    public Enumerator GetEnumerator() {
        return GetEnumerator(new Enumerator());
    }

    /// Declared to return the struct, as in .NET; the compiler adds the
    /// bridges that make it the interfaces' `GetEnumerator` as well. Like
    /// every method that returns a struct it is handed the object to fill in.
    public Enumerator GetEnumerator(Enumerator e) {
        e.$clear();
        e.list = this;
        e.version = version;
        return e;
    }

    /// `List<T>.Enumerator`, a struct in .NET. It therefore follows the
    /// protocol the translator expects of one: a no-argument constructor
    /// and `$copy`, `$assign` and `$clear`.
    public static final class Enumerator implements IEnumerator_1, Struct {
        List_1 list;
        int index;
        int version;
        Object current;

        public Enumerator $copy() {
            Enumerator e = new Enumerator();
            e.$assign(this);
            return e;
        }

        @Override
        public Object $copyValue() {
            return $copy();
        }

        public void $assign(Enumerator other) {
            list = other.list;
            index = other.index;
            version = other.version;
            current = other.current;
        }

        @Override
        public void $clear() {
            list = null;
            index = 0;
            version = 0;
            current = null;
        }

        @Override
        public boolean MoveNext() {
            if (version != list.version) {
                throw new InvalidOperationException(
                        "Collection was modified; enumeration operation may not execute.");
            }
            if (index < list.size) {
                current = list.items[index++];
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
            throw new NotSupportedException();
        }

        @Override
        public void Dispose() {
        }
    }
}
