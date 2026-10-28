import copy, pickle, copyreg

class P:
    def __init__(self, a, b):
        self.a = a
        self.b = b
    def __repr__(self):
        return "P(%r, %r)" % (self.a, self.b)

p = P(1, [2, 3])
r = p.__reduce_ex__(2)
print(r[0] is copyreg.__newobj__, r[1][0] is P, r[2])
print(p.__getstate__())
print(type(p.__reduce__()) is tuple)
q = copyreg.__newobj__(P)
print(type(q).__name__, q.__dict__)

c = copy.copy(p)
print(c, c.b is p.b)
d = copy.deepcopy(p)
print(d, d.b is p.b)
print(pickle.loads(pickle.dumps(p)))

class R:
    def __init__(self, v):
        self.v = v
    def __reduce__(self):
        print("reduce called")
        return (R, (self.v * 10,))
print(copy.copy(R(1)).v, copy.deepcopy(R(2)).v, pickle.loads(pickle.dumps(R(3))).v)

class S:
    def __init__(self):
        self.v = 1
        self.cache = "big"
    def __getstate__(self):
        st = dict(self.__dict__)
        del st['cache']
        return st
    def __setstate__(self, st):
        print("setstate", sorted(st))
        self.__dict__.update(st)
        self.cache = "rebuilt"
for f in (copy.copy, copy.deepcopy, lambda o: pickle.loads(pickle.dumps(o))):
    o = f(S())
    print(o.v, o.cache)

class Sl:
    __slots__ = ('x', 'y')
    def __init__(self):
        self.x = 1
        self.y = [1]
print(Sl().__getstate__())
c = copy.copy(Sl()); print(c.x, c.y)
c = copy.deepcopy(Sl()); print(c.x, c.y)
c = pickle.loads(pickle.dumps(Sl())); print(c.x, c.y)

class N:
    def __new__(cls, n):
        print("new", n)
        o = object.__new__(cls)
        o.n = n
        return o
    def __getnewargs__(self):
        return (self.n,)
c = copy.copy(N(4)); print(c.n)
c = pickle.loads(pickle.dumps(N(5))); print(c.n)

class NK:
    def __new__(cls, *, k):
        print("newkw", k)
        o = object.__new__(cls)
        o.k = k
        return o
    def __getnewargs_ex__(self):
        return ((), {'k': self.k})
c = copy.deepcopy(NK(k=7)); print(c.k)

class Node:
    def __init__(self, name):
        self.name = name
        self.peer = None
a = Node('a'); b = Node('b'); a.peer = b; b.peer = a
x = copy.deepcopy(a)
print(x.name, x.peer.name, x.peer.peer is x, x is not a)
y = pickle.loads(pickle.dumps(a))
print(y.peer.peer is y)
lst = [a, a]
z = copy.deepcopy(lst)
print(z[0] is z[1])

class St:
    def __init__(self):
        self.v = 3
    def __getstate__(self):
        return (self.v,)
    def __setstate__(self, s):
        self.v = s[0] + 100
print(copy.copy(St()).v, pickle.loads(pickle.dumps(St())).v)
