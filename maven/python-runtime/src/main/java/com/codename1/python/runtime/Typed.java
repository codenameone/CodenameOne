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

/// What the typed code the transpiler generates for a closed program calls.
///
/// In such code a value whose type the whole-program inference proved is
/// held unboxed: a float as a `double`, a bool as a `boolean`, an instance
/// of a compiled class as that Java class, with None as Java null. These
/// helpers convert at the edges with the generic code, and implement the
/// operations whose Python meaning is not the Java one. A conversion that
/// finds a value of another type means the inference was wrong; it raises
/// rather than letting a cast that is not checked on every port go through.
public final class Typed {
    /// A `while True` test javac cannot see as a constant: generated code
    /// after such a loop must not be "unreachable" to it.
    public static boolean yes = true;

    private Typed() {
    }

    /// The receiver of an attribute access, which raises for None.
    public static <T> T nz(T o, String attr) {
        if (o == null) {
            throw Py.attributeError("'NoneType' object has no attribute '" + attr + "'");
        }
        return o;
    }

    /// The failing half of an inline None check on reaching `attr`.
    public static <T> T nzf(String attr) {
        throw Py.attributeError("'NoneType' object has no attribute '" + attr + "'");
    }

    /// Subscripting None.
    public static PyList nzl(PyList l) {
        if (l == null) {
            throw Py.typeError("'NoneType' object is not subscriptable");
        }
        return l;
    }

    public static Object box(Object o) {
        return o == null ? Py.NONE : o;
    }

    public static PyException mismatch(Object o) {
        return Py.runtimeError("internal error: typed code received a value of type '" + Py.typeOf(o).name + "'");
    }

    public static boolean asBool(Object o) {
        if (o == Py.TRUE) { //NOPMD CompareObjectsWithEquals - identity is meant
            return true;
        }
        if (o == Py.FALSE) { //NOPMD CompareObjectsWithEquals - identity is meant
            return false;
        }
        throw mismatch(o);
    }

