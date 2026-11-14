# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""keyword: the reserved words and soft keywords of Python 3.14."""

__all__ = ["iskeyword", "issoftkeyword", "kwlist", "softkwlist"]

kwlist = ["False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
          "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is",
          "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"]

softkwlist = ["_", "case", "match", "type"]

_kw = frozenset(kwlist)
_soft = frozenset(softkwlist)


def iskeyword(s):
    return s in _kw


def issoftkeyword(s):
    return s in _soft
