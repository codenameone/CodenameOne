# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/tempfile.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""tempfile over the application's file system: temporary files live in
a tmp directory under the application's home, which the platform may clear
while the application is not running."""

import os as _os
import secrets as _secrets

TMP_MAX = 10000
template = "tmp"
tempdir = None


def gettempdir():
    global tempdir
    if tempdir is None:
        d = _os.environ.get("TMPDIR") or _os.path.join(_os.path.expanduser("~"), "tmp")
        _os.makedirs(d, exist_ok=True)
        tempdir = d
    return tempdir


def gettempdirb():
    return gettempdir().encode()


def gettempprefix():
    return template


def _name(prefix, suffix, dir):
    if dir is None:
        dir = gettempdir()
    for _ in range(TMP_MAX):
        p = _os.path.join(dir, (prefix if prefix is not None else template) + _secrets.token_hex(4)
                          + (suffix or ""))
        if not _os.path.exists(p):
            return p
    raise FileExistsError("No usable temporary file name found")


def mkstemp(suffix=None, prefix=None, dir=None, text=False):
    """Answers (None, path): there are no file descriptors, so open the path."""
    p = _name(prefix, suffix, dir)
    open(p, "wb").close()
    return (None, _os.path.abspath(p))


def mkdtemp(suffix=None, prefix=None, dir=None):
    p = _name(prefix, suffix, dir)
    _os.mkdir(p)
    return _os.path.abspath(p)


def mktemp(suffix="", prefix=template, dir=None):
    return _name(prefix, suffix, dir)


class _TemporaryFileWrapper:
    def __init__(self, file, name, delete=True):
        self.file = file
        self.name = name
        self.delete = delete

    def __getattr__(self, name):
        return getattr(self.file, name)

    def __iter__(self):
        return iter(self.file)

    def close(self):
        if not self.file.closed:
            self.file.close()
        if self.delete and _os.path.exists(self.name):
            _os.remove(self.name)

    def __enter__(self):
        self.file.__enter__()
        return self

    def __exit__(self, *exc):
        self.close()
        return False


def NamedTemporaryFile(mode="w+b", buffering=-1, encoding=None, newline=None, suffix=None, prefix=None,
                       dir=None, delete=True, *, errors=None, delete_on_close=True):
    p = _name(prefix, suffix, dir)
    open(p, "wb").close()
    f = open(p, mode.replace("x", "w") if "+" in mode or "w" in mode else mode + "+", encoding=encoding,
             newline=newline) if "b" not in mode else open(p, mode)
    return _TemporaryFileWrapper(f, _os.path.abspath(p), delete)


TemporaryFile = NamedTemporaryFile


class SpooledTemporaryFile:
    def __init__(self, max_size=0, mode="w+b", buffering=-1, encoding=None, newline=None, suffix=None,
                 prefix=None, dir=None, *, errors=None):
        import io
        self._file = io.BytesIO() if "b" in mode else io.StringIO()
        self._rolled = False

    def __getattr__(self, name):
        return getattr(self._file, name)

    def rollover(self):
        self._rolled = True

    def __iter__(self):
        return iter(self._file)

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self._file.close()
        return False


class TemporaryDirectory:
    def __init__(self, suffix=None, prefix=None, dir=None, ignore_cleanup_errors=False, *, delete=True):
        self.name = mkdtemp(suffix, prefix, dir)
        self._ignore = ignore_cleanup_errors
        self._delete = delete

    def cleanup(self):
        import shutil
        if _os.path.exists(self.name):
            shutil.rmtree(self.name, ignore_errors=self._ignore)

    def __enter__(self):
        return self.name

    def __exit__(self, *exc):
        if self._delete:
            self.cleanup()
        return False

    def __repr__(self):
        return "<TemporaryDirectory %r>" % (self.name,)
