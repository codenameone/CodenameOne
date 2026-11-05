# Generators, yield from, send/throw/close, generator expressions.


def count(n):
    i = 0
    while i < n:
        yield i
        i += 1


print(list(count(5)))
print(sum(x * x for x in range(10)))


def nat():
    n = 0
    while True:
        yield n
        n += 1


g = nat()
print(next(g), next(g), next(g))
print([v for _, v in zip(range(4), nat())])


def echo():
    got = None
    while True:
        try:
            got = yield got
        except ValueError as e:
            got = "caught " + str(e)
        if got == "stop":
            return "done"


e = echo()
print(e.send(None))
print(e.send("a"))
print(e.throw(ValueError("boom")))
try:
    e.send("stop")
except StopIteration as s:
    print("stopped", s.value)


def cleanup():
    try:
        yield 1
        yield 2
    finally:
        print("cleanup ran")


c = cleanup()
print(next(c))
c.close()
print(list(cleanup()))


def inner():
    x = yield "i1"
    print("inner got", x)
    yield "i2"
    return "inner result"


def outer():
    r = yield from inner()
    print("outer got", r)
    yield from [10, 20]
    yield "end"


o = outer()
print(next(o))
print(o.send("hello"))
print(list(o))


def loops():
    for i in range(3):
        for j in range(3):
            if j == 2:
                break
            if i == 1:
                continue
            yield i, j
    else:
        yield "else"


print(list(loops()))


def fin_break():
    for i in range(5):
        try:
            if i == 2:
                break
            yield i
        finally:
            print("fin", i)
    yield "after"


print(list(fin_break()))


def fin_return():
    try:
        yield 1
        return 5
    finally:
        print("finally before return")


try:
    f = fin_return()
    next(f)
    next(f)
except StopIteration as s:
    print("returned", s.value)


class Ctx:
    def __init__(self, name):
        self.name = name

    def __enter__(self):
        print("enter", self.name)
        return self.name

    def __exit__(self, t, v, tb):
        print("exit", self.name, t.__name__ if t else None)
        return t is KeyError


def with_gen():
    with Ctx("a") as a, Ctx("b") as b:
        yield a + b
        raise KeyError("k")
    yield "suppressed"


print(list(with_gen()))


def nested_expr():
    x = 1 + (yield "first") * 2
    yield x
    y = [(yield "p"), (yield "q")]
    yield y
    z = (yield "c") or "default"
    yield z


n = nested_expr()
print(next(n))
print(n.send(5))
print(next(n))
print(n.send("P"))
print(n.send("Q"))
print(n.send(""))


def raiser():
    yield 1
    raise RuntimeError("inside")


try:
    for v in raiser():
        print("got", v)
except RuntimeError as err:
    print("error", err)


def stop_iter():
    yield 1
    raise StopIteration


try:
    list(stop_iter())
except RuntimeError as err:
    print("pep479", err)


def fib():
    a, b = 0, 1
    while True:
        yield a
        a, b = b, a + b


gen = fib()
print([next(gen) for _ in range(10)])
squares = (i * i for i in range(5))
print(next(squares), list(squares))
print(any(x > 3 for x in [1, 2, 5]), all(x > 0 for x in [1, -1]))
print(dict((k, k * 2) for k in "ab"))


def closure_gen(k):
    def scale(v):
        return v * k
    for i in range(3):
        yield scale(i)


print(list(closure_gen(10)))
print(type(count(1)).__name__)
evens = (n for n in nat() if n % 2 == 0)
print(next(evens), next(evens), next(evens))
side = []
lazy = (side.append(i) or i for i in range(3))
print(side)
print(list(lazy), side)
print(list(x + y for x in range(3) if x for y in "ab" if y != "a" for _ in [0]) if False else
      list((x, y) for x in range(3) if x for y in "ab" if y != "a"))
