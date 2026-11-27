# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Standard operators as functions."""

from _cn1op import (lt, le, eq, ne, ge, gt, not_, truth, is_, is_not, is_none, is_not_none, add,
                   sub, mul, truediv, floordiv, mod, pow, lshift, rshift, and_, or_, xor, neg, pos, inv,
                   invert, contains, getitem, setitem, itemgetter, attrgetter, methodcaller)


from builtins import abs
def index(a): return a.__index__()
def matmul(a, b): return a @ b
def concat(a, b): return a + b
def countOf(a, b): return sum(1 for x in a if x is b or x == b)
def delitem(a, b): del a[b]


def indexOf(a, b):
    for i, x in enumerate(a):
        if x is b or x == b:
            return i
    raise ValueError("sequence.index(x): x not in sequence")


def length_hint(obj, default=0):
    try:
        return len(obj)
    except TypeError:
        return default


def iadd(a, b):
    a += b
    return a


def isub(a, b):
    a -= b
    return a


def imul(a, b):
    a *= b
    return a


def iconcat(a, b):
    a += b
    return a


def call(obj, *args, **kwargs):
    return obj(*args, **kwargs)
