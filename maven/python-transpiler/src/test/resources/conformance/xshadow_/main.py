from base import Base


class Child(Base):
    def __init__(self, i):
        super().__init__()
        self.idx = i
        self.kind = int
        self.fmt = "g"


b = Base()
print(b.show())
c = Child(5)
print(c.show())
print(Base.idx, Base.kind.__name__, isinstance(c, Base))
c.idx += 2
print(c.show(), b.show())
