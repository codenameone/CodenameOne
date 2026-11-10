# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Manage shelves of pickled objects.

A shelf is a persistent, dictionary-like object whose keys are strings and
whose values are anything `pickle` can write. `open()` returns one for a
file name.

Unlike CPython, whose shelves sit on a `dbm` database, the whole shelf here
is ONE file holding a pickle of a dictionary from key to pickled value. It is
read when the shelf is opened and written back by `sync()` and `close()` (and
when a `with` block ends), never earlier. A file made by CPython's `shelve`
cannot be opened here, and the other way round. Two programs must not use
one file at the same time.

`flag` is 'r' (read only, the file must exist), 'w' (read and write, the
file must exist), 'c' (read and write, created when missing: the default) or
'n' (read and write, always starts empty).
"""

import io
import pickle

__all__ = ["Shelf", "BsdDbShelf", "DbfilenameShelf", "open"]

_open = open


class _ClosedDict:
    def __getattr__(self, name):
        raise ValueError("invalid operation on closed shelf")


class Shelf:
    """A dictionary-like object over a mapping of key to pickled value."""

    def __init__(self, dict, protocol=None, writeback=False, keyencoding="utf-8"):
        self.dict = dict
        if protocol is None:
            protocol = pickle.DEFAULT_PROTOCOL
        self._protocol = protocol
        self.writeback = writeback
        self.cache = {}
        self.keyencoding = keyencoding
        self._closed = False

    def _check(self):
        if self._closed:
            raise ValueError("invalid operation on closed shelf")

    def _key(self, key):
        if not isinstance(key, str):
            raise TypeError("shelf keys must be str, not %s" % type(key).__name__)
        return key

    def keys(self):
        self._check()
        return list(self.dict.keys())

    def __len__(self):
        self._check()
        return len(self.dict)

    def __contains__(self, key):
        self._check()
        return key in self.dict

    def get(self, key, default=None):
        if key in self:
            return self[key]
        return default

    def __getitem__(self, key):
        self._check()
        key = self._key(key)
        if key in self.cache:
            return self.cache[key]
        value = pickle.loads(self.dict[key])
        if self.writeback:
            self.cache[key] = value
        return value

    def __setitem__(self, key, value):
        self._check()
        self._write_guard()
        key = self._key(key)
        if self.writeback:
            self.cache[key] = value
        f = io.BytesIO()
        pickle.Pickler(f, self._protocol).dump(value)
        self.dict[key] = f.getvalue()

    def __delitem__(self, key):
        self._check()
        self._write_guard()
        key = self._key(key)
        del self.dict[key]
        if key in self.cache:
            del self.cache[key]

    def __iter__(self):
        return iter(self.keys())

    def items(self):
        return [(k, self[k]) for k in self.keys()]

    def values(self):
        return [self[k] for k in self.keys()]

    def setdefault(self, key, default=None):
        if key in self:
            return self[key]
        self[key] = default
        return default

    def pop(self, key, *default):
        if len(default) > 1:
            raise TypeError("pop expected at most 2 arguments, got %d" % (1 + len(default)))
        if key in self:
            value = self[key]
            del self[key]
            return value
        if default:
            return default[0]
        raise KeyError(key)

    def popitem(self):
        keys = self.keys()
        if not keys:
            raise KeyError("popitem(): shelf is empty")
        key = keys[-1]
        value = self[key]
        del self[key]
        return (key, value)

    def update(self, other=(), **kwargs):
        if hasattr(other, "keys"):
            for k in other.keys():
                self[k] = other[k]
        else:
            for k, v in other:
                self[k] = v
        for k in kwargs:
            self[k] = kwargs[k]

    def clear(self):
        for k in self.keys():
            del self[k]

    def _write_guard(self):
        pass

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()

    def close(self):
        if self._closed:
            return
        try:
            self.sync()
        finally:
            self._closed = True
            self.cache = {}

    def sync(self):
        """Write cached entries (writeback) and the file out."""
        self._check()
        if self.writeback and self.cache:
            self.writeback = False
            for key in self.cache:
                self[key] = self.cache[key]
            self.writeback = True
            self.cache = {}
        self._flush()

    def _flush(self):
        pass


class BsdDbShelf(Shelf):
    pass


class DbfilenameShelf(Shelf):
    """A shelf kept in the file `filename`."""

    def __init__(self, filename, flag="c", protocol=None, writeback=False):
        if flag not in ("r", "w", "c", "n"):
            raise ValueError("Flag must be one of 'r', 'w', 'c', or 'n'")
        self._filename = filename
        self._readonly = flag == "r"
        data = {}
        exists = True
        if flag != "n":
            try:
                f = _open(filename, "rb")
            except FileNotFoundError:
                exists = False
                f = None
            if f is not None:
                try:
                    raw = f.read()
                finally:
                    f.close()
                if raw:
                    loaded = pickle.loads(raw)
                    if not isinstance(loaded, dict):
                        raise OSError("%s is not a shelf file" % filename)
                    data = loaded
            if not exists and flag != "c":
                raise FileNotFoundError("need 'c' or 'n' flag to open new shelf %r" % (filename,))
        Shelf.__init__(self, data, protocol, writeback)
        self._dirty = flag == "n" or not exists

    def _write_guard(self):
        if self._readonly:
            raise OSError("shelf %r is open read only" % (self._filename,))
        self._dirty = True

    def _flush(self):
        if self._readonly or not self._dirty:
            return
        f = _open(self._filename, "wb")
        try:
            f.write(pickle.dumps(self.dict, 4))
        finally:
            f.close()
        self._dirty = False


def open(filename, flag="c", protocol=None, writeback=False):
    """Open a persistent dictionary for reading and writing."""
    return DbfilenameShelf(filename, flag, protocol, writeback)
