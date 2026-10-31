# Parallel assignment stores left to right after evaluating every value,
# and the one-argument builtin constructors keep their subclass behaviour.
log = []


def v(x):
    log.append(x)
    return x


a, b = 0, 1
for _ in range(10):
    a, b = b, a + b
print(a, b)

items = [1, 2, 3]
i = 0
i, items[i] = 2, 9
print(i, items)

p, q = v("p"), v("q")
print(p, q, log)

x = [10, 20]
x[0], x[1] = x[1], x[0]
print(x)


class Box:
    def __init__(self):
        self.a = 1
        self.b = 2


bx = Box()
bx.a, bx.b = bx.b, bx.a
print(bx.a, bx.b)

[m, n] = 3, 4
print(m, n)


class Shout(str):
    def __new__(cls, s):
        return super().__new__(cls, s.upper())


class Big(int):
    pass


print(str(12), int("7"), int(3.9), float("1.5"), bool([]), bool([0]))
print(Shout("hi"), type(Shout("hi")).__name__, Big(5) + 1, type(Big(5)).__name__)


class Recv:
    def four(self, a, b, c, d):
        return (a, b, c, d)

    def five(self, a, b, c, d, e):
        return a + b + c + d + e


r = Recv()
print(r.four(1, 2, 3, 4), r.five(1, 2, 3, 4, 5))
f = r.five
r.five = lambda a, b, c, d, e: "inst"
print(r.five(1, 2, 3, 4, 5), f(1, 1, 1, 1, 1))
