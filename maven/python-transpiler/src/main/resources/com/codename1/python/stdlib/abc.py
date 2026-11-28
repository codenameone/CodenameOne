# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Abstract base classes."""

# ABCMeta is the runtime's own metaclass (runtime class Abc), as CPython's
# is C code in _abc: a class it governs is compiled with a layout, its
# __abstractmethods__ are computed once when the class is made, constructing
# it with any left raises TypeError, and isinstance/issubclass consult
# __subclasshook__, register() and the subclasses' registries. A metaclass
# written here in Python would leave every abstract class without a layout
# and read the abstract names with a computed getattr.
from _cn1op import ABCMeta


def abstractmethod(funcobj):
    funcobj.__isabstractmethod__ = True
    return funcobj


class ABC(metaclass=ABCMeta):
    __slots__ = ()


def get_cache_token():
    return 0


def update_abstractmethods(cls):
    return cls
