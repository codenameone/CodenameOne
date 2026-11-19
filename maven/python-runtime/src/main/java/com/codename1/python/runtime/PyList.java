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

/// Python's `list`, a growable array.
public final class PyList extends PyObject {
    // The storage is public for the typed code the transpiler generates
    // for a closed program: ParparVM does not inline a call into another
    // class, so an element access through a method here cost several times
    // the access itself. Nothing else may touch these fields.
    public Object[] items;
    public int size;

    /// Shared by every list that has never held an item; the first append
    /// replaces it, so an empty list costs one object.
    private static final Object[] NO_ITEMS = new Object[0];

    /// The items of a list that has only ever held floats, unboxed; `items`
    /// is then unused. A float region (generated code) reads and writes
    /// them without allocating. Anything else turns the list back into
    /// objects first ([#objects()]), once.
    public double[] floats;

    /// The items of a list that has only ever held ints that fit 32 bits,
    /// unboxed, as `floats` is for floats; never both. Four bytes an item, as
    /// a Java `int[]` takes; a wider int turns the list into objects, where
    /// a Long that fits 61 bits is an immediate under ParparVM, so reading
    /// one back allocates nothing either.
    public int[] ints;

    /// Set on a list Py.recycleSlice or Py.recycleList made for a local the
    /// transpiler proved never shares it; only such a list is reused. A list
    /// a user `__getitem__` returned may be held elsewhere and never is.
    boolean owned;

    public PyList() {
        items = NO_ITEMS;
    }

    /// A list of `items`, unboxed when every one of them is a float.
    static PyList of(Object[] items) {
        int n = items.length;
        if (n == 0) {
            return new PyList(items);
        }
        for (int i = 0; i < n; i++) {
            if (!(items[i] instanceof Double)) {
                return new PyList(items);
            }
        }
        PyList r = new PyList();
        double[] f = new double[n];
        for (int i = 0; i < n; i++) {
            f[i] = ((Double) items[i]).doubleValue();
        }
        r.floats = f;
        r.size = n;
        return r;
    }

    /// The items as objects, boxing an unboxed list for good.
    Object[] objects() {
        int[] l = ints;
        if (l != null) {
            Object[] o = new Object[Math.max(4, l.length)];
            for (int i = 0; i < size; i++) {
                o[i] = Long.valueOf(l[i]);
            }
            items = o;
            ints = null;
            return o;
        }
        double[] f = floats;
        if (f != null) {
            Object[] o = new Object[Math.max(4, f.length)];
            for (int i = 0; i < size; i++) {
                o[i] = Double.valueOf(f[i]);
            }
            items = o;
            floats = null;
        }
        return items;
    }

    /// Whether item `i` is a float a region can read unboxed.
    boolean isFloatAt(int i) {
        return floats != null || ints == null && items[i] instanceof Double;
    }

    double floatAt(int i) {
        return floats != null ? floats[i] : ((Double) items[i]).doubleValue();
    }

    /// Stores a float a region computed; an unboxed list keeps it unboxed.
    void setFloat(int i, double v) {
        if (floats != null) {
            floats[i] = v;
        } else {
            objects()[i] = Double.valueOf(v);
        }
    }

    /// Takes ownership of `items`.
    public PyList(Object[] items) {
        this.items = items;
        this.size = items.length;
    }

    public PyList(int capacity) {
        items = new Object[Math.max(4, capacity)];
    }

    @Override
    public PyType type() {
        return Py.LIST_TYPE;
    }

    public int size() {
        return size;
    }

    public Object get(int i) {
        int[] l = ints;
        if (l != null) {
            return Long.valueOf(l[i]);
        }
        return floats != null ? Double.valueOf(floats[i]) : items[i];
    }

    public void append(Object v) {
        if (ints != null || floats != null || size == 0 && items == NO_ITEMS) {
            if (v instanceof Double && ints == null) {
                appendFloat(((Double) v).doubleValue());
                return;
            }
            if (v instanceof Long && floats == null && fits(((Long) v).longValue())) {
                appendInt((int) ((Long) v).longValue());
                return;
            }
            objects();
        }
        appendObject(v);
    }

    /// Whether `v` may go into `ints`.
    static boolean fits(long v) {
        return (int) v == v;
    }

    private void appendInt(int v) {
        int[] l = ints;
        if (l == null) {
            l = new int[4];
            ints = l;
        } else if (size == l.length) {
            int[] n = new int[size * 2 + 4];
            System.arraycopy(l, 0, n, 0, size);
            ints = n;
            l = n;
        }
        l[size++] = v;
    }

