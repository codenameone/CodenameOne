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

/// HashSet is an implementation of a Set. All optional operations (adding and
/// removing) are supported. The elements can be any objects.
public class HashSet<E> extends AbstractSet<E> implements Set<E> {

    transient HashMap<E, HashSet<E>> backingMap;

    /// Constructs a new empty instance of `HashSet`.
    public HashSet() {
        this(new HashMap<E, HashSet<E>>());
    }

    /// Constructs a new instance of `HashSet` with the specified capacity.
    ///
    /// #### Parameters
    ///
    /// - `capacity`: the initial capacity of this `HashSet`.
    public HashSet(int capacity) {
        this(new HashMap<E, HashSet<E>>(capacity));
    }

    /// Constructs a new instance of `HashSet` with the specified capacity
    /// and load factor.
    ///
    /// #### Parameters
    ///
    /// - `capacity`: the initial capacity.
    ///
    /// - `loadFactor`: the initial load factor.
    public HashSet(int capacity, float loadFactor) {
        this(new HashMap<E, HashSet<E>>(capacity, loadFactor));
    }

    /// Constructs a new instance of `HashSet` containing the unique
    /// elements in the specified collection.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of elements to add.
    public HashSet(Collection<? extends E> collection) {
        this(new HashMap<E, HashSet<E>>(collection.size() < 6 ? 11 : collection
                .size() * 2));
        Iterator it = collection.iterator();
        while(it.hasNext()) {
            add((E)it.next());
        }
    }

    HashSet(HashMap<E, HashSet<E>> backingMap) {
        this.backingMap = backingMap;
    }

    /// Adds the specified object to this `HashSet` if not already present.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to add.
    ///
    /// #### Returns
    ///
    /// @return `true` when this `HashSet` did not already contain
    /// the object, `false` otherwise
    @Override
    public boolean add(E object) {
        return backingMap.put(object, this) == null;
    }

    /// Removes all elements from this `HashSet`, leaving it empty.
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


    /// Searches this `HashSet` for the specified object.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to search for.
    ///
    /// #### Returns
    ///
    /// @return `true` if `object` is an element of this
    /// `HashSet`, `false` otherwise.
    @Override
    public boolean contains(Object object) {
        return backingMap.containsKey(object);
    }

    /// Returns true if this `HashSet` has no elements, false otherwise.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `HashSet` has no elements,
    /// `false` otherwise.
    ///
    /// #### See also
    ///
    /// - #size
    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    /// Returns an Iterator on the elements of this `HashSet`.
    ///
    /// #### Returns
    ///
    /// an Iterator on the elements of this `HashSet`.
    ///
    /// #### See also
    ///
    /// - Iterator
    @Override
    public Iterator<E> iterator() {
        return backingMap.keySet().iterator();
    }

    /// Removes the specified object from this `HashSet`.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to remove.
    ///
    /// #### Returns
    ///
    /// `true` if the object was removed, `false` otherwise.
    @Override
    public boolean remove(Object object) {
        return backingMap.remove(object) != null;
    }

    /// Returns the number of elements in this `HashSet`.
    ///
    /// #### Returns
    ///
    /// the number of elements in this `HashSet`.
    @Override
    public int size() {
        return backingMap.size();
    }

    HashMap<E, HashSet<E>> createBackingMap(int capacity, float loadFactor) {
        return new HashMap<E, HashSet<E>>(capacity, loadFactor);
    }
}
