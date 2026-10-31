from typing import Annotated, get_type_hints
import sys

class F:
    def __init__(self, **kw):
        self.kw = kw
    def __repr__(self):
        return "F(%r)" % sorted(self.kw.items())

def conint(ge):
    return Annotated[int, F(ge=ge)]

class M:
    a: Annotated[int, F(gt=0)]
    b: conint(ge=2)
    c: Annotated[int, (lambda v: v * 2)]
    d: Annotated[str, F(pattern="x", n=-1)]

h = M.__annotations__
print(h["a"], h["b"], h["d"])
print(h["c"].__metadata__[0](21))
def f(x: conint(3)) -> Annotated[int, F()]:
    return x
print(f.__annotations__)
print(type(sys.modules[__name__].__dict__).__name__, "M" in sys.modules[__name__].__dict__)
print(f.__code__.co_argcount, f.__code__.co_varnames[:1], f.__defaults__)
import inspect
print(inspect.signature(f), list(inspect.signature(f).parameters))
class H: pass
print(H.__hash__ is object.__hash__, hash(H()) == object.__hash__(H()) if False else True)
T = type("T", (), {})
print(T, T.__module__)
class Fz:
    def __delattr__(self, name):
        raise AttributeError("frozen " + name)
z = Fz(); z.q = 1
try:
    del z.q
except AttributeError as e:
    print(e)
try:
    delattr(z, "q")
except AttributeError as e:
    print(e)


def _gen():
    try:
        yield 1
    finally:
        pass


async def _coro():
    return 1


async def _agen():
    yield 1


for _f in (_gen, _coro, _agen, len, lambda: 0):
    print(inspect.isgeneratorfunction(_f), inspect.iscoroutinefunction(_f),
          inspect.isasyncgenfunction(_f) if hasattr(inspect, "isasyncgenfunction") else None)
