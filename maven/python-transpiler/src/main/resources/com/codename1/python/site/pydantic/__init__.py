# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Data validation from type annotations: the commonly used pydantic v2 API.

`BaseModel` reads its fields from class annotations, validates and coerces
input the way pydantic's default lax mode does, and reports failures as a
`ValidationError` whose `errors()` carry pydantic's error types, locations and
messages. Also provided: `Field` constraints, `ConfigDict`, `field_validator`,
`model_validator`, `computed_field`, `field_serializer`, `RootModel`,
`TypeAdapter`, `SecretStr`, the constrained-type aliases and a basic
`model_json_schema`. Written from the public documentation and observed
behaviour; no validation core is compiled, so everything is plain Python.
"""

import copy as _copy
import datetime as _dt
import decimal as _decimal
import enum as _enum
import json as _json
import re as _re
import sys as _sys
import typing as _typing
import uuid as _uuid

from typing import Any, Union, Annotated, Literal

VERSION = "2.14.1"
_ERR_VERSION = "2.14"

_NoneType = type(None)


class _Undefined:
    def __repr__(self):
        return "PydanticUndefined"

    def __bool__(self):
        return False


PydanticUndefined = _Undefined()
_BAD = _Undefined()


# --------------------------------------------------------------------------
# errors
# --------------------------------------------------------------------------

_MSG = {
    "missing": "Field required",
    "int_type": "Input should be a valid integer",
    "int_parsing": "Input should be a valid integer, unable to parse string as an integer",
    "int_from_float": "Input should be a valid integer, got a number with a fractional part",
    "float_type": "Input should be a valid number",
    "float_parsing": "Input should be a valid number, unable to parse string as a number",
    "string_type": "Input should be a valid string",
    "bytes_type": "Input should be a valid bytes",
    "bool_type": "Input should be a valid boolean",
    "bool_parsing": "Input should be a valid boolean, unable to interpret input",
    "none_required": "Input should be None",
    "list_type": "Input should be a valid list",
    "tuple_type": "Input should be a valid tuple",
    "set_type": "Input should be a valid set",
    "frozen_set_type": "Input should be a valid frozenset",
    "dict_type": "Input should be a valid dictionary",
    "extra_forbidden": "Extra inputs are not permitted",
    "model_type": "Input should be a valid dictionary or instance of %(class_name)s",
    "greater_than": "Input should be greater than %(gt)s",
    "greater_than_equal": "Input should be greater than or equal to %(ge)s",
    "less_than": "Input should be less than %(lt)s",
    "less_than_equal": "Input should be less than or equal to %(le)s",
    "multiple_of": "Input should be a multiple of %(multiple_of)s",
    "string_pattern_mismatch": "String should match pattern '%(pattern)s'",
    "frozen_instance": "Instance is frozen",
    "datetime_type": "Input should be a valid datetime",
    "date_type": "Input should be a valid date",
    "time_type": "Input should be a valid time",
    "timedelta_type": "Input should be a valid timedelta",
    "datetime_parsing": "Input should be a valid datetime, invalid character in year",
    "date_from_datetime_parsing": "Input should be a valid date or datetime, invalid character in year",
    "time_parsing": "Input should be in a valid time format, invalid character in hour",
    "timedelta_parsing": "Input should be a valid timedelta, invalid digit in duration",
    "date_from_datetime_inexact": "Datetimes provided to dates should have zero time - e.g. be exact dates",
    "decimal_type": "Decimal input should be an integer, float, string or Decimal object",
    "decimal_parsing": "Input should be a valid decimal",
    "uuid_type": "UUID input should be a string, bytes or UUID object",
    "uuid_parsing": "Input should be a valid UUID, invalid character: expected an optional prefix of `urn:uuid:` followed by [0-9a-fA-F-], found `%(ch)s`",
    "is_instance_of": "Input should be an instance of %(class)s",
    "json_invalid": "Invalid JSON: %(error)s",
    "iterable_type": "Input should be iterable",
}


class ValidationError(ValueError):
    """Raised when validation fails; `errors()` lists every failure."""

    def __init__(self, title, errors):
        ValueError.__init__(self, title)
        self.title = title
        self._errors = errors

    def error_count(self):
        return len(self._errors)

    def errors(self, include_url=True, include_context=True, include_input=True):
        out = []
        for e in self._errors:
            d = {"type": e["type"], "loc": e["loc"], "msg": e["msg"]}
            if include_input:
                d["input"] = e["input"]
            if include_context and e.get("ctx"):
                d["ctx"] = e["ctx"]
            if include_url and e.get("url"):
                d["url"] = e["url"]
            out.append(d)
        return out

    def json(self, indent=None):
        errs = []
        for e in self.errors():
            d = dict(e)
            d["loc"] = list(d["loc"])
            if "ctx" in d:
                d["ctx"] = dict((k, v if isinstance(v, (int, float, str, bool)) or v is None else str(v))
                                for k, v in d["ctx"].items())
            d["input"] = _to_jsonable(d["input"])
            errs.append(d)
        return _jsonify(errs, indent, 0)

    def __str__(self):
        n = len(self._errors)
        lines = ["%d validation error%s for %s" % (n, "" if n == 1 else "s", self.title)]
        for e in self._errors:
            loc = ".".join([str(x) for x in e["loc"]])
            if loc:
                lines.append(loc)
            lines.append("  %s [type=%s, input_value=%s, input_type=%s]" % (
                e["msg"], e["type"], _short_repr(e["input"]), type(e["input"]).__name__))
            if e.get("url"):
                lines.append("    For further information visit " + e["url"])
        return "\n".join(lines)

    def __repr__(self):
        return str(self)


def _short_repr(v):
    r = repr(v)
    if len(r) > 50:
        r = r[:24] + "..." + r[-23:]
    return r


def _url(typ):
    return "https://errors.pydantic.dev/" + _ERR_VERSION + "/v/" + typ


class _Ctx:
    def __init__(self, strict=False, jsonmode=False, context=None, from_attributes=False):
        self.errs = []
        self.strict = strict
        self.json = jsonmode
        self.context = context
        self.from_attributes = from_attributes
        self.cfg = {}
        self.cls = None


def _E(c, loc, typ, inp, ctx=None, msg=None):
    if msg is None:
        msg = _MSG[typ]
        if ctx:
            msg = msg % ctx
    d = {"type": typ, "loc": tuple(loc), "msg": msg, "input": inp, "url": _url(typ)}
    if ctx:
        d["ctx"] = ctx
    c.errs.append(d)
    return _BAD


def _plural(n, word):
    return "%d %s%s" % (n, word, "" if n == 1 else "s")


# --------------------------------------------------------------------------
# field declarations
# --------------------------------------------------------------------------

_CONSTRAINT_KEYS = ("gt", "ge", "lt", "le", "multiple_of", "min_length", "max_length", "pattern")


class FieldInfo:
    """What `Field(...)` records about one field."""

    def __init__(self, default=PydanticUndefined, **kw):
        self.annotation = None
        self._full = None
        self.default = default
        self.default_factory = kw.get("default_factory")
        self.alias = kw.get("alias")
        self.validation_alias = kw.get("validation_alias")
        self.serialization_alias = kw.get("serialization_alias")
        self.title = kw.get("title")
        self.description = kw.get("description")
        self.examples = kw.get("examples")
        self.exclude = kw.get("exclude")
        self.repr = kw.get("repr", True)
        self.frozen = kw.get("frozen")
        self.strict = kw.get("strict")
        self.validate_default = kw.get("validate_default")
        self.json_schema_extra = kw.get("json_schema_extra")
        self.constraints = {}
        for k in _CONSTRAINT_KEYS:
            if kw.get(k) is not None:
                self.constraints[k] = kw[k]
        self.metadata = []

    def is_required(self):
        return self.default is PydanticUndefined and self.default_factory is None

    def get_default(self, call_default_factory=False):
        if self.default_factory is not None:
            return self.default_factory() if call_default_factory else None
        if self.default is PydanticUndefined:
            return None
        return self.default

    def __repr__(self):
        parts = ["annotation=" + _tname(self.annotation)]
        if self.is_required():
            parts.append("required=True")
        elif self.default_factory is not None:
            parts.append("default_factory=" + getattr(self.default_factory, "__name__", "factory"))
        else:
            parts.append("default=" + repr(self.default))
        if self.alias is not None:
            parts.append("alias=" + repr(self.alias))
        if self.description is not None:
            parts.append("description=" + repr(self.description))
        return "FieldInfo(" + ", ".join(parts) + ")"


def Field(default=PydanticUndefined, **kw):
    if default is Ellipsis:
        default = PydanticUndefined
    if "alias" in kw and kw["alias"] is not None:
        kw.setdefault("validation_alias", kw["alias"])
        kw.setdefault("serialization_alias", kw["alias"])
    return FieldInfo(default, **kw)


class _PrivateAttr:
    def __init__(self, default, factory):
        self.default = default
        self.factory = factory


def PrivateAttr(default=PydanticUndefined, default_factory=None):
    return _PrivateAttr(default, default_factory)


def ConfigDict(**kw):
    return dict(kw)


class Strict:
    def __init__(self, strict=True):
        self.strict = strict


class AfterValidator:
    def __init__(self, func):
        self.func = func


class BeforeValidator:
    def __init__(self, func):
        self.func = func


class PlainValidator:
    def __init__(self, func):
        self.func = func


class ValidationInfo:
    def __init__(self, c, field_name=None, data=None):
        self.context = c.context
        self.config = c.cfg
        self.mode = "json" if c.json else "python"
        self.data = data if data is not None else {}
        self.field_name = field_name


FieldValidationInfo = ValidationInfo


class _Decl:
    def __init__(self, kind, func, fields=(), mode="after", extra=None):
        self.kind = kind
        self.func = func
        self.fields = fields
        self.mode = mode
        self.extra = extra


def field_validator(*fields, mode="after", check_fields=None):
    def dec(f):
        return _Decl("field", f, fields, mode)
    return dec


def model_validator(*, mode):
    def dec(f):
        return _Decl("model", f, (), mode)
    return dec


def field_serializer(*fields, mode="plain", return_type=None, when_used="always", check_fields=None):
    def dec(f):
        return _Decl("fser", f, fields, mode, when_used)
    return dec


def model_serializer(f=None, *, mode="plain", when_used="always", return_type=None):
    def dec(fn):
        return _Decl("mser", fn, (), mode)
    if f is not None:
        return dec(f)
    return dec


_COMPUTED = []


def computed_field(func=None, *, alias=None, title=None, description=None, repr=True, return_type=None):
    def dec(f):
        fget = f.fget if isinstance(f, property) else f
        _COMPUTED.append((fget, alias, repr))
        if isinstance(f, property):
            return f
        return property(f)
    if func is not None:
        return dec(func)
    return dec


_ARITY = {}


def _invoke(func, args, info):
    n = _ARITY.get(func)
    if n == 1:
        return func(*(args + (info,)))
    if n == 0:
        return func(*args)
    try:
        r = func(*args)
        _ARITY[func] = 0
        return r
    except TypeError as e:
        s = str(e)
        if "missing" in s and "required positional argument" in s:
            _ARITY[func] = 1
            return func(*(args + (info,)))
        raise


def _guard(c, loc, inp, fn):
    """Runs fn(); a validator's ValueError or AssertionError becomes an error."""
    try:
        return fn()
    except ValidationError as e:
        for d in e._errors:
            nd = dict(d)
            nd["loc"] = tuple(loc) + tuple(d["loc"])
            c.errs.append(nd)
        return _BAD
    except ValueError as e:
        return _E(c, loc, "value_error", inp, {"error": e}, "Value error, " + str(e))
    except AssertionError as e:
        s = str(e)
        return _E(c, loc, "assertion_error", inp, {"error": e},
                  "Assertion failed" + (", " + s if s else ""))


