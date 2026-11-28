from base import Descriptor, Typed, make, NS


class Bool(Typed):
    """Rebinds base fields as class-body defaults, in another module."""
    expected_type = bool
    allow_none = True


class Count(Typed):
    expected_type = int
    pattern = "x"


b = Bool("b")
print(b.show(), b.check(None), b.check(True), b.check(3))
b2 = Bool("b2", allow_none=False, expected_type=int)
print(b2.show(), b2.check(None), b2.check(3))
c = Count("c")
print(c.show(), c.check(None), c.check(4), c.pattern)
c2 = Count("c2", allow_none=True)
print(c2.show(), c2.check(None))
t = make("t", expected_type=str)
print(t.show(), t.check("s"), t.check(None))
print(Typed("u").show(), Descriptor("d", expected_type=float).show())
Bool.allow_none = False
print(b.check(None), b2.allow_none)


class Nested(Typed):
    min = -5

    def to_tree(self, v):
        return "tree:%s" % v

    def emit(self, v):
        return self.to_tree(v)


def plain(v):
    return "plain:%s" % v


n = Nested("n")
n2 = Nested("n2", to_tree=plain, min=2)
print(n.emit(1), n2.emit(2), n.min, n2.min, n.to_tree(3), n2.to_tree(4))


class Rel(Typed):
    """Binds a base field to a constant global imported from another module."""
    expected_type = str
    namespace = NS


r = Rel("r")
r2 = Rel("r2", namespace="o")
print(r.namespace, r2.namespace, Typed("v").namespace, r.check("s"))
