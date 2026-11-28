# A method always called with literal attribute names is specialized per
# name, so its getattr/setattr/hasattr name no attribute at run time.


class Base:
    def __init__(self):
        self.width = None
        self.height = 4
        self.label = "base"

    @property
    def area(self):
        return 6


class Box(Base):
    def __init__(self, parent):
        super().__init__()
        self.parent = parent

    def effective(self, name):
        v = getattr(self, name)
        if v is not None:
            return v
        return self._inherited(name)

    def _inherited(self, name):
        p = self.parent
        if p is None:
            return None
        return getattr(p, name)

    def store(self, name, value):
        if hasattr(self, name):
            setattr(self, name, value)
        return getattr(self, name, "none")

    def area_of(self, name):
        return getattr(super(Box, self), name)


root = Box(None)
root.width = 10
b = Box(root)
print(b.effective("width"), b.effective("height"), b.effective("label"))
print(b.store("label", "set"), b.store("width", 3), b.label, b.width)
print(b.area_of("area"))
