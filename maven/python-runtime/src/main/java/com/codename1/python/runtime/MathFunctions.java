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

/// The `math` functions beyond the elementary ones: hyperbolic, error and
/// gamma functions, float decomposition and the integer combinatorics.
/// Device class libraries have no `Math.sinh`/`cbrt`/`expm1`, so each is
/// computed here from `exp` and `log`, with the series a small argument
/// needs to keep its precision.
final class MathFunctions {
    private static final double LN2 = 0.6931471805599453;

    private MathFunctions() {
    }

    private interface D1 {
        double f(double x);
    }

    private static void fn(PyModule m, String name, final D1 f) {
        fn(m, name, f, null);
    }

    /// `domain` is the ValueError message for an argument outside the
    /// function's domain, with `%s` for the argument.
    private static void fn(PyModule m, final String name, final D1 f, final String domain) {
        m.setAttr(name, PyBuiltin.positional(name, a -> {
            if (a.length != 1) {
                throw Py.typeError("math." + name + "() takes exactly one argument (" + a.length + " given)");
            }
            double x = Py.asDouble(a[0]);
            double r = f.f(x);
            if (Double.isNaN(r) && !Double.isNaN(x)) {
                if (domain != null) {
                    int at = domain.indexOf("%s");
                    throw Py.valueError(domain.substring(0, at) + Py.toRepr(Py.ofFloat(x)) + domain.substring(at + 2));
                }
                throw Py.valueError("math domain error");
            }
            if (Double.isInfinite(r) && !Double.isInfinite(x)) {
                throw Py.error(Py.OVERFLOW_ERROR, "math range error");
            }
            return Py.ofFloat(r);
        }));
    }

    static double expm1(double x) {
        if (x == 0) {
            // Keeps the sign of a zero, as the C library does.
            return x;
        }
        if (Math.abs(x) < 1e-5) {
            return x + x * x / 2 + x * x * x / 6;
        }
        if (Math.abs(x) < 0.5) {
            // exp(x)-1 = 2 tanh(x/2) / (1 - tanh(x/2)), no cancellation.
            double u = MathUtil.exp(x);
            if (u == 1.0) {
                return x;
            }
            double um1 = u - 1.0;
            return um1 * x / MathUtil.log(u);
        }
        return MathUtil.exp(x) - 1;
    }

    static double log1p(double x) {
        if (x == 0) {
            // Keeps the sign of a zero, as the C library does.
            return x;
        }
        if (x == -1) {
            return Double.NEGATIVE_INFINITY;
        }
        if (x < -1) {
            return Double.NaN;
        }
        double u = 1.0 + x;
        if (u == 1.0) {
            return x;
        }
        return MathUtil.log(u) * x / (u - 1.0);
    }

    private static double sinh(double x) {
        if (x == 0) {
            // Keeps the sign of a zero, as the C library does.
            return x;
        }
        if (Math.abs(x) < 0.5) {
            double e = expm1(Math.abs(x));
            double r = (e + e / (e + 1)) / 2;
            return x < 0 ? -r : r;
        }
        double e = MathUtil.exp(Math.abs(x));
        double r = (e - 1 / e) / 2;
        return x < 0 ? -r : r;
    }

    private static double cosh(double x) {
        double e = MathUtil.exp(Math.abs(x));
        return (e + 1 / e) / 2;
    }

    private static double tanh(double x) {
        if (x == 0) {
            // Keeps the sign of a zero, as the C library does.
            return x;
        }
        double ax = Math.abs(x);
        if (ax > 22) {
            return x < 0 ? -1 : 1;
        }
        double e = expm1(2 * ax);
        double r = e / (e + 2);
        return x < 0 || (x == 0 && 1 / x < 0) ? -r : r;
    }

    private static double asinh(double x) {
        if (x == 0) {
            // Keeps the sign of a zero, as the C library does.
            return x;
        }
        double ax = Math.abs(x);
        double r;
        if (ax > 1e8) {
            r = MathUtil.log(ax) + LN2;
        } else if (ax > 2) {
            r = MathUtil.log(2 * ax + 1 / (Math.sqrt(x * x + 1) + ax));
        } else {
            double t = x * x;
            r = log1p(ax + t / (1 + Math.sqrt(1 + t)));
        }
        return x < 0 ? -r : r;
    }

