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

/// `range(start, stop, step)`, lazily.
public final class PyRange extends PyObject {
    final long start;
    final long stop;
    final long step;

    public PyRange(long start, long stop, long step) {
        if (step == 0) {
            throw Py.valueError("range() arg 3 must not be zero");
        }
        this.start = start;
        this.stop = stop;
        this.step = step;
    }

    @Override
    public PyType type() {
        return Py.RANGE_TYPE;
    }

    @Override
    public Object getAttrOrNull(String name) {
        if ("start".equals(name)) {
            return PyInt.of(start);
        }
        if ("stop".equals(name)) {
            return PyInt.of(stop);
        }
        if ("step".equals(name)) {
            return PyInt.of(step);
        }
        return super.getAttrOrNull(name);
    }

    /// Ranges compare as the sequences they produce: equal lengths, and when
    /// not empty the same first element and (for more than one) the same step.
    @Override
    public Object eq(Object o) {
        if (!(o instanceof PyRange)) {
            return null;
        }
        PyRange r = (PyRange) o;
        long n = length();
        if (n != r.length()) {
            return Py.FALSE;
        }
        if (n == 0) {
            return Py.TRUE;
        }
        if (start != r.start) {
            return Py.FALSE;
        }
        return n == 1 || step == r.step ? Py.TRUE : Py.FALSE;
    }

    /// Consistent with `eq`: hashes the length, and the first element and
    /// step only where they decide equality.
    @Override
    public long hash() {
        long n = length();
        long h = n * 1000003L;
        if (n > 0) {
            h = h * 31 + start;
            if (n > 1) {
                h = h * 31 + step;
            }
        }
        return h == -1 ? -2 : h;
    }

    public long length() {
        if (step > 0) {
            return stop > start ? (stop - start + step - 1) / step : 0;
        }
        return start > stop ? (start - stop - step - 1) / -step : 0;
    }

    @Override
    public int len() {
        return (int) length();
    }

    @Override
    public boolean isTrue() {
        return length() != 0;
    }

    @Override
    public Object iter() {
        final long n = length();
        return new PyJavaIter(new PyJavaIter.Source() {
            long i;

            @Override
            public Object next() {
                if (i >= n) {
                    return null;
                }
                return PyInt.of(start + step * i++);
            }
        });
    }

    @Override
    public Object getItem(Object key) {
        long n = length();
        if (key instanceof PySlice) {
            int[] r = ((PySlice) key).indices((int) n);
            return new PyRange(start + r[0] * step, start + r[1] * step, step * r[2]);
        }
        long i = Py.index(key);
        if (i < 0) {
            i += n;
        }
        if (i < 0 || i >= n) {
            throw Py.indexError("range object index out of range");
        }
        return PyInt.of(start + i * step);
    }

    @Override
    public boolean contains(Object x) {
        if (!(Py.isInt(x)) || Py.intView(x).isBig()) {
            return super.contains(x);
        }
        long v = Py.intView(x).value;
        if (step > 0 ? v < start || v >= stop : v > start || v <= stop) {
            return false;
        }
        return (v - start) % step == 0;
    }

    @Override
    public String repr() {
        return step == 1 ? "range(" + start + ", " + stop + ")" : "range(" + start + ", " + stop + ", " + step + ")";
    }
}
