# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Weak references.

A referent goes when the garbage collector runs, not when its last strong
reference is dropped as under CPython, so code must not expect a ref to
read None right after a del. Callbacks are not called (no port reports a
referent's death); the weak containers drop dead entries whenever they are
read instead, and finalize() runs its function only when called or at exit.
"""

import atexit
from _weakref import ref, ReferenceType, referenceable as _referenceable

__all__ = ["ref", "proxy", "getweakrefcount", "getweakrefs", "WeakKeyDictionary", "ReferenceType",
           "ProxyType", "CallableProxyType", "ProxyTypes", "WeakValueDictionary", "WeakSet", "WeakMethod",
           "finalize"]


def getweakrefcount(obj):
    raise NotImplementedError("weakref.getweakrefcount is not available: references are not tracked per object")


def getweakrefs(obj):
    raise NotImplementedError("weakref.getweakrefs is not available: references are not tracked per object")


class ProxyType:
    __slots__ = ("_ref",)

    def __init__(self, obj, callback=None):
        object.__setattr__(self, "_ref", ref(obj, callback))

    def _target(self):
        o = object.__getattribute__(self, "_ref")()
        if o is None:
            raise ReferenceError("weakly-referenced object no longer exists")
        return o

    def __getattr__(self, name):
        return getattr(ProxyType._target(self), name)

    def __setattr__(self, name, value):
        setattr(ProxyType._target(self), name, value)

    def __delattr__(self, name):
        delattr(ProxyType._target(self), name)

    def __repr__(self):
        o = ProxyType._target(self)
        return "<weakproxy at 0x%x; to '%s' at 0x%x>" % (id(self), type(o).__name__, id(o))

    def __str__(self):
        return str(ProxyType._target(self))

    def __bool__(self):
        return bool(ProxyType._target(self))

    def __len__(self):
        return len(ProxyType._target(self))

    def __iter__(self):
        return iter(ProxyType._target(self))

    def __getitem__(self, k):
        return ProxyType._target(self)[k]

    def __setitem__(self, k, v):
        ProxyType._target(self)[k] = v

    def __contains__(self, k):
        return k in ProxyType._target(self)

    def __eq__(self, other):
        return ProxyType._target(self) == other

    def __lt__(self, other):
        return ProxyType._target(self) < other

    def __add__(self, other):
        return ProxyType._target(self) + other

    def __hash__(self):
        raise TypeError("unhashable type: 'weakref.ProxyType'")


class CallableProxyType(ProxyType):
    __slots__ = ()

    def __call__(self, *args, **kwargs):
        return ProxyType._target(self)(*args, **kwargs)


ProxyTypes = (ProxyType, CallableProxyType)


def proxy(obj, callback=None):
    if callable(obj):
        return CallableProxyType(obj, callback)
    return ProxyType(obj, callback)


class WeakMethod:
    __slots__ = ("_self_ref", "_func", "_meth_type")

    def __init__(self, meth, callback=None):
        try:
            obj = meth.__self__
            func = meth.__func__
        except AttributeError:
            raise TypeError("argument should be a bound method, not %s" % type(meth)) from None
        self._self_ref = ref(obj)
        self._func = ref(func)
        self._meth_type = type(meth)

    def __call__(self):
        obj = self._self_ref()
        func = self._func()
        if obj is None or func is None:
            return None
        return self._meth_type(func, obj)

    def __eq__(self, other):
        if isinstance(other, WeakMethod):
            a, b = self(), other()
            if a is None or b is None:
                return self is other
            return a == b
        return NotImplemented

    def __ne__(self, other):
        r = self.__eq__(other)
        return r if r is NotImplemented else not r

    def __hash__(self):
        return hash((self._self_ref, self._func))


class WeakValueDictionary:
    def __init__(self, other=(), /, **kw):
        self.data = {}
        self.update(other, **kw)

    def _live(self):
        dead = [k for k, r in self.data.items() if r() is None]
        for k in dead:
            del self.data[k]
        return self.data

    def __getitem__(self, key):
        o = self.data[key]()
        if o is None:
            del self.data[key]
            raise KeyError(key)
        return o

    def __delitem__(self, key):
        del self.data[key]

    def __len__(self):
        return len(self._live())

    def __contains__(self, key):
        try:
            o = self.data[key]()
        except KeyError:
            return False
        return o is not None

    def __repr__(self):
        return "<%s at %#x>" % (self.__class__.__name__, id(self))

    def __setitem__(self, key, value):
        self.data[key] = ref(value)

    def copy(self):
        new = WeakValueDictionary()
        for key, r in self._live().items():
            o = r()
            if o is not None:
                new[key] = o
        return new

    __copy__ = copy

    def get(self, key, default=None):
        try:
            r = self.data[key]
        except KeyError:
            return default
        o = r()
        return default if o is None else o

    def items(self):
        for k, r in list(self._live().items()):
            v = r()
            if v is not None:
                yield k, v

    def keys(self):
        for k, v in self.items():
            yield k

    __iter__ = keys

    def itervaluerefs(self):
        yield from list(self._live().values())

    def valuerefs(self):
        return list(self._live().values())

    def values(self):
        for k, v in self.items():
            yield v

    def popitem(self):
        while True:
            key, r = self.data.popitem()
            o = r()
            if o is not None:
                return key, o

    def pop(self, key, *args):
        try:
            o = self.data.pop(key)()
        except KeyError:
            o = None
        if o is None:
            if args:
                return args[0]
            raise KeyError(key)
        return o

    def setdefault(self, key, default=None):
        try:
            o = self.data[key]()
        except KeyError:
            o = None
        if o is None:
            self.data[key] = ref(default)
            return default
        return o

    def update(self, other=None, /, **kwargs):
        d = self.data
        if other is not None:
            if not hasattr(other, "items"):
                other = dict(other)
            for key, o in other.items():
                d[key] = ref(o)
        for key, o in kwargs.items():
            d[key] = ref(o)

    def __or__(self, other):
        c = self.copy()
        c.update(other)
        return c

    def __ior__(self, other):
        self.update(other)
        return self


class WeakKeyDictionary:
    def __init__(self, dict=None):
        self.data = {}
        if dict is not None:
            self.update(dict)

    def _live(self):
        dead = [k for k in self.data if k() is None]
        for k in dead:
            del self.data[k]
        return self.data

    def __delitem__(self, key):
        del self.data[ref(key)]

    def __getitem__(self, key):
        return self.data[ref(key)]

    def __len__(self):
        return len(self._live())

    def __repr__(self):
        return "<%s at %#x>" % (self.__class__.__name__, id(self))

    def __setitem__(self, key, value):
        self.data[ref(key)] = value

    def copy(self):
        new = WeakKeyDictionary()
        for key, value in self.items():
            new[key] = value
        return new

    __copy__ = copy

    def get(self, key, default=None):
        return self.data.get(ref(key), default)

    def __contains__(self, key):
        try:
            wr = ref(key)
        except TypeError:
            return False
        return wr in self.data

    def items(self):
        for wr, value in list(self._live().items()):
            key = wr()
            if key is not None:
                yield key, value

    def keys(self):
        for wr in list(self._live()):
            obj = wr()
            if obj is not None:
                yield obj

    __iter__ = keys

    def values(self):
        for wr, value in list(self._live().items()):
            if wr() is not None:
                yield value

    def keyrefs(self):
        return list(self._live())

    def popitem(self):
        while True:
            key, value = self.data.popitem()
            o = key()
            if o is not None:
                return o, value

    def pop(self, key, *args):
        return self.data.pop(ref(key), *args)

    def setdefault(self, key, default=None):
        return self.data.setdefault(ref(key), default)

    def update(self, dict=None, /, **kwargs):
        d = self.data
        if dict is not None:
            if not hasattr(dict, "items"):
                dict = type({})(dict)
            for key, value in dict.items():
                d[ref(key)] = value
        if len(kwargs):
            self.update(kwargs)


class WeakSet:
    def __init__(self, data=None):
        self.data = set()
        if data is not None:
            self.update(data)

    def _live(self):
        dead = [r for r in self.data if r() is None]
        for r in dead:
            self.data.discard(r)
        return self.data

    def __iter__(self):
        for itemref in list(self._live()):
            item = itemref()
            if item is not None:
                yield item

    def __len__(self):
        return len(self._live())

    def __contains__(self, item):
        try:
            wr = ref(item)
        except TypeError:
            return False
        return wr in self.data

    def __repr__(self):
        return "{%s(%r)}" % (type(self).__name__, list(self))

    def add(self, item):
        self.data.add(ref(item))

    def clear(self):
        self.data.clear()

    def copy(self):
        return self.__class__(self)

    def pop(self):
        while True:
            try:
                itemref = self.data.pop()
            except KeyError:
                raise KeyError("pop from empty WeakSet") from None
            item = itemref()
            if item is not None:
                return item

    def remove(self, item):
        self.data.remove(ref(item))

    def discard(self, item):
        self.data.discard(ref(item))

    def update(self, other):
        for element in other:
            self.add(element)

    def __ior__(self, other):
        self.update(other)
        return self

    def difference(self, other):
        return self.__class__(e for e in self if e not in other)

    __sub__ = difference

    def intersection(self, other):
        return self.__class__(item for item in other if item in self)

    __and__ = intersection

    def union(self, other):
        return self.__class__(list(self) + list(other))

    __or__ = union

    def issubset(self, other):
        return all(e in other for e in self)

    def __eq__(self, other):
        if not isinstance(other, self.__class__):
            return NotImplemented
        return set(self._live()) == set(other._live())


class finalize:
    __slots__ = ("_ref", "_func", "_args", "_kwargs", "_alive", "atexit")
    _registry = []

    def __init__(self, obj, func, /, *args, **kwargs):
        self._ref = ref(obj)
        self._func = func
        self._args = args
        self._kwargs = kwargs or {}
        self._alive = True
        self.atexit = True
        finalize._registry.append(self)

    def __call__(self, _=None):
        if self._alive:
            self._alive = False
            return self._func(*self._args, **self._kwargs)
        return None

    def detach(self):
        if self._alive:
            obj = self._ref()
            if obj is not None:
                self._alive = False
                return (obj, self._func, self._args, self._kwargs)
        return None

    def peek(self):
        if self._alive:
            obj = self._ref()
            if obj is not None:
                return (obj, self._func, self._args, self._kwargs)
        return None

    @property
    def alive(self):
        return self._alive

    def __repr__(self):
        obj = self._ref()
        if obj is None:
            return "<%s object at %#x; dead>" % (type(self).__name__, id(self))
        return "<%s object at %#x; for %r at %#x>" % (type(self).__name__, id(self), type(obj).__name__, id(obj))

    @staticmethod
    def _exitfunc():
        for f in list(finalize._registry):
            if f._alive and f.atexit:
                f()


atexit.register(finalize._exitfunc)
