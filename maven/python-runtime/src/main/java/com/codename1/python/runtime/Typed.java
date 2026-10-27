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
        int n = s.length();
        long r = n;
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c >= 0xD800 && c < 0xDC00 && i + 1 < n) {
                char d = s.charAt(i + 1);
                if (d >= 0xDC00 && d < 0xE000) {
                    r--;
                    i++;
                }
            }
        }
        return r;
    }

    /// Int `o` modulo 2^64: its low 64 bits, as two's complement keeps them.
    public static long low64(Object o) {
        if (o instanceof PyInt) {
            return ((PyInt) o).toBig().longValue();
        }
        return toLong(o);
    }

    /// The int a typed function returned in place of its `long` result:
    /// a big int, a bool, or a `Long.MIN_VALUE` (the marker itself).
    private static final ThreadLocal<Object> RETURNED = new ThreadLocal<Object>();

    /// Returns int `o` from a typed function whose result is a `long`: the
    /// marker, with `o` left for the caller's `take`.
    public static long retBig(Object o) {
        RETURNED.set(o);
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

    /// The int behind a `Long.MIN_VALUE` a typed function returned.
    public static Object take() {
        Object o = RETURNED.get();
        RETURNED.set(null);
        return o;
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
    public static double truediv(Object a, Object b) {
        return Py.asDouble(Py.truediv(a, b));
    }

    // ---- lists -----------------------------------------------------------

    public static Object get(PyList l, Object i) {
        nzl(l);
        if (i instanceof Long) {
            long k = ((Long) i).longValue();
            if (k >= 0 && k < l.size) {
                long[] n = l.ints;
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
                long[] n = l.ints;
                if (n != null) {
                    if (v instanceof Long) {
                        n[(int) k] = ((Long) v).longValue();
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
    public static long len(Object f, Object o) {
        if (f == Builtins.LEN) { //NOPMD CompareObjectsWithEquals - identity is meant
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
}