    private void appendObject(Object v) {
        if (size == items.length) {
            // 8 first, as CPython's list starts an unsized fill: a list
            // built from a filter or a generator of a dozen items then
            // allocates twice rather than three times.
            Object[] n = new Object[size + (size >> 1) + 8];
            System.arraycopy(items, 0, n, 0, size);
            items = n;
        }
        items[size++] = v;
    }

    private void appendFloat(double v) {
        double[] f = floats;
        if (f == null) {
            f = new double[4];
            floats = f;
        } else if (size == f.length) {
            double[] n = new double[size * 2 + 4];
            System.arraycopy(f, 0, n, 0, size);
            floats = n;
            f = n;
        }
        f[size++] = v;
    }

    public void extend(Object iterable) {
        if (iterable instanceof PyList) {
            PyList o = (PyList) iterable;
            int n = o.size;
            if (o.ints != null && intsOrEmpty()) {
                // One copy, into an array of the exact size when this list
                // was empty: no element by element growth.
                int[] l = intRoom(n);
                System.arraycopy(o.ints, 0, l, size, n);
                size += n;
                return;
            }
            if (o.ints == null && o.floats == null && ints == null && floats == null) {
                // Objects into objects: one copy, as CPython's list_extend
                // does, not an append (and a storage check) per element.
                // The source array and length are read first, so
                // x.extend(x) copies what x held before.
                Object[] src = o.items;
                Object[] dst = items;
                if (size + n > dst.length) {
                    Object[] g = new Object[Math.max(4, size == 0 ? n : size + n + (size >> 1))];
                    System.arraycopy(dst, 0, g, 0, size);
                    items = g;
                    dst = g;
                }
                System.arraycopy(src, 0, dst, size, n);
                size += n;
                return;
            }
            for (int i = 0; i < n; i++) {
                append(o.get(i));
            }
            return;
        }
        Object it = Py.iter(iterable);
        if (it instanceof PyGenerator) {
            // A generator drained under ONE handler, as a transpiled for
            // loop drains it: next() enters a try per value, a setjmp on
            // ParparVM.
            PyGenerator g = (PyGenerator) it;
            try {
                for (Object v = PyGenerator.loopNext(g); v != null; v = PyGenerator.loopNext(g)) {
                    append(v);
                }
            } catch (Throwable e) {
                throw PyGenerator.loopEscape(g, e);
            }
            return;
        }
        if (it instanceof Builtins.FilterIter) {
            // Both classes are final: next() is a direct call, not a
            // virtual one through Py.next per item.
            Builtins.FilterIter f = (Builtins.FilterIter) it;
            for (Object v = f.next(); v != null; v = f.next()) {
                append(v);
            }
            return;
        }
        if (it instanceof Builtins.MapIter) {
            Builtins.MapIter m = (Builtins.MapIter) it;
            for (Object v = m.next(); v != null; v = m.next()) {
                append(v);
            }
            return;
        }
        for (Object v = Py.next(it); v != null; v = Py.next(it)) {
            append(v);
        }
    }

    /// Whether ints may go into the unboxed `ints` storage.
    private boolean intsOrEmpty() {
        return ints != null || floats == null && size == 0 && items == NO_ITEMS; //NOPMD CompareObjectsWithEquals
    }

    /// `ints`, with room for `n` more after `size`.
    private int[] intRoom(int n) {
        int[] l = ints;
        if (l == null || l.length - size < n) {
            int[] g = new int[l == null ? n : Math.max(size + n, size * 2 + 4)];
            if (l != null) {
                System.arraycopy(l, 0, g, 0, size);
            }
            ints = g;
            l = g;
        }
        return l;
    }

    /// `extend([v] * n)`, without the intermediate list.
    public void extendRepeat(Object v, long n) {
        if (n <= 0) {
            return;
        }
        if (v instanceof Long && n <= Integer.MAX_VALUE - size && intsOrEmpty() && fits(((Long) v).longValue())) {
            int x = (int) ((Long) v).longValue();
            int[] l = intRoom((int) n);
            int end = size + (int) n;
            for (int i = size; i < end; i++) {
                l[i] = x;
            }
            size = end;
            return;
        }
        extend(new PyList(new Object[] {v}).mul(Long.valueOf(n)));
    }

