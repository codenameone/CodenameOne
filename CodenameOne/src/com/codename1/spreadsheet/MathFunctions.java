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
package com.codename1.spreadsheet;

import com.codename1.util.MathUtil;

/// The math and trigonometry functions.
final class MathFunctions {
    static final String SCALAR_NAMES = "ABS ACOS ACOSH ACOT ACOTH ARABIC ASIN ASINH ATAN ATAN2 ATANH BASE CEILING"
            + " CEILING.MATH CEILING.PRECISE COMBIN COMBINA COS COSH COT COTH CSC CSCH DECIMAL DEGREES EVEN EXP FACT"
            + " FACTDOUBLE FLOOR FLOOR.MATH FLOOR.PRECISE INT ISO.CEILING LN LOG LOG10 MOD MROUND ODD PERMUT"
            + " PERMUTATIONA PI POWER QUOTIENT RADIANS RAND RANDBETWEEN ROMAN ROUND ROUNDDOWN ROUNDUP SEC SECH SIGN SIN"
            + " SINH SQRT SQRTPI TAN TANH TRUNC";

    static final String RANGE_NAMES = "GCD LCM MDETERM MINVERSE MMULT MULTINOMIAL MUNIT PRODUCT SERIESSUM SUM SUMIF"
            + " SUMIFS SUMPRODUCT SUMSQ SUMX2MY2 SUMX2PY2 SUMXMY2";

    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

    private MathFunctions() {
    }

    private static Double d(double v) {
        return Double.valueOf(v);
    }

    /// A quotient that is a whole number but for the last bits is that whole number: 0.15 /
    /// 0.05 is 3.0000000000000004 in binary, and CEILING(0.15, 0.05) is still 0.15.
    static double snap(double q) {
        double r = Math.floor(q + 0.5);
        if (Math.abs(q - r) <= 1e-9 * Math.max(1, Math.abs(q))) {
            return r;
        }
        return q;
    }

    static double atan2(double y, double x) {
        if (x > 0) {
            return MathUtil.atan(y / x);
        }
        if (x < 0) {
            return y >= 0 ? MathUtil.atan(y / x) + Math.PI : MathUtil.atan(y / x) - Math.PI;
        }
        if (y > 0) {
            return Math.PI / 2;
        }
        if (y < 0) {
            return -Math.PI / 2;
        }
        throw EvalError.DIV0;
    }

    static double ln(double x) {
        if (x <= 0) {
            throw EvalError.NUM;
        }
        return MathUtil.log(x);
    }

    static double fact(double x) {
        if (x < 0) {
            throw EvalError.NUM;
        }
        int n = (int) Math.floor(x);
        if (n > 170) {
            throw EvalError.NUM;
        }
        double r = 1;
        for (int i = 2; i <= n; i++) {
            r *= i;
        }
        return r;
    }

    static double combin(double nn, double kk) {
        double n = Math.floor(nn);
        double k = Math.floor(kk);
        if (n < 0 || k < 0 || k > n) {
            throw EvalError.NUM;
        }
        if (k > n - k) {
            k = n - k;
        }
        double r = 1;
        // past what a double holds the answer is #NUM!, and the loop is not left to count
        // to a bound given by the file
        for (double i = 1; i <= k; i++) {
            r = r * (n - k + i) / i;
            if (Double.isInfinite(r)) {
                throw EvalError.NUM;
            }
        }
        return Math.floor(r + 0.5);
    }

    private static double ceiling(double x, double sig) {
        if (sig == 0 || x == 0) {
            return 0;
        }
        if (x > 0 && sig < 0) {
            throw EvalError.NUM;
        }
        return Math.ceil(snap(x / sig)) * sig;
    }

    private static double floor(double x, double sig) {
        if (x == 0) {
            return 0;
        }
        if (sig == 0) {
            throw EvalError.DIV0;
        }
        if (x > 0 && sig < 0) {
            throw EvalError.NUM;
        }
        return Math.floor(snap(x / sig)) * sig;
    }

