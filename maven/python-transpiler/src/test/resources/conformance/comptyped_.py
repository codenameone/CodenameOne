# Comprehension targets typed by what their iterable yields.


class Item:
    def __init__(self, name, price):
        self.name = name
        self.price = price

    def label(self):
        return self.name.upper() + "=" + str(self.price)


class Special(Item):
    def label(self):
        return "*" + Item.label(self)


def items():
    out = [Item("a", 1.5), Item("bb", 2.0)]
    out.append(Special("ccc", 3.25))
    return out


def names(xs):
    return [x.name for x in xs if len(x.name) > 1]


def labels(xs):
    return [x.label() for x in xs]


def total(xs):
    return sum([x.price * 2 for x in xs])


def nested(xs):
    return [c + "!" for x in xs for c in x.name]


def inner(xs):
    return [[x.name + y.name for y in xs] for x in xs]


def words(s):
    return [w.strip().lower() for w in s.split(",") if w.strip()]


def flags(xs):
    bs = [x.price > 2 for x in xs]
    return [not b for b in bs]


def mixed():
    vals = ["x", "y"]
    vals.append(3)
    vals.append(None)
    return [repr(v) for v in vals]


def pairs(xs):
    return [n + ":" + str(p) for n, p in [(x.name, x.price) for x in xs]]


def sets(xs):
    return sorted({x.name[0] for x in xs})


def dicts(xs):
    d = {x.name: x.price for x in xs}
    return sorted(d.items())


def shadow(xs):
    x = 42
    r = [x.name for x in xs]
    return r, x


def floats():
    fs = [0.5, 1.25, 2.0]
    return [f * f for f in fs]


xs = items()
print(names(xs))
print(labels(xs))
print(total(xs))
print(nested(xs))
print(inner(xs))
print(words(" Alpha, beta ,,GAMMA "))
print(flags(xs))
print(mixed())
print(pairs(xs))
print(sets(xs))
print(dicts(xs))
print(shadow(xs))
print(floats())
