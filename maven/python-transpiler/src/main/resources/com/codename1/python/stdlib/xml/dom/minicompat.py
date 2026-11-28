# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/dom/minicompat.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""xml.dom.minicompat: NodeList and EmptyNodeList. CPython's defproperty is
left out: it binds a property under a computed name, which is reflection,
and minidom declares its read-only properties directly."""

__all__ = ["NodeList", "EmptyNodeList", "StringTypes"]

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
