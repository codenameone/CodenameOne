"""Classes other modules walk, in the shape of bs4.element: a module
`__getattr__` that looks a deprecated name up in `globals()`, which reads
the namespace but binds nothing in it, so every class here stays a class
another module can name."""

_deprecated_names = {"OldTag": "Tag"}


def __getattr__(name):
    if name in _deprecated_names:
        return globals()[_deprecated_names[name]]
    if name in globals():
        return None
    raise AttributeError(name)


class Node:
    def __init__(self, name):
        self.name = name
        self.prefix = None
        self.size = 0

    def label(self):
        return self.name


class Tag(Node):
    def __init__(self, name, prefix=None):
        Node.__init__(self, name)
        self.prefix = prefix
        self.children = []

    def __bool__(self):
        return True

    def append(self, child):
        self.children.append(child)
        self.size += 1
        return child


class Bag:
    def __init__(self):
        self.items = []

    def __len__(self):
        return len(self.items)


def truthy(n: Node):
    # A subclass another module declares answers its own truth.
    return "yes" if n else "no"


def same(a: Node, b: Node):
    # A subclass another module declares compares itself.
    return a == b


def total(tags):
    n = 0
    for t in tags:
        n += t.size
    return n
