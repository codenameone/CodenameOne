def jh(s: str) -> int:
    h: int = 0
    for c in s:
        h = (h * 31 + ord(c)) & 0xFFFFFFFF
    return h - 0x100000000 if h > 0x7FFFFFFF else h


def chars(s: str) -> list:
    out = []
    for c in s:
        out.append(c)
    return out


def last(s: str) -> str:
    c = "none"
    for c in s:
        pass
    return c


def mixed(s: str) -> int:
    t: int = 0
    for c in s:
        t += ord(c)
        if c == "b":
            t += 1000
    return t


def label(i: int, flag: bool) -> str:
    return "item-" + str(i) + "-" + str(i * 31 ^ 0x55AA) + ":" + ("even" if flag else "odd") + str(flag) + str(2 ** 70)


for s in ("", "abc", "héllo", "\U0001F600x\U0001F601", "\ud800z"):
    print(jh(s), len(s), chars(s), last(s), mixed(s))
print(label(5, True), label(-12, False))
x = "\ud800z\U0001F600\udc00"
print(len(x), [ord(c) for c in x], ord(x[0]), x[1], ord(x[3]), x.find("z"), x[1:3] == "z\U0001F600")


def comp(s: str) -> list:
    return [ord(c) + 1 for c in s] + [c * 2 for c in s if c != "b"]


print(comp("abc"))
