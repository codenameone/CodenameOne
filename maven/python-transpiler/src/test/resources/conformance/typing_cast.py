# typing.cast answers its value, typed as the value is.
import typing
from typing import List, cast


class Rule:
    def __init__(self, s):
        self.s = s


class Strainer:
    def __init__(self, names):
        self.rules = cast(List[Rule], [Rule(n) for n in names])
        self.count = cast(int, len(names))
        self.label = typing.cast("str", "x" * self.count)

    def match(self, s):
        for r in self.rules:
            if r.s == s:
                return True
        return False


def side(v):
    return cast(object, v)


st = Strainer(["a", "b"])
print(st.match("b"), st.match("z"), st.count + 1, st.label, side(None), side([1]))
print(cast(str, 5) + 1)
