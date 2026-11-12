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
    /// Open-addressed entry indices by hash; null while the entries fit in
    /// `LINEAR` slots, where a scan of `hashes` finds a key as fast and the
    /// table would be one more allocation for every small dict.
    private int[] index;

    /// The most entry slots a dict keeps without an index table.
    private static final int LINEAR = 8;
    private static final Object[] NO_OBJECTS = new Object[0];
    private static final long[] NO_HASHES = new long[0];

    /// The shared key layout this dict's `keys`, `hashes` and `index` belong
    /// to, or null when they are its own. While it is set the dict has no
    /// deleted entry, `used == size == layout.n`, and those three arrays
    /// must never be written: every write path calls [#unshare] first.
    Layout layout;

    /// An empty dict allocates no arrays until its first entry: keyword
    /// dicts, per-call dicts and the like are often never filled.
    public PyDict() {
        keys = NO_OBJECTS;
        values = NO_OBJECTS;
        hashes = NO_HASHES;
        if (getClass() == PyDict.class) {
            layout = Layout.ROOT;
        }
    }

    /// A hidden class, as JavaScript engines have them: the keys, hashes and
    /// index table of every dict that received the same str keys in the same
    /// order. pdfplumber turns each char of a page into a dict of the same
    /// twenty keys; sharing the layout means each of those dicts holds only
    /// its values, is built without hashing or probing a key, and a
    /// `char["top"]` site ([KeySite]) finds the slot by one identity
    /// compare. A layout is immutable once made.
    static final class Layout {
        /// The most keys a layout holds, the most layouts that follow one
        /// (but the root, which every dict starts from: kwargs and module
        /// dicts reach it before any record does), and the most layouts in
        /// the program. Past any of them dicts keep their own arrays, as
        /// before; the last bounds what a program of map-like dicts costs.
        private static final int MAX_KEYS = 48;
        private static final int MAX_NEXT = 64;
        private static final int MAX_LAYOUTS = 8192;
        private static int layouts;

        final Object[] keys;
        final long[] hashes;
        final int[] index;
        final int n;
        private String[] nextKey = NO_KEYS;
        private Layout[] next = NO_LAYOUTS;
        private static final String[] NO_KEYS = new String[0];
        private static final Layout[] NO_LAYOUTS = new Layout[0];
        /// Declared after the empty arrays its fields start from.
        static final Layout ROOT = new Layout(NO_OBJECTS, NO_HASHES, null, 0);
        private int nNext;
        /// Keys one dict has added here that no layout follows yet. A step
        /// is made a layout the second time a dict takes it, so a dict used
        /// as a map (a word count) never builds a chain of layouts.
        private String[] seen = NO_KEYS;
        private int nSeen;

        private Layout(Object[] keys, long[] hashes, int[] index, int n) {
            this.keys = keys;
            this.hashes = hashes;
            this.index = index;
            this.n = n;
        }

        /// The layout after adding `key` (absent here), or null when the dict
        /// must take its own arrays.
        Layout step(String key, long h) {
            String[] nk = nextKey;
            if (nk.length > 0) {
                int mask = nk.length - 1;
                int i = (int) h & mask;
                String k;
                while ((k = nk[i]) != null) {
                    if (k.equals(key)) {
                        return next[i];
                    }
                    i = (i + 1) & mask;
                }
            }
            if (n >= MAX_KEYS || nNext >= MAX_NEXT && n > 0 || layouts >= MAX_LAYOUTS) {
                return null;
            }
            String[] sn = seen;
            for (int i = 0; i < nSeen; i++) {
                if (sn[i].equals(key)) {
                    sn[i] = sn[--nSeen];
                    sn[nSeen] = null;
                    return extend(key, h);
                }
            }
            if (sn.length == 0) {
                seen = sn = new String[4];
            }
            if (nSeen < sn.length) {
                sn[nSeen++] = key;
            } else {
                sn[(int) (h & (sn.length - 1))] = key;
            }
            return null;
        }

        private Layout extend(String key, long h) {
            int m = n + 1;
            Object[] k = new Object[m];
            long[] hs = new long[m];
            System.arraycopy(keys, 0, k, 0, n);
            System.arraycopy(hashes, 0, hs, 0, n);
            k[n] = key;
            hs[n] = h;
            int[] ix = null;
            if (m > LINEAR) {
                int t = 16;
                while (m * 3 >= t * 2) {
                    t *= 2;
                }
                ix = newIndex(t);
                int mask = t - 1;
                for (int e = 0; e < m; e++) {
                    long sp = spread(hs[e]);
                    int i = (int) (sp & mask);
                    long perturb = sp & Long.MAX_VALUE;
                    while (ix[i] != EMPTY) {
                        perturb >>>= 5;
                        i = (int) ((i * 5L + perturb + 1) & mask);
                    }
                    ix[i] = e;
                }
            }
            Layout l = new Layout(k, hs, ix, m);
            layouts++;
            if ((nNext + 1) * 2 > nextKey.length) {
                String[] ok = nextKey;
                Layout[] ol = next;
                nextKey = new String[Math.max(4, ok.length * 2)];
                next = new Layout[nextKey.length];
                for (int i = 0; i < ok.length; i++) {
                    if (ok[i] != null) {
                        place(ok[i], ol[i]);
                    }
                }
            }
            place(key, l);
            nNext++;
            return l;
        }

        private void place(String key, Layout l) {
            int mask = nextKey.length - 1;
            int i = key.hashCode() & mask;
            while (nextKey[i] != null) {
                i = (i + 1) & mask;
            }
            nextKey[i] = key;
            next[i] = l;
        }

        /// The entry of str `key` in this layout, or -1.
        int slotOf(String key) {
            Object[] k = keys;
            for (int e = 0; e < n; e++) {
                Object x = k[e];
                if (key.equals(x)) {
                    return e;
                }
            }
            return -1;
        }
    }

    /// Takes the next layout for a new str key: only the value is stored.
    private void appendShared(Layout nl, Object value) {
        int u = used;
        Object[] v = values;
        if (v.length <= u) {
            Object[] nv = new Object[Math.max(4, u + (u >> 1) + 1)];
            System.arraycopy(v, 0, nv, 0, u);
            values = v = nv;
        }
        v[u] = value;
        keys = nl.keys;
        hashes = nl.hashes;
        index = nl.index;
        layout = nl;
        used = u + 1;
        size = u + 1;
    }

    /// Gives this dict its own copies of the shared arrays, before a write
    /// no layout describes (a delete, a key that is not a str, a layout
    /// past its limits).
    void unshare() {
        Layout l = layout;
        if (l == null) {
            return;
        }
        layout = null;
        int cap = Math.max(4, values.length);
        Object[] k = new Object[cap];
        long[] h = new long[cap];
        System.arraycopy(l.keys, 0, k, 0, l.n);
        System.arraycopy(l.hashes, 0, h, 0, l.n);
        keys = k;
        hashes = h;
        if (values.length < cap) {
            Object[] nv = new Object[cap];
            System.arraycopy(values, 0, nv, 0, used);
            values = nv;
        }
        index = l.index == null ? null : l.index.clone();
        if (index == null && cap > LINEAR) {
            rebuildFor(cap);
        }
    }

    /// The index table for a dict of capacity `cap`, as grow() sizes it.
    private void rebuildFor(int cap) {
        int t = 16;
        while (cap * 3 >= t * 2) {
            t *= 2;
        }
        rebuild(t);
    }

    /// A dict sized for `n` entries, as a literal of that many pairs builds:
    /// its arrays are allocated once at that size rather than at the first
    /// growth's.
    public PyDict(int n) {
        if (n <= 0) {
            keys = NO_OBJECTS;
            values = NO_OBJECTS;
            hashes = NO_HASHES;
            if (getClass() == PyDict.class) {
                layout = Layout.ROOT;
            }
            return;
        }
        if (getClass() == PyDict.class) {
            keys = NO_OBJECTS;
            hashes = NO_HASHES;
            values = new Object[n];
            layout = Layout.ROOT;
            return;
        }
        allocate(n);
    }

    /// An empty dict about to be filled with `n` entries gets its arrays at
    /// that size at once, rather than through every growth on the way.
    void reserve(int n) {
        if (layout != null) {
            if (used == 0 && values.length < n) {
                values = new Object[n];
            }
            return;
        }
        if (used == 0 && keys.length < n) {
            allocate(n);
        }
    }

    /// Takes `n` entries whose keys, hashes and index table are fresh copies
    /// of ones an equal dict already built (an instance's `__dict__`, see
    /// `PyInstance.InstanceDict`): no key is hashed or probed again.
    void adopt(Object[] k, long[] h, int[] ix, Object[] v, int n) {
        layout = null;
        keys = k;
        hashes = h;
        index = ix;
        values = v;
        used = n;
        size = n;
    }

    /// The index table, or null while the entries are scanned.
    int[] indexTable() {
        return index;
    }

    private void allocate(int n) {
        layout = null;
        keys = new Object[n];
        values = new Object[n];
        hashes = new long[n];
        if (n > LINEAR) {
            // The index table at the size grow() would reach for n entries, once.
            int t = 16;
            while (n * 3 >= t * 2) {
                t *= 2;
            }
            index = newIndex(t);
        }
    }

    /// The entry a slot from `find` names: without an index table the
    /// finders answer the entry itself.
    /// The hash an index slot is taken from. A str's hash is Java's, whose
    /// low bits are a poor mix (31 squared is 1 modulo 64, so in a table of
    /// 64 slots a word's chars nearly cancel): words of one length collided
    /// on most probes. A multiply carries every bit into the high ones, and
    /// the fold brings them down to the slot bits. Hash values themselves,
    /// which Python code sees, are unchanged.
    static long spread(long h) {
        long x = h * 0x9E3779B97F4A7C15L;
        return x ^ (x >>> 29);
    }

    private int entry(int s) {
        return index == null ? s : index[s];
    }

    /// The entry holding `key` by a scan of the entries (no index table),
    /// or -1.
    private int scan(Object key, long h) {
        final long[] hs = hashes;
        final Object[] ks = keys;
        for (int e = 0; e < used; e++) {
            if (hs[e] == h) {
                Object k = ks[e];
                if (k != null && keyEq(k, h, key, h)) {
                    return e;
                }
            }
        }
        return -1;
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
        sync();
        return size;
    }

    /// Brings the entries up to date before a read. A plain dict holds its
    /// own entries; a view over something else (an instance's `__dict__`)
    /// refills itself here from what it stands for.
    void sync() {
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
        if (index == null) {
            final long[] hs = hashes;
            final Object[] ks = keys;
            for (int e = 0; e < used; e++) {
                if (hs[e] == h) {
                    Object k = ks[e];
                    if (k instanceof String ? key.equals(k) : k != null && keyEq(k, h, key, h)) {
                        return e;
                    }
                }
            }
            return -1;
        }
        int mask = index.length - 1;
        long m = spread(h);
        int i = (int) (m & mask);
        long perturb = m & Long.MAX_VALUE;
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
        if (index == null) {
            return scan(key, h);
        }
        int mask = index.length - 1;
        long m = spread(h);
        int i = (int) (m & mask);
        long perturb = m & Long.MAX_VALUE;
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
            final int[] ix = index;
            final Object[] ks = keys;
            int e = entry(s);
            Object cur = values[e];
            if (cur instanceof Long && inc instanceof Long) {
                // Two ints: no Python code runs, so the slot stays put.
                long a = (Long) cur;
                long b = (Long) inc;
                long c = a + b;
                if (((a ^ c) & (b ^ c)) >= 0) {
                    Object r = Long.valueOf(c);
                    values[e] = r;
                    return r;
                }
            }
            Object stored = ks[e];
            Object r = Py.add(cur, inc);
            if (index != ix || keys != ks || ix != null && ix[s] != e || ks[e] != stored) { //NOPMD CompareObjectsWithEquals - identity is meant
                // The addition ran Python code that changed the dict.
                s = find(key, h);
            }
            if (s >= 0) {
                values[entry(s)] = r;
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
        if (index == null) {
            final long[] hs = hashes;
            final Object[] ks = keys;
            for (int e = 0; e < used; e++) {
                if (hs[e] == h) {
                    Object x = ks[e];
                    if (x instanceof Long ? ((Long) x).longValue() == k : x != null && keyEq(x, h, Long.valueOf(k), h)) {
                        return e;
                    }
                }
            }
            return -1;
        }
        int mask = index.length - 1;
        long m = spread(h);
        int i = (int) (m & mask);
        long perturb = m & Long.MAX_VALUE;
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
        return s >= 0 ? values[entry(s)] : null;
    }

    /// `self[k] = value` for an int key; the key is boxed only when it is new.
    void setLong(long k, Object value) {
        long h = Hashing.ofLong(k);
        int s = findLong(k, h);
        if (s >= 0) {
            values[entry(s)] = value;
            return;
        }
        if (layout != null) {
            unshare();
            s = findLong(k, h);
        }
        if (used == keys.length) {
            grow();
            s = findLong(k, h);
        }
        if (index != null) {
            index[-1 - s] = used;
        }
        keys[used] = Long.valueOf(k);
        values[used] = value;
        hashes[used] = h;
        used++;
        size++;
        if (index != null && used * 3 >= index.length * 2) {
            rebuild(index.length * 2);
        }
    }

    /// `self[key]`, or `null` when absent.
    /// [#getOrNull] with the key's hash already taken, by a caller that
    /// had to hash it itself for its own error message (lru_cache).
    Object getOrNullHashed(Object key, long h) {
        sync();
        int s = find(key, h);
        return s >= 0 ? values[entry(s)] : null;
    }

    /// `self[key]` for a str key whose hash the caller took once, up front
    /// (operator.itemgetter), or `null` when absent.
    Object getStrHashed(String key, long h) {
        sync();
        int s = findStr(key, h);
        return s >= 0 ? values[entry(s)] : null;
    }

    public Object getOrNull(Object key) {
        sync();
        int s = find(key, Py.keyHash(key, "a dict key"));
        return s >= 0 ? values[entry(s)] : null;
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
                return Py.obj(missing).call2(this, key);
            }
            throw Py.keyError(key);
        }
        return v;
    }

    /// [#setItem] answering the dict: what a literal compiles to, one call per pair on a dict
    /// made at its final size, so building one allocates no array of its pairs.
    public PyDict put(Object key, Object value) {
        setItem(key, value);
        return this;
    }

    @Override
    public void setItem(Object key, Object value) {
        long h = Py.keyHash(key, "a dict key");
        int s = find(key, h);
        if (s >= 0) {
            values[entry(s)] = value;
            return;
        }
        if (layout != null) {
            if (key instanceof String) {
                Layout nl = layout.step((String) key, h);
                if (nl != null) {
                    appendShared(nl, value);
                    return;
                }
            }
            unshare();
            s = find(key, h);
        }
        if (used == keys.length) {
            grow();
            s = find(key, h);
        }
        if (index != null) {
            index[-1 - s] = used;
        }
        keys[used] = key;
        values[used] = value;
        hashes[used] = h;
        used++;
        size++;
        if (index != null && used * 3 >= index.length * 2) {
            rebuild(index.length * 2);
        }
    }

    private void grow() {
        // A dict's first entry gets room for four: most dicts filled one at
        // a time (keyword arguments, a route's values) stay that small.
        int cap = keys.length == 0 ? 4 : Math.max(8, size * 2);
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
        if (cap <= LINEAR) {
            index = null;
            return;
        }
        int n = index == null ? 16 : index.length;
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
            long m = spread(h);
            int i = (int) (m & mask);
            long perturb = m & Long.MAX_VALUE;
            while (index[i] != EMPTY) {
                perturb >>>= 5;
                i = (int) ((i * 5L + perturb + 1) & mask);
            }
            index[i] = e;
        }
    }

    /// Removes `key`, returning its value or `null` when absent.
    public Object remove(Object key) {
        long h = Py.keyHash(key, "a dict key");
        int s = find(key, h);
        if (s < 0) {
            return null;
        }
        if (layout != null) {
            unshare();
            s = find(key, h);
        }
        int e = entry(s);
        Object v = values[e];
        if (index != null) {
            index[s] = DUMMY;
        }
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
        sync();
        return find(x, Py.keyHash(x, "a dict key")) >= 0;
    }

    @Override
    public int len() {
        sync();
        return size;
    }

    @Override
    public boolean isTrue() {
        sync();
        return size != 0;
    }

    @Override
    public long hash() {
        throw Py.typeError("unhashable type: '" + type().name + "'");
    }

    public void clear() {
        if (layout != null) {
            // The shared arrays are not this dict's to null; its values are.
            nullOut(values, used);
            keys = NO_OBJECTS;
            hashes = NO_HASHES;
            index = null;
            layout = Layout.ROOT;
            used = 0;
            size = 0;
            return;
        }
        if (keys.length <= KEEP_ON_CLEAR) {
            // A dict cleared and refilled, a cache or a window, keeps its
            // capacity as Java's HashMap does: refilling it would otherwise
            // regrow it from eight entries, a dozen arrays of garbage each
            // time.
            nullOut(keys, used);
            nullOut(values, used);
            int[] ix = index;
            if (ix != null) {
                for (int i = 0; i < ix.length; i++) {
                    ix[i] = EMPTY;
                }
            }
        } else {
            keys = NO_OBJECTS;
            values = NO_OBJECTS;
            hashes = NO_HASHES;
            index = null;
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
            ((PyDict) other).sync();
        }
        if (other instanceof PyDict) {
            PyDict d = (PyDict) other;
            if (size == 0 && used == 0 && d.size == 0) {
                return;
            }
            if (size == 0 && used == 0 && d.layout != null && layout != null && d.size > 0) {
                // `dict(d)` and `d.copy()` of a dict on a shared layout: the
                // copy takes the same layout and a copy of the values.
                Object[] nv = new Object[d.used];
                System.arraycopy(d.values, 0, nv, 0, d.used);
                keys = d.keys;
                hashes = d.hashes;
                index = d.index;
                values = nv;
                layout = d.layout;
                used = d.used;
                size = d.size;
                return;
            }
            if (size == 0 && used == 0 && d.size <= LINEAR && getClass() == PyDict.class) {
                // `dict(small)`, as every `self.attrib = dict(attrib)` is: the
                // entries and their hashes copied once into arrays of the right
                // size, rather than hashed again and grown on the way.
                int cap = Math.max(4, d.size);
                Object[] nk = new Object[cap];
                Object[] nv = new Object[cap];
                long[] nh = new long[cap];
                int w = 0;
                for (int i = 0; i < d.used; i++) {
                    if (d.keys[i] != null) {
                        nk[w] = d.keys[i];
                        nv[w] = d.values[i];
                        nh[w] = d.hashes[i];
                        w++;
                    }
                }
                keys = nk;
                values = nv;
                hashes = nh;
                used = w;
                size = w;
                index = null;
                layout = null;
                return;
            }
            for (int i = 0; i < d.used; i++) {
                if (d.keys[i] != null) {
                    setItem(d.keys[i], d.values[i]);
                }
            }
            return;
        }
        if (other instanceof PyList) {
            // Pairs read straight off the list, its length read each step;
            // a list has no keys() to look up first, and the table is sized
            // once for every pair rather than grown on the way.
            PyList l = (PyList) other;
            if (size == 0 && getClass() == PyDict.class) {
                reserve(l.size);
            }
            for (int n = 0; n < l.size; n++) {
                pairItem(l.get(n), n);
            }
            return;
        }
        if (other instanceof PyTuple || other instanceof PyGenerator || other instanceof Builtins.FilterIter
                || other instanceof Builtins.MapIter) {
            // None of these has keys(): no attribute lookup per dict() call.
            updatePairs(other);
            return;
        }
        Object keysFn;
        if (!(other instanceof PyInstance)) {
            // A builtin's attributes are its type's: no __getattr__ can run,
            // so no handler (a setjmp per dict() call on ParparVM).
            keysFn = Py.obj(other).getAttrOrNull("keys");
        } else {
            keysFn = instanceKeys(other);
        }
        if (keysFn != null) {
            Object it = Py.iter(Py.obj(keysFn).call(Py.NO_ARGS, Py.NO_KW));
            for (Object k = Py.next(it); k != null; k = Py.next(it)) {
                setItem(k, Py.obj(other).getItem(k));
            }
            return;
        }
        updatePairs(other);
    }

    /// The pairs of an iterable with no keys(): a generator drained under
    /// one handler (a setjmp per value otherwise), a filter or map stepped
    /// by a direct call.
    private void updatePairs(Object other) {
        Object it = Py.iter(other);
        // dict(pairs) is usually built for more than the four entries a
        // dict's first growth makes room for: two growths become one.
        reserve(8);
        int n = 0;
        if (it instanceof PyGenerator) {
            PyGenerator g = (PyGenerator) it;
            try {
                for (Object pair = PyGenerator.loopNext(g); pair != null; pair = PyGenerator.loopNext(g), n++) {
                    pairItem(pair, n);
                }
            } catch (Throwable e) {
                throw PyGenerator.loopEscape(g, e);
            }
            return;
        }
        if (it instanceof Builtins.FilterIter) {
            Builtins.FilterIter f = (Builtins.FilterIter) it;
            for (Object pair = f.next(); pair != null; pair = f.next(), n++) {
                pairItem(pair, n);
            }
            return;
        }
        for (Object pair = Py.next(it); pair != null; pair = Py.next(it), n++) {
            pairItem(pair, n);
        }
    }

    private void pairItem(Object pair, int n) {
        Object[] kv;
        if (pair instanceof PyTuple) {
            kv = ((PyTuple) pair).items;
        } else if (pair instanceof PyList) {
            kv = ((PyList) pair).toArray();
        } else {
            Object it;
            try {
                it = Py.iter(pair);
            } catch (PyException e) {
                // CPython 3.14's wording for an element that is no sequence.
                if (e.matches(Py.TYPE_ERROR)) {
                    throw Py.typeError("object is not iterable"); //NOPMD PreserveStackTrace - CPython raises a fresh TypeError here too
                }
                throw e;
            }
            kv = Py.toArray(it);
        }
        if (kv.length != 2) {
            throw Py.valueError("dictionary update sequence element #" + n + " has length " + kv.length
                    + "; 2 is required");
        }
        setItem(kv[0], kv[1]);
    }

    private static Object instanceKeys(Object other) {
        Object keysFn;
        try {
            keysFn = Py.obj(other).getAttrOrNull("keys");
        } catch (PyException e) {
            // A __getattr__ that raises AttributeError means "no keys", and
            // the argument is read as pairs (dict(pydantic_model)).
            if (!e.matches(Py.ATTRIBUTE_ERROR)) {
                throw e;
            }
            keysFn = null;
        }
        return keysFn;
    }

    /// The live keys in order.
    public Object[] keyArray() {
        sync();
        Object[] r = new Object[size];
        int w = 0;
        for (int i = 0; i < used; i++) {
            if (keys[i] != null) {
                r[w++] = keys[i];
            }
        }
        return r;
    }

    /// Before each step of an iteration; `last` when the walk has no entry
    /// left, where a change of size must still be found. A view can do less
    /// than a full `sync` there, as long as what the step returns is current.
    void syncStep(boolean last) {
        sync();
    }

    /// [#sync] before an iteration, whose every step brings what it returns
    /// up to date through [#syncStep] and [#valueAt]: a view need not
    /// re-read every value first.
    void syncForWalk() {
        sync();
    }

    /// Whether a `for k, v in d.items()` may walk this dict through
    /// [#sync], [#syncStep] and [#valueAt] instead of calling its items():
    /// true for a view whose items() is exactly that walk.
    boolean walkAsView() {
        return false;
    }

    /// The value of live entry `e`, for an iteration step.
    Object valueAt(int e) {
        return values[e];
    }

    /// Iterates over entries; `kind` 0 = keys, 1 = values, 2 = items.
    Object iterate(final int kind) {
        syncForWalk();
        final int startSize = size;
        return new PyJavaIter(new PyJavaIter.Source() {
            int i;

            @Override
            public Object next() {
                // A view (an instance's __dict__) picks up what changed in the
                // object behind it, so a store made through the object while
                // iterating is caught as one made through the dict is. A plain
                // dict's sync does nothing.
                int j = i;
                while (j < used && keys[j] == null) {
                    j++;
                }
                syncStep(j >= used);
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
                    return valueAt(e);
                }
                return new PyTuple(new Object[] {keys[e], valueAt(e)});
            }
        });
    }

    @Override
    public Object iter() {
        return iterate(0);
    }

    @Override
    public String repr() {
        sync();
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
        sync();
        if (o instanceof PyDict) {
            ((PyDict) o).sync();
        }
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
