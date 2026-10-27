# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Type hints: the names exist and subscript, nothing is checked at run time."""

import collections.abc as _abc
import functools as _functools

TYPE_CHECKING = False


def _type_repr(obj):
    if obj is None or obj is type(None):
        return "None"
    if isinstance(obj, type):
        if obj.__module__ == "builtins":
            return obj.__qualname__
        return obj.__module__ + "." + obj.__qualname__
    if obj is ...:
        return "..."
    if type(obj).__name__ == "function":
        return obj.__name__
    if isinstance(obj, (list, tuple)):
        return "[" + ", ".join(_type_repr(a) for a in obj) + "]"
    return repr(obj)


class _Final:
    __slots__ = ()


class _SpecialForm(_Final):
    def __init__(self, name, getitem=None):
        self._name = name
        self._getitem = getitem

    def __repr__(self):
        return "typing." + self._name

    def __getitem__(self, params):
        if self._getitem is None:
            raise TypeError(self._name + " is not subscriptable")
        return self._getitem(self, params)

    def __call__(self, *args, **kwds):
        raise TypeError("Cannot instantiate " + repr(self))

    def __or__(self, other):
        return Union[self, other]

    def __ror__(self, other):
        return Union[other, self]

    def __hash__(self):
        return hash(self._name)

    def __eq__(self, other):
        return isinstance(other, _SpecialForm) and other._name == self._name


class _GenericAlias(_Final):
    def __init__(self, origin, args, name=None):
        if not isinstance(args, tuple):
            args = (args,)
        self.__origin__ = origin
        self.__args__ = args
        self._name = name
        self.__parameters__ = tuple(a for a in args if isinstance(a, TypeVar))

    def __repr__(self):
        if self._name:
            name = "typing." + self._name
        else:
            name = _type_repr(self.__origin__)
        if not self.__args__:
            return name + "[()]"
        return name + "[" + ", ".join(_type_repr(a) for a in self.__args__) + "]"

    def __eq__(self, other):
        if not isinstance(other, _GenericAlias):
            return NotImplemented
        return self.__origin__ == other.__origin__ and self.__args__ == other.__args__

    def __hash__(self):
        return hash((self.__origin__, self.__args__))

    def __call__(self, *args, **kwargs):
        result = self.__origin__(*args, **kwargs)
        try:
            result.__orig_class__ = self
        except Exception:
            pass
        return result

    def __getitem__(self, params):
        if not isinstance(params, tuple):
            params = (params,)
        subst = dict(zip(self.__parameters__, params))
        return _GenericAlias(self.__origin__, tuple(subst.get(a, a) for a in self.__args__), self._name)

    def __or__(self, other):
        return Union[self, other]

    def __ror__(self, other):
        return Union[other, self]

    def __mro_entries__(self, bases):
        return (self.__origin__,)

    def __instancecheck__(self, obj):
        raise TypeError("Subscripted generics cannot be used with class and instance checks")


class _UnionGenericAlias(_GenericAlias):
    def __repr__(self):
        return " | ".join(_type_repr(a) for a in self.__args__)

    def __eq__(self, other):
        if not isinstance(other, _UnionGenericAlias):
            return NotImplemented
        return set(self.__args__) == set(other.__args__)

    def __hash__(self):
        return hash(frozenset(self.__args__))


def _union(form, params):
    if not isinstance(params, tuple):
        params = (params,)
    flat = []
    for p in params:
        p = type(None) if p is None else p
        if isinstance(p, _UnionGenericAlias):
            items = p.__args__
        else:
            items = (p,)
        for i in items:
            if i not in flat:
                flat.append(i)
    if len(flat) == 1:
        return flat[0]
    return _UnionGenericAlias(Union, tuple(flat), "Union")


def _optional(form, param):
    return _union(form, (param, None))


def _subscript(form, params):
    return _GenericAlias(form, params, form._name)


def _literal(form, params):
    if not isinstance(params, tuple):
        params = (params,)
    return _GenericAlias(form, params, "Literal")


