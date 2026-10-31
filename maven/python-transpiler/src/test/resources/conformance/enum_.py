
from enum import *
class C(Enum):
    RED=1; GREEN=auto(); BLUE=auto(); ALIAS=1
    def describe(self): return self.name.lower()
print(list(C), C(2), C['BLUE'], repr(C.RED), C.ALIAS is C.RED, len(C), C.RED in C, C.GREEN.describe(), list(C.__members__))
class P(Flag):
    R=auto(); W=auto(); X=auto()
print(repr(P.R|P.W), P.R|P.W, list(P.R|P.X), P.W in (P.R|P.W), repr(~P.R), bool(P(0)), repr(P(0)))
class N(IntEnum):
    A=1; B=2
print(N.A+1, str(N.B), f'{N.A}', repr(N.B), N(2), N.A==1)
class M(int, Enum):
    A=1
print(str(M.A), repr(M.A), f'{M.A}')
class S(StrEnum):
    X=auto(); Y='yy'
print(S.X, repr(S.Y), S.X.upper())
class I(IntFlag):
    A=1; B=2
print(repr(I.A|I.B), I.A|I.B, repr(I(8)), I(3))
class Planet(Enum):
    EARTH=(5.97e24, 6.37e6)
    def __init__(self, mass, radius): self.mass=mass; self.radius=radius
print(Planet.EARTH.mass, Planet.EARTH.value)
A=Enum('A','x y z'); print(list(A), A.y.value)
try: C(9)
except ValueError as e: print(e)
try:
    @unique
    class U(Enum):
        a=1; b=1
except ValueError as e: print(e)


class Color(Enum):
    RED = "r"
    GREEN = "g"

    @property
    def pretty(self):
        return self.name.title()

    @classmethod
    def parse(cls, s):
        return cls(s)


print(Color.RED.pretty, Color.parse("g"), Color.RED.value, Color.GREEN.name, isinstance(Color.RED, Color))
print({Color.RED: 1}[Color.RED], Color.RED == Color.RED, Color.RED != Color.GREEN, Color.RED is Color("r"))
print([c.name for c in reversed(Color)], type(Color.RED).__name__, Color.__members__["GREEN"])


class Shape(Enum):
    CIRCLE = auto()
    SQUARE = auto()

    def area(self, s):
        match self:
            case Shape.CIRCLE:
                return 3 * s * s
            case Shape.SQUARE:
                return s * s


print(Shape.CIRCLE.area(2), Shape.SQUARE.area(2))
try:
    Color.RED = 5
except AttributeError as e:
    print("AttributeError", e)


class Status(IntEnum):
    OK = 200
    NOT_FOUND = 404


print(Status.OK < Status.NOT_FOUND, sorted([Status.NOT_FOUND, Status.OK]), Status(404).name, Status.OK + 0)
print(int(Status.OK), Status.OK == 200, "%d" % Status.OK, max(Status))
