# Scratch: the cost of each core operation, alone, in ns per operation.
import time


class Node:
    def __init__(self, name, parent=None):
        self.name = name
        self.parent = parent
        self.children = []
        self.attrs = {"class": "x", "id": "y"}

    def get(self, key, default=None):
        return self.attrs.get(key, default)

    def match(self, other, known=False):
        return known or other is self

    def __bool__(self):
        return True

    @property
    def label(self):
        return self.name


class Leaf(Node):
    def __init__(self, name):
        super().__init__(name)


def plain(a, b):
    return a


def defaults(a, b=1, c=None):
    return a


def star(*args):
    return args[0]


def kw(**kws):
    return kws


N = 200000
NODE = Node("div")
OTHER = Node("span")
LST = list(range(50))
S = "hello world, this is text"
D = {"a": 1, "b": 2, "c": 3}


def gen(n):
    for i in range(n):
        yield i


def p_call(n):
    t = 0
    for _ in range(n):
        t += plain(1, 2)
    return t


def p_defaults(n):
    t = 0
    for _ in range(n):
        t += defaults(1)
    return t


def p_kwcall(n):
    t = 0
    for _ in range(n):
        t += defaults(1, c=2)
    return t


def p_star(n):
    t = 0
    for _ in range(n):
        t += star(1, 2)
    return t


def p_method(n):
    t = 0
    nd = NODE
    for _ in range(n):
        if nd.get("class") is not None:
            t += 1
    return t


def p_method_kw(n):
    t = 0
    nd = NODE
    o = OTHER
    for _ in range(n):
        if nd.match(o, known=True):
            t += 1
    return t


def p_attr(n):
    t = 0
    nd = NODE
    for _ in range(n):
        if nd.parent is None:
            t += 1
    return t


def p_setattr(n):
    nd = Node("x")
    for i in range(n):
        nd.parent = i
    return nd.parent


def p_property(n):
    t = 0
    nd = NODE
    for _ in range(n):
        t += len(nd.label)
    return t


def p_truth(n):
    t = 0
    nd = NODE
    for _ in range(n):
        if nd:
            t += 1
    return t


def p_isinstance(n):
    t = 0
    nd = NODE
    for _ in range(n):
        if isinstance(nd, Node):
            t += 1
    return t


def p_isinstance_str(n):
    t = 0
    s = S
    for _ in range(n):
        if isinstance(s, str):
            t += 1
    return t


def p_new(n):
    t = 0
    for _ in range(n):
        t += len(Node("a").name)
    return t


def p_new_super(n):
    t = 0
    for _ in range(n):
        t += len(Leaf("a").name)
    return t


def p_gen(n):
    t = 0
    for i in gen(n):
        t += i
    return t


def p_next(n):
    g = gen(n)
    t = 0
    while True:
        try:
            i = next(g)
        except StopIteration:
            break
        t += i
    return t


def p_genexpr(n):
    t = 0
    for _ in range(n // 50):
        t += sum(x for x in LST)
    return t


def p_listcomp(n):
    t = 0
    for _ in range(n // 50):
        t += len([x for x in LST if x & 1])
    return t


def p_try(n):
    t = 0
    for i in range(n):
        try:
            t += i
        except ValueError:
            t -= 1
    return t


def p_raise(n):
    t = 0
    for i in range(n // 10):
        try:
            raise ValueError("x")
        except ValueError:
            t += 1
    return t


def p_dictget(n):
    t = 0
    d = D
    for _ in range(n):
        t += d.get("b", 0)
    return t


def p_dictin(n):
    t = 0
    d = D
    for _ in range(n):
        if "c" in d:
            t += 1
    return t


def p_dictset(n):
    d = {}
    for i in range(n):
        d[i & 255] = i
    return len(d)


def p_getattr3(n):
    t = 0
    nd = NODE
    for _ in range(n):
        if getattr(nd, "missing", None) is None:
            t += 1
    return t


def p_strmeth(n):
    t = 0
    s = S
    for _ in range(n):
        if s.startswith("hel"):
            t += 1
    return t


def p_strip(n):
    t = 0
    s = "  abc  "
    for _ in range(n):
        t += len(s.strip())
    return t


def p_strin(n):
    t = 0
    s = S
    for _ in range(n):
        if "this" in s:
            t += 1
    return t


def p_stridx(n):
    t = 0
    s = S
    for i in range(n):
        if s[i & 15] == "l":
            t += 1
    return t


def p_concat(n):
    t = 0
    for i in range(n):
        t += len("a" + S)
    return t


def p_fstring(n):
    t = 0
    for i in range(n):
        t += len(f"<{S}>{i}")
    return t


def p_join(n):
    t = 0
    parts = ["a", "b", "c", "d"]
    for _ in range(n):
        t += len("".join(parts))
    return t


def p_split(n):
    t = 0
    for _ in range(n):
        t += len(S.split())
    return t


def p_append(n):
    out = []
    for i in range(n):
        out.append(i)
    return len(out)


def p_len(n):
    t = 0
    lst = LST
    for _ in range(n):
        t += len(lst)
    return t


def p_listiter(n):
    t = 0
    for _ in range(n // 50):
        for x in LST:
            t += x
    return t


def p_closure(n):
    k = 3

    def f(x):
        return x + k
    t = 0
    for i in range(n):
        t += f(i)
    return t


def p_lambda_sort(n):
    t = 0
    data = [(i * 7) % 50 for i in range(50)]
    for _ in range(n // 50):
        t += sorted(data, key=lambda x: -x)[0]
    return t


def p_tuple_unpack(n):
    t = 0
    for i in range(n):
        a, b = i, 1
        t += a + b
    return t


def p_eq_str(n):
    t = 0
    s = "div"
    for _ in range(n):
        if s == "div":
            t += 1
    return t


def p_int_arith(n):
    t = 0
    for i in range(n):
        t += (i * 3 + 1) % 7
    return t


PARTS = [
    ("call", p_call), ("defaults", p_defaults), ("kwcall", p_kwcall), ("star", p_star),
    ("method", p_method), ("method_kw", p_method_kw), ("attr", p_attr), ("setattr", p_setattr),
    ("property", p_property), ("truth", p_truth), ("isinstance", p_isinstance),
    ("isinst_str", p_isinstance_str), ("new", p_new), ("new_super", p_new_super), ("gen", p_gen),
    ("next", p_next), ("genexpr", p_genexpr), ("listcomp", p_listcomp), ("try", p_try),
    ("raise", p_raise), ("dictget", p_dictget), ("dictin", p_dictin), ("dictset", p_dictset),
    ("getattr3", p_getattr3), ("strmeth", p_strmeth), ("strip", p_strip), ("strin", p_strin),
    ("stridx", p_stridx), ("concat", p_concat), ("fstring", p_fstring), ("join", p_join),
    ("split", p_split), ("append", p_append), ("len", p_len), ("listiter", p_listiter),
    ("closure", p_closure), ("sort_key", p_lambda_sort), ("unpack", p_tuple_unpack),
    ("eq_str", p_eq_str), ("int_arith", p_int_arith),
]


def main():
    import os
    only = os.environ.get("CORE_PART")
    if only:
        f = dict(PARTS)[only]
        for _ in range(400):
            f(N)
        return
    for name, f in PARTS:
        best = None
        for _ in range(5):
            t0 = time.perf_counter()
            f(N)
            dt = time.perf_counter() - t0
            if best is None or dt < best:
                best = dt
        print("PART %-12s %7.1f" % (name, best * 1e9 / N))


main()
