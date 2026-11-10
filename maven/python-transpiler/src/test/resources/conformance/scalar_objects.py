class Vec:
    __slots__ = ("x", "y")

    def __init__(self, x: int, y: int):
        self.x = x
        self.y = y

    def get_x(self) -> int:
        return self.x


class P:
    def __init__(self, a, b):
        self.a, self.b = b, a


def trace(v):
    print("arg", v)
    return v


def sums(n: int) -> int:
    total: int = 0
    for i in range(n):
        v = Vec(i, i * 2)
        total = (total + v.get_x() + v.y) & 0x3FFFFFFF
    return total


def big() -> int:
    v = Vec(2 ** 70, -1)
    return v.x + v.y


def order() -> float:
    p = P(trace(1.5), trace(2))
    return p.a * p.b


def escapes() -> int:
    v = Vec(3, 4)
    keep = [v]
    return keep[0].x + v.y


def rebound(f: bool) -> int:
    v = Vec(1, 2)
    if f:
        v = Vec(5, 6)
    return v.x * 10 + v.y


print(sums(1000))
print(big())
print(order())
print(escapes())
print(rebound(True), rebound(False))


def fill(n: int) -> int:
    a = []
    a.extend([7] * n)
    a.extend(2 * [1])
    a.extend(a)
    b = [1.5]
    b.extend([2] * 2)
    return len(a) + sum(a) + int(sum(b))


print(fill(5), fill(0), fill(-3))
