import importlib
import typing
from typing import TYPE_CHECKING
import lp

if TYPE_CHECKING:
    import lp.unused
if typing.TYPE_CHECKING:
    from lp import unused


def lazy():
    # Imported only inside a function that runs.
    from lp import chain
    return chain.top()


print(lazy())
print(lp.backend("fast").name)
print(importlib.import_module(".plugins.alpha", "lp").ALPHA)
mod, _, attr = "lp.plugins.beta:VALUE".partition(":")
print(getattr(importlib.import_module(mod), attr))
from lp.star import *
print(starred())
print(lp.__getattr__("late"))


def never():
    import lp.unused
    return lp.unused
