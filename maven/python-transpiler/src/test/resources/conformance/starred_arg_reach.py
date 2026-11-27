# A method whose only caller sits inside a starred argument must still be
# built: the call is reached through the spread value.


class Query:
    def __init__(self):
        self.parts = []

    def where(self, *conds):
        self.parts.extend(conds)
        return self

    def values(self, d):
        self.parts.append(sorted(d.items()))
        return self


class Table:
    def update(self):
        return Query()


def pick(registry, key):
    return registry[key]


class Session:
    def __init__(self):
        self.log = []

    def _pk_where(self, n):
        return ["id=%d" % n, "ver=%d" % (n * 2)]

    def _tags(self, n):
        return tuple("t%d" % i for i in range(n))

    def flush(self, registry, n):
        t = pick(registry, "t")
        q = t.update().where(*self._pk_where(n)).values({"a": n})
        self.log.append(q.parts)
        self.log.append(list(max(*self._tags(n + 2))))
        self.log.append(len([*self._tags(n)]))


s = Session()
reg = {"t": Table(), "u": 3}
s.flush(reg, 1)
s.flush(reg, 4)
for x in s.log:
    print(x)
