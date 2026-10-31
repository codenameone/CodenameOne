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
public final class PyTuple extends PyObject {
    public static final PyTuple EMPTY = new PyTuple(new Object[0]);

    final Object[] items;

    /// Wraps `items` without copying; the caller must not keep mutating it.
    public PyTuple(Object[] items) {
        this.items = items;
    }

    public static PyTuple of(Object... items) {
        return items.length == 0 ? EMPTY : new PyTuple(items);
    }

    @Override
    public PyType type() {
        return Py.TUPLE_TYPE;
    }

    public Object get(int i) {
        return items[i];
    }

    public int size() {
        return items.length;
    }

    @Override
    public int len() {
        return items.length;
    }

    @Override
    public boolean isTrue() {
        return items.length != 0;
    }

    @Override
    public String repr() {
        if (items.length == 1) {
            return "(" + Py.toRepr(items[0]) + ",)";
        }
        return Py.joinRepr("(", items, items.length, ")");
    }

    @Override
    public long hash() {
        return Hashing.ofTuple(items);
    }

    @Override
    public Object eq(Object o) {
        if (!(o instanceof PyTuple)) {
            return null;
        }
        Object[] b = ((PyTuple) o).items;
        return Py.bool(Py.seqEquals(items, items.length, b, b.length));
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
        Object[] b = ((PyTuple) o).items;
        return Py.seqCompare(items, items.length, b, b.length);
    }

    @Override
    public Object add(Object o) {
        if (!(o instanceof PyTuple)) {
            return null;
        }
        Object[] b = ((PyTuple) o).items;
        Object[] r = new Object[items.length + b.length];
        System.arraycopy(items, 0, r, 0, items.length);
        System.arraycopy(b, 0, r, items.length, b.length);
        return new PyTuple(r);
    }

    @Override
    public Object mul(Object o) {
        if (!(Py.isInt(o))) {
            return null;
        }
        long n = Math.max(0, Py.asLong(o));
        Object[] r = new Object[(int) (items.length * n)];
        for (int i = 0; i < n; i++) {
            System.arraycopy(items, 0, r, i * items.length, items.length);
        }
        return new PyTuple(r);
    }

    @Override
    public Object rmul(Object o) {
        return mul(o);
    }

    @Override
    public Object getItem(Object key) {
        if (key instanceof PySlice) {
            int[] r = ((PySlice) key).indices(items.length);
            int n = PySlice.length(r);
            Object[] out = new Object[n];
            for (int i = 0, j = r[0]; i < n; i++, j += r[2]) {
                out[i] = items[j];
            }
            return new PyTuple(out);
        }
        long i = Py.index(key);
        if (i < 0) {
            i += items.length;
        }
        if (i < 0 || i >= items.length) {
            throw Py.indexError("tuple index out of range");
        }
        return items[(int) i];
    }

    @Override
    public Object iter() {
        return new PyIter(items.length) {
            @Override
            Object at(int i) {
                return items[i];
            }
        };
    }
}
