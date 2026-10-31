# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Heap queue algorithm: a list kept as a binary min-heap."""

__all__ = ["heappush", "heappop", "heapify", "heapreplace", "heappushpop", "nlargest", "nsmallest",
           "merge"]


def _up(heap, start, pos):
    item = heap[pos]
    while pos > start:
        parent = (pos - 1) >> 1
        p = heap[parent]
        if item < p:
            heap[pos] = p
            pos = parent
        else:
            break
    heap[pos] = item


def _down(heap, pos):
    end = len(heap)
    start = pos
    item = heap[pos]
    child = 2 * pos + 1
    while child < end:
        right = child + 1
        if right < end and not heap[child] < heap[right]:
            child = right
        heap[pos] = heap[child]
        pos = child
        child = 2 * pos + 1
    heap[pos] = item
    _up(heap, start, pos)


def heappush(heap, item):
    heap.append(item)
    _up(heap, 0, len(heap) - 1)


def heappop(heap):
    last = heap.pop()
    if heap:
        item = heap[0]
        heap[0] = last
        _down(heap, 0)
        return item
    return last


def heapreplace(heap, item):
    top = heap[0]
    heap[0] = item
    _down(heap, 0)
    return top


def heappushpop(heap, item):
    if heap and heap[0] < item:
        item, heap[0] = heap[0], item
        _down(heap, 0)
    return item


def heapify(x):
    for i in reversed(range(len(x) // 2)):
        _down(x, i)


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
