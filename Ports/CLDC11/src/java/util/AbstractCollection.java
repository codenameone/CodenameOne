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

/// Class `AbstractCollection` is an abstract implementation of the `Collection` interface. A subclass must implement the abstract methods `iterator()` and `size()` to create an immutable collection. To create a
/// modifiable collection it's necessary to override the `add()` method that
/// currently throws an `UnsupportedOperationException`.
///
/// #### Since
///
/// 1.2
public abstract class AbstractCollection<E> implements Collection<E> {

    /// Constructs a new instance of this AbstractCollection.
    protected AbstractCollection() {
        super();
    }

    public boolean add(E object) {
        throw new UnsupportedOperationException();
    }

    /// Attempts to add all of the objects contained in `collection`
    /// to the contents of this `Collection` (optional). This implementation
    /// iterates over the given `Collection` and calls `add` for each
    /// element. If any of these calls return `true`, then `true` is
    /// returned as result of this method call, `false` otherwise. If this
    /// `Collection` does not support adding elements, an `UnsupportedOperationException` is thrown.
    ///
    /// If the passed `Collection` is changed during the process of adding elements
    /// to this `Collection`, the behavior depends on the behavior of the passed
    /// `Collection`.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `Collection` is modified, `false`
    /// otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if adding to this `Collection` is not supported.
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if the class of an object is inappropriate for this
    /// `Collection`.
    ///
    /// - `IllegalArgumentException`: if an object cannot be added to this `Collection`.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// if `collection` is `null`, or if it contains
    /// `null` elements and this `Collection` does not support
    /// such elements.
    public boolean addAll(Collection<? extends E> collection) {
        boolean result = false;
        Iterator<? extends E> it = collection.iterator();
        while (it.hasNext()) {
            if (add(it.next())) {
                result = true;
            }
        }
        return result;
    }

