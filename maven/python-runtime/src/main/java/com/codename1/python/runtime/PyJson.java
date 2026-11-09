/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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

import com.codename1.io.JSONParser;
import com.codename1.io.JSONValueFactory;
import com.codename1.util.BigInteger;
import java.io.IOException;

/// `_cn1json`: `json.loads` on Codename One's strict JSON reader.
///
/// CPython decodes JSON in C (`_json.scanstring`, `scan_once`); run as
/// transpiled Python the decoder dispatched generically on every character.
/// Here [JSONParser#parseStrict(String, JSONValueFactory)] walks the text and
/// this factory builds the Python values directly: a `dict` per object (a
/// repeated key keeps its first position and its last value, as CPython's
/// does), a `list` per array, `int` and `float`, and `NaN`/`Infinity`.
///
/// `loads` answers [#FAILED] for anything the reader refuses, and `json.py`
/// then decodes the text again in Python, which raises `JSONDecodeError` with
/// CPython's own message and position. A malformed document is the rare case,
/// so it pays for being parsed twice and the messages stay exactly CPython's.
final class PyJson {
    /// What `loads` answers when the reader refused the document.
    static final Object FAILED = new PyList();

    private PyJson() {
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1json", new String[0]);
        m.setAttr("FAILED", FAILED);
        // loads(s, strict, object_hook, object_pairs_hook)
        m.setAttr("loads", PyBuiltin.positional("loads", a -> {
            Values f = Values.take(Py.isTrue(a[1]), a[2] == Py.NONE ? null : a[2],
                    a[3] == Py.NONE ? null : a[3]);
            Object r = parse(Py.toStr(a[0]), f);
            Values.give(f);
            return r;
        }));
        // dumps(o, ensure_ascii, check_circular, allow_nan, sort_keys, indent,
        //       item_separator, key_separator, skipkeys, other)
        m.setAttr("dumps", PyBuiltin.positional("dumps", a -> {
            Encoder e = Encoder.take(Py.isTrue(a[1]), Py.isTrue(a[2]), Py.isTrue(a[3]), Py.isTrue(a[4]),
                    a[5] == Py.NONE ? null : Py.toStr(a[5]), Py.toStr(a[6]), Py.toStr(a[7]), Py.isTrue(a[8]), a[9]);
            e.value(a[0], 0);
            String r = new String(e.buf, 0, e.len);
            Encoder.give(e);
            return r;
        }));
        return m;
    }

    /// The parse, apart so that its handler is set up only by this call and not by every
    /// frame around it.
    private static Object parse(String text, Values f) {
        try {
            return JSONParser.parseStrict(text, f);
        } catch (IOException err) {
            return FAILED;
        }
    }

    /// `json.dumps` for the builtin types, as CPython's C encoder
    /// (`c_make_encoder`) writes them: one buffer, no list of fragments.
    ///
    /// Only an exact `str`, `int`, `float`, `bool`, `None`, `list`, `tuple`
    /// and `dict` is written here. Anything else -- a subclass of one of them,
    /// an object `default` converts, a dict whose keys must be sorted but are
    /// not all strings -- is handed to `other`, json.py's own encoder, for
    /// that one value, with the containers open around it so a cycle through
    /// it is still reported. What `isinstance` and `default` decide therefore
    /// stays in Python, exactly as CPython decides it.
    private static final class Encoder {
        /// The output, written straight into an array: a native StringBuilder
        /// append takes the builder's owner lock and the thread's identity
        /// each call, and an encoder appends several times per value.
        char[] buf = new char[256];
        int len;
        private boolean ascii;
        private boolean circular;
        private boolean allowNan;
        private boolean sortKeys;
        private String indent;
        private String itemSep;
        private String keySep;
        private boolean skipKeys;
        private Object other;
        private final java.util.ArrayList<Object> markers = new java.util.ArrayList<Object>();

