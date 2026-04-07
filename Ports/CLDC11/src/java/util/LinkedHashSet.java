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

/// LinkedHashSet is a variant of HashSet. Its entries are kept in a
/// doubly-linked list. The iteration order is the order in which entries were
/// inserted.
///
/// Null elements are allowed, and all the optional Set operations are supported.
///
/// Like HashSet, LinkedHashSet is not thread safe, so access by multiple threads
/// must be synchronized by an external mechanism such as
/// `Collections#synchronizedSet(Set)`.
///
/// #### Since
///
/// 1.4
public class LinkedHashSet<E> extends HashSet<E> implements Set<E> {

    /// Constructs a new empty instance of `LinkedHashSet`.
    public LinkedHashSet() {
        super(new LinkedHashMap<E, HashSet<E>>());
    }

    /// Constructs a new instance of `LinkedHashSet` with the specified
    /// capacity.
    ///
    /// #### Parameters
    ///
    /// - `capacity`: the initial capacity of this `LinkedHashSet`.
    public LinkedHashSet(int capacity) {
        super(new LinkedHashMap<E, HashSet<E>>(capacity));
    }

    /// Constructs a new instance of `LinkedHashSet` with the specified
    /// capacity and load factor.
    ///
    /// #### Parameters
    ///
    /// - `capacity`: the initial capacity.
    ///
    /// - `loadFactor`: the initial load factor.
    public LinkedHashSet(int capacity, float loadFactor) {
        super(new LinkedHashMap<E, HashSet<E>>(capacity, loadFactor));
    }

    /// Constructs a new instance of `LinkedHashSet` containing the unique
    /// elements in the specified collection.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of elements to add.
    public LinkedHashSet(Collection<? extends E> collection) {
        super(new LinkedHashMap<E, HashSet<E>>(collection.size() < 6 ? 11
                : collection.size() * 2));
        Iterator it = collection.iterator();
        while(it.hasNext()) {
            add((E)it.next());
        }
    }

    /* overrides method in HashMap */
    @Override
    HashMap<E, HashSet<E>> createBackingMap(int capacity, float loadFactor) {
        return new LinkedHashMap<E, HashSet<E>>(capacity, loadFactor);
    }
}
