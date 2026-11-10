# pprint layouts.
import pprint
from collections import OrderedDict, defaultdict, Counter, deque, ChainMap
from dataclasses import dataclass, field
from types import SimpleNamespace

data = {"name": "codename one", "targets": ["ios", "android", "javascript", "windows", "linux", "mac"],
        "nested": {"alpha": list(range(30)), "beta": {"x": 1.5, "y": (1, 2, 3), "z": None}, "gamma": "a short string"},
        "bytes": b"\x00\x01binary data that goes on and on and on and on and on and on and on", 3: "int key"}
pprint.pprint(data)
pprint.pprint(data, width=40, indent=2)
pprint.pprint(data, depth=1)
pprint.pprint(data, compact=True, width=50)
pprint.pp({"b": 1, "a": 2, "c": [3] * 40})
print(pprint.pformat(list(range(40)), width=30, compact=True))
print(pprint.pformat("this is a long string that will need to be wrapped across several lines of output " * 3, width=40))
print(pprint.pformat(("one",)), pprint.pformat(()), pprint.pformat([]), pprint.pformat({}), pprint.pformat(set()))
print(pprint.pformat({5, 3, 1, "a", (1, 2)}, width=10))
print(pprint.pformat(frozenset(range(25))))
print(pprint.pformat(OrderedDict((str(i), i * 1000) for i in range(12))))
dd = defaultdict(list)
for i in range(12):
    dd["key%d" % i].append(i * 11111)
print(pprint.pformat(dd))
print(pprint.pformat(Counter("the quick brown fox jumps over the lazy dog many many times")))
print(pprint.pformat(deque(range(30), maxlen=40)), pprint.pformat(deque(range(30))))
print(pprint.pformat(ChainMap({"a" * 30: 1}, {"b" * 30: 2}, {"c" * 30: 3})))

@dataclass
class Point:
    x: float
    y: float
    tags: list = field(default_factory=list)
    hidden: int = field(default=0, repr=False)

print(pprint.pformat(Point(1.0, 2.0, ["tag%d" % i for i in range(12)]), width=40))
print(pprint.pformat(SimpleNamespace(alpha="a" * 40, beta=list(range(20)), gamma={"k": "v"}), width=50))
r = [1, 2]
r.append(r)
d = {"self": None}
d["self"] = d
print(pprint.isrecursive(r), pprint.isreadable(r), pprint.isreadable([1, "a", (2,)]), pprint.isreadable(object()))
print(pprint.saferepr(r).split(" with id")[0], pprint.pformat(d).split(" with id")[0])
print(pprint.pformat(123456789, underscore_numbers=True), pprint.pformat([10**20], underscore_numbers=True))
print(pprint.pformat(bytearray(b"abcdefgh" * 12)))
pprint.PrettyPrinter(width=20, indent=4).pprint([["nested", "lists"], ["of", "strings"], {"k": ["v1", "v2"]}])
