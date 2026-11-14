# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/ntpath.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""ntpath: Windows path operations on strings (or bytes). Only the lexical
functions; nothing here touches a file system."""

sep = "\\"
altsep = "/"
extsep = "."
pathsep = ";"
curdir = "."
pardir = ".."
defpath = ".;C:\\bin"
devnull = "nul"

__all__ = ["normcase", "isabs", "join", "splitdrive", "splitroot", "split", "splitext", "basename", "dirname",
           "normpath", "abspath", "commonprefix", "sep", "altsep", "extsep", "pathsep", "curdir", "pardir", "devnull"]


def _seps(p):
    if isinstance(p, bytes):
        return b"\\", b"/", b":", b"", b".", b".."
    return "\\", "/", ":", "", ".", ".."


def normcase(s):
    bs, fs = _seps(s)[:2]
    return s.replace(fs, bs).lower()


def splitroot(p):
    bs, fs, colon, empty = _seps(p)[:4]
    normp = p.replace(fs, bs)
    if normp[:1] == bs:
        if normp[1:2] == bs:
            start = 8 if normp[:8].upper() == (b"\\\\?\\UNC\\" if isinstance(p, bytes) else "\\\\?\\UNC\\") else 2
            index = normp.find(bs, start)
            if index == -1:
                return p, empty, empty
            index2 = normp.find(bs, index + 1)
            if index2 == -1:
                return p, empty, empty
            return p[:index2], p[index2:index2 + 1], p[index2 + 1:]
        return empty, p[:1], p[1:]
    if normp[1:2] == colon:
        if normp[2:3] == bs:
            return p[:2], p[2:3], p[3:]
        return p[:2], empty, p[2:]
    return empty, empty, p


def splitdrive(p):
    drive, root, tail = splitroot(p)
    return drive, root + tail


def isabs(s):
    bs, fs = _seps(s)[:2]
    s = s[:3].replace(fs, bs)
    return s[:2] == bs + bs or s[1:3] == (b":" if isinstance(s, bytes) else ":") + bs


def join(path, *paths):
    bs, fs, colon, empty = _seps(path)[:4]
    seps = bs + fs
    result_drive, result_root, result_path = splitroot(path)
    for p in paths:
        p_drive, p_root, p_path = splitroot(p)
        if p_root:
            if p_drive or not result_drive:
                result_drive = p_drive
            result_root = p_root
            result_path = p_path
            continue
        elif p_drive and p_drive != result_drive:
            if p_drive.lower() != result_drive.lower():
                result_drive = p_drive
                result_root = p_root
                result_path = p_path
                continue
            result_drive = p_drive
        if result_path and result_path[-1:] not in seps:
            result_path = result_path + bs
        result_path = result_path + p_path
    if (result_path and not result_root and result_drive and result_drive[-1:] not in colon + seps):
        return result_drive + bs + result_path
    return result_drive + result_root + result_path


def split(p):
    bs, fs = _seps(p)[:2]
    seps = bs + fs
    d, r, p = splitroot(p)
    i = len(p)
    while i and p[i - 1:i] not in seps:
        i -= 1
    head, tail = p[:i], p[i:]
    return d + r + head.rstrip(seps), tail


def splitext(p):
    bs, fs, colon, empty, dot = _seps(p)[:5]
    sepIndex = max(p.rfind(bs), p.rfind(fs))
    dotIndex = p.rfind(dot)
    if dotIndex > sepIndex:
        filenameIndex = sepIndex + 1
        while filenameIndex < dotIndex:
            if p[filenameIndex:filenameIndex + 1] != dot:
                return p[:dotIndex], p[dotIndex:]
            filenameIndex += 1
    return p, p[:0]


def basename(p):
    return split(p)[1]


def dirname(p):
    return split(p)[0]


def normpath(path):
    bs, fs, colon, empty, curdir_, pardir_ = _seps(path)
    drive, root, path = splitroot(path)
    prefix = drive + root
    comps = path.replace(fs, bs).split(bs)
    i = 0
    while i < len(comps):
        if not comps[i] or comps[i] == curdir_:
            del comps[i]
        elif comps[i] == pardir_:
            if i > 0 and comps[i - 1] != pardir_:
                del comps[i - 1:i + 1]
                i -= 1
            elif i == 0 and root:
                del comps[i]
            else:
                i += 1
        else:
            i += 1
    if not prefix and not comps:
        comps.append(curdir_)
    return prefix.replace(fs, bs) + bs.join(comps)


def abspath(path):
    """The absolute form of a path: an absolute one normalised, a relative
    one joined onto the current directory first -- as CPython's ntpath does
    off Windows, where the current directory is a POSIX path."""
    if not isabs(path):
        import os
        cwd = os.getcwdb() if isinstance(path, bytes) else os.getcwd()
        path = join(cwd, path)
    return normpath(path)


def commonprefix(m):
    if not m:
        return ""
    s1 = min(m)
    s2 = max(m)
    for i, c in enumerate(s1):
        if c != s2[i]:
            return s1[:i]
    return s1
