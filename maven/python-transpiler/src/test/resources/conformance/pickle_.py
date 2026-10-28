import pickle
import copyreg
import dataclasses
import enum


def show(label, value):
    for proto in range(6):
        print(label, proto, pickle.dumps(value, proto).hex())


print(pickle.DEFAULT_PROTOCOL, pickle.HIGHEST_PROTOCOL)
print(pickle.dumps(None).hex())
show("none", None)
show("bool", (True, False))
show("ints", [0, 1, 255, 256, 65535, 65536, -1, -128, 2147483647, 2147483648, -2147483648, -2147483649])
show("big", [2 ** 64, -2 ** 64, 10 ** 30, -10 ** 30, 2 ** 2000, 255 << 8, -(255 << 8), 2 ** 63 - 1])
show("floats", [0.0, 1.5, -2.25, 1e300, 3.141592653589793])
show("strs", ["", "abc", "café", "line\nbreak\\", "中文", "\U0001f600", "x" * 300])
show("bytes", [b"", b"abc", bytes(range(256)), b"y" * 300])
show("bytearray", [bytearray(), bytearray(b"hey")])
show("tuples", [(), (1,), (1, 2), (1, 2, 3), (1, 2, 3, 4), ((4, 5), (6,))])
show("lists", [[], [1], [1, 2], list(range(1200)), [[], [[]]]])
show("dicts", [{}, {"a": 1}, {"a": 1, "b": [2, 3], "c": {"d": None}}, {i: i for i in range(1100)}])
show("sets", [set(), {1}, {1, 2, 3}, frozenset(), frozenset([1, 2, 3])])
show("complex", 1 + 2j)
show("range", range(1, 10, 2))
show("type", [int, str, dict])

shared = [1, 2]
show("shared", [shared, shared, (shared, shared)])
rec = []
rec.append(rec)
show("recursive", rec)
d = {}
d["self"] = d
show("recdict", d)
t = ("abc", "abc")
show("strmemo", [t, t, "abc"])


class Plain:
    def __init__(self, a, b):
        self.a = a
        self.b = b

    def __eq__(self, other):
        return type(other) is Plain and (self.a, self.b) == (other.a, other.b)


class Empty:
    pass


class WithState:
    def __init__(self):
        self.x = 1
        self.cache = "skip"

    def __getstate__(self):
        return {"x": self.x}

    def __setstate__(self, state):
        self.x = state["x"]
        self.cache = "restored"


class NewArgs:
    def __new__(cls, a, b):
        self = object.__new__(cls)
        self.a = a
        self.b = b
        return self

    def __getnewargs__(self):
        return (self.a, self.b)


class NewArgsEx:
    def __new__(cls, a, *, k=0):
        self = object.__new__(cls)
        self.a = a
        self.k = k
        return self

    def __getnewargs_ex__(self):
        return ((self.a,), {"k": self.k})


class Reduced:
    def __init__(self, v):
        self.v = v

    def __reduce__(self):
        return (Reduced, (self.v,))


class Slotted:
    __slots__ = ("p", "q")

    def __init__(self):
        self.p = 1
        self.q = "two"


class MyList(list):
    pass


class MyDict(dict):
    pass


@dataclasses.dataclass
class Point:
    x: int
    y: int = 2


class Color(enum.Enum):
    RED = 1
    GREEN = 2


def func(x):
    return x + 1


show("plain", Plain(1, "s"))
show("empty", Empty())
show("state", WithState())
show("newargs", NewArgs(1, 2))
show("reduced", Reduced(5))
show("dataclass", Point(3))
show("enum", Color.GREEN)
show("func", func)
show("class", Plain)
show("shareinst", [Plain(1, 2)] * 2)
for proto in range(2, 6):
    for label, value in (("newargsex", NewArgsEx(7, k=9)), ("slotted", Slotted()), ("mylist", MyList([1, 2])),
                         ("mydict", MyDict(a=1))):
        print(label, proto, pickle.dumps(value, proto).hex())
print("exc", [pickle.dumps(ValueError("bad", 1), p).hex() for p in range(6)])


def round_trip(value, **kw):
    for proto in range(6):
        out = pickle.loads(pickle.dumps(value, proto))
        if out != value:
            print("MISMATCH", proto, value, out)
    return pickle.loads(pickle.dumps(value, 5))


