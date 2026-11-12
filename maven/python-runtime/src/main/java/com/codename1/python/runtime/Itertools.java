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

/// `_cn1itertools`: the `itertools` module, which CPython implements in C.
/// It was Python here -- a generator per iterator, `groupby` a class whose
/// grouper re-entered `_fetch` through attribute sites per item -- and
/// pdfplumber's word extraction spent a quarter of its time in it. Each
/// callable is a type, as in CPython (`chain.from_iterable` is an attribute
/// of one), and each algorithm is the C module's: the same items consumed,
/// in the same order, with the same errors.
final class Itertools {

    private Itertools() {
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1itertools", new String[0]);
        m.setAttr("count", type("count", (t, a, k) -> {
            Object[] v = kwArgs("count", a, k, 2, new String[] {"start", "step"},
                    new Object[] {PyInt.of(0), PyInt.of(1)});
            return new Count(t, number(v[0]), number(v[1]));
        }));
        m.setAttr("cycle", type("cycle", (t, a, k) -> new Cycle(t, Py.iter(positional("cycle", a, k, 1, 1)[0]))));
        m.setAttr("repeat", type("repeat", (t, a, k) -> {
            Object[] v = kwArgs("repeat", a, k, 2, new String[] {"object", "times"}, new Object[] {null, Py.NONE});
            long times = v[1] == Py.NONE ? -1 : Math.max(0, Py.index(v[1])); //NOPMD CompareObjectsWithEquals - identity is meant
            return new Repeat(t, v[0], times);
        }));
        m.setAttr("accumulate", type("accumulate", (t, a, k) -> {
            Object[] v = kwArgs("accumulate", a, k, 2, new String[] {"iterable", "func", "initial"},
                    new Object[] {null, Py.NONE, Py.NONE});
            return new Accumulate(t, Py.iter(v[0]), v[1] == Py.NONE ? null : Py.obj(v[1]), v[2]); //NOPMD CompareObjectsWithEquals - identity is meant
        }));
        m.setAttr("batched", type("batched", (t, a, k) -> {
            Object[] v = kwArgs("batched", a, k, 2, new String[] {"iterable", "n", "strict"},
                    new Object[] {null, null, Py.FALSE});
            long n = Py.index(v[1]);
            if (n < 1) {
                throw Py.valueError("n must be at least one");
            }
            return new Batched(t, Py.iter(v[0]), (int) Math.min(n, Integer.MAX_VALUE - 8), Py.isTrue(v[2]));
        }));
        PyType chain = type("chain", (t, a, k) -> {
            noKeywords("chain", k);
            return new Chain(t, a, null);
        });
        chain.define("from_iterable", new PyClassMethod(PyBuiltin.fixed2("from_iterable",
                (cls, src) -> new Chain((PyType) cls, null, Py.iter(src)))));
        m.setAttr("chain", chain);
        m.setAttr("compress", type("compress", (t, a, k) -> {
            Object[] v = kwArgs("compress", a, k, 2, new String[] {"data", "selectors"}, new Object[] {null, null});
            return new Compress(t, Py.iter(v[0]), Py.iter(v[1]));
        }));
        m.setAttr("dropwhile", type("dropwhile", (t, a, k) -> {
            Object[] v = positional("dropwhile", a, k, 2, 2);
            return new Predicate(t, Py.obj(v[0]), Py.iter(v[1]), Predicate.DROP);
        }));
        m.setAttr("takewhile", type("takewhile", (t, a, k) -> {
            Object[] v = positional("takewhile", a, k, 2, 2);
            return new Predicate(t, Py.obj(v[0]), Py.iter(v[1]), Predicate.TAKE);
        }));
        m.setAttr("filterfalse", type("filterfalse", (t, a, k) -> {
            Object[] v = positional("filterfalse", a, k, 2, 2);
            return new Predicate(t, v[0] == Py.NONE ? null : Py.obj(v[0]), Py.iter(v[1]), Predicate.FALSE); //NOPMD CompareObjectsWithEquals - identity is meant
        }));
        final PyType grouper = type("_grouper", (t, a, k) -> {
            throw Py.typeError("cannot create '_grouper' instances");
        });
        m.setAttr("groupby", type("groupby", (t, a, k) -> {
            Object[] v = kwArgs("groupby", a, k, 2, new String[] {"iterable", "key"}, new Object[] {null, Py.NONE});
            return new GroupBy(t, grouper, Py.iter(v[0]), v[1] == Py.NONE ? null : Py.obj(v[1])); //NOPMD CompareObjectsWithEquals - identity is meant
        }));
        m.setAttr("islice", type("islice", Itertools::islice));
        m.setAttr("pairwise", type("pairwise", (t, a, k) -> new Pairwise(t, Py.iter(positional("pairwise", a, k, 1, 1)[0]))));
        m.setAttr("starmap", type("starmap", (t, a, k) -> {
            Object[] v = positional("starmap", a, k, 2, 2);
            return new Starmap(t, Py.obj(v[0]), Py.iter(v[1]));
        }));
        final PyType tee = type("_tee", (t, a, k) -> new Tee(t, new TeeData(Py.iter(positional("_tee", a, k, 1, 1)[0])), 0));
        tee.define("__copy__", PyBuiltin.method1(tee, "__copy__", s -> ((Tee) s).copy()));
        m.setAttr("_tee", tee);
        m.setAttr("tee", PyBuiltin.positional("tee", a -> {
            positional("tee", a, Py.NO_KW, 1, 2);
            long n = a.length > 1 ? Py.index(a[1]) : 2;
            if (n < 0) {
                throw Py.valueError("n must be >= 0");
            }
            Object[] r = new Object[(int) n];
            if (n == 0) {
                return PyTuple.EMPTY;
            }
            Tee first = a[0] instanceof Tee ? ((Tee) a[0]).copy() : new Tee(tee, new TeeData(Py.iter(a[0])), 0);
            r[0] = first;
            for (int i = 1; i < r.length; i++) {
                r[i] = first.copy();
            }
            return new PyTuple(r);
        }));
        m.setAttr("zip_longest", type("zip_longest", (t, a, k) -> {
            Object fill = Py.NONE;
            int npos = a.length - k.length;
            for (int i = 0; i < k.length; i++) {
                if (!"fillvalue".equals(k[i])) {
                    // CPython 3.14 names no argument here.
                    throw Py.typeError("zip_longest() got an unexpected keyword argument");
                }
                fill = a[npos + i];
            }
            PyObject[] its = new PyObject[npos];
            for (int i = 0; i < npos; i++) {
                its[i] = Py.iter(a[i]);
            }
            return new ZipLongest(t, its, fill);
        }));
        m.setAttr("product", type("product", (t, a, k) -> {
            long repeat = 1;
            int npos = a.length - k.length;
            if (k.length > 1) {
                throw Py.typeError("product() takes at most 1 keyword argument (" + k.length + " given)");
            }
            for (int i = 0; i < k.length; i++) {
                if (!"repeat".equals(k[i])) {
                    throw Py.typeError("product() got an unexpected keyword argument '" + k[i] + "'");
                }
                repeat = Py.index(a[npos + i]);
            }
            if (repeat < 0) {
                throw Py.valueError("repeat argument cannot be negative");
            }
            Object[][] given = new Object[npos][];
            for (int i = 0; i < npos; i++) {
                given[i] = Py.toArray(a[i]).clone();
            }
            Object[][] pools = new Object[(int) (npos * repeat)][];
            for (int i = 0; i < pools.length; i++) {
                pools[i] = given[i % Math.max(1, npos)];
            }
            return new Product(t, pools);
        }));
        m.setAttr("permutations", type("permutations", (t, a, k) -> {
            Object[] v = kwArgs("permutations", a, k, 2, new String[] {"iterable", "r"}, new Object[] {null, Py.NONE});
            Object[] pool = Py.toArray(v[0]).clone();
            long r = v[1] == Py.NONE ? pool.length : Py.index(v[1]); //NOPMD CompareObjectsWithEquals - identity is meant
            return new Combinatoric(t, pool, nonNegative(r), Combinatoric.PERMUTATIONS);
        }));
        m.setAttr("combinations", type("combinations", (t, a, k) -> {
            Object[] v = kwArgs("combinations", a, k, 2, new String[] {"iterable", "r"}, new Object[] {null, null});
            return new Combinatoric(t, Py.toArray(v[0]).clone(), nonNegative(Py.index(v[1])), Combinatoric.COMBINATIONS);
        }));
        m.setAttr("combinations_with_replacement", type("combinations_with_replacement", (t, a, k) -> {
            Object[] v = kwArgs("combinations_with_replacement", a, k, 2, new String[] {"iterable", "r"},
                    new Object[] {null, null});
            return new Combinatoric(t, Py.toArray(v[0]).clone(), nonNegative(Py.index(v[1])), Combinatoric.REPLACEMENT);
        }));
        return m;
    }

