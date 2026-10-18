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

import java.util.function.UnaryOperator;


/**
 * A {@code List} is a collection which maintains an ordering for its elements. Every
 * element in the {@code List} has an index. Each element can thus be accessed by its
 * index, with the first index being zero. Normally, {@code List}s allow duplicate
 * elements, as compared to Sets, where elements have to be unique.
 */
public interface List<E> extends Collection<E> {
    /**
     * Inserts the specified object into this {@code List} at the specified location.
     * The object is inserted before the current element at the specified
     * location. If the location is equal to the size of this {@code List}, the object
     * is added at the end. If the location is smaller than the size of this
     * {@code List}, then all elements beyond the specified location are moved by one
     * position towards the end of the {@code List}.
     * 
     * @param location
     *            the index at which to insert.
     * @param object
     *            the object to add.
     * @throws UnsupportedOperationException
     *                if adding to this {@code List} is not supported.
     * @throws ClassCastException
     *                if the class of the object is inappropriate for this
     *                {@code List}.
     * @throws IllegalArgumentException
     *                if the object cannot be added to this {@code List}.
     * @throws IndexOutOfBoundsException
     *                if {@code location < 0 || location > size()}
     */
    public void add(int location, E object);

    /**
     * Adds the specified object at the end of this {@code List}.
     * 
     * @param object
     *            the object to add.
     * @return always true.
     * @throws UnsupportedOperationException
     *                if adding to this {@code List} is not supported.
     * @throws ClassCastException
     *                if the class of the object is inappropriate for this
     *                {@code List}.
     * @throws IllegalArgumentException
     *                if the object cannot be added to this {@code List}.
     */
    public boolean add(E object);

    /**
     * Inserts the objects in the specified collection at the specified location
     * in this {@code List}. The objects are added in the order they are returned from
     * the collection's iterator.
     * 
     * @param location
     *            the index at which to insert.
     * @param collection
     *            the collection of objects to be inserted.
     * @return true if this {@code List} has been modified through the insertion, false
     *         otherwise (i.e. if the passed collection was empty).
     * @throws UnsupportedOperationException
     *                if adding to this {@code List} is not supported.
     * @throws ClassCastException
     *                if the class of an object is inappropriate for this
     *                {@code List}.
     * @throws IllegalArgumentException
     *                if an object cannot be added to this {@code List}.
     * @throws IndexOutOfBoundsException
     *                if {@code location < 0 || > size()}
     */
    public boolean addAll(int location, Collection<? extends E> collection);

    /**
     * Adds the objects in the specified collection to the end of this {@code List}. The
     * objects are added in the order in which they are returned from the
     * collection's iterator.
     * 
     * @param collection
     *            the collection of objects.
     * @return {@code true} if this {@code List} is modified, {@code false} otherwise
     *         (i.e. if the passed collection was empty).
     * @throws UnsupportedOperationException
     *                if adding to this {@code List} is not supported.
     * @throws ClassCastException
     *                if the class of an object is inappropriate for this
     *                {@code List}.
     * @throws IllegalArgumentException
     *                if an object cannot be added to this {@code List}.
     */
    public boolean addAll(Collection<? extends E> collection);

    /**
     * Removes all elements from this {@code List}, leaving it empty.
     * 
     * @throws UnsupportedOperationException
     *                if removing from this {@code List} is not supported.
     * @see #isEmpty
     * @see #size
     */
    public void clear();

    /**
     * Tests whether this {@code List} contains the specified object.
     * 
     * @param object
     *            the object to search for.
     * @return {@code true} if object is an element of this {@code List}, {@code false}
     *         otherwise
     */
    public boolean contains(Object object);

    /**
     * Tests whether this {@code List} contains all objects contained in the
     * specified collection.
     * 
     * @param collection
     *            the collection of objects
     * @return {@code true} if all objects in the specified collection are
     *         elements of this {@code List}, {@code false} otherwise.
     */
    public boolean containsAll(Collection<?> collection);

    /**
     * Compares the given object with the {@code List}, and returns true if they
     * represent the <em>same</em> object using a class specific comparison. For
     * {@code List}s, this means that they contain the same elements in exactly the same
     * order.
     * 
     * @param object
     *            the object to compare with this object.
     * @return boolean {@code true} if the object is the same as this object,
     *         and {@code false} if it is different from this object.
     * @see #hashCode
     */
    public boolean equals(Object object);

    /**
     * Returns the element at the specified location in this {@code List}.
     * 
     * @param location
     *            the index of the element to return.
     * @return the element at the specified location.
     * @throws IndexOutOfBoundsException
     *                if {@code location < 0 || >= size()}
     */
    public E get(int location);

