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

import dart.runtime.Funcs;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * An unmodifiable Dart map: a copy of the entries it is built from, whose every write
 * -- map[k] = v, remove, clear, addAll, removeWhere, or a remove through one of its
 * views -- is a Dart UnsupportedError, as Map.unmodifiable and the maps Dart hands out
 * read-only (Uri.queryParameters) are.
 */
final class DartUnmodifiableMap<K, V> extends DartMap<K, V> {

    private final boolean frozen;

    DartUnmodifiableMap(Map<? extends K, ? extends V> entries) {
        super.putAll(entries);
        frozen = true;
    }

    private static UnsupportedError refused() {
        return new UnsupportedError("Cannot modify unmodifiable map");
    }

    @Override
    public V put(K key, V value) {
        if (frozen) {
            throw refused();
        }
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> other) {
        if (frozen) {
            throw refused();
        }
        super.putAll(other);
    }

    @Override
    public V remove(Object key) {
        throw refused();
    }

    @Override
    public void clear() {
        throw refused();
    }

    @Override
    public void removeWhere(Funcs.Func2<K, V, Boolean> test) {
        throw refused();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return frozen ? Collections.unmodifiableSet(super.entrySet()) : super.entrySet();
    }

    @Override
    public Set<K> keySet() {
        return frozen ? Collections.unmodifiableSet(super.keySet()) : super.keySet();
    }

    @Override
    public Collection<V> values() {
        return frozen ? Collections.unmodifiableCollection(super.values()) : super.values();
    }
}
