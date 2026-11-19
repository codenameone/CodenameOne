# Pure str operations on locals stored once fold at transpile time; a
# literal %-format with typed values is one concatenation.


def folded(n):
    line = "  \t<b>Hello World</b>\x1f "
    out = []
    for i in range(n):
        s = line.strip()
        a = s.find(">") + 1
        b = s.rfind("<")
        cell = s[a:b].replace("o", "0")
        if cell.startswith("He"):
            out.append("%s|%d|%s|%i" % (cell.lower(), i - 2, cell.upper(), len(cell)))
        out.append("[%s]" % s[-4:])
        out.append("%d%%" % (i * 3))
    return out


def rebound(k):
    t = "abc"
    r = []
    for i in range(k):
        r.append(t.upper())
        t = t + "d"
    return r


def maybe(flag):
    if flag:
        v = "xyz"
    try:
        return v.upper()
    except NameError as e:
        return "unbound"


def nonascii():
    w = " café "
    return w.strip().upper(), w.find("é")


def mixed(x, y):
    return "%s-%d-%s" % (x, y, 4.5)


print(folded(3))
print(rebound(3))
print(maybe(True), maybe(False))
print(nonascii())
print(mixed("q", -7), "%s" % 12, "<%s>" % "z", "%d" % -0)