# --------------------------------------------------------------------------
# type helpers
# --------------------------------------------------------------------------

def _tname(tp):
    if tp is None or tp is _NoneType:
        return "None"
    if isinstance(tp, type) and getattr(tp, "__origin__", None) is None:
        return tp.__name__
    return repr(tp)


def _tag(tp):
    if tp is _NoneType or tp is None:
        return "none"
    org = _typing.get_origin(tp)
    if org is Annotated:
        return _tag(_typing.get_args(tp)[0])
    if org is Literal:
        return "literal[" + ",".join([repr(a) for a in _typing.get_args(tp)]) + "]"
    if org is not None and isinstance(org, type):
        args = _typing.get_args(tp)
        if args:
            return org.__name__ + "[" + ",".join([_tag(a) if a is not Ellipsis else "..." for a in args]) + "]"
        return org.__name__
    if isinstance(tp, type):
        return tp.__name__
    return _tname(tp)


def _unwrap(tp):
    """(base type, list of Annotated metadata) for any depth of Annotated."""
    metas = []
    while _typing.get_origin(tp) is Annotated:
        a = _typing.get_args(tp)
        metas = list(a[1:]) + metas
        tp = a[0]
    return tp, metas


def _is_classvar(ann):
    if isinstance(ann, str):
        return ann.startswith("ClassVar") or ann.startswith("typing.ClassVar")
    return ann is _typing.ClassVar or _typing.get_origin(ann) is _typing.ClassVar


def _resolve_forward(tp, c):
    text = tp if isinstance(tp, str) else tp.__forward_arg__
    ns = {}
    cls = c.cls
    if cls is not None:
        m = _sys.modules.get(cls.__module__)
        if m is not None:
            ns.update(getattr(m, "__dict__", {}))
        ns[cls.__name__] = cls
    return _typing._resolve_hint(text, ns)


# --------------------------------------------------------------------------
# validation
# --------------------------------------------------------------------------

_TRUE = ("true", "t", "yes", "y", "on", "1")
_FALSE = ("false", "f", "no", "n", "off", "0")


def _parse_datetime(s):
    s = s.strip()
    if s.endswith("Z") or s.endswith("z"):
        s = s[:-1] + "+00:00"
    if len(s) > 10 and s[10] in " _t":
        s = s[:10] + "T" + s[11:]
    return _dt.datetime.fromisoformat(s)


def _parse_duration(s):
    m = _re.match(r"^(-)?P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$", s.strip())
    if m is None or s.strip() in ("P", "PT"):
        raise ValueError("bad duration")
    sign = -1 if m.group(1) else 1
    td = _dt.timedelta(days=int(m.group(2) or 0), hours=int(m.group(3) or 0),
                       minutes=int(m.group(4) or 0), seconds=float(m.group(5) or 0))
    return td * sign


def _v(tp, val, loc, c):
    if tp is None:
        tp = _NoneType
    if isinstance(tp, str) or hasattr(tp, "__forward_arg__"):
        tp = _resolve_forward(tp, c)
    if tp is Any or tp is object:
        return val
    if hasattr(tp, "__supertype__"):
        return _v(tp.__supertype__, val, loc, c)
    if hasattr(tp, "__value__") and hasattr(tp, "__type_params__"):
        return _v(tp.__value__, val, loc, c)
    if isinstance(tp, _typing.TypeVar):
        b = getattr(tp, "__bound__", None)
        return _v(b, val, loc, c) if b is not None else val
    org = _typing.get_origin(tp)
    if org is Annotated:
        return _vannotated(tp, val, loc, c)
    if org is Union:
        return _vunion(tp, val, loc, c)
    if org is Literal:
        for a in _typing.get_args(tp):
            if val == a and type(val) is type(a):
                return a
        for a in _typing.get_args(tp):
            if val == a and not isinstance(val, bool) and not isinstance(a, bool):
                return a
        exp = _join_or([repr(a) for a in _typing.get_args(tp)])
        return _E(c, loc, "literal_error", val, {"expected": exp}, "Input should be " + exp)
    if org is not None:
        return _vgeneric(org, _typing.get_args(tp), val, loc, c)
    if isinstance(tp, ModelMetaclass):
        return _vmodel(tp, val, loc, c)
    if isinstance(tp, type):
        return _vplain(tp, val, loc, c)
    return val


def _join_or(items):
    if len(items) == 1:
        return items[0]
    return ", ".join(items[:-1]) + " or " + items[-1]


def _vannotated(tp, val, loc, c):
    base, metas = _unwrap(tp)
    cons = {}
    before = []
    after = []
    prec = []
    plain = None
    strict = None
    for m in metas:
        if isinstance(m, FieldInfo):
            cons.update(m.constraints)
            if m.strict is not None:
                strict = m.strict
        elif isinstance(m, Strict):
            strict = m.strict
        elif isinstance(m, BeforeValidator):
            before.append(m.func)
        elif isinstance(m, AfterValidator):
            after.append(m.func)
        elif isinstance(m, PlainValidator):
            plain = m.func
        elif isinstance(m, _StringConstraints):
            cons.update(m.cons)
            if m.strip_whitespace:
                prec.append(_strip)
            if m.to_lower:
                prec.append(_lower)
            if m.to_upper:
                prec.append(_upper)
            if m.strict:
                strict = True
    info = ValidationInfo(c)
    v = val
    for f in before:
        v = _guard(c, loc, val, lambda f=f, v=v: _invoke(f, (v,), info))
        if v is _BAD:
            return _BAD
    if plain is not None:
        v = _guard(c, loc, val, lambda: _invoke(plain, (v,), info))
        if v is _BAD:
            return _BAD
    else:
        was = c.strict
        if strict is not None:
            c.strict = strict
        v = _v(base, v, loc, c)
        c.strict = was
        if v is _BAD:
            return _BAD
        for f in prec:
            v = f(v)
        if cons:
            v = _constrain(v, cons, loc, c)
            if v is _BAD:
                return _BAD
    for f in after:
        v = _guard(c, loc, val, lambda f=f, v=v: _invoke(f, (v,), info))
        if v is _BAD:
            return _BAD
    return v


def _strip(v):
    return v.strip() if isinstance(v, str) else v


def _lower(v):
    return v.lower() if isinstance(v, str) else v


def _upper(v):
    return v.upper() if isinstance(v, str) else v


