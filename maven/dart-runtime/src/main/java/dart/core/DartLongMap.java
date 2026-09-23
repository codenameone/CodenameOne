/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package dart.core;

import dart.runtime.Funcs;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A primitive {@code long}&rarr;{@code long}, insertion-ordered map &mdash; the transpiler
 * targets this for Dart {@code Map<int, int>}. The hot paths ({@link #putLong(long, long)},
 * {@link #getLongOr(long, long)}, {@link #containsKeyLong(long)}) never box; the generic
 * {@link java.util.Map} surface (used for CN1 interop and the rare bare {@code m[k]} read)
 * boxes only at that boundary.
 *
 * <p>Storage mirrors Dart's own {@code _CompactLinkedHashMap}: an open-addressing {@code int}
 * hash index into a single insertion-ordered {@code data} array that holds each entry's key
 * and value INTERLEAVED (key at {@code 2e}, value at {@code 2e+1}). Interleaving is the point:
 * a lookup that finds a key at {@code data[2e]} then reads its value from {@code data[2e+1]} in
 * the SAME cache line, instead of a second random miss into a separate {@code vals[]} array.
 * Removal marks the entry as a hole and tombstones its index slot; holes are compacted away
 * when they dominate.</p>
 */
public final class DartLongMap extends AbstractMap<Long, Long> {

    private static final int EMPTY = -1;
    private static final int DELETED = -2;

    private long[] data;         // interleaved [key0, val0, key1, val1, ...] in insertion order
    private boolean[] present;   // present[e] false == hole left by a removal
    private int entryCount;      // appended entries incl. holes
    private int liveSize;        // live (non-hole) entries
    private int[] index;         // hash slot -> entry index (or EMPTY/DELETED)
    private int mask;            // index.length - 1 (index length is a power of two)

    // The probe reads index[slot] (compact int table, cache-friendly) then confirms data[2e]. It
    // does NOT re-check present[e]: index[slot] >= 0 already implies present[e] == true (appendEntry
    // sets both; removeLong sets index[slot]=DELETED and rehash rebuilds index only from present
    // entries), so the boolean[] load -- a separate random cache line at e -- is pure overhead and is
    // dropped. (A slot-local key/value mirror was tried and REVERTED: it 5x'd the table memory and
    // the cache/GC pressure on a large map outweighed avoiding the chase; interleaving key+value is
    // the memory-neutral win instead -- it removes the second random miss without adding any array.)
    /// Identity, like {@link DartMap}: Dart maps compare by identity unless a type
    /// overrides {@code operator ==}. The emitter specialises a Map&lt;int, int&gt; to
    /// this class, which inherited AbstractMap's structural equality, so two equal
    /// literals compared == and collapsed into one key where Dart keeps two.
    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    /// Consistent with {@link #equals}: identity.
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    public DartLongMap() {
        data = new long[16];       // 8 entries * 2 slots
        present = new boolean[8];
        index = new int[16];
        java.util.Arrays.fill(index, EMPTY);
        mask = index.length - 1;
    }

    /** Dart's {@code Map<int,int>.of}/{@code .from}: a shallow copy of {@code src}. */
    public static DartLongMap from(java.util.Map<? extends Number, ? extends Number> src) {
        DartLongMap m = new DartLongMap();
        if (src != null) {
            for (java.util.Map.Entry<? extends Number, ? extends Number> e : src.entrySet()) {
                m.putLong(e.getKey().longValue(), e.getValue().longValue());
            }
        }
        return m;
    }

    /** Literal helper for Dart's &lt;int, int&gt;{a: b, ...}. Pairs are key0, val0, key1, val1, ... */
    public static DartLongMap ofLongs(long... pairs) {
        DartLongMap m = new DartLongMap();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            m.putLong(pairs[i], pairs[i + 1]);
        }
        return m;
    }

    private static int hash(long k) {
        // SplitMix64-style finaliser so sequential keys (a common Dart case) spread across slots.
        long z = k;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        z = z ^ (z >>> 31);
        return (int) z;
    }

    /** Returns the entry index for key, or -1 if absent. */
    private int find(long key) {
        int slot = hash(key) & mask;
        while (true) {
            int e = index[slot];
            if (e == EMPTY) {
                return -1;
            }
            if (e != DELETED && data[e << 1] == key) {
                return e;
            }
            slot = (slot + 1) & mask;
        }
    }

    // --- primitive fast paths (transpiler targets these for Map<int, int>) ---

    /** Dart's m[key] = value; returns value so it composes in expression position. */
    public long putLong(long key, long value) {
        int slot = hash(key) & mask;
        int firstDeleted = -1;
        while (true) {
            int e = index[slot];
            if (e == EMPTY) {
                int target = firstDeleted >= 0 ? firstDeleted : slot;
                appendEntry(target, key, value);
                return value;
            }
            if (e == DELETED) {
                if (firstDeleted < 0) {
                    firstDeleted = slot;
                }
            } else if (data[e << 1] == key) {
                data[(e << 1) + 1] = value;
                return value;
            }
            slot = (slot + 1) & mask;
        }
    }

    private void appendEntry(int slot, long key, long value) {
        if (entryCount == present.length) {
            data = java.util.Arrays.copyOf(data, data.length * 2);
            present = java.util.Arrays.copyOf(present, present.length * 2);
        }
        int e = entryCount++;
        data[e << 1] = key;
        data[(e << 1) + 1] = value;
        present[e] = true;
        index[slot] = e;
        liveSize++;
        // Keep the index table under 0.75 load (live + tombstones vs capacity).
        if ((entryCount) * 4 >= index.length * 3) {
            rehash();
        }
    }

    private void rehash() {
        // Compact holes out of the insertion array first if they dominate, preserving order.
        if (entryCount - liveSize > (liveSize >> 1)) {
            int w = 0;
            for (int r = 0; r < entryCount; r++) {
                if (present[r]) {
                    data[w << 1] = data[r << 1];
                    data[(w << 1) + 1] = data[(r << 1) + 1];
                    present[w] = true;
                    w++;
                }
            }
            for (int i = w; i < entryCount; i++) {
                present[i] = false;
            }
            entryCount = w;
        }
        // Smallest power of two keeping the index table under ~0.5 load (avoids
        // Integer.highestOneBit, which the ParparVM minimal JavaAPI lacks).
        int newLen = 16;
        int want = entryCount * 2;
        while (newLen < want) {
            newLen <<= 1;
        }
        index = new int[newLen];
        java.util.Arrays.fill(index, EMPTY);
        mask = newLen - 1;
        for (int e = 0; e < entryCount; e++) {
            if (!present[e]) {
                continue;
            }
            int slot = hash(data[e << 1]) & mask;
            while (index[slot] != EMPTY) {
                slot = (slot + 1) & mask;
            }
            index[slot] = e;
        }
    }

    /** Dart's m[key] ?? orElse without boxing. Value shares the key's cache line (interleaved). */
    public long getLongOr(long key, long orElse) {
        int e = find(key);
        return e < 0 ? orElse : data[(e << 1) + 1];
    }

    public boolean containsKeyLong(long key) {
        return find(key) >= 0;
    }

    /** Boxed read for the bare Dart {@code m[key]} (returns null when absent). */
    public Long idxLong(long key) {
        int e = find(key);
        return e < 0 ? null : data[(e << 1) + 1];
    }

    public long removeLong(long key) {
        int slot = hash(key) & mask;
        while (true) {
            int e = index[slot];
            if (e == EMPTY) {
                return 0;
            }
            if (e != DELETED && data[e << 1] == key) {
                long old = data[(e << 1) + 1];
                present[e] = false;
                index[slot] = DELETED;
                liveSize--;
                return old;
            }
            slot = (slot + 1) & mask;
        }
    }

    // --- Dart map surface ---

    /**
     * Dart's {@code Map.remove(key)}: removes the entry and returns its former
     * value, or null when the key was absent.
     */
    public Long removeDart(long key) {
        int e = find(key);
        if (e < 0) {
            return null;
        }
        Long old = data[(e << 1) + 1];
        removeLong(key);
        return old;
    }

    public Long idx(long key) {
        return idxLong(key);
    }

    public long length() {
        return liveSize;
    }

    public boolean isNotEmpty() {
        return liveSize != 0;
    }

    public boolean isEmptyDart() {
        return liveSize == 0;
    }

    public DartIterable<Long> keys() {
        return DartIterable.wrap(new Iterable<Long>() {
            public Iterator<Long> iterator() {
                return keyIterator();
            }
        });
    }

    public DartIterable<Long> valuesIterable() {
        return DartIterable.wrap(new Iterable<Long>() {
            public Iterator<Long> iterator() {
                return valueIterator();
            }
        });
    }

    public void forEachDart(Funcs.VoidFunc2<Long, Long> action) {
        for (int e = 0; e < entryCount; e++) {
            if (present[e]) {
                action.call(data[e << 1], data[(e << 1) + 1]);
            }
        }
    }

    // --- java.util.Map interop (boxed boundary) ---

    @Override
    public int size() {
        return liveSize;
    }

    @Override
    public boolean isEmpty() {
        return liveSize == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof Long && containsKeyLong((Long) key);
    }

    @Override
    public Long get(Object key) {
        return key instanceof Long ? idxLong((Long) key) : null;
    }

    @Override
    public Long put(Long key, Long value) {
        Long old = idxLong(key);
        putLong(key, value);
        return old;
    }

    @Override
    public Long remove(Object key) {
        if (!(key instanceof Long)) {
            return null;
        }
        long k = (Long) key;
        int e = find(k);
        if (e < 0) {
            return null;
        }
        Long old = data[(e << 1) + 1];
        removeLong(k);
        return old;
    }

    @Override
    public void clear() {
        entryCount = 0;
        liveSize = 0;
        java.util.Arrays.fill(index, EMPTY);
        // present flags below entryCount are reset lazily as entries are re-appended;
        // clear the live prefix so stale holes never read as present.
        java.util.Arrays.fill(present, false);
    }

    private Iterator<Long> keyIterator() {
        return new Iterator<Long>() {
            int e = nextLive(0);
            public boolean hasNext() { return e < entryCount; }
            public Long next() {
                if (e >= entryCount) { throw new NoSuchElementException(); }
                long k = data[e << 1]; e = nextLive(e + 1); return k;
            }
        };
    }

    private Iterator<Long> valueIterator() {
        return new Iterator<Long>() {
            int e = nextLive(0);
            public boolean hasNext() { return e < entryCount; }
            public Long next() {
                if (e >= entryCount) { throw new NoSuchElementException(); }
                long v = data[(e << 1) + 1]; e = nextLive(e + 1); return v;
            }
        };
    }

    private int nextLive(int from) {
        int e = from;
        while (e < entryCount && !present[e]) {
            e++;
        }
        return e;
    }

    @Override
    public Set<Map.Entry<Long, Long>> entrySet() {
        return new AbstractSet<Map.Entry<Long, Long>>() {
            public int size() {
                return liveSize;
            }
            public Iterator<Map.Entry<Long, Long>> iterator() {
                return new Iterator<Map.Entry<Long, Long>>() {
                    int e = nextLive(0);
                    public boolean hasNext() {
                        return e < entryCount;
                    }
                    public Map.Entry<Long, Long> next() {
                        if (e >= entryCount) {
                            throw new NoSuchElementException();
                        }
                        Map.Entry<Long, Long> en =
                                new AbstractMap.SimpleImmutableEntry<Long, Long>(data[e << 1], data[(e << 1) + 1]);
                        e = nextLive(e + 1);
                        return en;
                    }
                };
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int e = 0; e < entryCount; e++) {
            if (!present[e]) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(data[e << 1]).append(": ").append(data[(e << 1) + 1]);
        }
        return sb.append("}").toString();
    }
}
