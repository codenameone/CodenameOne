# Spectral norm of an infinite matrix: integer arithmetic feeding float division.
from _harness import measure


def a(i, j):
    return 1.0 / (((i + j) * (i + j + 1) >> 1) + i + 1)


def mul_av(v, out):
    n = len(v)
    for i in range(n):
        s = 0.0
        for j in range(n):
            s += a(i, j) * v[j]
        out[i] = s


def mul_atv(v, out):
    n = len(v)
    for i in range(n):
        s = 0.0
        for j in range(n):
            s += a(j, i) * v[j]
        out[i] = s


def mul_atav(v, out, tmp):
    mul_av(v, tmp)
    mul_atv(tmp, out)


def work(n):
    u = [1.0] * n
    v = [0.0] * n
    tmp = [0.0] * n
    for _ in range(10):
        mul_atav(u, v, tmp)
        mul_atav(v, u, tmp)
    vbv = 0.0
    vv = 0.0
    for i in range(n):
        vbv += u[i] * v[i]
        vv += v[i] * v[i]
    return "%.9f" % ((vbv / vv) ** 0.5)


measure("spectral_norm", work, 480, 5)
