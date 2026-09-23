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

import dart.runtime.DartRuntime;
import dart.runtime.Funcs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dart's Map&lt;K,V&gt;: insertion-ordered (Dart map literals preserve
 * insertion order, matching LinkedHashMap). Extends LinkedHashMap so it
 * interoperates with Java/CN1 APIs directly.
 */
public class DartMap<K, V> extends LinkedHashMap<K, V> {

    /// Identity, as Dart's own {@code Map} has: {@code [1] == [1]} is false in Dart
    /// unless a type overrides {@code operator ==}. The inherited Java equality is
    /// structural, so two separately built collections compared equal and, used as
    /// keys, collapsed into one entry of a map -- different control flow and lost
    /// data in a transpiled application. Element-wise comparison is what Dart's
    /// listEquals, mapEquals and setEquals are for, and they say so explicitly.
    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    /// Consistent with {@link #equals}: identity.
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * The other boxed forms Dart's {@code ==} equates with a numeric key: 1 with
     * 1.0, and 0.0 with -0.0 and 0. Empty for anything that is not a number, so a
     * String key pays one instanceof. Java's wrappers are equal only within their
     * own class, which made {@code {1: 'a'}[1.0]} null and {@code m[1.0] = 'b'} a
     * second entry beside the int 1.
     */
    static Object[] numericTwins(Object key) {
        if (key instanceof Double) {
            double d = ((Double) key).doubleValue();
            // Integral and inside the long range, tested with a cast: Math.rint is not
            // in ParparVM's JavaAPI, and calling it broke every native build.
            if (d >= -9.223372036854775808E18 && d < 9.223372036854775808E18 && (double) (long) d == d) {
                Long asInt = Long.valueOf((long) d);
                return d == 0 ? new Object[] {asInt, Double.valueOf(-d)} : new Object[] {asInt};
            }
            return NO_TWINS;
        }
        if (key instanceof Long || key instanceof Integer) {
            long l = ((Number) key).longValue();
            double d = (double) l;
            if ((long) d == l && d < 9.223372036854775808E18) {
                return l == 0 ? new Object[] {Double.valueOf(0.0), Double.valueOf(-0.0)}
                        : new Object[] {Double.valueOf(d)};
            }
        }
        return NO_TWINS;
    }

    static final Object[] NO_TWINS = new Object[0];

    /**
     * The key this map already stores for {@code key} under Dart's {@code ==}, or
     * {@code key} itself. Dart keeps the key an entry was first stored under, so
     * {@code m[1.0] = 'b'} on a map holding 1 updates that entry and the key stays 1.
     */
    private Object storedKey(Object key) {
        if (!(key instanceof Number) || super.containsKey(key)) {
            return key;
        }
        for (Object twin : numericTwins(key)) {
            if (super.containsKey(twin)) {
                return twin;
            }
        }
        return key;
    }

