def scale(x, *, by=2, add=0):
    return x * by + add


class Box:
    def __init__(self, w, *, h, depth=1.0):
        self.w = w
        self.h = h
        self.depth = depth

    def area(self, *, factor=1):
        return self.w * self.h * factor

    def grow(self, n, *, step):
        return Box(self.w + n * step, h=self.h + step)


total = 0
for i in range(1000):
    total += scale(i) + scale(i, by=3) + scale(i, add=1, by=1) + scale(x=i, add=2)
print(total)

b = Box(2, h=3)
c = Box(w=4, depth=2.5, h=5)
print(b.area(), b.area(factor=2), c.area(), c.depth, b.depth)
g = b.grow(2, step=3)
print(g.w, g.h, g.area())
acc = 0
for i in range(500):
    acc += Box(i, h=2).area(factor=3)
print(acc)

for bad in (lambda: Box(1, 2), lambda: Box(1), lambda: scale(1, 2), lambda: b.area(2), lambda: b.grow(1, 2),
            lambda: Box(1, h=2, q=3), lambda: scale(1, by=2, zz=3)):
    try:
        bad()
    except TypeError as e:
        print("TypeError", e)

opts = {"by": 5}
print(scale(3, **opts), Box(1, **{"h": 7}).area())

GENERIC = 10.0
SPECIFIC = 0.0
NAME = "dflt"


class Registry:
    SPECIFIC = 99.0

    def __init__(self):
        self.items = []

    def add(self, item, *, priority=SPECIFIC, tag=NAME):
        self.items.append((priority, tag, item))

    def add_generic(self, item, priority=GENERIC):
        self.add(item, priority=priority)


reg = Registry()
reg.add("a")
reg.add("b", priority=GENERIC)
reg.add_generic("c")
reg.add("d", tag="t")
print(reg.items)


def late(x=LATER if False else 1):
    return x


LATER = 5
print(late())
