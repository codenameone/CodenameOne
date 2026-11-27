# Assignment expressions in typed bodies.
import re


def first_word(lines: list):
    out = []
    for line in lines:
        if (m := re.match(r"(\w+)", line)) and m.group(1) != "skip":
            out.append(m.group(1))
    return out, m is None


def chunks(data: str, size: int):
    pos = 0
    res = []
    while (piece := data[pos:pos + size]):
        res.append(piece)
        pos += size
    return res, piece


def side_order(xs):
    log = []

    def f(v):
        log.append(v)
        return v

    total = f(1) + (y := f(2)) * f(3)
    return total, y, log


def cell_walrus(seq):
    def peek():
        return last

    last = None
    found = [peek() for _ in seq if (last := _) or True]
    return found, peek()


print(first_word(["hello world", "skip me", "  ", "go"]))
print(chunks("abcdefg", 3))
print(side_order(None))
print(cell_walrus([1, 2, 3]))
