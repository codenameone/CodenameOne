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

import com.codename1.util.BigInteger;

/// The methods of the builtin types: `str.split`, `list.append`,
/// `dict.items` and the rest. Each receives the instance as `args[0]`.
final class TypeMethods {

    private TypeMethods() {
    }

    private static void def(PyType t, String name, PyBuiltin.Positional p) {
        t.define(name, PyBuiltin.method(t, name, p));
    }

    private static void defKw(PyType t, String name, PyBuiltin.Impl p) {
        t.define(name, PyBuiltin.methodKw(t, name, p));
    }

    static void install() {
        installStr();
        installList();
        installTuple();
        installDict();
        installSet();
        installNumbers();
    }

    // ---- helpers ---------------------------------------------------------

    private static String s(Object o) {
        String v = PyStr.sv(o);
        if (v == null) {
            throw Py.typeError("descriptor requires a 'str' object but received a '" + Py.typeOf(o).name + "'");
        }
        return v;
    }

    private static String strArg(Object o, String fn) {
        String v = PyStr.sv(o);
        if (v == null) {
            throw Py.typeError(fn + " arg must be None or str, not " + Py.typeOf(o).name);
        }
        return v;
    }

    /// Locale-independent ASCII lower case.
    static String asciiLower(String s) {
        StringBuilder sb = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (sb == null) {
                    sb = new StringBuilder(s);
                }
                sb.setCharAt(i, (char) (c + 32));
            }
        }
        return sb == null ? s : sb.toString();
    }

    /// Unicode case mapping per character, which (unlike
    /// `String.toLowerCase()`) does not depend on the device locale.
    static String mapCase(String s, int mode) {
        if (mode <= 1) {
            // ASCII: one array, and the string itself when nothing changes.
            int n = s.length();
            char[] out = null;
            int i = 0;
            for (; i < n; i++) {
                char c = s.charAt(i);
                if (c >= 128) {
                    break;
                }
                char r = mode == 1 ? (c >= 'a' && c <= 'z' ? (char) (c - 32) : c)
                        : (c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
                if (r != c && out == null) {
                    out = new char[n];
                    s.getChars(0, i, out, 0);
                }
                if (out != null) {
                    out[i] = r;
                }
            }
            if (i == n) {
                return out == null ? s : new String(out);
            }
        }
        StringBuilder sb = new StringBuilder(s.length());
        boolean prevCased = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char r;
            switch (mode) {
                case 0:
                    r = Character.toLowerCase(c);
                    break;
                case 1:
                    r = Character.toUpperCase(c);
                    break;
                case 2:
                    r = Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c);
                    break;
                default:
                    r = prevCased ? Character.toLowerCase(c) : Character.toUpperCase(c);
                    prevCased = Character.isLetter(c);
                    break;
            }
            if (mode == 1 && c == '\u00df') {
                sb.append("SS");
                continue;
            }
            sb.append(r);
        }
        return sb.toString();
    }

    /// The words of `str.split()` one at a time, for a loop that only walks
    /// them: a str cannot change, so splitting lazily yields the same words
    /// as splitting first, without the list.
    static final class Words extends PyObject {
        private final String v;
        private int pos;

        Words(String v) {
            this.v = v;
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
            int n = v.length();
            int a = pos;
            // The range test first: isSpace is a call, and words are mostly
            // printable ASCII.
            char c;
            while (a < n && ((c = v.charAt(a)) == ' ' || (c < ' ' || c >= '\u0085') && isSpace(c))) {
                a++;
            }
            if (a >= n) {
                pos = n;
                return null;
            }
            int b = a + 1;
            while (b < n && ((c = v.charAt(b)) > ' ' && c < '\u0085' || c != ' ' && !isSpace(c))) {
                b++;
            }
            pos = b;
            return v.substring(a, b);
        }
    }

    /// The pieces of `str.split(sep)` one at a time (see Words).
    static final class Pieces extends PyObject {
        private final String v;
        private final String sep;
        private int pos;

        Pieces(String v, String sep) {
            this.v = v;
            this.sep = sep;
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
            int a = pos;
            if (a < 0) {
                return null;
            }
            int b = v.indexOf(sep, a);
            if (b < 0) {
                pos = -1;
                return v.substring(a);
            }
            pos = b + sep.length();
            return v.substring(a, b);
        }
    }

    private static boolean isSpace(char c) {
        if (c > ' ' && c < '\u0085') {
            return false;
        }
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000b' || c == '\u000c'
                || c == '\u001c' || c == '\u001d' || c == '\u001e' || c == '\u001f' || c == '\u0085'
                || Character.isWhitespace(c) || c == '\u00a0';
    }

    private static String strip(String v, Object chars, boolean left, boolean right) {
        int a = 0;
        int b = v.length();
        String cs = chars == null || chars == Py.NONE ? null : strArg(chars, "strip"); //NOPMD CompareObjectsWithEquals - identity is meant
        while (left && a < b && (cs == null ? isSpace(v.charAt(a)) : cs.indexOf(v.charAt(a)) >= 0)) {
            a++;
        }
        while (right && b > a && (cs == null ? isSpace(v.charAt(b - 1)) : cs.indexOf(v.charAt(b - 1)) >= 0)) {
            b--;
        }
        return v.substring(a, b);
    }

    static PyList split(String v, Object sepO, long max) {
        if (sepO == null || sepO == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            // Counted first, so the list is one array of the right size.
            int n = v.length();
            int count = 0;
            for (int i = 0; i < n;) {
                while (i < n && isSpace(v.charAt(i))) {
                    i++;
                }
                if (i >= n) {
                    break;
                }
                count++;
                if (max >= 0 && count > max) {
                    break;
                }
                while (i < n && !isSpace(v.charAt(i))) {
                    i++;
                }
            }
            Object[] parts = new Object[count];
            int k = 0;
            int i = 0;
            while (k < count) {
                while (isSpace(v.charAt(i))) {
                    i++;
                }
                if (max >= 0 && k == max) {
                    parts[k++] = strip(v.substring(i), null, false, true);
                    break;
                }
                int j = i;
                while (j < n && !isSpace(v.charAt(j))) {
                    j++;
                }
                parts[k++] = v.substring(i, j);
                i = j;
            }
            return new PyList(parts);
        }
        PyList out = new PyList();
        String sep = strArg(sepO, "split");
        if (sep.length() == 0) {
            throw Py.valueError("empty separator");
        }
        int i = 0;
        while (max < 0 || out.size() < max) {
            int j = v.indexOf(sep, i);
            if (j < 0) {
                break;
            }
            out.append(Py.str(v.substring(i, j)));
            i = j + sep.length();
        }
        out.append(Py.str(v.substring(i)));
        return out;
    }

    private static PyList rsplit(String v, Object sepO, long max) {
        if (max < 0) {
            return split(v, sepO, max);
        }
        PyList rev = new PyList();
        if (sepO == null || sepO == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            int j = v.length();
            while (true) {
                while (j > 0 && isSpace(v.charAt(j - 1))) {
                    j--;
                }
                if (j <= 0) {
                    break;
                }
                if (rev.size() >= max) {
                    rev.append(Py.str(strip(v.substring(0, j), null, true, false)));
                    break;
                }
                int i = j;
                while (i > 0 && !isSpace(v.charAt(i - 1))) {
                    i--;
                }
                rev.append(Py.str(v.substring(i, j)));
                j = i;
            }
        } else {
            String sep = strArg(sepO, "rsplit");
            int j = v.length();
            while (rev.size() < max) {
                int i = v.lastIndexOf(sep, j - sep.length());
                if (i < 0) {
                    break;
                }
                rev.append(Py.str(v.substring(i + sep.length(), j)));
                j = i;
            }
            rev.append(Py.str(v.substring(0, j)));
        }
        rev.reverse();
        return rev;
    }

    /// Resolves the optional `start, end` arguments of find/count/startswith
    /// to UTF-16 offsets.
    private static int[] range(String self, Object[] a, int from) {
        int n = PyStr.codePoints(self);
        int[] r = new PySlice(a.length > from ? a[from] : Py.NONE, a.length > from + 1 ? a[from + 1] : Py.NONE,
                Py.NONE).indices(n);
        return new int[] {PyStr.offset(self, r[0]), PyStr.offset(self, Math.max(r[0], r[1]))};
    }

    private static int find(String self, Object[] a, boolean last) {
        String sub = strArg(a[1], "find");
        int[] r = range(self, a, 2);
        String v = self;
        if (r[1] - r[0] < sub.length()) {
            return -1;
        }
        int i = last ? v.lastIndexOf(sub, r[1] - sub.length()) : v.indexOf(sub, r[0]);
        if (i < r[0] || i + sub.length() > r[1]) {
            return -1;
        }
        return PyStr.cpIndex(self, i);
    }

    private static boolean affix(String self, Object[] a, boolean start) {
        int[] r = range(self, a, 2);
        String v = self.substring(r[0], r[1]);
        Object[] cands = a[1] instanceof PyTuple ? ((PyTuple) a[1]).items : new Object[] {a[1]};
        for (Object cand : cands) {
            String c = strArg(cand, start ? "startswith" : "endswith");
            if (start ? v.startsWith(c) : v.endsWith(c)) {
                return true;
            }
        }
        return false;
    }

    private static Object pad(Object[] a, int mode) {
        String v = s(a[0]);
        int width = (int) Py.asLong(a[1]);
        String fill = a.length > 2 ? strArg(a[2], "fill") : " ";
        int n = PyStr.codePoints((String) a[0]);
        if (width <= n) {
            return a[0];
        }
        int total = width - n;
        int left = mode == 0 ? 0 : mode == 1 ? total : total / 2 + (total & width & 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < left; i++) {
            sb.append(fill);
        }
        sb.append(v);
        for (int i = left; i < total; i++) {
            sb.append(fill);
        }
        return Py.str(sb.toString());
    }

    private static boolean all(String v, int kind) {
        if (v.length() == 0) {
            return false;
        }
        boolean cased = false;
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            boolean ok;
            switch (kind) {
                case 0:
                    ok = Character.isDigit(c);
                    break;
                case 1:
                    ok = Character.isLetter(c);
                    break;
                case 2:
                    ok = Character.isLetterOrDigit(c);
                    break;
                case 3:
                    ok = isSpace(c);
                    break;
                case 4:
                    ok = !Character.isLowerCase(c);
                    cased |= Character.isUpperCase(c);
                    break;
                default:
                    ok = !Character.isUpperCase(c);
                    cased |= Character.isLowerCase(c);
                    break;
            }
            if (!ok) {
                return false;
            }
        }
        return kind < 4 || cased;
    }

    /// `sep.join(items)`; a list is read in place, without an iterator.
    static Object join(String sep, Object items) {
        if (items instanceof PyList) {
            PyList l = (PyList) items;
            int n = l.size;
            if (n == 1 && l.get(0) instanceof String) {
                return l.get(0);
            }
            // String.join, which ParparVM builds in one allocation that the
            // parts' bytes are copied straight into.
            String[] parts = new String[n];
            for (int i = 0; i < n; i++) {
                Object v = l.get(i);
                if (!(v instanceof String)) {
                    throw Py.typeError("sequence item " + i + ": expected str instance, " + Py.typeOf(v).name + " found");
                }
                parts[i] = (String) v;
            }
            return String.join(sep, parts);
        }
        Object it = Py.iter(items);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        int i = 0;
        for (Object v = Py.next(it); v != null; v = Py.next(it), i++) {
            if (!(v instanceof String)) {
                throw Py.typeError("sequence item " + i + ": expected str instance, " + Py.typeOf(v).name + " found");
            }
            if (!first) {
                sb.append(sep);
            }
            first = false;
            sb.append(((String) v));
        }
        return Py.str(sb.toString());
    }

    private static void installStr() {
        PyType t = Py.STR_TYPE;
        def(t, "join", a -> join(s(a[0]), a[1]));
        defKw(t, "split", (args, kw) -> {
            Args a = new Args("split", args, kw).check(3, "sep", "maxsplit");
            return split(s(args[0]), a.get(1, "sep", Py.NONE), Py.asLong(a.get(2, "maxsplit", PyInt.of(-1))));
        });
        defKw(t, "rsplit", (args, kw) -> {
            Args a = new Args("rsplit", args, kw).check(3, "sep", "maxsplit");
            return rsplit(s(args[0]), a.get(1, "sep", Py.NONE), Py.asLong(a.get(2, "maxsplit", PyInt.of(-1))));
        });
        defKw(t, "splitlines", (args, kw) -> {
            Args a = new Args("splitlines", args, kw).check(2, "keepends");
            boolean keep = Py.isTrue(a.get(1, "keepends", Py.FALSE));
            String v = s(args[0]);
            PyList out = new PyList();
            int i = 0;
            int start = 0;
            while (i < v.length()) {
                char c = v.charAt(i);
                if (c == '\n' || c == '\r' || c == '\u000b' || c == '\u000c' || c == '\u001c' || c == '\u001d'
                        || c == '\u001e' || c == '\u0085' || c == '\u2028' || c == '\u2029') {
                    int end = i;
                    i++;
                    if (c == '\r' && i < v.length() && v.charAt(i) == '\n') {
                        i++;
                    }
                    out.append(Py.str(v.substring(start, keep ? i : end)));
                    start = i;
                } else {
                    i++;
                }
            }
            if (start < v.length()) {
                out.append(Py.str(v.substring(start)));
            }
            return out;
        });
        def(t, "strip", a -> Py.str(strip(s(a[0]), a.length > 1 ? a[1] : null, true, true)));
        def(t, "lstrip", a -> Py.str(strip(s(a[0]), a.length > 1 ? a[1] : null, true, false)));
        def(t, "rstrip", a -> Py.str(strip(s(a[0]), a.length > 1 ? a[1] : null, false, true)));
        def(t, "replace", a -> {
            String v = s(a[0]);
            String old = strArg(a[1], "replace");
            String nw = strArg(a[2], "replace");
            long count = a.length > 3 ? Py.asLong(a[3]) : -1;
            if (count < 0) {
                if (old.length() == 0) {
                    StringBuilder sb = new StringBuilder(nw);
                    for (int i = 0; i < v.length(); i++) {
                        sb.append(v.charAt(i)).append(nw);
                    }
                    return Py.str(sb.toString());
                }
                return Py.str(v.replace(old, nw));
            }
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (long k = 0; k < count; k++) {
                int j = v.indexOf(old, i);
                if (j < 0) {
                    break;
                }
                sb.append(v, i, j).append(nw);
                i = j + old.length();
                if (old.length() == 0) {
                    if (i < v.length()) {
                        sb.append(v.charAt(i));
                    }
                    i++;
                }
            }
            if (i <= v.length()) {
                sb.append(v.substring(Math.min(i, v.length())));
            }
            return Py.str(sb.toString());
        });
        def(t, "startswith", a -> Py.bool(affix(s(a[0]), a, true)));
        def(t, "endswith", a -> Py.bool(affix(s(a[0]), a, false)));
        def(t, "find", a -> PyInt.of(find(s(a[0]), a, false)));
        def(t, "rfind", a -> PyInt.of(find(s(a[0]), a, true)));
        def(t, "index", a -> {
            int i = find(s(a[0]), a, false);
            if (i < 0) {
                throw Py.valueError("substring not found");
            }
            return PyInt.of(i);
        });
        def(t, "rindex", a -> {
            int i = find(s(a[0]), a, true);
            if (i < 0) {
                throw Py.valueError("substring not found");
            }
            return PyInt.of(i);
        });
        def(t, "count", a -> {
            String self = s(a[0]);
            String sub = strArg(a[1], "count");
            int[] r = range(self, a, 2);
            String v = self.substring(r[0], r[1]);
            if (sub.length() == 0) {
                return PyInt.of(PyStr.codePoints(v) + 1);
            }
            int n = 0;
            for (int i = v.indexOf(sub); i >= 0; i = v.indexOf(sub, i + sub.length())) {
                n++;
            }
            return PyInt.of(n);
        });
        def(t, "upper", a -> Py.str(mapCase(s(a[0]), 1)));
        def(t, "lower", a -> Py.str(mapCase(s(a[0]), 0)));
        def(t, "casefold", a -> Py.str(mapCase(s(a[0]), 0)));
        def(t, "swapcase", a -> Py.str(mapCase(s(a[0]), 2)));
        def(t, "title", a -> Py.str(mapCase(s(a[0]), 3)));
        def(t, "capitalize", a -> {
            String v = s(a[0]);
            if (v.length() == 0) {
                return a[0];
            }
            return Py.str(Character.toUpperCase(v.charAt(0)) + mapCase(v.substring(1), 0));
        });
        def(t, "isdigit", a -> Py.bool(all(s(a[0]), 0)));
        def(t, "isdecimal", a -> Py.bool(all(s(a[0]), 0)));
        def(t, "isnumeric", a -> Py.bool(all(s(a[0]), 0)));
        def(t, "isalpha", a -> Py.bool(all(s(a[0]), 1)));
        def(t, "isalnum", a -> Py.bool(all(s(a[0]), 2)));
        def(t, "isspace", a -> Py.bool(all(s(a[0]), 3)));
        def(t, "isupper", a -> Py.bool(all(s(a[0]), 4)));
        def(t, "islower", a -> Py.bool(all(s(a[0]), 5)));
        def(t, "isascii", a -> {
            String v = s(a[0]);
            for (int i = 0; i < v.length(); i++) {
                if (v.charAt(i) > 127) {
                    return Py.FALSE;
                }
            }
            return Py.TRUE;
        });
        def(t, "isidentifier", a -> {
            String v = s(a[0]);
            if (v.length() == 0 || !(Character.isLetter(v.charAt(0)) || v.charAt(0) == '_')) {
                return Py.FALSE;
            }
            for (int i = 1; i < v.length(); i++) {
                if (!(Character.isLetterOrDigit(v.charAt(i)) || v.charAt(i) == '_')) {
                    return Py.FALSE;
                }
            }
            return Py.TRUE;
        });
        def(t, "ljust", a -> pad(a, 0));
        def(t, "rjust", a -> pad(a, 1));
        def(t, "center", a -> pad(a, 2));
        def(t, "zfill", a -> {
            String v = s(a[0]);
            int width = (int) Py.asLong(a[1]);
            if (v.length() >= width) {
                return a[0];
            }
            String sign = v.length() > 0 && (v.charAt(0) == '-' || v.charAt(0) == '+') ? v.substring(0, 1) : "";
            StringBuilder sb = new StringBuilder(sign);
            for (int i = v.length(); i < width; i++) {
                sb.append('0');
            }
            return Py.str(sb.append(v.substring(sign.length())).toString());
        });
        def(t, "partition", a -> {
            String v = s(a[0]);
            String sep = strArg(a[1], "partition");
            int i = v.indexOf(sep);
            if (i < 0) {
                return PyTuple.of(a[0], Py.EMPTY_STR, Py.EMPTY_STR);
            }
            return PyTuple.of(Py.str(v.substring(0, i)), a[1], Py.str(v.substring(i + sep.length())));
        });
        def(t, "rpartition", a -> {
            String v = s(a[0]);
            String sep = strArg(a[1], "rpartition");
            int i = v.lastIndexOf(sep);
            if (i < 0) {
                return PyTuple.of(Py.EMPTY_STR, Py.EMPTY_STR, a[0]);
            }
            return PyTuple.of(Py.str(v.substring(0, i)), a[1], Py.str(v.substring(i + sep.length())));
        });
        def(t, "removeprefix", a -> {
            String v = s(a[0]);
            String p = strArg(a[1], "removeprefix");
            return v.startsWith(p) ? Py.str(v.substring(p.length())) : a[0];
        });
        def(t, "removesuffix", a -> {
            String v = s(a[0]);
            String p = strArg(a[1], "removesuffix");
            return p.length() > 0 && v.endsWith(p) ? Py.str(v.substring(0, v.length() - p.length())) : a[0];
        });
        def(t, "expandtabs", a -> {
            String v = s(a[0]);
            int size = a.length > 1 ? (int) Py.asLong(a[1]) : 8;
            StringBuilder sb = new StringBuilder();
            int col = 0;
            for (int i = 0; i < v.length(); i++) {
                char c = v.charAt(i);
                if (c == '\t') {
                    int n = size <= 0 ? 0 : size - col % size;
                    for (int k = 0; k < n; k++) {
                        sb.append(' ');
                    }
                    col += n;
                } else {
                    sb.append(c);
                    col = c == '\n' || c == '\r' ? 0 : col + 1;
                }
            }
            return Py.str(sb.toString());
        });
        defKw(t, "format", (args, kw) -> {
            Object[] pos = new Object[args.length - kw.length - 1];
            System.arraycopy(args, 1, pos, 0, pos.length);
            PyDict named = new PyDict();
            for (int i = 0; i < kw.length; i++) {
                named.setItem(Py.str(kw[i]), args[args.length - kw.length + i]);
            }
            return Py.str(StrFormat.format(s(args[0]), pos, named));
        });
        def(t, "format_map", a -> Py.str(StrFormat.format(s(a[0]), Py.NO_ARGS, a[1])));
        defKw(t, "encode", (a, kw) -> {
            Object[] p = Args.bind("encode", a, kw, new String[] {"self", "encoding", "errors"},
                    new Object[] {null, Py.str("utf-8"), Py.str("strict")});
            return new PyBytes(Codecs.encode(s(p[0]), Py.toStr(p[1]), Py.toStr(p[2])));
        });
        def(t, "__len__", a -> PyInt.of(Py.len(a[0])));
        def(t, "__contains__", a -> Py.bool(Py.obj(a[0]).contains(a[1])));
        def(t, "__getitem__", a -> Py.obj(a[0]).getItem(a[1]));
        t.define("maketrans", new PyStaticMethod(PyBuiltin.positional("maketrans", a -> {
            PyDict d = new PyDict();
            if (a.length == 1) {
                PyDict src = (PyDict) a[0];
                Object[] keys = src.keyArray();
                for (Object key : keys) {
                    Object k = key instanceof String ? PyInt.of(((String) key).codePointAt(0)) : key;
                    d.setItem(k, src.getItem(key));
                }
                return d;
            }
            String from = s(a[0]);
            String to = s(a[1]);
            for (int i = 0; i < from.length(); i++) {
                d.setItem(PyInt.of(from.charAt(i)), PyInt.of(to.charAt(i)));
            }
            if (a.length > 2) {
                String del = s(a[2]);
                for (int i = 0; i < del.length(); i++) {
                    d.setItem(PyInt.of(del.charAt(i)), Py.NONE);
                }
            }
            return d;
        })));
        def(t, "translate", a -> {
            String v = s(a[0]);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < v.length(); i++) {
                int cp = v.codePointAt(i);
                if (cp > 0xffff) {
                    i++;
                }
                Object r;
                try {
                    r = Py.obj(a[1]).getItem(PyInt.of(cp));
                } catch (PyException e) {
                    if (!e.matches(Py.LOOKUP_ERROR)) {
                        throw e;
                    }
                    sb.appendCodePoint(cp);
                    continue;
                }
                if (r == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                    continue;
                }
                if (Py.isInt(r)) {
                    sb.appendCodePoint((int) Py.asLong(r));
                } else {
                    sb.append(Py.toStr(r));
                }
            }
            return Py.str(sb.toString());
        });
    }

    // ---- list ------------------------------------------------------------

    private static PyList l(Object o) {
        if (!(o instanceof PyList)) {
            throw Py.typeError("descriptor requires a 'list' object but received a '" + Py.typeOf(o).name + "'");
        }
        return (PyList) o;
    }

    private static void installList() {
        PyType t = Py.LIST_TYPE;
        def(t, "append", a -> {
            l(a[0]).append(a[1]);
            return Py.NONE;
        });
        def(t, "extend", a -> {
            l(a[0]).extend(a[1]);
            return Py.NONE;
        });
        def(t, "insert", a -> {
            l(a[0]).insert(Py.index(a[1]), a[2]);
            return Py.NONE;
        });
        def(t, "pop", a -> l(a[0]).pop(a.length > 1 ? Py.index(a[1]) : -1));
        def(t, "remove", a -> {
            l(a[0]).remove(a[1]);
            return Py.NONE;
        });
        def(t, "index", a -> PyInt.of(l(a[0]).index(a[1])));
        def(t, "count", a -> {
            PyList self = l(a[0]);
            int n = 0;
            for (int i = 0; i < self.size; i++) {
                if (Py.eqBool(self.get(i), a[1])) {
                    n++;
                }
            }
            return PyInt.of(n);
        });
        defKw(t, "sort", (args, kw) -> {
            Args a = new Args("sort", args, kw).check(1, "key", "reverse");
            Object rev = a.kw("reverse");
            l(args[0]).sort(a.kw("key"), rev != null && Py.isTrue(rev));
            return Py.NONE;
        });
        def(t, "reverse", a -> {
            l(a[0]).reverse();
            return Py.NONE;
        });
        def(t, "clear", a -> {
            l(a[0]).clear();
            return Py.NONE;
        });
        def(t, "copy", a -> new PyList(l(a[0]).toArray()));
        def(t, "__len__", a -> PyInt.of(Py.len(a[0])));
        def(t, "__getitem__", a -> Py.obj(a[0]).getItem(a[1]));
        def(t, "__setitem__", a -> {
            Py.obj(a[0]).setItem(a[1], a[2]);
            return Py.NONE;
        });
        def(t, "__contains__", a -> Py.bool(Py.obj(a[0]).contains(a[1])));
        def(t, "__iter__", a -> Py.iter(a[0]));
    }

    private static void installTuple() {
        PyType t = Py.TUPLE_TYPE;
        def(t, "count", a -> {
            Object[] items = ((PyTuple) a[0]).items;
            int n = 0;
            for (Object item : items) {
                if (Py.eqBool(item, a[1])) {
                    n++;
                }
            }
            return PyInt.of(n);
        });
        def(t, "index", a -> {
            Object[] items = ((PyTuple) a[0]).items;
            for (int i = 0; i < items.length; i++) {
                if (Py.eqBool(items[i], a[1])) {
                    return PyInt.of(i);
                }
            }
            throw Py.valueError("tuple.index(x): x not in tuple");
        });
        def(t, "__len__", a -> PyInt.of(Py.len(a[0])));
        def(t, "__getitem__", a -> Py.obj(a[0]).getItem(a[1]));
    }

    // ---- dict ------------------------------------------------------------

    private static PyDict d(Object o) {
        if (!(o instanceof PyDict)) {
            throw Py.typeError("descriptor requires a 'dict' object but received a '" + Py.typeOf(o).name + "'");
        }
        return (PyDict) o;
    }

    private static void installDict() {
        PyType t = Py.DICT_TYPE;
        def(t, "get", a -> {
            Object v = d(a[0]).getOrNull(a[1]);
            return v != null ? v : a.length > 2 ? a[2] : Py.NONE;
        });
        def(t, "keys", a -> new PyDictView(d(a[0]), 0));
        def(t, "values", a -> new PyDictView(d(a[0]), 1));
        def(t, "items", a -> new PyDictView(d(a[0]), 2));
        def(t, "pop", a -> {
            Object v = d(a[0]).remove(a[1]);
            if (v == null) {
                if (a.length > 2) {
                    return a[2];
                }
                throw Py.keyError(a[1]);
            }
            return v;
        });
        def(t, "popitem", a -> {
            PyDict self = d(a[0]);
            for (int i = self.used - 1; i >= 0; i--) {
                if (self.keys[i] != null) {
                    Object k = self.keys[i];
                    Object v = self.remove(k);
                    return PyTuple.of(k, v);
                }
            }
            throw Py.error(Py.KEY_ERROR, "popitem(): dictionary is empty");
        });
        def(t, "setdefault", a -> {
            PyDict self = d(a[0]);
            Object v = self.getOrNull(a[1]);
            if (v == null) {
                v = a.length > 2 ? a[2] : Py.NONE;
                self.setItem(a[1], v);
            }
            return v;
        });
        defKw(t, "update", (args, kw) -> {
            PyDict self = d(args[0]);
            int npos = args.length - kw.length;
            if (npos > 1) {
                self.update(args[1]);
            }
            for (int i = 0; i < kw.length; i++) {
                self.setItem(Py.str(kw[i]), args[npos + i]);
            }
            return Py.NONE;
        });
        def(t, "clear", a -> {
            d(a[0]).clear();
            return Py.NONE;
        });
        def(t, "copy", a -> d(a[0]).copy());
        t.define("fromkeys", new PyClassMethod(PyBuiltin.positional("fromkeys", a -> {
            PyDict r = new PyDict();
            Object v = a.length > 2 ? a[2] : Py.NONE;
            Object it = Py.iter(a[1]);
            for (Object k = Py.next(it); k != null; k = Py.next(it)) {
                r.setItem(k, v);
            }
            return r;
        })));
        def(t, "__getitem__", a -> Py.obj(a[0]).getItem(a[1]));
        def(t, "__setitem__", a -> {
            Py.obj(a[0]).setItem(a[1], a[2]);
            return Py.NONE;
        });
        def(t, "__contains__", a -> Py.bool(Py.obj(a[0]).contains(a[1])));
        def(t, "__len__", a -> PyInt.of(Py.len(a[0])));
        def(t, "__iter__", a -> Py.iter(a[0]));
    }

    // ---- set -------------------------------------------------------------

    private static PySet st(Object o) {
        if (!(o instanceof PySet)) {
            throw Py.typeError("descriptor requires a 'set' object but received a '" + Py.typeOf(o).name + "'");
        }
        return (PySet) o;
    }

    private static void installSet() {
        PyType[] types = {Py.SET_TYPE, Py.FROZENSET_TYPE};
        for (PyType type : types) {
            PyType t = type;
            def(t, "union", a -> {
                PySet r = st(a[0]);
                for (int i = 1; i < a.length; i++) {
                    r = r.union(a[i]);
                }
                return r == a[0] ? r.copyAs(r.frozen) : r;
            });
            def(t, "intersection", a -> {
                PySet r = st(a[0]).copyAs(st(a[0]).frozen);
                for (int i = 1; i < a.length; i++) {
                    r = r.intersection(a[i]);
                }
                return r;
            });
            def(t, "difference", a -> {
                PySet r = st(a[0]).copyAs(st(a[0]).frozen);
                for (int i = 1; i < a.length; i++) {
                    r = r.difference(a[i]);
                }
                return r;
            });
            def(t, "symmetric_difference", a -> st(a[0]).symmetricDifference(a[1]));
            def(t, "issubset", a -> Py.bool(st(a[0]).isSubset(a[1])));
            def(t, "issuperset", a -> Py.bool(PySet.of(a[1], true).isSubset(a[0])));
            def(t, "isdisjoint", a -> Py.bool(st(a[0]).intersection(a[1]).len() == 0));
            def(t, "copy", a -> st(a[0]).copyAs(st(a[0]).frozen));
            def(t, "__contains__", a -> Py.bool(Py.obj(a[0]).contains(a[1])));
            def(t, "__len__", a -> PyInt.of(Py.len(a[0])));
        }
        PyType t = Py.SET_TYPE;
        def(t, "add", a -> {
            st(a[0]).addItem(a[1]);
            return Py.NONE;
        });
        def(t, "discard", a -> {
            st(a[0]).discard(a[1]);
            return Py.NONE;
        });
        def(t, "remove", a -> {
            if (!st(a[0]).discard(a[1])) {
                throw Py.keyError(a[1]);
            }
            return Py.NONE;
        });
        def(t, "pop", a -> {
            return st(a[0]).pop();
        });
        def(t, "clear", a -> {
            st(a[0]).clear();
            return Py.NONE;
        });
        def(t, "update", a -> {
            PySet self = st(a[0]);
            if (self.frozen) {
                throw Py.attributeError("'frozenset' object has no attribute 'update'");
            }
            for (int i = 1; i < a.length; i++) {
                self.update(a[i]);
            }
            return Py.NONE;
        });
        def(t, "intersection_update", a -> {
            PySet self = st(a[0]);
            self.replaceWith(self.intersection(a[1]));
            return Py.NONE;
        });
        def(t, "symmetric_difference_update", a -> {
            PySet self = st(a[0]);
            self.replaceWith(self.symmetricDifference(a[1]));
            return Py.NONE;
        });
        def(t, "difference_update", a -> {
            PySet self = st(a[0]);
            Object it = Py.iter(a[1]);
            for (Object v = Py.next(it); v != null; v = Py.next(it)) {
                self.discard(v);
            }
            return Py.NONE;
        });
    }

    // ---- numbers ---------------------------------------------------------

    private static boolean little(Object order) {
        String o = Py.toStr(order);
        if ("little".equals(o)) {
            return true;
        }
        if ("big".equals(o)) {
            return false;
        }
        throw Py.valueError("byteorder must be either 'little' or 'big'");
    }

    private static byte[] toBytes(BigInteger v, long length, boolean little, boolean signed) {
        if (length < 0) {
            throw Py.valueError("length argument must be non-negative");
        }
        if (!signed && v.signum() < 0) {
            throw Py.error(Py.OVERFLOW_ERROR, "can't convert negative int to unsigned");
        }
        int bits = signed ? v.bitLength() + 1 : v.bitLength();
        if (bits > length * 8 && !(v.signum() == 0 && length == 0)) {
            throw Py.error(Py.OVERFLOW_ERROR, "int too big to convert");
        }
        byte[] two = v.toByteArray();
        byte[] r = new byte[(int) length];
        byte fill = (byte) (v.signum() < 0 ? 0xFF : 0);
        for (int i = 0; i < r.length; i++) {
            int src = two.length - 1 - i;
            byte bv = src >= 0 ? two[src] : fill;
            r[little ? i : r.length - 1 - i] = bv;
        }
        return r;
    }

    private static void installNumbers() {
        def(Py.INT_TYPE, "bit_length", a -> {
            if (a[0] instanceof Long) {
                long v = ((Long) a[0]).longValue();
                return PyInt.of(v == Long.MIN_VALUE ? 64 : 64 - Long.numberOfLeadingZeros(Math.abs(v)));
            }
            return PyInt.of(Py.intView(a[0]).toBig().abs().bitLength());
        });
        defKw(Py.INT_TYPE, "to_bytes", (args, kw) -> {
            Args a = new Args("to_bytes", args, kw).check(3, "length", "byteorder", "signed");
            return new PyBytes(toBytes(Py.intView(args[0]).toBig(), Py.asLong(a.get(1, "length", PyInt.of(1))),
                    little(a.get(2, "byteorder", Py.str("big"))), Py.isTrue(a.get(3, "signed", Py.FALSE))));
        });
        Py.INT_TYPE.define("from_bytes", new PyClassMethod(new PyBuiltin("from_bytes", (args, kw) -> {
            Args a = new Args("from_bytes", args, kw).check(3, "bytes", "byteorder", "signed");
            Object src = a.get(1, "bytes", null);
            byte[] b;
            if (src instanceof PyBytes) {
                b = ((PyBytes) src).value;
            } else {
                Object[] items = Py.toArray(src);
                b = new byte[items.length];
                for (int i = 0; i < b.length; i++) {
                    b[i] = (byte) Py.asLong(items[i]);
                }
            }
            if (little(a.get(2, "byteorder", Py.str("big")))) {
                byte[] r = new byte[b.length];
                for (int i = 0; i < b.length; i++) {
                    r[i] = b[b.length - 1 - i];
                }
                b = r;
            }
            BigInteger v = b.length == 0 ? BigInteger.ZERO
                    : Py.isTrue(a.get(3, "signed", Py.FALSE)) ? new BigInteger(b) : new BigInteger(1, b);
            Object cls = args[0];
            return cls == Py.INT_TYPE ? PyInt.of(v) : Py.call(cls, PyInt.of(v)); //NOPMD CompareObjectsWithEquals - identity is meant
        })));
        def(Py.INT_TYPE, "as_integer_ratio", a -> new PyTuple(new Object[] {a[0], PyInt.of(1)}));
        def(Py.INT_TYPE, "is_integer", a -> Py.TRUE);
        def(Py.INT_TYPE, "bit_count", a -> PyInt.of(Py.intView(a[0]).toBig().abs().bitCount()));
        def(Py.INT_TYPE, "conjugate", a -> a[0]);
        prop(Py.INT_TYPE, "real", a -> a[0]);
        prop(Py.INT_TYPE, "imag", a -> PyInt.of(0));
        prop(Py.INT_TYPE, "numerator", a -> a[0]);
        prop(Py.INT_TYPE, "denominator", a -> PyInt.of(1));
        prop(Py.FLOAT_TYPE, "real", a -> a[0]);
        prop(Py.FLOAT_TYPE, "imag", a -> Py.ofFloat(0.0));
        def(Py.INT_TYPE, "__index__", a -> a[0]);
        def(Py.INT_TYPE, "__int__", a -> a[0]);
        def(Py.INT_TYPE, "__float__", a -> Py.ofFloat(Py.asDouble(a[0])));
        def(Py.FLOAT_TYPE, "is_integer", a -> {
            double v = Py.asDouble(a[0]);
            return Py.bool(!Double.isInfinite(v) && v == Math.floor(v));
        });
        def(Py.FLOAT_TYPE, "as_integer_ratio", a -> floatRatio(Py.asDouble(a[0])));
        def(Py.FLOAT_TYPE, "__float__", a -> a[0]);
        def(Py.FLOAT_TYPE, "__int__", a -> Builtins.toInt(a[0]));
        def(Py.FLOAT_TYPE, "conjugate", a -> a[0]);
    }

    private static void prop(PyType t, String name, PyBuiltin.Positional body) {
        t.define(name, new PyProperty(PyBuiltin.positional(name, body), null, null));
    }

    /// The exact fraction a double holds, in lowest terms.
    private static Object floatRatio(double v) {
        if (Double.isNaN(v)) {
            throw Py.valueError("cannot convert NaN to integer ratio");
        }
        if (Double.isInfinite(v)) {
            throw Py.error(Py.OVERFLOW_ERROR, "cannot convert Infinity to integer ratio");
        }
        long bits = Double.doubleToLongBits(v);
        int exp = (int) ((bits >> 52) & 0x7ff);
        long mant = bits & ((1L << 52) - 1);
        if (exp == 0) {
            exp = 1;
        } else {
            mant |= 1L << 52;
        }
        exp -= 1075;
        if (mant == 0) {
            return new PyTuple(new Object[] {PyInt.of(0), PyInt.of(1)});
        }
        while ((mant & 1) == 0 && exp < 0) {
            mant >>= 1;
            exp++;
        }
        if (bits < 0) {
            mant = -mant;
        }
        if (exp >= 0) {
            return new PyTuple(new Object[] {PyInt.of(BigInteger.valueOf(mant).shiftLeft(exp)), PyInt.of(1)});
        }
        return new PyTuple(new Object[] {PyInt.of(mant), PyInt.of(BigInteger.valueOf(1).shiftLeft(-exp))});
    }
}
