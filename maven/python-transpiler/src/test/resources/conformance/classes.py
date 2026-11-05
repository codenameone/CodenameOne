# Classes, inheritance, properties, dunders and context managers.
class Shape:
    """A shape."""
    count = 0

    def __init__(self, name):
        self.name = name
        Shape.count += 1

    def area(self):
        raise NotImplementedError

    def __repr__(self):
        return f"{type(self).__name__}({self.name!r}, area={self.area():.1f})"

class Rect(Shape):
    def __init__(self, w, h):
        super().__init__("rect")
        self.w, self.h = w, h

    def area(self):
        return self.w * self.h

class Square(Rect):
    def __init__(self, s):
        super().__init__(s, s)
        self.name = "square"

    @property
    def side(self):
        return self.w

    @side.setter
    def side(self, v):
        self.w = self.h = v

    @staticmethod
    def unit():
        return Square(1)

    @classmethod
    def of(cls, s):
        return cls(s)

shapes = [Rect(2, 3), Square(4), Square.unit(), Square.of(2)]
print(shapes, Shape.count, Shape.__doc__)
sq = shapes[1]
sq.side = 5
print(sq.side, sq.area(), isinstance(sq, Rect), issubclass(Square, Shape), Square.__mro__ if False else "")

class Vec:
    def __init__(self, x, y):
        self.x, self.y = x, y
    def __add__(self, o):
        return Vec(self.x + o.x, self.y + o.y)
    def __mul__(self, k):
        return Vec(self.x * k, self.y * k)
    __rmul__ = __mul__
    def __eq__(self, o):
        return isinstance(o, Vec) and (self.x, self.y) == (o.x, o.y)
    def __hash__(self):
        return hash((self.x, self.y))
    def __len__(self):
        return 2
    def __getitem__(self, i):
        return (self.x, self.y)[i]
    def __iter__(self):
        return iter((self.x, self.y))
    def __lt__(self, o):
        return (self.x, self.y) < (o.x, o.y)
    def __repr__(self):
        return "Vec(%r, %r)" % (self.x, self.y)

v = Vec(1, 2) + Vec(3, 4)
print(v, v * 2, 3 * v, v == Vec(4, 6), len(v), v[1], list(v), {v, Vec(4, 6)}, sorted([Vec(2, 0), Vec(1, 5)]))

class Ctx:
    def __init__(self, tag):
        self.tag = tag
    def __enter__(self):
        print("enter", self.tag)
        return self
    def __exit__(self, t, v, tb):
        print("exit", self.tag, t.__name__ if t else None)
        return t is KeyError

with Ctx("a") as a, Ctx("b"):
    print("body", a.tag)
with Ctx("c"):
    raise KeyError("swallowed")
print("after")

class MyError(Exception):
    def __init__(self, code):
        super().__init__(f"code {code}")
        self.code = code

try:
    raise MyError(7)
except Exception as e:
    print(type(e).__name__, e, e.code, isinstance(e, MyError))

class Node:
    def __init__(self, val, nxt=None):
        self.val, self.nxt = val, nxt
    def __getattr__(self, name):
        return f"<missing {name}>"

n = Node(1, Node(2))
print(n.nxt.val, n.color, getattr(n, "val"), hasattr(n, "nxt"))
