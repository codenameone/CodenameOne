# Dicts the transpiler follows from a display: typed reads, and every way a
# dict can leave what it follows.


def counts(words):
    d = {}
    for w in words:
        if w in d:
            d[w] += 1
        else:
            d[w] = 1
    return d


def total(d, keys):
    t = 0
    for k in keys:
        try:
            t += d[k]
        except KeyError:
            t -= 1
    return t


def poison(d):
    d["x"] = "str"


def nones():
    d = {1: None, 2: 5}
    return d[1], d[2], d.get(3), d.get(3, 7), 1 in d, 3 in d, len(d)


def defaults():
    d = {}
    d.setdefault("a", 1)
    d.setdefault("a", 2)
    return d["a"], d.get("b", 0) + 1


def escaped():
    d = {"a": 1}
    poison(d)
    return d["x"] + "!"


def aliased():
    d = {"k": 1}
    e = d
    e["k"] = 2.5
    return d["k"] * 2


def in_list():
    d = {"k": 1}
    box = [d]
    box[0]["k"] = "s"
    return d["k"]


def missing():
    d = {1: 2}
    try:
        return d[3]
    except KeyError as e:
        return "KeyError " + repr(e.args[0])


c = counts("a b a c b a".split())
print(sorted(c.items()), total(c, ["a", "b", "z"]))
print(nones())
print(defaults())
print(escaped(), aliased(), in_list(), missing())
