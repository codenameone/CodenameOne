# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Rational numbers."""

import math
import numbers
import operator
import re

__all__ = ["Fraction"]

_HASH_MODULUS = (1 << 61) - 1
_HASH_INF = 314159

_RATIONAL_FORMAT = re.compile(r"""
    \A\s*
    (?P<sign>[-+]?)
    (?=\d|\.\d)
    (?P<num>\d*|\d+(_\d+)*)
    (?:
        (?:\s*/\s*(?P<denom>\d+(_\d+)*))?
    |
        (?:\.(?P<decimal>\d*|\d+(_\d+)*))?
        (?:E(?P<exp>[-+]?\d+(_\d+)*))?
    )
    \s*\Z
""", re.VERBOSE | re.IGNORECASE)


class Fraction(numbers.Rational):
    __slots__ = ("_numerator", "_denominator")

    def __new__(cls, numerator=0, denominator=None):
        self = object.__new__(cls)
        if denominator is None:
            if type(numerator) is int:
                self._numerator = numerator
                self._denominator = 1
                return self
            if isinstance(numerator, Fraction):
                self._numerator = numerator._numerator
                self._denominator = numerator._denominator
                return self
            if isinstance(numerator, numbers.Rational) and not isinstance(numerator, float):
                self._numerator = int(numerator.numerator)
                self._denominator = int(numerator.denominator)
                return self
            if isinstance(numerator, (float, int)) or hasattr(numerator, "as_integer_ratio"):
                if isinstance(numerator, int):
                    self._numerator = int(numerator)
                    self._denominator = 1
                    return self
                self._numerator, self._denominator = numerator.as_integer_ratio()
                return self
            if isinstance(numerator, str):
                m = _RATIONAL_FORMAT.match(numerator)
                if m is None:
                    raise ValueError("Invalid literal for Fraction: %r" % numerator)
                numerator = int(m.group("num") or "0")
                denom = m.group("denom")
                if denom:
                    denominator = int(denom)
                else:
                    denominator = 1
                    decimal = m.group("decimal")
                    if decimal:
                        decimal = decimal.replace("_", "")
                        scale = 10 ** len(decimal)
                        numerator = numerator * scale + int(decimal)
                        denominator *= scale
                    exp = m.group("exp")
                    if exp:
                        exp = int(exp)
                        if exp >= 0:
                            numerator *= 10 ** exp
                        else:
                            denominator *= 10 ** -exp
                if m.group("sign") == "-":
                    numerator = -numerator
            else:
                raise TypeError("argument should be a string or a Rational instance or have the "
                                "as_integer_ratio() method")
        elif type(numerator) is int and type(denominator) is int:
            pass
        elif isinstance(numerator, numbers.Rational) and isinstance(denominator, numbers.Rational) \
                and not isinstance(numerator, float) and not isinstance(denominator, float):
            numerator, denominator = (numerator.numerator * denominator.denominator,
                                      denominator.numerator * numerator.denominator)
        else:
            raise TypeError("both arguments should be Rational instances")
        if denominator == 0:
            raise ZeroDivisionError("Fraction(%s, 0)" % numerator)
        g = math.gcd(numerator, denominator)
        if denominator < 0:
            g = -g
        self._numerator = numerator // g
        self._denominator = denominator // g
        return self

    @classmethod
    def _from_coprime_ints(cls, numerator, denominator):
        obj = object.__new__(cls)
        obj._numerator = numerator
        obj._denominator = denominator
        return obj

    @classmethod
    def from_float(cls, f):
        if isinstance(f, numbers.Integral):
            return cls(f)
        if not isinstance(f, float):
            raise TypeError("%s.from_float() only takes floats, not %r (%s)" % (cls.__name__, f, type(f).__name__))
        return cls._from_coprime_ints(*f.as_integer_ratio())

    @classmethod
    def from_decimal(cls, dec):
        from decimal import Decimal
        if isinstance(dec, numbers.Integral):
            dec = Decimal(int(dec))
        elif not isinstance(dec, Decimal):
            raise TypeError("%s.from_decimal() only takes Decimals, not %r (%s)"
                            % (cls.__name__, dec, type(dec).__name__))
        return cls._from_coprime_ints(*dec.as_integer_ratio())

    @classmethod
    def from_number(cls, number):
        if isinstance(number, numbers.Rational) and not isinstance(number, float):
            return cls(number)
        if not isinstance(number, type) and hasattr(number, "as_integer_ratio"):
            return cls._from_coprime_ints(*number.as_integer_ratio())
        raise TypeError("%s.from_number() only takes numbers, not %r (%s)"
                        % (cls.__name__, number, type(number).__name__))

    def is_integer(self):
        return self._denominator == 1

    def as_integer_ratio(self):
        return (self._numerator, self._denominator)

    def limit_denominator(self, max_denominator=1000000):
        if max_denominator < 1:
            raise ValueError("max_denominator should be at least 1")
        if self._denominator <= max_denominator:
            return Fraction(self)
        p0, q0, p1, q1 = 0, 1, 1, 0
        n, d = self._numerator, self._denominator
        while True:
            a = n // d
            q2 = q0 + a * q1
            if q2 > max_denominator:
                break
            p0, q0, p1, q1 = p1, q1, p0 + a * p1, q2
            n, d = d, n - a * d
        k = (max_denominator - q0) // q1
        if 2 * d * (q0 + k * q1) <= self._denominator:
            return Fraction._from_coprime_ints(p1, q1)
        return Fraction._from_coprime_ints(p0 + k * p1, q0 + k * q1)

    @property
    def numerator(a):
        return a._numerator

    @property
    def denominator(a):
        return a._denominator

    def __repr__(self):
        return "%s(%s, %s)" % (self.__class__.__name__, self._numerator, self._denominator)

    def __str__(self):
        if self._denominator == 1:
            return str(self._numerator)
        return "%s/%s" % (self._numerator, self._denominator)

    def __format__(self, format_spec):
        if not format_spec:
            return str(self)
        m = re.fullmatch(r"(?:(?P<fill>.)?(?P<align>[<>=^]))?(?P<sign>[-+ ]?)(?P<alt>\#)?(?P<zero>0)?"
                         r"(?P<width>\d+)?(?P<thousands>[,_])?(?:\.(?P<prec>\d+))?(?P<type>[fF%])?", format_spec)
        if m is None or (m.group("type") is None and m.group("prec") is None):
            if m is not None and m.group("type") is None:
                return format(str(self), format_spec.replace(m.group("sign") or "\0", "") if False else format_spec)
            return format(float(self), format_spec)
        prec = int(m.group("prec") or 6)
        kind = m.group("type") or "f"
        value = self * 100 if kind == "%" else self
        neg = value < 0
        n = abs(value)
        scaled = n._numerator * 10 ** prec
        q, r = divmod(scaled, n._denominator)
        if 2 * r > n._denominator or (2 * r == n._denominator and q % 2 == 1):
            q += 1
        digits = str(q).rjust(prec + 1, "0")
        head = digits[:len(digits) - prec] if prec else digits
        if m.group("thousands"):
            sep = m.group("thousands")
            parts = []
            while len(head) > 3:
                parts.insert(0, head[-3:])
                head = head[:-3]
            parts.insert(0, head)
            head = sep.join(parts)
        body = head + ("." + digits[len(digits) - prec:] if prec else ("." if m.group("alt") else ""))
        if kind == "%":
            body += "%"
        sign = "-" if neg and q else ({"+": "+", " ": " "}.get(m.group("sign"), ""))
        width = int(m.group("width") or 0)
        fill = m.group("fill") or (" " if not m.group("zero") or m.group("align") else "0")
        align = m.group("align") or ("=" if m.group("zero") else ">")
        pad = max(0, width - len(sign) - len(body))
        if align == "<":
            return sign + body + fill * pad
        if align == "^":
            return fill * (pad // 2) + sign + body + fill * (pad - pad // 2)
        if align == "=":
            return sign + fill * pad + body
        return fill * pad + sign + body

    def _operator_fallbacks(monomorphic_operator, fallback_operator):
        def forward(a, b):
            if isinstance(b, Fraction):
                return monomorphic_operator(a, b)
            elif isinstance(b, int):
                return monomorphic_operator(a, Fraction._from_coprime_ints(b, 1))
            elif isinstance(b, float):
                return fallback_operator(float(a), b)
            elif isinstance(b, complex):
                return fallback_operator(complex(a), b)
            else:
                return NotImplemented

        def reverse(b, a):
            if isinstance(a, numbers.Rational) and not isinstance(a, float):
                return monomorphic_operator(Fraction(a), b)
            elif isinstance(a, numbers.Real):
                return fallback_operator(float(a), float(b))
            elif isinstance(a, numbers.Complex):
                return fallback_operator(complex(a), complex(b))
            else:
                return NotImplemented

        return forward, reverse

    def _add(a, b):
        na, da = a._numerator, a._denominator
        nb, db = b._numerator, b._denominator
        g = math.gcd(da, db)
        if g == 1:
            return Fraction._from_coprime_ints(na * db + da * nb, da * db)
        s = da // g
        t = na * (db // g) + nb * s
        g2 = math.gcd(t, g)
        if g2 == 1:
            return Fraction._from_coprime_ints(t, s * db)
        return Fraction._from_coprime_ints(t // g2, s * (db // g2))

    __add__, __radd__ = _operator_fallbacks(_add, operator.add)

    def _sub(a, b):
        na, da = a._numerator, a._denominator
        nb, db = b._numerator, b._denominator
        g = math.gcd(da, db)
        if g == 1:
            return Fraction._from_coprime_ints(na * db - da * nb, da * db)
        s = da // g
        t = na * (db // g) - nb * s
        g2 = math.gcd(t, g)
        if g2 == 1:
            return Fraction._from_coprime_ints(t, s * db)
        return Fraction._from_coprime_ints(t // g2, s * (db // g2))

    __sub__, __rsub__ = _operator_fallbacks(_sub, operator.sub)

    def _mul(a, b):
        na, da = a._numerator, a._denominator
        nb, db = b._numerator, b._denominator
        g1 = math.gcd(na, db)
        if g1 > 1:
            na //= g1
            db //= g1
        g2 = math.gcd(nb, da)
        if g2 > 1:
            nb //= g2
            da //= g2
        return Fraction._from_coprime_ints(na * nb, db * da)

    __mul__, __rmul__ = _operator_fallbacks(_mul, operator.mul)

    def _div(a, b):
        nb, db = b._numerator, b._denominator
        if nb == 0:
            raise ZeroDivisionError("Fraction(%s, 0)" % (db,))
        na, da = a._numerator, a._denominator
        g1 = math.gcd(na, nb)
        if g1 > 1:
            na //= g1
            nb //= g1
        g2 = math.gcd(db, da)
        if g2 > 1:
            da //= g2
            db //= g2
        n, d = na * db, nb * da
        if d < 0:
            n, d = -n, -d
        return Fraction._from_coprime_ints(n, d)

    __truediv__, __rtruediv__ = _operator_fallbacks(_div, operator.truediv)

    def _floordiv(a, b):
        return (a.numerator * b.denominator) // (a.denominator * b.numerator)

    __floordiv__, __rfloordiv__ = _operator_fallbacks(_floordiv, operator.floordiv)

    def _divmod(a, b):
        da, db = a.denominator, b.denominator
        div, n_mod = divmod(a.numerator * db, da * b.numerator)
        return div, Fraction(n_mod, da * db)

    __divmod__, __rdivmod__ = _operator_fallbacks(_divmod, divmod)

    def _mod(a, b):
        da, db = a.denominator, b.denominator
        return Fraction((a.numerator * db) % (b.numerator * da), da * db)

    __mod__, __rmod__ = _operator_fallbacks(_mod, operator.mod)

    def __pow__(a, b, modulo=None):
        if modulo is not None:
            return NotImplemented
        if isinstance(b, numbers.Rational) and not isinstance(b, float):
            if b.denominator == 1:
                power = b.numerator
                if power >= 0:
                    return Fraction._from_coprime_ints(a._numerator ** power, a._denominator ** power)
                elif a._numerator > 0:
                    return Fraction._from_coprime_ints(a._denominator ** -power, a._numerator ** -power)
                elif a._numerator == 0:
                    raise ZeroDivisionError("Fraction(%s, 0)" % (a._denominator ** -power,))
                else:
                    return Fraction._from_coprime_ints((-a._denominator) ** -power, (-a._numerator) ** -power)
            else:
                return float(a) ** float(b)
        elif isinstance(b, (float, complex)):
            return float(a) ** b
        else:
            return NotImplemented

    def __rpow__(b, a, modulo=None):
        if modulo is not None:
            return NotImplemented
        if b._denominator == 1 and b._numerator >= 0:
            return a ** b._numerator
        if isinstance(a, numbers.Rational) and not isinstance(a, float):
            return Fraction(a.numerator, a.denominator) ** b
        if b._denominator == 1:
            return a ** b._numerator
        return a ** float(b)

    def __pos__(a):
        return Fraction._from_coprime_ints(a._numerator, a._denominator)

    def __neg__(a):
        return Fraction._from_coprime_ints(-a._numerator, a._denominator)

    def __abs__(a):
        return Fraction._from_coprime_ints(abs(a._numerator), a._denominator)

    def __int__(a):
        if a._numerator < 0:
            return -(-a._numerator // a._denominator)
        return a._numerator // a._denominator

    def __trunc__(a):
        return a.__int__()

    def __floor__(a):
        return a._numerator // a._denominator

    def __ceil__(a):
        return -(-a._numerator // a._denominator)

    def __round__(self, ndigits=None):
        if ndigits is None:
            d = self._denominator
            floor, remainder = divmod(self._numerator, d)
            if remainder * 2 < d:
                return floor
            elif remainder * 2 > d:
                return floor + 1
            elif floor % 2 == 0:
                return floor
            return floor + 1
        shift = 10 ** abs(ndigits)
        if ndigits > 0:
            return Fraction(round(self * shift), shift)
        return Fraction(round(self / shift) * shift)

    def __float__(a):
        return a._numerator / a._denominator

    def __hash__(self):
        try:
            dinv = pow(self._denominator, -1, _HASH_MODULUS)
        except ValueError:
            hash_ = _HASH_INF
        else:
            hash_ = hash(hash(abs(self._numerator)) * dinv)
        result = hash_ if self._numerator >= 0 else -hash_
        return -2 if result == -1 else result

    def __eq__(a, b):
        if type(b) is int:
            return a._numerator == b and a._denominator == 1
        if isinstance(b, numbers.Rational) and not isinstance(b, float):
            return a._numerator == b.numerator and a._denominator == b.denominator
        if isinstance(b, numbers.Complex) and b.imag == 0:
            b = b.real
        if isinstance(b, float):
            if math.isnan(b) or math.isinf(b):
                return 0.0 == b
            return a == a.from_float(b)
        return NotImplemented

    def _richcmp(self, other, op):
        if isinstance(other, numbers.Rational) and not isinstance(other, float):
            return op(self._numerator * other.denominator, self._denominator * other.numerator)
        if isinstance(other, float):
            if math.isnan(other) or math.isinf(other):
                return op(0.0, other)
            return op(self, self.from_float(other))
        return NotImplemented

    def __lt__(a, b):
        return a._richcmp(b, operator.lt)

    def __gt__(a, b):
        return a._richcmp(b, operator.gt)

    def __le__(a, b):
        return a._richcmp(b, operator.le)

    def __ge__(a, b):
        return a._richcmp(b, operator.ge)

    def __bool__(a):
        return bool(a._numerator)

    def __reduce__(self):
        return (self.__class__, (self._numerator, self._denominator))

    def __copy__(self):
        if type(self) == Fraction:
            return self
        return self.__class__(self._numerator, self._denominator)

    def __deepcopy__(self, memo):
        if type(self) == Fraction:
            return self
        return self.__class__(self._numerator, self._denominator)
