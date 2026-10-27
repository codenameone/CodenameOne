from typing import (Any, Optional, Union, List, Dict, Tuple, Callable, TypeVar, Generic, Protocol, NamedTuple,
                    TypedDict, Literal, cast, overload, get_origin, get_args, runtime_checkable, Final, Annotated,
                    Iterator, Sequence, NewType, ClassVar, get_type_hints, TYPE_CHECKING)
from abc import ABC, abstractmethod, ABCMeta
from contextlib import contextmanager, suppress, ExitStack, closing, nullcontext, redirect_stdout
import io

print(Optional[int], Union[int, str], List[int], Dict[str, List[int]], Tuple[int, ...], Callable[[int], str])
print(Union[int, None] == Optional[int], Union[int, int], get_origin(List[int]), get_args(Dict[str, int]))
print(Literal["a", "b"], Annotated[int, "meta"], get_args(Annotated[int, "m"]), TYPE_CHECKING)
T = TypeVar("T")
K = TypeVar("K", bound=str)
print(T, K.__bound__ is str, cast(int, "x"))


class Stack(Generic[T]):
    def __init__(self):
        self.items: List[T] = []

    def push(self, item: T) -> None:
        self.items.append(item)

    def pop(self) -> T:
        return self.items.pop()


s = Stack[int]()
s.push(1)
s.push(2)
print(s.pop(), type(s).__name__, Stack.__orig_bases__ if hasattr(Stack, "__orig_bases__") else None)


class Employee(NamedTuple):
    name: str
    id: int = 3

    def badge(self):
        return self.name + "#" + str(self.id)


e = Employee("ann")
print(e, e.badge(), e._fields, Employee("b", 9).id, isinstance(e, tuple))


class Movie(TypedDict):
    title: str
    year: int


m = Movie(title="x", year=1)
print(m, type(m) is dict, Movie.__required_keys__ == {"title", "year"})


@runtime_checkable
class Closer(Protocol):
    def close(self) -> None:
        ...


class F:
    def close(self):
        print("closed")


print(isinstance(F(), Closer), isinstance(1, Closer))
UserId = NewType("UserId", int)
print(UserId(5), UserId.__name__)


def greet(name: str, times: int = 1) -> str:
    """Says hi."""
    return ("hi " + name) * times


print(greet.__annotations__, greet.__doc__, get_type_hints(greet)["times"])


@overload
def f(x: int) -> int:
    ...


@overload
def f(x: str) -> str:
    ...


def f(x):
    return x


print(f(3))


class Shape(ABC):
    @abstractmethod
    def area(self):
        ...

    def describe(self):
        return type(self).__name__ + " " + str(self.area())


class Sq(Shape):
    def __init__(self, s):
        self.s = s

    def area(self):
        return self.s * self.s


print(Sq(3).describe(), Shape.__abstractmethods__, isinstance(Sq(1), Shape))
try:
    Shape()
except TypeError as e:
    print("TypeError", e)


class Half(Shape):
    pass


try:
    Half()
except TypeError as e:
    print("TypeError", e)


class Plugin(metaclass=ABCMeta):
    pass


Plugin.register(int)
print(isinstance(3, Plugin), issubclass(int, Plugin), issubclass(str, Plugin))


@contextmanager
def tag(name):
    print("<" + name + ">")
    try:
        yield name.upper()
    finally:
        print("</" + name + ">")


with tag("p") as t:
    print("body", t)
try:
    with tag("q"):
        raise ValueError("boom")
except ValueError as e:
    print("caught", e)


@contextmanager
def swallow():
    try:
        yield
    except KeyError:
        print("swallowed")


with swallow():
    raise KeyError("k")
print("after")
with suppress(ZeroDivisionError):
    1 / 0
print("suppressed")
with ExitStack() as stack:
    for n in ("a", "b"):
        stack.callback(print, "exit", n)
    stack.enter_context(tag("x"))
    print("in stack")
with closing(F()) as fobj:
    print("using")
with nullcontext(5) as v:
    print(v)
buf = io.StringIO()
with redirect_stdout(buf):
    print("hidden")
print(repr(buf.getvalue()))


@tag("deco")
def decorated():
    print("inside")


decorated()
