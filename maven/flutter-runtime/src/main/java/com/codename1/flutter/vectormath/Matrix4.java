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

    private final dart.typed_data.Float64List m = new dart.typed_data.Float64List(16);

    private double get(int i) {
        return m.getDouble(i);
    }

    private void set(int i, double v) {
        m.setDouble(i, v);
    }

    private Matrix4() {
    }

    private static Matrix4 zero() {
        return new Matrix4();
    }

    /** vector_math's {@code Matrix4.identity()}. */
    public static Matrix4 identity() {
        Matrix4 r = new Matrix4();
        r.set(0, 1.0);
        r.set(5, 1.0);
        r.set(10, 1.0);
        r.set(15, 1.0);
        return r;
    }

    /** vector_math's {@code Matrix4.rotationX(radians)}. */
    public static Matrix4 rotationX(double radians) {
        Matrix4 r = identity();
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        r.set(5, c);
        r.set(6, s);
        r.set(9, -s);
        r.set(10, c);
        return r;
    }

    /** vector_math's {@code Matrix4.rotationY(radians)}. */
    public static Matrix4 rotationY(double radians) {
        Matrix4 r = identity();
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        r.set(0, c);
        r.set(2, -s);
        r.set(8, s);
        r.set(10, c);
        return r;
    }

    /** vector_math's {@code Matrix4.rotationZ(radians)}. */
    public static Matrix4 rotationZ(double radians) {
        Matrix4 r = identity();
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        r.set(0, c);
        r.set(1, s);
        r.set(4, -s);
        r.set(5, c);
        return r;
    }

    /** vector_math's {@code Matrix4.translationValues(x, y, z)}. */
    public static Matrix4 translationValues(double x, double y, double z) {
        Matrix4 r = identity();
        r.set(12, x);
        r.set(13, y);
        r.set(14, z);
        return r;
    }

    /** vector_math's {@code Matrix4.diagonal3Values(x, y, z)}. */
    public static Matrix4 diagonal3Values(double x, double y, double z) {
        Matrix4 r = new Matrix4();
        r.set(0, x);
        r.set(5, y);
        r.set(10, z);
        r.set(15, 1.0);
        return r;
    }

    /**
     * vector_math's {@code storage}: the matrix's own column-major entries, so
     * {@code matrix.storage[12] = 20} moves the matrix. It used to build a copy on
     * every call, and writes through it were silently lost.
     */
    public dart.typed_data.Float64List storage() {
        return m;
    }

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
            r.set(i, a.get(i) + (b.get(i) - a.get(i)) * t);
        }
        return r;
    }

    /** A copy of this matrix — vector_math's {@code clone()}. */
    public Matrix4 clone() {
        Matrix4 r = new Matrix4();
        for (int i = 0; i < 16; i++) {
            r.set(i, get(i));
        }
        return r;
    }

    /** vector_math's {@code setEntry(row, col, value)} (column-major storage). */
    public void setEntry(int row, int col, double value) {
        set(col * 4 + row, value);
    }

    /**
     * vector_math's {@code setRotationZ(radians)}: the whole upper 3x3 becomes the
     * rotation and the perspective row is cleared; only the translation column is
     * kept. Setting just the four sine/cosine entries left a reused matrix's scale,
     * skew or perspective in place.
     */
    public void setRotationZ(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        set(0, c);
        set(1, s);
        set(2, 0.0);
        set(3, 0.0);
        set(4, -s);
        set(5, c);
        set(6, 0.0);
        set(7, 0.0);
        set(8, 0.0);
        set(9, 0.0);
        set(10, 1.0);
        set(11, 0.0);
    }

    // vector_math's translate(x, [y, z]) — arity overloads for the transpiler.

    public void translate(double x) {
        translate(x, 0.0, 0.0);
    }

    public void translate(double x, double y) {
        translate(x, y, 0.0);
    }

    public void translate(double x, double y, double z) {
        set(12, get(12) + (get(0) * x + get(4) * y + get(8) * z));
        set(13, get(13) + (get(1) * x + get(5) * y + get(9) * z));
        set(14, get(14) + (get(2) * x + get(6) * y + get(10) * z));
        set(15, get(15) + (get(3) * x + get(7) * y + get(11) * z));
    }

    // vector_math's scale(x, [y, z]) — arity overloads for the transpiler.

    public void scale(double x) {
        scale(x, x, x);
    }

    public void scale(double x, double y) {
        scale(x, y, 1.0);
    }

    /// vector_math's scaleByDouble(sx, sy, sz, sw): scales the columns, the
    /// fourth by sw, as scale(x, y, z) does the first three.
    public void scaleByDouble(double sx, double sy, double sz, double sw) {
        scale(sx, sy, sz);
        for (int i = 0; i < 4; i++) {
            set(12 + i, get(12 + i) * (sw));
        }
    }

    public void scale(double x, double y, double z) {
        for (int i = 0; i < 4; i++) {
            set(i, get(i) * (x));
            set(4 + i, get(4 + i) * (y));
            set(8 + i, get(8 + i) * (z));
        }
    }
}