    private static double acosh(double x) {
        if (x < 1) {
            return Double.NaN;
        }
        if (x > 1e8) {
            return MathUtil.log(x) + LN2;
        }
        if (x > 2) {
            return MathUtil.log(2 * x - 1 / (x + Math.sqrt(x * x - 1)));
        }
        double t = x - 1;
        return log1p(t + Math.sqrt(2 * t + t * t));
    }

    private static double atanh(double x) {
        if (x == 0) {
            // Keeps the sign of a zero, as the C library does.
            return x;
        }
        double ax = Math.abs(x);
        if (ax >= 1) {
            return Double.NaN;
        }
        double r;
        if (ax < 0.5) {
            double t = ax + ax;
            r = 0.5 * log1p(t + t * ax / (1 - ax));
        } else {
            r = 0.5 * log1p((ax + ax) / (1 - ax));
        }
        return x < 0 ? -r : r;
    }

    private static double cbrt(double x) {
        if (x == 0 || Double.isNaN(x) || Double.isInfinite(x)) {
            return x;
        }
        double ax = Math.abs(x);
        double y = MathUtil.pow(ax, 1.0 / 3);
        y = y - (y * y * y - ax) / (3 * y * y);
        return x < 0 ? -y : y;
    }

    private static final double LANCZOS_G = 6.024680040776729583740234375;
    private static final double LANCZOS_G_MINUS_HALF = 5.524680040776729583740234375;
    private static final double[] LANCZOS_NUM = {
        23531376880.410759688572007674451636754734846804940,
        42919803642.649098768957899047001988850926355848959,
        35711959237.355668049440185451547166705960488635843,
        17921034426.037209699919755754458931112671403265390,
        6039542586.3520280050642916443072979210699388420708,
        1439720407.3117216736632230727949123939715485786772,
        248874557.86205415651146038641322942321632125127801,
        31426415.585400194380614231628318205362874684987640,
        2876370.6289353724412254090516208496135991145378768,
        186056.26539522349504029498971604569928220784236328,
        8071.6720023658162106380029022722506138218516325024,
        210.82427775157934587250973392071336271166969580291,
        2.5066282746310002701649081771338373386264310793408,
    };
    private static final double[] LANCZOS_DEN = {
        0.0, 39916800.0, 120543840.0, 150917976.0, 105258076.0, 45995730.0,
        13339535.0, 2637558.0, 357423.0, 32670.0, 1925.0, 66.0, 1.0,
    };
    private static final double[] GAMMA_INTEGRAL = {
        1.0, 1.0, 2.0, 6.0, 24.0, 120.0, 720.0, 5040.0, 40320.0, 362880.0, 3628800.0, 39916800.0,
        479001600.0, 6227020800.0, 87178291200.0, 1307674368000.0, 20922789888000.0, 355687428096000.0,
        6402373705728000.0, 121645100408832000.0, 2432902008176640000.0, 51090942171709440000.0,
        1124000727777607680000.0,
    };

    private static double lanczosSum(double x) {
        double num = 0;
        double den = 0;
        if (x < 5) {
            for (int i = 12; i >= 0; i--) {
                num = num * x + LANCZOS_NUM[i];
                den = den * x + LANCZOS_DEN[i];
            }
        } else {
            for (int i = 0; i < 13; i++) {
                num = num / x + LANCZOS_NUM[i];
                den = den / x + LANCZOS_DEN[i];
            }
        }
        return num / den;
    }

    private static double sinpi(double x) {
        double y = Math.abs(x) % 2.0;
        long n = Math.round(2.0 * y);
        double r;
        switch ((int) n) {
            case 0:
                r = Math.sin(Math.PI * y);
                break;
            case 1:
                r = Math.cos(Math.PI * (y - 0.5));
                break;
            case 2:
                r = Math.sin(Math.PI * (1.0 - y));
                break;
            case 3:
                r = -Math.cos(Math.PI * (y - 1.5));
                break;
            default:
                r = Math.sin(Math.PI * (y - 2.0));
                break;
        }
        return (x < 0 ? -1.0 : 1.0) * r;
    }

