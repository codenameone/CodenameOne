# Generators with typed locals: values held across suspensions, flow facts
# that must not survive a back edge, send, return values, finally and with.


class Node:
    def __init__(self, name, kids):
        self.name = name
        self.kids = kids
        self.parent = None

    def walk(self):
        yield self.name
        for k in self.kids:
            for n in k.walk():
                yield n

    def ancestors(self):
        p = self.parent
        while p is not None:
            yield p.name
            p = p.parent


def count(n: int, step: int):
    i = 0
    total = 0
    while i < n:
        total += i
        yield i * step
        i += 1
    return total


def big_ints(start: int):
    x = start
    for _ in range(4):
        yield x
        x = x * x


def words(text: str):
    w = ""
    for c in text:
        if c == " ":
            if w:
                yield w
            w = ""
        else:
            w += c
    if w:
        yield w


def maybe(xs):
    last = None
    for x in xs:
        if last is not None:
            yield last + x
        last = None if x < 0 else x


def accum():
    total = 0
    while True:
        got = yield total
        if got is None:
            return total
        total += got


def guarded(log, n: int):
    try:
        for i in range(n):
            yield i
            if i == 2:
                raise ValueError("at two")
    except ValueError as e:
        log.append("caught " + str(e))
        yield -1
    finally:
        log.append("finally")


class Ctx:
    def __init__(self, log):
        self.log = log

    def __enter__(self):
        self.log.append("enter")
        return self

    def __exit__(self, *a):
        self.log.append("exit")
        return False


def within(log, n: int):
    with Ctx(log):
        for i in range(n):
            yield i * 10


def annotated(s: str):
    # Called with an int too: the guess at the door must not hold.
    yield s
    yield s


def early(xs: list):
    for x in xs:
        if x == 3:
            return "stopped"
        yield x


def main():
    a = Node("a", [])
    b = Node("b", [a])
    c = Node("c", [b, Node("d", [])])
    a.parent = b
    b.parent = c
    print(list(c.walk()))
    print(list(a.ancestors()))
    g = count(5, 3)
    print(list(g))
    g = count(4, 2)
    out = []
    while True:
        try:
            out.append(next(g))
        except StopIteration as e:
            out.append(("ret", e.value))
            break
    print(out)
    print(list(big_ints(3)))
    print(list(big_ints(2 ** 40)))
    print(list(words("  the quick  brown fox ")))
    print(list(maybe([1, 2, -1, 4, 5])))
    acc = accum()
    print(next(acc), acc.send(5), acc.send(7))
    try:
        acc.send(None)
    except StopIteration as e:
        print("accum", e.value)
    log = []
    print(list(guarded(log, 5)), log)
    log = []
    g = guarded(log, 5)
    print(next(g), next(g))
    g.close()
    print(log)
    log = []
    print(list(within(log, 3)), log)
    print(list(annotated("x")), list(annotated(7)), list(annotated(None)))
    print(list(early([1, 2, 3, 4])))
    g = early([5, 3])
    print(next(g))
    try:
        next(g)
    except StopIteration as e:
        print("early", e.value)
    print(sum(count(10, 1)))


main()
