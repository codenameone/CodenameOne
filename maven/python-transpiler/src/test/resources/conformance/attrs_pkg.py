import attr
import attrs
from attrs import define, frozen, field, mutable, asdict, astuple, evolve, fields, has, validators, converters
from attrs import Factory, NOTHING
from attrs.exceptions import FrozenInstanceError


def show(label, fn):
    try:
        print(label, "->", fn())
    except Exception as e:
        print(label, "raised", type(e).__name__, e.args[0] if e.args else "")


@define
class Point:
    x: int
    y: int = 0


p = Point(1, 2)
print(p, Point(3), Point(y=5, x=4))
print(p == Point(1, 2), p != Point(1, 3), p == 5)
print(Point.__hash__ is None)
show("lt", lambda: p < Point(2, 2))
show("missing", lambda: Point())
show("extra", lambda: Point(1, 2, 3))
show("badkw", lambda: Point(1, z=3))
print(has(Point), has(int))
print([f.name for f in fields(Point)], fields(Point).x.name, fields(Point).y.default)
print(asdict(p), astuple(p))
p.x = 10
print(p)
print(Point.__match_args__)


@define(order=True)
class Ver:
    major: int
    minor: int = 0


print(Ver(1, 2) < Ver(1, 3), Ver(2) > Ver(1, 9), sorted([Ver(2), Ver(1, 5), Ver(1)]))
print(Ver(1) <= Ver(1), Ver(1) >= Ver(2))


@frozen
class Frozen:
    name: str
    tags: tuple = ()


f = Frozen("a", (1, 2))
print(f, hash(f) == hash(Frozen("a", (1, 2))), f == Frozen("a", (1, 2)))
show("frozen set", lambda: setattr(f, "name", "b"))
try:
    f.name = "c"
except FrozenInstanceError as e:
    print("FrozenInstanceError caught", isinstance(e, AttributeError))
try:
    f.name = "c"
except attr.exceptions.FrozenInstanceError:
    print("attr.exceptions caught")
print(evolve(f, name="z"), f)


@define
class WithFactory:
    items: list = Factory(list)
    d: dict = field(factory=dict)
    n: int = field(default=3)
    twice: int = field(init=False)

    @twice.default
    def _twice(self):
        return self.n * 2


a = WithFactory()
b = WithFactory(n=5)
a.items.append(1)
print(a, b, b.items)
print(Factory(list))


@define
class SelfFactory:
    a: int
    b: int = Factory(lambda self: self.a + 1, takes_self=True)


print(SelfFactory(1), SelfFactory(1, 7))


@define
class Validated:
    x: int = field(validator=validators.instance_of(int))
    name: str = field(default="n", validator=[validators.instance_of(str), validators.matches_re("^[a-z]+$")])
    opt: object = field(default=None, validator=validators.optional(validators.instance_of(int)))
    kind: str = field(default="a", validator=validators.in_(["a", "b"]))
    pos: int = field(default=1, validator=[validators.ge(0), validators.lt(100)])

    @x.validator
    def _check_x(self, attribute, value):
        if value == 13:
            raise ValueError("unlucky " + attribute.name)


print(Validated(1))
show("v type", lambda: Validated("s"))
show("v name", lambda: Validated(1, name="Bad1"))
show("v opt ok", lambda: Validated(1, opt=3))
show("v opt bad", lambda: Validated(1, opt="x"))
show("v in", lambda: Validated(1, kind="c"))
show("v pos", lambda: Validated(1, pos=-1))
show("v pos2", lambda: Validated(1, pos=100))
show("v dec", lambda: Validated(13))
v = Validated(2)
show("set invalid", lambda: setattr(v, "x", "q"))
show("set invalid2", lambda: setattr(v, "x", 13))
v.x = 4
print(v.x)
vv = Validated(1)
attrs.validate(vv)
object.__setattr__(vv, "x", "zz")
show("validate()", lambda: attrs.validate(vv))


@define
class Lists:
    xs: list = field(validator=validators.deep_iterable(validators.instance_of(int), validators.instance_of(list)))
    m: dict = field(default=Factory(dict),
                    validator=validators.deep_mapping(validators.instance_of(str), validators.instance_of(int)))
    s: str = field(default="ab", validator=[validators.min_len(1), validators.max_len(3)])
    c: object = field(default=len, validator=validators.is_callable())


print(Lists([1, 2]))
show("deep it", lambda: Lists([1, "a"]))
show("deep it2", lambda: Lists((1, 2)))
show("deep map", lambda: Lists([1], {"a": "b"}))
show("min", lambda: Lists([1], s=""))
show("max", lambda: Lists([1], s="abcd"))
show("callable", lambda: Lists([1], c=5))


@define
class Converted:
    n: int = field(converter=int)
    s: str = field(default=None, converter=converters.optional(str))
    z: int = field(default=None, converter=converters.default_if_none(7))
    b: bool = field(default="yes", converter=converters.to_bool)
    p: int = field(default="3", converter=converters.pipe(str.strip, int))


c = Converted("5")
print(c)
print(Converted(1, 2, None, "off", " 9 "))
c.n = "8"
print(c.n, type(c.n).__name__)
show("conv bad", lambda: Converted("x"))
show("to_bool bad", lambda: converters.to_bool("maybe"))


@define
class Kw:
    a: int
    b: int = field(kw_only=True, default=2)
    c: int = field(kw_only=True)


