# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Enumerations: classes whose attributes are a fixed set of named members."""

__all__ = ["EnumType", "EnumMeta", "Enum", "IntEnum", "StrEnum", "Flag", "IntFlag", "ReprEnum", "auto", "unique",
           "member", "nonmember", "global_enum", "verify", "UNIQUE", "CONTINUOUS", "NAMED_FLAGS", "EnumCheck",
           "FlagBoundary", "STRICT", "CONFORM", "EJECT", "KEEP", "property"]

_MIXINS = (int, str, float, bytes)
builtins_property = property
from _cn1op import EnumType, enum_install
# The library's own bases, whose __new__ never builds a member.
_BASES = []


class auto:
    """A placeholder replaced by an automatically chosen value."""
    _counter = 0

    def __init__(self, value=None):
        self.value = value
        auto._counter += 1
        self._order = auto._counter

    def __repr__(self):
        return "auto(" + repr(self.value) + ")"


class member:
    def __init__(self, value):
        self.value = value


class nonmember:
    def __init__(self, value):
        self.value = value




def _is_dunder(name):
    return len(name) > 4 and name[:2] == name[-2:] == "__" and name[2] != "_" and name[-3] != "_"


def _is_sunder(name):
    return len(name) > 2 and name[0] == name[-1] == "_" and name[1] != "_" and name[-2] != "_"


def _is_private(cls_name, name):
    return name.startswith("_" + cls_name.lstrip("_") + "__")


def _is_descriptor(obj):
    t = type(obj)
    if t.__name__ in ("function", "builtin_function_or_method", "method"):
        return True
    return isinstance(obj, (classmethod, staticmethod, type)) or hasattr(t, "__get__") or \
        hasattr(t, "__set__") or hasattr(t, "__delete__")


def _mixin_of(bases):
    for base in bases:
        for k in base.__mro__:
            if k in _MIXINS:
                return k
    return None


# EnumType is the runtime's own metaclass (runtime class EnumMeta), as
# ABCMeta is abc's: a class it makes is an ordinary class with a layout, and
# its class-level protocol (call, iteration, in, len, [name], __members__) is
# Java over the tables below. Right after making one, the runtime calls
# _finish, which sets those tables up; the compiler then defines every
# member once, in definition order (_member).


def _finish(enum_class, boundary):
    cls = enum_class.__name__
    bases = enum_class.__bases__
    type.__setattr__(enum_class, "_member_names_", [])
    type.__setattr__(enum_class, "_member_map_", {})
    type.__setattr__(enum_class, "_value2member_map_", {})
    type.__setattr__(enum_class, "_unhashable_values_", [])
    mixin = _mixin_of(bases)
    type.__setattr__(enum_class, "_member_type_", mixin if mixin is not None else object)
    flag = len(_FLAG) > 0 and any(isinstance(b, EnumType) and issubclass(b, _FLAG[0]) for b in bases)
    type.__setattr__(enum_class, "_is_flag_", flag)
    type.__setattr__(enum_class, "_boundary_", boundary)
    if any(isinstance(b, EnumType) and b._member_names_ for b in bases):
        raise TypeError(
            "<enum %r> cannot extend %s" % (cls, ", ".join("<enum %r>" % b.__name__ for b in bases
                                                            if isinstance(b, EnumType) and b._member_names_)))
    type.__setattr__(enum_class, "_last_values_", [])
    for name, value in enum_class.__dict__.items():
        if not (_is_dunder(name) or _is_sunder(name) or _is_private(cls, name) or _is_descriptor(value)):
            # The compiler defines every member after the class statement
            # (EnumLowering); a value left in the body is one it missed.
            raise TypeError("enum %s: member %r was not defined by the compiler" % (cls, name))
    if mixin is not None:
        # The mixin type comes first in the MRO, so its __repr__ and
        # __str__ would shadow the enum's: take the nearest enum's own. Its
        # hash stays the mixin's, as the MRO has it.
        for dunder in ("__repr__", "__str__", "__format__"):
            if dunder in enum_class.__dict__:
                continue
            for k in enum_class.__mro__:
                if isinstance(k, EnumType) and dunder in k.__dict__:
                    type.__setattr__(enum_class, dunder, k.__dict__[dunder])
                    break
        if "__hash__" not in enum_class.__dict__:
            type.__setattr__(enum_class, "__hash__", mixin.__hash__)
    type.__setattr__(enum_class, "_flag_mask_", 0)
    type.__setattr__(enum_class, "_all_bits_", 0)


def _make_member(enum_class, name, value, mixin):
    args = value if isinstance(value, tuple) else (value,)
    new = None
    for k in enum_class.__mro__:
        if "__new__" in k.__dict__ and k not in _BASES and k is not object:
            if k not in _MIXINS:
                new = k.__dict__["__new__"]
            break
    if new is not None:
        m = new(enum_class, *args)
    elif mixin is None:
        m = object.__new__(enum_class)
    else:
        m = mixin.__new__(enum_class, *args)
    if not hasattr(m, "_value_"):
        if mixin is None:
            m._value_ = value
        else:
            m._value_ = mixin(*args)
    m._name_ = name
    m._sort_order_ = len(enum_class._member_names_)
    init = getattr(enum_class, "__init__", None)
    if init is not None and init is not Enum.__init__:
        # The function found on the class, called with the member: what
        # m.__init__(*args) runs, but read off the class, which only a
        # loose class's __init__ can answer -- through the member, any
        # __init__ of the program could.
        init(m, *args)
    return m


