# Ints that typed code holds in a Java long (bounded by a mask, a range or a
# trip count) and ints that must not be: every result here has to equal
# CPython's, including the ones that leave 64 bits.
M32 = 0xFFFFFFFF
M64 = 0xFFFFFFFFFFFFFFFF


def i32(v: int) -> int:
    return ((v + 0x80000000) & M32) - 0x80000000


def i64(v: int) -> int:
    return ((v + 0x8000000000000000) & M64) - 0x8000000000000000


def wraps() -> None:
    a: int = 0x12345678
    b: int = i32(0x9E3779B9)
    for _ in range(1000):
        a = i32((a * 1103515245 + 12345) ^ ((b & M32) >> 3))
        b = i32((b + a) * 5 - (a << 7))
    print("wrap32", a, b)
    x: int = 0x0123456789ABCDEF
    y: int = -0x123456789
    for _ in range(1000):
        x = i64((x * 6364136223846793005 + 1442695040888963407) ^ ((y & M64) >> 7))
        y = i64((y ^ (x << 13)) + ((x & M64) >> 11))
    print("wrap64", x, y)


def accumulators() -> None:
    s: int = 0
    for i in range(100000):
        s += (i * 7919) & 0xFFFF
    print("bounded sum", s)
    big: int = 1
    for i in range(100):
        big = big * 3 + i
    print("grows past 64 bits", big)
    t: int = 0
    for i in range(70):
        t = t * 2 + 1
    print("doubling", t)
    d: int = 0
    for i in range(200000):
        d -= i
    print("down", d)
    n: int = 0
    k: int = 0
    while k < 50:
        n = n * 10 + 9
        k += 1
    print("while", n)


def signed_ops() -> None:
    for a in range(-7, 8, 3):
        for b in (-3, -1, 2, 5):
            print(a, b, a // b, a % b, a >> 1, a << 3, -a, ~a, a & b, a | b, a ^ b)
    v: int = -12345678901
    print((v & M64) >> 60, (v & M64) >> 1, v >> 3, v >> 70, (v & M32) >> 31)


def helpers_with_big_args() -> None:
    v: int = 1
    for i in range(80):
        v = v * 5
    print(i32(v), i64(v), i64(v * v), i32(-v))
    w: int = i64(v) >> 3
    print(w, i32(w // 7), w % 11)


def flags_and_mixed() -> None:
    c: int = 0
    for i in range(10):
        c += i if i % 2 == 0 else -i
    print("ifexp", c)
    s: int = 0
    for i in range(5):
        s += len("x" * i)
    print("len", s)
    t: int = 0
    for i in range(1, 6):
        t = (t + i) * i
    print("poly", t)


wraps()
accumulators()
signed_ops()
helpers_with_big_args()
flags_and_mixed()
