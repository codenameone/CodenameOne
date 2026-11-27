import time


def p_inline(n):
    t = 0
    for i in range(n):
        t += (i & 15) * 3
    return t


def p_lambda(n):
    t = 0
    for i in range(n):
        t += (lambda x, y: x * y)(i & 15, 3)
    return t


def p_lambda1(n):
    t = 0
    for i in range(n):
        t += (lambda x: x)(i)
    return t


def p_and(n):
    t = 0
    for i in range(n):
        t += i & 15
    return t


PARTS = [("inline", p_inline), ("lambda", p_lambda), ("lambda1", p_lambda1), ("and", p_and)]
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