    public void insert(long index, Object v) {
        objects();
        if (index < 0) {
            index = Math.max(0, index + size);
        }
        int i = (int) Math.min(index, size);
        appendObject(v);
        System.arraycopy(items, i, items, i + 1, size - 1 - i);
        items[i] = v;
    }

    public Object pop(long index) {
        if (ints != null && size > 0 && (index == -1 || index == size - 1)) {
            return Long.valueOf(ints[--size]);
        }
        objects();
        if (size == 0) {
            throw Py.indexError("pop from empty list");
        }
        if (index < 0) {
            index += size;
        }
        if (index < 0 || index >= size) {
            throw Py.indexError("pop index out of range");
        }
        int i = (int) index;
        Object v = items[i];
        System.arraycopy(items, i + 1, items, i, size - i - 1);
        items[--size] = null;
        return v;
    }

    public Object[] toArray() {
        Object[] r = new Object[size];
        if (ints != null) {
            for (int i = 0; i < size; i++) {
                r[i] = Long.valueOf(ints[i]);
            }
        } else if (floats != null) {
            for (int i = 0; i < size; i++) {
                r[i] = Double.valueOf(floats[i]);
            }
        } else {
            System.arraycopy(items, 0, r, 0, size);
        }
        return r;
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
        if (Py.enterRepr(this)) {
            return "[...]";
        }
        try {
            return Py.joinRepr("[", floats != null || ints != null ? toArray() : items, size, "]");
        } finally {
            Py.exitRepr(this);
        }
    }

    @Override
    public long hash() {
        throw Py.typeError("unhashable type: 'list'");
    }

    @Override
    public Object eq(Object o) {
        if (!(o instanceof PyList)) {
            return null;
        }
        PyList b = (PyList) o;
        return Py.bool(Py.seqEquals(objects(), size, b.objects(), b.size));
    }

    @Override
    public Object lt(Object o) {
        return o instanceof PyList ? Py.bool(cmp(o) < 0) : null;
    }

    @Override
    public Object le(Object o) {
        return o instanceof PyList ? Py.bool(cmp(o) <= 0) : null;
    }

    @Override
    public Object gt(Object o) {
        return o instanceof PyList ? Py.bool(cmp(o) > 0) : null;
    }

    @Override
    public Object ge(Object o) {
        return o instanceof PyList ? Py.bool(cmp(o) >= 0) : null;
    }

    private int cmp(Object o) {
        PyList b = (PyList) o;
        return Py.seqCompare(objects(), size, b.objects(), b.size);
    }

    @Override
    public Object add(Object o) {
        if (!(o instanceof PyList)) {
            return null;
        }
        PyList b = (PyList) o;
        Object[] r = new Object[size + b.size];
        System.arraycopy(objects(), 0, r, 0, size);
        System.arraycopy(b.objects(), 0, r, size, b.size);
        return new PyList(r);
    }

    @Override
    public Object iadd(Object o) {
        extend(o);
        return this;
    }

    @Override
    public Object inplace(String name, Object o) {
        objects();
        if ("__imul__".equals(name) && Py.isInt(o)) {
            Object[] items = toArray();
            long n = Py.asLong(o);
            clear();
            for (long i = 0; i < n; i++) {
                for (Object item : items) {
                    append(item);
                }
            }
            return this;
        }
        return null;
    }

    @Override
    public Object mul(Object o) {
        if (!(Py.isInt(o))) {
            return null;
        }
        long n = Math.max(0, Py.asLong(o));
        if (ints == null && floats == null && size > 0) {
            boolean all = true;
            for (int i = 0; i < size && all; i++) {
                all = items[i] instanceof Long && fits(((Long) items[i]).longValue());
            }
            if (all) {
                // `[0] * n` is a list of ints: unboxed.
                int[] one = new int[size];
                for (int i = 0; i < size; i++) {
                    one[i] = (int) ((Long) items[i]).longValue();
                }
                return repeatInts(one, n);
            }
        }
        if (ints != null) {
            return repeatInts(ints, n);
        }
        if (floats != null) {
            // `[0.0] * n` stays unboxed.
            double[] f = new double[Math.max(4, (int) (size * n))];
            for (int i = 0; i < n; i++) {
                System.arraycopy(floats, 0, f, i * size, size);
            }
            PyList r = new PyList();
            r.floats = f;
            r.size = (int) (size * n);
            return r;
        }
        Object[] r = new Object[(int) (size * n)];
        for (int i = 0; i < n; i++) {
            System.arraycopy(items, 0, r, i * size, size);
        }
        return new PyList(r);
    }

