# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Helper to provide extensibility for pickle.

This is the registry of reduction functions (`pickle`), the reconstructors
`pickle` writes for plain objects, and the extension-code registry.

The runtime's `object.__reduce_ex__`, `__reduce__` and `__getstate__` answer
through `_reduce_ex`, `_reduce` and `_getstate` here, so `pickle` and `copy`
both reach the same reduction; `__newobj__`, `__newobj_ex__` and
`_reconstructor` are the reconstructors it names.
"""

__all__ = ["pickle", "constructor", "add_extension", "remove_extension", "clear_extension_cache"]

dispatch_table = {}


def pickle(ob_type, pickle_function, constructor_ob=None):
    if not callable(pickle_function):
        raise TypeError("reduction functions must be callable")
    dispatch_table[ob_type] = pickle_function
    if constructor_ob is not None:
        constructor(constructor_ob)


def constructor(object):
    if not callable(object):
        raise TypeError("constructors must be callable")


def _reconstructor(cls, base, state):
    if base is object:
        obj = object.__new__(cls)
    else:
        obj = base.__new__(cls, state)
    return obj


def __newobj__(cls, *args):
    return cls.__new__(cls, *args)


def __newobj_ex__(cls, args, kwargs):
    return cls.__new__(cls, *args, **kwargs)


def _slotnames(cls):
    """Return the slot names of a class and its bases, in CPython's order."""
    names = []
    if not hasattr(cls, "__slots__"):
        return names
    for c in cls.__mro__:
        if "__slots__" in c.__dict__:
            slots = c.__dict__["__slots__"]
            if isinstance(slots, str):
                slots = (slots,)
            for name in slots:
                if name in ("__dict__", "__weakref__"):
                    continue
                elif name.startswith("__") and not name.endswith("__"):
                    stripped = c.__name__.lstrip("_")
                    if stripped:
                        names.append("_%s%s" % (stripped, name))
                    else:
                        names.append(name)
                else:
                    names.append(name)
    return names


def _has_dict(cls):
    for c in cls.__mro__:
        if c is object:
            continue
        if "__slots__" not in c.__dict__:
            return True
        slots = c.__dict__["__slots__"]
        if slots == "__dict__" or (not isinstance(slots, str) and "__dict__" in slots):
            return True
    return False


def _getstate(obj):
    """`object.__getstate__()`: the instance dict, plus `(dict, slots)` with slots."""
    t = type(obj)
    state = getattr(obj, "__dict__", None) if _has_dict(t) else None
    if state is not None:
        state = dict(state) if len(state) else None
    slots = _slotnames(t)
    if slots:
        slotstate = {}
        for name in slots:
            try:
                slotstate[name] = getattr(obj, name)
            except AttributeError:
                pass
        if slotstate:
            return (state, slotstate)
    return state


def _reduce_ex(obj, proto):
    """`object.__reduce_ex__(proto)`: a class's own `__reduce__` when it has one."""
    cls = type(obj)
    if getattr(cls, "__reduce__", None) is not object.__reduce__:
        return obj.__reduce__()
    if proto >= 2:
        return _reduce_newobj(obj, cls)
    return _reduce_legacy(obj, cls, proto)


def _reduce(obj):
    """`object.__reduce__()`: the protocol 0 reduction."""
    return _reduce_legacy(obj, type(obj), 0)


def _reduce_newobj(obj, cls):
    getnewargs_ex = getattr(obj, "__getnewargs_ex__", None)
    if getnewargs_ex is not None:
        args, kwargs = getnewargs_ex()
        if not isinstance(args, tuple):
            raise TypeError("__getnewargs_ex__ should return a tuple of (tuple, dict)")
        if kwargs:
            func = __newobj_ex__
            newargs = (cls, args, kwargs)
        else:
            func = __newobj__
            newargs = (cls,) + args
    else:
        getnewargs = getattr(obj, "__getnewargs__", None)
        if getnewargs is not None:
            args = getnewargs()
            if not isinstance(args, tuple):
                raise TypeError("__getnewargs__ should return a tuple")
        else:
            args = ()
        func = __newobj__
        newargs = (cls,) + args
    state = obj.__getstate__()
    listitems = iter(obj) if isinstance(obj, list) else None
    dictitems = iter(obj.items()) if isinstance(obj, dict) else None
    return (func, newargs, state, listitems, dictitems)


def _reduce_legacy(obj, cls, proto):
    if isinstance(obj, (list, dict, set, frozenset, bytes, bytearray, str, int, float, tuple)):
        raise TypeError("cannot pickle %r object with protocol %d" % (cls.__name__, proto))
    if getattr(cls, "__getstate__", None) is object.__getstate__:
        state = getattr(obj, "__dict__", None)
        if state is not None and len(state) == 0:
            state = None
        if _slotnames(cls):
            raise TypeError("a class that defines __slots__ without defining __getstate__ cannot be pickled with protocol %d" % proto)
    else:
        state = obj.__getstate__()
    args = (cls, object, None)
    if state is not None:
        return (_reconstructor, args, state)
    return (_reconstructor, args)


_extension_registry = {}
_inverted_registry = {}
_extension_cache = {}


def add_extension(module, name, code):
    code = int(code)
    if not 1 <= code <= 0x7fffffff:
        raise ValueError("code out of range")
    key = (module, name)
    if _extension_registry.get(key) == code and _inverted_registry.get(code) == key:
        return
    if key in _extension_registry:
        raise ValueError("key %s is already registered with code %s" % (key, _extension_registry[key]))
    if code in _inverted_registry:
        raise ValueError("code %s is already in use for key %s" % (code, _inverted_registry[code]))
    _extension_registry[key] = code
    _inverted_registry[code] = key


def remove_extension(module, name, code):
    key = (module, name)
    if _extension_registry.get(key) != code or _inverted_registry.get(code) != key:
        raise ValueError("key %s is not registered with code %s" % (key, code))
    del _extension_registry[key]
    del _inverted_registry[code]
    if code in _extension_cache:
        del _extension_cache[code]


def clear_extension_cache():
    _extension_cache.clear()
