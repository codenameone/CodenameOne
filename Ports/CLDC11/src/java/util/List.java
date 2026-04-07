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


/// A `List` is a collection which maintains an ordering for its elements. Every
/// element in the `List` has an index. Each element can thus be accessed by its
/// index, with the first index being zero. Normally, `List`s allow duplicate
/// elements, as compared to Sets, where elements have to be unique.
public interface List<E> extends Collection<E> {
    /// Inserts the specified object into this `List` at the specified location.
    /// The object is inserted before the current element at the specified
    /// location. If the location is equal to the size of this `List`, the object
    /// is added at the end. If the location is smaller than the size of this
    /// `List`, then all elements beyond the specified location are moved by one
    /// position towards the end of the `List`.
    ///
    /// #### Parameters
    ///
    /// - `location`: the index at which to insert.
    ///
    /// - `object`: the object to add.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if adding to this `List` is not supported.
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if the class of the object is inappropriate for this
    /// `List`.
    ///
    /// - `IllegalArgumentException`: if the object cannot be added to this `List`.
    ///
    /// - `IndexOutOfBoundsException`: if `location  size()`
    public void add(int location, E object);

    /// Adds the specified object at the end of this `List`.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to add.
    ///
    /// #### Returns
    ///
    /// always true.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if adding to this `List` is not supported.
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if the class of the object is inappropriate for this
    /// `List`.
    ///
    /// - `IllegalArgumentException`: if the object cannot be added to this `List`.
    public boolean add(E object);

    /// Inserts the objects in the specified collection at the specified location
    /// in this `List`. The objects are added in the order they are returned from
    /// the collection's iterator.
    ///
    /// #### Parameters
    ///
    /// - `location`: the index at which to insert.
    ///
    /// - `collection`: the collection of objects to be inserted.
    ///
    /// #### Returns
    ///
    /// @return true if this `List` has been modified through the insertion, false
    /// otherwise (i.e. if the passed collection was empty).
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if adding to this `List` is not supported.
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if the class of an object is inappropriate for this
    /// `List`.
    ///
    /// - `IllegalArgumentException`: if an object cannot be added to this `List`.
    ///
    /// - `IndexOutOfBoundsException`: if `location  size()`
    public boolean addAll(int location, Collection<? extends E> collection);

    /// Adds the objects in the specified collection to the end of this `List`. The
    /// objects are added in the order in which they are returned from the
    /// collection's iterator.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `List` is modified, `false` otherwise
    /// (i.e. if the passed collection was empty).
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if adding to this `List` is not supported.
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if the class of an object is inappropriate for this
    /// `List`.
    ///
    /// - `IllegalArgumentException`: if an object cannot be added to this `List`.
    public boolean addAll(Collection<? extends E> collection);

    /// Removes all elements from this `List`, leaving it empty.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if removing from this `List` is not supported.
    ///
    /// #### See also
    ///
    /// - #isEmpty
    ///
    /// - #size
    public void clear();

    /// Tests whether this `List` contains the specified object.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to search for.
    ///
    /// #### Returns
    ///
    /// @return `true` if object is an element of this `List`, `false`
    /// otherwise
    public boolean contains(Object object);

    /// Tests whether this `List` contains all objects contained in the
    /// specified collection.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects
    ///
    /// #### Returns
    ///
    /// @return `true` if all objects in the specified collection are
    /// elements of this `List`, `false` otherwise.
    public boolean containsAll(Collection<?> collection);

    /// Compares the given object with the `List`, and returns true if they
    /// represent the *same* object using a class specific comparison. For
    /// `List`s, this means that they contain the same elements in exactly the same
    /// order.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to compare with this object.
    ///
    /// #### Returns
    ///
    /// @return boolean `true` if the object is the same as this object,
    /// and `false` if it is different from this object.
    ///
    /// #### See also
    ///
    /// - #hashCode
    public boolean equals(Object object);

    /// Returns the element at the specified location in this `List`.
    ///
    /// #### Parameters
    ///
    /// - `location`: the index of the element to return.
    ///
    /// #### Returns
    ///
    /// the element at the specified location.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: if `location = size()`
    public E get(int location);

    /// Returns the hash code for this `List`. It is calculated by taking each
    /// element' hashcode and its position in the `List` into account.
    ///
    /// #### Returns
    ///
    /// the hash code of the `List`.
    public int hashCode();

    /// Searches this `List` for the specified object and returns the index of the
    /// first occurrence.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to search for.
    ///
    /// #### Returns
    ///
    /// @return the index of the first occurrence of the object or -1 if the
    /// object was not found.
    public int indexOf(Object object);