    static double gamma(double x) {
        if (Double.isNaN(x) || x == Double.POSITIVE_INFINITY) {
            return x;
        }
        if (x == Double.NEGATIVE_INFINITY || (x == Math.floor(x) && x <= 0)) {
            return Double.NaN;
        }
        if (x == Math.floor(x) && x <= GAMMA_INTEGRAL.length) {
            return GAMMA_INTEGRAL[(int) x - 1];
        }
        double absx = Math.abs(x);
        if (absx < 1e-20) {
            return 1.0 / x;
        }
        if (absx > 200.0) {
            if (x < 0) {
                return 0.0 / sinpi(x);
            }
            return Double.POSITIVE_INFINITY;
        }
        double y = absx + LANCZOS_G_MINUS_HALF;
        double q;
        if (absx > LANCZOS_G_MINUS_HALF) {
            q = y - absx;
            q = q - LANCZOS_G_MINUS_HALF;
        } else {
            q = y - LANCZOS_G_MINUS_HALF;
            q = q - absx;
        }
        double z = q * LANCZOS_G / y;
        double r;
        if (x < 0.0) {
            r = -Math.PI / sinpi(absx) / absx * MathUtil.exp(y) / lanczosSum(absx);
            r -= z * r;
            if (absx < 140.0) {
                r /= MathUtil.pow(y, absx - 0.5);
            } else {
                double sqrtpow = MathUtil.pow(y, absx / 2.0 - 0.25);
                r /= sqrtpow;
                r /= sqrtpow;
            }
        } else {
            r = lanczosSum(absx) / MathUtil.exp(y);
            r += z * r;
            if (absx < 140.0) {
                r *= MathUtil.pow(y, absx - 0.5);
            } else {
                double sqrtpow = MathUtil.pow(y, absx / 2.0 - 0.25);
                r *= sqrtpow;
                r *= sqrtpow;
            }
        }
        return r;
    }

    static double lgamma(double x) {
        if (Double.isNaN(x)) {
            return x;
        }
        if (Double.isInfinite(x)) {
            return Double.POSITIVE_INFINITY;
        }
        if (x == Math.floor(x) && x <= 2.0) {
            if (x <= 0.0) {
                return Double.NaN;
            }
            return 0.0;
        }
        double absx = Math.abs(x);
        if (absx < 1e-20) {
            return -MathUtil.log(absx);
        }
        double r = MathUtil.log(lanczosSum(absx)) - LANCZOS_G;
        r += (absx - 0.5) * (MathUtil.log(absx + LANCZOS_G - 0.5) - 1);
        if (x < 0.0) {
            r = 1.1447298858494002 - MathUtil.log(Math.abs(sinpi(absx))) - MathUtil.log(absx) - r;
        }
        return r;
    }

    private static double ldexp(double m, long e) {
        if (m == 0 || Double.isInfinite(m) || Double.isNaN(m)) {
            return m;
        }
        long bits = Double.doubleToLongBits(m);
        long me = ((bits >>> 52) & 0x7ff) - 1023;
        if (me == -1023) {
            // A subnormal m: make it normal first, which is exact.
            m *= 0x1p54;
            e -= 54;
            bits = Double.doubleToLongBits(m);
            me = ((bits >>> 52) & 0x7ff) - 1023;
        }
        if (e > 2200) {
            e = 2200;
        } else if (e < -2200) {
            e = -2200;
        }
        long ne = me + e;
        if (ne > 1023) {
            return m > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        if (ne < -1075) {
            // Below half the smallest subnormal: rounds to a signed zero.
            return m > 0 ? 0.0 : -0.0;
        }
        long mantissa = bits & 0x800fffffffffffffL;
        if (ne >= -1022) {
            return Double.longBitsToDouble(mantissa | ((ne + 1023) << 52));
        }
        // A subnormal result: build the exact value 2**64 times larger,
        // which is normal, and scale it down in one multiplication so it
        // is rounded exactly once.
        return Double.longBitsToDouble(mantissa | ((ne + 64 + 1023) << 52)) * 0x1p-64;
    }

    private static double ulp(double x) {
        if (Double.isNaN(x)) {
            return x;
        }
        x = Math.abs(x);
        if (Double.isInfinite(x)) {
            return x;
        }
        double next = nextUp(x);
        if (Double.isInfinite(next)) {
            return x - nextDown(x);
        }
        return next - x;
    }

    private static double nextUp(double x) {
        if (Double.isNaN(x) || x == Double.POSITIVE_INFINITY) {
            return x;
        }
        if (x == 0) {
            return Double.MIN_VALUE;
        }
        long b = Double.doubleToLongBits(x);
        return Double.longBitsToDouble(x > 0 ? b + 1 : b - 1);
    }

    private static double nextDown(double x) {
        return -nextUp(-x);
    }

    private static double remainder(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return Double.NaN;
        }
        if (Double.isInfinite(x) || y == 0) {
            return Double.NaN;
        }
        if (Double.isInfinite(y)) {
            return x;
        }
        double absx = Math.abs(x);
        double absy = Math.abs(y);
        double m = absx % absy;
        double c = absy - m;
        double r;
        if (m < c) {
            r = m;
        } else if (m > c) {
            r = -c;
        } else {
            // A tie: the quotient rounds to even.
            r = Math.floor(absx / absy) % 2 == 0 ? m : -c;
        }
        return x < 0 ? -r : r;
    }

