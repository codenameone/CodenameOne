ROOT = "urn:"
NS = ROOT + "rel"


class Descriptor:
    allow_none = False
    namespace = None

    def __init__(self, name=None, **kw):
        self.name = name
        for k, v in kw.items():
            setattr(self, k, v)

    def check(self, value):
        if value is None:
            return self.allow_none
        return isinstance(value, self.expected_type)

    def show(self):
        return "%s none=%s type=%s" % (self.name, self.allow_none, self.expected_type.__name__)


class Typed(Descriptor):
    expected_type = type(None)


def make(name, **kw):
    return Typed(name, **kw)