print(round_trip([None, True, 1, -5, 2 ** 100, 1.5, "s", b"b", bytearray(b"ba"), (1, 2), [3], {"k": "v"}, {4, 5}]))
print(round_trip(frozenset([1, 2])), round_trip(1 + 2j), list(pickle.loads(pickle.dumps(range(1, 8, 3)))), round_trip("中\U0001f600\n"))
out = round_trip(Plain([1], {2: 3}))
print(out.a, out.b)
x = pickle.loads(pickle.dumps(WithState()))
print(x.x, x.cache)
x = pickle.loads(pickle.dumps(NewArgs(1, 2)))
print(x.a, x.b)
x = pickle.loads(pickle.dumps(NewArgsEx(1, k=2)))
print(x.a, x.k)
x = pickle.loads(pickle.dumps(Slotted()))
print(x.p, x.q)
x = pickle.loads(pickle.dumps(MyList([1, 2])))
print(type(x).__name__, x)
x = pickle.loads(pickle.dumps(MyDict(a=1)))
print(type(x).__name__, x)
print(pickle.loads(pickle.dumps(Point(5, 6))), pickle.loads(pickle.dumps(Color.RED)) is Color.RED)
print(pickle.loads(pickle.dumps(func)) is func, pickle.loads(pickle.dumps(Plain)) is Plain)
print(pickle.loads(pickle.dumps(Reduced(9), 0)).v, type(pickle.loads(pickle.dumps(Empty(), 1))).__name__)
e = pickle.loads(pickle.dumps(ValueError("bad", 1)))
print(type(e).__name__, e.args)
r = pickle.loads(pickle.dumps(rec))
print(r[0] is r)
d2 = pickle.loads(pickle.dumps(d))
print(d2["self"] is d2)
s = pickle.loads(pickle.dumps([shared, shared]))
print(s[0] is s[1])

# Bytes CPython wrote (protocols 0 to 5 of one value).
value = {"n": [1, 2.5, None], "t": (True, "x"), "b": b"by", "s": {7}}
for proto in range(6):
    data = pickle.dumps(value, proto)
    print(proto, pickle.loads(data) == value)
print(pickle.loads(bytes.fromhex("286470300a566e0a70310a286c70320a49310a6146322e350a614e617356740a70330a284930310a56780a70340a7470350a7356620a70360a635f636f646563730a656e636f64650a70370a285662790a70380a566c6174696e310a70390a747031300a527031310a7356730a7031320a635f5f6275696c74696e5f5f0a7365740a7031330a28286c7031340a49370a61747031350a527031360a732e")) == value)
print(pickle.loads(bytes.fromhex("80027d71002858010000006e71015d7102284b014740040000000000004e6558010000007471038858010000007871048671055801000000627106635f636f646563730a656e636f64650a710758020000006279710858060000006c6174696e31710986710a52710b580100000073710c635f5f6275696c74696e5f5f0a7365740a710d5d710e4b076185710f527110752e")) == value)
print(pickle.loads(bytes.fromhex("80037d71002858010000006e71015d7102284b014740040000000000004e65580100000074710388580100000078710486710558010000006271064302627971075801000000737108636275696c74696e730a7365740a71095d710a4b076185710b52710c752e")) == value)
print(pickle.loads(bytes.fromhex("80049537000000000000007d94288c016e945d94284b014740040000000000004e658c017494888c01789486948c01629443026279948c0173948f94284b0790752e")) == value)
print(pickle.loads(bytes.fromhex("80059537000000000000007d94288c016e945d94284b014740040000000000004e658c017494888c01789486948c01629443026279948c0173948f94284b0790752e")) == value)
print(pickle.loads(b"(lp0\nI1\naI2\naF3.5\naS'abc'\np1\naV\\u4e2d\\u6587\np2\na."))
print(pickle.loads(b"\x80\x02]q\x00(K\x01G@\x04\x00\x00\x00\x00\x00\x00X\x03\x00\x00\x00abcq\x01e."))

# Pickler and Unpickler
import io
buf = io.BytesIO()
p = pickle.Pickler(buf, 2)
p.dump([1, 2])
p.dump("two")
buf.seek(0)
u = pickle.Unpickler(buf)
print(u.load(), u.load())
try:
    u.load()
except EOFError as ex:
    print("EOF", ex)
try:
    pickle.loads(b"\x80\x04\x95")
except Exception as ex:
    print(type(ex).__name__)
try:
    pickle.dumps(lambda: 1)
except pickle.PicklingError as ex:
    print("PicklingError")
try:
    pickle.dumps(Plain.__init__)
except pickle.PicklingError as ex:
    print("PicklingError2")
print(issubclass(pickle.PicklingError, pickle.PickleError), issubclass(pickle.UnpicklingError, pickle.PickleError))


class Persist(pickle.Pickler):
    def persistent_id(self, obj):
        if isinstance(obj, Empty):
            return "empty!"
        return None


class PersistLoad(pickle.Unpickler):
    def persistent_load(self, pid):
        return ("loaded", pid)


buf = io.BytesIO()
Persist(buf, 4).dump([Empty(), 1])
print(buf.getvalue().hex())
print(PersistLoad(io.BytesIO(buf.getvalue())).load())


class Restricted(pickle.Unpickler):
    def find_class(self, module, name):
        if name == "Plain":
            raise pickle.UnpicklingError("forbidden " + name)
        return pickle.Unpickler.find_class(self, module, name)


try:
    Restricted(io.BytesIO(pickle.dumps(Plain(1, 2)))).load()
except pickle.UnpicklingError as ex:
    print(ex)

copyreg.pickle(Empty, lambda obj: (Empty, ()))
print(pickle.dumps(Empty(), 4).hex())
print(pickle.encode_long(255).hex(), pickle.decode_long(b"\xff\x00"), pickle.encode_long(-256).hex())
