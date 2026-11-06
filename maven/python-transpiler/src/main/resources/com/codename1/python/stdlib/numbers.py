# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The numeric tower: Number > Complex > Real > Rational > Integral."""

from abc import ABCMeta, abstractmethod

__all__ = ["Number", "Complex", "Real", "Rational", "Integral"]


class Number(metaclass=ABCMeta):
    __slots__ = ()
    __hash__ = None


class Complex(Number):
    __slots__ = ()

    @abstractmethod
    def __complex__(self):
        """Return a builtin complex instance."""

    def __bool__(self):
        return self != 0

    @property
    @abstractmethod
    def real(self):
        raise NotImplementedError

    @property
    @abstractmethod
    def imag(self):
        raise NotImplementedError

    @abstractmethod
    def __add__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __radd__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __neg__(self):
        raise NotImplementedError

    @abstractmethod
    def __pos__(self):
        raise NotImplementedError

    def __sub__(self, other):
        return self + -other

    def __rsub__(self, other):
        return -self + other

    @abstractmethod
    def __mul__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __rmul__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __truediv__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __rtruediv__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __pow__(self, exponent):
        raise NotImplementedError

    @abstractmethod
    def __rpow__(self, base):
        raise NotImplementedError

    @abstractmethod
    def __abs__(self):
        raise NotImplementedError

    @abstractmethod
    def conjugate(self):
        raise NotImplementedError

    @abstractmethod
    def __eq__(self, other):
        raise NotImplementedError


Complex.register(complex)


class Real(Complex):
    __slots__ = ()

    @abstractmethod
    def __float__(self):
        raise NotImplementedError

    @abstractmethod
    def __trunc__(self):
        raise NotImplementedError

    @abstractmethod
    def __floor__(self):
        raise NotImplementedError

    @abstractmethod
    def __ceil__(self):
        raise NotImplementedError

    @abstractmethod
    def __round__(self, ndigits=None):
        raise NotImplementedError

    def __divmod__(self, other):
        return (self // other, self % other)

    def __rdivmod__(self, other):
        return (other // self, other % self)

    @abstractmethod
    def __floordiv__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __rfloordiv__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __mod__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __rmod__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __lt__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __le__(self, other):
        raise NotImplementedError

    def __complex__(self):
        return complex(float(self))

    @property
    def real(self):
        return +self

    @property
    def imag(self):
        return 0

    def conjugate(self):
        return +self


Real.register(float)


class Rational(Real):
    __slots__ = ()

    @property
    @abstractmethod
    def numerator(self):
        raise NotImplementedError

    @property
    @abstractmethod
    def denominator(self):
        raise NotImplementedError

    def __float__(self):
        return int(self.numerator) / int(self.denominator)


class Integral(Rational):
    __slots__ = ()

    @abstractmethod
    def __int__(self):
        raise NotImplementedError

    def __index__(self):
        return int(self)

    @abstractmethod
    def __pow__(self, exponent, modulus=None):
        raise NotImplementedError

    @abstractmethod
    def __lshift__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __rlshift__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __rshift__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __rrshift__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __and__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __rand__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __xor__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __rxor__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __or__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __ror__(self, other):
        raise NotImplementedError

    @abstractmethod
    def __invert__(self):
        raise NotImplementedError

    def __float__(self):
        return float(int(self))

    @property
    def numerator(self):
        return +self

    @property
    def denominator(self):
        return 1


Integral.register(int)
Integral.register(bool)