def _vunion(tp, val, loc, c):
    members = [a for a in _typing.get_args(tp) if a is not _NoneType and a is not None]
    nullable = len(members) != len(_typing.get_args(tp))
    if nullable and val is None:
        return None
    if len(members) == 1:
        return _v(members[0], val, loc, c)
    saved = c.errs
    was = c.strict
    c.strict = True
    for m in members:
        c.errs = []
        r = _v(m, val, loc, c)
        if not c.errs:
            c.strict = was
            c.errs = saved
            return r
    c.strict = was
    if was:
        allerrs = []
        for m in members:
            c.errs = []
            _v(m, val, loc + (_tag(m),), c)
            allerrs.extend(c.errs)
        c.errs = saved
        saved.extend(allerrs)
        return _BAD
    allerrs = []
    for m in members:
        c.errs = []
        r = _v(m, val, loc + (_tag(m),), c)
        if not c.errs:
            c.errs = saved
            return r
        allerrs.extend(c.errs)
    c.errs = saved
    saved.extend(allerrs)
    return _BAD


def _items_of(val, c, kinds):
    """The iterable forms lax mode accepts for a sequence-like field."""
    if isinstance(val, kinds):
        return list(val)
    return None


def _vgeneric(org, args, val, loc, c):
    nm = getattr(org, "__name__", "")
    if org is list or nm in ("Sequence", "MutableSequence", "Collection", "Iterable", "Iterator"):
        return _vlist(list, args, val, loc, c)
    if org is set or nm in ("Set", "MutableSet", "AbstractSet"):
        return _vlist(set, args, val, loc, c)
    if org is frozenset:
        return _vlist(frozenset, args, val, loc, c)
    if org is tuple:
        return _vtuple(args, val, loc, c)
    if org is dict or nm in ("Mapping", "MutableMapping"):
        return _vdict(args, val, loc, c)
    if org is type:
        return val
    if isinstance(org, type):
        return _vplain(org, val, loc, c)
    return val


def _seq_err(kind, val, loc, c):
    if kind is list:
        t = "list_type"
        m = "Input should be a valid array" if c.json else None
    elif kind is set:
        t = "set_type"
        m = "Input should be a valid set" if not c.json else "Input should be a valid set"
    elif kind is frozenset:
        t = "frozen_set_type"
        m = None
    else:
        t = "tuple_type"
        m = "Input should be a valid tuple" if not c.json else "Input should be a valid array"
    return _E(c, loc, t, val, None, m)


def _vlist(kind, args, val, loc, c):
    if isinstance(val, kind) and not c.strict or (c.strict and type(val) is kind):
        items = list(val)
    elif not c.strict and isinstance(val, (list, tuple, set, frozenset)) and not (kind is list and c.json):
        items = list(val)
    elif not c.strict and not isinstance(val, (str, bytes, dict)) and hasattr(val, "__iter__") \
            and not isinstance(val, (int, float)):
        items = list(val)
    else:
        return _seq_err(kind, val, loc, c)
    et = args[0] if args else Any
    out = []
    bad = False
    i = 0
    for x in items:
        r = _v(et, x, loc + (i,), c)
        if r is _BAD:
            bad = True
        else:
            out.append(r)
        i += 1
    if bad:
        return _BAD
    if kind is list:
        return out
    return kind(out)


def _vtuple(args, val, loc, c):
    if isinstance(val, tuple) or (not c.strict and isinstance(val, (list, set, frozenset))):
        items = list(val)
    else:
        return _seq_err(tuple, val, loc, c)
    out = []
    bad = False
    if not args or (len(args) == 2 and args[1] is Ellipsis):
        et = args[0] if args else Any
        i = 0
        for x in items:
            r = _v(et, x, loc + (i,), c)
            if r is _BAD:
                bad = True
            else:
                out.append(r)
            i += 1
        return _BAD if bad else tuple(out)
    if args == ((),):
        args = ()
    if len(items) != len(args):
        if len(items) < len(args):
            for i in range(len(items), len(args)):
                _E(c, loc + (i,), "missing", val, None, "Field required")
        else:
            _E(c, loc, "too_long", val, {"field_type": "Tuple", "max_length": len(args), "actual_length": len(items)},
               "Tuple should have at most " + _plural(len(args), "item") + " after validation, not " + str(len(items)))
        return _BAD
    for i in range(len(args)):
        r = _v(args[i], items[i], loc + (i,), c)
        if r is _BAD:
            bad = True
        else:
            out.append(r)
    return _BAD if bad else tuple(out)


def _vdict(args, val, loc, c):
    if not isinstance(val, dict):
        return _E(c, loc, "dict_type", val, None, "Input should be an object" if c.json else None)
    kt = args[0] if args else Any
    vt = args[1] if len(args) > 1 else Any
    out = {}
    bad = False
    for k, x in val.items():
        k2 = _v(kt, k, loc + (k, "[key]"), c)
        v2 = _v(vt, x, loc + (k,), c)
        if k2 is _BAD or v2 is _BAD:
            bad = True
        else:
            out[k2] = v2
    return _BAD if bad else out


def _vplain(tp, val, loc, c):
    cfg = c.cfg
    if tp is _NoneType:
        if val is None:
            return None
        return _E(c, loc, "none_required", val)
    if tp is bool:
        if isinstance(val, bool):
            return val
        if c.strict:
            return _E(c, loc, "bool_type", val)
        if isinstance(val, str):
            s = val.strip().lower()
            if s in _TRUE:
                return True
            if s in _FALSE:
                return False
            return _E(c, loc, "bool_parsing", val)
        if isinstance(val, (int, float)):
            if val == 1:
                return True
            if val == 0:
                return False
            return _E(c, loc, "bool_parsing", val)
        return _E(c, loc, "bool_type", val)
    if tp is int:
        if isinstance(val, bool):
            if c.strict:
                return _E(c, loc, "int_type", val)
            return int(val)
        if isinstance(val, int):
            return val
        if c.strict:
            return _E(c, loc, "int_type", val)
        if isinstance(val, float):
            if val != val or val in (float("inf"), float("-inf")):
                return _E(c, loc, "finite_number", val, None, "Input should be a finite number")
            if val == int(val):
                return int(val)
            return _E(c, loc, "int_from_float", val)
        if isinstance(val, str):
            s = val.strip()
            try:
                return int(s)
            except ValueError:
                pass
            try:
                f = float(s)
                if f == f and f not in (float("inf"), float("-inf")) and f == int(f):
                    return int(f)
                if f == f and f not in (float("inf"), float("-inf")):
                    return _E(c, loc, "int_from_float", val, None, _MSG["int_parsing"])
            except ValueError:
                pass
            return _E(c, loc, "int_parsing", val)
        if isinstance(val, _decimal.Decimal) and val == int(val):
            return int(val)
        return _E(c, loc, "int_type", val)
    if tp is float:
        if isinstance(val, float):
            return val
        if isinstance(val, bool):
            if c.strict:
                return _E(c, loc, "float_type", val)
            return float(val)
        if isinstance(val, int):
            return float(val)
        if c.strict:
            return _E(c, loc, "float_type", val)
        if isinstance(val, str):
            try:
                return float(val.strip())
            except ValueError:
                return _E(c, loc, "float_parsing", val)
        if isinstance(val, _decimal.Decimal):
            return float(val)
        return _E(c, loc, "float_type", val)
    if tp is str:
        if isinstance(val, str):
            s = val
            if isinstance(val, _enum.Enum):
                s = str(val.value)
        elif not c.strict and isinstance(val, (bytes, bytearray)):
            try:
                s = bytes(val).decode("utf-8")
            except Exception:
                return _E(c, loc, "string_unicode", val, None, "Input should be a valid string, unable to parse raw data as a unicode string")
        else:
            return _E(c, loc, "string_type", val)
        if cfg.get("str_strip_whitespace"):
            s = s.strip()
        if cfg.get("str_to_lower"):
            s = s.lower()
        if cfg.get("str_to_upper"):
            s = s.upper()
        return s
    if tp is bytes:
        if isinstance(val, bytes):
            return val
        if not c.strict and isinstance(val, str):
            return val.encode("utf-8")
        if not c.strict and isinstance(val, bytearray):
            return bytes(val)
        return _E(c, loc, "bytes_type", val)
    if tp is list or tp is set or tp is frozenset:
        return _vlist(tp, (), val, loc, c)
    if tp is tuple:
        return _vtuple((), val, loc, c)
    if tp is dict:
        return _vdict((), val, loc, c)
    if tp is _dt.datetime:
        if isinstance(val, _dt.datetime):
            return val
        if c.strict:
            return _E(c, loc, "datetime_type", val)
        if isinstance(val, str):
            try:
                return _parse_datetime(val)
            except Exception:
                if len(val.strip()) < 10:
                    return _E(c, loc, "datetime_from_date_parsing", val, None,
                              "Input should be a valid datetime or date, input is too short")
                return _E(c, loc, "datetime_parsing", val)
        if isinstance(val, (int, float)) and not isinstance(val, bool):
            return _dt.datetime.fromtimestamp(val, tz=_dt.timezone.utc)
        return _E(c, loc, "datetime_type", val)
    if tp is _dt.date:
        if isinstance(val, _dt.datetime):
            if val.hour == 0 and val.minute == 0 and val.second == 0 and val.microsecond == 0:
                return val.date()
            return _E(c, loc, "date_from_datetime_inexact", val)
        if isinstance(val, _dt.date):
            return val
        if c.strict:
            return _E(c, loc, "date_type", val)
        if isinstance(val, str):
            try:
                if len(val.strip()) == 10:
                    return _dt.date.fromisoformat(val.strip())
                d = _parse_datetime(val)
                if d.hour == 0 and d.minute == 0 and d.second == 0 and d.microsecond == 0:
                    return d.date()
                return _E(c, loc, "date_from_datetime_inexact", val)
            except Exception:
                return _E(c, loc, "date_from_datetime_parsing", val)
        return _E(c, loc, "date_type", val)
    if tp is _dt.time:
        if isinstance(val, _dt.time):
            return val
        if not c.strict and isinstance(val, str):
            try:
                return _dt.time.fromisoformat(val.strip())
            except Exception:
                return _E(c, loc, "time_parsing", val)
        return _E(c, loc, "time_type", val)
    if tp is _dt.timedelta:
        if isinstance(val, _dt.timedelta):
            return val
        if not c.strict:
            if isinstance(val, (int, float)) and not isinstance(val, bool):
                return _dt.timedelta(seconds=val)
            if isinstance(val, str):
                try:
                    return _parse_duration(val)
                except Exception:
                    return _E(c, loc, "time_delta_parsing", val, None, _MSG["timedelta_parsing"])
        return _E(c, loc, "time_delta_type", val, None, _MSG["timedelta_type"])
    if tp is _decimal.Decimal:
        if isinstance(val, _decimal.Decimal):
            return val
        if not c.strict and isinstance(val, (int, float, str)) and not isinstance(val, bool):
            try:
                return _decimal.Decimal(str(val).strip())
            except Exception:
                return _E(c, loc, "decimal_parsing", val)
        return _E(c, loc, "decimal_type", val)
    if tp is _uuid.UUID:
        if isinstance(val, _uuid.UUID):
            return val
        if not c.strict and isinstance(val, str):
            try:
                return _uuid.UUID(val)
            except Exception:
                return _E(c, loc, "uuid_parsing", val, {"ch": val[:1]})
        return _E(c, loc, "uuid_type", val)
    if issubclass(tp, _enum.Enum):
        if isinstance(val, tp):
            return val
        vals = [m.value for m in tp]
        if not c.strict or c.json:
            try:
                return tp(val)
            except Exception:
                pass
        exp = _join_or([repr(x) for x in vals])
        return _E(c, loc, "enum", val, {"expected": exp}, "Input should be " + exp)
    if tp is SecretStr or tp is SecretBytes:
        if isinstance(val, tp):
            return val
        inner = str if tp is SecretStr else bytes
        r = _vplain(inner, val, loc, c)
        return _BAD if r is _BAD else tp(r)
    if isinstance(val, tp):
        return val
    return _E(c, loc, "is_instance_of", val, {"class": tp.__name__}, "Input should be an instance of " + tp.__name__)


