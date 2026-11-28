from typing import Protocol, runtime_checkable, SupportsInt


@runtime_checkable
class Closer(Protocol):
    def close(self) -> None: ...


@runtime_checkable
class Named(Protocol):
    name: str


class NotRt(Protocol):
    def go(self) -> None: ...


@runtime_checkable
class Both(Closer, Protocol):
    def open(self) -> None: ...


class File:
    name = "f"

    def close(self):
        pass

    def open(self):
        pass


class Nope:
    close = None


class Impl(Closer):
    def close(self):
        pass


print(isinstance(File(), Closer), isinstance(Nope(), Closer), isinstance(3, Closer))
print(isinstance(File(), Named), isinstance(Nope(), Named))
print(isinstance(File(), Both), isinstance(Impl(), Both), isinstance(Impl(), Closer))
print(issubclass(File, Closer), issubclass(int, Closer), issubclass(File, Both))
print(isinstance(3, SupportsInt), isinstance("x", SupportsInt))
try:
    isinstance(File(), NotRt)
except TypeError as e:
    print("TypeError", e)
try:
    issubclass(File, Named)
except TypeError as e:
    print("TypeError", e)


def pick(x):
    if isinstance(x, Closer):
        return "closer"
    return "other"


print(pick(File()), pick(1), pick(Impl()))
