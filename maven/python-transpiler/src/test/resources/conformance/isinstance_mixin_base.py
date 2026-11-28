# isinstance against a class an instance derives from only through a base
# other than its first: the test passes, and the instance is still read.


class Base:
    def __init__(self, el):
        self._el = el

    def __eq__(self, other):
        if not isinstance(other, Base):
            return False
        return self._el is other._el

    def __hash__(self):
        return id(self._el)


class Shape(Base):
    pass


class Inherits:
    def __init__(self, el):
        self._el = el

    def width(self):
        return len(self._el)


class Placeholder(Inherits, Shape):
    pass


def same(a, b):
    return a == b


el = ["x", "y"]
s = Shape(el)
p = Placeholder(el)
print(same(s, p), same(p, s), same(s, Shape(el)), same(s, Shape([])), p.width())
print([same(a, b) for a in (s, p) for b in (p, s)])
