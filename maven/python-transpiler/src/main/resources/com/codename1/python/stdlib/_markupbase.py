# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/_markupbase.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""_markupbase: the position bookkeeping html.parser.HTMLParser builds on."""


class ParserBase:
    """Line and column tracking for a parser over self.rawdata."""

    def __init__(self):
        if self.__class__ is ParserBase:
            raise RuntimeError("_markupbase.ParserBase must be subclassed")

    def reset(self):
        self.lineno = 1
        self.offset = 0

    def getpos(self):
        return self.lineno, self.offset

    def updatepos(self, i, j):
        if i >= j:
            return j
        rawdata = self.rawdata
        nlines = rawdata.count("\n", i, j)
        if nlines:
            self.lineno = self.lineno + nlines
            pos = rawdata.rindex("\n", i, j)
            self.offset = j - (pos + 1)
        else:
            self.offset = self.offset + j - i
        return j

    _decl_otherchars = ""

    def error(self, message):
        raise NotImplementedError("subclasses of ParserBase must override error()")
