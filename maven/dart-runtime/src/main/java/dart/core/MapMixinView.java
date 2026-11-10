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

import dart.collection.MapMixin;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/// A live {@code Map} view of a program object that applies dart:collection's
/// {@code MapMixin} (or extends {@code MapBase}), made where the object is passed to a
/// parameter or variable declared {@code Map}. It holds no storage: reads go to the
/// object's {@code operator []} and {@code keys}, writes to its {@code operator []=},
/// {@code remove} and {@code clear}, so a write through the view reaches the object.
/// {@link #source()} answers the object, for identity and for a cast back to its class.
public final class MapMixinView<K, V> extends DartMap<K, V> implements MixinView {

    private final MapMixin<K, V> src;

    public MapMixinView(MapMixin<K, V> src) {
        this.src = src;
    }

    @Override
    public Object source() {
        return src;
    }

    /// The object's own toString, as Dart prints the object, not a re-rendering.
    @Override
    public String toString() {
        return dart.runtime.DartRuntime.str(src);
    }

    @Override
    public V get(Object key) {
        return src.$index(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return src.containsKey(key);
    }

    @Override
    public V put(K key, V value) {
        boolean had = src.containsKey(key);
        V old = had ? src.$index(key) : null;
        if (!had) {
            structuralChange();
        }
        src.$indexSet(key, value);
        return old;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> other) {
        for (Map.Entry<? extends K, ? extends V> e : other.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        if (src.containsKey(key)) {
            structuralChange();
        }
        return src.remove(key);
    }

    @Override
    public int size() {
        return (int) src.length();
    }

    @Override
    public boolean isEmpty() {
        return src.isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        return src.containsValue(value);
    }

    @Override
    public void clear() {
        if (!src.isEmpty()) {
            structuralChange();
        }
        src.clear();
    }

    /// The keys, live: iterates the object's {@code keys}; remove() goes to its remove.
    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            @Override
            public Iterator<K> iterator() {
                final Iterator<K> it = src.keys().iterator();
                return new Iterator<K>() {
                    private K last;
                    private boolean canRemove;

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public K next() {
                        last = it.next();
                        canRemove = true;
                        return last;
                    }

                    @Override
                    public void remove() {
                        if (!canRemove) {
                            throw new IllegalStateException();
                        }
                        canRemove = false;
                        MapMixinView.this.remove(last);
                    }
                };
            }

            @Override
            public int size() {
                return MapMixinView.this.size();
            }

            @Override
            public boolean contains(Object o) {
                return src.containsKey(o);
            }
        };
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                final Iterator<K> it = keySet().iterator();
                return new Iterator<V>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public V next() {
                        return src.$index(it.next());
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }

            @Override
            public int size() {
                return MapMixinView.this.size();
            }
        };
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                final Iterator<K> it = keySet().iterator();
                return new Iterator<Map.Entry<K, V>>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        final K k = it.next();
                        return new Map.Entry<K, V>() {
                            @Override
                            public K getKey() {
                                return k;
                            }

                            @Override
                            public V getValue() {
                                return src.$index(k);
                            }

                            @Override
                            public V setValue(V v) {
                                V old = src.$index(k);
                                src.$indexSet(k, v);
                                return old;
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
                return MapMixinView.this.size();
            }
        };
    }
}
