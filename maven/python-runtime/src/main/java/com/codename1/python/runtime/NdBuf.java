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

/// The storage behind a `numpy.ndarray`: one flat primitive array, `double`
/// for float64 and `long` for int64 and bool (0 or 1). The array's shape,
/// strides and offset live in Python; the loops over them are [NumPy]'s.
final class NdBuf extends PyObject {
    static final PyType TYPE = new PyType("_cn1np.buffer", new PyType[] {Py.OBJECT_TYPE}, null);

    final char kind;
    final double[] d;
    final long[] l;

    NdBuf(char kind, int n) {
        this.kind = kind;
        if (kind == 'f') {
            d = new double[n];
            l = null;
        } else {
            d = null;
            l = new long[n];
        }
    }

    int size() {
        return d != null ? d.length : l.length;
    }

    double getD(int i) {
        return d != null ? d[i] : l[i];
    }

    long getL(int i) {
        return d != null ? (long) d[i] : l[i];
    }

    void setD(int i, double v) {
        if (d != null) {
            d[i] = v;
        } else if (kind == 'b') {
            l[i] = v != 0 ? 1 : 0;
        } else {
            l[i] = (long) v;
        }
    }

    void setL(int i, long v) {
        if (d != null) {
            d[i] = v;
        } else {
            l[i] = kind == 'b' ? (v != 0 ? 1 : 0) : v;
        }
    }

    Object item(int i) {
        if (kind == 'f') {
            return Double.valueOf(d[i]);
        }
        if (kind == 'b') {
            return Py.bool(l[i] != 0);
        }
        return Long.valueOf(l[i]);
    }

    @Override
    public PyType type() {
        return TYPE;
    }

    @Override
    public String repr() {
        return "<ndarray buffer " + kind + "[" + size() + "]>";
    }
}
