class Element:
    def __init__(self, name):
        self.name = name
        self.parent = None
        self.children = []

    def label(self):
        return "<" + self.kind() + " " + self.name + ">"

    def kind(self):
        return "element"

    def add(self, child):
        child.parent = self
        self.children.append(child)
        return child

    def depth(self):
        d = 0
        p = self.parent
        while p is not None:
            d += 1
            p = p.parent
        return d

    def total(self):
        n = self.weight()
        for c in self.children:
            n += c.total()
        return n

    def weight(self):
        return 1


class Leaf(Element):
    def kind(self):
        return "leaf"