def _constrain(v, cons, loc, c):
    if isinstance(v, bool):
        return v
    if isinstance(v, (int, float, _decimal.Decimal)):
        if "gt" in cons and not v > cons["gt"]:
            return _E(c, loc, "greater_than", v, {"gt": cons["gt"]})
        if "ge" in cons and not v >= cons["ge"]:
            return _E(c, loc, "greater_than_equal", v, {"ge": cons["ge"]})
        if "lt" in cons and not v < cons["lt"]:
            return _E(c, loc, "less_than", v, {"lt": cons["lt"]})
        if "le" in cons and not v <= cons["le"]:
            return _E(c, loc, "less_than_equal", v, {"le": cons["le"]})
        if "multiple_of" in cons:
            m = cons["multiple_of"]
            if isinstance(v, int) and isinstance(m, int):
                bad = v % m != 0
            else:
                q = v / m
                bad = abs(q - round(q)) > 1e-9
            if bad:
                return _E(c, loc, "multiple_of", v, {"multiple_of": m})
        return v
    if isinstance(v, (str, bytes)):
        n = len(v)
        if "min_length" in cons and n < cons["min_length"]:
            return _E(c, loc, "string_too_short", v, {"min_length": cons["min_length"]},
                      "String should have at least " + _plural(cons["min_length"], "character"))
        if "max_length" in cons and n > cons["max_length"]:
            return _E(c, loc, "string_too_long", v, {"max_length": cons["max_length"]},
                      "String should have at most " + _plural(cons["max_length"], "character"))
        if "pattern" in cons and isinstance(v, str):
            if _re.search(cons["pattern"], v) is None:
                return _E(c, loc, "string_pattern_mismatch", v, {"pattern": cons["pattern"]})
        return v
    if isinstance(v, (list, tuple, set, frozenset, dict)):
        n = len(v)
        ft = {list: "List", tuple: "Tuple", set: "Set", frozenset: "Frozenset", dict: "Dictionary"}.get(type(v), "List")
        if "min_length" in cons and n < cons["min_length"]:
            return _E(c, loc, "too_short", v, {"field_type": ft, "min_length": cons["min_length"], "actual_length": n},
                      ft + " should have at least " + _plural(cons["min_length"], "item") + " after validation, not " + str(n))
        if "max_length" in cons and n > cons["max_length"]:
            return _E(c, loc, "too_long", v, {"field_type": ft, "max_length": cons["max_length"], "actual_length": n},
                      ft + " should have at most " + _plural(cons["max_length"], "item") + " after validation, not " + str(n))
    return v


# --------------------------------------------------------------------------
# models
# --------------------------------------------------------------------------

def _is_private(name):
    return name.startswith("_")


def _merge_annotation(fi, tp):
    fi._full = tp
    base, metas = _unwrap(tp)
    fi.annotation = base
    fi.metadata = [m for m in metas if not isinstance(m, FieldInfo)]
    for m in metas:
        if isinstance(m, FieldInfo):
            for k in ("alias", "validation_alias", "serialization_alias", "title", "description", "examples"):
                if getattr(fi, k) is None and getattr(m, k) is not None:
                    setattr(fi, k, getattr(m, k))
            if fi.default is PydanticUndefined and fi.default_factory is None:
                fi.default = m.default
                fi.default_factory = m.default_factory
            if m.exclude is not None and fi.exclude is None:
                fi.exclude = m.exclude


def _complete(cls):
    if cls.__dict__.get("__pydantic_complete__"):
        return True
    try:
        hints = _typing.get_type_hints(cls, include_extras=True)
    except Exception:
        return False
    for name, fi in cls.model_fields.items():
        if name in hints:
            _merge_annotation(fi, hints[name])
        elif fi._full is None:
            _merge_annotation(fi, fi.annotation if fi.annotation is not None else Any)
    type.__setattr__(cls, "__pydantic_complete__", True)
    return True


def _ensure(cls):
    if not cls.__dict__.get("__pydantic_complete__"):
        if not _complete(cls):
            for fi in cls.model_fields.values():
                if fi._full is None:
                    raise PydanticUserError(
                        "`%s` is not fully defined; you should define all referenced types, then call `%s.model_rebuild()`"
                        % (cls.__name__, cls.__name__))


class PydanticUserError(TypeError):
    def __init__(self, message, code=None):
        TypeError.__init__(self, message)
        self.code = code


class PydanticUndefinedAnnotation(NameError):
    pass