    private PyList repeatInts(int[] one, long n) {
        int k = size;
        int[] l = new int[Math.max(4, (int) (k * n))];
        for (int i = 0; i < n; i++) {
            System.arraycopy(one, 0, l, i * k, k);
        }
        PyList r = new PyList();
        r.ints = l;
        r.size = (int) (k * n);
        return r;
    }

    @Override
    public Object rmul(Object o) {
        return mul(o);
    }

    private int checkIndex(Object key) {
        long i = Py.subscript(key);
        if (i < 0) {
            i += size;
        }
        if (i < 0 || i >= size) {
            throw Py.indexError("list index out of range");
        }
        return (int) i;
    }

    @Override
    public Object getItem(Object key) {
        if (key instanceof Long) {
            long i = ((Long) key).longValue();
            if (i >= 0 && i < size) {
                return get((int) i);
            }
        }
        if (floats != null || ints != null) {
            if (!(key instanceof PySlice)) {
                return get(checkIndex(key));
            }
            objects();
        }
        if (key instanceof PySlice) {
            int[] r = ((PySlice) key).indices(size);
            int n = PySlice.length(r);
            Object[] out = new Object[n];
            if (r[2] == 1) {
                // A plain slice is one block copy, not a checked store per element.
                System.arraycopy(items, r[0], out, 0, n);
                return new PyList(out);
            }
            for (int i = 0, j = r[0]; i < n; i++, j += r[2]) {
                out[i] = items[j];
            }
            return new PyList(out);
        }
        return items[checkIndex(key)];
    }

    /// `self[start:stop:step]` without a slice object; NONE for a missing bound.
    public PyList getSlice(Object start, Object stop, Object step) {
        if (ints != null) {
            int st = step == Py.NONE ? 1 : PySlice.step(step); //NOPMD CompareObjectsWithEquals - identity is meant
            int lo = PySlice.lower(start, stop, st, size);
            int hi = PySlice.upper(stop, st, size);
            int n = PySlice.count(lo, hi, st);
            int[] out = new int[Math.max(4, n)];
            if (st == 1) {
                System.arraycopy(ints, lo, out, 0, n);
            } else {
                for (int i = 0, j = lo; i < n; i++, j += st) {
                    out[i] = ints[j];
                }
            }
            PyList r = new PyList();
            r.ints = out;
            r.size = n;
            return r;
        }
        objects();
        int st = step == Py.NONE ? 1 : PySlice.step(step); //NOPMD CompareObjectsWithEquals - identity is meant
        int lo = PySlice.lower(start, stop, st, size);
        int hi = PySlice.upper(stop, st, size);
        int n = PySlice.count(lo, hi, st);
        Object[] out = new Object[Math.max(4, n)];
        if (st == 1) {
            System.arraycopy(items, lo, out, 0, n);
        } else {
            for (int i = 0, j = lo; i < n; i++, j += st) {
                out[i] = items[j];
            }
        }
        PyList r = new PyList(out);
        r.size = n;
        return r;
    }

    /// `self[start:stop:step]` copied into `dst`, whose items array is
    /// reused when it is large enough. Only for a list nothing else can see:
    /// the generated code calls it for a local the transpiler proved holds
    /// the sole reference to its list.
    PyList sliceInto(PyList dst, Object start, Object stop, Object step) {
        if (ints != null) {
            int st = step == Py.NONE ? 1 : PySlice.step(step); //NOPMD CompareObjectsWithEquals - identity is meant
            int lo = PySlice.lower(start, stop, st, size);
            int hi = PySlice.upper(stop, st, size);
            int n = PySlice.count(lo, hi, st);
            int[] out = dst.ints;
            if (out == null || out.length < n) {
                out = new int[Math.max(4, n)];
            }
            if (st == 1) {
                System.arraycopy(ints, lo, out, 0, n);
            } else {
                for (int i = 0, j = lo; i < n; i++, j += st) {
                    out[i] = ints[j];
                }
            }
            dst.floats = null;
            dst.items = NO_ITEMS;
            dst.ints = out;
            dst.size = n;
            return dst;
        }
        objects();
        int st = step == Py.NONE ? 1 : PySlice.step(step); //NOPMD CompareObjectsWithEquals - identity is meant
        int lo = PySlice.lower(start, stop, st, size);
        int hi = PySlice.upper(stop, st, size);
        int n = PySlice.count(lo, hi, st);
        Object[] out = dst.items;
        if (dst.floats != null || dst.ints != null || out.length < n || out == NO_ITEMS) {
            out = new Object[Math.max(4, n)];
        }
        if (st == 1) {
            System.arraycopy(items, lo, out, 0, n);
        } else {
            for (int i = 0, j = lo; i < n; i++, j += st) {
                out[i] = items[j];
            }
        }
        for (int i = n; i < dst.size && i < out.length; i++) {
            out[i] = null;
        }
        dst.floats = null;
        dst.ints = null;
        dst.items = out;
        dst.size = n;
        return dst;
    }

