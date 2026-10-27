# Pancake flipping over every permutation: list slicing and reversal.
from _harness import measure


def work(n):
    perm1 = list(range(n))
    count = [0] * n
    max_flips = 0
    checksum = 0
    sign = 1
    r = n
    while True:
        while r != 1:
            count[r - 1] = r
            r -= 1
        perm = perm1[:]
        flips = 0
        k = perm[0]
        while k:
            perm[:k + 1] = perm[k::-1]
            flips += 1
            k = perm[0]
        if flips > max_flips:
            max_flips = flips
        checksum += flips if sign == 1 else -flips
        while True:
            if r == n:
                return "%d %d" % (checksum, max_flips)
            p0 = perm1[0]
            perm1[:r] = perm1[1:r + 1]
            perm1[r] = p0
            count[r] -= 1
            if count[r] > 0:
                break
            r += 1
        sign = -sign


measure("fannkuch", work, 10, 3)
