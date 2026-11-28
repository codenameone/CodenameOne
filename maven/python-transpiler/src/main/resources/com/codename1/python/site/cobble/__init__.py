# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""cobble: data classes and visitors, lowered at build time.

The real package generates __init__, __repr__, __eq__, __ne__, __hash__ and
the visitor's _accept methods as source text and exec()s it. Here the
transpiler writes them into each @data / @visitable class body with static
names (codegen.CobbleLowering) and drops the decorator, so data() and
visitable() are only reached by a class it could not lower, which strict
mode refuses anyway. copy() and visitor() dispatch through the generated
_cobble_copy and _accept0 / _accept1 methods.
"""

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


def data(cls):
    return cls


def visitable(cls):
    return cls


class _Visitor(object):
    # One class for every visitor() call, so a class built on it has one
    # known base. A visitor made with args=1 is always called with its one
    # extra argument, so the argument count picks the accept method.
    def visit(self, value, *rest):
        if rest:
            return value._accept1(self, *rest)
        return value._accept0(self)


def visitor(cls, args=None):
    if args is not None and args not in (0, 1):
        raise ValueError("cobble.visitor supports args=0 or args=1")
    return _Visitor


def copy(obj, **kwargs):
    return obj._cobble_copy(**kwargs)
