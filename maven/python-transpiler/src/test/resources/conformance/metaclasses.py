class Registry(type):
    classes = []

    def __new__(mcs, name, bases, ns, **kw):
        ns["tag"] = name.lower()
        cls = super().__new__(mcs, name, bases, ns)
        mcs.classes.append(name)
        return cls

    def __init__(cls, name, bases, ns, **kw):
        super().__init__(name, bases, ns)
        cls.extra = kw

    def describe(cls):
        return "class " + cls.__name__

    @property
    def upper(cls):
        return cls.__name__.upper()


class Base(metaclass=Registry):
    def hello(self):
        return "hello from " + type(self).__name__


class Child(Base, flavor="mint"):
    def hello(self):
        return super().hello() + "!"


print(Registry.classes, Base.tag, Child.tag, Child.extra, Base.extra)
print(type(Base).__name__, type(Child) is Registry, isinstance(Child, Registry), isinstance(Child, type))
print(Child().hello(), Child.describe(), Child.upper)


class Counted(type):
    def __call__(cls, *args, **kw):
        cls.made = getattr(cls, "made", 0) + 1
        return super().__call__(*args, **kw)

    def __len__(cls):
        return cls.made

    def __iter__(cls):
        return iter(range(cls.made))

    def __getitem__(cls, i):
        return cls.__name__ + str(i)

    def __repr__(cls):
        return "<counted " + cls.__name__ + ">"

    def __contains__(cls, x):
        return x == "yes"


class P(metaclass=Counted):
    def __init__(self, x):
        self.x = x


a = P(1)
b = P(x=2)
print(a.x, b.x, len(P), list(P), P[3], repr(P), "yes" in P, "no" in P)


class Plugin:
    registry = {}

    def __init_subclass__(cls, name=None, **kw):
        super().__init_subclass__(**kw)
        Plugin.registry[name or cls.__name__] = cls


class A(Plugin, name="alpha"):
    pass


class B(Plugin):
    pass


print(sorted(Plugin.registry))


class Field:
    def __set_name__(self, owner, name):
        self.name = "_" + name
        self.owner = owner.__name__

    def __get__(self, obj, objtype=None):
        return getattr(obj, self.name, "unset")

    def __set__(self, obj, value):
        setattr(obj, self.name, value)


class Rec:
    title = Field()


r = Rec()
print(Rec.__dict__["title"].name, Rec.__dict__["title"].owner, r.title)
r.title = "Dune"
print(r.title, r._title, type(Rec.title).__name__ if False else "ok")


class Lazy:
    def __init__(self, fn):
        self.fn = fn

    def __get__(self, obj, objtype=None):
        if obj is None:
            return self
        v = self.fn(obj)
        obj.__dict__[self.fn.__name__] = v
        return v


class Calc:
    calls = 0

    @Lazy
    def answer(self):
        Calc.calls += 1
        return 42


cc = Calc()
print(cc.answer, cc.answer, Calc.calls, type(Calc.answer).__name__, "answer" in vars(cc))


class Box:
    def __class_getitem__(cls, item):
        return cls.__name__ + "[" + item.__name__ + "]"


print(Box[int], list[int] is not None)

Dyn = type("Dyn", (Base,), {"x": 5, "hello": lambda self: "dyn"})
print(Dyn.x, Dyn().hello(), Dyn.tag, type(Dyn).__name__)

bases = (Plugin,)


class S(*bases, name="star"):
    pass


print("star" in Plugin.registry, S.__mro__[1].__name__)

try:
    class Q(x=1):
        pass
except TypeError as e:
    print("TypeError", e)
