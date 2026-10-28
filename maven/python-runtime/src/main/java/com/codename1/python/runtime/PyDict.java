/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.runtime;

/// Python's `dict`: insertion ordered, laid out as CPython's compact dict.
///
/// Entries are appended to parallel arrays in insertion order and a separate
/// open-addressed table of entry indices finds them by hash. Deleting leaves a
/// hole in the entry arrays, compacted on the next resize, so iteration order
/// is always insertion order and the table needs no linked list.
public class PyDict extends PyObject {
    private static final int EMPTY = -1;
    private static final int DUMMY = -2;

    Object[] keys;
    Object[] values;
    long[] hashes;
    /// Number of entry slots used, live or deleted.
    int used;
    /// Number of live entries.
    int size;
    private int[] index;

    public PyDict() {
        keys = new Object[8];
        values = new Object[8];
        hashes = new long[8];
        index = newIndex(16);
    }

    private static int[] newIndex(int n) {
        int[] ix = new int[n];
        for (int i = 0; i < n; i++) {
            ix[i] = EMPTY;
        }
        return ix;
    }

    @Override
    public PyType type() {
        return Py.DICT_TYPE;
    }

    public int size() {
        return size;
    }

    private static boolean keyEq(Object a, long ha, Object b, long hb) {
        if (a == b) { //NOPMD CompareObjectsWithEquals - identity is meant
            return true;
        }
        if (ha != hb) {
            return false;
        }
        if (a instanceof String && b instanceof String) {
            return ((String) a).equals(((String) b));
        }
        return Py.eqBool(a, b);
    }

    /// [#find] for a str key: a stored str is compared with `equals`
    /// directly, and only a key of another type with the same hash goes
    /// through the general comparison.
    private int findStr(String key, long h) {
        int mask = index.length - 1;
        int i = (int) (h & mask);
        long perturb = h & Long.MAX_VALUE;
        int freeSlot = -1;
        while (true) {
            int e = index[i];
            if (e == EMPTY) {
                return -1 - (freeSlot >= 0 ? freeSlot : i);
            }
            if (e == DUMMY) {
                if (freeSlot < 0) {
                    freeSlot = i;
                }
            } else if (hashes[e] == h) {
                // equals tests identity first. A key of another type with an
                // equal hash is left to its __eq__.
                Object k = keys[e];
                if (k instanceof String ? key.equals(k) : keyEq(k, h, key, h)) {
                    return i;
                }
            }
            perturb >>>= 5;
            i = (int) ((i * 5L + perturb + 1) & mask);
        }
    }

    /// The index slot holding `key`, or the negative of (1 + the first free slot).
    private int find(Object key, long h) {
        int mask = index.length - 1;
        int i = (int) (h & mask);
        long perturb = h & Long.MAX_VALUE;
        int freeSlot = -1;
        while (true) {
            int e = index[i];
            if (e == EMPTY) {
                return -1 - (freeSlot >= 0 ? freeSlot : i);
            }
            if (e == DUMMY) {
                if (freeSlot < 0) {
                    freeSlot = i;
                }
            } else if (keyEq(keys[e], hashes[e], key, h)) {
                return i;
            }
            perturb >>>= 5;
            i = (int) ((i * 5L + perturb + 1) & mask);
        }
    }

    /// `self[key] = self.get(key, dflt) + inc` with one probe: the slot found
    /// for the read is the one written, unless the addition grew the dict
    /// (it cannot, but a user `__add__` could), in which case it probes again.
    Object count(Object key, Object dflt, Object inc) {
        long h = Py.keyHash(key, "a dict key");
        int s = key instanceof String ? findStr((String) key, h) : find(key, h);
        if (s >= 0) {
            int e = index[s];
            Object stored = keys[e];
            Object r = Py.add(values[e], inc);
            if (index[s] != e || keys[e] != stored) { //NOPMD CompareObjectsWithEquals - identity is meant
                // The addition ran Python code that changed the dict.
                s = find(key, h);
            }
            if (s >= 0) {
                values[index[s]] = r;
                return r;
            }
            setItem(key, r);
            return r;
        }
        Object r = Py.add(dflt, inc);
        setItem(key, r);
        return r;
    }

