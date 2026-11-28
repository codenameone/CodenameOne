# Scratch: the closures kernel split into its parts, each timed alone.
import time


def make_adder(k):
    def add(x):
        return x + k
    return add


ADDERS = [make_adder(k) for k in range(8)]
ITEMS = [(i * 7919) % 1000 for i in range(40000)]
HANDLERS = {"a": str.upper, "b": str.lower, "c": str.strip}


def p_adders(n):
    t = 0
    for i in range(n):
        t += ADDERS[i & 7](i)
    return t


def p_lambda_call(n):
    t = 0
    for i in range(n):
        t += (lambda x, y: x * y)(i & 15, 3)
    return t


def p_make_adders(n):
    t = 0
    for i in range(n // 100):
        a = [make_adder(k) for k in range(8)]
        t += len(a)
    return t


def p_sorted_key(n):
    return sum(sorted(ITEMS, key=lambda v: -v)[:10])


def p_map_filter(n):
    return sum(map(lambda v: v & 3, filter(lambda v: v % 3 == 0, ITEMS)))


def p_unbound_str(n):
    t = 0
    for i in range(n // 10):
        t += len(HANDLERS["abc"[i % 3]](" Xy "))
    return t


PARTS = [("adders", p_adders), ("lambda_call", p_lambda_call), ("make_adders", p_make_adders),
         ("sorted_key", p_sorted_key), ("map_filter", p_map_filter), ("unbound_str", p_unbound_str)]

N = 400000
for name, f in PARTS:
    best = None
    for _ in range(7):
        t0 = time.perf_counter()
        f(N)
        dt = time.perf_counter() - t0
        if best is None or dt < best:
            best = dt
    print("PART", name, int(best * 1000000))
