# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Shallow and deep copy operations."""


class Error(Exception):
    pass


error = Error

_ATOMIC = (type(None), int, float, bool, str, bytes, type, range, type(Ellipsis),
           type(NotImplemented), property)


_FUNCTIONS = ("function", "builtin_function_or_method", "method")


def copy(x):
    cls = type(x)
    if cls in _ATOMIC or cls is tuple or cls is frozenset or isinstance(x, type) or cls.__name__ in _FUNCTIONS:
        return x
    if cls is list:
        return x[:]
    if cls is dict:
        return dict(x)
    if cls is set:
        return set(x)
    copier = getattr(cls, "__copy__", None)
    if copier is not None:
        return copier(x)
    return _reconstruct(x, None)


def deepcopy(x, memo=None, _nil=[]):
    if memo is None:
        memo = {}
    d = id(x)
    y = memo.get(d, _nil)
    if y is not _nil:
        return y
    cls = type(x)
    if cls in _ATOMIC or isinstance(x, type):
        return x
    if cls is list:
        y = []
        memo[d] = y
        for a in x:
            y.append(deepcopy(a, memo))
    elif cls is tuple:
        y = tuple(deepcopy(a, memo) for a in x)
        if all(a is b for a, b in zip(x, y)):
            y = x
    elif cls is dict:
        y = {}
        memo[d] = y
        for k, v in x.items():
            y[deepcopy(k, memo)] = deepcopy(v, memo)
    elif cls is set:
        y = {deepcopy(a, memo) for a in x}
    elif cls is frozenset:
        y = frozenset(deepcopy(a, memo) for a in x)
    elif hasattr(cls, "__deepcopy__"):
        y = x.__deepcopy__(memo)
    elif cls.__name__ in _FUNCTIONS:
        return x
    else:
        y = _reconstruct(x, memo)
    if y is not x:
        memo[d] = y
        _keep_alive(x, memo)
    return y


def _keep_alive(x, memo):
    try:
        memo[id(memo)].append(x)
    except KeyError:
        memo[id(memo)] = [x]


def _reconstruct(x, memo):
    cls = type(x)
    y = cls.__new__(cls) if not _builtin_base(cls) else _new_like(x, cls, memo)
    if memo is not None:
        memo[id(x)] = y
    state = getattr(x, "__getstate__", None)
    state = state() if state is not None and getattr(cls, "__getstate__", None) is not getattr(object, "__getstate__", None) else _state_of(x)
    if state:
        if memo is not None:
            state = deepcopy(state, memo)
        if hasattr(y, "__setstate__"):
            y.__setstate__(state)
        else:
            for k, v in state.items():
                setattr(y, k, v)
    return y


def _state_of(x):
    try:
        return dict(x.__dict__)
    except AttributeError:
        state = {}
        for k in type(x).__mro__:
            for name in k.__dict__.get("__slots__", ()):
                if hasattr(x, name):
                    state[name] = getattr(x, name)
        return state


def _builtin_base(cls):
    for k in cls.__mro__:
        if k in (list, dict, set, tuple, frozenset, int, float, str, bytes):
            return k
    return None


def _new_like(x, cls, memo):
    base = _builtin_base(cls)
    if base in (list, dict, set):
        y = cls.__new__(cls)
        if base is list:
            y.extend(x if memo is None else [deepcopy(a, memo) for a in x])
        elif base is dict:
            for k, v in x.items():
                y[k if memo is None else deepcopy(k, memo)] = v if memo is None else deepcopy(v, memo)
        else:
            for a in x:
                y.add(a if memo is None else deepcopy(a, memo))
        return y
    args = x.__getnewargs__() if hasattr(x, "__getnewargs__") else (base(x),)
    if memo is not None:
        args = deepcopy(args, memo)
    return cls.__new__(cls, *args)


def replace(obj, /, **changes):
    cls = type(obj)
    func = getattr(cls, "__replace__", None)
    if func is None:
        raise TypeError("replace() does not support " + cls.__name__ + " objects")
    return func(obj, **changes)
