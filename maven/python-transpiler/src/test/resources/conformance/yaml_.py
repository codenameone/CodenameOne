import yaml
import datetime


def show(label, value):
    print(label, ascii(value))


def err(label, text, loader=yaml.safe_load):
    try:
        loader(text)
        print(label, "no error")
    except yaml.YAMLError as e:
        print(label, type(e).__name__)
        print(str(e))


# ---- scalars and implicit resolution
doc = """
null1: ~
null2: null
null3: Null
null4:
t1: yes
t2: No
t3: on
t4: OFF
t5: TRUE
t6: false
i1: 42
i2: -17
i3: +5
i4: 0x1F
i5: 0b101
i6: 0o17
i7: 017
i8: 1_000_000
i9: 1:30
f1: 3.14
f2: -.5e+3
f3: .inf
f4: -.inf
f5: 6.8523015e+5
f6: 1_0.5
s1: hello world
s2: 'single ''quoted'' text'
s3: "double \\"quoted\\"\\t tab \\x41 \\u00e9 \\n newline"
s4: 1e3
s5: 12abc
s6: "123"
s7: '~'
d1: 2001-12-14
d2: 2001-12-14t21:59:43.10-05:00
d3: 2001-12-14 21:59:43.10
d4: 2002-12-14T21:59:43Z
"""
data = yaml.safe_load(doc)
for k in data:
    show(k, data[k])
print(data["f4"] == float("-inf"), data["f3"] == float("inf"))

# ---- block collections, flow collections
doc = """
- a
- - b1
  - b2
- key: value
  other: [1, 2, 3]
  nested: {x: 1, y: [a, b], z: {q: r}}
- ? complex
  : value
- !!str 123
- !!int "42"
- !!float 1
- !!bool "yes"
- !!null ""
- []
- {}
- [a, b, c]
-
- [1, [2, [3]]]
- {a: 1, b}
"""
show("seq", yaml.safe_load(doc))

# ---- block scalars
doc = """
lit: |
  line one
  line two

  after blank
fold: >
  folded text
  continues here

  new paragraph
    indented more
  back
strip: |-
  no trailing newline
keep: |+
  keep trailing

clip: >-
  stripped
  folded
ind: |2
    two extra
  base
last: end
"""
data = yaml.safe_load(doc)
for k in data:
    show(k, data[k])

# ---- comments and documents
doc = """# leading comment
a: 1   # trailing
# middle
b: 'x # not comment'
c: x#y
"""
show("comments", yaml.safe_load(doc))

docs = list(yaml.safe_load_all("---\na: 1\n---\n- 2\n- 3\n...\n---\nplain\n...\n--- >\n folded\n"))
show("docs", docs)
show("empty", yaml.safe_load(""))
show("onlycomment", yaml.safe_load("# nothing\n"))
show("explicit", yaml.safe_load("--- foo\n...\n"))
show("emptydocs", list(yaml.safe_load_all("---\n---\n")))

# ---- anchors, aliases, merge
doc = """
base: &base
  name: x
  size: 1
derived:
  <<: *base
  size: 2
multi:
  <<: [*base, {extra: true, name: y}]
list: &l [1, 2]
again: *l
scalar: &s hello
copy: *s
"""
data = yaml.safe_load(doc)
show("anchors", data)
print(data["list"] is data["again"])

doc = "a: &a [1, 2]\nb: *a\n"
data = yaml.safe_load(doc)
print(data["a"] is data["b"])
rec = yaml.safe_load("&x [1, *x]")
print(rec[1] is rec, len(rec))

# ---- explicit tags
show("binary", yaml.safe_load("!!binary |\n  AQID\n  BAU=\n"))
show("set", sorted(yaml.safe_load("!!set {a, b, c}")))
show("omap", yaml.safe_load("!!omap [a: 1, b: 2]"))
show("pairs", yaml.safe_load("!!pairs [a: 1, a: 2]"))
show("str", yaml.safe_load("!!str true"))
show("seqtag", yaml.safe_load("!!seq [1, 2]"))
show("maptag", yaml.safe_load("!!map {a: 1}"))
show("ts", yaml.safe_load("!!timestamp 2020-01-02"))
show("tagdirective", yaml.safe_load("%TAG !e! tag:yaml.org,2002:\n---\n!e!str 42\n"))
show("verbatim", yaml.safe_load("!<tag:yaml.org,2002:int> 5"))

