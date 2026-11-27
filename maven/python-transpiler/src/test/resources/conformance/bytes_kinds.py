import io
import re
import zipfile

d = {}
d[bytes] = "b"
d[str] = "s"
print(len(d), d[bytes], d[str])

buf = io.BytesIO()
with zipfile.ZipFile(buf, "w") as z:
    z.writestr("a.xml", "<a>text</a>")
    z.writestr("b.bin", b"\x89PNG")
with zipfile.ZipFile(buf) as z:
    for n in ("a.xml", "b.bin"):
        v = z.read(n)
        print(n, type(v).__name__, isinstance(v, bytes))
        with z.open(n) as f:
            w = f.read()
            print(n, type(w).__name__, isinstance(w, bytes))

for v in (io.BytesIO(b"<p>x</p>").read(), io.BytesIO(b"<p>x</p>").getvalue(),
          io.BytesIO(b"<p>x</p>").read(3), memoryview(b"ab").tobytes(), bytes(bytearray(b"z")),
          b"abc"[1:], b"a" + b"b", b"ab" * 2, "x".encode("utf-8"), b"%s" % b"q"):
    print(type(v).__name__, isinstance(v, bytes))

pat = re.compile(b"<\\s*meta[^>]+charset", re.I)
print(pat.search(b"<meta charset=utf-8>", endpos=100) is not None)
print(pat.search(bytearray(b"<meta charset=utf-8>")) is not None)
try:
    re.compile("x").search(b"x")
except TypeError as e:
    print("TypeError", e)
try:
    re.compile(b"x").search("x")
except TypeError as e:
    print("TypeError", e)

print(b"%s-%d-%5s|%-4b|%r|%a|%c%c|%x|%%" % (b"ab", 7, b"cd", bytearray(b"e"), b"q'", "\xe9", 65, b"z", 255))
print(bytearray(b"[%s]") % (memoryview(b"mv"),))
print(b"%(k)s=%(v)d" % {b"k": b"key", b"v": 3})


class B:
    def __bytes__(self):
        return b"custom"


print(b"<%s>" % B())
for bad in ("str", 1, 1.5):
    try:
        b"%s" % bad
    except TypeError as e:
        print("TypeError", e)


class Nav(str):
    pass


n = Nav("alpha beta")
w = re.compile(r"\w+")
m = w.search(n)
print(m.group(), type(m.string).__name__, m.string is n)
print(w.findall(n), re.split(" ", n), [x.group() for x in w.finditer(n)])
print(repr(w.sub("X", n)), repr(w.sub(lambda q: q.group().upper(), n)), w.fullmatch(Nav("one")) is not None)
print(re.match("a", n) is not None, re.sub("a", "b", n))


class Raw(bytes):
    pass


bw = re.compile(rb"\w+")
print(bw.findall(bytearray(b"ab cd")), bw.findall(memoryview(b"ef gh")), bw.findall(Raw(b"ij kl")))
print(bw.sub(b"-", bytearray(b"ab cd")), re.split(rb" ", bytearray(b"x y")))
for bad in (1, None):
    try:
        w.search(bad)
    except TypeError as e:
        print("TypeError", e)
