class Deco:
    def __init__(self, msg):
        self.msg = msg

    def __call__(self, arg):
        if isinstance(arg, type):
            orig = arg.__init__

            def init(self, *a, **k):
                print("init", self.__class__.__name__)
                orig(self, *a, **k)

            arg.__init__ = init
            return arg

        def wrapper(*a, **k):
            return (self.msg, arg(*a, **k))

        return wrapper


@Deco("m")
def twice(x):
    return x * 2


print(twice(4))


class Adder:
    def __init__(self, n):
        self.n = n

    def __call__(self, x):
        return x + self.n


a = Adder(3)
print(a(4), list(map(a, [1, 2])), sorted([3, 1, 2], key=Adder(-1)))
held = [a]
print(held[0](10))


class Sub(Adder):
    pass


fs = {"s": Sub(100)}
print(fs["s"](1))


@Deco("c")
class Box:
    def __init__(self, v):
        self.v = v


print(Box(9).v)
