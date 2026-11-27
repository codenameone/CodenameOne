# Locals not every path binds, read in typed functions: checked at the read.


def pick(xs: list, want: str):
    for x in xs:
        if x == want:
            found = x
            break
    try:
        return "found " + found
    except UnboundLocalError as e:
        return "missing: " + str(e)


def parts(s: str):
    if s:
        head = s[0]
        rest = s[1:]
    n = len(s)
    if n:
        return head + "|" + rest
    return "empty"


def loop_carried(rows: list):
    out = []
    for r in rows:
        if r is not None:
            last = r
        out.append(last)
    return out


print(pick(["a", "b"], "b"), pick(["a"], "z"))
print(parts("hey"), parts(""))
print(loop_carried(["p", None, "q"]))
try:
    loop_carried([None])
except UnboundLocalError as e:
    print(type(e).__name__, e)


calls = []


def noisy(v):
    calls.append(v)
    return v


def branchy(p: bool, a, b, c):
    n = 0
    for x in range(1, 3):
        n += x
    # The untaken branch, generic arithmetic over unknown values, must not run.
    r = 0.0 if p else noisy(a) - b * c + n
    s = noisy(b) * c - a if not p else 1.5
    return r, s


print(branchy(True, 1.0, 2.0, 3.0), calls)
print(branchy(False, 1, 2.0, "3" and 3), calls)
print(branchy(True, "a", [1], None), calls)


def fail(msg):
    raise ValueError(msg)


def standalone(sa):
    if sa is None:
        flag = -1
    elif sa == "yes":
        flag = 1
    elif sa == "maybe":
        pass
    else:
        fail("bad " + sa)
    total = flag * 2 + 1
    return total


for arg in (None, "yes", "no", "maybe"):
    try:
        print(standalone(arg))
    except (ValueError, UnboundLocalError) as e:
        print(type(e).__name__, e)


def width(rows: list, headers: list) -> int:
    n = len(rows[0]) if rows else len(headers)
    return n + (rows[0][0] if rows else 0)


print(width([], ["a", "b"]), width([[3, 4, 5]], []))


def boom():
    raise ValueError("value first")


def short(words: list, limit: int) -> str:
    total = 0
    for i, w in enumerate(words):
        total += len(w)
        if total > limit:
            return " ".join(words[: i + 1])
    while i > 0:
        i -= 1
    return str(i)


def acc_unbound(xs: list):
    for x in xs:
        s = ""
        s += x
    s += boom()
    return s


def confirmish(answer: str, abort: bool) -> bool:
    if answer == "y":
        rv = True
    elif answer == "n":
        rv = False
    if abort and not rv:
        return False
    return rv


for call in (lambda: short(["ab", "cd"], 10), lambda: short([], 3), lambda: acc_unbound([]),
             lambda: acc_unbound(["a"]), lambda: confirmish("y", True), lambda: confirmish("x", False),
             lambda: confirmish("x", True)):
    try:
        print(call())
    except (ValueError, UnboundLocalError) as e:
        print(type(e).__name__, e)


def ratio(a: int, b: int) -> int:
    return a // b + 1 if b else -1


def ratio2(a: float, b: float, ok: bool) -> bool:
    return ok and a / b > 1.0


print(ratio(7, 2), ratio(7, 0), ratio2(3.0, 0.0, False), ratio2(3.0, 2.0, True))


class Swallow:
    def __enter__(self):
        return self

    def __exit__(self, *a):
        return True


def headers(src: dict, fail: bool, scale: list):
    with Swallow():
        if fail:
            raise KeyError("x")
        data = src
    out = sorted((h * 2 for h in data["h"]), key=lambda x: -x)
    return out, [data for s in scale]


for args in (({"h": [1, 3, 2]}, False, [1]), ({"h": [1]}, True, []), ({"h": [1]}, True, [1])):
    try:
        print(headers(*args))
    except UnboundLocalError as e:
        print(type(e).__name__, e)


def elt_only(fail: bool, scale: list):
    with Swallow():
        if fail:
            raise KeyError("x")
        data = "d"
    return [data + str(s) for s in scale], sum(len(data) for s in scale)


for args in ((False, [1]), (True, []), (True, [2])):
    try:
        print(elt_only(*args))
    except UnboundLocalError as e:
        print(type(e).__name__, e)
