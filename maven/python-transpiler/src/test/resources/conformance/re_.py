import re

tests = [
    (r"a+", "caaab"), (r"(a)(b)?", "ac"), (r"(?P<word>\w+)\s(?P=word)", "hello hello world"),
    (r"\d{2,3}", "a12345"), (r"\d{,2}x", "123x"), (r"x{2}", "xxx"), (r"a{,}", "aaa"), (r"[a-c]+", "xxabcab"),
    (r"[^a-c]+", "abcxyzab"), (r"(?i)hello", "HeLLo"), (r"^abc$", "abc\n"), (r"(?m)^b$", "a\nb\nc"),
    (r"a.c", "a\nc"), (r"(?s)a.c", "a\nc"), (r"\bfoo\b", "a foo b"), (r"\Bfoo", "afoo"),
    (r"(a|ab)(c|bcd)(d*)", "abcd"), (r"(a*)*", "b"), (r"(a?)*", "b"), (r"(a*)+", "aab"),
    (r"(?:ab)+?c", "ababc"), (r"a*?b", "aaab"), (r"a{2,4}?", "aaaa"), (r"foo(?=bar)", "foobar"),
    (r"foo(?!bar)", "foobaz foobar"), (r"(?<=\$)\d+", "cost $42"), (r"(?<!\$)\b\d+", "$42 17"),
    (r"(?>a+)b", "aaab"), (r"a++b", "aaab"), (r"a++a", "aaaa"), (r"(\w)(\w)?\2", "abb"),
    (r"(a)?(?(1)b|c)", "ab"), (r"(a)?(?(1)b|c)", "c"), (r"[\d\s]+", "1 2 3x"), (r"[\w.-]+@[\w.-]+", "mail: a.b-c@x.org!"),
    (r"\x41é\101", "AéA"), (r"[\]]+", "]]a"), (r"[]a]+", "a]b"), (r"[-a]+", "-a-"), (r"[a-]+", "-a-"),
    (r"\.\*\+", ".*+"), (r"café", "CAFÉ"), (r"(?i)café", "CAFÉ"), (r"(?i)[a-z]+", "HeLLo"),
    (r"\w+", "naïve café"), (r"(?a)\w+", "naïve"), (r"(?x) a  b # comment\n c", "abc"),
    (r"(a)|(b)", "b"), (r"((a)(b))", "ab"), (r"\Aab\Z", "ab"), (r"$", "ab\n"), (r"x*", "aaa"),
    (r"(?i:a)b", "Ab"), (r"(?i:a)b", "AB"), (r"(?-i:a)b", "aB"), (r"\s+$", "abc  \t"),
]
for pat, s in tests:
    m = re.search(pat, s)
    if m:
        print(repr(pat), m.span(), repr(m.group()), m.groups(), m.lastindex, m.lastgroup)
    else:
        print(repr(pat), None)
    print(" ", re.match(pat, s) is not None, re.fullmatch(pat, s) is not None, re.findall(pat, s))

print(re.sub(r"x*", "-", "abxd"), re.sub(r"(\w+) (\w+)", r"\2 \1", "hello world"))
print(re.sub(r"(?P<a>\d)", r"<\g<a>\g<1>\g<0>>", "a1b22"), re.subn(r"a", "b", "aaa", count=2))
print(re.sub(r"\d+", lambda m: str(int(m.group()) * 2), "a1 b22 c333"), re.sub("", "-", "abc"))
print(re.sub(r"a", r"\n\t\\", "a") == "\n\t\\", re.sub("b", "x", "abcb", count=1))
print(re.split(r"\W+", "Words, words, words."), re.split(r"(\W+)", "Words, words, words."))
print(re.split(r"\W+", "Words, words, words.", maxsplit=1), re.split(r"x*", "axbc"), re.split("[a-f]", "0a3B9", flags=re.I))
print(re.findall(r"(\w)(\d)?", "a1b c3"), re.findall(r"\b\w", "one two"), re.findall("", "ab"))
print([m.span() for m in re.finditer(r"\w+", "ab cd  ef")], [m.group() for m in re.finditer(r"a*", "baac")])
p = re.compile(r"(?P<key>\w+)=(?P<val>\w*)")
m = p.search("x  k=v; z=")
print(p, p.pattern, p.groups, p.groupindex, m, m.groupdict(), m.group("key", 2), m["val"], m.start(2), m.end("key"))
print(m.span("val"), m.expand(r"\g<val>:\1"), m.pos, m.endpos, m.string, m.re is p, m.regs)
print(p.match("a=b", 0), p.search("zz a=b", 2), p.search("a=b c=d", 1, 5), p.fullmatch("a=b"), p.fullmatch("a=b c"))
print(re.compile("a", re.I | re.M), re.compile("a", re.S), re.I, re.I | re.M, re.compile(b"a+"), re.compile("x", re.X | re.A))
print(re.escape("a.b*c?d[e]f(g) h-i_j"), re.escape(b"a.b"), re.compile("a") == re.compile("a"))
print(re.search(rb"\d+", b"ab123").group(), re.findall(rb"[a-c]", b"abcd"), re.sub(rb"b", b"X", b"abc"))
print(re.match(r"(a)(b)", "ab").groups(), re.match(r"(a)(x)?", "a").groups("-"), re.match(r"(a)(x)?", "a").groupdict())
print(re.search(r"(?P<first>\w+) (?P<last>\w+)", "Jane Doe").lastgroup, re.purge())
bads = [r"a**", r"*a", r"(ab", r"ab)", r"[ab", r"\q", r"(?P<1>a)", r"(?P=x)", r"(?<=a+)b", r"a{3,2}", r"(?z)",
        r"\1(a)", "x(?i)y", r"(?P<n>a)(?P<n>b)", r"[z-a]", "abc\n(", r"(?", r"\x4"]
for b in bads:
    try:
        re.compile(b)
        print("compiled", repr(b))
    except re.error as e:
        print("error", repr(b), e, "|", e.msg, e.pos, e.lineno, e.colno)
try:
    re.search("a", b"a")
except TypeError as e:
    print("TypeError", e)
try:
    re.sub("(a)", r"\2", "a")
except re.error as e:
    print("error", e)
try:
    re.sub("a", r"\q", "a")
except re.error as e:
    print("error", e)
try:
    re.match("(a)", "a").group(5)
except IndexError as e:
    print("IndexError", e)
print(isinstance(re.error("x"), Exception), re.error.__name__, re.PatternError is re.error)
big = "ab" * 3000 + "c"
print(re.search(r"(ab)*c", big).span(), len(re.findall(r"a.", big)), re.match(r".*c", big).end())
print(re.match(r"(?:a|b)*?c", big).end(), bool(re.fullmatch(r"(?:ab)+c", big)))
