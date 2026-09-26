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
 * HashSet is an implementation of a Set. All optional operations (adding and
 * removing) are supported. The elements can be any objects.
 */
public class HashSet<E> extends AbstractSet<E> implements Set<E> {

    // A SET DOES NOT NEED A MAP. Every HashSet used to be a HashMap with the set
    // stored as every value, so each paid for a map object AND for that map's VALUES
    // reference array, which held one pointer repeated. Measured on the
    // HelloCodenameOne corpus: 155,787 live HashSets against 366,554 live HashMaps --
    // 42% of every map in the heap existed only to back a set.
    //
    // The table lives in the native heap and EVERY operation is a single C call (see
    // the CN1_COLL_SET block in nativeMethods.m). That is not just faster than a Java
    // probe over per-element native calls, it is the only correct shape: separate calls
    // leave a collection window between them, and during a rebuild that lets the marker
    // see a half-filled table while elements still in the old one are reachable from
    // nothing.
    //
    // The map-backed form lives in LinkedHashSet, NOT here. It is the only
    // subclass that needs one -- for the ordering links -- and holding the field
    // here charged every plain HashSet 8 bytes for it, which is not slack: with
    // BiBOP size classes it moved java.util.HashSet from the 56-byte class to the
    // 64-byte class, and a translation of the 5,326-class corpus allocates 432,372
    // of them. It also put a load and a branch on `backingMap != null` in front of
    // add, contains, remove, clear, size and iterator -- on every call, for a field
    // that is null in every plain HashSet ever made. LinkedHashSet overrides those
    // six instead, which is both smaller and faster here and no slower there.

    // volatile to match HashMap's table: the concurrent marker reads it while the
    // mutator may be swapping it during a rebuild. The slot markers follow the elements
    // in the same allocation (cn1SetTableAlloc), so this is the set's only table field.
    transient volatile long cn1KeysBlock;
    transient int cn1Cap;
    transient int cn1Size;
    transient int cn1Occupied;
    transient int cn1ModCount;

    // The smallest table, and the default. 2 rather than 16 for the reason given at
    // HashMap.DEFAULT_SIZE: 145k HashSets are live at the self-hosting peak, 32k of them
    // holding one element. The threshold (cn1HsThreshold in nativeMethods.m) is clamped to
    // cap - 1, so a 2-slot table holds that one element and keeps the empty slot probing
    // requires.
    private static final int DEFAULT_CAPACITY = 2;

    private native boolean cn1AddNative(Object element);
    private native boolean cn1ContainsNative(Object element);
    private native boolean cn1RemoveNative(Object element);
    private native void cn1ClearNative();
    private native int cn1NextOccupied(int from);
    private native Object cn1ElementAt(int index);
    private native void cn1RemoveSlot(int index);

    // THE JAVASCRIPT PORT'S IMPLEMENTATION of the seven natives above. The JS runtime
    // binds each native to its twin here (parparvm_runtime.js), the same way HashMap's
    // natives delegate to getImpl/putImpl; nothing in bytecode calls these, so they are
    // retention roots in JavascriptNativeRegistry and JavascriptReachability. Plain
    // linear probing over a reference block in cn1KeysBlock: deletion leaves a
    // tombstone rather than shifting, so removing through an iterator never moves an
    // element the iterator has not reached yet. The C targets never call these.
    // Created on first use rather than by an initializer, so HashSet gains no static
    // initializer on the targets that never run this code.
    private static Object CN1_JS_NULL;
    private static Object CN1_JS_DELETED;

    private static Object cn1JsKey(Object element) {
        if (CN1_JS_NULL == null) {
            CN1_JS_NULL = new Object();
            CN1_JS_DELETED = new Object();
        }
        return element == null ? CN1_JS_NULL : element;
    }

    private int cn1JsFind(Object key) {
        int cap = NativeStorage.capacity(cn1KeysBlock);
        if (cap == 0) {
            return -1;
        }
        int slot = (key.hashCode() & 0x7fffffff) % cap;
        for (int probes = 0; probes < cap; probes++) {
            Object at = NativeStorage.get(cn1KeysBlock, slot);
            if (at == null) {
                return -1;
            }
            if (at != CN1_JS_DELETED && (at == key || key.equals(at))) {
                return slot;
            }
            slot = slot + 1 == cap ? 0 : slot + 1;
        }
        return -1;
    }

    private void cn1JsGrow() {
        int oldCap = NativeStorage.capacity(cn1KeysBlock);
        long old = cn1KeysBlock;
        int cap = Math.max(cn1Cap < 4 ? 4 : cn1Cap, oldCap * 2);
        cn1KeysBlock = NativeStorage.references(cap);
        cn1Occupied = 0;
        for (int i = 0; i < oldCap; i++) {
            Object at = NativeStorage.get(old, i);
            if (at != null && at != CN1_JS_DELETED) {
                int slot = (at.hashCode() & 0x7fffffff) % cap;
                while (NativeStorage.get(cn1KeysBlock, slot) != null) {
                    slot = slot + 1 == cap ? 0 : slot + 1;
                }
                NativeStorage.set(cn1KeysBlock, slot, at);
                cn1Occupied++;
            }
        }
    }

