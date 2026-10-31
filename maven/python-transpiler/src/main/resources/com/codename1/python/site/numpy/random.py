# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""numpy.random over the stdlib's Mersenne Twister. The streams are not
numpy's: seeding gives a reproducible sequence, not the one numpy draws."""

import math as _math
import random as _random
import numpy as _np

_rng = _random.Random()


def seed(s=None):
    _rng.seed(s)


def _shape(size):
    if size is None:
        return None
    return (size,) if isinstance(size, int) else tuple(size)


def _fill(size, draw, dtype=float):
    shape = _shape(size)
    if shape is None:
        return draw()
    n = 1
    for s in shape:
        n *= s
    return _np.array([draw() for _ in range(n)], dtype=dtype).reshape(shape)


def rand(*shape):
    return _fill(shape or None, _rng.random)


def random(size=None):
    return _fill(size, _rng.random)


random_sample = random


def randn(*shape):
    return _fill(shape or None, lambda: _rng.gauss(0.0, 1.0))


def standard_normal(size=None):
    return _fill(size, lambda: _rng.gauss(0.0, 1.0))


def normal(loc=0.0, scale=1.0, size=None):
    return _fill(size, lambda: _rng.gauss(loc, scale))


def uniform(low=0.0, high=1.0, size=None):
    return _fill(size, lambda: low + (high - low) * _rng.random())


def randint(low, high=None, size=None):
    if high is None:
        low, high = 0, low
    return _fill(size, lambda: _rng.randrange(low, high), int)


def choice(a, size=None, replace=True, p=None):
    pool = list(range(a)) if isinstance(a, int) else _np.asarray(a).tolist()
    if p is not None:
        weights = _np.asarray(p).tolist()
        return _fill(size, lambda: _rng.choices(pool, weights)[0], None)
    if not replace:
        n = 1 if size is None else _np.prod(_np.array(_shape(size)))
        picked = _rng.sample(pool, int(n))
        return picked[0] if size is None else _np.array(picked).reshape(_shape(size))
    return _fill(size, lambda: _rng.choice(pool), None)


def shuffle(x):
    vals = x.tolist() if isinstance(x, _np.ndarray) else x
    _rng.shuffle(vals)
    if isinstance(x, _np.ndarray):
        x[...] = _np.array(vals)
    return None


def permutation(x):
    vals = list(range(x)) if isinstance(x, int) else _np.asarray(x).tolist()
    _rng.shuffle(vals)
    return _np.array(vals)


def exponential(scale=1.0, size=None):
    return _fill(size, lambda: _rng.expovariate(1.0 / scale))


def poisson(lam=1.0, size=None):
    def draw():
        limit, k, p = _math.exp(-lam), 0, 1.0
        while True:
            p *= _rng.random()
            if p <= limit:
                return k
            k += 1
    return _fill(size, draw, int)


class Generator:
    def __init__(self, s=None):
        self._r = _random.Random(s)

    def random(self, size=None):
        return _fill(size, self._r.random)

    def integers(self, low, high=None, size=None, endpoint=False):
        if high is None:
            low, high = 0, low
        if endpoint:
            high += 1
        return _fill(size, lambda: self._r.randrange(low, high), int)

    def normal(self, loc=0.0, scale=1.0, size=None):
        return _fill(size, lambda: self._r.gauss(loc, scale))

    def standard_normal(self, size=None):
        return _fill(size, lambda: self._r.gauss(0.0, 1.0))

    def uniform(self, low=0.0, high=1.0, size=None):
        return _fill(size, lambda: low + (high - low) * self._r.random())

    def choice(self, a, size=None, replace=True):
        pool = list(range(a)) if isinstance(a, int) else _np.asarray(a).tolist()
        return _fill(size, lambda: self._r.choice(pool), None)

    def shuffle(self, x):
        vals = x.tolist()
        self._r.shuffle(vals)
        x[...] = _np.array(vals)

    def permutation(self, x):
        vals = list(range(x)) if isinstance(x, int) else _np.asarray(x).tolist()
        self._r.shuffle(vals)
        return _np.array(vals)


def default_rng(s=None):
    return Generator(s)
