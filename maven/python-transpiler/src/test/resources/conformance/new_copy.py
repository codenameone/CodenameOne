from copy import copy


class Dim:
    def __init__(self, index=0, width=1.0, worksheet=None):
        self.index = index
        self.width = width
        self.parent = worksheet

    def __copy__(self):
        cp = self.__new__(self.__class__)
        attrib = dict(self.__dict__)
        attrib["worksheet"] = attrib.pop("parent")
        cp.__init__(**attrib)
        return cp


class Col(Dim):
    pass


class Other:
    def __init__(self, a):
        self.a = a


d = copy(Col(3, 2.5, "ws"))
print(type(d).__name__, d.index, d.width, d.parent)
print(Other(1).a)


class Styled:
    def __init__(self, sheet, style=None):
        self.parent = sheet
        self._style = style or [0]


class Strictish:
    pass


class Dim2(Strictish, Styled):
    def __init__(self, worksheet, index=0, style=None):
        super().__init__(worksheet, style)
        self.index = index

    def __copy__(self):
        cp = self.__new__(self.__class__)
        attrib = {"worksheet": self.parent, "index": self.index}
        cp.__init__(**attrib)
        cp._style = list(self._style)
        return cp


class Row2(Dim2):
    def __init__(self, worksheet, index=0, style=None, height=3):
        super().__init__(worksheet, index, style)
        self.height = height


class Unrelated:
    def __init__(self, worksheet=None, index=None):
        self.w = worksheet


r = copy(Row2("ws2", 4))
print(type(r).__name__, r.parent, r.index, r.height, r._style)
print(Unrelated(1).w)


class Tagged:
    tag = "t"


class Mixed(Tagged, Dim2):
    def __init__(self, worksheet, index=0, style=None):
        super().__init__(worksheet, index + 100, style)


m = copy(Mixed("ws3", 1))
print(type(m).__name__, m.parent, m.index, m.tag)


class Meta(type):
    def __new__(cls, name, bases, ns):
        ns["made"] = name
        return type.__new__(cls, name, bases, ns)


class Base3(metaclass=Meta):
    pass


class Swap(type):
    def __new__(cls, name, bases, ns):
        return type.__new__(cls, name, (Unrelated,), ns)


class Odd(metaclass=Swap):
    pass


class Dim3(Base3, Styled):
    def __copy__(self):
        cp = self.__new__(self.__class__)
        cp.__init__(sheet=self.parent)
        return cp


class Dim4(Odd, Styled):
    def __copy__(self):
        cp = self.__new__(self.__class__)
        cp.__init__(worksheet=self.w)
        return cp


c3 = copy(Dim3("s3"))
print(c3.parent, c3.made)
c4 = copy(Dim4(worksheet="s4"))
print(type(c4).__name__, c4.w)