    // ---- types and arguments -------------------------------------------------

    /// An itertools type: instances iterate themselves through `next()`,
    /// and `__iter__`/`__next__` are there for code that names them.
    private static PyType type(String name, PyType.Factory f) {
        PyType t = new PyType(name, new PyType[] {Py.OBJECT_TYPE}, f);
        t.define("__module__", "itertools");
        t.define("__iter__", PyBuiltin.method1(t, "__iter__", s -> s));
        t.define("__next__", PyBuiltin.method1(t, "__next__", s -> {
            Object v = ((PyObject) s).next();
            if (v == null) {
                throw Py.raise(Py.STOP_ITERATION);
            }
            return v;
        }));
        return t;
    }

    private static void noKeywords(String fn, String[] kw) {
        if (kw.length != 0) {
            throw Py.typeError(fn + "() takes no keyword arguments");
        }
    }

    /// Exactly `min`..`max` positional arguments and no keywords.
    private static Object[] positional(String fn, Object[] a, String[] kw, int min, int max) {
        noKeywords(fn, kw);
        if (a.length < min || a.length > max) {
            throw Py.typeError(fn + " expected " + (min == max ? "" : a.length < min ? "at least " : "at most ")
                    + (a.length < min ? min : max) + ((a.length < min ? min : max) == 1 ? " argument" : " arguments")
                    + ", got " + a.length);
        }
        return a;
    }

