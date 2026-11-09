# Functions as values: lambdas, closures over locals, sorted/map/filter with
# a key, and bound methods passed around -- what callbacks and visitor
# dispatch tables in the converters are made of.
from _harness import measure


def make_adder(k):
    def add(x):
        return x + k
    return add


def work(n):
    adders = [make_adder(k) for k in range(8)]
    total = 0
    for i in range(n):
        total += adders[i & 7](i)
        total += (lambda x, y: x * y)(i & 15, 3)
    items = [(i * 7919) % 1000 for i in range(n // 10)]
    total += sum(sorted(items, key=lambda v: -v)[:10])
    total += sum(map(lambda v: v & 3, filter(lambda v: v % 3 == 0, items)))
    handlers = {"a": str.upper, "b": str.lower, "c": str.strip}
    for i in range(n // 10):
        total += len(handlers["abc"[i % 3]](" Xy "))
    return str(total)


measure("closures", work, 400000, 5)
