# str.split with no separator: runs of whitespace, maxsplit, edge strings.
CASES = ["", "   ", "a", " a ", "a b", "  a  b  c  ", "a\tb\nc\x0bd\x0ce\rf", "a\x1cb\x1fc",
         "x y z", "　lead", "trail\u0085", "one  two   three    four"]
for s in CASES:
    print(repr(s.split()))
    for m in (0, 1, 2, -1):
        print(" ", m, repr(s.split(None, m)), repr(s.split(maxsplit=m)), repr(s.rsplit(None, m)))


def words(text):
    n = 0
    longest = ""
    for w in text.split():
        n += 1
        if len(w) > len(longest):
            longest = w
    return n, longest


print(words("the quick  brown\tfox jumps\nover the lazy dog"))
print(words(""))
