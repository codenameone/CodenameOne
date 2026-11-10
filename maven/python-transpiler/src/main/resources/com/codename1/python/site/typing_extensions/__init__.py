# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""typing_extensions over the runtime's typing module.

Every name the runtime's typing defines is re-exported; the few it lacks are
small objects defined here.
"""

import warnings as _warnings

from typing import (
    Any, AbstractSet, Annotated, AnyStr, AsyncGenerator, AsyncIterable,
    AsyncIterator, Awaitable, Callable, ChainMap, ClassVar, Collection,
    Concatenate, Container, Coroutine, Counter, DefaultDict, Deque, Dict,
    Final, FrozenSet, Generator, Generic, Hashable, ItemsView, Iterable,
    Iterator, KeysView, List, Literal, LiteralString, Mapping,
    MutableMapping, MutableSequence, MutableSet, NamedTuple, Never, NewType,
    NoReturn, NotRequired, Optional, OrderedDict, ParamSpec, ParamSpecArgs,
    ParamSpecKwargs, Protocol, ReadOnly, Required, Reversible, Self,
    Sequence, Set, Sized, SupportsAbs, SupportsFloat, SupportsIndex,
    SupportsInt, SupportsRound, TYPE_CHECKING, Text, Tuple, Type, TypeAlias,
    TypeAliasType, TypeGuard, TypeIs, TypeVar, TypeVarTuple, TypedDict,
    Union, Unpack, ValuesView, assert_never, assert_type, cast,
    clear_overloads, dataclass_transform, final, get_args, get_origin,
    get_overloads, get_type_hints, is_typeddict, no_type_check,
    no_type_check_decorator, overload, override, reveal_type,
    runtime_checkable,
)


class Doc:
    """Documentation carried inside Annotated metadata."""

    def __init__(self, documentation):
        self.documentation = documentation

    def __repr__(self):
        return "Doc(" + repr(self.documentation) + ")"

    def __hash__(self):
        return hash(self.documentation)

    def __eq__(self, other):
        if not isinstance(other, Doc):
            return NotImplemented
        return self.documentation == other.documentation


class deprecated:
    """Mark a class or function as deprecated; calling it warns."""

    def __init__(self, message, category=DeprecationWarning, stacklevel=1):
        if not isinstance(message, str):
            raise TypeError("Expected an object of type str for 'message', not " + type(message).__name__)
        self.message = message
        self.category = category
        self.stacklevel = stacklevel

    def __call__(self, arg):
        msg = self.message
        category = self.category
        stacklevel = self.stacklevel
        if category is None:
            arg.__deprecated__ = msg
            return arg
        if isinstance(arg, type):
            orig_init = arg.__init__

            def __init__(self, *args, **kwargs):
                _warnings.warn(msg, category, stacklevel + 1)
                orig_init(self, *args, **kwargs)

            arg.__init__ = __init__
            arg.__deprecated__ = msg
            return arg
        import functools as _ft

        @_ft.wraps(arg)
        def wrapper(*args, **kwargs):
            _warnings.warn(msg, category, stacklevel + 1)
            return arg(*args, **kwargs)

        wrapper.__deprecated__ = msg
        return wrapper


def get_original_bases(cls):
    return cls.__dict__.get("__orig_bases__", cls.__bases__)


def get_annotations(obj, globals=None, locals=None, eval_str=False):
    return dict(getattr(obj, "__annotations__", None) or {})


PEP_560 = True
Buffer = bytes
CapsuleType = object
NoDefault = object()
NoExtraItems = object()
