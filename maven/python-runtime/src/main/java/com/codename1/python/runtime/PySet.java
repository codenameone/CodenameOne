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

/// Python's `set` and `frozenset`.
///
/// The table follows CPython's `setobject.c` -- open addressing with the
/// same size policy, linear probes, perturbation and dummy entries -- so a
/// set iterates in the same order as on CPython wherever CPython's own
/// order is deterministic (numbers and tuples; str hashes are randomized
/// per process there, so no program can depend on their order).
public final class PySet extends PyObject {
    private static final int MINSIZE = 8;
    private static final int LINEAR_PROBES = 9;
    private static final int PERTURB_SHIFT = 5;
    /// Marks a deleted entry; its hash slot holds -1.
    private static final Object DUMMY = new PyObject() {
        @Override
        public PyType type() {
            return Py.OBJECT_TYPE;
        }
    };

    final boolean frozen;
    private Object[] keys = new Object[MINSIZE];
    private long[] hashes = new long[MINSIZE];
    private int mask = MINSIZE - 1;
    /// Active plus dummy entries.
    private int fill;
    /// Active entries.
    private int used;
    /// Where pop() resumes its search.
    private int finger;

    public PySet(boolean frozen) {
        this.frozen = frozen;
    }

    public PySet() {
        this(false);
    }

    public static PySet of(Object iterable, boolean frozen) {
        PySet s = new PySet(frozen);
        s.update(iterable);
        return s;
    }

    @Override
    public PyType type() {
        return frozen ? Py.FROZENSET_TYPE : Py.SET_TYPE;
    }

    private void checkMutable() {
        if (frozen) {
            throw Py.attributeError("'frozenset' object is immutable");
        }
    }

    // ---- the table -------------------------------------------------------

    private static boolean same(Object a, Object b) {
        return a == b || Py.eqBool(a, b); //NOPMD CompareObjectsWithEquals - identity is meant
    }

    private void insert(Object key, long hash) {
        long perturb = hash;
        int i = (int) (hash & mask);
        int freeslot = -1;
        while (true) {
            int probes = i + LINEAR_PROBES <= mask ? LINEAR_PROBES : 0;
            int j = i;
            while (true) {
                Object k = keys[j];
                if (k == null) {
                    if (freeslot >= 0) {
                        keys[freeslot] = key;
                        hashes[freeslot] = hash;
                        used++;
                        return;
                    }
                    keys[j] = key;
                    hashes[j] = hash;
                    fill++;
                    used++;
                    if ((long) fill * 5 >= (long) mask * 3) {
                        resize(used > 50000 ? used * 2 : used * 4);
                    }
                    return;
                }
                if (k == DUMMY) {
                    if (freeslot < 0) {
                        freeslot = j;
                    }
                } else if (hashes[j] == hash && same(k, key)) {
                    return;
                }
                if (probes == 0) {
                    break;
                }
                probes--;
                j++;
            }
            perturb >>>= PERTURB_SHIFT;
            i = (int) ((i * 5L + 1 + perturb) & mask);
        }
    }

    private int find(Object key, long hash) {
        long perturb = hash;
        int i = (int) (hash & mask);
        while (true) {
            int probes = i + LINEAR_PROBES <= mask ? LINEAR_PROBES : 0;
            int j = i;
            while (true) {
                Object k = keys[j];
                if (k == null) {
                    return -1;
                }
                if (k != DUMMY && hashes[j] == hash && same(k, key)) {
                    return j;
                }
                if (probes == 0) {
                    break;
                }
                probes--;
                j++;
            }
            perturb >>>= PERTURB_SHIFT;
            i = (int) ((i * 5L + 1 + perturb) & mask);
        }
    }

    /// `set_insert_clean`: into a table with no dummies and no equal key.
    private static void insertClean(Object[] ks, long[] hs, int m, Object key, long hash) {
        long perturb = hash;
        int i = (int) (hash & m);
        while (true) {
            if (ks[i] == null) {
                ks[i] = key;
                hs[i] = hash;
                return;
            }
            if (i + LINEAR_PROBES <= m) {
                for (int j = 1; j <= LINEAR_PROBES; j++) {
                    if (ks[i + j] == null) {
                        ks[i + j] = key;
                        hs[i + j] = hash;
                        return;
                    }
                }
            }
            perturb >>>= PERTURB_SHIFT;
            i = (int) ((i * 5L + 1 + perturb) & m);
        }
    }

    private void resize(int minused) {
        int size = MINSIZE;
        while (size <= minused) {
            size <<= 1;
        }
        Object[] oldKeys = keys;
        long[] oldHashes = hashes;
        keys = new Object[size];
        hashes = new long[size];
        mask = size - 1;
        for (int i = 0; i < oldKeys.length; i++) {
            Object k = oldKeys[i];
            if (k != null && k != DUMMY) {
                insertClean(keys, hashes, mask, k, oldHashes[i]);
            }
        }
        fill = used;
    }

