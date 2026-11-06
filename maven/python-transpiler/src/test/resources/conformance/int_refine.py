def down(n: int) -> int:
    j: int = n
    c: int = 0
    while j > -5:
        j -= 3 if j < 100 else j // 2
        c += 1
    return j * 1000 + c


def up(x: int) -> int:
    i: int = x
    while i < 2 ** 63 + 5:
        i += 2 ** 62
    return i


def walk(a: list, j: int) -> int:
    s: int = 0
    while j >= 0:
        s += a[j]
        j -= 1
    return s + j


def back(a: list) -> tuple:
    j: int = -1
    t: int = 0
    while True:
        t += a[j]
        j -= 1
        if j < -len(a):
            break
    return t, j


def clamp(v: int) -> int:
    if v > 2 ** 63:
        return v - 2 ** 64
    if v < -10:
        v = v * v * v
    elif not (v != 3):
        v = v << 70
    return v + 1


def mixed(n: int) -> int:
    i: int = 0
    k: int = n
    while i < n and k > 0:
        i += 1
        k -= 2
    return i * 100 + k


def fib(n: int) -> int:
    if n < 2:
        return n
    return fib(n - 1) + fib(n - 2)


def tri(n: int) -> int:
    if n <= 0:
        return 0
    return n + tri(n - 1)


print(down(10), down(-100), down(2 ** 63 - 1), down(2 ** 70))
print(up(0), up(-(2 ** 64)), up(2 ** 63 + 4))
print(walk([1, 2, 3, 4], 3), walk([5], -2))
print(back([1, 2, 3]))
print(clamp(2 ** 63 + 1), clamp(-11), clamp(3), clamp(-2 ** 62), clamp(0))
print(mixed(10), mixed(0), mixed(7), mixed(-2 ** 64))
print(fib(20), fib(-3), fib(True), fib(2 ** 65 // 2 ** 64))
print(tri(100), tri(-5), tri(True))
