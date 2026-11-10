# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Bisection algorithms over sorted lists."""


def bisect_left(a, x, lo=0, hi=None, *, key=None):
    if lo < 0:
        raise ValueError("lo must be non-negative")
    if hi is None:
        hi = len(a)
    while lo < hi:
        mid = (lo + hi) // 2
        v = a[mid] if key is None else key(a[mid])
        if v < x:
            lo = mid + 1
        else:
            hi = mid
    return lo


def bisect_right(a, x, lo=0, hi=None, *, key=None):
    if lo < 0:
        raise ValueError("lo must be non-negative")
    if hi is None:
        hi = len(a)
    while lo < hi:
        mid = (lo + hi) // 2
        v = a[mid] if key is None else key(a[mid])
        if x < v:
            hi = mid
        else:
            lo = mid + 1
    return lo


def insort_left(a, x, lo=0, hi=None, *, key=None):
    a.insert(bisect_left(a, x if key is None else key(x), lo, hi, key=key), x)


def insort_right(a, x, lo=0, hi=None, *, key=None):
    a.insert(bisect_right(a, x if key is None else key(x), lo, hi, key=key), x)


bisect = bisect_right
insort = insort_right
