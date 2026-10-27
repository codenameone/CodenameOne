# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Basic statistics: averages, spread and relations of numeric data.

Sums are exact (over fractions) and converted once at the end, as
CPython's statistics module does, so results match it to the last bit.
"""

import math
import numbers
from fractions import Fraction
from collections import Counter, namedtuple

__all__ = ["NormalDist", "StatisticsError", "correlation", "covariance", "fmean", "geometric_mean",
           "harmonic_mean", "linear_regression", "mean", "median", "median_grouped", "median_high",
           "median_low", "mode", "multimode", "pstdev", "pvariance", "quantiles", "stdev", "variance"]


class StatisticsError(ValueError):
    pass


def _exact_ratio(x):
    try:
        return x.as_integer_ratio()
    except AttributeError:
        pass
    except (OverflowError, ValueError):
        return (x, None)
    try:
        return (x.numerator, x.denominator)
    except AttributeError:
        raise TypeError("can't convert type '%s' to numerator/denominator" % type(x).__name__)


def _coerce(T, S):
    if T is S:
        return T
    if S is int or S is bool:
        return T
    if T is int:
        return S
    if issubclass(S, T):
        return S
    if issubclass(T, S):
        return T
    if issubclass(T, int):
        return S
    if issubclass(S, int):
        return T
    if issubclass(T, Fraction) and issubclass(S, float):
        return S
    if issubclass(T, float) and issubclass(S, Fraction):
        return T
    raise TypeError("can't convert type '%s' to '%s'" % (T.__name__, S.__name__))


def _sum(data):
    count = 0
    types = set()
    partials = {}
    T = int
    for typ, values in _groupby_type(data):
        types.add(typ)
        for n, d in map(_exact_ratio, values):
            count += 1
            partials[d] = partials.get(d, 0) + n
    if None in partials:
        total = partials[None]
        assert not _isfinite(total)
    else:
        total = sum(Fraction(n, d) for d, n in partials.items())
    for typ in types:
        T = _coerce(T, typ)
    return (T, total, count)


def _groupby_type(data):
    out = []
    cur = None
    group = []
    for x in data:
        t = type(x)
        if t is not cur:
            if group:
                out.append((cur, group))
            cur = t
            group = []
        group.append(x)
    if group:
        out.append((cur, group))
    return out


def _isfinite(x):
    try:
        return x.is_finite()
    except AttributeError:
        return math.isfinite(x)


def _ss(data, c=None):
    if c is not None:
        T, ssd, count = _sum((d := x - c) * d for x in data)
        return (T, ssd, c, count)
    count = 0
    types = set()
    sx_partials = {}
    sxx_partials = {}
    for x in data:
        types.add(type(x))
        n, d = _exact_ratio(x)
        count += 1
        sx_partials[d] = sx_partials.get(d, 0) + n
        sxx_partials[d * d] = sxx_partials.get(d * d, 0) + n * n
    if not count:
        ssd = c = Fraction(0)
    elif None in sx_partials:
        ssd = c = sx_partials[None]
    else:
        sx = sum(Fraction(n, d) for d, n in sx_partials.items())
        sxx = sum(Fraction(n, d) for d, n in sxx_partials.items())
        ssd = (count * sxx - sx * sx) / count
        c = sx / count
    T = int
    for typ in types:
        T = _coerce(T, typ)
    return (T, ssd, c, count)


def _convert(value, T):
    if type(value) is T:
        return value
    if issubclass(T, int) and value.denominator != 1:
        T = float
    try:
        return T(value)
    except TypeError:
        if issubclass(T, Decimal_type()):
            return T(value.numerator) / T(value.denominator)
        raise


def Decimal_type():
    try:
        from decimal import Decimal
        return Decimal
    except ImportError:
        return type(None)


def _fail_neg(values, errmsg="negative value"):
    for x in values:
        if x < 0:
            raise StatisticsError(errmsg)
        yield x


def _integer_sqrt_of_frac_rto(n, m):
    a = math.isqrt(n // m)
    return a | (a * a * m != n)


_sqrt_bit_width = 2 * 53 + 3


def _float_sqrt_of_frac(n, m):
    q = (n.bit_length() - m.bit_length() - _sqrt_bit_width) // 2
    if q >= 0:
        numerator = _integer_sqrt_of_frac_rto(n, m << 2 * q) << q
        denominator = 1
    else:
        numerator = _integer_sqrt_of_frac_rto(n << -2 * q, m)
        denominator = 1 << -q
    return numerator / denominator


def mean(data):
    T, total, n = _sum(data)
    if n < 1:
        raise StatisticsError("mean requires at least one data point")
    return _convert(total / n, T)


def fmean(data, weights=None):
    if weights is None:
        n = 0
        total = 0.0
        vals = []
        for x in data:
            n += 1
            vals.append(x)
        if not n:
            raise StatisticsError("fmean requires at least one data point")
        return math.fsum(vals) / n
    data = list(data)
    weights = list(weights)
    if len(weights) != len(data):
        raise StatisticsError("data and weights must be the same length")
    num = math.fsum(x * w for x, w in zip(data, weights))
    den = math.fsum(weights)
    if not den:
        raise StatisticsError("sum of weights must be non-zero")
    return num / den


def geometric_mean(data):
    n = 0
    found_zero = False
    logs = []
    for x in data:
        n += 1
        if x > 0.0:
            logs.append(math.log(x))
        elif x == 0.0:
            found_zero = True
        else:
            raise StatisticsError("No negative inputs allowed", x)
    if not n:
        raise StatisticsError("Must have a non-empty dataset")
    if found_zero:
        return 0.0
    return math.exp(math.fsum(logs) / n)


def harmonic_mean(data, weights=None):
    data = list(data)
    errmsg = "harmonic mean does not support negative values"
    n = len(data)
    if n < 1:
        raise StatisticsError("harmonic_mean requires at least one data point")
    elif n == 1 and weights is None:
        x = data[0]
        if isinstance(x, (numbers.Real, Decimal_type())):
            if x < 0:
                raise StatisticsError(errmsg)
            return x
        raise TypeError("unsupported type")
    if weights is None:
        weights = [1] * n
        sum_weights = n
    else:
        weights = list(weights)
        if len(weights) != n:
            raise StatisticsError("Number of weights does not match data size")
        _, sum_weights, _ = _sum(w for w in _fail_neg(weights, errmsg))
    try:
        data = list(_fail_neg(data, errmsg))
        T, total, count = _sum(w / x if w else 0 for w, x in zip(weights, data))
    except ZeroDivisionError:
        return 0
    if total <= 0:
        raise StatisticsError("Weighted sum must be positive")
    return _convert(sum_weights / total, T)


def median(data):
    data = sorted(data)
    n = len(data)
    if n == 0:
        raise StatisticsError("no median for empty data")
    if n % 2 == 1:
        return data[n // 2]
    i = n // 2
    return (data[i - 1] + data[i]) / 2


def median_low(data):
    data = sorted(data)
    n = len(data)
    if n == 0:
        raise StatisticsError("no median for empty data")
    if n % 2 == 1:
        return data[n // 2]
    return data[n // 2 - 1]


def median_high(data):
    data = sorted(data)
    n = len(data)
    if n == 0:
        raise StatisticsError("no median for empty data")
    return data[n // 2]


def median_grouped(data, interval=1.0):
    data = sorted(data)
    n = len(data)
    if not n:
        raise StatisticsError("no median for empty data")
    x = data[n // 2]
    i = j = n // 2
    while i > 0 and data[i - 1] == x:
        i -= 1
    while j < n and data[j] == x:
        j += 1
    try:
        interval = float(interval)
        x = float(x)
    except ValueError:
        raise TypeError("Value cannot be converted to a float")
    L = x - interval / 2.0
    cf = i
    f = j - i
    return L + interval * (n / 2 - cf) / f


def mode(data):
    pairs = Counter(iter(data)).most_common(1)
    try:
        return pairs[0][0]
    except IndexError:
        raise StatisticsError("no mode for empty data")


def multimode(data):
    counts = Counter(iter(data))
    if not counts:
        return []
    maxcount = max(counts.values())
    return [value for value, count in counts.items() if count == maxcount]


def quantiles(data, *, n=4, method="exclusive"):
    if n < 1:
        raise StatisticsError("n must be at least 1")
    data = sorted(data)
    ld = len(data)
    if ld < 2:
        if ld == 1:
            return data * (n - 1)
        raise StatisticsError("must have at least one data point")
    if method == "inclusive":
        m = ld - 1
        result = []
        for i in range(1, n):
            j, delta = divmod(i * m, n)
            interpolated = (data[j] * (n - delta) + data[j + 1] * delta) / n
            result.append(interpolated)
        return result
    if method == "exclusive":
        m = ld + 1
        result = []
        for i in range(1, n):
            j = i * m // n
            j = 1 if j < 1 else ld - 1 if j > ld - 1 else j
            delta = i * m - j * n
            interpolated = (data[j - 1] * (n - delta) + data[j] * delta) / n
            result.append(interpolated)
        return result
    raise ValueError("Unknown method: %r" % (method,))


def variance(data, xbar=None):
    T, ss, c, n = _ss(data, xbar)
    if n < 2:
        raise StatisticsError("variance requires at least two data points")
    return _convert(ss / (n - 1), T)


def pvariance(data, mu=None):
    T, ss, c, n = _ss(data, mu)
    if n < 1:
        raise StatisticsError("pvariance requires at least one data point")
    return _convert(ss / n, T)


def stdev(data, xbar=None):
    T, ss, c, n = _ss(data, xbar)
    if n < 2:
        raise StatisticsError("stdev requires at least two data points")
    mss = ss / (n - 1)
    if issubclass(T, Decimal_type()):
        return _convert(mss, T).sqrt()
    return _float_sqrt_of_frac(mss.numerator, mss.denominator)


def pstdev(data, mu=None):
    T, ss, c, n = _ss(data, mu)
    if n < 1:
        raise StatisticsError("pstdev requires at least one data point")
    mss = ss / n
    if issubclass(T, Decimal_type()):
        return _convert(mss, T).sqrt()
    return _float_sqrt_of_frac(mss.numerator, mss.denominator)


def _sqrtprod(x, y):
    h = math.sqrt(x * y)
    if not math.isfinite(h):
        if math.isinf(h) and not math.isinf(x) and not math.isinf(y):
            scale = 2.0 ** -512
            return _sqrtprod(scale * x, scale * y) / scale
        return h
    if not h:
        if x and y:
            scale = 2.0 ** 537
            return _sqrtprod(scale * x, scale * y) / scale
        return h
    d = Fraction(*h.as_integer_ratio()) ** 2 - Fraction(*x.as_integer_ratio()) * Fraction(*y.as_integer_ratio())
    return h - float(d) / (2.0 * h)


def covariance(x, y, /):
    n = len(x)
    if len(y) != n:
        raise StatisticsError("covariance requires that both inputs have same number of data points")
    if n < 2:
        raise StatisticsError("covariance requires at least two data points")
    xbar = fmean(x)
    ybar = fmean(y)
    sxy = math.fsum((xi - xbar) * (yi - ybar) for xi, yi in zip(x, y))
    return sxy / (n - 1)


def _ranks(data):
    order = sorted(range(len(data)), key=lambda i: data[i])
    result = [0.0] * len(data)
    i = 0
    while i < len(order):
        j = i
        while j + 1 < len(order) and data[order[j + 1]] == data[order[i]]:
            j += 1
        rank = (i + j) / 2 + 1
        for k in range(i, j + 1):
            result[order[k]] = rank
        i = j + 1
    return result


def correlation(x, y, /, *, method="linear"):
    n = len(x)
    if len(y) != n:
        raise StatisticsError("correlation requires that both inputs have same number of data points")
    if n < 2:
        raise StatisticsError("correlation requires at least two data points")
    if method not in {"linear", "ranked"}:
        raise ValueError("Unknown method: %r" % (method,))
    if method == "ranked":
        start = (n - 1) / -2
        x = [r + start - 1 for r in _ranks(x)]
        y = [r + start - 1 for r in _ranks(y)]
    else:
        xbar = fmean(x)
        ybar = fmean(y)
        x = [xi - xbar for xi in x]
        y = [yi - ybar for yi in y]
    sxy = math.sumprod(x, y)
    sxx = math.sumprod(x, x)
    syy = math.sumprod(y, y)
    try:
        return sxy / _sqrtprod(sxx, syy)
    except ZeroDivisionError:
        raise StatisticsError("at least one of the inputs is constant")


LinearRegression = namedtuple("LinearRegression", ("slope", "intercept"))


def linear_regression(x, y, /, *, proportional=False):
    n = len(x)
    if len(y) != n:
        raise StatisticsError("linear regression requires that both inputs have same number of data points")
    if n < 2:
        raise StatisticsError("linear regression requires at least two data points")
    if not proportional:
        xbar = fmean(x)
        ybar = fmean(y)
        x = [xi - xbar for xi in x]
        y = [yi - ybar for yi in y]
    sxy = math.sumprod(x, y) + 0.0
    sxx = math.sumprod(x, x)
    try:
        slope = sxy / sxx
    except ZeroDivisionError:
        raise StatisticsError("x is constant")
    intercept = 0.0 if proportional else ybar - slope * xbar
    return LinearRegression(slope=slope, intercept=intercept)


def _normal_dist_inv_cdf(p, mu, sigma):
    q = p - 0.5
    if math.fabs(q) <= 0.425:
        r = 0.180625 - q * q
        num = (((((((2.5090809287301226727e+3 * r + 3.3430575583588128105e+4) * r + 6.7265770927008700853e+4) * r
                   + 4.5921953931549871457e+4) * r + 1.3731693765509461125e+4) * r + 1.9715909503065514427e+3) * r
                + 1.3314166789178437745e+2) * r + 3.3871328727963666080e+0) * q
        den = (((((((5.2264952788528545610e+3 * r + 2.8729085735721942674e+4) * r + 3.9307895800092710610e+4) * r
                   + 2.1213794301586595867e+4) * r + 5.3941960214247511077e+3) * r + 6.8718700749205790830e+2) * r
                + 4.2313330701600911252e+1) * r + 1.0)
        x = num / den
        return mu + (x * sigma)
    r = p if q <= 0.0 else 1.0 - p
    r = math.sqrt(-math.log(r))
    if r <= 5.0:
        r = r - 1.6
        num = (((((((7.74545014278341407640e-4 * r + 2.27238449892691845833e-2) * r + 2.41780725177450611770e-1) * r
                   + 1.27045825245236838258e+0) * r + 3.64784832476320460504e+0) * r + 5.76949722146069140550e+0) * r
                + 4.63033784615654529590e+0) * r + 1.42343711074968357734e+0)
        den = (((((((1.05075007164441684324e-9 * r + 5.47593808499534494600e-4) * r + 1.51986665636164571966e-2) * r
                   + 1.48103976427480074590e-1) * r + 6.89767334985100004550e-1) * r + 1.67638483018380384940e+0) * r
                + 2.05319162663775882187e+0) * r + 1.0)
    else:
        r = r - 5.0
        num = (((((((2.01033439929228813265e-7 * r + 2.71155556874348757815e-5) * r + 1.24266094738807843860e-3) * r
                   + 2.65321895265761230930e-2) * r + 2.96560571828504891230e-1) * r + 1.78482653991729133580e+0) * r
                + 5.46378491116411436990e+0) * r + 6.65790464350110377720e+0)
        den = (((((((2.04426310338993978564e-15 * r + 1.42151175831644588870e-7) * r + 1.84631831751005468180e-5) * r
                   + 7.86869131145613259100e-4) * r + 1.48753612908506148525e-2) * r + 1.36929880922735805310e-1) * r
                + 5.99832206555887937690e-1) * r + 1.0)
    x = num / den
    if q < 0.0:
        x = -x
    return mu + (x * sigma)


class NormalDist:
    __slots__ = {"_mu": "Arithmetic mean of a normal distribution",
                 "_sigma": "Standard deviation of a normal distribution"}

    def __init__(self, mu=0.0, sigma=1.0):
        if sigma < 0.0:
            raise StatisticsError("sigma must be non-negative")
        self._mu = float(mu)
        self._sigma = float(sigma)

    @classmethod
    def from_samples(cls, data):
        return cls(*_mean_stdev(data))

    def samples(self, n, *, seed=None):
        import random
        rnd = random.random if seed is None else random.Random(seed).random
        mu, sigma = self._mu, self._sigma
        return [_normal_dist_inv_cdf(rnd(), mu, sigma) for i in range(n)]

    def pdf(self, x):
        variance = self._sigma * self._sigma
        if not variance:
            raise StatisticsError("pdf() not defined when sigma is zero")
        diff = x - self._mu
        return math.exp(diff * diff / (-2.0 * variance)) / math.sqrt(2.0 * math.pi * variance)

    def cdf(self, x):
        if not self._sigma:
            raise StatisticsError("cdf() not defined when sigma is zero")
        return 0.5 * math.erfc((self._mu - x) / (self._sigma * math.sqrt(2.0)))

    def inv_cdf(self, p):
        if p <= 0.0 or p >= 1.0:
            raise StatisticsError("p must be in the range 0.0 < p < 1.0")
        return _normal_dist_inv_cdf(p, self._mu, self._sigma)

    def quantiles(self, n=4):
        return [self.inv_cdf(i / n) for i in range(1, n)]

    def overlap(self, other):
        if not isinstance(other, NormalDist):
            raise TypeError("Expected another NormalDist instance")
        X, Y = self, other
        if (Y._sigma, Y._mu) < (X._sigma, X._mu):
            X, Y = Y, X
        X_var, Y_var = X.variance, Y.variance
        if not X_var or not Y_var:
            raise StatisticsError("overlap() not defined when sigma is zero")
        dv = Y_var - X_var
        dm = math.fabs(Y._mu - X._mu)
        if not dv:
            return 2.0 * NormalDist(dm, 2.0 * X._sigma).cdf(0)
        a = X._mu * Y_var - Y._mu * X_var
        b = X._sigma * Y._sigma * math.sqrt(dm * dm + dv * math.log(Y_var / X_var))
        x1 = (a + b) / dv
        x2 = (a - b) / dv
        return 1.0 - (math.fabs(Y.cdf(x1) - X.cdf(x1)) + math.fabs(Y.cdf(x2) - X.cdf(x2)))

    def zscore(self, x):
        if not self._sigma:
            raise StatisticsError("zscore() not defined when sigma is zero")
        return (x - self._mu) / self._sigma

    @property
    def mean(self):
        return self._mu

    @property
    def median(self):
        return self._mu

    @property
    def mode(self):
        return self._mu

    @property
    def stdev(self):
        return self._sigma

    @property
    def variance(self):
        return self._sigma * self._sigma

    def __add__(x1, x2):
        if isinstance(x2, NormalDist):
            return NormalDist(x1._mu + x2._mu, math.hypot(x1._sigma, x2._sigma))
        return NormalDist(x1._mu + x2, x1._sigma)

    def __sub__(x1, x2):
        if isinstance(x2, NormalDist):
            return NormalDist(x1._mu - x2._mu, math.hypot(x1._sigma, x2._sigma))
        return NormalDist(x1._mu - x2, x1._sigma)

    def __mul__(x1, x2):
        return NormalDist(x1._mu * x2, x1._sigma * math.fabs(x2))

    def __truediv__(x1, x2):
        return NormalDist(x1._mu / x2, x1._sigma / math.fabs(x2))

    def __pos__(x1):
        return NormalDist(x1._mu, x1._sigma)

    def __neg__(x1):
        return NormalDist(-x1._mu, x1._sigma)

    __radd__ = __add__

    def __rsub__(x1, x2):
        return -(x1 - x2)

    __rmul__ = __mul__

    def __eq__(x1, x2):
        if not isinstance(x2, NormalDist):
            return NotImplemented
        return x1._mu == x2._mu and x1._sigma == x2._sigma

    def __hash__(self):
        return hash((self._mu, self._sigma))

    def __repr__(self):
        return "%s(mu=%r, sigma=%r)" % (type(self).__name__, self._mu, self._sigma)


def _mean_stdev(data):
    T, ss, xbar, n = _ss(data)
    if n < 2:
        raise StatisticsError("stdev requires at least two data points")
    mss = ss / (n - 1)
    try:
        return float(xbar), _float_sqrt_of_frac(mss.numerator, mss.denominator)
    except AttributeError:
        return float(xbar), float(xbar) / float(ss)
