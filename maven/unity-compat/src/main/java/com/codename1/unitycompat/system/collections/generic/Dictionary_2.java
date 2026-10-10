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

import com.codename1.unitycompat.system.ArgumentException;
import com.codename1.unitycompat.system.ArgumentNullException;
import com.codename1.unitycompat.system.Interop;
import com.codename1.unitycompat.system.InvalidOperationException;
import com.codename1.unitycompat.system.NotSupportedException;
import com.codename1.unitycompat.system.Struct;
import java.util.HashMap;

/// `System.Collections.Generic.Dictionary<TKey, TValue>`. Keys compare by
/// `equals` and `hashCode`, which a translated struct defines field by
/// field and a boxed primitive or string has already.
///
/// One boxed primitive needs help. .NET's `float.Equals` and
/// `double.Equals` call `0.0` and `-0.0` the same number, so they are one
/// key; Java's boxes compare bits and call them two. The map is therefore
/// keyed by [Interop#hashKey], which is the key itself for everything but
/// a boxed negative zero, while `keys` keeps what was added, sign and all,
/// which is what .NET's `Keys` hands back.
///
/// The entries live in numbered slots and a `foreach` walks the slots, so
/// the order is .NET's and is the same on every target: the order things
/// were added in, with an entry added after a removal taking the slot most
/// recently freed. A `HashMap` alone would hand them out by hash code,
/// which for an object is whatever the virtual machine made up.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Dictionary_2 implements IEnumerable_1 {
    /// In `next`, a slot that holds an entry.
    private static final int LIVE = -2;

    private final HashMap<Object, Integer> slots = new HashMap<Object, Integer>();
    private Object[] keys;
    private Object[] values;
    /// For a free slot the next free one, or -1; [#LIVE] for a used one.
    private int[] next;
    /// Slots ever handed out: every slot below this is live or on the free list.
    private int high;
    private int free = -1;
    private int version;

    public Dictionary_2() {
        this(4);
    }

    public Dictionary_2(int capacity) {
        if (capacity < 0) {
            throw new com.codename1.unitycompat.system.ArgumentOutOfRangeException();
        }
        int n = capacity < 4 ? 4 : capacity;
        keys = new Object[n];
        values = new Object[n];
        next = new int[n];
    }

    private static Object key(Object key) {
        if (key == null) {
            throw new ArgumentNullException();
        }
        return key;
    }

    private int slot(Object key) {
        Integer at = slots.get(Interop.hashKey(key(key)));
        return at == null ? -1 : at.intValue();
    }

    private void insert(Object key, Object value) {
        int at;
        if (free >= 0) {
            at = free;
            free = next[at];
        } else {
            if (high == keys.length) {
                int n = high * 2;
                Object[] k = new Object[n];
                Object[] v = new Object[n];
                int[] x = new int[n];
                System.arraycopy(keys, 0, k, 0, high);
                System.arraycopy(values, 0, v, 0, high);
                System.arraycopy(next, 0, x, 0, high);
                keys = k;
                values = v;
                next = x;
            }
            at = high++;
        }
        keys[at] = key;
        values[at] = value;
        next[at] = LIVE;
        slots.put(Interop.hashKey(key), Integer.valueOf(at));
        version++;
    }

    public int get_Count() {
        return slots.size();
    }

    public void Add(Object key, Object value) {
        if (slot(key) >= 0) {
            throw new ArgumentException("An item with the same key has already been added.");
        }
        insert(key, value);
    }

    public boolean ContainsKey(Object key) {
        return slot(key) >= 0;
    }

    public boolean ContainsValue(Object value) {
        for (int i = 0; i < high; i++) {
            if (next[i] == LIVE && Interop.areEqual(values[i], value)) {
                return true;
            }
        }
        return false;
    }

    public Object get_Item(Object key) {
        int at = slot(key);
        if (at < 0) {
            throw new KeyNotFoundException();
        }
        return values[at];
    }

    /// Replacing the value of a key that is there leaves a running
    /// `foreach` valid, as it does in .NET.
    public void set_Item(Object key, Object value) {
        int at = slot(key);
        if (at < 0) {
            insert(key, value);
        } else {
            values[at] = value;
        }
    }

    /// `out TValue` is an array and an index, like every reference to
    /// something that is not a struct.
    public boolean TryGetValue(Object key, Object[] value, int at) {
        int found = slot(key);
        if (found < 0) {
            value[at] = null;
            return false;
        }
        value[at] = values[found];
        return true;
    }

    public boolean Remove(Object key) {
        int at = slot(key);
        if (at < 0) {
            return false;
        }
        slots.remove(Interop.hashKey(key));
        keys[at] = null;
        values[at] = null;
        next[at] = free;
        free = at;
        return true;
    }

    public void Clear() {
        slots.clear();
        for (int i = 0; i < high; i++) {
            keys[i] = null;
            values[i] = null;
        }
        high = 0;
        free = -1;
    }

    public KeyCollection get_Keys() {
        return new KeyCollection(this);
    }

    public ValueCollection get_Values() {
        return new ValueCollection(this);
    }

    /// Advances a walk over the slots: the first live slot at or after
    /// `from`, or -1 at the end.
    int advance(int from, int startedAt) {
        if (startedAt != version) {
            throw new InvalidOperationException("Collection was modified; enumeration operation may not execute.");
        }
        for (int i = from; i < high; i++) {
            if (next[i] == LIVE) {
                return i;
            }
        }
        return -1;
    }

    /// The interfaces' `GetEnumerator`: called through a reference that
    /// does not know a struct is coming back, so the result is boxed.
    @Override
    public Enumerator GetEnumerator() {
        return GetEnumerator(new Enumerator());
    }

    /// Declared to return the struct, as in .NET. Like every method that
    /// returns a struct it is handed the object to fill in.
    public Enumerator GetEnumerator(Enumerator e) {
        e.$clear();
        e.dictionary = this;
        e.version = version;
        return e;
    }

    /// `Dictionary<TKey, TValue>.Enumerator`, a struct in .NET, whose
    /// current element is a struct as well.
    public static final class Enumerator implements IEnumerator_1, Struct {
        Dictionary_2 dictionary;
        int index;
        int version;
        /// One more than the slot of the current entry: 0 is none.
        int current;

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
            dictionary = other.dictionary;
            index = other.index;
            version = other.version;
            current = other.current;
        }

        @Override
        public void $clear() {
            dictionary = null;
            index = 0;
            version = 0;
            current = 0;
        }

        @Override
        public boolean MoveNext() {
            // An enumerator nothing filled in walks nothing.
            index = dictionary == null ? 0 : dictionary.advance(index, version) + 1;
            current = index;
            if (index == 0) {
                index = Integer.MAX_VALUE;
            }
            return current > 0;
        }

        /// Through `IEnumerator`: a pair of its own, since nobody gave one.
        @Override
        public Object get_Current() {
            return get_Current(new KeyValuePair_2());
        }

        public KeyValuePair_2 get_Current(KeyValuePair_2 pair) {
            if (current == 0) {
                pair.$clear();
            } else {
                pair.key = dictionary.keys[current - 1];
                pair.value = dictionary.values[current - 1];
            }
            return pair;
        }

        @Override
        public void Reset() {
            throw new NotSupportedException();
        }

        @Override
        public void Dispose() {
        }
    }

    /// `Dictionary<TKey, TValue>.KeyCollection`: the keys, in the order a
    /// `foreach` over the dictionary gives them.
    public static final class KeyCollection implements IEnumerable_1 {
        private final Dictionary_2 dictionary;

        KeyCollection(Dictionary_2 dictionary) {
            this.dictionary = dictionary;
        }

        public int get_Count() {
            return dictionary.get_Count();
        }

        @Override
        public Enumerator GetEnumerator() {
            return GetEnumerator(new Enumerator());
        }

        public Enumerator GetEnumerator(Enumerator e) {
            e.$clear();
            e.dictionary = dictionary;
            e.version = dictionary.version;
            return e;
        }

        /// `Dictionary<TKey, TValue>.KeyCollection.Enumerator`.
        public static final class Enumerator implements IEnumerator_1, Struct {
            Dictionary_2 dictionary;
            int index;
            int version;
            int current;

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
                dictionary = other.dictionary;
                index = other.index;
                version = other.version;
                current = other.current;
            }

            @Override
            public void $clear() {
                dictionary = null;
                index = 0;
                version = 0;
                current = 0;
            }

            @Override
            public boolean MoveNext() {
                // An enumerator nothing filled in walks nothing.
                index = dictionary == null ? 0 : dictionary.advance(index, version) + 1;
                current = index;
                if (index == 0) {
                    index = Integer.MAX_VALUE;
                }
                return current > 0;
            }

            @Override
            public Object get_Current() {
                return current == 0 ? null : dictionary.keys[current - 1];
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

    /// `Dictionary<TKey, TValue>.ValueCollection`.
    public static final class ValueCollection implements IEnumerable_1 {
        private final Dictionary_2 dictionary;

        ValueCollection(Dictionary_2 dictionary) {
            this.dictionary = dictionary;
        }

        public int get_Count() {
            return dictionary.get_Count();
        }

        @Override
        public Enumerator GetEnumerator() {
            return GetEnumerator(new Enumerator());
        }

        public Enumerator GetEnumerator(Enumerator e) {
            e.$clear();
            e.dictionary = dictionary;
            e.version = dictionary.version;
            return e;
        }

        /// `Dictionary<TKey, TValue>.ValueCollection.Enumerator`.
        public static final class Enumerator implements IEnumerator_1, Struct {
            Dictionary_2 dictionary;
            int index;
            int version;
            int current;

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
                dictionary = other.dictionary;
                index = other.index;
                version = other.version;
                current = other.current;
            }

            @Override
            public void $clear() {
                dictionary = null;
                index = 0;
                version = 0;
                current = 0;
            }

            @Override
            public boolean MoveNext() {
                // An enumerator nothing filled in walks nothing.
                index = dictionary == null ? 0 : dictionary.advance(index, version) + 1;
                current = index;
                if (index == 0) {
                    index = Integer.MAX_VALUE;
                }
                return current > 0;
            }

            @Override
            public Object get_Current() {
                return current == 0 ? null : dictionary.values[current - 1];
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
}
