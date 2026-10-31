# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The attrs namespace: the modern names for the attr API."""

from attr import (NOTHING, Attribute, Converter, Factory, cmp_using, define, evolve, field, fields,
                  fields_dict, frozen, has, make_class, mutable, resolve_types, validate)
import attr as _attr
from attr import converters, exceptions, filters, setters, validators
from attr import attrib as _attrib


def asdict(inst, *, recurse=True, filter=None, value_serializer=None):
    """Like attr.asdict, but collection types are always retained."""
    return _attr.asdict(inst, recurse=recurse, filter=filter, retain_collection_types=True,
                        value_serializer=value_serializer)


def astuple(inst, *, recurse=True, filter=None):
    """Like attr.astuple, but collection types are always retained."""
    return _attr.astuple(inst, recurse=recurse, filter=filter, retain_collection_types=True)


__all__ = ["NOTHING", "Attribute", "Converter", "Factory", "asdict", "astuple", "cmp_using", "converters", "define",
           "evolve", "exceptions", "field", "fields", "fields_dict", "filters", "frozen", "has", "make_class",
           "mutable", "resolve_types", "setters", "validate", "validators"]
