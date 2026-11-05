# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Names for built-in types."""


def _f():
    pass


async def _c():
    pass


def _g():
    yield 1


async def _ag():
    yield 1


class _C:
    def _m(self):
        pass


FunctionType = type(_f)
LambdaType = type(lambda: None)
CodeType = None
MappingProxyType = dict
CellType = None
GeneratorType = type(_g())
_co = _c()
CoroutineType = type(_co)
_co.close()
AsyncGeneratorType = type(_ag())
MethodType = type(_C()._m)
BuiltinFunctionType = type(len)
BuiltinMethodType = type([].append)
NoneType = type(None)
NotImplementedType = type(NotImplemented)
EllipsisType = type(...)
GenericAlias = type(list[int])
UnionType = type(int | str)


class SimpleNamespace:
    def __init__(self, mapping=None, /, **kwargs):
        if mapping is not None:
            kwargs = {**dict(mapping), **kwargs}
        for k, v in kwargs.items():
            setattr(self, k, v)

    def __repr__(self):
        items = ("%s=%r" % (k, v) for k, v in self.__dict__.items())
        return "namespace(" + ", ".join(items) + ")"

    def __eq__(self, other):
        if isinstance(self, SimpleNamespace) and isinstance(other, SimpleNamespace):
            return self.__dict__ == other.__dict__
        return NotImplemented

    __hash__ = None


def new_class(name, bases=(), kwds=None, exec_body=None):
    ns = {}
    if exec_body is not None:
        exec_body(ns)
    meta = (kwds or {}).get("metaclass", type)
    return meta(name, tuple(bases), ns)


def resolve_bases(bases):
    out = []
    for b in bases:
        if isinstance(b, type):
            out.append(b)
        elif hasattr(b, "__mro_entries__"):
            out.extend(b.__mro_entries__(tuple(bases)))
        else:
            out.append(b)
    return tuple(out)


class DynamicClassAttribute:
    """A data descriptor read through instances only; on the class it
    defers to the metaclass's __getattr__."""

    def __init__(self, fget=None, fset=None, fdel=None, doc=None):
        self.fget = fget
        self.fset = fset
        self.fdel = fdel
        self.__doc__ = doc or (fget.__doc__ if fget is not None else None)
        self.overwrite_doc = doc is None
        self.__isabstractmethod__ = bool(getattr(fget, "__isabstractmethod__", False))

    def __get__(self, instance, ownerclass=None):
        if instance is None:
            if self.__isabstractmethod__:
                return self
            raise AttributeError()
        elif self.fget is None:
            raise AttributeError("unreadable attribute")
        return self.fget(instance)

    def __set__(self, instance, value):
        if self.fset is None:
            raise AttributeError("can't set attribute")
        self.fset(instance, value)

    def __delete__(self, instance):
        if self.fdel is None:
            raise AttributeError("can't delete attribute")
        self.fdel(instance)

    def getter(self, fget):
        fdoc = fget.__doc__ if self.overwrite_doc else None
        result = type(self)(fget, self.fset, self.fdel, fdoc or self.__doc__)
        result.overwrite_doc = self.overwrite_doc
        return result

    def setter(self, fset):
        result = type(self)(self.fget, fset, self.fdel, self.__doc__)
        result.overwrite_doc = self.overwrite_doc
        return result

    def deleter(self, fdel):
        result = type(self)(self.fget, self.fset, fdel, self.__doc__)
        result.overwrite_doc = self.overwrite_doc
        return result


def coroutine(func):
    return func


def get_original_bases(cls, /):
    return cls.__dict__.get("__orig_bases__", cls.__bases__)
