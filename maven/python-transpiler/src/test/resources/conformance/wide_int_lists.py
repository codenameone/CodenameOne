# Lists of ints are held four bytes an item while every item fits 32 bits;
# a wider one must turn the list into objects without losing a value.
def fill(n: int) -> list:
    a = [0] * n
    for i in range(n):
        a[i] = i * 3
    return a


def run() -> None:
    a = fill(8)
    a[3] = 2 ** 31
    a[4] = -2 ** 31 - 1
    a[5] = 2 ** 70
    print(a)
    b = [1, 2, 3]
    b.append(1 << 40)
    b.extend([7] * 3)
    print(b, sum(b))
    c = [5] * 4
    c[0] = 2147483647
    c[1] = -2147483648
    print(c, c[0] + c[1])
    d = [2 ** 33] * 3
    d.reverse()
    print(d, d[1:])
    e = fill(6)
    for i in range(6):
        e[i] = e[i] * 1000000000
    print(e)
    m = {}
    for k in range(5):
        m[k] = k * k
        m[k + 2 ** 62] = k
    print(m.get(3), m.get(2 ** 62 + 4), m.get(99), len(m))
    m.clear()
    m[True] = "t"
    print(m.get(1))


run()
