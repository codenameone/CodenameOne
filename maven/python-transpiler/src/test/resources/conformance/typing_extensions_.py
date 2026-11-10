import typing_extensions as te
from typing_extensions import (Self, Never, override, TypeAlias, ParamSpec, Concatenate,
    TypeGuard, Required, NotRequired, Unpack, LiteralString, deprecated, Doc,
    Annotated, Literal, Protocol, TypedDict, TypeVar, Optional, List, Dict,
    Final, final, get_args, get_origin, cast, overload, NamedTuple, runtime_checkable)
import warnings

print(te.TypeVar is TypeVar, te.List is List)


class Base:
    def f(self) -> int:
        return 1


class Child(Base):
    @override
    def f(self) -> int:
        return 2


print(Child().f())


def is_str(x: object) -> TypeGuard[str]:
    return isinstance(x, str)


print(is_str("a"), is_str(1))


class Movie(TypedDict):
    title: str
    year: NotRequired[int]


m: Movie = {"title": "x"}
print(m)

A = Annotated[int, Doc("count of things")]
print(get_args(A)[0], get_args(A)[1].documentation, get_args(A)[1])
print(Doc("a") == Doc("a"), Doc("a") == Doc("b"))


@deprecated("use g2")
def g():
    return 5


with warnings.catch_warnings(record=True) as w:
    warnings.simplefilter("always")
    print(g())
    print(len(w), w[0].category.__name__, str(w[0].message))
print(g.__deprecated__)


@deprecated("old class", category=None)
class Old:
    pass


print(Old.__deprecated__)

print(get_origin(List[int]) is list, get_args(Literal[1, 2]))
P = ParamSpec("P")
print(P.__name__)
T = TypeVar("T")
print(T.__name__)
print(cast(int, "x"))


@runtime_checkable
class HasLen(Protocol):
    def __len__(self) -> int: ...


print(isinstance([1], HasLen), isinstance(3, HasLen))
