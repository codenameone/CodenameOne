# statistics and cmath.
import statistics as st
import cmath
from fractions import Fraction as F

d = [2.75, 1.75, 1.25, 0.25, 0.5, 1.25, 3.5]
print(st.mean([1, 2, 3, 4, 4]), st.mean(d), st.mean([F(3, 7), F(1, 21), F(5, 3)]), st.fmean([3.5, 4.0, 5.25]), st.fmean([1, 2, 3], weights=[0.25, 0.5, 0.25]))
print(st.geometric_mean([54, 24, 36]), st.harmonic_mean([40, 60]), st.harmonic_mean([40, 60], weights=[5, 30]), st.harmonic_mean([2.5, 3, 10]))
print(st.median([1, 3, 5]), st.median([1, 3, 5, 7]), st.median_low([1, 3, 5, 7]), st.median_high([1, 3, 5, 7]), st.median_grouped([52, 52, 53, 54]), st.median_grouped([1, 3, 3, 5, 7], interval=2))
print(st.mode([1, 1, 2, 3, 3, 3, 3, 4]), st.mode(["red", "blue", "blue", "red", "green", "red", "red"]), st.multimode("aabbbbccddddeeffffgg"), st.multimode([]))
print(st.variance([2.75, 1.75, 1.25, 0.25, 0.5, 1.25, 3.5]), st.variance([1, 2, 3, 4]), st.variance([F(1, 6), F(1, 2), F(5, 3)]), st.pvariance([0.0, 0.25, 0.25, 1.25, 1.5, 1.75, 2.75, 3.25]))
print(st.stdev([1.5, 2.5, 2.5, 2.75, 3.25, 4.75]), st.pstdev([1.5, 2.5, 2.5, 2.75, 3.25, 4.75]), st.stdev([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]), st.variance(d, st.mean(d)))
print(st.quantiles([105, 129, 87, 86, 111, 111, 89, 81, 108, 92, 110, 100, 75, 105, 103, 109, 76, 119, 99, 91, 103, 129, 106, 101, 84, 111, 74, 87, 86, 103, 103, 106, 86, 111, 75, 87, 102, 121, 111, 88, 89, 101, 106, 95, 103, 107, 101, 81, 109, 104], n=10))
print(st.quantiles([1, 2, 3, 4, 5], n=4, method="inclusive"), st.quantiles([10], n=3))
x = [1, 2, 3, 4, 5, 6, 7, 8, 9]
y = [1, 2, 3, 1, 2, 3, 1, 2, 3]
print(st.covariance(x, y), st.correlation(x, x[::-1]), st.correlation(x, y), st.correlation([1, 2, 3, 4], [1, 4, 9, 16], method="ranked"))
print(st.linear_regression([1971, 1975, 1979, 1982, 1983], [1, 2, 3, 4, 5]), st.linear_regression([1, 2, 3], [2, 4, 6], proportional=True))
nd = st.NormalDist(100, 15)
# erf/erfc are the C library's in CPython, so their last bit is the platform's.
print(nd, round(nd.cdf(130), 13), nd.pdf(100), nd.inv_cdf(0.975), [round(q, 11) for q in nd.quantiles()], nd.zscore(115), round(nd.overlap(st.NormalDist(110, 10)), 13), nd + 5, nd * 2, nd.variance)
print(st.NormalDist.from_samples([2.5, 3.1, 2.1, 2.4, 2.7, 3.5]))
for f, a in [(st.mean, []), (st.variance, [1]), (st.median, []), (st.mode, []), (st.geometric_mean, [1, -2])]:
    try:
        f(a)
    except st.StatisticsError as e:
        print(type(e).__name__, e)
z = complex(1.5, -2.25)
for name in ["sqrt", "exp", "log", "log10", "sin", "cos", "tan", "sinh", "cosh", "tanh", "asin", "acos", "atan", "asinh", "acosh", "atanh"]:
    r = getattr(cmath, name)(z)
    print(name, round(r.real, 12), round(r.imag, 12))
print(cmath.phase(-1), cmath.polar(1j), cmath.rect(2, cmath.pi / 2).real < 1e-15, cmath.sqrt(-4), cmath.isclose(1 + 1j, 1.0000000001 + 1j), cmath.isinf(cmath.infj), cmath.log(8, 2))
