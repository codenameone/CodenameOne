# A classmethod called through self does not make the instance escape:
# its fields keep their types.
from collections import defaultdict
from typing import List, Optional, cast


class Rule:
    def __init__(self, string=None, present=None):
        self.string = string
        self.present = present

    def matches(self, s):
        return self.string == s


class Strainer:
    name_rules: List[Rule]

    def __init__(self, name=None, attrs=None, string=None, **kwargs):
        if name is None and not attrs and not string and not kwargs:
            self.name_rules = [Rule(present=True)]
        else:
            self.name_rules = cast(List[Rule], list(self._make(name, Rule)))
        self.attribute_rules = defaultdict(list)

    @classmethod
    def _make(cls, obj, rule_class):
        if obj is None:
            return
        if isinstance(obj, str):
            yield rule_class(string=obj)
        else:
            for o in obj:
                yield rule_class(string=o)

    def match(self, s):
        for r in self.name_rules:
            if r.matches(s):
                return True
        return False


print(Strainer("a").match("a"), Strainer(["b", "c"]).match("c"), Strainer().match("x"))
