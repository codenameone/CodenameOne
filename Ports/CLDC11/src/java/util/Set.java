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


/// A `Set` is a data structure which does not allow duplicate elements.
///
/// #### Since
///
/// 1.2
public interface Set<E> extends Collection<E> {
    
    /// Adds the specified object to this set. The set is not modified if it
    /// already contains the object.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to add.
    ///
    /// #### Returns
    ///
    /// `true` if this set is modified, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: when adding to this set is not supported.
    ///
    /// - `ClassCastException`: when the class of the object is inappropriate for this set.
    ///
    /// - `IllegalArgumentException`: when the object cannot be added to this set.
    public boolean add(E object);

    /// Adds the objects in the specified collection which do not exist yet in
    /// this set.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects.
    ///
    /// #### Returns
    ///
    /// `true` if this set is modified, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: when adding to this set is not supported.
    ///
    /// - `ClassCastException`: when the class of an object is inappropriate for this set.
    ///
    /// - `IllegalArgumentException`: when an object cannot be added to this set.
    public boolean addAll(Collection<? extends E> collection);

    /// Removes all elements from this set, leaving it empty.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: when removing from this set is not supported.
    ///
    /// #### See also
    ///
    /// - #isEmpty
    ///
    /// - #size
    public void clear();

    /// Searches this set for the specified object.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to search for.
    ///
    /// #### Returns
    ///
    /// @return `true` if object is an element of this set, `false`
    /// otherwise.
    public boolean contains(Object object);

    /// Searches this set for all objects in the specified collection.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects.
    ///
    /// #### Returns
    ///
    /// @return `true` if all objects in the specified collection are
    /// elements of this set, `false` otherwise.
    public boolean containsAll(Collection<?> collection);

    /// Compares the specified object to this set, and returns true if they
    /// represent the *same* object using a class specific comparison.
    /// Equality for a set means that both sets have the same size and the same
    /// elements.
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

    /// Returns the hash code for this set. Two set which are equal must return
    /// the same value.
    ///
    /// #### Returns
    ///
    /// the hash code of this set.
    ///
    /// #### See also
    ///
    /// - #equals
    public int hashCode();

    /// Returns true if this set has no elements.
    ///
    /// #### Returns
    ///
    /// @return `true` if this set has no elements, `false`
    /// otherwise.
    ///
    /// #### See also
    ///
    /// - #size
    public boolean isEmpty();

    /// Returns an iterator on the elements of this set. The elements are
    /// unordered.
    ///
    /// #### Returns
    ///
    /// an iterator on the elements of this set.
    ///
    /// #### See also
    ///
    /// - Iterator
    public Iterator<E> iterator();

    /// Removes the specified object from this set.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to remove.
    ///
    /// #### Returns
    ///
    /// `true` if this set was modified, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: when removing from this set is not supported.
    public boolean remove(Object object);

    /// Removes all objects in the specified collection from this set.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects to remove.
    ///
    /// #### Returns
    ///
    /// `true` if this set was modified, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: when removing from this set is not supported.
    public boolean removeAll(Collection<?> collection);

    /// Removes all objects from this set that are not contained in the specified
    /// collection.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects to retain.
    ///
    /// #### Returns
    ///
    /// `true` if this set was modified, `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: when removing from this set is not supported.
    public boolean retainAll(Collection<?> collection);

    /// Returns the number of elements in this set.
    ///
    /// #### Returns
    ///
    /// the number of elements in this set.
    public int size();

    /// Returns an array containing all elements contained in this set.
    ///
    /// #### Returns
    ///
    /// an array of the elements from this set.
    public Object[] toArray();

    /// Returns an array containing all elements contained in this set. If the
    /// specified array is large enough to hold the elements, the specified array
    /// is used, otherwise an array of the same type is created. If the specified
    /// array is used and is larger than this set, the array element following
    /// the collection elements is set to null.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array.
    ///
    /// #### Returns
    ///
    /// an array of the elements from this set.
    ///
    /// #### Throws
    ///
    /// - `ArrayStoreException`: @throws ArrayStoreException
    /// when the type of an element in this set cannot be stored in
    /// the type of the specified array.
    ///
    /// #### See also
    ///
    /// - Collection#toArray(Object[])
    public <T> T[] toArray(T[] array);
}
