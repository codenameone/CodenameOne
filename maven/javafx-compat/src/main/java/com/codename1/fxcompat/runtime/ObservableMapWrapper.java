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
package com.codename1.fxcompat.runtime;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

/// The observable map `FXCollections` hands out: a `java.util.Map` whose
/// modifications are reported one key at a time, also when they are made
/// through its key, value or entry views.
public final class ObservableMapWrapper<K, V> extends AbstractMap<K, V> implements ObservableMap<K, V> {

    private final Map<K, V> backing;
    private ListenerSet<MapChangeListener<? super K, ? super V>> listeners;
    private Set<Map.Entry<K, V>> entries;

    /// Wraps a map.
    public ObservableMapWrapper(Map<K, V> backing) {
        this.backing = backing;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        listeners = ListenerSet.addInvalidation(listeners, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        listeners = ListenerSet.removeInvalidation(listeners, listener);
    }

    @Override
    public void addListener(MapChangeListener<? super K, ? super V> listener) {
        listeners = ListenerSet.addChange(listeners, listener);
    }

    @Override
    public void removeListener(MapChangeListener<? super K, ? super V> listener) {
        listeners = ListenerSet.removeChange(listeners, listener);
    }

    private void fire(K key, V removed, V added, boolean wasAdded, boolean wasRemoved) {
        if (ListenerSet.hasListeners(listeners)) {
            Changes.fire(listeners, this, new Changes.OfMap<K, V>(this, key, removed, added, wasAdded, wasRemoved));
        }
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backing.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return backing.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return backing.get(key);
    }

    @Override
    public V put(K key, V value) {
        boolean present = backing.containsKey(key);
        V old = backing.put(key, value);
        if (!present) {
            fire(key, null, value, true, false);
        } else if (old == null ? value != null : !old.equals(value)) {
            fire(key, old, value, true, true);
        }
        return old;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        if (!backing.containsKey(key)) {
            return null;
        }
        V old = backing.remove(key);
        fire((K) key, old, null, false, true);
        return old;
    }

    @Override
    public void clear() {
        for (Iterator<Map.Entry<K, V>> it = backing.entrySet().iterator(); it.hasNext();) {
            Map.Entry<K, V> entry = it.next();
            K key = entry.getKey();
            V value = entry.getValue();
            it.remove();
            fire(key, value, null, false, true);
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (entries == null) {
            entries = new AbstractSet<Map.Entry<K, V>>() {
                @Override
                public int size() {
                    return backing.size();
                }

                @Override
                public Iterator<Map.Entry<K, V>> iterator() {
                    return new EntryIterator();
                }
            };
        }
        return entries;
    }

    /// Walks the entries, reporting a removal made through the iterator and
    /// a value set through an entry.
    private final class EntryIterator implements Iterator<Map.Entry<K, V>> {
        private final Iterator<Map.Entry<K, V>> source = backing.entrySet().iterator();
        private K lastKey;
        private V lastValue;

        @Override
        public boolean hasNext() {
            return source.hasNext();
        }

        @Override
        public Map.Entry<K, V> next() {
            final Map.Entry<K, V> entry = source.next();
            lastKey = entry.getKey();
            lastValue = entry.getValue();
            return new Map.Entry<K, V>() {
                @Override
                public K getKey() {
                    return entry.getKey();
                }

                @Override
                public V getValue() {
                    return entry.getValue();
                }

                @Override
                public V setValue(V value) {
                    V old = entry.setValue(value);
                    lastValue = value;
                    if (old == null ? value != null : !old.equals(value)) {
                        fire(entry.getKey(), old, value, true, true);
                    }
                    return old;
                }

                @Override
                public boolean equals(Object other) {
                    return entry.equals(other);
                }

                @Override
                public int hashCode() {
                    return entry.hashCode();
                }

                @Override
                public String toString() {
                    return entry.toString();
                }
            };
        }

        @Override
        public void remove() {
            source.remove();
            fire(lastKey, lastValue, null, false, true);
        }
    }
}
