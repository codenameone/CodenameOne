# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Higher-order functions and operations on callable objects."""

WRAPPER_ASSIGNMENTS = ("__module__", "__name__", "__qualname__", "__doc__")
WRAPPER_UPDATES = ("__dict__",)
_MISSING = object()

from _cn1functools import _lru_cache_wrapper


def reduce(function, iterable, initial=_MISSING):
    it = iter(iterable)
    if initial is _MISSING:
        try:
            value = next(it)
        except StopIteration:
            raise TypeError("reduce() of empty iterable with no initial value") from None
    else:
        value = initial
    for x in it:
        value = function(value, x)
    return value


class partial:
    __class_getitem__ = classmethod(type(list[int]))
    def __init__(self, func, *args, **keywords):
        if not callable(func):
            raise TypeError("the first argument must be callable")
        if isinstance(func, partial):
            args = func.args + args
            keywords = {**func.keywords, **keywords}
            func = func.func
        self.func = func
        self.args = args
        self.keywords = keywords

    def __call__(self, *args, **keywords):
        return self.func(*self.args, *args, **{**self.keywords, **keywords})

    def __reduce__(self):
        return (type(self), (self.func,), (self.func, self.args, self.keywords or None, None))

    def __setstate__(self, state):
        func, args, keywords, namespace = state
        if not callable(func) or not isinstance(args, tuple) or (keywords is not None and not isinstance(keywords, dict)):
            raise TypeError("invalid partial state")
        self.func = func
        self.args = args
        self.keywords = {} if keywords is None else dict(keywords)

    def __repr__(self):
        parts = [repr(self.func)]
        parts.extend(repr(a) for a in self.args)
        parts.extend(k + "=" + repr(v) for k, v in self.keywords.items())
        return "functools.partial(" + ", ".join(parts) + ")"


def update_wrapper(wrapper, wrapped, assigned=WRAPPER_ASSIGNMENTS, updated=WRAPPER_UPDATES):
    # Each attribute by its own name: a computed getattr/setattr cannot be
    # typed, and these are the only names a wrapper is given. A name
    # outside them is refused rather than copied blind.
    for attr in assigned:
        if attr not in WRAPPER_ASSIGNMENTS:
            raise TypeError("update_wrapper copies only " + repr(WRAPPER_ASSIGNMENTS))
    for attr in updated:
        if attr not in WRAPPER_UPDATES:
            raise TypeError("update_wrapper updates only " + repr(WRAPPER_UPDATES))
    if "__module__" in assigned:
        try:
            wrapper.__module__ = wrapped.__module__
        except (AttributeError, TypeError):
            pass
    if "__name__" in assigned:
        try:
            wrapper.__name__ = wrapped.__name__
        except (AttributeError, TypeError):
            pass
    if "__qualname__" in assigned:
        try:
            wrapper.__qualname__ = wrapped.__qualname__
        except (AttributeError, TypeError):
            pass
    if "__doc__" in assigned:
        try:
            wrapper.__doc__ = wrapped.__doc__
        except (AttributeError, TypeError):
            pass
    if "__dict__" in updated:
        try:
            wrapper.__dict__.update(wrapped.__dict__)
        except (AttributeError, TypeError):
            pass
    wrapper.__wrapped__ = wrapped
    return wrapper


def wraps(wrapped, assigned=WRAPPER_ASSIGNMENTS, updated=WRAPPER_UPDATES):
    def deco(wrapper):
        return update_wrapper(wrapper, wrapped, assigned, updated)
    return deco


def cmp_to_key(mycmp):
    class K:
        __slots__ = ["obj"]

        def __init__(self, obj):
            self.obj = obj

        def __lt__(self, other):
            return mycmp(self.obj, other.obj) < 0

        def __gt__(self, other):
            return mycmp(self.obj, other.obj) > 0

        def __eq__(self, other):
            return mycmp(self.obj, other.obj) == 0

        def __le__(self, other):
            return mycmp(self.obj, other.obj) <= 0

        def __ge__(self, other):
            return mycmp(self.obj, other.obj) >= 0

        __hash__ = None
    return K


