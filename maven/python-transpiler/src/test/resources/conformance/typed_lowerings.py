# Typed lowerings with fast paths and generic fallbacks: any/all over a
# generator, two-argument min/max, extend with a literal list, str.find with
# a start of no known type, a str subscript compared by char, set().


class Rule:
    def __init__(self, on):
        self.on = on


class Odd:
    def __init__(self, v):
        self.v = v

    def __lt__(self, o):
        return self.v % 7 < o.v % 7

    def __gt__(self, o):
        return self.v % 7 > o.v % 7

    def __repr__(self):
        return "Odd(%d)" % self.v


class Idx:
    def __index__(self):
        return 2


def flags(rules, limit):
    hit = any(r.on for r in rules)
    every = all(r.on for r in rules if r.on is not None)
    deep = any(a * b > limit for a in range(4) for b in range(4) if a != b)
    return hit, every, deep


def mins(a, b):
    return min(a, b), max(a, b)


def grow(xs, a, b, c):
    out = []
    out.extend([a, b])
    out.extend([a, b, c])
    xs.extend([c, c])
    return out, xs


def scan(text, start):
    k = start
    n = len(text)
    while k < n and text[k] not in " =>":
        k += 1
    q = text.find("=", start)
    s = text.startswith("b", start)
    return k, q, s, text[k] == ">" if k < n else None


def probe(text, start):
    q = text.find("c", start)
    return q, text.startswith("c", start), text.endswith("c", start)


def count(words):
    seen = set()
    for w in words:
        seen.add(w)
    return len(seen), sorted(seen)


print(flags([Rule(0), Rule(None), Rule(3)], 5))
print(flags([Rule(None)], 100))
print(flags([], -1))
print(mins(3, 9), mins(9, 3), mins(2.5, 2), mins("b", "a"), mins(10 ** 30, 5))
print(mins(Odd(3), Odd(10)), mins(Odd(8), Odd(1)))
print(grow([1], "x", 2, None))
print(scan("ab c=d>", 0), scan("ab=c>", True), scan("abc>", 10 ** 20))
print(probe("abcabc", Idx()), probe("abcabc", None), probe("abcabc", -2), probe("abcabc", 1))
print(scan("x\U0001f600y=z>", 0))
try:
    probe("abc", "1")
except TypeError as e:
    print("TypeError", e)
print(count(["a", "b", "a"]))
real_any, real_min = any, min
any = lambda it: "patched any %d" % len(list(it))
min = lambda a, b: "patched min"
print(flags([Rule(1)], 0))
print(mins(1, 2))
any, min = real_any, real_min
print(flags([Rule(1)], 0), mins(1, 2))