    /// The slot of int key `k` (hash `h`), as `find` answers; the key is
    /// compared without being boxed.
    private int findLong(long k, long h) {
        int mask = index.length - 1;
        int i = (int) (h & mask);
        long perturb = h & Long.MAX_VALUE;
        int freeSlot = -1;
        while (true) {
            int e = index[i];
            if (e == EMPTY) {
                return -1 - (freeSlot >= 0 ? freeSlot : i);
            }
            if (e == DUMMY) {
                if (freeSlot < 0) {
                    freeSlot = i;
                }
            } else if (hashes[e] == h) {
                Object x = keys[e];
                if (x instanceof Long ? ((Long) x).longValue() == k : keyEq(x, h, Long.valueOf(k), h)) {
                    return i;
                }
            }
            perturb >>>= 5;
            i = (int) ((i * 5L + perturb + 1) & mask);
        }
    }

    /// `self.get(k)` for an int key, or `null` when absent.
    Object getLong(long k) {
        int s = findLong(k, Hashing.ofLong(k));
        return s >= 0 ? values[index[s]] : null;
    }

    /// `self[k] = value` for an int key; the key is boxed only when it is new.
    void setLong(long k, Object value) {
        long h = Hashing.ofLong(k);
        int s = findLong(k, h);
        if (s >= 0) {
            values[index[s]] = value;
            return;
        }
        if (used == keys.length) {
            grow();
            s = findLong(k, h);
        }
        index[-1 - s] = used;
        keys[used] = Long.valueOf(k);
        values[used] = value;
        hashes[used] = h;
        used++;
        size++;
        if (used * 3 >= index.length * 2) {
            rebuild(index.length * 2);
        }
    }

    /// `self[key]`, or `null` when absent.
    public Object getOrNull(Object key) {
        int s = find(key, Py.keyHash(key, "a dict key"));
        return s >= 0 ? values[index[s]] : null;
    }

    public Object get(String key) {
        return getOrNull(key);
    }

    @Override
    public Object getItem(Object key) {
        Object v = getOrNull(key);
        if (v == null) {
            Object missing = type() == Py.DICT_TYPE ? null : type().lookup("__missing__"); //NOPMD CompareObjectsWithEquals - identity is meant
            if (missing != null) {
                return Py.obj(missing).call(new Object[] {this, key}, Py.NO_KW);
            }
            throw Py.keyError(key);
        }
        return v;
    }

    @Override
    public void setItem(Object key, Object value) {
        long h = Py.keyHash(key, "a dict key");
        int s = find(key, h);
        if (s >= 0) {
            values[index[s]] = value;
            return;
        }
        if (used == keys.length) {
            grow();
            s = find(key, h);
        }
        index[-1 - s] = used;
        keys[used] = key;
        values[used] = value;
        hashes[used] = h;
        used++;
        size++;
        if (used * 3 >= index.length * 2) {
            rebuild(index.length * 2);
        }
    }

    private void grow() {
        int cap = Math.max(8, size * 2);
        if (cap < keys.length && size < used) {
            cap = keys.length;
        }
        Object[] nk = new Object[cap];
        Object[] nv = new Object[cap];
        long[] nh = new long[cap];
        int w = 0;
        for (int i = 0; i < used; i++) {
            if (keys[i] != null) {
                nk[w] = keys[i];
                nv[w] = values[i];
                nh[w] = hashes[i];
                w++;
            }
        }
        keys = nk;
        values = nv;
        hashes = nh;
        used = w;
        int n = index.length;
        while (cap * 3 >= n * 2) {
            n *= 2;
        }
        rebuild(n);
    }

    private void rebuild(int n) {
        index = newIndex(n);
        int mask = n - 1;
        for (int e = 0; e < used; e++) {
            if (keys[e] == null) {
                continue;
            }
            long h = hashes[e];
            int i = (int) (h & mask);
            long perturb = h & Long.MAX_VALUE;
            while (index[i] != EMPTY) {
                perturb >>>= 5;
                i = (int) ((i * 5L + perturb + 1) & mask);
            }
            index[i] = e;
        }
    }

    /// Removes `key`, returning its value or `null` when absent.
    public Object remove(Object key) {
        int s = find(key, Py.keyHash(key, "a dict key"));
        if (s < 0) {
            return null;
        }
        int e = index[s];
        Object v = values[e];
        index[s] = DUMMY;
        keys[e] = null;
        values[e] = null;
        size--;
        return v;
    }

    @Override
    public void delItem(Object key) {
        if (remove(key) == null) {
            throw Py.keyError(key);
        }
    }

    @Override
    public boolean contains(Object x) {
        return find(x, Py.keyHash(x, "a dict key")) >= 0;
    }

    @Override
    public int len() {
        return size;
    }

    @Override
    public boolean isTrue() {
        return size != 0;
    }

    @Override
    public long hash() {
        throw Py.typeError("unhashable type: '" + type().name + "'");
    }

