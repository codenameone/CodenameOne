def words(text):
    out = []
    for w in text.split():
        if w == "skip":
            continue
        if w == "stop":
            break
        out.append(w.upper())
    else:
        out.append("<end>")
    return out


def pieces(text, sep):
    n = 0
    last = None
    for p in text.split(sep):
        n += 1
        last = p
    return n, last


def empty_sep(text):
    try:
        for p in text.split(""):
            return "ran"
    except ValueError as e:
        return str(e)


def keep(text):
    w = "before"
    for w in text.split():
        pass
    return w


print(words("  alpha skip\tbeta gamma\n"))
print(words("a stop b"))
print(pieces("a,,b,", ","))
print(pieces("", ","))
print(pieces("x--y--z", "--"))
print(empty_sep("abc"))
print(keep("   "), keep(" one two "))
