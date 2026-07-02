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
 * HashMap is an implementation of Map. All optional operations (adding and
 * removing) are supported. Keys and values can be any objects.
 *
 * <p>COMPACT LAYOUT (ParparVM): the map stores its content in three parallel
 * arrays -- keys, values and an int metadata word per slot -- probed with open
 * addressing (linear probing over a power-of-two capacity). There are NO entry
 * objects at all: no per-mapping allocation, no pointer chasing on lookup, no
 * entry-walk on {@link #clear()} (three array fills), and nothing extra for
 * the garbage collector to trace beyond the arrays themselves. The metadata
 * word is 0 for an empty slot, 1 for a tombstone (deleted), or the key's mixed
 * hash with the sign bit forced on for an occupied slot -- so probing compares
 * one negative int before ever touching the key. The hot operations
 * (get/put/remove/containsKey/clear) are implemented natively over the raw
 * array memory (see nativeMethods.m); the {@code *Impl} methods below are the
 * semantically-identical pure-Java source of truth (also used by the
 * JavaScript port).
 */
public class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {

    private static final long serialVersionUID = 362498820763181265L;

    private static final int DEFAULT_SIZE = 16;

    /** slot metadata: empty slot. */
    static final int META_EMPTY = 0;
    /** slot metadata: tombstone (previously occupied). */
    static final int META_TOMB = 1;

    /** parallel storage; length is always a power of two. */
    transient Object[] cn1Keys;
    transient Object[] cn1Vals;
    transient int[] cn1Meta;

    /** live mappings. */
    transient int elementCount;

    /** live + tombstones (drives resizing). */
    transient int cn1Occupied;

    /** structural modification count for iterator fail-fast. */
    transient int modCount = 0;

    final float loadFactor;

    /** resize when cn1Occupied reaches this. */
    int threshold;

    /** scratch results of cn1PutSlot for subclass (LinkedHashMap) hooks. */
    transient int cn1LastPut;
    transient boolean cn1LastInserted;

    /**
     * Constructs a new empty {@code HashMap} instance.
     */
    public HashMap() {
        this(DEFAULT_SIZE);
    }

    /**
     * Constructs a new {@code HashMap} instance with the specified capacity.
     *
     * @param capacity the initial capacity of this hash map.
     * @throws IllegalArgumentException when the capacity is less than zero.
     */
    public HashMap(int capacity) {
        this(capacity, 0.75f);  // default load factor of 0.75
    }

    /**
     * Calculates the capacity of storage required for storing given number of
     * elements
     */
    private static final int calculateCapacity(int x) {
        if (x >= 1 << 30) {
            return 1 << 30;
        }
        if (x == 0) {
            return 16;
        }
        x = x - 1;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return x + 1;
    }

    /**
     * Constructs a new {@code HashMap} instance with the specified capacity and
     * load factor.
     *
     * @param capacity the initial capacity of this hash map.
     * @param loadFactor the initial load factor.
     * @throws IllegalArgumentException when the capacity is less than zero or
     *         the load factor is less or equal to zero.
     */
    public HashMap(int capacity, float loadFactor) {
        if (capacity >= 0 && loadFactor > 0) {
            capacity = calculateCapacity(capacity);
            this.loadFactor = loadFactor;
            cn1Alloc(capacity);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Constructs a new {@code HashMap} instance containing the mappings from
     * the specified map.
     *
     * @param map the mappings to add.
     */
    public HashMap(Map<? extends K, ? extends V> map) {
        this(calculateCapacity(map.size()));
        putAllImpl(map);
    }

    final void cn1Alloc(int capacity) {
        cn1Keys = new Object[capacity];
        cn1Vals = new Object[capacity];
        cn1Meta = new int[capacity];
        elementCount = 0;
        cn1Occupied = 0;
        threshold = (int) (capacity * loadFactor);
        if (threshold >= capacity) {
            threshold = capacity - 1; // always keep one empty slot (probe termination)
        }
    }

    /**
     * The occupied-slot marker for a key: its mixed hash with the sign bit
     * forced on -- strictly negative, so it can never collide with META_EMPTY
     * or META_TOMB, and slot occupancy tests are a single sign check.
     */
    static int cn1Marker(Object key) {
        if (key == null) {
            return 0x80000000;
        }
        int h = computeHashCode(key);
        h ^= (h >>> 16);
        return h | 0x80000000;
    }

    /**
     * Probe for a key. Returns the slot index (>= 0) when found; otherwise
     * {@code -(insertionPoint + 1)} where insertionPoint is the first
     * tombstone met on the probe path (reuse), or the terminating empty slot.
     */
    final int cn1FindSlotImpl(Object key) {
        int marker = cn1Marker(key);
        int[] meta = cn1Meta;
        int mask = meta.length - 1;
        int i = marker & mask;
        int firstTomb = -1;
        while (true) {
            int m = meta[i];
            if (m == META_EMPTY) {
                int ins = firstTomb >= 0 ? firstTomb : i;
                return -(ins + 1);
            }
            if (m == marker) {
                Object k = cn1Keys[i];
                if (key == null ? k == null : (key == k || areEqualKeys(key, k))) {
                    return i;
                }
            } else if (m == META_TOMB && firstTomb < 0) {
                firstTomb = i;
            }
            i = (i + 1) & mask;
        }
    }

    /**
     * Shared put core (also used by LinkedHashMap). Sets {@link #cn1LastPut}
     * to the final slot of the mapping and {@link #cn1LastInserted} to whether
     * a NEW mapping was created (vs a value replacement).
     */
    final V cn1PutSlot(K key, V value) {
        int idx = cn1FindSlotImpl(key);
        if (idx >= 0) {
            @SuppressWarnings("unchecked")
            V old = (V) cn1Vals[idx];
            cn1Vals[idx] = value;
            cn1LastPut = idx;
            cn1LastInserted = false;
            return old;
        }
        int ins = -idx - 1;
        boolean wasEmpty = cn1Meta[ins] == META_EMPTY;
        cn1Meta[ins] = cn1Marker(key);
        cn1Keys[ins] = key;
        cn1Vals[ins] = value;
        elementCount++;
        if (wasEmpty) {
            cn1Occupied++;
        }
        modCount++;
        cn1LastPut = ins;
        cn1LastInserted = true;
        // NOTE: growth is the CALLER's responsibility (cn1MaybeGrow) so that
        // LinkedHashMap can link the fresh slot into its ordering chain BEFORE
        // the rebuild remaps every slot index.
        return null;
    }

    /** grow when the caller finished its post-insert bookkeeping. */
    final void cn1MaybeGrow() {
        if (cn1Occupied >= threshold) {
            cn1Grow();
        }
    }

    /**
     * Rebuild the table. Doubles the capacity when genuinely full; a
     * tombstone-heavy table is rebuilt at the same size (purging tombstones).
     * LinkedHashMap overrides this to preserve its ordering links.
     */
    void cn1Grow() {
        int cap = cn1Meta.length;
        int newCap = (elementCount * 2 >= cap) ? cap << 1 : cap;
        Object[] oldK = cn1Keys;
        Object[] oldV = cn1Vals;
        int[] oldM = cn1Meta;
        cn1Alloc(newCap);
        int count = 0;
        for (int i = 0; i < oldM.length; i++) {
            if (oldM[i] < 0) {
                cn1Insert(oldK[i], oldV[i], oldM[i]);
                count++;
            }
        }
        elementCount = count;
        cn1Occupied = count;
    }

    /** raw insert into a table known not to contain the key (rebuild path). */
    final int cn1Insert(Object key, Object value, int marker) {
        int[] meta = cn1Meta;
        int mask = meta.length - 1;
        int i = marker & mask;
        while (meta[i] != META_EMPTY) {
            i = (i + 1) & mask;
        }
        meta[i] = marker;
        cn1Keys[i] = key;
        cn1Vals[i] = value;
        return i;
    }

    /**
     * Clears a found slot (tombstone it). Virtual so LinkedHashMap can unlink
     * first; ALWAYS the single mutation point for removals (iterators too).
     */
    void cn1RemoveAtIndex(int idx) {
        cn1Meta[idx] = META_TOMB;
        cn1Keys[idx] = null;
        cn1Vals[idx] = null;
        elementCount--;
        modCount++;
    }

    /** first occupied slot in iteration order; -1 when empty. */
    int cn1FirstIndex() {
        return cn1NextOccupied(0);
    }

    /** next occupied slot after {@code idx} in iteration order; -1 at the end. */
    int cn1NextIndex(int idx) {
        return cn1NextOccupied(idx + 1);
    }

    final int cn1NextOccupied(int from) {
        int[] meta = cn1Meta;
        for (int i = from; i < meta.length; i++) {
            if (meta[i] < 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes all mappings from this hash map, leaving it empty. With the
     * compact layout this is three array fills (no per-entry work at all);
     * the native override is a pair of memsets.
     *
     * @see #isEmpty
     * @see #size
     */
    @Override
    public native void clear();

    void clearImpl() {
        if (elementCount > 0 || cn1Occupied > 0) {
            Object[] k = cn1Keys;
            Object[] v = cn1Vals;
            int[] m = cn1Meta;
            for (int i = 0; i < m.length; i++) {
                m[i] = META_EMPTY;
                k[i] = null;
                v[i] = null;
            }
            elementCount = 0;
            cn1Occupied = 0;
            modCount++;
        }
    }

    /**
     * Returns whether this map contains the specified key.
     *
     * @param key the key to search for.
     * @return {@code true} if this map contains the specified key,
     *         {@code false} otherwise.
     */
    @Override
    public native boolean containsKey(Object key);

    boolean containsKeyImpl(Object key) {
        return cn1FindSlotImpl(key) >= 0;
    }

    /**
     * Returns whether this map contains the specified value.
     *
     * @param value the value to search for.
     * @return {@code true} if this map contains the specified value,
     *         {@code false} otherwise.
     */
    @Override
    public boolean containsValue(Object value) {
        int[] meta = cn1Meta;
        Object[] vals = cn1Vals;
        if (value != null) {
            for (int i = 0; i < meta.length; i++) {
                if (meta[i] < 0 && (value == vals[i] || areEqualKeys(value, vals[i]))) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < meta.length; i++) {
                if (meta[i] < 0 && vals[i] == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a set containing all of the mappings in this map. Each mapping is
     * an instance of {@link Map.Entry}. As the set is backed by this map,
     * changes in one will be reflected in the other.
     *
     * @return a set of the mappings.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new CompactEntrySet<K, V>(this);
    }

    /**
     * Returns the value of the mapping with the specified key.
     *
     * @param key the key.
     * @return the value of the mapping with the specified key, or {@code null}
     *         if no mapping for the specified key is found.
     */
    @Override
    public native V get(Object key);

    V getImpl(Object key) {
        int idx = cn1FindSlotImpl(key);
        if (idx < 0) {
            return null;
        }
        @SuppressWarnings("unchecked")
        V v = (V) cn1Vals[idx];
        return v;
    }

    /**
     * Returns whether this map is empty.
     *
     * @return {@code true} if this map has no elements, {@code false}
     *         otherwise.
     * @see #size()
     */
    @Override
    public boolean isEmpty() {
        return elementCount == 0;
    }

    /**
     * Returns a set of the keys contained in this map. The set is backed by
     * this map so changes to one are reflected by the other. The set does not
     * support adding.
     *
     * @return a set of the keys.
     */
    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            keySet = new AbstractSet<K>() {
                @Override
                public boolean contains(Object object) {
                    return containsKey(object);
                }

                @Override
                public int size() {
                    return HashMap.this.size();
                }

                @Override
                public void clear() {
                    HashMap.this.clear();
                }

                @Override
                public boolean remove(Object key) {
                    return HashMap.this.cn1RemoveKey(key);
                }

                @Override
                public Iterator<K> iterator() {
                    return new KeyIterator<K, V>(HashMap.this);
                }
            };
        }
        return keySet;
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
    public native V put(K key, V value);

    V putImpl(K key, V value) {
        V old = cn1PutSlot(key, value);
        if (cn1LastInserted) {
            cn1MaybeGrow();
        }
        return old;
    }

    /**
     * Copies all the mappings in the specified map to this map. These mappings
     * will replace all mappings that this map had for any of the keys currently
     * in the given map.
     *
     * @param map the map to copy mappings from.
     * @throws NullPointerException if {@code map} is {@code null}.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (!map.isEmpty()) {
            putAllImpl(map);
        }
    }

    private void putAllImpl(Map<? extends K, ? extends V> map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<? extends K, ? extends V> entry = (Map.Entry<? extends K, ? extends V>) it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes the mapping with the specified key from this map.
     *
     * @param key the key of the mapping to remove.
     * @return the value of the removed mapping or {@code null} if no mapping
     *         for the specified key was found.
     */
    @Override
    public native V remove(Object key);

    V removeImpl(Object key) {
        int idx = cn1FindSlotImpl(key);
        if (idx < 0) {
            return null;
        }
        @SuppressWarnings("unchecked")
        V old = (V) cn1Vals[idx];
        cn1RemoveAtIndex(idx);
        return old;
    }

    /** keySet().remove support: true when the key was present. */
    boolean cn1RemoveKey(Object key) {
        int idx = cn1FindSlotImpl(key);
        if (idx < 0) {
            return false;
        }
        cn1RemoveAtIndex(idx);
        return true;
    }

    /**
     * Returns the number of elements in this map.
     *
     * @return the number of elements in this map.
     */
    @Override
    public int size() {
        return elementCount;
    }

    /**
     * Returns a collection of the values contained in this map. The collection
     * is backed by this map so changes to one are reflected by the other. The
     * collection supports remove, removeAll, retainAll and clear operations,
     * and it does not support add or addAll operations.
     *
     * @return a collection of the values contained in this map.
     */
    @Override
    public Collection<V> values() {
        if (valuesCollection == null) {
            valuesCollection = new AbstractCollection<V>() {
                @Override
                public boolean contains(Object object) {
                    return containsValue(object);
                }

                @Override
                public int size() {
                    return HashMap.this.size();
                }

                @Override
                public void clear() {
                    HashMap.this.clear();
                }

                @Override
                public Iterator<V> iterator() {
                    return new ValueIterator<K, V>(HashMap.this);
                }
            };
        }
        return valuesCollection;
    }

    /*
     * Contract-related functionality
     */
    static int computeHashCode(Object key) {
        return key.hashCode();
    }

    native static boolean areEqualKeys(Object key1, Object key2);

    // ------------------------------------------------------------------
    // Iteration over the compact layout. Order comes from cn1FirstIndex /
    // cn1NextIndex, which LinkedHashMap overrides with its links.
    // ------------------------------------------------------------------
    static class AbstractMapIterator<K, V> {
        int expectedModCount;
        int futureIndex;
        int currentIndex = -1;
        final HashMap<K, V> associatedMap;

        AbstractMapIterator(HashMap<K, V> hm) {
            associatedMap = hm;
            expectedModCount = hm.modCount;
            futureIndex = hm.cn1FirstIndex();
        }

        public boolean hasNext() {
            return futureIndex >= 0;
        }

        final void checkConcurrentMod() throws ConcurrentModificationException {
            if (expectedModCount != associatedMap.modCount) {
                throw new ConcurrentModificationException();
            }
        }

        final void makeNext() {
            checkConcurrentMod();
            if (futureIndex < 0) {
                throw new NoSuchElementException();
            }
            currentIndex = futureIndex;
            futureIndex = associatedMap.cn1NextIndex(futureIndex);
        }

        public final void remove() {
            checkConcurrentMod();
            if (currentIndex < 0) {
                throw new IllegalStateException();
            }
            associatedMap.cn1RemoveAtIndex(currentIndex);
            currentIndex = -1;
            expectedModCount++;
        }
    }

    static class EntryIterator<K, V> extends AbstractMapIterator<K, V> implements Iterator<Map.Entry<K, V>> {
        EntryIterator(HashMap<K, V> map) {
            super(map);
        }

        public Map.Entry<K, V> next() {
            makeNext();
            return new CompactEntry<K, V>(associatedMap, currentIndex);
        }
    }

    static class KeyIterator<K, V> extends AbstractMapIterator<K, V> implements Iterator<K> {
        KeyIterator(HashMap<K, V> map) {
            super(map);
        }

        @SuppressWarnings("unchecked")
        public K next() {
            makeNext();
            return (K) associatedMap.cn1Keys[currentIndex];
        }
    }

    static class ValueIterator<K, V> extends AbstractMapIterator<K, V> implements Iterator<V> {
        ValueIterator(HashMap<K, V> map) {
            super(map);
        }

        @SuppressWarnings("unchecked")
        public V next() {
            makeNext();
            return (V) associatedMap.cn1Vals[currentIndex];
        }
    }

    /**
     * A live view of one mapping: reads go straight to the map's arrays and
     * {@link #setValue} writes through (standard for map entry views). The view
     * is only guaranteed while the mapping stays in place, exactly like the
     * entry objects of other map implementations under structural change.
     */
    static final class CompactEntry<K, V> implements Map.Entry<K, V> {
        final HashMap<K, V> map;
        final int index;

        CompactEntry(HashMap<K, V> map, int index) {
            this.map = map;
            this.index = index;
        }

        @SuppressWarnings("unchecked")
        public K getKey() {
            return (K) map.cn1Keys[index];
        }

        @SuppressWarnings("unchecked")
        public V getValue() {
            return (V) map.cn1Vals[index];
        }

        public V setValue(V object) {
            V result = getValue();
            map.cn1Vals[index] = object;
            return result;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof Map.Entry) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
                Object k = getKey();
                Object v = getValue();
                return (k == null ? entry.getKey() == null : k.equals(entry.getKey()))
                        && (v == null ? entry.getValue() == null : v.equals(entry.getValue()));
            }
            return false;
        }

        @Override
        public int hashCode() {
            Object k = getKey();
            Object v = getValue();
            return (k == null ? 0 : k.hashCode()) ^ (v == null ? 0 : v.hashCode());
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    static class CompactEntrySet<KT, VT> extends AbstractSet<Map.Entry<KT, VT>> {
        private final HashMap<KT, VT> associatedMap;

        CompactEntrySet(HashMap<KT, VT> hm) {
            associatedMap = hm;
        }

        HashMap<KT, VT> hashMap() {
            return associatedMap;
        }

        @Override
        public int size() {
            return associatedMap.elementCount;
        }

        @Override
        public void clear() {
            associatedMap.clear();
        }

        @Override
        public boolean remove(Object object) {
            if (object instanceof Map.Entry) {
                Map.Entry<?, ?> oEntry = (Map.Entry<?, ?>) object;
                int idx = associatedMap.cn1FindSlotImpl(oEntry.getKey());
                if (idx >= 0 && valuesEq(associatedMap.cn1Vals[idx], oEntry)) {
                    associatedMap.cn1RemoveAtIndex(idx);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean contains(Object object) {
            if (object instanceof Map.Entry) {
                Map.Entry<?, ?> oEntry = (Map.Entry<?, ?>) object;
                int idx = associatedMap.cn1FindSlotImpl(oEntry.getKey());
                return idx >= 0 && valuesEq(associatedMap.cn1Vals[idx], oEntry);
            }
            return false;
        }

        private static boolean valuesEq(Object value, Map.Entry<?, ?> oEntry) {
            return (value == null)
                    ? (oEntry.getValue() == null)
                    : (value == oEntry.getValue() || areEqualKeys(value, oEntry.getValue()));
        }

        @Override
        public Iterator<Map.Entry<KT, VT>> iterator() {
            return new EntryIterator<KT, VT>(associatedMap);
        }
    }
}