    /// Removes all elements from this `Collection`, leaving it empty (optional).
    /// This implementation iterates over this `Collection` and calls the `remove` method on each element. If the iterator does not support removal
    /// of elements, an `UnsupportedOperationException` is thrown.
    ///
    /// Concrete implementations usually can clear a `Collection` more efficiently
    /// and should therefore overwrite this method.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: @throws UnsupportedOperationException
    /// it the iterator does not support removing elements from
    /// this `Collection`
    ///
    /// #### See also
    ///
    /// - #iterator
    ///
    /// - #isEmpty
    ///
    /// - #size
    public void clear() {
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    /// Tests whether this `Collection` contains the specified object. This
    /// implementation iterates over this `Collection` and tests, whether any
    /// element is equal to the given object. If `object != null` then
    /// `object.equals(e)` is called for each element `e` returned by
    /// the iterator until the element is found. If `object == null` then
    /// each element `e` returned by the iterator is compared with the test
    /// `e == null`.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to search for.
    ///
    /// #### Returns
    ///
    /// `true` if object is an element of this `Collection`, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if the object to look for isn't of the correct type.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// if the object to look for is `null` and this
    /// `Collection` doesn't support `null` elements.
    public boolean contains(Object object) {
        Iterator<E> it = iterator();
        if (object != null) {
            while (it.hasNext()) {
                if (object.equals(it.next())) {
                    return true;
                }
            }
        } else {
            while (it.hasNext()) {
                if (it.next() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Tests whether this `Collection` contains all objects contained in the
    /// specified `Collection`. This implementation iterates over the specified
    /// `Collection`. If one element returned by the iterator is not contained in
    /// this `Collection`, then `false` is returned; `true` otherwise.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects.
    ///
    /// #### Returns
    ///
    /// @return `true` if all objects in the specified `Collection` are
    /// elements of this `Collection`, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if one or more elements of `collection` isn't of the
    /// correct type.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// if `collection` contains at least one `null`
    /// element and this `Collection` doesn't support `null`
    /// elements.
    ///
    /// - `NullPointerException`: if `collection` is `null`.
    public boolean containsAll(Collection<?> collection) {
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            if (!contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    /// Returns if this `Collection` contains no elements. This implementation
    /// tests, whether `size` returns 0.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `Collection` has no elements, `false`
    /// otherwise.
    ///
    /// #### See also
    ///
    /// - #size
    public boolean isEmpty() {
        return size() == 0;
    }

    /// Returns an instance of `Iterator` that may be used to access the
    /// objects contained by this `Collection`. The order in which the elements are
    /// returned by the `Iterator` is not defined unless the instance of the
    /// `Collection` has a defined order.  In that case, the elements are returned in that order.
    ///
    /// In this class this method is declared abstract and has to be implemented
    /// by concrete `Collection` implementations.
    ///
    /// #### Returns
    ///
    /// an iterator for accessing the `Collection` contents.
    public abstract Iterator<E> iterator();

    /// Removes one instance of the specified object from this `Collection` if one
    /// is contained (optional). This implementation iterates over this
    /// `Collection` and tests for each element `e` returned by the iterator,
    /// whether `e` is equal to the given object. If `object != null`
    /// then this test is performed using `object.equals(e)`, otherwise
    /// using `object == null`. If an element equal to the given object is
    /// found, then the `remove` method is called on the iterator and
    /// `true` is returned, `false` otherwise. If the iterator does
    /// not support removing elements, an `UnsupportedOperationException`
    /// is thrown.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to remove.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `Collection` is modified, `false`
    /// otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if removing from this `Collection` is not supported.
    ///
    /// - `ClassCastException`: if the object passed is not of the correct type.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// if `object` is `null` and this `Collection`
    /// doesn't support `null` elements.
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

    /// Removes all occurrences in this `Collection` of each object in the
    /// specified `Collection` (optional). After this method returns none of the
    /// elements in the passed `Collection` can be found in this `Collection`
    /// anymore.
    ///
    /// This implementation iterates over this `Collection` and tests for each
    /// element `e` returned by the iterator, whether it is contained in
    /// the specified `Collection`. If this test is positive, then the `remove` method is called on the iterator. If the iterator does not
    /// support removing elements, an `UnsupportedOperationException` is
    /// thrown.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects to remove.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `Collection` is modified, `false`
    /// otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if removing from this `Collection` is not supported.
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if one or more elements of `collection` isn't of the
    /// correct type.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// if `collection` contains at least one `null`
    /// element and this `Collection` doesn't support `null`
    /// elements.
    ///
    /// - `NullPointerException`: if `collection` is `null`.
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

    /// Removes all objects from this `Collection` that are not also found in the
    /// `Collection` passed (optional). After this method returns this `Collection`
    /// will only contain elements that also can be found in the `Collection`
    /// passed to this method.
    ///
    /// This implementation iterates over this `Collection` and tests for each
    /// element `e` returned by the iterator, whether it is contained in
    /// the specified `Collection`. If this test is negative, then the `remove` method is called on the iterator. If the iterator does not
    /// support removing elements, an `UnsupportedOperationException` is
    /// thrown.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects to retain.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `Collection` is modified, `false`
    /// otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if removing from this `Collection` is not supported.
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if one or more elements of `collection`
    /// isn't of the correct type.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// if `collection` contains at least one
    /// `null` element and this `Collection` doesn't support
    /// `null` elements.
    ///
    /// - `NullPointerException`: if `collection` is `null`.
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

    /// Returns a count of how many objects this `Collection` contains.
    ///
    /// In this class this method is declared abstract and has to be implemented
    /// by concrete `Collection` implementations.
    ///
    /// #### Returns
    ///
    /// @return how many objects this `Collection` contains, or `Integer.MAX_VALUE`
    /// if there are more than `Integer.MAX_VALUE` elements in this
    /// `Collection`.
    public abstract int size();

    /// Returns the string representation of this `Collection`. The presentation
    /// has a specific format. It is enclosed by square brackets ("[]"). Elements
    /// are separated by ', ' (comma and space).
    ///
    /// #### Returns
    ///
    /// the string representation of this `Collection`.
    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]"; //$NON-NLS-1$
        }

        StringBuffer buffer = new StringBuffer(size() * 16);
        buffer.append('[');
        Iterator<?> it = iterator();
        while (it.hasNext()) {
            Object next = it.next();
            if (next != this) {
                buffer.append(next);
            } else {
                buffer.append("(this Collection)"); //$NON-NLS-1$
            }
            if (it.hasNext()) {
                buffer.append(", "); //$NON-NLS-1$
            }
        }
        buffer.append(']');
        return buffer.toString();
    }


    /// Returns a new array containing all elements contained in this
    /// `ArrayList`.
    ///
    /// #### Returns
    ///
    /// an array of the elements from this `ArrayList`
    @Override
    public Object[] toArray() {
        return ArrayList.toObjectArray(this);
    }

    /// Returns an array containing all elements contained in this
    /// `ArrayList`. If the specified array is large enough to hold the
    /// elements, the specified array is used, otherwise an array of the same
    /// type is created. If the specified array is used and is larger than this
    /// `ArrayList`, the array element following the collection elements
    /// is set to null.
    ///
    /// #### Parameters
    ///
    /// - `contents`: the array.
    ///
    /// #### Returns
    ///
    /// an array of the elements from this `ArrayList`.
    ///
    /// #### Throws
    ///
    /// - `ArrayStoreException`: @throws ArrayStoreException
    /// when the type of an element in this `ArrayList` cannot
    /// be stored in the type of the specified array.
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] contents) {
        Object[] arr = contents;
        if (size() > arr.length) {
            arr = new Object[size()];
        }
        Iterator it = iterator();
        for(int iter = 0 ; iter < arr.length ; iter++) {
            arr[iter] = it.next();
        }
        return (T[])arr;
    }
}
