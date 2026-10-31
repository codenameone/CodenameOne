# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Pseudo-random numbers from a Mersenne Twister (MT19937).

A seeded generator produces the same sequence CPython does: the seeding,
the 53-bit float construction and the rejection sampling of `_randbelow`
follow the published algorithms the reference implementation uses, so a
program that prints seeded draws prints the same values on every target.
"""

from math import log as _log, exp as _exp, sqrt as _sqrt, cos as _cos, sin as _sin, pi as _pi
from math import floor as _floor, ceil as _ceil, isfinite as _isfinite, acos as _acos
from itertools import accumulate as _accumulate, repeat as _repeat
from bisect import bisect as _bisect

__all__ = ["Random", "SystemRandom", "seed", "random", "uniform", "randint", "choice", "sample",
           "randrange", "shuffle", "normalvariate", "lognormvariate", "expovariate",
           "vonmisesvariate", "gammavariate", "triangular", "gauss", "betavariate",
           "paretovariate", "weibullvariate", "getstate", "setstate", "getrandbits", "choices",
           "randbytes", "binomialvariate"]

NV_MAGICCONST = 4 * _exp(-0.5) / _sqrt(2.0)
TWOPI = 2.0 * _pi
LOG4 = _log(4.0)
SG_MAGICCONST = 1.0 + _log(4.5)
_N = 624
_M = 397
_MASK = 0xFFFFFFFF


def _index(v):
    if isinstance(v, int):
        return int(v)
    if hasattr(type(v), "__index__"):
        return v.__index__()
    raise TypeError("'%s' object cannot be interpreted as an integer" % type(v).__name__)


class Random:
    VERSION = 3

    def __init__(self, x=None):
        self._mt = [0] * _N
        self._mti = _N + 1
        self.gauss_next = None
        self.seed(x)

    def _init_genrand(self, s):
        mt = self._mt
        mt[0] = s & _MASK
        for i in range(1, _N):
            prev = mt[i - 1]
            mt[i] = (1812433253 * (prev ^ (prev >> 30)) + i) & _MASK
        self._mti = _N

    def _init_by_array(self, key):
        self._init_genrand(19650218)
        mt = self._mt
        i = 1
        j = 0
        klen = len(key)
        k = _N if _N > klen else klen
        while k:
            prev = mt[i - 1]
            mt[i] = ((mt[i] ^ ((prev ^ (prev >> 30)) * 1664525)) + key[j] + j) & _MASK
            i += 1
            j += 1
            if i >= _N:
                mt[0] = mt[_N - 1]
                i = 1
            if j >= klen:
                j = 0
            k -= 1
        k = _N - 1
        while k:
            prev = mt[i - 1]
            mt[i] = ((mt[i] ^ ((prev ^ (prev >> 30)) * 1566083941)) - i) & _MASK
            i += 1
            if i >= _N:
                mt[0] = mt[_N - 1]
                i = 1
            k -= 1
        mt[0] = 0x80000000

    def _genrand(self):
        mt = self._mt
        if self._mti >= _N:
            for kk in range(_N):
                y = (mt[kk] & 0x80000000) | (mt[(kk + 1) % _N] & 0x7FFFFFFF)
                v = mt[(kk + _M) % _N] ^ (y >> 1)
                if y & 1:
                    v ^= 0x9908B0DF
                mt[kk] = v
            self._mti = 0
        y = mt[self._mti]
        self._mti += 1
        y ^= y >> 11
        y ^= (y << 7) & 0x9D2C5680
        y ^= (y << 15) & 0xEFC60000
        y ^= y >> 18
        return y

    def seed(self, a=None, version=2):
        if a is None:
            import time
            a = int(time.time() * 1000000) ^ id(self)
        elif isinstance(a, (str, bytes)) and version == 2:
            if isinstance(a, str):
                a = a.encode("utf-8")
            from hashlib import sha512
            a = int.from_bytes(a + sha512(a).digest(), "big")
        elif isinstance(a, float):
            a = hash(a)
        elif not isinstance(a, int):
            raise TypeError("The only supported seed types are:\nNone, int, float, str, bytes, "
                            "and bytearray.")
        a = abs(int(a))
        key = []
        while a:
            key.append(a & _MASK)
            a >>= 32
        if not key:
            key.append(0)
        self._init_by_array(key)
        self.gauss_next = None

    def getstate(self):
        return (self.VERSION, tuple(self._mt) + (self._mti,), self.gauss_next)

    def setstate(self, state):
        version, internal, self.gauss_next = state
        if version != self.VERSION or len(internal) != _N + 1:
            raise ValueError("state with version %s passed to Random.setstate() of version %s"
                             % (version, self.VERSION))
        self._mt = list(internal[:_N])
        self._mti = internal[_N]

    def random(self):
        a = self._genrand() >> 5
        b = self._genrand() >> 6
        return (a * 67108864.0 + b) * (1.0 / 9007199254740992.0)

    def getrandbits(self, k):
        if k < 0:
            raise ValueError("number of bits must be non-negative")
        if k == 0:
            return 0
        if k <= 32:
            return self._genrand() >> (32 - k)
        result = 0
        shift = 0
        while k > 0:
            r = self._genrand()
            if k < 32:
                r >>= 32 - k
            result |= r << shift
            shift += 32
            k -= 32
        return result

    def randbytes(self, n):
        return self.getrandbits(n * 8).to_bytes(n, "little")

    def _randbelow(self, n):
        k = n.bit_length()
        r = self.getrandbits(k)
        while r >= n:
            r = self.getrandbits(k)
        return r

    def randrange(self, start, stop=None, step=1):
        istart = _index(start)
        if stop is None:
            if step != 1:
                raise TypeError("Missing a non-None stop argument")
            if istart > 0:
                return self._randbelow(istart)
            raise ValueError("empty range for randrange()")
        istop = _index(stop)
        width = istop - istart
        istep = _index(step)
        if istep == 1:
            if width > 0:
                return istart + self._randbelow(width)
            raise ValueError("empty range in randrange(%s, %s)" % (start, stop))
        if istep > 0:
            n = (width + istep - 1) // istep
        elif istep < 0:
            n = (width + istep + 1) // istep
        else:
            raise ValueError("zero step for randrange()")
        if n <= 0:
            raise ValueError("empty range in randrange(%s, %s, %s)" % (start, stop, step))
        return istart + istep * self._randbelow(n)

    def randint(self, a, b):
        return self.randrange(a, b + 1)

    def choice(self, seq):
        if not len(seq):
            raise IndexError("Cannot choose from an empty sequence")
        return seq[self._randbelow(len(seq))]

    def shuffle(self, x):
        randbelow = self._randbelow
        for i in reversed(range(1, len(x))):
            j = randbelow(i + 1)
            x[i], x[j] = x[j], x[i]

    def sample(self, population, k, *, counts=None):
        if isinstance(population, (set, frozenset, dict)):
            raise TypeError("Population must be a sequence.  For dicts or sets, use sorted(d).")
        n = len(population)
        if counts is not None:
            cum_counts = list(_accumulate(counts))
            if len(cum_counts) != n:
                raise ValueError("The number of counts does not match the population")
            total = cum_counts.pop() if cum_counts else 0
            if not isinstance(total, int):
                raise TypeError("Counts must be integers")
            if total < 0:
                raise ValueError("Counts must be non-negative")
            selections = self.sample(range(total), k=k)
            return [population[_bisect(cum_counts, s)] for s in selections]
        randbelow = self._randbelow
        if not 0 <= k <= n:
            raise ValueError("Sample larger than population or is negative")
        result = [None] * k
        setsize = 21
        if k > 5:
            setsize += 4 ** _ceil(_log(k * 3, 4))
        if n <= setsize:
            pool = list(population)
            for i in range(k):
                j = randbelow(n - i)
                result[i] = pool[j]
                pool[j] = pool[n - i - 1]
        else:
            selected = set()
            for i in range(k):
                j = randbelow(n)
                while j in selected:
                    j = randbelow(n)
                selected.add(j)
                result[i] = population[j]
        return result

    def choices(self, population, weights=None, *, cum_weights=None, k=1):
        random = self.random
        n = len(population)
        if cum_weights is None:
            if weights is None:
                n += 0.0
                return [population[_floor(random() * n)] for i in _repeat(None, k)]
            cum_weights = list(_accumulate(weights))
        elif weights is not None:
            raise TypeError("Cannot specify both weights and cumulative weights")
        if len(cum_weights) != n:
            raise ValueError("The number of weights does not match the population")
        total = cum_weights[-1] + 0.0
        if total <= 0.0:
            raise ValueError("Total of weights must be greater than zero")
        if not _isfinite(total):
            raise ValueError("Total of weights must be finite")
        hi = n - 1
        return [population[_bisect(cum_weights, random() * total, 0, hi)] for i in _repeat(None, k)]

    def uniform(self, a, b):
        return a + (b - a) * self.random()

    def triangular(self, low=0.0, high=1.0, mode=None):
        u = self.random()
        try:
            c = 0.5 if mode is None else (mode - low) / (high - low)
        except ZeroDivisionError:
            return low
        if u > c:
            u = 1.0 - u
            c = 1.0 - c
            low, high = high, low
        return low + (high - low) * _sqrt(u * c)

    def normalvariate(self, mu=0.0, sigma=1.0):
        random = self.random
        while True:
            u1 = random()
            u2 = 1.0 - random()
            z = NV_MAGICCONST * (u1 - 0.5) / u2
            zz = z * z / 4.0
            if zz <= -_log(u2):
                break
        return mu + z * sigma

    def gauss(self, mu=0.0, sigma=1.0):
        random = self.random
        z = self.gauss_next
        self.gauss_next = None
        if z is None:
            x2pi = random() * TWOPI
            g2rad = _sqrt(-2.0 * _log(1.0 - random()))
            z = _cos(x2pi) * g2rad
            self.gauss_next = _sin(x2pi) * g2rad
        return mu + z * sigma

    def lognormvariate(self, mu, sigma):
        return _exp(self.normalvariate(mu, sigma))

    def expovariate(self, lambd=1.0):
        return -_log(1.0 - self.random()) / lambd

    def vonmisesvariate(self, mu, kappa):
        random = self.random
        if kappa <= 1e-6:
            return TWOPI * random()
        s = 0.5 / kappa
        r = s + _sqrt(1.0 + s * s)
        while True:
            u1 = random()
            z = _cos(_pi * u1)
            d = z / (r + z)
            u2 = random()
            if u2 < 1.0 - d * d or u2 <= (1.0 - d) * _exp(d):
                break
        q = 1.0 / r
        f = (q + z) / (1.0 + q * z)
        u3 = random()
        if u3 > 0.5:
            theta = (mu + _acos(f)) % TWOPI
        else:
            theta = (mu - _acos(f)) % TWOPI
        return theta

    def gammavariate(self, alpha, beta):
        if alpha <= 0.0 or beta <= 0.0:
            raise ValueError("gammavariate: alpha and beta must be > 0.0")
        random = self.random
        if alpha > 1.0:
            ainv = _sqrt(2.0 * alpha - 1.0)
            bbb = alpha - LOG4
            ccc = alpha + ainv
            while True:
                u1 = random()
                if not 1e-7 < u1 < 0.9999999:
                    continue
                u2 = 1.0 - random()
                v = _log(u1 / (1.0 - u1)) / ainv
                x = alpha * _exp(v)
                z = u1 * u1 * u2
                r = bbb + ccc * v - x
                if r + SG_MAGICCONST - 4.5 * z >= 0.0 or r >= _log(z):
                    return x * beta
        elif alpha == 1.0:
            return -_log(1.0 - random()) * beta
        else:
            while True:
                u = random()
                b = (_exp(1.0) + alpha) / _exp(1.0)
                p = b * u
                if p <= 1.0:
                    x = p ** (1.0 / alpha)
                else:
                    x = -_log((b - p) / alpha)
                u1 = random()
                if p > 1.0:
                    if u1 <= x ** (alpha - 1.0):
                        break
                elif u1 <= _exp(-x):
                    break
            return x * beta

    def betavariate(self, alpha, beta):
        y = self.gammavariate(alpha, 1.0)
        if y:
            return y / (y + self.gammavariate(beta, 1.0))
        return 0.0

    def paretovariate(self, alpha):
        u = 1.0 - self.random()
        return u ** (-1.0 / alpha)

    def weibullvariate(self, alpha, beta):
        u = 1.0 - self.random()
        return alpha * (-_log(u)) ** (1.0 / beta)

    def binomialvariate(self, n=1, p=0.5):
        if n < 0:
            raise ValueError("n must be non-negative")
        if p <= 0.0 or p >= 1.0:
            if p == 0.0:
                return 0
            if p == 1.0:
                return n
            raise ValueError("p must be in the range 0.0 <= p <= 1.0")
        random = self.random
        if n == 1:
            return int(random() < p)
        if p > 0.5:
            return n - self.binomialvariate(n, 1.0 - p)
        if n * p < 10.0:
            x = y = 0
            c = _log2(1.0 - p)
            if not c:
                return x
            while True:
                y += _floor(_log2(random()) / c) + 1
                if y > n:
                    return x
                x += 1
        # Large n * p: the BTRS transformed rejection, as the reference uses.
        spq = _sqrt(n * p * (1.0 - p))
        b = 1.15 + 2.53 * spq
        a = -0.0873 + 0.0248 * b + 0.01 * p
        c = n * p + 0.5
        vr = 0.92 - 4.2 / b
        setup_complete = False
        alpha = lpq = m = h = 0.0
        while True:
            u = random()
            u -= 0.5
            us = 0.5 - abs(u)
            k = _floor((2.0 * a / us + b) * u + c)
            if k < 0 or k > n:
                continue
            v = random()
            if us >= 0.07 and v <= vr:
                return k
            if not setup_complete:
                alpha = (2.83 + 5.1 / b) * spq
                lpq = _log(p / (1.0 - p))
                m = _floor((n + 1) * p)
                h = _lgamma(m + 1) + _lgamma(n - m + 1)
                setup_complete = True
            v *= alpha / (a / (us * us) + b)
            if _log(v) <= h - _lgamma(k + 1) - _lgamma(n - k + 1) + (k - m) * lpq:
                return k


def _log2(x):
    from math import log2
    return log2(x)


def _lgamma(x):
    from math import lgamma
    return lgamma(x)


class SystemRandom(Random):
    """Draws from the platform's entropy rather than a seeded sequence."""

    def seed(self, *args, **kwds):
        Random.seed(self, None)

    def getstate(self, *args, **kwds):
        raise NotImplementedError("System entropy source does not have state.")

    def setstate(self, *args, **kwds):
        raise NotImplementedError("System entropy source does not have state.")


_inst = Random()
seed = _inst.seed
random = _inst.random
uniform = _inst.uniform
triangular = _inst.triangular
randint = _inst.randint
choice = _inst.choice
randrange = _inst.randrange
sample = _inst.sample
shuffle = _inst.shuffle
choices = _inst.choices
normalvariate = _inst.normalvariate
lognormvariate = _inst.lognormvariate
expovariate = _inst.expovariate
vonmisesvariate = _inst.vonmisesvariate
gammavariate = _inst.gammavariate
gauss = _inst.gauss
betavariate = _inst.betavariate
binomialvariate = _inst.binomialvariate
paretovariate = _inst.paretovariate
weibullvariate = _inst.weibullvariate
getstate = _inst.getstate
setstate = _inst.setstate
getrandbits = _inst.getrandbits
randbytes = _inst.randbytes
