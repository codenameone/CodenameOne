# Guessed field, local and return types: a value of another type stored
# where the compiler guessed one must read back exactly, through typed
# expressions over it, truth tests, arithmetic and method calls.


class Box:
    def __init__(self):
        self.label = "start"
        self.weight = 1.5
        self.flag = True
        self.items = [1, 2]
        self.child = None


class Node:
    def __init__(self, name):
        self.name = name


def fill(obj, attrs):
    for k, v in attrs.items():
        setattr(obj, k, v)


def describe(b):
    out = []
    out.append(b.label.upper() if isinstance(b.label, str) else repr(b.label))
    out.append(b.label + b.label)
    out.append(b.weight * 2 + 1)
    out.append("yes" if b.flag else "no")
    out.append(len(b.items) + 1)
    out.append(b.items[0])
    if b.weight > 1:
        out.append("heavy")
    return out


def pick(n):
    if n == 0:
        return "zero"
    r = getattr(Box(), "label")
    return r if n == 1 else n


def total(xs):
    acc = xs[0]
    for x in xs[1:]:
        acc = acc + x
    return acc


b = Box()
print(describe(b))
fill(b, {"label": "dyn", "weight": 3.0, "flag": False})
print(describe(b))
fill(b, {"label": 7, "weight": 4, "flag": [], "items": (9, 8, 7)})
print(describe(b))
fill(b, {"label": ["a"], "weight": True, "flag": "x", "items": "abc"})
print(describe(b))
for i in range(3):
    v = pick(i)
    print(v, v * 2)
print(total([1, 2, 3]), total(["a", "b"]), total([[1], [2]]))
n = Node("n1")
setattr(n, "name", 42)
print(n.name + 1)
setattr(n, "name", "back")
print(n.name.title(), len(n.name))
words = "a bb ccc".split()
for w in words:
    w = w * 2
    print(w)


class Fallback:
    def tag(self):
        return "fb"


class Real:
    def tag(self):
        return "real"


class Holder:
    def __init__(self):
        self.first = Fallback()
        self.all = [Fallback()]


def walk(h):
    x = h.first
    out = [x.tag()]
    for x in getattr(h, "all"):
        out.append(x.tag())
    for x in h.all:
        out.append(x.tag())
    out.append(x.tag())
    return out


h = Holder()
print(walk(h))
fill(h, {"first": Real(), "all": [Real(), Fallback()]})
print(walk(h))


class Doc:
    def __init__(self):
        self.xrefs = []
        self.xrefs.append(Fallback())

    def names(self):
        out = []
        for xref in self.xrefs:
            out.append(xref.tag())
        return out


def add_any(d, v):
    getattr(d, "xrefs").append(v)


d = Doc()
print(d.names())
add_any(d, Real())
print(d.names())
