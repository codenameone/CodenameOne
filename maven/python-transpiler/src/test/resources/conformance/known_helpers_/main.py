import warnings

from bs4 import Soup
from bs4.element import Node

a = Node("a")
b = Node("b")
s = Soup("root", [a, b, Node("a")])
with warnings.catch_warnings(record=True) as caught:
    warnings.simplefilter("always")
    a.nextSibling = b
    print(a.next_sibling.name, a.nextSibling.name)
    print(len(s.findAll("a")), len(s.findAll("a", limit=1)), len(s.findAll(name="b")))
    print(s.findChild("b").name, len(s.fetch("a")), s.findChild("z"))
for w in caught:
    print(w.category.__name__, w.message)
print(Node.fetch.__name__, Soup.findChild.__name__)
print(isinstance(Node.nextSibling, property))
