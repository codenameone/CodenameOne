# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Shallow and deep copy operations."""

import copyreg
import _cn1copy


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
    rv = _reduction(x, cls)
    if rv is None:
        return _reconstruct(x, None)
    if isinstance(rv, str):
        return x
    return _reconstruct_from(x, None, *rv)


def _reduction(x, cls):
    """What x reduces to when its class asks for a reduction of its own;
    None when it keeps object's default, which _reconstruct answers by
    copying the compiled fields directly (_cn1copy)."""
    reductor = copyreg.dispatch_table.get(cls)
    if reductor is not None:
        return reductor(x)
    if getattr(cls, "__reduce_ex__", None) is not object.__reduce_ex__:
        return x.__reduce_ex__(4)
    if getattr(cls, "__reduce__", None) is not object.__reduce__:
        return x.__reduce__()
    return None


def _no_setstate(y):
    raise TypeError("cannot copy " + type(y).__name__ + ": its reduction returns a state and the class "
                    "has no __setstate__; applying the state would need setattr with a computed name")


def _reconstruct_from(x, memo, func, args, state=None, listiter=None, dictiter=None):
    deep = memo is not None
    if deep and args:
        args = deepcopy(args, memo)
    y = func(*args)
    if deep:
        memo[id(x)] = y
    if state is not None:
        if deep:
            state = deepcopy(state, memo)
        if hasattr(y, "__setstate__"):
            y.__setstate__(state)
        else:
            _no_setstate(y)
    if listiter is not None:
        for item in listiter:
            y.append(deepcopy(item, memo) if deep else item)
    if dictiter is not None:
        for k, v in dictiter:
            if deep:
                k = deepcopy(k, memo)
                v = deepcopy(v, memo)
            y[k] = v
    return y


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
        rv = _reduction(x, cls)
        if rv is None:
            y = _reconstruct(x, memo)
        elif isinstance(rv, str):
            return x
        else:
            y = _reconstruct_from(x, memo, *rv)
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
    if _builtin_base(cls):
        y = _new_like(x, cls, memo)
    elif getattr(cls, "__getnewargs_ex__", None) is not None:
        args, kwargs = x.__getnewargs_ex__()
        if memo is not None:
            args = deepcopy(args, memo)
            kwargs = deepcopy(kwargs, memo)
        y = cls.__new__(cls, *args, **kwargs)
    elif getattr(cls, "__getnewargs__", None) is not None:
        args = x.__getnewargs__()
        if memo is not None:
            args = deepcopy(args, memo)
        y = cls.__new__(cls, *args)
    else:
        y = cls.__new__(cls)
    if memo is not None:
        memo[id(x)] = y
    if getattr(cls, "__getstate__", None) is not object.__getstate__ or hasattr(y, "__setstate__"):
        state = x.__getstate__()
        if state:
            if memo is not None:
                state = deepcopy(state, memo)
            if hasattr(y, "__setstate__"):
                y.__setstate__(state)
            else:
                _no_setstate(y)
        return y
    # Every field by index through the methods the compiler generates per
    # class, and the attributes held beside them: no name is looked up.
    _cn1copy.copy_state(x, y, memo, None if memo is None else deepcopy)
    return y


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