    boolean cn1AddImpl(Object element) {
        Object key = cn1JsKey(element);
        if (cn1JsFind(key) >= 0) {
            return false;
        }
        int cap = NativeStorage.capacity(cn1KeysBlock);
        if (cap == 0 || (cn1Occupied + 1) * 4 > cap * 3) {
            cn1JsGrow();
            cap = NativeStorage.capacity(cn1KeysBlock);
        }
        int slot = (key.hashCode() & 0x7fffffff) % cap;
        while (true) {
            Object at = NativeStorage.get(cn1KeysBlock, slot);
            if (at == null || at == CN1_JS_DELETED) {
                if (at == null) {
                    cn1Occupied++;
                }
                break;
            }
            slot = slot + 1 == cap ? 0 : slot + 1;
        }
        NativeStorage.set(cn1KeysBlock, slot, key);
        cn1Size++;
        cn1ModCount++;
        return true;
    }

    boolean cn1ContainsImpl(Object element) {
        return cn1JsFind(cn1JsKey(element)) >= 0;
    }

    boolean cn1RemoveImpl(Object element) {
        int slot = cn1JsFind(cn1JsKey(element));
        if (slot < 0) {
            return false;
        }
        cn1RemoveSlotImpl(slot);
        return true;
    }

    void cn1ClearImpl() {
        int cap = NativeStorage.capacity(cn1KeysBlock);
        if (cap > 0) {
            NativeStorage.clear(cn1KeysBlock, 0, cap);
        }
        cn1Size = 0;
        cn1Occupied = 0;
        cn1ModCount++;
    }

    int cn1NextOccupiedImpl(int from) {
        cn1JsKey(null);
        int cap = NativeStorage.capacity(cn1KeysBlock);
        for (int i = from < 0 ? 0 : from; i < cap; i++) {
            Object at = NativeStorage.get(cn1KeysBlock, i);
            if (at != null && at != CN1_JS_DELETED) {
                return i;
            }
        }
        return -1;
    }

    Object cn1ElementAtImpl(int index) {
        cn1JsKey(null);
        Object at = NativeStorage.get(cn1KeysBlock, index);
        return at == CN1_JS_NULL ? null : at;
    }

    void cn1RemoveSlotImpl(int index) {
        cn1JsKey(null);
        NativeStorage.set(cn1KeysBlock, index, CN1_JS_DELETED);
        cn1Size--;
        cn1ModCount++;
    }

    public HashSet() { this(DEFAULT_CAPACITY, 0.75f); }

    public HashSet(int capacity) { this(capacity, 0.75f); }

    public HashSet(int capacity, float loadFactor) {
        if (capacity < 0) throw new IllegalArgumentException();
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) throw new IllegalArgumentException();
        int cap = DEFAULT_CAPACITY;
        while (cap < capacity) cap <<= 1;
        /* No threshold field. It was always (int)(cap * 0.75f) -- the loadFactor
         * argument is validated and then ignored -- and for the only capacities this
         * table ever has, powers of two starting at 4, that is exactly
         * cap - (cap >> 2). Deriving it costs a shift and a subtract where the field
         * cost 4 bytes on every set, which is what took java.util.HashSet from the
         * 48-byte BiBOP slot class into the 64-byte one. See cn1HsThreshold in
         * nativeMethods.m, which also keeps a 2-slot table's empty slot. */
        cn1Cap = cap;
    }

    public HashSet(Collection<? extends E> collection) {
        this(collection.size() < 6 ? 11 : collection.size() * 2, 0.75f);
        Iterator it = collection.iterator();
        while(it.hasNext()) {
            add((E)it.next());
        }
    }

    /**
     * Subclass-delegating construction: allocates NO native table, because the
     * subclass keeps the elements somewhere else. The flag is not stored -- it
     * exists to distinguish this from HashSet(int), which would otherwise be the
     * same call after erasure of the default capacity.
     *
     * @param mapBacked always true; present to select this constructor
     */
    HashSet(boolean mapBacked) {
    }

    @Override
    public boolean add(E object) {
        return cn1AddNative(object);
    }

    @Override
    public void clear() {
        cn1ClearNative();
    }

    @Override
    public boolean contains(Object object) {
        return cn1ContainsNative(object);
    }

    @Override
    public boolean isEmpty() { return size() == 0; }

    @Override
    public Iterator<E> iterator() {
        return new HashSetIterator();
    }

    private final class HashSetIterator implements Iterator<E> {
        private int cursor;
        private int lastReturned = -1;
        private int expectedModCount = cn1ModCount;

        public boolean hasNext() { return cn1NextOccupied(cursor) >= 0; }

        public E next() {
            if (expectedModCount != cn1ModCount) throw new ConcurrentModificationException();
            int slot = cn1NextOccupied(cursor);
            if (slot < 0) throw new NoSuchElementException();
            lastReturned = slot;
            cursor = slot + 1;
            return (E) cn1ElementAt(slot);
        }

        public void remove() {
            if (lastReturned < 0) throw new IllegalStateException();
            if (expectedModCount != cn1ModCount) throw new ConcurrentModificationException();
            cn1RemoveSlot(lastReturned);
            expectedModCount = cn1ModCount;
            lastReturned = -1;
        }
    }

    @Override
    public boolean remove(Object object) {
        return cn1RemoveNative(object);
    }

    @Override
    public int size() { return cn1Size; }

}
