# Plain objects: construction, attribute reads and writes, method calls,
# properties and isinstance -- the shape of python-pptx's and openpyxl's
# element proxies and of bs4's Tag.
from _harness import measure


class Node:
    def __init__(self, name, parent=None):
        self.name = name
        self.parent = parent
        self.children = []
        self.attrs = {}

    def append(self, child):
        self.children.append(child)
        child.parent = self
        return child

    @property
    def depth(self):
        d = 0
        p = self.parent
        while p is not None:
            d += 1
            p = p.parent
        return d


class Text(Node):
    def __init__(self, value, parent=None):
        Node.__init__(self, "#text", parent)
        self.value = value


def work(n):
    root = Node("root")
    cur = root
    total = 0
    for i in range(n):
        if i % 8 == 0:
            cur = root.append(Node("p"))
        t = cur.append(Text("w%d" % (i % 10)))
        t.attrs["i"] = i
        total += t.depth + len(t.value)
        if isinstance(t, Text) and t.parent is cur:
            total += 1
    return "%d %d" % (total, len(root.children))


measure("objects", work, 100000, 5)