        /// One encoder kept between calls, with its buffer, so a dumps allocates only its
        /// result, as CPython's does. A call takes it and gives it back when done; a dumps
        /// made while one is running (from a default function) finds none and makes its own,
        /// and one that raises simply does not give it back, so no handler is needed for it.
        private static Encoder spare;

        static Encoder take(boolean ascii, boolean circular, boolean allowNan, boolean sortKeys, String indent,
                String itemSep, String keySep, boolean skipKeys, Object other) {
            Encoder e = spare;
            if (e == null) {
                return new Encoder(ascii, circular, allowNan, sortKeys, indent, itemSep, keySep, skipKeys, other);
            }
            spare = null;
            e.set(ascii, circular, allowNan, sortKeys, indent, itemSep, keySep, skipKeys, other);
            e.len = 0;
            e.markers.clear();
            return e;
        }

        static void give(Encoder e) {
            if (e.buf.length > 4096) {
                // One large document must not stay held for the life of the program.
                e.buf = new char[256];
            }
            e.other = null;
            spare = e;
        }

        Encoder(boolean ascii, boolean circular, boolean allowNan, boolean sortKeys, String indent, String itemSep,
                String keySep, boolean skipKeys, Object other) {
            set(ascii, circular, allowNan, sortKeys, indent, itemSep, keySep, skipKeys, other);
        }

        private void set(boolean ascii, boolean circular, boolean allowNan, boolean sortKeys, String indent,
                String itemSep, String keySep, boolean skipKeys, Object other) {
            this.ascii = ascii;
            this.circular = circular;
            this.allowNan = allowNan;
            this.sortKeys = sortKeys;
            this.indent = indent;
            this.itemSep = itemSep;
            this.keySep = keySep;
            this.skipKeys = skipKeys;
            this.other = other;
        }


        private void room(int n) {
            if (len + n > buf.length) {
                char[] b = new char[Math.max(buf.length * 2, len + n)];
                System.arraycopy(buf, 0, b, 0, len);
                buf = b;
            }
        }

        private void put(char c) {
            if (len == buf.length) {
                room(1);
            }
            buf[len++] = c;
        }

        private void put(String s) {
            int n = s.length();
            room(n);
            s.getChars(0, n, buf, len);
            len += n;
        }

        private void putLong(long v) {
            if (v >= 0 && v < 10) {
                put((char) ('0' + v));
                return;
            }
            if (v == Long.MIN_VALUE) {
                put("-9223372036854775808");
                return;
            }
            room(20);
            if (v < 0) {
                buf[len++] = '-';
                v = -v;
            }
            int start = len;
            while (v > 0) {
                buf[len++] = (char) ('0' + v % 10);
                v /= 10;
            }
            for (int i = start, j = len - 1; i < j; i++, j--) {
                char t = buf[i];
                buf[i] = buf[j];
                buf[j] = t;
            }
        }

        private static final char[] HEX = "0123456789abcdef".toCharArray();

        /// A JSON string literal, as json's encoder writes one: copied in one
        /// block, then escaped in place from the first character that needs it.
        private void quote(String s) {
            int n = s.length();
            room(n + 2);
            buf[len++] = '"';
            s.getChars(0, n, buf, len);
            int limit = ascii ? 0x7f : 0x10000;
            int i = len;
            int end = len + n;
            while (i < end) {
                char c = buf[i];
                if (c < 0x20 || c == '"' || c == '\\' || c >= limit) {
                    break;
                }
                i++;
            }
            if (i == end) {
                len = end;
                buf[len++] = '"';
                return;
            }
            len = i;
            int from = i - (end - n) ;
            for (int k = from; k < n; k++) {
                char c = s.charAt(k);
                if (c >= 0x20 && c != '"' && c != '\\' && c < limit) {
                    put(c);
                    continue;
                }
                room(6);
                buf[len++] = '\\';
                switch (c) {
                    case '"':
                        buf[len++] = '"';
                        break;
                    case '\\':
                        buf[len++] = '\\';
                        break;
                    case '\n':
                        buf[len++] = 'n';
                        break;
                    case '\r':
                        buf[len++] = 'r';
                        break;
                    case '\t':
                        buf[len++] = 't';
                        break;
                    case '\b':
                        buf[len++] = 'b';
                        break;
                    case '\f':
                        buf[len++] = 'f';
                        break;
                    default:
                        buf[len++] = 'u';
                        buf[len++] = HEX[c >> 12 & 15];
                        buf[len++] = HEX[c >> 8 & 15];
                        buf[len++] = HEX[c >> 4 & 15];
                        buf[len++] = HEX[c & 15];
                }
            }
            put('"');
        }

