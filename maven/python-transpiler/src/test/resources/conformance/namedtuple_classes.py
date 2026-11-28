# Named tuple classes written out at build time (NamedTupleLowering) and
# classes deriving from typing's collection aliases.
from collections import namedtuple
from typing import NamedTuple, Mapping, Container
from urllib.parse import urlsplit, urlparse


class Version(namedtuple("Version", ["major", "minor", "micro"])):
    def bump(self):
        return self._replace(micro=self.micro + 1)


class Point(NamedTuple):
    """A point."""

    x: int
    y: int


class Frozen(Mapping[str, int]):
    def __init__(self, d):
        self._d = dict(d)

    def __getitem__(self, k):
        return self._d[k]

    def __iter__(self):
        return iter(self._d)

    def __len__(self):
        return len(self._d)


class Bag(Container[int]):
    def __init__(self, items):
        self.items = items

    def __contains__(self, x):
        return x in self.items


v = Version(1, 2, 3)
print(v, v.bump(), v._asdict(), v._fields, isinstance(v, tuple), len(v), v[1])
print(Version._make([4, 5, 6]), tuple(v), v == (1, 2, 3), hash(v) == hash((1, 2, 3)))
try:
    v._replace(nope=1)
except (TypeError, ValueError) as e:
    print("replace error", type(e).__name__)
try:
    Version._make([1])
except TypeError as e:
    print("make error", e)
p = Point(1, y=2)
print(p, p.x, p.y, Point.__doc__, Point._fields, Point.__bases__ == (tuple,), p._replace(x=5))
print(Point.__annotations__ if not isinstance(Point.__annotations__, str) else "")
x, y = p
print(x + y, Point.__name__, Version.__name__, repr(v))
f = Frozen({"a": 1, "b": 2})
print(f["a"], len(f), sorted(f), "a" in f, f.get("z", 9), sorted(f.items()), f == {"a": 1, "b": 2})
b = Bag([1, 2])
print(1 in b, 3 in b, Frozen.__bases__[0].__module__, Bag.__mro__[1].__name__)
s = urlsplit("http://u:p@h.com:80/a?q=1#f")
print(s, s.hostname, s.port, s.geturl(), s._replace(path="/b").geturl(), s.encode())
print(urlparse("https://x.org/p;r?q#z")._asdict(), isinstance(s, tuple), s[1], type(s).__name__)
