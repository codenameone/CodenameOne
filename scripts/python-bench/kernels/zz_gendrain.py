# Scratch: one generator drained, repeated long enough to profile.
import sys


def numbers(n):
    i = 0
    while i < n:
        yield i
        i += 1


def p_drain(n):
    s = 0
    for x in numbers(n):
        s += x
    return s


def p_sum_range(n):
    return sum(range(n))


def walk(node):
    yield node[0]
    for child in node[1]:
        yield from walk(child)


def tree(depth, fan):
    if depth == 0:
        return (depth, [])
    return (depth, [tree(depth - 1, fan) for _ in range(fan)])


T = tree(7, 4)


def p_walk(n):
    c = 0
    for _ in range(25):
        for d in walk(T):
            c += 1
    return c


which = sys.argv[1] if len(sys.argv) > 1 else "drain"
t = 0
for _ in range(200):
    t += p_drain(600000) if which == "drain" else p_walk(0)
print(t)
