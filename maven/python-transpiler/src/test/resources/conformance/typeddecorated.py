# Decorated and rebound functions: typed bodies entered only by their door.
import functools


def logged(fn):
    @functools.wraps(fn)
    def wrapper(*a):
        return "<" + str(fn(*a)) + ">"
    return wrapper


class Base:
    def __init__(self, w: int):
        self._w = w

    @property
    def width(self):
        total = 0
        for i in range(self._w):
            total += i
        return total

    @width.setter
    def width(self, v):
        if v < 0:
            raise ValueError("negative " + str(v))
        self._w = v

    @classmethod
    def make(cls, n):
        return cls(n * 2)

    @staticmethod
    def twice(s):
        return s + s

    def describe(self):
        return "base " + str(self._w)


class Derived(Base):
    @property
    def width(self):
        return super().width * 10

    def describe(self):
        return super().describe() + " derived"


@logged
def wrapped(x, y):
    return x * y + 1


if True:
    def chosen(s):
        return s.upper()
else:
    def chosen(s):
        return s


def main():
    b = Base(4)
    print(b.width, Base.make(3).width, Base.twice("ab"), b.describe())
    b.width = 2
    print(b.width)
    try:
        b.width = -1
    except ValueError as e:
        print("error", e)
    d = Derived(3)
    print(d.width, d.describe(), Derived.make(1).describe(), type(Derived.make(1)).__name__)
    print(wrapped(3, 4), wrapped(2.5, 2), chosen("hey"), Base.twice(3))


main()