    /// Up to `maxPos` positional arguments, then keywords by name; a null
    /// default makes the parameter required. Names past `maxPos` are
    /// keyword only.
    private static Object[] kwArgs(String fn, Object[] a, String[] kw, int maxPos, String[] names, Object[] defaults) {
        int npos = a.length - kw.length;
        if (npos > maxPos && names.length > maxPos) {
            // A function with keyword-only parameters words it as CPython's
            // argument clinic does: batched's n is required, accumulate's
            // func is not.
            throw Py.typeError(fn + "() takes " + (defaults[maxPos - 1] == null ? "exactly " : "at most ") + maxPos
                    + " positional arguments (" + npos + " given)");
        }
        Args args = new Args(fn, a, kw).check(maxPos, names);
        Object[] r = new Object[names.length];
        for (int i = 0; i < names.length; i++) {
            int pos = i < maxPos ? i : -1;
            r[i] = defaults[i] == null ? args.required(pos, names[i]) : args.get(pos, names[i], defaults[i]);
        }
        return r;
    }

    private static Object number(Object o) {
        if (o instanceof String || o instanceof PyBytes || o instanceof PyList || o instanceof PyTuple
                || o instanceof PyDict || o == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            throw Py.typeError("a number is required");
        }
        return o;
    }

    private static int nonNegative(long r) {
        if (r < 0) {
            throw Py.valueError("r must be non-negative");
        }
        return (int) Math.min(r, Integer.MAX_VALUE);
    }

    /// A tuple of `src[idx[0..n)]`.
    private static PyTuple pick(Object[] src, int[] idx, int n) {
        Object[] r = new Object[n];
        for (int i = 0; i < n; i++) {
            r[i] = src[idx[i]];
        }
        return new PyTuple(r);
    }

    /// The base of every iterator here: its type, and itself as its iterator.
    private abstract static class It extends PyObject {
        private final PyType t;

        It(PyType t) {
            this.t = t;
        }

        @Override
        public final PyType type() {
            return t;
        }

        @Override
        public final Object iter() {
            return this;
        }
    }

    // ---- infinite ------------------------------------------------------------

    private static final class Count extends It {
        private Object n;
        private final Object step;

        Count(PyType t, Object start, Object step) {
            super(t);
            this.n = start;
            this.step = step;
        }

        @Override
        public Object next() {
            Object r = n;
            n = Py.add(r, step);
            return r;
        }