    /// `self[start:stop:step] = value` without a slice object. A contiguous
    /// assignment that keeps the length (the common `a[:k] = b[k::-1]`)
    /// writes in place and allocates nothing.
    public void setSlice(Object start, Object stop, Object step, Object value) {
        objects();
        int st = step == Py.NONE ? 1 : PySlice.step(step); //NOPMD CompareObjectsWithEquals - identity is meant
        int lo = PySlice.lower(start, stop, st, size);
        int hi = PySlice.upper(stop, st, size);
        Object[] src;
        int srcLen;
        if (value instanceof PyList && value != this) { //NOPMD CompareObjectsWithEquals - identity is meant
            src = ((PyList) value).objects();
            srcLen = ((PyList) value).size;
        } else {
            src = Py.toArray(value);
            srcLen = src.length;
        }
        if (st == 1) {
            if (hi < lo) {
                hi = lo;
            }
            int newSize = size - (hi - lo) + srcLen;
            if (newSize <= items.length) {
                System.arraycopy(items, hi, items, lo + srcLen, size - hi);
                System.arraycopy(src, 0, items, lo, srcLen);
                for (int i = newSize; i < size; i++) {
                    items[i] = null;
                }
            } else {
                Object[] n = new Object[Math.max(4, newSize + (newSize >> 3))];
                System.arraycopy(items, 0, n, 0, lo);
                System.arraycopy(src, 0, n, lo, srcLen);
                System.arraycopy(items, hi, n, lo + srcLen, size - hi);
                items = n;
            }
            size = newSize;
            return;
        }
        int count = PySlice.count(lo, hi, st);
        if (count != srcLen) {
            throw Py.valueError("attempt to assign sequence of size " + srcLen
                    + " to extended slice of size " + count);
        }
        for (int i = 0, j = lo; i < count; i++, j += st) {
            items[j] = src[i];
        }
    }

