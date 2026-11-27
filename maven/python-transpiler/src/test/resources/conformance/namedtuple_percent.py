from collections import namedtuple
from urllib.parse import urlsplit

P = namedtuple('Point', 'x y')

class Q(namedtuple('Pair', ['a', 'b'])):
    def show(self):
        return "<%s|%s>" % self

p = P(1, 'two')
print("%s and %r" % p)
print(repr(p))
q = Q(3, 4)
print(q.show())
print(repr(q))
print(Q.__bases__[0].__name__, Q.__bases__[0].__qualname__)
print(type(urlsplit('http://a/b')).__bases__[0].__name__)
print(type(urlsplit('http://a/b')).__bases__[0].__qualname__)
print("%d-%d" % (5, 6))
