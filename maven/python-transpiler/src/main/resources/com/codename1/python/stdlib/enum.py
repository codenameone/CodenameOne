# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Enumerations: classes whose attributes are a fixed set of named members."""

__all__ = ["EnumType", "EnumMeta", "Enum", "IntEnum", "StrEnum", "Flag", "IntFlag", "ReprEnum", "auto", "unique",
           "member", "nonmember", "global_enum", "verify", "UNIQUE", "CONTINUOUS", "NAMED_FLAGS", "EnumCheck",
           "FlagBoundary", "STRICT", "CONFORM", "EJECT", "KEEP", "property"]

_MIXINS = (int, str, float, bytes)
builtins_property = property
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


class EnumType(type):
    @classmethod
    def __prepare__(metacls, cls, bases, **kwds):
        return {}

    def __new__(metacls, cls, bases, classdict, *, boundary=None, _simple=False, **kwds):
        enum_class = super().__new__(metacls, cls, bases, classdict, **kwds)
        type.__setattr__(enum_class, "_member_names_", [])
        type.__setattr__(enum_class, "_member_map_", {})
        type.__setattr__(enum_class, "_value2member_map_", {})
        type.__setattr__(enum_class, "_unhashable_values_", [])
        mixin = _mixin_of(bases)
        type.__setattr__(enum_class, "_member_type_", mixin if mixin is not None else object)
        flag = Flag is not None and any(isinstance(b, EnumType) and issubclass(b, Flag) for b in bases)
        type.__setattr__(enum_class, "_is_flag_", flag)
        type.__setattr__(enum_class, "_boundary_", boundary)
        if any(isinstance(b, EnumType) and b._member_names_ for b in bases):
            raise TypeError(
                "<enum %r> cannot extend %s" % (cls, ", ".join("<enum %r>" % b.__name__ for b in bases
                                                                if isinstance(b, EnumType) and b._member_names_)))
        last_values = []
        all_bits = 0
        for name, value in list(classdict.items()):
            if isinstance(value, nonmember):
                type.__setattr__(enum_class, name, value.value)
                continue
            if isinstance(value, member):
                value = value.value
            elif _is_dunder(name) or _is_sunder(name) or _is_private(cls, name) or _is_descriptor(value):
                continue
            if name in enum_class._member_map_:
                raise TypeError("%r already defined as %r" % (name, enum_class._member_map_[name]))
            if isinstance(value, auto):
                if value.value is None:
                    value = enum_class._generate_next_value_(name, 1, len(enum_class._member_names_),
                                                             last_values[:])
                else:
                    value = value.value
            elif isinstance(value, tuple) and any(isinstance(v, auto) for v in value):
                value = tuple(enum_class._generate_next_value_(name, 1, len(enum_class._member_names_),
                                                               last_values[:]) if isinstance(v, auto) else v
                              for v in value)
            last_values.append(value)
            m = metacls._make_member(enum_class, name, value, mixin)
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
                if flag and isinstance(m._value_, int):
                    all_bits |= m._value_
            enum_class._member_map_[name] = m
            try:
                enum_class._value2member_map_.setdefault(m._value_, m)
            except TypeError:
                enum_class._unhashable_values_.append(m._value_)
            type.__setattr__(enum_class, name, m)
        if mixin is not None:
            # The mixin type comes first in the MRO, so its __repr__ and
            # __str__ would shadow the enum's: take the nearest enum's own.
            for dunder in ("__repr__", "__str__", "__format__"):
                if dunder in classdict:
                    continue
                for k in enum_class.__mro__:
                    if isinstance(k, EnumType) and dunder in k.__dict__:
                        type.__setattr__(enum_class, dunder, k.__dict__[dunder])
                        break
        type.__setattr__(enum_class, "_flag_mask_", all_bits)
        type.__setattr__(enum_class, "_all_bits_", all_bits)
        return enum_class

    @staticmethod
    def _make_member(enum_class, name, value, mixin):
        args = value if isinstance(value, tuple) else (value,)
        custom_new = enum_class.__dict__.get("_new_member_", None)
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
            m.__init__(*args)
        return m

    def __call__(cls, value, names=None, *values, module=None, qualname=None, type=None, start=1, boundary=None):
        if names is None:
            return cls.__new_member_lookup__(value)
        return cls._create_(value, names, module=module, qualname=qualname, type=type, start=start,
                            boundary=boundary)

    def __new_member_lookup__(cls, value):
        if isinstance(value, cls):
            return value
        try:
            return cls._value2member_map_[value]
        except (KeyError, TypeError):
            pass
        for m in cls._member_map_.values():
            if m._value_ == value:
                return m
        result = cls._missing_(value)
        if isinstance(result, cls):
            return result
        if result is None:
            raise ValueError("%r is not a valid %s" % (value, cls.__qualname__))
        raise TypeError("error in %s._missing_: returned %r instead of None or a valid member"
                        % (cls.__name__, result))

    def _create_(cls, class_name, names, *, module=None, qualname=None, type=None, start=1, boundary=None):
        bases = (cls,) if type is None else (type, cls)
        classdict = {}
        if isinstance(names, str):
            names = names.replace(",", " ").split()
        if isinstance(names, (tuple, list)) and names and isinstance(names[0], str):
            original, names, last = names, [], []
            for count, name in enumerate(original):
                value = cls._generate_next_value_(name, start, count, last[:])
                last.append(value)
                names.append((name, value))
        if isinstance(names, dict):
            names = list(names.items())
        for name, value in names:
            classdict[name] = value
        return EnumType(class_name, bases, classdict, boundary=boundary)

    def __contains__(cls, value):
        if isinstance(value, cls):
            return True
        try:
            if value in cls._value2member_map_:
                return True
        except TypeError:
            pass
        return value in cls._unhashable_values_

    def __delattr__(cls, attr):
        if attr in cls._member_map_:
            raise AttributeError("%r cannot delete member %r." % (cls.__name__, attr))
        super().__delattr__(attr)

    def __getitem__(cls, name):
        return cls._member_map_[name]

    def __iter__(cls):
        return (cls._member_map_[name] for name in cls._member_names_)

    def __reversed__(cls):
        return (cls._member_map_[name] for name in reversed(cls._member_names_))

    def __len__(cls):
        return len(cls._member_names_)

    def __bool__(cls):
        return True

    @builtins_property
    def __members__(cls):
        return dict(cls._member_map_)

    def __repr__(cls):
        if Flag is not None and issubclass(cls, Flag):
            return "<flag %r>" % cls.__name__
        return "<enum %r>" % cls.__name__

    def __setattr__(cls, name, value):
        member_map = cls.__dict__.get("_member_map_", {})
        if name in member_map:
            raise AttributeError("cannot reassign member %r" % (name,))
        super().__setattr__(name, value)


EnumMeta = EnumType
Flag = None


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
    pass


_BASES.extend((ReprEnum, IntEnum))


class StrEnum(str, ReprEnum):
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


class IntFlag(int, ReprEnum, Flag, boundary=KEEP):
    __str__ = ReprEnum.__str__
    __format__ = ReprEnum.__format__
    __repr__ = Flag.__repr__
    __or__ = __ror__ = Flag.__or__
    __and__ = __rand__ = Flag.__and__
    __xor__ = __rxor__ = Flag.__xor__
    __invert__ = Flag.__invert__


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
