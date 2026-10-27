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

import com.codename1.util.MathUtil;

/// The `math` functions over doubles, with CPython's domain and range
/// errors. The `math` module and typed code (which calls these directly
/// when the arguments are known numbers) share them, so both raise alike.
public final class PyMath {
    private PyMath() {
    }

    /// `ValueError: math domain error`.
    private static PyException domain() {
        return Py.valueError("math domain error");
    }

    private static double finite(double x, double r) {
        if (Double.isInfinite(r) && !Double.isInfinite(x) && !Double.isNaN(x)) {
            throw Py.error(Py.OVERFLOW_ERROR, "math range error");
        }
        return r;
    }

    public static double sqrt(double x) {
        if (x < 0) {
            throw domain();
        }
        return Math.sqrt(x);
    }

    public static double sin(double x) {
        if (Double.isInfinite(x)) {
            throw domain();
        }
        return Math.sin(x);
    }

    public static double cos(double x) {
        if (Double.isInfinite(x)) {
            throw domain();
        }
        return Math.cos(x);
    }

    public static double tan(double x) {
        if (Double.isInfinite(x)) {
            throw domain();
        }
        return Math.tan(x);
    }

    public static double asin(double x) {
        if (x < -1 || x > 1) {
            throw domain();
        }
        return MathUtil.asin(x);
    }

    public static double acos(double x) {
        if (x < -1 || x > 1) {
            throw domain();
        }
        return MathUtil.acos(x);
    }

    public static double atan(double x) {
        return MathUtil.atan(x);
    }

    public static double atan2(double y, double x) {
        return MathUtil.atan2(y, x);
    }

    public static double exp(double x) {
        return finite(x, MathUtil.exp(x));
    }

    public static double fabs(double x) {
        return Math.abs(x);
    }

    public static double log(double x) {
        if (x <= 0) {
            throw domain();
        }
        return MathUtil.log(x);
    }

    public static double log10(double x) {
        if (x <= 0) {
            throw domain();
        }
        return MathUtil.log10(x);
    }

    /// C's fmod, which is Java's `%` on doubles; CPython rejects a zero
    /// divisor and an infinite dividend.
    public static double fmod(double x, double y) {
        if (y == 0 && !Double.isNaN(x) || Double.isInfinite(x) && !Double.isNaN(y)) {
            throw domain();
        }
        return x % y;
    }
}
