# str, bytes and formatting.
s = "  Hello, World!  "
print(s.strip(), s.lstrip(), s.rstrip(), s.lower(), s.upper(), s.title(), s.swapcase())
print(s.strip().split(", "), "a,b,,c".split(","), "a b  c".split(), "a,b,c".split(",", 1), "a,b,c".rsplit(",", 1))
print("-".join(["x", "y", "z"]), "abc".replace("b", "BB"), "hello".find("l"), "hello".rfind("l"), "hello".index("e"))
print("hello".startswith("he"), "hello".endswith(("lo", "x")), "hello".count("l"), "42".isdigit(), "ab".isalpha(), " ".isspace())
print("abc".center(9, "*"), "abc".ljust(5) + "|", "abc".rjust(5), "7".zfill(3), "-7".zfill(4))
print("a\tb".expandtabs(4), "line1\nline2\r\nline3".splitlines(), "x".join("abc"), "Hello".casefold())
print("hello world".partition(" "), "a.b.c".rpartition("."), "abc".removeprefix("a"), "abc".removesuffix("c"))
print(repr("it's"), repr('say "hi"'), repr("both ' and \""), repr("\n\t\\"), repr("\x00\x7fÿĀ"))
print(str(1.0), str(1e16), str(1.5e-5), repr(0.1), str(123456789.123456789), str(float("inf")), str(-float("inf")))
print(f"{'left':<8}|{'right':>8}|{'mid':^8}|", f"{3:03d} {-3:+d} {3.5:e} {0.25:%} {1234.5:,.1f}")
print(f"{12:b} {12:o} {255:X} {65:c} {'s':5}|{7:<4}|", "{0}{1}{0}".format("a", "b"), "{x}-{y}".format(x=1, y=2))
print("%5s|%-5s|%05d|%x|%o|%e|%g|%%" % ("a", "b", 42, 255, 8, 12345.678, 0.0001))
w = "wörld\U0001F600"
print(len(w), w[5], w.encode("utf-8"), w.encode("utf-8").decode("utf-8") == w, list(w[-2:]))
b = b"bytes\x00\xff"
print(b, len(b), b[0], b[-1], b[1:3], b + b"!", b.hex(), bytes([65, 66]), b"abc".upper())
print(ord("€"), chr(0x1F600) == "\U0001F600", "€".encode(), str(b"x", "ascii"))
print("abc" < "abd", "Z" < "a", "a" * 0, "" in "abc", "bc" in "abc", sorted("hello"), max("hello"))
print("Mississippi".replace("ss", "SS", 1), "abcabc".strip("ac"), "{:>{w}}".format("x", w=4))
print(b"caf\xc3\xa9".decode(), "café".encode("latin-1"), b"\xff".decode("utf-8", "replace"), "xé".encode("ascii", "ignore"))
try:
    "é".encode("ascii")
except UnicodeEncodeError as e:
    print("encode error", type(e).__name__)
except UnicodeError as e:
    print("encode error (base)", type(e).__name__)
print(hash(2**61), hash(-2**63), hash(1.5), hash(-0.1), hash(1e300), hash(5e-324), hash((1, 2)), hash(()), hash((1, (2, 3.5))), hash(-1), hash(1.0) == hash(1) == hash(True))
print({(1, 2), (3, 4), (0, 0), (2, 1)}, {-1, 10, 100, 1000, 7, 2**40}, {1.5, 2.25, 0.5}, set(range(20, 0, -3)))
print(ascii("\N{EM DASH}\N{latin small letter a}\N{CJK UNIFIED IDEOGRAPH-4E00}\N{HANGUL SYLLABLE GAG}\N{GRINNING FACE}"), b"\N{x}", rb"\N")


def words(text):
    n = 0
    for w in text.split():
        n += len(w)
        print(repr(w), end=" ")
    print(n)
    for w in "".split():
        print("never")
    for w in "   ".split():
        print("never")


class Splitter:
    def split(self):
        return ["a", "b"]


words("  alpha\tbeta\n\ngamma delta \x1c eps  ")
for w in Splitter().split():
    print(w)

print(len("\ud800"), len("a\U0001F600b"), len("\udc00\ud800x"), len("x" * 40 + "\U0001F600"))
