from base import Element, Leaf
import base


class Document(Element):
    """A subclass in another module: its own fields, overrides called from
    the base module's compiled methods, super() and the inherited fields."""

    def __init__(self, name, builder):
        super().__init__(name)
        self.builder = builder
        self.stack = [self]
        self.current = self
        self.count = 0

    def kind(self):
        return "document"

    def weight(self):
        return 10

    def push(self, name):
        e = self.current.add(Element(name))
        self.stack.append(e)
        self.current = e
        self.count += 1
        return e

    def pop(self):
        self.stack.pop()
        self.current = self.stack[-1]


class Fragment(Document):
    def kind(self):
        return "fragment:" + super().kind()


class Odd(base.Element):
    @property
    def label(self):
        return "odd " + self.name


class Shadow(Leaf):
    kind = None


d = Document("root", "html")
d.push("body")
d.push("p")
d.current.add(Leaf("text"))
d.pop()
d.push("div")
print(d.label(), d.count, len(d.stack), d.current.name, d.current.depth())
print(d.total(), d.children[0].total(), d.builder)
print([c.label() for c in d.children[0].children])
f = Fragment("frag", "xml")
f.push("x")
print(f.label(), f.total(), f.current.label(), isinstance(f, Element), isinstance(f, Document))
o = Odd("o")
print(o.label, o.depth(), o.total())
s = Shadow("s")
print(s.kind, s.name)
state = dict(d.__dict__)
g = Document.__new__(Document)
g.__dict__ = state
print(g.name, g.builder, g.count, g.current.name, g.label())
g.extra = 5
print(sorted(k for k in vars(g) if k != "children" and k != "stack"), g.extra)
del g.builder
print(hasattr(g, "builder"), getattr(g, "builder", "gone"))
items = [d, f, Leaf("l"), Element("e")]
print([x.kind() for x in items], [x.weight() for x in items])
print(sum(x.total() for x in items))
