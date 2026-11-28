# Statements typed functions now lower themselves: assert, annotated
# assignment, global, local imports, del and with.

counter = 0


class Quiet:
    def __init__(self, log: list, swallow: bool):
        self.log = log
        self.swallow = swallow

    def __enter__(self):
        self.log.append("in")
        return len(self.log)

    def __exit__(self, t, v, tb):
        self.log.append("out " + (t.__name__ if t is not None else "-"))
        return self.swallow


def bump(n: int):
    global counter
    counter += n
    return counter


def guarded(log: list, x: int):
    name = None
    with Quiet(log, True) as k:
        name = "set"
        if x > 2:
            raise KeyError(x)
        name = "kept " + str(k)
    # The exit swallowed the KeyError: name may still be "set".
    return name if name is not None else "none"


def through(log: list, xs: list):
    for x in xs:
        with Quiet(log, False):
            if x == 2:
                continue
            if x == 4:
                break
            if x == 5:
                return "returned"
            log.append(x)
    return "done"


def asserted(n: int):
    total: int = 0
    for i in range(n):
        assert i < 3, "too big " + str(i)
        total += i
    return total


def imports(s: str):
    import math
    import os.path
    from collections import OrderedDict as OD
    d = OD()
    d[s] = math.floor(2.5)
    return list(d.items()), os.path.basename("/a/b/" + s)


def deleting(d: dict, keys: list):
    tmp = [1, 2, 3]
    for k in keys:
        del d[k]
    del tmp[0]
    obj = Quiet([], False)
    del obj.swallow
    gone = hasattr(obj, "swallow")
    x = d
    del x
    return sorted(d), tmp, gone


def unbound_after_del(d: dict):
    x = d
    del x
    try:
        print(x)
    except NameError as e:
        print("unbound", type(e).__name__)


def main():
    log = []
    print(guarded(log, 1), guarded(log, 3), log)
    log = []
    print(through(log, [1, 2, 3, 4]), log)
    log = []
    print(through(log, [5]), log)
    print(asserted(3))
    try:
        asserted(5)
    except AssertionError as e:
        print("assert", e)
    print(imports("k"))
    print(deleting({"a": 1, "b": 2, "c": 3}, ["a", "c"]))
    unbound_after_del({})
    print(bump(2), bump(3), counter)


main()
