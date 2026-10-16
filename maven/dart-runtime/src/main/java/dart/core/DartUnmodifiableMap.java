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
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An unmodifiable Dart map: a copy of the entries it is built from, whose every write
 * -- map[k] = v, remove, clear, addAll, removeWhere, or a remove through one of its
 * views -- is a Dart UnsupportedError, as Map.unmodifiable and the maps Dart hands out
 * read-only (Uri.queryParameters) are. Measured against the Dart 3.9 VM
 * (Map.unmodifiable): every mutator throws immediately, before calling any callback
 * argument it was given, even when the mutation would otherwise be a no-op (the key
 * already holds the value being proposed, the map being merged in is empty, and so on).
 *
 * <p>Every mutator below is therefore overridden to throw up front, unconditionally --
 * not just {@link #put}/{@link #putAll}/{@link #remove}/{@link #clear}, which this class
 * always held. Two classes of mutator used to slip through:</p>
 *
 * <ul>
 * <li>{@link DartMap}'s own extensions ({@link #putIfAbsentDart}, {@link #update}) read
 * the map first and only fall through to {@code put}/{@code get} when a write is
 * actually needed, so a lookup that found the key already present, or a callback that
 * ran before the (overridden) write, returned a value or had a side effect instead of
 * throwing -- {@code Uri.parse("?a=1").queryParameters().putIfAbsentDart("a", cb)}
 * returned the existing value rather than throwing, and {@code update} ran its callback
 * before the {@code put} it feeds into failed.</li>
 * <li>{@code java.util.HashMap}, which {@link DartMap} extends, implements
 * {@code putIfAbsent}, {@code compute}, {@code computeIfAbsent}, {@code
 * computeIfPresent}, {@code merge}, both {@code replace} overloads, {@code remove(Object,
 * Object)} and {@code replaceAll} by manipulating its table directly rather than calling
 * the public {@code put}/{@code remove} -- confirmed by probing a subclass that overrides
 * {@code put}/{@code remove} to record every call; none of those nine methods triggers
 * either override. Overriding only {@code put}/{@code remove}, the usual way to guard a
 * {@code HashMap} subclass, therefore leaves every one of them able to mutate a
 * supposedly-frozen map.</li>
 * </ul>
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
    public V removeDart(Object key) {
        throw refused();
    }

    @Override
    public void addAll(Map<? extends K, ? extends V> other) {
        throw refused();
    }

    @Override
    public void addEntries(Iterable<? extends MapEntry<K, V>> entries) {
        throw refused();
    }

    @Override
    public V idxSet(K key, V value) {
        throw refused();
    }

    @Override
    public V putIfAbsentDart(K key, Funcs.Func0<V> ifAbsent) {
        throw refused();
    }

    @Override
    public V update(K key, Funcs.Func1<V, V> update, Funcs.Func0<V> ifAbsent) {
        throw refused();
    }

    // --- java.util.Map's own mutators: HashMap implements every one of these by
    // --- manipulating its table directly (see the class comment), so none of them
    // --- reaches put()/remove() above and each needs its own override.

    @Override
    public V putIfAbsent(K key, V value) {
        throw refused();
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw refused();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        throw refused();
    }

    @Override
    public V replace(K key, V value) {
        throw refused();
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        throw refused();
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw refused();
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw refused();
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        throw refused();
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
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
