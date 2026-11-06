class Guarded:
    def __init__(self):
        self._v = 1

    @property
    def value(self):
        return self._v

    def __setattr__(self, name, val):
        print("setattr", name, val)
        if name == "value":
            object.__setattr__(self, "_v", val * 10)
            return
        object.__setattr__(self, name, val)


g = Guarded()
g.value = 4
print(g.value, g._v)


class Plain:
    @property
    def ro(self):
        return 7


p = Plain()
try:
    p.ro = 3
except AttributeError:
    print("read-only")
try:
    object.__setattr__(p, "ro", 3)
except AttributeError:
    print("still read-only")
print(p.ro)


class WithSetter:
    def __init__(self):
        self.log = []

    @property
    def x(self):
        return 0

    @x.setter
    def x(self, v):
        self.log.append(v)

    def __setattr__(self, name, val):
        if name == "x":
            val = val + 100
        object.__setattr__(self, name, val)


w = WithSetter()
w.x = 1
print(w.log)
