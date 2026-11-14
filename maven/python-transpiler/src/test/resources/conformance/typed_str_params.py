# Functions whose annotations name str, int and bytes, called with what the
# annotation says, with something else, and with nothing the build can tell.


def lex(sql: str) -> list:
    out = []
    i = 0
    while i < len(sql):
        start = i
        ch = sql[i]
        if ch.isspace():
            i += 1
            while i < len(sql) and sql[i].isspace():
                i += 1
            out.append(("ws", sql[start:i]))
            continue
        if sql.startswith("--", i):
            nl = sql.find("\n", i + 2)
            i = len(sql) if nl == -1 else nl + 1
            out.append(("comment", sql[start:i]))
            continue
        if ch in ("'", '"'):
            quote = ch
            i += 1
            while i < len(sql):
                if sql[i] == quote:
                    i += 1
                    break
                i += 1
            else:
                raise ValueError(f"Unterminated {quote} quoted token")
            out.append(("string", sql[start:i]))
            continue
        i += 1
        while i < len(sql) and not sql[i].isspace() and sql[i] not in "'\"":
            i += 1
        out.append(("word", sql[start:i]))
    return out


def kinds(s: str):
    return [s.isspace(), s.isdigit(), s.isdecimal(), s.isnumeric(), s.isalpha(), s.isalnum()]


def probe(s: str, sub: str, a: int, b: int):
    return [s.find(sub, a), s.rfind(sub, a), s.find(sub, a, b), s.rfind(sub, a, b),
            s.startswith(sub, a), s.endswith(sub, a), s.startswith(sub, a, b), s.endswith(sub, a, b)]


def at(s: str, i: int):
    try:
        return s[i]
    except IndexError as e:
        return "IndexError: " + str(e)
    except TypeError as e:
        return "TypeError"


def byte_at(data: bytes, i: int):
    try:
        return data[i]
    except IndexError as e:
        return "IndexError: " + str(e)


def repeat(text: str, n: int):
    return text * n


def total(data: bytes):
    t = 0
    for b in data:
        t += b
    return t


class Text(str):
    pass


values = {"sql": "select 'a b' -- note\nfrom \"t\"  where x", "n": 3}
print(lex(values["sql"]))
print(lex("plain words only"))
print(lex(Text("a 'b' c")))
try:
    lex("bad 'never closed")
except ValueError as e:
    print("ValueError:", e)
for bad in (None, 7, b"bytes", ["a", " ", "b"]):
    try:
        print(lex(bad))
    except (TypeError, AttributeError) as e:
        print(type(e).__name__)

for s in ("", " \t\n", "123", "²", "٣", "abc", "abc1", "a b", "½"):
    print(ascii(s), kinds(s))

text = "abcabc\U0001f600abcabc"
for a in (-20, -3, 0, 2, 7, 13, 99):
    for b in (-20, -2, 0, 5, 13, 99):
        print(a, b, probe(text, "abc", a, b), probe(text, "", a, b), probe("abcabc", "bc", a, b))
print(probe(values["sql"], "from", values["n"], 2 ** 70))
for i in (0, 5, 6, 7, 12, 13, -1, -7, -13, -14, 2 ** 70, True):
    print(ascii(at(text, i)), ascii(at("abc", i)))
print(at(values["sql"], values["n"]), at(values["sql"], "x"), at([1, 2, 3], 1))
print(repeat("ab", 3), repeat([1], 2), repeat(values["sql"][:2], values["n"]))
print(total(b"\x01\x02\xff"), total(bytearray(b"\x05\x06")), total([1, 2, 3]))
for i in (0, 2, -1, 3, -4, 2 ** 70, True):
    print(byte_at(b"abc", i), byte_at(values["sql"].encode(), i))
print("abc".count("", 5), "abc".count("", 3), "abc".count("", 2, 1), "abc".count("b", -2 ** 70, 2 ** 70))
big = 2 ** 70
print("abcdef"[1:big], "abcdef"[-big:2], "abcdef"[big:], [1, 2, 3][-big:big], (1, 2)[:big], b"xyz"[-big:big])
for seq in ([1, 2], (1, 2), range(3), bytearray(b"ab")):
    for i in (big, -big):
        try:
            seq[i]
        except IndexError as e:
            print("IndexError:", e)