Any = _SpecialForm("Any")
NoReturn = _SpecialForm("NoReturn")
Never = _SpecialForm("Never")
Self = _SpecialForm("Self")
LiteralString = _SpecialForm("LiteralString")
TypeAlias = _SpecialForm("TypeAlias")
Union = _SpecialForm("Union", _union)
Optional = _SpecialForm("Optional", _optional)
ClassVar = _SpecialForm("ClassVar", _subscript)
Final = _SpecialForm("Final", _subscript)
Literal = _SpecialForm("Literal", _literal)
TypeGuard = _SpecialForm("TypeGuard", _subscript)
TypeIs = _SpecialForm("TypeIs", _subscript)
Required = _SpecialForm("Required", _subscript)
NotRequired = _SpecialForm("NotRequired", _subscript)
ReadOnly = _SpecialForm("ReadOnly", _subscript)
Concatenate = _SpecialForm("Concatenate", _subscript)
Unpack = _SpecialForm("Unpack", _subscript)


class _AnnotatedAlias(_GenericAlias):
    def __init__(self, origin, metadata):
        super().__init__(origin, (origin,))
        self.__metadata__ = metadata

    def __repr__(self):
        return "typing.Annotated[" + _type_repr(self.__origin__) + ", " + \
            ", ".join(repr(m) for m in self.__metadata__) + "]"


def _annotated(form, params):
    if not isinstance(params, tuple) or len(params) < 2:
        raise TypeError("Annotated[...] should be used with at least two arguments (a type and an annotation).")
    return _AnnotatedAlias(params[0], tuple(params[1:]))


Annotated = _SpecialForm("Annotated", _annotated)


class _SpecialGenericAlias(_Final):
    def __init__(self, origin, name, nparams=-1):
        self.__origin__ = origin
        self._name = name
        self._nparams = nparams

    def __repr__(self):
        return "typing." + self._name

    def __getitem__(self, params):
        if self._name == "Callable" and isinstance(params, tuple) and len(params) == 2:
            args, result = params
            if args is ...:
                return _CallableGenericAlias(self.__origin__, (..., result), self._name)
            return _CallableGenericAlias(self.__origin__, tuple(args) + (result,), self._name)
        return _GenericAlias(self.__origin__, params, self._name)

    def __call__(self, *args, **kwargs):
        return self.__origin__(*args, **kwargs)

    def __mro_entries__(self, bases):
        return (self.__origin__,)

    def __instancecheck__(self, obj):
        return isinstance(obj, self.__origin__)

    def __subclasscheck__(self, cls):
        return issubclass(cls, self.__origin__)

    def __or__(self, other):
        return Union[self, other]

    def __ror__(self, other):
        return Union[other, self]


class _CallableGenericAlias(_GenericAlias):
    def __repr__(self):
        args = self.__args__
        if len(args) == 2 and args[0] is ...:
            return "typing.Callable[..., " + _type_repr(args[1]) + "]"
        return "typing.Callable[[" + ", ".join(_type_repr(a) for a in args[:-1]) + "], " + \
            _type_repr(args[-1]) + "]"


