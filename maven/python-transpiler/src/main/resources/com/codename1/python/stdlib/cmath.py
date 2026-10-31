# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Mathematical functions over complex numbers."""

import math

__all__ = ["acos", "acosh", "asin", "asinh", "atan", "atanh", "cos", "cosh", "e", "exp", "inf", "infj",
           "isclose", "isfinite", "isinf", "isnan", "log", "log10", "nan", "nanj", "phase", "pi", "polar",
           "rect", "sin", "sinh", "sqrt", "tan", "tanh", "tau"]

pi = math.pi
e = math.e
tau = math.tau
inf = math.inf
nan = math.nan
infj = complex(0.0, inf)
nanj = complex(0.0, nan)


def _c(z):
    if isinstance(z, complex):
        return z
    if hasattr(z, "__complex__"):
        return z.__complex__()
    try:
        return complex(float(z), 0.0)
    except TypeError:
        raise TypeError("must be real number, not %s" % type(z).__name__)


def phase(z):
    z = _c(z)
    return math.atan2(z.imag, z.real)


def polar(z):
    z = _c(z)
    return (abs(z), math.atan2(z.imag, z.real))


def rect(r, phi):
    if math.isinf(r) and math.isfinite(phi) and phi != 0.0:
        re = math.copysign(math.inf, math.cos(phi)) if r > 0 else -math.copysign(math.inf, math.cos(phi))
        im = math.copysign(math.inf, math.sin(phi)) if r > 0 else -math.copysign(math.inf, math.sin(phi))
        return complex(re, im)
    if phi == 0.0:
        return complex(r, phi * r if math.isfinite(r) else phi)
    return complex(r * math.cos(phi), r * math.sin(phi))


def isfinite(z):
    z = _c(z)
    return math.isfinite(z.real) and math.isfinite(z.imag)


def isinf(z):
    z = _c(z)
    return math.isinf(z.real) or math.isinf(z.imag)


def isnan(z):
    z = _c(z)
    return math.isnan(z.real) or math.isnan(z.imag)


def isclose(a, b, *, rel_tol=1e-09, abs_tol=0.0):
    a = _c(a)
    b = _c(b)
    if rel_tol < 0.0 or abs_tol < 0.0:
        raise ValueError("tolerances must be non-negative")
    if a == b:
        return True
    if isinf(a) or isinf(b):
        return False
    diff = abs(b - a)
    return diff <= abs(rel_tol * b) or diff <= abs(rel_tol * a) or diff <= abs_tol


def exp(z):
    z = _c(z)
    if z.imag == 0.0:
        return complex(math.exp(z.real), z.imag)
    l = math.exp(z.real)
    return complex(l * math.cos(z.imag), l * math.sin(z.imag))


def sqrt(z):
    z = _c(z)
    x, y = z.real, z.imag
    if x == 0.0 and y == 0.0:
        return complex(0.0, y)
    if math.isinf(y):
        return complex(math.inf, y)
    if math.isnan(x) or math.isnan(y):
        return complex(math.nan, math.nan)
    if math.isinf(x):
        if x > 0:
            return complex(x, math.copysign(0.0, y))
        return complex(0.0, math.copysign(math.inf, y))
    ax = math.fabs(x)
    ay = math.fabs(y)
    s = 2.0 * math.sqrt(ax / 8.0 + math.hypot(ax / 8.0, ay / 8.0))
    d = ay / (2.0 * s)
    if x >= 0.0:
        return complex(s, math.copysign(d, y))
    return complex(d, math.copysign(s, y))


def log(z, base=None):
    z = _c(z)
    if z.real == 0.0 and z.imag == 0.0:
        raise ValueError("math domain error")
    r = complex(math.log(abs(z)), math.atan2(z.imag, z.real))
    if base is None:
        return r
    return r / log(base)


def log10(z):
    r = log(z)
    return complex(r.real / math.log(10.0), r.imag / math.log(10.0))


def sinh(z):
    z = _c(z)
    return complex(math.sinh(z.real) * math.cos(z.imag), math.cosh(z.real) * math.sin(z.imag))


def cosh(z):
    z = _c(z)
    return complex(math.cosh(z.real) * math.cos(z.imag), math.sinh(z.real) * math.sin(z.imag))


def tanh(z):
    z = _c(z)
    x, y = z.real, z.imag
    if math.fabs(x) > 354.0:
        return complex(math.copysign(1.0, x), 4.0 * math.sin(y) * math.cos(y) * math.exp(-2.0 * math.fabs(x)))
    tx = math.tanh(x)
    ty = math.tan(y)
    cx = 1.0 / math.cosh(x)
    txty = tx * ty
    denom = 1.0 + txty * txty
    return complex(tx * (1.0 + ty * ty) / denom, ((ty / denom) * cx) * cx)


def sin(z):
    z = _c(z)
    r = sinh(complex(-z.imag, z.real))
    return complex(r.imag, -r.real)


def cos(z):
    z = _c(z)
    return cosh(complex(-z.imag, z.real))


def tan(z):
    z = _c(z)
    r = tanh(complex(-z.imag, z.real))
    return complex(r.imag, -r.real)


def asinh(z):
    z = _c(z)
    s1 = sqrt(complex(1.0 + z.imag, -z.real))
    s2 = sqrt(complex(1.0 - z.imag, z.real))
    re = math.asinh(s1.real * s2.imag - s2.real * s1.imag)
    im = math.atan2(z.imag, s1.real * s2.real - s1.imag * s2.imag)
    return complex(re, im)


def asin(z):
    z = _c(z)
    r = asinh(complex(-z.imag, z.real))
    return complex(r.imag, -r.real)


def acos(z):
    z = _c(z)
    s1 = sqrt(complex(1.0 - z.real, -z.imag))
    s2 = sqrt(complex(1.0 + z.real, z.imag))
    re = 2.0 * math.atan2(s1.real, s2.real)
    im = math.asinh(s2.real * s1.imag - s2.imag * s1.real)
    return complex(re, im)


def acosh(z):
    z = _c(z)
    s1 = sqrt(complex(z.real - 1.0, z.imag))
    s2 = sqrt(complex(z.real + 1.0, z.imag))
    re = math.asinh(s1.real * s2.real + s1.imag * s2.imag)
    im = 2.0 * math.atan2(s1.imag, s2.real)
    return complex(re, im)


def atanh(z):
    z = _c(z)
    if z.real < 0.0:
        r = atanh(complex(-z.real, -z.imag))
        return complex(-r.real, -r.imag)
    x, y = z.real, z.imag
    ay = math.fabs(y)
    if x == 1.0 and ay < 1e-154:
        if ay == 0.0:
            raise ValueError("math domain error")
        re = -math.log(math.sqrt(ay) / math.sqrt(math.hypot(ay, 2.0)))
        im = math.copysign(math.atan2(2.0, -ay) / 2.0, y)
        return complex(re, im)
    re = math.log1p(4.0 * x / ((1.0 - x) * (1.0 - x) + ay * ay)) / 4.0
    im = -math.atan2(-2.0 * y, (1.0 - x) * (1.0 + x) - ay * ay) / 2.0
    return complex(re, im)


def atan(z):
    z = _c(z)
    r = atanh(complex(-z.imag, z.real))
    return complex(r.imag, -r.real)
