# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.

# Negative behaviour: what a program does wrong must fail exactly as in CPython,
# with the same exception type, message, cause and context.


def show(fn, *a):
    try:
        r = fn(*a)
    except BaseException as e:
        cause = type(e.__cause__).__name__ if e.__cause__ is not None else None
        ctx = type(e.__context__).__name__ if e.__context__ is not None else None
        print(type(e).__name__, "|", e, "|", cause, ctx, e.__suppress_context__)
    else:
        print("ok", repr(r))


def gen_stop():
    yield 1
    raise StopIteration("inner")


show(lambda: list(gen_stop()))
show(lambda: 1 / 0)
show(lambda: 1 // 0)
show(lambda: 1 % 0)
show(lambda: 1.0 / 0)
show(lambda: [][0])
show(lambda: (1, 2)[5])
show(lambda: "abc"[10])
show(lambda: {}["k"])
show(lambda: {"a": 1}[("t", 1)])
show(lambda: int("12x"))
show(lambda: int("9", 8))
show(lambda: float("nan?"))
show(lambda: "a" + 1)
show(lambda: 1 + "a")
show(lambda: [1] * "x")
show(lambda: len(5))
show(lambda: None.foo)
show(lambda: object().bar)
show(lambda: undefined_name)
show(lambda: [].pop())
show(lambda: {}.popitem())
show(lambda: set().pop())
show(lambda: [1, 2].index(3))
show(lambda: "abc".index("z"))
show(lambda: next(iter([])))
show(lambda: iter(5))
show(lambda: hash([]))
show(lambda: {[]: 1})
show(lambda: sorted([1, "a"]))
show(lambda: max([]))
show(lambda: chr(-1))
show(lambda: ord("ab"))
show(lambda: "x".encode("ascii") + "y")
show(lambda: b"\xff".decode("utf-8"))
show(lambda: "é".encode("ascii"))
show(lambda: divmod(1, 0))
show(lambda: pow(0, -1))
show(lambda: 2 ** 10000 * 1.0)
show(lambda: round(float("inf")))
show(lambda: int(float("nan")))
show(lambda: range(0, 10, 0))
show(lambda: [1, 2, 3][::0])
show(lambda: tuple.__new__(5))
show(lambda: (lambda x: x)())
show(lambda: (lambda x: x)(1, 2))
show(lambda: (lambda x: x)(y=1))
show(lambda: (lambda *, k: k)(1))


def f(a, b=2, *, c):
    return a, b, c


show(f, 1)
show(lambda: f(1, 2, 3))
show(lambda: f(1, a=1, c=3))


class Thing:
    __slots__ = ("x",)


def set_y():
    t = Thing()
    t.y = 1


show(set_y)


def unbound():
    print(v)
    v = 1


show(unbound)


def chained():
    try:
        {}["missing"]
    except KeyError as e:
        raise ValueError("bad") from e


show(chained)


def implicit():
    try:
        1 / 0
    except ZeroDivisionError:
        raise TypeError("during")


show(implicit)


def suppressed():
    try:
        1 / 0
    except ZeroDivisionError:
        raise TypeError("quiet") from None


show(suppressed)


def reraise():
    try:
        raise OSError(2, "No such file", "f.txt")
    except OSError:
        raise


show(reraise)
show(lambda: open("/definitely/not/here.txt"))


class Boom(Exception):
    def __init__(self, code):
        super().__init__("boom", code)
        self.code = code


def custom():
    raise Boom(7)


show(custom)
try:
    custom()
except Boom as e:
    print(e.args, e.code, repr(e))


def assert_fail():
    assert 1 == 2, "math"


show(assert_fail)
show(lambda: int.from_bytes(b"\x01", "middle"))
show(lambda: [].sort(key=1))
show(lambda: dict([(1, 2, 3)]))
show(lambda: "{}{}".format(1))
show(lambda: "{x}".format(y=1))
show(lambda: "%d" % "s")
show(lambda: "%s %s" % (1,))
show(lambda: f"{1:q}")
show(lambda: bytes([256]))
show(lambda: bytearray(b"ab").remove(99))
show(lambda: frozenset([1]).add)
