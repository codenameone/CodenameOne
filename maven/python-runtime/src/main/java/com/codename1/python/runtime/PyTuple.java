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

/// Python's `tuple`.
public class PyTuple extends PyObject {
    public static final PyTuple EMPTY = new PyTuple(new Object[0]);

    /// The items; null in a fixed-size tuple ([T2] to [T6]) until [#arr]
    /// is first asked for them.
    Object[] elems;

    /// Wraps `items` without copying; the caller must not keep mutating it.
    public PyTuple(Object[] items) {
        this.elems = items;
    }

    public static PyTuple of(Object... items) {
        return items.length == 0 ? EMPTY : new PyTuple(items);
    }

    /// The items as an array, which the caller must not change. A
    /// fixed-size tuple makes it on the first call; hot paths read
    /// [#get] and [#size] instead.
    Object[] arr() {
        return elems;
    }

    @Override
    public PyType type() {
        return Py.TUPLE_TYPE;
    }

    public Object get(int i) {
        return elems[i];
    }

    public int size() {
        return elems.length;
    }

    @Override
    public int len() {
        return size();
    }

    @Override
    public boolean isTrue() {
        return size() != 0;
    }

    @Override
    public String repr() {
        if (size() == 1) {
            return "(" + Py.toRepr(get(0)) + ",)";
        }
        Object[] a = arr();
        return Py.joinRepr("(", a, a.length, ")");
    }

    @Override
    public long hash() {
        return Hashing.ofTuple(arr());
    }

    @Override
    public Object eq(Object o) {
        if (!(o instanceof PyTuple)) {
            return null;
        }
        PyTuple t = (PyTuple) o;
        int n = size();
        if (t.size() != n) {
            return Py.FALSE;
        }
        for (int i = 0; i < n; i++) {
            Object x = get(i);
            Object y = t.get(i);
            if (x != y && !Py.eqBool(x, y)) { //NOPMD CompareObjectsWithEquals - identity is meant
                return Py.FALSE;
            }
        }
        return Py.TRUE;
    }

    @Override
    public Object lt(Object o) {
        return o instanceof PyTuple ? Py.bool(cmp(o) < 0) : null;
    }

    @Override
    public Object le(Object o) {
        return o instanceof PyTuple ? Py.bool(cmp(o) <= 0) : null;
    }

    @Override
    public Object gt(Object o) {
        return o instanceof PyTuple ? Py.bool(cmp(o) > 0) : null;
    }

    @Override
    public Object ge(Object o) {
        return o instanceof PyTuple ? Py.bool(cmp(o) >= 0) : null;
    }

    private int cmp(Object o) {
        Object[] a = arr();
        Object[] b = ((PyTuple) o).arr();
        return Py.seqCompare(a, a.length, b, b.length);
    }

