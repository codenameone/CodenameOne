# Typed int results that are big ints or Long.MIN_VALUE, consumed bare.


def fib(n: int) -> int:
    if n < 2:
        return n
    return fib(n - 1) + fib(n - 2)


def big(n: int) -> int:
    if n == 0:
        return 1 << 70
    return big(n - 1) * 3 + n


def two(n: int) -> int:
    return big(n) + big(n + 1)


def edge(x: int) -> int:
    return x - 9223372036854775807 - 1


def climb(n: int) -> int:
    if n == 0:
        return 4611686018427387904
    return climb(n - 1) + 4611686018427387904


def boom(n: int) -> int:
    if n > 3:
        raise ValueError("boom " + str(n))
    return 1 << 80


def mixed(n: int) -> int:
    return boom(1) + boom(n)


def cmp(n: int) -> bool:
    return big(n) < big(n + 1) - fib(n)


def main() -> None:
    print(fib(20), fib(1), fib(0))
    print(two(0), two(5))
    print(edge(0), edge(1), edge(-1) + 1)
    for i in range(4):
        print(i, climb(i))
    caught = 0
    for i in range(1000):
        try:
            mixed(5)
        except ValueError:
            caught += 1
    print(caught, mixed(2))
    print(cmp(3), two(2) - two(1))


main()
