import datetime
import enum
from typing import Annotated, Any, Dict, List, Literal, Optional, Union

from pydantic import (AfterValidator, BaseModel, BeforeValidator, ConfigDict, Field, RootModel, SecretStr,
                      TypeAdapter, ValidationError, computed_field, field_serializer, field_validator,
                      model_validator, PositiveInt, conint, constr)


def err(fn):
    try:
        r = fn()
        print("OK", r)
    except ValidationError as e:
        print(e.error_count(), [(d["type"], d["loc"], d["msg"]) for d in e.errors()])


class User(BaseModel):
    id: int
    name: str
    email: Optional[str] = None
    tags: List[str] = []
    score: float = 1.5


u = User(id="7", name="Ann")
print(u)
print(repr(u))
print(u.id, type(u.id).__name__, u.tags, u.score)
print(u.model_dump())
print(u.model_dump_json())
print(u.model_dump_json(indent=2))
print(sorted(u.model_fields_set))
print(User(id=1, name="x") == User(id=1, name="x"), User(id=1, name="x") == User(id=2, name="x"))
print(list(User.model_fields))
print(User.model_fields["id"].is_required(), User.model_fields["tags"].is_required())

# lax coercion
class C(BaseModel):
    i: int
    f: float
    s: str
    b: bool


print(C(i="12", f="1.5", s="x", b="yes"))
print(C(i=3.0, f=2, s="y", b=0))
print(C(i=True, f="  3 ", s="z", b="off"))

# error cases
err(lambda: User(id="x", name="a"))
err(lambda: User(name="a"))
err(lambda: User(id=1.5, name="a"))
err(lambda: User(id=1, name=5))
err(lambda: User(id=1, name="a", tags="abc"))
err(lambda: User(id=1, name="a", tags=[1, "b"]))
err(lambda: User(id=1, name="a", score="zz"))
err(lambda: User(id=None, name=None))
err(lambda: C(i=1, f=1, s="a", b="maybe"))
err(lambda: C(i=1, f=1, s="a", b=7))
err(lambda: C(i=[], f={}, s="a", b=[]))
err(lambda: User.model_validate("nope"))
err(lambda: User.model_validate({"id": 1}))
err(lambda: User.model_validate_json('{"id": 1, "name": "q"}'))
err(lambda: User.model_validate_json('{"id": "z"}'))

try:
    User(id="x", name="a")
except ValidationError as e:
    print(str(e))
    print(e.errors()[0])
    print(isinstance(e, ValueError), e.title)

try:
    User.model_validate_json("{bad")
except ValidationError as e:
    print([d["type"] for d in e.errors()])

# Field constraints
class P(BaseModel):
    age: int = Field(gt=0, le=120)
    name: str = Field(min_length=2, max_length=5)
    code: str = Field(pattern=r"^[A-Z]{2}\d+$", default="AB1")
    ratio: float = Field(default=0.5, ge=0, lt=1)
    qty: int = Field(default=10, multiple_of=5)
    items: List[int] = Field(default_factory=list)


print(P(age=5, name="abc"))
err(lambda: P(age=0, name="abc"))
err(lambda: P(age=121, name="abc"))
err(lambda: P(age=5, name="a"))
err(lambda: P(age=5, name="abcdef"))
err(lambda: P(age=5, name="abc", code="xx"))
err(lambda: P(age=5, name="abc", ratio=1))
err(lambda: P(age=5, name="abc", ratio=-0.1))
err(lambda: P(age=5, name="abc", qty=7))
try:
    P(age=0, name="a")
except ValidationError as e:
    print(e.errors()[0]["ctx"], e.errors()[1]["ctx"])


YsT = Annotated[List[str], Field(min_length=1)]
Ge2 = conint(ge=2)
Stripped = constr(strip_whitespace=True, min_length=2)


