# Lambdas in typed functions: closures over locals that never change once
# bound capture their values; a loop variable keeps the function generic.


def keyed(rows: list, col: int):
    width = col + 1
    by = sorted(rows, key=lambda r: (len(r) - width, r))
    f = lambda s: s[:width] + "/" + str(col)
    return by, [f(r) for r in rows], list(map(lambda r: r * 2, rows))


def defaults(n: int):
    x = n * 10
    g = lambda y, x=x: x + y
    return g(1), g(1, 2)


def nested_lam(xs: list, k: int):
    m = k + 1
    h = lambda a: [b * m for b in a]
    return h(xs), (lambda: m)()


def late_lambdas(n: int):
    fs = []
    for i in range(n):
        fs.append(lambda: i)
    return [f() for f in fs]


print(keyed(["bb", "a", "ccc"], 1))
print(defaults(3))
print(nested_lam([1, 2], 2))
print(late_lambdas(3))
