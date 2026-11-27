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
package com.codename1.art;

/// An immutable 2D affine transform, in SVG's `matrix(a b c d e f)` order.
///
/// A point maps as `x' = a*x + c*y + e` and `y' = b*x + d*y + f`.
public final class Matrix {
    /// The identity transform.
    public static final Matrix IDENTITY = new Matrix(1, 0, 0, 1, 0, 0);

    private final double a;
    private final double b;
    private final double c;
    private final double d;
    private final double e;
    private final double f;

    /// Creates a transform from its six coefficients.
    ///
    /// #### Parameters
    ///
    /// - `a`: x scale
    /// - `b`: y shear
    /// - `c`: x shear
    /// - `d`: y scale
    /// - `e`: x translation
    /// - `f`: y translation
    public Matrix(double a, double b, double c, double d, double e, double f) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        this.f = f;
    }

    /// A translation.
    ///
    /// #### Parameters
    ///
    /// - `x`: horizontal offset
    /// - `y`: vertical offset
    public static Matrix translate(double x, double y) {
        return new Matrix(1, 0, 0, 1, x, y);
    }

    /// A scale about the origin.
    ///
    /// #### Parameters
    ///
    /// - `sx`: horizontal factor
    /// - `sy`: vertical factor
    public static Matrix scale(double sx, double sy) {
        return new Matrix(sx, 0, 0, sy, 0, 0);
    }

    /// A rotation about the origin.
    ///
    /// #### Parameters
    ///
    /// - `radians`: the angle, clockwise on screen
    public static Matrix rotate(double radians) {
        double cs = Math.cos(radians);
        double sn = Math.sin(radians);
        return new Matrix(cs, sn, -sn, cs, 0, 0);
    }

    /// A skew.
    ///
    /// #### Parameters
    ///
    /// - `ax`: horizontal skew angle in radians
    /// - `ay`: vertical skew angle in radians
    public static Matrix skew(double ax, double ay) {
        return new Matrix(1, Math.tan(ay), Math.tan(ax), 1, 0, 0);
    }

    /// Returns the transform that applies `other` first and then this one.
    ///
    /// #### Parameters
    ///
    /// - `other`: the inner transform
    public Matrix multiply(Matrix other) {
        return new Matrix(
                a * other.a + c * other.b,
                b * other.a + d * other.b,
                a * other.c + c * other.d,
                b * other.c + d * other.d,
                a * other.e + c * other.f + e,
                b * other.e + d * other.f + f);
    }

    /// Returns the inverse, or null when the transform collapses the plane.
    public Matrix invert() {
        double det = a * d - b * c;
        if (det == 0 || Double.isNaN(det) || Double.isInfinite(det)) {
            return null;
        }
        return new Matrix(d / det, -b / det, -c / det, a / det,
                (c * f - d * e) / det, (b * e - a * f) / det);
    }

    /// Maps the x coordinate of a point.
    ///
    /// #### Parameters
    ///
    /// - `x`: the point's x
    /// - `y`: the point's y
    public double mapX(double x, double y) {
        return a * x + c * y + e;
    }

    /// Maps the y coordinate of a point.
    ///
    /// #### Parameters
    ///
    /// - `x`: the point's x
    /// - `y`: the point's y
    public double mapY(double x, double y) {
        return b * x + d * y + f;
    }

    /// The mean scale factor, used to size stroke widths and blur radii.
    public double meanScale() {
        double det = Math.abs(a * d - b * c);
        return Math.sqrt(det);
    }

    /// True for the identity transform.
    public boolean isIdentity() {
        return a == 1 && b == 0 && c == 0 && d == 1 && e == 0 && f == 0;
    }

    /// The `a` coefficient.
    public double getA() {
        return a;
    }

    /// The `b` coefficient.
    public double getB() {
        return b;
    }

    /// The `c` coefficient.
    public double getC() {
        return c;
    }

    /// The `d` coefficient.
    public double getD() {
        return d;
    }

    /// The `e` coefficient.
    public double getE() {
        return e;
    }

    /// The `f` coefficient.
    public double getF() {
        return f;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Matrix)) {
            return false;
        }
        Matrix m = (Matrix) o;
        return Double.compare(a, m.a) == 0 && Double.compare(b, m.b) == 0
                && Double.compare(c, m.c) == 0 && Double.compare(d, m.d) == 0
                && Double.compare(e, m.e) == 0 && Double.compare(f, m.f) == 0;
    }

    @Override
    public int hashCode() {
        long h = Double.doubleToLongBits(a);
        h = h * 31 + Double.doubleToLongBits(b);
        h = h * 31 + Double.doubleToLongBits(c);
        h = h * 31 + Double.doubleToLongBits(d);
        h = h * 31 + Double.doubleToLongBits(e);
        h = h * 31 + Double.doubleToLongBits(f);
        return (int) (h ^ (h >>> 32));
    }

    @Override
    public String toString() {
        return "matrix(" + a + " " + b + " " + c + " " + d + " " + e + " " + f + ")";
    }
}
