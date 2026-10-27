# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Converters for attribute values."""

from attr import NOTHING, Factory

__all__ = ["default_if_none", "optional", "pipe", "to_bool"]


def optional(converter):
    def optional_converter(val):
        if val is None:
            return None
        return converter(val)
    return optional_converter


def pipe(*converters):
    def pipe_converter(val):
        for c in converters:
            val = c(val)
        return val
    return pipe_converter


def default_if_none(default=NOTHING, factory=None):
    if default is NOTHING and factory is None:
        raise TypeError("Must pass either 'default' or 'factory'.")
    if default is not NOTHING and factory is not None:
        raise TypeError("Must pass either 'default' or 'factory' but not both.")
    if factory is not None:
        default = Factory(factory)
    if isinstance(default, Factory):
        if default.takes_self:
            raise ValueError("'takes_self' is not supported for converters.")

        def default_if_none_converter(val):
            if val is not None:
                return val
            return default.factory()
    else:
        def default_if_none_converter(val):
            if val is not None:
                return val
            return default
    return default_if_none_converter


def to_bool(val):
    if isinstance(val, str):
        v = val.lower()
        if v in ("true", "t", "yes", "y", "on", "1"):
            return True
        if v in ("false", "f", "no", "n", "off", "0"):
            return False
    elif val is True or val is False:
        return val
    elif val == 1:
        return True
    elif val == 0:
        return False
    raise ValueError("Cannot convert value to bool: " + repr(val))
