# decimal: construction, arithmetic, rounding, contexts, signals and formatting.
import decimal
from decimal import Decimal as D, getcontext, localcontext, Context
from fractions import Fraction

print(D("1.10") + D("2.20"), D("1.3") * D("1.30"), D(1) / D(3), D(2) / D(4), D("1e3") / D(5), D(10) / D("0.1"))
print(D("7.5") // D(2), D("-7.5") // D(2), D("7.5") % D(2), D("-7.5") % D(2), divmod(D(-7), D(2)))
print(D(0.1), D(1.5), D(-0.0), D(10 ** 30), D("  -12.3400e-2 "), D("1_000.5"), D((1, (3, 1, 4), -2)))
print(repr(D("-0")), D("Infinity"), D("-inf"), D("nan"), D("sNaN12"), D("NaN").is_nan(), D("1E+2"), D("0.000001"))
print(D("0.0000001"), D("123E-10"), D("1.23E+5"), D("100").to_eng_string(), D("1.23E+5").to_eng_string(),
      D("0E-7").to_eng_string(), D("123E-10").to_eng_string())
print(D(2).sqrt(), D(16).sqrt(), D("0.25").sqrt(), D(1).exp(), D(10).ln(), D(100).log10(), D(2).log10(), D("0.5").ln())
print(D(2) ** 10, D("1.5") ** 3, D(2) ** -2, D(4) ** D("0.5"), D(2) ** D("0.5"), D(-2) ** 3, D(10) ** 30,
      D(3) ** 100, pow(D(3), 4, 5))
print(D("3.14159").quantize(D("0.01")), D("2.675").quantize(D("0.01")), D("-2.5").quantize(D(1)),
      D("2.5").quantize(D(1), rounding=decimal.ROUND_HALF_UP), D("1.2345").quantize(D("1e-2"), decimal.ROUND_DOWN))
for r in [decimal.ROUND_UP, decimal.ROUND_DOWN, decimal.ROUND_CEILING, decimal.ROUND_FLOOR, decimal.ROUND_HALF_UP,
          decimal.ROUND_HALF_DOWN, decimal.ROUND_HALF_EVEN, decimal.ROUND_05UP]:
    print(r, [str(D(x).quantize(D(1), rounding=r)) for x in ["2.5", "-2.5", "1.1", "-1.1", "0.5", "5.01", "-0.4"]])
print(round(D("2.5")), round(D("3.5")), round(D("1.2345"), 2), int(D("-7.9")), float(D("1.25")), D("1.5").__floor__(),
      D("-1.5").__ceil__(), D("12.50").as_tuple(), D("0.75").as_integer_ratio(), D(-3).as_integer_ratio())
print(D("2.50").normalize(), D("1200").normalize(), D("0.00").normalize(), D("-1.0E+2").normalize(),
      D("1.23").adjusted(), D("123E5").adjusted(), D(5).compare(D(3)), D(1).max(D("1.0")), D(-1).min(D(2)))
print(D(1) == 1, D("1.0") == D(1), D("0.5") == 0.5, D("0.1") == 0.1, D("0.5") == Fraction(1, 2), D(2) < 3,
      D("NaN") == D("NaN"), D(1) < D("Infinity"), sorted([D(3), D("1.5"), D(-2), D("1.50")]))
print(hash(D(1)) == hash(1), hash(D("0.5")) == hash(0.5), hash(D("1.10")) == hash(D("1.1")), hash(D("Inf")),
      {D("1.0"): "a"}[D(1)], hash(D("-7.25")) == hash(-7.25))
c = getcontext()
print(c.prec, c.rounding, c)
with localcontext() as ctx:
    ctx.prec = 5
    print(D(1) / D(7), D("123456789") + 0, +D("1.23456789"), ctx.prec)
with localcontext(prec=3, rounding=decimal.ROUND_DOWN):
    print(D(2) / D(3), D("9.999") * 1)
print(getcontext().prec, Context(prec=6).create_decimal("3.14159265"), Context(prec=4).divide(1, 3))
c.clear_flags()
x = D(1) / D(3)
print(c.flags[decimal.Inexact], c.flags[decimal.Rounded], c.flags[decimal.DivisionByZero], c)
c.clear_flags()
for expr in [lambda: D(1) / 0, lambda: D(0) / 0, lambda: D("abc"), lambda: D("9e999999") * 10,
             lambda: D(-1).sqrt(), lambda: D("NaN") < 1, lambda: D(1).quantize(D("1e-30")), lambda: D(1) % 0,
             lambda: int(D("NaN")), lambda: int(D("Inf"))]:
    try:
        print(expr())
    except (decimal.DecimalException, ValueError, OverflowError) as e:
        print(type(e).__name__, e)
c.clear_flags()
ext = decimal.ExtendedContext.copy()
print(ext.divide(1, 0), ext.divide(0, 0), ext.sqrt(-1), ext.flags[decimal.DivisionByZero],
      ext.flags[decimal.InvalidOperation])
for spec in [",.2f", "e", ".3e", "%", "", "012,.1f", "^10", "+.1f", "g", ".2g", "_.1f", "E", "<8.2f|", "08.3f"]:
    for v in ["1234567.891", "-0.000123", "12.5", "1e10", "0"]:
        try:
            print(repr(format(D(v), spec.rstrip("|"))), end=" ")
        except ValueError as e:
            print("ValueError", end=" ")
    print()
print(f"{D('3.14159'):.3f}", f"{D('-0.0'):z.1f}", f"{D('Infinity'):>10}", f"{D('NaN'):+}")
print(D("1.5").is_signed(), D(-0).is_zero(), D("1e-1000000").is_subnormal(), D(5).number_class(),
      D("-Inf").number_class(), D("0.1").same_quantum(D("0.2")), D(3).copy_sign(D(-1)), D("1.5").fma(2, D("0.25")),
      D(7).remainder_near(D(4)), D(18).scaleb(-2), D(1234).logb())
print(D(2) + 1, 1 + D(2), 3 - D("0.5"), 2 * D("1.5"), 1 / D(8), 7 // D(2), 7 % D(3), -D(5), abs(D("-2.5")),
      D("1.5") ** 2, 2 ** D(3), bool(D(0)), bool(D("0.1")))
try:
    D(1) + 1.5
except TypeError as e:
    print("TypeError")
print(D.from_float(0.25), D.from_float(1e100), isinstance(D(1), decimal.Decimal),
      issubclass(decimal.DivisionByZero, ZeroDivisionError), issubclass(decimal.FloatOperation, TypeError))
import numbers
print(isinstance(D(1), numbers.Number), isinstance(D(1), numbers.Real), D(1).real, D(2).imag)
