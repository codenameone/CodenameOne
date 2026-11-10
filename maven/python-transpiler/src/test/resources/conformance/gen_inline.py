log = []

def numbers(n):
    i = 0
    while i < n:
        log.append("n%d" % i)
        yield i
        i += 1

def evens(it):
    for x in it:
        if x % 2 == 0:
            yield x

def scaled(it, k):
    for x in it:
        yield x * k

def interleave():
    out = []
    for v in numbers(3):
        log.append("b%d" % v)
        out.append(v)
    return out

print(interleave(), log)

def pipeline(n):
    total = 0
    for v in scaled(evens(numbers(n)), 3):
        total += v
    return total

del log[:]
print(pipeline(10), len(log))

def sums(n):
    a = sum(x * 3 for x in evens(numbers(n)))
    c = 1
    c += sum(1 for d in numbers(n) if d > 2)
    return a, c

print(sums(8))

def boom(n):
    for i in range(n):
        if i == 2:
            raise ValueError("at %d" % i)
        yield i

def catcher():
    seen = []
    try:
        for v in boom(5):
            seen.append(v)
    except ValueError as e:
        seen.append(str(e))
    return seen

print(catcher())

base = 100

def offset(n):
    for i in range(n):
        yield i + base

def clash():
    base = 1
    return [v for v in offset(2)], base

def noclash():
    r = []
    for v in offset(2):
        r.append(v)
    return r

print(clash(), noclash())

def early():
    r = []
    for v in numbers(100):
        if v > 3:
            break
        r.append(v)
    return r

print(early())

def nested_loops():
    r = []
    for a in numbers(2):
        for b in numbers(2):
            r.append((a, b))
    return r

print(nested_loops())

def leaked():
    for v in numbers(3):
        pass
    return v

print(leaked())

def tree(depth, fan):
    if depth == 0:
        return (depth, [])
    return (depth, [tree(depth - 1, fan) for _ in range(fan)])

def spread(depth, fan):
    return [depth * i + fan for i in range(fan) if i != depth]

print(tree(2, 2), spread(1, 3), spread(2, 3))