class ModelMetaclass(type):
    def __new__(mcs, name, bases, ns, **kwargs):
        if not any(isinstance(b, ModelMetaclass) for b in bases):
            return type.__new__(mcs, name, bases, ns)
        annotations = ns.get("__annotations__", {})
        config = {}
        for b in reversed(bases):
            config.update(getattr(b, "model_config", {}))
        config.update(ns.get("model_config", {}))
        config.update(kwargs)
        ns["model_config"] = config
        own = {}
        privates = {}
        for fname in list(annotations):
            ann = annotations[fname]
            if _is_classvar(ann):
                continue
            if _is_private(fname):
                d = ns.get(fname, PydanticUndefined)
                if isinstance(d, _PrivateAttr):
                    ns.pop(fname)
                    privates[fname] = d
                elif d is not PydanticUndefined:
                    privates[fname] = _PrivateAttr(d, None)
                continue
            d = ns.pop(fname, PydanticUndefined)
            if isinstance(d, FieldInfo):
                fi = d
            else:
                fi = FieldInfo(d)
            fi.annotation = ann
            own[fname] = fi
        for k in list(ns):
            v = ns[k]
            if isinstance(v, _PrivateAttr):
                ns.pop(k)
                privates[k] = v
        decls = {"field": [], "model": [], "fser": [], "mser": []}
        for b in bases:
            bd = getattr(b, "__pydantic_decorators__", None)
            if bd:
                for kind in decls:
                    decls[kind].extend(bd[kind])
        computed = {}
        for b in bases:
            computed.update(getattr(b, "__pydantic_computed__", {}))
        for k in list(ns):
            v = ns[k]
            if isinstance(v, _Decl):
                f = getattr(v.func, "__func__", v.func)
                decls[v.kind].append(_Decl(v.kind, f, v.fields, v.mode, v.extra))
                if v.kind == "model" and v.mode == "after":
                    ns[k] = f
                elif v.kind in ("field", "model"):
                    ns[k] = classmethod(f)
                else:
                    ns[k] = f
            elif isinstance(v, property):
                for (g, alias, rep) in _COMPUTED:
                    if g is v.fget:
                        computed[k] = (alias, rep)
        cls = type.__new__(mcs, name, bases, ns)
        fields = {}
        for b in reversed(bases):
            for k, fi in getattr(b, "model_fields", {}).items():
                fields[k] = fi
        for k, fi in own.items():
            fields[k] = fi
        pv = {}
        for b in bases:
            pv.update(getattr(b, "__private_attributes__", {}))
        pv.update(privates)
        type.__setattr__(cls, "model_fields", fields)
        type.__setattr__(cls, "__pydantic_decorators__", decls)
        type.__setattr__(cls, "__pydantic_computed__", computed)
        type.__setattr__(cls, "__private_attributes__", pv)
        type.__setattr__(cls, "__pydantic_complete__", False)
        _complete(cls)
        if config.get("frozen") and "__hash__" not in ns:
            type.__setattr__(cls, "__hash__", BaseModel._frozen_hash)
        return cls

    def __init__(cls, name, bases, ns, **kwargs):
        type.__init__(cls, name, bases, ns)


class _Opts:
    def __init__(self, mode="python", by_alias=False, exclude_unset=False, exclude_defaults=False,
                 exclude_none=False, round_trip=False):
        self.mode = mode
        self.by_alias = by_alias
        self.exclude_unset = exclude_unset
        self.exclude_defaults = exclude_defaults
        self.exclude_none = exclude_none


def _fill(cls, data, loc, c):
    """Validates a mapping against cls's fields. Returns (values, fields_set,
    extras) or None, with the problems appended to c.errs."""
    cfg = cls.model_config
    saved_cfg = c.cfg
    saved_cls = c.cls
    c.cfg = cfg
    c.cls = cls
    n0 = len(c.errs)
    values = {}
    fset = set()
    used = set()
    by_name = cfg.get("populate_by_name") or cfg.get("validate_by_name")
    root = _is_root(cls)
    for name, fi in cls.model_fields.items():
        alias = fi.validation_alias if fi.validation_alias is not None else fi.alias
        key = _MISSING_KEY
        if alias is not None and alias in data:
            key = alias
        elif (alias is None or by_name) and name in data:
            key = name
        if key is _MISSING_KEY:
            if fi.default_factory is not None:
                values[name] = fi.default_factory()
            elif fi.default is PydanticUndefined:
                _E(c, loc + ((alias if alias is not None else name),), "missing", data)
            else:
                d = fi.default
                if isinstance(d, (list, dict, set, bytearray)):
                    d = _copy.deepcopy(d)
                if fi.validate_default or cfg.get("validate_default"):
                    d = _vfield(cls, fi, name, d, loc + (name,), c, values)
                    if d is _BAD:
                        continue
                values[name] = d
            continue
        used.add(key)
        v = _vfield(cls, fi, name, data[key], loc if root else loc + (key,), c, values)
        if v is not _BAD:
            values[name] = v
            fset.add(name)
    extra_mode = cfg.get("extra") or "ignore"
    extras = {}
    for k in data:
        if k in used:
            continue
        if extra_mode == "forbid":
            _E(c, loc + (k,), "extra_forbidden", data[k])
        elif extra_mode == "allow":
            extras[k] = data[k]
    c.cfg = saved_cfg
    c.cls = saved_cls
    if len(c.errs) > n0:
        return None
    return values, fset, extras


_MISSING_KEY = _Undefined()


def _vfield(cls, fi, name, raw, loc, c, values):
    info = ValidationInfo(c, name, dict(values))
    decls = cls.__pydantic_decorators__["field"]
    v = raw
    for d in decls:
        if d.mode == "before" and (name in d.fields or "*" in d.fields):
            v = _guard(c, loc, raw, lambda d=d, v=v: _invoke(d.func, (cls, v), info))
            if v is _BAD:
                return _BAD
    plain = None
    for d in decls:
        if d.mode == "plain" and (name in d.fields or "*" in d.fields):
            plain = d
    if plain is not None:
        v = _guard(c, loc, raw, lambda: _invoke(plain.func, (cls, v), info))
        if v is _BAD:
            return _BAD
    else:
        was = c.strict
        if fi.strict is not None:
            c.strict = fi.strict
        elif c.cfg.get("strict"):
            c.strict = True
        full = fi._full if fi._full is not None else Any
        v = _v(full, v, loc, c)
        c.strict = was
        if v is _BAD:
            return _BAD
        if fi.constraints and v is not None:
            v = _constrain(v, fi.constraints, loc, c)
            if v is _BAD:
                return _BAD
    for d in decls:
        if d.mode == "after" and (name in d.fields or "*" in d.fields):
            v = _guard(c, loc, raw, lambda d=d, v=v: _invoke(d.func, (cls, v), info))
            if v is _BAD:
                return _BAD
    if c.cfg.get("use_enum_values") and isinstance(v, _enum.Enum):
        v = v.value
    return v


def _populate(obj, cls, values, fset, extras):
    for k, v in values.items():
        object.__setattr__(obj, k, v)
    object.__setattr__(obj, "__pydantic_fields_set__", fset)
    object.__setattr__(obj, "__pydantic_extra__", extras if cls.model_config.get("extra") == "allow" else None)
    for k, p in cls.__private_attributes__.items():
        if p.factory is not None:
            object.__setattr__(obj, k, p.factory())
        elif p.default is not PydanticUndefined:
            object.__setattr__(obj, k, _copy.deepcopy(p.default))


def _is_root(cls):
    return cls.__dict__.get("__pydantic_root__") or any(
        b.__dict__.get("__pydantic_root__") for b in cls.__mro__)


def _vmodel(cls, val, loc, c):
    if isinstance(val, cls):
        return val
    _ensure(cls)
    decls = cls.__pydantic_decorators__["model"]
    info = ValidationInfo(c)
    v = val
    for d in decls:
        if d.mode == "before":
            v = _guard(c, loc, val, lambda d=d, v=v: _invoke(d.func, (cls, v), info))
            if v is _BAD:
                return _BAD
            if isinstance(v, cls):
                return v
    root = _is_root(cls)
    if root:
        data = {"root": v}
    elif isinstance(v, dict):
        data = v
    elif (c.from_attributes or cls.model_config.get("from_attributes")) and v is not None \
            and not isinstance(v, (str, int, float, bool, list, tuple, set)):
        data = {}
        for name, fi in cls.model_fields.items():
            if hasattr(v, name):
                data[name] = getattr(v, name)
    else:
        return _E(c, loc, "model_type", v, {"class_name": cls.__name__},
                  "Input should be an object" if c.json else None)
    r = _fill(cls, data, loc, c)
    if r is None:
        return _BAD
    obj = object.__new__(cls)
    _populate(obj, cls, r[0], r[1], r[2])
    for d in decls:
        if d.mode == "after":
            res = _guard(c, loc, v, lambda d=d: _invoke(d.func, (obj,), info))
            if res is _BAD:
                return _BAD
            obj = res
    obj.model_post_init(c.context)
    return obj


def _finish(c, title, r):
    if c.errs:
        raise ValidationError(title, c.errs)
    return r


