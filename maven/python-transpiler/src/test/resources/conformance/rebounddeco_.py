# A module that rebinds `staticmethod`: a method it decorates is an ordinary
# method, so a call of it from __init__ sees the instance half built.


def staticmethod(f):
    return f


class A:
    def __init__(self):
        self.seen = self.peek()
        self.x = 1

    @staticmethod
    def peek(self):
        return hasattr(self, "x")


class B:
    def __init__(self):
        self.seen = self.peek()
        self.x = 2

    @classmethod
    def peek(cls):
        return cls.__name__


a = A()
print(a.seen, a.x)
b = B()
print(b.seen, b.x)