def _member(enum_class, name, value):
    """Defines member `name` of `value`: the compiler calls it once per
    member, in definition order, right after the class statement."""
    if name in enum_class._member_map_:
        raise TypeError("%r already defined as %r" % (name, enum_class._member_map_[name]))
    last_values = enum_class._last_values_
    if isinstance(value, auto):
        if value.value is None:
            value = enum_class._generate_next_value_(name, 1, len(enum_class._member_names_), last_values[:])
        else:
            value = value.value
    elif isinstance(value, tuple) and any(isinstance(v, auto) for v in value):
        value = tuple(enum_class._generate_next_value_(name, 1, len(enum_class._member_names_),
                                                       last_values[:]) if isinstance(v, auto) else v
                      for v in value)
    last_values.append(value)
    m = _make_member(enum_class, name, value, _mixin_of(enum_class.__bases__))
    canonical = None
    for other in enum_class._member_names_:
        o = enum_class._member_map_[other]
        if o._value_ == m._value_:
            canonical = o
            break
    if canonical is not None:
        m = canonical
    else:
        enum_class._member_names_.append(name)
        if enum_class._is_flag_ and isinstance(m._value_, int):
            bits = enum_class._all_bits_ | m._value_
            type.__setattr__(enum_class, "_flag_mask_", bits)
            type.__setattr__(enum_class, "_all_bits_", bits)
    enum_class._member_map_[name] = m
    try:
        enum_class._value2member_map_.setdefault(m._value_, m)
    except TypeError:
        enum_class._unhashable_values_.append(m._value_)
    return m


enum_install(_finish)


EnumMeta = EnumType
# Flag once it exists; a list, so the name Flag is bound once (by its class)
# and keeps a layout its subclasses extend.
_FLAG = []


class Enum(metaclass=EnumType):
    def __init__(self, *args, **kwds):
        pass

    @staticmethod
    def _generate_next_value_(name, start, count, last_values):
        if not last_values:
            return start
        try:
            last = last_values[-1]
            for v in reversed(last_values):
                if isinstance(v, int) and not isinstance(v, bool):
                    last = v
                    break
            return last + 1
        except TypeError:
            raise TypeError("unable to sort non-numeric values") from None

    @classmethod
    def _missing_(cls, value):
        return None

    def __repr__(self):
        v_repr = type(self)._member_type_.__repr__ if type(self)._member_type_ is not object else repr
        return "<%s.%s: %s>" % (type(self).__name__, self._name_, v_repr(self._value_))

    def __str__(self):
        return "%s.%s" % (type(self).__name__, self._name_)

    def __format__(self, format_spec):
        return str.__format__(str(self), format_spec)

    def __hash__(self):
        return hash(self._name_)

    def __reduce_ex__(self, proto):
        return self.__class__, (self._value_,)

    def __deepcopy__(self, memo):
        return self

    def __copy__(self):
        return self

    def __dir__(self):
        return ["__class__", "__doc__", "__eq__", "__hash__", "__module__", "name", "value"]

    @builtins_property
    def name(self):
        return self._name_

    @builtins_property
    def value(self):
        return self._value_


_BASES.append(Enum)


class ReprEnum(Enum):
    def __str__(self):
        mixin = type(self)._member_type_
        return str.__str__(self) if issubclass(mixin, str) else mixin.__repr__(self)

    def __format__(self, format_spec):
        return type(self)._member_type_.__format__(self, format_spec)


class IntEnum(int, ReprEnum):
    __str__ = ReprEnum.__str__
    __format__ = ReprEnum.__format__


_BASES.extend((ReprEnum, IntEnum))


class StrEnum(str, ReprEnum):
    __str__ = ReprEnum.__str__
    __format__ = ReprEnum.__format__

    @staticmethod
    def _generate_next_value_(name, start, count, last_values):
        return name.lower()


_BASES.append(StrEnum)


class FlagBoundary(StrEnum):
    STRICT = auto()
    CONFORM = auto()
    EJECT = auto()
    KEEP = auto()


STRICT, CONFORM, EJECT, KEEP = FlagBoundary.STRICT, FlagBoundary.CONFORM, FlagBoundary.EJECT, FlagBoundary.KEEP


def _bits(n):
    out = []
    while n:
        low = n & (0 - n)
        out.append(low)
        n ^= low
    return out


