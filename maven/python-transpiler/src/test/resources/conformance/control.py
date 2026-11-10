# Control flow, functions, closures and exceptions.
def fib(n):
    a, b = 0, 1
    for _ in range(n):
        a, b = b, a + b
    return a

print([fib(i) for i in range(15)], fib(100))

def counter():
    count = 0
    def inc(by=1):
        nonlocal count
        count += by
        return count
    return inc

c = counter()
c(); c(5)
print(c())

def kw(a, b=2, *args, c, d=4, **kw):
    return (a, b, args, c, d, sorted(kw.items()))

print(kw(1, c=3), kw(1, 2, 3, 4, c=5, e=6, f=7), kw(*[1, 2], **{"c": 9}))
f = lambda x, y=10: x * y
print(f(3), f(3, 4), (lambda: 7)())

total = 0
for i in range(10):
    if i % 2:
        continue
    if i > 6:
        break
    total += i
else:
    total = -1
print(total)

n = 0
while n < 3:
    n += 1
else:
    print("while-else", n)

for i in []:
    pass
else:
    print("for-else on empty")

def risky(x):
    try:
        if x == 0:
            raise ValueError("zero")
        r = 10 // x
    except ValueError as e:
        return "value:" + str(e)
    except ZeroDivisionError:
        return "zdiv"
    else:
        return "ok:" + str(r)
    finally:
        print("finally", x)

print(risky(0), risky(5))
try:
    [][1]
except IndexError as e:
    print("index", e)
try:
    {}["k"]
except KeyError as e:
    print("key", repr(e))
try:
    try:
        1 / 0
    except ZeroDivisionError:
        raise
except ArithmeticError as e:
    print("re-raised", type(e).__name__, e)
try:
    undefined_name
except NameError as e:
    print(e)

def g():
    x = 1
    def h():
        return x + y
    y = 2
    return h()
print(g())

squares = {x: x * x for x in range(5) if x != 2}
print(squares, [y for x in [[1, 2], [3]] for y in x], {c for c in "hello"} == set("helo"))
print(sum(i * i for i in range(10)), max((len(w), w) for w in ["a", "bbb", "cc"]))
if (m := len("walrus")) > 3:
    print("walrus", m)
print([last := v for v in range(3)], last)
assert 1 + 1 == 2, "math"
try:
    assert False, "boom"
except AssertionError as e:
    print("assert", e)
