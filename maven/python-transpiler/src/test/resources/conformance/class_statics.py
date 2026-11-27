BASE = 40


def big():
    return 1 << 70


class Point:
    def __init__(self, x):
        self.x = x


class Config:
    LIMIT = BASE + 2
    HUGE = big()
    NAME = "cfg-" + str(BASE)
    RATIO = BASE / 8
    ORIGIN = Point(3)

    def limit(self):
        return self.LIMIT + 1

    def huge(self):
        return self.HUGE

    @classmethod
    def name(cls):
        return cls.NAME


c = Config()
print(Config.LIMIT, Config.LIMIT * 2, c.limit())
print(Config.HUGE, c.huge() + 1)
print(Config.NAME, Config.name(), Config.RATIO)
print(Config.ORIGIN.x)


class Shade:
    def __init__(self, n):
        self.n = n

    def bright(self):
        return Shade.DARK.n + self.n


Shade.DARK = Shade(3)
Shade.LABEL = "shade"
print(Shade.DARK.n, Shade.LABEL, Shade(4).bright())
