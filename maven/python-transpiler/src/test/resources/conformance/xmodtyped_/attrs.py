"""A module that takes an instance's dict with vars(o): not its own
namespace, so its globals stay stable."""


class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y


def fields(p):
    return sorted(vars(p))


ORIGIN = Point(0, 0)
