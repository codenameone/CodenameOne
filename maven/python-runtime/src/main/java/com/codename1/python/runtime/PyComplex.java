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

/// `complex`: a pair of doubles with CPython's arithmetic, repr and hash.
public final class PyComplex extends PyObject {
    /// CPython's `_PyHASH_IMAG`, the multiplier of the imaginary part's hash.
    private static final long HASH_IMAG = 1000003L;

    final double real;
    final double imag;

    public PyComplex(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }

    @Override
    public PyType type() {
        return Py.COMPLEX_TYPE;
    }

    private static String part(double d) {
        String s = PyFloat.repr(d);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    @Override
    public String repr() {
        if (real == 0 && 1 / real > 0) {
            return part(imag) + "j";
        }
        String im = part(imag);
        if (!im.startsWith("-")) {
            im = "+" + im;
        }
        return "(" + part(real) + im + "j)";
    }

    @Override
    public boolean isTrue() {
        return real != 0 || imag != 0;
    }

    @Override
    public long hash() {
        long h = Hashing.ofDouble(real, this) + HASH_IMAG * Hashing.ofDouble(imag, this);
        return h == -1 ? -2 : h;
    }

    /// `o` as a complex, or null when it is not a number.
    static PyComplex of(Object o) {
        if (o instanceof PyComplex) {
            return (PyComplex) o;
        }
        if (Py.isFloat(o) || Py.isInt(o)) {
            return new PyComplex(Py.asDouble(o), 0);
        }
        return null;
    }

    @Override
    public Object add(Object o) {
        PyComplex c = of(o);
        return c == null ? null : new PyComplex(real + c.real, imag + c.imag);
    }

    @Override
    public Object radd(Object o) {
        return add(o);
    }

    @Override
    public Object sub(Object o) {
        PyComplex c = of(o);
        return c == null ? null : new PyComplex(real - c.real, imag - c.imag);
    }

    @Override
    public Object rsub(Object o) {
        PyComplex c = of(o);
        return c == null ? null : new PyComplex(c.real - real, c.imag - imag);
    }

    private static PyComplex mul(PyComplex a, PyComplex b) {
        return new PyComplex(a.real * b.real - a.imag * b.imag, a.real * b.imag + a.imag * b.real);
    }

    @Override
    public Object mul(Object o) {
        PyComplex c = of(o);
        return c == null ? null : mul(this, c);
    }

    @Override
    public Object rmul(Object o) {
        return mul(o);
    }

    /// CPython's `_Py_c_quot`: Smith's algorithm, scaled by the larger part
    /// of the divisor so that neither product overflows early.
    private static PyComplex quot(PyComplex a, PyComplex b) {
        double absBr = Math.abs(b.real);
        double absBi = Math.abs(b.imag);
        if (absBr == 0 && absBi == 0) {
            throw Py.error(Py.ZERO_DIVISION_ERROR, "division by zero");
        }
        if (absBr >= absBi) {
            double ratio = b.imag / b.real;
            double denom = b.real + b.imag * ratio;
            return new PyComplex((a.real + a.imag * ratio) / denom, (a.imag - a.real * ratio) / denom);
        }
        if (absBi >= absBr) {
            double ratio = b.real / b.imag;
            double denom = b.real * ratio + b.imag;
            return new PyComplex((a.real * ratio + a.imag) / denom, (a.imag * ratio - a.real) / denom);
        }
        return new PyComplex(Double.NaN, Double.NaN);
    }

    @Override
    public Object truediv(Object o) {
        PyComplex c = of(o);
        return c == null ? null : quot(this, c);
    }

    @Override
    public Object rtruediv(Object o) {
        PyComplex c = of(o);
        return c == null ? null : quot(c, this);
    }

    private static PyComplex pow(PyComplex a, PyComplex b) {
        if (b.real == 0 && b.imag == 0) {
            return new PyComplex(1, 0);
        }
        if (a.real == 0 && a.imag == 0) {
            if (b.imag != 0 || b.real < 0) {
                throw Py.error(Py.ZERO_DIVISION_ERROR, "zero to a negative or complex power");
            }
            return new PyComplex(0, 0);
        }
        if (b.imag == 0 && b.real == Math.floor(b.real) && Math.abs(b.real) <= 100) {
            // CPython's c_powi: square-and-multiply for small integral powers.
            long n = (long) b.real;
            PyComplex r = new PyComplex(1, 0);
            PyComplex x = a;
            long m = Math.abs(n);
            while (m > 0) {
                if ((m & 1) != 0) {
                    r = mul(r, x);
                }
                x = mul(x, x);
                m >>= 1;
            }
            return n < 0 ? quot(new PyComplex(1, 0), r) : r;
        }
        double vabs = Math.hypot(a.real, a.imag);
        double len = Math.pow(vabs, b.real);
        double at = Math.atan2(a.imag, a.real);
        double phase = at * b.real;
        if (b.imag != 0) {
            len /= Math.exp(at * b.imag);
            phase += b.imag * Math.log(vabs);
        }
        return new PyComplex(len * Math.cos(phase), len * Math.sin(phase));
    }

    @Override
    public Object pow(Object o) {
        PyComplex c = of(o);
        return c == null ? null : pow(this, c);
    }

    @Override
    public Object rpow(Object o) {
        PyComplex c = of(o);
        return c == null ? null : pow(c, this);
    }

    @Override
    public Object neg() {
        return new PyComplex(-real, -imag);
    }

    @Override
    public Object pos() {
        return this;
    }

    @Override
    public Object eq(Object o) {
        if (o instanceof PyComplex) {
            PyComplex c = (PyComplex) o;
            return Py.bool(real == c.real && imag == c.imag);
        }
        if (Py.isFloat(o) || Py.isInt(o)) {
            return Py.bool(imag == 0 && Py.isTrue(Py.eq(Py.ofFloat(real), o)));
        }
        return null;
    }

    @Override
    public Object getAttrOrNull(String name) {
        if ("real".equals(name)) {
            return Py.ofFloat(real);
        }
        if ("imag".equals(name)) {
            return Py.ofFloat(imag);
        }
        return super.getAttrOrNull(name);
    }

    static double abs(PyComplex c) {
        if (Double.isInfinite(c.real) || Double.isInfinite(c.imag)) {
            return Double.POSITIVE_INFINITY;
        }
        double r = Math.hypot(c.real, c.imag);
        if (Double.isInfinite(r)) {
            throw Py.error(Py.OVERFLOW_ERROR, "absolute value too large");
        }
        return r;
    }

    static void install() {
        PyType t = Py.COMPLEX_TYPE;
        t.define("conjugate", PyBuiltin.method(t, "conjugate", a -> {
            PyComplex c = self(a[0]);
            return new PyComplex(c.real, -c.imag);
        }));
        t.define("__abs__", PyBuiltin.method(t, "__abs__", a -> Py.ofFloat(abs(self(a[0])))));
        t.define("__complex__", PyBuiltin.method(t, "__complex__", a -> self(a[0])));
        t.factory = (type, args, kw) -> {
            Args p = new Args("complex", args, kw).check(2, "real", "imag");
            Object re = p.get(0, "real", null);
            Object im = p.get(1, "imag", null);
            if (re instanceof String || re instanceof PyStr) {
                if (im != null) {
                    throw Py.typeError("complex() argument 'real' must be a real number, not str");
                }
                return parse(Py.toStr(re));
            }
            if (im instanceof String || im instanceof PyStr) {
                throw Py.typeError("complex() argument 'imag' must be a real number, not str");
            }
            PyComplex a = re == null ? new PyComplex(0, 0) : coerce(re, true);
            PyComplex b = im == null ? null : coerce(im, false);
            // CPython adds the other part only from an argument that is
            // itself complex, so complex(0, -0.0) keeps its negative zero.
            double r = a.real;
            double i = re instanceof PyComplex ? a.imag : 0;
            if (b != null) {
                r = im instanceof PyComplex ? r - b.imag : r;
                i = re instanceof PyComplex ? i + b.real : b.real;
            }
            return new PyComplex(r, i);
        };
    }

    private static PyComplex self(Object o) {
        if (!(o instanceof PyComplex)) {
            throw Py.typeError("descriptor requires a 'complex' object but received a '" + Py.typeOf(o).name + "'");
        }
        return (PyComplex) o;
    }

    private static PyComplex coerce(Object o, boolean first) {
        PyComplex c = of(o);
        if (c != null) {
            return c;
        }
        Object m = Py.typeOf(o).lookup("__complex__");
        if (m != null) {
            Object r = Py.obj(m).call(new Object[] {o}, Py.NO_KW);
            if (r instanceof PyComplex) {
                return (PyComplex) r;
            }
            throw Py.typeError("__complex__ returned non-complex (type " + Py.typeOf(r).name + ")");
        }
        m = Py.typeOf(o).lookup("__float__");
        if (m != null) {
            return new PyComplex(Py.asDouble(Py.obj(m).call(new Object[] {o}, Py.NO_KW)), 0);
        }
        throw Py.typeError(first ? "complex() argument must be a string or a number, not " + Py.typeOf(o).name
                : "complex() argument 'imag' must be a real number, not " + Py.typeOf(o).name);
    }

    /// `complex("1+2j")`: an optional pair of parentheses around a real,
    /// an imaginary, or a real followed by a signed imaginary part.
    static PyComplex parse(String text) {
        String s = text.trim();
        if (s.startsWith("(") && s.endsWith(")")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        try {
            if (s.isEmpty()) {
                throw new NumberFormatException();
            }
            char last = s.charAt(s.length() - 1);
            if (last != 'j' && last != 'J') {
                return new PyComplex(number(s), 0);
            }
            String body = s.substring(0, s.length() - 1);
            int split = -1;
            for (int i = body.length() - 1; i > 0; i--) {
                char c = body.charAt(i);
                char prev = body.charAt(i - 1);
                if ((c == '+' || c == '-') && prev != 'e' && prev != 'E') {
                    split = i;
                    break;
                }
            }
            if (split < 0) {
                return new PyComplex(0, imagPart(body));
            }
            return new PyComplex(number(body.substring(0, split)), imagPart(body.substring(split)));
        } catch (NumberFormatException | PyException e) {
            throw Py.valueError("complex() arg is a malformed string"); //NOPMD PreserveStackTrace - CPython's error has no cause
        }
    }

    private static double imagPart(String s) {
        if (s.isEmpty() || "+".equals(s)) {
            return 1;
        }
        if ("-".equals(s)) {
            return -1;
        }
        return number(s);
    }

    private static double number(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '_') {
                throw new NumberFormatException();
            }
        }
        return Py.asDouble(Builtins.toFloat(s));
    }
}
