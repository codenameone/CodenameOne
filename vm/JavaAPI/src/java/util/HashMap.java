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

    /**
     * PARALLEL STORAGE, HELD IN C BLOCKS RATHER THAN JAVA ARRAYS. Capacity is always a
     * power of two and lives in {@link #cn1Cap}; there is no array to ask for a length.
     *
     * No Java code outside this class ever sees these, so they do not need to be arrays,
     * and being arrays is expensive: on the self-hosting corpus HashMap's three arrays are
     * 349,806 of the 577,239 live Object[] and 174,903 of the 185,860 live int[], each
     * paying a 32-byte array header, a BiBOP size-class rounding and a slot for the sweep
     * to walk. A block pays a malloc header and is freed the moment the map dies.
     *
     * THE COLLECTOR STILL TRACES THE REFERENCE BLOCKS. ByteCodeClass.NATIVE_REF_BLOCKS
     * makes the generated __GC_MARK_ walk them and the generated __FINALIZER_ free them;
     * the finalize() below is what puts this class on the per-slot reclaim path, without
     * which a page taking the O(1) all-dead reclaim would never free a block.
     */
    transient long cn1KeysBlock;
    transient long cn1ValsBlock;
    transient long cn1MetaBlock;

    /** capacity of all three blocks, always a power of two. */
    transient int cn1Cap;

    // Block primitives. Static and typeless on purpose: the same C helpers back
    // Hashtable and ArrayList when they convert, and a native call that LTO inlines is
    // cheaper than the array access it replaces -- no header, no ->data indirection.
    static native long cn1BlkRefNew(int n);
    static native long cn1BlkIntNew(int n);
    static native void cn1BlkFree(long b);
    /** replace-while-live: the sweep frees it, after any marker walking it has finished. */
    static native void cn1BlkRetire(long b);
    static native Object cn1BlkRefGet(long b, int i);
    static native void cn1BlkRefSet(long b, int i, Object v);
    static native int cn1BlkIntGet(long b, int i);
    static native void cn1BlkIntSet(long b, int i, int v);
    /** scan loop in C: one crossing per call instead of one per slot. */
    static native int cn1BlkNextOccupied(long metaBlock, int from, int cap);
    /** the clear fast path -- two memsets and a barriered blank of the reference blocks. */
    static native void cn1BlkClearAll(long keys, long vals, long meta, int cap);

    /**
     * Frees the three blocks. Declaring it is also what keeps this class off the O(1)
     * all-dead page reclaim (the sweep's needsReclaim test reads finalizerFunction), which
     * is the path on which the generated finalizer -- and therefore the free -- runs.
     */
    protected void finalize() {
        cn1BlkFree(cn1KeysBlock);
        cn1BlkFree(cn1ValsBlock);
        cn1BlkFree(cn1MetaBlock);
        cn1KeysBlock = 0;
        cn1ValsBlock = 0;
        cn1MetaBlock = 0;
    }

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
        cn1BlkFree(cn1KeysBlock);
        cn1BlkFree(cn1ValsBlock);
        cn1BlkFree(cn1MetaBlock);
        cn1KeysBlock = cn1BlkRefNew(capacity);
        cn1ValsBlock = cn1BlkRefNew(capacity);
        cn1MetaBlock = cn1BlkIntNew(capacity);
        cn1Cap = capacity;
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
     *
     * <p>The spread is deliberately the JDK's and deliberately weak: it leaves
     * {@code Integer} keys placed at {@code slot == value}, so a dense key
     * range is stored in ascending slot order and building or scanning one
     * walks memory sequentially. That locality is worth keeping -- what makes
     * it safe here is that the probe SEQUENCE no longer walks neighbours.
     * See {@link #cn1NextSlot}.
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
     * The next slot on a key's probe path.
     *
     * <p><b>Not {@code i + 1}.</b> Linear probing puts colliding keys in
     * NEIGHBOURING slots, so a run of occupied slots is walked end to end by
     * any probe that enters it and does not find its key. With the weak
     * spread above, {@code Integer} keys from a dense range form exactly one
     * such run -- 100k sequential keys occupy slots [0, 99999] with no gap --
     * and every lookup that MISSES inside it walked the whole thing. Measured
     * average probe length for a miss, before this: 2547 slots at 20k keys,
     * 16742 at 100k, 222721 at 1M. That is O(n) per unsuccessful lookup, and
     * it reached {@code get} returning null, {@code containsKey} returning
     * false, and {@code put} of a key that is not adjacent to the run.
     *
     * <p>Hits stayed at exactly one probe throughout, which is why a hit-only
     * churn benchmark never saw it.
     *
     * <p>This is CPython's dict recurrence. The first probe is still
     * {@code marker & mask}, so the sequential-key locality above survives
     * untouched; every probe after it jumps pseudo-randomly, so a miss leaves
     * the run immediately instead of walking it (1.4 to 2.0 probes at the
     * sizes above). {@code perturb} decays to zero within seven steps, after
     * which {@code 5 * i + 1} is a full-period permutation of a power-of-two
     * table (Hull-Dobell: 1 is coprime to 2^k and 4 divides 5 - 1), so the
     * walk always reaches the empty slot that terminates it.
     *
     * @param i the current slot
     * @param perturb the decaying perturbation, seeded with the marker and
     *        shifted down by the caller on every step
     * @param mask {@code capacity - 1}
     */
    static int cn1NextSlot(int i, int perturb, int mask) {
        return ((i << 2) + i + 1 + perturb) & mask;
    }

    /**
     * Probe for a key. Returns the slot index (>= 0) when found; otherwise
     * {@code -(insertionPoint + 1)} where insertionPoint is the first
     * tombstone met on the probe path (reuse), or the terminating empty slot.
     */
    final int cn1FindSlotImpl(Object key) {
        int marker = cn1Marker(key);
        long meta = cn1MetaBlock;
        int mask = cn1Cap - 1;
        int i = marker & mask;
        int perturb = marker;
        int firstTomb = -1;
        while (true) {
            int m = cn1BlkIntGet(meta, i);
            if (m == META_EMPTY) {
                int ins = firstTomb >= 0 ? firstTomb : i;
                return -(ins + 1);
            }
            if (m == marker) {
                Object k = cn1BlkRefGet(cn1KeysBlock, i);
                if (key == null ? k == null : (key == k || areEqualKeys(key, k))) {
                    return i;
                }
            } else if (m == META_TOMB && firstTomb < 0) {
                firstTomb = i;
            }
            perturb >>>= 5;
            i = cn1NextSlot(i, perturb, mask);
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
            V old = (V) cn1BlkRefGet(cn1ValsBlock, idx);
            cn1BlkRefSet(cn1ValsBlock, idx, value);
            cn1LastPut = idx;
            cn1LastInserted = false;
            return old;
        }
        int ins = -idx - 1;
        boolean wasEmpty = cn1BlkIntGet(cn1MetaBlock, ins) == META_EMPTY;
        cn1BlkIntSet(cn1MetaBlock, ins, cn1Marker(key));
        cn1BlkRefSet(cn1KeysBlock, ins, key);
        cn1BlkRefSet(cn1ValsBlock, ins, value);
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
        int cap = cn1Cap;
        // Grow when the LIVE count has reached the threshold; rebuild at the
        // same size only when the threshold was reached because of TOMBSTONES.
        //
        // Testing capacity directly (elementCount * 2 >= cap) silently assumed
        // a load factor of 0.5 or more. Below that the threshold is reached
        // while the table is still less than half full, so the rebuild kept the
        // same capacity, the rebuilt table was immediately at its threshold
        // again, and every subsequent put rebuilt the whole table: inserting
        // 20000 entries at a load factor of 0.25 did 19999 rebuilds and
        // rehashed 200 million entries. The two-argument constructor accepts
        // any positive load factor, so this was reachable from ordinary code.
        // At 0.75 and 0.5 the two rules agree exactly, rebuild for rebuild.
        int newCap = (elementCount >= threshold) ? cap << 1 : cap;
        // REHASH OUT OF THE CURRENT BLOCKS, THEN SWAP -- not the other way round.
        //
        // The array version could allocate first and rehash from locals, because the old
        // arrays were heap objects the collector still traced. A block is traced only
        // while a FIELD of this map points at it, so allocating into the fields first
        // would leave the old block holding the only reference to every key and value
        // while it is no longer traced -- collectible mid-rehash. Reading from the fields
        // and writing into locals keeps the source traced for the whole loop, and every
        // reference written into the new blocks is also still in the old ones, so nothing
        // is unreachable at any point.
        long newKeys = cn1BlkRefNew(newCap);
        long newVals = cn1BlkRefNew(newCap);
        long newMeta = cn1BlkIntNew(newCap);
        int newMask = newCap - 1;
        int count = 0;
        for (int i = 0; i < cap; i++) {
            int m = cn1BlkIntGet(cn1MetaBlock, i);
            if (m < 0) {
                int j = m & newMask;
                int perturb = m;
                while (cn1BlkIntGet(newMeta, j) != META_EMPTY) {
                    perturb >>>= 5;
                    j = cn1NextSlot(j, perturb, newMask);
                }
                cn1BlkIntSet(newMeta, j, m);
                cn1BlkRefSet(newKeys, j, cn1BlkRefGet(cn1KeysBlock, i));
                cn1BlkRefSet(newVals, j, cn1BlkRefGet(cn1ValsBlock, i));
                count++;
            }
        }
        long oldK = cn1KeysBlock, oldV = cn1ValsBlock, oldM = cn1MetaBlock;
        cn1KeysBlock = newKeys;
        cn1ValsBlock = newVals;
        cn1MetaBlock = newMeta;
        cn1Cap = newCap;
        threshold = (int) (newCap * loadFactor);
        if (threshold >= newCap) {
            threshold = newCap - 1;
        }
        // RETIRE, not free: a marker that loaded these pointers before the swap above
        // may still be walking them. The sweep releases them once the mark has ended.
        cn1BlkRetire(oldK);
        cn1BlkRetire(oldV);
        cn1BlkRetire(oldM);
        elementCount = count;
        cn1Occupied = count;
    }

    /** raw insert into a table known not to contain the key (rebuild path). */
    final int cn1Insert(Object key, Object value, int marker) {
        long meta = cn1MetaBlock;
        int mask = cn1Cap - 1;
        int i = marker & mask;
        int perturb = marker;
        while (cn1BlkIntGet(meta, i) != META_EMPTY) {
            perturb >>>= 5;
            i = cn1NextSlot(i, perturb, mask);
        }
        cn1BlkIntSet(meta, i, marker);
        cn1BlkRefSet(cn1KeysBlock, i, key);
        cn1BlkRefSet(cn1ValsBlock, i, value);
        return i;
    }

    /**
     * Clears a found slot (tombstone it). Virtual so LinkedHashMap can unlink
     * first; ALWAYS the single mutation point for removals (iterators too).
     */
    void cn1RemoveAtIndex(int idx) {
        cn1BlkIntSet(cn1MetaBlock, idx, META_TOMB);
        cn1BlkRefSet(cn1KeysBlock, idx, null);
        cn1BlkRefSet(cn1ValsBlock, idx, null);
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
        return cn1BlkNextOccupied(cn1MetaBlock, from, cn1Cap);
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
            cn1BlkClearAll(cn1KeysBlock, cn1ValsBlock, cn1MetaBlock, cn1Cap);
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
        long meta = cn1MetaBlock;
        long vals = cn1ValsBlock;
        if (value != null) {
            for (int i = 0; i < cn1Cap; i++) {
                if (cn1BlkIntGet(meta, i) < 0) {
                    Object v = cn1BlkRefGet(vals, i);
                    if (value == v || areEqualKeys(value, v)) {
                        return true;
                    }
                }
            }
        } else {
            for (int i = 0; i < cn1Cap; i++) {
                if (cn1BlkIntGet(meta, i) < 0 && cn1BlkRefGet(vals, i) == null) {
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
        V v = (V) cn1BlkRefGet(cn1ValsBlock, idx);
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
        V old = (V) cn1BlkRefGet(cn1ValsBlock, idx);
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
            return (K) cn1BlkRefGet(associatedMap.cn1KeysBlock, currentIndex);
        }
    }

    static class ValueIterator<K, V> extends AbstractMapIterator<K, V> implements Iterator<V> {
        ValueIterator(HashMap<K, V> map) {
            super(map);
        }

        @SuppressWarnings("unchecked")
        public V next() {
            makeNext();
            return (V) cn1BlkRefGet(associatedMap.cn1ValsBlock, currentIndex);
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
            return (K) cn1BlkRefGet(map.cn1KeysBlock, index);
        }

        @SuppressWarnings("unchecked")
        public V getValue() {
            return (V) cn1BlkRefGet(map.cn1ValsBlock, index);
        }

        public V setValue(V object) {
            V result = getValue();
            cn1BlkRefSet(map.cn1ValsBlock, index, object);
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
                if (idx >= 0 && valuesEq(cn1BlkRefGet(associatedMap.cn1ValsBlock, idx), oEntry)) {
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
                return idx >= 0 && valuesEq(cn1BlkRefGet(associatedMap.cn1ValsBlock, idx), oEntry);
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
