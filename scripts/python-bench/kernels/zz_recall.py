# Scratch: the cost of one re call, taken apart. Each part adds one step to the previous.
import re
import time

P_FAIL = re.compile(r'h(\d+)')
P_NOGROUP = re.compile(r'd')
P_GROUP = re.compile(r'(d)')
P_NAMED = re.compile(r'(?P<x>d)')
P_THREE = re.compile(r'^(\n*)((?:.*[^\n])?)(\n*)$', flags=re.DOTALL)
S = "div"
N = 200000


def p_fail(n):
    t = 0
    for _ in range(n):
        if P_FAIL.match(S) is None:
            t += 1
    return t


def p_ok_nogroup(n):
    t = 0
    for _ in range(n):
        if P_NOGROUP.match(S) is not None:
            t += 1
    return t


def p_ok_group(n):
    t = 0
    for _ in range(n):
        if P_GROUP.match(S) is not None:
            t += 1
    return t


def p_ok_named(n):
    t = 0
    for _ in range(n):
        if P_NAMED.match(S) is not None:
            t += 1
    return t


def p_truth(n):
    t = 0
    for _ in range(n):
        if P_NOGROUP.match(S):
            t += 1
    return t


def p_end(n):
    t = 0
    for _ in range(n):
        t += P_NOGROUP.match(S).end()
    return t


def p_group1(n):
    t = 0
    for _ in range(n):
        t += len(P_GROUP.match(S).group(1))
    return t


def p_groups3(n):
    t = 0
    for _ in range(n):
        a, b, c = P_THREE.match("\nabc\n").groups()
        t += len(a) + len(b) + len(c)
    return t


def p_match3(n):
    t = 0
    for _ in range(n):
        if P_THREE.match("\nabc\n") is not None:
            t += 1
    return t


def p_loop(n):
    t = 0
    for _ in range(n):
        if S is not None:
            t += 1
    return t


def part(name, f, n, repeat):
    best = None
    r = None
    for _ in range(repeat):
        t0 = time.perf_counter()
        r = f(n)
        dt = time.perf_counter() - t0
        if best is None or dt < best:
            best = dt
    print("PART %-14s %7.1f ns/call  %s" % (name, best * 1e9 / n, r))


def main():
    part("loop", p_loop, N, 7)
    part("fail", p_fail, N, 7)
    part("ok_nogroup", p_ok_nogroup, N, 7)
    part("ok_group", p_ok_group, N, 7)
    part("ok_named", p_ok_named, N, 7)
    part("truth", p_truth, N, 7)
    part("end", p_end, N, 7)
    part("group1", p_group1, N, 7)
    part("match3", p_match3, N, 7)
    part("groups3", p_groups3, N, 7)


main()
