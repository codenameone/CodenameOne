# Untyped locals accumulated with += / -=: a long while they hold ints,
# Python's in-place operators on whatever else they come to hold.


def source(i):
    return [1, 2.5, "x", 2 ** 62, None][i % 5]


def mixed(n):
    acc = 0
    seen = []
    for i in range(n):
        v = source(i)
        if v is None:
            continue
        if isinstance(v, str):
            seen.append(acc)
            continue
        acc += v
        acc -= 1
    return acc, seen


def overflow():
    t = 0
    for i in range(6):
        t += 2 ** 62 if i % 2 == 0 else -1
        t += source(0)
    return t


def lists():
    acc = []
    alias = acc
    for i in range(3):
        acc += [i]
    return acc is alias, acc


def strings(words):
    out = ""
    n = 0
    for w in words:
        out += w
        n += len(w)
    return out, n


def raises():
    t = 0
    t += source(1)
    try:
        t += "s"
    except TypeError as e:
        return t, "TypeError"


print(mixed(12))
print(overflow())
print(strings(["ab", "c", "def"]))
print(raises())
print(lists())
