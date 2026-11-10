# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Generated __init__, __repr__, __eq__ and friends for classes of fields.

CPython builds these methods by exec'ing source text; the methods here are
closures over the field list instead, since no source is compiled at run time.
"""

import copy as _copy

__all__ = ["dataclass", "field", "Field", "FrozenInstanceError", "InitVar", "KW_ONLY", "MISSING", "fields",
           "asdict", "astuple", "make_dataclass", "replace", "is_dataclass"]


class _MissingType:
    def __repr__(self):
        return "MISSING"


MISSING = _MissingType()


class _KwOnly:
    pass


KW_ONLY = _KwOnly


class FrozenInstanceError(AttributeError):
    pass


class InitVar:
    def __init__(self, type):
        self.type = type

    def __class_getitem__(cls, type):
        return InitVar(type)

    def __repr__(self):
        t = self.type
        return "dataclasses.InitVar[" + (t.__name__ if isinstance(t, type) else repr(t)) + "]"


_FIELD = "_FIELD"
_FIELD_CLASSVAR = "_FIELD_CLASSVAR"
_FIELD_INITVAR = "_FIELD_INITVAR"


class Field:
    __slots__ = ("name", "type", "default", "default_factory", "repr", "hash", "init", "compare", "metadata",
                 "kw_only", "_field_type")

    def __init__(self, default, default_factory, init, repr, hash, compare, metadata, kw_only):
        self.name = None
        self.type = None
        self.default = default
        self.default_factory = default_factory
        self.init = init
        self.repr = repr
        self.hash = hash
        self.compare = compare
        self.metadata = {} if metadata is None else metadata
        self.kw_only = kw_only
        self._field_type = None

    def __repr__(self):
        return ("Field(name=%r,type=%r,default=%r,default_factory=%r,init=%r,repr=%r,hash=%r,compare=%r,"
                "metadata=%r,kw_only=%r,_field_type=%s)" % (self.name, self.type, self.default,
                                                           self.default_factory, self.init, self.repr, self.hash,
                                                           self.compare, self.metadata, self.kw_only,
                                                           self._field_type))

    def __set_name__(self, owner, name):
        func = getattr(type(self.default), "__set_name__", None)
        if func:
            func(self.default, owner, name)


def field(*, default=MISSING, default_factory=MISSING, init=True, repr=True, hash=None, compare=True,
          metadata=None, kw_only=MISSING):
    if default is not MISSING and default_factory is not MISSING:
        raise ValueError("cannot specify both default and default_factory")
    return Field(default, default_factory, init, repr, hash, compare, metadata, kw_only)


_PARAMS = "__dataclass_params__"
_FIELDS = "__dataclass_fields__"


class _DataclassParams:
    def __init__(self, init, repr, eq, order, unsafe_hash, frozen, match_args, kw_only, slots, weakref_slot):
        self.init = init
        self.repr = repr
        self.eq = eq
        self.order = order
        self.unsafe_hash = unsafe_hash
        self.frozen = frozen
        self.match_args = match_args
        self.kw_only = kw_only
        self.slots = slots
        self.weakref_slot = weakref_slot


def _is_classvar(a_type):
    if a_type is _ClassVarMarker:
        return True
    name = getattr(a_type, "_name", None) or getattr(a_type, "__name__", None)
    if name == "ClassVar":
        return True
    if isinstance(a_type, str):
        return a_type.startswith("ClassVar") or a_type.startswith("typing.ClassVar")
    origin = getattr(a_type, "__origin__", None)
    return origin is not None and getattr(origin, "_name", None) == "ClassVar"


_ClassVarMarker = object()


def _is_initvar(a_type):
    return a_type is InitVar or isinstance(a_type, InitVar) or isinstance(a_type, str) and \
        a_type.startswith("InitVar")


def _is_kw_only(a_type):
    return a_type is KW_ONLY or isinstance(a_type, str) and a_type in ("KW_ONLY", "dataclasses.KW_ONLY")


def _get_field(cls, a_name, a_type, default_kw_only):
    default = getattr(cls, a_name, MISSING)
    if isinstance(default, Field):
        f = default
    else:
        f = field(default=default)
    f.name = a_name
    f.type = a_type
    f._field_type = _FIELD
    if _is_classvar(a_type):
        f._field_type = _FIELD_CLASSVAR
    elif _is_initvar(a_type):
        f._field_type = _FIELD_INITVAR
    if f._field_type in (_FIELD_CLASSVAR, _FIELD_INITVAR):
        if f.default_factory is not MISSING:
            raise TypeError("field " + f.name + " cannot have a default factory")
    if f._field_type in (_FIELD, _FIELD_INITVAR):
        if f.kw_only is MISSING:
            f.kw_only = default_kw_only
    else:
        f.kw_only = False
    if f._field_type is _FIELD and f.default_factory is MISSING and isinstance(f.default, (list, dict, set)):
        raise ValueError("mutable default " + str(type(f.default)) + " for field " + f.name +
                         " is not allowed: use default_factory")
    return f


def _set_new_attribute(cls, name, value):
    if name in cls.__dict__:
        return True
    setattr(cls, name, value)
    return False


def _fields_in_init_order(fields):
    std = [f for f in fields if f.init and not f.kw_only]
    kw = [f for f in fields if f.init and f.kw_only]
    return std, kw


def _make_init(cls, fields, std_fields, kw_only_fields, frozen, has_post_init):
    seen_default = None
    for f in std_fields:
        if f.default is not MISSING or f.default_factory is not MISSING:
            seen_default = f
        elif seen_default is not None:
            raise TypeError("non-default argument " + repr(f.name) + " follows default argument " +
                            repr(seen_default.name))
    qualname = cls.__qualname__ + ".__init__"
    nstd = len(std_fields)
    nreq = len([f for f in std_fields if f.default is MISSING and f.default_factory is MISSING])
    names = [f.name for f in std_fields + kw_only_fields]
    initvars = [f.name for f in fields if f._field_type is _FIELD_INITVAR]

    def __init__(self, *args, **kwargs):
        if len(args) > nstd:
            if nreq == nstd:
                takes = "%d positional argument%s" % (nstd + 1, "" if nstd == 0 else "s")
            else:
                takes = "from %d to %d positional arguments" % (nreq + 1, nstd + 1)
            raise TypeError("%s() takes %s but %d were given" % (qualname, takes, len(args) + 1))
        values = {}
        for i in range(len(args)):
            values[std_fields[i].name] = args[i]
        for k, v in kwargs.items():
            if k not in names:
                raise TypeError(qualname + "() got an unexpected keyword argument " + repr(k))
            if k in values:
                raise TypeError(qualname + "() got multiple values for argument " + repr(k))
            values[k] = v
        missing = [f.name for f in std_fields if f.name not in values and f.default is MISSING and
                   f.default_factory is MISSING]
        if missing:
            raise TypeError("%s() missing %d required positional argument%s: %s"
                            % (qualname, len(missing), "" if len(missing) == 1 else "s",
                               _and_join(["'" + m + "'" for m in missing])))
        missing = [f.name for f in kw_only_fields if f.name not in values and f.default is MISSING and
                   f.default_factory is MISSING]
        if missing:
            raise TypeError("%s() missing %d required keyword-only argument%s: %s"
                            % (qualname, len(missing), "" if len(missing) == 1 else "s",
                               _and_join(["'" + m + "'" for m in missing])))
        for f in fields:
            if f._field_type is not _FIELD:
                continue
            if f.init and f.name in values:
                v = values[f.name]
            elif f.default_factory is not MISSING:
                v = f.default_factory()
            elif f.default is not MISSING:
                if not f.init:
                    continue
                v = f.default
            else:
                continue
            if frozen:
                object.__setattr__(self, f.name, v)
            else:
                setattr(self, f.name, v)
        if has_post_init:
            extra = []
            for name in initvars:
                if name in values:
                    extra.append(values[name])
                else:
                    extra.append(next(f.default for f in fields if f.name == name))
            self.__post_init__(*extra)
    __init__.__qualname__ = qualname
    return __init__


def _and_join(items):
    if len(items) == 1:
        return items[0]
    if len(items) == 2:
        return items[0] + " and " + items[1]
    return ", ".join(items[:-1]) + ", and " + items[-1]


def _make_repr(fields):
    shown = [f for f in fields if f._field_type is _FIELD and f.repr]
    active = set()

    def __repr__(self):
        key = id(self)
        if key in active:
            return "..."
        active.add(key)
        try:
            return type(self).__qualname__ + "(" + ", ".join(f.name + "=" + repr(getattr(self, f.name))
                                                             for f in shown) + ")"
        finally:
            active.discard(key)
    # pprint lays out a dataclass field by field only when its repr is ours.
    __repr__._dataclass_repr = True
    return __repr__


def _tuple_of(fields):
    compared = [f.name for f in fields if f._field_type is _FIELD and f.compare]

    def as_tuple(self):
        return tuple(getattr(self, n) for n in compared)
    return as_tuple


def _make_eq(as_tuple):
    def __eq__(self, other):
        if self is other:
            return True
        if other.__class__ is self.__class__:
            return as_tuple(self) == as_tuple(other)
        return NotImplemented
    return __eq__


def _make_order(as_tuple, op):
    def compare(self, other):
        if other.__class__ is not self.__class__:
            return NotImplemented
        a = as_tuple(self)
        b = as_tuple(other)
        if op == "__lt__":
            return a < b
        if op == "__le__":
            return a <= b
        if op == "__gt__":
            return a > b
        return a >= b
    compare.__name__ = op
    return compare


def _make_hash(fields):
    hashed = [f.name for f in fields if f._field_type is _FIELD and (f.compare if f.hash is None else f.hash)]

    def __hash__(self):
        return hash(tuple(getattr(self, n) for n in hashed))
    return __hash__


def _make_frozen(cls, fields):
    names = {f.name for f in fields}

    def __setattr__(self, name, value):
        if type(self) is cls or name in names:
            raise FrozenInstanceError("cannot assign to field " + repr(name))
        object.__setattr__(self, name, value)

    def __delattr__(self, name):
        if type(self) is cls or name in names:
            raise FrozenInstanceError("cannot delete field " + repr(name))
        object.__delattr__(self, name)
    return __setattr__, __delattr__


def _process_class(cls, init, repr, eq, order, unsafe_hash, frozen, match_args, kw_only, slots, weakref_slot):
    fields = {}
    setattr(cls, _PARAMS, _DataclassParams(init, repr, eq, order, unsafe_hash, frozen, match_args, kw_only,
                                           slots, weakref_slot))
    any_frozen_base = False
    has_dataclass_bases = False
    for b in cls.__mro__[-1:0:-1]:
        base_fields = getattr(b, _FIELDS, None)
        if base_fields is not None:
            has_dataclass_bases = True
            for f in base_fields.values():
                fields[f.name] = f
            if getattr(b, _PARAMS).frozen:
                any_frozen_base = True
    cls_annotations = cls.__dict__.get("__annotations__", {})
    cls_fields = []
    kw_only_seen = False
    for name, type_ in cls_annotations.items():
        if _is_kw_only(type_):
            if kw_only_seen:
                raise TypeError(cls.__name__ + " is KW_ONLY, but KW_ONLY has already been specified")
            kw_only_seen = True
            kw_only = True
        else:
            cls_fields.append(_get_field(cls, name, type_, kw_only))
    for f in cls_fields:
        fields[f.name] = f
        if isinstance(getattr(cls, f.name, None), Field):
            if f.default is MISSING:
                delattr(cls, f.name)
            else:
                setattr(cls, f.name, f.default)
    for name, value in cls.__dict__.items():
        if isinstance(value, Field) and name not in cls_annotations:
            raise TypeError(repr(name) + " is a field but has no type annotation")
    if has_dataclass_bases:
        if any_frozen_base and not frozen:
            raise TypeError("cannot inherit non-frozen dataclass from a frozen one")
        if not any_frozen_base and frozen:
            raise TypeError("cannot inherit frozen dataclass from a non-frozen one")
    setattr(cls, _FIELDS, fields)
    class_hash = cls.__dict__.get("__hash__", MISSING)
    has_explicit_hash = not (class_hash is MISSING or (class_hash is None and "__eq__" in cls.__dict__))
    all_fields = list(fields.values())
    field_list = [f for f in all_fields if f._field_type is _FIELD]
    std, kw = _fields_in_init_order([f for f in all_fields if f._field_type in (_FIELD, _FIELD_INITVAR)])
    if init:
        has_post_init = hasattr(cls, "__post_init__")
        _set_new_attribute(cls, "__init__", _make_init(cls, all_fields, std, kw, frozen, has_post_init))
    if repr:
        _set_new_attribute(cls, "__repr__", _make_repr(field_list))
    as_tuple = _tuple_of(field_list)
    if eq:
        _set_new_attribute(cls, "__eq__", _make_eq(as_tuple))
    if order:
        for op in ("__lt__", "__le__", "__gt__", "__ge__"):
            if _set_new_attribute(cls, op, _make_order(as_tuple, op)):
                raise TypeError("Cannot overwrite attribute " + op + " in class " + cls.__name__ +
                                ". Consider using functools.total_ordering")
    if frozen:
        setter, deleter = _make_frozen(cls, field_list)
        for name, fn in (("__setattr__", setter), ("__delattr__", deleter)):
            if _set_new_attribute(cls, name, fn):
                raise TypeError("Cannot overwrite attribute " + name + " in class " + cls.__name__)
    if unsafe_hash:
        if has_explicit_hash:
            raise TypeError("Cannot overwrite attribute __hash__ in class " + cls.__name__)
        cls.__hash__ = _make_hash(field_list)
    elif not has_explicit_hash:
        if eq and frozen:
            cls.__hash__ = _make_hash(field_list)
        elif eq:
            cls.__hash__ = None
    if match_args:
        _set_new_attribute(cls, "__match_args__", tuple(f.name for f in std))
    if "__replace__" not in cls.__dict__:
        cls.__replace__ = _replace
    return cls


def dataclass(cls=None, /, *, init=True, repr=True, eq=True, order=False, unsafe_hash=False, frozen=False,
              match_args=True, kw_only=False, slots=False, weakref_slot=False):
    def wrap(cls):
        if order and not eq:
            raise ValueError("eq must be true if order is true")
        return _process_class(cls, init, repr, eq, order, unsafe_hash, frozen, match_args, kw_only, slots,
                              weakref_slot)
    if cls is None:
        return wrap
    return wrap(cls)


def fields(class_or_instance):
    try:
        fs = getattr(class_or_instance, _FIELDS)
    except AttributeError:
        raise TypeError("must be called with a dataclass type or instance") from None
    return tuple(f for f in fs.values() if f._field_type is _FIELD)


def is_dataclass(obj):
    cls = obj if isinstance(obj, type) else type(obj)
    return hasattr(cls, _FIELDS)


def _is_dataclass_instance(obj):
    return hasattr(type(obj), _FIELDS)


def asdict(obj, *, dict_factory=dict):
    if not _is_dataclass_instance(obj):
        raise TypeError("asdict() should be called on dataclass instances")
    return _asdict_inner(obj, dict_factory)


def _asdict_inner(obj, dict_factory):
    if _is_dataclass_instance(obj):
        return dict_factory([(f.name, _asdict_inner(getattr(obj, f.name), dict_factory)) for f in fields(obj)])
    if isinstance(obj, tuple) and hasattr(obj, "_fields"):
        return type(obj)(*[_asdict_inner(v, dict_factory) for v in obj])
    if isinstance(obj, (list, tuple)):
        return type(obj)(_asdict_inner(v, dict_factory) for v in obj)
    if isinstance(obj, dict):
        return type(obj)((_asdict_inner(k, dict_factory), _asdict_inner(v, dict_factory)) for k, v in obj.items())
    return _copy.deepcopy(obj)


def astuple(obj, *, tuple_factory=tuple):
    if not _is_dataclass_instance(obj):
        raise TypeError("astuple() should be called on dataclass instances")
    return _astuple_inner(obj, tuple_factory)


def _astuple_inner(obj, tuple_factory):
    if _is_dataclass_instance(obj):
        return tuple_factory([_astuple_inner(getattr(obj, f.name), tuple_factory) for f in fields(obj)])
    if isinstance(obj, tuple) and hasattr(obj, "_fields"):
        return type(obj)(*[_astuple_inner(v, tuple_factory) for v in obj])
    if isinstance(obj, (list, tuple)):
        return type(obj)(_astuple_inner(v, tuple_factory) for v in obj)
    if isinstance(obj, dict):
        return type(obj)((_astuple_inner(k, tuple_factory), _astuple_inner(v, tuple_factory))
                         for k, v in obj.items())
    return _copy.deepcopy(obj)


def make_dataclass(cls_name, fields, *, bases=(), namespace=None, init=True, repr=True, eq=True, order=False,
                   unsafe_hash=False, frozen=False, match_args=True, kw_only=False, slots=False,
                   weakref_slot=False, module=None):
    ns = {} if namespace is None else dict(namespace)
    annotations = {}
    for item in fields:
        if isinstance(item, str):
            annotations[item] = "typing.Any"
        elif len(item) == 2:
            annotations[item[0]] = item[1]
        else:
            annotations[item[0]] = item[1]
            ns[item[0]] = item[2]
    ns["__annotations__"] = annotations
    cls = type(cls_name, bases, ns)
    return dataclass(cls, init=init, repr=repr, eq=eq, order=order, unsafe_hash=unsafe_hash, frozen=frozen,
                     match_args=match_args, kw_only=kw_only, slots=slots, weakref_slot=weakref_slot)


def _replace(self, /, **changes):
    for f in getattr(self, _FIELDS).values():
        if f._field_type is _FIELD_CLASSVAR:
            continue
        if not f.init:
            if f.name in changes:
                raise TypeError("field " + f.name + " is declared with init=False, it cannot be specified with "
                                "replace()")
            continue
        if f.name not in changes:
            if f._field_type is _FIELD_INITVAR and f.default is MISSING:
                raise TypeError("InitVar " + repr(f.name) + " must be specified with replace()")
            changes[f.name] = getattr(self, f.name)
    return self.__class__(**changes)


def replace(obj, /, **changes):
    if not _is_dataclass_instance(obj):
        raise TypeError("replace() should be called on dataclass instances")
    return _replace(obj, **changes)
