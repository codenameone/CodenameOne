# Scratch: the hot shapes of document-conversion PDF and HTML paths, alone, in ns per operation.
import logging
import time

log = logging.getLogger("shapes")


class Ref:
    def __init__(self, objid):
        self.objid = objid

    def resolve(self):
        return self.objid


class Tag:
    def __init__(self, name, attrs):
        self.name = name
        self.attrs = attrs

    def match(self, other, _known_rules=False):
        return _known_rules and other.name == self.name


def resolve_all(x, depth=0):
    if isinstance(x, Ref):
        return resolve_all(x.resolve(), depth + 1)
    elif isinstance(x, (list, tuple)):
        return type(x)(resolve_all(v, depth + 1) for v in x)
    elif isinstance(x, dict):
        return {k: resolve_all(v, depth + 1) for k, v in x.items()}
    return x


N = 200000
OBJ = {"Type": "Font", "W": [1, 2, Ref(3), (4, 5)], "D": Ref({"a": 1})}
ATTRS = {"stroke": True, "fill": None, "linewidth": 1.0, "dash": None, "x0": 3.5}
TAGS = [Tag("a", {}), Tag("div", {}), Tag("a", {}), Tag("span", {})] * 5
PROBE = Tag("a", {})


def p_resolve_all(n):
    t = 0
    for _ in range(n // 20):
        t += len(resolve_all(OBJ))
    return t


def p_isinst_tuple(n):
    t = 0
    x = 3.5
    for _ in range(n):
        if isinstance(x, (int, float)):
            t += 1
    return t


def p_type_call_genexpr(n):
    t = 0
    x = (1, 2, 3, 4)
    for _ in range(n // 4):
        t += len(type(x)(v for v in x))
    return t


def p_dictcomp(n):
    t = 0
    for _ in range(n // 5):
        t += len({k: v for k, v in ATTRS.items()})
    return t


def p_dict_filter_map(n):
    t = 0
    for _ in range(n // 5):
        t += len(dict(filter(lambda kv: kv[1] is not None, map(lambda kv: (kv[0], kv[1]), ATTRS.items()))))
    return t


def p_gen_kwmethod(n):
    t = 0
    for _ in range(n // 20):
        t += sum(1 for i in TAGS if PROBE.match(i, _known_rules=True))
    return t


def p_log_debug(n):
    t = 0
    for i in range(n):
        log.debug("token %r", i)
        t += 1
    return t


def p_bytes_slice(n):
    t = 0
    b = b"/Name 12 0 R << /Type /Page >> stream"
    for i in range(n):
        if b[i & 15:(i & 15) + 1] == b"/":
            t += 1
    return t


def p_bytes_find(n):
    t = 0
    b = b"/Name 12 0 R << /Type /Page >> stream"
    for _ in range(n):
        t += b.find(b">>", 3)
    return t


def p_float_mul(n):
    t = 0.0
    a, b, c, d, e, f = 1.0, 0.0, 0.0, 1.0, 3.0, 4.0
    for i in range(n):
        x = float(i & 7)
        t += a * x + c * x + e
    return t


def p_tuple_build(n):
    t = 0
    for i in range(n):
        r = (i, i + 1, i + 2, i + 3)
        t += r[2]
    return t


PARTS = [
    ("resolve_all", p_resolve_all), ("isinst_tuple", p_isinst_tuple),
    ("type_genexpr", p_type_call_genexpr), ("dictcomp", p_dictcomp),
    ("dict_filtmap", p_dict_filter_map), ("gen_kwmeth", p_gen_kwmethod),
    ("log_debug", p_log_debug), ("bytes_slice", p_bytes_slice), ("bytes_find", p_bytes_find),
    ("float_mul", p_float_mul), ("tuple_build", p_tuple_build),
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