        @Override
        public String repr() {
            boolean unit = step instanceof Long && ((Long) step).longValue() == 1;
            return "count(" + Py.toRepr(n) + (unit ? "" : ", " + Py.toRepr(step)) + ")";
        }
    }

    private static final class Cycle extends It {
        private PyObject it;
        private Object[] saved = new Object[8];
        private int size;
        private int pos;

        Cycle(PyType t, PyObject it) {
            super(t);
            this.it = it;
        }

        @Override
        public Object next() {
            if (it != null) {
                Object v = it.next();
                if (v != null) {
                    if (size == saved.length) {
                        Object[] g = new Object[size * 2];
                        System.arraycopy(saved, 0, g, 0, size);
                        saved = g;
                    }
                    saved[size++] = v;
                    return v;
                }
                it = null;
            }
            if (size == 0) {
                return null;
            }
            Object v = saved[pos];
            pos = pos + 1 == size ? 0 : pos + 1;
            return v;
        }
    }

    private static final class Repeat extends It {
        private final Object value;
        /// Remaining, or -1 for forever.
        private long times;

        Repeat(PyType t, Object value, long times) {
            super(t);
            this.value = value;
            this.times = times;
        }

        @Override
        public Object next() {
            if (times < 0) {
                return value;
            }
            if (times == 0) {
                return null;
            }
            times--;
            return value;
        }

        @Override
        public String repr() {
            return "repeat(" + Py.toRepr(value) + (times < 0 ? "" : ", " + times) + ")";
        }
    }

    // ---- terminating on the shortest input ----------------------------------

    private static final class Accumulate extends It {
        private final PyObject it;
        private final PyObject func;
        private Object initial;
        private Object total;

        Accumulate(PyType t, PyObject it, PyObject func, Object initial) {
            super(t);
            this.it = it;
            this.func = func;
            this.initial = initial;
        }

        @Override
        public Object next() {
            if (initial != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                total = initial;
                initial = Py.NONE;
                return total;
            }
            Object x = it.next();
            if (x == null) {
                return null;
            }
            Object s = total;
            total = s == null ? x : func == null ? Py.add(s, x) : func.call2(s, x);
            return total;
        }
    }

    private static final class Batched extends It {
        private PyObject it;
        private final int n;
        private final boolean strict;

        Batched(PyType t, PyObject it, int n, boolean strict) {
            super(t);
            this.it = it;
            this.n = n;
            this.strict = strict;
        }

        @Override
        public Object next() {
            PyObject src = it;
            if (src == null) {
                return null;
            }
            Object[] b = new Object[Math.min(n, 64)];
            int k = 0;
            while (k < n) {
                Object v = src.next();
                if (v == null) {
                    it = null;
                    break;
                }
                if (k == b.length) {
                    Object[] g = new Object[(int) Math.min((long) n, (long) k * 2)];
                    System.arraycopy(b, 0, g, 0, k);
                    b = g;
                }
                b[k++] = v;
            }
            if (k == 0) {
                return null;
            }
            if (strict && k != n) {
                throw Py.valueError("batched(): incomplete batch");
            }
            if (k != b.length) {
                Object[] e = new Object[k];
                System.arraycopy(b, 0, e, 0, k);
                b = e;
            }
            return new PyTuple(b);
        }
    }

    private static final class Chain extends It {
        private Object[] sources;
        private PyObject sourceIt;
        private int i;
        private PyObject cur;

        Chain(PyType t, Object[] sources, PyObject sourceIt) {
            super(t);
            this.sources = sources;
            this.sourceIt = sourceIt;
        }

        @Override
        public Object next() {
            while (true) {
                PyObject c = cur;
                if (c != null) {
                    Object v = c.next();
                    if (v != null) {
                        return v;
                    }
                    cur = null;
                }
                if (sources != null) {
                    if (i >= sources.length) {
                        sources = null;
                        return null;
                    }
                    cur = Py.iter(sources[i++]);
                } else if (sourceIt != null) {
                    Object s = sourceIt.next();
                    if (s == null) {
                        sourceIt = null;
                        return null;
                    }
                    cur = Py.iter(s);
                } else {
                    return null;
                }
            }
        }
    }

    private static final class Compress extends It {
        private final PyObject data;
        private final PyObject selectors;

        Compress(PyType t, PyObject data, PyObject selectors) {
            super(t);
            this.data = data;
            this.selectors = selectors;
        }

