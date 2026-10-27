import json

data = {"name": "café", "n": [1, 2.5, -3e-7, True, False, None], "nested": {"b": 1, "a": [{}]},
        "big": 12345678901234567890, "esc": "tab\there \"q\" \\ \x01 \U0001F600"}
print(json.dumps(data))
print(json.dumps(data, ensure_ascii=False))
print(json.dumps(data, indent=2, sort_keys=True))
print(json.dumps([1, [2, 3]], separators=(",", ":")))
print(json.dumps({1: "a", 2.5: "b", None: "c", True: "d"}))
print(json.dumps(float("nan")), json.dumps(float("inf")), json.dumps(1e100), json.dumps(0.1))
print(json.dumps((1, 2)), json.dumps([]), json.dumps({}), json.dumps(""))
print(json.dumps([1, {}], indent="\t"))

back = json.loads(json.dumps(data))
print(back == data, back["esc"] == data["esc"])
print(json.loads('  {"a": [1, 2.0, -0, 1E3, "x\\u00e9\\ud83d\\ude00"], "b": null} '))
print(json.loads("[NaN, Infinity, -Infinity]"))
print(json.loads('{"a": 1, "a": 2}'))
print(json.loads('{"x": 1}', object_hook=lambda d: sorted(d.items())))
print(json.loads('{"x": 1, "y": 2}', object_pairs_hook=list))
print(json.loads("1.5", parse_float=lambda s: "F" + s), json.loads("7", parse_int=lambda s: int(s) * 2))
print(json.loads(b'[1]'))


class Point:
    def __init__(self, x, y):
        self.x, self.y = x, y


print(json.dumps(Point(1, 2), default=lambda o: o.__dict__))


class Enc(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, set):
            return sorted(o)
        return super().default(o)


print(json.dumps({"s": {3, 1, 2}}, cls=Enc))

for bad in ['', '[1,]', '{"a" 1}', '{"a": 1,}', '[1 2]', '"abc', 'nul', '{1: 2}', '[1] x', '"\\q"']:
    try:
        json.loads(bad)
    except json.JSONDecodeError as e:
        print(type(e).__name__, isinstance(e, ValueError), e, e.pos, e.lineno, e.colno)

try:
    json.dumps(object())
except TypeError as e:
    print("TypeError", e)
try:
    json.dumps(float("nan"), allow_nan=False)
except ValueError as e:
    print("ValueError", e)
a = []
a.append(a)
try:
    json.dumps(a)
except ValueError as e:
    print("ValueError", e)
try:
    json.dumps({(1, 2): 3})
except TypeError as e:
    print("TypeError", e)
print(json.dumps({(1, 2): 3, "k": 4}, skipkeys=True))
print(json.loads("\n\n  [1,\n  x]".replace("x", "2")))
try:
    json.loads("[1,\n  x]")
except ValueError as e:
    print(e)
