# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Regular expressions with the syntax and matching rules of Python's re.

A pattern compiles to a small program for a backtracking machine
(`re._engine`). The machine keeps its choice points on an explicit stack
rather than the call stack, so a long subject cannot overflow a device's
thread stack, and undoes group captures through a trail when it backtracks.
"""

from enum import IntFlag as _IntFlag
from re import _parser
from re import _engine
import _cn1re

__all__ = ["match", "fullmatch", "search", "sub", "subn", "split", "findall", "finditer", "compile",
           "purge", "escape", "error", "Pattern", "Match", "A", "I", "L", "M", "S", "X", "U",
           "ASCII", "IGNORECASE", "LOCALE", "MULTILINE", "DOTALL", "VERBOSE", "UNICODE", "NOFLAG",
           "RegexFlag", "PatternError"]


class RegexFlag(_IntFlag):
    NOFLAG = 0
    TEMPLATE = 1
    IGNORECASE = 2
    LOCALE = 4
    MULTILINE = 8
    DOTALL = 16
    UNICODE = 32
    VERBOSE = 64
    DEBUG = 128
    ASCII = 256

    def __repr__(self):
        v = int(self)
        if v == 0:
            return "re.NOFLAG"
        names = []
        for name in ("TEMPLATE", "IGNORECASE", "LOCALE", "MULTILINE", "DOTALL", "UNICODE",
                     "VERBOSE", "DEBUG", "ASCII"):
            bit = int(RegexFlag[name])
            if v & bit:
                names.append("re." + name)
                v &= ~bit
        if v:
            names.append(hex(v))
        return "|".join(names)

    __str__ = __repr__


NOFLAG = RegexFlag.NOFLAG
A = ASCII = RegexFlag.ASCII
I = IGNORECASE = RegexFlag.IGNORECASE
L = LOCALE = RegexFlag.LOCALE
M = MULTILINE = RegexFlag.MULTILINE
S = DOTALL = RegexFlag.DOTALL
U = UNICODE = RegexFlag.UNICODE
X = VERBOSE = RegexFlag.VERBOSE
T = TEMPLATE = RegexFlag.TEMPLATE
DEBUG = RegexFlag.DEBUG

error = _parser.error
PatternError = error



def _sliceable(string):
    """The subject the pieces are cut from: a bytearray or memoryview as
    bytes, since split, sub and findall answer bytes for any buffer, as sre
    does."""
    if isinstance(string, (bytearray, memoryview)):
        return bytes(string)
    return string

class Pattern:
    def __init__(self, pattern, flags, code):
        self.pattern = pattern
        self._code = code
        self.flags = flags
        self.groups = code.groups
        self.groupindex = dict(code.groupindex)
        self._templates = {}
        self._prog = _engine.native(code)

    def __repr__(self):
        p = repr(self.pattern)
        if len(p) > 200:
            p = p[:200]
        f = self.flags & ~(int(UNICODE) if isinstance(self.pattern, str) else 0)
        if f:
            return "re.compile(%s, %s)" % (p, repr(RegexFlag(f)))
        return "re.compile(%s)" % p

    def __eq__(self, other):
        if not isinstance(other, Pattern):
            return NotImplemented
        return self.pattern == other.pattern and self.flags == other.flags

    def __hash__(self):
        return hash((self.pattern, self.flags))

    def _check(self, string):
        if isinstance(self.pattern, str):
            if not isinstance(string, str):
                raise TypeError("cannot use a string pattern on a bytes-like object")
        elif isinstance(string, str):
            raise TypeError("cannot use a bytes pattern on a string-like object")

    # Java builtins on the class, called with the pattern prepended: the
    # checks, the bounds, the run and the Match are one call, as sre's are C.
    match = _cn1re.pattern_match
    fullmatch = _cn1re.pattern_fullmatch
    search = _cn1re.pattern_search

    def finditer(self, string, pos=0, endpos=None):
        prog = self._prog
        forbid = -1
        at = pos if pos > 0 else 0
        while True:
            m = _cn1re.iter(prog, self, string, pos, endpos, at, forbid)
            if m is None:
                return
            yield m
            s, e = m.span()
            # After an empty match the next one may not be empty at the
            # same place; after a non-empty one it may.
            forbid = e if s == e else -1
            at = e

    def findall(self, string, pos=0, endpos=None):
        string = _sliceable(string)
        if isinstance(string, str) and isinstance(self.pattern, str):
            # The loop in Java, without a Match per hit.
            n = len(string)
            if endpos is None or endpos > n:
                endpos = n
            if pos < 0:
                pos = 0
            return _engine.findall(self._code, string, pos, endpos)
        out = []
        for m in self.finditer(string, pos, endpos):
            if self.groups == 0:
                out.append(m.group())
            elif self.groups == 1:
                out.append(m.group(1) if m.group(1) is not None else string[:0])
            else:
                out.append(tuple(g if g is not None else string[:0] for g in m.groups()))
        return out

    def split(self, string, maxsplit=0):
        if isinstance(string, str) and isinstance(self.pattern, str):
            # The loop in Java, without a Match per piece.
            return _cn1re.split(self._prog, string, maxsplit)
        self._check(string)
        string = _sliceable(string)
        out = []
        last = 0
        n = 0
        for m in self.finditer(string):
            if maxsplit and n >= maxsplit:
                break
            s, e = m.span()
            out.append(string[last:s])
            out.extend(m.groups())
            last = e
            n += 1
        out.append(string[last:])
        return out

    # A str template over a str is Java's from end to end (_cn1re.sub), with
    # the template compiled once per pattern as sre caches it; _subn is the
    # general loop for a callable or bytes.
    def subn(self, repl, string, count=0):
        return _cn1re.sub(self._prog, self, repl, string, count, True)

    def sub(self, repl, string, count=0):
        return _cn1re.sub(self._prog, self, repl, string, count, False)

    def _subn(self, repl, string, count):
        if callable(repl):
            fn = repl
        else:
            template = _compile_template(self, repl)
            if len(template) == 1 and isinstance(template[0], (str, bytes)):
                literal = template[0]
                fn = lambda m: literal
            else:
                fn = lambda m: _expand(template, m)
        string = _sliceable(string)
        out = []
        last = 0
        n = 0
        for m in self.finditer(string):
            if count and n >= count:
                break
            s, e = m.span()
            out.append(string[last:s])
            out.append(fn(m))
            last = e
            n += 1
        out.append(string[last:])
        return string[:0].join(out), n

    def scanner(self, string, pos=0, endpos=None):
        raise NotImplementedError("Pattern.scanner is not supported")

    def __copy__(self):
        return self

    def __deepcopy__(self, memo):
        return self


Match = _cn1re.Match


_cache = {}


def _compile(pattern, flags):
    if isinstance(flags, RegexFlag):
        flags = int(flags)
    if isinstance(pattern, Pattern):
        if flags:
            raise ValueError("cannot process flags argument with a compiled pattern")
        return pattern
    if not isinstance(pattern, (str, bytes)):
        raise TypeError("first argument must be string or compiled pattern")
    key = (type(pattern), pattern, flags)
    p = _cache.get(key)
    if p is not None:
        return p
    text = pattern if isinstance(pattern, str) else pattern.decode("latin-1")
    code = _parser.compile(text, flags, isinstance(pattern, str))
    p = Pattern(pattern, code.flags, code)
    if len(_cache) >= 512:
        _cache.clear()
    _cache[key] = p
    return p


def compile(pattern, flags=0):
    return _compile(pattern, flags)


def purge():
    _cache.clear()


def match(pattern, string, flags=0):
    return _compile(pattern, flags).match(string)


def fullmatch(pattern, string, flags=0):
    return _compile(pattern, flags).fullmatch(string)


def search(pattern, string, flags=0):
    return _compile(pattern, flags).search(string)


def sub(pattern, repl, string, count=0, flags=0):
    return _compile(pattern, flags).sub(repl, string, count)


def subn(pattern, repl, string, count=0, flags=0):
    return _compile(pattern, flags).subn(repl, string, count)


def split(pattern, string, maxsplit=0, flags=0):
    return _compile(pattern, flags).split(string, maxsplit)


def findall(pattern, string, flags=0):
    return _compile(pattern, flags).findall(string)


def finditer(pattern, string, flags=0):
    return _compile(pattern, flags).finditer(string)


_SPECIAL = frozenset("()[]{}?*+-|^$\\.&~# \t\n\r\v\f")


def escape(pattern):
    if isinstance(pattern, str):
        return "".join("\\" + c if c in _SPECIAL else c for c in pattern)
    text = pattern.decode("latin-1")
    return "".join("\\" + c if c in _SPECIAL else c for c in text).encode("latin-1")


_TEMPLATE_ESCAPES = {"n": "\n", "t": "\t", "r": "\r", "f": "\f", "v": "\v", "a": "\a", "b": "\b",
                     "\\": "\\"}


def _compile_template(pattern, repl):
    """Splits a replacement into literal text and group numbers."""
    is_bytes = isinstance(repl, bytes)
    text = repl.decode("latin-1") if is_bytes else repl
    parts = []
    lit = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c != "\\":
            lit.append(c)
            i += 1
            continue
        i += 1
        if i >= n:
            raise error("bad escape (end of pattern)", text, i - 1)
        c = text[i]
        if c == "g":
            if i + 1 >= n or text[i + 1] != "<":
                raise error("missing <", text, i + 1)
            close = text.find(">", i + 2)
            if close < 0:
                raise error("missing >, unterminated name", text, i + 2)
            name = text[i + 2:close]
            if not name:
                raise error("missing group name", text, i + 2)
            if name.isdigit():
                idx = int(name)
                if idx > pattern.groups:
                    raise error("invalid group reference %d" % idx, text, i + 2)
            else:
                if not name.isidentifier():
                    raise error("bad character in group name '%s'" % name, text, i + 2)
                if name not in pattern.groupindex:
                    raise IndexError("unknown group name '%s'" % name)
                idx = pattern.groupindex[name]
            parts.append("".join(lit))
            lit = []
            parts.append(idx)
            i = close + 1
        elif c == "0":
            j = i + 1
            v = 0
            while j < n and j < i + 3 and text[j] in "01234567":
                v = v * 8 + int(text[j])
                j += 1
            lit.append(chr(v))
            i = j
        elif c.isdigit():
            j = i
            while j < n and j < i + 2 and text[j].isdigit():
                j += 1
            if j < n and j == i + 2 and text[i] in "01234567" and text[i + 1] in "01234567" \
                    and text[j] in "01234567":
                lit.append(chr(int(text[i:j + 1], 8)))
                i = j + 1
                continue
            idx = int(text[i:j])
            if idx > pattern.groups:
                raise error("invalid group reference %d" % idx, text, i)
            parts.append("".join(lit))
            lit = []
            parts.append(idx)
            i = j
        elif c in _TEMPLATE_ESCAPES:
            lit.append(_TEMPLATE_ESCAPES[c])
            i += 1
        elif c.isascii() and c.isalpha():
            raise error("bad escape \\" + c, text, i - 1)
        else:
            lit.append("\\" + c)
            i += 1
    parts.append("".join(lit))
    if is_bytes:
        return [p.encode("latin-1") if isinstance(p, str) else p for p in parts]
    return parts


def _expand(template, m):
    empty = m.string[:0]
    out = []
    for p in template:
        if isinstance(p, int):
            g = m.group(p)
            if g is not None:
                out.append(g)
        else:
            out.append(p)
    return empty.join(out)


_cn1re.set_hooks(lambda m, template: _expand(_compile_template(m.re, template), m), _compile_template,
                 Pattern._subn)
