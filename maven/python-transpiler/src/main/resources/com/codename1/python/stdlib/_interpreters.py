# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""_interpreters: subinterpreters (PEP 734). A translated program is one
compiled image with no interpreter to start a second copy of, so create()
raises InterpreterError; libraries that offer an interpreter pool import
and fall back to threads."""


class InterpreterError(Exception):
    pass


class InterpreterNotFoundError(InterpreterError):
    pass


class NotShareableError(ValueError):
    pass


def _unavailable():
    raise InterpreterError("subinterpreters are not available in a translated program")


def create(config=None, *, reqrefs=False):
    _unavailable()


def destroy(id, *, restrict=False):
    raise InterpreterNotFoundError(id)


def list_all(*, require_ready=False):
    return []


def get_current():
    return (0, 0)


def get_main():
    return (0, 0)


def set___main___attrs(id, updates, *, restrict=False):
    raise InterpreterNotFoundError(id)


def exec(id, code, shared=None, *, restrict=False):
    raise InterpreterNotFoundError(id)


def call(id, callable, args=None, kwargs=None, *, preserve_exc=False, restrict=False):
    raise InterpreterNotFoundError(id)


def is_shareable(obj):
    return obj is None or isinstance(obj, (bool, int, float, str, bytes))