List = _SpecialGenericAlias(list, "List", 1)
Dict = _SpecialGenericAlias(dict, "Dict", 2)
Set = _SpecialGenericAlias(set, "Set", 1)
FrozenSet = _SpecialGenericAlias(frozenset, "FrozenSet", 1)
Tuple = _SpecialGenericAlias(tuple, "Tuple")
Type = _SpecialGenericAlias(type, "Type", 1)
Callable = _SpecialGenericAlias(_abc.Callable, "Callable", 2)
Iterable = _SpecialGenericAlias(_abc.Iterable, "Iterable", 1)
Iterator = _SpecialGenericAlias(_abc.Iterator, "Iterator", 1)
Generator = _SpecialGenericAlias(_abc.Generator, "Generator", 3)
AsyncIterable = _SpecialGenericAlias(_abc.AsyncIterable, "AsyncIterable", 1)
AsyncIterator = _SpecialGenericAlias(_abc.AsyncIterator, "AsyncIterator", 1)
AsyncGenerator = _SpecialGenericAlias(_abc.AsyncGenerator, "AsyncGenerator", 2)
Awaitable = _SpecialGenericAlias(_abc.Awaitable, "Awaitable", 1)
Coroutine = _SpecialGenericAlias(_abc.Coroutine, "Coroutine", 3)
Sequence = _SpecialGenericAlias(_abc.Sequence, "Sequence", 1)
MutableSequence = _SpecialGenericAlias(_abc.MutableSequence, "MutableSequence", 1)
Mapping = _SpecialGenericAlias(_abc.Mapping, "Mapping", 2)
MutableMapping = _SpecialGenericAlias(_abc.MutableMapping, "MutableMapping", 2)
AbstractSet = _SpecialGenericAlias(_abc.Set, "AbstractSet", 1)
MutableSet = _SpecialGenericAlias(_abc.MutableSet, "MutableSet", 1)
Collection = _SpecialGenericAlias(_abc.Collection, "Collection", 1)
Container = _SpecialGenericAlias(_abc.Container, "Container", 1)
Sized = _SpecialGenericAlias(_abc.Sized, "Sized", 0)
Hashable = _SpecialGenericAlias(_abc.Hashable, "Hashable", 0)
Reversible = _SpecialGenericAlias(_abc.Reversible, "Reversible", 1)
KeysView = _SpecialGenericAlias(_abc.KeysView, "KeysView", 1)
ItemsView = _SpecialGenericAlias(_abc.ItemsView, "ItemsView", 2)
ValuesView = _SpecialGenericAlias(_abc.ValuesView, "ValuesView", 1)


def _lazy_collections():
    import collections
    return collections


DefaultDict = _SpecialGenericAlias(_lazy_collections().defaultdict, "DefaultDict", 2)
OrderedDict = _SpecialGenericAlias(_lazy_collections().OrderedDict, "OrderedDict", 2)
Counter = _SpecialGenericAlias(_lazy_collections().Counter, "Counter", 1)
Deque = _SpecialGenericAlias(_lazy_collections().deque, "Deque", 1)
ChainMap = _SpecialGenericAlias(_lazy_collections().ChainMap, "ChainMap", 2)
Text = str
AnyStr = None


class TypeVar(_Final):
    def __init__(self, name, *constraints, bound=None, covariant=False, contravariant=False,
                 infer_variance=False, default=None):
        self.__name__ = name
        self.__constraints__ = constraints
        self.__bound__ = bound
        self.__covariant__ = covariant
        self.__contravariant__ = contravariant
        self.__infer_variance__ = infer_variance
        self.__default__ = default

    def __repr__(self):
        if self.__infer_variance__:
            prefix = ""
        elif self.__covariant__:
            prefix = "+"
        elif self.__contravariant__:
            prefix = "-"
        else:
            prefix = "~"
        return prefix + self.__name__

    def __or__(self, other):
        return Union[self, other]

    def __ror__(self, other):
        return Union[other, self]


AnyStr = TypeVar("AnyStr", bytes, str)


class ParamSpecArgs(_Final):
    def __init__(self, origin):
        self.__origin__ = origin

    def __repr__(self):
        return self.__origin__.__name__ + ".args"


class ParamSpecKwargs(_Final):
    def __init__(self, origin):
        self.__origin__ = origin

    def __repr__(self):
        return self.__origin__.__name__ + ".kwargs"


class ParamSpec(_Final):
    def __init__(self, name, *, bound=None, covariant=False, contravariant=False, default=None):
        self.__name__ = name
        self.__bound__ = bound
        self.args = ParamSpecArgs(self)
        self.kwargs = ParamSpecKwargs(self)

    def __repr__(self):
        return "~" + self.__name__


class TypeVarTuple(_Final):
    def __init__(self, name, *, default=None):
        self.__name__ = name

    def __repr__(self):
        return self.__name__

    def __iter__(self):
        yield Unpack[self]


class Generic:
    __slots__ = ()

    def __class_getitem__(cls, params):
        if not isinstance(params, tuple):
            params = (params,)
        if cls is Generic or cls is Protocol:
            return _GenericAlias(cls, params, cls.__name__)
        return _GenericAlias(cls, params)

    def __init_subclass__(cls, *args, **kwargs):
        super().__init_subclass__(*args, **kwargs)


