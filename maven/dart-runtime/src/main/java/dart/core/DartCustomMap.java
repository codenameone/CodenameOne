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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/// Dart's {@code LinkedHashMap(equals:, hashCode:, isValidKey:)} (and {@code HashMap}'s): keys
/// match under the caller's equality, so a map keyed case-insensitively finds
/// {@code 'content-type'} under {@code 'Content-Type'}. The entries stay in the inherited
/// storage under the key each was first stored with, in insertion order, as Dart keeps it;
/// a side index from the custom hash to those stored keys answers which one a lookup means.
/// The index is pruned lazily: a key removed some other way (an iterator) fails the
/// containsKey check and is dropped the next time its bucket is read.
public final class DartCustomMap<K, V> extends DartMap<K, V> {
    private final Funcs.Func2<K, K, Boolean> equalsFn;
    private final Funcs.Func1<K, Long> hashFn;
    private final Funcs.Func1<Object, Boolean> validKey;
    private final HashMap<Long, List<Object>> index = new HashMap<Long, List<Object>>();

    /// Any of the three may be null: Dart's defaults are {@code ==}, {@code hashCode}, and
    /// (for a custom equality) a type test this erased runtime cannot make, so all keys pass.
    public DartCustomMap(Funcs.Func2<K, K, Boolean> equals, Funcs.Func1<K, Long> hashCode,
            Funcs.Func1<Object, Boolean> isValidKey) {
        this.equalsFn = equals;
        this.hashFn = hashCode;
        this.validKey = isValidKey;
    }

    private long hashOf(K key) {
        if (hashFn != null) {
            return hashFn.call(key).longValue();
        }
        return key == null ? 0 : key.hashCode();
    }

    private boolean same(K a, K b) {
        if (equalsFn != null) {
            return equalsFn.call(a, b).booleanValue();
        }
        return a == null ? b == null : a.equals(b);
    }

    /// The stored key equal to {@code key}, or null with {@code found[0]} false.
    @SuppressWarnings("unchecked")
    private Object find(Object key, boolean[] found) {
        found[0] = false;
        if (validKey != null && !validKey.call(key).booleanValue()) {
            return null;
        }
        List<Object> bucket = index.get(Long.valueOf(hashOf((K) key)));
        if (bucket == null) {
            return null;
        }
        for (int i = 0; i < bucket.size(); i++) { // NOPMD ForLoopCanBeForeach - prunes in place
            Object k = bucket.get(i);
            if (!rawContains(k)) {
                bucket.remove(i--);
                continue;
            }
            if (same((K) k, (K) key)) {
                found[0] = true;
                return k;
            }
        }
        return null;
    }

    private boolean rawContains(Object storedKey) {
        return super.containsKey(storedKey);
    }

    @Override
    public V get(Object key) {
        boolean[] f = new boolean[1];
        Object k = find(key, f);
        return f[0] ? super.get(k) : null;
    }

    @Override
    public boolean containsKey(Object key) {
        boolean[] f = new boolean[1];
        find(key, f);
        return f[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        boolean[] f = new boolean[1];
        Object k = find(key, f);
        if (f[0]) {
            return super.put((K) k, value);
        }
        Long h = Long.valueOf(hashOf(key));
        List<Object> bucket = index.get(h);
        if (bucket == null) {
            bucket = new ArrayList<Object>();
            index.put(h, bucket);
        }
        bucket.add(key);
        return super.put(key, value);
    }

    @Override
    public V remove(Object key) {
        boolean[] f = new boolean[1];
        Object k = find(key, f);
        return f[0] ? super.remove(k) : null;
    }

    @Override
    public void clear() {
        index.clear();
        super.clear();
    }
}