    /// Returns whether this `List` contains no elements.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `List` has no elements, `false`
    /// otherwise.
    ///
    /// #### See also
    ///
    /// - #size
    public boolean isEmpty();

    /// Returns an iterator on the elements of this `List`. The elements are
    /// iterated in the same order as they occur in the `List`.
    ///
    /// #### Returns
    ///
    /// an iterator on the elements of this `List`.
    ///
    /// #### See also
    ///
    /// - Iterator
    public Iterator<E> iterator();

    /// Searches this `List` for the specified object and returns the index of the
    /// last occurrence.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to search for.
    ///
    /// #### Returns
    ///
    /// @return the index of the last occurrence of the object, or -1 if the
    /// object was not found.
    public int lastIndexOf(Object object);

    /// Returns a `List` iterator on the elements of this `List`. The elements are
    /// iterated in the same order that they occur in the `List`.
    ///
    /// #### Returns
    ///
    /// a `List` iterator on the elements of this `List`
    ///
    /// #### See also
    ///
    /// - ListIterator
    public ListIterator<E> listIterator();

    /// Returns a list iterator on the elements of this `List`. The elements are
    /// iterated in the same order as they occur in the `List`. The iteration
    /// starts at the specified location.
    ///
    /// #### Parameters
    ///
    /// - `location`: the index at which to start the iteration.
    ///
    /// #### Returns
    ///
    /// a list iterator on the elements of this `List`.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: if `location  size()`
    ///
    /// #### See also
    ///
    /// - ListIterator
    public ListIterator<E> listIterator(int location);

    /// Removes the object at the specified location from this `List`.
    ///
    /// #### Parameters
    ///
    /// - `location`: the index of the object to remove.
    ///
    /// #### Returns
    ///
    /// the removed object.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if removing from this `List` is not supported.
    ///
    /// - `IndexOutOfBoundsException`: if `location = size()`
    public E remove(int location);

    /// Removes the first occurrence of the specified object from this `List`.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to remove.
    ///
    /// #### Returns
    ///
    /// @return true if this `List` was modified by this operation, false
    /// otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if removing from this `List` is not supported.
    public boolean remove(Object object);

    /// Removes all occurrences in this `List` of each object in the specified
    /// collection.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects to remove.
    ///
    /// #### Returns
    ///
    /// `true` if this `List` is modified, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if removing from this `List` is not supported.
    public boolean removeAll(Collection<?> collection);

    /// Removes all objects from this `List` that are not contained in the
    /// specified collection.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects to retain.
    ///
    /// #### Returns
    ///
    /// `true` if this `List` is modified, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if removing from this `List` is not supported.
    public boolean retainAll(Collection<?> collection);

    /// Replaces the element at the specified location in this `List` with the
    /// specified object. This operation does not change the size of the `List`.
    ///
    /// #### Parameters
    ///
    /// - `location`: the index at which to put the specified object.
    ///
    /// - `object`: the object to insert.
    ///
    /// #### Returns
    ///
    /// the previous element at the index.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if replacing elements in this `List` is not supported.
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if the class of an object is inappropriate for this
    /// `List`.
    ///
    /// - `IllegalArgumentException`: if an object cannot be added to this `List`.
    ///
    /// - `IndexOutOfBoundsException`: if `location = size()`
    public E set(int location, E object);

    /// Returns the number of elements in this `List`.
    ///
    /// #### Returns
    ///
    /// the number of elements in this `List`.
    public int size();

    /// Returns a `List` of the specified portion of this `List` from the given start
    /// index to the end index minus one. The returned `List` is backed by this
    /// `List` so changes to it are reflected by the other.
    ///
    /// #### Parameters
    ///
    /// - `start`: the index at which to start the sublist.
    ///
    /// - `end`: the index one past the end of the sublist.
    ///
    /// #### Returns
    ///
    /// a list of a portion of this `List`.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: @throws IndexOutOfBoundsException
    /// if `start  end` or `end >
    /// size()`
    public List<E> subList(int start, int end);

    /// Returns an array containing all elements contained in this `List`.
    ///
    /// #### Returns
    ///
    /// an array of the elements from this `List`.
    public Object[] toArray();

    /// Returns an array containing all elements contained in this `List`. If the
    /// specified array is large enough to hold the elements, the specified array
    /// is used, otherwise an array of the same type is created. If the specified
    /// array is used and is larger than this `List`, the array element following
    /// the collection elements is set to null.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array.
    ///
    /// #### Returns
    ///
    /// an array of the elements from this `List`.
    ///
    /// #### Throws
    ///
    /// - `ArrayStoreException`: @throws ArrayStoreException
    /// if the type of an element in this `List` cannot be stored
    /// in the type of the specified array.
    public <T> T[] toArray(T[] array);
}
