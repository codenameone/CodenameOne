import elements
import late
from attrs import ORIGIN, Point, fields
from elements import Node, Tag, Bag, truthy, same, total
from filters import Strainer, names, fill, count, check


class Empty(Node):
    def __len__(self):
        return 0


class Keyed(Node):
    def __eq__(self, other):
        return isinstance(other, Node) and other.name == self.name

    __hash__ = Node.__hash__


class Duck:
    """No Tag: what the guards send to the generic versions."""

    def __init__(self, name, prefix=None):
        self.name = name
        self.prefix = prefix
        self.size = 7

    def append(self, child):
        self.size += 1
        return child


s = Strainer("p")
tags = [Tag("p"), Tag("div"), Tag("p", "svg"), Tag("q", "skip")]
print([s.matches(t) for t in tags])
print([check(s.matches, t) for t in tags])
print(s.matches(Duck("p")), check(s.matches, Duck("q")), s.matches(Duck("x", "svg")))
print(names(tags), names([Duck("a"), Duck("b", "ns")]))
root = Tag("root")
print(fill(root, 3), fill(Duck("d"), 2), total([root, Tag("e"), Duck("f")]))
b = Bag()
print(count(b), check(count, b))
b.items.append(1)
print(count(b), check(count, b))
print(truthy(Tag("x")), truthy(Node("y")), truthy(Empty("z")))
print(same(Keyed("a"), Node("a")), same(Node("a"), Node("a")), same(Keyed("a"), Keyed("b")))
print(elements.OldTag is Tag, elements.Tag is Tag)
late.setup()
print(late.read(), late.value)
print(fields(ORIGIN), fields(Point(1, 2)), ORIGIN.x)
