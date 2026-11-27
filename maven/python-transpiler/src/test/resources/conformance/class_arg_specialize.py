# A field every construction sets to one literal attribute name is folded
# to it, so the setattr/getattr that use it name the attribute literally.
from collections import defaultdict


class Bound(defaultdict):
    def __init__(self, reference=None, *args, **kw):
        self.reference = reference
        super().__init__(*args, **kw)

    def __getitem__(self, key):
        value = super().__getitem__(key)
        if self.reference is not None:
            setattr(value, self.reference, key)
        return value

    def peek(self, key):
        return getattr(super().__getitem__(key), self.reference)


class Dim:
    def __init__(self):
        self.index = None


class Holder(Bound):
    def __init__(self, owner, reference="index", default_factory=None):
        self.owner = owner
        super().__init__(reference, default_factory)


h = Holder(owner="w", default_factory=Dim)
print(h["A"].index, h["B"].index, h.peek("A"), h.reference)
print(isinstance(h, Bound), isinstance(h, defaultdict), sorted(h.keys()))
g = Holder("x", default_factory=Dim)
print(g[3].index, len(g), g.owner)
