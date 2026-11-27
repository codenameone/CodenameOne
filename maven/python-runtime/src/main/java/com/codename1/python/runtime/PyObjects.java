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

/// `_cn1obj`: an instance made with all of its attributes in one call.
///
/// `make(cls, "a\nb\nc", va, vb, vc)` (the names one a line) answers what `object.__new__(cls)`
/// followed by `o.a = va; o.b = vb; o.c = vc` does. The standard library
/// builds value objects that way where it has already validated their
/// fields -- datetime above all: a `datetime` sets ten attributes, and set
/// one at a time each store walked a shape transition and grew the slot
/// array, which with the collector it fed was most of
/// `datetime.now(timezone.utc)` (measured against CPython, whose datetime is
/// one C allocation).
///
/// The names are a constant of the calling module, so the shape they make
/// is looked up by that string's identity. A class whose instances are not
/// plain -- a compiled layout, a builtin base -- gets the attributes stored
/// one by one, through `setattr`, exactly as the Python would.
final class PyObjects {
    private static final int CACHE = 16;
    private static final String[] KEYS = new String[CACHE];
    private static final Shape[] SHAPES = new Shape[CACHE];
    private static final String[][] NAMES = new String[CACHE][];

    private PyObjects() {
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1obj", new String[0]);
        m.setAttr("make", PyBuiltin.positional("make", PyObjects::make));
        return m;
    }

    private static Object make(Object[] a) {
        if (a.length < 2 || !(a[0] instanceof PyType) || !(a[1] instanceof String)) {
            throw Py.error(Py.TYPE_ERROR, "make(cls, names, *values)");
        }
        return build((PyType) a[0], (String) a[1], a, 2);
    }

    /// The instance of `cls` with the attributes `key` names (one a line)
    /// set to `a[from]`, `a[from + 1]` and on.
    static PyInstance build(PyType cls, String key, Object[] a, int from) {
        int slot = (System.identityHashCode(key) & 0x7fffffff) % CACHE;
        if (KEYS[slot] != key) { //NOPMD CompareObjectsWithEquals - the cache is keyed by identity
            String[] names = Py.names(key);
            Shape s = Shape.EMPTY;
            for (int i = 0; i < names.length; i++) {
                names[i] = names[i].intern();
                s = s.with(names[i]);
            }
            NAMES[slot] = names;
            SHAPES[slot] = s;
            KEYS[slot] = key;
        }
        String[] names = NAMES[slot];
        int n = names.length;
        if (a.length != n + from) {
            throw Py.error(Py.TYPE_ERROR, "make() takes " + n + " values, got " + (a.length - from));
        }
        PyInstance o = cls.newInstance();
        if (o.getClass() == PyInstance.class && o.base == null && o.shape == null) { //NOPMD CompareObjectsWithEquals - exactly the plain instance class
            Object[] slots = new Object[n];
            System.arraycopy(a, from, slots, 0, n);
            o.slots = slots;
            o.shape = SHAPES[slot];
            o.extra = true;
            return o;
        }
        for (int i = 0; i < n; i++) {
            Py.setattr(o, names[i], a[i + from]);
        }
        return o;
    }
}
