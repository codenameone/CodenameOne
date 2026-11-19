def counts(n):
    d = {}
    hits = 0
    for i in range(n):
        key = i % 5
        try:
            hits += d[key]
        except KeyError:
            d[key] = 1
    return hits, sorted(d.items())


print(counts(20))


class KeyError(Exception):
    pass


def rebound():
    d = {}
    hits = 0
    try:
        for i in range(3):
            try:
                hits += d[i]
            except KeyError:
                hits += 100
    except LookupError as e:
        return "real", type(e).__name__, hits
    return "caught", hits


print(rebound())
