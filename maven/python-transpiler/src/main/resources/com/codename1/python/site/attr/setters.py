# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Hooks run when an attribute of an instance is assigned."""

from attr.exceptions import FrozenAttributeError


def pipe(*setters):
    def wrapped_pipe(instance, attrib, new_value):
        rv = new_value
        for setter in setters:
            rv = setter(instance, attrib, rv)
        return rv
    return wrapped_pipe


def frozen(_, __, ___):
    raise FrozenAttributeError()


def validate(instance, attrib, new_value):
    import attr
    return attr._run_validator(instance, attrib, new_value)


def convert(instance, attrib, new_value):
    import attr
    return attr._convert_value(instance, attrib, new_value)


def NO_OP(_, __, new_value):
    return new_value
