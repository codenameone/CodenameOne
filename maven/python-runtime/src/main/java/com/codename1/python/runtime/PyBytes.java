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

/// Python's `bytes`, immutable.
public final class PyBytes extends PyObject {
    final byte[] value;

    public PyBytes(byte[] value) {
        this.value = value;
    }

    @Override
    public PyType type() {
        return Py.BYTES_TYPE;
    }

    public byte[] bytes() {
        byte[] r = new byte[value.length];
        System.arraycopy(value, 0, r, 0, r.length);
        return r;
    }

    @Override
    public Object toJava() {
        return bytes();
    }

    @Override
    public int len() {
        return value.length;
    }

    @Override
    public boolean isTrue() {
        return value.length != 0;
    }

    @Override
    public String repr() {
        boolean sq = false;
        boolean dq = false;
        for (byte x : value) {
            sq |= x == '\'';
            dq |= x == '"';
        }
        char q = sq && !dq ? '"' : '\'';
        StringBuilder sb = new StringBuilder("b");
        sb.append(q);
        for (byte e : value) {
            int c = e & 0xff;
            if (c == q || c == '\\') {
                sb.append('\\').append((char) c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 0x20 || c >= 0x7f) {
                sb.append("\\x");
                PyStr.hex(sb, c, 2);
            } else {
                sb.append((char) c);
            }
        }
        return sb.append(q).toString();
    }

    @Override
    public long hash() {
        long h = 0;
        for (byte e : value) {
            h = h * 31 + e;
        }
        return h;
    }

    /// The bytes of a bytes-like object (bytes, bytearray, memoryview), or
    /// null for anything else.
    static byte[] like(Object o) {
        if (o instanceof PyBytes) {
            return ((PyBytes) o).value;
        }
        if (o instanceof PyByteArray) {
            return ((PyByteArray) o).bytes();
        }
        if (o instanceof PyMemoryView) {
            return ((PyMemoryView) o).bytes();
        }
        return null;
    }

    /// An iterable of ints in range(0, 256), as bytes.
    static byte[] fromInts(Object o) {
        Object[] items = Py.toArray(o);
        byte[] r = new byte[items.length];
        for (int i = 0; i < items.length; i++) {
            if (!Py.isInt(items[i])) {
                throw Py.typeError("'" + Py.typeOf(items[i]).name + "' object cannot be interpreted as an integer");
            }
            long v = Py.asLong(items[i]);
            if (v < 0 || v > 255) {
                throw Py.valueError("byte must be in range(0, 256)");
            }
            r[i] = (byte) v;
        }
        return r;
    }

    static boolean same(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /// Unsigned lexicographic order.
    static int compare(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int d = (a[i] & 0xff) - (b[i] & 0xff);
            if (d != 0) {
                return d;
            }
        }
        return a.length - b.length;
    }

    static int indexOf(byte[] hay, byte[] needle) {
        for (int i = 0; i + needle.length <= hay.length; i++) {
            if (matchesAt(hay, i, needle)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean matchesAt(byte[] hay, int i, byte[] needle) {
        for (int j = 0; j < needle.length; j++) {
            if (hay[i + j] != needle[j]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(Object x) {
        byte[] b = like(x);
        if (b != null) {
            return indexOf(value, b) >= 0;
        }
        if (!Py.isInt(x)) {
            throw Py.typeError("a bytes-like object is required, not '" + Py.typeOf(x).name + "'");
        }
        long v = Py.asLong(x);
        if (v < 0 || v > 255) {
            throw Py.valueError("byte must be in range(0, 256)");
        }
        for (byte e : value) {
            if ((e & 0xff) == v) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object eq(Object o) {
        byte[] b = like(o);
        return b == null ? null : Py.bool(same(value, b));
    }

    @Override
    public Object lt(Object o) {
        byte[] b = like(o);
        return b == null ? null : Py.bool(compare(value, b) < 0);
    }

    @Override
    public Object le(Object o) {
        byte[] b = like(o);
        return b == null ? null : Py.bool(compare(value, b) <= 0);
    }

    @Override
    public Object gt(Object o) {
        byte[] b = like(o);
        return b == null ? null : Py.bool(compare(value, b) > 0);
    }

    @Override
    public Object ge(Object o) {
        byte[] b = like(o);
        return b == null ? null : Py.bool(compare(value, b) >= 0);
    }

    @Override
    public Object mul(Object o) {
        if (!(Py.isInt(o))) {
            return null;
        }
        long n = Math.max(0, Py.asLong(o));
        byte[] r = new byte[(int) (value.length * n)];
        for (int i = 0; i < n; i++) {
            System.arraycopy(value, 0, r, i * value.length, value.length);
        }
        return new PyBytes(r);
    }

    @Override
    public Object rmul(Object o) {
        return mul(o);
    }

    @Override
    public Object add(Object o) {
        byte[] b = like(o);
        if (b == null) {
            // Concatenation, not addition: only a class of the program's
            // own may still answer through __radd__.
            if (o instanceof PyInstance) {
                return null;
            }
            throw Py.typeError("can't concat " + Py.typeOf(o).name + " to bytes");
        }
        byte[] r = new byte[value.length + b.length];
        System.arraycopy(value, 0, r, 0, value.length);
        System.arraycopy(b, 0, r, value.length, b.length);
        return new PyBytes(r);
    }

    @Override
    public Object getItem(Object key) {
        if (key instanceof PySlice) {
            int[] r = ((PySlice) key).indices(value.length);
            int n = PySlice.length(r);
            byte[] out = new byte[n];
            for (int i = 0, j = r[0]; i < n; i++, j += r[2]) {
                out[i] = value[j];
            }
            return new PyBytes(out);
        }
        long i = Py.index(key);
        if (i < 0) {
            i += value.length;
        }
        if (i < 0 || i >= value.length) {
            throw Py.indexError("index out of range");
        }
        return PyInt.of(value[(int) i] & 0xff);
    }

    @Override
    public Object iter() {
        return new PyIter(value.length) {
            @Override
            Object at(int i) {
                return PyInt.of(value[i] & 0xff);
            }
        };
    }

    static void install() {
        Py.BYTES_TYPE.define("decode", PyBuiltin.methodKw(Py.BYTES_TYPE, "decode", (a, kw) -> {
            Object[] p = Args.bind("decode", a, kw, new String[] {"self", "encoding", "errors"},
                    new Object[] {null, Py.str("utf-8"), Py.str("strict")});
            return Py.str(Codecs.decode(((PyBytes) p[0]).value, Py.toStr(p[1]), Py.toStr(p[2])));
        }));
        Py.BYTES_TYPE.define("hex", PyBuiltin.methodKw(Py.BYTES_TYPE, "hex", (args, kw) -> {
            Object[] p = Args.bind("hex", args, kw, new String[] {"self", "sep", "bytes_per_sep"},
                    new Object[] {null, Py.NONE, PyInt.of(1)});
            byte[] v = self(p[0]).value;
            String sep = null;
            if (p[1] != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                byte[] sb = like(p[1]);
                sep = sb == null ? Py.toStr(p[1]) : sb.length == 1 ? String.valueOf((char) (sb[0] & 0xff)) : "--";
                if (sep.length() != 1) {
                    throw Py.valueError("sep must be length 1.");
                }
                if (sep.charAt(0) > 127) {
                    throw Py.valueError("sep must be ASCII.");
                }
            }
            long per = Py.asLong(p[2]);
            int group = (int) Math.min(Math.abs(per), Integer.MAX_VALUE);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < v.length; i++) {
                if (sep != null && group > 0 && i > 0) {
                    // A positive count groups from the right, a negative one from the left.
                    boolean boundary = per > 0 ? (v.length - i) % group == 0 : i % group == 0;
                    if (boundary) {
                        sb.append(sep);
                    }
                }
                PyStr.hex(sb, v[i] & 0xff, 2);
            }
            return Py.str(sb.toString());
        }));
        installLatin1Delegates();
        Py.BYTES_TYPE.define("fromhex", new PyClassMethod(PyBuiltin.positional("fromhex", a -> {
            String h = Py.toStr(a[1]);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            int i = 0;
            while (i < h.length()) {
                char c = h.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    i++;
                    continue;
                }
                int hi = Character.digit(c, 16);
                int lo = i + 1 < h.length() ? Character.digit(h.charAt(i + 1), 16) : -1;
                if (hi < 0 || lo < 0) {
                    throw Py.valueError("non-hexadecimal number found in fromhex() arg at position "
                            + (hi < 0 ? i : i + 1));
                }
                out.write(hi * 16 + lo);
                i += 2;
            }
            return new PyBytes(out.toByteArray());
        })));
        Py.BYTES_TYPE.define("upper", PyBuiltin.method(Py.BYTES_TYPE, "upper", a -> asciiCase((PyBytes) a[0], true)));
        Py.BYTES_TYPE.define("lower", PyBuiltin.method(Py.BYTES_TYPE, "lower", a -> asciiCase((PyBytes) a[0], false)));
        Py.BYTES_TYPE.define("isdigit", PyBuiltin.method(Py.BYTES_TYPE, "isdigit",
                a -> Py.bool(allAscii((PyBytes) a[0], "0123456789"))));
        Py.BYTES_TYPE.define("isalpha", PyBuiltin.method(Py.BYTES_TYPE, "isalpha",
                a -> Py.bool(allAscii((PyBytes) a[0], LETTERS))));
        Py.BYTES_TYPE.define("isalnum", PyBuiltin.method(Py.BYTES_TYPE, "isalnum",
                a -> Py.bool(allAscii((PyBytes) a[0], LETTERS + "0123456789"))));
        Py.BYTES_TYPE.define("isspace", PyBuiltin.method(Py.BYTES_TYPE, "isspace",
                a -> Py.bool(allAscii((PyBytes) a[0], " \t\n\r\u000b\f"))));
        installAsciiText();
        Py.BYTES_TYPE.factory = (t, args, kw) -> {
            if (args.length == 0) {
                return new PyBytes(new byte[0]);
            }
            Object x = args[0];
            if (x instanceof String) {
                return new PyBytes(Utf8.encode(((String) x)));
            }
            if (Py.isInt(x)) {
                return new PyBytes(new byte[(int) Py.asLong(x)]);
            }
            Object[] items = Py.toArray(x);
            byte[] r = new byte[items.length];
            for (int i = 0; i < items.length; i++) {
                long v = Py.asLong(items[i]);
                if (v < 0 || v > 255) {
                    throw Py.valueError("bytes must be in range(0, 256)");
                }
                r[i] = (byte) v;
            }
            return new PyBytes(r);
        };
    }

    private static boolean isUpper(int c) {
        return c >= 'A' && c <= 'Z';
    }

    private static boolean isLower(int c) {
        return c >= 'a' && c <= 'z';
    }

    private static PyBytes self(Object o) {
        if (!(o instanceof PyBytes)) {
            throw Py.typeError("descriptor requires a 'bytes' object but received a '" + Py.typeOf(o).name + "'");
        }
        return (PyBytes) o;
    }

    /// The case and translation methods, which see only ASCII letters.
    private static void installAsciiText() {
        PyType t = Py.BYTES_TYPE;
        t.define("maketrans", new PyStaticMethod(PyBuiltin.positional("maketrans", a -> {
            if (a.length != 2) {
                throw Py.typeError("maketrans expected 2 arguments, got " + a.length);
            }
            byte[] from = like(a[0]);
            byte[] to = like(a[1]);
            if (from == null || to == null) {
                throw Py.typeError("a bytes-like object is required, not '"
                        + Py.typeOf(from == null ? a[0] : a[1]).name + "'");
            }
            if (from.length != to.length) {
                throw Py.valueError("maketrans arguments must have same length");
            }
            byte[] table = new byte[256];
            for (int i = 0; i < 256; i++) {
                table[i] = (byte) i;
            }
            for (int i = 0; i < from.length; i++) {
                table[from[i] & 0xff] = to[i];
            }
            return new PyBytes(table);
        })));
        t.define("translate", PyBuiltin.methodKw(t, "translate", (args, kw) -> {
            Object[] p = Args.bind("translate", args, kw, new String[] {"self", "table", "delete"},
                    new Object[] {null, null, new PyBytes(new byte[0])});
            byte[] v = self(p[0]).value;
            byte[] table = null;
            if (p[1] != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                table = like(p[1]);
                if (table == null) {
                    throw Py.typeError("a bytes-like object is required, not '" + Py.typeOf(p[1]).name + "'");
                }
                if (table.length != 256) {
                    throw Py.valueError("translation table must be 256 characters long");
                }
            }
            byte[] del = like(p[2]);
            if (del == null) {
                throw Py.typeError("a bytes-like object is required, not '" + Py.typeOf(p[2]).name + "'");
            }
            boolean[] drop = new boolean[256];
            for (byte e : del) {
                drop[e & 0xff] = true;
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(v.length);
            for (byte el : v) {
                int c = el & 0xff;
                if (!drop[c]) {
                    out.write(table == null ? c : table[c] & 0xff);
                }
            }
            return new PyBytes(out.toByteArray());
        }));
        t.define("isascii", PyBuiltin.method(t, "isascii", a -> {
            byte[] v = self(a[0]).value;
            for (byte e : v) {
                if (e < 0) {
                    return Py.FALSE;
                }
            }
            return Py.TRUE;
        }));
        t.define("islower", PyBuiltin.method(t, "islower", a -> Py.bool(cased(self(a[0]).value, false))));
        t.define("isupper", PyBuiltin.method(t, "isupper", a -> Py.bool(cased(self(a[0]).value, true))));
        t.define("istitle", PyBuiltin.method(t, "istitle", a -> {
            byte[] v = self(a[0]).value;
            boolean cased = false;
            boolean prevCased = false;
            for (byte e : v) {
                int c = e & 0xff;
                if (isUpper(c)) {
                    if (prevCased) {
                        return Py.FALSE;
                    }
                    prevCased = true;
                    cased = true;
                } else if (isLower(c)) {
                    if (!prevCased) {
                        return Py.FALSE;
                    }
                    prevCased = true;
                    cased = true;
                } else {
                    prevCased = false;
                }
            }
            return Py.bool(cased);
        }));
        t.define("swapcase", PyBuiltin.method(t, "swapcase", a -> {
            byte[] v = self(a[0]).value;
            byte[] r = new byte[v.length];
            for (int i = 0; i < v.length; i++) {
                int c = v[i] & 0xff;
                r[i] = (byte) (isUpper(c) ? c + 32 : isLower(c) ? c - 32 : c);
            }
            return new PyBytes(r);
        }));
        t.define("capitalize", PyBuiltin.method(t, "capitalize", a -> {
            byte[] v = self(a[0]).value;
            byte[] r = new byte[v.length];
            for (int i = 0; i < v.length; i++) {
                int c = v[i] & 0xff;
                if (i == 0) {
                    r[i] = (byte) (isLower(c) ? c - 32 : c);
                } else {
                    r[i] = (byte) (isUpper(c) ? c + 32 : c);
                }
            }
            return new PyBytes(r);
        }));
        t.define("title", PyBuiltin.method(t, "title", a -> {
            byte[] v = self(a[0]).value;
            byte[] r = new byte[v.length];
            boolean prevCased = false;
            for (int i = 0; i < v.length; i++) {
                int c = v[i] & 0xff;
                if (isLower(c)) {
                    r[i] = (byte) (prevCased ? c : c - 32);
                    prevCased = true;
                } else if (isUpper(c)) {
                    r[i] = (byte) (prevCased ? c + 32 : c);
                    prevCased = true;
                } else {
                    r[i] = (byte) c;
                    prevCased = false;
                }
            }
            return new PyBytes(r);
        }));
    }

    /// Whether `v` has a cased letter and every one is upper (or lower) case.
    private static boolean cased(byte[] v, boolean upper) {
        boolean any = false;
        for (byte e : v) {
            int c = e & 0xff;
            if (upper ? isLower(c) : isUpper(c)) {
                return false;
            }
            if (isUpper(c) || isLower(c)) {
                any = true;
            }
        }
        return any;
    }

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /// The str methods whose bytes versions behave identically on a Latin-1
    /// view of the bytes (one char per byte); their results map back.
    private static final String[] LATIN1_DELEGATES = {"strip", "lstrip", "rstrip", "split", "rsplit", "startswith",
        "endswith", "find", "rfind", "index", "rindex", "count", "replace", "join", "partition", "rpartition",
        "removeprefix", "removesuffix", "splitlines", "center", "ljust", "rjust", "zfill", "expandtabs"};

    private static void installLatin1Delegates() {
        for (String e : LATIN1_DELEGATES) {
            final String name = e;
            Py.BYTES_TYPE.define(name, PyBuiltin.methodKw(Py.BYTES_TYPE, name, (args, kw) -> {
                Object[] mapped = new Object[args.length];
                for (int k = 0; k < args.length; k++) {
                    Object v = args[k];
                    if ("join".equals(name) && k == 1 && !(v instanceof PyBytes)) {
                        // Any iterable of bytes: materialise it so each item maps.
                        v = new PyList(Py.toArray(v));
                    }
                    mapped[k] = toLatin1(v);
                }
                Object m = Py.STR_TYPE.lookup(name);
                return fromLatin1(Py.obj(m).call(mapped, kw));
            }));
        }
    }

    private static Object toLatin1(Object o) {
        byte[] v = like(o);
        if (v != null) {
            char[] c = new char[v.length];
            for (int i = 0; i < v.length; i++) {
                c[i] = (char) (v[i] & 0xff);
            }
            return new String(c);
        }
        if (o instanceof String) {
            throw Py.typeError("a bytes-like object is required, not 'str'");
        }
        if (o instanceof PyTuple || o instanceof PyList) {
            Object[] items = Py.toArray(o);
            Object[] r = new Object[items.length];
            for (int i = 0; i < items.length; i++) {
                r[i] = toLatin1(items[i]);
            }
            return o instanceof PyTuple ? new PyTuple(r) : new PyList(r);
        }
        return o;
    }

    private static Object fromLatin1(Object o) {
        if (o instanceof String) {
            String s = ((String) o);
            byte[] b = new byte[s.length()];
            for (int i = 0; i < b.length; i++) {
                b[i] = (byte) s.charAt(i);
            }
            return new PyBytes(b);
        }
        if (o instanceof PyTuple || o instanceof PyList) {
            Object[] items = Py.toArray(o);
            Object[] r = new Object[items.length];
            for (int i = 0; i < items.length; i++) {
                r[i] = fromLatin1(items[i]);
            }
            return o instanceof PyTuple ? new PyTuple(r) : new PyList(r);
        }
        return o;
    }

    private static Object asciiCase(PyBytes b, boolean upper) {
        byte[] r = new byte[b.value.length];
        for (int i = 0; i < r.length; i++) {
            byte c = b.value[i];
            if (upper && c >= 'a' && c <= 'z') {
                c = (byte) (c - 32);
            } else if (!upper && c >= 'A' && c <= 'Z') {
                c = (byte) (c + 32);
            }
            r[i] = c;
        }
        return new PyBytes(r);
    }

    private static boolean allAscii(PyBytes b, String set) {
        if (b.value.length == 0) {
            return false;
        }
        for (int i = 0; i < b.value.length; i++) {
            if (set.indexOf((char) (b.value[i] & 0xff)) < 0) {
                return false;
            }
        }
        return true;
    }
}
