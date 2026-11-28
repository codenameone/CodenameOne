import cobble


class Node(object):
    pass


@cobble.data
class Text(Node):
    value = cobble.field()


@cobble.data
class Element(Node):
    tag_name = cobble.field()
    children = cobble.field()
    attributes = cobble.field(default=None)


@cobble.data
class BoldElement(Element):
    weight = cobble.field(default=None)


@cobble.visitable
class Comment(Node):
    def __init__(self, text):
        self.text = text

    def __repr__(self):
        return "Comment(" + repr(self.text) + ")"


NodeVisitor = cobble.visitor(Node)
ArgVisitor = cobble.visitor(Node, args=1)


class Printer(NodeVisitor):
    def visit_text(self, node):
        return "text:" + node.value

    def visit_element(self, node):
        return "<" + node.tag_name + ">" + "".join(self.visit(c) for c in node.children)

    def visit_bold_element(self, node):
        return "**" + str(node.weight) + self.visit_element(node)

    def visit_comment(self, node):
        return "#" + node.text


class Depth(ArgVisitor):
    def visit_text(self, node, depth):
        return depth

    def visit_element(self, node, depth):
        return max([depth] + [self.visit(c, depth + 1) for c in node.children])

    def visit_bold_element(self, node, depth):
        return self.visit_element(node, depth)

    def visit_comment(self, node, depth):
        return depth


t = Text("hi")
e = Element("p", [t, Comment("c")])
b = BoldElement("b", [e], {"k": "v"}, weight=7)
print(repr(t))
print(repr(e.attributes), repr(b))
print(t == Text("hi"), t == Text("ho"), t != Text("ho"), t == "hi")
print(hash(t) == hash(Text("hi")))
print(Printer().visit(b))
print(Depth().visit(b, 0))
c = cobble.copy(e, tag_name="div")
print(repr(c), c == e, isinstance(c, Element))
print(repr(cobble.copy(b, weight=1)))
try:
    Text()
except TypeError:
    print("missing argument")
try:
    cobble.copy(t, nope=1)
except TypeError:
    print("unexpected keyword")
