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


/// SortedSet is a Set which iterates over its elements in a sorted order. The
/// order is determined either by the elements natural ordering, or by a
/// `Comparator` which is passed into a concrete implementation at
/// construction time. All elements in this set must be mutually comparable. The
/// ordering in this set must be consistent with `equals` of its elements.
///
/// #### See also
///
/// - Comparator
///
/// - Comparable
public interface SortedSet<E> extends Set<E> {
    
    /// Returns the comparator used to compare elements in this `SortedSet`.
    ///
    /// #### Returns
    ///
    /// a comparator or null if the natural ordering is used.
    public Comparator<? super E> comparator();

    /// Returns the first element in this `SortedSet`. The first element
    /// is the lowest element.
    ///
    /// #### Returns
    ///
    /// the first element.
    ///
    /// #### Throws
    ///
    /// - `NoSuchElementException`: when this `SortedSet` is empty.
    public E first();

    /// Returns a `SortedSet` of the specified portion of this
    /// `SortedSet` which contains elements less than the end element. The
    /// returned `SortedSet` is backed by this `SortedSet` so changes
    /// to one set are reflected by the other.
    ///
    /// #### Parameters
    ///
    /// - `end`: the end element.
    ///
    /// #### Returns
    ///
    /// a subset where the elements are less than `end`.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// when the class of the end element is inappropriate for this
    /// SubSet.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// when the end element is null and this `SortedSet` does
    /// not support null elements.
    public SortedSet<E> headSet(E end);

    /// Returns the last element in this `SortedSet`. The last element is
    /// the highest element.
    ///
    /// #### Returns
    ///
    /// the last element.
    ///
    /// #### Throws
    ///
    /// - `NoSuchElementException`: when this `SortedSet` is empty.
    public E last();

    /// Returns a `SortedSet` of the specified portion of this
    /// `SortedSet` which contains elements greater or equal to the start
    /// element but less than the end element. The returned `SortedSet` is
    /// backed by this SortedMap so changes to one set are reflected by the
    /// other.
    ///
    /// #### Parameters
    ///
    /// - `start`: the start element.
    ///
    /// - `end`: the end element.
    ///
    /// #### Returns
    ///
    /// @return a subset where the elements are greater or equal to `start`
    /// and less than `end`.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// when the class of the start or end element is inappropriate
    /// for this SubSet.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// when the start or end element is null and this
    /// `SortedSet` does not support null elements.
    ///
    /// - `IllegalArgumentException`: when the start element is greater than the end element.
    public SortedSet<E> subSet(E start, E end);

    /// Returns a `SortedSet` of the specified portion of this
    /// `SortedSet` which contains elements greater or equal to the start
    /// element. The returned `SortedSet` is backed by this
    /// `SortedSet` so changes to one set are reflected by the other.
    ///
    /// #### Parameters
    ///
    /// - `start`: the start element.
    ///
    /// #### Returns
    ///
    /// a subset where the elements are greater or equal to `start` .
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// when the class of the start element is inappropriate for this
    /// SubSet.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// when the start element is null and this `SortedSet`
    /// does not support null elements.
    public SortedSet<E> tailSet(E start);
}