class _ProtocolMeta(type(_abc.Sized)):
    def __instancecheck__(cls, instance):
        if not cls.__dict__.get("_is_runtime_protocol", False) and not cls.__dict__.get("_is_protocol", False):
            return super().__instancecheck__(instance)
        if super().__instancecheck__(instance):
            return True
        if cls.__dict__.get("_is_protocol", False) and not cls.__dict__.get("_is_runtime_protocol", False):
            raise TypeError("Instance and class checks can only be used with @runtime_checkable protocols")
        for attr in cls.__protocol_attrs__:
            if not hasattr(instance, attr):
                return False
        return True


class Protocol(Generic, metaclass=_ProtocolMeta):
    __slots__ = ()
    _is_protocol = True

    def __init_subclass__(cls, *args, **kwargs):
        super().__init_subclass__(*args, **kwargs)
        if not cls.__dict__.get("_is_protocol", False):
            cls._is_protocol = any(b is Protocol for b in cls.__bases__)
        attrs = set()
        for base in cls.__mro__[:-1]:
            if base.__name__ in ("Protocol", "Generic", "object"):
                continue
            for name in list(base.__dict__.keys()) + list(base.__dict__.get("__annotations__", {}).keys()):
                if not name.startswith("_abc_") and name not in ("__module__", "__qualname__", "__doc__",
                                                                "__annotations__", "__dict__", "__weakref__",
                                                                "__init__", "__slots__", "_is_protocol",
                                                                "__abstractmethods__", "__parameters__",
                                                                "__orig_bases__", "__protocol_attrs__",
                                                                "_is_runtime_protocol", "__init_subclass__",
                                                                "__subclasshook__", "__match_args__",
                                                                "__firstlineno__", "__static_attributes__",
                                                                "_abc_registry"):
                    attrs.add(name)
        cls.__protocol_attrs__ = attrs


def runtime_checkable(cls):
    cls._is_runtime_protocol = True
    return cls


def cast(typ, val):
    return val


def assert_type(val, typ, /):
    return val


def assert_never(arg, /):
    raise AssertionError("Expected code to be unreachable, but got: " + repr(arg)[:100])


def reveal_type(obj, /):
    import sys
    print("Runtime type is " + repr(type(obj).__name__), file=sys.stderr)
    return obj


def overload(func):
    def _overload_dummy(*args, **kwds):
        raise NotImplementedError("You should not call an overloaded function. "
                                  "A series of @typing.overload-decorated functions "
                                  "outside a stub module should always be followed "
                                  "by an implementation that is not @typing.overload-ed.")
    return _overload_dummy


def get_overloads(func):
    return []


def clear_overloads():
    pass


def final(f):
    try:
        f.__final__ = True
    except (AttributeError, TypeError):
        pass
    return f


def override(method, /):
    try:
        method.__override__ = True
    except (AttributeError, TypeError):
        pass
    return method


def no_type_check(arg):
    return arg


def no_type_check_decorator(decorator):
    return decorator


def dataclass_transform(*, eq_default=True, order_default=False, kw_only_default=False, frozen_default=False,
                        field_specifiers=(), **kwargs):
    def decorator(cls_or_fn):
        cls_or_fn.__dataclass_transform__ = {"eq_default": eq_default, "order_default": order_default,
                                             "kw_only_default": kw_only_default, "frozen_default": frozen_default,
                                             "field_specifiers": field_specifiers, "kwargs": kwargs}
        return cls_or_fn
    return decorator


def get_origin(tp):
    if isinstance(tp, _AnnotatedAlias):
        return Annotated
    if isinstance(tp, (_GenericAlias, _SpecialGenericAlias)):
        return tp.__origin__
    if tp is Generic:
        return Generic
    return None


def get_args(tp):
    if isinstance(tp, _AnnotatedAlias):
        return (tp.__origin__,) + tp.__metadata__
    if isinstance(tp, _CallableGenericAlias):
        args = tp.__args__
        if args[0] is ...:
            return args
        return (list(args[:-1]), args[-1])
    if isinstance(tp, _GenericAlias):
        return tp.__args__
    return ()


