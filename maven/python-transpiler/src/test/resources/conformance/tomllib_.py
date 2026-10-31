import tomllib

DOC = '''
# a comment
title = "TOML Example"   # trailing
int1 = +99
int2 = 1_000
hex = 0xDEADbeef
oct = 0o755
bin = 0b1101
neg = -17
f1 = 3.14
f2 = -2e3
f3 = 6.626e-34
f4 = 1_0.5_0
inf1 = inf
inf2 = -inf
b1 = true
b2 = false
lit = 'C:\\Users\\x'
esc = "tab\\there \\u00e9 \\U0001F600 q\\"q"
ml = """
Roses
  are \\
     red"""
mll = \'\'\'
raw \\n text\'\'\'
quoted."key with space".bare = 1
"a.b" = 2
d1 = 1979-05-27
t1 = 07:32:00
t2 = 00:32:00.999999
dt1 = 1979-05-27T07:32:00
dt2 = 1979-05-27 07:32:00Z
dt3 = 1979-05-27T00:32:00.5-07:00
arr = [1, 2, 3,]
nested = [[1, 2], ["a", "b"], [
  # comment
  1.5,
]]
empty = []
inl = { x = 1, y = { z = "w" }, p.q = 3 }
emptyinl = {}

[owner]
name = "Tom"
dob = 1979-05-27T07:32:00-08:00

[database]
ports = [ 8000, 8001 ]
data = [ ["delta", "phi"], [3.14] ]

[servers.alpha]
ip = "10.0.0.1"

[servers.beta]
ip = "10.0.0.2"

[[products]]
name = "Hammer"
sku = 738594937

[[products]]

[[products]]
name = "Nail"
[products.meta]
tags = ["a"]

[[fruit]]
name = "apple"
[fruit.physical]
color = "red"
[[fruit.variety]]
name = "red delicious"
[[fruit.variety]]
name = "granny smith"

[[fruit]]
name = "banana"
[[fruit.variety]]
name = "plantain"

[x.y.z]
w = 1
[x]
v = 2
'''

data = tomllib.loads(DOC)


def show(d, indent=0):
    for k in d:
        v = d[k]
        if isinstance(v, dict):
            print(" " * indent + repr(k) + ":")
            show(v, indent + 2)
        else:
            print(" " * indent + repr(k), repr(v))


show(data)

print(tomllib.loads("a = nan")["a"] != tomllib.loads("a = nan")["a"])
print(tomllib.loads("") == {})
print(tomllib.loads("a = 1\r\nb = 2\r\n"))
print(tomllib.loads("a = 3.0", parse_float=lambda s: "F" + s))

BAD = [
    "a",
    "a =",
    "a = ",
    "a = 1 b = 2",
    "a = [1, 2",
    "a = {x = 1",
    "a = {x = 1,}",
    "[a",
    "[[a]",
    "a = 1\na = 2",
    "[a]\n[a]",
    "a = 1\n[a]",
    "a.b = 1\na = 2",
    "a = {x = 1}\n[a]",
    "a = {x = 1}\na.y = 2",
    "a = [1]\n[[a]]",
    "a = 'abc",
    'a = "abc',
    'a = "abc\n"',
    'a = "\\q"',
    'a = "\\u12"',
    'a = """abc',
    "a = 01",
    "a = 1__0",
    "a = _1",
    "a = 1.",
    "a = .5",
    "a = 0x",
    "a = 2023-13-45",
    "a = 12:61:00",
    "= 1",
    "a = 1 # \x01",
    "a = \x00",
    "[a]\nb = 1\n[a.b]",
    "[a.b]\n[a]\n[a]",
    "a = tru",
    "a = +inf2",
    "x = 'a' 'b'",
    "a=1 .b",
    "@",
    "a = \"\"\"x\"\"\"\"\"\"",
]
for s in BAD:
    try:
        r = tomllib.loads(s)
        print("OK", repr(s), r)
    except tomllib.TOMLDecodeError as e:
        print(repr(s), "->", e)

try:
    tomllib.loads(b"a = 1")
except TypeError as e:
    print("TypeError", e)
try:
    import io
    tomllib.load(io.StringIO("a = 1"))
except TypeError as e:
    print("TypeError", e)
import io
print(tomllib.load(io.BytesIO(b"a = 1\nb = 'x'")))
