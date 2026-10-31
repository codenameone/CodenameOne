class P:
    def __init__(self, x):
        self.x = x

    def m(self):
        return "m" + str(self.x)


def read(o):
    return o.x


def call(o):
    return o.m()


ps = [P(1), P(2)]
print([read(p) for p in ps], [call(p) for p in ps])
q = P(3)
q.y = 9
print(read(q), call(q), q.y)
q.m = lambda: "shadowed"
print(call(q), call(ps[0]))
del q.m
print(call(q))
P.m = lambda self: "replaced" + str(self.x)
print(call(ps[0]), call(q))
del q.x
try:
    read(q)
except AttributeError as e:
    print("AttributeError", e)
q.x = 5
print(read(q), sorted(vars(q).items()) if False else q.__dict__)


class Q:
    pass


r = Q()
r.x = "q"
print(read(r))
P.x = property(lambda self: "prop")
print(read(ps[0]), read(ps[1]))
del P.x
print(read(ps[0]))


class Base:
    def who(self):
        return "base"


class Kid(Base):
    pass


k = Kid()
print(call_who := k.who())
Base.who = lambda self: "base2"
print(k.who())
Kid.who = lambda self: "kid"
print(k.who(), Base().who())


class Counter:
    def __init__(self):
        self.n = 0

    def bump(self, by):
        self.n += by
        return self


c = Counter()
for i in range(5):
    c.bump(i).bump(1)
print(c.n)
print([1, 2].index(2), "a,b".split(","), {"k": 1}.get("k"))
