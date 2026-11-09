# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""cobble: data classes and visitors, written without exec.

The real package generates __init__, __repr__, __eq__, __ne__, __hash__ and
the visitor's _accept methods as source text and exec()s it, which a
translated program cannot do. Here the same methods are closures over the
field list, so @data classes and visitor() behave the same.
"""

import abc
import itertools

__all__ = ["data", "field", "visitable", "visitor", "copy"]

_sort_key_count = itertools.count()
_undefined = object()


class _Field(object):
    def __init__(self, sort_key, default):
        self.sort_key = sort_key
        self.default = default


def field(default=_undefined):
    if default not in [_undefined, None]:
        raise TypeError("default value must be None")
    return _Field(next(_sort_key_count), default=default)


def _underscore(word):
    out = []
    for i, ch in enumerate(word):
        if ch.isupper():
            if i and (word[i - 1].islower() or word[i - 1].isdigit()
                      or (i + 1 < len(word) and word[i + 1].islower() and word[i - 1].isupper())):
                out.append("_")
            out.append(ch.lower())
        elif ch == "-":
            out.append("_")
        else:
            out.append(ch)
    return "".join(out)


def _visit_method_name(cls):
    return "visit_" + _underscore(cls.__name__)


def data(cls):
    fields = []
    for name in dir(cls):
        member = getattr(cls, name)
        if isinstance(member, _Field):
            fields.append((name, member))
    fields.sort(key=lambda f: f[1].sort_key)
    names = [name for name, f in fields]
    defaults = [f.default for name, f in fields]
    cls_name = cls.__name__

    def __init__(self, *args, **kwargs):
        if len(args) > len(names):
            raise TypeError("__init__() takes %d positional arguments but %d were given"
                            % (len(names) + 1, len(args) + 1))
        values = list(args)
        for i in range(len(args), len(names)):
            n = names[i]
            if n in kwargs:
                values.append(kwargs.pop(n))
            elif defaults[i] is not _undefined:
                values.append(defaults[i])
            else:
                raise TypeError("__init__() missing required argument: '%s'" % n)
        for n in kwargs:
            if n in names:
                raise TypeError("__init__() got multiple values for argument '%s'" % n)
            raise TypeError("__init__() got an unexpected keyword argument '%s'" % n)
        for n, v in zip(names, values):
            setattr(self, n, v)
        super(cls, self).__init__()

    def __repr__(self):
        return "%s(%s)" % (cls_name, ", ".join(
            "%s=%s" % (n, repr(getattr(self, n))) for n in names))

    def __eq__(self, other):
        if not isinstance(other, cls):
            return False
        for n in names:
            if not (getattr(self, n) == getattr(other, n)):
                return False
        return True

    def __ne__(self, other):
        return not (self == other)

    def __hash__(self):
        return hash(tuple([cls] + [getattr(self, n) for n in names]))

    cls.__init__ = __init__
    cls.__repr__ = __repr__
    cls.__eq__ = __eq__
    cls.__ne__ = __ne__
    cls.__hash__ = __hash__
    visitable(cls)
    cls._cobble_fields = fields
    return cls


_visitables = set()


def visitable(cls):
    _visitables.add(cls)
    return cls


def _subclasses(cls):
    subclasses = cls.__subclasses__()
    index = 0
    while index < len(subclasses):
        subclasses += _subclasses(subclasses[index])
        index += 1
    return subclasses


def _make_accept(method_name):
    def _accept(self, visitor, *args):
        return getattr(visitor, method_name)(self, *args)
    return _accept


def visitor(cls, args=None):
    if args is None:
        args = 0
    subclasses = set(s for s in _subclasses(cls) if s in _visitables)
    namespace = {}
    for subclass in subclasses:
        method_name = _visit_method_name(subclass)
        setattr(subclass, "_accept" + str(args), _make_accept(method_name))
        namespace[method_name] = abc.abstractmethod(_abstract_visit)
    accept_name = "_accept" + str(args)

    def visit(self, value, *rest):
        return getattr(value, accept_name)(self, *rest)
    namespace["visit"] = visit
    return abc.ABCMeta(cls.__name__ + "Visitor", (object,), namespace)


def _abstract_visit(self, value):
    pass


def copy(obj, **kwargs):
    obj_type = type(obj)
    attrs = dict((name, getattr(obj, name)) for name, f in obj_type._cobble_fields)
    attrs.update(kwargs)
    return obj_type(**attrs)
