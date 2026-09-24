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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Dart's {@code List.asMap()}: indices to elements, backed by the list and unmodifiable.
 * Reads see the list as it is now; every write is refused with UnsupportedError. The
 * inherited map storage is never used.
 */
final class DartListMapView<E> extends DartMap<Long, E> {

    private final DartList<E> list;

    DartListMapView(DartList<E> list) {
        this.list = list;
    }

    private int index(Object key) {
        if (key instanceof Long || key instanceof Integer) {
            long i = ((Number) key).longValue();
            if (i >= 0 && i < list.size()) {
                return (int) i;
            }
        }
        return -1;
    }

    @Override
    public E get(Object key) {
        int i = index(key);
        return i < 0 ? null : list.get(i);
    }

    @Override
    public boolean containsKey(Object key) {
        return index(key) >= 0;
    }

    @Override
    public boolean containsValue(Object value) {
        return list.contains(value);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    private static UnsupportedError unmodifiable() {
        return new UnsupportedError("Cannot modify an unmodifiable map");
    }

    @Override
    public E put(Long key, E value) {
        throw unmodifiable();
    }

    @Override
    public void putAll(Map<? extends Long, ? extends E> other) {
        throw unmodifiable();
    }

    @Override
    public E remove(Object key) {
        throw unmodifiable();
    }

    @Override
    public void clear() {
        throw unmodifiable();
    }

    @Override
    public Collection<E> values() {
        return java.util.Collections.unmodifiableList(list);
    }

    @Override
    public Set<Long> keySet() {
        return new AbstractSet<Long>() {
            @Override
            public Iterator<Long> iterator() {
                return new Iterator<Long>() {
                    int i;

                    @Override
                    public boolean hasNext() {
                        return i < list.size();
                    }

                    @Override
                    public Long next() {
                        if (i >= list.size()) {
                            throw new NoSuchElementException();
                        }
                        return Long.valueOf(i++);
                    }
                };
            }

            @Override
            public int size() {
                return list.size();
            }
        };
    }

    @Override
    public Set<Map.Entry<Long, E>> entrySet() {
        return new AbstractSet<Map.Entry<Long, E>>() {
            @Override
            public Iterator<Map.Entry<Long, E>> iterator() {
                return new Iterator<Map.Entry<Long, E>>() {
                    int i;

                    @Override
                    public boolean hasNext() {
                        return i < list.size();
                    }

                    @Override
                    public Map.Entry<Long, E> next() {
                        if (i >= list.size()) {
                            throw new NoSuchElementException();
                        }
                        final int at = i++;
                        return new Map.Entry<Long, E>() {
                            @Override
                            public Long getKey() {
                                return Long.valueOf(at);
                            }

                            @Override
                            public E getValue() {
                                return list.get(at);
                            }

                            @Override
                            public E setValue(E v) {
                                throw unmodifiable();
                            }
                        };
                    }
                };
            }

            @Override
            public int size() {
                return list.size();
            }
        };
    }
}
