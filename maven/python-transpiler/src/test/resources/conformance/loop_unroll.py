# A loop over attribute names the program spells out is unrolled per name,
# per concrete class where the names come from a class tuple; and a method
# with an annotated name parameter is specialized like any other.

FIELDS = ("left", "right")


class Model:
    __attrs__ = ("a", "b")

    def __init__(self):
        self.a = 1
        self.b = None
        self.c = 3
        self.left = "L"
        self.right = None

    def __iter__(self):
        for attr in self.__attrs__:
            value = getattr(self, attr)
            if value is not None:
                yield attr, value

    def skipping(self):
        out = []
        for attr in ("a", "b", "c", "left"):
            if attr == "b":
                continue
            value = getattr(self, attr)
            for ch in str(value):
                if ch == "3":
                    continue
                out.append(ch)
            if value == 1:
                continue
            out.append(attr)
        return out

    def sides(self):
        out = []
        for k in FIELDS:
            v = getattr(self, k, "none")
            out.append((k, v))
        return out

    def copy_into(self, other):
        for k in ["a", "c"]:
            if hasattr(self, k):
                setattr(other, k, getattr(self, k))
        return other

    def clear(self):
        for k in ("left",):
            if hasattr(self, k):
                delattr(self, k)

    def get(self, name: str) -> object:
        return getattr(self, name)


class Wide(Model):
    __attrs__ = Model.__attrs__ + ("c",)


class Wider(Wide):
    pass


class Same(Model):
    pass


class Target:
    def __init__(self):
        self.a = 0
        self.c = 0


print(list(Model()))
print(list(Wide()))
print(list(Wider()))
print(list(Same()))
print(Model().sides())
t = Model().copy_into(Target())
print(t.a, t.c)
m = Model()
m.clear()
print(hasattr(m, "left"))
print(m.get("a"), m.get("c"))
print(Model().skipping())


class Box:
    def __init__(self):
        self.top = 1
        self.left = 2
        self.right = 3

    def total(self):
        # A loop over a function local bound once to a literal list.
        names = ['top', 'left', 'right']
        out = 0
        for name in names:
            out += getattr(self, name)
        return out


print(Box().total())
