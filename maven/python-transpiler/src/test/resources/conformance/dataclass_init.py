# Dataclass constructors: defaults, field(), keyword-only, frozen,
# __post_init__, and the errors a wrong call raises.
from dataclasses import dataclass, field, FrozenInstanceError
import dataclasses


@dataclass
class Point:
    x: int
    y: int = 0
    tags: list = field(init=False, default_factory=list)
    label: str = field(default="p", repr=False)


@dataclass(kw_only=True, frozen=True)
class Reg:
    name: str
    priority: float = 0.0


@dataclass
class Sized:
    w: float
    h: float
    area: float = field(init=False, default=0.0)

    def __post_init__(self):
        self.area = self.w * self.h


class Base:
    def hello(self):
        return "hi " + str(self.v)


@dataclasses.dataclass(order=True)
class Child(Base):
    v: int
    k: str = "a"


@dataclass
class Empty:
    pass


def show(e):
    print(type(e).__name__, e)


p = Point(1)
q = Point(2, y=5)
p.tags.append(3)
print(p, q, p.tags, q.tags, p.label, p == Point(1), p == q)
r = Reg(name="a", priority=2.5)
print(r, r.priority + 1, Reg(name="b"), hash(r) == hash(Reg(name="a", priority=2.5)))
try:
    r.name = "z"
except FrozenInstanceError as e:
    print("frozen", e)
s = Sized(2.0, 3.5)
print(s, s.area)
c = Child(3)
print(c, c.hello(), c < Child(4), sorted([Child(5, "b"), Child(5, "a"), Child(1)]))
print(Empty(), Empty() == Empty())
for bad in (lambda: Point(), lambda: Point(1, 2, 3), lambda: Point(1, x=2), lambda: Point(1, z=2),
            lambda: Reg("a"), lambda: Reg(), lambda: Sized(1.0, 2.0, 3.0), lambda: Empty(1)):
    try:
        bad()
    except TypeError as e:
        show(e)
print([f.name for f in dataclasses.fields(Point)], dataclasses.asdict(q), dataclasses.replace(q, y=9))
total = 0.0
for i in range(1000):
    total += Reg(name="n", priority=float(i)).priority + Point(i, i).y
print(total)


@dataclass(frozen=True)
class Key:
    kind: str
    rank: int = 1


k = Key("a")
keys = {k: 1, Key("b", 2): 2}
print(k, k.rank + 1, keys[Key("a")], Key("b", 2) in keys, k == Key("a", 1))
n = "ra" + "nk"
for act in (lambda: setattr(k, n, 5), lambda: delattr(k, n), lambda: setattr(k, "x" + "y", 1)):
    try:
        act()
    except FrozenInstanceError as e:
        print("frozen", e)
print(k.rank, sum(Key("z", i).rank for i in range(100)))
