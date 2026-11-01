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

/// The runtime side of dart:collection's {@code MapMixin}/{@code MapBase}. A program class
/// applying either supplies the five members Dart's {@code MapMixin} leaves abstract
/// ({@code operator []}, {@code operator []=}, {@code keys}, {@code remove}, {@code clear});
/// the rest of the Map protocol is the defaults below, written as Dart's MapMixin writes
/// them, on top of those. Where the object is passed as a {@code Map},
/// {@code dart.core.MapMixinView} bridges it. {@link #mapToString} is the static helper a
/// map implementation calls from its {@code toString}.
public interface MapMixin<K, V> {

    /// Dart's {@code operator [](key)}.
    V $index(Object key);

    /// Dart's {@code operator []=(key, value)}.
    void $indexSet(K key, V value);

    /// Dart's {@code keys}.
    Iterable<K> keys();

    /// Dart's {@code remove(key)}: the removed value, or null.
    V remove(Object key);

    /// Dart's {@code clear}.
    void clear();

    /// Dart's {@code length}: counts {@link #keys}.
    default long length() {
        long n = 0;
        for (K ignored : keys()) {
            n++;
        }
        return n;
    }

    default boolean isEmpty() {
        return !keys().iterator().hasNext();
    }

    default boolean isNotEmpty() {
        return !isEmpty();
    }

    /// Dart's {@code containsKey}: searches {@link #keys} with Dart's {@code ==}.
    default boolean containsKey(Object key) {
        for (K k : keys()) {
            if (dart.runtime.DartRuntime.eq(k, key)) {
                return true;
            }
        }
        return false;
    }

    /// Dart's {@code containsValue}.
    default boolean containsValue(Object value) {
        for (K k : keys()) {
            if (dart.runtime.DartRuntime.eq($index(k), value)) {
                return true;
            }
        }
        return false;
    }

    /// Dart's {@code forEach}.
    default void forEach(dart.runtime.Funcs.VoidFunc2<K, V> action) {
        for (K k : keys()) {
            action.call(k, $index(k));
        }
    }

    /// Dart's {@code addAll}.
    default void addAll(java.util.Map<? extends K, ? extends V> other) {
        for (java.util.Map.Entry<? extends K, ? extends V> e : other.entrySet()) {
            $indexSet(e.getKey(), e.getValue());
        }
    }

    /// Dart's {@code putIfAbsent}.
    default V putIfAbsent(K key, dart.runtime.Funcs.Func0<V> ifAbsent) {
        if (containsKey(key)) {
            return $index(key);
        }
        V v = ifAbsent.call();
        $indexSet(key, v);
        return v;
    }

    /// {@code MapBase.mapToString(map)}: {@code {k: v, k2: v2}}.
    static String mapToString(java.util.Map<?, ?> m) {
        final StringBuilder sb = new StringBuilder("{");
        if (m instanceof dart.core.DartMap) {
            // A program class implementing Map over a DartMap keeps its entries elsewhere
            // (its own backing map) and overrides forEach; the inherited java.util.Map
            // storage is empty, so read it the Dart way.
            final boolean[] firstEntry = {true};
            dart.runtime.Funcs.VoidFunc2<Object, Object> each = new dart.runtime.Funcs.VoidFunc2<Object, Object>() {
                @Override
                public void call(Object k, Object v) {
                    if (!firstEntry[0]) {
                        sb.append(", ");
                    }
                    firstEntry[0] = false;
                    sb.append(dart.runtime.DartRuntime.str(k)).append(": ").append(dart.runtime.DartRuntime.str(v));
                }
            };
            // The transpiler forwards a program class's own forEach override to forEachDart.
            ((dart.core.DartMap<Object, Object>) m).forEachDart(each);
            return sb.append('}').toString();
        }
        boolean first = true;
        for (java.util.Map.Entry<?, ?> e : m.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(dart.runtime.DartRuntime.str(e.getKey())).append(": ")
                    .append(dart.runtime.DartRuntime.str(e.getValue()));
        }
        return sb.append('}').toString();
    }
}
