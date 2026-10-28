# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Helper to provide extensibility for pickle.

This is the registry of reduction functions (`pickle`), the reconstructors
`pickle` writes for plain objects, and the extension-code registry.

`pickle` does not ask an instance for a default `__reduce_ex__`, because the
runtime's `object` has none: `pickle` builds the same reduction itself, and
`__newobj__`, `__newobj_ex__` and `_reconstructor` here are what it writes.
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
