# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/selectors.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""selectors: the API shape. There are no raw sockets to wait on (see
socket), so a selector holds registrations and select() reports none ready."""

from collections import namedtuple as _namedtuple

EVENT_READ = 1
EVENT_WRITE = 2

SelectorKey = _namedtuple("SelectorKey", ["fileobj", "fd", "events", "data"])


def _fileobj_to_fd(fileobj):
    if isinstance(fileobj, int):
        return fileobj
    return int(fileobj.fileno())


class BaseSelector:
    def __init__(self):
        self._keys = {}

    def register(self, fileobj, events, data=None):
        if not events or events & ~(EVENT_READ | EVENT_WRITE):
            raise ValueError("Invalid events: %r" % (events,))
        if fileobj in self._keys:
            raise KeyError("%r is already registered" % (fileobj,))
        key = SelectorKey(fileobj, _fileobj_to_fd(fileobj), events, data)
        self._keys[fileobj] = key
        return key

    def unregister(self, fileobj):
        return self._keys.pop(fileobj)

    def modify(self, fileobj, events, data=None):
        self.unregister(fileobj)
        return self.register(fileobj, events, data)

    def select(self, timeout=None):
        return []

    def close(self):
        self._keys.clear()

    def get_key(self, fileobj):
        return self._keys[fileobj]

    def get_map(self):
        return dict(self._keys)

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()


class SelectSelector(BaseSelector):
    pass


PollSelector = SelectSelector
EpollSelector = SelectSelector
KqueueSelector = SelectSelector
DefaultSelector = SelectSelector
