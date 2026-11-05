# The math functions beyond the elementary ones.
import math

def r(x):
    return float("%.12g" % x)

for f in ["sinh", "cosh", "tanh", "asinh", "atanh", "expm1", "log1p", "erf", "erfc", "cbrt", "exp2"]:
    print(f, [r(getattr(math, f)(x)) for x in [-0.75, -1e-9, 0.0, 1e-9, 0.3, 0.9]])
print([r(math.acosh(x)) for x in [1, 1.5, 10, 1e10]])
print([r(math.erf(x)) for x in [1.6, 3, 6]], [r(math.erfc(x)) for x in [1.6, 3, 6, -2]])
print([r(math.gamma(x)) for x in [0.5, 1, 5, 7.5, -0.5, -2.5, 30.2]], math.gamma(4))
print([r(math.lgamma(x)) for x in [0.5, 1, 2, 3, 10.5, 100, -0.5, -2.5]])
print(math.frexp(8.0), math.frexp(-0.15625), math.frexp(0.0), math.frexp(5e-324), math.ldexp(0.5, 4), math.ldexp(1, -1074), math.ldexp(3, 1))
print(math.modf(3.75), math.modf(-3.75), math.modf(2.0), math.modf(math.inf))
print(math.remainder(10, 3), math.remainder(11, 3), math.remainder(-7, 2), math.remainder(5, 2), math.remainder(7, 2), math.remainder(4.5, 1))
print(math.nextafter(1.0, 2.0), math.nextafter(1.0, 0.0), math.nextafter(0.0, 1.0), math.nextafter(1.0, 2.0, steps=3), math.ulp(1.0), math.ulp(1e300), math.ulp(0.0))
print(math.dist((0, 0), (3, 4)), math.dist([1, 2, 3], [4, 6, 3]), math.lcm(4, 6), math.lcm(), math.lcm(3, 0), math.lcm(2, 3, 4))
print(math.perm(5), math.perm(5, 2), math.perm(3, 5), math.sumprod([1, 2, 3], [4, 5, 6]), math.sumprod([1.5, 2], [2, 2]), math.fma(2.0, 3.0, 1.0))
print(math.tanh(100), math.tanh(-0.0), math.sinh(-0.0), math.cbrt(-27), math.cbrt(8))
for f, x in [(math.acosh, 0.5), (math.atanh, 1), (math.gamma, 0), (math.gamma, -3), (math.lgamma, -2), (math.log1p, -2)]:
    try:
        f(x)
    except ValueError as e:
        print(f.__name__, e)
for f, x in [(math.sinh, 1000), (math.exp2, 2000), (math.gamma, 200)]:
    try:
        f(x)
    except OverflowError as e:
        print(f.__name__, e)
try:
    math.ldexp(1.0, 1.5)
except TypeError as e:
    print(e)
try:
    math.perm(-1)
except ValueError as e:
    print(e)
print([math.ldexp(*a) for a in [(1.5, -1074), (0.75, -1074), (1.0000000000000002, -1075), (1.0, -1075),
                                (5e-324, 1), (5e-324, 1074), (1.7976931348623157e308, -2000), (-3.0, -1073),
                                (0.9999999999999999, -1021), (1.0, 1023), (2.5, -10**6)]])
try:
    math.ldexp(1.0, 1024)
except OverflowError as e:
    print("OverflowError", e)
