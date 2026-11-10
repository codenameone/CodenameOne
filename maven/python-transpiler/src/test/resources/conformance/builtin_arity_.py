# Fixed-arity builtins answer exactly what the general path answers.
def err(f):
    try:
        f()
    except TypeError as e:
        print("TypeError:", e)


print(len("abc"), ord("A"), repr("x"), abs(-3), hex(255), isinstance(3, (str, int)))
print(getattr(3, "real"), getattr(3, "nope", "dflt"), issubclass(bool, int))
err(lambda: len())
err(lambda: len("a", "b"))
err(lambda: ord("ab"))
try:
    getattr(3, "nope")
except AttributeError as e:
    print("AttributeError:", e)
f = len
print(f([1, 2]), list(map(ord, "hi")))


class L(list):
    pass


x = L([3, 1, 2])
x.sort()
print(x, type(x).__name__, x.count(1), x.index(2))
