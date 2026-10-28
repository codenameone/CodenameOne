from collections import namedtuple
import typing
P = namedtuple("P", "x y")
print(P.__module__, P(1, 2))
Q = namedtuple("Q", "a", module="mine")
print(Q.__module__)
class N(typing.NamedTuple):
    a: int
print(N.__module__, N(3))
from urllib.parse import urlsplit
print(type(urlsplit("http://a/b")).__module__)
