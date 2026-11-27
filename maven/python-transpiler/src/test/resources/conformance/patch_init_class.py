class P:
    def __init__(self, a, c=3):
        self.v = (a, c)


def make():
    return P(4, 8).v


print(make())
P.__init__ = lambda self, a, c=0: setattr(self, "v", ("new", a, c))
print(make())
