# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Filters for asdict and astuple."""


def _split(what):
    types = []
    names = []
    attrs = []
    for w in what:
        if isinstance(w, type):
            types.append(w)
        elif isinstance(w, str):
            names.append(w)
        else:
            attrs.append(w)
    return tuple(types), names, attrs


def include(*what):
    types, names, attrs = _split(what)

    def include_(attribute, value):
        return value.__class__ in types or attribute in attrs or attribute.name in names
    return include_


def exclude(*what):
    types, names, attrs = _split(what)

    def exclude_(attribute, value):
        return not (value.__class__ in types or attribute in attrs or attribute.name in names)
    return exclude_