class L(BaseModel):
    xs: List[int] = Field(min_length=1, max_length=2)
    ys: YsT = ["a"]
    n: PositiveInt = 1
    m: Ge2 = 2
    k: Stripped = "ab"


print(L(xs=[1], n=3, k="  hello "))
err(lambda: L(xs=[]))
err(lambda: L(xs=[1, 2, 3]))
err(lambda: L(xs=[1], ys=[]))
err(lambda: L(xs=[1], n=0))
err(lambda: L(xs=[1], m=1))
err(lambda: L(xs=[1], k=" a "))

# aliases and config
class A(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    first_name: str = Field(alias="firstName")
    age: int = 0


a = A(firstName="Z")
print(a, a.first_name)
print(A(first_name="Y"))
print(a.model_dump(), a.model_dump(by_alias=True))
print(a.model_dump_json(by_alias=True))
err(lambda: A(age=1))


class NoPop(BaseModel):
    first_name: str = Field(alias="firstName")


err(lambda: NoPop(first_name="x"))
print(NoPop(firstName="x"))


class Forbid(BaseModel):
    model_config = ConfigDict(extra="forbid")
    a: int


class Allow(BaseModel):
    model_config = ConfigDict(extra="allow")
    a: int


class Ignore(BaseModel):
    a: int


err(lambda: Forbid(a=1, b=2, c=3))
al = Allow(a=1, b=[1], c="x")
print(al, al.b, al.model_extra, al.model_dump())
print(Ignore(a=1, b=2), Ignore(a=1, b=2).model_dump())


class Frozen(BaseModel):
    model_config = ConfigDict(frozen=True)
    a: int
    b: tuple = ()


fz = Frozen(a=1)
try:
    fz.a = 2
except ValidationError as e:
    print([(d["type"], d["loc"], d["msg"]) for d in e.errors()])
print(hash(Frozen(a=1)) == hash(Frozen(a=1)), fz == Frozen(a=1))


class Strip(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True, str_to_lower=True)
    s: str


print(Strip(s="  HeLLo "))


class Mut(BaseModel):
    a: int


m = Mut(a=1)
m.a = 5
print(m, m.model_fields_set)
try:
    m.zzz = 1
except ValueError as e:
    print("ValueError", e)


class VA(BaseModel):
    model_config = ConfigDict(validate_assignment=True)
    a: int


v = VA(a=1)
v.a = "5"
print(v)
err(lambda: setattr(v, "a", "q"))

# nested
class Addr(BaseModel):
    city: str
    zip: Optional[str] = None


class Person(BaseModel):
    name: str
    addr: Addr
    others: List[Addr] = []
    by_name: Dict[str, Addr] = {}


p = Person(name="n", addr={"city": "X"}, others=[{"city": "Y", "zip": "1"}], by_name={"k": Addr(city="Z")})
print(p)
print(p.model_dump())
print(p.model_dump(exclude_none=True))
print(p.model_dump_json())
print(p.addr.city, type(p.others[0]).__name__)
err(lambda: Person(name="n", addr={}, others=[{"city": 1}, "q"], by_name={"k": {}}))
err(lambda: Person(name="n", addr=5))

# unions, literals, enums, optional
class Color(enum.Enum):
    RED = "r"
    BLUE = "b"


class Num(enum.IntEnum):
    ONE = 1
    TWO = 2


class U(BaseModel):
    a: Union[int, str]
    b: Optional[int] = None
    c: Literal["x", "y"] = "x"
    d: Color = Color.RED
    e: Num = Num.ONE
    f: Union[List[int], int, None] = None
    g: Literal[1, 2, 3] = 1


print(U(a="5"), U(a=5), U(a=5.0), U(a="abc"))
print(U(a=1, b="3", c="y", d="b", e=2, f=[1, "2"]))
print(U(a=1, d=Color.BLUE).model_dump(), U(a=1, d=Color.BLUE).model_dump(mode="json"))
err(lambda: U(a=[1]))
err(lambda: U(a=1, b="z"))
err(lambda: U(a=1, c="z"))
err(lambda: U(a=1, d="g"))
err(lambda: U(a=1, e=3))
err(lambda: U(a=1, f="s"))
err(lambda: U(a=1, g=4))

# collections
class Coll(BaseModel):
    s: set = set()
    fs: frozenset = frozenset()
    t: tuple = ()
    t2: tuple[int, str] = (0, "")
    t3: tuple[int, ...] = ()
    d: dict[str, int] = {}
    ss: set[int] = set()


cc = Coll(s=[1, 2], t=[1], t2=["3", "4"], t3=("1", 2), d={"a": "1"}, ss=[1, 1, "2"])
print(cc.s == {1, 2}, cc.t, cc.t2, cc.t3, cc.d, cc.ss == {1, 2})
print(cc.model_dump_json() == cc.model_dump_json())
print(Coll(t3=[1, 2]).model_dump(mode="json")["t3"], Coll(ss=[1]).model_dump(mode="json")["ss"])
err(lambda: Coll(t2=[1]))
err(lambda: Coll(t2=[1, 2, 3]))
err(lambda: Coll(d={"a": "z", "b": 1}))
err(lambda: Coll(d=[1]))
err(lambda: Coll(ss={"a"}))

# datetime
class Ev(BaseModel):
    when: datetime.datetime
    day: datetime.date = datetime.date(2020, 1, 1)
    gap: datetime.timedelta = datetime.timedelta(seconds=90)


ev = Ev(when="2024-03-05T10:20:30", day="2024-03-06")
print(ev)
print(ev.model_dump_json())
print(ev.model_dump(mode="json"))
print(Ev(when=datetime.datetime(2020, 1, 2, 3, 4, 5), day=datetime.date(2021, 2, 3), gap=30))
print(Ev(when="2024-03-05T10:20:30Z").when.tzinfo is not None)
err(lambda: Ev(when="garbage"))
err(lambda: Ev(when=None))

for bad in ["x", "2020-13-45"]:
    try:
        Ev(when="2020-01-01T00:00:00", day=bad)
    except ValidationError as e:
        print([(d["type"], d["loc"]) for d in e.errors()])

# validators
class V(BaseModel):
    name: str
    age: int
    tags: List[str] = []

    @field_validator("name")
    @classmethod
    def upper(cls, v):
        return v.upper()

    @field_validator("age")
    @classmethod
    def adult(cls, v):
        if v < 18:
            raise ValueError("too young")
        return v

    @field_validator("tags", mode="before")
    @classmethod
    def split(cls, v):
        if isinstance(v, str):
            return v.split(",")
        return v

    @field_validator("name", "age")
    @classmethod
    def positive_name(cls, v, info):
        if info.field_name == "name" and v == "BAD":
            raise ValueError("bad name")
        return v


print(V(name="bob", age=20, tags="a,b"))
err(lambda: V(name="bob", age=3))
err(lambda: V(name="bad", age="x"))
try:
    V(name="bob", age=3)
except ValidationError as e:
    print(str(e))


class MV(BaseModel):
    a: int
    b: int
    total: int = 0

    @model_validator(mode="before")
    @classmethod
    def pre(cls, data):
        if isinstance(data, dict) and "a_b" in data:
            x, y = data["a_b"].split("-")
            return {"a": x, "b": y}
        return data

    @model_validator(mode="after")
    def post(self):
        if self.a > self.b:
            raise ValueError("a must not exceed b")
        self.total = self.a + self.b
        return self


print(MV(a=1, b=2))
print(MV(a_b="3-4"))
err(lambda: MV(a=5, b=2))
err(lambda: MV(a_b="x-y"))


class AssertV(BaseModel):
    n: int

    @field_validator("n")
    @classmethod
    def chk(cls, v):
        assert v % 2 == 0, "n must be even"
        return v


err(lambda: AssertV(n=3))
print(AssertV(n=4))


Doubled = Annotated[int, AfterValidator(lambda v: v * 2)]
StrB = Annotated[str, BeforeValidator(lambda v: str(v))]


class AnnV(BaseModel):
    a: Doubled
    b: StrB = "zz"


print(AnnV(a=2, b=100))
err(lambda: AnnV(a="x"))


# computed fields and serializers
class Rect(BaseModel):
    w: int
    h: int

    @computed_field
    @property
    def area(self) -> int:
        return self.w * self.h

    @field_serializer("w")
    def ser_w(self, v):
        return "w=" + str(v)


r = Rect(w=2, h=3)
print(r)
print(r.area, r.model_dump(), r.model_dump_json())

# include/exclude, copy
big = Person(name="n", addr={"city": "X", "zip": "9"}, others=[{"city": "Y"}, {"city": "Z"}])
print(big.model_dump(include={"name", "addr"}))
print(big.model_dump(exclude={"others", "by_name"}))
print(big.model_dump(exclude={"addr": {"zip"}, "by_name": True, "others": {0: True}}))
print(big.model_dump(include={"addr": {"city"}}))
print(big.model_dump(exclude_unset=True))
print(User(id=1, name="a").model_dump(exclude_defaults=True))
cp = big.model_copy(update={"name": "other"})
print(cp.name, big.name, cp.addr is big.addr)
dp = big.model_copy(deep=True)
print(dp == big, dp.addr is big.addr)
print(User.model_construct(id="no", name="check"))

# schema
print(User.model_json_schema())
print(P.model_json_schema())
print(Person.model_json_schema())
print(U.model_json_schema())
print(Forbid.model_json_schema())

# TypeAdapter
ta = TypeAdapter(List[int])
print(ta.validate_python(["1", 2]), ta.validate_json("[1, 2, 3]"))
err(lambda: ta.validate_python(["x", 2, None]))
print(TypeAdapter(Dict[str, float]).validate_python({"a": "1.5"}))
print(TypeAdapter(Optional[int]).validate_python(None), TypeAdapter(int).validate_python("4"))
print(ta.dump_python([1, 2]), ta.dump_json([1, 2]))
print(TypeAdapter(User).validate_python({"id": 1, "name": "t"}))
print(TypeAdapter(List[User]).dump_python([User(id=1, name="t")]))
print(TypeAdapter(List[int]).json_schema())
print(TypeAdapter(Annotated[int, Field(gt=5)]).validate_python(6))
err(lambda: TypeAdapter(Annotated[int, Field(gt=5)]).validate_python(2))

# RootModel, SecretStr, self reference, private attributes
class Pets(RootModel[List[str]]):
    pass


pt = Pets(["a", "b"])
print(pt.root, pt.model_dump(), pt.model_dump_json())
print(Pets.model_validate(["c"]))
err(lambda: Pets.model_validate("x"))


class Sec(BaseModel):
    pw: SecretStr


sc = Sec(pw="hunter2")
print(sc, sc.pw.get_secret_value(), sc.model_dump_json())


class Tree(BaseModel):
    v: int
    kids: List["Tree"] = []
    parent: Optional["Tree"] = None


t = Tree(v=1, kids=[{"v": 2, "kids": [{"v": 3}]}])
print(t)
print(t.model_dump())
err(lambda: Tree(v=1, kids=[{"v": "x"}]))


class Priv(BaseModel):
    a: int
    _hidden: int = 5

    def bump(self):
        self._hidden += 1
        return self._hidden


pr = Priv(a=1)
print(pr, pr.bump(), pr.model_dump())

# inheritance
class Base(BaseModel):
    id: int


class Child(Base):
    name: str = "c"


print(Child(id="2"), list(Child.model_fields))
err(lambda: Child())
print(isinstance(Child(id=1), Base))
print(Base.model_validate(Child(id=1)))