class Flag(Enum, boundary=STRICT):
    @staticmethod
    def _generate_next_value_(name, start, count, last_values):
        if not count:
            return start if start is not None else 1
        last = max(v for v in last_values if isinstance(v, int)) if last_values else 0
        return 1 << last.bit_length() if last else 1

    @classmethod
    def _missing_(cls, value):
        if not isinstance(value, int):
            raise ValueError("%r is not a valid %s" % (value, cls.__qualname__))
        mask = cls._flag_mask_
        boundary = cls._boundary_
        neg = value < 0
        if neg:
            value = value & mask if mask else value
        if value & ~mask:
            if boundary is STRICT:
                raise ValueError("%s invalid value %r\n    given %s\n  allowed %s"
                                 % (cls.__name__, value, bin(value), bin(mask)))
            if boundary is CONFORM:
                value = value & mask
            elif boundary is EJECT:
                return value
        mixin = cls._member_type_
        if mixin is object:
            pseudo = object.__new__(cls)
        else:
            pseudo = mixin.__new__(cls, value)
        pseudo._value_ = value
        names = [m._name_ for m in cls._member_map_.values() if m._value_ and m._value_ & value == m._value_
                 and len(_bits(m._value_)) == 1]
        unnamed = value
        for m in cls:
            if m._value_ & value == m._value_:
                unnamed &= ~m._value_
        if unnamed and boundary is KEEP:
            names.append(hex(unnamed))
        pseudo._name_ = "|".join(names) if names or value else None
        if pseudo._name_ is not None and not names:
            pseudo._name_ = None
        pseudo._sort_order_ = 0
        cls._value2member_map_.setdefault(value, pseudo)
        return cls._value2member_map_[value]

    def __contains__(self, other):
        if not isinstance(other, self.__class__):
            raise TypeError("unsupported operand type(s) for 'in': %r and %r"
                            % (type(other).__qualname__, self.__class__.__qualname__))
        return other._value_ & self._value_ == other._value_

    def __iter__(self):
        for m in type(self):
            if m._value_ and m._value_ & self._value_ == m._value_:
                yield m

    def __len__(self):
        return len(_bits(self._value_))

    def __repr__(self):
        cls_name = self.__class__.__name__
        v_repr = type(self)._member_type_.__repr__ if type(self)._member_type_ is not object else repr
        if self._name_ is None:
            return "<%s: %s>" % (cls_name, v_repr(self._value_))
        return "<%s.%s: %s>" % (cls_name, self._name_, v_repr(self._value_))

    def __str__(self):
        cls_name = self.__class__.__name__
        if self._name_ is None:
            return "%s(%r)" % (cls_name, self._value_)
        return "%s.%s" % (cls_name, self._name_)

    def __bool__(self):
        return bool(self._value_)

    def _get_value(self, flag):
        if isinstance(flag, self.__class__):
            return flag._value_
        if self._member_type_ is not object and isinstance(flag, self._member_type_):
            return flag
        return NotImplemented

    def __or__(self, other):
        v = self._get_value(other)
        if v is NotImplemented:
            return NotImplemented
        return self.__class__(self._value_ | v)

    def __and__(self, other):
        v = self._get_value(other)
        if v is NotImplemented:
            return NotImplemented
        return self.__class__(self._value_ & v)

    def __xor__(self, other):
        v = self._get_value(other)
        if v is NotImplemented:
            return NotImplemented
        return self.__class__(self._value_ ^ v)

    def __invert__(self):
        return self.__class__(self._flag_mask_ ^ self._value_)

    __ror__ = __or__
    __rand__ = __and__
    __rxor__ = __xor__


_FLAG.append(Flag)


# CPython's bases are (int, ReprEnum, Flag); ReprEnum adds only the two
# methods the body takes from it, so it is left out of the bases, which keeps
# the class to one compiled base after its builtin.
class IntFlag(int, Flag, boundary=KEEP):
    __str__ = ReprEnum.__str__
    __format__ = ReprEnum.__format__
    __repr__ = Flag.__repr__
    __or__ = __ror__ = Flag.__or__
    __and__ = __rand__ = Flag.__and__
    __xor__ = __rxor__ = Flag.__xor__
    __invert__ = Flag.__invert__
    __bool__ = Flag.__bool__


_BASES.extend((Flag, IntFlag))
property = builtins_property


def unique(enumeration):
    duplicates = []
    for name, m in enumeration.__members__.items():
        if name != m._name_:
            duplicates.append((name, m._name_))
    if duplicates:
        alias_details = ", ".join("%s -> %s" % (alias, name) for (alias, name) in duplicates)
        raise ValueError("duplicate values found in %r: %s" % (enumeration, alias_details))
    return enumeration


class EnumCheck(StrEnum):
    CONTINUOUS = "no skipped integer values"
    NAMED_FLAGS = "multi-flag aliases may not contain unnamed flags"
    UNIQUE = "one name per value"


CONTINUOUS, NAMED_FLAGS, UNIQUE = EnumCheck.CONTINUOUS, EnumCheck.NAMED_FLAGS, EnumCheck.UNIQUE


class verify:
    def __init__(self, *checks):
        self.checks = checks

    def __call__(self, enumeration):
        if UNIQUE in self.checks:
            unique(enumeration)
        return enumeration


def global_enum(cls):
    return cls
