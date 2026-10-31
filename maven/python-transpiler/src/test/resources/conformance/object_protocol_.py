import copy
import hmac


# __getattribute__ raising AttributeError falls back to __getattr__.
class Fallback:
    def __init__(self):
        self.real = 1

    def __getattribute__(self, name):
        if name.startswith("hidden"):
            raise AttributeError(name)
        return object.__getattribute__(self, name)

    def __getattr__(self, name):
        return "fallback:" + name


f = Fallback()
print(f.real, f.hidden_x, f.missing)
print(getattr(f, "hidden_y"), hasattr(f, "anything"))


class Counting:
    def __init__(self):
        object.__setattr__(self, "reads", 0)

    def __getattribute__(self, name):
        if name != "reads":
            object.__setattr__(self, "reads", object.__getattribute__(self, "reads") + 1)
        return object.__getattribute__(self, name)


c = Counting()
c.a = 5
_ = c.a
_ = c.a
print("reads", c.reads)


# Writing an unrelated class does not change what an instance reads.
class Plain:
    x = 1


p = Plain()
p.y = 2


class Other:
    pass


Other.z = 3
print(p.x, p.y)
Plain.x = 10
print(p.x)

# __dict__ with keys that are not strings.
o = Plain()
o.__dict__[1] = "one"
o.__dict__["name"] = "n"
print(sorted(map(str, o.__dict__.keys())), o.name)
print(hasattr(o, "1"), o.__dict__.get(1))
d = o.__dict__
del d[1]
print(1 in o.__dict__, list(o.__dict__))

# Mutating the instance while iterating its __dict__ raises, as for a dict.
o.a = 1
o.b = 2
try:
    for k in o.__dict__:
        o.__dict__["new_" + str(k)] = 0
    print("no error")
except RuntimeError as e:
    print("RuntimeError", e)
o2 = Plain()
o2.a = 1
o2.b = 2
seen = []
for k in o2.__dict__:
    seen.append(k)
    o2.a = 5
print(seen, o2.a)


# copyreg reached through a computed getattr.
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y


name = "__reduce" + "_ex__"
red = getattr(Point(1, 2), name)(2)
print(red[0].__name__, red[1][0] is Point)
q = copy.copy(Point(3, 4))
print(q.x, q.y)

# hmac.compare_digest on strings.
print(hmac.compare_digest("abc", "abc"), hmac.compare_digest("abc", "abd"))
try:
    hmac.compare_digest("café", "café")
except TypeError as e:
    print("TypeError", e)
try:
    hmac.compare_digest("abc", b"abc")
except TypeError:
    print("TypeError mixed")

# Deleting one key and adding another through the dict keeps the size: still an error.
o3 = Plain()
o3.a = 1
o3.b = 2
d3 = o3.__dict__
try:
    for k in d3:
        if k == "a":
            del d3["a"]
            d3["c"] = 3
    print("no error")
except RuntimeError as e:
    print("RuntimeError", e)
print(sorted(o3.__dict__), o3.c)

# A dict-level delete, then an attribute write that restores the old shape.
c = Plain()
c.a = 1
c.b = 2
dd = c.__dict__
print(len(dd))
del dd["b"]
c.b = 9
print(dd)
# popitem after an attribute was added behind the dict's back.
c.z = 5
print(dd.popitem(), sorted(c.__dict__), hasattr(c, "z"))


class Slotted:
    __slots__ = ("x",)


s = Slotted()
s.x = 1
try:
    s.__dict__
    print("has dict")
except AttributeError as e:
    print("AttributeError", e)


# The __dict__ view is one object per instance, and a counting store through
# it (d[k] = d.get(k, 0) + n, which plain dicts run with one probe) reaches
# the instance.
class Counted:
    def __init__(self):
        self.a = 1


def count_through_dict():
    c = Counted()
    print(c.__dict__ is c.__dict__, vars(c) is c.__dict__)
    d = c.__dict__
    k = "a"
    d[k] = d.get(k, 0) + 10
    print(c.a, d[k])
    k = "b"
    d[k] = d.get(k, 0) + 5
    print(c.b, sorted(d.items()))


count_through_dict()


def len_through_cached_dict():
    class C:
        pass
    o = C()
    o.a = 1
    d = o.__dict__
    print(len(d))
    o.x = 1
    print(len(o.__dict__), len(d))


len_through_cached_dict()
