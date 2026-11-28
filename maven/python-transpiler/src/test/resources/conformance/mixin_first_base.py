# A field-free mixin first, the field-bearing base second: the class takes
# the second base's layout, lookups follow the C3 MRO.


class Dims:
    @property
    def area(self):
        return self.w * self.h

    @property
    def width(self):
        v = getattr(super(Dims, self), "width")
        if v is not None:
            return v
        return self._inherited("width")

    def _inherited(self, name):
        b = self._base
        if b is None:
            return None
        return getattr(b, name)

    @property
    def _base(self):
        raise NotImplementedError("subclass")

    def describe(self):
        return "dims %d" % self.area


class Shape:
    def __init__(self, w, h):
        self.w = w
        self.h = h

    def describe(self):
        return "shape"

    @property
    def width(self):
        return None

    def kind(self):
        return "shape-kind"


class Placeholder(Dims, Shape):
    @property
    def _base(self):
        return Master(1, 1)

    def __init__(self, w, h, idx):
        super().__init__(w, h)
        self.idx = idx


class Master(Shape):
    @property
    def width(self):
        return 99


class Placeholder2(Dims, Shape):
    @property
    def _base(self):
        return Master(1, 1)


class Slide(Dims, Shape):
    @property
    def _base(self):
        return Placeholder2(5, 6)


class Layout(Placeholder):
    def kind(self):
        return "layout " + super().kind()


p = Placeholder(2, 3, 7)
print(p.area, p.describe(), p.kind(), p.idx)
q = Layout(4, 5, 1)
print(q.area, q.describe(), q.kind(), q.idx, q.w)
print(isinstance(q, Dims), isinstance(q, Shape), isinstance(p, Layout))
print([k.__name__ for k in Layout.__mro__])
print(p.width, q.width, Placeholder2(1, 2).width)
print(Slide(1, 1).width)