        @Override
        public Object next() {
            while (true) {
                Object d = data.next();
                if (d == null) {
                    return null;
                }
                Object s = selectors.next();
                if (s == null) {
                    return null;
                }
                if (Py.isTrue(s)) {
                    return d;
                }
            }
        }
    }

    /// dropwhile, takewhile and filterfalse: one predicate over one input.
    private static final class Predicate extends It {
        static final int DROP = 0;
        static final int TAKE = 1;
        static final int FALSE = 2;
        private final PyObject fn;
        private final PyObject it;
        private final int kind;
        /// dropwhile: still dropping. takewhile: stopped.
        private boolean flag;

        Predicate(PyType t, PyObject fn, PyObject it, int kind) {
            super(t);
            this.fn = fn;
            this.it = it;
            this.kind = kind;
            this.flag = kind == DROP;
        }

        @Override
        public Object next() {
            if (kind == TAKE && flag) {
                return null;
            }
            while (true) {
                Object v = it.next();
                if (v == null) {
                    return null;
                }
                if (kind == DROP) {
                    if (!flag || !Py.isTrue(fn.call1(v))) {
                        flag = false;
                        return v;
                    }
                } else if (kind == TAKE) {
                    if (Py.isTrue(fn.call1(v))) {
                        return v;
                    }
                    flag = true;
                    return null;
                } else if (!Py.isTrue(fn == null ? v : fn.call1(v))) {
                    return v;
                }
            }
        }
    }

    /// groupby, as CPython's: the key of the current item and the item, a
    /// target key, and the one grouper still allowed to read.
    private static final class GroupBy extends It {
        private final PyType grouperType;
        private final PyObject it;
        private final PyObject key;
        Object currKey;
        Object currValue;
        private Object tgtKey;
        Grouper currGrouper;

        GroupBy(PyType t, PyType grouperType, PyObject it, PyObject key) {
            super(t);
            this.grouperType = grouperType;
            this.it = it;
            this.key = key;
        }

        /// Reads the next item and its key; false at the end.
        boolean step() {
            Object v = it.next();
            if (v == null) {
                return false;
            }
            Object k = key == null ? v : key.call1(v);
            currValue = v;
            currKey = k;
            return true;
        }

        @Override
        public Object next() {
            currGrouper = null;
            while (true) {
                if (currKey != null) {
                    if (tgtKey == null || !Py.eqBool(tgtKey, currKey)) {
                        break;
                    }
                }
                if (!step()) {
                    return null;
                }
            }
            tgtKey = currKey;
            Grouper g = new Grouper(grouperType, this, tgtKey);
            currGrouper = g;
            return new PyTuple(new Object[] {currKey, g});
        }
    }

    private static final class Grouper extends It {
        private final GroupBy parent;
        private final Object tgtKey;

        Grouper(PyType t, GroupBy parent, Object tgtKey) {
            super(t);
            this.parent = parent;
            this.tgtKey = tgtKey;
        }

        @Override
        public Object next() {
            GroupBy p = parent;
            if (p.currGrouper != this) { //NOPMD CompareObjectsWithEquals - identity is meant
                return null;
            }
            if (p.currValue == null && !p.step()) {
                return null;
            }
            if (!Py.eqBool(tgtKey, p.currKey)) {
                return null;
            }
            Object r = p.currValue;
            p.currValue = null;
            p.currKey = null;
            return r;
        }
    }

    private static Object islice(PyType t, Object[] a, String[] kw) {
        positional("islice", a, kw, 2, 4);
        long start = 0;
        long stop;
        long step = 1;
        if (a.length == 2) {
            stop = sliceIndex(a[1], -1, "Stop argument for islice() must be None or an integer: 0 <= x <= sys.maxsize.");
        } else {
            String m = "Indices for islice() must be None or an integer: 0 <= x <= sys.maxsize.";
            start = sliceIndex(a[1], 0, m);
            stop = sliceIndex(a[2], -1, m);
            if (a.length == 4) {
                step = a[3] == Py.NONE ? 1 : Py.typeOf(a[3]).lookup("__index__") == null ? 0 : Py.index(a[3]); //NOPMD CompareObjectsWithEquals - identity is meant
                if (step < 1) {
                    throw Py.valueError("Step for islice() must be a positive integer or None.");
                }
            }
        }
        return new Islice(t, Py.iter(a[0]), start, stop, step);
    }

