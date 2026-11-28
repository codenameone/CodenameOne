# Attribute stores reach only the classes their receiver can be; a loop
# over a class tuple elsewhere still unrolls, and one at module level too.
import math


class Item:
    __elements__ = ("a", "b")

    def __init__(self):
        self.a = 1
        self.b = 2
        self.c = 3

    def retag(self):
        self.__elements__ = ("c",)

    def dump(self):
        return [(n, self.a if n == "a" else self.b if n == "b" else self.c) for n in self.__elements__]


class Fill:
    __elements__ = ("x", "y")

    def __init__(self):
        self.x = 10
        self.y = 20

    def dump(self):
        out = []
        for name in self.__elements__:
            out.append((name, getattr(self, name)))
        return out


class Holder:
    def __init__(self):
        self.items = [Item(), Item()]

    def retag(self):
        for s in self.items:
            s.retag()


h = Holder()
print([i.dump() for i in h.items])
h.retag()
print([i.dump() for i in h.items])
print(Fill().dump())

for name in ["sqrt", "floor", "ceil"]:
    print(name, getattr(math, name)(2.5))
