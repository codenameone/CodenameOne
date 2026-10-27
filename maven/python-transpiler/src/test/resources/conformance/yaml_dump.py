import yaml
import datetime


def out(label, text):
    print("== " + label)
    print(text, end="")
    print("<<")


# ---- scalars
for v in [None, True, False, 0, 42, -7, 10 ** 20, 3.14, -0.5, 1e16, 1e-7, 1.0, float("inf"),
          float("-inf"), float("nan"), "plain", "", " lead", "trail ", "a: b", "a #b", "#c",
          "- x", "-", "?", "? x", ":", "x:", "yes", "No", "on", "null", "~", "true", "123",
          "1.5", "0x1f", "1e3", "2001-01-01", "1:30", "<<", "=", "@at", "`tick", "%pct", "!bang",
          "&amp", "*star", "|pipe", ">gt", "'q'", '"dq"', "a'b", 'a"b', "[x]", "{x}", "a,b",
          "tab\there", "nl\nx", "two\n\nlines", "end\n", "\nstart", "caf\u00e9", "\u4e2d\u6587",
          "bell\x07", "nul\x00", "---", "...", "--- x", "a  b", "a\n b", " ", "x" * 100,
          "word " * 30, "\u2028sep", "\x85next"]:
    out(ascii(v), yaml.safe_dump(v))
out("unicode allowed", yaml.safe_dump("caf\u00e9 \u4e2d\u6587 \U0001f600", allow_unicode=True))
out("unicode escaped", yaml.safe_dump("caf\u00e9 \u4e2d\u6587 \U0001f600"))
out("bytes", yaml.safe_dump(b"\x00\x01binary data here"))
out("date", yaml.safe_dump(datetime.date(2020, 2, 29)))
out("datetime", yaml.safe_dump(datetime.datetime(2020, 2, 29, 12, 30, 45)))
out("datetime micro", yaml.safe_dump(datetime.datetime(2020, 2, 29, 12, 30, 45, 123456)))
out("datetime tz", yaml.safe_dump(datetime.datetime(2020, 2, 29, 12, 30, 45, tzinfo=datetime.timezone.utc)))

# ---- structures
data = {
    "name": "demo",
    "version": 1.5,
    "tags": ["a", "b", "c"],
    "nested": {"x": 1, "y": [1, 2, {"z": None}], "empty_list": [], "empty_dict": {}},
    "matrix": [[1, 2], [3, 4]],
    "flag": True,
    "text": "multi\nline",
    "none": None,
    "123": "numeric key",
    "long key " + "k" * 20: "v",
}
out("default", yaml.safe_dump(data))
out("unsorted", yaml.safe_dump(data, sort_keys=False))
out("flow", yaml.safe_dump(data, default_flow_style=True))
out("flow none", yaml.safe_dump(data, default_flow_style=None))
out("indent4", yaml.safe_dump(data, indent=4))
out("width20", yaml.safe_dump({"k": "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda"}, width=20))
out("width flow", yaml.safe_dump(list(range(40)), default_flow_style=True, width=40))
out("explicit start", yaml.safe_dump(data["tags"], explicit_start=True))
out("explicit both", yaml.safe_dump({"a": 1}, explicit_start=True, explicit_end=True))
out("version", yaml.safe_dump({"a": 1}, version=(1, 1)))
out("list of dicts", yaml.safe_dump([{"a": 1, "b": 2}, {"c": [1, 2]}, [], [[]], [{}]]))
out("list in list", yaml.safe_dump([[1, [2, 3]], [[4]]]))
out("tuple", yaml.safe_dump((1, 2, (3, 4))))
out("set", yaml.safe_dump({3, 1, 2}))
out("set flow", yaml.safe_dump({"s": {"a", "b"}}, default_flow_style=True))
out("mixed keys", yaml.safe_dump({1: "a", "b": 2, None: 3, True: 4}))
out("int keys", yaml.safe_dump({3: "c", 1: "a", 2: "b"}))
out("tuple key", yaml.safe_dump({(1, 2): "t"}))
out("complex key", yaml.safe_dump({"k" * 130: 1, "multi\nline": 2}))
out("dict in flow", yaml.safe_dump([{"a": [1, {"b": 2}]}], default_flow_style=True))
out("long plain", yaml.safe_dump(["word " * 40]))
out("long quoted", yaml.safe_dump({"k": "it's " * 30}))
out("long dq", yaml.safe_dump({"k": "tab\t " * 30}))

# ---- aliases
shared = [1, 2]
out("aliases", yaml.safe_dump({"a": shared, "b": shared, "c": {"d": shared}}))
a = {"n": 1}
b = {"n": 2}
out("alias order", yaml.safe_dump([a, b, b, a]))
rec = [1]
rec.append(rec)
out("recursive", yaml.safe_dump(rec))
d1 = datetime.date(2020, 1, 1)
out("date alias", yaml.safe_dump([d1, d1]))


