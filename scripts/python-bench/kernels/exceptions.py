# Exceptions as control flow: KeyError/AttributeError misses, getattr with a
# default and try/except/finally around small operations, as dict lookups
# with fallbacks and duck typing do throughout the converters.
from _harness import measure


class Thing:
    def __init__(self, i):
        self.a = i


def work(n):
    d = {}
    hits = 0
    for i in range(n):
        key = i % 50
        try:
            hits += d[key]
        except KeyError:
            d[key] = 1
        t = Thing(i)
        hits += getattr(t, "b", 2)
        try:
            hits += t.c
        except AttributeError:
            hits += 3
        try:
            if i % 100 == 0:
                raise ValueError(i)
        except ValueError as e:
            hits += e.args[0] % 7
        finally:
            hits += 1
    return str(hits)


measure("exceptions", work, 100000, 5)