    /// `self[start:stop:step] = src[sStart:sStop:sStep]`. When the target is
    /// contiguous and as long as the source, the items are copied directly;
    /// a list read into itself is only done in place when that is exact (a
    /// forward copy, or reversing the very range it writes). Everything
    /// else takes the source slice as a list first.
    void setSliceFrom(Object start, Object stop, Object step, PyList src, Object sStart, Object sStop, Object sStep) {
        boolean li = ints != null && src.ints != null;
        if ((li || floats == null && ints == null && src.floats == null && src.ints == null) && step == Py.NONE //NOPMD CompareObjectsWithEquals - identity is meant
                && (sStep == Py.NONE || sStep instanceof Long)) { //NOPMD CompareObjectsWithEquals - identity is meant
            // Plain bounds, no objects to box: the common shapes, without
            // a call per bound.
            int lo = bound(start, 0, size);
            int hr = bound(stop, size, size);
            int hi = Math.max(lo, hr);
            long ss = sStep == Py.NONE ? 1 : ((Long) sStep).longValue(); //NOPMD CompareObjectsWithEquals - identity is meant
            if (ss == 1) {
                int slo = bound(sStart, 0, src.size);
                int shr = bound(sStop, src.size, src.size);
                int shi = Math.max(slo, shr);
                if (hi - lo == shi - slo && lo >= 0 && hr >= 0 && slo >= 0 && shr >= 0) {
                    if (li) {
                        System.arraycopy(src.ints, slo, ints, lo, hi - lo);
                    } else {
                        System.arraycopy(src.items, slo, items, lo, hi - lo);
                    }
                    return;
                }
            } else if (ss == -1 && src == this && sStop == Py.NONE && sStart instanceof Long) { //NOPMD CompareObjectsWithEquals - identity is meant
                long s0 = ((Long) sStart).longValue();
                if (s0 < 0) {
                    s0 += size;
                }
                if (s0 >= size) {
                    s0 = size - 1;
                }
                if (lo == 0 && hr >= 0 && hi == s0 + 1 && li) {
                    int[] it = ints;
                    for (int i = 0, j = (int) s0; i < j; i++, j--) {
                        int t = it[i];
                        it[i] = it[j];
                        it[j] = t;
                    }
                    return;
                }
                if (lo == 0 && hr >= 0 && hi == s0 + 1) {
                    Object[] it = items;
                    for (int i = 0, j = (int) s0; i < j; i++, j--) {
                        Object t = it[i];
                        it[i] = it[j];
                        it[j] = t;
                    }
                    return;
                }
            }
        }
        objects();
        src.objects();
        int sst = sStep == Py.NONE ? 1 : PySlice.step(sStep); //NOPMD CompareObjectsWithEquals - identity is meant
        int slo = PySlice.lower(sStart, sStop, sst, src.size);
        int shi = PySlice.upper(sStop, sst, src.size);
        int n = PySlice.count(slo, shi, sst);
        int st = step == Py.NONE ? 1 : PySlice.step(step); //NOPMD CompareObjectsWithEquals - identity is meant
        if (st == 1) {
            int lo = PySlice.lower(start, stop, 1, size);
            int hi = Math.max(lo, PySlice.upper(stop, 1, size));
            if (hi - lo == n) {
                if (sst == 1) {
                    System.arraycopy(src.items, slo, items, lo, n);
                    return;
                }
                if (src != this) { //NOPMD CompareObjectsWithEquals - identity is meant
                    for (int i = 0, j = slo; i < n; i++, j += sst) {
                        items[lo + i] = src.items[j];
                    }
                    return;
                }
                if (sst == -1 && slo == hi - 1) {
                    for (int i = lo, j = hi - 1; i < j; i++, j--) {
                        Object t = items[i];
                        items[i] = items[j];
                        items[j] = t;
                    }
                    return;
                }
            }
        }
        setSlice(start, stop, step, src.getSlice(sStart, sStop, sStep));
    }

    /// A bound of a step-1 slice over `n` items, `dflt` when absent;
    /// -1 when it is not a plain int (the caller then takes the general path).
    private static int bound(Object b, int dflt, int n) {
        if (b == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            return dflt;
        }
        if (!(b instanceof Long)) {
            return -1;
        }
        long v = ((Long) b).longValue();
        if (v < 0) {
            v += n;
            if (v < 0) {
                v = 0;
            }
        }
        return v > n ? n : (int) v;
    }

    @Override
    public void setItem(Object key, Object value) {
        if (ints != null && value instanceof Long && !(key instanceof PySlice) && fits(((Long) value).longValue())) {
            ints[checkIndex(key)] = (int) ((Long) value).longValue();
            return;
        }
        if (floats != null && value instanceof Double && !(key instanceof PySlice)) {
            floats[checkIndex(key)] = ((Double) value).doubleValue();
            return;
        }
        objects();
        if (key instanceof Long) {
            long i = ((Long) key).longValue();
            if (i >= 0 && i < size) {
                items[(int) i] = value;
                return;
            }
        }
        if (key instanceof PySlice) {
            int[] r = ((PySlice) key).indices(size);
            Object[] src = Py.toArray(value);
            if (r[2] == 1) {
                int lo = r[0];
                int hi = Math.max(r[0], r[1]);
                int newSize = size - (hi - lo) + src.length;
                Object[] n = new Object[Math.max(4, newSize)];
                System.arraycopy(items, 0, n, 0, lo);
                System.arraycopy(src, 0, n, lo, src.length);
                System.arraycopy(items, hi, n, lo + src.length, size - hi);
                items = n;
                size = newSize;
                return;
            }
            int count = PySlice.length(r);
            if (count != src.length) {
                throw Py.valueError("attempt to assign sequence of size " + src.length
                        + " to extended slice of size " + count);
            }
            for (int i = 0, j = r[0]; i < count; i++, j += r[2]) {
                items[j] = src[i];
            }
            return;
        }
        items[checkIndex(key)] = value;
    }

