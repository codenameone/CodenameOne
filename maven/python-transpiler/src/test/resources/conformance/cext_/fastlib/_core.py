# What CPython runs for the expected output; the transpiled program
# runs Core.java in its place, as it would for a compiled module.
VERSION = "1.0"


def fast_sum(xs):
    if not isinstance(xs, list):
        raise TypeError("fast_sum() needs a list")
    return sum(xs)


def scale(xs, k):
    return tuple(x * k for x in xs)


def join_words(ws):
    return "-".join(ws)


def describe(d):
    return sorted(d.items())
