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
 * Class {@code AbstractCollection} is an abstract implementation of the {@code
 * Collection} interface. A subclass must implement the abstract methods {@code
 * iterator()} and {@code size()} to create an immutable collection. To create a
 * modifiable collection it's necessary to override the {@code add()} method that
 * currently throws an {@code UnsupportedOperationException}.
 *
 * @since 1.2
 */
public abstract class AbstractCollection<E> implements Collection<E> {

    /**
     * Constructs a new instance of this AbstractCollection.
     */
    protected AbstractCollection() {
        super();
    }

    public boolean add(E object) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to add all of the objects contained in {@code collection}
     * to the contents of this {@code Collection} (optional). This implementation
     * iterates over the given {@code Collection} and calls {@code add} for each
     * element. If any of these calls return {@code true}, then {@code true} is
     * returned as result of this method call, {@code false} otherwise. If this
     * {@code Collection} does not support adding elements, an {@code
     * UnsupportedOperationException} is thrown.
     * <p>
     * If the passed {@code Collection} is changed during the process of adding elements
     * to this {@code Collection}, the behavior depends on the behavior of the passed
     * {@code Collection}.
     * 
     * @param collection
     *            the collection of objects.
     * @return {@code true} if this {@code Collection} is modified, {@code false}
     *         otherwise.
     * @throws UnsupportedOperationException
     *                if adding to this {@code Collection} is not supported.
     * @throws ClassCastException
     *                if the class of an object is inappropriate for this
     *                {@code Collection}.
     * @throws IllegalArgumentException
     *                if an object cannot be added to this {@code Collection}.
     * @throws NullPointerException
     *                if {@code collection} is {@code null}, or if it contains
     *                {@code null} elements and this {@code Collection} does not support
     *                such elements.
     */
    /* for-each, NOT an explicit iterator. The translator lowers a for-each over a
     * collection whose runtime layout it recognises into an index walk of the backing
     * storage -- no iterator object, no interface dispatch per element. It matches on
     * the BYTECODE SHAPE javac emits for for-each, so the equivalent
     * `Iterator it = c.iterator(); while (it.hasNext())` that used to be here is not
     * recognised and allocates a real iterator every call.
     *
     * These are the base implementations every collection inherits, so each one that
     * still spells the loop out by hand costs an allocation on every call for every
     * collection that does not override it. */
    public boolean addAll(Collection<? extends E> collection) {
        boolean result = false;
        for (E element : collection) {
            if (add(element)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Removes all elements from this {@code Collection}, leaving it empty (optional).
     * This implementation iterates over this {@code Collection} and calls the {@code
     * remove} method on each element. If the iterator does not support removal
     * of elements, an {@code UnsupportedOperationException} is thrown.
     * <p>
     * Concrete implementations usually can clear a {@code Collection} more efficiently
     * and should therefore overwrite this method.
     * 
     * @throws UnsupportedOperationException
     *                it the iterator does not support removing elements from
     *                this {@code Collection}
     * @see #iterator
     * @see #isEmpty
     * @see #size
     */
    public void clear() {
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    /**
     * Tests whether this {@code Collection} contains the specified object. This
     * implementation iterates over this {@code Collection} and tests, whether any
     * element is equal to the given object. If {@code object != null} then
     * {@code object.equals(e)} is called for each element {@code e} returned by
     * the iterator until the element is found. If {@code object == null} then
     * each element {@code e} returned by the iterator is compared with the test
     * {@code e == null}.
     * 
     * @param object
     *            the object to search for.
     * @return {@code true} if object is an element of this {@code Collection}, {@code
     *         false} otherwise.
     * @throws ClassCastException
     *                if the object to look for isn't of the correct type.
     * @throws NullPointerException
     *                if the object to look for is {@code null} and this
     *                {@code Collection} doesn't support {@code null} elements.
     */
    public boolean contains(Object object) {
        // The null test stays hoisted out of the loop, as it was: it is loop
        // invariant and equals must not be called on a null.
        if (object != null) {
            for (E element : this) {
                if (object.equals(element)) {
                    return true;
                }
            }
        } else {
            for (E element : this) {
                if (element == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests whether this {@code Collection} contains all objects contained in the
     * specified {@code Collection}. This implementation iterates over the specified
     * {@code Collection}. If one element returned by the iterator is not contained in
     * this {@code Collection}, then {@code false} is returned; {@code true} otherwise.
     * 
     * @param collection
     *            the collection of objects.
     * @return {@code true} if all objects in the specified {@code Collection} are
     *         elements of this {@code Collection}, {@code false} otherwise.
     * @throws ClassCastException
     *                if one or more elements of {@code collection} isn't of the
     *                correct type.
     * @throws NullPointerException
     *                if {@code collection} contains at least one {@code null}
     *                element and this {@code Collection} doesn't support {@code null}
     *                elements.
     * @throws NullPointerException
     *                if {@code collection} is {@code null}.
     */
    public boolean containsAll(Collection<?> collection) {
        for (Object element : collection) {
            if (!contains(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns if this {@code Collection} contains no elements. This implementation
     * tests, whether {@code size} returns 0.
     * 
     * @return {@code true} if this {@code Collection} has no elements, {@code false}
     *         otherwise.
     *
     * @see #size
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns an instance of {@link Iterator} that may be used to access the
     * objects contained by this {@code Collection}. The order in which the elements are
     * returned by the {@link Iterator} is not defined unless the instance of the
     * {@code Collection} has a defined order.  In that case, the elements are returned in that order.
     * <p>
     * In this class this method is declared abstract and has to be implemented
     * by concrete {@code Collection} implementations.
     * 
     * @return an iterator for accessing the {@code Collection} contents.
     */
    public abstract Iterator<E> iterator();

    /**
     * Removes one instance of the specified object from this {@code Collection} if one
     * is contained (optional). This implementation iterates over this
     * {@code Collection} and tests for each element {@code e} returned by the iterator,
     * whether {@code e} is equal to the given object. If {@code object != null}
     * then this test is performed using {@code object.equals(e)}, otherwise
     * using {@code object == null}. If an element equal to the given object is
     * found, then the {@code remove} method is called on the iterator and
     * {@code true} is returned, {@code false} otherwise. If the iterator does
     * not support removing elements, an {@code UnsupportedOperationException}
     * is thrown.
     * 
     * @param object
     *            the object to remove.
     * @return {@code true} if this {@code Collection} is modified, {@code false}
     *         otherwise.
     * @throws UnsupportedOperationException
     *                if removing from this {@code Collection} is not supported.
     * @throws ClassCastException
     *                if the object passed is not of the correct type.
     * @throws NullPointerException
     *                if {@code object} is {@code null} and this {@code Collection}
     *                doesn't support {@code null} elements.
     */
    public boolean remove(Object object) {
        Iterator<?> it = iterator();
        if (object != null) {
            while (it.hasNext()) {
                if (object.equals(it.next())) {
                    it.remove();
                    return true;
                }
            }
        } else {
            while (it.hasNext()) {
                if (it.next() == null) {
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes all occurrences in this {@code Collection} of each object in the
     * specified {@code Collection} (optional). After this method returns none of the
     * elements in the passed {@code Collection} can be found in this {@code Collection}
     * anymore.
     * <p>
     * This implementation iterates over this {@code Collection} and tests for each
     * element {@code e} returned by the iterator, whether it is contained in
     * the specified {@code Collection}. If this test is positive, then the {@code
     * remove} method is called on the iterator. If the iterator does not
     * support removing elements, an {@code UnsupportedOperationException} is
     * thrown.
     * 
     * @param collection
     *            the collection of objects to remove.
     * @return {@code true} if this {@code Collection} is modified, {@code false}
     *         otherwise.
     * @throws UnsupportedOperationException
     *                if removing from this {@code Collection} is not supported.
     * @throws ClassCastException
     *                if one or more elements of {@code collection} isn't of the
     *                correct type.
     * @throws NullPointerException
     *                if {@code collection} contains at least one {@code null}
     *                element and this {@code Collection} doesn't support {@code null}
     *                elements.
     * @throws NullPointerException
     *                if {@code collection} is {@code null}.
     */
    public boolean removeAll(Collection<?> collection) {
        boolean result = false;
        Iterator<?> it = iterator();
        while (it.hasNext()) {
            if (collection.contains(it.next())) {
                it.remove();
                result = true;
            }
        }
        return result;
    }

    /**
     * Removes all objects from this {@code Collection} that are not also found in the
     * {@code Collection} passed (optional). After this method returns this {@code Collection}
     * will only contain elements that also can be found in the {@code Collection}
     * passed to this method.
     * <p>
     * This implementation iterates over this {@code Collection} and tests for each
     * element {@code e} returned by the iterator, whether it is contained in
     * the specified {@code Collection}. If this test is negative, then the {@code
     * remove} method is called on the iterator. If the iterator does not
     * support removing elements, an {@code UnsupportedOperationException} is
     * thrown.
     * 
     * @param collection
     *            the collection of objects to retain.
     * @return {@code true} if this {@code Collection} is modified, {@code false}
     *         otherwise.
     * @throws UnsupportedOperationException
     *                if removing from this {@code Collection} is not supported.
     * @throws ClassCastException
     *                if one or more elements of {@code collection}
     *                isn't of the correct type.
     * @throws NullPointerException
     *                if {@code collection} contains at least one
     *                {@code null} element and this {@code Collection} doesn't support
     *                {@code null} elements.
     * @throws NullPointerException
     *                if {@code collection} is {@code null}.
     */
    public boolean retainAll(Collection<?> collection) {
        boolean result = false;
        Iterator<?> it = iterator();
        while (it.hasNext()) {
            if (!collection.contains(it.next())) {
                it.remove();
                result = true;
            }
        }
        return result;
    }

    /**
     * Returns a count of how many objects this {@code Collection} contains.
     * <p>
     * In this class this method is declared abstract and has to be implemented
     * by concrete {@code Collection} implementations.
     * 
     * @return how many objects this {@code Collection} contains, or {@code Integer.MAX_VALUE}
     *         if there are more than {@code Integer.MAX_VALUE} elements in this
     *         {@code Collection}.
     */
    public abstract int size();

    /**
     * Returns the string representation of this {@code Collection}. The presentation
     * has a specific format. It is enclosed by square brackets ("[]"). Elements
     * are separated by ', ' (comma and space).
     * 
     * @return the string representation of this {@code Collection}.
     */
    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]"; //$NON-NLS-1$
        }

        StringBuffer buffer = new StringBuffer(size() * 16);
        buffer.append('[');
        // The separator used to be decided by asking the iterator whether another
        // element follows. A for-each cannot ask that, and does not need to: writing
        // the separator BEFORE every element except the first produces the same
        // string, and lets the loop be the shape the translator lowers.
        boolean first = true;
        for (Object next : this) {
            if (!first) {
                buffer.append(", "); //$NON-NLS-1$
            }
            first = false;
            if (next != this) {
                buffer.append(next);
            } else {
                buffer.append("(this Collection)"); //$NON-NLS-1$
            }
        }
        buffer.append(']');
        return buffer.toString();
    }


    /**
     * Returns a new array containing all elements contained in this
     * {@code ArrayList}.
     * 
     * @return an array of the elements from this {@code ArrayList}
     */
    @Override
    public Object[] toArray() {
        return ArrayList.toObjectArray(this);
    }

    /**
     * Returns an array containing all elements contained in this
     * {@code ArrayList}. If the specified array is large enough to hold the
     * elements, the specified array is used, otherwise an array of the same
     * type is created. If the specified array is used and is larger than this
     * {@code ArrayList}, the array element following the collection elements
     * is set to null.
     * 
     * @param contents
     *            the array.
     * @return an array of the elements from this {@code ArrayList}.
     * @throws ArrayStoreException
     *             when the type of an element in this {@code ArrayList} cannot
     *             be stored in the type of the specified array.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] contents) {
        int size = size();
        Object[] arr = contents;
        if (size > arr.length) {
            arr = (Object[])java.lang.reflect.Array.newInstance(contents.getClass().getComponentType(), size);
        }
        int at = 0;
        for (E element : this) {
            arr[at++] = element;
        }
        // The contract this method's own javadoc states: when the supplied array is
        // longer than the collection, the element AFTER the last is set to null. The
        // previous loop ran to arr.length and kept calling next(), so a caller passing
        // an oversized array got a NoSuchElementException instead of their array back.
        if (arr.length > size) {
            arr[size] = null;
        }
        return (T[])arr;
    }
}
