/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.util;


/**
 * {@code Collections} contains static methods which operate on
 * {@code Collection} classes.
 * 
 * @since 1.2
 */
public class Collections {

    private static final class CopiesList<E> extends AbstractList<E> {
        private final int n;

        private final E element;

        CopiesList(int length, E object) {
            if (length < 0) {
                throw new IllegalArgumentException();
            }
            n = length;
            element = object;
        }

        @Override
        public boolean contains(Object object) {
            return element == null ? object == null : element.equals(object);
        }

        @Override
        public int size() {
            return n;
        }

        @Override
        public E get(int location) {
            if (0 <= location && location < n) {
                return element;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    @SuppressWarnings("unchecked")
    private static final class EmptyList extends AbstractList implements
            RandomAccess {
        private static final long serialVersionUID = 8842843931221139166L;

        @Override
        public boolean contains(Object object) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Object get(int location) {
            throw new IndexOutOfBoundsException();
        }

        private Object readResolve() {
            return Collections.EMPTY_LIST;
        }
    }

    @SuppressWarnings("unchecked")
    private static final class EmptySet extends AbstractSet {
        private static final long serialVersionUID = 1582296315990362920L;

        @Override
        public boolean contains(Object object) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Iterator iterator() {
            return new Iterator() {
                public boolean hasNext() {
                    return false;
                }

                public Object next() {
                    throw new NoSuchElementException();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        private Object readResolve() {
            return Collections.EMPTY_SET;
        }
    }

    @SuppressWarnings("unchecked")
    private static final class EmptyMap extends AbstractMap {
        private static final long serialVersionUID = 6428348081105594320L;

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Set entrySet() {
            return EMPTY_SET;
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public Set keySet() {
            return EMPTY_SET;
        }

        @Override
        public Collection values() {
            return EMPTY_LIST;
        }

        private Object readResolve() {
            return Collections.EMPTY_MAP;
        }
    }

    /**
     * An empty immutable instance of {@link List}.
     */
    @SuppressWarnings("unchecked")
    public static final List EMPTY_LIST = new EmptyList();

    /**
     * An empty immutable instance of {@link Set}.
     */
    @SuppressWarnings("unchecked")
    public static final Set EMPTY_SET = new EmptySet();

    /**
     * An empty immutable instance of {@link Map}.
     */
    @SuppressWarnings("unchecked")
    public static final Map EMPTY_MAP = new EmptyMap();

    /**
     * This class is a singleton so that equals() and hashCode() work properly.
     */
    private static final class ReverseComparator<T> implements Comparator<T> {

        private static final ReverseComparator<Object> INSTANCE
                = new ReverseComparator<Object>();

        @SuppressWarnings("unchecked")
        public int compare(T o1, T o2) {
            java.lang.Comparable<T> c2 = (java.lang.Comparable<T>) o2;
            return c2.compareTo(o1);
        }
    }

    private static final class ReverseComparatorWithComparator<T> implements
            Comparator<T> {
        private static final long serialVersionUID = 4374092139857L;

        private final Comparator<T> comparator;

        ReverseComparatorWithComparator(Comparator<T> comparator) {
            super();
            this.comparator = comparator;
        }

        public int compare(T o1, T o2) {
            return comparator.compare(o2, o1);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ReverseComparatorWithComparator
                    && ((ReverseComparatorWithComparator) o).comparator
                            .equals(comparator);
        }

        @Override
        public int hashCode() {
            return ~comparator.hashCode();
        }
    }

    private static final class SingletonSet<E> extends AbstractSet<E> {
        final E element;

        SingletonSet(E object) {
            element = object;
        }

        @Override
        public boolean contains(Object object) {
            return element == null ? object == null : element.equals(object);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                boolean hasNext = true;

                public boolean hasNext() {
                    return hasNext;
                }

                public E next() {
                    if (hasNext) {
                        hasNext = false;
                        return element;
                    }
                    throw new NoSuchElementException();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    private static final class SingletonList<E> extends AbstractList<E> {
        final E element;

        SingletonList(E object) {
            element = object;
        }

        @Override
        public boolean contains(Object object) {
            return element == null ? object == null : element.equals(object);
        }

        @Override
        public E get(int location) {
            if (location == 0) {
                return element;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public int size() {
            return 1;
        }
    }

    private static final class SingletonMap<K, V> extends AbstractMap<K, V> {
        final K k;

        final V v;

        SingletonMap(K key, V value) {
            k = key;
            v = value;
        }

        @Override
        public boolean containsKey(Object key) {
            return k == null ? key == null : k.equals(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return v == null ? value == null : v.equals(value);
        }

        @Override
        public V get(Object key) {
            if (containsKey(key)) {
                return v;
            }
            return null;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new AbstractSet<Map.Entry<K, V>>() {
                @Override
                public boolean contains(Object object) {
                    if (object instanceof Map.Entry) {
                        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
                        return containsKey(entry.getKey())
                                && containsValue(entry.getValue());
                    }
                    return false;
                }

                @Override
                public int size() {
                    return 1;
                }

                @Override
                public Iterator<Map.Entry<K, V>> iterator() {
                    return new Iterator<Map.Entry<K, V>>() {
                        boolean hasNext = true;

                        public boolean hasNext() {
                            return hasNext;
                        }

                        public Map.Entry<K, V> next() {
                            if (!hasNext) {
                                throw new NoSuchElementException();
                            }

                            hasNext = false;
                            return new MapEntry<K, V>(k, v) {
                                @Override
                                public V setValue(V value) {
                                    throw new UnsupportedOperationException();
                                }
                            };
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        }
    }

    static class SynchronizedCollection<E> implements Collection<E> {
        private static final long serialVersionUID = 3053995032091335093L;

        final Collection<E> c;

        final Object mutex;

        SynchronizedCollection(Collection<E> collection) {
            c = collection;
            mutex = this;
        }

        SynchronizedCollection(Collection<E> collection, Object mutex) {
            c = collection;
            this.mutex = mutex;
        }

        public boolean add(E object) {
            synchronized (mutex) {
                return c.add(object);
            }
        }

        public boolean addAll(Collection<? extends E> collection) {
            synchronized (mutex) {
                return c.addAll(collection);
            }
        }

        public void clear() {
            synchronized (mutex) {
                c.clear();
            }
        }

        public boolean contains(Object object) {
            synchronized (mutex) {
                return c.contains(object);
            }
        }

        public boolean containsAll(Collection<?> collection) {
            synchronized (mutex) {
                return c.containsAll(collection);
            }
        }

        public boolean isEmpty() {
            synchronized (mutex) {
                return c.isEmpty();
            }
        }

        public Iterator<E> iterator() {
            synchronized (mutex) {
                return c.iterator();
            }
        }

        public boolean remove(Object object) {
            synchronized (mutex) {
                return c.remove(object);
            }
        }

        public boolean removeAll(Collection<?> collection) {
            synchronized (mutex) {
                return c.removeAll(collection);
            }
        }

        public boolean retainAll(Collection<?> collection) {
            synchronized (mutex) {
                return c.retainAll(collection);
            }
        }

        public int size() {
            synchronized (mutex) {
                return c.size();
            }
        }

        public java.lang.Object[] toArray() {
            synchronized (mutex) {
                return c.toArray();
            }
        }

        @Override
        public String toString() {
            synchronized (mutex) {
                return c.toString();
            }
        }

        public <T> T[] toArray(T[] array) {
            synchronized (mutex) {
                return c.toArray(array);
            }
        }
    }

    static class SynchronizedRandomAccessList<E> extends SynchronizedList<E>
            implements RandomAccess {
        private static final long serialVersionUID = 1530674583602358482L;

        SynchronizedRandomAccessList(List<E> l) {
            super(l);
        }

        SynchronizedRandomAccessList(List<E> l, Object mutex) {
            super(l, mutex);
        }

        @Override
        public List<E> subList(int start, int end) {
            synchronized (mutex) {
                return new SynchronizedRandomAccessList<E>(list.subList(start,
                        end), mutex);
            }
        }

        /**
         * Replaces this SynchronizedRandomAccessList with a SynchronizedList so
         * that JREs before 1.4 can deserialize this object without any
         * problems. This is necessary since RandomAccess API was introduced
         * only in 1.4.
         * <p>
         * 
         * @return SynchronizedList
         * 
         * @see SynchronizedList#readResolve()
         */
        private Object writeReplace() {
            return new SynchronizedList<E>(list);
        }
    }

    static class SynchronizedList<E> extends SynchronizedCollection<E>
            implements List<E> {
        private static final long serialVersionUID = -7754090372962971524L;

        final List<E> list;

        SynchronizedList(List<E> l) {
            super(l);
            list = l;
        }

        SynchronizedList(List<E> l, Object mutex) {
            super(l, mutex);
            list = l;
        }

        public void add(int location, E object) {
            synchronized (mutex) {
                list.add(location, object);
            }
        }

        public boolean addAll(int location, Collection<? extends E> collection) {
            synchronized (mutex) {
                return list.addAll(location, collection);
            }
        }

        @Override
        public boolean equals(Object object) {
            synchronized (mutex) {
                return list.equals(object);
            }
        }

        public E get(int location) {
            synchronized (mutex) {
                return list.get(location);
            }
        }

        @Override
        public int hashCode() {
            synchronized (mutex) {
                return list.hashCode();
            }
        }

        public int indexOf(Object object) {
            final int size;
            final Object[] array;
            synchronized (mutex) {
                size = list.size();
                array = new Object[size];
                list.toArray(array);
            }
            if (null != object)
                for (int i = 0; i < size; i++) {
                    if (object.equals(array[i])) {
                        return i;
                    }
                }
            else {
                for (int i = 0; i < size; i++) {
                    if (null == array[i]) {
                        return i;
                    }
                }
            }
            return -1;
        }

        public int lastIndexOf(Object object) {
            final int size;
            final Object[] array;
            synchronized (mutex) {
                size = list.size();
                array = new Object[size];
                list.toArray(array);
            }
            if (null != object)
                for (int i = size - 1; i >= 0; i--) {
                    if (object.equals(array[i])) {
                        return i;
                    }
                }
            else {
                for (int i = size - 1; i >= 0; i--) {
                    if (null == array[i]) {
                        return i;
                    }
                }
            }
            return -1;
        }

        public ListIterator<E> listIterator() {
            synchronized (mutex) {
                return list.listIterator();
            }
        }

        public ListIterator<E> listIterator(int location) {
            synchronized (mutex) {
                return list.listIterator(location);
            }
        }

        public E remove(int location) {
            synchronized (mutex) {
                return list.remove(location);
            }
        }

        public E set(int location, E object) {
            synchronized (mutex) {
                return list.set(location, object);
            }
        }

        public List<E> subList(int start, int end) {
            synchronized (mutex) {
                return new SynchronizedList<E>(list.subList(start, end), mutex);
            }
        }

        /**
         * Resolves SynchronizedList instances to SynchronizedRandomAccessList
         * instances if the underlying list is a Random Access list.
         * <p>
         * This is necessary since SynchronizedRandomAccessList instances are
         * replaced with SynchronizedList instances during serialization for
         * compliance with JREs before 1.4.
         * <p>
         * 
         * @return a SynchronizedList instance if the underlying list implements
         *         RandomAccess interface, or this same object if not.
         * 
         * @see SynchronizedRandomAccessList#writeReplace()
         */
        private Object readResolve() {
            if (list instanceof RandomAccess) {
                return new SynchronizedRandomAccessList<E>(list, mutex);
            }
            return this;
        }
    }

    static class SynchronizedMap<K, V> implements Map<K, V> {
        private final Map<K, V> m;

        final Object mutex;

        SynchronizedMap(Map<K, V> map) {
            m = map;
            mutex = this;
        }

        SynchronizedMap(Map<K, V> map, Object mutex) {
            m = map;
            this.mutex = mutex;
        }

        public void clear() {
            synchronized (mutex) {
                m.clear();
            }
        }

        public boolean containsKey(Object key) {
            synchronized (mutex) {
                return m.containsKey(key);
            }
        }

        public boolean containsValue(Object value) {
            synchronized (mutex) {
                return m.containsValue(value);
            }
        }

        public Set<Map.Entry<K, V>> entrySet() {
            synchronized (mutex) {
                return new SynchronizedSet<Map.Entry<K, V>>(m.entrySet(), mutex);
            }
        }

        @Override
        public boolean equals(Object object) {
            synchronized (mutex) {
                return m.equals(object);
            }
        }

        public V get(Object key) {
            synchronized (mutex) {
                return m.get(key);
            }
        }

        @Override
        public int hashCode() {
            synchronized (mutex) {
                return m.hashCode();
            }
        }

        public boolean isEmpty() {
            synchronized (mutex) {
                return m.isEmpty();
            }
        }

        public Set<K> keySet() {
            synchronized (mutex) {
                return new SynchronizedSet<K>(m.keySet(), mutex);
            }
        }

        public V put(K key, V value) {
            synchronized (mutex) {
                return m.put(key, value);
            }
        }

        public void putAll(Map<? extends K, ? extends V> map) {
            synchronized (mutex) {
                m.putAll(map);
            }
        }

        public V remove(Object key) {
            synchronized (mutex) {
                return m.remove(key);
            }
        }

        public int size() {
            synchronized (mutex) {
                return m.size();
            }
        }

        public Collection<V> values() {
            synchronized (mutex) {
                return new SynchronizedCollection<V>(m.values(), mutex);
            }
        }

        @Override
        public String toString() {
            synchronized (mutex) {
                return m.toString();
            }
        }
    }

    static class SynchronizedSet<E> extends SynchronizedCollection<E> implements
            Set<E> {
        private static final long serialVersionUID = 487447009682186044L;

        SynchronizedSet(Set<E> set) {
            super(set);
        }

        SynchronizedSet(Set<E> set, Object mutex) {
            super(set, mutex);
        }

        @Override
        public boolean equals(Object object) {
            synchronized (mutex) {
                return c.equals(object);
            }
        }

        @Override
        public int hashCode() {
            synchronized (mutex) {
                return c.hashCode();
            }
        }
    }

    static class SynchronizedSortedMap<K, V> extends SynchronizedMap<K, V>
            implements SortedMap<K, V> {
        private static final long serialVersionUID = -8798146769416483793L;

        private final SortedMap<K, V> sm;

        SynchronizedSortedMap(SortedMap<K, V> map) {
            super(map);
            sm = map;
        }

        SynchronizedSortedMap(SortedMap<K, V> map, Object mutex) {
            super(map, mutex);
            sm = map;
        }

        public Comparator<? super K> comparator() {
            synchronized (mutex) {
                return sm.comparator();
            }
        }

        public K firstKey() {
            synchronized (mutex) {
                return sm.firstKey();
            }
        }

        public SortedMap<K, V> headMap(K endKey) {
            synchronized (mutex) {
                return new SynchronizedSortedMap<K, V>(sm.headMap(endKey),
                        mutex);
            }
        }

        public K lastKey() {
            synchronized (mutex) {
                return sm.lastKey();
            }
        }

        public SortedMap<K, V> subMap(K startKey, K endKey) {
            synchronized (mutex) {
                return new SynchronizedSortedMap<K, V>(sm.subMap(startKey,
                        endKey), mutex);
            }
        }

        public SortedMap<K, V> tailMap(K startKey) {
            synchronized (mutex) {
                return new SynchronizedSortedMap<K, V>(sm.tailMap(startKey),
                        mutex);
            }
        }
    }

    static class SynchronizedSortedSet<E> extends SynchronizedSet<E> implements
            SortedSet<E> {
        private static final long serialVersionUID = 8695801310862127406L;

        private final SortedSet<E> ss;

        SynchronizedSortedSet(SortedSet<E> set) {
            super(set);
            ss = set;
        }

        SynchronizedSortedSet(SortedSet<E> set, Object mutex) {
            super(set, mutex);
            ss = set;
        }

        public Comparator<? super E> comparator() {
            synchronized (mutex) {
                return ss.comparator();
            }
        }

        public E first() {
            synchronized (mutex) {
                return ss.first();
            }
        }

        public SortedSet<E> headSet(E end) {
            synchronized (mutex) {
                return new SynchronizedSortedSet<E>(ss.headSet(end), mutex);
            }
        }

        public E last() {
            synchronized (mutex) {
                return ss.last();
            }
        }

        public SortedSet<E> subSet(E start, E end) {
            synchronized (mutex) {
                return new SynchronizedSortedSet<E>(ss.subSet(start, end),
                        mutex);
            }
        }

        public SortedSet<E> tailSet(E start) {
            synchronized (mutex) {
                return new SynchronizedSortedSet<E>(ss.tailSet(start), mutex);
            }
        }
    }

    private static class UnmodifiableCollection<E> implements Collection<E> {
        final Collection<E> c;

        UnmodifiableCollection(Collection<E> collection) {
            c = collection;
        }

        public boolean add(E object) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public boolean contains(Object object) {
            return c.contains(object);
        }

        public boolean containsAll(Collection<?> collection) {
            return c.containsAll(collection);
        }

        public boolean isEmpty() {
            return c.isEmpty();
        }

        public Iterator<E> iterator() {
            return new Iterator<E>() {
                Iterator<E> iterator = c.iterator();

                public boolean hasNext() {
                    return iterator.hasNext();
                }

                public E next() {
                    return iterator.next();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public boolean remove(Object object) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            return c.size();
        }

        public Object[] toArray() {
            return c.toArray();
        }

        public <T> T[] toArray(T[] array) {
            return c.toArray(array);
        }

        @Override
        public String toString() {
            return c.toString();
        }
    }

    private static class UnmodifiableRandomAccessList<E> extends
            UnmodifiableList<E> implements RandomAccess {
        UnmodifiableRandomAccessList(List<E> l) {
            super(l);
        }

        @Override
        public List<E> subList(int start, int end) {
            return new UnmodifiableRandomAccessList<E>(list.subList(start, end));
        }

        /**
         * Replaces this UnmodifiableRandomAccessList with an UnmodifiableList
         * so that JREs before 1.4 can deserialize this object without any
         * problems. This is necessary since RandomAccess API was introduced
         * only in 1.4.
         * <p>
         * 
         * @return UnmodifiableList
         * 
         * @see UnmodifiableList#readResolve()
         */
        private Object writeReplace() {
            return new UnmodifiableList<E>(list);
        }
    }

    private static class UnmodifiableList<E> extends UnmodifiableCollection<E>
            implements List<E> {
        final List<E> list;

        UnmodifiableList(List<E> l) {
            super(l);
            list = l;
        }

        public void add(int location, E object) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(int location, Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object object) {
            return list.equals(object);
        }

        public E get(int location) {
            return list.get(location);
        }

        @Override
        public int hashCode() {
            return list.hashCode();
        }

        public int indexOf(Object object) {
            return list.indexOf(object);
        }

        public int lastIndexOf(Object object) {
            return list.lastIndexOf(object);
        }

        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        public ListIterator<E> listIterator(final int location) {
            return new ListIterator<E>() {
                ListIterator<E> iterator = list.listIterator(location);

                public void add(E object) {
                    throw new UnsupportedOperationException();
                }

                public boolean hasNext() {
                    return iterator.hasNext();
                }

                public boolean hasPrevious() {
                    return iterator.hasPrevious();
                }

                public E next() {
                    return iterator.next();
                }

                public int nextIndex() {
                    return iterator.nextIndex();
                }

                public E previous() {
                    return iterator.previous();
                }

                public int previousIndex() {
                    return iterator.previousIndex();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }

                public void set(E object) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public E remove(int location) {
            throw new UnsupportedOperationException();
        }

        public E set(int location, E object) {
            throw new UnsupportedOperationException();
        }

        public List<E> subList(int start, int end) {
            return new UnmodifiableList<E>(list.subList(start, end));
        }

        /**
         * Resolves UnmodifiableList instances to UnmodifiableRandomAccessList
         * instances if the underlying list is a Random Access list.
         * <p>
         * This is necessary since UnmodifiableRandomAccessList instances are
         * replaced with UnmodifiableList instances during serialization for
         * compliance with JREs before 1.4.
         * <p>
         * 
         * @return an UnmodifiableList instance if the underlying list
         *         implements RandomAccess interface, or this same object if
         *         not.
         * 
         * @see UnmodifiableRandomAccessList#writeReplace()
         */
        private Object readResolve() {
            if (list instanceof RandomAccess) {
                return new UnmodifiableRandomAccessList<E>(list);
            }
            return this;
        }
    }

    private static class UnmodifiableMap<K, V> implements Map<K, V> {
        private final Map<K, V> m;

        private static class UnmodifiableEntrySet<K, V> extends
                UnmodifiableSet<Map.Entry<K, V>> {
            private static final long serialVersionUID = 7854390611657943733L;

            private static class UnmodifiableMapEntry<K, V> implements
                    Map.Entry<K, V> {
                Map.Entry<K, V> mapEntry;

                UnmodifiableMapEntry(Map.Entry<K, V> entry) {
                    mapEntry = entry;
                }

                @Override
                public boolean equals(Object object) {
                    return mapEntry.equals(object);
                }

                public K getKey() {
                    return mapEntry.getKey();
                }

                public V getValue() {
                    return mapEntry.getValue();
                }

                @Override
                public int hashCode() {
                    return mapEntry.hashCode();
                }

                public V setValue(V object) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String toString() {
                    return mapEntry.toString();
                }
            }

            UnmodifiableEntrySet(Set<Map.Entry<K, V>> set) {
                super(set);
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>() {
                    Iterator<Map.Entry<K, V>> iterator = c.iterator();

                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public Map.Entry<K, V> next() {
                        return new UnmodifiableMapEntry<K, V>(iterator.next());
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public Object[] toArray() {
                int length = c.size();
                Object[] result = new Object[length];
                Iterator<?> it = iterator();
                for (int i = length; --i >= 0;) {
                    result[i] = it.next();
                }
                return result;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T[] toArray(T[] contents) {
                int size = c.size(), index = 0;
                Iterator<Map.Entry<K, V>> it = iterator();
                Object[] arr = contents;
                if (size > arr.length) {
                    arr = new Object[size];
                }
                while (index < size) {
                    arr[index++] = (T) it.next();
                }
                if (index < arr.length) {
                    arr[index] = null;
                }
                return (T[])arr;
            }
        }

        UnmodifiableMap(Map<K, V> map) {
            m = map;
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public boolean containsKey(Object key) {
            return m.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return m.containsValue(value);
        }

        public Set<Map.Entry<K, V>> entrySet() {
            return new UnmodifiableEntrySet<K, V>(m.entrySet());
        }

        @Override
        public boolean equals(Object object) {
            return m.equals(object);
        }

        public V get(Object key) {
            return m.get(key);
        }

        @Override
        public int hashCode() {
            return m.hashCode();
        }

        public boolean isEmpty() {
            return m.isEmpty();
        }

        public Set<K> keySet() {
            return new UnmodifiableSet<K>(m.keySet());
        }

        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        public void putAll(Map<? extends K, ? extends V> map) {
            throw new UnsupportedOperationException();
        }

        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            return m.size();
        }

        public Collection<V> values() {
            return new UnmodifiableCollection<V>(m.values());
        }

        @Override
        public String toString() {
            return m.toString();
        }
    }

    private static class UnmodifiableSet<E> extends UnmodifiableCollection<E>
            implements Set<E> {
        private static final long serialVersionUID = -9215047833775013803L;

        UnmodifiableSet(Set<E> set) {
            super(set);
        }

        @Override
        public boolean equals(Object object) {
            return c.equals(object);
        }

        @Override
        public int hashCode() {
            return c.hashCode();
        }
    }

    private static class UnmodifiableSortedMap<K, V> extends
            UnmodifiableMap<K, V> implements SortedMap<K, V> {
        private static final long serialVersionUID = -8806743815996713206L;

        private final SortedMap<K, V> sm;

        UnmodifiableSortedMap(SortedMap<K, V> map) {
            super(map);
            sm = map;
        }

        public Comparator<? super K> comparator() {
            return sm.comparator();
        }

        public K firstKey() {
            return sm.firstKey();
        }

        public SortedMap<K, V> headMap(K before) {
            return new UnmodifiableSortedMap<K, V>(sm.headMap(before));
        }

        public K lastKey() {
            return sm.lastKey();
        }

        public SortedMap<K, V> subMap(K start, K end) {
            return new UnmodifiableSortedMap<K, V>(sm.subMap(start, end));
        }

        public SortedMap<K, V> tailMap(K after) {
            return new UnmodifiableSortedMap<K, V>(sm.tailMap(after));
        }
    }

    private static class UnmodifiableSortedSet<E> extends UnmodifiableSet<E>
            implements SortedSet<E> {
        private static final long serialVersionUID = -4929149591599911165L;

        private final SortedSet<E> ss;

        UnmodifiableSortedSet(SortedSet<E> set) {
            super(set);
            ss = set;
        }

        public Comparator<? super E> comparator() {
            return ss.comparator();
        }

        public E first() {
            return ss.first();
        }

        public SortedSet<E> headSet(E before) {
            return new UnmodifiableSortedSet<E>(ss.headSet(before));
        }

        public E last() {
            return ss.last();
        }

        public SortedSet<E> subSet(E start, E end) {
            return new UnmodifiableSortedSet<E>(ss.subSet(start, end));
        }

        public SortedSet<E> tailSet(E after) {
            return new UnmodifiableSortedSet<E>(ss.tailSet(after));
        }
    }

    private Collections() {
        /* empty */
    }

    /**
     * Performs a binary search for the specified element in the specified
     * sorted list. The list needs to be already sorted in natural sorting
     * order. Searching in an unsorted array has an undefined result. It's also
     * undefined which element is found if there are multiple occurrences of the
     * same element.
     * 
     * @param list
     *            the sorted list to search.
     * @param object
     *            the element to find.
     * @return the non-negative index of the element, or a negative index which
     *         is the {@code -index - 1} where the element would be inserted
     * @throws ClassCastException
     *             if an element in the List or the search element does not
     *             implement Comparable, or cannot be compared to each other.
     */
    @SuppressWarnings("unchecked")
    public static <T> int binarySearch(
            List<? extends java.lang.Comparable<? super T>> list, T object) {
        if (list == null) {
            throw new NullPointerException();
        }
        if (list.isEmpty()) {
            return -1;
        }

               
        if (!(list instanceof RandomAccess)) {
            ListIterator<? extends java.lang.Comparable<? super T>> it = list.listIterator();
            while (it.hasNext()) {
                int result;
                if ((result = -it.next().compareTo(object)) <= 0) {    
                    if (result == 0) {
                        return it.previousIndex();
                    }
                    return -it.previousIndex() - 1;
                }
            }
            return -list.size() - 1;
        }

        int low = 0, mid = list.size(), high = mid - 1, result = -1;
        while (low <= high) {
            mid = (low + high) >> 1;
            if ((result = -list.get(mid).compareTo(object)) > 0) {
                low = mid + 1;
            } else if (result == 0) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -mid - (result < 0 ? 1 : 2);
    }

    /**
     * Performs a binary search for the specified element in the specified
     * sorted list using the specified comparator. The list needs to be already
     * sorted according to the comparator passed. Searching in an unsorted array
     * has an undefined result. It's also undefined which element is found if
     * there are multiple occurrences of the same element.
     * 
     * @param <T> The element type
     * @param list
     *            the sorted List to search.
     * @param object
     *            the element to find.
     * @param comparator
     *            the comparator. If the comparator is {@code null} then the
     *            search uses the objects' natural ordering.
     * @return the non-negative index of the element, or a negative index which
     *         is the {@code -index - 1} where the element would be inserted.
     * @throws ClassCastException
     *             when an element in the list and the searched element cannot
     *             be compared to each other using the comparator.
     */
    @SuppressWarnings("unchecked")
    public static <T> int binarySearch(List<? extends T> list, T object,
            Comparator<? super T> comparator) {
        if (comparator == null) {
            return Collections.binarySearch(
                    (List<? extends java.lang.Comparable<? super T>>) list, object);
        }
        if (!(list instanceof RandomAccess)) {
            ListIterator<? extends T> it = list.listIterator();
            while (it.hasNext()) {
                int result;
                if ((result = -comparator.compare(it.next(), object)) <= 0) {
                    if (result == 0) {
                        return it.previousIndex();
                    }
                    return -it.previousIndex() - 1;
                }
            }
            return -list.size() - 1;
        }

        int low = 0, mid = list.size(), high = mid - 1, result = -1;
        while (low <= high) {
            mid = (low + high) >> 1;
            if ((result = -comparator.compare(list.get(mid),object)) > 0) {
                low = mid + 1;
            } else if (result == 0) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        return -mid - (result < 0 ? 1 : 2);
    }

    /**
     * Copies the elements from the source list to the destination list. At the
     * end both lists will have the same objects at the same index. If the
     * destination array is larger than the source list, the elements in the
     * destination list with {@code index >= source.size()} will be unchanged.
     * 
     * @param destination
     *            the list whose elements are set from the source list.
     * @param source
     *            the list with the elements to be copied into the destination.
     * @throws IndexOutOfBoundsException
     *             when the destination list is smaller than the source list.
     * @throws UnsupportedOperationException
     *             when replacing an element in the destination list is not
     *             supported.
     */
    public static <T> void copy(List<? super T> destination,
            List<? extends T> source) {
        if (destination.size() < source.size()) {
            // luni.38=Source size {0} does not fit into destination
            throw new IndexOutOfBoundsException("" + source.size() + " out of: " + destination.size());
        }
        Iterator<? extends T> srcIt = source.iterator();
        ListIterator<? super T> destIt = destination.listIterator();
        while (srcIt.hasNext()) {
            try {
                destIt.next();
            } catch (NoSuchElementException e) {
                // luni.38=Source size {0} does not fit into destination
                throw new IndexOutOfBoundsException("" + source.size());
            }
            destIt.set(srcIt.next());
        }
    }

    /**
     * Returns an {@code Enumeration} on the specified collection.
     * 
     * @param collection
     *            the collection to enumerate.
     * @return an Enumeration.
     */
    public static <T> java.util.Enumeration<T> enumeration(Collection<T> collection) {
        final Collection<T> c = collection;
        return new java.util.Enumeration<T>() {
            Iterator<T> it = c.iterator();

            public boolean hasMoreElements() {
                return it.hasNext();
            }

            public T nextElement() {
                return it.next();
            }
        };
    }

    /**
     * Fills the specified list with the specified element.
     * 
     * @param list
     *            the list to fill.
     * @param object
     *            the element to fill the list with.
     * @throws UnsupportedOperationException
     *             when replacing an element in the List is not supported.
     */
    public static <T> void fill(List<? super T> list, T object) {
        ListIterator<? super T> it = list.listIterator();
        while (it.hasNext()) {
            it.next();
            it.set(object);
        }
    }

    /**
     * Searches the specified collection for the maximum element.
     * 
     * @param collection
     *            the collection to search.
     * @return the maximum element in the Collection.
     * @throws ClassCastException
     *             when an element in the collection does not implement
     *             {@code Comparable} or elements cannot be compared to each
     *             other.
     */
    public static <T extends Object & java.lang.Comparable<? super T>> T max(
            Collection<? extends T> collection) {
        Iterator<? extends T> it = collection.iterator();
        T max = it.next();
        while (it.hasNext()) {
            T next = it.next();
            if (max.compareTo(next) < 0) {
                max = next;
            }
        }
        return max;
    }

    /**
     * Searches the specified collection for the maximum element using the
     * specified comparator.
     * 
     * @param collection
     *            the collection to search.
     * @param comparator
     *            the comparator.
     * @return the maximum element in the Collection.
     * @throws ClassCastException
     *             when elements in the collection cannot be compared to each
     *             other using the {@code Comparator}.
     */
    public static <T> T max(Collection<? extends T> collection,
            Comparator<? super T> comparator) {
        if (comparator == null) {
            @SuppressWarnings("unchecked") // null comparator? T is comparable
            T result = (T) max((Collection<java.lang.Comparable>) collection);
            return result;
        }

        Iterator<? extends T> it = collection.iterator();
        T max = it.next();
        while (it.hasNext()) {
            T next = it.next();
            if (comparator.compare(max, next) < 0) {
                max = next;
            }
        }
        return max;
    }

    /**
     * Searches the specified collection for the minimum element.
     * 
     * @param collection
     *            the collection to search.
     * @return the minimum element in the collection.
     * @throws ClassCastException
     *             when an element in the collection does not implement
     *             {@code Comparable} or elements cannot be compared to each
     *             other.
     */
    public static <T extends Object & java.lang.Comparable<? super T>> T min(
            Collection<? extends T> collection) {
        Iterator<? extends T> it = collection.iterator();
        T min = it.next();
        while (it.hasNext()) {
            T next = it.next();
            if (min.compareTo(next) > 0) {
                min = next;
            }
        }
        return min;
    }

    /**
     * Searches the specified collection for the minimum element using the
     * specified comparator.
     * 
     * @param collection
     *            the collection to search.
     * @param comparator
     *            the comparator.
     * @return the minimum element in the collection.
     * @throws ClassCastException
     *             when elements in the collection cannot be compared to each
     *             other using the {@code Comparator}.
     */
    public static <T> T min(Collection<? extends T> collection,
            Comparator<? super T> comparator) {
        if (comparator == null) {
            @SuppressWarnings("unchecked") // null comparator? T is comparable
            T result = (T) min((Collection<java.lang.Comparable>) collection);
            return result;
        }

        Iterator<? extends T> it = collection.iterator();
        T min = it.next();
        while (it.hasNext()) {
            T next = it.next();
            if (comparator.compare(min, next) > 0) {
                min = next;
            }
        }
        return min;
    }

    /**
     * Returns a list containing the specified number of the specified element.
     * The list cannot be modified. The list is serializable.
     * 
     * @param length
     *            the size of the returned list.
     * @param object
     *            the element to be added {@code length} times to a list.
     * @return a list containing {@code length} copies of the element.
     * @throws IllegalArgumentException
     *             when {@code length < 0}.
     */
    public static <T> List<T> nCopies(final int length, T object) {
        return new CopiesList<T>(length, object);
    }

    /**
     * Modifies the specified {@code List} by reversing the order of the
     * elements.
     * 
     * @param list
     *            the list to reverse.
     * @throws UnsupportedOperationException
     *             when replacing an element in the List is not supported.
     */
    @SuppressWarnings("unchecked")
    public static void reverse(List<?> list) {
        int size = list.size();
        ListIterator<Object> front = (ListIterator<Object>) list.listIterator();
        ListIterator<Object> back = (ListIterator<Object>) list
                .listIterator(size);
        for (int i = 0; i < size / 2; i++) {
            Object frontNext = front.next();
            Object backPrev = back.previous();
            front.set(backPrev);
            back.set(frontNext);
        }
    }

    /**
     * A comparator which reverses the natural order of the elements. The
     * {@code Comparator} that's returned is {@link Serializable}.
     *
     * @return a {@code Comparator} instance.
     * @see Comparator
     * @see Comparable
     * @see Serializable
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> reverseOrder() {
        return (Comparator) ReverseComparator.INSTANCE;
    }

    /**
     * Returns a {@link Comparator} that reverses the order of the
     * {@code Comparator} passed. If the {@code Comparator} passed is
     * {@code null}, then this method is equivalent to {@link #reverseOrder()}.
     * <p>
     * The {@code Comparator} that's returned is {@link Serializable} if the
     * {@code Comparator} passed is serializable or {@code null}.
     *
     * @param c
     *            the {@code Comparator} to reverse or {@code null}.
     * @return a {@code Comparator} instance.
     * @see Comparator
     * @since 1.5
     */
    public static <T> Comparator<T> reverseOrder(Comparator<T> c) {
        if (c == null) {
            return reverseOrder();
        }
        if (c instanceof ReverseComparatorWithComparator) {
            return ((ReverseComparatorWithComparator<T>) c).comparator;
        }
        return new ReverseComparatorWithComparator<T>(c);
    }

    /**
     * Moves every element of the list to a random new position in the list.
     * 
     * @param list
     *            the List to shuffle.
     * 
     * @throws UnsupportedOperationException
     *             when replacing an element in the List is not supported.
     */
    public static void shuffle(List<?> list) {
        shuffle(list, new java.util.Random());
    }

    /**
     * Moves every element of the list to a random new position in the list
     * using the specified random number generator.
     * 
     * @param list
     *            the list to shuffle.
     * @param random
     *            the random number generator.
     * @throws UnsupportedOperationException
     *             when replacing an element in the list is not supported.
     */
    public static void shuffle(List<?> list, java.util.Random random) {
        @SuppressWarnings("unchecked") // we won't put foreign objects in
        final List<Object> objectList = (List<Object>) list;

        if (list instanceof RandomAccess) {
            for (int i = objectList.size() - 1; i > 0; i--) {
                int index = random.nextInt(i + 1);
                objectList.set(index, objectList.set(i, objectList.get(index)));
            }
        } else {
            Object[] array = objectList.toArray();
            for (int i = array.length - 1; i > 0; i--) {
                int index = random.nextInt(i + 1);
                Object temp = array[i];
                array[i] = array[index];
                array[index] = temp;
            }

            int i = 0;
            ListIterator<Object> it = objectList.listIterator();
            while (it.hasNext()) {
                it.next();
                it.set(array[i++]);
            }
        }
    }

    /**
     * Returns a set containing the specified element. The set cannot be
     * modified. The set is serializable.
     * 
     * @param object
     *            the element.
     * @return a set containing the element.
     */
    public static <E> Set<E> singleton(E object) {
        return new SingletonSet<E>(object);
    }

    /**
     * Returns a list containing the specified element. The list cannot be
     * modified. The list is serializable.
     * 
     * @param object
     *            the element.
     * @return a list containing the element.
     */
    public static <E> List<E> singletonList(E object) {
        return new SingletonList<E>(object);
    }

    /**
     * Returns a Map containing the specified key and value. The map cannot be
     * modified. The map is serializable.
     * 
     * @param key
     *            the key.
     * @param value
     *            the value.
     * @return a Map containing the key and value.
     */
    public static <K, V> Map<K, V> singletonMap(K key, V value) {
        return new SingletonMap<K, V>(key, value);
    }

    /**
     * Sorts the specified list in ascending natural order. The algorithm is
     * stable which means equal elements don't get reordered.
     * 
     * @param list
     *            the list to be sorted.
     * @throws ClassCastException
     *             when an element in the List does not implement Comparable or
     *             elements cannot be compared to each other.
     */
    @SuppressWarnings("unchecked")
    public static <T extends java.lang.Comparable<? super T>> void sort(List<T> list) {
        Object[] array = list.toArray();
        Arrays.sort(array);
        int i = 0;
        ListIterator<T> it = list.listIterator();
        while (it.hasNext()) {
            it.next();
            it.set((T) array[i++]);
        }
    }

    /**
     * Sorts the specified list using the specified comparator. The algorithm is
     * stable which means equal elements don't get reordered.
     * 
     * @param list
     *            the list to be sorted.
     * @param comparator
     *            the comparator.
     * @throws ClassCastException
     *             when elements in the list cannot be compared to each other
     *             using the comparator.
     */
    @SuppressWarnings("unchecked")
    public static <T> void sort(List<T> list, Comparator<? super T> comparator) {
        T[] array = list.toArray((T[]) new Object[list.size()]);
        Arrays.sort(array, comparator);
        int i = 0;
        ListIterator<T> it = list.listIterator();
        while (it.hasNext()) {
            it.next();
            it.set(array[i++]);
        }
    }

    /**
     * Swaps the elements of list {@code list} at indices {@code index1} and
     * {@code index2}.
     * 
     * @param list
     *            the list to manipulate.
     * @param index1
     *            position of the first element to swap with the element in
     *            index2.
     * @param index2
     *            position of the other element.
     * 
     * @throws IndexOutOfBoundsException
     *             if index1 or index2 is out of range of this list.
     * @since 1.4
     */
    @SuppressWarnings("unchecked")
    public static void swap(List<?> list, int index1, int index2) {
        if (list == null) {
            throw new NullPointerException();
        }
        final int size = list.size();
        if (index1 < 0 || index1 >= size || index2 < 0 || index2 >= size) {
            throw new IndexOutOfBoundsException();
        }
        if (index1 == index2) {
            return;
        }
        List<Object> rawList = (List<Object>) list;
        rawList.set(index2, rawList.set(index1, rawList.get(index2)));
    }

    /**
     * Replaces all occurrences of Object {@code obj} in {@code list} with
     * {@code newObj}. If the {@code obj} is {@code null}, then all
     * occurrences of {@code null} are replaced with {@code newObj}.
     * 
     * @param list
     *            the list to modify.
     * @param obj
     *            the object to find and replace occurrences of.
     * @param obj2
     *            the object to replace all occurrences of {@code obj} in
     *            {@code list}.
     * @return true, if at least one occurrence of {@code obj} has been found in
     *         {@code list}.
     * @throws UnsupportedOperationException
     *             if the list does not support setting elements.
     */
    public static <T> boolean replaceAll(List<T> list, T obj, T obj2) {
        int index;
        boolean found = false;

        while ((index = list.indexOf(obj)) > -1) {
            found = true;
            list.set(index, obj2);
        }
        return found;
    }

    /**
     * Rotates the elements in {@code list} by the distance {@code dist}
     * <p>
     * e.g. for a given list with elements [1, 2, 3, 4, 5, 6, 7, 8, 9, 0],
     * calling rotate(list, 3) or rotate(list, -7) would modify the list to look
     * like this: [8, 9, 0, 1, 2, 3, 4, 5, 6, 7]
     *
     * @param lst
     *            the list whose elements are to be rotated.
     * @param dist
     *            is the distance the list is rotated. This can be any valid
     *            integer. Negative values rotate the list backwards.
     */
    @SuppressWarnings("unchecked")
    public static void rotate(List<?> lst, int dist) {
        List<Object> list = (List<Object>) lst;
        int size = list.size();

        // Can't sensibly rotate an empty collection
        if (size == 0) {
            return;
        }

        // normalize the distance
        int normdist;
        if (dist > 0) {
            normdist = dist % size;
        } else {
            normdist = size - ((dist % size) * (-1));
        }

        if (normdist == 0 || normdist == size) {
            return;
        }

        if (list instanceof RandomAccess) {
            // make sure each element gets juggled
            // with the element in the position it is supposed to go to
            Object temp = list.get(0);
            int index = 0, beginIndex = 0;
            for (int i = 0; i < size; i++) {
                index = (index + normdist) % size;
                temp = list.set(index, temp);
                if (index == beginIndex) {
                    index = ++beginIndex;
                    temp = list.get(beginIndex);
                }
            }
        } else {
            int divideIndex = (size - normdist) % size;
            List<Object> sublist1 = list.subList(0, divideIndex);
            List<Object> sublist2 = list.subList(divideIndex, size);
            reverse(sublist1);
            reverse(sublist2);
            reverse(list);
        }
    }

    /**
     * Searches the {@code list} for {@code sublist} and returns the beginning
     * index of the first occurrence.
     * <p>
     * -1 is returned if the {@code sublist} does not exist in {@code list}.
     * 
     * @param list
     *            the List to search {@code sublist} in.
     * @param sublist
     *            the List to search in {@code list}.
     * @return the beginning index of the first occurrence of {@code sublist} in
     *         {@code list}, or -1.
     */
    public static int indexOfSubList(List<?> list, List<?> sublist) {
        int size = list.size();
        int sublistSize = sublist.size();

        if (sublistSize > size) {
            return -1;
        }

        if (sublistSize == 0) {
            return 0;
        }

        // find the first element of sublist in the list to get a head start
        Object firstObj = sublist.get(0);
        int index = list.indexOf(firstObj);
        if (index == -1) {
            return -1;
        }

        while (index < size && (size - index >= sublistSize)) {
            ListIterator<?> listIt = list.listIterator(index);

            if ((firstObj == null) ? listIt.next() == null : firstObj
                    .equals(listIt.next())) {

                // iterate through the elements in sublist to see
                // if they are included in the same order in the list
                ListIterator<?> sublistIt = sublist.listIterator(1);
                boolean difFound = false;
                while (sublistIt.hasNext()) {
                    Object element = sublistIt.next();
                    if (!listIt.hasNext()) {
                        return -1;
                    }
                    if ((element == null) ? listIt.next() != null : !element
                            .equals(listIt.next())) {
                        difFound = true;
                        break;
                    }
                }
                // All elements of sublist are found in main list
                // starting from index.
                if (!difFound) {
                    return index;
                }
            }
            // This was not the sequence we were looking for,
            // continue search for the firstObj in main list
            // at the position after index.
            index++;
        }
        return -1;
    }

    /**
     * Searches the {@code list} for {@code sublist} and returns the beginning
     * index of the last occurrence.
     * <p>
     * -1 is returned if the {@code sublist} does not exist in {@code list}.
     * 
     * @param list
     *            the list to search {@code sublist} in.
     * @param sublist
     *            the list to search in {@code list}.
     * @return the beginning index of the last occurrence of {@code sublist} in
     *         {@code list}, or -1.
     */
    public static int lastIndexOfSubList(List<?> list, List<?> sublist) {
        int sublistSize = sublist.size();
        int size = list.size();

        if (sublistSize > size) {
            return -1;
        }

        if (sublistSize == 0) {
            return size;
        }

        // find the last element of sublist in the list to get a head start
        Object lastObj = sublist.get(sublistSize - 1);
        int index = list.lastIndexOf(lastObj);

        while ((index > -1) && (index + 1 >= sublistSize)) {
            ListIterator<?> listIt = list.listIterator(index + 1);

            if ((lastObj == null) ? listIt.previous() == null : lastObj
                    .equals(listIt.previous())) {
                // iterate through the elements in sublist to see
                // if they are included in the same order in the list
                ListIterator<?> sublistIt = sublist
                        .listIterator(sublistSize - 1);
                boolean difFound = false;
                while (sublistIt.hasPrevious()) {
                    Object element = sublistIt.previous();
                    if (!listIt.hasPrevious()) {
                        return -1;
                    }
                    if ((element == null) ? listIt.previous() != null
                            : !element.equals(listIt.previous())) {
                        difFound = true;
                        break;
                    }
                }
                // All elements of sublist are found in main list
                // starting from listIt.nextIndex().
                if (!difFound) {
                    return listIt.nextIndex();
                }
            }
            // This was not the sequence we were looking for,
            // continue search for the lastObj in main list
            // at the position before index.
            index--;
        }
        return -1;
    }

    /**
     * Returns an {@code ArrayList} with all the elements in the {@code
     * enumeration}. The elements in the returned {@code ArrayList} are in the
     * same order as in the {@code enumeration}.
     * 
     * @param enumeration
     *            the source {@link Enumeration}.
     * @return an {@code ArrayList} from {@code enumeration}.
     */
    public static <T> ArrayList<T> list(java.util.Enumeration<T> enumeration) {
        return null;
    }

    /**
     * Returns a wrapper on the specified collection which synchronizes all
     * access to the collection.
     * 
     * @param collection
     *            the Collection to wrap in a synchronized collection.
     * @return a synchronized Collection.
     */
    public static <T> Collection<T> synchronizedCollection(
            Collection<T> collection) {
        if (collection == null) {
            throw new NullPointerException();
        }
        return new SynchronizedCollection<T>(collection);
    }

    /**
     * Returns a wrapper on the specified List which synchronizes all access to
     * the List.
     * 
     * @param list
     *            the List to wrap in a synchronized list.
     * @return a synchronized List.
     */
    public static <T> List<T> synchronizedList(List<T> list) {
        if (list == null) {
            throw new NullPointerException();
        }
        if (list instanceof RandomAccess) {
            return new SynchronizedRandomAccessList<T>(list);
        }
        return new SynchronizedList<T>(list);
    }

    /**
     * Returns a wrapper on the specified map which synchronizes all access to
     * the map.
     * 
     * @param map
     *            the map to wrap in a synchronized map.
     * @return a synchronized Map.
     */
    public static <K, V> Map<K, V> synchronizedMap(Map<K, V> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        return new SynchronizedMap<K, V>(map);
    }

    /**
     * Returns a wrapper on the specified set which synchronizes all access to
     * the set.
     * 
     * @param set
     *            the set to wrap in a synchronized set.
     * @return a synchronized set.
     */
    public static <E> Set<E> synchronizedSet(Set<E> set) {
        if (set == null) {
            throw new NullPointerException();
        }
        return new SynchronizedSet<E>(set);
    }

    /**
     * Returns a wrapper on the specified sorted map which synchronizes all
     * access to the sorted map.
     * 
     * @param map
     *            the sorted map to wrap in a synchronized sorted map.
     * @return a synchronized sorted map.
     */
    public static <K, V> SortedMap<K, V> synchronizedSortedMap(
            SortedMap<K, V> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        return new SynchronizedSortedMap<K, V>(map);
    }

    /**
     * Returns a wrapper on the specified sorted set which synchronizes all
     * access to the sorted set.
     * 
     * @param set
     *            the sorted set to wrap in a synchronized sorted set.
     * @return a synchronized sorted set.
     */
    public static <E> SortedSet<E> synchronizedSortedSet(SortedSet<E> set) {
        if (set == null) {
            throw new NullPointerException();
        }
        return new SynchronizedSortedSet<E>(set);
    }

    /**
     * Returns a wrapper on the specified collection which throws an
     * {@code UnsupportedOperationException} whenever an attempt is made to
     * modify the collection.
     * 
     * @param collection
     *            the collection to wrap in an unmodifiable collection.
     * @return an unmodifiable collection.
     */
    @SuppressWarnings("unchecked")
    public static <E> Collection<E> unmodifiableCollection(
            Collection<? extends E> collection) {
        if (collection == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableCollection<E>((Collection<E>) collection);
    }

    /**
     * Returns a wrapper on the specified list which throws an
     * {@code UnsupportedOperationException} whenever an attempt is made to
     * modify the list.
     * 
     * @param list
     *            the list to wrap in an unmodifiable list.
     * @return an unmodifiable List.
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> unmodifiableList(List<? extends E> list) {
        if (list == null) {
            throw new NullPointerException();
        }
        if (list instanceof RandomAccess) {
            return new UnmodifiableRandomAccessList<E>((List<E>) list);
        }
        return new UnmodifiableList<E>((List<E>) list);
    }

    /**
     * Returns a wrapper on the specified map which throws an
     * {@code UnsupportedOperationException} whenever an attempt is made to
     * modify the map.
     * 
     * @param map
     *            the map to wrap in an unmodifiable map.
     * @return a unmodifiable map.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> unmodifiableMap(
            Map<? extends K, ? extends V> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableMap<K, V>((Map<K, V>) map);
    }

    /**
     * Returns a wrapper on the specified set which throws an
     * {@code UnsupportedOperationException} whenever an attempt is made to
     * modify the set.
     * 
     * @param set
     *            the set to wrap in an unmodifiable set.
     * @return a unmodifiable set
     */
    @SuppressWarnings("unchecked")
    public static <E> Set<E> unmodifiableSet(Set<? extends E> set) {
        if (set == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableSet<E>((Set<E>) set);
    }

    /**
     * Returns a wrapper on the specified sorted map which throws an
     * {@code UnsupportedOperationException} whenever an attempt is made to
     * modify the sorted map.
     * 
     * @param map
     *            the sorted map to wrap in an unmodifiable sorted map.
     * @return a unmodifiable sorted map
     */
    @SuppressWarnings("unchecked")
    public static <K, V> SortedMap<K, V> unmodifiableSortedMap(
            SortedMap<K, ? extends V> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableSortedMap<K, V>((SortedMap<K, V>) map);
    }

    /**
     * Returns a wrapper on the specified sorted set which throws an
     * {@code UnsupportedOperationException} whenever an attempt is made to
     * modify the sorted set.
     * 
     * @param set
     *            the sorted set to wrap in an unmodifiable sorted set.
     * @return a unmodifiable sorted set.
     */
    public static <E> SortedSet<E> unmodifiableSortedSet(SortedSet<E> set) {
        if (set == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableSortedSet<E>(set);
    }

    /**
     * Returns the number of elements in the {@code Collection} that match the
     * {@code Object} passed. If the {@code Object} is {@code null}, then the
     * number of {@code null} elements is returned.
     * 
     * @param c
     *            the {@code Collection} to search.
     * @param o
     *            the {@code Object} to search for.
     * @return the number of matching elements.
     * @throws NullPointerException
     *             if the {@code Collection} parameter is {@code null}.
     * @since 1.5
     */
    public static int frequency(Collection<?> c, Object o) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c.isEmpty()) {
            return 0;
        }
        int result = 0;
        Iterator<?> itr = c.iterator();
        while (itr.hasNext()) {
            Object e = itr.next();
            if (o == null ? e == null : o.equals(e)) {
                result++;
            }
        }
        return result;
    }

    /**
     * Returns a type-safe empty, immutable {@link List}.
     * 
     * @return an empty {@link List}.
     * @since 1.5
     * @see #EMPTY_LIST
     */
    @SuppressWarnings("unchecked")
    public static final <T> List<T> emptyList() {
        return EMPTY_LIST;
    }

    /**
     * Returns a type-safe empty, immutable {@link Set}.
     * 
     * @return an empty {@link Set}.
     * @since 1.5
     * @see #EMPTY_SET
     */
    @SuppressWarnings("unchecked")
    public static final <T> Set<T> emptySet() {
        return EMPTY_SET;
    }

    /**
     * Returns a type-safe empty, immutable {@link Map}.
     * 
     * @return an empty {@link Map}.
     * @since 1.5
     * @see #EMPTY_MAP
     */
    @SuppressWarnings("unchecked")
    public static final <K, V> Map<K, V> emptyMap() {
        return EMPTY_MAP;
    }

    /**
     * Returns a dynamically typesafe view of the specified collection. Trying
     * to insert an element of the wrong type into this collection throws a
     * {@code ClassCastException}. At creation time the types in {@code c} are
     * not checked for correct type.
     * 
     * @param c
     *            the collection to be wrapped in a typesafe collection.
     * @param type
     *            the type of the elements permitted to insert.
     * @return a typesafe collection.
     */
    public static <E> Collection<E> checkedCollection(Collection<E> c,
            Class<E> type) {
        return new CheckedCollection<E>(c, type);
    }


    /**
     * Adds all the specified elements to the specified collection.
     * 
     * @param c
     *            the collection the elements are to be inserted into.
     * @param a
     *            the elements to insert.
     * @return true if the collection changed during insertion.
     * @throws UnsupportedOperationException
     *             when the method is not supported.
     * @throws NullPointerException
     *             when {@code c} or {@code a} is {@code null}, or {@code a}
     *             contains one or more {@code null} elements and {@code c}
     *             doesn't support {@code null} elements.
     * @throws IllegalArgumentException
     *             if at least one of the elements can't be inserted into the
     *             collection.
     */
    public static <T> boolean addAll(Collection<? super T> c, T... a) {
        boolean modified = false;
        for (int i = 0; i < a.length; i++) {
            modified |= c.add(a[i]);
        }
        return modified;
    }

    /**
     * Returns whether the specified collections have no elements in common.
     * 
     * @param c1
     *            the first collection.
     * @param c2
     *            the second collection.
     * @return {@code true} if the collections have no elements in common,
     *         {@code false} otherwise.
     * @throws NullPointerException
     *             if one of the collections is {@code null}.
     */
    public static boolean disjoint(Collection<?> c1, Collection<?> c2) {
        if ((c1 instanceof Set) && !(c2 instanceof Set)
                || (c2.size()) > c1.size()) {
            Collection<?> tmp = c1;
            c1 = c2;
            c2 = tmp;
        }
        Iterator<?> it = c1.iterator();
        while (it.hasNext()) {
            if (c2.contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if specified object is instance of specified class. Used for a
     * dynamically typesafe view of the collections.
     * 
     * @param obj -
     *            object is to be checked
     * @param type -
     *            class of object that should be
     * @return specified object
     */
    static <E> E checkType(E obj, Class<? extends E> type) {
        if (obj != null && !type.isInstance(obj)) {
            // luni.05=Attempt to insert {0} element into collection with
            // element type {1}
            throw new IndexOutOfBoundsException("" + obj.getClass().getName() + " type: " + type);
        }
        return obj;
    }
    
    /**
     * Answers a set backed by a map. And the map must be empty when this method
     * is called.
     * 
     * @param <E>
     *            type of elements in set
     * @param map
     *            the backing map
     * @return the set from the map
     * @throws IllegalArgumentException
     *             if the map is not empty
     * @since 1.6
     */
    public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
        if (map.isEmpty()) {
            return new SetFromMap<E>(map);
        }
        throw new IllegalArgumentException();
    }

    /**
     * Answers a LIFO Queue as a view of a Deque. Methods in the returned Queue
     * need to be re-written to implement the LIFO feature.
     * 
     * @param <T>
     *            type of elements
     * @param deque
     *            the Deque
     * @return the LIFO Queue
     * @since 1.6
     */
    public static <T> Queue<T> asLifoQueue(Deque<T> deque) {
        return new AsLIFOQueue<T>(deque);
    }

    private static class SetFromMap<E> extends AbstractSet<E> {
        private static final long serialVersionUID = 2454657854757543876L;

        // must named as it, to pass serialization compatibility test.
        private Map<E, Boolean> m;

        private transient Set<E> backingSet;

        SetFromMap(final Map<E, Boolean> map) {
            super();
            m = map;
            backingSet = map.keySet();
        }

        @Override
        public boolean equals(Object object) {
            return backingSet.equals(object);
        }

        @Override
        public int hashCode() {
            return backingSet.hashCode();
        }

        @Override
        public boolean add(E object) {
            return m.put(object, Boolean.TRUE) == null;
        }

        @Override
        public void clear() {
            m.clear();
        }

        @Override
        public String toString() {
            return backingSet.toString();
        }

        @Override
        public boolean contains(Object object) {
            return backingSet.contains(object);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return backingSet.containsAll(collection);
        }

        @Override
        public boolean isEmpty() {
            return m.isEmpty();
        }

        @Override
        public boolean remove(Object object) {
            return m.remove(object) != null;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return backingSet.retainAll(collection);
        }

        @Override
        public Object[] toArray() {
            return backingSet.toArray();
        }

        @Override
        public <T> T[] toArray(T[] contents) {
            return backingSet.toArray(contents);
        }

        @Override
        public Iterator<E> iterator() {
            return backingSet.iterator();
        }

        @Override
        public int size() {
            return m.size();
        }
    }

    private static class AsLIFOQueue<E> extends AbstractQueue<E> {
        // must named as it, to pass serialization compatibility test.
        private final Deque<E> q;

        AsLIFOQueue(final Deque<E> deque) {
            super();
            this.q = deque;
        }

        @Override
        public Iterator<E> iterator() {
            return q.iterator();
        }

        @Override
        public int size() {
            return q.size();
        }

        public boolean offer(E o) {
            return q.offerFirst(o);
        }

        public E peek() {
            return q.peekFirst();
        }

        public E poll() {
            return q.pollFirst();
        }

        @Override
        public boolean add(E o) {
            q.push(o);
            return true;
        }

        @Override
        public void clear() {
            q.clear();
        }

        @Override
        public E element() {
            return q.getFirst();
        }

        @Override
        public E remove() {
            return q.pop();
        }

        @Override
        public boolean contains(Object object) {
            return q.contains(object);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return q.containsAll(collection);
        }

        @Override
        public boolean isEmpty() {
            return q.isEmpty();
        }

        @Override
        public boolean remove(Object object) {
            return q.remove(object);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return q.removeAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return q.retainAll(collection);
        }

        @Override
        public Object[] toArray() {
            return q.toArray();
        }

        @Override
        public <T> T[] toArray(T[] contents) {
            return q.toArray(contents);
        }

        @Override
        public String toString() {
            return q.toString();
        }
    }
    
    /**
     * Class represents a dynamically typesafe view of the specified collection.
     */
    private static class CheckedCollection<E> implements Collection<E> {

        private static final long serialVersionUID = 1578914078182001775L;

        Collection<E> c;

        Class<E> type;

        /**
         * Constructs a dynamically typesafe view of the specified collection.
         * 
         * @param c -
         *            the collection for which an unmodifiable view is to be
         *            constructed.
         */
        public CheckedCollection(Collection<E> c, Class<E> type) {
            if (c == null || type == null) {
                throw new NullPointerException();
            }
            this.c = c;
            this.type = type;
        }

        /**
         * @see java.util.Collection#size()
         */
        public int size() {
            return c.size();
        }

        /**
         * @see java.util.Collection#isEmpty()
         */
        public boolean isEmpty() {
            return c.isEmpty();
        }

        /**
         * @see java.util.Collection#contains(Object)
         */
        public boolean contains(Object obj) {
            return c.contains(obj);
        }

        /**
         * @see java.util.Collection#iterator()
         */
        public Iterator<E> iterator() {
            Iterator<E> i = c.iterator();
            if (i instanceof ListIterator) {
                i = new CheckedListIterator<E>((ListIterator<E>) i, type);
            }
            return i;
        }

        /**
         * @see java.util.Collection#toArray()
         */
        public Object[] toArray() {
            return c.toArray();
        }

        /**
         * @see java.util.Collection#toArray(Object[])
         */
        public <T> T[] toArray(T[] arr) {
            return c.toArray(arr);
        }

        /**
         * @see java.util.Collection#add(Object)
         */
        public boolean add(E obj) {
            return c.add(checkType(obj, type));
        }

        /**
         * @see java.util.Collection#remove(Object)
         */
        public boolean remove(Object obj) {
            return c.remove(obj);
        }

        /**
         * @see java.util.Collection#containsAll(Collection)
         */
        public boolean containsAll(Collection<?> c1) {
            return c.containsAll(c1);
        }

        /**
         * @see java.util.Collection#addAll(Collection)
         */
        @SuppressWarnings("unchecked")
        public boolean addAll(Collection<? extends E> c1) {
            Object[] array = c1.toArray();
            for (Object o : array) {
                checkType(o, type);
            }
            return c.addAll((List<E>) Arrays.asList(array));
        }

        /**
         * @see java.util.Collection#removeAll(Collection)
         */
        public boolean removeAll(Collection<?> c1) {
            return c.removeAll(c1);
        }

        /**
         * @see java.util.Collection#retainAll(Collection)
         */
        public boolean retainAll(Collection<?> c1) {
            return c.retainAll(c1);
        }

        /**
         * @see java.util.Collection#clear()
         */
        public void clear() {
            c.clear();
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return c.toString();
        }
    }

    /**
     * Class represents a dynamically typesafe view of the specified
     * ListIterator.
     */
    private static class CheckedListIterator<E> implements ListIterator<E> {

        private ListIterator<E> i;

        private Class<E> type;

        /**
         * Constructs a dynamically typesafe view of the specified ListIterator.
         * 
         * @param i -
         *            the listIterator for which a dynamically typesafe view to
         *            be constructed.
         */
        public CheckedListIterator(ListIterator<E> i, Class<E> type) {
            this.i = i;
            this.type = type;
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return i.hasNext();
        }

        /**
         * @see java.util.Iterator#next()
         */
        public E next() {
            return i.next();
        }

        /**
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            i.remove();
        }

        /**
         * @see java.util.ListIterator#hasPrevious()
         */
        public boolean hasPrevious() {
            return i.hasPrevious();
        }

        /**
         * @see java.util.ListIterator#previous()
         */
        public E previous() {
            return i.previous();
        }

        /**
         * @see java.util.ListIterator#nextIndex()
         */
        public int nextIndex() {
            return i.nextIndex();
        }

        /**
         * @see java.util.ListIterator#previousIndex()
         */
        public int previousIndex() {
            return i.previousIndex();
        }

        /**
         * @see java.util.ListIterator#set(Object)
         */
        public void set(E obj) {
            i.set(checkType(obj, type));
        }

        /**
         * @see java.util.ListIterator#add(Object)
         */
        public void add(E obj) {
            i.add(checkType(obj, type));
        }
    }

}
