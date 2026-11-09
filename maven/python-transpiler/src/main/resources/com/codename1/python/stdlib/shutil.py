# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/shutil.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""shutil over the application's file system: copying, moving and removing
files and trees. which() finds nothing, since a device runs no programs."""

import os as _os
import collections as _collections


class Error(OSError):
    pass


class SameFileError(Error):
    pass


class SpecialFileError(OSError):
    pass


class ReadError(OSError):
    pass


class RegistryError(Exception):
    pass


COPY_BUFSIZE = 64 * 1024


def copyfileobj(fsrc, fdst, length=0):
    length = length or COPY_BUFSIZE
    while True:
        buf = fsrc.read(length)
        if not buf:
            break
        fdst.write(buf)


def copyfile(src, dst, *, follow_symlinks=True):
    src, dst = _os.fspath(src), _os.fspath(dst)
    if _os.path.abspath(src) == _os.path.abspath(dst):
        raise SameFileError("%r and %r are the same file" % (src, dst))
    with open(src, "rb") as f, open(dst, "wb") as g:
        copyfileobj(f, g)
    return dst


def copymode(src, dst, *, follow_symlinks=True):
    pass


def copystat(src, dst, *, follow_symlinks=True):
    pass


def copy(src, dst, *, follow_symlinks=True):
    dst = _os.fspath(dst)
    if _os.path.isdir(dst):
        dst = _os.path.join(dst, _os.path.basename(_os.fspath(src)))
    return copyfile(src, dst)


copy2 = copy


def ignore_patterns(*patterns):
    import fnmatch

    def _ignore(path, names):
        out = set()
        for p in patterns:
            out.update(fnmatch.filter(names, p))
        return out
    return _ignore


def copytree(src, dst, symlinks=False, ignore=None, copy_function=copy2, ignore_dangling_symlinks=False,
             dirs_exist_ok=False):
    src, dst = _os.fspath(src), _os.fspath(dst)
    names = _os.listdir(src)
    skip = ignore(src, names) if ignore is not None else set()
    _os.makedirs(dst, exist_ok=dirs_exist_ok)
    for n in names:
        if n in skip:
            continue
        s, d = _os.path.join(src, n), _os.path.join(dst, n)
        if _os.path.isdir(s):
            copytree(s, d, symlinks, ignore, copy_function, ignore_dangling_symlinks, dirs_exist_ok)
        else:
            copy_function(s, d)
    return dst


def rmtree(path, ignore_errors=False, onerror=None, *, onexc=None, dir_fd=None):
    path = _os.fspath(path)
    try:
        for n in _os.listdir(path):
            p = _os.path.join(path, n)
            if _os.path.isdir(p):
                rmtree(p, ignore_errors, onerror, onexc=onexc)
            else:
                _os.remove(p)
        _os.rmdir(path)
    except OSError as e:
        if ignore_errors:
            return
        if onexc is not None:
            onexc(rmtree, path, e)
        elif onerror is not None:
            onerror(rmtree, path, (type(e), e, e.__traceback__))
        else:
            raise


def move(src, dst, copy_function=copy2):
    src, dst = _os.fspath(src), _os.fspath(dst)
    if _os.path.isdir(dst):
        dst = _os.path.join(dst, _os.path.basename(src.rstrip("/")))
    try:
        _os.rename(src, dst)
    except OSError:
        if _os.path.isdir(src):
            copytree(src, dst, copy_function=copy_function)
            rmtree(src)
        else:
            copy_function(src, dst)
            _os.remove(src)
    return dst


_ntuple_diskusage = _collections.namedtuple("usage", "total used free")


def disk_usage(path):
    return _ntuple_diskusage(0, 0, 0)


def which(cmd, mode=None, path=None):
    return None


def get_terminal_size(fallback=(80, 24)):
    try:
        cols = int(_os.environ["COLUMNS"])
        lines = int(_os.environ["LINES"])
        return _os.terminal_size((cols, lines))
    except (KeyError, ValueError, AttributeError):
        pass
    try:
        return _os.get_terminal_size()
    except OSError:
        return _os.terminal_size(fallback)


def chown(path, user=None, group=None):
    pass


def make_archive(base_name, format, root_dir=None, base_dir=None, verbose=0, dry_run=0, owner=None,
                 group=None, logger=None):
    if format != "zip":
        raise ValueError("unknown archive format '%s'" % (format,))
    import zipfile
    root_dir = root_dir or _os.getcwd()
    out = base_name + ".zip"
    with zipfile.ZipFile(out, "w") as z:
        for d, ds, fs in _os.walk(_os.path.join(root_dir, base_dir or "")):
            for f in fs:
                full = _os.path.join(d, f)
                z.write(full, _os.path.relpath(full, root_dir))
    return out


def unpack_archive(filename, extract_dir=None, format=None, *, filter=None):
    import zipfile
    with zipfile.ZipFile(filename) as z:
        z.extractall(extract_dir or _os.getcwd())
