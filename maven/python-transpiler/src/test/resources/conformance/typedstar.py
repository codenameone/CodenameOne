# *args, **kwargs and keyword-only parameters in typed bodies.


def joined(sep, *parts, upper=False):
    out = sep.join(str(p) for p in parts)
    return out.upper() if upper else out


def options(name, **kw):
    keys = sorted(kw)
    total = 0
    for k in keys:
        total += len(str(kw[k]))
    return name + ":" + ",".join(keys) + ":" + str(total)


def mixed(a, b=2, *rest, scale, **extra):
    s = (a + b) * scale
    for r in rest:
        s += r
    return s, len(extra)


class Fmt:
    def __init__(self, width: int):
        self.width = width

    def cells(self, *vals, fill=" "):
        return "|".join(str(v).rjust(self.width, fill) for v in vals)


def main():
    print(joined("-", 1, 2, "x"), joined("+", "a", upper=True), joined("."))
    print(options("o", b=10, a="xy"), options("none"))
    print(mixed(1, scale=3), mixed(1, 5, 7, 9, scale=2, z=1, y=2))
    f = Fmt(3)
    print(f.cells(1, 22, "abc"), f.cells(4, fill="0"))
    try:
        mixed(1)
    except TypeError as e:
        print("TypeError", "scale" in str(e))


main()
