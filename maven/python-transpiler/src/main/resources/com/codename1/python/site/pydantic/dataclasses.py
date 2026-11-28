# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""`pydantic.dataclasses.dataclass`: a standard dataclass whose constructor
validates and coerces each field against its annotation."""

import dataclasses as _dc
import typing as _typing

from typing import Any

from pydantic import _Ctx, _v, _BAD, ValidationError, Field


def _cn1_validate(self, npos, fields):
    """Validates and coerces each field of a constructed pydantic dataclass.
    `fields` is the class's own (name, load, store) rows, each accessor
    naming its field literally (written by the transpiler, DataclassInit)."""
    hints = _typing.get_type_hints(type(self), include_extras=True)
    ctx = _Ctx()
    ctx.cls = type(self)
    i = 0
    for name, load, store in fields:
        v = _v(hints.get(name, Any), load(self), (i if i < npos else name,), ctx)
        i += 1
        if v is not _BAD:
            store(self, v)
    if ctx.errs:
        raise ValidationError(type(self).__name__, ctx.errs)


def dataclass(cls=None, **kw):
    def wrap(c):
        c = _dc.dataclass(c, **kw)
        if not c.__dict__.get("__cn1_pd_validated__"):
            raise TypeError("pydantic.dataclasses.dataclass: %s was not lowered at build time; apply "
                            "the decorator by name in a class statement" % c.__name__)
        return c
    if cls is None:
        return wrap
    return wrap(cls)
