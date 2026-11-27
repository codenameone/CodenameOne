# A class constant read through self is folded only while the whole program
# proves no instance holds the name and nothing wrote to the class: every
# way around that must still read what CPython reads.


class Base:
    SIZE = 64

    def size(self):
        return self.SIZE

    def twice(self):
        return self.SIZE + self.SIZE


class Small(Base):
    SIZE = 32


class Same(Base):
    pass


for o in (Base(), Small(), Same()):
    print(type(o).__name__, o.size(), o.twice())


# A computed setattr on the class itself, and on a class above it.
class Tunable:
    LIMIT = 5

    def limit(self):
        return self.LIMIT

    @classmethod
    def configure(cls, **kw):
        for k, v in kw.items():
            setattr(cls, k, v)


class TunableChild(Tunable):
    pass


t = Tunable()
c = TunableChild()
print("before", t.limit(), c.limit())
Tunable.configure(LIMIT=9)
print("after", t.limit(), c.limit())


# A store by name on one instance.
class Moded:
    MODE = "a"

    def mode(self):
        return self.MODE


m1 = Moded()
m2 = Moded()
m2.MODE = "b"
print("mode", m1.mode(), m2.mode())


# A computed store through the instance's dict.
class Kinded:
    KIND = 1

    def kind(self):
        return self.KIND


k = Kinded()
name = "KI" + "ND"
k.__dict__[name] = 2
print("kind", Kinded().kind(), k.kind())


# A class built at run time below a compiled one, with its own value.
class Wide:
    WIDTH = 3

    def width(self):
        return self.WIDTH


Wider = type("Wider", (Wide,), {"WIDTH": 7})
print("width", Wide().width(), Wider().width())
