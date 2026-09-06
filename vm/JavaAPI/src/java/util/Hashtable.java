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
 * Hashtable associates keys with values. Both keys and values cannot be null.
 * The size of the Hashtable is the number of key/value pairs it contains. The
 * capacity is the number of key/value pairs the Hashtable can hold. The load
 * factor is a float value which determines how full the Hashtable gets before
 * expanding the capacity. If the load factor of the Hashtable is exceeded, the
 * capacity is doubled.
 * 
 * @see Enumeration
 * @see java.io.Serializable
 * @see java.lang.Object#equals
 * @see java.lang.Object#hashCode
 */

public class Hashtable<K, V> extends Dictionary<K, V> implements Map<K, V> {
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

    private float loadFactor;

    private int threshold;

    transient int modCount;

    private static final java.util.Enumeration<?> EMPTY_ENUMERATION = new java.util.Enumeration<Object>() {
        public boolean hasMoreElements() {
            return false;
        }

        public Object nextElement() {
            throw new NoSuchElementException();
        }
    };

    private static final Iterator<?> EMPTY_ITERATOR = new Iterator<Object>() {

        public boolean hasNext() {
            return false;
        }

        public Object next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new IllegalStateException();
        }
    };

    /**
     * A view of one occupied slot.
     *
     * <p>There are no node objects in this representation, so unlike the
     * chained layout this replaced -- where the Entry WAS the storage and cost
     * an allocation per mapping -- an Entry is built only when an iterator
     * hands one out, and only for {@code entrySet()}. It keeps the table and
     * slot so {@link #setValue} still writes through.
     */
    private static class Entry<K, V> extends MapEntry<K, V> {
        private final Hashtable<K, V> table;

        private final int index;

        @SuppressWarnings("unchecked")
        Entry(Hashtable<K, V> table, int index) {
            super((K) table.cn1Keys[index], (V) table.cn1Vals[index]);
            this.table = table;
            this.index = index;
        }

        @Override
        public V setValue(V object) {
            if (object == null) {
                throw new NullPointerException();
            }
            V result = value;
            value = object;
            table.cn1Vals[index] = object;
            return result;
        }

        @Override
        public String toString() {
            return key + "=" + value; //$NON-NLS-1$
        }
    }

    /**
     * The occupied-slot marker for a key: its spread hash with the sign bit
     * forced on -- strictly negative, so it can never collide with META_EMPTY
     * or META_TOMB and an occupancy test is a single sign check.
     *
     * <p>Null keys are rejected by {@code put}, and every read path reaches
     * {@code key.hashCode()} here, so a null key still faults exactly where it
     * did before rather than being silently accepted.
     */
    static int cn1Marker(Object key) {
        int h = key.hashCode();
        h ^= (h >>> 16);
        return h | 0x80000000;
    }

    /**
     * The next slot on a key's probe path -- the same recurrence
     * {@link HashMap#cn1NextSlot} uses, and for the same reason: linear
     * probing walks a run of occupied slots end to end, which turns a dense
     * key range into an O(n) miss. See the rationale there.
     */
    static int cn1NextSlot(int i, int perturb, int mask) {
        return ((i << 2) + i + 1 + perturb) & mask;
    }

    /** Round up to a power of two, at least 8. */
    static int cn1Capacity(int requested) {
        int cap = 8;
        while (cap < requested && cap < (1 << 30)) {
            cap <<= 1;
        }
        return cap;
    }

    final void cn1Alloc(int capacity) {
        cn1Keys = new Object[capacity];
        cn1Vals = new Object[capacity];
        cn1Meta = new int[capacity];
        elementCount = 0;
        cn1Occupied = 0;
        computeMaxSize();
    }

    /**
     * Probe for a key. Returns the slot index (>= 0) when found; otherwise
     * {@code -(insertionPoint + 1)}, where insertionPoint is the first
     * tombstone on the path (reuse) or the terminating empty slot.
     */
    final int cn1FindSlot(Object key) {
        int marker = cn1Marker(key);
        int[] meta = cn1Meta;
        int mask = meta.length - 1;
        int i = marker & mask;
        int perturb = marker;
        int firstTomb = -1;
        while (true) {
            int m = meta[i];
            if (m == META_EMPTY) {
                return -((firstTomb >= 0 ? firstTomb : i) + 1);
            }
            if (m == marker) {
                Object k = cn1Keys[i];
                if (key == k || key.equals(k)) {
                    return i;
                }
            } else if (m == META_TOMB && firstTomb < 0) {
                firstTomb = i;
            }
            perturb >>>= 5;
            i = cn1NextSlot(i, perturb, mask);
        }
    }

    /** raw insert into a table known not to contain the key (rebuild path). */
    final void cn1Insert(Object key, Object value, int marker) {
        int[] meta = cn1Meta;
        int mask = meta.length - 1;
        int i = marker & mask;
        int perturb = marker;
        while (meta[i] != META_EMPTY) {
            perturb >>>= 5;
            i = cn1NextSlot(i, perturb, mask);
        }
        meta[i] = marker;
        cn1Keys[i] = key;
        cn1Vals[i] = value;
    }

    /** Tombstone a found slot. The single mutation point for removals. */
    final void cn1RemoveAtIndex(int idx) {
        cn1Meta[idx] = META_TOMB;
        cn1Keys[idx] = null;
        cn1Vals[idx] = null;
        elementCount--;
        modCount++;
    }

    private class HashIterator<E> implements Iterator<E> {
        /** next slot to examine. */
        int position;

        /** slot of the entry last returned, or -1. */
        int lastPosition = -1;

        int expectedModCount;

        final MapEntry.Type<E, K, V> type;

        boolean canRemove = false;

        HashIterator(MapEntry.Type<E, K, V> value) {
            type = value;
            position = 0;
            expectedModCount = modCount;
        }

        /** Advance past empty and tombstoned slots. */
        final boolean advance() {
            int[] meta = cn1Meta;
            while (position < meta.length) {
                if (meta[position] < 0) {
                    return true;
                }
                position++;
            }
            return false;
        }

        public boolean hasNext() {
            return advance();
        }

        public E next() {
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (!advance()) {
                throw new NoSuchElementException();
            }
            lastPosition = position;
            position++;
            canRemove = true;
            return type.get(new Entry<K, V>(Hashtable.this, lastPosition));
        }

        public void remove() {
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (!canRemove) {
                throw new IllegalStateException();
            }
            canRemove = false;
            synchronized (Hashtable.this) {
                cn1RemoveAtIndex(lastPosition);
                expectedModCount++;
            }
        }
    }

    /**
     * Constructs a new {@code Hashtable} using the default capacity and load
     * factor.
     */
    public Hashtable() {
        this(11);
    }

    /**
     * Constructs a new {@code Hashtable} using the specified capacity and the
     * default load factor.
     * 
     * @param capacity
     *            the initial capacity.
     */
    public Hashtable(int capacity) {
        if (capacity >= 0) {
            loadFactor = 0.75f;
            cn1Alloc(cn1Capacity(capacity));
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Constructs a new {@code Hashtable} using the specified capacity and load
     * factor.
     * 
     * @param capacity
     *            the initial capacity.
     * @param loadFactor
     *            the initial load factor.
     */
    public Hashtable(int capacity, float loadFactor) {
        if (capacity >= 0 && loadFactor > 0) {
            this.loadFactor = loadFactor;
            cn1Alloc(cn1Capacity(capacity));
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Constructs a new instance of {@code Hashtable} containing the mappings
     * from the specified map.
     * 
     * @param map
     *            the mappings to add.
     */
    public Hashtable(Map<? extends K, ? extends V> map) {
        this(map.size() < 6 ? 11 : (map.size() * 4 / 3) + 11);
        putAll(map);
    }

    /**
     * Removes all key/value pairs from this {@code Hashtable}, leaving the
     * size zero and the capacity unchanged.
     * 
     * @see #isEmpty
     * @see #size
     */
    public synchronized void clear() {
        if (elementCount > 0 || cn1Occupied > 0) {
            Arrays.fill(cn1Keys, null);
            Arrays.fill(cn1Vals, null);
            Arrays.fill(cn1Meta, META_EMPTY);
            elementCount = 0;
            cn1Occupied = 0;
            modCount++;
        }
    }


    /**
     * The occupancy at which the table is rebuilt.
     *
     * <p>Clamped below the capacity, which chaining did not have to do: an open
     * addressed probe terminates on an empty slot, so a completely full table
     * would loop forever. A load factor of 1 or more is legal to ASK for -- the
     * two-argument constructor accepts any positive float -- so the clamp is
     * load-bearing rather than defensive.
     */
    private void computeMaxSize() {
        int capacity = cn1Meta.length;
        threshold = (int) (capacity * loadFactor);
        if (threshold >= capacity) {
            threshold = capacity - 1;
        }
    }

    /**
     * Returns true if this {@code Hashtable} contains the specified object as
     * the value of at least one of the key/value pairs.
     * 
     * @param value
     *            the object to look for as a value in this {@code Hashtable}.
     * @return {@code true} if object is a value in this {@code Hashtable},
     *         {@code false} otherwise.
     * @see #containsKey
     * @see java.lang.Object#equals
     */
    public synchronized boolean contains(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }

        int[] meta = cn1Meta;
        for (int i = 0; i < meta.length; i++) {
            if (meta[i] < 0 && value.equals(cn1Vals[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this {@code Hashtable} contains the specified object as a
     * key of one of the key/value pairs.
     * 
     * @param key
     *            the object to look for as a key in this {@code Hashtable}.
     * @return {@code true} if object is a key in this {@code Hashtable},
     *         {@code false} otherwise.
     * @see #contains
     * @see java.lang.Object#equals
     */
    public synchronized boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    /**
     * Searches this {@code Hashtable} for the specified value.
     * 
     * @param value
     *            the object to search for.
     * @return {@code true} if {@code value} is a value of this
     *         {@code Hashtable}, {@code false} otherwise.
     */
    public boolean containsValue(Object value) {
        return contains(value);
    }

    /**
     * Returns an enumeration on the values of this {@code Hashtable}. The
     * results of the Enumeration may be affected if the contents of this
     * {@code Hashtable} are modified.
     * 
     * @return an enumeration of the values of this {@code Hashtable}.
     * @see #keys
     * @see #size
     * @see Enumeration
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized java.util.Enumeration<V> elements() {
        if (elementCount == 0) {
            return (java.util.Enumeration<V>) EMPTY_ENUMERATION;
        }
        return new HashEnumIterator<V>(new MapEntry.Type<V, K, V>() {
            public V get(MapEntry<K, V> entry) {
                return entry.value;
            }
        }, true);
    }

    /**
     * Returns a set of the mappings contained in this {@code Hashtable}. Each
     * element in the set is a {@link Map.Entry}. The set is backed by this
     * {@code Hashtable} so changes to one are reflected by the other. The set
     * does not support adding.
     * 
     * @return a set of the mappings.
     */
    public Set<Map.Entry<K, V>> entrySet() {
        return new Collections.SynchronizedSet<Map.Entry<K, V>>(
                new AbstractSet<Map.Entry<K, V>>() {
                    @Override
                    public int size() {
                        return elementCount;
                    }

                    @Override
                    public void clear() {
                        Hashtable.this.clear();
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public boolean remove(Object object) {
                        if (contains(object)) {
                            Hashtable.this.remove(((Map.Entry<K, V>) object)
                                    .getKey());
                            return true;
                        }
                        return false;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public boolean contains(Object object) {
                        Entry<K, V> entry = getEntry(((Map.Entry<K, V>) object)
                                .getKey());
                        return object.equals(entry);
                    }

                    @Override
                    public Iterator<Map.Entry<K, V>> iterator() {
                        return new HashIterator<Map.Entry<K, V>>(
                                new MapEntry.Type<Map.Entry<K, V>, K, V>() {
                                    public Map.Entry<K, V> get(
                                            MapEntry<K, V> entry) {
                                        return entry;
                                    }
                                });
                    }
                }, this);
    }

    /**
     * Compares this {@code Hashtable} with the specified object and indicates
     * if they are equal. In order to be equal, {@code object} must be an
     * instance of Map and contain the same key/value pairs.
     * 
     * @param object
     *            the object to compare with this object.
     * @return {@code true} if the specified object is equal to this Map,
     *         {@code false} otherwise.
     * @see #hashCode
     */
    @Override
    public synchronized boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            if (size() != map.size()) {
                return false;
            }

            Set<Map.Entry<K, V>> entries = entrySet();
            Iterator it = map.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>)it.next();
                if (!entries.contains(e)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the value associated with the specified key in this
     * {@code Hashtable}.
     * 
     * @param key
     *            the key of the value returned.
     * @return the value associated with the specified key, or {@code null} if
     *         the specified key does not exist.
     * @see #put
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized V get(Object key) {
        int idx = cn1FindSlot(key);
        return idx < 0 ? null : (V) cn1Vals[idx];
    }

    Entry<K, V> getEntry(Object key) {
        int idx = cn1FindSlot(key);
        return idx < 0 ? null : new Entry<K, V>(this, idx);
    }

    @Override
    public synchronized int hashCode() {
        int result = 0;
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            Object key = entry.getKey();
            if (key == this) {
                continue;
            }
            Object value = entry.getValue();
            if (value == this) {
                continue;
            }
            int hash = (key != null ? key.hashCode() : 0)
                    ^ (value != null ? value.hashCode() : 0);
            result += hash;
        }
        return result;
    }

    /**
     * Returns true if this {@code Hashtable} has no key/value pairs.
     * 
     * @return {@code true} if this {@code Hashtable} has no key/value pairs,
     *         {@code false} otherwise.
     * @see #size
     */
    @Override
    public synchronized boolean isEmpty() {
        return elementCount == 0;
    }

    /**
     * Returns an enumeration on the keys of this {@code Hashtable} instance.
     * The results of the enumeration may be affected if the contents of this
     * {@code Hashtable} are modified.
     * 
     * @return an enumeration of the keys of this {@code Hashtable}.
     * @see #elements
     * @see #size
     * @see Enumeration
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized java.util.Enumeration<K> keys() {
        if (elementCount == 0) {
            return (java.util.Enumeration<K>) EMPTY_ENUMERATION;
        }
        return new HashEnumIterator<K>(new MapEntry.Type<K, K, V>() {
            public K get(MapEntry<K, V> entry) {
                return entry.key;
            }
        }, true);
    }

    /**
     * Returns a set of the keys contained in this {@code Hashtable}. The set
     * is backed by this {@code Hashtable} so changes to one are reflected by
     * the other. The set does not support adding.
     * 
     * @return a set of the keys.
     */
    public Set<K> keySet() {
        return new Collections.SynchronizedSet<K>(new AbstractSet<K>() {
            @Override
            public boolean contains(Object object) {
                return containsKey(object);
            }

            @Override
            public int size() {
                return elementCount;
            }

            @Override
            public void clear() {
                Hashtable.this.clear();
            }

            @Override
            public boolean remove(Object key) {
                if (containsKey(key)) {
                    Hashtable.this.remove(key);
                    return true;
                }
                return false;
            }

            @Override
            public Iterator<K> iterator() {
                if (this.size() == 0) {
                    return (Iterator<K>) EMPTY_ITERATOR;
                }
                return new HashEnumIterator<K>(new MapEntry.Type<K, K, V>() {
                    public K get(MapEntry<K, V> entry) {
                        return entry.key;
                    }
                });
            }
        }, this);
    }

    class HashEnumIterator<E> extends HashIterator<E> implements java.util.Enumeration<E> {

        private boolean isEnumeration = false;

        HashEnumIterator(MapEntry.Type<E, K, V> value) {
            super(value);
        }

        HashEnumIterator(MapEntry.Type<E, K, V> value, boolean isEnumeration) {
            super(value);
            this.isEnumeration = isEnumeration;
        }

        public boolean hasMoreElements() {
            return advance();
        }

        public boolean hasNext() {
            if (isEnumeration) {
                return hasMoreElements();
            }
            return super.hasNext();
        }

        public E next() {
            if (isEnumeration) {
                if (expectedModCount == modCount) {
                    return nextElement();
                }
                throw new ConcurrentModificationException();
            }
            return super.next();
        }

        /**
         * An Enumeration is deliberately NOT fail-fast -- the class docs say
         * its results "may be affected if the contents are modified" -- so this
         * does not consult modCount, matching the chained implementation it
         * replaces. {@link #next} still does.
         */
        public E nextElement() {
            if (isEnumeration) {
                if (!advance()) {
                    throw new NoSuchElementException();
                }
                int idx = position;
                position++;
                return type.get(new Entry<K, V>(Hashtable.this, idx));
            }
            return super.next();
        }

        public void remove() {
            if (isEnumeration) {
                throw new UnsupportedOperationException();
            }
            super.remove();
        }
    }

    /**
     * Associate the specified value with the specified key in this
     * {@code Hashtable}. If the key already exists, the old value is replaced.
     * The key and value cannot be null.
     * 
     * @param key
     *            the key to add.
     * @param value
     *            the value to add.
     * @return the old value associated with the specified key, or {@code null}
     *         if the key did not exist.
     * @see #elements
     * @see #get
     * @see #keys
     * @see java.lang.Object#equals
     */
    @Override
    public synchronized V put(K key, V value) {
        if (key != null && value != null) {
            int idx = cn1FindSlot(key);
            if (idx >= 0) {
                @SuppressWarnings("unchecked")
                V result = (V) cn1Vals[idx];
                cn1Vals[idx] = value;
                return result;
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
            if (cn1Occupied >= threshold) {
                rehash();
            }
            return null;
        }
        throw new NullPointerException();
    }

    /**
     * Copies every mapping to this {@code Hashtable} from the specified map.
     * 
     * @param map
     *            the map to copy mappings from.
     */
    public synchronized void putAll(Map<? extends K, ? extends V> map) {
        Iterator it = map.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<? extends K, ? extends V> entry = (Map.Entry<? extends K, ? extends V>)it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Increases the capacity of this {@code Hashtable}. This method is called
     * when the size of this {@code Hashtable} exceeds the load factor.
     */
    protected void rehash() {
        int capacity = cn1Meta.length;
        // Double only when genuinely full; a table that is merely
        // tombstone-heavy is rebuilt at the same size, which purges them.
        int newCapacity = (elementCount * 2 >= capacity) ? capacity << 1 : capacity;
        if (newCapacity <= 0) {
            newCapacity = capacity;
        }
        Object[] oldKeys = cn1Keys;
        Object[] oldVals = cn1Vals;
        int[] oldMeta = cn1Meta;
        cn1Alloc(newCapacity);
        int count = 0;
        for (int i = 0; i < oldMeta.length; i++) {
            if (oldMeta[i] < 0) {
                // The stored marker is reused, so a rebuild makes no
                // hashCode() calls at all.
                cn1Insert(oldKeys[i], oldVals[i], oldMeta[i]);
                count++;
            }
        }
        elementCount = count;
        cn1Occupied = count;
    }

    /**
     * Removes the key/value pair with the specified key from this
     * {@code Hashtable}.
     * 
     * @param key
     *            the key to remove.
     * @return the value associated with the specified key, or {@code null} if
     *         the specified key did not exist.
     * @see #get
     * @see #put
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized V remove(Object key) {
        int idx = cn1FindSlot(key);
        if (idx < 0) {
            return null;
        }
        V result = (V) cn1Vals[idx];
        cn1RemoveAtIndex(idx);
        return result;
    }

    /**
     * Returns the number of key/value pairs in this {@code Hashtable}.
     * 
     * @return the number of key/value pairs in this {@code Hashtable}.
     * @see #elements
     * @see #keys
     */
    @Override
    public synchronized int size() {
        return elementCount;
    }

    /**
     * Returns the string representation of this {@code Hashtable}.
     * 
     * @return the string representation of this {@code Hashtable}.
     */
    @Override
    public synchronized String toString() {
        if (isEmpty()) {
            return "{}"; //$NON-NLS-1$
        }

        StringBuffer buffer = new StringBuffer(size() * 28);
        buffer.append('{');
        int[] meta = cn1Meta;
        for (int i = 0; i < meta.length; i++) {
            if (meta[i] < 0) {
                Object k = cn1Keys[i];
                Object v = cn1Vals[i];
                if (k != this) {
                    buffer.append(k);
                } else {
                    // luni.04=this Map
                    buffer.append("(this)"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                }
                buffer.append('=');
                if (v != this) {
                    buffer.append(v);
                } else {
                    // luni.04=this Map
                    buffer.append("(this)"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                }
                buffer.append(", "); //$NON-NLS-1$
            }
        }
        // Remove the last ", "
        if (elementCount > 0) {
            buffer.setLength(buffer.length() - 2);
        }
        buffer.append('}');
        return buffer.toString();
    }

    /**
     * Returns a collection of the values contained in this {@code Hashtable}.
     * The collection is backed by this {@code Hashtable} so changes to one are
     * reflected by the other. The collection does not support adding.
     * 
     * @return a collection of the values.
     */
    public Collection<V> values() {
        return new Collections.SynchronizedCollection<V>(
                new AbstractCollection<V>() {
                    @Override
                    public boolean contains(Object object) {
                        return Hashtable.this.contains(object);
                    }

                    @Override
                    public int size() {
                        return elementCount;
                    }

                    @Override
                    public void clear() {
                        Hashtable.this.clear();
                    }

                    @Override
                    public Iterator<V> iterator() {
                        return new HashIterator<V>(
                                new MapEntry.Type<V, K, V>() {
                                    public V get(MapEntry<K, V> entry) {
                                        return entry.value;
                                    }
                                });
                    }
                }, this);
    }
}
