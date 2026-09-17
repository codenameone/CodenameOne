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
    private transient volatile long cn1Storage;
    private transient int capacity;
    private transient int size;

    public ArrayList() { capacity = 10; }

    public ArrayList(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException();
        cn1Storage = NativeStorage.references(capacity);
        this.capacity = capacity;
    }

    public ArrayList(E... elements) {
        this(elements.length);
        for (E value : elements) add(value);
    }

    public ArrayList(Collection<? extends E> collection) {
        int nativeResult = initFromNative(collection);
        if (nativeResult >= 0) return;
        if (nativeResult == -2) throw new OutOfMemoryError();
        capacity = collection.size();
        if (capacity < 0) throw new IllegalArgumentException();
        cn1Storage = NativeStorage.references(capacity);
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

    private void checkIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
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
        NativeStorage.set(cn1Storage, index, value);
        return old;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    private void reserve(int required) {
        if (required < 0) throw new OutOfMemoryError();
        if (required <= capacity) {
            if (required != 0 && cn1Storage == 0) resize(capacity);
            return;
        }
        int grown = capacity + (capacity >> 1) + 1;
        resize(grown < required || grown < 0 ? required : grown);
    }

    private void resize(int newCapacity) {
        long fresh = NativeStorage.references(newCapacity);
        long old = cn1Storage;
        NativeStorage.copy(old, 0, fresh, 0, size);
        cn1Storage = fresh;
        capacity = newCapacity;
        NativeStorage.retire(old);
    }

    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > capacity) {
            reserve(minimumCapacity);
            modCount++;
        }
    }

    public void trimToSize() {
        modCount++;
        if (capacity != size) resize(size);
    }

    public boolean add(E value) {
        reserve(size + 1);
        NativeStorage.set(cn1Storage, size++, value);
        modCount++;
        return true;
    }

    public void add(int index, E value) {
        checkPosition(index);
        reserve(size + 1);
        NativeStorage.move(cn1Storage, index, index + 1, size - index);
        NativeStorage.set(cn1Storage, index, value);
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
        for (int i = 0; i < values.length; i++) NativeStorage.set(cn1Storage, index + i, values[i]);
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
            if (modCount != expectedModCount || cursor >= size || cursor >= capacity) return nextSlow();
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
