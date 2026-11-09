# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""jiter: JSON parsing with a partial mode, for reading a JSON document
while it is still streaming in (the openai SDK's structured outputs).

The PyPI package is a Rust extension; this is the same API in Python. With
partial_mode on, an unfinished array or object is closed where the input
stops; an unfinished string is dropped ('on') or kept ('trailing-strings');
an unfinished number, literal or key is dropped.
"""

import decimal as _decimal

__all__ = ["from_json", "cache_clear", "cache_usage", "LosslessFloat"]
__version__ = "0.12.0"


class LosslessFloat:
    def __init__(self, json_float):
        self._raw = bytes(json_float)
        float(self._raw)

    def as_decimal(self):
        return _decimal.Decimal(self._raw.decode("ascii"))

    def __float__(self):
        return float(self._raw)

    def __bytes__(self):
        return self._raw

    def __str__(self):
        return self._raw.decode("ascii")

    def __repr__(self):
        return "LosslessFloat(%s)" % (self._raw.decode("ascii"),)


class _Incomplete(Exception):
    """The input ended inside the value being parsed."""


_WS = " \t\n\r"
_ESC = {'"': '"', "\\": "\\", "/": "/", "b": "\b", "f": "\f", "n": "\n", "r": "\r", "t": "\t"}


class _Parser:
    def __init__(self, s, allow_inf_nan, partial, trailing, dup, float_mode):
        self.s = s
        self.n = len(s)
        self.i = 0
        self.allow_inf_nan = allow_inf_nan
        self.partial = partial
        self.trailing = trailing
        self.dup = dup
        self.float_mode = float_mode

    def error(self, msg):
        line = self.s.count("\n", 0, self.i) + 1
        col = self.i - (self.s.rfind("\n", 0, self.i) + 1) + 1
        return ValueError("%s at line %d column %d" % (msg, line, col))

    def ws(self):
        s, i, n = self.s, self.i, self.n
        while i < n and s[i] in _WS:
            i += 1
        self.i = i

    def eof(self):
        if self.partial:
            raise _Incomplete()
        raise self.error("EOF while parsing a value")

    def value(self):
        self.ws()
        if self.i >= self.n:
            self.eof()
        c = self.s[self.i]
        if c == "{":
            return self.obj()
        if c == "[":
            return self.arr()
        if c == '"':
            return self.string(False)
        if c == "t":
            return self.literal("true", True)
        if c == "f":
            return self.literal("false", False)
        if c == "n":
            return self.literal("null", None)
        if c == "N":
            return self.special("NaN", float("nan"))
        if c == "I":
            return self.special("Infinity", float("inf"))
        if c == "-" and self.s.startswith("-I", self.i):
            return self.special("-Infinity", float("-inf"))
        if c == "-" or c.isdigit():
            return self.number()
        raise self.error("expected value")

    def literal(self, word, value):
        end = self.i + len(word)
        if self.s[self.i:end] == word:
            self.i = end
            return value
        if end > self.n and word.startswith(self.s[self.i:]):
            self.eof()
        raise self.error("expected ident")

    def special(self, word, value):
        if not self.allow_inf_nan:
            raise self.error("expected value")
        return self.literal(word, value)

    def number(self):
        s, i, n = self.s, self.i, self.n
        start = i
        if s[i] == "-":
            i += 1
        is_float = False
        while i < n and s[i].isdigit():
            i += 1
        if i < n and s[i] == ".":
            is_float = True
            i += 1
            while i < n and s[i].isdigit():
                i += 1
        if i < n and s[i] in "eE":
            is_float = True
            i += 1
            if i < n and s[i] in "+-":
                i += 1
            while i < n and s[i].isdigit():
                i += 1
        text = s[start:i]
        digits = text.lstrip("-")
        if len(digits) > 1 and digits[0] == "0" and digits[1].isdigit():
            self.i = i
            raise self.error("invalid number")
        if not digits or digits[0] == "." or digits.endswith((".", "e", "E", "+", "-")) \
                or (len(digits) > 1 and digits[0] == "0" and digits[1].isdigit()):
            if i >= n and self.partial:
                # Not a number yet; the next chunk may finish it.
                raise _Incomplete()
            self.i = i
            raise self.error("invalid number")
        self.i = i
        if not is_float:
            return int(text)
        if self.float_mode == "decimal":
            return _decimal.Decimal(text)
        if self.float_mode == "lossless-float":
            return LosslessFloat(text.encode("ascii"))
        return float(text)

    def string(self, key):
        s, n = self.s, self.n
        i = self.i + 1
        out = []
        start = i
        while True:
            j = s.find('"', i)
            k = s.find("\\", i, j if j >= 0 else n)
            if k >= 0:
                out.append(s[start:k])
                if k + 1 >= n:
                    return self.unfinished(out, key)
                e = s[k + 1]
                if e == "u":
                    h = s[k + 2:k + 6]
                    if len(h) < 4:
                        return self.unfinished(out, key)
                    cp = int(h, 16)
                    nxt = k + 6
                    if 0xD800 <= cp < 0xDC00 and n - nxt < 6 and "\\u"[:n - nxt] == s[nxt:nxt + 2]:
                        # Half of a surrogate pair: the rest is in the next chunk.
                        return self.unfinished(out, key)
                    if 0xD800 <= cp < 0xDC00 and s.startswith("\\u", nxt):
                        h2 = s[nxt + 2:nxt + 6]
                        if len(h2) < 4:
                            return self.unfinished(out, key)
                        lo = int(h2, 16)
                        if 0xDC00 <= lo < 0xE000:
                            cp = 0x10000 + ((cp - 0xD800) << 10) + (lo - 0xDC00)
                            nxt += 6
                    out.append(chr(cp))
                    i = start = nxt
                elif e in _ESC:
                    out.append(_ESC[e])
                    i = start = k + 2
                else:
                    self.i = k + 1
                    raise self.error("invalid escape")
                continue
            if j < 0:
                out.append(s[start:])
                return self.unfinished(out, key)
            out.append(s[start:j])
            self.i = j + 1
            return "".join(out)

    def unfinished(self, out, key):
        self.i = self.n
        if self.partial and self.trailing and not key:
            return _Trailing("".join(out))
        self.eof()

    def obj(self):
        self.i += 1
        result = {}
        self.ws()
        if self.i >= self.n:
            if self.partial:
                return result
            self.eof()
        if self.s[self.i] == "}":
            self.i += 1
            return result
        while True:
            self.ws()
            if self.i >= self.n:
                return self.close(result)
            if self.s[self.i] != '"':
                raise self.error("key must be a string")
            try:
                k = self.string(True)
                self.ws()
                if self.i >= self.n:
                    raise _Incomplete()
                if self.s[self.i] != ":":
                    raise self.error("expected ':'")
                self.i += 1
                v = self.value()
            except _Incomplete:
                return self.close(result)
            if self.dup and k in result:
                raise self.error('Detected duplicate key "%s"' % (k,))
            if isinstance(v, _Trailing):
                result[k] = v.text
                return self.close(result)
            result[k] = v
            self.ws()
            if self.i >= self.n:
                return self.close(result)
            c = self.s[self.i]
            self.i += 1
            if c == "}":
                return result
            if c != ",":
                self.i -= 1
                raise self.error("expected ',' or '}'")

    def arr(self):
        self.i += 1
        result = []
        self.ws()
        if self.i >= self.n:
            if self.partial:
                return result
            self.eof()
        if self.s[self.i] == "]":
            self.i += 1
            return result
        while True:
            try:
                v = self.value()
            except _Incomplete:
                return self.close(result)
            if isinstance(v, _Trailing):
                result.append(v.text)
                return self.close(result)
            result.append(v)
            self.ws()
            if self.i >= self.n:
                return self.close(result)
            c = self.s[self.i]
            self.i += 1
            if c == "]":
                return result
            if c != ",":
                self.i -= 1
                raise self.error("expected ',' or ']'")

    def close(self, result):
        if not self.partial:
            self.eof()
        self.i = self.n
        return result


class _Trailing:
    def __init__(self, text):
        self.text = text


def from_json(json_data, /, *, allow_inf_nan=True, cache_mode="all", partial_mode=False,
              catch_duplicate_keys=False, float_mode="float"):
    if isinstance(json_data, (bytes, bytearray, memoryview)):
        text = bytes(json_data).decode("utf-8")
    elif isinstance(json_data, str):
        text = json_data
    else:
        raise TypeError("Expected bytes, bytearray or str")
    if partial_mode in (True, "on"):
        partial, trailing = True, False
    elif partial_mode == "trailing-strings":
        partial, trailing = True, True
    elif partial_mode in (False, "off"):
        partial, trailing = False, False
    else:
        raise ValueError("Invalid partial mode, should be `'off'`, `'on'`, `'trailing-strings'` or a `bool`")
    p = _Parser(text, allow_inf_nan, partial, trailing, catch_duplicate_keys, float_mode)
    try:
        v = p.value()
    except _Incomplete:
        raise p.error("EOF while parsing a value")
    if isinstance(v, _Trailing):
        v = v.text
    p.ws()
    if p.i < p.n:
        raise p.error("trailing characters")
    return v


def cache_clear():
    pass


def cache_usage():
    return 0
