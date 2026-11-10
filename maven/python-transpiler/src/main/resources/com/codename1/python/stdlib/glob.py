# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Path names matching a shell pattern."""

import os
import fnmatch

__all__ = ["glob", "iglob", "escape", "has_magic"]


def has_magic(s):
    return any(c in s for c in "*?[")


def escape(pathname):
    drive, rest = "", pathname
    out = []
    for c in rest:
        out.append("[" + c + "]" if c in "*?[" else c)
    return drive + "".join(out)


def glob(pathname, *, root_dir=None, dir_fd=None, recursive=False, include_hidden=False):
    return list(iglob(pathname, root_dir=root_dir, recursive=recursive, include_hidden=include_hidden))


def iglob(pathname, *, root_dir=None, dir_fd=None, recursive=False, include_hidden=False):
    absolute = pathname.startswith("/")
    parts = [p for p in pathname.split("/") if p]
    base = "/" if absolute else ""
    yield from _walk(root_dir, base, parts, recursive, include_hidden, pathname.endswith("/"))


def _full(root_dir, p):
    if root_dir is None or p.startswith("/"):
        return p or "."
    return os.path.join(root_dir, p) if p else root_dir


def _walk(root_dir, prefix, parts, recursive, hidden, want_dir):
    if not parts:
        if os.path.exists(_full(root_dir, prefix)):
            if not want_dir or os.path.isdir(_full(root_dir, prefix)):
                yield prefix + ("/" if want_dir and not prefix.endswith("/") else "")
        return
    head = parts[0]
    rest = parts[1:]
    if recursive and head == "**":
        for sub in _rdirs(root_dir, prefix, hidden):
            if rest:
                yield from _walk(root_dir, sub, rest, recursive, hidden, want_dir)
            elif sub != prefix or prefix:
                yield (sub.rstrip("/") if sub not in ("/",) else sub) + ("/" if want_dir and sub else "")
        return
    if not has_magic(head):
        yield from _walk(root_dir, _join(prefix, head), rest, recursive, hidden, want_dir)
        return
    d = _full(root_dir, prefix)
    if not os.path.isdir(d):
        return
    for name in os.listdir(d):
        if name.startswith(".") and not hidden and not head.startswith("."):
            continue
        if fnmatch.fnmatchcase(name, head):
            p = _join(prefix, name)
            if rest and not os.path.isdir(_full(root_dir, p)):
                continue
            yield from _walk(root_dir, p, rest, recursive, hidden, want_dir)


def _join(prefix, name):
    if not prefix:
        return name
    return prefix + name if prefix.endswith("/") else prefix + "/" + name


def _rdirs(root_dir, prefix, hidden):
    yield prefix
    d = _full(root_dir, prefix)
    if not os.path.isdir(d):
        return
    for name in os.listdir(d):
        if name.startswith(".") and not hidden:
            continue
        p = _join(prefix, name)
        if os.path.isdir(_full(root_dir, p)):
            yield from _rdirs(root_dir, p, hidden)
        else:
            yield p
