from base import Parser, peers


class Detects:
    def detect(self):
        return "detect:" + str(self.count)


class Mixed(Parser, Detects):
    def __init__(self):
        Parser.__init__(self)
        self.extra = 1

    def tag(self):
        return 42

    def label(self):
        return "mixed"

    @property
    def raw(self):
        return self.__dict__.get("_raw", "")

    @raw.setter
    def raw(self, v):
        self.__dict__["_raw"] = v.upper()


class Plain(Parser):
    pass


p = Parser()
p.feed("abc>def")
print(p.count, p.last, p.owner is p, p.label())
m = Mixed()
m.feed("xy>z")
print(m.count, m.last, type(m.owner).__name__, m.detect(), m.raw)
q = Parser()
q.feed("hello>")
print(peers(q, m), q.last, q.count)
print(peers(m, p), m.last, m.count)
w = Plain()
w.feed("ab>")
print(peers(p, w), p.last, p.count, isinstance(m, Parser), isinstance(m, Detects))
items = [p, m, w, q]
print([x.label() for x in items], [x.count for x in items])
q.owner = m
print(type(q.owner).__name__, q.owner.label())
