# typing.cast(T, v) to a compiled class: v typed as T, checked.
from typing import cast


class Shape:
    def __init__(self, n):
        self.n = n
        self.kind = "plain"

    def area(self):
        return self.n * self.n


class Square(Shape):
    pass


def make(k):
    return Square(k) if k else None


def build(k):
    s = cast(Shape, make(k))
    if s is None:
        return "none"
    s.kind = "set"
    return (s.area(), s.kind, type(s).__name__)


print(build(3), build(0))
items = [cast(Shape, Square(i)) for i in range(3)]
print(sum(x.area() for x in items))