        void value(Object o, int level) {
            if (o instanceof String) {
                quote((String) o);
            } else if (o == Py.NONE) { //NOPMD CompareObjectsWithEquals - singleton
                put("null");
            } else if (o == Py.TRUE) { //NOPMD CompareObjectsWithEquals - singleton
                put("true");
            } else if (o == Py.FALSE) { //NOPMD CompareObjectsWithEquals - singleton
                put("false");
            } else if (o instanceof Long) {
                putLong(((Long) o).longValue());
            } else if (o instanceof Double) {
                floatText(((Double) o).doubleValue());
            } else if (o instanceof PyInt && o.getClass() == PyInt.class) {
                put(((PyInt) o).repr());
            } else if (o instanceof PyList && Py.typeOf(o) == Py.LIST_TYPE) { //NOPMD CompareObjectsWithEquals
                PyList l = (PyList) o;
                int n = l.size();
                sequence(o, n, l, null, level);
            } else if (o instanceof PyTuple && Py.typeOf(o) == Py.TUPLE_TYPE) { //NOPMD CompareObjectsWithEquals
                PyTuple t = (PyTuple) o;
                sequence(o, t.size(), null, t, level);
            } else if (o instanceof PyDict && Py.typeOf(o) == Py.DICT_TYPE) { //NOPMD CompareObjectsWithEquals
                dict((PyDict) o, level);
            } else {
                delegate(o, level);
            }
        }

        private void delegate(Object o, int level) {
            put(Py.toStr(Py.call(other, o, new PyList(markers.toArray()), Py.of((long) level))));
        }

        private void floatText(double d) {
            String text;
            if (Double.isNaN(d)) {
                text = "NaN";
            } else if (d == Double.POSITIVE_INFINITY) {
                text = "Infinity";
            } else if (d == Double.NEGATIVE_INFINITY) {
                text = "-Infinity";
            } else {
                put(PyFloat.repr(d));
                return;
            }
            if (!allowNan) {
                throw Py.valueError("Out of range float values are not JSON compliant: " + PyFloat.repr(d));
            }
            put(text);
        }

        private void mark(Object o) {
            if (!circular) {
                return;
            }
            for (int i = markers.size() - 1; i >= 0; i--) {
                if (markers.get(i) == o) { //NOPMD CompareObjectsWithEquals - identity is meant
                    throw Py.valueError("Circular reference detected");
                }
            }
            markers.add(o);
        }

        private void unmark() {
            if (circular) {
                markers.remove(markers.size() - 1);
            }
        }

        private void newline(int level) {
            if (indent != null) {
                put('\n');
                for (int i = 0; i < level; i++) {
                    put(indent);
                }
            }
        }

        private void sequence(Object o, int n, PyList l, PyTuple t, int level) {
            mark(o);
            if (n == 0) {
                put("[]");
            } else {
                put('[');
                for (int i = 0; i < n; i++) {
                    if (i > 0) {
                        put(itemSep);
                    }
                    newline(level + 1);
                    value(l != null ? l.get(i) : t.get(i), level + 1);
                }
                newline(level);
                put(']');
            }
            unmark();
        }