# ---- multi-line flow / plain / quoted
doc = """
plain: this is
  a multi line
  plain scalar
quoted: "folded
  double"
single: 'folded

  single'
esc: "a\\
  b"
flow: [a,
  b,
  c]
"""
show("multiline", yaml.safe_load(doc))
show("urlish", yaml.safe_load("a: http://x.y/z?q=1\nb: [x:y, 'a: b']\nc: a:b\n"))
show("flowkeys", yaml.safe_load("{a: 1, 'b c': 2, ? d : 3, [1,2]: 4}") if False else yaml.safe_load("{a: 1, 'b c': 2, ? d : 3, e: }"))
show("unicode", yaml.safe_load("k: caf\u00e9 \u4e2d\u6587\n"))
show("bytesin", yaml.safe_load(b"a: 1\nb: [x]\n"))

# ---- custom constructors
yaml.add_constructor("!pt", lambda loader, node: tuple(loader.construct_sequence(node)), Loader=yaml.SafeLoader)
show("custom", yaml.safe_load("p: !pt [1, 2]"))


def upper(loader, node):
    return loader.construct_scalar(node).upper()


yaml.add_constructor("!up", upper, Loader=yaml.SafeLoader)
show("custom2", yaml.safe_load("[!up abc, !up 'x y']"))


def multi(loader, suffix, node):
    return suffix + ":" + str(loader.construct_scalar(node))


yaml.add_multi_constructor("!m/", multi, Loader=yaml.SafeLoader)
show("multi", yaml.safe_load("!m/one 1"))


class MyLoader(yaml.SafeLoader):
    pass


MyLoader.add_constructor("!x", lambda l, n: "X" + l.construct_scalar(n))
show("subclass", yaml.load("!x y", Loader=MyLoader))
show("full", yaml.load("a: 1", Loader=yaml.FullLoader))
show("plain Loader", yaml.load("a: [1, 2]", Loader=yaml.Loader))
show("unsafe", yaml.unsafe_load("a: 1"))
show("fullload", yaml.full_load("[1, 2]"))
show("tuple", yaml.load("!!python/tuple [1, 2]", Loader=yaml.FullLoader))
show("base", yaml.load("a: 1\nb: [2]", Loader=yaml.BaseLoader))

# ---- errors
err("e1", "a: b: c")
err("e2", "a: [1, 2")
err("e3", "a: 'unterminated")
err("e4", "- a\nb: c\n")
err("e5", "a: *missing")
err("e6", "{a: 1, b: 2")
err("e7", "a:\n  - b\n c: d")
err("e8", "a: !foo bar")
err("e9", "a: 1\n---\nb: 2\n")
err("e10", "[1, 2]]")
err("e11", "a: \"bad \\q escape\"")
err("e12", "key: value\n\tindented: x\n")
err("e13", "? [1, 2]\n: x\n- y")
err("e14", "a: &x 1\nb: &x 2\n")
err("e15", "{[1]: 2}")
err("e16", "a: b\n  c: d\n")
err("e18", "x: @foo")
err("e19", "a: 'x'y")
err("e20", "- - -\n  - ]")
err("e21", "!!binary 'not base64!'")
err("e22", "a: |\n  x\n y")
err("e23", "{a: 1, a: [}")
err("e24", "%YAML 2.0\n--- a")
err("e25", "- a\n- b\n  - c\n")
try:
    list(yaml.safe_load_all("a: 1\n---\n[\n"))
except yaml.MarkedYAMLError as e:
    print("lazy", type(e).__name__, e.problem, e.problem_mark.line, e.problem_mark.column)
try:
    yaml.safe_load("a: [")
except yaml.scanner.ScannerError as e:
    print("ns", "ScannerError")
except yaml.parser.ParserError as e:
    print("ns", "ParserError", e.context, e.problem)
print(issubclass(yaml.constructor.ConstructorError, yaml.MarkedYAMLError), issubclass(yaml.MarkedYAMLError, yaml.YAMLError))
print(yaml.__version__)
