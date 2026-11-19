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

/// `_cn1heapq` and `_cn1bisect`: the heap and bisection primitives CPython
/// implements in C (`_heapq`, `_bisect`). `heapq.merge`, `nlargest` and
/// `nsmallest` stay Python, as they are in CPython. Each algorithm is the C
/// module's, so a heap of objects with a user `__lt__` sees the same
/// comparisons in the same order; and as there, a comparison that changes
/// the list's size is an error rather than a read past its end.
final class HeapqBisect {

    private HeapqBisect() {
    }

    static boolean provide(String name) {
        if ("_cn1heapq".equals(name)) {
            PyModule.registerModule(heapq());
            return true;
        }
        if ("_cn1bisect".equals(name)) {
            PyModule.registerModule(bisect());
            return true;
        }
        return false;
    }

    private static PyList list(String fn, Object o) {
        if (!(o instanceof PyList)) {
            throw Py.typeError(fn + "() argument 1 must be list, not " + Py.typeOf(o).name);
        }
        return (PyList) o;
    }

    private static PyException changed() {
        return Py.runtimeError("list changed size during iteration");
    }

    /// `a < b` (or `b < a` for a max-heap) on two slots, checking that the
    /// comparison left the list as long as it was.
    private static boolean less(PyList h, int n, Object a, Object b) {
        boolean r = Py.ltBool(a, b);
        if (h.size != n) {
            throw changed();
        }
        return r;
    }

    /// CPython's `siftdown`: moves the item at `pos` up towards `start`.
    private static void up(PyList h, int start, int pos, boolean max) {
        int n = h.size;
        Object item = h.items[pos];
        while (pos > start) {
            int parent = (pos - 1) >> 1;
            Object p = h.items[parent];
            if (!(max ? less(h, n, p, item) : less(h, n, item, p))) {
                break;
            }
            Object[] it = h.items;
            it[pos] = it[parent];
            pos = parent;
        }
        h.items[pos] = item;
    }

    /// CPython's `siftup`: the smaller child moves up to the leaf, then the
    /// displaced item sifts back.
    private static void down(PyList h, int pos, boolean max) {
        int end = h.size;
        int start = pos;
        Object item = h.items[pos];
        int child = 2 * pos + 1;
        while (child < end) {
            int right = child + 1;
            if (right < end) {
                Object[] it = h.items;
                boolean pickLeft = max ? less(h, end, it[right], it[child]) : less(h, end, it[child], it[right]);
                if (!pickLeft) {
                    child = right;
                }
            }
            Object[] it = h.items;
            it[pos] = it[child];
            pos = child;
            child = 2 * pos + 1;
        }
        h.items[pos] = item;
        up(h, start, pos, max);
    }

    private static Object push(String fn, Object heap, Object item, boolean max) {
        PyList h = list(fn, heap);
        h.append(item);
        h.objects();
        up(h, 0, h.size - 1, max);
        return Py.NONE;
    }

    private static Object pop(String fn, Object heap, boolean max) {
        PyList h = list(fn, heap);
        if (h.size == 0) {
            throw Py.indexError("index out of range");
        }
        Object[] it = h.objects();
        int last = --h.size;
        Object lastItem = it[last];
        it[last] = null;
        if (last == 0) {
            return lastItem;
        }
        Object top = it[0];
        it[0] = lastItem;
        down(h, 0, max);
        return top;
    }

    private static Object replace(String fn, Object heap, Object item, boolean max) {
        PyList h = list(fn, heap);
        if (h.size == 0) {
            throw Py.indexError("index out of range");
        }
        Object[] it = h.objects();
        Object top = it[0];
        it[0] = item;
        down(h, 0, max);
        return top;
    }

    private static Object pushPop(String fn, Object heap, Object item, boolean max) {
        PyList h = list(fn, heap);
        if (h.size == 0) {
            return item;
        }
        Object[] it = h.objects();
        Object top = it[0];
        int n = h.size;
        if (!(max ? less(h, n, item, top) : less(h, n, top, item))) {
            return item;
        }
        if (h.size == 0) {
            throw Py.indexError("index out of range");
        }
        top = h.items[0];
        h.items[0] = item;
        down(h, 0, max);
        return top;
    }

    private static Object heapify(String fn, Object heap, boolean max) {
        PyList h = list(fn, heap);
        h.objects();
        for (int i = h.size / 2 - 1; i >= 0; i--) {
            down(h, i, max);
        }
        return Py.NONE;
    }

    private static PyModule heapq() {
        PyModule m = new PyModule("_cn1heapq", new String[0]);
        m.setAttr("heappush", PyBuiltin.fixed2("heappush", (h, x) -> push("heappush", h, x, false)));
        m.setAttr("heappop", PyBuiltin.fixed1("heappop", h -> pop("heappop", h, false)));
        m.setAttr("heapreplace", PyBuiltin.fixed2("heapreplace", (h, x) -> replace("heapreplace", h, x, false)));
        m.setAttr("heappushpop", PyBuiltin.fixed2("heappushpop", (h, x) -> pushPop("heappushpop", h, x, false)));
        m.setAttr("heapify", PyBuiltin.fixed1("heapify", h -> heapify("heapify", h, false)));
        m.setAttr("heappush_max", PyBuiltin.fixed2("heappush_max", (h, x) -> push("heappush_max", h, x, true)));
        m.setAttr("heappop_max", PyBuiltin.fixed1("heappop_max", h -> pop("heappop_max", h, true)));
        m.setAttr("heapreplace_max", PyBuiltin.fixed2("heapreplace_max",
                (h, x) -> replace("heapreplace_max", h, x, true)));
        m.setAttr("heappushpop_max", PyBuiltin.fixed2("heappushpop_max",
                (h, x) -> pushPop("heappushpop_max", h, x, true)));
        m.setAttr("heapify_max", PyBuiltin.fixed1("heapify_max", h -> heapify("heapify_max", h, true)));
        return m;
    }

