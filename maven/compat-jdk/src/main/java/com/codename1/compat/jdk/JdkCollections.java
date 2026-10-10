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
package com.codename1.compat.jdk;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/// The members of the collection classes a desktop application names and
/// the device's classes do not have: the way into a stream, the immutable
/// `List.of`, `Set.of` and `Map.of` families, and a few more.
///
/// The build's remap step redirects each such call here; an instance method
/// arrives with its receiver as the first argument. `List.of`, `Set.of` and
/// `Map.of` differ only in what they return, which a class cannot overload
/// on, so each is here under a name of its own.
///
/// The immutable collections reject null elements and every mutation, as
/// the JDK's do. A set and a map iterate in the order they were given.
public final class JdkCollections {

    private JdkCollections() {
    }

    // ---- into a stream ----

    /// `collection.stream()`: the collection's own `stream()` where its
    /// class declares one, its elements otherwise.
    @SuppressWarnings("unchecked")
    public static <E> Stream<E> stream(Collection<E> collection) {
        if (collection instanceof StreamSource) {
            return ((StreamSource<E>) collection).stream();
        }
        return defaultStream(collection);
    }

    /// `collection.parallelStream()`. There is one thread to run on, so the
    /// stream is sequential, which the JDK allows a parallel stream to be.
    @SuppressWarnings("unchecked")
    public static <E> Stream<E> parallelStream(Collection<E> collection) {
        if (collection instanceof ParallelStreamSource) {
            return ((ParallelStreamSource<E>) collection).parallelStream();
        }
        return defaultStream(collection);
    }

    /// The elements of `collection`, as `Collection`'s own default
    /// `stream()` answers them. This is where `super.stream()` goes: unlike
    /// [#stream(Collection)] it never calls back into the collection's own
    /// method, which is the one asking.
    public static <E> Stream<E> defaultStream(Collection<E> collection) {
        Objects.requireNonNull(collection);
        return ObjPipeline.<E>over(collection);
    }

    /// `stream()` called on a type the build has no rule for by name: an
    /// application's own class, or an interface of another library that
    /// extends `Collection`. The receiver arrives as whatever it is. A class
    /// that declares `stream()` was marked a [StreamSource] by the build and
    /// answers for itself; any other collection gives its elements. What is
    /// neither inherited its `stream()` from a class the build never saw,
    /// and is refused, loudly, rather than run wrongly.
    @SuppressWarnings("unchecked")
    public static <E> Stream<E> streamOf(Object receiver) {
        if (receiver instanceof StreamSource) {
            return ((StreamSource<E>) receiver).stream();
        }
        if (receiver instanceof Collection) {
            return defaultStream((Collection<E>) receiver);
        }
        throw noStream("stream", receiver);
    }

    /// As [#streamOf], for `parallelStream()`.
    @SuppressWarnings("unchecked")
    public static <E> Stream<E> parallelStreamOf(Object receiver) {
        if (receiver instanceof ParallelStreamSource) {
            return ((ParallelStreamSource<E>) receiver).parallelStream();
        }
        if (receiver instanceof Collection) {
            return defaultStream((Collection<E>) receiver);
        }
        throw noStream("parallelStream", receiver);
    }

    private static RuntimeException noStream(String method, Object receiver) {
        if (receiver == null) {
            return new NullPointerException();
        }
        return new UnsupportedOperationException(method + "() is supported on a java.util.Collection and on a "
                + "class that declares it, not on " + receiver.getClass().getName());
    }

    public static <T> Stream<T> stream(T[] array) {
        return ObjPipeline.ofArray(array, 0, array.length);
    }

    public static <T> Stream<T> stream(T[] array, int startInclusive, int endExclusive) {
        return ObjPipeline.ofArray(array, startInclusive, endExclusive);
    }

    public static IntStream stream(int[] array) {
        return IntPipeline.ofArray(array, 0, array.length);
    }

    public static IntStream stream(int[] array, int startInclusive, int endExclusive) {
        return IntPipeline.ofArray(array, startInclusive, endExclusive);
    }

