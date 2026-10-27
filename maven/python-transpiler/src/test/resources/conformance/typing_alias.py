import typing
from typing import Optional, Union, Literal, Annotated, Any, get_origin, get_args
import datetime, enum

class Color(enum.Enum):
    RED = "red"

class M:
    a: int
    b: list[int]
    c: dict[str, float]
    d: Optional[str] = None
    e: Union[int, str] = 1
    f: Literal["x", "y"] = "x"
    g: Annotated[int, "meta"] = 0
    h: "M | None" = None
    i: int | None = None
    j: tuple[int, ...] = ()
    k: datetime.date = None
    l: Color = Color.RED
    m: set[str] = set()
    n: Any = None
    o: list["M"] = []

for k, v in M.__annotations__.items():
    print(k, v, get_origin(v), get_args(v))
print(typing.get_type_hints(M)["h"])
print(M.__dict__.get("d"), "e" in vars(M), M.__mro__[0].__name__)
