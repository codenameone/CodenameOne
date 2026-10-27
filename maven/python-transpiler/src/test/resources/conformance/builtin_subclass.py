class Stack(list):
    def push(self, x):
        self.append(x)
        return self

    def peek(self):
        return self[-1]


s = Stack([1, 2])
s.push(3).push(4)
print(s, len(s), s.peek(), isinstance(s, list), type(s).__name__)
s += [5]
print(type(s).__name__, s, sum(s), sorted(s, reverse=True), list(s), s[1:3])


class Default(dict):
    def __missing__(self, key):
        v = self[key] = len(key)
        return v


d = Default(a=1)
print(d["a"], d["xyz"], d, dict(d), sorted(d.items()), "xyz" in d)


class Logged(dict):
    def __setitem__(self, key, value):
        print("set", key, value)
        super().__setitem__(key, value.upper())

    def __getitem__(self, key):
        return "<" + super().__getitem__(key) + ">"


lg = Logged()
lg["k"] = "v"
print(lg["k"], lg.get("k"), len(lg))


class Celsius(float):
    def __new__(cls, v):
        return super().__new__(cls, v)

    def to_f(self):
        return self * 9 / 5 + 32


c = Celsius(100)
print(c, c.to_f(), c + 1, c > 50, isinstance(c, float), repr(c))


class Color(str):
    def shout(self):
        return self.upper() + "!"

    def __repr__(self):
        return "Color(" + str.__repr__(self) + ")"


r = Color("red")
print(r, r.shout(), r == "red", r + "dish", len(r), [r], r.startswith("re"), {r: 1}["red"])


class Flags(int):
    def has(self, bit):
        return bool(self & bit)


f = Flags(6)
print(f, f.has(2), f.has(1), f + 1, f * 2, hex(f), int(f), [0, 1, 2, 3, 4, 5, 6, 7][f])


class Point(tuple):
    def __new__(cls, x, y):
        return super().__new__(cls, (x, y))

    @property
    def x(self):
        return self[0]


p = Point(3, 4)
print(p, p.x, p[1], len(p), p == (3, 4), hash(p) == hash((3, 4)))
x, y = p
print(x, y)


class Tags(set):
    def add_all(self, *xs):
        for x in xs:
            self.add(x)


t = Tags()
t.add_all("a", "b", "a")
print(sorted(t), len(t), "a" in t, sorted(t | {"c"}))

try:
    class Bad(bool):
        pass
except TypeError as e:
    print("TypeError", e)
