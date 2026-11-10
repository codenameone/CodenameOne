class Thing:
    def __init__(self, i):
        self.a = i


def probe(n):
    hits = 0
    for i in range(n):
        t = Thing(i)
        hits += getattr(t, "b", 2)
        hits += getattr(t, "a", 100)
        try:
            hits += t.c
        except AttributeError:
            hits += 3
    return hits


def described():
    t = Thing(5)
    try:
        return t.zz
    except AttributeError:
        return "missing"


print(probe(10))
print(described())
Thing.c = 1000
Thing.b = 7
print(probe(10))


class Bound:
    def __init__(self, v):
        self.v = v


def method_later(n):
    out = []
    for i in range(n):
        b = Bound(i)
        out.append(getattr(b, "twice", None))
    return out


print(method_later(2))
Bound.twice = lambda self: self.v * 2
print([f() for f in method_later(3)])
