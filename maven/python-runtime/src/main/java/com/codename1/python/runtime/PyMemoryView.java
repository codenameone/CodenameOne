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

/// `memoryview` over bytes or a bytearray: a window of unsigned bytes
/// (format `B`) that shares the bytearray's storage, so writes through the
/// view land in it. Only one-dimensional byte views are supported; `cast`
/// to another format is not.
public final class PyMemoryView extends PyObject {
    private final Object obj;
    private final int start;
    private final int step;
    private final int length;
    private boolean released;

    private PyMemoryView(Object obj, int start, int step, int length) {
        this.obj = obj;
        this.start = start;
        this.step = step;
        this.length = length;
    }

    @Override
    public PyType type() {
        return Py.MEMORYVIEW_TYPE;
    }

    private byte[] store() {
        if (released) {
            throw Py.valueError("operation forbidden on released memoryview object");
        }
        if (obj instanceof PyByteArray) {
            return ((PyByteArray) obj).buf;
        }
        return ((PyBytes) obj).value;
    }

    byte[] bytes() {
        byte[] s = store();
        byte[] r = new byte[length];
        for (int i = 0; i < length; i++) {
            r[i] = s[start + i * step];
        }
        return r;
    }

    @Override
    public int len() {
        store();
        return length;
    }

    @Override
    public String repr() {
        return released ? "<released memory at 0x" + Integer.toHexString(System.identityHashCode(this)) + ">"
                : "<memory at 0x" + Integer.toHexString(System.identityHashCode(this)) + ">";
    }

    @Override
    public long hash() {
        if (obj instanceof PyByteArray) {
            throw Py.valueError("cannot hash writable memoryview object");
        }
        return new PyBytes(bytes()).hash();
    }

    @Override
    public Object eq(Object o) {
        if (o == this) { //NOPMD CompareObjectsWithEquals - identity is meant
            return Py.TRUE;
        }
        byte[] b = PyBytes.like(o);
        if (b == null) {
            return Py.FALSE;
        }
        return Py.bool(PyBytes.same(bytes(), b));
    }

    @Override
    public Object getItem(Object key) {
        store();
        if (key instanceof PySlice) {
            int[] r = ((PySlice) key).indices(length);
            return new PyMemoryView(obj, start + r[0] * step, step * r[2], PySlice.length(r));
        }
        return PyInt.of(store()[start + index(key) * step] & 0xff);
    }

    private int index(Object key) {
        long i = Py.index(key);
        if (i < 0) {
            i += length;
        }
        if (i < 0 || i >= length) {
            throw Py.indexError("index out of bounds on dimension 1");
        }
        return (int) i;
    }

    @Override
    public void setItem(Object key, Object value) {
        if (!(obj instanceof PyByteArray)) {
            throw Py.typeError("cannot modify read-only memory");
        }
        byte[] s = store();
        if (key instanceof PySlice) {
            int[] r = ((PySlice) key).indices(length);
            int n = PySlice.length(r);
            byte[] src = PyBytes.like(value);
            if (src == null) {
                throw Py.typeError("a bytes-like object is required, not '" + Py.typeOf(value).name + "'");
            }
            if (src.length != n) {
                throw Py.valueError("memoryview assignment: lvalue and rvalue have different structures");
            }
            for (int i = 0; i < n; i++) {
                s[start + (r[0] + i * r[2]) * step] = src[i];
            }
            return;
        }
        long v = Py.asLong(value);
        if (v < 0 || v > 255) {
            throw Py.valueError("memoryview: invalid value for format 'B'");
        }
        s[start + index(key) * step] = (byte) v;
    }

    @Override
    public Object iter() {
        return byteIter(bytes());
    }

    private static PyIter byteIter(final byte[] b) {
        return new PyIter(b.length) {
            @Override
            Object at(int i) {
                return PyInt.of(b[i] & 0xff);
            }
        };
    }

    @Override
    public Object getAttrOrNull(String name) {
        if ("obj".equals(name)) {
            store();
            return obj;
        }
        if ("nbytes".equals(name)) {
            store();
            return PyInt.of(length);
        }
        if ("readonly".equals(name)) {
            store();
            return Py.bool(!(obj instanceof PyByteArray));
        }
        if ("format".equals(name)) {
            return Py.str("B");
        }
        if ("itemsize".equals(name) || "ndim".equals(name)) {
            return PyInt.of(1);
        }
        if ("shape".equals(name)) {
            return new PyTuple(new Object[] {PyInt.of(length)});
        }
        if ("strides".equals(name)) {
            return new PyTuple(new Object[] {PyInt.of(step)});
        }
        if ("contiguous".equals(name) || "c_contiguous".equals(name) || "f_contiguous".equals(name)) {
            return Py.bool(step == 1);
        }
        return super.getAttrOrNull(name);
    }

    private static PyMemoryView self(Object o) {
        if (!(o instanceof PyMemoryView)) {
            throw Py.typeError("descriptor requires a 'memoryview' object but received a '"
                    + Py.typeOf(o).name + "'");
        }
        return (PyMemoryView) o;
    }

    static void install() {
        PyType t = Py.MEMORYVIEW_TYPE;
        t.define("tobytes", PyBuiltin.method(t, "tobytes", a -> new PyBytes(self(a[0]).bytes())));
        t.define("tolist", PyBuiltin.method(t, "tolist", a -> {
            byte[] b = self(a[0]).bytes();
            Object[] r = new Object[b.length];
            for (int i = 0; i < b.length; i++) {
                r[i] = PyInt.of(b[i] & 0xff);
            }
            return new PyList(r);
        }));
        t.define("hex", PyBuiltin.method(t, "hex", a -> Py.call(Py.BYTES_TYPE.lookup("hex"),
                new PyBytes(self(a[0]).bytes()))));
        t.define("release", PyBuiltin.method(t, "release", a -> {
            self(a[0]).released = true;
            return Py.NONE;
        }));
        t.define("toreadonly", PyBuiltin.method(t, "toreadonly", a -> {
            PyMemoryView m = self(a[0]);
            return new PyMemoryView(new PyBytes(m.bytes()), 0, 1, m.length);
        }));
        t.define("__enter__", PyBuiltin.method(t, "__enter__", a -> self(a[0])));
        t.define("__exit__", PyBuiltin.method(t, "__exit__", a -> {
            self(a[0]).released = true;
            return Py.FALSE;
        }));
        t.factory = (type, args, kw) -> {
            Object o = args.length > 0 ? args[0] : null;
            if (o instanceof PyBytes) {
                return new PyMemoryView(o, 0, 1, ((PyBytes) o).value.length);
            }
            if (o instanceof PyByteArray) {
                return new PyMemoryView(o, 0, 1, ((PyByteArray) o).size);
            }
            if (o instanceof PyMemoryView) {
                PyMemoryView m = (PyMemoryView) o;
                return new PyMemoryView(m.obj, m.start, m.step, m.length);
            }
            throw Py.typeError("memoryview: a bytes-like object is required, not '"
                    + (o == null ? "NoneType" : Py.typeOf(o).name) + "'");
        };
    }
}
