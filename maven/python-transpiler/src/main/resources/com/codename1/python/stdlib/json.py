# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""JSON encoding and decoding (RFC 8259), with the json module's interface."""

__all__ = ["dump", "dumps", "load", "loads", "JSONDecoder", "JSONDecodeError", "JSONEncoder"]

_ESC = {'"': '\\"', "\\": "\\\\", "\n": "\\n", "\r": "\\r", "\t": "\\t", "\b": "\\b", "\f": "\\f"}
_HEX = "0123456789abcdef"


def _u4(n):
    return "\\u" + _HEX[(n >> 12) & 15] + _HEX[(n >> 8) & 15] + _HEX[(n >> 4) & 15] + _HEX[n & 15]


def _quote(s, ensure_ascii):
    out = ['"']
    for ch in s:
        e = _ESC.get(ch)
        if e is not None:
            out.append(e)
            continue
        n = ord(ch)
        if n < 0x20:
            out.append(_u4(n))
        elif ensure_ascii and n > 0x7E:
            if n > 0xFFFF:
                n -= 0x10000
                out.append(_u4(0xD800 | (n >> 10)))
                out.append(_u4(0xDC00 | (n & 0x3FF)))
            else:
                out.append(_u4(n))
        else:
            out.append(ch)
    out.append('"')
    return "".join(out)


class JSONDecodeError(ValueError):
    def __init__(self, msg, doc, pos):
        lineno = doc.count("\n", 0, pos) + 1
        colno = pos - doc.rfind("\n", 0, pos)
        ValueError.__init__(self, "%s: line %d column %d (char %d)" % (msg, lineno, colno, pos))
        self.msg = msg
        self.doc = doc
        self.pos = pos
        self.lineno = lineno
        self.colno = colno


class JSONEncoder:
    item_separator = ", "
    key_separator = ": "

    def __init__(self, *, skipkeys=False, ensure_ascii=True, check_circular=True, allow_nan=True,
                 sort_keys=False, indent=None, separators=None, default=None):
        self.skipkeys = skipkeys
        self.ensure_ascii = ensure_ascii
        self.check_circular = check_circular
        self.allow_nan = allow_nan
        self.sort_keys = sort_keys
        if indent is not None and not isinstance(indent, str):
            indent = " " * indent
        self.indent = indent
        if separators is not None:
            self.item_separator, self.key_separator = separators
        elif indent is not None:
            self.item_separator = ","
        if default is not None:
            self.default = default

    def default(self, o):
        raise TypeError("Object of type %s is not JSON serializable" % type(o).__name__)

    def encode(self, o):
        return "".join(self.iterencode(o))

    def iterencode(self, o, _one_shot=False):
        out = []
        self._enc(o, out, 0, {} if self.check_circular else None)
        return out

    def _float(self, f):
        if f != f:
            text = "NaN"
        elif f == float("inf"):
            text = "Infinity"
        elif f == -float("inf"):
            text = "-Infinity"
        else:
            return float.__repr__(f)
        if not self.allow_nan:
            raise ValueError("Out of range float values are not JSON compliant: " + repr(f))
        return text

    def _key(self, k):
        if isinstance(k, str):
            return k
        if isinstance(k, float):
            return self._float(k)
        if k is True:
            return "true"
        if k is False:
            return "false"
        if k is None:
            return "null"
        if isinstance(k, int):
            return int.__repr__(k)
        if self.skipkeys:
            return None
        raise TypeError("keys must be str, int, float, bool or None, not " + type(k).__name__)

    def _nl(self, out, level):
        if self.indent is not None:
            out.append("\n" + self.indent * level)

    def _enc(self, o, out, level, markers):
        if isinstance(o, str):
            out.append(_quote(o, self.ensure_ascii))
        elif o is None:
            out.append("null")
        elif o is True:
            out.append("true")
        elif o is False:
            out.append("false")
        elif isinstance(o, int):
            out.append(int.__repr__(o))
        elif isinstance(o, float):
            out.append(self._float(o))
        elif isinstance(o, (list, tuple)):
            self._mark(o, markers)
            if not o:
                out.append("[]")
            else:
                out.append("[")
                first = True
                for v in o:
                    if not first:
                        out.append(self.item_separator)
                    first = False
                    self._nl(out, level + 1)
                    self._enc(v, out, level + 1, markers)
                self._nl(out, level)
                out.append("]")
            self._unmark(o, markers)
        elif isinstance(o, dict):
            self._mark(o, markers)
            if not o:
                out.append("{}")
            else:
                out.append("{")
                items = list(o.items())
                if self.sort_keys:
                    items.sort(key=lambda kv: kv[0])
                first = True
                for k, v in items:
                    k = self._key(k)
                    if k is None:
                        continue
                    if not first:
                        out.append(self.item_separator)
                    first = False
                    self._nl(out, level + 1)
                    out.append(_quote(k, self.ensure_ascii))
                    out.append(self.key_separator)
                    self._enc(v, out, level + 1, markers)
                self._nl(out, level)
                out.append("}")
            self._unmark(o, markers)
        else:
            self._mark(o, markers)
            self._enc(self.default(o), out, level, markers)
            self._unmark(o, markers)

    def _mark(self, o, markers):
        if markers is not None:
            if id(o) in markers:
                raise ValueError("Circular reference detected")
            markers[id(o)] = o

    def _unmark(self, o, markers):
        if markers is not None:
            del markers[id(o)]


