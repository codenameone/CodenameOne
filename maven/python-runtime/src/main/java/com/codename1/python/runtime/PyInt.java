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

import com.codename1.util.BigInteger;

/// Python's `int`: arbitrary precision.
///
/// The value lives in a `long` and the arithmetic stays there until a result
/// overflows, when it moves to a [BigInteger]. A big value that comes back into
/// `long` range is normalised back, so `big == null` always means "fits".
public class PyInt extends PyObject {
    private static final int CACHE_LOW = -5;
    private static final int CACHE_HIGH = 256;
    private static final PyInt[] CACHE = new PyInt[CACHE_HIGH - CACHE_LOW + 1];
    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    static {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new PyInt(i + CACHE_LOW);
        }
    }

    public final long value;
    final BigInteger big;

    PyInt(long value) {
        this.value = value;
        this.big = null;
    }

    private PyInt(BigInteger big) {
        this.value = 0;
        this.big = big;
    }

    public static Object of(long v) {
        return Long.valueOf(v);
    }

    public static Object of(BigInteger b) {
        if (b.compareTo(LONG_MIN) >= 0 && b.compareTo(LONG_MAX) <= 0) {
            return of(b.longValue());
        }
        return new PyInt(b);
    }

    /// A PyInt for the PyInt behaviour of a value already known to be an int.
    static PyInt view(long v) {
        if (v >= CACHE_LOW && v <= CACHE_HIGH) {
            return CACHE[(int) v - CACHE_LOW];
        }
        return new PyInt(v);
    }

    /// Parses an integer literal or `int(str, base)` text.
    public static Object parse(String s, int base) {
        String t = s.trim().replace("_", "");
        boolean neg = false;
        if (t.startsWith("-") || t.startsWith("+")) {
            neg = t.charAt(0) == '-';
            t = t.substring(1);
        }
        if (base == 0 || base == 16 || base == 8 || base == 2) {
            String lower = t.length() > 1 ? t.substring(0, 2) : "";
            if ("0x".equals(lower) || "0X".equals(lower)) {
                base = 16;
                t = t.substring(2);
            } else if ("0o".equals(lower) || "0O".equals(lower)) {
                base = 8;
                t = t.substring(2);
            } else if ("0b".equals(lower) || "0B".equals(lower)) {
                base = 2;
                t = t.substring(2);
            } else if (base == 0) {
                base = 10;
            }
        }
        if (t.length() == 0) {
            throw Py.valueError("invalid literal for int() with base " + base + ": '" + s + "'");
        }
        for (int i = 0; i < t.length(); i++) {
            if (Character.digit(t.charAt(i), base) < 0) {
                throw Py.valueError("invalid literal for int() with base " + base + ": '" + s + "'");
            }
        }
        if (t.length() <= 12 || base == 10 && t.length() <= 18) {
            long v = Long.parseLong(t, base);
            return of(neg ? -v : v);
        }
        BigInteger b = new BigInteger(t, base);
        return of(neg ? b.negate() : b);
    }

    @Override
    public PyType type() {
        return Py.INT_TYPE;
    }

    public boolean isBig() {
        return big != null;
    }

    public BigInteger toBig() {
        return big != null ? big : BigInteger.valueOf(value);
    }

    @Override
    public String repr() {
        return big != null ? big.toString() : Long.toString(value);
    }

    @Override
    public boolean isTrue() {
        return big != null || value != 0;
    }

    @Override
    public long asLong() {
        if (big != null) {
            throw Py.overflowError("Python int too large to convert to C long");
        }
        return value;
    }

    @Override
    public double asDouble() {
        if (big == null) {
            return value;
        }
        double d = Double.parseDouble(big.toString());
        if (Double.isInfinite(d)) {
            throw Py.overflowError("int too large to convert to float");
        }
        return d;
    }

    @Override
    public Object toJava() {
        return big != null ? (Object) big : (Object) Long.valueOf(value);
    }

    @Override
    public long hash() {
        if (big != null) {
            return Hashing.ofBig(big);
        }
        return Hashing.ofLong(value);
    }

    @Override
    public Object add(Object o) {
        if (Py.isInt(o)) {
            PyInt b = Py.intView(o);
            if (big == null && b.big == null) {
                long r = value + b.value;
                if (((value ^ r) & (b.value ^ r)) < 0) {
                    return of(toBig().add(b.toBig()));
                }
                return of(r);
            }
            return of(toBig().add(b.toBig()));
        }
        if (Py.isFloat(o)) {
            return Py.ofFloat(asDouble() + Py.asDouble(o));
        }
        return null;
    }

    @Override
    public Object radd(Object o) {
        return add(o);
    }

    @Override
    public Object sub(Object o) {
        if (Py.isInt(o)) {
            PyInt b = Py.intView(o);
            if (big == null && b.big == null) {
                long r = value - b.value;
                if (((value ^ b.value) & (value ^ r)) < 0) {
                    return of(toBig().subtract(b.toBig()));
                }
                return of(r);
            }
            return of(toBig().subtract(b.toBig()));
        }
        if (Py.isFloat(o)) {
            return Py.ofFloat(asDouble() - Py.asDouble(o));
        }
        return null;
    }

    @Override
    public Object rsub(Object o) {
        if (Py.isFloat(o)) {
            return Py.ofFloat(Py.asDouble(o) - asDouble());
        }
        return Py.isInt(o) ? Py.intView(o).sub(this) : null;
    }

    @Override
    public Object mul(Object o) {
        if (Py.isInt(o)) {
            PyInt b = Py.intView(o);
            if (big == null && b.big == null) {
                long a = value;
                long c = b.value;
                long hi = Math.abs(a) | Math.abs(c);
                if ((hi >>> 31) == 0) {
                    return of(a * c);
                }
                long r = a * c;
                if (a != 0 && (r / a != c || a == -1 && c == Long.MIN_VALUE)) {
                    return of(toBig().multiply(b.toBig()));
                }
                return of(r);
            }
            return of(toBig().multiply(b.toBig()));
        }
        if (Py.isFloat(o)) {
            return Py.ofFloat(asDouble() * Py.asDouble(o));
        }
        if (o instanceof String || o instanceof PyList || o instanceof PyTuple) {
            return Py.obj(o).mul(this);
        }
        return null;
    }

    @Override
    public Object rmul(Object o) {
        return mul(o);
    }

    @Override
    public Object truediv(Object o) {
        if (Py.isInt(o)) {
            return Py.ofFloat(divide(toBig(), Py.intView(o).toBig()));
        }
        if (Py.isFloat(o)) {
            double d = Py.asDouble(o);
            if (d == 0) {
                throw Py.zeroDivision("division by zero");
            }
            return Py.ofFloat(asDouble() / d);
        }
        return null;
    }

    /// `a / b` correctly rounded, as CPython divides two ints: converting
    /// either to a double first rounds twice once it passes 2**53.
    static double divide(BigInteger a, BigInteger b) {
        if (b.signum() == 0) {
            throw Py.zeroDivision("division by zero");
        }
        boolean negative = a.signum() * b.signum() < 0;
        a = a.abs();
        b = b.abs();
        if (a.signum() == 0) {
            return negative ? -0.0 : 0.0;
        }
        // A quotient of 55 or 56 bits, with the remainder folded into its
        // lowest bit, rounds to 53 bits exactly as the full quotient would.
        int shift = 55 - (a.bitLength() - b.bitLength());
        BigInteger n = shift >= 0 ? a.shiftLeft(shift) : a;
        BigInteger d = shift >= 0 ? b : b.shiftLeft(-shift);
        BigInteger[] qr = n.divideAndRemainder(d);
        long q = qr[0].longValue();
        if (qr[1].signum() != 0) {
            q |= 1;
        }
        double r = (double) q;
        // Scale by 2**-shift in steps that stay exact.
        int e = -shift;
        while (e > 0) {
            int k = Math.min(e, 1000);
            r *= Double.longBitsToDouble((long) (1023 + k) << 52);
            e -= k;
            if (Double.isInfinite(r)) {
                throw Py.overflowError("integer division result too large for a float");
            }
        }
        while (e < 0) {
            int k = Math.min(-e, 1000);
            r *= Double.longBitsToDouble((long) (1023 - k) << 52);
            e += k;
        }
        return negative ? -r : r;
    }

    @Override
    public Object rtruediv(Object o) {
        if (Py.isFloat(o)) {
            return Py.obj(o).truediv(this);
        }
        return Py.isInt(o) ? Py.intView(o).truediv(this) : null;
    }

    @Override
    public Object floordiv(Object o) {
        if (Py.isInt(o)) {
            PyInt b = Py.intView(o);
            if (!b.isTrue()) {
                throw Py.zeroDivision("division by zero");
            }
            if (big == null && b.big == null && !(value == Long.MIN_VALUE && b.value == -1)) {
                return of(Math.floorDiv(value, b.value));
            }
            BigInteger[] qr = toBig().divideAndRemainder(b.toBig());
            BigInteger q = qr[0];
            if (qr[1].signum() != 0 && qr[1].signum() != b.toBig().signum()) {
                q = q.subtract(BigInteger.ONE);
            }
            return of(q);
        }
        if (Py.isFloat(o)) {
            return new PyFloat(asDouble()).floordiv(o);
        }
        return null;
    }

    @Override
    public Object rfloordiv(Object o) {
        if (Py.isFloat(o)) {
            return Py.obj(o).floordiv(Py.ofFloat(asDouble()));
        }
        return Py.isInt(o) ? Py.intView(o).floordiv(this) : null;
    }

    @Override
    public Object mod(Object o) {
        if (Py.isInt(o)) {
            PyInt b = Py.intView(o);
            if (!b.isTrue()) {
                throw Py.zeroDivision("division by zero");
            }
            if (big == null && b.big == null) {
                return of(Math.floorMod(value, b.value));
            }
            BigInteger r = toBig().remainder(b.toBig());
            if (r.signum() != 0 && r.signum() != b.toBig().signum()) {
                r = r.add(b.toBig());
            }
            return of(r);
        }
        if (Py.isFloat(o)) {
            return new PyFloat(asDouble()).mod(o);
        }
        return null;
    }

    @Override
    public Object rmod(Object o) {
        if (Py.isFloat(o)) {
            return Py.obj(o).mod(Py.ofFloat(asDouble()));
        }
        return Py.isInt(o) ? Py.intView(o).mod(this) : null;
    }

    @Override
    public Object pow(Object o) {
        if (Py.isInt(o)) {
            PyInt e = Py.intView(o);
            if (e.big != null) {
                throw Py.overflowError("exponent too large");
            }
            if (e.value < 0) {
                return new PyFloat(asDouble()).pow(Py.ofFloat(e.value));
            }
            if (e.value > Integer.MAX_VALUE) {
                throw Py.overflowError("exponent too large");
            }
            if (big == null) {
                try {
                    long result = 1;
                    long base = value;
                    long n = e.value;
                    while (n > 0) {
                        if ((n & 1) != 0) {
                            result = Math.multiplyExact(result, base);
                        }
                        n >>= 1;
                        if (n > 0) {
                            base = Math.multiplyExact(base, base);
                        }
                    }
                    return of(result);
                } catch (ArithmeticException overflow) {
                    // falls through to the arbitrary-precision path
                }
            }
            return of(toBig().pow((int) e.value));
        }
        if (Py.isFloat(o)) {
            return new PyFloat(asDouble()).pow(o);
        }
        return null;
    }

    @Override
    public Object rpow(Object o) {
        if (Py.isFloat(o)) {
            return Py.obj(o).pow(Py.ofFloat(asDouble()));
        }
        return Py.isInt(o) ? Py.intView(o).pow(this) : null;
    }

    @Override
    public Object lshift(Object o) {
        if (!(Py.isInt(o))) {
            return null;
        }
        long n = Py.asLong(o);
        if (n < 0) {
            throw Py.valueError("negative shift count");
        }
        if (big == null && n < 63 && (value == 0 || Math.abs(value) < (1L << (62 - n)))) {
            return of(value << n);
        }
        return of(toBig().shiftLeft((int) n));
    }

    @Override
    public Object rshift(Object o) {
        if (!(Py.isInt(o))) {
            return null;
        }
        long n = Py.asLong(o);
        if (n < 0) {
            throw Py.valueError("negative shift count");
        }
        if (big == null) {
            return of(n >= 64 ? (value < 0 ? -1 : 0) : value >> n);
        }
        return of(big.shiftRight((int) n));
    }

    @Override
    public Object and(Object o) {
        if (!(Py.isInt(o))) {
            return null;
        }
        PyInt b = Py.intView(o);
        if (big == null && b.big == null) {
            return Py.wrapIntResult(this, b, value & b.value);
        }
        return of(toBig().and(b.toBig()));
    }

    @Override
    public Object rand(Object o) {
        return and(o);
    }

    @Override
    public Object or(Object o) {
        if (!(Py.isInt(o))) {
            return null;
        }
        PyInt b = Py.intView(o);
        if (big == null && b.big == null) {
            return Py.wrapIntResult(this, b, value | b.value);
        }
        return of(toBig().or(b.toBig()));
    }

    @Override
    public Object ror(Object o) {
        return or(o);
    }

    @Override
    public Object xor(Object o) {
        if (!(Py.isInt(o))) {
            return null;
        }
        PyInt b = Py.intView(o);
        if (big == null && b.big == null) {
            return Py.wrapIntResult(this, b, value ^ b.value);
        }
        return of(toBig().xor(b.toBig()));
    }

    @Override
    public Object rxor(Object o) {
        return xor(o);
    }

    @Override
    public Object neg() {
        if (big == null && value != Long.MIN_VALUE) {
            return of(-value);
        }
        return of(toBig().negate());
    }

    @Override
    public Object pos() {
        return big == null ? of(value) : this;
    }

    @Override
    public Object invert() {
        return big == null ? of(~value) : of(big.not());
    }

    private int compare(PyInt b) {
        if (big == null && b.big == null) {
            return value < b.value ? -1 : (value == b.value ? 0 : 1);
        }
        return toBig().compareTo(b.toBig());
    }

    @Override
    public Object eq(Object o) {
        if (Py.isInt(o)) {
            return Py.bool(compare(Py.intView(o)) == 0);
        }
        if (Py.isFloat(o)) {
            if (big == null) {
                int c = PyFloat.compareLong(Py.asDouble(o), value);
                return Py.bool(c == 0);
            }
            return Py.bool(asDouble() == Py.asDouble(o));
        }
        return null;
    }

    @Override
    public Object lt(Object o) {
        if (Py.isInt(o)) {
            return Py.bool(compare(Py.intView(o)) < 0);
        }
        if (Py.isFloat(o)) {
            if (big == null) {
                int c = PyFloat.compareLong(Py.asDouble(o), value);
                return Py.bool(c == 1);
            }
            return Py.bool(asDouble() < Py.asDouble(o));
        }
        return null;
    }

    @Override
    public Object le(Object o) {
        if (Py.isInt(o)) {
            return Py.bool(compare(Py.intView(o)) <= 0);
        }
        if (Py.isFloat(o)) {
            if (big == null) {
                int c = PyFloat.compareLong(Py.asDouble(o), value);
                return Py.bool(c == 1 || c == 0);
            }
            return Py.bool(asDouble() <= Py.asDouble(o));
        }
        return null;
    }

    @Override
    public Object gt(Object o) {
        if (Py.isInt(o)) {
            return Py.bool(compare(Py.intView(o)) > 0);
        }
        if (Py.isFloat(o)) {
            if (big == null) {
                int c = PyFloat.compareLong(Py.asDouble(o), value);
                return Py.bool(c == -1);
            }
            return Py.bool(asDouble() > Py.asDouble(o));
        }
        return null;
    }

    @Override
    public Object ge(Object o) {
        if (Py.isInt(o)) {
            return Py.bool(compare(Py.intView(o)) >= 0);
        }
        if (Py.isFloat(o)) {
            if (big == null) {
                int c = PyFloat.compareLong(Py.asDouble(o), value);
                return Py.bool(c == -1 || c == 0);
            }
            return Py.bool(asDouble() >= Py.asDouble(o));
        }
        return null;
    }
}