    private static long sliceIndex(Object v, long none, String message) {
        if (v == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            return none;
        }
        if (Py.typeOf(v).lookup("__index__") == null) {
            throw Py.valueError(message);
        }
        long r = Py.index(v);
        if (r < 0) {
            throw Py.valueError(message);
        }
        return r;
    }

    private static final class Islice extends It {
        private PyObject it;
        private long next;
        private final long stop;
        private final long step;
        private long cnt;

        Islice(PyType t, PyObject it, long start, long stop, long step) {
            super(t);
            this.it = it;
            this.next = start;
            this.stop = stop;
            this.step = step;
        }

        @Override
        public Object next() {
            PyObject src = it;
            if (src == null) {
                return null;
            }
            while (cnt < next) {
                if (src.next() == null) {
                    it = null;
                    return null;
                }
                cnt++;
            }
            if (stop != -1 && cnt >= stop) {
                it = null;
                return null;
            }
            Object v = src.next();
            if (v == null) {
                it = null;
                return null;
            }
            cnt++;
            long old = next;
            next += step;
            if (next < old || stop != -1 && next > stop) {
                next = stop;
            }
            return v;
        }
    }

    private static final class Pairwise extends It {
        private PyObject it;
        private Object old;

        Pairwise(PyType t, PyObject it) {
            super(t);
            this.it = it;
        }

        @Override
        public Object next() {
            PyObject src = it;
            if (src == null) {
                return null;
            }
            if (old == null) {
                old = src.next();
                if (old == null) {
                    it = null;
                    return null;
                }
            }
            Object n = src.next();
            if (n == null) {
                it = null;
                old = null;
                return null;
            }
            PyTuple r = new PyTuple(new Object[] {old, n});
            old = n;
            return r;
        }
    }

    private static final class Starmap extends It {
        private final PyObject fn;
        private final PyObject it;

        Starmap(PyType t, PyObject fn, PyObject it) {
            super(t);
            this.fn = fn;
            this.it = it;
        }

        @Override
        public Object next() {
            Object row = it.next();
            if (row == null) {
                return null;
            }
            Object[] a = Py.toArray(row);
            switch (a.length) {
                case 0:
                    return fn.call0();
                case 1:
                    return fn.call1(a[0]);
                case 2:
                    return fn.call2(a[0], a[1]);
                case 3:
                    return fn.call3(a[0], a[1], a[2]);
                default:
                    return fn.call(a.clone(), Py.NO_KW);
            }
        }
    }

    /// The values a set of tee iterators share: blocks of what was read,
    /// each linked to the next, dropped once every iterator is past it.
    private static final class TeeData {
        static final int CELLS = 57;
        final PyObject it;
        final Object[] values = new Object[CELLS];
        int n;
        TeeData next;
        boolean running;

        TeeData(PyObject it) {
            this.it = it;
        }

        TeeData nextLink() {
            if (next == null) {
                next = new TeeData(it);
            }
            return next;
        }

        Object get(int i) {
            if (i < n) {
                return values[i];
            }
            if (running) {
                throw Py.runtimeError("cannot re-enter the tee iterator");
            }
            running = true;
            Object v;
            try {
                v = it.next();
            } finally {
                running = false;
            }
            if (v == null) {
                return null;
            }
            values[n++] = v;
            return v;
        }
    }

    private static final class Tee extends It {
        private TeeData data;
        private int index;

        Tee(PyType t, TeeData data, int index) {
            super(t);
            this.data = data;
            this.index = index;
        }

        Tee copy() {
            return new Tee(type(), data, index);
        }

        @Override
        public Object next() {
            if (index >= TeeData.CELLS) {
                data = data.nextLink();
                index = 0;
            }
            Object v = data.get(index);
            if (v != null) {
                index++;
            }
            return v;
        }
    }

    private static final class ZipLongest extends It {
        private final PyObject[] its;
        private final Object fill;
        private int active;

        ZipLongest(PyType t, PyObject[] its, Object fill) {
            super(t);
            this.its = its;
            this.fill = fill;
            this.active = its.length;
        }

