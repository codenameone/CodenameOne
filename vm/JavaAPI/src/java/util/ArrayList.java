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

/** A resizable sequence whose private backing buffer is owned by the VM. */
public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess {
    private static final long serialVersionUID = 8683452581122892189L;
    /* FIELD ORDER IS LOAD BEARING, and there is no separate capacity field.
     *
     * The object was 40 bytes: 16 of header, then AbstractList's modCount, then 4
     * bytes of padding to align the 8-byte storage handle, then the handle, then
     * capacity and size. Declaring size before the handle puts it in that padding,
     * and dropping capacity leaves 16 + modCount + size + handle = 32 exactly --
     * the next BiBOP size class down, on the most frequently allocated collection
     * in the VM (a translation of the 5,326-class corpus allocates about three
     * million of them).
     *
     * Capacity is not stored because the block already knows it: every native
     * reference block carries its own element count in its header, which
     * cn1RefBlockCount reads with one load, and which the C fast paths in
     * cn1_intrinsics.h and addAllNative now read the same way. A handle of 0 --
     * the lazy, never-allocated state -- reports 0.
     *
     * Consequence worth knowing: there is nowhere left to park a REQUESTED capacity
     * that has not been allocated yet. The no-argument constructor used to set
     * capacity = 10 and allocate nothing; now it allocates nothing and says
     * nothing, and reserve() applies the same default on the first growth. */
    private transient int size;
    private transient volatile long cn1Storage;

    private static final int DEFAULT_CAPACITY = 10;

    /** Element count of the backing block; 0 when nothing is allocated yet. */
    private int capacity() {
        return NativeStorage.capacity(cn1Storage);
    }

    public ArrayList() { }

    public ArrayList(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException();
        cn1Storage = NativeStorage.references(capacity);
    }

    public ArrayList(E... elements) {
        this(elements.length);
        for (E value : elements) add(value);
    }

    public ArrayList(Collection<? extends E> collection) {
        int nativeResult = initFromNative(collection);
        if (nativeResult >= 0) return;
        if (nativeResult == -2) throw new OutOfMemoryError();
        int initial = collection.size();
        if (initial < 0) throw new IllegalArgumentException();
        cn1Storage = NativeStorage.references(initial);
        addAll(collection);
    }

    private native int initFromNative(Collection<?> collection);

    static void toObjectArray(Object[] objects, Collection collection) {
        Iterator iterator = collection.iterator();
        for (int i = 0; i < objects.length; i++) objects[i] = iterator.next();
    }

    static Object[] toObjectArray(Collection collection) {
        Object[] objects = new Object[collection.size()];
        toObjectArray(objects, collection);
        return objects;
    }

    /* Same split: the guard is two compares, the throw drags in an exception
     * allocation and its constructor. Keeping them together costs every get()
     * and set() a call. */
    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throwIndex();
        }
    }

    private void throwIndex() {
        throw new IndexOutOfBoundsException();
    }

    private void checkPosition(int index) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException();
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        checkIndex(index);
        return (E) NativeStorage.get(cn1Storage, index);
    }

    @SuppressWarnings("unchecked")
    public E set(int index, E value) {
        checkIndex(index);
        E old = (E) NativeStorage.get(cn1Storage, index);
        NativeStorage.setOwned(this, cn1Storage, index, value);
        return old;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    /* SPLIT SO THE GUARD CAN BE INLINED, and the growth cannot drag it down.
     *
     * This used to be one method whose hot path is the single comparison below
     * and whose body also holds the growth policy, a resize (allocation plus a
     * copy) and an OutOfMemoryError construction. clang costs a call by the whole
     * callee, so the three-instruction guard was never inlined into add(): the
     * disassembly of ArrayList.add carried `bl _java_util_ArrayList_reserve___int`
     * even with -inline-threshold raised from 225 to 1200, which only grew the
     * binary 60% and made nothing faster.
     *
     * Splitting it is what actually offers clang the choice -- the same shape
     * CN1_FAST_NEW already uses, an inline fast path over an out-of-line
     * fallback. The decision stays with the compiler; this just stops hiding it. */
    private void reserve(int required) {
        /* `required >= 0` is not redundant. capacity() is never negative, so a
         * NEGATIVE required -- which addAll reaches by integer overflow on
         * size + values.length -- satisfies `required <= capacity()` and would
         * take the fast path back out of here, skipping the OutOfMemoryError
         * that reserveSlow still throws for it. The two compares fold into one
         * unsigned compare; the check costs nothing and the omission would be a
         * silent out-of-range write. */
        if (required >= 0 && required <= capacity()) {
            return;
        }
        reserveSlow(required);
    }

    private void reserveSlow(int required) {
        if (required < 0) throw new OutOfMemoryError();
        int cap = capacity();
        if (cap == 0) {
            // First growth. DEFAULT_CAPACITY unless more was asked for, which is what
            // the old `capacity = 10` placeholder produced on the first add.
            resize(required > DEFAULT_CAPACITY ? required : DEFAULT_CAPACITY);
            return;
        }
        int grown = cap + (cap >> 1) + 1;
        resize(grown < required || grown < 0 ? required : grown);
    }

    private void resize(int newCapacity) {
        long fresh = NativeStorage.references(newCapacity);
        long old = cn1Storage;
        NativeStorage.copy(old, 0, fresh, 0, size);
        cn1Storage = fresh;
        NativeStorage.retire(old);
    }

    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > capacity()) {
            reserve(minimumCapacity);
            modCount++;
        }
    }

    public void trimToSize() {
        modCount++;
        if (capacity() != size) resize(size);
    }

    /* THE PUBLIC METHOD IS THE HOT PATH, and the growth is a separate private
     * call it does not make.
     *
     * add() used to open with reserve(size + 1), so the common case -- there is
     * room, nothing to do -- was a CALL. clang costs a call by the whole callee,
     * and reserve carried the growth policy, a resize and an OutOfMemoryError
     * construction, so the guard was never inlined: ArrayList.add's disassembly
     * carried `bl _java_util_ArrayList_reserve___int` even with clang's inline
     * threshold raised from 225 to 1200 (which only grew the binary 60% and made
     * nothing faster).
     *
     * Writing the capacity test here and delegating only the growth means the
     * frequent path is straight-line code in the caller and the rare path is one
     * call that is supposed to be a call. The decision to inline is still
     * clang's; this stops handing it a small guard welded to a large body. */
    public boolean add(E value) {
        int s = size;
        if (s < capacity()) {
            NativeStorage.setOwned(this, cn1Storage, s, value);
            size = s + 1;
            modCount++;
            return true;
        }
        return addGrow(value);
    }

    /* Deliberately NOT inlinable and deliberately not on the common path: it
     * exists so add() above can stay small. */
    private boolean addGrow(E value) {
        reserveSlow(size + 1);
        NativeStorage.setOwned(this, cn1Storage, size++, value);
        modCount++;
        return true;
    }

    public void add(int index, E value) {
        checkPosition(index);
        reserve(size + 1);
        NativeStorage.move(cn1Storage, index, index + 1, size - index);
        NativeStorage.setOwned(this, cn1Storage, index, value);
        size++;
        modCount++;
    }

    public boolean addAll(Collection<? extends E> collection) {
        return addAll(size, collection);
    }

    // Returns -1 for an unsupported implementation, -2 for allocation failure.
    // Native layouts copy directly into the destination block without a Java
    // array, iterator, or per-element method call.
    private native int addAllNative(int index, Collection<?> collection);

    public boolean addAll(int index, Collection<? extends E> collection) {
        checkPosition(index);
        int nativeResult = addAllNative(index, collection);
        if (nativeResult >= 0) return nativeResult != 0;
        if (nativeResult == -2) throw new OutOfMemoryError();
        Object[] values = collection.toArray();
        if (values.length == 0) return false;
        reserve(size + values.length);
        NativeStorage.move(cn1Storage, index, index + values.length, size - index);
        for (int i = 0; i < values.length; i++) NativeStorage.setOwned(this, cn1Storage, index + i, values[i]);
        size += values.length;
        modCount++;
        return true;
    }

    @SuppressWarnings("unchecked")
    public E remove(int index) {
        checkIndex(index);
        E old = (E) NativeStorage.get(cn1Storage, index);
        NativeStorage.move(cn1Storage, index + 1, index, size - index - 1);
        NativeStorage.clear(cn1Storage, --size, 1);
        modCount++;
        return old;
    }

    public boolean remove(Object value) {
        int index = indexOf(value);
        if (index < 0) return false;
        remove(index);
        return true;
    }

    protected void removeRange(int from, int to) {
        if (from < 0 || to > size || from > to) throw new IndexOutOfBoundsException();
        int count = to - from;
        NativeStorage.move(cn1Storage, to, from, size - to);
        NativeStorage.clear(cn1Storage, size - count, count);
        size -= count;
        modCount++;
    }

    public void clear() {
        NativeStorage.clear(cn1Storage, 0, size);
        size = 0;
        modCount++;
    }

    public int indexOf(Object value) {
        for (int i = 0; i < size; i++) {
            Object element = NativeStorage.get(cn1Storage, i);
            if (value == null ? element == null : value.equals(element)) return i;
        }
        return -1;
    }

    public int lastIndexOf(Object value) {
        for (int i = size - 1; i >= 0; i--) {
            Object element = NativeStorage.get(cn1Storage, i);
            if (value == null ? element == null : value.equals(element)) return i;
        }
        return -1;
    }

    public boolean contains(Object value) { return indexOf(value) >= 0; }

    public Object[] toArray() {
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++) result[i] = NativeStorage.get(cn1Storage, i);
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] result) {
        if (result.length < size) {
            result = (T[]) java.lang.reflect.Array.newInstance(result.getClass().getComponentType(), size);
        }
        for (int i = 0; i < size; i++) result[i] = (T) NativeStorage.get(cn1Storage, i);
        if (result.length > size) result[size] = null;
        return result;
    }

    class ArrayListIterator implements Iterator<E> {
        private int cursor;
        private int lastReturned = -1;
        private int expectedModCount = modCount;

        public boolean hasNext() { return cursor != size; }

        @SuppressWarnings("unchecked")
        public E next() {
            // The capacity term is what keeps an inconsistent size from turning into
            // an unchecked read past the block -- NativeStorage.get has no bounds
            // check. It reads the block header, which the very next line touches
            // anyway, rather than a field that no longer exists.
            if (modCount != expectedModCount || cursor >= size
                    || cursor >= NativeStorage.capacity(cn1Storage)) return nextSlow();
            lastReturned = cursor++;
            return (E) NativeStorage.get(cn1Storage, lastReturned);
        }

        private E nextSlow() {
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
            if (cursor >= size) throw new NoSuchElementException();
            throw new ConcurrentModificationException();
        }

        public void remove() {
            if (lastReturned < 0) throw new IllegalStateException();
            if (modCount != expectedModCount) throw new ConcurrentModificationException();
            ArrayList.this.remove(lastReturned);
            cursor = lastReturned;
            lastReturned = -1;
            expectedModCount = modCount;
        }
    }

    public Iterator<E> iterator() { return new ArrayListIterator(); }
}