    /**
     * Returns the hash code for this {@code List}. It is calculated by taking each
     * element' hashcode and its position in the {@code List} into account.
     * 
     * @return the hash code of the {@code List}.
     */
    public int hashCode();

    /**
     * Searches this {@code List} for the specified object and returns the index of the
     * first occurrence.
     * 
     * @param object
     *            the object to search for.
     * @return the index of the first occurrence of the object or -1 if the
     *         object was not found.
     */
    public int indexOf(Object object);

    /**
     * Returns whether this {@code List} contains no elements.
     * 
     * @return {@code true} if this {@code List} has no elements, {@code false}
     *         otherwise.
     * @see #size
     */
    public boolean isEmpty();

    /**
     * Returns an iterator on the elements of this {@code List}. The elements are
     * iterated in the same order as they occur in the {@code List}.
     * 
     * @return an iterator on the elements of this {@code List}.
     * @see Iterator
     */
    public Iterator<E> iterator();

    /**
     * Searches this {@code List} for the specified object and returns the index of the
     * last occurrence.
     * 
     * @param object
     *            the object to search for.
     * @return the index of the last occurrence of the object, or -1 if the
     *         object was not found.
     */
    public int lastIndexOf(Object object);

    /**
     * Returns a {@code List} iterator on the elements of this {@code List}. The elements are
     * iterated in the same order that they occur in the {@code List}.
     * 
     * @return a {@code List} iterator on the elements of this {@code List}
     * 
     * @see ListIterator
     */
    public ListIterator<E> listIterator();

    /**
     * Returns a list iterator on the elements of this {@code List}. The elements are
     * iterated in the same order as they occur in the {@code List}. The iteration
     * starts at the specified location.
     * 
     * @param location
     *            the index at which to start the iteration.
     * @return a list iterator on the elements of this {@code List}.
     * @throws IndexOutOfBoundsException
     *                if {@code location < 0 || location > size()}
     * @see ListIterator
     */
    public ListIterator<E> listIterator(int location);

    /**
     * Removes the object at the specified location from this {@code List}.
     * 
     * @param location
     *            the index of the object to remove.
     * @return the removed object.
     * @throws UnsupportedOperationException
     *                if removing from this {@code List} is not supported.
     * @throws IndexOutOfBoundsException
     *                if {@code location < 0 || >= size()}
     */
    public E remove(int location);

    /**
     * Removes the first occurrence of the specified object from this {@code List}.
     * 
     * @param object
     *            the object to remove.
     * @return true if this {@code List} was modified by this operation, false
     *         otherwise.
     * @throws UnsupportedOperationException
     *                if removing from this {@code List} is not supported.
     */
    public boolean remove(Object object);

    /**
     * Removes all occurrences in this {@code List} of each object in the specified
     * collection.
     * 
     * @param collection
     *            the collection of objects to remove.
     * @return {@code true} if this {@code List} is modified, {@code false} otherwise.
     * @throws UnsupportedOperationException
     *                if removing from this {@code List} is not supported.
     */
    public boolean removeAll(Collection<?> collection);

    /**
     * Removes all objects from this {@code List} that are not contained in the
     * specified collection.
     * 
     * @param collection
     *            the collection of objects to retain.
     * @return {@code true} if this {@code List} is modified, {@code false} otherwise.
     * @throws UnsupportedOperationException
     *                if removing from this {@code List} is not supported.
     */
    public boolean retainAll(Collection<?> collection);

    /**
     * Replaces the element at the specified location in this {@code List} with the
     * specified object. This operation does not change the size of the {@code List}.
     * 
     * @param location
     *            the index at which to put the specified object.
     * @param object
     *            the object to insert.
     * @return the previous element at the index.
     * @throws UnsupportedOperationException
     *                if replacing elements in this {@code List} is not supported.
     * @throws ClassCastException
     *                if the class of an object is inappropriate for this
     *                {@code List}.
     * @throws IllegalArgumentException
     *                if an object cannot be added to this {@code List}.
     * @throws IndexOutOfBoundsException
     *                if {@code location < 0 || >= size()}
     */
    public E set(int location, E object);

    /**
     * Returns the number of elements in this {@code List}.
     * 
     * @return the number of elements in this {@code List}.
     */
    public int size();

    /**
     * Returns a {@code List} of the specified portion of this {@code List} from the given start
     * index to the end index minus one. The returned {@code List} is backed by this
     * {@code List} so changes to it are reflected by the other.
     * 
     * @param start
     *            the index at which to start the sublist.
     * @param end
     *            the index one past the end of the sublist.
     * @return a list of a portion of this {@code List}.
     * @throws IndexOutOfBoundsException
     *                if {@code start < 0, start > end} or {@code end >
     *                size()}
     */
    public List<E> subList(int start, int end);

