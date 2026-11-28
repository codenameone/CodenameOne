# Slice stores and augmented subscripts on containers of unknown type.


class Box:
    def __init__(self):
        self.d = {}

    def __getitem__(self, k):
        return self.d.get(k, 0)

    def __setitem__(self, k, v):
        self.d[k] = v


def fill(target, src, k):
    n = 0
    for i in range(3):
        n += i
    target[1:3] = src
    target[k] += n
    target[k] *= 2
    return target


def grow(d, k, items):
    for i in range(2):
        d[k] += items
    return d


b = Box()
b[0] = 1
print(fill([1, 2, 3, 4], ["a"], 0), fill([0, 0, 0], (7, 8), 2))
print(sorted(fill(b, None, "x").d.items(), key=str) if False else b[0])
shared = [1]
d = {"k": shared}
print(grow(d, "k", [2]), shared is d["k"])
print(grow({"k": "s"}, "k", "t"))
