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


c = Char(3, "x")


class Text3:
    pass


class Char3(Item, Text3):
    def __init__(self, w):
        Item.__init__(self, w)
        Text3.__init__(self, w * 2)


setattr(Text3, "__init__", lambda self, v: setattr(self, "t3", v))
print(Char3(4).t3)


class Plain:
    def __init__(self, v):
        self.v = v


def same_init(k):
    init = getattr(k, "__init__", None)
    return init is Plain.__init__


print(same_init(Plain), Plain(3).v)


class Text4:
    pass


class Char4(Item, Text4):
    def __init__(self, w):
        Item.__init__(self, w)
        Text4.__init__(self, w + 1)


def _name(*parts):
    return "".join(parts)


setattr(Text4, _name("__in", "it__"), lambda self, v: setattr(self, "t4", v))
print(Char4(5).t4, Char4(6).w)