    public static LongStream stream(long[] array) {
        return LongPipeline.ofArray(array, 0, array.length);
    }

    public static LongStream stream(long[] array, int startInclusive, int endExclusive) {
        return LongPipeline.ofArray(array, startInclusive, endExclusive);
    }

    public static DoubleStream stream(double[] array) {
        return DoublePipeline.ofArray(array, 0, array.length);
    }

    public static DoubleStream stream(double[] array, int startInclusive, int endExclusive) {
        return DoublePipeline.ofArray(array, startInclusive, endExclusive);
    }

    // ---- Collection, Iterator, Collections ----

    /// `collection.toArray(String[]::new)`.
    public static <T> T[] toArray(Collection<?> collection, IntFunction<T[]> generator) {
        return collection.toArray(generator.apply(0));
    }

    public static <E> void forEachRemaining(Iterator<E> iterator, Consumer<? super E> action) {
        Objects.requireNonNull(action);
        while (iterator.hasNext()) {
            action.accept(iterator.next());
        }
    }

    public static <T> Iterator<T> emptyIterator() {
        return Collections.<T>emptyList().iterator();
    }

    // ---- Map.Entry ----

    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new ImmutableEntry<K, V>(key, value);
    }

    public static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K, V>> comparingByKey() {
        return (a, b) -> a.getKey().compareTo(b.getKey());
    }

    public static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K, V>> comparingByValue() {
        return (a, b) -> a.getValue().compareTo(b.getValue());
    }

    public static <K, V> Comparator<Map.Entry<K, V>> comparingByKey(Comparator<? super K> comparator) {
        Objects.requireNonNull(comparator);
        return (a, b) -> comparator.compare(a.getKey(), b.getKey());
    }

    public static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(Comparator<? super V> comparator) {
        Objects.requireNonNull(comparator);
        return (a, b) -> comparator.compare(a.getValue(), b.getValue());
    }

    // ---- List.of, Set.of, Map.of ----

    /// A list of `elements`, which the caller hands over: nothing else may
    /// hold the array.
    static <E> List<E> newList(Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            Objects.requireNonNull(elements[i]);
        }
        return new ImmutableList<E>(elements);
    }

    /// A set of `elements`. Two equal ones are an error when `strict`, as in
    /// `Set.of`, and one element otherwise, as in `Set.copyOf`.
    @SuppressWarnings("unchecked")
    static <E> Set<E> newSet(Object[] elements, boolean strict) {
        LinkedHashSet<E> all = new LinkedHashSet<E>();
        for (int i = 0; i < elements.length; i++) {
            if (!all.add((E) Objects.requireNonNull(elements[i])) && strict) {
                throw new IllegalArgumentException("duplicate element: " + elements[i]);
            }
        }
        return new ImmutableSet<E>(all);
    }

    /// A map of `keysAndValues`, alternating.
    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> newMap(Object[] keysAndValues) {
        LinkedHashMap<K, V> all = new LinkedHashMap<K, V>();
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

    public static <E> List<E> listOf() {
        return newList(new Object[0]);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> List<E> listOf(E... elements) {
        return newList(elements.clone());
    }

    public static <E> List<E> listCopyOf(Collection<? extends E> collection) {
        if (collection instanceof ImmutableList) {
            @SuppressWarnings("unchecked")
            List<E> same = (List<E>) collection;
            return same;
        }
        return newList(collection.toArray());
    }

    public static <E> Set<E> setOf() {
        return newSet(new Object[0], true);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> Set<E> setOf(E... elements) {
        return newSet(elements, true);
    }

    public static <E> Set<E> setCopyOf(Collection<? extends E> collection) {
        if (collection instanceof ImmutableSet) {
            @SuppressWarnings("unchecked")
            Set<E> same = (Set<E>) collection;
            return same;
        }
        return newSet(collection.toArray(), false);
    }

    public static <K, V> Map<K, V> mapOf() {
        return newMap(new Object[0]);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <K, V> Map<K, V> mapOfEntries(Map.Entry<? extends K, ? extends V>... entries) {
        Object[] flat = new Object[entries.length * 2];
        for (int i = 0; i < entries.length; i++) {
            flat[i * 2] = entries[i].getKey();
            flat[i * 2 + 1] = entries[i].getValue();
        }
        return newMap(flat);
    }

    public static <K, V> Map<K, V> mapCopyOf(Map<? extends K, ? extends V> map) {
        if (map instanceof ImmutableMap) {
            @SuppressWarnings("unchecked")
            Map<K, V> same = (Map<K, V>) map;
            return same;
        }
        Object[] flat = new Object[map.size() * 2];
        int at = 0;
        for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
            flat[at++] = e.getKey();
            flat[at++] = e.getValue();
        }
        return newMap(flat);
    }

    public static <E> List<E> listOf(E e1) {
        return newList(new Object[] {e1});
    }

    public static <E> List<E> listOf(E e1, E e2) {
        return newList(new Object[] {e1, e2});
    }

    public static <E> List<E> listOf(E e1, E e2, E e3) {
        return newList(new Object[] {e1, e2, e3});
    }

    public static <E> List<E> listOf(E e1, E e2, E e3, E e4) {
        return newList(new Object[] {e1, e2, e3, e4});
    }

    public static <E> List<E> listOf(E e1, E e2, E e3, E e4, E e5) {
        return newList(new Object[] {e1, e2, e3, e4, e5});
    }

    public static <E> List<E> listOf(E e1, E e2, E e3, E e4, E e5, E e6) {
        return newList(new Object[] {e1, e2, e3, e4, e5, e6});
    }

    public static <E> List<E> listOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return newList(new Object[] {e1, e2, e3, e4, e5, e6, e7});
    }

    public static <E> List<E> listOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return newList(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8});
    }

    public static <E> List<E> listOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return newList(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9});
    }

    public static <E> List<E> listOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return newList(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9, e10});
    }

    public static <E> Set<E> setOf(E e1) {
        return newSet(new Object[] {e1}, true);
    }

    public static <E> Set<E> setOf(E e1, E e2) {
        return newSet(new Object[] {e1, e2}, true);
    }

    public static <E> Set<E> setOf(E e1, E e2, E e3) {
        return newSet(new Object[] {e1, e2, e3}, true);
    }

    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4) {
        return newSet(new Object[] {e1, e2, e3, e4}, true);
    }

    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5) {
        return newSet(new Object[] {e1, e2, e3, e4, e5}, true);
    }

    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5, E e6) {
        return newSet(new Object[] {e1, e2, e3, e4, e5, e6}, true);
    }

    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return newSet(new Object[] {e1, e2, e3, e4, e5, e6, e7}, true);
    }

    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return newSet(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8}, true);
    }

    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return newSet(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9}, true);
    }

    public static <E> Set<E> setOf(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return newSet(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9, e10}, true);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1) {
        return newMap(new Object[] {k1, v1});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        return newMap(new Object[] {k1, v1, k2, v2});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        return newMap(new Object[] {k1, v1, k2, v2, k3, v3});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return newMap(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return newMap(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        return newMap(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        return newMap(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
        return newMap(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        return newMap(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return newMap(new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10});
    }

    private static UnsupportedOperationException immutable() {
        return new UnsupportedOperationException();
    }

    private static final class ImmutableEntry<K, V> implements Map.Entry<K, V> {

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

    private static final class ImmutableList<E> extends AbstractList<E> implements RandomAccess {

        private final Object[] elements;

        ImmutableList(Object[] elements) {
            this.elements = elements;
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

    private static final class ImmutableSet<E> extends AbstractSet<E> {

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

    private static final class ImmutableMap<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> entries;
        private final Set<Map.Entry<K, V>> entrySet;

        ImmutableMap(LinkedHashMap<K, V> entries) {
            this.entries = entries;
            LinkedHashSet<Map.Entry<K, V>> all = new LinkedHashSet<Map.Entry<K, V>>();
            for (Map.Entry<K, V> e : entries.entrySet()) {
                all.add(new ImmutableEntry<K, V>(e.getKey(), e.getValue()));
            }
            this.entrySet = new ImmutableSet<Map.Entry<K, V>>(all);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return entrySet;
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