class NoAlias(yaml.SafeDumper):
    def ignore_aliases(self, data):
        return True


out("no alias", yaml.dump({"a": shared, "b": shared}, Dumper=NoAlias))

# ---- dump / dump_all / Dumper
out("dump", yaml.dump({"b": [1, 2], "a": (1, 2)}))
out("dump flow", yaml.dump({"b": [1, 2], "a": "x"}, default_flow_style=True))
out("dump_all", yaml.dump_all([{"a": 1}, [1, 2], "plain", 5]))
out("safe_dump_all", yaml.safe_dump_all([1, "two", [3]], explicit_start=True))
out("dump_all explicit", yaml.dump_all([{"a": 1}, {"b": 2}], explicit_start=True, explicit_end=True))
out("dump scalar dumper", yaml.dump("hello", Dumper=yaml.Dumper))
out("default_style", yaml.safe_dump({"a": "b", "c": [1, "d"]}, default_style='"'))
out("default_style sq", yaml.safe_dump({"a": "b", "c": [1, "d"]}, default_style="'"))
out("default_style lit", yaml.safe_dump({"a": "b\nc", "d": "x"}, default_style="|"))
out("default_style fold", yaml.safe_dump({"a": "b c d", "d": "x"}, default_style=">"))
print(type(yaml.safe_dump("x", encoding="utf-8")).__name__)
print(yaml.safe_dump("caf\u00e9", encoding="utf-8", allow_unicode=True))


class Buf:
    def __init__(self):
        self.parts = []

    def write(self, s):
        self.parts.append(s)


buf = Buf()
print(yaml.safe_dump({"k": [1, 2]}, buf))
print("".join(buf.parts), end="")


# ---- custom representers
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y


def point_rep(dumper, p):
    return dumper.represent_mapping("!point", {"x": p.x, "y": p.y})


yaml.add_representer(Point, point_rep, Dumper=yaml.SafeDumper)
out("custom rep", yaml.safe_dump({"p": Point(1, 2), "list": [Point(3, 4)]}))


def str_literal(dumper, s):
    style = "|" if "\n" in s else None
    return dumper.represent_scalar("tag:yaml.org,2002:str", s, style=style)


class LitDumper(yaml.SafeDumper):
    pass


LitDumper.add_representer(str, str_literal)
out("literal rep", yaml.dump({"a": "one\ntwo\n", "b": "single", "c": "x\ny"}, Dumper=LitDumper))


def seq_rep(dumper, s):
    return dumper.represent_sequence("!seq3", list(s), flow_style=True)


class Triple(tuple):
    pass


yaml.SafeDumper.add_representer(Triple, seq_rep)
out("flow seq rep", yaml.safe_dump({"t": Triple((1, 2, 3))}))


class IndentDumper(yaml.SafeDumper):
    def increase_indent(self, flow=False, indentless=False):
        return super().increase_indent(flow, False)


out("indented lists", yaml.dump({"a": [1, 2, {"b": [3]}], "c": 1}, Dumper=IndentDumper))

try:
    yaml.safe_dump(object())
except yaml.YAMLError as e:
    print(type(e).__name__, str(e)[:30])


class Obj(yaml.YAMLObject):
    yaml_tag = "!Obj"
    yaml_loader = yaml.SafeLoader
    yaml_dumper = yaml.SafeDumper

    def __init__(self, a, b):
        self.a = a
        self.b = b


out("yamlobject", yaml.safe_dump(Obj(1, [2, 3])))
o = yaml.safe_load("!Obj {a: 5, b: text}")
print(type(o).__name__, o.a, o.b)

# ---- round trips
samples = [
    {"a": 1, "b": [1, 2, 3], "c": {"d": "e"}},
    ["x", "y: z", "123", "", None, True, 1.5, "multi\nline\n", " pad "],
    {"k": "caf\u00e9", "date": datetime.date(2001, 1, 2), "dt": datetime.datetime(2001, 1, 2, 3, 4, 5)},
    [[], {}, [[]], [{}], {"a": []}],
    "just a string",
    42,
    {"nested": {"deep": {"deeper": [1, {"x": [2, 3]}]}}},
    b"\x00\xff bytes",
    {"set": {1, 2, 3}},
    ["tab\there", "quote'single", 'quote"double', "back\\slash", "null", "~", "yes"],
]
for i, s in enumerate(samples):
    text = yaml.safe_dump(s)
    back = yaml.safe_load(text)
    print(i, back == s)
    text2 = yaml.safe_dump(s, default_flow_style=True, allow_unicode=True)
    print(i, yaml.safe_load(text2) == s)
    text3 = yaml.safe_dump(s, width=30, indent=4)
    print(i, yaml.safe_load(text3) == s)
out("tuples dump", yaml.dump({"t": (1, 2)}))
print(yaml.load(yaml.dump({"t": (1, 2)}), Loader=yaml.FullLoader))
