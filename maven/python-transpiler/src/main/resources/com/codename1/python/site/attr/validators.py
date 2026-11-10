# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Validators run on attribute values after assignment."""

import re as _re

from attr.exceptions import NotCallableError

__all__ = ["and_", "deep_iterable", "deep_mapping", "disabled", "ge", "get_disabled", "gt", "in_", "instance_of",
           "is_callable", "le", "lt", "matches_re", "max_len", "min_len", "not_", "optional", "set_disabled"]

_config = {"disabled": False}


def set_disabled(disabled):
    _config["disabled"] = bool(disabled)


def get_disabled():
    return _config["disabled"]


class disabled:
    """Context manager switching validators off inside the block."""

    def __enter__(self):
        self._prev = get_disabled()
        set_disabled(True)
        return self

    def __exit__(self, *exc):
        set_disabled(self._prev)
        return False


class _InstanceOfValidator:
    def __init__(self, type):
        self.type = type

    def __call__(self, inst, attr, value):
        if not isinstance(value, self.type):
            raise TypeError("'%s' must be %r (got %r that is a %r)." % (attr.name, self.type, value, value.__class__),
                            attr, self.type, value)

    def __repr__(self):
        return "<instance_of validator for type %r>" % (self.type,)


def instance_of(type):
    return _InstanceOfValidator(type)


class _OptionalValidator:
    def __init__(self, validator):
        self.validator = validator

    def __call__(self, inst, attr, value):
        if value is None:
            return
        self.validator(inst, attr, value)

    def __repr__(self):
        return "<optional validator for %r or None>" % (self.validator,)


def optional(validator):
    if isinstance(validator, (list, tuple)):
        return _OptionalValidator(_AndValidator(validator))
    return _OptionalValidator(validator)


class _InValidator:
    def __init__(self, options):
        self.options = options

    def __call__(self, inst, attr, value):
        try:
            ok = value in self.options
        except TypeError:
            ok = False
        if not ok:
            raise ValueError("'%s' must be in %r (got %r)" % (attr.name, self.options, value), attr, self.options,
                             value)

    def __repr__(self):
        return "<in_ validator with options %r>" % (self.options,)


def in_(options):
    return _InValidator(options)


class _AndValidator:
    def __init__(self, validators):
        self._validators = tuple(validators)

    def __call__(self, inst, attr, value):
        for v in self._validators:
            v(inst, attr, value)


def and_(*validators):
    vals = []
    for v in validators:
        if isinstance(v, _AndValidator):
            vals.extend(v._validators)
        else:
            vals.append(v)
    return _AndValidator(tuple(vals))


class _IsCallableValidator:
    def __call__(self, inst, attr, value):
        if not callable(value):
            raise NotCallableError("'%s' must be callable (got %r that is a %r)." % (attr.name, value,
                                                                                    value.__class__), value)

    def __repr__(self):
        return "<is_callable validator>"


def is_callable():
    return _IsCallableValidator()


class _DeepIterable:
    def __init__(self, member_validator, iterable_validator):
        self.member_validator = member_validator
        self.iterable_validator = iterable_validator

    def __call__(self, inst, attr, value):
        if self.iterable_validator is not None:
            self.iterable_validator(inst, attr, value)
        for member in value:
            self.member_validator(inst, attr, member)


def deep_iterable(member_validator, iterable_validator=None):
    if isinstance(member_validator, (list, tuple)):
        member_validator = and_(*member_validator)
    return _DeepIterable(member_validator, iterable_validator)


class _DeepMapping:
    def __init__(self, key_validator, value_validator, mapping_validator):
        self.key_validator = key_validator
        self.value_validator = value_validator
        self.mapping_validator = mapping_validator

    def __call__(self, inst, attr, value):
        if self.mapping_validator is not None:
            self.mapping_validator(inst, attr, value)
        for key in value:
            if self.key_validator is not None:
                self.key_validator(inst, attr, key)
            if self.value_validator is not None:
                self.value_validator(inst, attr, value[key])


def deep_mapping(key_validator=None, value_validator=None, mapping_validator=None):
    return _DeepMapping(key_validator, value_validator, mapping_validator)


class _MatchesRe:
    def __init__(self, pattern, func):
        self.pattern = pattern
        self.func = func

    def __call__(self, inst, attr, value):
        if not self.func(value):
            raise ValueError("'%s' must match regex %r (%r doesn't)" % (attr.name, self.pattern.pattern,
                                                                              value), attr, self.pattern, value)


def matches_re(regex, flags=0, func=None):
    if isinstance(regex, str):
        regex = _re.compile(regex, flags)
    if func is None:
        func = regex.match
    return _MatchesRe(regex, func)


class _Bound:
    def __init__(self, bound, op, text):
        self.bound = bound
        self.op = op
        self.text = text

    def __call__(self, inst, attr, value):
        if not self.op(value, self.bound):
            raise ValueError("'%s' must be %s %s: %s" % (attr.name, self.text, self.bound, value))

    def __repr__(self):
        return "<Validator for x %s %s>" % (self.text, self.bound)


def lt(val):
    return _Bound(val, lambda a, b: a < b, "<")


def le(val):
    return _Bound(val, lambda a, b: a <= b, "<=")


def ge(val):
    return _Bound(val, lambda a, b: a >= b, ">=")


def gt(val):
    return _Bound(val, lambda a, b: a > b, ">")


class _MaxLen:
    def __init__(self, n):
        self.n = n

    def __call__(self, inst, attr, value):
        if len(value) > self.n:
            raise ValueError("Length of '%s' must be <= %d: %d" % (attr.name, self.n, len(value)))


class _MinLen:
    def __init__(self, n):
        self.n = n

    def __call__(self, inst, attr, value):
        if len(value) < self.n:
            raise ValueError("Length of '%s' must be >= %d: %d" % (attr.name, self.n, len(value)))


def max_len(length):
    return _MaxLen(length)


def min_len(length):
    return _MinLen(length)


class _Not:
    def __init__(self, validator, msg, exc_types):
        self.validator = validator
        self.msg = msg
        self.exc_types = exc_types

    def __call__(self, inst, attr, value):
        try:
            self.validator(inst, attr, value)
        except self.exc_types:
            return
        msg = self.msg
        if msg is None:
            msg = "not_ validator child '%r' did not raise a captured error" % (self.validator,)
        raise ValueError(msg, attr, self.validator, value, self.exc_types)


def not_(validator, *, msg=None, exc_types=(ValueError, TypeError)):
    if isinstance(exc_types, type):
        exc_types = (exc_types,)
    return _Not(validator, msg, tuple(exc_types))