def _own(own, base):
    return own is not None and own is not base


def total_ordering(cls):
    # Every name is written out: no getattr or setattr with a computed name.
    d = cls.__dict__
    roots = []
    if _own(d.get("__lt__"), getattr(object, "__lt__", None)):
        roots.append("__lt__")
    if _own(d.get("__le__"), getattr(object, "__le__", None)):
        roots.append("__le__")
    if _own(d.get("__gt__"), getattr(object, "__gt__", None)):
        roots.append("__gt__")
    if _own(d.get("__ge__"), getattr(object, "__ge__", None)):
        roots.append("__ge__")
    if not roots:
        raise ValueError("must define at least one ordering operation: < > <= >=")
    root = roots[0]

    def lt_from(self, other):
        if root == "__lt__":
            return self.__lt__(other)
        if root == "__le__":
            r = self.__le__(other)
            return r if r is NotImplemented else (r and self != other)
        if root == "__gt__":
            r = self.__gt__(other)
            return r if r is NotImplemented else (not r and self != other)
        r = self.__ge__(other)
        return r if r is NotImplemented else not r

    def le(self, other):
        r = lt_from(self, other)
        return r if r is NotImplemented else (r or self == other)

    def gt(self, other):
        r = lt_from(self, other)
        return r if r is NotImplemented else (not r and self != other)

    def ge(self, other):
        r = lt_from(self, other)
        return r if r is NotImplemented else not r

    if "__lt__" not in roots:
        cls.__lt__ = lt_from
    if "__le__" not in roots:
        cls.__le__ = le
    if "__gt__" not in roots:
        cls.__gt__ = gt
    if "__ge__" not in roots:
        cls.__ge__ = ge
    return cls


class _CacheInfo(tuple):
    def __new__(cls, hits, misses, maxsize, currsize):
        return tuple.__new__(cls, (hits, misses, maxsize, currsize))

    @property
    def hits(self):
        return self[0]

    @property
    def misses(self):
        return self[1]

    @property
    def maxsize(self):
        return self[2]

    @property
    def currsize(self):
        return self[3]


    def __repr__(self):
        return "CacheInfo(hits=%r, misses=%r, maxsize=%r, currsize=%r)" % tuple(self)


def lru_cache(maxsize=128, typed=False):
    if callable(maxsize) and not isinstance(maxsize, int):
        return lru_cache(128, False)(maxsize)
    if maxsize is not None and maxsize < 0:
        maxsize = 0

    def decorating(user_function):
        wrapper = _lru_cache_wrapper(user_function, maxsize, typed, _CacheInfo)
        wrapper.cache_parameters = lambda: {"maxsize": maxsize, "typed": typed}
        return update_wrapper(wrapper, user_function)
    return decorating


def cache(user_function):
    return lru_cache(None)(user_function)


class cached_property:
    __class_getitem__ = classmethod(type(list[int]))
    def __init__(self, func):
        self.func = func
        self.attrname = None
        self.__doc__ = getattr(func, "__doc__", None)

    def __set_name__(self, owner, name):
        self.attrname = name

    def __get__(self, instance, owner=None):
        if instance is None:
            return self
        d = instance.__dict__
        if self.attrname in d:
            return d[self.attrname]
        val = self.func(instance)
        d[self.attrname] = val
        return val


def singledispatch(func):
    registry = {object: func}

    def dispatch(cls):
        for k in cls.__mro__:
            if k in registry:
                return registry[k]
        return func

    def register(cls, f=None):
        if f is None:
            if isinstance(cls, type):
                return lambda g: register(cls, g)
            f = cls
            ann = getattr(f, "__annotations__", {})
            cls = next(iter(v for k, v in ann.items() if k != "return"))
        registry[cls] = f
        return f

    def wrapper(*args, **kw):
        if not args:
            raise TypeError(getattr(func, "__name__", "singledispatch function") + " requires at least 1 positional argument")
        return dispatch(type(args[0]))(*args, **kw)

    wrapper.register = register
    wrapper.dispatch = dispatch
    wrapper.registry = registry
    return update_wrapper(wrapper, func)
