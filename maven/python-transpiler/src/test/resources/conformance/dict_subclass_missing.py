# dict.__getitem__ reached through super() on a dict subclass still calls
# type(self).__missing__, as CPython's dict lookup does.
from collections import defaultdict


class B(defaultdict):
    def __getitem__(self, key):
        return super().__getitem__(key)


class D:
    pass


b = B(D)
print(type(b["A"]).__name__, sorted(b.keys()))


class M(dict):
    def __missing__(self, key):
        return key * 2

    def __getitem__(self, key):
        return dict.__getitem__(self, key)


m = M(a=1)
print(m["a"], m["zz"], len(m))


class N(dict):
    def __getitem__(self, key):
        return super().__getitem__(key)


n = N(x=1)
try:
    n["y"]
except KeyError as e:
    print("KeyError", e)
print(dict.__getitem__({"k": 5}, "k"))
