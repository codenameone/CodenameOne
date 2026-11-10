# Five- and six-parameter functions take their arguments directly, with
# defaults filled for shorter calls, through every way of calling them.
import json


def f5(a, b, c, d, e):
    return (a, b, c, d, e)


def f6(a, b, c, d=4, e=5, f=6):
    return a + b + c + d + e + f


def f7(a, b, c, d, e, f, g=7):
    return [a, b, c, d, e, f, g]


class K:
    def m4(self, a, b, c, d):
        return a * b * c * d

    def m5(self, a, b, c, d, e=10):
        return (a, b, c, d, e)

    @staticmethod
    def s5(a, b, c, d, e):
        return a - b - c - d - e


print(f5(1, 2, 3, 4, 5), f6(1, 2, 3), f6(1, 2, 3, 0), f6(1, 2, 3, 0, 0), f6(1, 2, 3, 0, 0, 0))
print(f7(1, 2, 3, 4, 5, 6), f7(1, 2, 3, 4, 5, 6, 0), f6(1, 2, 3, f=0))
k = K()
print(k.m4(1, 2, 3, 4), k.m5(1, 2, 3, 4), k.m5(1, 2, 3, 4, 5), K.s5(20, 1, 2, 3, 4))
bm = k.m5
print(bm(5, 6, 7, 8), K.m5(k, 1, 1, 1, 1, 1), list(map(f5, "a", "b", "c", "d", "e")))
for bad in (lambda: f5(1, 2, 3, 4), lambda: f6(1, 2), lambda: k.m4(1, 2, 3, 4, 5)):
    try:
        bad()
    except TypeError as e:
        print("TypeError:", e)
print(json.dumps({"a": [1, 2]}, indent=None, separators=(",", ":"), sort_keys=True))


def kw3(a, b=2, c=3):
    return (a, b, c)


print(kw3(1, c=9), kw3(a=5), kw3(c=0, b=1, a=2), kw3(**{"a": 7, "c": 8}), k.m5(1, 2, 3, d=4))
print(f6(1, 2, c=3, f=0), f7(1, 2, 3, 4, 5, f=6))
for bad in (lambda: kw3(b=1), lambda: kw3(1, a=2), lambda: kw3(1, z=2), lambda: kw3(1, 2, 3, c=4)):
    try:
        bad()
    except TypeError as e:
        print("TypeError:", e)
