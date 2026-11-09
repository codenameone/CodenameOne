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

/// Python's `str`: an immutable sequence of code points.
///
/// A Python string IS a `java.lang.String`, the way an int is a `Long`: the
/// transpiled code and the runtime pass `String`s around, and nothing wraps
/// one. `PyStr` is the view [Py#obj(Object)] hands out for the operations
/// that go through the object protocol (attribute lookup, the operators the
/// fast paths in [Py] do not take); it is transient and never a Python value
/// itself.
///
/// Python indexes by code point and Java by UTF-16 unit; the two agree unless
/// the text holds a supplementary character, so the slow path that translates
/// indices runs only for such strings.
public final class PyStr extends PyObject {
    public final String value;

    public PyStr(String value) {
        this.value = value;
    }

    /// The text of a str value, or null when `o` is not a str.
    public static String sv(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof PyStr) {
            return ((PyStr) o).value;
        }
        return null;
    }

    @Override
    public PyType type() {
        return Py.STR_TYPE;
    }

    /// A long string's surrogate pair count, cached so that indexing or
    /// slicing it in a loop does not rescan it on every access. The holder
    /// is immutable, so a reader on another thread sees either the old entry
    /// or the new one, never a mix.
    private static final class Count {
        final String s;
        final int n;

        Count(String s, int n) {
            this.s = s;
            this.n = n;
        }
    }

    /// Direct mapped by identity hash. A parser works on several long
    /// strings at once -- its buffer, the text a match holds, the slice it
    /// is building -- and a cache of one or two entries evicted one of them
    /// on every access, which still cost MarkItDown a tenth of its time.
    ///
    /// Two tables, by length: the many short slices a parser makes must not
    /// evict the page it is walking, whose rescan is what costs. And the slot
    /// is the identity hash MIXED: on ParparVM it is the object's address,
    /// whose low bits are allocation alignment, so masking it directly put
    /// every string in a few slots and the page was rescanned on every
    /// regex call.
    private static final Count[] COUNTS = new Count[256];
    private static final Count[] LONG_COUNTS = new Count[64];
    private static final int LONG = 1024;

    private static int slot(String s, int size) {
        int h = System.identityHashCode(s);
        h ^= (h >>> 16);
        h *= 0x45d9f3b;
        h ^= (h >>> 16);
        return h & (size - 1);
    }

    /// Surrogate pairs in `s`, each one code point of two chars: zero for
    /// every string without a supplementary character. A lone surrogate is
    /// not a pair; it is one char and one code point.
    static int surrogates(String s) {
        int len = s.length();
        if (len <= 32) {
            // codePointCount counts a pair as one code point, as scan does, and
            // answers a Latin-1 string -- nearly every short one -- from its
            // storage without reading a char, on ParparVM and on a JDK alike.
            return len - s.codePointCount(0, len);
        }
        Count[] table = len >= LONG ? LONG_COUNTS : COUNTS;
        int slot = slot(s, table.length);
        Count c = table[slot];
        if (c != null && c.s == s) { //NOPMD CompareObjectsWithEquals - identity is meant
            return c.n;
        }
        int n = len - s.codePointCount(0, len);
        table[slot] = new Count(s, n);
        return n;
    }

    /// The pair count of `s` if the cache holds it, else -1. Never scans:
    /// for `len`, where a miss costs the same scan a lookup would have
    /// saved and a Latin-1 string answers without one.
    static int cachedSurrogates(String s) {
        int len = s.length();
        if (len <= 32) {
            return -1;
        }
        Count[] table = len >= LONG ? LONG_COUNTS : COUNTS;
        Count c = table[slot(s, table.length)];
        return c != null && c.s == s ? c.n : -1; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    /// Records the pair count of `s` that its maker already knows (a
    /// decoder), so `len` and indexing do not scan for it.
    static void knownSurrogates(String s, int n) {
        int len = s.length();
        if (len > 32) {
            Count[] table = len >= LONG ? LONG_COUNTS : COUNTS;
            table[slot(s, table.length)] = new Count(s, n);
        }
    }

    /// Length in code points; a lone surrogate counts as one, as in Python.
    /// The pair count of the last long string is kept, so indexing or slicing
    /// one in a loop does not rescan it each time: `html.parser` walks a
    /// page that way, and a page with one non-Latin-1 character went
    /// quadratic through `String.codePointCount`.
    public static int codePoints(String s) {
        return s.length() - surrogates(s);
    }

    public int codePoints() {
        return codePoints(value);
    }

    /// The UTF-16 offset of code point `cp`.
    static int offset(String s, int cp) {
        if (surrogates(s) == 0) {
            return cp;
        }
        int off = 0;
        for (int i = 0; i < cp; i++) {
            off += pairAt(s, off) ? 2 : 1;
        }
        return off;
    }

    /// Whether a surrogate pair, one code point, starts at offset `i`; a
    /// lone surrogate is a code point of its own.
    static boolean pairAt(String s, int i) {
        return i + 1 < s.length() && Character.isHighSurrogate(s.charAt(i))
                && Character.isLowSurrogate(s.charAt(i + 1));
    }

    int offset(int cp) {
        return offset(value, cp);
    }

    /// The code point index of UTF-16 offset `off`.
    static int cpIndex(String s, int off) {
        if (surrogates(s) == 0) {
            return off;
        }
        int cp = 0;
        for (int i = 0; i < off; i++) {
            if (i == 0 || !pairAt(s, i - 1)) {
                cp++;
            }
        }
        return cp;
    }

    int cpIndex(int off) {
        return cpIndex(value, off);
    }

    /// The substring of code points `[from, to)`.
    public static String substr(String s, int from, int to) {
        if (surrogates(s) == 0) {
            return s.substring(from, to);
        }
        return s.substring(offset(s, from), offset(s, to));
    }

    public String substr(int from, int to) {
        return substr(value, from, to);
    }

    @Override
    public Object getAttrOrNull(String name) {
        // Methods bind to the string itself, never to this transient view.
        Object attr = type().lookup(name);
        if (attr == null && "__class__".equals(name)) {
            return Py.STR_TYPE;
        }
        return attr != null ? Py.bindAttr(attr, value) : null;
    }

    @Override
    public String str() {
        return value;
    }

    @Override
    public Object toJava() {
        return value;
    }

    @Override
    public String repr() {
        return repr(value);
    }
    /// CPython's quoting rules: single quotes unless the text holds a single
    /// quote and no double quote.
    public static String repr(String s) {
        char q = s.indexOf('\'') >= 0 && s.indexOf('"') < 0 ? '"' : '\'';
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append(q);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == q || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 0x20 || c >= 0x7f && c <= 0xa0 || c == 0xad) {
                // C0 and C1 controls and the two unprintable Latin-1 spaces.
                sb.append("\\x");
                hex(sb, c, 2);
            } else if (c >= 0xD800 && c < 0xE000 && !pairAt(s, i) && (i == 0 || !pairAt(s, i - 1))) {
                // A lone surrogate is not printable.
                sb.append('\\').append('u');
                hex(sb, c, 4);
            } else if (c > 0xff) {
                int cp = pairAt(s, i) ? Character.codePointAt(s, i) : c;
                int n = Character.charCount(cp);
                if (printable(cp)) {
                    sb.append(s, i, i + n);
                } else if (cp < 0x10000) {
                    sb.append('\\').append('u');
                    hex(sb, cp, 4);
                } else {
                    sb.append('\\').append('U');
                    hex(sb, cp, 8);
                }
                i += n - 1;
            } else {
                sb.append(c);
            }
        }
        sb.append(q);
        return sb.toString();
    }

    /// str.isprintable() for one code point: the C0 and C1 controls, DEL,
    /// NBSP and the soft hyphen are not; above Latin-1, separators,
    /// controls, format characters, private use and unassigned code points
    /// are escaped by repr().
    static boolean isPrintable(int cp) {
        if (cp < 0x20 || cp >= 0x7f && cp <= 0xa0 || cp == 0xad) {
            return false;
        }
        return cp <= 0xff || printable(cp);
    }

    /// From CPython's own tables (`UnicodeTables.PRINTABLE`): the device's
    /// java.lang.Character has no categories above Latin-1, so asking it
    /// escaped every emoji and CJK character in repr() on iOS.
    private static boolean printable(int cp) {
        return Unicode.printable(cp);
    }

    static void hex(StringBuilder sb, int v, int digits) {
        String h = Integer.toHexString(v);
        for (int i = h.length(); i < digits; i++) {
            sb.append('0');
        }
        sb.append(h);
    }

    @Override
    public boolean isTrue() {
        return value.length() != 0;
    }

    @Override
    public int len() {
        return codePoints();
    }

    @Override
    public long hash() {
        return value.hashCode();
    }

    @Override
    public Object eq(Object o) {
        String b = sv(o);
        return b != null ? Py.bool(value.equals(b)) : null;
    }

    private int cmp(Object o) {
        return compare(value, sv(o));
    }

    /// Code point order; UTF-16 order differs only around surrogates.
    static int compare(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            int ca = a.codePointAt(i);
            int cb = b.codePointAt(i);
            if (ca != cb) {
                return ca < cb ? -1 : 1;
            }
        }
        return a.length() - b.length();
    }

    @Override
    public Object lt(Object o) {
        return sv(o) != null ? Py.bool(cmp(o) < 0) : null;
    }

    @Override
    public Object le(Object o) {
        return sv(o) != null ? Py.bool(cmp(o) <= 0) : null;
    }

    @Override
    public Object gt(Object o) {
        return sv(o) != null ? Py.bool(cmp(o) > 0) : null;
    }

    @Override
    public Object ge(Object o) {
        return sv(o) != null ? Py.bool(cmp(o) >= 0) : null;
    }

    @Override
    public Object add(Object o) {
        String b = sv(o);
        return b != null ? value.concat(b) : null;
    }

    @Override
    public Object mul(Object o) {
        if (!(Py.isInt(o))) {
            return null;
        }
        long n = Py.asLong(o);
        if (n <= 0 || value.length() == 0) {
            return Py.EMPTY_STR;
        }
        StringBuilder sb = new StringBuilder((int) Math.min(Integer.MAX_VALUE, value.length() * n));
        for (long i = 0; i < n; i++) {
            sb.append(value);
        }
        return sb.toString();
    }

    @Override
    public Object rmul(Object o) {
        return mul(o);
    }

    @Override
    public Object mod(Object o) {
        return Formatting.percent(value, o);
    }

    @Override
    public boolean contains(Object x) {
        String b = sv(x);
        if (b == null) {
            throw Py.typeError("'in <string>' requires string as left operand, not " + Py.typeOf(x).name);
        }
        return value.indexOf(b) >= 0;
    }

    @Override
    public Object getItem(Object key) {
        int n = codePoints();
        if (key instanceof PySlice) {
            PySlice s = (PySlice) key;
            int[] r = s.indices(n);
            if (r[2] == 1) {
                return r[1] <= r[0] ? Py.EMPTY_STR : substr(r[0], r[1]);
            }
            StringBuilder sb = new StringBuilder();
            int count = PySlice.length(r);
            for (int i = 0, j = r[0]; i < count; i++, j += r[2]) {
                sb.append(substr(j, j + 1));
            }
            return sb.toString();
        }
        long i = Py.index(key);
        if (i < 0) {
            i += n;
        }
        if (i < 0 || i >= n) {
            throw Py.indexError("string index out of range");
        }
        return n == value.length() ? Typed.ch(value.charAt((int) i)) : Py.str(substr((int) i, (int) i + 1));
    }

    @Override
    public Object iter() {
        final int n = codePoints();
        // Without surrogates a code point is a char, and Latin-1 ones are
        // shared strings: a loop over ASCII text allocates nothing per step.
        final boolean bmp = n == value.length();
        return new PyIter(n) {
            @Override
            Object at(int i) {
                return bmp ? Typed.ch(value.charAt(i)) : Py.str(substr(i, i + 1));
            }
        };
    }
}