        @Override
        public Object next() {
            if (active == 0) {
                return null;
            }
            Object[] r = new Object[its.length];
            for (int i = 0; i < its.length; i++) {
                PyObject s = its[i];
                Object v;
                if (s == null) {
                    v = fill;
                } else {
                    v = s.next();
                    if (v == null) {
                        active--;
                        if (active == 0) {
                            return null;
                        }
                        its[i] = null;
                        v = fill;
                    }
                }
                r[i] = v;
            }
            return new PyTuple(r);
        }
    }

    // ---- combinatoric --------------------------------------------------------

    private static final class Product extends It {
        private final Object[][] pools;
        private final int[] indices;
        private boolean started;
        private boolean stopped;

        Product(PyType t, Object[][] pools) {
            super(t);
            this.pools = pools;
            this.indices = new int[pools.length];
        }

        @Override
        public Object next() {
            if (stopped) {
                return null;
            }
            int n = pools.length;
            if (!started) {
                started = true;
                for (int i = 0; i < n; i++) {
                    if (pools[i].length == 0) {
                        stopped = true;
                        return null;
                    }
                }
            } else {
                int i = n - 1;
                while (i >= 0) {
                    indices[i]++;
                    if (indices[i] < pools[i].length) {
                        break;
                    }
                    indices[i] = 0;
                    i--;
                }
                if (i < 0) {
                    stopped = true;
                    return null;
                }
            }
            Object[] r = new Object[n];
            for (int i = 0; i < n; i++) {
                r[i] = pools[i][indices[i]];
            }
            return new PyTuple(r);
        }
    }

    /// permutations, combinations and combinations_with_replacement, each the
    /// documented equivalent's index walk.
    private static final class Combinatoric extends It {
        static final int PERMUTATIONS = 0;
        static final int COMBINATIONS = 1;
        static final int REPLACEMENT = 2;
        private final Object[] pool;
        private final int r;
        private final int kind;
        private final int[] indices;
        private final int[] cycles;
        private boolean started;
        private boolean stopped;

        Combinatoric(PyType t, Object[] pool, int r, int kind) {
            super(t);
            this.pool = pool;
            this.r = r;
            this.kind = kind;
            int n = pool.length;
            if (kind == PERMUTATIONS) {
                indices = new int[n];
                cycles = new int[Math.min(r, n)];
                for (int i = 0; i < n; i++) {
                    indices[i] = i;
                }
                for (int i = 0; i < cycles.length; i++) {
                    cycles[i] = n - i;
                }
                stopped = r > n;
            } else if (kind == COMBINATIONS) {
                indices = new int[Math.min(r, n)];
                cycles = null;
                for (int i = 0; i < indices.length; i++) {
                    indices[i] = i;
                }
                stopped = r > n;
            } else {
                indices = new int[r];
                cycles = null;
                stopped = n == 0 && r > 0;
            }
        }

        @Override
        public Object next() {
            if (stopped) {
                return null;
            }
            if (!started) {
                started = true;
                return pick(pool, indices, r);
            }
            int n = pool.length;
            if (kind == PERMUTATIONS) {
                if (n == 0) {
                    stopped = true;
                    return null;
                }
                for (int i = r - 1; i >= 0; i--) {
                    cycles[i]--;
                    if (cycles[i] == 0) {
                        int first = indices[i];
                        System.arraycopy(indices, i + 1, indices, i, n - i - 1);
                        indices[n - 1] = first;
                        cycles[i] = n - i;
                    } else {
                        int j = cycles[i];
                        int tmp = indices[i];
                        indices[i] = indices[n - j];
                        indices[n - j] = tmp;
                        return pick(pool, indices, r);
                    }
                }
                stopped = true;
                return null;
            }
            int i = r - 1;
            if (kind == COMBINATIONS) {
                while (i >= 0 && indices[i] == i + n - r) {
                    i--;
                }
                if (i < 0) {
                    stopped = true;
                    return null;
                }
                indices[i]++;
                for (int j = i + 1; j < r; j++) {
                    indices[j] = indices[j - 1] + 1;
                }
                return pick(pool, indices, r);
            }
            while (i >= 0 && indices[i] == n - 1) {
                i--;
            }
            if (i < 0) {
                stopped = true;
                return null;
            }
            int v = indices[i] + 1;
            for (int j = i; j < r; j++) {
                indices[j] = v;
            }
            return pick(pool, indices, r);
        }
    }
}
