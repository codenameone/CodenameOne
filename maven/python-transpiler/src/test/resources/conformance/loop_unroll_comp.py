# A list comprehension over attribute names the program spells out becomes
# a list display, per concrete class; and a dir()-based options helper over a
# nested options class becomes a dict display of the class's public names.


class Part:
    __attrs__ = ("a", "b")
    __elements__ = ("c",)

    def __init__(self):
        self.a = 0
        self.b = ""
        self.c = None

    def __bool__(self):
        parts = [getattr(self, attr) for attr in self.__attrs__ + self.__elements__]
        return any(parts)

    def names(self):
        return [(n, getattr(self, n)) for n in self.__attrs__]


class Wide(Part):
    __attrs__ = ("a", "b", "d")

    def __init__(self):
        Part.__init__(self)
        self.d = 4


def _todict(obj):
    return dict((k, getattr(obj, k)) for k in dir(obj) if not k.startswith('_'))


class Converter(object):
    class DefaultOptions:
        wrap = False
        bullets = '*+-'
        _hidden = 1
        keep = []

    class Options(DefaultOptions):
        pass

    def __init__(self, **options):
        self.options = _todict(self.DefaultOptions)
        self.options.update(_todict(self.Options))
        self.options.update(options)


class Custom(Converter):
    pass


p = Part()
print(bool(p), p.names())
p.c = 5
print(bool(p))
w = Wide()
print(bool(w), w.names())
c = Converter(wrap=True)
print(sorted(c.options.items(), key=lambda kv: kv[0]), list(c.options))
print(Custom().options, Custom().options["keep"] is Converter().options["keep"])
