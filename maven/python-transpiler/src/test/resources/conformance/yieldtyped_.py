# Iterating a generator method or a generator-expression property: the loop
# variable is typed by what the body yields, and checked as it arrives.


class Node:
    def __init__(self, name, kids=None):
        self.name = name
        self.kids = kids or []
        self.weight = len(name)

    @property
    def children(self):
        return (k for k in self.kids)

    def walk(self):
        yield self
        for k in self.kids:
            yield from k.walk()

    def leaves(self):
        for k in self.kids:
            if not k.kids:
                yield k

    def total(self):
        t = 0
        for c in self.children:
            t += c.weight
        for c in self.leaves():
            t += c.weight * 10
        return t

    def names(self):
        return [n.name for n in self.walk()]


class Odd(Node):
    def leaves(self):
        yield "not a node"


def describe(n):
    out = []
    for c in n.children:
        out.append(c.name.upper())
    return out


tree = Node("root", [Node("ab"), Node("cde", [Node("f")]), Node("gh")])
print(tree.total())
print(tree.names())
print(describe(tree))
odd = Odd("odd", [Node("x")])
try:
    print(odd.total())
except AttributeError as e:
    print("AttributeError", "weight" in str(e))
print(describe(odd))
