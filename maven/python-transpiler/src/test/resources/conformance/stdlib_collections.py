from collections import deque, defaultdict, Counter, OrderedDict, namedtuple, ChainMap
import collections.abc as cabc
import copy
import string

d = deque([1, 2, 3], maxlen=4)
d.append(4)
d.append(5)
d.appendleft(0)
print(d, len(d), d[0], d[-1], d.maxlen)
d.rotate(1)
print(d, d.pop(), d.popleft(), list(reversed(d)), 3 in d)
d2 = deque()
for i in range(20):
    d2.append(i)
for i in range(15):
    d2.popleft()
print(d2, d2.index(17), d2.count(18))
d2.extendleft([100, 101])
print(d2, d2 == deque([101, 100, 15, 16, 17, 18, 19]))
try:
    deque().pop()
except IndexError as e:
    print("IndexError", e)

dd = defaultdict(list)
for k, v in [("a", 1), ("b", 2), ("a", 3)]:
    dd[k].append(v)
print(dd, dd["zz"], sorted(dd), isinstance(dd, dict))
counts = defaultdict(int)
for ch in "mississippi":
    counts[ch] += 1
print(dict(counts))

c = Counter("abracadabra")
print(c, c.most_common(2), c["z"], c.total())
c.update("aaz")
c.subtract({"a": 2})
print(sorted(c.elements())[:6], c + Counter(b=5), c - Counter(a=100), Counter(a=3) & Counter(a=1, b=2), Counter(a=1) | Counter(a=4))
print(Counter(cats=4, dogs=8), -Counter(a=-2, b=3), +Counter(a=-1, b=2))

od = OrderedDict()
od["x"] = 1
od["y"] = 2
od["z"] = 3
od.move_to_end("x")
print(od, od.popitem(), od.popitem(last=False), od)
od["w"] = 0
od.move_to_end("w", last=False)
print(list(od.keys()), OrderedDict.fromkeys("ab", 0))

Point = namedtuple("Point", "x y")
p = Point(1, y=2)
print(p, p.x + p.y, p[0], p._asdict(), p._replace(x=10), Point._fields, Point._make([5, 6]))
x, y = p
print(x, y, p == (1, 2), isinstance(p, tuple), len(p))
P3 = namedtuple("P3", ["a", "b", "c"], defaults=[0])
print(P3(1, 2), P3(1, 2, 3), P3._field_defaults)
try:
    Point(1)
except TypeError as e:
    print("TypeError", e)


match p:
    case Point(x=1, y=yy):
        print("matched", yy)

cm = ChainMap({"a": 1}, {"a": 2, "b": 3})
print(cm["a"], cm["b"], len(cm), sorted(cm), "b" in cm)
cm["c"] = 9
print(cm.maps[0], cm.new_child({"a": 0})["a"], cm.parents["a"])

print(isinstance([], cabc.Sequence), isinstance({}, cabc.Mapping), isinstance(set(), cabc.Set))
print(isinstance([], cabc.Iterable), isinstance(5, cabc.Iterable), isinstance(len, cabc.Callable))
print(issubclass(list, cabc.MutableSequence), issubclass(tuple, cabc.MutableSequence))


class Bag(cabc.Sized, cabc.Iterable):
    def __init__(self, items):
        self.items = list(items)

    def __len__(self):
        return len(self.items)

    def __iter__(self):
        return iter(self.items)


class Ranged(cabc.Sequence):
    def __init__(self, n):
        self.n = n

    def __len__(self):
        return self.n

    def __getitem__(self, i):
        if i >= self.n:
            raise IndexError(i)
        return i * i


r = Ranged(4)
print(len(Bag("abc")), list(r), 9 in r, r.index(4), list(reversed(r)), r.count(1))


class Thing:
    def __len__(self):
        return 0


print(isinstance(Thing(), cabc.Sized), isinstance(Thing(), cabc.Container))

nested = {"a": [1, 2, {"b": [3]}], "t": (1, [2])}
shallow = copy.copy(nested)
deep = copy.deepcopy(nested)
nested["a"][2]["b"].append(4)
print(shallow["a"][2], deep["a"][2], shallow is nested, deep["t"])


class Box:
    def __init__(self, v):
        self.v = v


b = Box([1])
b2 = copy.copy(b)
b3 = copy.deepcopy(b)
b.v.append(2)
print(b2.v, b3.v, type(b3).__name__)
lst = [1]
pair = [lst, lst]
dp = copy.deepcopy(pair)
print(dp[0] is dp[1], dp[0] is lst)

print(string.ascii_letters, string.digits, string.punctuation)
print(string.capwords("hello   big world"))
t = string.Template("$who likes ${what}; $$5")
print(t.substitute(who="tim", what="pie"), t.safe_substitute(who="x"))
try:
    t.substitute(who="a")
except KeyError as e:
    print("KeyError", e)
