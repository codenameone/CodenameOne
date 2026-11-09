# Generator pipelines: the way bs4 walks a tree (descendants filtered by a
# predicate) and pdfminer streams tokens.
from _harness import measure


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


def work(n):
    s = sum(x * 3 for x in evens(numbers(n)))
    t = tree(7, 4)
    c = 0
    for _ in range(4):
        c += sum(1 for d in walk(t) if d > 2)
    return "%d %d" % (s, c)


measure("generators", work, 600000, 5)
