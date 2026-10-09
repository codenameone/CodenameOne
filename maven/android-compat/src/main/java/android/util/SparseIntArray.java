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

/// Maps integers to integers; a sorted-array map like Android's.
public class SparseIntArray implements Cloneable {

    private int[] keys;
    private int[] values;
    private int size;

    public SparseIntArray() {
        this(10);
    }

    public SparseIntArray(int initialCapacity) {
        keys = new int[Math.max(1, initialCapacity)];
        values = new int[keys.length];
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

    public int get(int key) {
        return get(key, 0);
    }

    public int get(int key, int valueIfKeyNotFound) {
        int i = search(key);
        return i < 0 ? valueIfKeyNotFound : values[i];
    }

    public void delete(int key) {
        int i = search(key);
        if (i >= 0) {
            removeAt(i);
        }
    }

    public void removeAt(int index) {
        System.arraycopy(keys, index + 1, keys, index, size - index - 1);
        System.arraycopy(values, index + 1, values, index, size - index - 1);
        size--;
    }

    public void put(int key, int value) {
        int i = search(key);
        if (i >= 0) {
            values[i] = value;
            return;
        }
        i = ~i;
        if (size == keys.length) {
            int[] nk = new int[keys.length * 2];
            int[] nv = new int[keys.length * 2];
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

    /// Puts a key known to be greater than every key already present, as
    /// Android's does; falls back to `put` otherwise.
    public void append(int key, int value) {
        if (size != 0 && key <= keys[size - 1]) {
            put(key, value);
            return;
        }
        if (size == keys.length) {
            int[] nk = new int[keys.length * 2];
            int[] nv = new int[keys.length * 2];
            System.arraycopy(keys, 0, nk, 0, size);
            System.arraycopy(values, 0, nv, 0, size);
            keys = nk;
            values = nv;
        }
        keys[size] = key;
        values[size] = value;
        size++;
    }

    public int size() {
        return size;
    }

    public int keyAt(int index) {
        return keys[index];
    }

    public int valueAt(int index) {
        return values[index];
    }

    public void setValueAt(int index, int value) {
        values[index] = value;
    }

    public int indexOfKey(int key) {
        int i = search(key);
        return i < 0 ? -1 : i;
    }

    public int indexOfValue(int value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public void clear() {
        size = 0;
    }

    /// A copy: Codename One's Object.clone() copies nothing.
    @Override
    public SparseIntArray clone() {
        SparseIntArray c = new SparseIntArray(Math.max(1, size));
        System.arraycopy(keys, 0, c.keys, 0, size);
        System.arraycopy(values, 0, c.values, 0, size);
        c.size = size;
        return c;
    }

    public int[] copyKeys() {
        int[] k = new int[size];
        System.arraycopy(keys, 0, k, 0, size);
        return k;
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
