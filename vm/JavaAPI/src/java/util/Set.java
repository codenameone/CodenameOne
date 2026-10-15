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
 * A {@code Set} is a data structure which does not allow duplicate elements.
 *
 * @since 1.2
 */
public interface Set<E> extends Collection<E> {
    
    /**
     * Adds the specified object to this set. The set is not modified if it
     * already contains the object.
     * 
     * @param object
     *            the object to add.
     * @return {@code true} if this set is modified, {@code false} otherwise.
     * @throws UnsupportedOperationException
     *             when adding to this set is not supported.
     * @throws ClassCastException
     *             when the class of the object is inappropriate for this set.
     * @throws IllegalArgumentException
     *             when the object cannot be added to this set.
     */
    public boolean add(E object);

    /**
     * Adds the objects in the specified collection which do not exist yet in
     * this set.
     * 
     * @param collection
     *            the collection of objects.
     * @return {@code true} if this set is modified, {@code false} otherwise.
     * @throws UnsupportedOperationException
     *             when adding to this set is not supported.
     * @throws ClassCastException
     *             when the class of an object is inappropriate for this set.
     * @throws IllegalArgumentException
     *             when an object cannot be added to this set.
     */
    public boolean addAll(Collection<? extends E> collection);

    /**
     * Removes all elements from this set, leaving it empty.
     * 
     * @throws UnsupportedOperationException
     *             when removing from this set is not supported.
     * @see #isEmpty
     * @see #size
     */
    public void clear();

    /**
     * Searches this set for the specified object.
     * 
     * @param object
     *            the object to search for.
     * @return {@code true} if object is an element of this set, {@code false}
     *         otherwise.
     */
    public boolean contains(Object object);

    /**
     * Searches this set for all objects in the specified collection.
     * 
     * @param collection
     *            the collection of objects.
     * @return {@code true} if all objects in the specified collection are
     *         elements of this set, {@code false} otherwise.
     */
    public boolean containsAll(Collection<?> collection);

    /**
     * Compares the specified object to this set, and returns true if they
     * represent the <em>same</em> object using a class specific comparison.
     * Equality for a set means that both sets have the same size and the same
     * elements.
     * 
     * @param object
     *            the object to compare with this object.
     * @return boolean {@code true} if the object is the same as this object,
     *         and {@code false} if it is different from this object.
     * @see #hashCode
     */
    public boolean equals(Object object);

    /**
     * Returns the hash code for this set. Two set which are equal must return
     * the same value.
     * 
     * @return the hash code of this set.
     * 
     * @see #equals
     */
    public int hashCode();

    /**
     * Returns true if this set has no elements.
     * 
     * @return {@code true} if this set has no elements, {@code false}
     *         otherwise.
     * @see #size
     */
    public boolean isEmpty();

    /**
     * Returns an iterator on the elements of this set. The elements are
     * unordered.
     * 
     * @return an iterator on the elements of this set.
     * @see Iterator
     */
    public Iterator<E> iterator();

    /**
     * Removes the specified object from this set.
     * 
     * @param object
     *            the object to remove.
     * @return {@code true} if this set was modified, {@code false} otherwise.
     * @throws UnsupportedOperationException
     *             when removing from this set is not supported.
     */
    public boolean remove(Object object);

    /**
     * Removes all objects in the specified collection from this set.
     * 
     * @param collection
     *            the collection of objects to remove.
     * @return {@code true} if this set was modified, {@code false} otherwise.
     * @throws UnsupportedOperationException
     *             when removing from this set is not supported.
     */
    public boolean removeAll(Collection<?> collection);

    /**
     * Removes all objects from this set that are not contained in the specified
     * collection.
     * 
     * @param collection
     *            the collection of objects to retain.
     * @return {@code true} if this set was modified, {@code false} otherwise.
     * @throws UnsupportedOperationException
     *             when removing from this set is not supported.
     */
    public boolean retainAll(Collection<?> collection);

    /**
     * Returns the number of elements in this set.
     * 
     * @return the number of elements in this set.
     */
    public int size();

    /**
     * Returns an array containing all elements contained in this set.
     * 
     * @return an array of the elements from this set.
     */
    public Object[] toArray();

    /**
     * Returns an array containing all elements contained in this set. If the
     * specified array is large enough to hold the elements, the specified array
     * is used, otherwise an array of the same type is created. If the specified
     * array is used and is larger than this set, the array element following
     * the collection elements is set to null.
     * 
     * @param array
     *            the array.
     * @return an array of the elements from this set.
     * @throws ArrayStoreException
     *             when the type of an element in this set cannot be stored in
     *             the type of the specified array.
     * @see Collection#toArray(Object[])
     */
    public <T> T[] toArray(T[] array);

    /// An unmodifiable set of no elements.
    static <E> Set<E> of() {
        return ImmutableCollections.set(new Object[] {}, true);
    }

    /// An unmodifiable set of the given elements, none of which may be null and no two equal.
    static <E> Set<E> of(E e1) {
        return ImmutableCollections.set(new Object[] {e1}, true);
    }

    /// An unmodifiable set of the given elements, none of which may be null and no two equal.
    static <E> Set<E> of(E e1, E e2) {
        return ImmutableCollections.set(new Object[] {e1, e2}, true);
    }

    /// An unmodifiable set of the given elements, none of which may be null and no two equal.
    static <E> Set<E> of(E e1, E e2, E e3) {
        return ImmutableCollections.set(new Object[] {e1, e2, e3}, true);
    }

    /// An unmodifiable set of the given elements, none of which may be null and no two equal.
    static <E> Set<E> of(E e1, E e2, E e3, E e4) {
        return ImmutableCollections.set(new Object[] {e1, e2, e3, e4}, true);
    }

    /// An unmodifiable set of the given elements, none of which may be null and no two equal.
    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
        return ImmutableCollections.set(new Object[] {e1, e2, e3, e4, e5}, true);
    }

    /// An unmodifiable set of the given elements, none of which may be null and no two equal.
    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return ImmutableCollections.set(new Object[] {e1, e2, e3, e4, e5, e6}, true);
    }

    /// An unmodifiable set of the given elements, none of which may be null and no two equal.
    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return ImmutableCollections.set(new Object[] {e1, e2, e3, e4, e5, e6, e7}, true);
    }

    /// An unmodifiable set of the given elements, none of which may be null and no two equal.
    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return ImmutableCollections.set(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8}, true);
    }

    /// An unmodifiable set of the given elements, none of which may be null and no two equal.
    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return ImmutableCollections.set(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9}, true);
    }

    /// An unmodifiable set of the given elements, none of which may be null and no two equal.
    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return ImmutableCollections.set(new Object[] {e1, e2, e3, e4, e5, e6, e7, e8, e9, e10}, true);
    }

    /// An unmodifiable set of `elements`, none of which may be null and no two equal.
    @SafeVarargs
    static <E> Set<E> of(E... elements) {
        return ImmutableCollections.set(elements, true);
    }

    /// An unmodifiable set of the elements of `collection`, which may not hold
    /// null; `collection` itself when it is already such a set.
    static <E> Set<E> copyOf(Collection<? extends E> collection) {
        return ImmutableCollections.setCopyOf(collection);
    }

}
