# Name resolution, globals, closures in loops, recursion and big ints.
G = 10
def read_global():
    return G
def write_global():
    global G
    G += 1
    return G
print(read_global(), write_global(), G)

def make_adders():
    return [lambda x, i=i: x + i for i in range(3)], [lambda x: x + i for i in range(3)]
early, late = make_adders()
print([f(10) for f in early], [f(10) for f in late])

def outer():
    v = "outer"
    def mid():
        def inner():
            return v
        return inner()
    return mid()
print(outer())

class K:
    attr = 5
    doubled = attr * 2
    items = [attr + i for i in range(2)] if False else None
    def get(self):
        return self.attr
print(K.doubled, K().get(), K.items)

def unbound():
    try:
        print(local_var)
    except UnboundLocalError as e:
        print("unbound:", e)
    local_var = 1
unbound()

def fact(n):
    return 1 if n <= 1 else n * fact(n - 1)
print(fact(30), fact(25) // fact(23), 2 ** 64 - 1, -(2 ** 63), (2 ** 64) % 1000007, 10 ** 20 // 3)
print(int("123456789012345678901234567890") * 3, 7 ** 77 % 13, abs(-2 ** 70), hex(2 ** 70), (1 << 100) >> 98)
print(9223372036854775807 + 1, -9223372036854775808 - 1, 3037000500 * 3037000500, divmod(-7, 2), -7 % 2)
def ack(m, n):
    if m == 0:
        return n + 1
    if n == 0:
        return ack(m - 1, 1)
    return ack(m - 1, ack(m, n - 1))
print(ack(2, 3))

def deco(fn):
    def wrapper(*a, **k):
        return "<" + str(fn(*a, **k)) + ">"
    return wrapper

@deco
def hello(name, punct="!"):
    return "hi " + name + punct
print(hello("bob"), hello("al", punct="?"), hello.__name__)

def counter_gen():
    state = {"n": 0}
    def step():
        state["n"] += 1
        return state["n"]
    return step
st = counter_gen(); st(); print(st())
x = 1
def shadow(x=x):
    x = x + 1
    return x
print(shadow(), x)
if __name__ == "__main__":
    print("main module")


def unbound_paths(flag):
    out = []
    x = 1
    for i in range(2):
        try:
            out.append(x)
            del x
        except UnboundLocalError:
            out.append("unbound")
    if flag:
        y = 2
    try:
        out.append(y)
    except UnboundLocalError:
        out.append("no y")
    e = 5
    try:
        raise ValueError("v")
    except ValueError as e:
        out.append(str(e))
    try:
        out.append(e)
    except UnboundLocalError:
        out.append("e cleared")
    try:
        z = int("q")
    except ValueError:
        pass
    try:
        out.append(z)
    except UnboundLocalError:
        out.append("no z")
    while True:
        w = 3
        break
    out.append(w)
    return out


print(unbound_paths(True), unbound_paths(False))
