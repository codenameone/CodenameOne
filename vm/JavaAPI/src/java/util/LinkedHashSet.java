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
 * LinkedHashSet is a variant of HashSet. Its entries are kept in a
 * doubly-linked list. The iteration order is the order in which entries were
 * inserted.
 * <p>
 * Null elements are allowed, and all the optional Set operations are supported.
 * <p>
 * Like HashSet, LinkedHashSet is not thread safe, so access by multiple threads
 * must be synchronized by an external mechanism such as
 * {@link Collections#synchronizedSet(Set)}.
 *
 * @since 1.4
 */
public class LinkedHashSet<E> extends HashSet<E> implements Set<E> {

    /**
     * Constructs a new empty instance of {@code LinkedHashSet}.
     */
    /* The backing map is held HERE rather than in HashSet, so a plain HashSet
     * carries no field for it and no branch on it -- see the comment on the field
     * that used to be there. super(true) selects the constructor that allocates no
     * native table, because the elements live in this map instead.
     *
     * Assigned before anything can add: the Collection constructor below adds
     * through the overrides in this class, which read this field. */
    transient HashMap<E, HashSet<E>> backingMap;

    public LinkedHashSet() {
        super(true);
        backingMap = new LinkedHashMap<E, HashSet<E>>();
    }

    /**
     * Constructs a new instance of {@code LinkedHashSet} with the specified
     * capacity.
     * 
     * @param capacity
     *            the initial capacity of this {@code LinkedHashSet}.
     */
    public LinkedHashSet(int capacity) {
        super(true);
        backingMap = new LinkedHashMap<E, HashSet<E>>(capacity);
    }

    /**
     * Constructs a new instance of {@code LinkedHashSet} with the specified
     * capacity and load factor.
     * 
     * @param capacity
     *            the initial capacity.
     * @param loadFactor
     *            the initial load factor.
     */
    public LinkedHashSet(int capacity, float loadFactor) {
        super(true);
        backingMap = new LinkedHashMap<E, HashSet<E>>(capacity, loadFactor);
    }

    /**
     * Constructs a new instance of {@code LinkedHashSet} containing the unique
     * elements in the specified collection.
     * 
     * @param collection
     *            the collection of elements to add.
     */
    public LinkedHashSet(Collection<? extends E> collection) {
        super(true);
        backingMap = new LinkedHashMap<E, HashSet<E>>(collection.size() < 6 ? 11
                : collection.size() * 2);
        Iterator it = collection.iterator();
        while(it.hasNext()) {
            add((E)it.next());
        }
    }

    /* Every operation HashSet used to fork on `backingMap != null` is overridden
     * here instead. Same behaviour, and the fork is gone from the plain HashSet
     * path entirely rather than being paid on every call. isEmpty needs no
     * override: it is defined in terms of size(). */

    @Override
    public boolean add(E object) {
        return backingMap.put(object, this) == null;
    }

    @Override
    public void clear() {
        backingMap.clear();
    }

    @Override
    public boolean contains(Object object) {
        return backingMap.containsKey(object);
    }

    @Override
    public Iterator<E> iterator() {
        return backingMap.keySet().iterator();
    }

    @Override
    public boolean remove(Object object) {
        return backingMap.remove(object) != null;
    }

    @Override
    public int size() {
        return backingMap.size();
    }
}