    private static Object index(Object o) {
        if (Py.isInt(o)) {
            return o;
        }
        throw Py.typeError("'" + Py.typeOf(o).name + "' object cannot be interpreted as an integer");
    }

    static void install(final PyModule m) {
        fn(m, "sinh", MathFunctions::sinh);
        fn(m, "cosh", MathFunctions::cosh);
        fn(m, "tanh", MathFunctions::tanh);
        fn(m, "asinh", MathFunctions::asinh);
        fn(m, "acosh", MathFunctions::acosh, "expected argument value not less than 1, got %s");
        fn(m, "atanh", MathFunctions::atanh, "expected a number between -1 and 1, got %s");
        fn(m, "cbrt", MathFunctions::cbrt);
        fn(m, "exp2", x -> MathUtil.pow(2, x));
        fn(m, "expm1", MathFunctions::expm1);
        fn(m, "log1p", MathFunctions::log1p, "expected argument value > -1, got %s");
        fn(m, "erf", Erf::erf);
        fn(m, "erfc", Erf::erfc);
        fn(m, "gamma", MathFunctions::gamma, "expected a noninteger or positive integer, got %s");
        fn(m, "lgamma", MathFunctions::lgamma, "expected a noninteger or positive integer, got %s");
        m.setAttr("frexp", PyBuiltin.positional("frexp", a -> {
            double x = Py.asDouble(a[0]);
            if (x == 0 || Double.isNaN(x) || Double.isInfinite(x)) {
                return new PyTuple(new Object[] {Py.ofFloat(x), PyInt.of(0)});
            }
            int e = 0;
            double ax = Math.abs(x);
            if (ax < 0x1p-1000) {
                ax *= 0x1p100;
                e -= 100;
            }
            long bits = Double.doubleToLongBits(ax);
            int be = (int) ((bits >> 52) & 0x7ff);
            if (be == 0) {
                ax *= 0x1p60;
                e -= 60;
                bits = Double.doubleToLongBits(ax);
                be = (int) ((bits >> 52) & 0x7ff);
            }
            e += be - 1022;
            double mant = Double.longBitsToDouble((bits & 0x800fffffffffffffL) | (1022L << 52));
            return new PyTuple(new Object[] {Py.ofFloat(x < 0 ? -mant : mant), PyInt.of(e)});
        }));
        m.setAttr("ldexp", PyBuiltin.positional("ldexp", a -> {
            if (!Py.isInt(a[1])) {
                throw Py.typeError("Expected an int as second argument to ldexp.");
            }
            double x = Py.asDouble(a[0]);
            Object e = a[1];
            long n;
            com.codename1.util.BigInteger big = Py.intView(e).toBig();
            if (big.bitLength() > 62) {
                n = big.signum() < 0 ? -10000 : 10000;
            } else {
                n = big.longValue();
            }
            double r = ldexp(x, n);
            if (Double.isInfinite(r) && !Double.isInfinite(x)) {
                throw Py.error(Py.OVERFLOW_ERROR, "math range error");
            }
            return Py.ofFloat(r);
        }));
        m.setAttr("modf", PyBuiltin.positional("modf", a -> {
            double x = Py.asDouble(a[0]);
            if (Double.isInfinite(x)) {
                return new PyTuple(new Object[] {Py.ofFloat(x > 0 ? 0.0 : -0.0), Py.ofFloat(x)});
            }
            double ip = x < 0 ? Math.ceil(x) : Math.floor(x);
            double fp = x - ip;
            if (fp == 0 && x < 0 || (x == 0 && 1 / x < 0)) {
                fp = -0.0;
            }
            return new PyTuple(new Object[] {Py.ofFloat(fp), Py.ofFloat(ip)});
        }));
        m.setAttr("fma", PyBuiltin.positional("fma", a -> {
            // Not fused: device class libraries have no exact fma.
            double r = Py.asDouble(a[0]) * Py.asDouble(a[1]) + Py.asDouble(a[2]);
            return Py.ofFloat(r);
        }));
        m.setAttr("remainder", PyBuiltin.positional("remainder", a -> {
            double x = Py.asDouble(a[0]);
            double y = Py.asDouble(a[1]);
            double r = remainder(x, y);
            if (Double.isNaN(r) && !Double.isNaN(x) && !Double.isNaN(y)) {
                throw Py.valueError("math domain error");
            }
            return Py.ofFloat(r);
        }));
        m.setAttr("nextafter", new PyBuiltin("nextafter", (args, kw) -> {
            Args a = new Args("nextafter", args, kw).check(3, "x", "y", "steps");
            double x = Py.asDouble(a.get(0, "x", null));
            double y = Py.asDouble(a.get(1, "y", null));
            Object stepsArg = a.get(2, "steps", Py.NONE);
            if (stepsArg != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                long steps = Py.asLong(stepsArg);
                if (steps < 0) {
                    throw Py.valueError("steps must be a non-negative integer");
                }
                for (long i = 0; i < steps && x != y; i++) {
                    x = Double.isNaN(x) || Double.isNaN(y) ? Double.NaN : (y > x ? nextUp(x) : nextDown(x));
                }
                return Py.ofFloat(x);
            }
            if (Double.isNaN(x) || Double.isNaN(y)) {
                return Py.ofFloat(Double.NaN);
            }
            if (x == y) {
                return Py.ofFloat(y);
            }
            return Py.ofFloat(y > x ? nextUp(x) : nextDown(x));
        }));
        fn(m, "ulp", MathFunctions::ulp);
        m.setAttr("dist", PyBuiltin.positional("dist", a -> {
            Object[] p = Py.toArray(a[0]);
            Object[] q = Py.toArray(a[1]);
            if (p.length != q.length) {
                throw Py.valueError("both points must have the same number of dimensions");
            }
            double max = 0;
            double[] d = new double[p.length];
            for (int i = 0; i < p.length; i++) {
                d[i] = Math.abs(Py.asDouble(p[i]) - Py.asDouble(q[i]));
                max = Math.max(max, d[i]);
            }
            if (max == 0 || Double.isInfinite(max)) {
                return Py.ofFloat(max);
            }
            double s = 0;
            for (double o : d) {
                double v = o / max;
                s += v * v;
            }
            return Py.ofFloat(max * Math.sqrt(s));
        }));
        m.setAttr("lcm", PyBuiltin.positional("lcm", a -> {
            com.codename1.util.BigInteger r = com.codename1.util.BigInteger.ONE;
            for (int i = 0; i < a.length; i++) {
                com.codename1.util.BigInteger v = Py.intView(index(a[i])).toBig().abs();
                if (v.signum() == 0) {
                    return PyInt.of(0);
                }
                r = r.divide(r.gcd(v)).multiply(v);
            }
            return PyInt.of(r);
        }));
        m.setAttr("perm", PyBuiltin.positional("perm", a -> {
            long n = Py.asLong(index(a[0]));
            if (n < 0) {
                // CPython's wording: perm(n) is factorial(n).
                throw Py.valueError(a.length > 1 && a[1] != Py.NONE ? "n must be a non-negative integer"
                        : "factorial() not defined for negative values");
            }
            long k = n;
            if (a.length > 1 && a[1] != Py.NONE) {
                k = Py.asLong(index(a[1]));
                if (k < 0) {
                    throw Py.valueError("k must be a non-negative integer");
                }
            }
            if (k > n) {
                return PyInt.of(0);
            }
            com.codename1.util.BigInteger r = com.codename1.util.BigInteger.ONE;
            for (long i = n - k + 1; i <= n; i++) {
                r = r.multiply(com.codename1.util.BigInteger.valueOf(i));
            }
            return PyInt.of(r);
        }));
        m.setAttr("sumprod", PyBuiltin.positional("sumprod", a -> {
            Object[] p = Py.toArray(a[0]);
            Object[] q = Py.toArray(a[1]);
            if (p.length != q.length) {
                throw Py.valueError("Inputs are not the same length");
            }
            Object total = PyInt.of(0);
            for (int i = 0; i < p.length; i++) {
                total = Py.add(total, Py.mul(p[i], q[i]));
            }
            return total;
        }));
    }
}
