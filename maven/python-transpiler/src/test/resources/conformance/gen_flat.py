# Exceptions passing through generators that resume one another: each one
# finishes, PEP 479 applies, and a handler around a yield from still sees it.


def boom(n):
    for i in range(n):
        yield i
    raise ValueError("boom")


def stopper():
    yield 1
    raise StopIteration("early")


def relay(it):
    for x in it:
        yield x * 10


def chain(depth, inner):
    if depth == 0:
        yield from inner
    else:
        yield from chain(depth - 1, inner)


def guarded(inner):
    try:
        yield from inner
    except ValueError as e:
        yield "caught " + str(e)


def collect(g):
    out = []
    try:
        for v in g:
            out.append(v)
    except Exception as e:
        out.append(type(e).__name__ + ": " + str(e))
    return out


b = boom(2)
r = relay(b)
print(collect(r))
print(list(r), list(b))
print(collect(relay(stopper())))
c = chain(5, boom(3))
print(collect(c))
print(next(c, "done"))
print(collect(chain(3, stopper())))
print(collect(guarded(chain(2, boom(1)))))
print(collect(relay(guarded(boom(2)))))
g = relay(boom(1))
print(next(g))
try:
    next(g)
except ValueError as e:
    print("raised", e)
print(next(g, "finished"))


def selfish():
    yield next(me)


me = selfish()
print(collect(me))
print(sum(x for x in relay(relay(range(5)))))
