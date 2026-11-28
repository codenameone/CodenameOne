# Methods replaced by a store: each must be looked up per call, while the
# methods no store reaches stay typed.


def with_init(cls):
    def __init__(self, v):
        self.v = v * 10
    cls.__init__ = __init__
    return cls


class Plain:
    def __init__(self, v):
        self.v = v

    def get(self):
        return self.v


class Decorated:
    def __init__(self, v):
        self.v = v

    def get(self):
        return self.v


with_init(Decorated)


class Shape:
    def __init__(self, n):
        self.n = n

    def area(self):
        return self.n * self.n

    def name(self):
        return "shape"


def triple(self):
    return self.n * 3


Shape.area = triple


class Greeter:
    def __init__(self, who):
        self.who = who

    def greet(self):
        return "hello " + self.who


g = Greeter("a")
g.greet = lambda: "shadowed"


class Holder:
    def __init__(self):
        self.k = 1

    def describe(self):
        return "holder %d" % self.k


holders = [Holder(), Holder()]


def poke(objs):
    for o in objs:
        o.describe = lambda: "poked"


poke(holders)


class Base:
    def __init__(self):
        self.x = 2

    def value(self):
        return self.x


class Child(Base):
    pass


def patch_base(b: Base):
    b.value = lambda: 99


ch = Child()
patch_base(ch)

print(Plain(3).get())
print(Decorated(3).get())
print(Shape(4).area(), Shape(4).name())
print(g.greet(), Greeter("b").greet())
print(holders[0].describe(), holders[1].describe(), Holder().describe())
print(ch.value(), Child().value(), Base().value())