    /**
     * Returns an array containing all elements contained in this {@code List}.
     * 
     * @return an array of the elements from this {@code List}.
     */
    public Object[] toArray();

    /**
     * Returns an array containing all elements contained in this {@code List}. If the
     * specified array is large enough to hold the elements, the specified array
     * is used, otherwise an array of the same type is created. If the specified
     * array is used and is larger than this {@code List}, the array element following
     * the collection elements is set to null.
     * 
     * @param array
     *            the array.
     * @return an array of the elements from this {@code List}.
     * @throws ArrayStoreException
     *                if the type of an element in this {@code List} cannot be stored
     *                in the type of the specified array.
     */
    public <T> T[] toArray(T[] array);

    /**
     * Replaces each element of this list with the result of applying the
     * operator to that element.
     */
    default void replaceAll(UnaryOperator<E> operator) {
        if (operator == null) {
            throw new NullPointerException();
        }
        ListIterator<E> it = listIterator();
        while (it.hasNext()) {
            it.set(operator.apply(it.next()));
        }
    }

    /**
     * Sorts this list according to the order induced by the specified
     * {@code Comparator}. A {@code null} comparator sorts by natural ordering.
     */
    default void sort(Comparator<? super E> c) {
        Object[] a = toArray();
        Arrays.sort(a, (Comparator) c);
        ListIterator<E> it = listIterator();
        for (Object e : a) {
            it.next();
            it.set((E) e);
        }
    }

    /// Inserts `e` before the first element.
    default void addFirst(E e) {
        add(0, e);
    }

    /// Appends `e` after the last element.
    default void addLast(E e) {
        add(e);
    }

    /// The first element; NoSuchElementException when there is none.
    default E getFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return get(0);
    }

    /// The last element; NoSuchElementException when there is none.
    default E getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return get(size() - 1);
    }

    /// Removes and answers the first element; NoSuchElementException when
    /// there is none.
    default E removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return remove(0);
    }

    /// Removes and answers the last element; NoSuchElementException when
    /// there is none.
    default E removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return remove(size() - 1);
    }

    /// A view of this list in the opposite order. It reads and writes
    /// through to this list, and its own `reversed()` is this list.
    default List<E> reversed() {
        // An indexed list stays one reversed, so RandomAccess-aware code
        // such as Collections.binarySearch keeps its indexed path.
        return this instanceof RandomAccess ? new ReversedList.Indexed<E>(this) : new ReversedList<E>(this);
    }

    /// An unmodifiable list of no elements.
    static <E> List<E> of() {
        return ImmutableCollections.list(new Object[] {});
    }

    /// An unmodifiable list of the given elements, none of which may be null.
    static <E> List<E> of(E e1) {
        return ImmutableCollections.list(new Object[] {e1});
    }

    /// An unmodifiable list of the given elements, none of which may be null.
    static <E> List<E> of(E e1, E e2) {
        return ImmutableCollections.list(new Object[] {e1, e2});
    }

    /// An unmodifiable list of the given elements, none of which may be null.
    static <E> List<E> of(E e1, E e2, E e3) {
        return ImmutableCollections.list(new Object[] {e1, e2, e3});
    }

    /// An unmodifiable list of the given elements, none of which may be null.
    static <E> List<E> of(E e1, E e2, E e3, E e4) {
        return ImmutableCollections.list(new Object[] {e1, e2, e3, e4});
    }

    /// An unmodifiable list of the given elements, none of which may be null.
    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
        return ImmutableCollections.list(new Object[] {e1, e2, e3, e4, e5});
    }

    /// An unmodifiable list of the given elements, none of which may be null.
    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return ImmutableCollections.list(new Object[] {e1, e2, e3, e4, e5, e6});
    }

    /// An unmodifiable list of the given elements, none of which may be null.
    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return ImmutableCollections.list(new Object[] {e1, e2, e3, e4, e5, e6, e7});
    }

    /// An unmodifiable list of the given elements, none of which may be null.
    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return ImmutableCollections.list(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8});
    }

    /// An unmodifiable list of the given elements, none of which may be null.
    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return ImmutableCollections.list(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9});
    }

    /// An unmodifiable list of the given elements, none of which may be null.
    static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return ImmutableCollections.list(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9, e10});
    }

    /// An unmodifiable list of `elements`, none of which may be null.
    @SafeVarargs
    static <E> List<E> of(E... elements) {
        return ImmutableCollections.listCopy(elements);
    }

    /// An unmodifiable list of the elements of `collection`, which may not hold
    /// null; `collection` itself when it is already such a list.
    static <E> List<E> copyOf(Collection<? extends E> collection) {
        return ImmutableCollections.listCopyOf(collection);
    }

}
