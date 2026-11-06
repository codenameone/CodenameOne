from dataclasses import dataclass, field, fields, asdict, astuple, replace, is_dataclass, FrozenInstanceError, \
    InitVar, KW_ONLY, make_dataclass
from typing import ClassVar, Optional, List


@dataclass
class Point:
    x: int
    y: int = 0

    def norm2(self):
        return self.x * self.x + self.y * self.y


p = Point(3, 4)
print(p, p.norm2(), Point(1), Point(y=2, x=1), p == Point(3, 4), p != Point(0, 0))
print([f.name for f in fields(p)], asdict(p), astuple(p), replace(p, y=9), is_dataclass(p), is_dataclass(int))
try:
    Point()
except TypeError as e:
    print("TypeError", e)
try:
    Point(1, 2, 3)
except TypeError as e:
    print("TypeError", e)
try:
    hash(p)
except TypeError as e:
    print("TypeError", e)


@dataclass(order=True, frozen=True)
class Version:
    major: int
    minor: int = 0
    label: str = field(default="", compare=False)


vs = sorted([Version(2, 1), Version(1, 5, "x"), Version(2, 0)])
print(vs, Version(1) < Version(1, 1), Version(3, 0, "a") == Version(3, 0, "b"), hash(Version(1)) == hash(Version(1)))
try:
    vs[0].major = 9
except FrozenInstanceError as e:
    print("FrozenInstanceError", e)


@dataclass
class Inventory:
    items: List[str] = field(default_factory=list)
    counts: dict = field(default_factory=dict, repr=False)
    owner: Optional[str] = None
    total: ClassVar[int] = 0

    def add(self, name):
        self.items.append(name)
        self.counts[name] = self.counts.get(name, 0) + 1
        Inventory.total += 1


a = Inventory()
b = Inventory(owner="bo")
a.add("apple")
a.add("apple")
print(a, b, a.counts, Inventory.total, [f.name for f in fields(Inventory)])


@dataclass
class Rect:
    w: float
    h: float
    area: float = field(init=False)
    scale: InitVar[int] = 1

    def __post_init__(self, scale):
        self.w *= scale
        self.h *= scale
        self.area = self.w * self.h


print(Rect(2, 3), Rect(2, 3, 10).area)


@dataclass
class Base:
    a: int
    b: str = "b"


@dataclass
class Child(Base):
    c: float = 1.5
    _: KW_ONLY
    d: int = 4


print(Child(1), Child(1, "x", 2.5, d=7), [f.name for f in fields(Child)])


@dataclass(kw_only=True)
class Opts:
    verbose: bool = False
    level: int


print(Opts(level=3), Opts(level=1, verbose=True))


@dataclass(eq=False)
class Ident:
    v: int


i1 = Ident(1)
print(i1 == Ident(1), i1 == i1, hash(i1) == hash(i1))


@dataclass
class Node:
    val: int
    children: list = field(default_factory=list)


tree = Node(1, [Node(2), Node(3, [Node(4)])])
print(tree)
print(asdict(tree))

match Point(0, 5):
    case Point(0, y):
        print("on y axis at", y)

Pair = make_dataclass("Pair", [("left", int), ("right", int, field(default=0))])
print(Pair(1), Pair(1, 2) == Pair(1, 2))
try:
    @dataclass
    class Bad:
        xs: list = []
except ValueError as e:
    print("ValueError", e)