    @Override
    public Object add(Object o) {
        if (!(o instanceof PyTuple)) {
            return null;
        }
        Object[] a = arr();
        Object[] b = ((PyTuple) o).arr();
        Object[] r = new Object[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return new PyTuple(r);
    }

    @Override
    public Object mul(Object o) {
        if (!(Py.isInt(o))) {
            return null;
        }
        Object[] a = arr();
        long n = Math.max(0, Py.asLong(o));
        Object[] r = new Object[(int) (a.length * n)];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a, 0, r, i * a.length, a.length);
        }
        return new PyTuple(r);
    }

    @Override
    public Object rmul(Object o) {
        return mul(o);
    }

    @Override
    public Object getItem(Object key) {
        int n = size();
        if (key instanceof PySlice) {
            Object[] a = arr();
            int[] r = ((PySlice) key).indices(n);
            int m = PySlice.length(r);
            Object[] out = new Object[m];
            if (r[2] == 1) {
                // A plain slice is one block copy, not a checked store per element.
                System.arraycopy(a, r[0], out, 0, m);
                return new PyTuple(out);
            }
            for (int i = 0, j = r[0]; i < m; i++, j += r[2]) {
                out[i] = a[j];
            }
            return new PyTuple(out);
        }
        long i = Py.subscript(key);
        if (i < 0) {
            i += n;
        }
        if (i < 0 || i >= n) {
            throw Py.indexError("tuple index out of range");
        }
        return get((int) i);
    }

    /// By index, identity first as CPython's tuple compares: the inherited
    /// loop made an iterator and a `next` call per item for every `x in t`.
    @Override
    public boolean contains(Object x) {
        int n = size();
        for (int i = 0; i < n; i++) {
            Object v = get(i);
            if (v == x || Py.eqBool(v, x)) { //NOPMD CompareObjectsWithEquals - identity is meant
                return true;
            }
        }
        return false;
    }

    @Override
    public Object iter() {
        return new PyIter(size()) {
            @Override
            Object at(int i) {
                return get(i);
            }
        };
    }

    /// A tuple of 2 held in fields: one object where a wrapped array is two.
    static final class T2 extends PyTuple {
        private final Object a;
        private final Object b;

        T2(Object a, Object b) {
            super(null);
            this.a = a;
            this.b = b;
        }

        @Override
        public Object get(int i) {
            switch (i) {
                case 0:
                    return a;
                case 1:
                    return b;
                default:
                    throw new ArrayIndexOutOfBoundsException(i);
            }
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        Object[] arr() {
            Object[] r = elems;
            if (r == null) {
                r = new Object[] {a, b};
                elems = r;
            }
            return r;
        }
    }

    /// A tuple of 3 held in fields: one object where a wrapped array is two.
    static final class T3 extends PyTuple {
        private final Object a;
        private final Object b;
        private final Object c;

        T3(Object a, Object b, Object c) {
            super(null);
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public Object get(int i) {
            switch (i) {
                case 0:
                    return a;
                case 1:
                    return b;
                case 2:
                    return c;
                default:
                    throw new ArrayIndexOutOfBoundsException(i);
            }
        }

        @Override
        public int size() {
            return 3;
        }

        @Override
        Object[] arr() {
            Object[] r = elems;
            if (r == null) {
                r = new Object[] {a, b, c};
                elems = r;
            }
            return r;
        }
    }

    /// A tuple of 4 held in fields: one object where a wrapped array is two.
    static final class T4 extends PyTuple {
        private final Object a;
        private final Object b;
        private final Object c;
        private final Object d;

        T4(Object a, Object b, Object c, Object d) {
            super(null);
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        @Override
        public Object get(int i) {
            switch (i) {
                case 0:
                    return a;
                case 1:
                    return b;
                case 2:
                    return c;
                case 3:
                    return d;
                default:
                    throw new ArrayIndexOutOfBoundsException(i);
            }
        }

        @Override
        public int size() {
            return 4;
        }

        @Override
        Object[] arr() {
            Object[] r = elems;
            if (r == null) {
                r = new Object[] {a, b, c, d};
                elems = r;
            }
            return r;
        }
    }

    /// A tuple of 5 held in fields: one object where a wrapped array is two.
    static final class T5 extends PyTuple {
        private final Object a;
        private final Object b;
        private final Object c;
        private final Object d;
        private final Object e;

        T5(Object a, Object b, Object c, Object d, Object e) {
            super(null);
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
        }

        @Override
        public Object get(int i) {
            switch (i) {
                case 0:
                    return a;
                case 1:
                    return b;
                case 2:
                    return c;
                case 3:
                    return d;
                case 4:
                    return e;
                default:
                    throw new ArrayIndexOutOfBoundsException(i);
            }
        }

        @Override
        public int size() {
            return 5;
        }

        @Override
        Object[] arr() {
            Object[] r = elems;
            if (r == null) {
                r = new Object[] {a, b, c, d, e};
                elems = r;
            }
            return r;
        }
    }

    /// A tuple of 6 held in fields: one object where a wrapped array is two.
    static final class T6 extends PyTuple {
        private final Object a;
        private final Object b;
        private final Object c;
        private final Object d;
        private final Object e;
        private final Object f;

        T6(Object a, Object b, Object c, Object d, Object e, Object f) {
            super(null);
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            this.f = f;
        }

        @Override
        public Object get(int i) {
            switch (i) {
                case 0:
                    return a;
                case 1:
                    return b;
                case 2:
                    return c;
                case 3:
                    return d;
                case 4:
                    return e;
                case 5:
                    return f;
                default:
                    throw new ArrayIndexOutOfBoundsException(i);
            }
        }

        @Override
        public int size() {
            return 6;
        }

        @Override
        Object[] arr() {
            Object[] r = elems;
            if (r == null) {
                r = new Object[] {a, b, c, d, e, f};
                elems = r;
            }
            return r;
        }
    }
}