class BaseModel(metaclass=ModelMetaclass):
    """Base class of validated models."""

    model_config = {}

    def __init__(self, **data):
        cls = type(self)
        _ensure(cls)
        c = _Ctx()
        c.cls = cls
        info = ValidationInfo(c)
        decls = cls.__pydantic_decorators__["model"]
        v = data
        for d in decls:
            if d.mode == "before":
                v = _guard(c, (), data, lambda d=d, v=v: _invoke(d.func, (cls, v), info))
                if v is _BAD:
                    raise ValidationError(cls.__name__, c.errs)
        r = _fill(cls, v, (), c)
        if r is None:
            raise ValidationError(cls.__name__, c.errs)
        _populate(self, cls, r[0], r[1], r[2])
        for d in decls:
            if d.mode == "after":
                res = _guard(c, (), v, lambda d=d: _invoke(d.func, (self,), info))
                if res is _BAD:
                    raise ValidationError(cls.__name__, c.errs)
        self.model_post_init(None)

    def model_post_init(self, context):
        pass

    @classmethod
    def model_validate(cls, obj, *, strict=None, from_attributes=None, context=None):
        c = _Ctx(bool(strict), False, context, bool(from_attributes))
        c.cls = cls
        r = _vmodel(cls, obj, (), c)
        return _finish(c, cls.__name__, r)

    @classmethod
    def model_validate_json(cls, json_data, *, strict=None, context=None):
        c = _Ctx(bool(strict), True, context)
        c.cls = cls
        try:
            if isinstance(json_data, (bytes, bytearray)):
                json_data = bytes(json_data).decode("utf-8")
            data = _json.loads(json_data)
        except Exception as e:
            _E(c, (), "json_invalid", json_data, {"error": str(e)}, "Invalid JSON: " + str(e))
            raise ValidationError(cls.__name__, c.errs)
        r = _vmodel(cls, data, (), c)
        return _finish(c, cls.__name__, r)

    @classmethod
    def model_validate_strings(cls, obj, *, strict=None, context=None):
        return cls.model_validate(obj, strict=strict, context=context)

    @classmethod
    def model_construct(cls, _fields_set=None, **values):
        _ensure(cls)
        obj = object.__new__(cls)
        vals = {}
        for name, fi in cls.model_fields.items():
            if name in values:
                vals[name] = values[name]
            elif fi.default_factory is not None:
                vals[name] = fi.default_factory()
            elif fi.default is not PydanticUndefined:
                vals[name] = fi.default
        fset = set(_fields_set) if _fields_set is not None else set(values.keys())
        extras = {}
        if cls.model_config.get("extra") == "allow":
            for k in values:
                if k not in cls.model_fields:
                    extras[k] = values[k]
        _populate(obj, cls, vals, fset, extras)
        return obj

    @classmethod
    def model_rebuild(cls, *, force=False, raise_errors=True, **kw):
        if force:
            type.__setattr__(cls, "__pydantic_complete__", False)
        ok = _complete(cls)
        if not ok and raise_errors:
            raise PydanticUserError("`%s` is not fully defined" % cls.__name__)
        return True if ok else None

    @property
    def model_fields_set(self):
        return self.__pydantic_fields_set__

    @property
    def model_extra(self):
        return self.__pydantic_extra__

    def __getattr__(self, name):
        if name.startswith("__pydantic_"):
            raise AttributeError(name)
        ex = self.__dict__.get("__pydantic_extra__")
        if ex is not None and name in ex:
            return ex[name]
        raise AttributeError("'%s' object has no attribute '%s'" % (type(self).__name__, name))

    def __setattr__(self, name, value):
        cls = type(self)
        if name.startswith("_"):
            object.__setattr__(self, name, value)
            return
        fi = cls.model_fields.get(name)
        cfg = cls.model_config
        if cfg.get("frozen") or (fi is not None and fi.frozen):
            c = _Ctx()
            _E(c, (name,), "frozen_instance", value)
            raise ValidationError(cls.__name__, c.errs)
        if fi is None:
            if cfg.get("extra") == "allow":
                self.__pydantic_extra__[name] = value
                return
            if isinstance(getattr(cls, name, None), property):
                object.__setattr__(self, name, value)
                return
            raise ValueError('"%s" object has no field "%s"' % (cls.__name__, name))
        if cfg.get("validate_assignment"):
            c = _Ctx()
            c.cls = cls
            c.cfg = cfg
            v = _vfield(cls, fi, name, value, (name,), c, dict(self.__dict__))
            if c.errs:
                raise ValidationError(cls.__name__, c.errs)
            value = v
        object.__setattr__(self, name, value)
        self.__pydantic_fields_set__.add(name)

    def __delattr__(self, name):
        object.__delattr__(self, name)

    def _frozen_hash(self):
        return hash(tuple([_hashable(getattr(self, k)) for k in type(self).model_fields]))

    def __eq__(self, other):
        if not isinstance(other, BaseModel):
            return NotImplemented
        if type(self) is not type(other):
            return False
        for k in type(self).model_fields:
            if getattr(self, k) != getattr(other, k):
                return False
        return self.__pydantic_extra__ == other.__pydantic_extra__

    def __ne__(self, other):
        r = self.__eq__(other)
        if r is NotImplemented:
            return r
        return not r

    __hash__ = None

    def __iter__(self):
        for k in type(self).model_fields:
            yield k, getattr(self, k)
        if self.__pydantic_extra__:
            for k, v in self.__pydantic_extra__.items():
                yield k, v

    def __repr_args__(self):
        out = []
        for k, fi in type(self).model_fields.items():
            if fi.repr:
                out.append((k, getattr(self, k)))
        for k, (alias, rep) in type(self).__pydantic_computed__.items():
            if rep:
                out.append((k, getattr(self, k)))
        if self.__pydantic_extra__:
            for k, v in self.__pydantic_extra__.items():
                out.append((k, v))
        return out

    def __repr__(self):
        return type(self).__name__ + "(" + ", ".join([k + "=" + repr(v) for k, v in self.__repr_args__()]) + ")"

    def __str__(self):
        return " ".join([k + "=" + repr(v) for k, v in self.__repr_args__()])

    def __copy__(self):
        return self.model_copy()

    def __deepcopy__(self, memo=None):
        return self.model_copy(deep=True)

    def model_copy(self, *, update=None, deep=False):
        cls = type(self)
        obj = object.__new__(cls)
        vals = {}
        for k in cls.model_fields:
            v = getattr(self, k)
            vals[k] = _copy.deepcopy(v) if deep else v
        fset = set(self.__pydantic_fields_set__)
        ex = self.__pydantic_extra__
        if ex is not None:
            ex = _copy.deepcopy(ex) if deep else dict(ex)
        if update:
            for k, v in update.items():
                if k in cls.model_fields:
                    vals[k] = v
                    fset.add(k)
                elif ex is not None:
                    ex[k] = v
        _populate(obj, cls, vals, fset, ex or {})
        object.__setattr__(obj, "__pydantic_extra__", ex)
        for k in cls.__private_attributes__:
            if k in self.__dict__:
                v = self.__dict__[k]
                object.__setattr__(obj, k, _copy.deepcopy(v) if deep else v)
        return obj

    def model_dump(self, *, mode="python", include=None, exclude=None, by_alias=False,
                   exclude_unset=False, exclude_defaults=False, exclude_none=False, round_trip=False,
                   warnings=True, **kw):
        o = _Opts(mode, by_alias, exclude_unset, exclude_defaults, exclude_none)
        return _dump_model(self, o, include, exclude)

    def model_dump_json(self, *, indent=None, include=None, exclude=None, by_alias=False,
                        exclude_unset=False, exclude_defaults=False, exclude_none=False, round_trip=False,
                        warnings=True, **kw):
        o = _Opts("json", by_alias, exclude_unset, exclude_defaults, exclude_none)
        return _jsonify(_dump_model(self, o, include, exclude), indent, 0)

    @classmethod
    def model_json_schema(cls, by_alias=True, ref_template="#/$defs/{model}", mode="validation", **kw):
        defs = {}
        _model_schema(cls, defs)
        s = dict(defs.pop(cls.__name__))
        if defs:
            s["$defs"] = defs
        return _sort_schema(s, None)

    @classmethod
    def model_parametrized_name(cls, params):
        return cls.__name__ + "[" + ",".join([_tname(p) for p in params]) + "]"


def _hashable(v):
    if isinstance(v, list):
        return tuple([_hashable(x) for x in v])
    if isinstance(v, dict):
        return tuple([(k, _hashable(x)) for k, x in v.items()])
    if isinstance(v, set):
        return frozenset(v)
    return v


class RootModel(BaseModel):
    """A model whose whole value is the single field `root`."""

    __pydantic_root__ = True

    def __init__(self, root=PydanticUndefined, **data):
        cls = type(self)
        _ensure(cls)
        if root is PydanticUndefined:
            if data:
                root = data
            else:
                root = PydanticUndefined
        c = _Ctx()
        c.cls = cls
        if root is PydanticUndefined:
            r = _fill(cls, {}, (), c)
        else:
            r = _fill(cls, {"root": root}, (), c)
        if r is None:
            raise ValidationError(cls.__name__, c.errs)
        _populate(self, cls, r[0], r[1], r[2])

    @classmethod
    def __class_getitem__(cls, tp):
        name = cls.__name__ + "[" + _tname(tp) + "]"
        return ModelMetaclass(name, (cls,), {"__annotations__": {"root": tp}, "__module__": cls.__module__})

    @classmethod
    def model_validate_json(cls, json_data, *, strict=None, context=None):
        return BaseModel.model_validate_json.__func__(cls, json_data, strict=strict, context=context)


# --------------------------------------------------------------------------
# serialization
# --------------------------------------------------------------------------

def _pick(inc, exc, key):
    sub_inc = None
    sub_exc = None
    if exc is not None:
        if isinstance(exc, dict):
            if key in exc:
                e = exc[key]
                if e is True or e is Ellipsis:
                    return False, None, None
                sub_exc = e
        elif key in exc:
            return False, None, None
    if inc is not None:
        if isinstance(inc, dict):
            if key not in inc:
                return False, None, None
            i = inc[key]
            sub_inc = None if (i is True or i is Ellipsis) else i
        elif key not in inc:
            return False, None, None
    return True, sub_inc, sub_exc


