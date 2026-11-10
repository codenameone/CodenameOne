# list, dict, set, tuple behaviour.
a = [3, 1, 4, 1, 5, 9, 2, 6]
a.append(5); a.extend([3, 5]); a.insert(0, 0); print(a)
print(a.pop(), a.pop(0), a.index(9), a.count(5), a)
a.remove(1); a.reverse(); print(a)
print(sorted(a, key=lambda v: (v % 3, -v)), a[2:-2], a[::-3], a[100:], a[-100:2])
a[1:3] = [10, 20, 30]; print(a)
del a[::2]; print(a)
b = a.copy(); b.clear(); print(a == b, len(a), b, a * 0, [[0] * 2] * 2)
words = ["pear", "apple", "fig", "banana"]
print(sorted(words, key=len), sorted(words, key=str.upper, reverse=True), min(words, key=len))
d = dict(a=1, b=2)
d.update({"c": 3}, d=4); d.setdefault("e", 5); d.setdefault("a", 100)
print(d, d.pop("b"), d.pop("zz", None), list(d.keys()), list(d.values()), d.popitem())
print({**d, "x": 0}, dict(zip("ab", [1, 2])), dict.fromkeys("xy", 0), sorted(d.items(), key=lambda kv: -kv[1]))
for k in list(d):
    if d[k] % 2:
        del d[k]
print(d, {1: "a", True: "b", 1.0: "c"})
s = {1, 2, 3}
s.add(4); s.discard(10); s.remove(1)
print(s, s | {9}, s - {2}, s ^ {3, 7}, s <= {2, 3, 4, 5}, {2, 3} < s, s.isdisjoint({8}), frozenset([1, 2]) == {1, 2})
t = (1, 2, 3, 2)
print(t.count(2), t.index(3), t + (4,), t[::-1], (1, [2]) == (1, [2]), hash((1, 2)) == hash((1, 2)), tuple("ab"))
m = [[i * j for j in range(4)] for i in range(3)]
print(m, [row[1] for row in m], list(zip(*m)), sum(m, []))
print(list(range(10, 0, -3)), range(5)[-1], len(range(0, 100, 7)), 3 in range(5), range(0, 10, 2)[1:3])
nested = {"k": [1, {"z": (2, 3)}]}
print(nested["k"][1]["z"][1], repr(nested), str([None, True, 1.0, "s"]))
x = y = []
x.append(1)
print(x is y, y, [1, 2] == [1, 2], [1, 2] is [1, 2], [] == [], not [])
print(list(map(lambda p, q: p * q, [1, 2, 3], [4, 5, 6])), list(enumerate(["a", "b"])), sum([0.1] * 10))


# A local re-sliced in a loop: its list is reused, which must stay invisible.
def recycle(src):
    out = []
    perm = None
    for k in range(4):
        perm = src[:]
        perm[0] = k
        out.append(perm[:])
    shorter = src[1:]
    for k in range(3):
        shorter = shorter[1:]
        print(shorter[:])
    big = src[:]
    big = src[::2]
    print(big[:], len(big[:]))
    floats = [1.5, 2.5, 3.5]
    f = floats[:]
    f = floats[1:]
    f[0] = 9.0
    print(f[:], floats)
    return out


print(recycle([10, 20, 30, 40, 50]))


def leaks(src):
    keep = []
    p = src[:]
    keep.append(p)
    p = src[:]
    p[0] = -1
    print(keep, p[:])


leaks([1, 2, 3])


def rows(n):
    parts = []
    for i in range(n):
        row = []
        for j in range(i + 1):
            row.append(str(j))
        parts.append("-".join(row))
        row[0] = "x"
    return parts


print(rows(4))

SHARED = [7, 8, 9]


class Lender:
    def __getitem__(self, k):
        return SHARED


def borrowed():
    p = Lender()[:]
    p = [1, 2, 3][:]
    p[0] = 0
    q = Lender()[1:]
    q = []
    q.append(5)
    print(SHARED, p[:], q[:])


borrowed()


def fused_counts():
    class Loud:
        def __init__(self):
            self.d = {}

        def get(self, k, d=None):
            return 100

        def __setitem__(self, k, v):
            self.d[k] = v

        def items(self):
            return self.d.items()
    words = ["a", "b", "a", 1, 1.0, "c"]
    plain = {}
    loud = Loud()
    for w in words:
        plain[w] = plain.get(w, 0) + 1
        loud[w] = loud.get(w, 0) + 1
    t = []
    for w in "xy":
        t = t
        t.append(len(w))

    class Bag:
        def __init__(self):
            self.items = []

        def append(self, x):
            self.items.append(x * 2)
    b = Bag()
    for w in [1, 2]:
        b.append(w)
    acc = {"k": "x"}
    k = "k"
    acc[k] = acc.get(k, "") + "y"
    print(sorted(plain.items(), key=str), sorted(loud.items(), key=str), t, b.items, acc)


def shadowed_len():
    def len(x):
        return -1
    w = "abc"
    print(len(w))


fused_counts()
shadowed_len()