_WS = " \t\n\r"
_UNESC = {'"': '"', "\\": "\\", "/": "/", "b": "\b", "f": "\f", "n": "\n", "r": "\r", "t": "\t"}
_CONST = {"NaN": float("nan"), "Infinity": float("inf"), "-Infinity": -float("inf")}


class JSONDecoder:
    def __init__(self, *, object_hook=None, parse_float=None, parse_int=None, parse_constant=None,
                 strict=True, object_pairs_hook=None):
        self.object_hook = object_hook
        self.parse_float = parse_float or float
        self.parse_int = parse_int or int
        self.parse_constant = parse_constant or _CONST.__getitem__
        self.strict = strict
        self.object_pairs_hook = object_pairs_hook

    def decode(self, s):
        obj, end = self.raw_decode(s, self._ws(s, 0))
        end = self._ws(s, end)
        if end != len(s):
            raise JSONDecodeError("Extra data", s, end)
        return obj

    def raw_decode(self, s, idx=0):
        return self._value(s, idx)

    def _ws(self, s, i):
        n = len(s)
        while i < n and s[i] in _WS:
            i += 1
        return i

    def _value(self, s, i):
        if i >= len(s):
            raise JSONDecodeError("Expecting value", s, i)
        c = s[i]
        if c == '"':
            return self._string(s, i + 1)
        if c == "{":
            return self._object(s, i + 1)
        if c == "[":
            return self._array(s, i + 1)
        if c == "n" and s.startswith("null", i):
            return None, i + 4
        if c == "t" and s.startswith("true", i):
            return True, i + 4
        if c == "f" and s.startswith("false", i):
            return False, i + 5
        if c == "N" and s.startswith("NaN", i):
            return self.parse_constant("NaN"), i + 3
        if c == "I" and s.startswith("Infinity", i):
            return self.parse_constant("Infinity"), i + 8
        if c == "-" and s.startswith("-Infinity", i):
            return self.parse_constant("-Infinity"), i + 9
        return self._number(s, i)

    def _number(self, s, i):
        n = len(s)
        j = i
        if j < n and s[j] == "-":
            j += 1
        if j < n and s[j] == "0":
            j += 1
        elif j < n and "1" <= s[j] <= "9":
            while j < n and "0" <= s[j] <= "9":
                j += 1
        else:
            raise JSONDecodeError("Expecting value", s, i)
        frac = False
        if j + 1 < n and s[j] == "." and "0" <= s[j + 1] <= "9":
            frac = True
            j += 1
            while j < n and "0" <= s[j] <= "9":
                j += 1
        if j < n and s[j] in "eE":
            k = j + 1
            if k < n and s[k] in "+-":
                k += 1
            if k < n and "0" <= s[k] <= "9":
                while k < n and "0" <= s[k] <= "9":
                    k += 1
                frac = True
                j = k
        text = s[i:j]
        return (self.parse_float(text) if frac else self.parse_int(text)), j

    def _string(self, s, i):
        start = i - 1
        out = []
        n = len(s)
        while True:
            j = i
            while j < n and s[j] != '"' and s[j] != "\\":
                if self.strict and ord(s[j]) < 0x20:
                    raise JSONDecodeError("Invalid control character at", s, j)
                j += 1
            out.append(s[i:j])
            if j >= n:
                raise JSONDecodeError("Unterminated string starting at", s, start)
            if s[j] == '"':
                return "".join(out), j + 1
            j += 1
            if j >= n:
                raise JSONDecodeError("Unterminated string starting at", s, start)
            e = s[j]
            if e == "u":
                cp = self._hex4(s, j + 1)
                j += 5
                if 0xD800 <= cp <= 0xDBFF and s.startswith("\\u", j):
                    lo = self._hex4(s, j + 2)
                    if 0xDC00 <= lo <= 0xDFFF:
                        cp = 0x10000 + (((cp - 0xD800) << 10) | (lo - 0xDC00))
                        j += 6
                out.append(chr(cp))
                i = j
            else:
                r = _UNESC.get(e)
                if r is None:
                    raise JSONDecodeError("Invalid \\escape", s, j - 1)
                out.append(r)
                i = j + 1

    def _hex4(self, s, i):
        h = s[i:i + 4]
        if len(h) == 4:
            v = 0
            for ch in h:
                d = _HEX.find(ch.lower())
                if d < 0:
                    break
                v = v * 16 + d
            else:
                return v
        raise JSONDecodeError("Invalid \\uXXXX escape", s, i - 1)

    def _object(self, s, i):
        pairs = []
        i = self._ws(s, i)
        if i < len(s) and s[i] == "}":
            i += 1
        else:
            while True:
                if i >= len(s) or s[i] != '"':
                    raise JSONDecodeError("Expecting property name enclosed in double quotes", s, i)
                key, i = self._string(s, i + 1)
                i = self._ws(s, i)
                if i >= len(s) or s[i] != ":":
                    raise JSONDecodeError("Expecting ':' delimiter", s, i)
                value, i = self._value(s, self._ws(s, i + 1))
                pairs.append((key, value))
                i = self._ws(s, i)
                if i < len(s) and s[i] == ",":
                    comma = i
                    i = self._ws(s, i + 1)
                    if i < len(s) and s[i] == "}":
                        raise JSONDecodeError("Illegal trailing comma before end of object", s, comma)
                    continue
                if i < len(s) and s[i] == "}":
                    i += 1
                    break
                raise JSONDecodeError("Expecting ',' delimiter", s, i)
        if self.object_pairs_hook is not None:
            return self.object_pairs_hook(pairs), i
        d = dict(pairs)
        if self.object_hook is not None:
            d = self.object_hook(d)
        return d, i

    def _array(self, s, i):
        values = []
        i = self._ws(s, i)
        if i < len(s) and s[i] == "]":
            return values, i + 1
        while True:
            v, i = self._value(s, i)
            values.append(v)
            i = self._ws(s, i)
            if i < len(s) and s[i] == ",":
                comma = i
                i = self._ws(s, i + 1)
                if i < len(s) and s[i] == "]":
                    raise JSONDecodeError("Illegal trailing comma before end of array", s, comma)
                continue
            if i < len(s) and s[i] == "]":
                return values, i + 1
            raise JSONDecodeError("Expecting ',' delimiter", s, i)


def dumps(obj, *, skipkeys=False, ensure_ascii=True, check_circular=True, allow_nan=True, cls=None,
          indent=None, separators=None, default=None, sort_keys=False, **kw):
    if cls is None:
        cls = JSONEncoder
    return cls(skipkeys=skipkeys, ensure_ascii=ensure_ascii, check_circular=check_circular,
               allow_nan=allow_nan, indent=indent, separators=separators, default=default,
               sort_keys=sort_keys, **kw).encode(obj)


def dump(obj, fp, **kw):
    fp.write(dumps(obj, **kw))


def loads(s, *, cls=None, **kw):
    if isinstance(s, bytes):
        s = s.decode("utf-8")
    if not isinstance(s, str):
        raise TypeError("the JSON object must be str, bytes or bytearray, not " + type(s).__name__)
    if cls is None:
        cls = JSONDecoder
    return cls(**kw).decode(s)


def load(fp, **kw):
    return loads(fp.read(), **kw)
