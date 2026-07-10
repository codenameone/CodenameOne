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
 * LinkedHashMap is a variant of HashMap. Its entries are kept in a
 * doubly-linked list. The iteration order is, by default, the order in which
 * keys were inserted. Reinserting an already existing key doesn't change the
 * order. A key is existing if a call to {@code containsKey} would return true.
 * <p>
 * If the three argument constructor is used, and {@code order} is specified as
 * {@code true}, the iteration will be in the order that entries were accessed.
 * The access order gets affected by put(), get(), putAll() operations, but not
 * by operations on the collection views.
 * <p>
 * Null elements are allowed, and all the optional map operations are supported.
 * <p>
 * COMPACT LAYOUT (ParparVM): the ordering chain is kept as two parallel int
 * arrays of slot indices (prev/next) over the base class's open-addressed
 * storage -- like the base map, there are no entry objects at all.
 */
public class LinkedHashMap<K, V> extends HashMap<K, V> implements Map<K, V> {

    private static final long serialVersionUID = 3801124242820219131L;

    private final boolean accessOrder;

    /** doubly-linked ordering chain as slot indices; -1 terminates. */
    transient int[] cn1Prev;
    transient int[] cn1Next;
    transient int cn1Head = -1;
    transient int cn1Tail = -1;

    /**
     * Constructs a new empty {@code LinkedHashMap} instance.
     */
    public LinkedHashMap() {
        super();
        accessOrder = false;
        cn1InitLinks();
    }

    /**
     * Constructs a new {@code LinkedHashMap} instance with the specified
     * capacity.
     *
     * @param s the initial capacity of this map.
     * @throws IllegalArgumentException if the capacity is less than zero.
     */
    public LinkedHashMap(int s) {
        super(s);
        accessOrder = false;
        cn1InitLinks();
    }

    /**
     * Constructs a new {@code LinkedHashMap} instance with the specified
     * capacity and load factor.
     *
     * @param s the initial capacity of this map.
     * @param lf the initial load factor.
     * @throws IllegalArgumentException when the capacity is less than zero or
     *         the load factor is less or equal to zero.
     */
    public LinkedHashMap(int s, float lf) {
        super(s, lf);
        accessOrder = false;
        cn1InitLinks();
    }

    /**
     * Constructs a new {@code LinkedHashMap} instance with the specified
     * capacity, load factor and a flag specifying the ordering behavior.
     *
     * @param s the initial capacity of this hash map.
     * @param lf the initial load factor.
     * @param order {@code true} for access-order (least-recently accessed
     *        first), {@code false} for insertion order.
     * @throws IllegalArgumentException when the capacity is less than zero or
     *         the load factor is less or equal to zero.
     */
    public LinkedHashMap(int s, float lf, boolean order) {
        super(s, lf);
        accessOrder = order;
        cn1InitLinks();
    }

    /**
     * Constructs a new {@code LinkedHashMap} instance containing the mappings
     * from the specified map. The order of the elements is preserved.
     *
     * @param m the mappings to add.
     */
    public LinkedHashMap(Map<? extends K, ? extends V> m) {
        super(m.size() < 6 ? 11 : m.size() * 2);
        accessOrder = false;
        cn1InitLinks();
        putAll(m);
    }

    private void cn1InitLinks() {
        cn1Prev = new int[cn1Meta.length];
        cn1Next = new int[cn1Meta.length];
        cn1Head = -1;
        cn1Tail = -1;
    }

    private void cn1LinkAppend(int idx) {
        cn1Prev[idx] = cn1Tail;
        cn1Next[idx] = -1;
        if (cn1Tail >= 0) {
            cn1Next[cn1Tail] = idx;
        } else {
            cn1Head = idx;
        }
        cn1Tail = idx;
    }

    private void cn1Unlink(int idx) {
        int p = cn1Prev[idx];
        int n = cn1Next[idx];
        if (p >= 0) {
            cn1Next[p] = n;
        } else {
            cn1Head = n;
        }
        if (n >= 0) {
            cn1Prev[n] = p;
        } else {
            cn1Tail = p;
        }
    }

