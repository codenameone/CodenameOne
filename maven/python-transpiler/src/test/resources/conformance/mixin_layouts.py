# Compiled classes with extra bases: typing.Generic and method-only mixins.
from typing import Generic, TypeVar

T = TypeVar("T")


class Text:
    """A mixin: methods only, reached after the compiled chain."""

    def get_text(self):
        return "text:" + self.label()

    def __repr__(self):
        return "<%s %s>" % (type(self).__name__, self.label())


class Item:
    def __init__(self, x):
        self.x = x
        self.width = 0

    def label(self):
        return "item%d" % self.x

    def grow(self, n):
        for _ in range(n):
            self.width += 1
        return self.width


class Char(Item, Text):
    def __init__(self, x, c):
        Item.__init__(self, x)
        self.c = c

    def label(self):
        return "char-" + self.c


class Box(Item, Generic[T]):
    def __init__(self, x):
        super().__init__(x)
        self.items = []

    def add(self, v: T):
        self.items.append(v)
        return len(self.items)


class Holder(Generic[T]):
    def __init__(self, v):
        self.v = v

    def get(self):
        return self.v


class Line(Box):
    def label(self):
        return "line%d" % len(self.items)


def run():
    c = Char(1, "a")
    print(c.get_text(), repr(c), c.grow(3), c.width, c.x, c.c)
    print(isinstance(c, Text), isinstance(c, Item), [k.__name__ for k in type(c).__mro__])
    b = Box(2)
    print(b.add("p"), b.add(c), b.grow(2), b.label())
    ln = Line(3)
    ln.add(1)
    print(ln.label(), ln.grow(4), isinstance(ln, Box))
    h = Holder(5)
    print(h.get(), Holder[int](6).get())
    c.extra = 7
    print(c.extra, getattr(c, "missing", None))
    Text.shout = lambda self: self.label().upper()
    print(c.shout())


run()
