# Positional calls that leave trailing parameters to their defaults, at every
# arity a function body is compiled for, through plain, method and stored
# references -- and the errors when too few are given.
def one(a=1):
    return a

def two(a, b=2):
    return (a, b)

def three(a, b=20, c=30):
    return (a, b, c)

def four(a, b, c=300, d=400):
    return (a, b, c, d)

def acc(x, into=[]):
    into.append(x)
    return into

class K:
    def m(self, a, b="b", c="c"):
        return (a, b, c)

fs = [one, two, three, four]
print(one(), one(5), fs[0](), fs[0](6))
print(two(1), two(1, 9), fs[1](7))
print(three(1), three(1, 2), three(1, 2, 3), fs[2](8), fs[2](8, 9))
print(four(1, 2), four(1, 2, 3), four(1, 2, 3, 4), fs[3](5, 6))
print(acc(1), acc(2), acc(3, []), acc(4))
k = K()
print(k.m(1), k.m(1, 2), K.m(k, 1), getattr(k, "m")(9))
for f, args in [(two, ()), (four, (1,)), (three, ())]:
    try:
        f(*args)
    except TypeError as e:
        print("TypeError", e)
