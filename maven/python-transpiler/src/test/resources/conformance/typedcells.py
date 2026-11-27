# Typed bodies over cells that closures share and rebind.


def counter(start: int):
    n = start

    def bump(by=1):
        nonlocal n
        n += by
        return n

    bump()
    bump(5)
    return n, bump


def late_binding(k: int):
    fns = []
    for i in range(k):
        fns.append(lambda: i * 10)
    i = 99
    return [f() for f in fns]


def gen_sees_rebind(items):
    total = 0
    g = (x + total for x in items)
    total = 100
    return list(g)


def nonlocal_type_change():
    v = 1

    def flip():
        nonlocal v
        v = "now a string"

    flip()
    return v


def unbound_then_bound(flag: bool):
    def read():
        return y

    try:
        read()
    except NameError as e:
        msg = type(e).__name__
    y = 5
    if flag:
        del y
        try:
            read()
        except NameError:
            msg += "+again"
    else:
        msg += str(read())
    return msg


def recursive(n: int):
    def fact(k):
        return 1 if k <= 1 else k * fact(k - 1)

    return fact(n)


def param_cell(x: int, scale):
    def mul():
        return x * scale

    x += 1
    return mul()


print(counter(3)[0], counter(0)[1]())
print(late_binding(3))
print(gen_sees_rebind([1, 2]))
print(nonlocal_type_change())
print(unbound_then_bound(True), unbound_then_bound(False))
print(recursive(6), param_cell(2, 3), param_cell(2, "ab"))


def runs(data):
    out = []
    it = iter(data)
    while True:
        length = next(it, 128)
        if length == 128:
            break
        # The first iterable is read now; only `it` is read later.
        out.extend(next(it) for _ in range(length + 1))
    return out


def kept_late(xs: list):
    gens = []
    for k in range(3):
        gens.append(x * k for x in xs)
    return [list(g) for g in gens]


print(runs([1, 5, 6, 0, 9, 128]), kept_late([1, 2]))


def rows(grid: list, values_only: bool):
    for r in range(len(grid)):
        cells = (grid[r][c] for c in range(len(grid[r])))
        if values_only:
            yield tuple(v * 10 for v in cells)
        else:
            yield tuple(cells)


def gen_counter(k: int):
    n = 0

    def bump():
        nonlocal n
        n += 1
        return n

    for _ in range(k):
        yield bump()
    yield n


print(list(rows([[1, 2], [3]], True)), list(rows([[1, 2], [3]], False)), list(gen_counter(3)))
