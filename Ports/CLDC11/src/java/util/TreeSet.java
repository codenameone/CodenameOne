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

/// TreeSet is an implementation of SortedSet. All optional operations (adding
/// and removing) are supported. The elements can be any objects which are
/// comparable to each other either using their natural order or a specified
/// Comparator.
///
/// #### Since
///
/// 1.2
public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E> {

    /// Keys are this set's elements. Values are always Boolean.TRUE
    private transient NavigableMap<E, Object> backingMap;

    private transient NavigableSet<E> descendingSet;

    TreeSet(NavigableMap<E, Object> map) {
        backingMap = map;
    }

    /// Constructs a new empty instance of `TreeSet` which uses natural
    /// ordering.
    public TreeSet() {
        backingMap = new TreeMap<E, Object>();
    }

    /// Constructs a new instance of `TreeSet` which uses natural ordering
    /// and containing the unique elements in the specified collection.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of elements to add.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// when an element in the collection does not implement the
    /// Comparable interface, or the elements in the collection
    /// cannot be compared.
    public TreeSet(Collection<? extends E> collection) {
        this();
        addAll(collection);
    }

    /// Constructs a new empty instance of `TreeSet` which uses the
    /// specified comparator.
    ///
    /// #### Parameters
    ///
    /// - `comparator`: the comparator to use.
    public TreeSet(Comparator<? super E> comparator) {
        backingMap = new TreeMap<E, Object>(comparator);
    }

    /// Constructs a new instance of `TreeSet` containing the elements of
    /// the specified SortedSet and using the same Comparator.
    ///
    /// #### Parameters
    ///
    /// - `set`: the SortedSet of elements to add.
    public TreeSet(SortedSet<E> set) {
        this(set.comparator());
        Iterator<E> it = set.iterator();
        while (it.hasNext()) {
            add(it.next());
        }
    }

    /// Adds the specified object to this `TreeSet`.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to add.
    ///
    /// #### Returns
    ///
    /// @return `true` when this `TreeSet` did not already contain
    /// the object, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// when the object cannot be compared with the elements in this
    /// `TreeSet`.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// when the object is null and the comparator cannot handle
    /// null.
    @Override
    public boolean add(E object) {
        return backingMap.put(object, Boolean.TRUE) == null;
    }

    /// Adds the objects in the specified collection to this `TreeSet`.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects to add.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `TreeSet` was modified, `false`
    /// otherwise.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// when an object in the collection cannot be compared with the
    /// elements in this `TreeSet`.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// when an object in the collection is null and the comparator
    /// cannot handle null.
    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return super.addAll(collection);
    }

    /// Removes all elements from this `TreeSet`, leaving it empty.
    ///
    /// #### See also
    ///
    /// - #isEmpty
    ///
    /// - #size
    @Override
    public void clear() {
        backingMap.clear();
    }

    /// Returns the comparator used to compare elements in this `TreeSet`.
    ///
    /// #### Returns
    ///
    /// a Comparator or null if the natural ordering is used
    public Comparator<? super E> comparator() {
        return backingMap.comparator();
    }

    /// Searches this `TreeSet` for the specified object.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to search for.
    ///
    /// #### Returns
    ///
    /// @return `true` if `object` is an element of this
    /// `TreeSet`, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// when the object cannot be compared with the elements in this
    /// `TreeSet`.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// when the object is null and the comparator cannot handle
    /// null.
    @Override
    public boolean contains(Object object) {
        return backingMap.containsKey(object);
    }

    /// Returns true if this `TreeSet` has no element, otherwise false.
    ///
    /// #### Returns
    ///
    /// true if this `TreeSet` has no element.
    ///
    /// #### See also
    ///
    /// - #size
    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    /// Returns an Iterator on the elements of this `TreeSet`.
    ///
    /// #### Returns
    ///
    /// an Iterator on the elements of this `TreeSet`.
    ///
    /// #### See also
    ///
    /// - Iterator
    @Override
    public Iterator<E> iterator() {
        return backingMap.keySet().iterator();
    }

    /// {@inheritDoc}
    ///
    /// #### Since
    ///
    /// 1.6
    ///
    /// #### See also
    ///
    /// - java.util.NavigableSet#descendingIterator()
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    /// Removes an occurrence of the specified object from this `TreeSet`.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to remove.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `TreeSet` was modified, `false`
    /// otherwise.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// when the object cannot be compared with the elements in this
    /// `TreeSet`.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// when the object is null and the comparator cannot handle
    /// null.
    @Override
    public boolean remove(Object object) {
        return backingMap.remove(object) != null;
    }

    /// Returns the number of elements in this `TreeSet`.
    ///
    /// #### Returns
    ///
    /// the number of elements in this `TreeSet`.
    @Override
    public int size() {
        return backingMap.size();
    }

    /// Answers the first element in this TreeSet.
    ///
    /// #### Returns
    ///
    /// the first element
    ///
    /// #### Throws
    ///
    /// - `NoSuchElementException`: when this TreeSet is empty
    public E first() {
        return backingMap.firstKey();
    }

    /// Answers the last element in this TreeSet.
    ///
    /// #### Returns
    ///
    /// the last element
    ///
    /// #### Throws
    ///
    /// - `NoSuchElementException`: when this TreeSet is empty
    public E last() {
        return backingMap.lastKey();
    }

    /// {@inheritDoc}
    ///
    /// #### Since
    ///
    /// 1.6
    ///
    /// #### See also
    ///
    /// - java.util.NavigableSet#pollFirst()
    public E pollFirst() {
        Map.Entry<E, Object> entry = backingMap.pollFirstEntry();
        return (null == entry) ? null : entry.getKey();
    }

    /// {@inheritDoc}
    ///
    /// #### Since
    ///
    /// 1.6
    ///
    /// #### See also
    ///
    /// - java.util.NavigableSet#pollLast()
    public E pollLast() {
        Map.Entry<E, Object> entry = backingMap.pollLastEntry();
        return (null == entry) ? null : entry.getKey();
    }

    /// {@inheritDoc}
    ///
    /// #### Since
    ///
    /// 1.6
    ///
    /// #### See also
    ///
    /// - java.util.NavigableSet#higher(java.lang.Object)
    public E higher(E e) {
        return backingMap.higherKey(e);
    }

    /// {@inheritDoc}
    ///
    /// #### Since
    ///
    /// 1.6
    ///
    /// #### See also
    ///
    /// - java.util.NavigableSet#lower(java.lang.Object)
    public E lower(E e) {
        return backingMap.lowerKey(e);
    }

    /// {@inheritDoc}
    ///
    /// #### Since
    ///
    /// 1.6
    ///
    /// #### See also
    ///
    /// - java.util.NavigableSet#ceiling(java.lang.Object)
    public E ceiling(E e) {
        return backingMap.ceilingKey(e);
    }

    /// {@inheritDoc}
    ///
    /// #### Since
    ///
    /// 1.6
    ///
    /// #### See also
    ///
    /// - java.util.NavigableSet#floor(java.lang.Object)
    public E floor(E e) {
        return backingMap.floorKey(e);
    }

    /// {@inheritDoc}
    ///
    /// #### Since
    ///
    /// 1.6
    ///
    /// #### See also
    ///
    /// - java.util.NavigableSet#descendingSet()
    public NavigableSet<E> descendingSet() {
        return (null != descendingSet) ? descendingSet
                : (descendingSet = new TreeSet<E>(backingMap.descendingMap()));
    }

    /// {@inheritDoc}
    ///
    /// #### Since
    ///
    /// 1.6
    ///
    /// #### See also
    ///
    /// - java.util.NavigableSet#subSet(Object, boolean, Object, boolean)
    @SuppressWarnings("unchecked")
    public NavigableSet<E> subSet(E start, boolean startInclusive, E end,
            boolean endInclusive) {
        Comparator<? super E> c = backingMap.comparator();
        int compare = (c == null) ? ((java.lang.Comparable<E>) start).compareTo(end) : c
                .compare(start, end);
        if (compare <= 0) {
            return new TreeSet<E>(backingMap.subMap(start, startInclusive, end,
                    endInclusive));
        }
        throw new IllegalArgumentException();
    }

    /// {@inheritDoc}
    ///
    /// #### Since
    ///
    /// 1.6
    ///
    /// #### See also
    ///
    /// - java.util.NavigableSet#headSet(Object, boolean)
    @SuppressWarnings("unchecked")
    public NavigableSet<E> headSet(E end, boolean endInclusive) {
        // Check for errors
        Comparator<? super E> c = backingMap.comparator();
        if (c == null) {
            ((java.lang.Comparable<E>) end).compareTo(end);
        } else {
            c.compare(end, end);
        }
        return new TreeSet<E>(backingMap.headMap(end, endInclusive));
    }

    /// {@inheritDoc}
    ///
    /// #### Since
    ///
    /// 1.6
    ///
    /// #### See also
    ///
    /// - java.util.NavigableSet#tailSet(Object, boolean)
    @SuppressWarnings("unchecked")
    public NavigableSet<E> tailSet(E start, boolean startInclusive) {
        // Check for errors
        Comparator<? super E> c = backingMap.comparator();
        if (c == null) {
            ((java.lang.Comparable<E>) start).compareTo(start);
        } else {
            c.compare(start, start);
        }
        return new TreeSet<E>(backingMap.tailMap(start, startInclusive));
    }

    /// Answers a SortedSet of the specified portion of this TreeSet which
    /// contains elements greater or equal to the start element but less than the
    /// end element. The returned SortedSet is backed by this TreeSet so changes
    /// to one are reflected by the other.
    ///
    /// #### Parameters
    ///
    /// - `start`: the start element
    ///
    /// - `end`: the end element
    ///
    /// #### Returns
    ///
    /// @return a subset where the elements are greater or equal to
    /// `start` and less than `end`
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @exception ClassCastException
    /// when the start or end object cannot be compared with the
    /// elements in this TreeSet
    ///
    /// - `NullPointerException`: @exception NullPointerException
    /// when the start or end object is null and the comparator
    /// cannot handle null
    @SuppressWarnings("unchecked")
    public SortedSet<E> subSet(E start, E end) {
        return subSet(start, true, end, false);
    }

    /// Answers a SortedSet of the specified portion of this TreeSet which
    /// contains elements less than the end element. The returned SortedSet is
    /// backed by this TreeSet so changes to one are reflected by the other.
    ///
    /// #### Parameters
    ///
    /// - `end`: the end element
    ///
    /// #### Returns
    ///
    /// a subset where the elements are less than `end`
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @exception ClassCastException
    /// when the end object cannot be compared with the elements
    /// in this TreeSet
    ///
    /// - `NullPointerException`: @exception NullPointerException
    /// when the end object is null and the comparator cannot
    /// handle null
    @SuppressWarnings("unchecked")
    public SortedSet<E> headSet(E end) {
        return headSet(end, false);
    }

    /// Answers a SortedSet of the specified portion of this TreeSet which
    /// contains elements greater or equal to the start element. The returned
    /// SortedSet is backed by this TreeSet so changes to one are reflected by
    /// the other.
    ///
    /// #### Parameters
    ///
    /// - `start`: the start element
    ///
    /// #### Returns
    ///
    /// @return a subset where the elements are greater or equal to
    /// `start`
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @exception ClassCastException
    /// when the start object cannot be compared with the elements
    /// in this TreeSet
    ///
    /// - `NullPointerException`: @exception NullPointerException
    /// when the start object is null and the comparator cannot
    /// handle null
    @SuppressWarnings("unchecked")
    public SortedSet<E> tailSet(E start) {
        return tailSet(start, true);
    }
}
