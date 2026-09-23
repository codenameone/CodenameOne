/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dart's {@code Map.identity()}: keys match only when they are the SAME object,
 * whatever their {@code ==} says. Insertion ordered, like every Dart map.
 *
 * <p>{@code Map.identity()} used to answer a plain DartMap, so two distinct but equal
 * keys -- two separately built {@code ValueKey('a')}, say -- collapsed into one
 * entry, which is exactly what an identity map exists to prevent.</p>
 *
 * <p>The entries live in an inner map keyed by an identity wrapper; the inherited
 * storage is never used. Every access path DartMap and generated code take --
 * get, put, containsKey, remove, size, clear and the three views -- is overridden
 * to go through the inner map.</p>
 */
public final class DartIdentityMap<K, V> extends DartMap<K, V> {

    /** A key compared by identity. */
    private static final class IdKey {
        final Object key;

        IdKey(Object key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof IdKey && ((IdKey) o).key == key;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(key);
        }
    }

    private final LinkedHashMap<IdKey, V> inner = new LinkedHashMap<IdKey, V>();

    @Override
    public V get(Object key) {
        return inner.get(new IdKey(key));
    }

    @Override
    public boolean containsKey(Object key) {
        return inner.containsKey(new IdKey(key));
    }

    @Override
    public V put(K key, V value) {
        return inner.put(new IdKey(key), value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> other) {
        for (Map.Entry<? extends K, ? extends V> e : other.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        return inner.remove(new IdKey(key));
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        return inner.containsValue(value);
    }

    @Override
    public void clear() {
        inner.clear();
    }

    @Override
    public Collection<V> values() {
        return inner.values();
    }

    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            @Override
            public Iterator<K> iterator() {
                final Iterator<IdKey> it = inner.keySet().iterator();
                return new Iterator<K>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public K next() {
                        return (K) it.next().key;
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }

            @Override
            public int size() {
                return inner.size();
            }
        };
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                final Iterator<Map.Entry<IdKey, V>> it = inner.entrySet().iterator();
                return new Iterator<Map.Entry<K, V>>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        final Map.Entry<IdKey, V> e = it.next();
                        return new Map.Entry<K, V>() {
                            @Override
                            @SuppressWarnings("unchecked")
                            public K getKey() {
                                return (K) e.getKey().key;
                            }

                            @Override
                            public V getValue() {
                                return e.getValue();
                            }

                            @Override
                            public V setValue(V v) {
                                return e.setValue(v);
                            }
                        };
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }

            @Override
            public int size() {
                return inner.size();
            }
        };
    }
}
