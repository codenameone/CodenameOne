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

/// `slice(start, stop, step)`; any part may be `None`.
public final class PySlice extends PyObject {
    final Object start;
    final Object stop;
    final Object step;

    public PySlice(Object start, Object stop, Object step) {
        this.start = start == null ? Py.NONE : start;
        this.stop = stop == null ? Py.NONE : stop;
        this.step = step == null ? Py.NONE : step;
    }

    @Override
    public PyType type() {
        return Py.SLICE_TYPE;
    }

    /// `slice.indices(length)` as `{start, stop, step}`, clamped as CPython does.
    public int[] indices(int length) {
        int st = step == Py.NONE ? 1 : (int) Py.index(step); //NOPMD CompareObjectsWithEquals - identity is meant
        if (st == 0) {
            throw Py.valueError("slice step cannot be zero");
        }
        int lo;
        int hi;
        if (st > 0) {
            lo = start == Py.NONE ? 0 : clamp(Py.index(start), length, 0, length); //NOPMD CompareObjectsWithEquals - identity is meant
            hi = stop == Py.NONE ? length : clamp(Py.index(stop), length, 0, length); //NOPMD CompareObjectsWithEquals - identity is meant
        } else {
            lo = start == Py.NONE ? length - 1 : clamp(Py.index(start), length, -1, length - 1); //NOPMD CompareObjectsWithEquals - identity is meant
            hi = stop == Py.NONE ? -1 : clamp(Py.index(stop), length, -1, length - 1); //NOPMD CompareObjectsWithEquals - identity is meant
        }
        return new int[] {lo, hi, st};
    }

    /// The step of a slice, checked to be nonzero.
    static int step(Object step) {
        int st = (int) Py.index(step);
        if (st == 0) {
            throw Py.valueError("slice step cannot be zero");
        }
        return st;
    }

    /// The first index a slice selects, as in [#indices(int)].
    static int lower(Object start, Object stop, int st, int length) {
        if (st > 0) {
            return start == Py.NONE ? 0 : clamp(Py.index(start), length, 0, length); //NOPMD CompareObjectsWithEquals - identity is meant
        }
        return start == Py.NONE ? length - 1 : clamp(Py.index(start), length, -1, length - 1); //NOPMD CompareObjectsWithEquals - identity is meant
    }

    /// The bound a slice stops at, as in [#indices(int)].
    static int upper(Object stop, int st, int length) {
        if (st > 0) {
            return stop == Py.NONE ? length : clamp(Py.index(stop), length, 0, length); //NOPMD CompareObjectsWithEquals - identity is meant
        }
        return stop == Py.NONE ? -1 : clamp(Py.index(stop), length, -1, length - 1); //NOPMD CompareObjectsWithEquals - identity is meant
    }

    /// The number of indices from `lo` towards `hi` by `st`.
    static int count(int lo, int hi, int st) {
        if (st > 0) {
            return hi > lo ? (hi - lo + st - 1) / st : 0;
        }
        return lo > hi ? (lo - hi - st - 1) / -st : 0;
    }

    private static int clamp(long v, int length, int min, int max) {
        if (v < 0) {
            v += length;
        }
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return (int) v;
    }

    /// The number of elements `indices` selects.
    public static int length(int[] r) {
        if (r[2] > 0) {
            return r[1] > r[0] ? (r[1] - r[0] + r[2] - 1) / r[2] : 0;
        }
        return r[0] > r[1] ? (r[0] - r[1] - r[2] - 1) / -r[2] : 0;
    }

    @Override
    public Object getAttrOrNull(String name) {
        if ("start".equals(name)) {
            return start;
        }
        if ("stop".equals(name)) {
            return stop;
        }
        if ("step".equals(name)) {
            return step;
        }
        if ("indices".equals(name)) {
            return PyBuiltin.positional("indices", a -> {
                if (a.length != 1) {
                    throw Py.typeError("slice.indices() takes exactly one argument (" + a.length + " given)");
                }
                long n = Py.asLong(a[0]);
                if (n < 0) {
                    throw Py.valueError("length should not be negative");
                }
                int[] r = indices((int) n);
                return new PyTuple(new Object[] {Py.of((long) r[0]), Py.of((long) r[1]), Py.of((long) r[2])});
            });
        }
        return super.getAttrOrNull(name);
    }

    @Override
    public String repr() {
        return "slice(" + Py.toRepr(start) + ", " + Py.toRepr(stop) + ", " + Py.toRepr(step) + ")";
    }
}
