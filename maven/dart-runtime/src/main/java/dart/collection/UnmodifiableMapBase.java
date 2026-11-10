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

/// dart:collection's {@code UnmodifiableMapBase}: a {@link MapMixin} whose mutators throw
/// {@code UnsupportedError("Cannot modify unmodifiable map")}, as Dart's do. A program
/// class extending it supplies {@code operator []} and {@code keys}.
public interface UnmodifiableMapBase<K, V> extends MapMixin<K, V> {
    @Override
    default void $indexSet(K key, V value) {
        throw unmodifiable();
    }

    /// The {@code map[key] = value} form of an assignment whose receiver is typed by
    /// this interface rather than by the program class.
    default V idxSet(K key, V value) {
        throw unmodifiable();
    }

    @Override
    default V remove(Object key) {
        throw unmodifiable();
    }

    @Override
    default void clear() {
        throw unmodifiable();
    }

    @Override
    default void addAll(java.util.Map<? extends K, ? extends V> other) {
        throw unmodifiable();
    }

    @Override
    default V putIfAbsent(K key, dart.runtime.Funcs.Func0<V> ifAbsent) {
        throw unmodifiable();
    }

    static RuntimeException unmodifiable() {
        return new dart.core.UnsupportedError("Cannot modify unmodifiable map");
    }
}
