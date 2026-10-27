# Ints held as a Java long with an overflow object: every place one
# crosses (parameter, return, field, list, method) at the edges of 64 bits.
MIN = -(2 ** 63)
MAX = 2 ** 63 - 1


def ident(x: int) -> int:
    return x


def twice(x: int) -> int:
    return x + x


def fact(n: int) -> int:
    if n < 2:
        return 1
    return n * fact(n - 1)


def fib(n: int) -> int:
    if n < 2:
        return n
    return fib(n - 1) + fib(n - 2)


def neg(x: int) -> int:
    return -x


def pick(c: bool, a: int, b: int) -> int:
    return a if c else b


class Acc:
    __slots__ = ("total", "count")

    def __init__(self, start: int):
        self.total = start
        self.count = 0

    def add(self, v: int) -> int:
        self.total += v
        self.count += 1
        return self.total

    def get(self) -> int:
        return self.total


class Shape:
    def area(self, k: int) -> int:
        return k


class Sq(Shape):
    def area(self, k: int) -> int:
        return k * k


def main() -> None:
    print(ident(MIN), ident(MAX), ident(MIN - 1), ident(MAX + 1))
    print(twice(MAX), twice(MIN), twice(3))
    print(fact(25), fact(20), fact(21))
    print(fib(30))
    print(neg(MIN), neg(MAX), neg(0))
    print(pick(True, MIN, 1), pick(False, 1, MAX + 5))
    print(ident(True), twice(True), ident(False) + 1)
    a = Acc(MAX - 2)
    for i in range(5):
        a.add(1)
    print(a.get(), a.count, a.total - MAX)
    a.total = True
    print(a.total, a.get() + 1)
    b = Acc(MIN)
    b.add(-1)
    print(b.total, b.add(1), b.add(MIN))
    xs = [MIN, MAX, 0, MAX + 1]
    s: int = 0
    for x in xs:
        s += x
    print(s)
    shapes = [Shape(), Sq(), Sq()]
    t: int = 0
    for sh in shapes:
        t += sh.area(2 ** 40)
    print(t)
    k: int = 1
    for i in range(70):
        k = k * 2
    print(k, k >> 10, k - 1)
    m: int = MAX
    m += 1
    m -= 1
    print(m, m == MAX)
    c: int = 0
    while c < 3:
        c = c + True
    print(c)


main()
