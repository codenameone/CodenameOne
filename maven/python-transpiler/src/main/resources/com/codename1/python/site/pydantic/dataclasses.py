# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""`pydantic.dataclasses.dataclass`: a standard dataclass whose constructor
validates and coerces each field against its annotation."""

import dataclasses as _dc
import typing as _typing

from typing import Any

from pydantic import _Ctx, _v, _BAD, ValidationError, Field


def dataclass(cls=None, **kw):
    def wrap(c):
        c = _dc.dataclass(c, **kw)
        orig = c.__init__

        def __init__(self, *a, **k):
            orig(self, *a, **k)
            hints = _typing.get_type_hints(type(self), include_extras=True)
            ctx = _Ctx()
            ctx.cls = type(self)
            i = 0
            for f in _dc.fields(self):
                v = _v(hints.get(f.name, Any), getattr(self, f.name), (i if i < len(a) else f.name,), ctx)
                i += 1
                if v is not _BAD:
                    object.__setattr__(self, f.name, v)
            if ctx.errs:
                raise ValidationError(type(self).__name__, ctx.errs)
        c.__init__ = __init__
        return c
    if cls is None:
        return wrap
    return wrap(cls)
