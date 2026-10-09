/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package android.util;

/// Maps integers to objects; a sorted-array map like Android's, so iteration
/// is in key order.
public class SparseArray<E> implements Cloneable {

    private int[] keys;
    private Object[] values;
    private int size;

    public SparseArray() {
        this(10);
    }

    public SparseArray(int initialCapacity) {
        keys = new int[Math.max(1, initialCapacity)];
        values = new Object[keys.length];
    }

    private int search(int key) {
        int lo = 0;
        int hi = size - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int v = keys[mid];
            if (v < key) {
                lo = mid + 1;
            } else if (v > key) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return ~lo;
    }

    public E get(int key) {
        return get(key, null);
    }

    @SuppressWarnings("unchecked")
    public E get(int key, E valueIfKeyNotFound) {
        int i = search(key);
        return i < 0 ? valueIfKeyNotFound : (E) values[i];
    }

    public void delete(int key) {
        int i = search(key);
        if (i >= 0) {
            removeAt(i);
        }
    }

    public void remove(int key) {
        delete(key);
    }

    public void removeAt(int index) {
        System.arraycopy(keys, index + 1, keys, index, size - index - 1);
        System.arraycopy(values, index + 1, values, index, size - index - 1);
        size--;
        values[size] = null;
    }

    public void put(int key, E value) {
        int i = search(key);
        if (i >= 0) {
            values[i] = value;
            return;
        }
        i = ~i;
        if (size == keys.length) {
            int[] nk = new int[size * 2];
            Object[] nv = new Object[size * 2];
            System.arraycopy(keys, 0, nk, 0, size);
            System.arraycopy(values, 0, nv, 0, size);
            keys = nk;
            values = nv;
        }
        System.arraycopy(keys, i, keys, i + 1, size - i);
        System.arraycopy(values, i, values, i + 1, size - i);
        keys[i] = key;
        values[i] = value;
        size++;
    }

    public void append(int key, E value) {
        put(key, value);
    }

    public int size() {
        return size;
    }

    public int keyAt(int index) {
        return keys[index];
    }

    @SuppressWarnings("unchecked")
    public E valueAt(int index) {
        return (E) values[index];
    }

    public void setValueAt(int index, E value) {
        values[index] = value;
    }

    public int indexOfKey(int key) {
        return search(key);
    }

    public int indexOfValue(E value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(int key) {
        return search(key) >= 0;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            values[i] = null;
        }
        size = 0;
    }

    @Override
    public SparseArray<E> clone() {
        SparseArray<E> c = new SparseArray<E>(keys.length);
        System.arraycopy(keys, 0, c.keys, 0, size);
        System.arraycopy(values, 0, c.values, 0, size);
        c.size = size;
        return c;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(keys[i]).append('=').append(values[i]);
        }
        return sb.append('}').toString();
    }
}