    private static long hashOf(Object key) {
        return Py.keyHash(key, "a set element");
    }

    // ---- element operations ----------------------------------------------

    public void addItem(Object v) {
        checkMutable();
        insert(v, hashOf(v));
    }

    /// Adds without the frozen check, for building a new set.
    void addRaw(Object v) {
        insert(v, hashOf(v));
    }

    public boolean discard(Object v) {
        checkMutable();
        int i = find(v, hashOf(v));
        if (i < 0) {
            return false;
        }
        keys[i] = DUMMY;
        hashes[i] = -1;
        used--;
        return true;
    }

    public void clear() {
        checkMutable();
        keys = new Object[MINSIZE];
        hashes = new long[MINSIZE];
        mask = MINSIZE - 1;
        fill = 0;
        used = 0;
        finger = 0;
    }

    /// `set.pop()`: CPython's finger walk, so successive pops come out in
    /// table order.
    public Object pop() {
        checkMutable();
        if (used == 0) {
            throw Py.error(Py.KEY_ERROR, "pop from an empty set");
        }
        int i = finger & mask;
        while (keys[i] == null || keys[i] == DUMMY) {
            i++;
            if (i > mask) {
                i = 0;
            }
        }
        Object k = keys[i];
        keys[i] = DUMMY;
        hashes[i] = -1;
        used--;
        finger = i + 1;
        return k;
    }

    /// The members in iteration order.
    public Object[] toArray() {
        Object[] r = new Object[used];
        int n = 0;
        for (int i = 0; i <= mask; i++) {
            Object k = keys[i];
            if (k != null && k != DUMMY) {
                r[n++] = k;
            }
        }
        return r;
    }

    /// `set_update_internal`: a set or dict merges in table/insertion order
    /// after sizing the table once, as CPython does.
    void update(Object iterable) {
        if (iterable instanceof PySet) {
            merge((PySet) iterable);
            return;
        }
        if (iterable instanceof PyDict) {
            PyDict d = (PyDict) iterable;
            int n = d.len();
            if ((long) (fill + n) * 5 >= (long) mask * 3) {
                resize((used + n) * 2);
            }
            Object[] ks = d.keyArray();
            for (Object e : ks) {
                insert(e, hashOf(e));
            }
            return;
        }
        Object it = Py.iter(iterable);
        for (Object v = Py.next(it); v != null; v = Py.next(it)) {
            insert(v, hashOf(v));
        }
    }

    /// `set_merge`.
    /// A set display whose elements are all constants: CPython compiles it
    /// to a frozenset constant merged into an empty set, which sizes the
    /// table differently from adding the elements one by one, and so
    /// iterates in a different order.
    public static PySet ofConstants(Object... items) {
        PySet constant = new PySet(true);
        for (Object item : items) {
            constant.addRaw(item);
        }
        PySet s = new PySet();
        s.merge(constant);
        return s;
    }

    private void merge(PySet other) {
        if (other == this || other.used == 0) { //NOPMD CompareObjectsWithEquals - identity is meant
            return;
        }
        if ((long) (fill + other.used) * 5 >= (long) mask * 3) {
            resize((used + other.used) * 2);
        }
        if (fill == 0 && mask == other.mask && other.fill == other.used) {
            for (int i = 0; i <= other.mask; i++) {
                if (other.keys[i] != null) {
                    keys[i] = other.keys[i];
                    hashes[i] = other.hashes[i];
                }
            }
            fill = other.used;
            used = other.used;
            return;
        }
        if (fill == 0) {
            for (int i = 0; i <= other.mask; i++) {
                Object k = other.keys[i];
                if (k != null && k != DUMMY) {
                    insertClean(keys, hashes, mask, k, other.hashes[i]);
                    fill++;
                    used++;
                }
            }
            return;
        }
        for (int i = 0; i <= other.mask; i++) {
            Object k = other.keys[i];
            if (k != null && k != DUMMY) {
                insert(k, other.hashes[i]);
            }
        }
    }

    PySet copyAs(boolean asFrozen) {
        PySet r = new PySet(asFrozen);
        r.merge(this);
        return r;
    }

    @Override
    public boolean contains(Object x) {
        return find(x, hashOf(x)) >= 0;
    }

    @Override
    public int len() {
        return used;
    }

    @Override
    public boolean isTrue() {
        return used != 0;
    }

    @Override
    public Object iter() {
        final int expected = used;
        return new PyJavaIter(new PyJavaIter.Source() {
            int pos;

            @Override
            public Object next() {
                if (used != expected) {
                    throw Py.runtimeError("Set changed size during iteration");
                }
                while (pos <= mask) {
                    Object k = keys[pos++];
                    if (k != null && k != DUMMY) {
                        return k;
                    }
                }
                return null;
            }
        });
    }

