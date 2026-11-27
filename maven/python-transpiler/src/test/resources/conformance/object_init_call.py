class Text:
    def get_text(self):
        return "t"


class Item:
    def __init__(self, w):
        self.w = w


class Char(Item, Text):
    def __init__(self, w, text):
        Item.__init__(self, w)
        Text.__init__(self)
        self.text = text


class Other:
    def __init__(self, a=1, b=2):
        self.a = a
        self.b = b


c = Char(3, "x")
print(c.w, c.text, c.get_text())
print(Other(5).a, Other(b=7).b)
f = object.__init__
print(f(c), str.join("-", ["a", "b"]))


class Text2:
    pass


class Text3:
    pass


class Char2(Item, Text2):
    def __init__(self, w):
        Item.__init__(self, w)
        Text2.__init__(self, "x", k=1)


class Char3(Item, Text3):
    def __init__(self, w):
        Item.__init__(self, w)
        Text3.__init__(self, w * 2)


def _t2_init(self, a, k=0):
    self.t2 = (a, k)


Text2.__init__ = _t2_init
setattr(Text3, "__init__", lambda self, v: setattr(self, "t3", v))
print(Char2(1).t2, Char3(4).t3)


class Plain:
    def __init__(self, v):
        self.v = v


class Child(Plain):
    pass


def same_init(k):
    init = getattr(k, "__init__", None)
    return init is Plain.__init__, init is not Plain.__init__


print("identity", same_init(Plain), same_init(Child), Plain(3).v)