    @Override
    public void delItem(Object key) {
        objects();
        if (key instanceof PySlice) {
            int[] r = ((PySlice) key).indices(size);
            int count = PySlice.length(r);
            if (count == 0) {
                return;
            }
            boolean[] drop = new boolean[size];
            for (int i = 0, j = r[0]; i < count; i++, j += r[2]) {
                drop[j] = true;
            }
            int w = 0;
            for (int i = 0; i < size; i++) {
                if (!drop[i]) {
                    items[w++] = items[i];
                }
            }
            for (int i = w; i < size; i++) {
                items[i] = null;
            }
            size = w;
            return;
        }
        pop(checkIndex(key));
    }

    @Override
    public boolean contains(Object x) {
        objects();
        for (int i = 0; i < size; i++) {
            if (items[i] == x || Py.eqBool(items[i], x)) { //NOPMD CompareObjectsWithEquals - identity is meant
                return true;
            }
        }
        return false;
    }

    @Override
    public Object iter() {
        return new PyIter(size) {
            @Override
            int liveLength() {
                return size;
            }

            @Override
            Object at(int i) {
                return get(i);
            }
        };
    }

    /// `list.sort(key=None, reverse=False)`: a stable merge sort. The order
    /// is an int array, and keys that are all ints compare as longs.
    public void sort(Object key, boolean reverse) {
        boolean none = key == null || key == Py.NONE; //NOPMD CompareObjectsWithEquals - identity is meant
        if (none && ints != null) {
            // Unboxed ints: equal ones cannot be told apart, so stability is
            // moot and the array sorts in place, never boxed.
            java.util.Arrays.sort(ints, 0, size);
            if (reverse) {
                int[] l = ints;
                for (int i = 0, j = size - 1; i < j; i++, j--) {
                    int t = l[i];
                    l[i] = l[j];
                    l[j] = t;
                }
            }
            return;
        }
        objects();
        PyObject kf = none ? null : Py.obj(key);
        Object[] keys = new Object[size];
        boolean longs = true;
        boolean strs = true;
        for (int i = 0; i < size; i++) {
            Object k = none ? items[i] : kf.call1(items[i]);
            keys[i] = k;
            longs &= k instanceof Long;
            strs &= k instanceof String;
        }
        if (longs && size > 1 && packable(keys)) {
            sortPacked(keys, reverse);
            return;
        }
        long[] lk = null;
        Object[] tie = keys;
        if (longs) {
            lk = new long[size];
            for (int i = 0; i < size; i++) {
                lk[i] = (Long) keys[i];
            }
            tie = null;
        } else if (!strs) {
            lk = new long[size];
            if (!tupleFirsts(keys, lk)) {
                lk = null;
            }
        }
        int[] order = new int[size];
        for (int i = 0; i < size; i++) {
            order[i] = i;
        }
        mergeSort(order, new int[size], tie, lk, strs && size > 0, 0, size, reverse);
        Object[] out = new Object[Math.max(4, size)];
        for (int i = 0; i < size; i++) {
            out[i] = items[order[i]];
        }
        items = out;
    }

    /// When every key is a tuple whose first item is an int, those ints, so
    /// that most comparisons are settled without touching the tuples (as
    /// CPython's unsafe_tuple_compare does), written into `r`; false when
    /// some key is not such a tuple.
    private static boolean tupleFirsts(Object[] keys, long[] r) {
        for (int i = 0; i < keys.length; i++) {
            if (!(keys[i] instanceof PyTuple)) {
                return false;
            }
            Object[] t = ((PyTuple) keys[i]).arr();
            if (t.length == 0 || !(t[0] instanceof Long)) {
                return false;
            }
            r[i] = ((Long) t[0]).longValue();
        }
        return true;
    }

    private static boolean packable(Object[] keys) {
        for (Object k : keys) {
            long v = ((Long) k).longValue();
            if (v != (int) v) {
                return false;
            }
        }
        return true;
    }

    /// Int keys that fit in 32 bits: each packed above its index into one
    /// long, so a primitive merge sort orders by key and breaks ties by
    /// position -- stable, with no index indirection or boxed compare.
    /// Reversed, the key is complemented, which keeps equal keys in their
    /// original order as Python's reverse=True does.
    private void sortPacked(Object[] keys, boolean reverse) {
        int n = size;
        long[] p = new long[n];
        for (int i = 0; i < n; i++) {
            int k = (int) ((Long) keys[i]).longValue();
            p[i] = ((long) (reverse ? ~k : k) << 32) | i;
        }
        mergeLongs(p, new long[n], 0, n);
        Object[] src = items;
        Object[] out = new Object[Math.max(4, n)];
        for (int i = 0; i < n; i++) {
            out[i] = src[(int) p[i]];
        }
        items = out;
    }

