/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.vectormath;

import dart.core.DartList;

/**
 * A 4x4 column-major transform matrix — {@code package:vector_math_64}'s
 * {@code Matrix4}. new_gallery builds one (via {@link #identity()},
 * {@link #rotationZ(double)}, {@link #translationValues}, ...) and hands it to a
 * {@code Transform} as its {@code transform} argument, or reads {@link #storage()}.
 *
 * <p>The 16 entries are stored column-major, matching vector_math so
 * {@code storage[12..14]} hold the translation and the transpiler's index math
 * stays valid.</p>
 */
public class Matrix4 {

    private final double[] m = new double[16];

    private Matrix4() {
    }

    private static Matrix4 zero() {
        return new Matrix4();
    }

    /** vector_math's {@code Matrix4.identity()}. */
    public static Matrix4 identity() {
        Matrix4 r = new Matrix4();
        r.m[0] = 1.0;
        r.m[5] = 1.0;
        r.m[10] = 1.0;
        r.m[15] = 1.0;
        return r;
    }

    /** vector_math's {@code Matrix4.rotationX(radians)}. */
    public static Matrix4 rotationX(double radians) {
        Matrix4 r = identity();
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        r.m[5] = c;
        r.m[6] = s;
        r.m[9] = -s;
        r.m[10] = c;
        return r;
    }

    /** vector_math's {@code Matrix4.rotationY(radians)}. */
    public static Matrix4 rotationY(double radians) {
        Matrix4 r = identity();
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        r.m[0] = c;
        r.m[2] = -s;
        r.m[8] = s;
        r.m[10] = c;
        return r;
    }

    /** vector_math's {@code Matrix4.rotationZ(radians)}. */
    public static Matrix4 rotationZ(double radians) {
        Matrix4 r = identity();
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        r.m[0] = c;
        r.m[1] = s;
        r.m[4] = -s;
        r.m[5] = c;
        return r;
    }

    /** vector_math's {@code Matrix4.translationValues(x, y, z)}. */
    public static Matrix4 translationValues(double x, double y, double z) {
        Matrix4 r = identity();
        r.m[12] = x;
        r.m[13] = y;
        r.m[14] = z;
        return r;
    }

    /** vector_math's {@code Matrix4.diagonal3Values(x, y, z)}. */
    public static Matrix4 diagonal3Values(double x, double y, double z) {
        Matrix4 r = new Matrix4();
        r.m[0] = x;
        r.m[5] = y;
        r.m[10] = z;
        r.m[15] = 1.0;
        return r;
    }

    /**
     * vector_math's {@code storage}: the matrix's own column-major entries, so
     * {@code matrix.storage[12] = 20} moves the matrix. It used to build a copy on
     * every call, and writes through it were silently lost.
     */
    public DartList<Double> storage() {
        if (storageView == null) {
            storageView = dart.core.DartDoubleList.view(m);
        }
        return storageView;
    }

    private DartList<Double> storageView;

    /**
     * Component-wise interpolation of two matrices, which is what Flutter's
     * {@code Matrix4Tween} does.
     *
     * <p>It lives here rather than in the tween because the storage does: doing it
     * through {@code storage()} would build two lists per frame for an animation that
     * runs per frame.</p>
     */
    public static Matrix4 lerp(Matrix4 a, Matrix4 b, double t) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            a = zero();
        }
        if (b == null) {
            b = zero();
        }
        Matrix4 r = new Matrix4();
        for (int i = 0; i < 16; i++) {
            r.m[i] = a.m[i] + (b.m[i] - a.m[i]) * t;
        }
        return r;
    }

    /** A copy of this matrix — vector_math's {@code clone()}. */
    public Matrix4 clone() {
        Matrix4 r = new Matrix4();
        System.arraycopy(m, 0, r.m, 0, m.length);
        return r;
    }

    /** vector_math's {@code setEntry(row, col, value)} (column-major storage). */
    public void setEntry(int row, int col, double value) {
        m[col * 4 + row] = value;
    }

    /** vector_math's {@code setRotationZ(radians)}. */
    public void setRotationZ(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        m[0] = c;
        m[1] = s;
        m[4] = -s;
        m[5] = c;
    }

    // vector_math's translate(x, [y, z]) — arity overloads for the transpiler.

    public void translate(double x) {
        translate(x, 0.0, 0.0);
    }

    public void translate(double x, double y) {
        translate(x, y, 0.0);
    }

    public void translate(double x, double y, double z) {
        m[12] += m[0] * x + m[4] * y + m[8] * z;
        m[13] += m[1] * x + m[5] * y + m[9] * z;
        m[14] += m[2] * x + m[6] * y + m[10] * z;
        m[15] += m[3] * x + m[7] * y + m[11] * z;
    }

    // vector_math's scale(x, [y, z]) — arity overloads for the transpiler.

    public void scale(double x) {
        scale(x, x, x);
    }

    public void scale(double x, double y) {
        scale(x, y, 1.0);
    }

    public void scale(double x, double y, double z) {
        for (int i = 0; i < 4; i++) {
            m[i] *= x;
            m[4 + i] *= y;
            m[8 + i] *= z;
        }
    }
}
