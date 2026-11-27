# Calls on values whose class the build cannot pin to one: each is
# dispatched to the method of that name on whatever the value is, a
# method's own default where a class answers through __getattr__, a class
# attribute holding a class, descriptors on such values, and a class
# method called through the class with the instance as its argument.


class Shape:
    def area(self):
        return 0

    def describe(self, prefix="shape"):
        return prefix + ":" + str(self.area())


class Square(Shape):
    def __init__(self, n):
        self.n = n

    def area(self):
        return self.n * self.n

    @property
    def side(self):
        return self.n

    @classmethod
    def unit(cls):
        return cls(1)


class Circle:
    def __init__(self, r):
        self.r = r

    def area(self):
        return 3 * self.r * self.r

    def describe(self, prefix="circle"):
        return prefix + "!" + str(self.area())

    @property
    def side(self):
        return self.r * 2


class Proxy:
    """Answers every unknown name with a callable of its own."""

    def __init__(self, tag):
        self.tag = tag

    def __getattr__(self, name):
        def call(*args, **kwargs):
            return self.tag + "." + name + str(len(args))
        return call


class Forwarder:
    """Forwards unknown names to the object it wraps."""

    def __init__(self, inner):
        self.inner = inner

    def __getattr__(self, name):
        return getattr(self.inner, name)


class Suite:
    def __init__(self, items):
        self.items = items


class Loader:
    suiteClass = Suite

    def make(self, items):
        return self.suiteClass(items)


def pick(i):
    if i == 0:
        return Square(3)
    if i == 1:
        return Circle(2)
    if i == 2:
        return Proxy("p")
    return Forwarder(Square(4))


for i in range(4):
    v = pick(i)
    print(i, v.area(), v.describe(), v.describe(prefix="x"))

for i in (0, 1, 3):
    print("side", pick(i).side)

print(Square.unit().area(), Square.area(Square(5)), Shape.describe(Circle(1), "via"))
print(len(Loader().make([1, 2]).items))
mixed = [Square(2), Circle(1), Square(1)]
print(sum(s.area() for s in mixed), [s.describe() for s in mixed])
