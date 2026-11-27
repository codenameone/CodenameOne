class P:
    def __init__(self, a, b=2, *, c=3, d="x"):
        self.v = (a, b, c, d)


class Q(P):
    pass


class PosOnly:
    def __init__(self, a, /, b=1):
        self.v = (a, b)


class BadInit:
    def __init__(self):
        return 5


def order(tag, val):
    print("eval", tag)
    return val


print(P(1).v)
print(P(1, 5).v)
print(P(1, d="y").v)
print(P(a=7, c=9).v)
print(Q(order("a", 1), c=order("c", 4)).v)
print(PosOnly(1, b=2).v)
try:
    PosOnly(a=1)
except TypeError:
    print("posonly kw rejected")
try:
    P(1, 2, 3)
except TypeError:
    print("too many rejected")
try:
    P(1, e=2)
except TypeError:
    print("unknown kw rejected")
try:
    BadInit()
except TypeError:
    print("non-None init rejected")


def make():
    return P(4, c=8).v


print(make())
P.__init__ = lambda self, a, b=0, *, c=0, d=0: setattr(self, "v", ("new", a, c))
print(make())
Q2 = Q
print(Q2(3).v)
