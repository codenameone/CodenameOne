# Typed functions holding expressions only generic code can evaluate: the
# typed locals they read are boxed, what they answer comes back unboxed.


def fmt(a, b, sep="-", end="."):
    return str(a) + sep + str(b) + end


class Box:
    unit = "cm"

    def __init__(self, w: int, h: int):
        self.w = w
        self.h = h

    def area(self) -> int:
        return self.w * self.h

    @classmethod
    def square(cls, n):
        return cls(n, n)

    def describe(self, prefix: str):
        a = self.area()
        inside = 0 < a <= 100
        label = fmt(prefix, a, sep=":", end="!")
        return label + " " + str(inside) + " " + Box.unit


def scan(xs: list, lo: int, hi: int):
    n = 0
    keep = []
    for x in xs:
        if lo <= x < hi:
            n += 1
            keep.append(fmt(x, n, end=""))
    return n, keep, {len(keep), n}


def gen(words: list, k: int):
    total = 0
    for w in words:
        total += len(w)
        yield fmt(w, total, sep="=")
        if 3 < total < 2 ** k:
            yield sorted([w, str(total)], key=len, reverse=True), len({w, str(total)})


def star(args: list, big: int):
    big = big * big * big * big
    return max(*args, big // 10 ** 30), fmt(*args[:2], **{"sep": "/"})


def main():
    b = Box(4, 5)
    print(b.describe("box"))
    print(Box.square(11).describe("sq"))
    print(scan([1, 5, 9, 12, 3], 2, 10))
    print(list(gen(["ab", "cde", "f"], 4)))
    print(star([3, 8, 2], 10 ** 9))
    print(star([3, 8, 2], 7))


main()


def drained(xs: list, lim: int):
    n = 0
    for x in xs:
        n += 1
    # Generator expressions a consumer drains on the spot, reading locals.
    return any(x > lim for x in xs), sum(x * n for x in xs), ",".join(str(x + n) for x in xs), \
        tuple(x for x in xs if x != lim)


print(drained([1, 2, 3], 2))
print(drained([], 0))


def lazy_any(xs: list, lim: int):
    seen = []
    k = 0
    for x in xs:
        k += x
    # any stops at the first hit: the generator must stay lazy.
    hit = any(seen.append(x) or x + k > lim for x in xs)
    return hit, seen, max(x - k for x in xs), min((x for x in xs if x > lim), default=-1)


print(lazy_any([1, 5, 2, 9], 20))


def padded_hit(rows: list, padding: int):
    n = 0
    for r in rows:
        n += len(r)
    # Temporaries of the generic code inside the capturing lambda.
    return any(r[-padding:] == "zz" for r in rows if len(r) > n // 10), n


print(padded_hit(["abzz", "cd", "zz"], 2), padded_hit(["ab"], 1))


def kept_gens(xs: list, scale: int):
    base = scale * 2
    out = []
    # Kept past the statement: base and scale never change, so the values
    # captured now are the ones CPython reads later.
    g = (x * base + scale for x in xs)
    out.extend(x - base for x in xs)
    return list(g), out, late_gens(xs)


def late_gens(xs: list):
    late = []
    for k in range(3):
        # k is rebound by the loop: this one must stay generic.
        late.append(k + y for y in xs)
    return [list(h) for h in late]


print(kept_gens([1, 2], 3))
