class weekday:
    def __init__(self, wd, n=None):
        self.weekday = wd
        self.n = n

    def __call__(self, n):
        if n == self.n:
            return self
        return self.__class__(self.weekday, n)

    def __repr__(self):
        return "wd(%d, %r)" % (self.weekday, self.n)


MO, TU, WE = [weekday(i) for i in range(3)]


def pick(w):
    return w


print(MO(+2), TU(-1), repr(WE))
print(pick(MO)(3))
