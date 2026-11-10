# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Shell-style wildcard matching: *, ?, [seq] and [!seq]."""

import re
import os

__all__ = ["filter", "filterfalse", "fnmatch", "fnmatchcase", "translate"]

_cache = {}


def _compile(pat):
    r = _cache.get(pat)
    if r is None:
        if isinstance(pat, bytes):
            r = re.compile(translate(pat.decode("latin-1")).encode("latin-1")).match
        else:
            r = re.compile(translate(pat)).match
        if len(_cache) >= 32768:
            _cache.clear()
        _cache[pat] = r
    return r


def fnmatch(name, pat):
    name = os.path.normcase(name)
    pat = os.path.normcase(pat)
    return fnmatchcase(name, pat)


def fnmatchcase(name, pat):
    return _compile(pat)(name) is not None


def filter(names, pat):
    m = _compile(os.path.normcase(pat))
    return [n for n in names if m(os.path.normcase(n))]


def filterfalse(names, pat):
    m = _compile(os.path.normcase(pat))
    return [n for n in names if not m(os.path.normcase(n))]


def translate(pat):
    return "(?s:" + _translate(pat) + ")\\z"


def _translate(pat):
    res = []
    i = 0
    n = len(pat)
    while i < n:
        c = pat[i]
        i += 1
        if c == "*":
            if not res or res[-1] != ".*":
                res.append(".*")
        elif c == "?":
            res.append(".")
        elif c == "[":
            j = i
            if j < n and pat[j] == "!":
                j += 1
            if j < n and pat[j] == "]":
                j += 1
            while j < n and pat[j] != "]":
                j += 1
            if j >= n:
                res.append("\\[")
            else:
                stuff = pat[i:j]
                if "-" not in stuff:
                    stuff = stuff.replace("\\", "\\\\")
                else:
                    chunks = []
                    k = i + 2 if pat[i] == "!" else i + 1
                    while True:
                        k = pat.find("-", k, j)
                        if k < 0:
                            break
                        chunks.append(pat[i:k])
                        i = k + 1
                        k = k + 3
                    chunk = pat[i:j]
                    if chunk:
                        chunks.append(chunk)
                    else:
                        chunks[-1] += "-"
                    for k in range(len(chunks) - 1, 0, -1):
                        if chunks[k - 1][-1] > chunks[k][0]:
                            chunks[k - 1] = chunks[k - 1][:-1] + chunks[k][1:]
                            del chunks[k]
                    stuff = "-".join(s.replace("\\", "\\\\").replace("-", "\\-") for s in chunks)
                stuff = re.sub(r"([&~|])", r"\\\1", stuff)
                i = j + 1
                if not stuff:
                    res.append("(?!)")
                elif stuff == "!":
                    res.append(".")
                else:
                    if stuff[0] == "!":
                        stuff = "^" + stuff[1:]
                    elif stuff[0] in ("^", "["):
                        stuff = "\\" + stuff
                    res.append("[" + stuff + "]")
        else:
            res.append(re.escape(c))
    return "".join(res)
