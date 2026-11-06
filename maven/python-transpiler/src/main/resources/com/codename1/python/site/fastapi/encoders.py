# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""jsonable_encoder: turn endpoint results into JSON-compatible values."""
import dataclasses
import datetime
import decimal
import enum
import uuid

from pydantic import BaseModel


def jsonable_encoder(obj, include=None, exclude=None, by_alias=True, exclude_unset=False,
                     exclude_defaults=False, exclude_none=False, custom_encoder=None, sqlalchemy_safe=True):
    if custom_encoder:
        for tp, fn in custom_encoder.items():
            if isinstance(obj, tp):
                return fn(obj)
    if isinstance(obj, BaseModel):
        return obj.model_dump(mode="json", include=include, exclude=exclude, by_alias=by_alias,
                              exclude_unset=exclude_unset, exclude_defaults=exclude_defaults,
                              exclude_none=exclude_none)
    if dataclasses.is_dataclass(obj) and not isinstance(obj, type):
        return jsonable_encoder(dataclasses.asdict(obj), by_alias=by_alias, exclude_none=exclude_none,
                                custom_encoder=custom_encoder)
    if isinstance(obj, enum.Enum):
        return obj.value
    if obj is None or isinstance(obj, (str, int, float, bool)):
        return obj
    if isinstance(obj, (bytes, bytearray)):
        return bytes(obj).decode("utf-8")
    if isinstance(obj, decimal.Decimal):
        if obj.as_tuple().exponent >= 0:
            return int(obj)
        return float(obj)
    if isinstance(obj, uuid.UUID):
        return str(obj)
    if isinstance(obj, datetime.datetime) or isinstance(obj, datetime.date) or isinstance(obj, datetime.time):
        return obj.isoformat()
    if isinstance(obj, datetime.timedelta):
        return obj.total_seconds()
    if isinstance(obj, dict):
        out = {}
        for k, v in obj.items():
            if exclude_none and v is None:
                continue
            if include is not None and k not in include:
                continue
            if exclude is not None and k in exclude:
                continue
            out[jsonable_encoder(k, by_alias=by_alias, custom_encoder=custom_encoder)] = jsonable_encoder(
                v, by_alias=by_alias, exclude_unset=exclude_unset, exclude_defaults=exclude_defaults,
                exclude_none=exclude_none, custom_encoder=custom_encoder)
        return out
    if isinstance(obj, (list, tuple, set, frozenset)) or hasattr(obj, "__next__"):
        return [jsonable_encoder(v, by_alias=by_alias, exclude_unset=exclude_unset,
                                 exclude_defaults=exclude_defaults, exclude_none=exclude_none,
                                 custom_encoder=custom_encoder) for v in obj]
    if hasattr(obj, "__dict__"):
        return jsonable_encoder(dict(vars(obj)), by_alias=by_alias, exclude_none=exclude_none,
                                custom_encoder=custom_encoder)
    raise ValueError("Unable to serialize unknown type: %r" % (type(obj),))
