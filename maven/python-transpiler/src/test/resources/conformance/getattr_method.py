class Base:
    name = "base"

    def common(self):
        return "common"


class Child(Base):
    name = "child"

    def special(self, x):
        return "special:" + str(x)


class GrandChild(Child):
    pass


class Holder:
    def __init__(self, d):
        self.dialect = d


class Ctx:
    def __init__(self):
        self.dialect = Base()

    def use(self, d):
        self.dialect = d
        return self


def compile_with(ctx):
    fn = getattr(ctx.dialect, "special", None)
    return fn("x") if fn is not None else "fallback"


for obj in (Base(), Child(), GrandChild()):
    m = getattr(obj, "special", None)
    print(type(obj).__name__, m is None, m(1) if m is not None else "-")
    c = getattr(obj, "common", None)
    print(c is None, c())
    print(getattr(obj, "missing", "dflt"), hasattr(obj, "special"))

h = Holder(Child())
f = getattr(h.dialect, "special", None)
print(f is None, f(2) if f is not None else "-")
h.dialect = GrandChild()
f = getattr(h.dialect, "special", None)
print(f is None, f(3) if f is not None else "-")
print(getattr(Child, "special", None) is None)
ctx = Ctx()
print(compile_with(ctx))
print(compile_with(ctx.use(Child())))
print(compile_with(ctx.use(GrandChild())))
