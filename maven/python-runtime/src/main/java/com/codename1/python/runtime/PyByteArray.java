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

/// `bytearray`: a growable buffer of bytes.
///
/// The methods that read without changing it are `bytes`' own, run on a
/// snapshot; what they answer in bytes comes back as a bytearray, as
/// CPython's do.
public final class PyByteArray extends PyObject {
    private static final String[] READERS = {"decode", "hex", "upper", "lower", "isdigit", "isalpha",
        "isalnum", "isspace", "strip", "lstrip", "rstrip", "split", "rsplit", "startswith", "endswith", "find",
        "rfind", "index", "rindex", "count", "replace", "join", "partition", "rpartition", "removeprefix",
        "removesuffix", "splitlines", "center", "ljust", "rjust", "zfill", "expandtabs", "translate", "isascii",
        "islower", "isupper", "istitle", "swapcase", "capitalize", "title"};

    byte[] buf;
    int size;

    public PyByteArray(byte[] value) {
        this.buf = value;
        this.size = value.length;
    }

    @Override
    public PyType type() {
        return Py.BYTEARRAY_TYPE;
    }

    /// The live contents, copied.
    public byte[] bytes() {
        byte[] r = new byte[size];
        System.arraycopy(buf, 0, r, 0, size);
        return r;
    }

