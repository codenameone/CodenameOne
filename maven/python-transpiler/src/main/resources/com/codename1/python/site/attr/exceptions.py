# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Exceptions raised by attrs."""


class FrozenError(AttributeError):
    """A frozen instance or attribute was modified."""
    msg = "can't set attribute"

    def __init__(self, *args):
        super(FrozenError, self).__init__(*(args if args else (self.msg,)))


class FrozenInstanceError(FrozenError):
    """An instance of a frozen class was modified."""


class FrozenAttributeError(FrozenError):
    """A frozen attribute was modified."""


class AttrsAttributeNotFoundError(ValueError):
    """An attrs function was asked about an attribute the class lacks."""


class NotAnAttrsClassError(ValueError):
    """A non-attrs class was passed where an attrs class is required."""


class DefaultAlreadySetError(RuntimeError):
    """A default was set twice on one attribute."""


class UnannotatedAttributeError(RuntimeError):
    """An attribute of an auto_attribs class has no annotation."""


class PythonTooOldError(RuntimeError):
    """Kept for API compatibility."""


class NotCallableError(TypeError):
    """A value that must be callable is not."""

    def __init__(self, msg, value):
        super(NotCallableError, self).__init__(msg, value)
        self.msg = msg
        self.value = value

    def __str__(self):
        return str(self.msg)
