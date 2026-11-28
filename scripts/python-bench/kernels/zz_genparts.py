# Scratch: the generators kernel split into its parts, each timed alone.
import time


def numbers(n):
    i = 0
    while i < n:
        yield i
        i += 1


def evens(it):
    for x in it:
        if x % 2 == 0:
            yield x


def walk(node):
    yield node[0]
    for child in node[1]:
        yield from walk(child)


def tree(depth, fan):
    if depth == 0:
        return (depth, [])
    return (depth, [tree(depth - 1, fan) for _ in range(fan)])


def p_while(n):
    i = 0
    s = 0
    while i < n:
        s += i
        i += 1
    return s


def p_mod(n):
    i = 0
    s = 0
    while i < n:
        if i % 2 == 0:
            s += i
        i += 1
    return s


def p_drain(n):
    s = 0
    for x in numbers(n):
        s += x
    return s


def p_filter(n):
    s = 0
    for x in evens(numbers(n)):
        s += x
    return s


def p_sum_genexpr(n):
    return sum(x * 3 for x in range(n))


def p_sum_range(n):
    return sum(range(n))


def p_create(n):
    c = 0
    for _ in range(n // 10):
        for x in numbers(1):
            c += x
    return c


def p_tree(n):
    return len(tree(7, 4)[1])


T = tree(7, 4)


def p_walk(n):
    c = 0
    for d in walk(T):
        c += 1
    return c


def p_walk_filter_sum(n):
    return sum(1 for d in walk(T) if d > 2)


def p_full(n):
    s = sum(x * 3 for x in evens(numbers(n)))
    c = 0
    for _ in range(4):
        c += sum(1 for d in walk(T) if d > 2)
    return s + c


PARTS = [("while", p_while), ("while_mod", p_mod), ("drain_gen", p_drain),
         ("filter_gen", p_filter), ("sum_genexpr", p_sum_genexpr),
         ("sum_range", p_sum_range), ("create_gen", p_create), ("tree", p_tree),
         ("walk_yieldfrom", p_walk), ("walk_filter_sum", p_walk_filter_sum),
         ("full", p_full)]

N = 600000
for name, f in PARTS:
    best = None
    for _ in range(7):
        t0 = time.perf_counter()
        f(N)
        dt = time.perf_counter() - t0
        if best is None or dt < best:
            best = dt
    print("PART", name, int(best * 1000000))
