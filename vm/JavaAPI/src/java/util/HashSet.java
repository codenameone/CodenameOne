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
    // backingMap SURVIVES FOR LinkedHashSet, which extends this class and passes a
    // LinkedHashMap because it needs that map's ordering links.
    transient HashMap<E, HashSet<E>> backingMap;

    // volatile to match HashMap's blocks: the concurrent marker reads these while the
    // mutator may be swapping them during a rebuild.
    transient volatile long cn1KeysBlock;
    transient volatile long cn1MetaBlock;
    transient int cn1Cap;
    transient int cn1Size;
    transient int cn1Occupied;
    transient int cn1Threshold;
    transient int cn1ModCount;

    private static final int DEFAULT_CAPACITY = 16;

    private native boolean cn1AddNative(Object element);
    private native boolean cn1ContainsNative(Object element);
    private native boolean cn1RemoveNative(Object element);
    private native void cn1ClearNative();
    private native int cn1NextOccupied(int from);
    private native Object cn1ElementAt(int index);
    private native void cn1RemoveSlot(int index);

    public HashSet() { this(DEFAULT_CAPACITY, 0.75f); }

    public HashSet(int capacity) { this(capacity, 0.75f); }

    public HashSet(int capacity, float loadFactor) {
        if (capacity < 0) throw new IllegalArgumentException();
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) throw new IllegalArgumentException();
        int cap = DEFAULT_CAPACITY;
        while (cap < capacity) cap <<= 1;
        cn1Cap = cap;
        cn1Threshold = (int) (cap * 0.75f);
        if (cn1Threshold >= cap) cn1Threshold = cap - 1;
    }

    public HashSet(Collection<? extends E> collection) {
        this(collection.size() < 6 ? 11 : collection.size() * 2, 0.75f);
        Iterator it = collection.iterator();
        while(it.hasNext()) {
            add((E)it.next());
        }
    }

    /** Map-backed construction. Used by LinkedHashSet, which needs ordering links. */
    HashSet(HashMap<E, HashSet<E>> backingMap) {
        this.backingMap = backingMap;
    }

    @Override
    public boolean add(E object) {
        return backingMap != null ? backingMap.put(object, this) == null : cn1AddNative(object);
    }

    @Override
    public void clear() {
        if (backingMap != null) backingMap.clear(); else cn1ClearNative();
    }

    @Override
    public boolean contains(Object object) {
        return backingMap != null ? backingMap.containsKey(object) : cn1ContainsNative(object);
    }

    @Override
    public boolean isEmpty() { return size() == 0; }

    @Override
    public Iterator<E> iterator() {
        return backingMap != null ? backingMap.keySet().iterator() : new HashSetIterator();
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
        return backingMap != null ? backingMap.remove(object) != null : cn1RemoveNative(object);
    }

    @Override
    public int size() { return backingMap != null ? backingMap.size() : cn1Size; }

    HashMap<E, HashSet<E>> createBackingMap(int capacity, float loadFactor) {
        return new HashMap<E, HashSet<E>>(capacity, loadFactor);
    }
}