print(Kw(1, c=3))
show("kw missing", lambda: Kw(1))
show("kw positional", lambda: Kw(1, 2, 3))


@define(kw_only=True)
class AllKw:
    a: int
    b: int = 5


print(AllKw(a=1))
show("allkw", lambda: AllKw(1))


@define
class Post:
    a: int
    b: int = field(init=False)

    def __attrs_post_init__(self):
        self.b = self.a * 10


print(Post(4), Post(4).b)


@define
class Priv:
    _hidden: int = 1
    shown: int = 2


print(Priv(), Priv(hidden=5), fields(Priv)._hidden.alias)


@define
class Opts:
    a: int
    secret: str = field(default="s", repr=False)
    ignored: int = field(default=0, eq=False)
    shout: str = field(default="x", repr=lambda v: v.upper())


print(Opts(1), Opts(1, ignored=3) == Opts(1, ignored=4), Opts(1, secret="q") == Opts(1))


@define
class Base:
    a: int
    b: int = 2


@define
class Child(Base):
    c: int = 3
    b: int = 20


print(Child(1), [f.name for f in fields(Child)], [f.inherited for f in fields(Child)])
print(Child(1) == Base(1), Child(1) == Child(1))
show("mandatory after default", lambda: define(type("Bad", (), {"__annotations__": {"a": int, "b": int},
                                                              "a": 1})))


@define
class Inner:
    v: int


@define
class Outer:
    inner: Inner
    items: list
    mapping: dict
    tup: tuple = ()


o = Outer(Inner(1), [Inner(2), 3], {"k": Inner(4)}, (Inner(5), 6))
print(asdict(o))
print(asdict(o, recurse=False))
print(attr.asdict(o, retain_collection_types=True))
print(astuple(o))
print(asdict(o, filter=lambda a, v: a.name != "tup"))
print(asdict(o, filter=attrs.filters.exclude(attrs.fields(Outer).tup, "items")))
print(asdict(Inner(3), value_serializer=lambda inst, a, v: v + 1))
print(evolve(o, tup=(1,)).tup, o.tup)
print(evolve(Priv(), hidden=9))
show("evolve bad", lambda: evolve(Point(1), q=2))
show("fields bad", lambda: fields(int))
show("fields inst", lambda: fields(Point(1)))


# attr.s / attr.ib style
@attr.s
class Old(object):
    x = attr.ib()
    y = attr.ib(default=5, validator=attr.validators.instance_of(int))
    z = attr.ib(factory=list)
    w = attr.ib(default="w", repr=False)


o1 = Old(1)
print(o1, o1 == Old(1), o1 < Old(2), Old(1, 2) > Old(1, 1))
print([a.name for a in attr.fields(Old)], attr.fields(Old).y.default)
show("old validator", lambda: Old(1, "no"))
o1.x = "changes freely"
print(o1.x, hash(o1) if False else "unhashable" if Old.__hash__ is None else "hashable")


@attr.s(frozen=True, auto_attribs=True)
class OldFrozen:
    a: int
    b: str = "b"


of = OldFrozen(1)
print(of, hash(of) == hash(OldFrozen(1)))
show("old frozen", lambda: setattr(of, "a", 2))
print(attr.evolve(of, b="c"))
print(attr.asdict(of))


@attr.s(eq=False)
class NoEq:
    a = attr.ib()


print(NoEq(1) == NoEq(1), hash(NoEq(1)) is not None)


@attr.s(order=False)
class NoOrder:
    a = attr.ib()


show("no order", lambda: NoOrder(1) < NoOrder(2))


@attr.s
class OrderBy:
    b = attr.ib(default=1)
    a = attr.ib(default=2)


print(OrderBy(), [f.name for f in attr.fields(OrderBy)])


@attr.s
class Decorated:
    n = attr.ib()
    m = attr.ib()

    @n.validator
    def _vn(self, attribute, value):
        if value < 0:
            raise ValueError("neg")

    @m.default
    def _dm(self):
        return self.n + 1


print(Decorated(2))
show("decorated", lambda: Decorated(-1))


@attr.s(auto_attribs=True)
class Auto:
    a: int
    b: list = attr.Factory(list)
    c: int = attr.ib(default=3)


print(Auto(1), Auto(1) == Auto(1, [], 3))

Dyn = attr.make_class("Dyn", ["a", "b"])
print(Dyn(1, 2), Dyn(1, 2) == Dyn(1, 2))
Dyn2 = attrs.make_class("Dyn2", {"x": attr.ib(default=1), "y": attr.ib(default=2)}, frozen=True)
print(Dyn2(), Dyn2(y=5))


@define
class Custom:
    a: int

    def __repr__(self):
        return "custom!"

    def __eq__(self, other):
        return True


print(Custom(1), Custom(1) == Custom(2))


@define(repr=False, eq=False)
class Plain:
    a: int


print(Plain(1) == Plain(1))
print(attr.NOTHING, repr(attr.NOTHING), bool(attr.NOTHING))

with validators.disabled():
    print(Validated("not an int"))
print(validators.get_disabled())


@define
class SetHooks:
    a: int = field(default=1, on_setattr=attrs.setters.NO_OP, converter=int)
    b: int = field(default=1, converter=int)


sh = SetHooks()
sh.a = "5"
sh.b = "6"
print(sh.a, sh.b)
print(validators.instance_of(int))
print(repr(validators.optional(validators.instance_of(int))))