def get_type_hints(obj, globalns=None, localns=None, include_extras=False):
    hints = {}
    if isinstance(obj, type):
        for base in reversed(obj.__mro__):
            hints.update(base.__dict__.get("__annotations__", {}))
    else:
        hints.update(getattr(obj, "__annotations__", {}))
    if not include_extras:
        for k, v in list(hints.items()):
            if isinstance(v, _AnnotatedAlias):
                hints[k] = v.__origin__
    return hints


def is_typeddict(tp):
    return getattr(tp, "__total__", None) is not None and isinstance(tp, type) and issubclass(tp, dict)


class NewType:
    def __init__(self, name, tp):
        self.__qualname__ = name
        self.__name__ = name
        self.__supertype__ = tp

    def __repr__(self):
        return self.__name__

    def __call__(self, x):
        return x

    def __or__(self, other):
        return Union[self, other]

    def __ror__(self, other):
        return Union[other, self]


class TypeAliasType:
    def __init__(self, name, value, *, type_params=()):
        self.__name__ = name
        self.__value__ = value
        self.__type_params__ = type_params

    def __repr__(self):
        return self.__name__

    def __getitem__(self, params):
        return _GenericAlias(self, params)


class _NamedTupleMeta(type):
    def __new__(cls, typename, bases, ns):
        import collections
        types = ns.get("__annotations__", {})
        default_names = [n for n in types if n in ns]
        nm_tpl = collections.namedtuple(typename, list(types.keys()),
                                        defaults=[ns[n] for n in default_names])
        nm_tpl.__annotations__ = types
        for key, val in ns.items():
            if key not in ("__annotations__", "__module__", "__qualname__") and key not in types and \
                    key not in ("__slots__", "_fields", "__new__", "__init__"):
                setattr(nm_tpl, key, val)
        return nm_tpl


def NamedTuple(typename, fields=None, /, **kwargs):
    import collections
    if fields is None:
        fields = kwargs.items()
    fields = list(fields)
    nt = collections.namedtuple(typename, [n for n, t in fields])
    nt.__annotations__ = dict(fields)
    return nt


_NamedTuple = type.__new__(_NamedTupleMeta, "NamedTuple", (), {})


def _namedtuple_mro_entries(bases):
    return (_NamedTuple,)


NamedTuple.__mro_entries__ = _namedtuple_mro_entries


class _TypedDictMeta(type):
    def __new__(cls, name, bases, ns, total=True):
        annotations = {}
        for base in bases:
            annotations.update(base.__dict__.get("__annotations__", {}))
        annotations.update(ns.get("__annotations__", {}))
        tp_dict = type.__new__(_TypedDictMeta, name, (dict,), ns)
        tp_dict.__annotations__ = annotations
        tp_dict.__total__ = total
        tp_dict.__required_keys__ = frozenset(annotations) if total else frozenset()
        tp_dict.__optional_keys__ = frozenset() if total else frozenset(annotations)
        return tp_dict

    def __call__(cls, *args, **kwargs):
        # A TypedDict describes a plain dict; instances are exactly that.
        return dict(*args, **kwargs)

    def __instancecheck__(cls, other):
        raise TypeError("TypedDict does not support instance and class checks")

    __subclasscheck__ = __instancecheck__


def TypedDict(typename, fields=None, /, *, total=True, **kwargs):
    if fields is None:
        fields = kwargs
    ns = {"__annotations__": dict(fields)}
    return _TypedDictMeta(typename, (), ns, total=total)


_TypedDict = type.__new__(_TypedDictMeta, "TypedDict", (), {})


def _typeddict_mro_entries(bases):
    return (_TypedDict,)


TypedDict.__mro_entries__ = _typeddict_mro_entries


class SupportsInt(Protocol):
    def __int__(self):
        pass


class SupportsFloat(Protocol):
    def __float__(self):
        pass


class SupportsIndex(Protocol):
    def __index__(self):
        pass


class SupportsAbs(Protocol):
    def __abs__(self):
        pass


class SupportsRound(Protocol):
    def __round__(self, ndigits=0):
        pass


runtime_checkable(SupportsInt)
runtime_checkable(SupportsFloat)
runtime_checkable(SupportsIndex)
runtime_checkable(SupportsAbs)
runtime_checkable(SupportsRound)
