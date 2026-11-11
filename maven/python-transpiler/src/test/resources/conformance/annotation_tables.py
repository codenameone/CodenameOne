# Annotations of functions and of class bodies, read back: names, attributes,
# subscripts, unions, tuples and lists of them, constants, and a call.
import collections.abc
import typing
from typing import Callable, Optional


class Point:
    x: int
    y: "Point"
    tags: list[str]
    table: dict[str, tuple[int, ...]]
    maybe: Optional[int] = None
    either: int | None = None
    fn: Callable[[int, str], bool] = None
    seq: collections.abc.Sequence[int] = ()
    lit: typing.Literal["a", "b"] = "a"
    flag: typing.Annotated[int, str("meta")] = 0


def plain(a: int, b: str = "", *rest: bytes, key: float = 0.0, **more: bool) -> None:
    return None


def shaped(a: list[int], b: dict[str, Point], c: int | str | None, d: typing.Any, e: "Point") -> tuple[int, str]:
    return (1, "")


def called(a: typing.Annotated[int, str("x")], b: Callable[..., int] = None) -> Optional[Point]:
    return None


def none():
    pass


PLAIN = (int, str, bytes, float, bool, Point)


def show(d):
    for k, v in d.items():
        if isinstance(v, str):
            print(" ", k, "= str", v)
        elif v is None:
            print(" ", k, "= None")
        elif any(v is p for p in PLAIN):
            print(" ", k, "=", v.__name__)
        else:
            print(" ", k, "= alias", typing.get_origin(v) is not None, len(typing.get_args(v)))


for owner in (Point, plain, shaped, called, none):
    print(owner.__name__, len(owner.__annotations__))
    show(owner.__annotations__)
print(plain.__annotations__ is plain.__annotations__)
print(shaped.__annotations__["b"] == dict[str, Point], shaped.__annotations__["c"] == (int | str | None))
print(Point.__annotations__["tags"] == list[str], typing.get_args(Point.__annotations__["fn"]))
print(Point.__annotations__["maybe"] == Optional[int], Point.__annotations__["either"] == (int | None))
print(typing.get_args(Point.__annotations__["lit"]), typing.get_args(called.__annotations__["a"]))
print(typing.get_args(Point.__annotations__["table"]), shaped.__annotations__["d"] is typing.Any)