    /// An int the compiler proved fits in 64 bits, as a `long`. Ints in
    /// that range are always held as a `Long`.
    public static long toLong(Object o) {
        if (o instanceof Long) {
            return ((Long) o).longValue();
        }
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue() ? 1L : 0L;
        }
        throw mismatch(o);
    }

    private static final String[] CHARS = new String[256];

    /// The one-character string of code point `cp`, as iterating a string
    /// yields it: shared below 256, so a loop over Latin-1 text allocates
    /// nothing.
    public static String ch(int cp) {
        if (cp < 256) {
            String s = CHARS[cp];
            if (s == null) {
                s = String.valueOf((char) cp);
                CHARS[cp] = s;
            }
            return s;
        }
        return new String(Character.toChars(cp));
    }

    /// `len(s)`: code points, which Java's UTF-16 length overcounts by one
    /// per surrogate pair.
    public static long slen(String s) {
        // codePointCount counts a lone surrogate as one, as Python does, and
        // on ParparVM answers a Latin-1 string without reading it; a wide
        // string a decoder made has its count in PyStr's cache.
        int pairs = PyStr.cachedSurrogates(s);
        return pairs >= 0 ? s.length() - pairs : s.codePointCount(0, s.length());
    }

    /// Int `o` modulo 2^64: its low 64 bits, as two's complement keeps them.
    public static long low64(Object o) {
        if (o instanceof PyInt) {
            return ((PyInt) o).toBig().longValue();
        }
        return toLong(o);
    }

    /// The ints typed functions returned in place of their `long` results
    /// (a big int, a bool, or a `Long.MIN_VALUE`, the marker itself), last
    /// returned on top. A stack rather than one slot so a caller may hold a
    /// result as its bare marker while it makes further calls, and take
    /// the object only on the slow path, in a method of its own: a ParparVM
    /// method with an object local pays for a stack frame on every call,
    /// the fast path included. Each marker is taken exactly once, the last
    /// first.
    private static final class Returned {
        Object[] items = new Object[8];
        int size;
    }

    /// Each thread's own stack, looked up only when the GIL changes hands.
    private static final ThreadLocal<Returned> RETURNED = new ThreadLocal<Returned>();

    /// The stack of the thread that holds the GIL. One thread runs Python at
    /// a time, so the stack is a static that [Gil] points at the new owner's
    /// as it takes the lock: a `try` and a big-int return were each a
    /// ThreadLocal lookup, which was a tenth of a loop that catches. Code that
    /// runs Python without ever taking the lock shares the first stack, which
    /// is right for the one thread such a program has.
    private static Returned current = new Returned();

    /// Points [#current] at the calling thread's stack: [Gil] calls it on
    /// every acquisition, while it holds the lock.
    static void adoptThread() {
        Returned r = RETURNED.get();
        if (r == null) {
            r = current.size == 0 && !adopted ? current : new Returned();
            adopted = true;
            RETURNED.set(r);
        }
        current = r;
    }

    /// Whether some thread has claimed the initial stack as its own.
    private static boolean adopted;

    private static Returned returned() {
        return current;
    }

    /// Returns int `o` from a typed function whose result is a `long`: the
    /// marker, with `o` left for the caller's `take`.
    public static long retBig(Object o) {
        Returned r = returned();
        if (r.size == r.items.length) {
            Object[] n = new Object[r.size * 2];
            System.arraycopy(r.items, 0, n, 0, r.size);
            r.items = n;
        }
        r.items[r.size++] = o;
        return Long.MIN_VALUE;
    }

    /// `retBig` for an int that may be a `Long` that fits.
    public static long retObj(Object o) {
        if (o instanceof Long) {
            long v = ((Long) o).longValue();
            if (v != Long.MIN_VALUE) {
                return v;
            }
        }
        return retBig(o);
    }

    /// The int behind the last `Long.MIN_VALUE` a typed function returned.
    public static Object take() {
        Returned r = returned();
        Object o = r.items[--r.size];
        r.items[r.size] = null;
        return o;
    }

    /// How many results are waiting to be taken: kept at the start of a
    /// `try`, for `untake`.
    public static int taken() {
        return current.size;
    }

    /// Drops the results an exception left untaken, back to `mark`: one
    /// raised between a call returning a marker and its caller taking it.
    public static void untake(int mark) {
        Returned r = current;
        while (r.size > mark) {
            r.items[--r.size] = null;
        }
    }

    /// A typed function's `long` result as an object.
    public static Object unret(long v) {
        return v != Long.MIN_VALUE ? Long.valueOf(v) : take();
    }

    public static double asFloat(Object o) {
        if (o instanceof Double) {
            return ((Double) o).doubleValue();
        }
        throw mismatch(o);
    }

    public static String asStr(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        if (o == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        throw mismatch(o);
    }

    public static PyList asList(Object o) {
        if (o instanceof PyList) {
            return (PyList) o;
        }
        if (o == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        throw mismatch(o);
    }

    /// `float(i)` of an int.
    public static double toFloat(Object i) {
        if (i instanceof Long) {
            return ((Long) i).longValue();
        }
        return Py.asDouble(i);
    }

    /// A range bound, which must fit a Java long here.
    public static long bound(Object i) {
        if (i instanceof Long) {
            return ((Long) i).longValue();
        }
        return Py.asLong(i);
    }

    // ---- ints ------------------------------------------------------------

    public static boolean lt(Object a, Object b) {
        if (a instanceof Long && b instanceof Long) {
            return ((Long) a).longValue() < ((Long) b).longValue();
        }
        return Py.isTrue(Py.lt(a, b));
    }

    public static boolean le(Object a, Object b) {
        if (a instanceof Long && b instanceof Long) {
            return ((Long) a).longValue() <= ((Long) b).longValue();
        }
        return Py.isTrue(Py.le(a, b));
    }

    public static boolean gt(Object a, Object b) {
        if (a instanceof Long && b instanceof Long) {
            return ((Long) a).longValue() > ((Long) b).longValue();
        }
        return Py.isTrue(Py.gt(a, b));
    }

    public static boolean ge(Object a, Object b) {
        if (a instanceof Long && b instanceof Long) {
            return ((Long) a).longValue() >= ((Long) b).longValue();
        }
        return Py.isTrue(Py.ge(a, b));
    }

    /// `x in c`, without the view a str container would otherwise allocate.
    public static boolean in(Object x, Object c) {
        if (c instanceof String && x instanceof String) {
            return ((String) c).indexOf((String) x) >= 0;
        }
        if (c instanceof PyDict) {
            return ((PyDict) c).contains(x);
        }
        return Py.obj(c).contains(x);
    }

    public static boolean eq(Object a, Object b) {
        if (a instanceof Long && b instanceof Long) {
            return ((Long) a).longValue() == ((Long) b).longValue();
        }
        return Py.eqBool(a, b);
    }

    // ---- floats ----------------------------------------------------------

    public static double div(double a, double b) {
        if (b == 0) {
            throw Py.zeroDivision("division by zero");
        }
        return a / b;
    }

    public static double floordiv(double a, double b) {
        return Py.asDouble(Py.floordiv(Double.valueOf(a), Double.valueOf(b)));
    }

    public static double mod(double a, double b) {
        return Py.asDouble(Py.mod(Double.valueOf(a), Double.valueOf(b)));
    }

    /// `a / b` of two ints.
    /// `x ** y` for two floats the transpiler proved has a real answer (an
    /// integral exponent, or a base that cannot be negative). The cases Python
    /// raises on, a zero base to a negative power and an overflow, go through
    /// the boxed operator so they raise as it does.
    public static double pow(double x, double y) {
        double r = Py.powd(x, y);
        if (!Double.isNaN(r) && !Double.isInfinite(r)) {
            return r;
        }
        Object o = Py.pow(Double.valueOf(x), Double.valueOf(y));
        if (o instanceof Double) {
            return ((Double) o).doubleValue();
        }
        throw Py.error(Py.TYPE_ERROR, "float ** float answered a complex");
    }

    public static double truediv(Object a, Object b) {
        return Py.asDouble(Py.truediv(a, b));
    }

    // ---- lists -----------------------------------------------------------

    public static Object get(PyList l, Object i) {
        nzl(l);
        if (i instanceof Long) {
            long k = ((Long) i).longValue();
            if (k >= 0 && k < l.size) {
                int[] n = l.ints;
                if (n != null) {
                    return Long.valueOf(n[(int) k]);
                }
                double[] f = l.floats;
                if (f != null) {
                    return Double.valueOf(f[(int) k]);
                }
                return l.items[(int) k];
            }
        }
        return l.getItem(i);
    }

    public static double getF(PyList l, Object i) {
        nzl(l);
        double[] f = l.floats;
        if (f != null && i instanceof Long) {
            long k = ((Long) i).longValue();
            if (k >= 0 && k < l.size) {
                return f[(int) k];
            }
        }
        return asFloat(l.getItem(i));
    }

    public static void set(PyList l, Object i, Object v) {
        nzl(l);
        if (i instanceof Long) {
            long k = ((Long) i).longValue();
            if (k >= 0 && k < l.size) {
                int[] n = l.ints;
                if (n != null) {
                    if (v instanceof Long && PyList.fits(((Long) v).longValue())) {
                        n[(int) k] = (int) ((Long) v).longValue();
                        return;
                    }
                } else if (l.floats == null) {
                    l.items[(int) k] = v;
                    return;
                }
            }
        }
        l.setItem(i, v);
    }

    public static void setF(PyList l, Object i, double v) {
        nzl(l);
        double[] f = l.floats;
        if (f != null && i instanceof Long) {
            long k = ((Long) i).longValue();
            if (k >= 0 && k < l.size) {
                f[(int) k] = v;
                return;
            }
        }
        l.setItem(i, Double.valueOf(v));
    }

    public static Object len(PyList l) {
        return Long.valueOf(nzl(l).size);
    }

    /// Iterating over None.
    public static PyList iterable(PyList l) {
        if (l == null) {
            throw Py.typeError("'NoneType' object is not iterable");
        }
        return l;
    }

    /// Item `i` of a float list, `i` in range.
    public static double atF(PyList l, int i) {
        double[] f = l.floats;
        return f != null ? f[i] : asFloat(l.get(i));
    }

    public static PyList list(Object[] items) {
        return PyList.of(items);
    }

    /// `list(range(stop))`.
    public static PyList rangeList(Object stop) {
        return rangeList(Long.valueOf(0), stop, Long.valueOf(1));
    }

    /// `list(range(start, stop))`.
    public static PyList rangeList(Object start, Object stop) {
        return rangeList(start, stop, Long.valueOf(1));
    }

    /// `list(range(start, stop, step))`.
    public static PyList rangeList(Object start, Object stop, Object step) {
        long a = bound(start);
        long b = bound(stop);
        long c = bound(step);
        if (c == 0) {
            throw Py.valueError("range() arg 3 must not be zero");
        }
        PyList l = new PyList();
        if (c > 0) {
            for (long i = a; i < b; i += c) {
                l.append(Long.valueOf(i));
                if (b - i <= c) {
                    // The next step would pass the end (or overflow).
                    break;
                }
            }
        } else {
            for (long i = a; i > b; i += c) {
                l.append(Long.valueOf(i));
                if (c == Long.MIN_VALUE || i - b <= -c) {
                    break;
                }
            }
        }
        return l;
    }

    /// `list(l)`, a shallow copy.
    public static PyList copy(PyList l) {
        return iterable(l).getSlice(Py.NONE, Py.NONE, Py.NONE);
    }

    public static Object append(PyList l, Object v) {
        nzl(l).append(v);
        return Py.NONE;
    }

    /// `l.extend(other)` for two lists.
    public static Object extend(PyList l, PyList other) {
        nzl(l).extend(nzl(other));
        return Py.NONE;
    }

    /// `len(o)` where `f` is what the name `len` holds, for typed code,
    /// which takes the result to be an int as the builtin's is.
    /// `m.get(k)` for an int key: a dict answers without the call or a
    /// boxed key; anything else goes through `site`.
    public static Object dget(AttrSite site, Object m, long k) {
        if (m instanceof PyDict) {
            Object v = ((PyDict) m).getLong(k);
            return v != null ? v : Py.NONE;
        }
        return site.call1(m, Long.valueOf(k));
    }

    /// `m[k] = v` for an int key.
    /// `s.split()` / `s.split(sep)` on a str: a fresh list of strs.
    public static PyList split(String s, Object sep) {
        return TypeMethods.split(s, sep, -1);
    }

    /// `d[k]` on a dict the transpiler followed from a display: a plain
    /// `PyDict`, so a key that is not there raises KeyError, as the generic
    /// read does.
    public static Object dgetv(Object d, Object k) {
        Object v = ((PyDict) d).getOrNull(k);
        return v != null ? v : Py.getitem(d, k);
    }

    /// `d.get(k, def)` on a plain `PyDict`: no attribute lookup, no call.
    public static Object dgetd(Object d, Object k, Object def) {
        Object v = ((PyDict) d).getOrNull(k);
        return v != null ? v : def;
    }

    /// Whether `o` is a plain dict, of no subclass: what a typed function
    /// taking a dict the transpiler followed may be entered with.
    public static boolean isDict(Object o) {
        return o != null && o.getClass() == PyDict.class;
    }

    public static void dset(Object m, long k, Object v) {
        if (m instanceof PyDict) {
            ((PyDict) m).setLong(k, v);
            return;
        }
        Py.setitem(m, Long.valueOf(k), v);
    }

    public static long len(Object f, Object o) {
        if (f == Builtins.LEN) { //NOPMD CompareObjectsWithEquals - identity is meant
            if (o instanceof PyDict) {
                return ((PyDict) o).size();
            }
            return Py.len(o);
        }
        Object r = Py.call(f, o);
        if (r instanceof Long) {
            return ((Long) r).longValue();
        }
        throw Py.typeError("len was rebound to a function that does not return an int");
    }

    /// `l.extend([v] * n)`.
    public static Object extendRepeat(PyList l, Object v, Object n) {
        nzl(l).extendRepeat(v, Py.index(n));
        return Py.NONE;
    }

    /// `ord(s)`: TypeError unless `s` is one character.
    public static Object ord(String s) {
        return Builtins.ord(s);
    }

    public static boolean truthy(String s) {
        return s != null && s.length() != 0;
    }

    public static boolean truthy(PyList l) {
        return l != null && l.size != 0;
    }

    public static String concat(String a, String b) {
        return a.concat(b);
    }

    public static Object abs(Object i) {
        if (i instanceof Long) {
            long v = ((Long) i).longValue();
            if (v != Long.MIN_VALUE) {
                return Long.valueOf(v < 0 ? -v : v);
            }
        }
        return Py.call(Builtins.lookup("abs"), i);
    }

    /// A None-typed value where a nullable one is expected.
    public static <T> T none(Object ignored) {
        return null;
    }

    /// An expression statement whose value is dropped.
    public static void drop(Object ignored) {
    }

    /// `s.strip()`, `lstrip()` and `rstrip()` with no argument, on a str
    /// the transpiler knows is one.
    public static String strip(String s, boolean left, boolean right) {
        return TypeMethods.strip(s, null, left, right);
    }

    /// `s.strip(chars)` and its one-sided forms.
    public static String strip(String s, String chars, boolean left, boolean right) {
        return TypeMethods.strip(s, chars, left, right);
    }

    /// `s.find(sub)` and `s.rfind(sub)` over the whole string.
    public static long find(String s, String sub, boolean last) {
        return TypeMethods.findWhole(s, sub, last);
    }

    /// `s.replace(old, new)` with no count.
    public static String replace(String v, String old, String nw) {
        if (old.length() == 0) {
            StringBuilder sb = new StringBuilder(nw);
            for (int i = 0; i < v.length(); i++) {
                sb.append(v.charAt(i)).append(nw);
            }
            return sb.toString();
        }
        return v.replace(old, nw);
    }

    /// `s.count(sub)` over the whole string.
    public static long count(String v, String sub) {
        if (sub.length() == 0) {
            return PyStr.codePoints(v) + 1;
        }
        long n = 0;
        for (int i = v.indexOf(sub); i >= 0; i = v.indexOf(sub, i + sub.length())) {
            n++;
        }
        return n;
    }

    /// `s.lower()` (mode 0) and `s.upper()` (mode 1).
    public static String mapCase(String s, int mode) {
        return TypeMethods.mapCase(s, mode);
    }

    /// `sep.join(items)`.
    public static String join(String sep, Object items) {
        return TypeMethods.join(sep, items);
    }

    /// `s[a:b:c]` on a str the transpiler knows is one.
    public static String slice(String s, Object a, Object b, Object c) {
        Object r = Py.getslice(s, a, b, c);
        return r instanceof String ? (String) r : String.valueOf(r);
    }

    /// `fmt % args` for a str format and a tuple of values.
    public static String format(String fmt, Object args) {
        Object r = Py.mod(fmt, args);
        return r instanceof String ? (String) r : Py.toStr(r);
    }
}