    /// Rounds towards or away from zero at a decimal place.
    static double roundDirected(double x, int places, boolean up) {
        if (x == 0 || places > 15) {
            return x;
        }
        double abs = Math.abs(x);
        double scale = Values.pow10(Math.abs(places));
        double scaled = snap(places >= 0 ? abs * scale : abs / scale);
        double r = up ? Math.ceil(scaled) : Math.floor(scaled);
        double result = places >= 0 ? r / scale : r * scale;
        return x < 0 ? -result : result;
    }

    static Object call(String name, Evaluator ev, Object[] a) {
        switch (Functions.index(SWITCH_1, name)) {
            case 0: // ABS
                return d(Math.abs(Evaluator.num(a, 0)));
            case 1: { // ACOS
                double x = Evaluator.num(a, 0);
                if (x < -1 || x > 1) {
                    throw EvalError.NUM;
                }
                return d(MathUtil.acos(x));
            }
            case 2: { // ACOSH
                double x = Evaluator.num(a, 0);
                if (x < 1) {
                    throw EvalError.NUM;
                }
                return d(MathUtil.log(x + Math.sqrt(x * x - 1)));
            }
            case 3: { // ACOT
                double x = Evaluator.num(a, 0);
                return d(x == 0 ? Math.PI / 2 : x > 0 ? MathUtil.atan(1 / x) : MathUtil.atan(1 / x) + Math.PI);
            }
            case 4: { // ACOTH
                double x = Evaluator.num(a, 0);
                if (Math.abs(x) <= 1) {
                    throw EvalError.NUM;
                }
                return d(0.5 * MathUtil.log((x + 1) / (x - 1)));
            }
            case 5: { // ASIN
                double x = Evaluator.num(a, 0);
                if (x < -1 || x > 1) {
                    throw EvalError.NUM;
                }
                return d(MathUtil.asin(x));
            }
            case 6: { // ASINH
                double x = Evaluator.num(a, 0);
                double r = MathUtil.log(Math.abs(x) + Math.sqrt(x * x + 1));
                return d(x < 0 ? -r : r);
            }
            case 7: // ATAN
                return d(MathUtil.atan(Evaluator.num(a, 0)));
            case 8: // ATAN2
                return d(atan2(Evaluator.num(a, 1), Evaluator.num(a, 0)));
            case 9: { // ATANH
                double x = Evaluator.num(a, 0);
                if (Math.abs(x) >= 1) {
                    throw EvalError.NUM;
                }
                return d(0.5 * MathUtil.log((1 + x) / (1 - x)));
            }
            case 10: // CEILING
                return d(ceiling(Evaluator.num(a, 0), Evaluator.num(a, 1, 1)));
            case 11: { // CEILING.MATH
                double x = Evaluator.num(a, 0);
                double sig = Math.abs(Evaluator.num(a, 1, 1));
                if (sig == 0) {
                    return d(0);
                }
                if (x < 0 && Evaluator.num(a, 2, 0) != 0) {
                    return d(-Math.ceil(snap(-x / sig)) * sig);
                }
                return d(Math.ceil(snap(x / sig)) * sig);
            }
            case 12: // CEILING.PRECISE
            case 13: { // ISO.CEILING
                double sig = Math.abs(Evaluator.num(a, 1, 1));
                return d(sig == 0 ? 0 : Math.ceil(snap(Evaluator.num(a, 0) / sig)) * sig);
            }
            case 14: // COMBIN
                return d(combin(Evaluator.num(a, 0), Evaluator.num(a, 1)));
            case 15: { // COMBINA
                double n = Math.floor(Evaluator.num(a, 0));
                double k = Math.floor(Evaluator.num(a, 1));
                if (n < 0 || k < 0) {
                    throw EvalError.NUM;
                }
                return d(n == 0 && k == 0 ? 1 : combin(n + k - 1, k));
            }
            case 16: // COS
                return d(Math.cos(Evaluator.num(a, 0)));
            case 17: // COSH
                return d(Math.cosh(Evaluator.num(a, 0)));
            case 18: { // COT
                double t = Math.tan(Evaluator.num(a, 0));
                if (t == 0) {
                    throw EvalError.DIV0;
                }
                return d(1 / t);
            }
            case 19: { // COTH
                double t = Math.tanh(Evaluator.num(a, 0));
                if (t == 0) {
                    throw EvalError.DIV0;
                }
                return d(1 / t);
            }
            case 20: { // CSC
                double t = Math.sin(Evaluator.num(a, 0));
                if (t == 0) {
                    throw EvalError.DIV0;
                }
                return d(1 / t);
            }
            case 21: { // CSCH
                double t = Math.sinh(Evaluator.num(a, 0));
                if (t == 0) {
                    throw EvalError.DIV0;
                }
                return d(1 / t);
            }
            case 22: // DEGREES
                return d(Evaluator.num(a, 0) * 180 / Math.PI);
            case 23: { // EVEN
                double x = Evaluator.num(a, 0);
                double r = Math.ceil(snap(Math.abs(x) / 2)) * 2;
                return d(x < 0 ? -r : r);
            }
            case 24: // EXP
                return d(MathUtil.exp(Evaluator.num(a, 0)));
            case 25: // FACT
                return d(fact(Evaluator.num(a, 0)));
            case 26: { // FACTDOUBLE
                double x = Math.floor(Evaluator.num(a, 0));
                if (x < -1 || x > 300) {
                    throw EvalError.NUM;
                }
                double r = 1;
                for (int i = (int) x; i > 1; i -= 2) {
                    r *= i;
                }
                return d(r);
            }
            case 27: // FLOOR
                return d(floor(Evaluator.num(a, 0), Evaluator.num(a, 1, 1)));
            case 28: { // FLOOR.MATH
                double x = Evaluator.num(a, 0);
                double sig = Math.abs(Evaluator.num(a, 1, 1));
                if (sig == 0) {
                    return d(0);
                }
                if (x < 0 && Evaluator.num(a, 2, 0) != 0) {
                    return d(-Math.floor(snap(-x / sig)) * sig);
                }
                return d(Math.floor(snap(x / sig)) * sig);
            }
            case 29: { // FLOOR.PRECISE
                double sig = Math.abs(Evaluator.num(a, 1, 1));
                return d(sig == 0 ? 0 : Math.floor(snap(Evaluator.num(a, 0) / sig)) * sig);
            }
            case 30: // INT
                return d(Math.floor(Evaluator.num(a, 0)));
            case 31: // LN
                return d(ln(Evaluator.num(a, 0)));
            case 32: { // LOG
                double base = Evaluator.num(a, 1, 10);
                if (base <= 0 || base == 1) {
                    throw base == 1 ? EvalError.DIV0 : EvalError.NUM;
                }
                double x = Evaluator.num(a, 0);
                if (base == 10) {
                    if (x <= 0) {
                        throw EvalError.NUM;
                    }
                    return d(MathUtil.log10(x));
                }
                return d(ln(x) / MathUtil.log(base));
            }
            case 33: { // LOG10
                double x = Evaluator.num(a, 0);
                if (x <= 0) {
                    throw EvalError.NUM;
                }
                return d(MathUtil.log10(x));
            }
            case 34: { // MOD
                double x = Evaluator.num(a, 0);
                double y = Evaluator.num(a, 1);
                if (y == 0) {
                    throw EvalError.DIV0;
                }
                double r = x - y * Math.floor(x / y);
                // the remainder of an exact multiple can come out as the divisor itself
                return d(Math.abs(r) >= Math.abs(y) ? 0 : r);
            }
            case 35: { // MROUND
                double x = Evaluator.num(a, 0);
                double m = Evaluator.num(a, 1);
                if (m == 0 || x == 0) {
                    return d(0);
                }
                if (x < 0 != m < 0) {
                    throw EvalError.NUM;
                }
                return d(Math.floor(snap(x / m) + 0.5) * m);
            }
            case 36: { // ODD
                double x = Evaluator.num(a, 0);
                double abs = Math.ceil(snap(Math.abs(x)));
                if (abs % 2 == 0) {
                    abs++;
                }
                return d(x < 0 ? -abs : abs);
            }
            case 37: { // PERMUT
                double n = Math.floor(Evaluator.num(a, 0));
                double k = Math.floor(Evaluator.num(a, 1));
                if (n < 0 || k < 0 || k > n) {
                    throw EvalError.NUM;
                }
                double r = 1;
                for (double i = n - k + 1; i <= n; i++) {
                    r *= i;
                    if (Double.isInfinite(r)) {
                        throw EvalError.NUM;
                    }
                }
                return d(r);
            }
            case 38: { // PERMUTATIONA
                double n = Math.floor(Evaluator.num(a, 0));
                double k = Math.floor(Evaluator.num(a, 1));
                if (n < 0 || k < 0) {
                    throw EvalError.NUM;
                }
                return d(n == 0 && k == 0 ? 1 : Evaluator.power(n, k));
            }
            case 39: // PI
                return d(Math.PI);
            case 40: // POWER
                return d(Evaluator.power(Evaluator.num(a, 0), Evaluator.num(a, 1)));
            case 41: { // QUOTIENT
                double y = Evaluator.num(a, 1);
                if (y == 0) {
                    throw EvalError.DIV0;
                }
                double q = Evaluator.num(a, 0) / y;
                return d(q < 0 ? Math.ceil(q) : Math.floor(q));
            }
            case 42: // RADIANS
                return d(Evaluator.num(a, 0) * Math.PI / 180);
            case 43: // RAND
                return d(Math.random());
            case 44: { // RANDBETWEEN
                double lo = Math.ceil(Evaluator.num(a, 0));
                double hi = Math.floor(Evaluator.num(a, 1));
                if (lo > hi) {
                    throw EvalError.NUM;
                }
                return d(lo + Math.floor(Math.random() * (hi - lo + 1)));
            }
            case 45: // ROUND
                return d(Values.round(Evaluator.num(a, 0), Evaluator.integer(a, 1, 0)));
            case 46: // ROUNDDOWN
            case 47: // TRUNC
                return d(roundDirected(Evaluator.num(a, 0), Evaluator.integer(a, 1, 0), false));
            case 48: // ROUNDUP
                return d(roundDirected(Evaluator.num(a, 0), Evaluator.integer(a, 1, 0), true));
            case 49: { // SEC
                double t = Math.cos(Evaluator.num(a, 0));
                if (t == 0) {
                    throw EvalError.DIV0;
                }
                return d(1 / t);
            }
            case 50: // SECH
                return d(1 / Math.cosh(Evaluator.num(a, 0)));
            case 51: { // SIGN
                double x = Evaluator.num(a, 0);
                return d(x > 0 ? 1 : x < 0 ? -1 : 0);
            }
            case 52: // SIN
                return d(Math.sin(Evaluator.num(a, 0)));
            case 53: // SINH
                return d(Math.sinh(Evaluator.num(a, 0)));
            case 54: { // SQRT
                double x = Evaluator.num(a, 0);
                if (x < 0) {
                    throw EvalError.NUM;
                }
                return d(Math.sqrt(x));
            }
            case 55: { // SQRTPI
                double x = Evaluator.num(a, 0);
                if (x < 0) {
                    throw EvalError.NUM;
                }
                return d(Math.sqrt(x * Math.PI));
            }
            case 56: // TAN
                return d(Math.tan(Evaluator.num(a, 0)));
            case 57: // TANH
                return d(Math.tanh(Evaluator.num(a, 0)));
            case 58: // BASE
                return base(a);
            case 59: // DECIMAL
                return decimal(Evaluator.text(a, 0), Evaluator.integer(a, 1));
            case 60: // ROMAN
                return roman(Evaluator.integer(a, 0));
            case 61: // ARABIC
                return arabic(Evaluator.text(a, 0));
            default:
                return range(name, ev, a);
        }
    }

