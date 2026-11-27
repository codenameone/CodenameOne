# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Field records for classes the build gave __init__, __repr__, __eq__ and friends.

CPython builds those methods by exec'ing source text. Nothing is compiled at run
time here: the build writes them into every @dataclass class as plain methods
(DataclassInit), and the decorator only records the fields and checks that they
are the ones the build saw.
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
    # The compiler moved every field(...) of the class body into
    # __dataclass_field_specs__, leaving the class attribute the default alone.
    specs = cls.__dict__.get("__dataclass_field_specs__")
    f = specs.get(a_name) if specs is not None else None
    if f is None:
        default = cls.__dict__.get(a_name, MISSING)
        if isinstance(default, Field):
            raise TypeError("dataclass " + cls.__qualname__ + " was not lowered at build time")
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


_repr_running = set()


def _repr_enter(obj):
    # Called by every generated __repr__: true when obj is already being shown
    # further up the stack, which CPython's recursive_repr prints as "...".
    key = id(obj)
    if key in _repr_running:
        return True
    _repr_running.add(key)
    return False


def _repr_leave(obj):
    _repr_running.discard(id(obj))


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
    lowered = cls.__dict__.get("__cn1_dc_lowered__")
    if lowered is None:
        raise TypeError("dataclass " + cls.__qualname__ + " was not lowered at build time")
    fields = {}
    cls.__dataclass_params__ = (_DataclassParams(init, repr, eq, order, unsafe_hash, frozen, match_args, kw_only,
                                           slots, weakref_slot))
    any_frozen_base = False
    has_dataclass_bases = False
    for b in cls.__mro__[-1:0:-1]:
        base_fields = getattr(b, "__dataclass_fields__", None)
        if base_fields is not None:
            has_dataclass_bases = True
            for f in base_fields.values():
                fields[f.name] = f
            if getattr(b, "__dataclass_params__").frozen:
                any_frozen_base = True
    cls_annotations = cls.__dict__.get("__annotations__", {})
    kw_only_seen = False
    for name, type_ in cls_annotations.items():
        if _is_kw_only(type_):
            if kw_only_seen:
                raise TypeError(cls.__name__ + " is KW_ONLY, but KW_ONLY has already been specified")
            kw_only_seen = True
            kw_only = True
        else:
            f = _get_field(cls, name, type_, kw_only)
            fields[f.name] = f
            if isinstance(cls.__dict__.get(f.name), Field):
                raise TypeError("dataclass " + cls.__qualname__ + " was not lowered at build time")
    for name, value in cls.__dict__.items():
        if isinstance(value, Field) and name not in cls_annotations:
            raise TypeError(repr(name) + " is a field but has no type annotation")
    if has_dataclass_bases:
        if any_frozen_base and not frozen:
            raise TypeError("cannot inherit non-frozen dataclass from a frozen one")
        if not any_frozen_base and frozen:
            raise TypeError("cannot inherit frozen dataclass from a non-frozen one")
    cls.__dataclass_fields__ = fields
    # The build wrote every method from the fields it could see; a base it
    # could not (one from another module) must not have added any.
    names, post_init = lowered
    seen = tuple(f.name for f in fields.values() if f._field_type is not _FIELD_CLASSVAR)
    if seen != names:
        raise TypeError("dataclass " + cls.__qualname__ + " has fields " + repr(seen) + " but was built for " +
                        repr(names) + ": its dataclass bases must be in the same module")
    if post_init is not None and post_init != hasattr(cls, "__post_init__"):
        raise TypeError("dataclass " + cls.__qualname__ + " inherits a __post_init__ the build could not see")
    if frozen:
        setter, deleter = _make_frozen(cls, [f for f in fields.values() if f._field_type is _FIELD])
        for name in ("__setattr__", "__delattr__"):
            if name in cls.__dict__:
                raise TypeError("Cannot overwrite attribute " + name + " in class " + cls.__name__)
        cls.__setattr__ = setter
        cls.__delattr__ = deleter
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
        fs = getattr(class_or_instance, "__dataclass_fields__")
    except AttributeError:
        raise TypeError("must be called with a dataclass type or instance") from None
    return tuple(f for f in fs.values() if f._field_type is _FIELD)


def is_dataclass(obj):
    cls = obj if isinstance(obj, type) else type(obj)
    return hasattr(cls, "__cn1_dc_lowered__")


def _is_dataclass_instance(obj):
    return hasattr(type(obj), "__cn1_dc_lowered__")


def asdict(obj, *, dict_factory=dict):
    if not _is_dataclass_instance(obj):
        raise TypeError("asdict() should be called on dataclass instances")
    return obj.__cn1_asdict__(dict_factory)


def _asdict_inner(obj, dict_factory):
    if _is_dataclass_instance(obj):
        return obj.__cn1_asdict__(dict_factory)
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
    return obj.__cn1_astuple__(tuple_factory)


def _astuple_inner(obj, tuple_factory):
    if _is_dataclass_instance(obj):
        return obj.__cn1_astuple__(tuple_factory)
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
    # A call with a literal field list is turned into a class statement at
    # build time and never reaches this; a class is not assembled from names
    # at run time.
    raise TypeError("make_dataclass(" + repr(cls_name) + ") needs a literal field list to be lowered at build time")


def replace(obj, /, **changes):
    if not _is_dataclass_instance(obj):
        raise TypeError("replace() should be called on dataclass instances")
    return obj.__replace__(**changes)
