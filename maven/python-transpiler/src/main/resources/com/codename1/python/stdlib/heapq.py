# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Heap queue algorithm: a list kept as a binary min-heap."""

__all__ = ["heappush", "heappop", "heapify", "heapreplace", "heappushpop", "nlargest", "nsmallest",
           "merge", "heappush_max", "heappop_max", "heapify_max",
           "heapreplace_max", "heappushpop_max"]


from _cn1heapq import (heappush, heappop, heapify, heapreplace, heappushpop, heappush_max, heappop_max,
                       heapify_max, heapreplace_max, heappushpop_max)


def nsmallest(n, iterable, key=None):
    if key is None:
        return sorted(iterable)[:n]
    return sorted(iterable, key=key)[:n]


def nlargest(n, iterable, key=None):
    if key is None:
        return sorted(iterable, reverse=True)[:n]
    return sorted(iterable, key=key, reverse=True)[:n]


def merge(*iterables, key=None, reverse=False):
    """Lazily merges sorted inputs; equal items come in input order."""
    its = [iter(i) for i in iterables]
    heads = []
    for idx in range(len(its)):
        for v in its[idx]:
            heads.append([v if key is None else key(v), idx, v])
            break
    while heads:
        best = 0
        for i in range(1, len(heads)):
            if reverse:
                if heads[best][0] < heads[i][0]:
                    best = i
            elif heads[i][0] < heads[best][0]:
                best = i
        idx = heads[best][1]
        yield heads[best][2]
        for v in its[idx]:
            heads[best] = [v if key is None else key(v), idx, v]
            break
        else:
            del heads[best]
