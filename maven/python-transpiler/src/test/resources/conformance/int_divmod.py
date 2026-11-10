def dm(a: int, b: int):
    try:
        return (a % b, a // b, (a & 0xFF) % b + 1, a // b * 2)
    except ZeroDivisionError as e:
        return str(e)


M = -9223372036854775808
for a in (7, -7, 0, M, 2 ** 63 - 1, 2 ** 70, -(2 ** 70)):
    for b in (3, -3, 1, -1, 0, 2 ** 64, M):
        print(a, b, dm(a, b))


def scan(n: int) -> int:
    t: int = 0
    idx: int = 12345
    for i in range(n):
        t += (idx & 0x7FFFFFFF) % 97 - i // 7
        idx = idx * 31 + 7
    return t


print(scan(200))