    private static void mergeLongs(long[] a, long[] tmp, int lo, int hi) {
        if (hi - lo <= 12) {
            for (int i = lo + 1; i < hi; i++) {
                long v = a[i];
                int j = i - 1;
                while (j >= lo && a[j] > v) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = v;
            }
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeLongs(a, tmp, lo, mid);
        mergeLongs(a, tmp, mid, hi);
        if (a[mid - 1] < a[mid]) {
            return;
        }
        int i = lo;
        int j = mid;
        int k = lo;
        while (i < mid && j < hi) {
            tmp[k++] = a[j] < a[i] ? a[j++] : a[i++];
        }
        while (i < mid) {
            tmp[k++] = a[i++];
        }
        while (j < hi) {
            tmp[k++] = a[j++];
        }
        System.arraycopy(tmp, lo, a, lo, hi - lo);
    }

    private static void mergeSort(int[] a, int[] tmp, Object[] keys, long[] lk, boolean strs, int lo, int hi,
            boolean rev) {
        if (hi - lo < 2) {
            return;
        }
        if (hi - lo <= 8) {
            for (int i = lo + 1; i < hi; i++) {
                int v = a[i];
                int j = i - 1;
                while (j >= lo && before(keys, lk, strs, v, a[j], rev)) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = v;
            }
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSort(a, tmp, keys, lk, strs, lo, mid, rev);
        mergeSort(a, tmp, keys, lk, strs, mid, hi, rev);
        if (!before(keys, lk, strs, a[mid], a[mid - 1], rev)) {
            // Already in order: the two runs need no merge.
            return;
        }
        int i = lo;
        int j = mid;
        int k = lo;
        while (i < mid && j < hi) {
            if (before(keys, lk, strs, a[j], a[i], rev)) {
                tmp[k++] = a[j++];
            } else {
                tmp[k++] = a[i++];
            }
        }
        while (i < mid) {
            tmp[k++] = a[i++];
        }
        while (j < hi) {
            tmp[k++] = a[j++];
        }
        System.arraycopy(tmp, lo, a, lo, hi - lo);
    }

    private static boolean before(Object[] keys, long[] lk, boolean strs, int x, int y, boolean rev) {
        if (lk != null) {
            long a = lk[x];
            long b = lk[y];
            if (a != b) {
                return rev ? b < a : a < b;
            }
            // Equal ints never move; equal first items of two tuples leave
            // the rest of the tuples to decide.
            return keys != null && before(keys[x], keys[y], rev);
        }
        if (strs) {
            // Every key a str, checked once up front as CPython's
            // unsafe_latin_compare is: no comparison can run user code.
            int c = PyStr.compare((String) keys[x], (String) keys[y]);
            return rev ? c > 0 : c < 0;
        }
        return before(keys[x], keys[y], rev);
    }

    /// Whether `x` must move before `y`: `x < y`, or `y < x` reversed. Equal
    /// keys never move, which keeps the sort stable in both directions.
    private static boolean before(Object x, Object y, boolean rev) {
        return rev ? Py.ltBool(y, x) : Py.ltBool(x, y);
    }

    public void reverse() {
        if (ints != null) {
            int[] l = ints;
            for (int i = 0, j = size - 1; i < j; i++, j--) {
                int t = l[i];
                l[i] = l[j];
                l[j] = t;
            }
            return;
        }
        objects();
        for (int i = 0, j = size - 1; i < j; i++, j--) {
            Object t = items[i];
            items[i] = items[j];
            items[j] = t;
        }
    }

    public long index(Object v) {
        objects();
        for (int i = 0; i < size; i++) {
            if (Py.eqBool(items[i], v)) {
                return i;
            }
        }
        throw Py.valueError("list.index(x): x not in list");
    }

    public void remove(Object v) {
        pop(index(v));
    }

    /// Empties the list but keeps its items array for the next fill; for a
    /// list nothing else can see (see Py.recycleList).
    void reset() {
        if (floats != null || ints != null) {
            floats = null;
            ints = null;
            items = NO_ITEMS;
        } else {
            for (int i = 0; i < size; i++) {
                items[i] = null;
            }
        }
        size = 0;
    }

    public void clear() {
        items = NO_ITEMS;
        floats = null;
        ints = null;
        size = 0;
    }
}
