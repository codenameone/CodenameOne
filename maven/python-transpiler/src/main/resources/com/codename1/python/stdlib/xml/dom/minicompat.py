# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/dom/minicompat.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""xml.dom.minicompat: NodeList, EmptyNodeList and defproperty."""

__all__ = ["NodeList", "EmptyNodeList", "StringTypes", "defproperty"]

StringTypes = (str,)


class NodeList(list):
    __slots__ = ()

    def item(self, index):
        if 0 <= index < len(self):
            return self[index]
        return None

    def _get_length(self):
        return len(self)

    def _set_length(self, value):
        raise AttributeError("attempt to modify read-only attribute 'length'")

    length = property(_get_length, _set_length, doc="The number of nodes in the NodeList.")

    def __setstate__(self, state):
        if state is None:
            state = []
        self[:] = state


class EmptyNodeList(tuple):
    __slots__ = ()

    def __add__(self, other):
        NL = NodeList()
        NL.extend(other)
        return NL

    def __radd__(self, other):
        NL = NodeList()
        NL.extend(other)
        return NL

    def item(self, index):
        return None

    def _get_length(self):
        return 0

    def _set_length(self, value):
        raise AttributeError("attempt to modify read-only attribute 'length'")

    length = property(_get_length, _set_length, doc="The number of nodes in the NodeList.")


def defproperty(klass, name, doc):
    get = getattr(klass, ("_get_" + name))

    def set(self, value, name=name):
        raise AttributeError("attempt to modify read-only attribute " + repr(name))

    assert not hasattr(klass, "_set_" + name), "expected not to find _set_" + name
    prop = property(get, set, doc=doc)
    setattr(klass, name, prop)
