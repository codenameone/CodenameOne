# Core values, operators and builtins.
print(1 + 2, 7 // 2, -7 // 2, 7 % -3, 2 ** 100, 10 / 4)
print(0.1 + 0.2, 1e300 * 10, -0.0, 3.0, 1.5e-7)
print(True + True, None, ..., not 0)
print("abc" * 3, "a" < "b", [1, 2] + [3], (1,) * 3)
print(1 < 2 < 3, 3 > 2 > 2, 1 == 1.0, 2 is 2)
print(0 or "x", 1 and 2, [] or [], None and 1)
s = "héllo w\U0001f600rld"
print(len(s), s[1], s[-1], s[::-1], s.upper(), repr(s))
x = [5, 3, 8, 1]
x.sort()
print(x, sorted(x, reverse=True), x[1:3], x[::2], min(x), max(x), sum(x))
d = {"b": 2, "a": 1}
d["c"] = 3
print(d, list(d), d.get("z", 0), "a" in d, len(d))
del d["b"]
print(d, {k: v * 2 for k, v in d.items()})
print({1, 2, 3} & {2, 3, 4}, sorted({3, 1, 2}), (1, 2) < (1, 3))
a, *b, c = range(6)
print(a, b, c)
print(f"{3.14159:.2f} {42:>6} {'x'!r} {255:#x} {1000000:,}")
print("%s-%d-%.3f" % ("a", 5, 2.5), "{} {}".format(1, "two"))
print(int("42") + int("ff", 16), float("1.5"), str(12), bool(""), abs(-3))
print(divmod(17, 5), round(2.675, 2), round(3.5), hex(255), bin(5), chr(65), ord("A"))
print(list(zip([1, 2], "ab")), list(enumerate("xy", 1)), list(map(str, [1, 2])))
print(any([0, 1]), all([]), list(filter(None, [0, 1, 2])), list(reversed([1, 2, 3])))
print(2 ** -1, 7 // 0.5, 1 << 70, -1 >> 3, ~5, 6 ^ 3, 6 | 3, 6 & 3)
print(isinstance(3, int), isinstance(True, int), type(3.0).__name__, type("") is str)