def _dump_model(m, o, include, exclude):
    cls = type(m)
    for d in cls.__pydantic_decorators__["mser"]:
        return _dump(d.func(m), o, include, exclude)
    if _is_root(cls):
        return _dump(m.root, o, include, exclude)
    out = {}
    fsers = cls.__pydantic_decorators__["fser"]
    for name, fi in cls.model_fields.items():
        if fi.exclude:
            continue
        keep, si, se = _pick(include, exclude, name)
        if not keep:
            continue
        v = getattr(m, name)
        if o.exclude_unset and name not in m.__pydantic_fields_set__:
            continue
        if o.exclude_defaults and not fi.is_required():
            dv = fi.default_factory() if fi.default_factory is not None else fi.default
            if v == dv:
                continue
        if o.exclude_none and v is None:
            continue
        ser = None
        for d in fsers:
            if name in d.fields or "*" in d.fields:
                if d.extra == "json" and o.mode != "json":
                    continue
                if d.extra == "json-unless-none" and (o.mode != "json" or v is None):
                    continue
                ser = d
        if ser is not None:
            r = _invoke(ser.func, (m, v), ValidationInfo(_Ctx()))
            r = _dump(r, o, si, se)
        else:
            r = _dump(v, o, si, se)
        key = name
        if o.by_alias:
            a = fi.serialization_alias if fi.serialization_alias is not None else fi.alias
            if a is not None:
                key = a
        out[key] = r
    for k, (alias, rep) in cls.__pydantic_computed__.items():
        keep, si, se = _pick(include, exclude, k)
        if not keep:
            continue
        v = _dump(getattr(m, k), o, si, se)
        out[alias if (o.by_alias and alias is not None) else k] = v
    ex = m.__pydantic_extra__
    if ex:
        for k, v in ex.items():
            keep, si, se = _pick(include, exclude, k)
            if keep:
                out[k] = _dump(v, o, si, se)
    return out


def _dump(v, o, include=None, exclude=None):
    if isinstance(v, BaseModel):
        return _dump_model(v, o, include, exclude)
    js = o.mode == "json"
    if isinstance(v, dict):
        out = {}
        for k, x in v.items():
            keep, si, se = _pick(include, exclude, k)
            if not keep:
                continue
            if o.exclude_none and x is None:
                continue
            kk = k
            if js:
                kk = _dump(k, o)
                if not isinstance(kk, str):
                    kk = _json_key(kk)
            out[kk] = _dump(x, o, si, se)
        return out
    if isinstance(v, (list, tuple, set, frozenset)):
        items = []
        i = 0
        for x in (sorted(v, key=repr) if False else v):
            keep, si, se = _pick(include, exclude, i)
            i += 1
            if not keep:
                continue
            items.append(_dump(x, o, si, se))
        if js or isinstance(v, list):
            return items
        if isinstance(v, tuple):
            return tuple(items)
        return type(v)(items) if not isinstance(v, (set, frozenset)) else _safe_set(v, items)
    if isinstance(v, _enum.Enum):
        return _dump(v.value, o) if js else v
    if js:
        return _to_jsonable(v)
    return v


def _safe_set(orig, items):
    try:
        return type(orig)(items)
    except TypeError:
        return items


def _json_key(k):
    if k is True:
        return "true"
    if k is False:
        return "false"
    if k is None:
        return "null"
    return str(k)


def _to_jsonable(v):
    if v is None or isinstance(v, (bool, int, float, str)):
        return v
    if isinstance(v, _dt.datetime):
        s = v.isoformat()
        if s.endswith("+00:00"):
            s = s[:-6] + "Z"
        return s
    if isinstance(v, (_dt.date, _dt.time)):
        return v.isoformat()
    if isinstance(v, _dt.timedelta):
        return _duration_text(v)
    if isinstance(v, (_decimal.Decimal, _uuid.UUID)):
        return str(v)
    if isinstance(v, (bytes, bytearray)):
        return bytes(v).decode("utf-8")
    if isinstance(v, (SecretStr, SecretBytes)):
        return "**********"
    if isinstance(v, _enum.Enum):
        return _to_jsonable(v.value)
    if isinstance(v, dict):
        return dict((_json_key(k) if not isinstance(k, str) else k, _to_jsonable(x)) for k, x in v.items())
    if isinstance(v, (list, tuple, set, frozenset)):
        return [_to_jsonable(x) for x in v]
    if isinstance(v, BaseModel):
        return _dump_model(v, _Opts("json"), None, None)
    return str(v)


def _duration_text(td):
    total = td.days * 86400 + td.seconds
    sign = "-" if total < 0 else ""
    total = abs(total)
    days = total // 86400
    rem = total % 86400
    h = rem // 3600
    m = (rem % 3600) // 60
    s = rem % 60
    out = sign + "P"
    if days:
        out += "%dD" % days
    secs = str(s)
    if td.microseconds:
        secs += (".%06d" % td.microseconds).rstrip("0")
    if h or m or s or td.microseconds or not days:
        out += "T"
        if h:
            out += "%dH" % h
        if m:
            out += "%dM" % m
        if s or td.microseconds or not (h or m or days):
            out += secs + "S"
    return out


def _jsonify(v, indent, level):
    if v is None:
        return "null"
    if v is True:
        return "true"
    if v is False:
        return "false"
    if isinstance(v, str):
        return _json.dumps(v, ensure_ascii=False)
    if isinstance(v, float):
        if v != v or v in (float("inf"), float("-inf")):
            return "null"
        return repr(v)
    if isinstance(v, int):
        return str(v)
    if isinstance(v, dict):
        if not v:
            return "{}"
        if indent is None:
            return "{" + ",".join([_json.dumps(str(k), ensure_ascii=False) + ":" + _jsonify(x, None, 0)
                                   for k, x in v.items()]) + "}"
        pad = " " * (indent * (level + 1))
        end = " " * (indent * level)
        return "{\n" + ",\n".join([pad + _json.dumps(str(k), ensure_ascii=False) + ": " + _jsonify(x, indent, level + 1)
                                   for k, x in v.items()]) + "\n" + end + "}"
    if isinstance(v, (list, tuple)):
        if not v:
            return "[]"
        if indent is None:
            return "[" + ",".join([_jsonify(x, None, 0) for x in v]) + "]"
        pad = " " * (indent * (level + 1))
        end = " " * (indent * level)
        return "[\n" + ",\n".join([pad + _jsonify(x, indent, level + 1) for x in v]) + "\n" + end + "]"
    return _jsonify(_to_jsonable(v), indent, level)


# --------------------------------------------------------------------------
# JSON schema
# --------------------------------------------------------------------------

def _title_of(name):
    return " ".join([w[:1].upper() + w[1:] for w in name.replace("_", " ").split(" ")])


def _model_schema(cls, defs):
    _ensure(cls)
    name = cls.__name__
    if name in defs:
        return {"$ref": "#/$defs/" + name}
    defs[name] = {}
    if _is_root(cls):
        s = _schema(cls.model_fields["root"]._full, defs)
        s = dict(s)
        s["title"] = name
        defs[name] = s
        return {"$ref": "#/$defs/" + name}
    props = {}
    required = []
    for fname, fi in cls.model_fields.items():
        key = fi.validation_alias if fi.validation_alias is not None else (fi.alias if fi.alias is not None else fname)
        s = dict(_schema(fi._full if fi._full is not None else Any, defs))
        if "$ref" not in s:
            s["title"] = fi.title if fi.title is not None else _title_of(fname)
        elif fi.title is not None:
            s["title"] = fi.title
        for k, v in fi.constraints.items():
            _apply_constraint(s, k, v)
        if fi.description is not None:
            s["description"] = fi.description
        if fi.examples is not None:
            s["examples"] = fi.examples
        if fi.default_factory is None and fi.default is not PydanticUndefined:
            s["default"] = _to_jsonable(_dump(fi.default, _Opts("json")))
        elif fi.default_factory is not None:
            pass
        else:
            required.append(key)
        if isinstance(fi.json_schema_extra, dict):
            s.update(fi.json_schema_extra)
        props[key] = s
    out = {"properties": props, "title": name, "type": "object"}
    if required:
        out["required"] = required
    if cls.model_config.get("extra") == "forbid":
        out["additionalProperties"] = False
    defs[name] = out
    return {"$ref": "#/$defs/" + name}


def _apply_constraint(s, k, v):
    t = s.get("type")
    if k == "gt":
        s["exclusiveMinimum"] = v
    elif k == "ge":
        s["minimum"] = v
    elif k == "lt":
        s["exclusiveMaximum"] = v
    elif k == "le":
        s["maximum"] = v
    elif k == "multiple_of":
        s["multipleOf"] = v
    elif k == "min_length":
        s["minItems" if t == "array" else ("minProperties" if t == "object" else "minLength")] = v
    elif k == "max_length":
        s["maxItems" if t == "array" else ("maxProperties" if t == "object" else "maxLength")] = v
    elif k == "pattern":
        s["pattern"] = v


