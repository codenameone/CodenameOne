# Keyword calls every override binds alike, constant truth tests, and the
# desugared next()/StopIteration loop.


class Filter:
    def __init__(self, tag):
        self.tag = tag

    def match(self, element, _known=False):
        return _known or element == self.tag

    def scale(self, x, factor=2):
        return x * factor

    def run(self, items):
        out = []
        for i in items:
            if self.match(i, _known=False):
                out.append(i)
        return out

    def scaled(self, xs):
        return [self.scale(x, factor=3) for x in xs] + [self.scale(x) for x in xs]


class Strainer(Filter):
    def match(self, element, _known=False):
        return element.startswith(self.tag)

    def scale(self, x, factor=5):
        return x + factor


class Node:
    def __init__(self, name):
        self.name = name

    def __bool__(self):
        return True

    def __len__(self):
        return 0


class Empty(Node):
    def __bool__(self):
        """Never true."""
        return False


class Counted(Node):
    def __bool__(self):
        return len(self.name) > 2


class Late:
    def __bool__(self):
        return True


def truths(nodes):
    return [bool(n) for n in nodes] + ["y" if n else "n" for n in nodes]


def drain(it):
    out = []
    while True:
        try:
            v = next(it)
        except StopIteration:
            break
        if v:
            out.append(v.name)
    return out


def gen(nodes):
    for n in nodes:
        yield n


print(Filter("a").run(["a", "ab", "b"]))
print(Strainer("a").run(["a", "ab", "b"]))
print(Filter("a").scaled([1, 2]))
print(Strainer("a").scaled([1, 2]))
nodes = [Node("x"), Empty("e"), Counted("ab"), Counted("abc")]
print(truths(nodes))
print(drain(gen(nodes)))
print(drain(iter(nodes)))
late = Late()
print(bool(late))
Late.__bool__ = lambda self: False
print(bool(late), "t" if late else "f")
Node.__bool__ = lambda self: self.name == "x"
print(truths(nodes))


def next(it, *rest):
    return Node("patched")


def once():
    try:
        v = next(iter([]))
    except StopIteration:
        return "stopped"
    return v.name


print(once())
