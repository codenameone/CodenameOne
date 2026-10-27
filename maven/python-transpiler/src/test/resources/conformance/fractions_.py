# fractions and numbers.
from fractions import Fraction as F
import numbers
import math

print(F(3, 4), repr(F(6, -8)), F(5), F("3/7"), F(" -1.25e2 "), F("1_000.5"), F(0.1), F(1.5), F(F(1, 3)), F(F(1, 2), F(3, 4)))
a, b = F(1, 3), F(1, 6)
print(a + b, a - b, a * b, a / b, a // b, a % b, divmod(F(7, 2), F(2, 3)), -a, +a, abs(F(-2, 5)))
print(a + 1, 1 + a, a - 1, 2 - a, a * 3, 3 / a, a + 0.5, 0.5 + a, a * 1j, a ** 2, a ** -2, F(-2, 3) ** -3, F(4) ** F(1, 2), 2 ** F(3), 2 ** F(1, 2))
print(a == F(2, 6), a == 1 / 3, F(1, 2) == 0.5, F(1, 2) < 0.6, a < b, a > b, F(1) == 1, F(3, 2) >= 1, F(1, 2) != F(1, 3), 1 < F(3, 2))
print(int(F(-7, 2)), math.trunc(F(7, 2)), math.floor(F(-7, 2)), math.ceil(F(-7, 2)), round(F(5, 2)), round(F(7, 2)), round(F(-5, 2)), round(F(1234, 100), 1), round(F(1234), -2))
print(float(F(1, 3)), F(1, 3).numerator, F(1, 3).denominator, F(10, 4).as_integer_ratio(), F(4, 2).is_integer(), bool(F(0)))
print(F(3.141592653589793).limit_denominator(1000), F(3.141592653589793).limit_denominator(10), F(1, 3).limit_denominator(5), F.from_float(0.25), F.from_number(7))
print(hash(F(1, 2)) == hash(0.5), hash(F(3)) == hash(3), hash(F(1, 3)), hash(F(-1, 3)), hash(F(10**20, 7)))
print(f"{F(1, 3):.5f}", f"{F(2, 3):10.2f}|", f"{F(-1, 8):.2%}", f"{F(1234567, 1):,.1f}", f"{F(1, 3)}", f"{F(5, 2):<8.1f}|", f"{F(1, 3):+.3f}")
print(sorted([F(1, 2), F(1, 3), 0.4, 1]), sum([F(1, 2), F(1, 3), F(1, 6)]), max(F(1, 2), F(2, 3)))
print(isinstance(F(1, 2), numbers.Rational), isinstance(1, numbers.Integral), isinstance(1.5, numbers.Real), isinstance(1.5, numbers.Rational),
      isinstance(1j, numbers.Complex), isinstance(True, numbers.Number), issubclass(int, numbers.Real), isinstance("x", numbers.Number))
for args in [(1, 0), ("abc",), ("1/0",), (1.5, 2), (float("inf"),), (float("nan"),)]:
    try:
        F(*args)
    except (ValueError, ZeroDivisionError, TypeError, OverflowError) as e:
        print(type(e).__name__, e)
try:
    F(1, 2) / 0
except ZeroDivisionError as e:
    print(e)
