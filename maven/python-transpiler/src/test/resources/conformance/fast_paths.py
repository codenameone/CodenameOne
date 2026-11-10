# Int and float fast paths, fused float expressions, slices and constant indexes.

def show(label, f):
    try:
        r = f()
        print(label, type(r).__name__, repr(r))
    except Exception as e:
        print(label, type(e).__name__, e)


a, b, c = 1.5, 2.25, -3.0
i, j = 7, -2
show("fused float", lambda: (a * a + b * b + c * c) ** 0.5)
show("fused mixed", lambda: a * i + b - j)
show("fused ints", lambda: i * i + j * j - 3)
show("int leaf float const", lambda: i * 2.0 + j * 0.5)
show("zero div", lambda: a * b / (c - c))
show("int zero div", lambda: i // (j - j))
show("pow overflow", lambda: (a * 1e200) ** 2.0)
show("neg", lambda: -a * b + -(c - a))
show("float overflow inf", lambda: a * 1e308 * 10.0)
show("big int add", lambda: 9223372036854775807 + 1)
show("big int mul", lambda: 4611686018427387904 * 4)
show("big int sub", lambda: -9223372036854775807 - 10)
show("floordiv neg", lambda: -7 // 2)
show("mod neg", lambda: -7 % 3)
show("mod neg divisor", lambda: 7 % -3)
show("mixed cmp", lambda: (1 < 1.5, 2 == 2.0, 3 >= 3.0, 2 ** 53 + 1 == float(2 ** 53 + 1)))
show("shift", lambda: (1 << 62, 1 << 64, -16 >> 2, 5 >> 70))
show("bits", lambda: (6 & 3, 6 | 3, 6 ^ 3, ~5))
show("true div ints", lambda: (7 / 2, -7 / 2, 2 ** 60 / 3))
show("hash eq", lambda: ({1: "a"}[1.0], {2.0: "b"}[2], hash(1) == hash(1.0)))
show("bool arith", lambda: (True + True, True * 2.5, -True))

xs = list(range(10))
show("slice", lambda: xs[2:8:2])
show("slice neg", lambda: xs[8:2:-3])
show("slice copy", lambda: xs[:])
show("const index", lambda: (xs[0], xs[-1], xs[9]))
show("const index err", lambda: xs[10])
ys = list(range(6))
ys[:3] = ys[2::-1]
print("same len assign", ys)
ys[1:2] = [10, 11, 12]
print("grow assign", ys)
ys[0:4] = []
print("shrink assign", ys)
ys[::2] = [0, 0]
print("extended assign", ys)
try:
    ys[::2] = [1]
except ValueError as e:
    print("extended err", e)
zs = [1, 2, 3]
zs[:] = zs
print("self assign", zs)
zs[1:] = zs
print("self grow", zs)
t = (4, 5, 6)
show("tuple const index", lambda: (t[0], t[-1]))
show("str slice", lambda: "hello"[1:4])

# Wide int products: exact in 64 bits, and the ones that overflow into big ints.
print(1103515245 * 1987654321, 3037000499 * 3037000499, 3037000500 * 3037000500)
print(-9223372036854775807 * 1, (-9223372036854775807 - 1) * -1, 4611686018427387904 * 2)
s = 12345
for _ in range(5):
    s = (s * 1103515245 + 12345) & 0x7fffffff
print(s)

# Builtin methods called directly.
d = {"a": 1}
print(d.get("a"), d.get("b"), d.get("b", 7), d.get("a", 7))
lst = []
print(lst.append(3), lst)
print("x y  z".split(), "a,b,,c".split(","), "AbC".upper(), "AbC".lower())
print("-".join(["p", "q"]), "-".join(("p", "q")), "-".join(c for c in "rs"), "".join([]))
print(len("abc"), len([1, 2]), len({}))
try:
    "-".join(["p", 1])
except TypeError as e:
    print("TypeError", e)

# Float regions: straight-line float statements, their bail-outs and locals.
def region(a, b, v):
    dx = a - b
    dy = a * b
    mag = (dx * dx + dy * dy) ** 0.5
    v[0] -= dx * mag
    v[1] += dy / mag
    return v


print(region(3.5, 1.25, [1.0, 2.0]))
print(region(3, 1, [1, 2]))          # ints all the way: the generic path
print(region(3.5, 1, [1.0, 2]))      # mixed: bails part way
print(region(3.5, 1.25, (1.0, 2.0)) if False else "skip")
try:
    region(3.5, 1.25, (1.0, 2.0))
except TypeError as e:
    print("TypeError", e)
try:
    region(1.5, 1.5, [0.0, 0.0])     # zero magnitude: ZeroDivisionError in the region
except ZeroDivisionError as e:
    print("ZeroDivisionError", e)


def region_live(a, b):
    t = a * b
    u = t + a
    w = u - b
    return t, w                       # t is read after the region


for k in range(12):                   # ints first, then floats: the fast path turns itself off
    print(region_live(k, 2) if k < 10 else region_live(k + 0.5, 2.0), end=" ")
print()


def region_loop(xs):
    total = 0.0
    for x in xs:
        sq = x * x
        total += sq
    return total


print(region_loop([0.5, 1.5, 2.5]), region_loop([1, 2, 3]), region_loop([1.0, 2, 3.5]))


def region_unbound(f):
    if f:
        q = 1.0
    r = q * 2.0
    s = r + 1.0
    return s


print(region_unbound(True))
try:
    region_unbound(False)
except UnboundLocalError as e:
    print("UnboundLocalError", e)

# Lists of floats, kept unboxed until something else goes in.
fl = [1.5, 2.5, 3.5]
fl.append(4.5)
fl[0] = -1.0
print(fl, len(fl), fl[-1], fl[1:3], 2.5 in fl, fl.index(3.5), sum(fl))
z = [0.0] * 3
z[1] += 2.25
z[2] = 7                              # an int: the list becomes boxed
print(z, z * 2, z + fl)
fl.append("s")
print(fl)
g = []
for q in range(4):
    g.append(q * 0.5)
g.insert(0, 9.0)
g.sort()
g.reverse()
print(g, g.pop(), g == [9.0, 1.5, 1.0, 0.5], [x * 2 for x in g])
a1, a2 = [0.25, 0.75]
print(a1, a2, sorted([3.0, 1.0, 2.0]), list(reversed([1.0, 2.0])))
h = [1.0, 2.0, 3.0]
h[1:2] = [5.0, 6.0]
del h[0]
print(h, h.count(6.0), max(h), min(h))


def region_list(v, w):
    d = v[0] - w[0]
    e = d * 2.0
    v[1] += e
    w[1] -= e
    return v, w


print(region_list([1.0, 2.0], [0.5, 0.5]), region_list([1.0, 2], [0.5, 0.5]), region_list([1, 2], [0, 0]))

# A list changed while a for loop walks it.
grow = [1, 2, 3]
for x in grow:
    if x < 3:
        grow.append(x + 10)
print(grow)
shrink = [1, 2, 3, 4, 5]
for x in shrink:
    shrink.remove(x)
print(shrink)
seen = []
rebound = [1, 2]
for x in rebound:
    rebound = [9, 9, 9]
    seen.append(x)
print(seen, rebound)
for x in []:
    print("never")
else:
    print("empty else")

# Fused int trees: every edge where long arithmetic leaves Python's ints.
def lcg(seed):
    return (seed * 1103515245 + 12345) & 0x7fffffff


def int_tree(a, b, c):
    return [(a + b) * (a + b + 1) >> 1, a * b + c, (a - b) // c, (a - b) % c, a // c - b % c,
            ~a + (b << 3), -a * b - c, (a ^ b) | c & 7, 1.0 / (((a + b) * (a + b + 1) >> 1) + a * a + 1), a * c + b - 0.5 * c]


s = 12345
for _ in range(5):
    s = lcg(s)
print(s)
print(int_tree(7, 3, 2), int_tree(-7, 3, 2), int_tree(7, -3, -2), int_tree(2 ** 40, 2 ** 40, 3))
print(int_tree(2 ** 62, 2 ** 62, 5), int_tree(-2 ** 63, 1, -1))
big = 2 ** 61
print(big * 4 + 1, big * big - 1, (big << 3) >> 2, (1 << 70) >> 68, 5 >> 100, -5 >> 100, (-big - big) * 2 - 1)
print((2 ** 53 + 1) / 3 + 0, (2 ** 60) / (2 ** 59 + 1) + 0, 7 // 2 * 3 + 1, -7 // 2 * 3 + 1, 7 % -3 * 2 + 1)
print(True + True * 3, "%d-%d" % (3, 4) * 2, 3 * 2 ** 2 + 1, 2 ** -1 + 1, 10 // 3 + 10 % 3)
for bad in [(1, 0), (0, 0)]:
    try:
        print(bad[0] // bad[1] + 1)
    except ZeroDivisionError as e:
        print("ZeroDivisionError", e)
try:
    print((1 << -1) + 1)
except ValueError as e:
    print("ValueError", e)

# Pure float functions, and loops that keep their float locals unboxed.
def tri(i, j):
    return 1.0 / (((i + j) * (i + j + 1) >> 1) + i + 1)


def scale(x, k):
    return x * k - 0.5


def dot(u, v, n):
    s = 0.0
    for i in range(n):
        s += u[i] * v[i]
    return s


def accumulate(n, items):
    s = 0
    t = 1.0
    for j in range(n):
        s += tri(j, 3) * items[j]
        t *= 1.5
    return s, t


def inf_minus(a, b):
    return a * 2.0 - b


print(dot([1.0, 2.0, 3.0], [0.5, 0.25, 2.0], 3), dot([1, 2, 3], [4, 5, 6], 3), dot([1.0, 2, 3.0], [1.0, 1.0, 1.0], 3))
print(dot([], [], 0), accumulate(4, [1.0, 2.0, 3.0, 4.0]), accumulate(4, [1.0, 2, 3.0, 4]), accumulate(0, []))
print(tri(2, 3), tri(-5, 3), tri(2 ** 62, 2 ** 62), scale(3, 2), scale(3.0, 2.0), scale(True, 2.5))
print(inf_minus(float("inf"), float("inf")), [inf_minus(x, 1.0) for x in (1.0, 2.5)])
try:
    accumulate(3, [1.0, "x", 2.0])
except TypeError as e:
    print("TypeError", e)
saved = tri
tri = lambda i, j: 2
print(accumulate(3, [1.0, 1.0, 1.0]))
tri = saved
total = 0.0
for k in range(5):
    total += scale(k, 0.5)
print(total)


# A counted loop whose index a float region reads unboxed.
def k2(i, j):
    return 1.0 / (((i + j) * (i + j + 1) >> 1) + i + 1)


def sweep(v, start, stop):
    s = 0.0
    j = "before"
    for j in range(start, stop):
        s += k2(3, j) * v[j] + j
    return s, j


vals = [0.5, 1.5, 2.5, 3.5, 4.5]
print(sweep(vals, 0, 5), sweep(vals, 2, 2), sweep(vals, 1, 4))
mixed = [0.5, 1.5, "x", 3.5]
try:
    print(sweep(mixed, 0, 4))
except TypeError as e:
    print("TypeError", e)


def sweep_big(n):
    s = 0.0
    for j in range(2 ** 62, 2 ** 62 + n):
        s += j * 0.5
    return s, j


print(sweep_big(3))


def shadowed():
    def range(a, b):
        return [10, 20]
    s = 0.0
    for j in range(0, 2):
        s += k2(j, j) * 2.0
    return s, j


print(shadowed())


def rebound(n):
    global k2
    s = 0.0
    for j in range(n):
        s += k2(1, j)
        if_rebind = 0
    return s


print(rebound(4))
old_k2 = k2
k2 = lambda i, j: 2.5
print(rebound(4), sweep(vals, 0, 2))
k2 = old_k2


def int_parts(n, base):
    s = 0.5
    for j in range(base, base + n):
        s += j * j * j
        s -= j / 3
        s += (j - base) ** 2 * 0.25
    t = 0
    for j in range(n):
        t = j + 1
    lst = [0.5] * 4
    for j in range(4):
        lst[j] = lst[j] * 2.0 + j
    return s, t, lst


print(int_parts(5, 0), int_parts(3, 300000), int_parts(3, 2 ** 53 - 1), int_parts(2, 3037000499))
