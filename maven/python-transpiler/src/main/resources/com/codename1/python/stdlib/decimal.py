# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Decimal fixed and floating point arithmetic (the General Decimal
Arithmetic Specification), with the results and signals of CPython's
decimal module.

A coefficient is kept as a string of digits, the way the specification
describes it. Every operation the specification defines exactly is exact
here; exp, ln, log10 and non-integral powers are computed with twenty
guard digits and then rounded once, which agrees with CPython's correctly
rounded results except for an input whose true result lies within
1e-20 ulp of a rounding boundary. There is one context for the whole
program: an application is single threaded.
"""

import math
import numbers
from collections import namedtuple as _namedtuple

__all__ = ["Decimal", "Context", "DecimalTuple", "DefaultContext", "BasicContext", "ExtendedContext",
           "DecimalException", "Clamped", "InvalidOperation", "DivisionByZero", "Inexact", "Rounded", "Subnormal",
           "Overflow", "Underflow", "FloatOperation", "DivisionImpossible", "InvalidContext", "ConversionSyntax",
           "DivisionUndefined", "ROUND_DOWN", "ROUND_HALF_UP", "ROUND_HALF_EVEN", "ROUND_CEILING", "ROUND_FLOOR",
           "ROUND_UP", "ROUND_HALF_DOWN", "ROUND_05UP", "setcontext", "getcontext", "localcontext", "MAX_PREC",
           "MAX_EMAX", "MIN_EMIN", "MIN_ETINY", "HAVE_THREADS", "HAVE_CONTEXTVAR"]

__version__ = "1.70"
__libmpdec_version__ = "2.5.1"

ROUND_DOWN = "ROUND_DOWN"
ROUND_HALF_UP = "ROUND_HALF_UP"
ROUND_HALF_EVEN = "ROUND_HALF_EVEN"
ROUND_CEILING = "ROUND_CEILING"
ROUND_FLOOR = "ROUND_FLOOR"
ROUND_UP = "ROUND_UP"
ROUND_HALF_DOWN = "ROUND_HALF_DOWN"
ROUND_05UP = "ROUND_05UP"
_ROUNDINGS = (ROUND_DOWN, ROUND_HALF_UP, ROUND_HALF_EVEN, ROUND_CEILING, ROUND_FLOOR, ROUND_UP, ROUND_HALF_DOWN,
              ROUND_05UP)

HAVE_THREADS = True
HAVE_CONTEXTVAR = True
MAX_PREC = 999999999999999999
MAX_EMAX = 999999999999999999
MIN_EMIN = -999999999999999999
MIN_ETINY = MIN_EMIN - (MAX_PREC - 1)

DecimalTuple = _namedtuple("DecimalTuple", "sign digits exponent", module="decimal")


class DecimalException(ArithmeticError):
    def handle(self, context, *args):
        pass


class Clamped(DecimalException):
    pass


class InvalidOperation(DecimalException):
    def handle(self, context, *args):
        return _NaN


class ConversionSyntax(InvalidOperation):
    pass


class DivisionByZero(DecimalException, ZeroDivisionError):
    def handle(self, context, sign, *args):
        return _SignedInfinity[sign]


class DivisionImpossible(InvalidOperation):
    pass


class DivisionUndefined(InvalidOperation, ZeroDivisionError):
    pass


class Inexact(DecimalException):
    pass


class InvalidContext(InvalidOperation):
    pass


class Rounded(DecimalException):
    pass


class Subnormal(DecimalException):
    pass


class Overflow(Inexact, Rounded):
    def handle(self, context, sign, *args):
        if context.rounding in (ROUND_HALF_UP, ROUND_HALF_EVEN, ROUND_HALF_DOWN, ROUND_UP):
            return _SignedInfinity[sign]
        if sign == 0:
            if context.rounding == ROUND_CEILING:
                return _SignedInfinity[sign]
            return _dec(sign, "9" * context.prec, context.Emax - context.prec + 1)
        if context.rounding == ROUND_FLOOR:
            return _SignedInfinity[sign]
        return _dec(sign, "9" * context.prec, context.Emax - context.prec + 1)


class Underflow(Inexact, Rounded, Subnormal):
    pass


class FloatOperation(DecimalException, TypeError):
    pass


# The order CPython lists them in a context's repr.
_signals = [Clamped, InvalidOperation, DivisionByZero, Inexact, FloatOperation, Overflow, Rounded, Subnormal,
            Underflow]

_condition_map = {ConversionSyntax: InvalidOperation, DivisionImpossible: InvalidOperation,
                  DivisionUndefined: InvalidOperation, InvalidContext: InvalidOperation}

_rounding_names = {r: r for r in _ROUNDINGS}


class Context:
    def __init__(self, prec=None, rounding=None, Emin=None, Emax=None, capitals=None, clamp=None, flags=None,
                 traps=None, _ignored_flags=None):
        dc = DefaultContext if "DefaultContext" in globals() else None
        self.prec = prec if prec is not None else (dc.prec if dc else 28)
        self.rounding = rounding if rounding is not None else (dc.rounding if dc else ROUND_HALF_EVEN)
        self.Emin = Emin if Emin is not None else (dc.Emin if dc else -999999)
        self.Emax = Emax if Emax is not None else (dc.Emax if dc else 999999)
        self.capitals = capitals if capitals is not None else (dc.capitals if dc else 1)
        self.clamp = clamp if clamp is not None else (dc.clamp if dc else 0)
        self._ignored_flags = [] if _ignored_flags is None else _ignored_flags
        if traps is None:
            self.traps = dict(dc.traps) if dc else {s: False for s in _signals}
        elif not isinstance(traps, dict):
            self.traps = {s: s in traps for s in _signals}
        else:
            self.traps = {s: bool(traps.get(s, False)) for s in _signals}
        if flags is None:
            self.flags = {s: False for s in _signals}
        elif not isinstance(flags, dict):
            self.flags = {s: s in flags for s in _signals}
        else:
            self.flags = {s: bool(flags.get(s, False)) for s in _signals}

    def __setattr__(self, name, value):
        if name == "prec":
            if not isinstance(value, int):
                raise TypeError("an integer is required")
            if not 1 <= value <= MAX_PREC:
                raise ValueError("valid range for prec is [1, MAX_PREC]")
        elif name == "Emin":
            if not isinstance(value, int):
                raise TypeError("an integer is required")
            if not MIN_EMIN <= value <= 0:
                raise ValueError("valid range for Emin is [MIN_EMIN, 0]")
        elif name == "Emax":
            if not isinstance(value, int):
                raise TypeError("an integer is required")
            if not 0 <= value <= MAX_EMAX:
                raise ValueError("valid range for Emax is [0, MAX_EMAX]")
        elif name == "capitals" or name == "clamp":
            if value not in (0, 1):
                raise ValueError("valid values for %s are [0, 1]" % name)
        elif name == "rounding":
            if value not in _ROUNDINGS:
                raise TypeError("valid values for rounding are:\n  [ROUND_CEILING, ROUND_FLOOR, ROUND_UP, ROUND_DOWN,"
                                "\n   ROUND_HALF_UP, ROUND_HALF_DOWN, ROUND_HALF_EVEN,\n   ROUND_05UP]")
        object.__setattr__(self, name, value)

    def __repr__(self):
        return ("Context(prec=%d, rounding=%s, Emin=%d, Emax=%d, capitals=%d, clamp=%d, flags=[%s], traps=[%s])"
                % (self.prec, self.rounding, self.Emin, self.Emax, self.capitals, self.clamp,
                   ", ".join(s.__name__ for s in _signals if self.flags[s]),
                   ", ".join(s.__name__ for s in _signals if self.traps[s])))

    def clear_flags(self):
        for flag in self.flags:
            self.flags[flag] = False

    def clear_traps(self):
        for flag in self.traps:
            self.traps[flag] = False

    def _shallow_copy(self):
        return Context(self.prec, self.rounding, self.Emin, self.Emax, self.capitals, self.clamp, self.flags,
                       self.traps, self._ignored_flags)

    def copy(self):
        return Context(self.prec, self.rounding, self.Emin, self.Emax, self.capitals, self.clamp, self.flags.copy(),
                       self.traps.copy(), self._ignored_flags)

    __copy__ = copy

    def _raise_error(self, condition, explanation=None, *args):
        error = _condition_map.get(condition, condition)
        if error in self._ignored_flags:
            return error().handle(self, *args)
        self.flags[error] = True
        if not self.traps[error]:
            return condition().handle(self, *args)
        raise error([condition])

    def _quiet(self):
        """A copy that records flags but never raises, for internal steps."""
        c = self.copy()
        c.clear_traps()
        return c

    def Etiny(self):
        return int(self.Emin - self.prec + 1)

    def Etop(self):
        return int(self.Emax - self.prec + 1)

    def _set_rounding(self, type):
        rounding = self.rounding
        self.rounding = type
        return rounding

    def create_decimal(self, num="0"):
        if isinstance(num, str) and (num != num.strip() or "_" in num):
            return self._raise_error(ConversionSyntax, "trailing or leading whitespace and underscores are not "
                                                       "permitted.")
        d = Decimal(num, context=self)
        if d._isnan() and len(d._int) > self.prec - self.clamp:
            return self._raise_error(ConversionSyntax, "diagnostic info too long in NaN")
        return d._fix(self)

    def create_decimal_from_float(self, f):
        d = Decimal.from_float(f)
        return d._fix(self)

    def abs(self, a):
        return _convert(a, True).__abs__(context=self)

    def add(self, a, b):
        return _convert(a, True).__add__(b, context=self)

    def subtract(self, a, b):
        return _convert(a, True).__sub__(b, context=self)

    def multiply(self, a, b):
        return _convert(a, True).__mul__(b, context=self)

    def divide(self, a, b):
        return _convert(a, True).__truediv__(b, context=self)

    def divide_int(self, a, b):
        return _convert(a, True).__floordiv__(b, context=self)

    def remainder(self, a, b):
        return _convert(a, True).__mod__(b, context=self)

    def divmod(self, a, b):
        return _convert(a, True).__divmod__(b, context=self)

    def power(self, a, b, modulo=None):
        return _convert(a, True).__pow__(b, modulo, context=self)

    def plus(self, a):
        return _convert(a, True).__pos__(context=self)

    def minus(self, a):
        return _convert(a, True).__neg__(context=self)

    def sqrt(self, a):
        return _convert(a, True).sqrt(context=self)

    def exp(self, a):
        return _convert(a, True).exp(context=self)

    def ln(self, a):
        return _convert(a, True).ln(context=self)

    def log10(self, a):
        return _convert(a, True).log10(context=self)

    def quantize(self, a, b):
        return _convert(a, True).quantize(b, context=self)

    def compare(self, a, b):
        return _convert(a, True).compare(b, context=self)

    def max(self, a, b):
        return _convert(a, True).max(b, context=self)

    def min(self, a, b):
        return _convert(a, True).min(b, context=self)

    def normalize(self, a):
        return _convert(a, True).normalize(context=self)

    def fma(self, a, b, c):
        return _convert(a, True).fma(b, c, context=self)

    def to_sci_string(self, a):
        return _convert(a, True).__str__(context=self)

    def to_eng_string(self, a):
        return _convert(a, True).to_eng_string(context=self)

    def to_integral_value(self, a):
        return _convert(a, True).to_integral_value(context=self)

    to_integral = to_integral_value

    def to_integral_exact(self, a):
        return _convert(a, True).to_integral_exact(context=self)

    def is_zero(self, a):
        return _convert(a, True).is_zero()

    def is_signed(self, a):
        return _convert(a, True).is_signed()

    def copy_abs(self, a):
        return _convert(a, True).copy_abs()

    def copy_negate(self, a):
        return _convert(a, True).copy_negate()

    def number_class(self, a):
        return _convert(a, True).number_class(context=self)

    def radix(self):
        return Decimal(10)


DefaultContext = Context(prec=28, rounding=ROUND_HALF_EVEN, traps=[DivisionByZero, Overflow, InvalidOperation],
                         flags=[], Emax=999999, Emin=-999999, capitals=1, clamp=0)
BasicContext = Context(prec=9, rounding=ROUND_HALF_UP, traps=[DivisionByZero, Overflow, InvalidOperation, Clamped,
                                                             Underflow], flags=[])
ExtendedContext = Context(prec=9, rounding=ROUND_HALF_EVEN, traps=[], flags=[])

_current = None


def getcontext():
    global _current
    if _current is None:
        _current = DefaultContext.copy()
    return _current


def setcontext(context):
    global _current
    if context in (DefaultContext, BasicContext, ExtendedContext):
        context = context.copy()
        context.clear_flags()
    _current = context


class _ContextManager:
    def __init__(self, new_context):
        self.new_context = new_context.copy()

    def __enter__(self):
        self.saved_context = getcontext()
        setcontext(self.new_context)
        return self.new_context

    def __exit__(self, t, v, tb):
        setcontext(self.saved_context)


def localcontext(ctx=None, **kwargs):
    if ctx is None:
        ctx = getcontext()
    manager = _ContextManager(ctx)
    for key, value in kwargs.items():
        if key == "flags" or key == "traps":
            setattr(manager.new_context, key, {s: s in value for s in _signals})
        elif key in ("prec", "rounding", "Emin", "Emax", "capitals", "clamp"):
            setattr(manager.new_context, key, value)
        else:
            raise TypeError("'%s' is an invalid keyword argument for this function" % key)
    return manager


def _all_zeros(s, i):
    return s[i:].strip("0") == ""


def _exact_half(s, i):
    return i < len(s) and s[i] == "5" and _all_zeros(s, i + 1)


def _dec(sign, coefficient, exponent, special=False):
    self = object.__new__(Decimal)
    self._sign = sign
    self._int = coefficient
    self._exp = exponent
    self._is_special = special
    return self


def _convert(other, raiseit=False, allow_float=False):
    if isinstance(other, Decimal):
        return other
    if isinstance(other, int):
        return Decimal(other)
    if allow_float and isinstance(other, float):
        return Decimal.from_float(other)
    if raiseit:
        raise TypeError("conversion from %s to Decimal is not supported" % type(other).__name__)
    return NotImplemented


def _parse(s):
    """Answers (sign, digits, exponent, special) for a numeric string, or None."""
    s = s.strip()
    if "_" in s:
        for i, ch in enumerate(s):
            if ch == "_" and (i == 0 or i == len(s) - 1 or not s[i - 1].isdigit() or not s[i + 1].isdigit()):
                return None
        s = s.replace("_", "")
    sign = 0
    if s[:1] in ("+", "-"):
        sign = 1 if s[0] == "-" else 0
        s = s[1:]
    low = s.lower()
    if low in ("inf", "infinity"):
        return (sign, "0", "F", True)
    for prefix, kind in (("snan", "N"), ("nan", "n")):
        if low.startswith(prefix):
            payload = low[len(prefix):]
            if payload and not (payload.isdigit() and payload.isascii()):
                return None
            payload = payload.lstrip("0")
            return (sign, payload, kind, True)
    exp = 0
    epos = low.find("e")
    if epos >= 0:
        e = low[epos + 1:]
        low = low[:epos]
        if e[:1] in ("+", "-"):
            digits = e[1:]
        else:
            digits = e
        if not digits or not (digits.isdigit() and digits.isascii()):
            return None
        exp = int(e)
    if "." in low:
        intpart, _, frac = low.partition(".")
    else:
        intpart, frac = low, ""
    if not intpart and not frac:
        return None
    for part in (intpart, frac):
        if part and not (part.isdigit() and part.isascii()):
            return None
    digits = (intpart + frac).lstrip("0") or "0"
    return (sign, digits, exp - len(frac), False)


class Decimal:
    __slots__ = ("_exp", "_int", "_sign", "_is_special")

    def __new__(cls, value="0", context=None):
        self = object.__new__(cls)
        if isinstance(value, str):
            p = _parse(value)
            if p is None:
                if context is None:
                    context = getcontext()
                return context._raise_error(ConversionSyntax, "Invalid literal for Decimal: %r" % value)
            self._sign, self._int, self._exp, self._is_special = p
            return self
        if isinstance(value, int):
            self._sign = 1 if value < 0 else 0
            self._int = str(abs(value))
            self._exp = 0
            self._is_special = False
            return self
        if isinstance(value, Decimal):
            self._sign = value._sign
            self._int = value._int
            self._exp = value._exp
            self._is_special = value._is_special
            return self
        if isinstance(value, (list, tuple)):
            if len(value) != 3:
                raise ValueError("argument must be a sequence of length 3")
            if not (isinstance(value[0], int) and value[0] in (0, 1)):
                raise ValueError("sign must be an integer with the value 0 or 1")
            if value[2] == "F":
                self._int = "0"
                self._exp = value[2]
                self._is_special = True
            else:
                digits = []
                for digit in value[1]:
                    if isinstance(digit, int) and 0 <= digit <= 9:
                        if digits or digit != 0:
                            digits.append(digit)
                    else:
                        raise ValueError("The second value in the tuple must be composed of integers in the range "
                                         "0 through 9.")
                if value[2] in ("n", "N"):
                    self._int = "".join(map(str, digits))
                    self._exp = value[2]
                    self._is_special = True
                elif isinstance(value[2], int):
                    self._int = "".join(map(str, digits or [0]))
                    self._exp = value[2]
                    self._is_special = False
                else:
                    raise ValueError("The third value in the tuple must be an integer, or one of the strings "
                                     "'F', 'n', 'N'.")
            self._sign = value[0]
            return self
        if isinstance(value, float):
            if context is None:
                context = getcontext()
            context._raise_error(FloatOperation, "strict semantics for mixing floats and Decimals are enabled")
            value = Decimal.from_float(value)
            self._sign = value._sign
            self._int = value._int
            self._exp = value._exp
            self._is_special = value._is_special
            return self
        raise TypeError("conversion from %s to Decimal is not supported" % type(value).__name__)

    @classmethod
    def from_float(cls, f):
        if isinstance(f, int):
            sign = 0 if f >= 0 else 1
            k = 0
            coeff = str(abs(f))
        elif isinstance(f, float):
            if math.isinf(f) or math.isnan(f):
                return cls(repr(f))
            sign = 0 if math.copysign(1.0, f) == 1.0 else 1
            n, d = abs(f).as_integer_ratio()
            k = d.bit_length() - 1
            coeff = str(n * 5 ** k)
        else:
            raise TypeError("argument must be int or float.")
        result = _dec(sign, coeff, -k)
        if cls is Decimal:
            return result
        return cls(result)

    @classmethod
    def from_number(cls, number):
        if isinstance(number, (int, Decimal, float)):
            return cls(number) if not isinstance(number, float) else cls.from_float(number)
        raise TypeError("conversion from %s to Decimal is not supported" % type(number).__name__)

    # -- classification -------------------------------------------------

    def _isnan(self):
        if self._is_special:
            exp = self._exp
            if exp == "n":
                return 1
            elif exp == "N":
                return 2
        return 0

    def _isinfinity(self):
        if self._exp == "F":
            return -1 if self._sign else 1
        return 0

    def _check_nans(self, other=None, context=None):
        self_is_nan = self._isnan()
        other_is_nan = other._isnan() if other is not None else False
        if self_is_nan or other_is_nan:
            if context is None:
                context = getcontext()
            if self_is_nan == 2:
                return context._raise_error(InvalidOperation, "sNaN", self)
            if other_is_nan == 2:
                return context._raise_error(InvalidOperation, "sNaN", other)
            if self_is_nan:
                return self._fix_nan(context)
            return other._fix_nan(context)
        return 0

    def _compare_check_nans(self, other, context):
        if context is None:
            context = getcontext()
        if self._is_special or other._is_special:
            if self._isnan() == 2 or other._isnan() == 2:
                return context._raise_error(InvalidOperation, "comparison involving sNaN")
            if self._isnan() or other._isnan():
                return context._raise_error(InvalidOperation, "comparison involving NaN")
        return 0

    def __bool__(self):
        return self._is_special or self._int != "0"

    def _isinteger(self):
        if self._is_special:
            return False
        if self._exp >= 0:
            return True
        rest = self._int[self._exp:]
        return rest == "0" * len(rest)

    def _iseven(self):
        if not self or self._exp > 0:
            return True
        return self._int[-1 + self._exp] in "02468"

    def adjusted(self):
        try:
            return self._exp + len(self._int) - 1
        except TypeError:
            return 0

    def is_canonical(self):
        return True

    def is_finite(self):
        return not self._is_special

    def is_infinite(self):
        return self._exp == "F"

    def is_nan(self):
        return self._exp in ("n", "N")

    def is_qnan(self):
        return self._exp == "n"

    def is_snan(self):
        return self._exp == "N"

    def is_signed(self):
        return self._sign == 1

    def is_zero(self):
        return not self._is_special and self._int == "0"

    def is_normal(self, context=None):
        if self._is_special or not self:
            return False
        if context is None:
            context = getcontext()
        return context.Emin <= self.adjusted()

    def is_subnormal(self, context=None):
        if self._is_special or not self:
            return False
        if context is None:
            context = getcontext()
        return self.adjusted() < context.Emin

    def number_class(self, context=None):
        if self.is_snan():
            return "sNaN"
        if self.is_qnan():
            return "NaN"
        inf = self._isinfinity()
        if inf == 1:
            return "+Infinity"
        if inf == -1:
            return "-Infinity"
        if self.is_zero():
            return "-Zero" if self._sign else "+Zero"
        if self.is_subnormal(context):
            return "-Subnormal" if self._sign else "+Subnormal"
        return "-Normal" if self._sign else "+Normal"

    def canonical(self):
        return self

    def radix(self):
        return Decimal(10)

    def same_quantum(self, other, context=None):
        other = _convert(other, raiseit=True)
        if self._is_special or other._is_special:
            return (self.is_nan() and other.is_nan() or self.is_infinite() and other.is_infinite())
        return self._exp == other._exp

    # -- text --------------------------------------------------------------

    def __str__(self, eng=False, context=None):
        sign = ["", "-"][self._sign]
        if self._is_special:
            if self._exp == "F":
                return sign + "Infinity"
            elif self._exp == "n":
                return sign + "NaN" + self._int
            else:
                return sign + "sNaN" + self._int
        leftdigits = self._exp + len(self._int)
        if self._exp <= 0 and leftdigits > -6:
            dotplace = leftdigits
        elif not eng:
            dotplace = 1
        elif self._int == "0":
            dotplace = (leftdigits + 1) % 3 - 1
        else:
            dotplace = (leftdigits - 1) % 3 + 1
        if dotplace <= 0:
            intpart = "0"
            fracpart = "." + "0" * (-dotplace) + self._int
        elif dotplace >= len(self._int):
            intpart = self._int + "0" * (dotplace - len(self._int))
            fracpart = ""
        else:
            intpart = self._int[:dotplace]
            fracpart = "." + self._int[dotplace:]
        if leftdigits == dotplace:
            exp = ""
        else:
            if context is None:
                context = getcontext()
            exp = ["e", "E"][context.capitals] + "%+d" % (leftdigits - dotplace)
        return sign + intpart + fracpart + exp

    def to_eng_string(self, context=None):
        return self.__str__(eng=True, context=context)

    def __repr__(self):
        return "Decimal('%s')" % str(self)

    def as_tuple(self):
        if self._is_special:
            return DecimalTuple(self._sign, tuple(map(int, self._int)), self._exp)
        return DecimalTuple(self._sign, tuple(map(int, self._int)), self._exp)

    def as_integer_ratio(self):
        if self._is_special:
            if self._isnan():
                raise ValueError("cannot convert NaN to integer ratio")
            raise OverflowError("cannot convert Infinity to integer ratio")
        if not self:
            return 0, 1
        n = int(self._int)
        if self._exp >= 0:
            n, d = n * 10 ** self._exp, 1
        else:
            d5 = -self._exp
            while d5 > 0 and n % 5 == 0:
                n //= 5
                d5 -= 1
            d2 = -self._exp
            shift2 = min((n & -n).bit_length() - 1, d2)
            if shift2:
                n >>= shift2
                d2 -= shift2
            d = 5 ** d5 << d2
        if self._sign:
            n = -n
        return n, d

    # -- conversion --------------------------------------------------------

    def __int__(self):
        if self._is_special:
            if self._isnan():
                raise ValueError("cannot convert NaN to integer")
            raise OverflowError("cannot convert Infinity to integer")
        s = (-1) ** self._sign
        if self._exp >= 0:
            return s * int(self._int) * 10 ** self._exp
        return s * int(self._int[:self._exp] or "0")

    __trunc__ = __int__

    def __index__(self):
        raise TypeError("'decimal.Decimal' object cannot be interpreted as an integer")

    def __float__(self):
        if self._isnan():
            if self.is_snan():
                raise ValueError("cannot convert signaling NaN to float")
            s = "-nan" if self._sign else "nan"
        else:
            s = str(self)
        return float(s)

    def __complex__(self):
        return complex(float(self))

    @property
    def real(self):
        return self

    @property
    def imag(self):
        return Decimal(0)

    def conjugate(self):
        return self

    def __floor__(self):
        if self._is_special:
            if self.is_nan():
                raise ValueError("cannot round a NaN")
            raise OverflowError("cannot round an infinity")
        return int(self._rescale(0, ROUND_FLOOR))

    def __ceil__(self):
        if self._is_special:
            if self.is_nan():
                raise ValueError("cannot round a NaN")
            raise OverflowError("cannot round an infinity")
        return int(self._rescale(0, ROUND_CEILING))

    def __round__(self, n=None):
        if n is not None:
            if not isinstance(n, int):
                raise TypeError("Second argument to round should be integral")
            exp = _dec(0, "1", -n)
            return self.quantize(exp)
        if self._is_special:
            if self.is_nan():
                raise ValueError("cannot round a NaN")
            raise OverflowError("cannot round an infinity")
        return int(self._rescale(0, ROUND_HALF_EVEN))

    # -- hashing and comparison --------------------------------------------

    def __hash__(self):
        if self._is_special:
            if self.is_snan():
                raise TypeError("Cannot hash a signaling NaN value.")
            elif self.is_nan():
                return object.__hash__(self)
            return -314159 if self._sign else 314159
        modulus = (1 << 61) - 1
        if self._exp >= 0:
            exp_hash = pow(10, self._exp, modulus)
        else:
            exp_hash = pow(_PyHASH_10INV, -self._exp, modulus)
        hash_ = int(self._int) * exp_hash % modulus
        ans = hash_ if self >= 0 else -hash_
        return -2 if ans == -1 else ans

    def _cmp(self, other):
        if self._is_special or other._is_special:
            self_inf = self._isinfinity()
            other_inf = other._isinfinity()
            if self_inf == other_inf:
                return 0
            elif self_inf < other_inf:
                return -1
            else:
                return 1
        if not self:
            if not other:
                return 0
            return -((-1) ** other._sign)
        if not other:
            return (-1) ** self._sign
        if other._sign < self._sign:
            return -1
        if self._sign < other._sign:
            return 1
        self_adjusted = self.adjusted()
        other_adjusted = other.adjusted()
        if self_adjusted == other_adjusted:
            self_padded = self._int + "0" * (self._exp - other._exp)
            other_padded = other._int + "0" * (other._exp - self._exp)
            if self_padded == other_padded:
                return 0
            elif self_padded < other_padded:
                return -((-1) ** self._sign)
            return (-1) ** self._sign
        elif self_adjusted > other_adjusted:
            return (-1) ** self._sign
        return -((-1) ** self._sign)

    def _rational_other(self, other, equality_op=False):
        """Converts the other operand of a comparison, or answers NotImplemented."""
        if isinstance(other, Decimal):
            return other
        if isinstance(other, int):
            return Decimal(other)
        if isinstance(other, float):
            context = getcontext()
            if equality_op:
                context.flags[FloatOperation] = True
            else:
                context._raise_error(FloatOperation, "strict semantics for mixing floats and Decimals are enabled")
            return Decimal.from_float(other)
        if isinstance(other, numbers.Rational):
            return ("rational", other.numerator, other.denominator)
        return NotImplemented

    def _cmp_rational(self, r):
        if self._is_special:
            return 1 if self._isinfinity() == 1 else -1
        n, d = self.as_integer_ratio()
        a = n * r[2]
        b = r[1] * d
        return (a > b) - (a < b)

    def __eq__(self, other, context=None):
        other = self._rational_other(other, equality_op=True)
        if other is NotImplemented:
            return other
        if isinstance(other, tuple):
            return not self._isnan() and self._cmp_rational(other) == 0
        if self._isnan() or other._isnan():
            return False
        return self._cmp(other) == 0

    def _order(self, other, context):
        other = self._rational_other(other)
        if other is NotImplemented:
            return other
        if isinstance(other, tuple):
            if self._isnan():
                ans = self._compare_check_nans(self, context)
                return None
            return self._cmp_rational(other)
        if self._compare_check_nans(other, context):
            return None
        return self._cmp(other)

    def __lt__(self, other, context=None):
        c = self._order(other, context)
        if c is NotImplemented:
            return c
        return c is not None and c < 0

    def __le__(self, other, context=None):
        c = self._order(other, context)
        if c is NotImplemented:
            return c
        return c is not None and c <= 0

    def __gt__(self, other, context=None):
        c = self._order(other, context)
        if c is NotImplemented:
            return c
        return c is not None and c > 0

    def __ge__(self, other, context=None):
        c = self._order(other, context)
        if c is NotImplemented:
            return c
        return c is not None and c >= 0

    def compare(self, other, context=None):
        other = _convert(other, raiseit=True)
        if self._is_special or other._is_special:
            ans = self._check_nans(other, context)
            if ans:
                return ans
        return Decimal(self._cmp(other))

    def compare_total(self, other, context=None):
        other = _convert(other, raiseit=True)
        if self._sign and not other._sign:
            return _NegativeOne
        if not self._sign and other._sign:
            return _One
        sign = self._sign
        self_nan = self._isnan()
        other_nan = other._isnan()
        if self_nan or other_nan:
            if self_nan == other_nan:
                self_key = len(self._int), self._int
                other_key = len(other._int), other._int
                if self_key < other_key:
                    return _NegativeOne if not sign else _One
                if self_key > other_key:
                    return _One if not sign else _NegativeOne
                return _Zero
            if sign:
                if self_nan == 1:
                    return _NegativeOne
                if other_nan == 1:
                    return _One
                if self_nan == 2:
                    return _NegativeOne
                if other_nan == 2:
                    return _One
            else:
                if self_nan == 1:
                    return _One
                if other_nan == 1:
                    return _NegativeOne
                if self_nan == 2:
                    return _One
                if other_nan == 2:
                    return _NegativeOne
        if self < other:
            return _NegativeOne
        if self > other:
            return _One
        if self._exp < other._exp:
            return _One if sign else _NegativeOne
        if self._exp > other._exp:
            return _NegativeOne if sign else _One
        return _Zero

    def max(self, other, context=None):
        other = _convert(other, raiseit=True)
        if context is None:
            context = getcontext()
        if self._is_special or other._is_special:
            sn = self._isnan()
            on = other._isnan()
            if sn or on:
                if on == 1 and sn == 0:
                    return self._fix(context)
                if sn == 1 and on == 0:
                    return other._fix(context)
                return self._check_nans(other, context)
        c = self._cmp(other)
        if c == 0:
            c = self.compare_total(other)._cmp(_Zero)
        ans = other if c == -1 else self
        return ans._fix(context)

    def min(self, other, context=None):
        other = _convert(other, raiseit=True)
        if context is None:
            context = getcontext()
        if self._is_special or other._is_special:
            sn = self._isnan()
            on = other._isnan()
            if sn or on:
                if on == 1 and sn == 0:
                    return self._fix(context)
                if sn == 1 and on == 0:
                    return other._fix(context)
                return self._check_nans(other, context)
        c = self._cmp(other)
        if c == 0:
            c = self.compare_total(other)._cmp(_Zero)
        ans = self if c == -1 else other
        return ans._fix(context)

    # -- rounding ------------------------------------------------------------

    def _fix_nan(self, context):
        payload = self._int
        max_payload_len = context.prec - context.clamp
        if len(payload) > max_payload_len:
            payload = payload[len(payload) - max_payload_len:].lstrip("0")
            return _dec(self._sign, payload, self._exp, True)
        return Decimal(self)

    def _fix(self, context):
        if self._is_special:
            if self._isnan():
                return self._fix_nan(context)
            return Decimal(self)
        Etiny = context.Etiny()
        Etop = context.Etop()
        if not self:
            exp_max = [context.Emax, Etop][context.clamp]
            new_exp = min(max(self._exp, Etiny), exp_max)
            if new_exp != self._exp:
                context._raise_error(Clamped)
                return _dec(self._sign, "0", new_exp)
            return Decimal(self)
        exp_min = len(self._int) + self._exp - context.prec
        if exp_min > Etop:
            ans = context._raise_error(Overflow, "above Emax", self._sign)
            context._raise_error(Inexact)
            context._raise_error(Rounded)
            return ans
        self_is_subnormal = exp_min < Etiny
        if self_is_subnormal:
            exp_min = Etiny
        if self._exp < exp_min:
            digits = len(self._int) + self._exp - exp_min
            if digits < 0:
                self = _dec(self._sign, "1", exp_min - 1)
                digits = 0
            changed = self._round_by(context.rounding, digits)
            coeff = self._int[:digits] or "0"
            if changed > 0:
                coeff = str(int(coeff) + 1)
                if len(coeff) > context.prec:
                    coeff = coeff[:-1]
                    exp_min += 1
            if exp_min > Etop:
                ans = context._raise_error(Overflow, "above Emax", self._sign)
            else:
                ans = _dec(self._sign, coeff, exp_min)
            if changed and self_is_subnormal:
                context._raise_error(Underflow)
            if self_is_subnormal:
                context._raise_error(Subnormal)
            if changed:
                context._raise_error(Inexact)
            context._raise_error(Rounded)
            if not ans:
                context._raise_error(Clamped)
            return ans
        if self_is_subnormal:
            context._raise_error(Subnormal)
        if context.clamp == 1 and self._exp > Etop:
            context._raise_error(Clamped)
            self_padded = self._int + "0" * (self._exp - Etop)
            return _dec(self._sign, self_padded, Etop)
        return Decimal(self)

    def _round_down(self, prec):
        return 0 if _all_zeros(self._int, prec) else -1

    def _round_by(self, rounding, prec):
        """1 if dropping the digits from prec on rounds up, 0 if exact, -1 if it truncates."""
        s = self._int
        if rounding == ROUND_DOWN:
            return self._round_down(prec)
        if rounding == ROUND_UP:
            return -self._round_down(prec)
        if rounding == ROUND_HALF_UP:
            if s[prec] in "56789":
                return 1
            return 0 if _all_zeros(s, prec) else -1
        if rounding == ROUND_HALF_DOWN:
            if _exact_half(s, prec):
                return -1
            return self._round_by(ROUND_HALF_UP, prec)
        if rounding == ROUND_HALF_EVEN:
            if _exact_half(s, prec) and (prec == 0 or s[prec - 1] in "02468"):
                return -1
            return self._round_by(ROUND_HALF_UP, prec)
        if rounding == ROUND_CEILING:
            return self._round_down(prec) if self._sign else -self._round_down(prec)
        if rounding == ROUND_FLOOR:
            return -self._round_down(prec) if self._sign else self._round_down(prec)
        if prec and s[prec - 1] not in "05":
            return self._round_down(prec)
        return -self._round_down(prec)

    def _rescale(self, exp, rounding):
        if self._is_special:
            return Decimal(self)
        if not self:
            return _dec(self._sign, "0", exp)
        if self._exp >= exp:
            return _dec(self._sign, self._int + "0" * (self._exp - exp), exp)
        digits = len(self._int) + self._exp - exp
        if digits < 0:
            self = _dec(self._sign, "1", exp - 1)
            digits = 0
        changed = self._round_by(rounding, digits)
        coeff = self._int[:digits] or "0"
        if changed == 1:
            coeff = str(int(coeff) + 1)
        return _dec(self._sign, coeff, exp)

    def _round(self, places, rounding):
        if places <= 0:
            raise ValueError("argument should be at least 1 in _round")
        if self._is_special or not self:
            return Decimal(self)
        ans = self._rescale(self.adjusted() + 1 - places, rounding)
        if ans.adjusted() != self.adjusted():
            ans = ans._rescale(ans.adjusted() + 1 - places, rounding)
        return ans

    def quantize(self, exp, rounding=None, context=None):
        exp = _convert(exp, raiseit=True)
        if context is None:
            context = getcontext()
        if rounding is None:
            rounding = context.rounding
        if self._is_special or exp._is_special:
            ans = self._check_nans(exp, context)
            if ans:
                return ans
            if exp._isinfinity() or self._isinfinity():
                if exp._isinfinity() and self._isinfinity():
                    return Decimal(self)
                return context._raise_error(InvalidOperation, "quantize with one INF")
        if not (context.Etiny() <= exp._exp <= context.Emax):
            return context._raise_error(InvalidOperation, "target exponent out of bounds in quantize")
        if not self:
            ans = _dec(self._sign, "0", exp._exp)
            return ans._fix(context)
        self_adjusted = self.adjusted()
        if self_adjusted > context.Emax:
            return context._raise_error(InvalidOperation, "exponent of quantize result too large for current "
                                                          "context")
        if self_adjusted - exp._exp + 1 > context.prec:
            return context._raise_error(InvalidOperation, "quantize result has too many digits for current context")
        ans = self._rescale(exp._exp, rounding)
        if ans.adjusted() > context.Emax:
            return context._raise_error(InvalidOperation, "exponent of quantize result too large for current "
                                                          "context")
        if len(ans._int) > context.prec:
            return context._raise_error(InvalidOperation, "quantize result has too many digits for current context")
        if ans and ans.adjusted() < context.Emin:
            context._raise_error(Subnormal)
        if ans._exp > self._exp:
            if ans != self:
                context._raise_error(Inexact)
            context._raise_error(Rounded)
        return ans._fix(context)

    def to_integral_exact(self, rounding=None, context=None):
        if self._is_special:
            ans = self._check_nans(context=context)
            if ans:
                return ans
            return Decimal(self)
        if self._exp >= 0:
            return Decimal(self)
        if not self:
            return _dec(self._sign, "0", 0)
        if context is None:
            context = getcontext()
        if rounding is None:
            rounding = context.rounding
        ans = self._rescale(0, rounding)
        if ans != self:
            context._raise_error(Inexact)
        context._raise_error(Rounded)
        return ans

    def to_integral_value(self, rounding=None, context=None):
        if context is None:
            context = getcontext()
        if rounding is None:
            rounding = context.rounding
        if self._is_special:
            ans = self._check_nans(context=context)
            if ans:
                return ans
            return Decimal(self)
        if self._exp >= 0:
            return Decimal(self)
        return self._rescale(0, rounding)

    to_integral = to_integral_value

    def normalize(self, context=None):
        if context is None:
            context = getcontext()
        if self._is_special:
            ans = self._check_nans(context=context)
            if ans:
                return ans
        dup = self._fix(context)
        if dup._isinfinity():
            return dup
        if not dup:
            return _dec(dup._sign, "0", 0)
        exp_max = [context.Emax, context.Etop()][context.clamp]
        end = len(dup._int)
        exp = dup._exp
        while dup._int[end - 1] == "0" and exp < exp_max:
            exp += 1
            end -= 1
        return _dec(dup._sign, dup._int[:end], exp)

    def scaleb(self, other, context=None):
        if context is None:
            context = getcontext()
        other = _convert(other, raiseit=True)
        ans = self._check_nans(other, context)
        if ans:
            return ans
        if other._exp != 0 or other._is_special:
            return context._raise_error(InvalidOperation)
        if self._isinfinity():
            return Decimal(self)
        d = _dec(self._sign, self._int, self._exp + int(other))
        return d._fix(context)

    def logb(self, context=None):
        ans = self._check_nans(context=context)
        if ans:
            return ans
        if context is None:
            context = getcontext()
        if self._isinfinity():
            return _Infinity
        if not self:
            return context._raise_error(DivisionByZero, "logb(0)", 1)
        return Decimal(self.adjusted())._fix(context)

    # -- sign operations ---------------------------------------------------

    def copy_abs(self):
        return _dec(0, self._int, self._exp, self._is_special)

    def copy_negate(self):
        return _dec(1 - self._sign, self._int, self._exp, self._is_special)

    def copy_sign(self, other, context=None):
        other = _convert(other, raiseit=True)
        return _dec(other._sign, self._int, self._exp, self._is_special)

    def __neg__(self, context=None):
        if self._is_special:
            ans = self._check_nans(context=context)
            if ans:
                return ans
        if context is None:
            context = getcontext()
        if not self and context.rounding != ROUND_FLOOR:
            ans = self.copy_abs()
        else:
            ans = self.copy_negate()
        return ans._fix(context)

    def __pos__(self, context=None):
        if self._is_special:
            ans = self._check_nans(context=context)
            if ans:
                return ans
        if context is None:
            context = getcontext()
        if not self and context.rounding != ROUND_FLOOR:
            ans = self.copy_abs()
        else:
            ans = Decimal(self)
        return ans._fix(context)

    def __abs__(self, round=True, context=None):
        if not round:
            return self.copy_abs()
        if self._is_special:
            ans = self._check_nans(context=context)
            if ans:
                return ans
        if self._sign:
            return self.__neg__(context=context)
        return self.__pos__(context=context)

    # -- arithmetic --------------------------------------------------------

    def __add__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        if context is None:
            context = getcontext()
        if self._is_special or other._is_special:
            ans = self._check_nans(other, context)
            if ans:
                return ans
            if self._isinfinity():
                if self._sign != other._sign and other._isinfinity():
                    return context._raise_error(InvalidOperation, "-INF + INF")
                return Decimal(self)
            if other._isinfinity():
                return Decimal(other)
        exp = min(self._exp, other._exp)
        negativezero = 0
        if context.rounding == ROUND_FLOOR and self._sign != other._sign:
            negativezero = 1
        if not self and not other:
            sign = min(self._sign, other._sign)
            if negativezero:
                sign = 1
            return _dec(sign, "0", exp)._fix(context)
        if not self:
            exp = max(exp, other._exp - context.prec - 1)
            return other._rescale(exp, context.rounding)._fix(context)
        if not other:
            exp = max(exp, self._exp - context.prec - 1)
            return self._rescale(exp, context.rounding)._fix(context)
        a = int(self._int) * 10 ** (self._exp - exp)
        b = int(other._int) * 10 ** (other._exp - exp)
        if self._sign:
            a = -a
        if other._sign:
            b = -b
        total = a + b
        if total == 0:
            return _dec(negativezero, "0", exp)._fix(context)
        return _dec(1 if total < 0 else 0, str(abs(total)), exp)._fix(context)

    __radd__ = __add__

    def __sub__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        if self._is_special or other._is_special:
            ans = self._check_nans(other, context=context)
            if ans:
                return ans
        return self.__add__(other.copy_negate(), context=context)

    def __rsub__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        return other.__sub__(self, context=context)

    def __mul__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        if context is None:
            context = getcontext()
        resultsign = self._sign ^ other._sign
        if self._is_special or other._is_special:
            ans = self._check_nans(other, context)
            if ans:
                return ans
            if self._isinfinity():
                if not other:
                    return context._raise_error(InvalidOperation, "(+-)INF * 0")
                return _SignedInfinity[resultsign]
            if other._isinfinity():
                if not self:
                    return context._raise_error(InvalidOperation, "0 * (+-)INF")
                return _SignedInfinity[resultsign]
        resultexp = self._exp + other._exp
        if not self or not other:
            return _dec(resultsign, "0", resultexp)._fix(context)
        return _dec(resultsign, str(int(self._int) * int(other._int)), resultexp)._fix(context)

    __rmul__ = __mul__

    def __truediv__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        if context is None:
            context = getcontext()
        sign = self._sign ^ other._sign
        if self._is_special or other._is_special:
            ans = self._check_nans(other, context)
            if ans:
                return ans
            if self._isinfinity() and other._isinfinity():
                return context._raise_error(InvalidOperation, "(+-)INF/(+-)INF")
            if self._isinfinity():
                return _SignedInfinity[sign]
            if other._isinfinity():
                context._raise_error(Clamped, "Division by infinity")
                return _dec(sign, "0", context.Etiny())
        if not other:
            if not self:
                return context._raise_error(DivisionUndefined, "0 / 0")
            return context._raise_error(DivisionByZero, "x / 0", sign)
        if not self:
            exp = self._exp - other._exp
            coeff = 0
        else:
            shift = len(other._int) - len(self._int) + context.prec + 1
            exp = self._exp - other._exp - shift
            if shift >= 0:
                coeff, remainder = divmod(int(self._int) * 10 ** shift, int(other._int))
            else:
                coeff, remainder = divmod(int(self._int), int(other._int) * 10 ** -shift)
            if remainder:
                if coeff % 5 == 0:
                    coeff += 1
            else:
                ideal_exp = self._exp - other._exp
                while exp < ideal_exp and coeff % 10 == 0:
                    coeff //= 10
                    exp += 1
        return _dec(sign, str(coeff), exp)._fix(context)

    def __rtruediv__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        return other.__truediv__(self, context=context)

    def _divide(self, other, context):
        sign = self._sign ^ other._sign
        if other._isinfinity():
            ideal_exp = self._exp
        else:
            ideal_exp = min(self._exp, other._exp)
        expdiff = self.adjusted() - other.adjusted()
        if not self or other._isinfinity() or expdiff <= -2:
            return (_dec(sign, "0", 0), self._rescale(ideal_exp, context.rounding))
        if expdiff <= context.prec:
            a = int(self._int)
            b = int(other._int)
            if self._exp >= other._exp:
                a *= 10 ** (self._exp - other._exp)
            else:
                b *= 10 ** (other._exp - self._exp)
            q, r = divmod(a, b)
            if q < 10 ** context.prec:
                return (_dec(sign, str(q), 0), _dec(self._sign, str(r), ideal_exp))
        ans = context._raise_error(DivisionImpossible, "quotient too large in //, % or divmod")
        return ans, ans

    def __divmod__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        if context is None:
            context = getcontext()
        ans = self._check_nans(other, context)
        if ans:
            return (ans, ans)
        sign = self._sign ^ other._sign
        if self._isinfinity():
            if other._isinfinity():
                ans = context._raise_error(InvalidOperation, "divmod(INF, INF)")
                return ans, ans
            return (_SignedInfinity[sign], context._raise_error(InvalidOperation, "INF % x"))
        if not other:
            if not self:
                ans = context._raise_error(DivisionUndefined, "divmod(0, 0)")
                return ans, ans
            return (context._raise_error(DivisionByZero, "x // 0", sign),
                    context._raise_error(InvalidOperation, "x % 0"))
        quotient, remainder = self._divide(other, context)
        remainder = remainder._fix(context)
        return quotient, remainder

    def __rdivmod__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        return other.__divmod__(self, context=context)

    def __mod__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        if context is None:
            context = getcontext()
        ans = self._check_nans(other, context)
        if ans:
            return ans
        if self._isinfinity():
            return context._raise_error(InvalidOperation, "INF % x")
        elif not other:
            if self:
                return context._raise_error(InvalidOperation, "x % 0")
            return context._raise_error(DivisionUndefined, "0 % 0")
        remainder = self._divide(other, context)[1]
        return remainder._fix(context)

    def __rmod__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        return other.__mod__(self, context=context)

    def remainder_near(self, other, context=None):
        if context is None:
            context = getcontext()
        other = _convert(other, raiseit=True)
        ans = self._check_nans(other, context)
        if ans:
            return ans
        if self._isinfinity():
            return context._raise_error(InvalidOperation, "remainder_near(infinity, x)")
        if not other:
            if self:
                return context._raise_error(InvalidOperation, "remainder_near(x, 0)")
            return context._raise_error(DivisionUndefined, "remainder_near(0, 0)")
        if other._isinfinity():
            return Decimal(self)._fix(context)
        ideal_exponent = min(self._exp, other._exp)
        if not self:
            return _dec(self._sign, "0", ideal_exponent)._fix(context)
        expdiff = self.adjusted() - other.adjusted()
        if expdiff >= context.prec + 1:
            return context._raise_error(DivisionImpossible)
        if expdiff <= -2:
            return self._rescale(ideal_exponent, context.rounding)._fix(context)
        a = int(self._int)
        b = int(other._int)
        if self._exp >= other._exp:
            a *= 10 ** (self._exp - other._exp)
        else:
            b *= 10 ** (other._exp - self._exp)
        q, r = divmod(a, b)
        if 2 * r + (q & 1) > b:
            r -= b
            q += 1
        if q >= 10 ** context.prec:
            return context._raise_error(DivisionImpossible)
        sign = self._sign
        if r < 0:
            sign = 1 - sign
            r = -r
        return _dec(sign, str(r), ideal_exponent)._fix(context)

    def __floordiv__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        if context is None:
            context = getcontext()
        ans = self._check_nans(other, context)
        if ans:
            return ans
        if self._isinfinity():
            if other._isinfinity():
                return context._raise_error(InvalidOperation, "INF // INF")
            return _SignedInfinity[self._sign ^ other._sign]
        if not other:
            if self:
                return context._raise_error(DivisionByZero, "x // 0", self._sign ^ other._sign)
            return context._raise_error(DivisionUndefined, "0 // 0")
        return self._divide(other, context)[0]

    def __rfloordiv__(self, other, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        return other.__floordiv__(self, context=context)

    def fma(self, other, third, context=None):
        other = _convert(other, raiseit=True)
        third = _convert(third, raiseit=True)
        if context is None:
            context = getcontext()
        if self._is_special or other._is_special:
            if self.is_snan() or other.is_snan() or self.is_nan() or other.is_nan():
                ans = self._check_nans(other, context)
                return ans
            product = self.__mul__(other, context)
        else:
            product = _dec(self._sign ^ other._sign, str(int(self._int) * int(other._int)), self._exp + other._exp)
        return product.__add__(third, context)

    # -- powers, roots and logarithms --------------------------------------

    def sqrt(self, context=None):
        if context is None:
            context = getcontext()
        if self._is_special:
            ans = self._check_nans(context=context)
            if ans:
                return ans
            if self._isinfinity() and self._sign == 0:
                return Decimal(self)
        if not self:
            return _dec(self._sign, "0", self._exp // 2)._fix(context)
        if self._sign == 1:
            return context._raise_error(InvalidOperation, "sqrt(-x), x > 0")
        prec = context.prec + 1
        e = self._exp >> 1
        if self._exp & 1:
            c = int(self._int) * 10
            l = (len(self._int) >> 1) + 1
        else:
            c = int(self._int)
            l = len(self._int) + 1 >> 1
        shift = prec - l
        if shift >= 0:
            c *= 100 ** shift
            exact = True
        else:
            c, remainder = divmod(c, 100 ** -shift)
            exact = not remainder
        e -= shift
        n = 10 ** prec
        while True:
            q = c // n
            if n <= q:
                break
            n = n + q >> 1
        exact = exact and n * n == c
        if exact:
            if shift >= 0:
                n //= 10 ** shift
            else:
                n *= 10 ** -shift
            e += shift
        else:
            if n % 5 == 0:
                n += 1
        ans = _dec(0, str(n), e)
        context = context._shallow_copy()
        rounding = context._set_rounding(ROUND_HALF_EVEN)
        ans = ans._fix(context)
        context.rounding = rounding
        return ans

    def _guarded(self, context):
        """A trap-free context with twenty guard digits, for the series below."""
        c = Context(prec=context.prec + 20, rounding=ROUND_HALF_EVEN, Emin=MIN_EMIN, Emax=MAX_EMAX, traps=[])
        return c

    def _round_result(self, value, context):
        """Rounds a guarded result to context, signalling Inexact and Rounded."""
        ans = value._fix(context)
        if ans != value or len(value._int) > context.prec:
            context._raise_error(Inexact)
            context._raise_error(Rounded)
        return ans

    def exp(self, context=None):
        if context is None:
            context = getcontext()
        ans = self._check_nans(context=context)
        if ans:
            return ans
        if self._isinfinity() == -1:
            return _Zero
        if not self:
            return _One
        if self._isinfinity() == 1:
            return Decimal(self)
        adj = self.adjusted()
        if self._sign == 0 and adj > len(str((context.Emax + 1) * 3)):
            ans = _dec(0, "1", context.Emax + 1)
            return ans._fix(context)
        if self._sign == 1 and adj > len(str((-context.Etiny() + 1) * 3)):
            ans = _dec(0, "1", context.Etiny() - 1)
            return ans._fix(context)
        g = self._guarded(context)
        return self._round_result(_exp_series(self, g), context)

    def ln(self, context=None):
        if context is None:
            context = getcontext()
        ans = self._check_nans(context=context)
        if ans:
            return ans
        if not self:
            return _NegativeInfinity
        if self._isinfinity() == 1:
            return _Infinity
        if self == _One:
            return _Zero
        if self._sign == 1:
            return context._raise_error(InvalidOperation, "ln of a negative value")
        g = self._guarded(context)
        return self._round_result(_ln(self, g), context)

    def log10(self, context=None):
        if context is None:
            context = getcontext()
        ans = self._check_nans(context=context)
        if ans:
            return ans
        if not self:
            return _NegativeInfinity
        if self._isinfinity() == 1:
            return _Infinity
        if self._sign == 1:
            return context._raise_error(InvalidOperation, "log10 of a negative value")
        if self._int[0] == "1" and self._int[1:] == "0" * (len(self._int) - 1):
            return Decimal(self._exp + len(self._int) - 1)
        g = self._guarded(context)
        return self._round_result(_ln(self, g).__truediv__(_ln10(g), g), context)

    def __pow__(self, other, modulo=None, context=None):
        if modulo is not None:
            return self._power_modulo(other, modulo, context)
        other = _convert(other)
        if other is NotImplemented:
            return other
        if context is None:
            context = getcontext()
        ans = self._check_nans(other, context)
        if ans:
            return ans
        if not other:
            if not self:
                return context._raise_error(InvalidOperation, "0 ** 0")
            return _One
        result_sign = 0
        if self._sign == 1:
            if other._isinteger():
                if not other._iseven():
                    result_sign = 1
            elif self:
                return context._raise_error(InvalidOperation, "x ** y with x negative and y not an integer")
            self = self.copy_negate()
        if not self:
            if other._sign == 0:
                return _dec(result_sign, "0", 0)
            return _SignedInfinity[result_sign]
        if self._isinfinity():
            if other._sign == 0:
                return _SignedInfinity[result_sign]
            return _dec(result_sign, "0", 0)
        if other._isinfinity():
            c = self._cmp(_One)
            if c == 0:
                context._raise_error(Inexact)
                context._raise_error(Rounded)
                return _dec(0, "1" + "0" * (context.prec - 1), 1 - context.prec)
            if (c > 0) == (other._sign == 0):
                return _Infinity
            return _Zero
        if other._isinteger():
            n = int(other)
            # An exact power when it is small enough to compute exactly.
            if 0 < n and len(self._int) * n <= 4 * context.prec + 400:
                coeff = int(self._int) ** n
                ans = _dec(result_sign, str(coeff), self._exp * n)
                return ans._fix(context)
            if n < 0 and len(self._int) * -n <= 4 * context.prec + 400:
                denom = _dec(0, str(int(self._int) ** -n), self._exp * -n)
                ans = _One.__truediv__(denom, context)
                if result_sign:
                    ans = ans.copy_negate()
                return ans
        g = self._guarded(context)
        g.prec += len(str(abs(other.adjusted()))) + 5
        val = _exp_series(_ln(self, g).__mul__(other, g), g)
        if other._isinteger():
            pass
        else:
            exact = _exact_root(self, other, context)
            if exact is not None:
                val = exact
        ans = self._round_result(val, context)
        if result_sign:
            ans = ans.copy_negate()
        return ans

    def __rpow__(self, other, modulo=None, context=None):
        other = _convert(other)
        if other is NotImplemented:
            return other
        return other.__pow__(self, modulo, context=context)

    def _power_modulo(self, other, modulo, context=None):
        if context is None:
            context = getcontext()
        other = _convert(other, raiseit=True)
        modulo = _convert(modulo, raiseit=True)
        for v in (self, other, modulo):
            if v._is_special:
                return context._raise_error(InvalidOperation, "pow() 3rd argument not allowed unless all "
                                                              "arguments are integers")
        if not other._isinteger() or not self._isinteger() or not modulo._isinteger():
            return context._raise_error(InvalidOperation, "pow() 3rd argument not allowed unless all arguments "
                                                          "are integers")
        if other < 0:
            return context._raise_error(InvalidOperation, "pow() 2nd argument cannot be negative when 3rd "
                                                          "argument specified")
        if not modulo:
            return context._raise_error(InvalidOperation, "pow() 3rd argument cannot be 0")
        if modulo.adjusted() >= context.prec:
            return context._raise_error(InvalidOperation, "insufficient precision: pow() 3rd argument must not "
                                                          "have more than precision digits")
        if not other and not self:
            return context._raise_error(InvalidOperation, "at least one of pow() 1st argument and 2nd argument "
                                                          "must be nonzero; 0**0 is not defined")
        sign = 1 if other._iseven() is False and self._sign else 0
        m = abs(int(modulo))
        r = pow(abs(int(self)), int(other), m)
        return _dec(sign, str(r), 0)

    # -- formatting --------------------------------------------------------

    def __format__(self, specifier, context=None, _localeconv=None):
        if context is None:
            context = getcontext()
        spec = _parse_format_specifier(specifier)
        if self._is_special:
            sign = _format_sign(self._sign, spec)
            body = str(self.copy_abs())
            if spec["type"] == "%":
                body += "%"
            return _format_align(sign, body, spec)
        if spec["type"] is None:
            spec["type"] = ["g", "G"][context.capitals]
        if spec["type"] == "%":
            self = _dec(self._sign, self._int, self._exp + 2)
        rounding = context.rounding
        precision = spec["precision"]
        if precision is not None:
            if spec["type"] in "eE":
                self = self._round(precision + 1, rounding)
            elif spec["type"] in "fF%":
                self = self._rescale(-precision, rounding)
            elif spec["type"] in "gG" and len(self._int) > precision:
                self = self._round(precision, rounding)
        if not self and self._exp > 0 and spec["type"] in "fF%":
            self = self._rescale(0, rounding)
        if not self and spec["no_neg_0"] and self._sign:
            adjusted_sign = 0
        else:
            adjusted_sign = self._sign
        leftdigits = self._exp + len(self._int)
        if spec["type"] in "eE":
            if not self and precision is not None:
                dotplace = 1 - precision
            else:
                dotplace = 1
        elif spec["type"] in "fF%":
            dotplace = leftdigits
        else:
            if self._exp <= 0 and leftdigits > -6:
                dotplace = leftdigits
            else:
                dotplace = 1
        if dotplace < 0:
            intpart = "0"
            fracpart = "0" * (-dotplace) + self._int
        elif dotplace > len(self._int):
            intpart = self._int + "0" * (dotplace - len(self._int))
            fracpart = ""
        else:
            intpart = self._int[:dotplace] or "0"
            fracpart = self._int[dotplace:]
        exp = leftdigits - dotplace
        return _format_number(adjusted_sign, intpart, fracpart, exp, spec)

    def __reduce__(self):
        return (self.__class__, (str(self),))

    def __copy__(self):
        return self

    def __deepcopy__(self, memo):
        return self


def _exp_series(x, g):
    """e**x in the guarded context g."""
    k = 0
    # Halve until |x| < 0.1, then square the result back up.
    ax = x.copy_abs()
    while ax._cmp(_dec(0, "1", -1)) > 0:
        ax = ax.__truediv__(_Two, g)
        k += 1
    y = x.copy_abs()
    for _ in range(k):
        y = y.__truediv__(_Two, g)
    total = _One
    term = _One
    n = 1
    eps = _dec(0, "1", -(g.prec + 2))
    while True:
        term = term.__mul__(y, g).__truediv__(Decimal(n), g)
        if term.copy_abs()._cmp(eps) < 0:
            break
        total = total.__add__(term, g)
        n += 1
    for _ in range(k):
        total = total.__mul__(total, g)
    if x._sign:
        total = _One.__truediv__(total, g)
    return total


_ln10_cache = {}


def _ln10(g):
    v = _ln10_cache.get(g.prec)
    if v is None:
        v = _ln_reduced(Decimal(10), g)
        _ln10_cache[g.prec] = v
    return v


def _ln(x, g):
    """ln(x) for a finite positive x, in the guarded context g."""
    k = x.adjusted()
    m = _dec(0, x._int, -(len(x._int) - 1))
    r = _ln_reduced(m, g)
    if k:
        r = r.__add__(_ln10(g).__mul__(Decimal(k), g), g)
    return r


def _ln_reduced(m, g):
    """ln(m) for m near [1, 10), by square roots and the atanh series."""
    j = 0
    lo = _dec(0, "9", -1)
    hi = _dec(0, "11", -1)
    while m._cmp(hi) > 0 or m._cmp(lo) < 0:
        m = m.sqrt(g)
        j += 1
    z = m.__sub__(_One, g).__truediv__(m.__add__(_One, g), g)
    z2 = z.__mul__(z, g)
    total = z
    term = z
    n = 3
    eps = _dec(0, "1", -(g.prec + 2))
    while True:
        term = term.__mul__(z2, g)
        t = term.__truediv__(Decimal(n), g)
        if t.copy_abs()._cmp(eps) < 0:
            break
        total = total.__add__(t, g)
        n += 2
    return total.__mul__(Decimal(2 ** (j + 1)), g)


def _exact_root(x, y, context):
    """x ** y when y = p/q and x has an exact q-th root, padded as CPython pads it; else None."""
    n, d = y.as_integer_ratio()
    if d > 64 or n <= 0:
        return None
    coeff = int(x._int)
    exp = x._exp
    while exp % d:
        coeff *= 10
        exp -= 1
    lo, hi = 0, 1
    while hi ** d <= coeff:
        hi *= 2
    while lo < hi:
        mid = (lo + hi) // 2
        if mid ** d < coeff:
            lo = mid + 1
        else:
            hi = mid
    if lo ** d != coeff:
        return None
    root = lo ** n
    rexp = exp // d * n
    digits = str(root)
    pad = context.prec - len(digits)
    if pad > 0:
        digits += "0" * pad
        rexp -= pad
    return _dec(0, digits, rexp)


def _parse_format_specifier(spec):
    fill = " "
    align = None
    i = 0
    if len(spec) >= 2 and spec[1] in "<>=^":
        fill = spec[0]
        align = spec[1]
        i = 2
    elif spec[:1] in ("<", ">", "=", "^"):
        align = spec[0]
        i = 1
    sign = "-"
    if spec[i:i + 1] in ("+", "-", " "):
        sign = spec[i]
        i += 1
    no_neg_0 = False
    if spec[i:i + 1] == "z":
        no_neg_0 = True
        i += 1
    alt = False
    if spec[i:i + 1] == "#":
        alt = True
        i += 1
    zeropad = False
    if spec[i:i + 1] == "0":
        zeropad = True
        if align is None:
            fill = "0"
            align = "="
        i += 1
    j = i
    while j < len(spec) and spec[j].isdigit():
        j += 1
    minimumwidth = int(spec[i:j]) if j > i else 0
    i = j
    thousands_sep = ""
    if spec[i:i + 1] in (",", "_"):
        thousands_sep = spec[i]
        i += 1
    precision = None
    if spec[i:i + 1] == ".":
        j = i + 1
        while j < len(spec) and spec[j].isdigit():
            j += 1
        if j == i + 1:
            raise ValueError("Invalid format specifier: " + spec)
        precision = int(spec[i + 1:j])
        i = j
    type = None
    if i < len(spec):
        type = spec[i]
        i += 1
        if type not in "eEfFgGn%":
            raise ValueError("Invalid format specifier: " + spec)
    if i != len(spec):
        raise ValueError("Invalid format specifier: " + spec)
    if type in ("g", "G", "n") and precision == 0:
        precision = 1
    if type == "n":
        type = "g"
    if align is None:
        align = ">"
    return {"fill": fill, "align": align, "sign": sign, "no_neg_0": no_neg_0, "alt": alt, "zeropad": zeropad,
            "minimumwidth": minimumwidth, "thousands_sep": thousands_sep, "precision": precision, "type": type,
            "decimal_point": ".", "grouping": [3, 0]}


def _format_sign(is_negative, spec):
    if is_negative:
        return "-"
    elif spec["sign"] in " +":
        return spec["sign"]
    return ""


def _format_align(sign, body, spec):
    minimumwidth = spec["minimumwidth"]
    fill = spec["fill"]
    padding = fill * (minimumwidth - len(sign) - len(body))
    align = spec["align"]
    if align == "<":
        result = sign + body + padding
    elif align == ">":
        result = padding + sign + body
    elif align == "=":
        result = sign + padding + body
    else:
        half = len(padding) // 2
        result = padding[:half] + sign + body + padding[half:]
    return result


def _insert_thousands_sep(digits, spec, min_width=1):
    sep = spec["thousands_sep"]
    if not sep:
        if len(digits) < min_width:
            digits = "0" * (min_width - len(digits)) + digits
        return digits
    groups = []
    min_width = max(min_width, 1)
    while True:
        l = min(max(len(digits), min_width, 1), 3)
        groups.append("0" * (l - len(digits)) + digits[-l:])
        digits = digits[:-l]
        min_width -= l
        if not digits and min_width <= 0:
            break
        min_width -= len(sep)
    return sep.join(reversed(groups))


def _format_number(is_negative, intpart, fracpart, exp, spec):
    sign = _format_sign(is_negative, spec)
    if fracpart or spec["alt"]:
        fracpart = spec["decimal_point"] + fracpart
    if exp != 0 or spec["type"] in "eE":
        echar = {"E": "E", "e": "e", "G": "E", "g": "e"}[spec["type"]]
        fracpart += "{0}{1:+}".format(echar, exp)
    if spec["type"] == "%":
        fracpart += "%"
    if spec["zeropad"]:
        min_width = spec["minimumwidth"] - len(fracpart) - len(sign)
    else:
        min_width = 0
    intpart = _insert_thousands_sep(intpart, spec, min_width)
    return _format_align(sign, intpart + fracpart, spec)


_PyHASH_10INV = pow(10, (1 << 61) - 3, (1 << 61) - 1)

_Infinity = Decimal("Inf")
_NegativeInfinity = Decimal("-Inf")
_NaN = Decimal("NaN")
_Zero = Decimal(0)
_One = Decimal(1)
_Two = Decimal(2)
_NegativeOne = Decimal(-1)
_SignedInfinity = (_Infinity, _NegativeInfinity)

numbers.Number.register(Decimal)
