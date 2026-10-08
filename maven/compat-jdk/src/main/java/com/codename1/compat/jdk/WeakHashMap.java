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

import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// `java.util.WeakHashMap` for the Codename One runtime: a map that does not
/// keep its keys alive. An entry disappears once nothing else refers to its
/// key, which is what lets desktop code hang data off a component -- a layout
/// manager's cached constraints, say -- without ever removing it.
///
/// Keys are held through the device's `WeakReference` and compared with
/// `equals`, as the JDK's are. Entries whose key is gone are dropped the next
/// time the map is used; there is no reference queue on a device, so nothing
/// is dropped in between, and [#size()] counts only the entries still alive.
///
/// [#entrySet()], [#keySet()] and [#values()] are snapshots taken when they
/// are called: removing through them, or through their iterators, does not
/// change the map. The JDK's are live views; code that edits a weak map
/// through a view is rare enough that the simpler contract is kept.
///
/// Like every shared JDK class here it is not synchronized, and neither is
/// the JDK's.
public class WeakHashMap<K, V> extends AbstractMap<K, V> {

    /// Stands in for the null key, which a weak reference cannot tell from a
    /// key that was collected.
    private static final Object NULL_KEY = new Object();

    /// The entries by their key's hash code.
    private final HashMap<Integer, List<Cell<V>>> buckets = new HashMap<Integer, List<Cell<V>>>();

    private static final class Cell<V> {
        @SuppressWarnings("rawtypes")
        private final WeakReference key;
        private V value;

        @SuppressWarnings({"rawtypes", "unchecked"})
        Cell(Object key, V value) {
            this.key = new WeakReference(key);
            this.value = value;
        }
    }

    public WeakHashMap() {
        // Nothing to size: the buckets grow as they are used.
    }

    public WeakHashMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Initial Capacity: " + initialCapacity);
        }
    }

    public WeakHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity);
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal Load factor: " + loadFactor);
        }
    }

    public WeakHashMap(Map<? extends K, ? extends V> m) {
        putAll(m);
    }

    private static Object mask(Object key) {
        return key == null ? NULL_KEY : key;
    }

    /// The cell of `key` (already masked), or null.
    private Cell<V> find(Object key) {
        List<Cell<V>> bucket = buckets.get(Integer.valueOf(key.hashCode()));
        if (bucket == null) {
            return null;
        }
        for (int i = 0; i < bucket.size(); i++) {
            Cell<V> c = bucket.get(i);
            Object k = c.key.get();
            if (k == key || (k != null && k.equals(key))) {
                return c;
            }
        }
        return null;
    }

    /// Drops every entry whose key has been collected.
    private void expunge() {
        Iterator<List<Cell<V>>> all = buckets.values().iterator();
        while (all.hasNext()) {
            List<Cell<V>> bucket = all.next();
            for (int i = bucket.size() - 1; i >= 0; i--) {
                if (bucket.get(i).key.get() == null) {
                    bucket.remove(i);
                }
            }
            if (bucket.isEmpty()) {
                all.remove();
            }
        }
    }

    @Override
    public int size() {
        expunge();
        int n = 0;
        for (List<Cell<V>> bucket : buckets.values()) {
            n += bucket.size();
        }
        return n;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return find(mask(key)) != null;
    }

    @Override
    public V get(Object key) {
        Cell<V> c = find(mask(key));
        return c == null ? null : c.value;
    }

    @Override
    public V put(K key, V value) {
        Object k = mask(key);
        Cell<V> c = find(k);
        if (c != null) {
            V old = c.value;
            c.value = value;
            return old;
        }
        expunge();
        Integer hash = Integer.valueOf(k.hashCode());
        List<Cell<V>> bucket = buckets.get(hash);
        if (bucket == null) {
            bucket = new ArrayList<Cell<V>>(1);
            buckets.put(hash, bucket);
        }
        bucket.add(new Cell<V>(k, value));
        return null;
    }

    @Override
    public V remove(Object key) {
        Object k = mask(key);
        Integer hash = Integer.valueOf(k.hashCode());
        List<Cell<V>> bucket = buckets.get(hash);
        if (bucket == null) {
            return null;
        }
        for (int i = 0; i < bucket.size(); i++) {
            Cell<V> c = bucket.get(i);
            Object held = c.key.get();
            if (held == k || (held != null && held.equals(k))) {
                bucket.remove(i);
                if (bucket.isEmpty()) {
                    buckets.remove(hash);
                }
                return c.value;
            }
        }
        return null;
    }

    @Override
    public void clear() {
        buckets.clear();
    }

    /// A snapshot of the live entries; see the class comment.
    @Override
    @SuppressWarnings("unchecked")
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> out = new HashSet<Map.Entry<K, V>>();
        for (List<Cell<V>> bucket : buckets.values()) {
            for (int i = 0; i < bucket.size(); i++) {
                Cell<V> c = bucket.get(i);
                Object k = c.key.get();
                if (k != null) {
                    out.add(new AbstractMap.SimpleEntry<K, V>(k == NULL_KEY ? null : (K) k, c.value));
                }
            }
        }
        return out;
    }
}
