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

/// `for k, v in d.items()`, in a statement or a comprehension, without an
/// items view, an iterator or a tuple per entry.
///
/// The generated code takes it only where the loop target is exactly two
/// names, so nothing can keep the pair: each step returns the walk itself,
/// and [Py#unpackNames] hands back [#current], which the generated code reads
/// at once, as it reads a tuple's own items. A plain dict is walked over its entry
/// arrays with the same size check its iterator makes. Anything else -- a dict
/// subclass, which may override `items`, or any object with an `items` method
/// -- has `items()` called as written, and each item unpacked as before.
public final class ItemWalk extends PyObject {
    private final Object[] pair = new Object[2];
    /// The pair of the last step: [#pair], or a tuple's own items.
    Object[] current;
    private final PyDict dict;
    private final int startSize;
    private final PyObject it;
    /// A dict that is a view of something else (an instance's `__dict__`),
    /// walked through its iteration hooks as its own items() would be.
    private final boolean view;
    private int at;

    private ItemWalk(PyDict dict, PyObject it, boolean view) {
        this.dict = dict;
        this.it = it;
        this.view = view;
        this.startSize = dict != null ? dict.size : 0;
    }

    /// The walk over `o.items()`.
    public static ItemWalk of(Object o) {
        if (o instanceof PyDict && o.getClass() == PyDict.class) {
            return new ItemWalk((PyDict) o, null, false);
        }
        if (o instanceof PyDict && ((PyDict) o).walkAsView()) {
            // As PyDict.iterate does: brought up to date first, then each
            // step through syncStep and valueAt -- no items() lookup and
            // no tuple per entry.
            PyDict d = (PyDict) o;
            d.sync();
            return new ItemWalk(d, null, true);
        }
        return new ItemWalk(null, Py.iter(Py.call(Py.getattr(o, "items"))), false);
    }

    @Override
    public PyType type() {
        return Py.ITERATOR_TYPE;
    }

    @Override
    public Object iter() {
        return this;
    }

    @Override
    public Object next() {
        PyDict d = dict;
        if (d == null) {
            Object v = it.next();
            if (v == null) {
                return null;
            }
            current = v instanceof PyTuple && ((PyTuple) v).arr().length == 2 ? ((PyTuple) v).arr()
                    : Py.unpack(v, 2);
            return this;
        }
        if (view) {
            Object[] ks = d.keys;
            int u = d.used;
            int j = at;
            while (j < u && ks[j] == null) {
                j++;
            }
            d.syncStep(j >= u);
        }
        if (d.size != startSize) {
            throw Py.runtimeError("dictionary changed size during iteration");
        }
        Object[] keys = d.keys;
        int used = d.used;
        int i = at;
        while (i < used && keys[i] == null) {
            i++;
        }
        if (i >= used) {
            return null;
        }
        at = i + 1;
        Object[] p = pair;
        p[0] = keys[i];
        p[1] = view ? d.valueAt(i) : d.values[i];
        current = p;
        return this;
    }
}