    private void cn1MoveToTail(int idx) {
        if (cn1Tail == idx) {
            return;
        }
        cn1Unlink(idx);
        cn1LinkAppend(idx);
    }

    // ---- ordering-aware overrides of the base hooks ----------------------

    @Override
    int cn1FirstIndex() {
        return cn1Head;
    }

    @Override
    int cn1NextIndex(int idx) {
        return cn1Next[idx];
    }

    @Override
    void cn1RemoveAtIndex(int idx) {
        cn1Unlink(idx);
        super.cn1RemoveAtIndex(idx);
    }

    /**
     * Rebuild preserving the ordering chain: reinsert in link order, appending
     * links as we go.
     */
    @Override
    void cn1Grow() {
        int n = elementCount;
        Object[] keys = new Object[n];
        Object[] vals = new Object[n];
        int c = 0;
        for (int i = cn1Head; i >= 0; i = cn1Next[i]) {
            keys[c] = cn1Keys[i];
            vals[c] = cn1Vals[i];
            c++;
        }
        int cap = cn1Meta.length;
        int newCap = (n * 2 >= cap) ? cap << 1 : cap;
        cn1Alloc(newCap);
        cn1InitLinks();
        for (int i = 0; i < c; i++) {
            int idx = cn1Insert(keys[i], vals[i], cn1Marker(keys[i]));
            cn1LinkAppend(idx);
        }
        elementCount = c;
        cn1Occupied = c;
    }

    /**
     * Returns the value of the mapping with the specified key. In access-order
     * mode the accessed mapping moves to the end of the iteration order.
     *
     * @param key the key.
     * @return the value of the mapping with the specified key, or {@code null}
     *         if no mapping for the specified key is found.
     */
    @Override
    public V get(Object key) {
        int idx = cn1FindSlotImpl(key);
        if (idx < 0) {
            return null;
        }
        if (accessOrder) {
            cn1MoveToTail(idx);
        }
        @SuppressWarnings("unchecked")
        V v = (V) cn1Vals[idx];
        return v;
    }

    /**
     * Maps the specified key to the specified value.
     *
     * @param key the key.
     * @param value the value.
     * @return the value of any previous mapping with the specified key or
     *         {@code null} if there was no such mapping.
     */
    @Override
    public V put(K key, V value) {
        V result = cn1PutSlot(key, value);
        if (cn1LastInserted) {
            cn1LinkAppend(cn1LastPut);
            cn1MaybeGrow();     // AFTER linking (the rebuild remaps slot indices)
        } else if (accessOrder) {
            cn1MoveToTail(cn1LastPut);
        }
        if (cn1Head >= 0 && removeEldestEntry(new CompactEntry<K, V>(this, cn1Head))) {
            @SuppressWarnings("unchecked")
            K eldest = (K) cn1Keys[cn1Head];
            remove(eldest);
        }
        return result;
    }

    @Override
    V putImpl(K key, V value) {
        return put(key, value);
    }

    /**
     * Removes the mapping with the specified key from this map.
     *
     * @param key the key of the mapping to remove.
     * @return the value of the removed mapping or {@code null} if no mapping
     *         for the specified key was found.
     */
    @Override
    public V remove(Object key) {
        return removeImpl(key);
    }

    /**
     * This method is queried from the put and putAll methods to check if the
     * eldest member of the map should be deleted before adding the new member.
     * If this map was created with accessOrder = true, then the result of
     * removeEldestEntry is assumed to be false.
     *
     * @param eldest the entry to check if it should be removed.
     * @return {@code true} if the eldest member should be removed.
     */
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return false;
    }

    /**
     * Removes all elements from this map, leaving it empty.
     *
     * @see #isEmpty()
     * @see #size()
     */
    @Override
    public void clear() {
        clearImpl();
        cn1Head = -1;
        cn1Tail = -1;
    }
}