    /// `x < a[i]` (right) or `a[i] < x` (left) over any sequence; a list is
    /// read in place, bounds checked every step since `key` may shrink it.
    private static long search(Object a, Object x, long lo, long hi, Object key, boolean right) {
        if (lo < 0) {
            throw Py.valueError("lo must be non-negative");
        }
        PyObject k = key == Py.NONE ? null : Py.obj(key); //NOPMD CompareObjectsWithEquals - identity is meant
        if (hi == -1) {
            hi = a instanceof PyList ? ((PyList) a).size : Py.len(a);
        }
        PyList l = a instanceof PyList ? (PyList) a : null;
        if (l != null && k == null && x instanceof Long && l.ints != null && hi <= l.size) {
            // A list of small ints and an int probe: no comparison can run
            // user code, so the search reads the unboxed array directly.
            int[] v = l.ints;
            long t = ((Long) x).longValue();
            int ilo = (int) lo;
            int ihi = (int) hi;
            while (ilo < ihi) {
                int mid = (ilo + ihi) >>> 1;
                if (right ? t < v[mid] : v[mid] < t) {
                    if (right) {
                        ihi = mid;
                    } else {
                        ilo = mid + 1;
                    }
                } else if (right) {
                    ilo = mid + 1;
                } else {
                    ihi = mid;
                }
            }
            return ilo;
        }
        if (l != null && k == null && l.ints == null && l.floats == null) {
            return searchObjects(l, x, lo, hi, right);
        }
        while (lo < hi) {
            long mid = (lo + hi) >>> 1;
            Object v;
            if (l != null) {
                if (mid >= l.size) {
                    throw Py.indexError("list index out of range");
                }
                v = l.get((int) mid);
            } else {
                v = Py.getitem(a, mid);
            }
            if (k != null) {
                v = k.call1(v);
            }
            if (right ? Py.ltBool(x, v) : Py.ltBool(v, x)) {
                if (right) {
                    hi = mid;
                } else {
                    lo = mid + 1;
                }
            } else if (right) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /// [#search] over a list of objects with no key: the array and length
    /// stay in locals, and are read again only after a comparison that
    /// could have run Python code.
    private static long searchObjects(PyList l, Object x, long lo, long hi, boolean right) {
        Object[] it = l.items;
        int n = l.size;
        boolean xl = x instanceof Long;
        long t = xl ? ((Long) x).longValue() : 0;
        int ilo = (int) lo;
        int ihi = (int) Math.min(hi, Integer.MAX_VALUE);
        while (ilo < ihi) {
            int mid = (ilo + ihi) >>> 1;
            if (mid >= n) {
                throw Py.indexError("list index out of range");
            }
            Object v = it[mid];
            boolean before;
            if (xl && v instanceof Long) {
                long e = ((Long) v).longValue();
                before = right ? t < e : e < t;
            } else {
                before = right ? Py.ltBool(x, v) : Py.ltBool(v, x);
                if (l.ints != null || l.floats != null) {
                    l.objects();
                }
                it = l.items;
                n = l.size;
            }
            if (before == right) {
                ihi = mid;
            } else {
                ilo = mid + 1;
            }
        }
        return ilo;
    }

    private static final String[] NAMES = {"a", "x", "lo", "hi", "key"};

    private static PyBuiltin searcher(final String fn, final boolean right, final boolean insert) {
        return new PyBuiltin(fn, (a, kw) -> {
            Args args = new Args(fn, a, kw).check(4, NAMES);
            Object seq = args.required(0, "a");
            Object x = args.required(1, "x");
            long lo = Py.index(args.get(2, "lo", PyInt.of(0)));
            Object hiArg = args.get(3, "hi", Py.NONE);
            long hi = hiArg == Py.NONE ? -1 : Py.index(hiArg); //NOPMD CompareObjectsWithEquals - identity is meant
            Object key = args.get(-1, "key", Py.NONE);
            return run(seq, x, lo, hi, key, right, insert);
        }).with2((seq, x) -> run(seq, x, 0, -1, Py.NONE, right, insert));
    }

    private static Object run(Object seq, Object x, long lo, long hi, Object key, boolean right, boolean insert) {
        if (!insert) {
            return PyInt.of(search(seq, x, lo, hi, key, right));
        }
        Object probe = key == Py.NONE ? x : Py.obj(key).call1(x); //NOPMD CompareObjectsWithEquals - identity is meant
        long i = search(seq, probe, lo, hi, key, right);
        if (seq.getClass() == PyList.class) {
            ((PyList) seq).insert(i, x);
        } else {
            Py.callMethod(seq, "insert", PyInt.of(i), x);
        }
        return Py.NONE;
    }

    private static PyModule bisect() {
        PyModule m = new PyModule("_cn1bisect", new String[0]);
        m.setAttr("bisect_left", searcher("bisect_left", false, false));
        m.setAttr("bisect_right", searcher("bisect_right", true, false));
        m.setAttr("insort_left", searcher("insort_left", false, true));
        m.setAttr("insort_right", searcher("insort_right", true, true));
        return m;
    }
}
