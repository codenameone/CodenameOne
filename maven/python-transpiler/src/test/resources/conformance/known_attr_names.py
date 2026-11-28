# setattr/getattr with a name taken from a fixed set of strings: each name
# is a store or read of its own, and the same name in another scope is
# another variable.


class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.tag = "p"

    def copy(self):
        c = Point(0, 0)
        for attr in ("x", "y"):
            setattr(c, attr, getattr(self, attr))
        return c

    def scaled(self, k):
        c = Point(0, 0)
        attr = "x"
        setattr(c, attr, getattr(self, attr) * k)
        # A comprehension's `attr` is its own variable, not the local above.
        [setattr(c, attr, getattr(self, attr) * k) for attr in ["y"]]
        return c

    def relabel(self):
        name = "tag"

        def inner():
            nonlocal name
            name = "x"

        inner()
        setattr(self, name, "relabelled")
        return self


class Animal:
    def speak(self):
        return "..."

    def eat(self):
        return "eats"


def loud(self):
    return "LOUD"


for meth in ("speak",):
    setattr(Animal, meth, loud)

p = Point(2, 3)
q = p.copy()
print(q.x, q.y, q.tag)
s = p.scaled(10)
print(s.x, s.y)
r = Point(5, 6).relabel()
print(r.x, r.y, r.tag)
a = Animal()
print(a.speak(), a.eat())