    private static Object range(String name, Evaluator ev, Object[] a) {
        switch (Functions.index(SWITCH_2, name)) {
            case 0: // SUM
                return d(ev.numbers(a, 0).sum());
            case 1: { // SUMSQ
                DoubleList list = ev.numbers(a, 0);
                double total = 0;
                for (int i = 0; i < list.size; i++) {
                    total += list.data[i] * list.data[i];
                }
                return d(total);
            }
            case 2: { // PRODUCT
                DoubleList list = ev.numbers(a, 0);
                double product = 1;
                for (int i = 0; i < list.size; i++) {
                    product *= list.data[i];
                }
                return d(list.size == 0 ? 0 : product);
            }
            case 3: // SUMPRODUCT
                return sumProduct(ev, a);
            case 4: { // SUMIF
                if (a.length < 2) {
                    throw EvalError.VALUE;
                }
                boolean[][] mask = Criteria.mask(ev, a, 0, 2);
                Object[][] values = ev.shaped(Evaluator.has(a, 2) ? a[2] : a[0], mask.length, Evaluator.width2(mask));
                return d(maskedSum(mask, values));
            }
            case 5: { // SUMIFS
                if (a.length < 3) {
                    throw EvalError.VALUE;
                }
                boolean[][] mask = Criteria.mask(ev, a, 1, a.length);
                Object[][] values = ev.matrix(a[0]);
                // unlike SUMIF, which stretches what it sums to the size of what it tests
                if (values.length != mask.length || Evaluator.width(values) != Evaluator.width2(mask)) {
                    throw EvalError.VALUE;
                }
                return d(maskedSum(mask, values));
            }
            case 6: // GCD
            case 7: { // LCM
                DoubleList list = ev.numbers(a, 0);
                boolean gcd = "GCD".equals(name);
                double acc = gcd ? 0 : 1;
                for (int i = 0; i < list.size; i++) {
                    double v = Math.floor(list.data[i]);
                    if (v < 0 || v > 9.007199254740992E15) {
                        throw EvalError.NUM;
                    }
                    if (gcd) {
                        acc = gcd(acc, v);
                    } else {
                        acc = v == 0 || acc == 0 ? 0 : acc / gcd(acc, v) * v;
                    }
                }
                return d(acc);
            }
            case 8: { // MULTINOMIAL
                DoubleList list = ev.numbers(a, 0);
                double sum = 0;
                double denominator = 1;
                for (int i = 0; i < list.size; i++) {
                    double v = Math.floor(list.data[i]);
                    sum += v;
                    denominator *= fact(v);
                }
                return d(fact(sum) / denominator);
            }
            case 9: // SUMX2MY2
            case 10: // SUMX2PY2
            case 11: { // SUMXMY2
                if (a.length < 2) {
                    throw EvalError.VALUE;
                }
                Object[][] x = ev.matrix(a[0]);
                Object[][] y = ev.matrix(a[1]);
                if (x.length != y.length || Evaluator.width(x) != Evaluator.width(y)) {
                    throw EvalError.NA;
                }
                double total = 0;
                for (int r = 0; r < x.length; r++) {
                    for (int c = 0; c < x[r].length; c++) {
                        if (x[r][c] instanceof Double && y[r][c] instanceof Double) {
                            double p = ((Double) x[r][c]).doubleValue();
                            double q = ((Double) y[r][c]).doubleValue();
                            if ("SUMX2MY2".equals(name)) {
                                total += p * p - q * q;
                            } else if ("SUMX2PY2".equals(name)) {
                                total += p * p + q * q;
                            } else {
                                total += (p - q) * (p - q);
                            }
                        }
                    }
                }
                return d(total);
            }
            case 12: { // SERIESSUM
                double x = ev.scalarNum(a, 0);
                double n = ev.scalarNum(a, 1);
                double m = ev.scalarNum(a, 2);
                DoubleList coefficients = new DoubleList();
                ev.numbers(Evaluator.arg(a, 3), coefficients, false);
                double total = 0;
                for (int i = 0; i < coefficients.size; i++) {
                    total += coefficients.data[i] * MathUtil.pow(x, n + i * m);
                }
                return d(total);
            }
            case 13: { // MMULT
                if (a.length < 2) {
                    throw EvalError.VALUE;
                }
                double[][] x = numeric(ev.matrix(a[0]));
                double[][] y = numeric(ev.matrix(a[1]));
                if (x.length == 0 || y.length == 0 || x[0].length != y.length) {
                    throw EvalError.VALUE;
                }
                Object[][] out = new Object[x.length][y[0].length];
                for (int i = 0; i < x.length; i++) {
                    for (int j = 0; j < y[0].length; j++) {
                        double total = 0;
                        for (int k = 0; k < y.length; k++) {
                            total += x[i][k] * y[k][j];
                        }
                        out[i][j] = d(total);
                    }
                }
                return out;
            }
            case 14: { // MDETERM
                double[][] m = numeric(ev.matrix(Evaluator.arg(a, 0)));
                if (m.length == 0 || m.length != m[0].length) {
                    throw EvalError.VALUE;
                }
                return d(invert(m, null));
            }
            case 15: { // MINVERSE
                double[][] m = numeric(ev.matrix(Evaluator.arg(a, 0)));
                if (m.length == 0 || m.length != m[0].length) {
                    throw EvalError.VALUE;
                }
                double[][] inverse = new double[m.length][m.length];
                if (invert(m, inverse) == 0) {
                    throw EvalError.NUM;
                }
                Object[][] out = new Object[m.length][m.length];
                for (int i = 0; i < m.length; i++) {
                    for (int j = 0; j < m.length; j++) {
                        out[i][j] = d(inverse[i][j]);
                    }
                }
                return out;
            }
            case 16: { // MUNIT
                int n = (int) ev.scalarNum(a, 0);
                if (n < 1 || n > 4096) {
                    throw EvalError.VALUE;
                }
                Object[][] out = new Object[n][n];
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        out[i][j] = d(i == j ? 1 : 0);
                    }
                }
                return out;
            }
            default:
                return CellError.NAME;
        }
    }

    private static double maskedSum(boolean[][] mask, Object[][] values) {
        DoubleList list = new DoubleList();
        for (int r = 0; r < mask.length; r++) {
            for (int c = 0; c < mask[r].length; c++) {
                if (mask[r][c] && values[r][c] instanceof Double) {
                    list.add(((Double) values[r][c]).doubleValue());
                }
            }
        }
        return list.sum();
    }

    private static Object sumProduct(Evaluator ev, Object[] a) {
        if (a.length == 0) {
            throw EvalError.VALUE;
        }
        Object[][] first = ev.matrix(a[0]);
        int rows = first.length;
        int cols = Evaluator.width(first);
        double[][] product = new double[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                product[r][c] = 1;
            }
        }
        for (int i = 0; i < a.length; i++) {
            Object[][] m = i == 0 ? first : ev.matrix(a[i]);
            if (m.length != rows || Evaluator.width(m) != cols) {
                throw EvalError.VALUE;
            }
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Object v = m[r][c];
                    if (v instanceof CellError) {
                        throw EvalError.of((CellError) v);
                    }
                    // text and logical values count as zero here, unlike in SUM
                    product[r][c] *= v instanceof Double ? ((Double) v).doubleValue() : 0;
                }
            }
        }
        DoubleList list = new DoubleList();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                list.add(product[r][c]);
            }
        }
        return d(list.sum());
    }

    static double gcd(double x, double y) {
        while (y != 0) {
            double t = x - y * Math.floor(x / y);
            x = y;
            y = t;
        }
        return x;
    }

    static double[][] numeric(Object[][] m) {
        double[][] out = new double[m.length][Evaluator.width(m)];
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < out[r].length; c++) {
                Object v = m[r][c];
                if (v instanceof CellError) {
                    throw EvalError.of((CellError) v);
                }
                if (!(v instanceof Double)) {
                    throw EvalError.VALUE;
                }
                out[r][c] = ((Double) v).doubleValue();
            }
        }
        return out;
    }

    /// Gauss-Jordan elimination with partial pivoting. Answers the determinant and, when
    /// `inverse` is given and the determinant is not zero, fills it in.
    static double invert(double[][] source, double[][] inverse) {
        int n = source.length;
        double[][] m = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(source[i], 0, m[i], 0, n);
            if (inverse != null) {
                for (int j = 0; j < n; j++) {
                    inverse[i][j] = i == j ? 1 : 0;
                }
            }
        }
        double det = 1;
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(m[r][col]) > Math.abs(m[pivot][col])) {
                    pivot = r;
                }
            }
            if (Math.abs(m[pivot][col]) < 1e-14) {
                return 0;
            }
            if (pivot != col) {
                double[] t = m[pivot];
                m[pivot] = m[col];
                m[col] = t;
                if (inverse != null) {
                    t = inverse[pivot];
                    inverse[pivot] = inverse[col];
                    inverse[col] = t;
                }
                det = -det;
            }
            double p = m[col][col];
            det *= p;
            for (int j = 0; j < n; j++) {
                m[col][j] /= p;
                if (inverse != null) {
                    inverse[col][j] /= p;
                }
            }
            for (int r = 0; r < n; r++) {
                if (r != col) {
                    double f = m[r][col];
                    if (f != 0) {
                        for (int j = 0; j < n; j++) {
                            m[r][j] -= f * m[col][j];
                            if (inverse != null) {
                                inverse[r][j] -= f * inverse[col][j];
                            }
                        }
                    }
                }
            }
        }
        return det;
    }

    private static Object base(Object[] a) {
        double value = Math.floor(Evaluator.num(a, 0));
        int radix = Evaluator.integer(a, 1);
        int min = Evaluator.integer(a, 2, 0);
        if (value < 0 || value >= 9.007199254740992E15 || radix < 2 || radix > 36 || min < 0 || min > 255) {
            throw EvalError.NUM;
        }
        long v = (long) value;
        StringBuilder sb = new StringBuilder();
        do {
            int digit = (int) (v % radix);
            sb.insert(0, (char) (digit < 10 ? '0' + digit : 'A' + digit - 10));
            v /= radix;
        } while (v > 0);
        while (sb.length() < min) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }

    private static Object decimal(String text, int radix) {
        if (radix < 2 || radix > 36) {
            throw EvalError.NUM;
        }
        double v = 0;
        String s = text.trim();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int digit;
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            } else if (c >= 'a' && c <= 'z') {
                digit = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'Z') {
                digit = c - 'A' + 10;
            } else {
                throw EvalError.NUM;
            }
            if (digit >= radix) {
                throw EvalError.NUM;
            }
            v = v * radix + digit;
        }
        return d(v);
    }

    private static Object roman(int n) {
        if (n < 0 || n > 3999) {
            throw EvalError.VALUE;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (n >= ROMAN_VALUES[i]) {
                sb.append(ROMAN_SYMBOLS[i]);
                n -= ROMAN_VALUES[i];
            }
        }
        return sb.toString();
    }

    private static Object arabic(String text) {
        String s = FormulaParser.asciiUpper(text.trim());
        boolean negative = s.startsWith("-");
        int total = 0;
        int previous = 0;
        for (int i = s.length() - 1; i >= (negative ? 1 : 0); i--) {
            int v;
            switch (s.charAt(i)) {
                case 'I':
                    v = 1;
                    break;
                case 'V':
                    v = 5;
                    break;
                case 'X':
                    v = 10;
                    break;
                case 'L':
                    v = 50;
                    break;
                case 'C':
                    v = 100;
                    break;
                case 'D':
                    v = 500;
                    break;
                case 'M':
                    v = 1000;
                    break;
                default:
                    throw EvalError.VALUE;
            }
            if (v < previous) {
                total -= v;
            } else {
                total += v;
                previous = v;
            }
        }
        return d(negative ? -total : total);
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "ABS", "ACOS", "ACOSH", "ACOT", "ACOTH", "ASIN", "ASINH", "ATAN", "ATAN2", "ATANH",
            "CEILING", "CEILING.MATH", "CEILING.PRECISE", "ISO.CEILING", "COMBIN", "COMBINA", "COS",
            "COSH", "COT", "COTH", "CSC", "CSCH", "DEGREES", "EVEN", "EXP", "FACT", "FACTDOUBLE",
            "FLOOR", "FLOOR.MATH", "FLOOR.PRECISE", "INT", "LN", "LOG", "LOG10", "MOD", "MROUND",
            "ODD", "PERMUT", "PERMUTATIONA", "PI", "POWER", "QUOTIENT", "RADIANS", "RAND",
            "RANDBETWEEN", "ROUND", "ROUNDDOWN", "TRUNC", "ROUNDUP", "SEC", "SECH", "SIGN", "SIN",
            "SINH", "SQRT", "SQRTPI", "TAN", "TANH", "BASE", "DECIMAL", "ROMAN", "ARABIC");

    private static final java.util.HashMap<String, Integer> SWITCH_2 = Functions.switchTable(
            "SUM", "SUMSQ", "PRODUCT", "SUMPRODUCT", "SUMIF", "SUMIFS", "GCD", "LCM", "MULTINOMIAL",
            "SUMX2MY2", "SUMX2PY2", "SUMXMY2", "SERIESSUM", "MMULT", "MDETERM", "MINVERSE", "MUNIT");
}
