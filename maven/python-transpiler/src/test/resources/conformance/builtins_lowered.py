# sorted, min, max, sum and round over values the runtime handles as
# primitives, and the same calls over values it does not.


class Box:
    def __init__(self, v):
        self.v = v

    def __lt__(self, other):
        return self.v < other.v

    def __repr__(self):
        return "Box(%d)" % self.v


def stats(xs):
    total = sum(xs)
    lo = min(xs)
    hi = max(xs)
    return total, lo, hi, round(total / len(xs), 2)


def main():
    ints = [5, 3, 9, 1]
    floats = [2.5, 0.5, 1.25]
    words = ["pear", "apple", "fig"]
    s = sorted(ints)
    s.append(10)
    print(s, ints)
    print(sorted(words), sorted(words, key=len), sorted(words, reverse=True))
    print(sorted("banana"), sorted({"b": 1, "a": 2}))
    print(stats(ints), stats(floats))
    print(min(3, 7), max(3, 7), min(2.5, 1.5), max("a", "b"))
    print(sum(floats), sum(ints, 10), sum(ints, 0.5), sum([True, True, False]))
    print(round(2.5), round(3.5), round(-0.5), round(2.675, 2), round(7, -1), round(1.5))
    boxes = [Box(3), Box(1), Box(2)]
    print(sorted(boxes), min(boxes), max(boxes))
    print(sorted(boxes, key=lambda b: -b.v))
    n = 0
    for v in sorted(ints):
        n = n * 10 + v
    print(n)
    try:
        min([])
    except ValueError as e:
        print("ValueError", e)
    mixed = [1, 2.5]
    print(sum(mixed), max(mixed), min(1, 2.0), type(max(1, 1.0)).__name__)



def loops():
    ints = [4, 8, 15]
    names = ["a", "b", "c"]
    total = 0
    for i, v in enumerate(ints):
        total += i * v
    print(total)
    for i, v in enumerate(names, 1):
        print(i, v.upper())
    for n, v in zip(names, ints):
        print(n + str(v + 1))
    for a, b, c in zip(ints, names, "xyz"):
        print(a, b, c)
    acc = ""
    for v in reversed(names):
        acc += v
    print(acc, [v * 2 for v in reversed(ints)])
    d = {"x": 1, "y": 2}
    for k in d.keys():
        print(k.upper())
    s = 0
    for v in d.values():
        s += v
    print(s)
    for k, v in d.items():
        print(k, v + 10)
    pairs = [(k, v) for k, v in d.items()]
    print(pairs, [i + v for i, v in enumerate(ints)])
    opt = {"a": None, "b": "z"}
    for k, v in opt.items():
        print(k, v is None, v)
    for i, ch in enumerate("hi"):
        print(i, ch)


main()
loops()