    public void clear() {
        if (keys.length <= KEEP_ON_CLEAR) {
            // A dict cleared and refilled, a cache or a window, keeps its
            // capacity as Java's HashMap does: refilling it would otherwise
            // regrow it from eight entries, a dozen arrays of garbage each
            // time.
            nullOut(keys, used);
            nullOut(values, used);
            int[] ix = index;
            for (int i = 0; i < ix.length; i++) {
                ix[i] = EMPTY;
            }
        } else {
            keys = new Object[8];
            values = new Object[8];
            hashes = new long[8];
            index = newIndex(16);
        }
        used = 0;
        size = 0;
    }

    private static final Object[] NULLS = new Object[256];

    /// Nulls the first `n` slots of `a` in bulk copies: one store per slot
    /// would pass each through the collector's write barrier.
    private static void nullOut(Object[] a, int n) {
        for (int i = 0; i < n; i += NULLS.length) {
            System.arraycopy(NULLS, 0, a, i, Math.min(NULLS.length, n - i));
        }
    }

    /// The largest entry capacity `clear` keeps.
    private static final int KEEP_ON_CLEAR = 1 << 17;

    /// `dict.update(other)` for a mapping or an iterable of pairs.
    public void update(Object other) {
        if (other instanceof PyDict) {
            PyDict d = (PyDict) other;
            for (int i = 0; i < d.used; i++) {
                if (d.keys[i] != null) {
                    setItem(d.keys[i], d.values[i]);
                }
            }
            return;
        }
        Object keysFn = Py.obj(other).getAttrOrNull("keys");
        if (keysFn != null) {
            Object it = Py.iter(Py.obj(keysFn).call(Py.NO_ARGS, Py.NO_KW));
            for (Object k = Py.next(it); k != null; k = Py.next(it)) {
                setItem(k, Py.obj(other).getItem(k));
            }
            return;
        }
        Object it = Py.iter(other);
        int n = 0;
        for (Object pair = Py.next(it); pair != null; pair = Py.next(it), n++) {
            Object[] kv = Py.toArray(pair);
            if (kv.length != 2) {
                throw Py.valueError("dictionary update sequence element #" + n + " has length " + kv.length
                        + "; 2 is required");
            }
            setItem(kv[0], kv[1]);
        }
    }

    /// The live keys in order.
    public Object[] keyArray() {
        Object[] r = new Object[size];
        int w = 0;
        for (int i = 0; i < used; i++) {
            if (keys[i] != null) {
                r[w++] = keys[i];
            }
        }
        return r;
    }

    /// Iterates over entries; `kind` 0 = keys, 1 = values, 2 = items.
    Object iterate(final int kind) {
        final int startSize = size;
        return new PyJavaIter(new PyJavaIter.Source() {
            int i;

            @Override
            public Object next() {
                if (size != startSize) {
                    throw Py.runtimeError("dictionary changed size during iteration");
                }
                while (i < used && keys[i] == null) {
                    i++;
                }
                if (i >= used) {
                    return null;
                }
                int e = i++;
                if (kind == 0) {
                    return keys[e];
                }
                if (kind == 1) {
                    return values[e];
                }
                return new PyTuple(new Object[] {keys[e], values[e]});
            }
        });
    }

    @Override
    public Object iter() {
        return iterate(0);
    }

    @Override
    public String repr() {
        if (size == 0) {
            return "{}";
        }
        if (Py.enterRepr(this)) {
            return "{...}";
        }
        try {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (int i = 0; i < used; i++) {
                if (keys[i] == null) {
                    continue;
                }
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(Py.toRepr(keys[i])).append(": ").append(Py.toRepr(values[i]));
            }
            return sb.append('}').toString();
        } finally {
            Py.exitRepr(this);
        }
    }

    @Override
    public Object eq(Object o) {
        if (!(o instanceof PyDict)) {
            return null;
        }
        PyDict b = (PyDict) o;
        if (b.size != size) {
            return Py.FALSE;
        }
        for (int i = 0; i < used; i++) {
            if (keys[i] == null) {
                continue;
            }
            Object v = b.getOrNull(keys[i]);
            if (v == null || !Py.eqBool(values[i], v)) {
                return Py.FALSE;
            }
        }
        return Py.TRUE;
    }

    @Override
    public Object or(Object o) {
        if (!(o instanceof PyDict)) {
            return null;
        }
        PyDict r = copy();
        r.update(o);
        return r;
    }

    @Override
    public Object inplace(String name, Object o) {
        if ("__ior__".equals(name)) {
            update(o);
            return this;
        }
        return null;
    }

    public PyDict copy() {
        PyDict r = new PyDict();
        r.update(this);
        return r;
    }
}