    /// CPython's `frozenset_hash`, so equal frozensets hash alike regardless
    /// of insertion history.
    @Override
    public long hash() {
        if (!frozen) {
            throw Py.typeError("unhashable type: 'set'");
        }
        long hash = 0;
        for (int i = 0; i <= mask; i++) {
            Object k = keys[i];
            if (k != null && k != DUMMY) {
                long h = hashes[i];
                hash ^= ((h ^ 89869747L) ^ (h << 16)) * 3644798167L;
            }
        }
        hash ^= ((long) used + 1) * 1927868237L;
        hash ^= (hash >>> 11) ^ (hash >>> 25);
        hash = hash * 69069L + 907133923L;
        return hash == -1 ? 590923713L : hash;
    }

    @Override
    public String repr() {
        if (used == 0) {
            return frozen ? "frozenset()" : "set()";
        }
        Object[] k = toArray();
        String body = Py.joinRepr("{", k, k.length, "}");
        return frozen ? "frozenset(" + body + ")" : body;
    }

    private static PySet asSet(Object o) {
        return o instanceof PySet ? (PySet) o : null;
    }

    private static PySet setOf(Object o) {
        PySet s = asSet(o);
        return s != null ? s : of(o, true);
    }

    // ---- set algebra, in CPython's iteration orders ---------------------

    public PySet union(Object o) {
        PySet r = copyAs(frozen);
        r.update(o);
        return r;
    }

    public PySet intersection(Object o) {
        PySet other = setOf(o);
        PySet big = this;
        PySet small = other;
        if (other.used > used) {
            big = other;
            small = this;
        }
        PySet r = new PySet(frozen);
        for (int i = 0; i <= small.mask; i++) {
            Object k = small.keys[i];
            if (k != null && k != DUMMY && big.find(k, small.hashes[i]) >= 0) {
                r.insert(k, small.hashes[i]);
            }
        }
        return r;
    }

    public PySet difference(Object o) {
        PySet other = setOf(o);
        PySet r = new PySet(frozen);
        for (int i = 0; i <= mask; i++) {
            Object k = keys[i];
            if (k != null && k != DUMMY && other.find(k, hashes[i]) < 0) {
                r.insert(k, hashes[i]);
            }
        }
        return r;
    }

    public PySet symmetricDifference(Object o) {
        PySet other = setOf(o);
        PySet r = copyAs(frozen);
        for (int i = 0; i <= other.mask; i++) {
            Object k = other.keys[i];
            if (k == null || k == DUMMY) {
                continue;
            }
            int at = r.find(k, other.hashes[i]);
            if (at >= 0) {
                r.keys[at] = DUMMY;
                r.hashes[at] = -1;
                r.used--;
            } else {
                r.insert(k, other.hashes[i]);
            }
        }
        return r;
    }

    public boolean isSubset(Object o) {
        PySet other = setOf(o);
        if (used > other.used) {
            return false;
        }
        for (int i = 0; i <= mask; i++) {
            Object k = keys[i];
            if (k != null && k != DUMMY && other.find(k, hashes[i]) < 0) {
                return false;
            }
        }
        return true;
    }

    /// In-place forms: `|=`, `&=`, `-=`, `^=` keep the object's identity.
    void replaceWith(PySet s) {
        keys = s.keys;
        hashes = s.hashes;
        mask = s.mask;
        fill = s.fill;
        used = s.used;
        finger = 0;
    }

    @Override
    public Object inplace(String name, Object o) {
        if (frozen || asSet(o) == null) {
            return null;
        }
        switch (name) {
            case "__ior__":
                update(o);
                return this;
            case "__iand__":
                replaceWith(intersection(o));
                return this;
            case "__isub__":
                replaceWith(difference(o));
                return this;
            case "__ixor__":
                replaceWith(symmetricDifference(o));
                return this;
            default:
                return null;
        }
    }

    @Override
    public Object or(Object o) {
        return asSet(o) == null ? null : union(o);
    }

    @Override
    public Object and(Object o) {
        return asSet(o) == null ? null : intersection(o);
    }

    @Override
    public Object sub(Object o) {
        return asSet(o) == null ? null : difference(o);
    }

    @Override
    public Object xor(Object o) {
        return asSet(o) == null ? null : symmetricDifference(o);
    }

    @Override
    public Object eq(Object o) {
        PySet b = asSet(o);
        if (b == null) {
            return null;
        }
        return Py.bool(b.used == used && isSubset(b));
    }

    @Override
    public Object le(Object o) {
        return asSet(o) == null ? null : Py.bool(isSubset(o));
    }

    @Override
    public Object lt(Object o) {
        PySet b = asSet(o);
        return b == null ? null : Py.bool(used < b.used && isSubset(b));
    }

    @Override
    public Object ge(Object o) {
        PySet b = asSet(o);
        return b == null ? null : Py.bool(b.isSubset(this));
    }

    @Override
    public Object gt(Object o) {
        PySet b = asSet(o);
        return b == null ? null : Py.bool(used > b.used && b.isSubset(this));
    }
}
