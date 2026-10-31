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

/// Python's `float`, an IEEE double.
public final class PyFloat extends PyObject {
    public final double value;

    public PyFloat(double value) {
        this.value = value;
    }

    @Override
    public PyType type() {
        return Py.FLOAT_TYPE;
    }

    @Override
    public String repr() {
        return repr(value);
    }

    /// CPython's `repr(float)`: the shortest text that round-trips, which Java's
    /// `Double.toString` also produces, re-spelled in Python's notation
    /// (`1e+16`, `1e-05`, `100.0`).
    public static String repr(double d) {
        if (Double.isNaN(d)) {
            return "nan";
        }
        if (Double.isInfinite(d)) {
            return d > 0 ? "inf" : "-inf";
        }
        if (d == 0) {
            return 1 / d < 0 ? "-0.0" : "0.0";
        }
        String j = Double.toString(Math.abs(d));
        String mant;
        int exp;
        int e = j.indexOf('E');
        if (e >= 0) {
            mant = j.substring(0, e);
            exp = Integer.parseInt(j.substring(e + 1));
        } else {
            mant = j;
            exp = 0;
        }
        int dot = mant.indexOf('.');
        String digits = mant.substring(0, dot) + mant.substring(dot + 1);
        int pointPos = dot + exp;
        int lead = 0;
        while (lead < digits.length() - 1 && digits.charAt(lead) == '0') {
            lead++;
            pointPos--;
        }
        digits = digits.substring(lead);
        int end = digits.length();
        while (end > 1 && digits.charAt(end - 1) == '0') {
            end--;
        }
        digits = digits.substring(0, end);
        // Double.toString is not always the shortest round trip before JDK 19
        // (JDK-4511638: 6.9175290276410819E18 for 6.917529027641082e+18), so a
        // long answer is shortened to the fewest digits that still read back
        // as the same double, which is what CPython prints. Subnormals are
        // always checked: the smallest one is 4.9E-324 there, 5e-324 here.
        if (digits.length() >= 16 || digits.length() > 1 && Math.abs(d) < 2.2250738585072014E-308) {
            double a = Math.abs(d);
            for (int k = 1; k < digits.length(); k++) {
                String r = roundDigits(digits, k);
                int shift = r.length() > k ? 1 : 0;
                r = r.substring(0, k);
                if (Double.parseDouble(r + "E" + (pointPos + shift - k)) == a) {
                    end = r.length();
                    while (end > 1 && r.charAt(end - 1) == '0') {
                        end--;
                    }
                    digits = r.substring(0, end);
                    pointPos += shift;
                    break;
                }
            }
        }
        int sciExp = pointPos - 1;
        StringBuilder sb = new StringBuilder();
        if (d < 0) {
            sb.append('-');
        }
        if (sciExp < -4 || sciExp >= 16) {
            sb.append(digits.charAt(0));
            if (digits.length() > 1) {
                sb.append('.').append(digits, 1, digits.length());
            }
            sb.append('e').append(sciExp < 0 ? '-' : '+');
            int a = Math.abs(sciExp);
            if (a < 10) {
                sb.append('0');
            }
            sb.append(a);
        } else if (pointPos <= 0) {
            sb.append("0.");
            for (int i = 0; i < -pointPos; i++) {
                sb.append('0');
            }
            sb.append(digits);
        } else if (pointPos >= digits.length()) {
            sb.append(digits);
            for (int i = digits.length(); i < pointPos; i++) {
                sb.append('0');
            }
            sb.append(".0");
        } else {
            sb.append(digits, 0, pointPos).append('.').append(digits, pointPos, digits.length());
        }
        return sb.toString();
    }

    /// The first `k` digits of `digits`, rounded half up on the next one; a
    /// carry out of the first digit makes the answer one digit longer.
    private static String roundDigits(String digits, int k) {
        char[] c = digits.substring(0, k).toCharArray();
        if (digits.charAt(k) >= '5') {
            int i = k - 1;
            while (i >= 0 && c[i] == '9') {
                c[i] = '0';
                i--;
            }
            if (i < 0) {
                return "1" + new String(c);
            }
            c[i]++;
        }
        return new String(c);
    }

    @Override
    public boolean isTrue() {
        return value != 0;
    }

    @Override
    public double asDouble() {
        return value;
    }

    @Override
    public Object toJava() {
        return Double.valueOf(value);
    }

    @Override
    public long hash() {
        return Hashing.ofDouble(value, this);
    }

    private static double num(Object o) {
        if (Py.isFloat(o)) {
            return Py.asDouble(o);
        }
        if (Py.isInt(o)) {
            return Py.asDouble(o);
        }
        return Double.NaN;
    }

