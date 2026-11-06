# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Classes without boilerplate: the attrs API.

Written from the documented behaviour of attrs. The generated methods are
closures over the attribute list, since no source text is compiled at run
time.
"""

from dataclasses import _is_classvar

from attr import exceptions
from attr.exceptions import (FrozenInstanceError, NotAnAttrsClassError, UnannotatedAttributeError)

__all__ = ["Attribute", "Factory", "NOTHING", "asdict", "assoc", "astuple", "attr", "attrib", "attributes", "attrs",
           "converters", "define", "evolve", "exceptions", "field", "fields", "fields_dict", "filters", "frozen",
           "has", "ib", "make_class", "mutable", "resolve_types", "s", "setters", "validate", "validators",
           "Converter", "cmp_using"]


class _Nothing:
    _singleton = None

    def __repr__(self):
        return "NOTHING"

    def __str__(self):
        return "_Nothing.NOTHING"

    def __bool__(self):
        return False


NOTHING = _Nothing()


class Factory:
    """A default computed by calling a function at instantiation."""

    def __init__(self, factory, takes_self=False):
        self.factory = factory
        self.takes_self = takes_self

    def __repr__(self):
        return "Factory(factory=%r, takes_self=%r)" % (self.factory, self.takes_self)

    def __eq__(self, other):
        if other.__class__ is not Factory:
            return NotImplemented
        return self.factory == other.factory and self.takes_self == other.takes_self

    def __hash__(self):
        return hash((self.factory, self.takes_self))


class Converter:
    """A converter that also receives the instance and/or the attribute."""

    def __init__(self, converter, *, takes_self=False, takes_field=False):
        self.converter = converter
        self.takes_self = takes_self
        self.takes_field = takes_field

    def __call__(self, value, instance=None, field=None):
        if self.takes_self and self.takes_field:
            return self.converter(value, instance, field)
        if self.takes_self:
            return self.converter(value, instance)
        if self.takes_field:
            return self.converter(value, field)
        return self.converter(value)


_counter = [0]


def _next_count():
    _counter[0] += 1
    return _counter[0]


class Attribute:
    """One attribute of an attrs class, as listed by fields()."""

    def __init__(self, name, default, validator, repr, cmp, hash, init, inherited, metadata=None, type=None,
                 converter=None, kw_only=False, eq=None, eq_key=None, order=None, order_key=None, on_setattr=None,
                 alias=None):
        if eq is None:
            eq = cmp if cmp is not None else True
        if order is None:
            order = eq
        self.name = name
        self.default = default
        self.validator = validator
        self.repr = repr
        self.eq = eq
        self.eq_key = eq_key
        self.order = order
        self.order_key = order_key
        self.hash = hash
        self.init = init
        self.metadata = {} if metadata is None else metadata
        self.type = type
        self.converter = converter
        self.kw_only = kw_only
        self.inherited = inherited
        self.on_setattr = on_setattr
        self.alias = alias

    @property
    def cmp(self):
        return self.eq and self.order

    def evolve(self, **changes):
        new = Attribute(self.name, self.default, self.validator, self.repr, None, self.hash, self.init,
                        self.inherited, self.metadata, self.type, self.converter, self.kw_only, self.eq,
                        self.eq_key, self.order, self.order_key, self.on_setattr, self.alias)
        for k, v in changes.items():
            setattr(new, k, v)
        return new

    def __repr__(self):
        return ("Attribute(name=%r, default=%r, validator=%r, repr=%r, eq=%r, eq_key=%r, order=%r, order_key=%r, "
                "hash=%r, init=%r, metadata=mappingproxy(%r), type=%r, converter=%r, kw_only=%r, inherited=%r, "
                "on_setattr=%r, alias=%r)" % (self.name, self.default, self.validator, self.repr, self.eq,
                                              self.eq_key, self.order, self.order_key, self.hash, self.init,
                                              self.metadata, self.type, self.converter, self.kw_only,
                                              self.inherited, self.on_setattr, self.alias))

    def __eq__(self, other):
        if not isinstance(other, Attribute):
            return NotImplemented
        return (self.name == other.name and self.default == other.default and self.validator == other.validator and
                self.repr == other.repr and self.eq == other.eq and self.order == other.order and
                self.hash == other.hash and self.init == other.init and self.type == other.type and
                self.converter == other.converter and self.kw_only == other.kw_only and
                self.inherited == other.inherited and self.alias == other.alias)

    def __hash__(self):
        return hash((self.name, self.inherited, self.alias))


class _CountingAttr:
    """What attr.ib()/field() return until the class decorator collects it."""

    def __init__(self, default, validator, repr, eq, eq_key, order, order_key, hash, init, metadata, type,
                 converter, kw_only, on_setattr, alias):
        self.counter = _next_count()
        self._default = default
        self._validator = validator
        self.repr = repr
        self.eq = eq
        self.eq_key = eq_key
        self.order = order
        self.order_key = order_key
        self.hash = hash
        self.init = init
        self.metadata = metadata
        self.type = type
        self.converter = converter
        self.kw_only = kw_only
        self.on_setattr = on_setattr
        self.alias = alias

    def validator(self, meth):
        if self._validator is None:
            self._validator = meth
        else:
            self._validator = validators.and_(self._validator, meth)
        return meth

    def default(self, meth):
        if self._default is not NOTHING:
            raise exceptions.DefaultAlreadySetError()
        self._default = Factory(meth, takes_self=True)
        return meth


def _eq_order(cmp, eq, order):
    if cmp is not None and (eq is not None or order is not None):
        raise ValueError("Don't mix `cmp` with `eq' and `order`.")
    if cmp is not None:
        return cmp, cmp
    if eq is None:
        eq = True
    if order is None:
        order = eq
    if eq is False and order is True:
        raise ValueError("`order` can only be True if `eq` is True too.")
    return eq, order


def _key_of(value):
    if callable(value):
        return True, value
    return value, None


def attrib(default=NOTHING, validator=None, repr=True, cmp=None, hash=None, init=True, metadata=None, type=None,
           converter=None, factory=None, kw_only=False, eq=None, order=None, on_setattr=None, alias=None):
    eq, order = _eq_order(cmp, eq, order)
    eq_key = None
    order_key = None
    if callable(eq):
        eq_key = eq
        eq = True
        if order is True or order is None:
            order_key = eq_key
    if callable(order):
        order_key = order
        order = True
    if hash is not None and hash is not True and hash is not False:
        raise TypeError("Invalid value for hash.  Must be True, False, or None.")
    if factory is not None:
        if default is not NOTHING:
            raise ValueError("The `default` and `factory` arguments are mutually exclusive.")
        if not callable(factory):
            raise ValueError("The `factory` argument must be a callable.")
        default = Factory(factory)
    if isinstance(validator, (list, tuple)):
        validator = validators.and_(*validator)
    if isinstance(converter, (list, tuple)):
        converter = converters.pipe(*converter)
    return _CountingAttr(default, validator, repr, eq, eq_key, order, order_key, hash, init, metadata, type,
                         converter, kw_only, on_setattr, alias)


ib = attrib
attr = attrib


def field(*, default=NOTHING, validator=None, repr=True, hash=None, init=True, metadata=None, type=None,
          converter=None, factory=None, kw_only=False, eq=None, order=None, on_setattr=None, alias=None):
    return attrib(default=default, validator=validator, repr=repr, hash=hash, init=init, metadata=metadata,
                  type=type, converter=converter, factory=factory, kw_only=kw_only, eq=eq, order=order,
                  on_setattr=on_setattr, alias=alias)


class _AttrsTuple(tuple):
    """The tuple fields() returns: also reachable by attribute name."""

    def __getattr__(self, name):
        for a in self:
            if a.name == name:
                return a
        raise AttributeError(name)


def fields(cls):
    if not isinstance(cls, type):
        cls = cls.__class__
    attrs = getattr(cls, "__attrs_attrs__", None)
    if attrs is None:
        raise NotAnAttrsClassError(repr(cls) + " is not an attrs-decorated class.")
    return attrs


def fields_dict(cls):
    if not isinstance(cls, type):
        raise TypeError("Passed object must be a class.")
    attrs = getattr(cls, "__attrs_attrs__", None)
    if attrs is None:
        raise NotAnAttrsClassError(repr(cls) + " is not an attrs-decorated class.")
    return dict((a.name, a) for a in attrs)


def has(cls):
    attrs = getattr(cls, "__attrs_attrs__", None)
    if attrs is not None:
        return True
    return False


def resolve_types(cls, globalns=None, localns=None, attribs=None, include_extras=True):
    return cls


def _convert_one(c, value, instance, attrib):
    if isinstance(c, Converter):
        return c(value, instance, attrib)
    return c(value)


def _convert_value(instance, attrib, value):
    if attrib.converter is None:
        return value
    return _convert_one(attrib.converter, value, instance, attrib)


def _run_validator(instance, attrib, value):
    if attrib.validator is not None and not validators.get_disabled():
        attrib.validator(instance, attrib, value)
    return value


def validate(inst):
    if validators.get_disabled():
        return
    for a in fields(inst.__class__):
        if a.validator is not None:
            a.validator(inst, a, getattr(inst, a.name))


def _is_attrs_instance(obj):
    return getattr(obj.__class__, "__attrs_attrs__", None) is not None


def asdict(inst, recurse=True, filter=None, dict_factory=dict, retain_collection_types=False,
           value_serializer=None):
    result = dict_factory()
    items = []
    for a in fields(inst.__class__):
        v = getattr(inst, a.name)
        if filter is not None and not filter(a, v):
            continue
        if value_serializer is not None:
            v = value_serializer(inst, a, v)
        if recurse is True:
            v = _asdict_value(v, filter, dict_factory, retain_collection_types, value_serializer)
        items.append((a.name, v))
    if dict_factory is dict:
        return dict(items)
    for k, v in items:
        result[k] = v
    return result


def _asdict_value(v, filter, dict_factory, retain, serializer):
    if _is_attrs_instance(v):
        return asdict(v, True, filter, dict_factory, retain, serializer)
    if isinstance(v, (tuple, list, set, frozenset)):
        cf = v.__class__ if retain is True else list
        items = [_asdict_value(i, filter, dict_factory, retain, serializer) for i in v]
        try:
            return cf(items)
        except TypeError:
            if not isinstance(v, tuple):
                raise
            return cf(*items)
    if isinstance(v, dict):
        df = dict_factory if dict_factory is not dict else v.__class__
        return df((_asdict_value(kk, filter, dict_factory, retain, serializer),
                   _asdict_value(vv, filter, dict_factory, retain, serializer)) for kk, vv in v.items())
    return v


def astuple(inst, recurse=True, filter=None, tuple_factory=tuple, retain_collection_types=False):
    rv = []
    for a in fields(inst.__class__):
        v = getattr(inst, a.name)
        if filter is not None and not filter(a, v):
            continue
        if recurse is True:
            v = _astuple_value(v, filter, tuple_factory, retain_collection_types)
        rv.append(v)
    return rv if tuple_factory is list else tuple_factory(rv)


def _astuple_value(v, filter, tuple_factory, retain):
    if _is_attrs_instance(v):
        return astuple(v, True, filter, tuple_factory, retain)
    if isinstance(v, (tuple, list, set, frozenset)):
        cf = v.__class__ if retain is True else list
        items = [_astuple_value(i, filter, tuple_factory, retain) for i in v]
        try:
            return cf(items)
        except TypeError:
            if not isinstance(v, tuple):
                raise
            return cf(*items)
    if isinstance(v, dict):
        df = v.__class__
        return df((_astuple_value(kk, filter, tuple_factory, retain), _astuple_value(vv, filter, tuple_factory,
                                                                                    retain)) for kk, vv in v.items())
    return v


def evolve(*args, **changes):
    if len(args) != 1:
        raise TypeError("evolve() takes 1 positional argument, but %d were given" % len(args))
    inst = args[0]
    cls = inst.__class__
    for a in fields(cls):
        if not a.init:
            continue
        name = a.alias
        if name not in changes:
            changes[name] = getattr(inst, a.name)
    return cls(**changes)


def assoc(inst, **changes):
    new = inst.__class__.__new__(inst.__class__)
    names = set(a.name for a in fields(inst.__class__))
    for k in changes:
        if k not in names:
            raise exceptions.AttrsAttributeNotFoundError(
                "{k} is not an attrs attribute on {cl}.".format(k=k, cl=new.__class__))
    for a in fields(inst.__class__):
        object.__setattr__(new, a.name, changes.get(a.name, getattr(inst, a.name)))
    return new


def cmp_using(eq=None, lt=None, le=None, gt=None, ge=None, require_same_type=True, class_name="Comparable"):
    body = {}
    if eq is not None:
        def __eq__(self, other):
            if require_same_type and other.__class__ is not self.__class__:
                return NotImplemented
            return eq(self.value, other.value)
        body["__eq__"] = __eq__
        body["__hash__"] = None
    for name, fn in (("__lt__", lt), ("__le__", le), ("__gt__", gt), ("__ge__", ge)):
        if fn is not None:
            body[name] = _cmp_method(fn, require_same_type)

    def __init__(self, value):
        self.value = value
    body["__init__"] = __init__
    return type(class_name, (object,), body)


def _cmp_method(fn, require_same_type):
    def method(self, other):
        if require_same_type and other.__class__ is not self.__class__:
            return NotImplemented
        return fn(self.value, other.value)
    return method


# --------------------------------------------------------------------------------------------------------------
# Class building


def _public_name(name):
    return name.lstrip("_")


def _attribute_from(name, ca, annotation, kw_only):
    default = ca._default
    metadata = ca.metadata
    a = Attribute(name, default, ca._validator, ca.repr, None, ca.hash, ca.init, False, metadata,
                  ca.type if ca.type is not None else annotation, ca.converter,
                  ca.kw_only or kw_only, ca.eq, ca.eq_key, ca.order, ca.order_key, ca.on_setattr,
                  ca.alias if ca.alias is not None else _public_name(name))
    return a


def _collect_own(cls, these, auto_attribs, kw_only):
    own = cls.__dict__
    annotations = own.get("__annotations__", {})
    if these is not None:
        pairs = [(k, v) for k, v in these.items()]
        if not isinstance(these, dict):
            pairs = sorted(pairs, key=lambda kv: kv[1].counter)
        return [_attribute_from(k, v, annotations.get(k), kw_only) for k, v in pairs], False
    if auto_attribs is not False:
        names = []
        stray = False
        for name, v in own.items():
            if isinstance(v, _CountingAttr) and name not in annotations:
                stray = True
        if stray and auto_attribs is None:
            auto_attribs = False
        elif stray:
            raise UnannotatedAttributeError(
                "The following `attr.ib`s lack a type annotation: " +
                ", ".join(k for k, v in own.items() if isinstance(v, _CountingAttr) and k not in annotations) +
                ".")
    if auto_attribs is None and len(annotations) == 0:
        auto_attribs = False
    if auto_attribs is not False:
        result = []
        for name, ann in annotations.items():
            if _is_classvar(ann) or (isinstance(ann, str) and "ClassVar" in ann):
                continue
            if name.startswith("__") and not name.endswith("__"):
                pass
            v = own.get(name, NOTHING)
            if isinstance(v, _CountingAttr):
                ca = v
            else:
                ca = attrib(default=v)
            result.append(_attribute_from(name, ca, ann, kw_only))
        return result, True
    cas = [(k, v) for k, v in own.items() if isinstance(v, _CountingAttr)]
    cas = sorted(cas, key=lambda kv: kv[1].counter)
    return [_attribute_from(k, v, annotations.get(k), kw_only) for k, v in cas], True


def _collect_base(cls, taken, kw_only):
    base_attrs = []
    for base in reversed(cls.__mro__[1:-1]):
        for a in base.__dict__.get("__attrs_attrs__", ()):
            if a.inherited or a.name in taken:
                continue
            base_attrs.append(a.evolve(inherited=True))
    seen = set()
    filtered = []
    for a in reversed(base_attrs):
        if a.name in seen:
            continue
        filtered.insert(0, a)
        seen.add(a.name)
    if kw_only:
        filtered = [a.evolve(kw_only=True) for a in filtered]
    return filtered


def _and_join(items):
    if len(items) == 1:
        return items[0]
    if len(items) == 2:
        return items[0] + " and " + items[1]
    return ", ".join(items[:-1]) + ", and " + items[-1]


def _initial(attrib, inst):
    d = attrib.default
    if isinstance(d, Factory):
        if d.takes_self:
            return d.factory(inst)
        return d.factory()
    return d


def _make_init(cls, attrs, frozen_class, pre_init, post_init):
    init_attrs = [a for a in attrs if a.init]
    std = [a for a in init_attrs if not a.kw_only]
    kwo = [a for a in init_attrs if a.kw_only]
    qualname = cls.__qualname__ + ".__init__"
    nstd = len(std)
    nreq = len([a for a in std if a.default is NOTHING])
    names = [a.alias for a in std + kwo]

    def __init__(self, *args, **kwargs):
        if len(args) > nstd:
            if nreq == nstd:
                takes = "%d positional argument%s" % (nstd + 1, "" if nstd == 0 else "s")
            else:
                takes = "from %d to %d positional arguments" % (nreq + 1, nstd + 1)
            raise TypeError("%s() takes %s but %d were given" % (qualname, takes, len(args) + 1))
        values = {}
        for i in range(len(args)):
            values[std[i].alias] = args[i]
        for k, v in kwargs.items():
            if k not in names:
                raise TypeError(qualname + "() got an unexpected keyword argument " + repr(k))
            if k in values:
                raise TypeError(qualname + "() got multiple values for argument " + repr(k))
            values[k] = v
        missing = [a.alias for a in std if a.alias not in values and a.default is NOTHING]
        if missing:
            raise TypeError("%s() missing %d required positional argument%s: %s"
                            % (qualname, len(missing), "" if len(missing) == 1 else "s",
                               _and_join(["'" + m + "'" for m in missing])))
        missing = [a.alias for a in kwo if a.alias not in values and a.default is NOTHING]
        if missing:
            raise TypeError("%s() missing %d required keyword-only argument%s: %s"
                            % (qualname, len(missing), "" if len(missing) == 1 else "s",
                               _and_join(["'" + m + "'" for m in missing])))
        if pre_init:
            self.__attrs_pre_init__()
        assigned = []
        for a in attrs:
            if a.init and a.alias in values:
                v = values[a.alias]
            elif a.default is not NOTHING:
                v = _initial(a, self)
            else:
                continue
            if a.converter is not None:
                v = _convert_one(a.converter, v, self, a)
            object.__setattr__(self, a.name, v)
            assigned.append(a)
        if not validators.get_disabled():
            for a in assigned:
                if a.validator is not None:
                    a.validator(self, a, getattr(self, a.name))
        if post_init:
            self.__attrs_post_init__()
    __init__.__qualname__ = qualname
    return __init__


_active = set()


def _make_repr(attrs):
    shown = [a for a in attrs if a.repr is not False]

    def __repr__(self):
        key = id(self)
        if key in _active:
            return "..."
        _active.add(key)
        try:
            qn = self.__class__.__qualname__
            qn = qn.rsplit(">.", 1)[-1]
            parts = []
            for a in shown:
                v = getattr(self, a.name)
                parts.append(a.name + "=" + (repr(v) if a.repr is True else a.repr(v)))
            return qn + "(" + ", ".join(parts) + ")"
        finally:
            _active.discard(key)
    return __repr__


def _tuple_getter(attrs, flag):
    chosen = []
    for a in attrs:
        if flag == "eq":
            if a.eq:
                chosen.append((a.name, a.eq_key))
        elif a.order:
            chosen.append((a.name, a.order_key))

    def as_tuple(self):
        out = []
        for name, key in chosen:
            v = getattr(self, name)
            out.append(v if key is None else key(v))
        return tuple(out)
    return as_tuple


def _make_eq(as_tuple):
    def __eq__(self, other):
        if other.__class__ is not self.__class__:
            return NotImplemented
        return as_tuple(self) == as_tuple(other)

    def __ne__(self, other):
        result = __eq__(self, other)
        if result is NotImplemented:
            return NotImplemented
        return not result
    return __eq__, __ne__


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


def _make_hash(attrs, cls):
    hashed = []
    for a in attrs:
        if (a.eq if a.hash is None else a.hash):
            hashed.append(a.name)
    seed = cls.__qualname__

    def __hash__(self):
        return hash((seed,) + tuple(getattr(self, n) for n in hashed))
    return __hash__


def _hook_for(a, class_hook):
    hook = a.on_setattr if a.on_setattr is not None else class_hook
    if hook is None or hook is setters.NO_OP:
        return None
    return hook


def _make_setattr(attrs, frozen_class, class_hook, parent_setattr):
    by_name = dict((a.name, a) for a in attrs)
    hooks = {}
    if not frozen_class:
        for a in attrs:
            h = _hook_for(a, class_hook)
            if h is not None:
                hooks[a.name] = h

    def __setattr__(self, name, value):
        if frozen_class:
            raise FrozenInstanceError()
        h = hooks.get(name)
        if h is not None:
            value = h(self, by_name[name], value)
        object.__setattr__(self, name, value)

    def __delattr__(self, name):
        if frozen_class:
            raise FrozenInstanceError()
        object.__delattr__(self, name)
    return __setattr__, __delattr__, len(hooks) > 0


def _has_own(cls, name):
    return name in cls.__dict__


def _wrap(cls, these, repr_, eq, order, hash_, init, frozen_, kw_only, auto_attribs, auto_detect, on_setattr,
          define_mode, match_args, field_transformer, unsafe_hash):
    if unsafe_hash is not None:
        hash_ = unsafe_hash
    if auto_detect:
        if _has_own(cls, "__repr__"):
            repr_ = False
        if _has_own(cls, "__init__"):
            init = False
        has_eq = _has_own(cls, "__eq__") or _has_own(cls, "__ne__")
        if has_eq and eq is None:
            eq = False
        has_order = (_has_own(cls, "__lt__") or _has_own(cls, "__le__") or _has_own(cls, "__gt__") or
                     _has_own(cls, "__ge__"))
        if has_order and order is None:
            order = False
        if hash_ is None and _has_own(cls, "__hash__"):
            hash_ = False
    if eq is None:
        eq = True
    if order is None:
        order = eq if not define_mode else False
    if order and not eq:
        raise ValueError("`order` can only be True if `eq` is True too.")
    if isinstance(on_setattr, (list, tuple)):
        on_setattr = setters.pipe(*on_setattr)
    if frozen_ and on_setattr is not None:
        raise ValueError("Frozen classes can't use on_setattr.")
    own, _ = _collect_own(cls, these, auto_attribs, kw_only)
    taken = set(a.name for a in own)
    base = _collect_base(cls, taken, kw_only)
    attrs = base + own
    if field_transformer is not None:
        attrs = list(field_transformer(cls, attrs))
    had_default = False
    for a in attrs:
        if a.init is not False and a.kw_only is False:
            if had_default and a.default is NOTHING:
                raise ValueError("No mandatory attributes allowed after an attribute with a default value or "
                                 "factory.  Attribute in question: " + repr(a))
            if a.default is not NOTHING:
                had_default = True
    # Class-level cleanup: the placeholders must not remain as class attributes.
    base_names = set(a.name for a in base)
    for a in own:
        if a.name not in base_names or True:
            v = cls.__dict__.get(a.name, NOTHING)
            if v is not NOTHING and (isinstance(v, _CountingAttr) or these is None):
                delattr(cls, a.name)
    cls.__attrs_attrs__ = _AttrsTuple(attrs)
    if define_mode and on_setattr is None and not frozen_:
        on_setattr = setters.pipe(setters.convert, setters.validate)
    if init:
        cls.__init__ = _make_init(cls, attrs, frozen_, hasattr(cls, "__attrs_pre_init__"),
                                  hasattr(cls, "__attrs_post_init__"))
    if repr_:
        cls.__repr__ = _make_repr(attrs)
    if eq:
        as_tuple = _tuple_getter(attrs, "eq")
        cls.__eq__, cls.__ne__ = _make_eq(as_tuple)
    if order:
        ot = _tuple_getter(attrs, "order")
        for op in ("__lt__", "__le__", "__gt__", "__ge__"):
            setattr(cls, op, _make_order(ot, op))
    if frozen_:
        cls.__setattr__, cls.__delattr__, _ = _make_setattr(attrs, True, None, None)
    elif on_setattr is not None or any(a.on_setattr is not None for a in attrs):
        any_hooks = False
        s, d, any_hooks = _make_setattr(attrs, False, on_setattr, None)
        if any_hooks and (define_mode is False or any(a.validator is not None or a.converter is not None or
                                                      a.on_setattr is not None for a in attrs)):
            cls.__setattr__ = s
    if hash_ is True:
        cls.__hash__ = _make_hash(attrs, cls)
    elif hash_ is None:
        if eq and frozen_:
            cls.__hash__ = _make_hash(attrs, cls)
        elif eq:
            cls.__hash__ = None
    elif hash_ is False:
        pass
    if match_args and "__match_args__" not in cls.__dict__:
        cls.__match_args__ = tuple(a.alias for a in attrs if a.init and not a.kw_only)
    return cls


def attrs(maybe_cls=None, these=None, repr_ns=None, repr=None, cmp=None, hash=None, init=None, slots=False,
          frozen=False, weakref_slot=True, str=False, auto_attribs=False, kw_only=False, cache_hash=False,
          auto_exc=False, eq=None, order=None, auto_detect=False, collect_by_mro=False, getstate_setstate=None,
          on_setattr=None, field_transformer=None, match_args=True, unsafe_hash=None):
    eq, order = _eq_order_class(cmp, eq, order)

    def wrap(cls):
        return _wrap(cls, these, True if repr is None else repr, eq, order, hash, True if init is None else init,
                     frozen, kw_only, auto_attribs, auto_detect, on_setattr, False, match_args, field_transformer,
                     unsafe_hash)
    if maybe_cls is None:
        return wrap
    return wrap(maybe_cls)


def _eq_order_class(cmp, eq, order):
    if cmp is not None and (eq is not None or order is not None):
        raise ValueError("Don't mix `cmp` with `eq' and `order`.")
    if cmp is not None:
        return cmp, cmp
    return eq, order


s = attrs
attributes = attrs


def dataclass(maybe_cls=None, *, these=None, repr=True, unsafe_hash=None, hash=None, init=True, slots=False,
              frozen=False, weakref_slot=True, str=False, auto_attribs=True, kw_only=False, cache_hash=False,
              auto_exc=True, eq=True, order=False, auto_detect=True, getstate_setstate=None, on_setattr=None,
              field_transformer=None, match_args=True):
    return attrs(maybe_cls, these=these, repr=repr, hash=hash, init=init, slots=slots, frozen=frozen,
                 auto_attribs=auto_attribs, kw_only=kw_only, eq=eq, order=order, auto_detect=auto_detect,
                 on_setattr=on_setattr, field_transformer=field_transformer, match_args=match_args,
                 unsafe_hash=unsafe_hash)


def define(maybe_cls=None, *, these=None, repr=None, unsafe_hash=None, hash=None, init=None, slots=True,
           frozen=False, weakref_slot=True, str=False, auto_attribs=None, kw_only=False, cache_hash=False,
           auto_exc=True, eq=None, order=False, auto_detect=True, getstate_setstate=None, on_setattr=None,
           field_transformer=None, match_args=True):
    def wrap(cls):
        return _wrap(cls, these, True if repr is None else repr, eq, order, hash, True if init is None else init,
                     frozen, kw_only, auto_attribs, auto_detect, on_setattr, True, match_args, field_transformer,
                     unsafe_hash)
    if maybe_cls is None:
        return wrap
    return wrap(maybe_cls)


mutable = define


def frozen(maybe_cls=None, **kwargs):
    kwargs["frozen"] = True
    return define(maybe_cls, **kwargs)


def make_class(name, attrs, bases=(object,), class_body=None, **attributes_arguments):
    if isinstance(attrs, dict):
        cls_dict = {}
        for k, v in attrs.items():
            cls_dict[k] = v
        cd = dict((k, _recount(v)) for k, v in cls_dict.items())
    elif isinstance(attrs, (list, tuple)):
        cd = {}
        for k in attrs:
            cd[k] = attrib()
    else:
        raise TypeError("attrs argument must be a dict or a list.")
    if class_body is not None:
        cd.update(class_body)
    cls = type(name, tuple(bases), cd)
    return attrs_decorator(cls, attributes_arguments)


def _recount(v):
    if isinstance(v, _CountingAttr):
        v.counter = _next_count()
    return v


def attrs_decorator(cls, kwargs):
    return globals()["attrs"](cls, **kwargs)


from attr import converters, filters, setters, validators  # noqa: E402  (they import names defined above)
