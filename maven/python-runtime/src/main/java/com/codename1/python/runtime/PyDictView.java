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

/// `dict.keys()`, `dict.values()` and `dict.items()`: live views.
final class PyDictView extends PyObject {
    private static final String[] NAMES = {"dict_keys", "dict_values", "dict_items"};
    private final PyDict d;
    private final int kind;

    PyDictView(PyDict d, int kind) {
        this.d = d;
        this.kind = kind;
    }

    @Override
    public PyType type() {
        return Py.ITERATOR_TYPE;
    }

    @Override
    public Object iter() {
        return d.iterate(kind);
    }

    @Override
    public int len() {
        return d.len();
    }

    @Override
    public boolean isTrue() {
        return d.isTrue();
    }

    @Override
    public boolean contains(Object x) {
        if (kind == 0) {
            return d.contains(x);
        }
        if (kind == 2) {
            Object[] kv = Py.unpack(x, 2);
            Object v = d.getOrNull(kv[0]);
            return v != null && Py.eqBool(v, kv[1]);
        }
        return super.contains(x);
    }

    @Override
    public String repr() {
        Object[] items = Py.toArray(this);
        return NAMES[kind] + Py.joinRepr("([", items, items.length, "])");
    }

    @Override
    public Object eq(Object o) {
        if (kind != 1 && (o instanceof PyDictView || o instanceof PySet)) {
            return Py.eq(PySet.of(this, true), PySet.of(o, true));
        }
        return null;
    }

    private Object setOp(Object o, int op) {
        if (kind == 1) {
            return null;
        }
        PySet a = PySet.of(this, false);
        switch (op) {
            case 0:
                return a.union(o);
            case 1:
                return a.intersection(o);
            case 2:
                return a.difference(o);
            default:
                return a.symmetricDifference(o);
        }
    }

    @Override
    public Object or(Object o) {
        return setOp(o, 0);
    }

    @Override
    public Object and(Object o) {
        return setOp(o, 1);
    }

    @Override
    public Object sub(Object o) {
        return setOp(o, 2);
    }

    @Override
    public Object xor(Object o) {
        return setOp(o, 3);
    }
}
