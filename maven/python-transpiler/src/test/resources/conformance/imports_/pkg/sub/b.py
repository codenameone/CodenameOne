from .. import a
from ..a import twice
from . import __name__ as subname
from .. import VALUE
NAME = "b in " + subname
def combined():
    return twice(VALUE) + a.counter
