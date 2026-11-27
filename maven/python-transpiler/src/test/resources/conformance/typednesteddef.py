# Nested functions with decorators and defaults in typed bodies.
import functools


def shout(fn):
    @functools.wraps(fn)
    def inner(*a, **k):
        return str(fn(*a, **k)).upper()

    return inner


def build(tag: str, scale: int):
    @shout
    def name(x="d"):
        return tag + x

    def times(v, k=scale * 2, *, sep=tag):
        return sep.join([str(v)] * k)

    @functools.lru_cache(maxsize=None)
    def sq(n):
        return n * n

    return name(), name("y"), name.__name__, times(3), times(4, 1), sq(5), sq(5)


def counter_with_default(start: int):
    n = start

    def bump(by=start):
        nonlocal n
        n += by
        return n

    bump()
    bump(10)
    return n


def alias_of(old: str, new: str):
    @property
    def alias(self):
        return getattr(self, new) + "<-" + old

    @alias.setter
    def alias(self, value):
        setattr(self, new, value)

    return alias


class Thing:
    real = "r"
    old = alias_of("old", "real")


def columns(lines: list, width: int):
    return ["".join(line[i] for line in lines) for i in range(width)]


def columns2(lines: list, flag: bool):
    if flag:
        width = max(map(len, lines))
        return "|".join("".join(line[i] for line in lines) for i in range(width))
    return ""


print(build("t", 1))
t = Thing()
t.old = "x"
print(t.old, t.real, columns(["ab", "cd"], 2), columns2(["ab", "cd"], True))
print(counter_with_default(2))


def make_classes(base_name: str, n: int):
    out = []
    lut = bytearray(4)
    pal = b"\x01\x02\x03\x04\x05"
    lut[0:2] = pal[1:3]
    for i in range(n):
        class Item(dict if i % 2 else object):
            tag = base_name + str(i)

            def show(self):
                return self.tag + ":" + base_name

        out.append((Item.__name__, Item().show(), Item.__mro__[1].__name__))
    return out, list(lut)


print(make_classes("b", 2))
