# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Abstract base classes."""


def abstractmethod(funcobj):
    funcobj.__isabstractmethod__ = True
    return funcobj


def _is_abstract(value):
    if isinstance(value, (classmethod, staticmethod)):
        value = value.__func__
    elif isinstance(value, property):
        return any(getattr(f, "__isabstractmethod__", False) for f in (value.fget, value.fset, value.fdel))
    return getattr(value, "__isabstractmethod__", False)


class ABCMeta(type):
    def __new__(mcls, name, bases, namespace, **kwargs):
        cls = super().__new__(mcls, name, bases, namespace, **kwargs)
        abstracts = {n for n, v in namespace.items() if _is_abstract(v)}
        for base in bases:
            for n in getattr(base, "__abstractmethods__", ()):
                if _is_abstract(getattr(cls, n, None)):
                    abstracts.add(n)
        cls.__abstractmethods__ = frozenset(abstracts)
        cls._abc_registry = []
        return cls

    def __call__(cls, *args, **kwargs):
        missing = cls.__abstractmethods__
        if missing:
            names = sorted(missing)
            raise TypeError("Can't instantiate abstract class %s without an implementation for abstract method%s %s"
                            % (cls.__name__, "s" if len(names) > 1 else "", ", ".join("'" + n + "'" for n in names)))
        return super().__call__(*args, **kwargs)

    def register(cls, subclass):
        if not cls.__subclasscheck__(subclass):
            cls._abc_registry.append(subclass)
        return subclass

    def __instancecheck__(cls, instance):
        return cls.__subclasscheck__(type(instance))

    def __subclasscheck__(cls, subclass):
        if cls in subclass.__mro__:
            return True
        hook = getattr(cls, "__subclasshook__", None)
        if hook is not None:
            ok = hook(subclass)
            if ok is not NotImplemented:
                return bool(ok)
        for k in subclass.__mro__:
            for r in cls._abc_registry:
                if k is r or issubclass(k, r):
                    return True
        for sub in cls.__subclasses__() if hasattr(cls, "__subclasses__") else ():
            if issubclass(subclass, sub):
                return True
        return False


class ABC(metaclass=ABCMeta):
    __slots__ = ()


def get_cache_token():
    return 0


def update_abstractmethods(cls):
    return cls