    @Override
    public V get(Object key) {
        return super.get(storedKey(key));
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(storedKey(key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        return super.put((K) storedKey(key), value);
    }

    @Override
    public V remove(Object key) {
        return super.remove(storedKey(key));
    }

    public DartMap() {
    }

    /** Literal helper for {@code {a: 1, b: 2}}: pairs of key, value. */
    @SuppressWarnings("unchecked")
    public static <K, V> DartMap<K, V> of(Object... pairs) {
        DartMap<K, V> m = new DartMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((K) pairs[i], (V) pairs[i + 1]);
        }
        return m;
    }

    /**
     * Dart's {@code Map.of(other)} / {@code Map.from(other)} — a new insertion-ordered
     * map holding a shallow copy of {@code other}'s entries. The fixed-arity overload
     * takes priority over the varargs {@link #of(Object...)} literal helper (a single
     * {@code Map} argument is more specific), so map literals continue to resolve to
     * the pairs form.
     */
    public static <K, V> DartMap<K, V> of(Map<? extends K, ? extends V> other) {
        return from(other);
    }

    /**
     * Dart's {@code Map.from(other)} — copy the entries of another map. Accepts any {@code Map}
     * (Dart's {@code Map.from} takes an untyped map and the caller supplies K/V), casting the
     * entries to the requested K/V per Dart's dynamic-map semantics.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> DartMap<K, V> from(Map<?, ?> other) {
        DartMap<K, V> m = new DartMap<>();
        if (other != null) {
            m.putAll((Map<? extends K, ? extends V>) other);
        }
        return m;
    }

    /**
     * Dart's {@code Map.fromIterable(iterable, {key, value})}. When {@code key} or
     * {@code value} is null the element itself is used (Dart's identity default).
     */
    @SuppressWarnings("unchecked")
    public static <E, K, V> DartMap<K, V> fromIterable(
            Iterable<E> iterable,
            Funcs.Func1<E, K> key,
            Funcs.Func1<E, V> value) {
        DartMap<K, V> m = new DartMap<>();
        if (iterable != null) {
            for (E e : iterable) {
                K k = key != null ? key.call(e) : (K) e;
                V v = value != null ? value.call(e) : (V) e;
                m.put(k, v);
            }
        }
        return m;
    }

    /** Dart's {@code Map.fromEntries(entries)}. */
    public static <K, V> DartMap<K, V> fromEntries(Iterable<? extends MapEntry<K, V>> entries) {
        DartMap<K, V> m = new DartMap<>();
        if (entries != null) {
            for (MapEntry<K, V> e : entries) {
                m.put(e.key(), e.value());
            }
        }
        return m;
    }

    /** Dart's {@code Map.identity()}: keys match only when they are the same object. */
    public static <K, V> DartMap<K, V> identity() {
        return new DartIdentityMap<K, V>();
    }

    /** Dart's map[key]. */
    public V idx(K key) {
        return get(key);
    }

    /** Dart's map[key] = value. */
    public V idxSet(K key, V value) {
        put(key, value);
        return value;
    }

    public long length() {
        return size();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public DartIterable<K> keys() {
        return DartIterable.wrap(keySet());
    }

    public DartIterable<V> valuesIterable() {
        return DartIterable.wrap(values());
    }

    public V putIfAbsentDart(K key, Funcs.Func0<V> ifAbsent) {
        if (containsKey(key)) {
            return get(key);
        }
        V v = ifAbsent.call();
        put(key, v);
        return v;
    }

    public void forEachDart(Funcs.VoidFunc2<K, V> action) {
        for (Map.Entry<K, V> e : entrySet()) {
            action.call(e.getKey(), e.getValue());
        }
    }

    /** Dart's Map.remove returns the removed value (or null). */
    public V removeDart(Object key) {
        return remove(key);
    }

    /** Dart's {@code Map.addAll(other)} — copies every entry of {@code other} in. */
    public void addAll(Map<? extends K, ? extends V> other) {
        if (other != null) {
            putAll(other);
        }
    }

    /** Dart's {@code Map.addEntries(entries)}. */
    public void addEntries(Iterable<? extends MapEntry<K, V>> entries) {
        if (entries != null) {
            for (MapEntry<K, V> e : entries) {
                put(e.key(), e.value());
            }
        }
    }

    /** Dart's {@code Map.entries} getter — an iterable of key/value pairs. */
    /**
     * Dart's {@code map.entries}: a live view, as Dart's is -- an iterable kept and
     * walked after the map changed sees the change. It used to copy the entries up
     * front, so a retained view went stale.
     */
    public DartIterable<MapEntry<K, V>> entries() {
        return entriesOf(this);
    }

    /** A live MapEntry view over any Java map's entries. */
    static <K, V> DartIterable<MapEntry<K, V>> entriesOf(final Map<K, V> map) {
        return DartIterable.wrap(new Iterable<MapEntry<K, V>>() {
            @Override
            public java.util.Iterator<MapEntry<K, V>> iterator() {
                final java.util.Iterator<Map.Entry<K, V>> it = map.entrySet().iterator();
                return new java.util.Iterator<MapEntry<K, V>>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public MapEntry<K, V> next() {
                        Map.Entry<K, V> e = it.next();
                        return new MapEntry<K, V>(e.getKey(), e.getValue());
                    }
                };
            }
        });
    }

    /** Dart's {@code Map.removeWhere(test)}. */
    public void removeWhere(Funcs.Func2<K, V, Boolean> test) {
        java.util.Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> e = it.next();
            if (Boolean.TRUE.equals(test.call(e.getKey(), e.getValue()))) {
                it.remove();
            }
        }
    }

    /** Dart's {@code Map.update(key, update, {ifAbsent})}. */
    public V update(K key, Funcs.Func1<V, V> update, Funcs.Func0<V> ifAbsent) {
        if (containsKey(key)) {
            V v = update.call(get(key));
            put(key, v);
            return v;
        }
        if (ifAbsent != null) {
            V v = ifAbsent.call();
            put(key, v);
            return v;
        }
        throw new ArgumentError("Key not in map: " + key);
    }

    /** Dart's {@code Map.map(transform)} — returns a new map of transformed entries. */
    public <K2, V2> DartMap<K2, V2> mapEntries(Funcs.Func2<K, V, MapEntry<K2, V2>> transform) {
        DartMap<K2, V2> m = new DartMap<>();
        for (Map.Entry<K, V> e : entrySet()) {
            MapEntry<K2, V2> me = transform.call(e.getKey(), e.getValue());
            m.put(me.key(), me.value());
        }
        return m;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<K, V> e : entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(DartRuntime.str(e.getKey())).append(": ").append(DartRuntime.str(e.getValue()));
            first = false;
        }
        return sb.append("}").toString();
    }
}
