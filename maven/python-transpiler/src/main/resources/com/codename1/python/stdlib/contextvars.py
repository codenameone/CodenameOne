# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Context variables.

Each thread has its own current context, as under CPython: a worker thread
that runs a copied context (asyncio.to_thread, anyio's worker threads) must
not change the values the event loop's thread sees. Context.run sets the
variables it holds for the duration of the call and restores the previous
values afterwards, which is what asyncio and the libraries that copy a
context around rely on.
"""

from _thread import get_ident as _get_ident

_MISSING = object()
# The current context's values, by thread. Java thread ids are never
# reused, and threading drops a thread's entry when it ends (_forget).
_currents = {}


def _current():
    me = _get_ident()
    d = _currents.get(me)
    if d is None:
        d = {}
        _currents[me] = d
    return d


def _forget(ident):
    _currents.pop(ident, None)


class Token:
    MISSING = _MISSING

    def __init__(self, var, old_value):
        self.var = var
        self.old_value = old_value
        self._used = False


class ContextVar:
    def __init__(self, name, *, default=_MISSING):
        self.name = name
        self._default = default

    def get(self, *default):
        v = _current().get(self, _MISSING)
        if v is not _MISSING:
            return v
        if default:
            return default[0]
        if self._default is not _MISSING:
            return self._default
        raise LookupError(self)

    def set(self, value):
        cur = _current()
        t = Token(self, cur.get(self, _MISSING))
        cur[self] = value
        return t

    def reset(self, token):
        if token._used:
            raise RuntimeError("Token has already been used once")
        token._used = True
        cur = _current()
        if token.old_value is _MISSING:
            cur.pop(self, None)
        else:
            cur[self] = token.old_value

    def __class_getitem__(cls, item):
        return cls

    def __repr__(self):
        return "<ContextVar name=%r>" % (self.name,)


class Context:
    def __init__(self, values=None):
        self._values = dict(values or {})

    def run(self, fn, *args, **kwargs):
        me = _get_ident()
        saved = _currents.get(me)
        running = dict(self._values)
        _currents[me] = running
        try:
            return fn(*args, **kwargs)
        finally:
            self._values = _currents.get(me, running)
            if saved is None:
                _currents.pop(me, None)
            else:
                _currents[me] = saved

    def copy(self):
        return Context(self._values)

    def get(self, var, default=None):
        return self._values.get(var, default)

    def __getitem__(self, var):
        return self._values[var]

    def __contains__(self, var):
        return var in self._values

    def __iter__(self):
        return iter(self._values)

    def __len__(self):
        return len(self._values)

    def keys(self):
        return self._values.keys()

    def values(self):
        return self._values.values()

    def items(self):
        return self._values.items()


def copy_context():
    return Context(_current())
