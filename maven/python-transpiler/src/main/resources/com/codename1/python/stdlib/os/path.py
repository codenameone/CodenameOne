# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""POSIX path names, over the application's file system."""

import _cn1fs
import io as _io

sep = "/"
altsep = None
extsep = "."
pathsep = ":"
curdir = "."
pardir = ".."
defpath = "/bin:/usr/bin"
devnull = "/dev/null"
supports_unicode_filenames = False

__all__ = ["join", "split", "splitext", "basename", "dirname", "exists", "lexists", "isfile",
           "isdir", "islink", "ismount", "isabs", "abspath", "normpath", "realpath", "relpath",
           "getsize", "getmtime", "getatime", "getctime", "expanduser", "expandvars",
           "commonprefix", "commonpath", "splitdrive", "splitroot", "normcase", "samefile",
           "sep", "altsep", "extsep", "pathsep", "curdir", "pardir", "defpath", "devnull"]


def _s(p):
    return _io._fspath(p)


def _bytes_or(p, f):
    if isinstance(p, bytes):
        return f(p.decode("utf-8")).encode("utf-8")
    return f(_s(p))


def normcase(s):
    return _s(s)


def isabs(s):
    return _s(s).startswith("/")


def join(a, *p):
    path = _s(a)
    for b in p:
        b = _s(b)
        if b.startswith("/"):
            path = b
        elif not path or path.endswith("/"):
            path += b
        else:
            path += "/" + b
    return path


def split(p):
    p = _s(p)
    i = p.rfind("/") + 1
    head, tail = p[:i], p[i:]
    if head and head != "/" * len(head):
        head = head.rstrip("/")
    return head, tail


def splitext(p):
    p = _s(p)
    sep_i = p.rfind("/")
    dot = p.rfind(".")
    if dot > sep_i:
        f = sep_i + 1
        while f < dot:
            if p[f] != ".":
                return p[:dot], p[dot:]
            f += 1
    return p, p[:0]


def splitdrive(p):
    p = _s(p)
    return p[:0], p


def splitroot(p):
    p = _s(p)
    if p[:1] != "/":
        return "", "", p
    if p[1:2] != "/" or p[2:3] == "/":
        return "", "/", p[1:]
    return "", p[:2], p[2:]


def basename(p):
    p = _s(p)
    return p[p.rfind("/") + 1:]


def dirname(p):
    p = _s(p)
    i = p.rfind("/") + 1
    head = p[:i]
    if head and head != "/" * len(head):
        head = head.rstrip("/")
    return head


def normpath(path):
    path = _s(path)
    if path == "":
        return "."
    initial = 0
    if path.startswith("/"):
        initial = 2 if path.startswith("//") and not path.startswith("///") else 1
    comps = path.split("/")
    new = []
    for comp in comps:
        if comp in ("", "."):
            continue
        if comp != ".." or (not initial and not new) or (new and new[-1] == ".."):
            new.append(comp)
        elif new:
            new.pop()
    path = "/" * initial + "/".join(new)
    return path or "."


def abspath(path):
    path = _s(path)
    if not isabs(path):
        path = join(_io._getcwd(), path)
    return normpath(path)


realpath = abspath


def relpath(path, start=None):
    path = _s(path)
    if not path:
        raise ValueError("no path specified")
    start = curdir if start is None else _s(start)
    start_list = [x for x in abspath(start).split("/") if x]
    path_list = [x for x in abspath(path).split("/") if x]
    i = len(commonprefix([start_list, path_list]))
    rel = [pardir] * (len(start_list) - i) + path_list[i:]
    if not rel:
        return curdir
    return join(*rel)


def commonprefix(m):
    if not m:
        return ""
    s1 = min(m)
    s2 = max(m)
    for i, c in enumerate(s1):
        if c != s2[i]:
            return s1[:i]
    return s1


def commonpath(paths):
    paths = [_s(p) for p in paths]
    if not paths:
        raise ValueError("commonpath() arg is an empty sequence")
    abss = set(p.startswith("/") for p in paths)
    if len(abss) != 1:
        raise ValueError("Can't mix absolute and relative paths")
    split_paths = [[c for c in p.split("/") if c and c != "."] for p in paths]
    s1 = min(split_paths)
    s2 = max(split_paths)
    common = s1
    for i, c in enumerate(s1):
        if c != s2[i]:
            common = s1[:i]
            break
    prefix = "/" if paths[0].startswith("/") else ""
    return prefix + "/".join(common)


def exists(path):
    try:
        return _cn1fs.exists(_io._abspath(_s(path)))
    except (OSError, ValueError, TypeError):
        return False


lexists = exists


def isdir(s):
    try:
        return _cn1fs.isdir(_io._abspath(_s(s)))
    except (OSError, ValueError, TypeError):
        return False


def isfile(path):
    try:
        p = _io._abspath(_s(path))
        return _cn1fs.exists(p) and not _cn1fs.isdir(p)
    except (OSError, ValueError, TypeError):
        return False


def islink(path):
    return False


def ismount(path):
    return abspath(path) == "/"


def _need(path):
    p = _io._abspath(_s(path))
    if not _cn1fs.exists(p):
        raise FileNotFoundError(2, "No such file or directory", path)
    return p


def getsize(filename):
    return _cn1fs.size(_need(filename))


def getmtime(filename):
    return _cn1fs.mtime(_need(filename))


getatime = getmtime
getctime = getmtime


def samefile(f1, f2):
    return _need(f1) == _need(f2)


def expanduser(path):
    path = _s(path)
    if path == "~" or path.startswith("~/"):
        return _cn1fs.home() + path[1:]
    return path


def expandvars(path):
    return _s(path)