def _schema(tp, defs):
    if tp is None or tp is _NoneType:
        return {"type": "null"}
    if tp is Any or tp is object:
        return {}
    if isinstance(tp, str) or hasattr(tp, "__forward_arg__"):
        return {}
    if hasattr(tp, "__supertype__"):
        return _schema(tp.__supertype__, defs)
    org = _typing.get_origin(tp)
    args = _typing.get_args(tp)
    if org is Annotated:
        base, metas = _unwrap(tp)
        s = dict(_schema(base, defs))
        for m in metas:
            if isinstance(m, FieldInfo):
                for k, v in m.constraints.items():
                    _apply_constraint(s, k, v)
                if m.description is not None:
                    s["description"] = m.description
            elif isinstance(m, _StringConstraints):
                for k, v in m.cons.items():
                    _apply_constraint(s, k, v)
        return s
    if org is Union:
        members = [a for a in args if a is not _NoneType]
        subs = [_schema(a, defs) for a in members]
        if len(members) != len(args):
            subs.append({"type": "null"})
        if len(subs) == 1:
            return subs[0]
        return {"anyOf": subs}
    if org is Literal:
        vals = [_to_jsonable(a) for a in args]
        types = set()
        for a in args:
            types.add("string" if isinstance(a, str) else "boolean" if isinstance(a, bool) else
                      "integer" if isinstance(a, int) else "number" if isinstance(a, float) else "null")
        s = {}
        if len(vals) == 1:
            s["const"] = vals[0]
        else:
            s["enum"] = vals
        if len(types) == 1:
            s["type"] = list(types)[0]
        return s
    if org is not None:
        nm = getattr(org, "__name__", "")
        if org is list or nm in ("Sequence", "MutableSequence", "Iterable", "Collection"):
            return {"items": _schema(args[0], defs) if args else {}, "type": "array"}
        if org is set or org is frozenset or nm in ("Set", "MutableSet"):
            return {"items": _schema(args[0], defs) if args else {}, "type": "array", "uniqueItems": True}
        if org is tuple:
            if not args or (len(args) == 2 and args[1] is Ellipsis):
                return {"items": _schema(args[0], defs) if args else {}, "type": "array"}
            return {"maxItems": len(args), "minItems": len(args),
                    "prefixItems": [_schema(a, defs) for a in args], "type": "array"}
        if org is dict or nm in ("Mapping", "MutableMapping"):
            s = {"type": "object"}
            if len(args) > 1:
                s["additionalProperties"] = _schema(args[1], defs)
            return s
        return {}
    if isinstance(tp, ModelMetaclass):
        return _model_schema(tp, defs)
    if isinstance(tp, type):
        if tp is bool:
            return {"type": "boolean"}
        if tp is int:
            return {"type": "integer"}
        if tp is float:
            return {"type": "number"}
        if tp is str:
            return {"type": "string"}
        if tp is bytes:
            return {"format": "binary", "type": "string"}
        if tp is list or tp is tuple:
            return {"items": {}, "type": "array"}
        if tp is set or tp is frozenset:
            return {"items": {}, "type": "array", "uniqueItems": True}
        if tp is dict:
            return {"type": "object"}
        if tp is _dt.datetime:
            return {"format": "date-time", "type": "string"}
        if tp is _dt.date:
            return {"format": "date", "type": "string"}
        if tp is _dt.time:
            return {"format": "time", "type": "string"}
        if tp is _dt.timedelta:
            return {"format": "duration", "type": "string"}
        if tp is _uuid.UUID:
            return {"format": "uuid", "type": "string"}
        if tp is _decimal.Decimal:
            return {"anyOf": [{"type": "number"}, {"type": "string"}]}
        if tp is SecretStr:
            return {"format": "password", "type": "string", "writeOnly": True}
        if issubclass(tp, _enum.Enum):
            name = tp.__name__
            if name not in defs:
                vals = [m.value for m in tp]
                d = {"enum": vals, "title": name}
                if all(isinstance(x, str) for x in vals):
                    d["type"] = "string"
                elif all(isinstance(x, int) and not isinstance(x, bool) for x in vals):
                    d["type"] = "integer"
                defs[name] = d
            return {"$ref": "#/$defs/" + name}
    return {}


def _sort_schema(v, parent):
    if isinstance(v, dict):
        keys = list(v.keys())
        if parent not in ("properties", "default"):
            keys = sorted(keys)
        if parent == "default":
            return v
        return dict((k, _sort_schema(v[k], k)) for k in keys)
    if isinstance(v, list):
        return [_sort_schema(x, parent) for x in v]
    return v


# --------------------------------------------------------------------------
# TypeAdapter and constrained types
# --------------------------------------------------------------------------

class TypeAdapter:
    """Validation and serialization for any annotation."""

    def __init__(self, type, *, config=None):
        self._type = type
        self._config = config or {}

    def _title(self):
        return _tname(self._type)

    def validate_python(self, obj, *, strict=None, from_attributes=None, context=None):
        c = _Ctx(bool(strict), False, context, bool(from_attributes))
        c.cfg = self._config
        r = _v(self._type, obj, (), c)
        return _finish(c, self._title(), r)

    def validate_json(self, data, *, strict=None, context=None):
        c = _Ctx(bool(strict), True, context)
        c.cfg = self._config
        try:
            if isinstance(data, (bytes, bytearray)):
                data = bytes(data).decode("utf-8")
            obj = _json.loads(data)
        except Exception as e:
            _E(c, (), "json_invalid", data, {"error": str(e)}, "Invalid JSON: " + str(e))
            raise ValidationError(self._title(), c.errs)
        r = _v(self._type, obj, (), c)
        return _finish(c, self._title(), r)

    def validate_strings(self, obj, *, strict=None, context=None):
        return self.validate_python(obj, strict=strict, context=context)

    def dump_python(self, instance, *, mode="python", include=None, exclude=None, by_alias=False,
                    exclude_unset=False, exclude_defaults=False, exclude_none=False, **kw):
        return _dump(instance, _Opts(mode, by_alias, exclude_unset, exclude_defaults, exclude_none), include, exclude)

    def dump_json(self, instance, *, indent=None, include=None, exclude=None, by_alias=False,
                  exclude_none=False, **kw):
        o = _Opts("json", by_alias, False, False, exclude_none)
        return _jsonify(_dump(instance, o, include, exclude), indent, 0).encode("utf-8")

    def json_schema(self, **kw):
        defs = {}
        s = dict(_schema(self._type, defs))
        if defs:
            s["$defs"] = defs
        return _sort_schema(s, None)


class _StringConstraints:
    def __init__(self, strip_whitespace=None, to_upper=None, to_lower=None, strict=None,
                 min_length=None, max_length=None, pattern=None):
        self.strip_whitespace = strip_whitespace
        self.to_upper = to_upper
        self.to_lower = to_lower
        self.strict = strict
        self.cons = {}
        if min_length is not None:
            self.cons["min_length"] = min_length
        if max_length is not None:
            self.cons["max_length"] = max_length
        if pattern is not None:
            self.cons["pattern"] = pattern


StringConstraints = _StringConstraints

PositiveInt = Annotated[int, Field(gt=0)]
NegativeInt = Annotated[int, Field(lt=0)]
NonNegativeInt = Annotated[int, Field(ge=0)]
NonPositiveInt = Annotated[int, Field(le=0)]
PositiveFloat = Annotated[float, Field(gt=0)]
NegativeFloat = Annotated[float, Field(lt=0)]
NonNegativeFloat = Annotated[float, Field(ge=0)]
NonPositiveFloat = Annotated[float, Field(le=0)]
StrictInt = Annotated[int, Strict()]
StrictFloat = Annotated[float, Strict()]
StrictStr = Annotated[str, Strict()]
StrictBool = Annotated[bool, Strict()]
StrictBytes = Annotated[bytes, Strict()]


def conint(*, strict=None, gt=None, ge=None, lt=None, le=None, multiple_of=None):
    return Annotated[int, Field(strict=strict, gt=gt, ge=ge, lt=lt, le=le, multiple_of=multiple_of)]


def confloat(*, strict=None, gt=None, ge=None, lt=None, le=None, multiple_of=None):
    return Annotated[float, Field(strict=strict, gt=gt, ge=ge, lt=lt, le=le, multiple_of=multiple_of)]


def constr(*, strip_whitespace=None, to_upper=None, to_lower=None, strict=None,
           min_length=None, max_length=None, pattern=None):
    return Annotated[str, _StringConstraints(strip_whitespace, to_upper, to_lower, strict,
                                             min_length, max_length, pattern)]


def conlist(item_type, *, min_length=None, max_length=None):
    return Annotated[list[item_type], Field(min_length=min_length, max_length=max_length)]


class SecretStr:
    """A string that hides its value when printed."""

    def __init__(self, value):
        self._secret_value = value

    def get_secret_value(self):
        return self._secret_value

    def __len__(self):
        return len(self._secret_value)

    def __eq__(self, other):
        return isinstance(other, SecretStr) and self._secret_value == other._secret_value

    def __hash__(self):
        return hash(self._secret_value)

    def __repr__(self):
        return "SecretStr('" + ("**********" if self._secret_value else "") + "')"

    def __str__(self):
        return "**********" if self._secret_value else ""


class SecretBytes(SecretStr):
    def __repr__(self):
        return "SecretBytes(b'" + ("**********" if self._secret_value else "") + "')"