    /// -1, 0 or 1 as this float is below, equal to or above number `o`, and
    /// 2 when either is NaN. An int is compared exactly, as CPython does:
    /// `2**53 + 1 == float(2**53 + 1)` is false although the conversion of
    /// the int to a float would round it to equal.
    private int cmpTo(Object o) {
        if (o instanceof Long || o instanceof PyInt && !((PyInt) o).isBig()) {
            return compareLong(value, Py.asLong(o));
        }
        double d = num(o);
        if (Double.isNaN(value) || Double.isNaN(d)) {
            return 2;
        }
        return value < d ? -1 : value > d ? 1 : 0;
    }

    /// The exact comparison of `d` with `v`: -1, 0, 1, or 2 for NaN.
    static int compareLong(double d, long v) {
        if (Double.isNaN(d)) {
            return 2;
        }
        if (d >= 9.223372036854775807E18) {
            return 1;
        }
        if (d < -9.223372036854775808E18) {
            return -1;
        }
        long f = (long) Math.floor(d);
        if (f < v) {
            return -1;
        }
        if (f > v) {
            return 1;
        }
        return d > f ? 1 : 0;
    }

    private static boolean isNum(Object o) {
        return Py.isFloat(o) || Py.isInt(o);
    }

    @Override
    public Object add(Object o) {
        return isNum(o) ? Py.ofFloat(value + num(o)) : null;
    }

    @Override
    public Object radd(Object o) {
        return add(o);
    }

    @Override
    public Object sub(Object o) {
        return isNum(o) ? Py.ofFloat(value - num(o)) : null;
    }

    @Override
    public Object rsub(Object o) {
        return isNum(o) ? Py.ofFloat(num(o) - value) : null;
    }

    @Override
    public Object mul(Object o) {
        return isNum(o) ? Py.ofFloat(value * num(o)) : null;
    }

    @Override
    public Object rmul(Object o) {
        return mul(o);
    }

    @Override
    public Object truediv(Object o) {
        if (!isNum(o)) {
            return null;
        }
        double d = num(o);
        if (d == 0) {
            throw Py.zeroDivision("division by zero");
        }
        return Py.ofFloat(value / d);
    }

    @Override
    public Object rtruediv(Object o) {
        return isNum(o) ? new PyFloat(num(o)).truediv(this) : null;
    }

    @Override
    public Object floordiv(Object o) {
        if (!isNum(o)) {
            return null;
        }
        double d = num(o);
        if (d == 0) {
            throw Py.zeroDivision("division by zero");
        }
        return Py.ofFloat(Math.floor(value / d));
    }

    @Override
    public Object rfloordiv(Object o) {
        return isNum(o) ? new PyFloat(num(o)).floordiv(this) : null;
    }

    @Override
    public Object mod(Object o) {
        if (!isNum(o)) {
            return null;
        }
        double d = num(o);
        if (d == 0) {
            throw Py.zeroDivision("division by zero");
        }
        double r = value % d;
        if (r != 0 && (r < 0) != (d < 0)) {
            r += d;
        }
        return Py.ofFloat(r);
    }

    @Override
    public Object rmod(Object o) {
        return isNum(o) ? new PyFloat(num(o)).mod(this) : null;
    }

    @Override
    public Object pow(Object o) {
        if (!isNum(o)) {
            return null;
        }
        double e = num(o);
        if (value == 0 && e < 0) {
            throw Py.zeroDivision("zero to a negative power");
        }
        double r = Math.pow(value, e);
        if (Double.isNaN(r) && !Double.isNaN(value) && !Double.isNaN(e)) {
            // CPython answers a complex number here, which is not supported.
            throw Py.valueError("complex numbers are not supported: negative number raised to a fractional power");
        }
        if (Double.isInfinite(r) && !Double.isInfinite(value) && !Double.isInfinite(e)) {
            throw Py.overflowError("(34, 'Result too large')");
        }
        return Py.ofFloat(r);
    }

    @Override
    public Object rpow(Object o) {
        return isNum(o) ? new PyFloat(num(o)).pow(this) : null;
    }

    @Override
    public Object neg() {
        return Py.ofFloat(-value);
    }

    @Override
    public Object pos() {
        return Double.valueOf(value);
    }

    @Override
    public Object eq(Object o) {
        if (!isNum(o)) {
            return null;
        }
        int c = cmpTo(o);
        return Py.bool(c == 0);
    }

    @Override
    public Object lt(Object o) {
        if (!isNum(o)) {
            return null;
        }
        int c = cmpTo(o);
        return Py.bool(c == -1);
    }

    @Override
    public Object le(Object o) {
        if (!isNum(o)) {
            return null;
        }
        int c = cmpTo(o);
        return Py.bool(c == -1 || c == 0);
    }

    @Override
    public Object gt(Object o) {
        if (!isNum(o)) {
            return null;
        }
        int c = cmpTo(o);
        return Py.bool(c == 1);
    }

    @Override
    public Object ge(Object o) {
        if (!isNum(o)) {
            return null;
        }
        int c = cmpTo(o);
        return Py.bool(c == 1 || c == 0);
    }
}