        private void dict(PyDict d, int level) {
            int used = d.used;
            Object[] keys = d.keys;
            Object[] values = d.values;
            int[] order = null;
            if (sortKeys && d.size() > 1) {
                order = sortedStringKeys(keys, used);
                if (order == null) {
                    delegate(d, level);
                    return;
                }
            }
            mark(d);
            if (d.size() == 0) {
                put("{}");
                unmark();
                return;
            }
            put('{');
            boolean first = true;
            int count = order != null ? order.length : used;
            for (int j = 0; j < count; j++) {
                int i = order != null ? order[j] : j;
                Object k = keys[i];
                if (k == null) {
                    continue;
                }
                String key = key(k);
                if (key == null) {
                    continue;
                }
                if (!first) {
                    put(itemSep);
                }
                first = false;
                newline(level + 1);
                quote(key);
                put(keySep);
                value(values[i], level + 1);
            }
            newline(level);
            put('}');
            unmark();
        }

        /// The entry indices in key order when every key is a str, else null:
        /// sorting mixed keys is Python's comparison to make, and raises there.
        private static int[] sortedStringKeys(Object[] keys, int used) {
            int n = 0;
            for (int i = 0; i < used; i++) {
                if (keys[i] != null) {
                    if (!(keys[i] instanceof String)) {
                        return null;
                    }
                    n++;
                }
            }
            Integer[] idx = new Integer[n];
            n = 0;
            for (int i = 0; i < used; i++) {
                if (keys[i] != null) {
                    idx[n++] = Integer.valueOf(i);
                }
            }
            final Object[] k = keys;
            java.util.Arrays.sort(idx, new java.util.Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return codePointCompare((String) k[a.intValue()], (String) k[b.intValue()]);
                }
            });
            int[] out = new int[n];
            for (int i = 0; i < n; i++) {
                out[i] = idx[i].intValue();
            }
            return out;
        }

        /// Python orders str by code point; String.compareTo orders UTF-16
        /// units, which differ for a supplementary character against U+E000
        /// to U+FFFF.
        private static int codePointCompare(String a, String b) {
            int n = Math.min(a.length(), b.length());
            for (int i = 0; i < n; i++) {
                char x = a.charAt(i);
                char y = b.charAt(i);
                if (x != y) {
                    boolean sx = x >= 0xD800 && x < 0xE000;
                    boolean sy = y >= 0xD800 && y < 0xE000;
                    if (sx != sy) {
                        return sx ? 1 : -1;
                    }
                    return x - y;
                }
            }
            return a.length() - b.length();
        }

        private String key(Object k) {
            if (k instanceof String) {
                return (String) k;
            }
            if (k instanceof Double) {
                int mark = len;
                floatText(((Double) k).doubleValue());
                String t = new String(buf, mark, len - mark);
                len = mark;
                return t;
            }
            if (k == Py.TRUE) { //NOPMD CompareObjectsWithEquals - singleton
                return "true";
            }
            if (k == Py.FALSE) { //NOPMD CompareObjectsWithEquals - singleton
                return "false";
            }
            if (k == Py.NONE) { //NOPMD CompareObjectsWithEquals - singleton
                return "null";
            }
            if (k instanceof Long || k instanceof PyInt && k.getClass() == PyInt.class) {
                return k instanceof Long ? k.toString() : ((PyInt) k).repr();
            }
            PyType kt = Py.typeOf(k);
            if (kt.isSubtype(Py.STR_TYPE) || kt.isSubtype(Py.INT_TYPE) || kt.isSubtype(Py.FLOAT_TYPE)) {
                return Py.toStr(Py.call(other, k, Py.NONE, Py.of(-1L)));
            }
            if (skipKeys) {
                return null;
            }
            throw Py.typeError("keys must be str, int, float, bool or None, not " + kt.name);
        }
    }

    /// Builds Python values for the reader.
    private static final class Values extends JSONValueFactory {
        private boolean strict;
        private Object objectHook;
        private Object pairsHook;

        Values(boolean strict, Object objectHook, Object pairsHook) {
            this.strict = strict;
            this.objectHook = objectHook;
            this.pairsHook = pairsHook;
        }

        /// One builder kept between calls, as [Encoder#take] keeps an encoder: a hook that
        /// calls loads while one is running finds none and makes its own.
        private static Values spare;

        static Values take(boolean strict, Object objectHook, Object pairsHook) {
            Values v = spare;
            if (v == null) {
                return new Values(strict, objectHook, pairsHook);
            }
            spare = null;
            v.strict = strict;
            v.objectHook = objectHook;
            v.pairsHook = pairsHook;
            v.top = 0;
            v.depth = 0;
            return v;
        }

        static void give(Values v) {
            if (v.stack.length > 1024) {
                v.stack = new Object[64];
            } else {
                // What the last document held must not stay reachable from here.
                java.util.Arrays.fill(v.stack, 0, v.stack.length, null);
            }
            v.objectHook = null;
            v.pairsHook = null;
            spare = v;
        }

        /// Members and elements of the containers being read, innermost last.
        /// A container is built once, at its final size, when it closes: an
        /// object's dict then allocates its arrays once instead of growing
        /// from four entries to eight to sixteen, and a list holds exactly its
        /// elements.
        private Object[] stack = new Object[64];
        private int top;
        /// One frame per open container, reused: where its entries start.
        private Frame[] frames = new Frame[16];
        private int depth;

        private static final class Frame {
            int start;
        }

        private Frame open() {
            if (depth == frames.length) {
                Frame[] f = new Frame[depth * 2];
                System.arraycopy(frames, 0, f, 0, depth);
                frames = f;
            }
            Frame f = frames[depth];
            if (f == null) {
                f = new Frame();
                frames[depth] = f;
            }
            depth++;
            f.start = top;
            return f;
        }

        private void push(Object o) {
            if (top == stack.length) {
                Object[] g = new Object[top * 2];
                System.arraycopy(stack, 0, g, 0, top);
                stack = g;
            }
            stack[top++] = o;
        }

        @Override
        public Object newObject() {
            return open();
        }

        @Override
        public void put(Object object, String key, Object value) {
            push(key);
            push(value);
        }

        @Override
        public Object endObject(Object object) {
            int start = ((Frame) object).start;
            int n = (top - start) >> 1;
            Object result;
            if (pairsHook != null) {
                Object[] pairs = new Object[n];
                for (int i = 0; i < n; i++) {
                    pairs[i] = PyTuple.of(stack[start + 2 * i], stack[start + 2 * i + 1]);
                }
                result = new PyList(pairs);
            } else {
                PyDict d = new PyDict(n);
                for (int i = start; i < top; i += 2) {
                    d.setItem(stack[i], stack[i + 1]);
                }
                result = d;
            }
            close(start);
            if (pairsHook != null) {
                return Py.call(pairsHook, result);
            }
            return objectHook != null ? Py.call(objectHook, result) : result;
        }

        private void close(int start) {
            for (int i = start; i < top; i++) {
                stack[i] = null;
            }
            top = start;
            depth--;
        }

        @Override
        public Object newArray() {
            return open();
        }

        @Override
        public void add(Object array, Object value) {
            push(value);
        }

        @Override
        public Object endArray(Object array) {
            int start = ((Frame) array).start;
            Object[] items = new Object[top - start];
            System.arraycopy(stack, start, items, 0, items.length);
            close(start);
            return PyList.of(items);
        }

        @Override
        public Object integer(long value) {
            return Py.of(value);
        }

        @Override
        public Object bigInteger(String digits) {
            return PyInt.of(new BigInteger(digits));
        }

        @Override
        public Object decimal(double value) {
            return Py.ofFloat(value);
        }

        @Override
        public Object bool(boolean value) {
            return Py.bool(value);
        }

        @Override
        public Object nullValue() {
            return Py.NONE;
        }

        @Override
        public boolean allowNonFinite() {
            return true;
        }

        @Override
        public boolean allowControlCharacters() {
            return !strict;
        }
    }
}
