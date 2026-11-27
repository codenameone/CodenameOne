# Two compiled bases that both bind a method a super() call names, and an
# extra base that binds __init__: every super() follows the C3 MRO of the
# instance's class, and construction runs the MRO's __init__.


class Descriptor:
    def __init__(self, name=None, **kw):
        self.name = name
        self.log = []

    def put(self, value):
        self.log.append("Descriptor")
        self.value = value


class Typed(Descriptor):
    def put(self, value):
        self.log.append("Typed")
        super().put(value)


class Convertible(Typed):
    def put(self, value):
        self.log.append("Convertible")
        super().put(int(value))


class Nested(Descriptor):
    def put(self, value):
        self.log.append("Nested")
        super().put(value)


class NestedValue(Nested, Convertible):
    pass


class Choice(Descriptor):
    def __init__(self, name=None, values=(), **kw):
        self.values = list(values)
        super().__init__(name, **kw)


class NestedChoice(Nested, Choice):
    pass


n = NestedValue("a")
n.put("41")
print(n.log, n.value, n.name)
print([c.__name__ for c in NestedValue.__mro__])
p = Nested("b")
p.put("x")
print(p.log, p.value)
c = NestedChoice("c", values=(1, 2))
c.put(3)
print(c.values, c.name, c.log, c.value)
print(isinstance(c, Choice), isinstance(n, Convertible), isinstance(n, Nested))
