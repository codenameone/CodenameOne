import pytest


class Box:
    size = 1


class Recorder:
    def __init__(self):
        self.calls = []

    def setattr(self, a, b, c=None):
        self.calls.append(("set", a, b, c))

    def delattr(self, a, b):
        self.calls.append(("del", a, b))


class Holder:
    def __init__(self, p):
        self.p = p


def main():
    patcher = pytest.MonkeyPatch()
    patcher.setattr(Box, "size", 2)
    print("set", Box.size)
    h = Holder(patcher)
    h.p.setattr(Box, "color", "red", raising=False)
    print("attr receiver", Box.color)
    h.p.delattr(Box, "color")
    print("deleted", hasattr(Box, "color"))
    r = Recorder()
    r.setattr("x", "y", 3)
    r.delattr("x", "y")
    print("other receiver", r.calls)
    patcher.undo()
    print("undone", Box.size, hasattr(Box, "color"))


main()
