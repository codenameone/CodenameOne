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
package com.codename1.compat.jdk;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/// `java.util.concurrent.ConcurrentHashMap` for the Codename One runtime,
/// over a `HashMap`.
///
/// **It does not synchronize.** Codename One user interface code is single
/// threaded: everything that touches components runs on the event dispatch
/// thread, and an application ported from the desktop keeps that shape. A map
/// used from a background thread must stay confined to that one thread, and
/// results cross to the event dispatch thread through `callSerially`, not
/// through a shared map.
///
/// What it does keep from the JDK class is everything visible on one thread:
///
/// - `null` keys and values are rejected with `NullPointerException`.
/// - Iterators never throw `ConcurrentModificationException`. Each iterates
///   over the entries present when it was created, so a loop may add to and
///   remove from the map it is walking; `Iterator.remove` and
///   `Map.Entry.setValue` write through to the map.
/// - The compound operations (`putIfAbsent`, `computeIfAbsent`, `compute`,
///   `merge`, ...) are atomic in the only sense that applies here: nothing
///   else runs between their read and their write.
///
/// The bulk parallel operations (`forEach(long, ...)`, `search`, `reduce`)
/// and `forEachKey`/`forEachValue` are not provided.
public class ConcurrentHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final HashMap<K, V> map;

    public ConcurrentHashMap() {
        map = new HashMap<K, V>();
    }

    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException();
        }
        map = new HashMap<K, V>(initialCapacity);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0) {
            throw new IllegalArgumentException();
        }
        map = new HashMap<K, V>(initialCapacity);
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        map = new HashMap<K, V>();
        putAll(m);
    }

    private static void requireNonNull(Object o) {
        if (o == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    public long mappingCount() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public V get(Object key) {
        requireNonNull(key);
        return map.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        requireNonNull(key);
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        requireNonNull(value);
        return map.containsValue(value);
    }

    public boolean contains(Object value) {
        return containsValue(value);
    }

    @Override
    public V put(K key, V value) {
        requireNonNull(key);
        requireNonNull(value);
        return map.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        requireNonNull(key);
        return map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        return v == null ? defaultValue : v;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        requireNonNull(key);
        requireNonNull(value);
        V old = map.get(key);
        if (old == null) {
            map.put(key, value);
        }
        return old;
    }

    @Override
    public boolean remove(Object key, Object value) {
        requireNonNull(key);
        if (value == null) {
            return false;
        }
        V old = map.get(key);
        if (old != null && old.equals(value)) {
            map.remove(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        requireNonNull(key);
        requireNonNull(oldValue);
        requireNonNull(newValue);
        V old = map.get(key);
        if (old != null && old.equals(oldValue)) {
            map.put(key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        requireNonNull(key);
        requireNonNull(value);
        V old = map.get(key);
        if (old != null) {
            map.put(key, value);
        }
        return old;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        requireNonNull(key);
        requireNonNull(mappingFunction);
        V old = map.get(key);
        if (old != null) {
            return old;
        }
        V created = mappingFunction.apply(key);
        if (created != null) {
            map.put(key, created);
        }
        return created;
    }

    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        requireNonNull(key);
        requireNonNull(remappingFunction);
        V old = map.get(key);
        if (old == null) {
            return null;
        }
        V updated = remappingFunction.apply(key, old);
        if (updated == null) {
            map.remove(key);
        } else {
            map.put(key, updated);
        }
        return updated;
    }

    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        requireNonNull(key);
        requireNonNull(remappingFunction);
        V old = map.get(key);
        V updated = remappingFunction.apply(key, old);
        if (updated == null) {
            if (old != null) {
                map.remove(key);
            }
        } else {
            map.put(key, updated);
        }
        return updated;
    }

    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        requireNonNull(key);
        requireNonNull(value);
        requireNonNull(remappingFunction);
        V old = map.get(key);
        if (old == null) {
            map.put(key, value);
            return value;
        }
        V updated = remappingFunction.apply(old, value);
        if (updated == null) {
            map.remove(key);
        } else {
            map.put(key, updated);
        }
        return updated;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        requireNonNull(action);
        for (Map.Entry<K, V> e : snapshot()) {
            action.accept(e.getKey(), e.getValue());
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        requireNonNull(function);
        for (Map.Entry<K, V> e : snapshot()) {
            V updated = function.apply(e.getKey(), e.getValue());
            requireNonNull(updated);
            if (map.containsKey(e.getKey())) {
                map.put(e.getKey(), updated);
            }
        }
    }

    /// The entries present now, as entries that write through to this map.
    /// Every iterator walks one of these, which is what lets a caller modify
    /// the map while iterating it.
    private List<Map.Entry<K, V>> snapshot() {
        List<Map.Entry<K, V>> out = new ArrayList<Map.Entry<K, V>>(map.size());
        for (Map.Entry<K, V> e : map.entrySet()) {
            out.add(new WriteThroughEntry<K, V>(this, e.getKey(), e.getValue()));
        }
        return out;
    }

    /// The keys, as a view of this map. Compiled code names the JDK's return
    /// type, `ConcurrentHashMap.KeySetView`, so that is what this returns.
    @Override
    public KeySetView<K, V> keySet() {
        return new KeySetView<K, V>(this, null);
    }

    public KeySetView<K, V> keySet(V mappedValue) {
        requireNonNull(mappedValue);
        return new KeySetView<K, V>(this, mappedValue);
    }

    public static <K> KeySetView<K, Boolean> newKeySet() {
        return new KeySetView<K, Boolean>(new ConcurrentHashMap<K, Boolean>(), Boolean.TRUE);
    }

    public static <K> KeySetView<K, Boolean> newKeySet(int initialCapacity) {
        return new KeySetView<K, Boolean>(new ConcurrentHashMap<K, Boolean>(initialCapacity), Boolean.TRUE);
    }

    @Override
    public Collection<V> values() {
        return new ValuesView<K, V>(this);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new EntrySetView<K, V>(this);
    }

    public Enumeration<K> keys() {
        return new IteratorEnumeration<K>(keySet().iterator());
    }

    public Enumeration<V> elements() {
        return new IteratorEnumeration<V>(values().iterator());
    }

    private static final class WriteThroughEntry<K, V> implements Map.Entry<K, V> {
        private final ConcurrentHashMap<K, V> owner;
        private final K key;
        private V value;

        WriteThroughEntry(ConcurrentHashMap<K, V> owner, K key, V value) {
            this.owner = owner;
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V newValue) {
            requireNonNull(newValue);
            V old = value;
            value = newValue;
            owner.map.put(key, newValue);
            return old;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
            Object otherKey = other.getKey();
            Object otherValue = other.getValue();
            return key.equals(otherKey) && value.equals(otherValue);
        }

        @Override
        public int hashCode() {
            return key.hashCode() ^ value.hashCode();
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    /// Walks a snapshot of the entries; `remove` removes the key from the
    /// live map.
    private static final class SnapshotIterator<K, V> {
        private final ConcurrentHashMap<K, V> owner;
        private final List<Map.Entry<K, V>> entries;
        private int next;
        private Map.Entry<K, V> last;

        SnapshotIterator(ConcurrentHashMap<K, V> owner) {
            this.owner = owner;
            this.entries = owner.snapshot();
        }

        boolean hasNext() {
            return next < entries.size();
        }

        Map.Entry<K, V> nextEntry() {
            if (next >= entries.size()) {
                throw new NoSuchElementException();
            }
            last = entries.get(next++);
            return last;
        }

        void remove() {
            if (last == null) {
                throw new IllegalStateException();
            }
            owner.map.remove(last.getKey());
            last = null;
        }
    }

    private static final class KeyIterator<K, V> implements Iterator<K> {
        private final SnapshotIterator<K, V> it;

        KeyIterator(ConcurrentHashMap<K, V> owner) {
            it = new SnapshotIterator<K, V>(owner);
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public K next() {
            return it.nextEntry().getKey();
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    private static final class ValueIterator<K, V> implements Iterator<V> {
        private final SnapshotIterator<K, V> it;

        ValueIterator(ConcurrentHashMap<K, V> owner) {
            it = new SnapshotIterator<K, V>(owner);
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public V next() {
            return it.nextEntry().getValue();
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    private static final class EntryIterator<K, V> implements Iterator<Map.Entry<K, V>> {
        private final SnapshotIterator<K, V> it;

        EntryIterator(ConcurrentHashMap<K, V> owner) {
            it = new SnapshotIterator<K, V>(owner);
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Map.Entry<K, V> next() {
            return it.nextEntry();
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    private static final class IteratorEnumeration<T> implements Enumeration<T> {
        private final Iterator<T> it;

        IteratorEnumeration(Iterator<T> it) {
            this.it = it;
        }

        @Override
        public boolean hasMoreElements() {
            return it.hasNext();
        }

        @Override
        public T nextElement() {
            return it.next();
        }
    }

    private static final class ValuesView<K, V> extends AbstractCollection<V> {
        private final ConcurrentHashMap<K, V> owner;

        ValuesView(ConcurrentHashMap<K, V> owner) {
            this.owner = owner;
        }

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator<K, V>(owner);
        }

        @Override
        public int size() {
            return owner.size();
        }

        @Override
        public boolean contains(Object o) {
            return o != null && owner.containsValue(o);
        }

        @Override
        public void clear() {
            owner.clear();
        }
    }

    private static final class EntrySetView<K, V> extends AbstractSet<Map.Entry<K, V>> {
        private final ConcurrentHashMap<K, V> owner;

        EntrySetView(ConcurrentHashMap<K, V> owner) {
            this.owner = owner;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator<K, V>(owner);
        }

        @Override
        public int size() {
            return owner.size();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Object key = e.getKey();
            Object value = e.getValue();
            if (key == null || value == null) {
                return false;
            }
            Object mine = owner.get(key);
            return mine != null && mine.equals(value);
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Object key = e.getKey();
            Object value = e.getValue();
            return key != null && value != null && owner.remove(key, value);
        }

        @Override
        public void clear() {
            owner.clear();
        }
    }

    /// `java.util.concurrent.ConcurrentHashMap.KeySetView`: the keys of a
    /// map as a set. A view created with a mapped value (`keySet(V)`,
    /// `newKeySet()`) also accepts `add`, which maps the key to that value.
    public static class KeySetView<K, V> extends AbstractSet<K> implements java.io.Serializable {

        private static final long serialVersionUID = 1L;

        private final ConcurrentHashMap<K, V> owner;
        private final V value;

        KeySetView(ConcurrentHashMap<K, V> owner, V value) {
            this.owner = owner;
            this.value = value;
        }

        /// The map this is a view of. It is the live map, as in the JDK:
        /// the view exists to share it.
        public ConcurrentHashMap<K, V> getMap() {
            return owner;
        }

        public V getMappedValue() {
            return value;
        }

        @Override
        public Iterator<K> iterator() {
            return new KeyIterator<K, V>(owner);
        }

        @Override
        public int size() {
            return owner.size();
        }

        @Override
        public boolean isEmpty() {
            return owner.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return owner.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return owner.remove(o) != null;
        }

        @Override
        public void clear() {
            owner.clear();
        }

        @Override
        public boolean add(K e) {
            if (value == null) {
                throw new UnsupportedOperationException();
            }
            return owner.putIfAbsent(e, value) == null;
        }

        @Override
        public boolean addAll(Collection<? extends K> c) {
            boolean added = false;
            for (K e : c) {
                if (add(e)) {
                    added = true;
                }
            }
            return added;
        }
    }
}
