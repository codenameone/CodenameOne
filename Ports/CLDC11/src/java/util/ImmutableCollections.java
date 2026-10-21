/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package java.util;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/// What `List.of`, `Set.of`, `Map.of`, their `copyOf` forms and `Map.entry`
/// answer: collections that reject null elements and every mutation, as the
/// JDK's do. A set and a map iterate in no particular order, which the JDK
/// leaves unspecified too; they are hashed rather than linked so an
/// application that never calls them does not keep `LinkedHashMap`.
final class ImmutableCollections {

    private ImmutableCollections() {
    }

    /// A list of `elements`, which the caller hands over: nothing else may
    /// hold the array.
    static <E> List<E> list(Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            Objects.requireNonNull(elements[i]);
        }
        return new ImmutableList<E>(elements);
    }

    /// A set of `elements`. Two equal ones are an error when `strict`, as in
    /// `Set.of`, and one element otherwise, as in `Set.copyOf`.
    @SuppressWarnings("unchecked")
    static <E> Set<E> set(Object[] elements, boolean strict) {
        HashSet<E> all = new HashSet<E>();
        for (int i = 0; i < elements.length; i++) {
            if (!all.add((E) Objects.requireNonNull(elements[i])) && strict) {
                throw new IllegalArgumentException("duplicate element: " + elements[i]);
            }
        }
        return new ImmutableSet<E>(all);
    }

    /// A map of `keysAndValues`, alternating.
    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> map(Object[] keysAndValues) {
        HashMap<K, V> all = new HashMap<K, V>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            K key = (K) Objects.requireNonNull(keysAndValues[i]);
            V value = (V) Objects.requireNonNull(keysAndValues[i + 1]);
            if (all.containsKey(key)) {
                throw new IllegalArgumentException("duplicate key: " + key);
            }
            all.put(key, value);
        }
        return new ImmutableMap<K, V>(all);
    }

    /// A list of a copy of `elements`.
    static <E> List<E> listCopy(Object[] elements) {
        Object[] copy = new Object[elements.length];
        System.arraycopy(elements, 0, copy, 0, elements.length);
        return list(copy);
    }

    @SuppressWarnings("unchecked")
    static <E> List<E> listCopyOf(Collection<? extends E> collection) {
        if (collection instanceof ImmutableList) {
            return (List<E>) collection;
        }
        return list(collection.toArray());
    }

    @SuppressWarnings("unchecked")
    static <E> Set<E> setCopyOf(Collection<? extends E> collection) {
        if (collection instanceof ImmutableSet) {
            return (Set<E>) collection;
        }
        return set(collection.toArray(), false);
    }

    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> mapCopyOf(Map<? extends K, ? extends V> map) {
        if (map instanceof ImmutableMap) {
            return (Map<K, V>) map;
        }
        Object[] flat = new Object[map.size() * 2];
        int at = 0;
        for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
            flat[at++] = e.getKey();
            flat[at++] = e.getValue();
        }
        return map(flat);
    }

    static <K, V> Map<K, V> mapOfEntries(Map.Entry<? extends K, ? extends V>[] entries) {
        Object[] flat = new Object[entries.length * 2];
        for (int i = 0; i < entries.length; i++) {
            flat[i * 2] = entries[i].getKey();
            flat[i * 2 + 1] = entries[i].getValue();
        }
        return map(flat);
    }

    private static UnsupportedOperationException immutable() {
        return new UnsupportedOperationException();
    }

    static final class ImmutableEntry<K, V> implements Map.Entry<K, V> {

        private final K key;
        private final V value;

        ImmutableEntry(K key, V value) {
            this.key = Objects.requireNonNull(key);
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V newValue) {
            throw immutable();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
            return key.equals(other.getKey()) && value.equals(other.getValue());
        }

        @Override
        public int hashCode() {
            return key.hashCode() ^ value.hashCode();
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    // None of the factory collections implements Serializable: Codename One
    // has no Java object serialization (no ObjectOutputStream on any device
    // runtime), so the JDK's serialization contract for List.of, Set.of and
    // Map.of has nothing to apply to.
    static final class ImmutableList<E> extends AbstractList<E> implements RandomAccess {

        private final Object[] elements;

        ImmutableList(Object[] elements) {
            this.elements = elements;
        }

        /// An immutable copy, not AbstractList's view: the view's clear()
        /// of an empty range succeeds, where this list must always refuse.
        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            if (fromIndex < 0 || toIndex > elements.length) {
                throw new IndexOutOfBoundsException();
            }
            if (fromIndex > toIndex) {
                throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
            }
            Object[] part = new Object[toIndex - fromIndex];
            System.arraycopy(elements, fromIndex, part, 0, part.length);
            return new ImmutableList<E>(part);
        }

        @Override
        @SuppressWarnings("unchecked")
        public E get(int index) {
            if (index < 0 || index >= elements.length) {
                throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length "
                        + elements.length);
            }
            return (E) elements[index];
        }

        @Override
        public int size() {
            return elements.length;
        }

        @Override
        public boolean contains(Object o) {
            return indexOf(o) >= 0;
        }

        @Override
        public int indexOf(Object o) {
            Objects.requireNonNull(o);
            for (int i = 0; i < elements.length; i++) {
                if (o.equals(elements[i])) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            Objects.requireNonNull(o);
            for (int i = elements.length - 1; i >= 0; i--) {
                if (o.equals(elements[i])) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean add(E e) {
            throw immutable();
        }

        @Override
        public void add(int index, E e) {
            throw immutable();
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw immutable();
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            throw immutable();
        }

        @Override
        public void clear() {
            throw immutable();
        }

        @Override
        public E remove(int index) {
            throw immutable();
        }

        @Override
        public boolean remove(Object o) {
            throw immutable();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw immutable();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw immutable();
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            throw immutable();
        }

        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            throw immutable();
        }

        @Override
        public void sort(Comparator<? super E> c) {
            throw immutable();
        }

        @Override
        public E set(int index, E e) {
            throw immutable();
        }
    }

    static final class ImmutableSet<E> extends AbstractSet<E> {

        private final Set<E> elements;

        ImmutableSet(Set<E> elements) {
            this.elements = elements;
        }

        @Override
        public Iterator<E> iterator() {
            Iterator<E> it = elements.iterator();
            return new Iterator<E>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    return it.next();
                }

                @Override
                public void remove() {
                    throw immutable();
                }
            };
        }

        @Override
        public int size() {
            return elements.size();
        }

        @Override
        public boolean contains(Object o) {
            return elements.contains(Objects.requireNonNull(o));
        }

        @Override
        public boolean add(E e) {
            throw immutable();
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw immutable();
        }

        @Override
        public void clear() {
            throw immutable();
        }

        @Override
        public boolean remove(Object o) {
            throw immutable();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw immutable();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw immutable();
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            throw immutable();
        }
    }

    static final class ImmutableMap<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> entries;
        private final Set<Map.Entry<K, V>> entrySet;

        ImmutableMap(HashMap<K, V> entries) {
            this.entries = entries;
            HashSet<Map.Entry<K, V>> all = new HashSet<Map.Entry<K, V>>();
            for (Map.Entry<K, V> e : entries.entrySet()) {
                all.add(new ImmutableEntry<K, V>(e.getKey(), e.getValue()));
            }
            this.entrySet = new ImmutableSet<Map.Entry<K, V>>(all);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return entrySet;
        }

        // AbstractMap's views clear through their iterator, which on an
        // empty map removes nothing and so returns normally; these throw
        // on every mutation, empty or not.
        @Override
        public Set<K> keySet() {
            return Collections.unmodifiableSet(super.keySet());
        }

        @Override
        public Collection<V> values() {
            return Collections.unmodifiableCollection(super.values());
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public V get(Object key) {
            return entries.get(Objects.requireNonNull(key));
        }

        @Override
        public boolean containsKey(Object key) {
            return entries.containsKey(Objects.requireNonNull(key));
        }

        @Override
        public boolean containsValue(Object value) {
            return entries.containsValue(Objects.requireNonNull(value));
        }

        @Override
        public V put(K key, V value) {
            throw immutable();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            throw immutable();
        }

        @Override
        public V remove(Object key) {
            throw immutable();
        }

        @Override
        public void clear() {
            throw immutable();
        }

        @Override
        public V putIfAbsent(K key, V value) {
            throw immutable();
        }

        @Override
        public boolean remove(Object key, Object value) {
            throw immutable();
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            throw immutable();
        }

        @Override
        public V replace(K key, V value) {
            throw immutable();
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            throw immutable();
        }

        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            throw immutable();
        }

        @Override
        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw immutable();
        }

        @Override
        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw immutable();
        }

        @Override
        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            throw immutable();
        }
    }
}