    @Override
    public Object toJava() {
        return bytes();
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
    public String repr() {
        return "bytearray(" + new PyBytes(bytes()).repr() + ")";
    }

    @Override
    public long hash() {
        throw Py.typeError("unhashable type: 'bytearray'");
    }

    @Override
    public Object eq(Object o) {
        byte[] b = PyBytes.like(o);
        return b == null ? null : Py.bool(PyBytes.same(bytes(), b));
    }

    @Override
    public Object lt(Object o) {
        byte[] b = PyBytes.like(o);
        return b == null ? null : Py.bool(PyBytes.compare(bytes(), b) < 0);
    }

    @Override
    public Object le(Object o) {
        byte[] b = PyBytes.like(o);
        return b == null ? null : Py.bool(PyBytes.compare(bytes(), b) <= 0);
    }

    @Override
    public Object gt(Object o) {
        byte[] b = PyBytes.like(o);
        return b == null ? null : Py.bool(PyBytes.compare(bytes(), b) > 0);
    }

    @Override
    public Object ge(Object o) {
        byte[] b = PyBytes.like(o);
        return b == null ? null : Py.bool(PyBytes.compare(bytes(), b) >= 0);
    }

    @Override
    public Object add(Object o) {
        byte[] b = PyBytes.like(o);
        if (b == null) {
            // Concatenation, not addition: only a class of the program's
            // own may still answer through __radd__.
            if (o instanceof PyInstance) {
                return null;
            }
            throw Py.typeError("can't concat " + Py.typeOf(o).name + " to bytearray");
        }
        byte[] r = new byte[size + b.length];
        System.arraycopy(buf, 0, r, 0, size);
        System.arraycopy(b, 0, r, size, b.length);
        return new PyByteArray(r);
    }

    @Override
    public Object iadd(Object o) {
        byte[] b = PyBytes.like(o);
        if (b == null) {
            return null;
        }
        insertAt(size, b);
        return this;
    }

    @Override
    public Object mul(Object o) {
        Object r = new PyBytes(bytes()).mul(o);
        return r instanceof PyBytes ? new PyByteArray(((PyBytes) r).value) : r;
    }

    @Override
    public Object rmul(Object o) {
        return mul(o);
    }

    @Override
    public Object inplace(String dunder, Object o) {
        if ("__imul__".equals(dunder) && Py.isInt(o)) {
            Object r = mul(o);
            byte[] b = ((PyByteArray) r).buf;
            buf = b;
            size = b.length;
            return this;
        }
        return null;
    }

    @Override
    public boolean contains(Object x) {
        return new PyBytes(bytes()).contains(x);
    }

    @Override
    public Object getItem(Object key) {
        if (!(key instanceof PySlice)) {
            return PyInt.of(buf[index(key)] & 0xff);
        }
        Object r = new PyBytes(bytes()).getItem(key);
        return r instanceof PyBytes ? new PyByteArray(((PyBytes) r).value) : r;
    }

    private static byte byteOf(Object v) {
        if (!Py.isInt(v)) {
            throw Py.typeError("'" + Py.typeOf(v).name + "' object cannot be interpreted as an integer");
        }
        long n = Py.asLong(v);
        if (n < 0 || n > 255) {
            throw Py.valueError("byte must be in range(0, 256)");
        }
        return (byte) n;
    }

    private int index(Object key) {
        long i = Py.index(key);
        if (i < 0) {
            i += size;
        }
        if (i < 0 || i >= size) {
            throw Py.indexError("bytearray index out of range");
        }
        return (int) i;
    }

    private void insertAt(int at, byte[] b) {
        if (size + b.length > buf.length) {
            byte[] n = new byte[Math.max(size + b.length, buf.length * 2 + 8)];
            System.arraycopy(buf, 0, n, 0, size);
            buf = n;
        }
        System.arraycopy(buf, at, buf, at + b.length, size - at);
        System.arraycopy(b, 0, buf, at, b.length);
        size += b.length;
    }

    private void removeRange(int lo, int hi) {
        System.arraycopy(buf, hi, buf, lo, size - hi);
        size -= hi - lo;
    }

    @Override
    public void setItem(Object key, Object value) {
        if (key instanceof PySlice) {
            int[] r = ((PySlice) key).indices(size);
            byte[] src = PyBytes.like(value);
            if (src == null) {
                if (Py.isInt(value)) {
                    throw Py.typeError("can assign only bytes, buffers, or iterables of ints in range(0, 256)");
                }
                src = PyBytes.fromInts(value);
            }
            if (r[2] == 1) {
                int lo = r[0];
                int hi = Math.max(r[0], r[1]);
                removeRange(lo, hi);
                insertAt(lo, src);
                return;
            }
            int count = PySlice.length(r);
            if (count != src.length) {
                throw Py.valueError("attempt to assign bytes of size " + src.length
                        + " to extended slice of size " + count);
            }
            for (int i = 0, j = r[0]; i < count; i++, j += r[2]) {
                buf[j] = src[i];
            }
            return;
        }
        buf[index(key)] = byteOf(value);
    }

    @Override
    public void delItem(Object key) {
        if (key instanceof PySlice) {
            int[] r = ((PySlice) key).indices(size);
            int count = PySlice.length(r);
            boolean[] drop = new boolean[size];
            for (int i = 0, j = r[0]; i < count; i++, j += r[2]) {
                drop[j] = true;
            }
            int w = 0;
            for (int i = 0; i < size; i++) {
                if (!drop[i]) {
                    buf[w++] = buf[i];
                }
            }
            size = w;
            return;
        }
        int i = index(key);
        removeRange(i, i + 1);
    }

    @Override
    public Object iter() {
        return new PyIter(size) {
            @Override
            Object at(int i) {
                return PyInt.of(buf[i] & 0xff);
            }

            @Override
            int liveLength() {
                return size;
            }
        };
    }

    private static PyByteArray self(Object o) {
        if (!(o instanceof PyByteArray)) {
            throw Py.typeError("descriptor requires a 'bytearray' object but received a '"
                    + Py.typeOf(o).name + "'");
        }
        return (PyByteArray) o;
    }

    private static Object toArray(Object o) {
        if (o instanceof PyBytes) {
            return new PyByteArray(((PyBytes) o).value);
        }
        if (o instanceof PyList || o instanceof PyTuple) {
            Object[] items = Py.toArray(o);
            Object[] r = new Object[items.length];
            for (int i = 0; i < items.length; i++) {
                r[i] = toArray(items[i]);
            }
            return o instanceof PyTuple ? new PyTuple(r) : new PyList(r);
        }
        return o;
    }

    static void install() {
        final PyType t = Py.BYTEARRAY_TYPE;
        for (String e : READERS) {
            final String name = e;
            t.define(name, PyBuiltin.methodKw(t, name, (args, kw) -> {
                Object[] mapped = new Object[args.length];
                mapped[0] = new PyBytes(self(args[0]).bytes());
                for (int k = 1; k < args.length; k++) {
                    mapped[k] = args[k] instanceof PyByteArray ? new PyBytes(((PyByteArray) args[k]).bytes())
                            : args[k];
                }
                return toArray(Py.obj(Py.BYTES_TYPE.lookup(name)).call(mapped, kw));
            }));
        }
        t.define("append", PyBuiltin.method(t, "append", a -> {
            PyByteArray b = self(a[0]);
            b.insertAt(b.size, new byte[] {byteOf(a[1])});
            return Py.NONE;
        }));
        t.define("extend", PyBuiltin.method(t, "extend", a -> {
            PyByteArray b = self(a[0]);
            byte[] src = PyBytes.like(a[1]);
            b.insertAt(b.size, src != null ? src : PyBytes.fromInts(a[1]));
            return Py.NONE;
        }));
        t.define("insert", PyBuiltin.method(t, "insert", a -> {
            PyByteArray b = self(a[0]);
            long i = Py.index(a[1]);
            if (i < 0) {
                i = Math.max(0, i + b.size);
            }
            b.insertAt((int) Math.min(i, b.size), new byte[] {byteOf(a[2])});
            return Py.NONE;
        }));
        t.define("pop", PyBuiltin.method(t, "pop", a -> {
            PyByteArray b = self(a[0]);
            if (b.size == 0) {
                throw Py.indexError("pop from empty bytearray");
            }
            int i = a.length > 1 ? b.index(a[1]) : b.size - 1;
            int v = b.buf[i] & 0xff;
            b.removeRange(i, i + 1);
            return PyInt.of(v);
        }));
        t.define("remove", PyBuiltin.method(t, "remove", a -> {
            PyByteArray b = self(a[0]);
            byte v = byteOf(a[1]);
            for (int i = 0; i < b.size; i++) {
                if (b.buf[i] == v) {
                    b.removeRange(i, i + 1);
                    return Py.NONE;
                }
            }
            throw Py.valueError("value not found in bytearray");
        }));
        t.define("clear", PyBuiltin.method(t, "clear", a -> {
            self(a[0]).size = 0;
            return Py.NONE;
        }));
        t.define("reverse", PyBuiltin.method(t, "reverse", a -> {
            PyByteArray b = self(a[0]);
            for (int i = 0, j = b.size - 1; i < j; i++, j--) {
                byte x = b.buf[i];
                b.buf[i] = b.buf[j];
                b.buf[j] = x;
            }
            return Py.NONE;
        }));
        t.define("copy", PyBuiltin.method(t, "copy", a -> new PyByteArray(self(a[0]).bytes())));
        t.define("maketrans", Py.BYTES_TYPE.lookup("maketrans"));
        t.define("fromhex", new PyClassMethod(PyBuiltin.positional("fromhex", a -> {
            Object b = Py.obj(Py.BYTES_TYPE.lookup("fromhex")).bind(Py.BYTES_TYPE);
            return new PyByteArray(((PyBytes) Py.call(b, a[1])).value);
        })));
        t.factory = (type, args, kw) -> {
            if (args.length == 0) {
                return new PyByteArray(new byte[0]);
            }
            Object x = args[0];
            if (x instanceof String || x instanceof PyStr) {
                if (args.length < 2 && kw.length == 0) {
                    throw Py.typeError("string argument without an encoding");
                }
            }
            Object b = Py.BYTES_TYPE.factory.create(Py.BYTES_TYPE, args, kw);
            if (!(b instanceof PyBytes)) {
                throw Py.typeError("cannot convert '" + Py.typeOf(x).name + "' object to bytearray");
            }
            return new PyByteArray(((PyBytes) b).bytes());
        };
    }
}
