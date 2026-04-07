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


/// `Collection` is the root of the collection hierarchy. It defines operations on
/// data collections and the behavior that they will have in all implementations
/// of `Collection`s.
///
/// All direct or indirect implementations of `Collection` should implement at
/// least two constructors. One with no parameters which creates an empty
/// collection and one with a parameter of type `Collection`. This second
/// constructor can be used to create a collection of different type as the
/// initial collection but with the same elements. Implementations of `Collection`
/// cannot be forced to implement these two constructors but at least all
/// implementations under `java.util` do.
///
/// Methods that change the content of a collection throw an
/// `UnsupportedOperationException` if the underlying collection does not
/// support that operation, though it's not mandatory to throw such an `Exception`
/// in cases where the requested operation would not change the collection. In
/// these cases it's up to the implementation whether it throws an
/// `UnsupportedOperationException` or not.
///
/// Methods marked with (optional) can throw an
/// `UnsupportedOperationException` if the underlying collection doesn't
/// support that method.
public interface Collection<E> extends java.lang.Iterable<E> {

    /// Attempts to add `object` to the contents of this
    /// `Collection` (optional).
    ///
    /// After this method finishes successfully it is guaranteed that the object
    /// is contained in the collection.
    ///
    /// If the collection was modified it returns `true`, `false` if
    /// no changes were made.
    ///
    /// An implementation of `Collection` may narrow the set of accepted
    /// objects, but it has to specify this in the documentation. If the object
    /// to be added does not meet this restriction, then an
    /// `IllegalArgumentException` is thrown.
    ///
    /// If a collection does not yet contain an object that is to be added and
    /// adding the object fails, this method *must* throw an appropriate
    /// unchecked Exception. Returning false is not permitted in this case
    /// because it would violate the postcondition that the element will be part
    /// of the collection after this method finishes.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to add.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `Collection` is
    /// modified, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if adding to this `Collection` is not supported.
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if the class of the object is inappropriate for this
    /// collection.
    ///
    /// - `IllegalArgumentException`: if the object cannot be added to this `Collection`.
    ///
    /// - `NullPointerException`: if null elements cannot be added to the `Collection`.
    public boolean add(E object);

    /// Attempts to add all of the objects contained in `Collection`
    /// to the contents of this `Collection` (optional). If the passed `Collection`
    /// is changed during the process of adding elements to this `Collection`, the
    /// behavior is not defined.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the `Collection` of objects.
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
    /// if `collection` is `null`, or if it
    /// contains `null` elements and this `Collection` does
    /// not support such elements.
    public boolean addAll(Collection<? extends E> collection);

    /// Removes all elements from this `Collection`, leaving it empty (optional).
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if removing from this `Collection` is not supported.
    ///
    /// #### See also
    ///
    /// - #isEmpty
    ///
    /// - #size
    public void clear();

    /// Tests whether this `Collection` contains the specified object. Returns
    /// `true` if and only if at least one element `elem` in this
    /// `Collection` meets following requirement:
    /// `(object==null ? elem==null : object.equals(elem))`.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to search for.
    ///
    /// #### Returns
    ///
    /// @return `true` if object is an element of this `Collection`,
    /// `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if the object to look for isn't of the correct
    /// type.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// if the object to look for is `null` and this
    /// `Collection` doesn't support `null` elements.
    public boolean contains(Object object);

    /// Tests whether this `Collection` contains all objects contained in the
    /// specified `Collection`. If an element `elem` is contained several
    /// times in the specified `Collection`, the method returns `true` even
    /// if `elem` is contained only once in this `Collection`.
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
    public boolean containsAll(Collection<?> collection);

    /// Compares the argument to the receiver, and returns true if they represent
    /// the *same* object using a class specific comparison.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to compare with this object.
    ///
    /// #### Returns
    ///
    /// @return `true` if the object is the same as this object and
    /// `false` if it is different from this object.
    ///
    /// #### See also
    ///
    /// - #hashCode
    public boolean equals(Object object);

    /// Returns an integer hash code for the receiver. Objects which are equal
    /// return the same value for this method.
    ///
    /// #### Returns
    ///
    /// the receiver's hash.
    ///
    /// #### See also
    ///
    /// - #equals
    public int hashCode();

    /// Returns if this `Collection` contains no elements.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `Collection` has no elements, `false`
    /// otherwise.
    ///
    /// #### See also
    ///
    /// - #size
    public boolean isEmpty();

    /// Returns an instance of `Iterator` that may be used to access the
    /// objects contained by this `Collection`. The order in which the elements are
    /// returned by the iterator is not defined. Only if the instance of the
    /// `Collection` has a defined order the elements are returned in that order.
    ///
    /// #### Returns
    ///
    /// an iterator for accessing the `Collection` contents.
    public Iterator<E> iterator();

    /// Removes one instance of the specified object from this `Collection` if one
    /// is contained (optional). The element `elem` that is removed
    /// complies with `(object==null ? elem==null : object.equals(elem)`.
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
    public boolean remove(Object object);

    /// Removes all occurrences in this `Collection` of each object in the
    /// specified `Collection` (optional). After this method returns none of the
    /// elements in the passed `Collection` can be found in this `Collection`
    /// anymore.
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
    /// if one or more elements of `collection`
    /// isn't of the correct type.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// if `collection` contains at least one
    /// `null` element and this `Collection` doesn't support
    /// `null` elements.
    ///
    /// - `NullPointerException`: if `collection` is `null`.
    public boolean removeAll(Collection<?> collection);

    /// Removes all objects from this `Collection` that are not also found in the
    /// `Collection` passed (optional). After this method returns this `Collection`
    /// will only contain elements that also can be found in the `Collection`
    /// passed to this method.
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
    public boolean retainAll(Collection<?> collection);

    /// Returns a count of how many objects this `Collection` contains.
    ///
    /// #### Returns
    ///
    /// @return how many objects this `Collection` contains, or Integer.MAX_VALUE
    /// if there are more than Integer.MAX_VALUE elements in this
    /// `Collection`.
    public int size();

    /// Returns a new array containing all elements contained in this `Collection`.
    ///
    /// If the implementation has ordered elements it will return the element
    /// array in the same order as an iterator would return them.
    ///
    /// The array returned does not reflect any changes of the `Collection`. A new
    /// array is created even if the underlying data structure is already an
    /// array.
    ///
    /// #### Returns
    ///
    /// an array of the elements from this `Collection`.
    public Object[] toArray();

    /// Returns an array containing all elements contained in this `Collection`. If
    /// the specified array is large enough to hold the elements, the specified
    /// array is used, otherwise an array of the same type is created. If the
    /// specified array is used and is larger than this `Collection`, the array
    /// element following the `Collection` elements is set to null.
    ///
    /// If the implementation has ordered elements it will return the element
    /// array in the same order as an iterator would return them.
    ///
    /// `toArray(new Object[0])` behaves exactly the same way as
    /// `toArray()` does.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array.
    ///
    /// #### Returns
    ///
    /// an array of the elements from this `Collection`.
    ///
    /// #### Throws
    ///
    /// - `ArrayStoreException`: @throws ArrayStoreException
    /// if the type of an element in this `Collection` cannot be
    /// stored in the type of the specified array.
    public <T> T[] toArray(T[] array);
}
