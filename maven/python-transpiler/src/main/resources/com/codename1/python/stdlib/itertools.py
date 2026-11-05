# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Iterator building blocks."""


def count(start=0, step=1):
    n = start
    while True:
        yield n
        n += step


def cycle(iterable):
    saved = []
    for x in iterable:
        yield x
        saved.append(x)
    while saved:
        for x in saved:
            yield x


def repeat(obj, times=None):
    if times is None:
        while True:
            yield obj
    else:
        for _ in range(times):
            yield obj


def accumulate(iterable, func=None, *, initial=None):
    it = iter(iterable)
    total = initial
    if initial is None:
        try:
            total = next(it)
        except StopIteration:
            return
    yield total
    for x in it:
        total = total + x if func is None else func(total, x)
        yield total


def batched(iterable, n, *, strict=False):
    if n < 1:
        raise ValueError("n must be at least one")
    it = iter(iterable)
    while True:
        batch = tuple(islice(it, n))
        if not batch:
            return
        if strict and len(batch) != n:
            raise ValueError("batched(): incomplete batch")
        yield batch


def chain(*iterables):
    for it in iterables:
        for x in it:
            yield x


def _from_iterable(iterables):
    for it in iterables:
        for x in it:
            yield x


chain.from_iterable = _from_iterable


def compress(data, selectors):
    for d, s in zip(data, selectors):
        if s:
            yield d


def dropwhile(predicate, iterable):
    it = iter(iterable)
    for x in it:
        if not predicate(x):
            yield x
            break
    for x in it:
        yield x


def filterfalse(predicate, iterable):
    if predicate is None:
        predicate = bool
    for x in iterable:
        if not predicate(x):
            yield x


_NO_KEY = object()


class groupby:
    def __init__(self, iterable, key=None):
        self._key = key
        self._it = iter(iterable)
        self._have = False
        self._cur_key = None
        self._cur_val = None
        self._tgt = _NO_KEY

    def __iter__(self):
        return self

    def __next__(self):
        while True:
            if not self._have:
                self._fetch()
            if self._tgt is _NO_KEY or self._cur_key != self._tgt:
                break
            self._have = False
        self._tgt = self._cur_key
        return (self._cur_key, self._grouper(self._tgt))

    def _fetch(self):
        self._cur_val = next(self._it)
        self._cur_key = self._cur_val if self._key is None else self._key(self._cur_val)
        self._have = True

    def _grouper(self, tgt):
        while True:
            if not self._have:
                try:
                    self._fetch()
                except StopIteration:
                    return
            if self._tgt is not tgt or self._cur_key != tgt:
                return
            self._have = False
            yield self._cur_val


def islice(iterable, *args):
    s = slice(*args)
    start = 0 if s.start is None else s.start
    stop = s.stop
    step = 1 if s.step is None else s.step
    if start < 0 or (stop is not None and stop < 0) or step <= 0:
        raise ValueError("Indices for islice() must be None or an integer: 0 <= x <= sys.maxsize.")
    it = iter(iterable)
    i = 0
    nxt = start
    while stop is None or i < stop:
        try:
            x = next(it)
        except StopIteration:
            return
        if i == nxt:
            yield x
            nxt += step
        i += 1


def pairwise(iterable):
    it = iter(iterable)
    try:
        a = next(it)
    except StopIteration:
        return
    for b in it:
        yield (a, b)
        a = b


def starmap(function, iterable):
    for args in iterable:
        yield function(*args)


def takewhile(predicate, iterable):
    for x in iterable:
        if not predicate(x):
            return
        yield x


def tee(iterable, n=2):
    it = iter(iterable)
    buffers = [[] for _ in range(n)]

    def gen(mine):
        while True:
            if not mine:
                try:
                    v = next(it)
                except StopIteration:
                    return
                for b in buffers:
                    b.append(v)
            yield mine.pop(0)
    return tuple(gen(b) for b in buffers)


def zip_longest(*iterables, fillvalue=None):
    its = [iter(i) for i in iterables]
    active = len(its)
    if not active:
        return
    while True:
        row = []
        for i in range(len(its)):
            try:
                row.append(next(its[i]))
            except StopIteration:
                active -= 1
                if not active:
                    return
                its[i] = repeat(fillvalue)
                row.append(fillvalue)
        yield tuple(row)


def product(*iterables, repeat=1):
    pools = [tuple(p) for p in iterables] * repeat
    result = [[]]
    for pool in pools:
        result = [x + [y] for x in result for y in pool]
    for prod in result:
        yield tuple(prod)


def permutations(iterable, r=None):
    pool = tuple(iterable)
    n = len(pool)
    r = n if r is None else r
    if r > n:
        return
    indices = list(range(n))
    cycles = list(range(n, n - r, -1))
    yield tuple(pool[i] for i in indices[:r])
    while n:
        for i in reversed(range(r)):
            cycles[i] -= 1
            if cycles[i] == 0:
                indices[i:] = indices[i + 1:] + indices[i:i + 1]
                cycles[i] = n - i
            else:
                j = cycles[i]
                indices[i], indices[-j] = indices[-j], indices[i]
                yield tuple(pool[i] for i in indices[:r])
                break
        else:
            return


def combinations(iterable, r):
    pool = tuple(iterable)
    n = len(pool)
    if r > n:
        return
    indices = list(range(r))
    yield tuple(pool[i] for i in indices)
    while True:
        for i in reversed(range(r)):
            if indices[i] != i + n - r:
                break
        else:
            return
        indices[i] += 1
        for j in range(i + 1, r):
            indices[j] = indices[j - 1] + 1
        yield tuple(pool[i] for i in indices)


def combinations_with_replacement(iterable, r):
    pool = tuple(iterable)
    n = len(pool)
    if not n and r:
        return
    indices = [0] * r
    yield tuple(pool[i] for i in indices)
    while True:
        for i in reversed(range(r)):
            if indices[i] != n - 1:
                break
        else:
            return
        indices[i:] = [indices[i] + 1] * (r - i)
        yield tuple(pool[i] for i in indices)
