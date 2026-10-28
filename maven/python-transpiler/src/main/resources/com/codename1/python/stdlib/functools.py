# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Higher-order functions and operations on callable objects."""

WRAPPER_ASSIGNMENTS = ("__module__", "__name__", "__qualname__", "__doc__")
WRAPPER_UPDATES = ("__dict__",)
_MISSING = object()


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
    for attr in assigned:
        try:
            value = getattr(wrapped, attr)
        except AttributeError:
            continue
        try:
            setattr(wrapper, attr, value)
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


def total_ordering(cls):
    def has(name):
        own = cls.__dict__.get(name)
        return own is not None and own is not getattr(object, name, None)
    roots = [op for op in ("__lt__", "__le__", "__gt__", "__ge__") if has(op)]
    if not roots:
        raise ValueError("must define at least one ordering operation: < > <= >=")
    root = roots[0]

    def lt_from(self, other):
        r = getattr(self, root)(other)
        if r is NotImplemented:
            return r
        if root == "__lt__":
            return r
        if root == "__le__":
            return r and self != other
        if root == "__gt__":
            return not r and self != other
        return not r

    def le(self, other):
        r = lt_from(self, other)
        return r if r is NotImplemented else (r or self == other)

    def gt(self, other):
        r = lt_from(self, other)
        return r if r is NotImplemented else (not r and self != other)

    def ge(self, other):
        r = lt_from(self, other)
        return r if r is NotImplemented else not r

    for name, fn in (("__lt__", lt_from), ("__le__", le), ("__gt__", gt), ("__ge__", ge)):
        if name not in roots:
            setattr(cls, name, fn)
    return cls


class _CacheInfo(tuple):
    def __new__(cls, hits, misses, maxsize, currsize):
        return tuple.__new__(cls, (hits, misses, maxsize, currsize))

    hits = property(lambda self: self[0])
    misses = property(lambda self: self[1])
    maxsize = property(lambda self: self[2])
    currsize = property(lambda self: self[3])

    def __repr__(self):
        return "CacheInfo(hits=%r, misses=%r, maxsize=%r, currsize=%r)" % tuple(self)


def _make_key(args, kwds, typed):
    key = args
    if kwds:
        key = key + (_MISSING,)
        for item in kwds.items():
            key = key + item
    if typed:
        key = key + tuple(type(v) for v in args)
        if kwds:
            key = key + tuple(type(v) for v in kwds.values())
    elif len(key) == 1 and type(key[0]) in (int, str):
        return key[0]
    return key


def lru_cache(maxsize=128, typed=False):
    if callable(maxsize) and not isinstance(maxsize, int):
        return lru_cache(128, False)(maxsize)
    if maxsize is not None and maxsize < 0:
        maxsize = 0

    def decorating(user_function):
        cache = {}
        stats = [0, 0]

        def wrapper(*args, **kwds):
            key = _make_key(args, kwds, typed)
            if key in cache:
                stats[0] += 1
                result = cache.pop(key)
                cache[key] = result
                return result
            stats[1] += 1
            result = user_function(*args, **kwds)
            if maxsize != 0:
                cache[key] = result
                if maxsize is not None and len(cache) > maxsize:
                    del cache[next(iter(cache))]
            return result

        def cache_info():
            return _CacheInfo(stats[0], stats[1], maxsize, len(cache))

        def cache_clear():
            cache.clear()
            stats[0] = 0
            stats[1] = 0

        wrapper.cache_info = cache_info
        wrapper.cache_clear = cache_clear
        return update_wrapper(wrapper, user_function)
    return decorating


def cache(user_function):
    return lru_cache(None)(user_function)


class cached_property:
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
