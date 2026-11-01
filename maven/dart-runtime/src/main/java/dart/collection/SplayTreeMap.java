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
package dart.collection;

import dart.core.DartComparable;
import dart.core.DartMap;
import dart.runtime.Funcs;

/**
 * dart:collection's SplayTreeMap: a Map whose keys iterate in comparator order. It is a
 * DartMap, so every Map member works on it; inserting a new key re-sorts the entries.
 */
public class SplayTreeMap<K, V> extends DartMap<K, V> {
    private final Funcs.Func2<Object, Object, Long> compare;
    private boolean sorting;

    public SplayTreeMap() {
        this(null);
    }

    @SuppressWarnings("unchecked")
    public SplayTreeMap(Object compare) {
        this.compare = (Funcs.Func2<Object, Object, Long>) compare;
    }

    private int cmp(Object a, Object b) {
        long r = compare == null ? DartComparable.compare(a, b) : compare.call(a, b).longValue();
        return r < 0 ? -1 : r > 0 ? 1 : 0;
    }

    @Override
    public V put(K key, V value) {
        if (sorting || containsKey(key)) {
            return super.put(key, value);
        }
        java.util.ArrayList<java.util.Map.Entry<K, V>> all =
                new java.util.ArrayList<java.util.Map.Entry<K, V>>();
        for (java.util.Map.Entry<K, V> en : entrySet()) {
            all.add(new java.util.AbstractMap.SimpleEntry<K, V>(en.getKey(), en.getValue()));
        }
        all.add(new java.util.AbstractMap.SimpleEntry<K, V>(key, value));
        java.util.Collections.sort(all, new java.util.Comparator<java.util.Map.Entry<K, V>>() {
            public int compare(java.util.Map.Entry<K, V> a, java.util.Map.Entry<K, V> b) {
                return cmp(a.getKey(), b.getKey());
            }
        });
        sorting = true;
        try {
            super.clear();
            for (java.util.Map.Entry<K, V> en : all) {
                super.put(en.getKey(), en.getValue());
            }
        } finally {
            sorting = false;
        }
        return null;
    }
}
