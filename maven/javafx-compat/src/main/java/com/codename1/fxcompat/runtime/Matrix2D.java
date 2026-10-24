/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.fxcompat.runtime;

/// Arithmetic on the affine matrices the layer passes around as
/// `{a, b, c, d, tx, ty}`: `x' = a*x + c*y + tx`, `y' = b*x + d*y + ty`.
/// In JavaFX's names that is `{mxx, myx, mxy, myy, tx, ty}`.
public final class Matrix2D {

    private Matrix2D() {
    }

    /// Returns a new identity matrix.
    public static double[] identity() {
        return new double[] {1, 0, 0, 1, 0, 0};
    }

    /// Returns whether a matrix changes nothing.
    public static boolean isIdentity(double[] m) {
        return Double.compare(m[0], 1) == 0 && isZero(m[1]) && isZero(m[2]) && Double.compare(m[3], 1) == 0
                && isZero(m[4]) && isZero(m[5]);
    }

    private static boolean isZero(double v) {
        // Both zeroes count.
        return !(v > 0) && !(v < 0) && !Double.isNaN(v);
    }

    /// Returns `left x right`: the matrix that applies `right` first and
    /// `left` to its result.
    public static double[] multiply(double[] left, double[] right) {
        return new double[] {left[0] * right[0] + left[2] * right[1], left[1] * right[0] + left[3] * right[1],
            left[0] * right[2] + left[2] * right[3], left[1] * right[2] + left[3] * right[3],
            left[0] * right[4] + left[2] * right[5] + left[4], left[1] * right[4] + left[3] * right[5] + left[5]};
    }

    /// Returns the determinant of the linear part.
    public static double determinant(double[] m) {
        return m[0] * m[3] - m[1] * m[2];
    }

    /// Writes the inverse of a matrix into `out` and answers `true`;
    /// answers `false`, leaving `out` alone, when it has none.
    public static boolean invert(double[] m, double[] out) {
        double det = determinant(m);
        if (isZero(det) || Double.isNaN(det) || Double.isInfinite(det)) {
            return false;
        }
        double a = m[3] / det;
        double b = -m[1] / det;
        double c = -m[2] / det;
        double d = m[0] / det;
        double tx = -(a * m[4] + c * m[5]);
        double ty = -(b * m[4] + d * m[5]);
        out[0] = a;
        out[1] = b;
        out[2] = c;
        out[3] = d;
        out[4] = tx;
        out[5] = ty;
        return true;
    }

    /// Returns the x of a point mapped through a matrix.
    public static double x(double[] m, double x, double y) {
        return m[0] * x + m[2] * y + m[4];
    }

    /// Returns the y of a point mapped through a matrix.
    public static double y(double[] m, double x, double y) {
        return m[1] * x + m[3] * y + m[5];
    }

    /// Returns the factor a matrix scales lengths by, the square root of
    /// the area it maps a unit square to.
    public static double uniformScale(double[] m) {
        return Math.sqrt(Math.abs(determinant(m)));
    }

    /// Returns the bounds `{minX, minY, maxX, maxY}` of a rectangle mapped
    /// through a matrix.
    public static double[] bounds(double[] m, double x, double y, double w, double h) {
        double[] xs = {x, x + w, x + w, x};
        double[] ys = {y, y, y + h, y + h};
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double px = x(m, xs[i], ys[i]);
            double py = y(m, xs[i], ys[i]);
            minX = Math.min(minX, px);
            minY = Math.min(minY, py);
            maxX = Math.max(maxX, px);
            maxY = Math.max(maxY, py);
        }
        return new double[] {minX, minY, maxX, maxY};
    }
}
