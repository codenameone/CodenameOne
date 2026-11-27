class Desc:
    def __get__(self, inst, cls):
        if inst is None:
            return self
        return getattr(inst, "_fmt", "unset")

    def __set__(self, inst, value):
        inst._fmt = "set:" + str(value)


class Base:
    idx = 0
    kind = float
    fmt = Desc()

    def __init__(self):
        self.a = 1

    def show(self):
        return (self.idx, self.kind.__name__, self.fmt, self.a)
