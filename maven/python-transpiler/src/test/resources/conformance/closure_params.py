# Closures over a typed function's parameters: the cell sits beside the
# typed local; anything that rebinds a captured name keeps generic code.

SCALE = 3


def make_adder(k):
    def add(x):
        return x + k
    return add


def make_scaled(k, s):
    def f(x):
        return (x + k) * s * SCALE
    def g():
        return [k, s]
    return f, g


def make_counter(start):
    def inc():
        nonlocal start
        start += 1
        return start
    return inc


def make_rec(n):
    def fact(m):
        return 1 if m <= 1 else m * fact(m - 1)
    return fact(n)


def make_late(k):
    def get():
        return k
    k = k * 2
    return get


def make_gen(k):
    def gen(n):
        for i in range(n):
            yield i * k
    return gen


adders = [make_adder(k) for k in range(4)]
print([a(10) for a in adders], make_adder("ab")("cd"), make_adder(2 ** 70)(1))
f, g = make_scaled(2, 5)
print(f(1), g(), make_scaled(1.5, 2)[0](0.5))
c = make_counter(5)
print(c(), c(), c())
print(make_rec(6), make_late(4)(), list(make_gen(3)(4)))
print(make_adder.__name__, adders[0].__qualname__)

def bound_param(k):
    def get():
        return k
    return get

def rebound_param(k):
    def get():
        return k
    del k
    return get

def nonlocal_param(k):
    def setk():
        nonlocal k
        del k
    def get():
        return k
    return setk, get

print(bound_param(5)())
try:
    rebound_param(1)()
except NameError as e:
    print("NameError", "k" in str(e))
s, g = nonlocal_param(2)
print(g())
s()
try:
    g()
except NameError as e:
    print("NameError", "k" in str(e))
