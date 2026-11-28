class LazyMeta(type):
    _populated = False

    @property
    def table(cls):
        if not cls._populated:
            cls._fill()
        return cls._table

    def _fill(cls):
        cls._table = {"a": 1, "b": 2}
        cls._populated = True

    @property
    def mode(cls):
        return cls._mode

    @mode.setter
    def mode(cls, v):
        cls._mode = v * 2

    def __getattr__(cls, name):
        if name.startswith("dyn_"):
            return name[4:]
        raise AttributeError(name)


class Lazy(metaclass=LazyMeta):
    def __init__(self, v):
        self.v = v

    def get(self, k):
        return type(self).table[k] + self.v


print(Lazy._populated, Lazy.table, Lazy._populated, Lazy(10).get("b"))
Lazy.mode = 3
print(Lazy.mode, Lazy._mode, "mode" in Lazy.__dict__, Lazy.dyn_hello)
try:
    Lazy.missing
except AttributeError as e:
    print("AttributeError", e)
try:
    Lazy(1).table
except AttributeError:
    print("instance does not see metaclass property")


class Singleton(type):
    def __init__(cls, name, bases, ns):
        super().__init__(name, bases, ns)
        cls._inst = None

    def __call__(cls, *a, **k):
        if cls._inst is None:
            cls._inst = super().__call__(*a, **k)
        return cls._inst


class Config(metaclass=Singleton):
    def __init__(self, n):
        self.n = n


c1 = Config(1)
c2 = Config(2)
print(c1 is c2, c1.n, type(c1).__name__, type(Config).__name__)


class Swap(type):
    def __new__(mcs, name, bases, ns):
        ns["made_by"] = mcs.__name__
        ns["double"] = lambda self: self.x * 2
        return super().__new__(mcs, name, bases, ns)


class SubSwap(Swap):
    def __new__(mcs, name, bases, ns):
        cls = super().__new__(mcs, name, bases, ns)
        cls.sub = True
        return cls


class S(metaclass=SubSwap):
    def __init__(self, x):
        self.x = x


class T(S):
    pass


print(S.made_by, S.sub, T(4).double(), type(T) is SubSwap, isinstance(T, Swap), issubclass(SubSwap, type))
Dyn = Swap("Dyn", (object,), {"x": 5})
print(Dyn.made_by, Dyn().double(), type(Dyn).__name__)
