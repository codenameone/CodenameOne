import itertools as it
import functools
import operator
import bisect
from functools import reduce, partial, lru_cache, wraps, total_ordering, cached_property, singledispatch, cmp_to_key

print(list(it.islice(it.count(5, 2), 4)))
print(list(it.islice(it.cycle("ab"), 5)))
print(list(it.repeat("x", 3)))
print(list(it.accumulate([1, 2, 3, 4])), list(it.accumulate([1, 2, 3], operator.mul, initial=10)))
print(list(it.batched(range(7), 3)))
print(list(it.chain("ab", [1, 2])), list(it.chain.from_iterable([[1], [2, 3]])))
print(list(it.compress("abcdef", [1, 0, 1, 0, 1, 1])))
print(list(it.dropwhile(lambda x: x < 3, [1, 2, 3, 1])), list(it.takewhile(lambda x: x < 3, [1, 2, 3, 1])))
print(list(it.filterfalse(None, [0, 1, "", "a"])))
print([(k, list(g)) for k, g in it.groupby("aaabccaa")])
print([(k, len(list(g))) for k, g in it.groupby(sorted([3, 1, 4, 1, 5, 9, 2, 6]), key=lambda v: v % 2)])
print(list(it.islice(range(20), 2, 15, 3)), list(it.pairwise("abcd")))
print(list(it.starmap(pow, [(2, 3), (3, 2)])))
a, b = it.tee(iter([1, 2, 3]))
print(list(a), list(b))
print(list(it.zip_longest("ab", "xyz", fillvalue="-")))
print(list(it.product("ab", range(2))), len(list(it.product(range(3), repeat=3))))
print(list(it.permutations("abc", 2)))
print(list(it.combinations(range(4), 2)), list(it.combinations_with_replacement("ab", 2)))

print(reduce(operator.add, [1, 2, 3, 4]), reduce(lambda x, y: x * y, [], 7))
try:
    reduce(operator.add, [])
except TypeError as e:
    print("TypeError", e)
p = partial(int, base=2)
print(p("101"), partial(partial(max, 3), 9)(5))


@lru_cache(maxsize=None)
def fib(n):
    return n if n < 2 else fib(n - 1) + fib(n - 2)


print(fib(60), fib.cache_info())


@lru_cache(maxsize=2)
def sq(x):
    return x * x


for v in (1, 2, 1, 3, 2):
    sq(v)
print(sq.cache_info())


def deco(f):
    @wraps(f)
    def inner(*a):
        return f(*a) + 1
    return inner


@deco
def answer():
    """Doc."""
    return 41


print(answer(), answer.__name__, answer.__doc__, answer.__wrapped__())


@total_ordering
class V:
    def __init__(self, v):
        self.v = v

    def __eq__(self, o):
        return self.v == o.v

    def __lt__(self, o):
        return self.v < o.v


print(V(1) <= V(2), V(3) > V(2), V(2) >= V(2), V(1) > V(2))


class Circle:
    def __init__(self, r):
        self.r = r
        self.calls = 0

    @cached_property
    def area(self):
        self.calls += 1
        return 3 * self.r * self.r


c = Circle(2)
print(c.area, c.area, c.calls)


@singledispatch
def show(x):
    return "obj"


@show.register
def _(x: int):
    return "int " + str(x)


@show.register(list)
def _(x):
    return "list " + str(len(x))


print(show(1), show([1, 2]), show("s"), show(True))
print(sorted([3, 1, 2], key=cmp_to_key(lambda a, b: b - a)))

print(operator.itemgetter(1)([5, 6]), operator.itemgetter(0, 2)("abc"), operator.attrgetter("r")(c))
print(operator.methodcaller("upper")("hi"), operator.add(2, 3), operator.neg(4), operator.contains([1], 1))
print(sorted([(1, "b"), (0, "a")], key=operator.itemgetter(1)))

xs = [1, 2, 4, 4, 8]
print(bisect.bisect_left(xs, 4), bisect.bisect_right(xs, 4), bisect.bisect(xs, 5))
bisect.insort(xs, 3)
print(xs, bisect.bisect_left(xs, 7, key=lambda v: v))
