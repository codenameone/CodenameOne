class A:
    pass


a = A()
a.x = 1
a.y = 2
a.z = 3
print(a.__dict__.pop("x", None), a.__dict__.pop("q", 5), "x" in a.__dict__, hasattr(a, "x"), a.__dict__)
print(a.__dict__.popitem(), hasattr(a, "z"), sorted(vars(a)))
a.__dict__.clear()
print(hasattr(a, "y"), a.__dict__)
a.w = 4
print(a.w, a.__dict__)
try:
    a.__dict__.pop("nope")
except KeyError as e:
    print("KeyError", e)
a.v = 9
del a.__dict__["v"]
print(hasattr(a, "v"), a.__dict__)
