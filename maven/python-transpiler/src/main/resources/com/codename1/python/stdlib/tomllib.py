# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""A TOML 1.0 parser, written from the specification."""

from datetime import date, datetime, time, timedelta, timezone

__all__ = ("loads", "load", "TOMLDecodeError")

_WS = " \t"
_BARE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
_DIGITS = "0123456789"
_HEX = "0123456789abcdefABCDEF"
_ESCAPES = {
    "b": "\b", "t": "\t", "n": "\n", "f": "\f", "r": "\r",
    '"': '"', "\\": "\\",
}


class TOMLDecodeError(ValueError):
    """An error raised if a document is not valid TOML."""

    def __init__(self, msg=None, doc=None, pos=None):
        if doc is None or pos is None:
            ValueError.__init__(self, msg)
            self.msg = msg
            self.doc = None
            self.pos = None
            self.lineno = None
            self.colno = None
            return
        if pos >= len(doc):
            lineno = doc.count("\n") + 1
            colno = 1 if lineno == 1 else len(doc) - doc.rindex("\n")
            where = "end of document"
        else:
            lineno = doc.count("\n", 0, pos) + 1
            if lineno == 1:
                colno = pos + 1
            else:
                colno = pos - doc.rindex("\n", 0, pos)
            where = "line " + str(lineno) + ", column " + str(colno)
        ValueError.__init__(self, msg + " (at " + where + ")")
        self.msg = msg
        self.doc = doc
        self.pos = pos
        self.lineno = lineno
        self.colno = colno


def load(fp, parse_float=float):
    """Parse TOML from a binary file object."""
    b = fp.read()
    try:
        s = b.decode()
    except AttributeError:
        raise TypeError("File must be opened in binary mode, e.g. use `open('foo.toml', 'rb')`") from None
    return loads(s, parse_float=parse_float)


def loads(s, parse_float=float):
    """Parse TOML from a string."""
    if not isinstance(s, str):
        raise TypeError("Expected str object, not '" + type(s).__name__ + "'")
    src = s.replace("\r\n", "\n")
    return _Parser(src, parse_float).run()


def _err(src, pos, msg):
    return TOMLDecodeError(msg, src, pos)


class _Parser:

    def __init__(self, src, parse_float):
        self.src = src
        self.parse_float = parse_float
        self.out = {}
        self.explicit = set()
        self.dotted = set()
        self.frozen = set()
        self.arrays = set()

    # ---- scanning helpers

    def skip_ws(self, pos):
        src = self.src
        n = len(src)
        while pos < n and src[pos] in _WS:
            pos += 1
        return pos

    def skip_comment(self, pos):
        src = self.src
        pos = self.skip_ws(pos)
        if src[pos:pos + 1] == "#":
            pos += 1
            n = len(src)
            while pos < n and src[pos] != "\n":
                c = src[pos]
                if (c < " " and c != "\t") or c == "\x7f":
                    raise _err(src, pos, "Found invalid character " + repr(c))
                pos += 1
        return pos

    def skip_comments_and_ws(self, pos):
        src = self.src
        n = len(src)
        while True:
            start = pos
            pos = self.skip_ws(pos)
            pos = self.skip_comment(pos)
            if pos < n and src[pos] == "\n":
                pos += 1
            if pos == start:
                return pos

    # ---- document

    def run(self):
        src = self.src
        n = len(src)
        cur_path = ()
        cur = self.out
        pos = 0
        while True:
            pos = self.skip_ws(pos)
            if pos >= n:
                break
            c = src[pos]
            if c == "\n":
                pos += 1
                continue
            if c == "#":
                pos = self.skip_comment(pos)
                if pos < n:
                    pos += 1
                continue
            if c in _BARE or c == '"' or c == "'":
                pos = self.key_value(pos, cur_path, cur)
            elif src.startswith("[[", pos):
                pos, cur_path, cur = self.header(pos + 2, True)
            elif c == "[":
                pos, cur_path, cur = self.header(pos + 1, False)
            else:
                raise _err(src, pos, "Invalid statement")
            pos = self.skip_comment(pos)
            if pos < n:
                if src[pos] != "\n":
                    raise _err(src, pos, "Expected newline or end of document after a statement")
                pos += 1
        return self.out

    def header(self, pos, is_array):
        src = self.src
        pos = self.skip_ws(pos)
        pos, key = self.parse_key(pos)
        kpos = pos
        close = "]]" if is_array else "]"
        if not src.startswith(close, pos):
            if is_array:
                raise _err(src, pos, "Expected ']]' at the end of an array declaration")
            raise _err(src, pos, "Expected ']' at the end of a table declaration")
        pos += len(close)
        path = tuple(key)
        node = self.out
        for i in range(len(path) - 1):
            p = path[:i + 1]
            k = path[i]
            if p in self.frozen:
                raise _err(src, kpos, "Cannot mutate immutable namespace " + repr(p))
            if k in node:
                nxt = node[k]
                if isinstance(nxt, list):
                    if p not in self.arrays:
                        raise _err(src, kpos, "Cannot overwrite a value")
                    nxt = nxt[-1]
                elif not isinstance(nxt, dict):
                    raise _err(src, kpos, "Cannot overwrite a value")
            else:
                nxt = {}
                node[k] = nxt
            node = nxt
        last = path[-1]
        if is_array:
            if last in node:
                if path in self.frozen:
                    raise _err(src, kpos, "Cannot mutate immutable namespace " + repr(path))
                if path not in self.arrays:
                    raise _err(src, kpos, "Cannot overwrite a value")
                d = {}
                node[last].append(d)
                self.reset_below(path)
            else:
                d = {}
                node[last] = [d]
                self.arrays.add(path)
            return pos, path, d
        if last in node:
            existing = node[last]
            if not isinstance(existing, dict):
                raise _err(src, kpos, "Cannot overwrite a value")
            if path in self.frozen or path in self.explicit or path in self.dotted:
                raise _err(src, kpos, "Cannot declare " + repr(path) + " twice")
            d = existing
        else:
            d = {}
            node[last] = d
        self.explicit.add(path)
        return pos, path, d

    def reset_below(self, path):
        n = len(path)
        for s in (self.explicit, self.dotted, self.arrays):
            drop = [p for p in s if len(p) > n and p[:n] == path]
            for p in drop:
                s.discard(p)

    # ---- keys

    def parse_key(self, pos):
        src = self.src
        parts = []
        pos, part = self.key_part(pos)
        parts.append(part)
        pos = self.skip_ws(pos)
        while src[pos:pos + 1] == ".":
            pos = self.skip_ws(pos + 1)
            pos, part = self.key_part(pos)
            parts.append(part)
            pos = self.skip_ws(pos)
        return pos, parts

    def key_part(self, pos):
        src = self.src
        c = src[pos:pos + 1]
        if c != "" and c in _BARE:
            start = pos
            n = len(src)
            while pos < n and src[pos] in _BARE:
                pos += 1
            return pos, src[start:pos]
        if c == "'":
            return self.literal_string(pos)
        if c == '"':
            return self.basic_string(pos)
        raise _err(src, pos, "Invalid initial character for a key part")

    def key_value(self, pos, base, table):
        src = self.src
        pos, key = self.parse_key(pos)
        if src[pos:pos + 1] != "=":
            raise _err(src, pos, "Expected '=' after a key in a key/value pair")
        pos = self.skip_ws(pos + 1)
        pos, value = self.parse_value(pos)
        self.assign(table, base, key, value, pos)
        return pos

    def assign(self, table, base, key, value, pos):
        src = self.src
        node = table
        path = base
        for i in range(len(key) - 1):
            k = key[i]
            path = path + (k,)
            if path in self.frozen:
                raise _err(src, pos, "Cannot mutate immutable namespace " + repr(path))
            if k in node:
                nxt = node[k]
                if not isinstance(nxt, dict) or path in self.explicit:
                    raise _err(src, pos, "Cannot overwrite a value")
            else:
                nxt = {}
                node[k] = nxt
            self.dotted.add(path)
            node = nxt
        last = key[-1]
        full = path + (last,)
        if last in node:
            raise _err(src, pos, "Cannot overwrite a value")
        node[last] = value
        if isinstance(value, dict) or isinstance(value, list):
            self.freeze(full)

    def freeze(self, path):
        self.frozen.add(path)

    # ---- values

    def parse_value(self, pos):
        src = self.src
        c = src[pos:pos + 1]
        if c == '"':
            if src.startswith('"""', pos):
                return self.multiline_basic(pos)
            return self.basic_string(pos)
        if c == "'":
            if src.startswith("'''", pos):
                return self.multiline_literal(pos)
            return self.literal_string(pos)
        if c == "t" and src.startswith("true", pos):
            return pos + 4, True
        if c == "f" and src.startswith("false", pos):
            return pos + 5, False
        if c == "[":
            return self.parse_array(pos)
        if c == "{":
            return self.parse_inline_table(pos)
        return self.parse_scalar(pos)

    def parse_array(self, pos):
        src = self.src
        pos += 1
        items = []
        pos = self.skip_comments_and_ws(pos)
        if src.startswith("]", pos):
            return pos + 1, items
        while True:
            pos, v = self.parse_value(pos)
            items.append(v)
            pos = self.skip_comments_and_ws(pos)
            c = src[pos:pos + 1]
            if c == "]":
                return pos + 1, items
            if c != ",":
                raise _err(src, pos, "Unclosed array")
            pos = self.skip_comments_and_ws(pos + 1)
            if src.startswith("]", pos):
                return pos + 1, items

    def parse_inline_table(self, pos):
        src = self.src
        pos += 1
        table = {}
        sub = _Parser(src, self.parse_float)
        pos = self.skip_ws(pos)
        if src.startswith("}", pos):
            return pos + 1, table
        while True:
            pos, key = self.parse_key(pos)
            if src[pos:pos + 1] != "=":
                raise _err(src, pos, "Expected '=' after a key in a key/value pair")
            pos = self.skip_ws(pos + 1)
            pos, value = self.parse_value(pos)
            sub.assign(table, (), key, value, pos)
            pos = self.skip_ws(pos)
            c = src[pos:pos + 1]
            if c == "}":
                return pos + 1, table
            if c != ",":
                raise _err(src, pos, "Unclosed inline table")
            pos = self.skip_ws(pos + 1)

    # ---- strings

    def literal_string(self, pos):
        src = self.src
        pos += 1
        start = pos
        n = len(src)
        while pos < n:
            c = src[pos]
            if c == "'":
                return pos + 1, src[start:pos]
            if c == "\n" or (c < " " and c != "\t") or c == "\x7f":
                raise _err(src, pos, "Illegal character " + repr(c))
            pos += 1
        raise _err(src, pos, "Expected \"'\"")

    def multiline_literal(self, pos):
        src = self.src
        pos += 3
        if src.startswith("\n", pos):
            pos += 1
        n = len(src)
        start = pos
        while True:
            if pos >= n:
                raise _err(src, pos, "Unterminated string")
            if src.startswith("'''", pos):
                end = pos
                pos += 3
                while src.startswith("'", pos) and pos - end < 5:
                    pos += 1
                extra = pos - end - 3
                if extra > 2:
                    raise _err(src, end, "Unterminated string")
                return pos, src[start:pos - 3]
            c = src[pos]
            if (c < " " and c != "\t" and c != "\n") or c == "\x7f":
                raise _err(src, pos, "Illegal character " + repr(c))
            pos += 1

    def escape(self, pos, multiline):
        """pos is at the backslash; returns (pos, text)."""
        src = self.src
        c = src[pos + 1:pos + 2]
        if c in _ESCAPES and c != "":
            return pos + 2, _ESCAPES[c]
        if multiline and c != "" and (c in _WS or c == "\n"):
            p = pos + 1
            n = len(src)
            while p < n and src[p] in _WS:
                p += 1
            if p < n and src[p] == "\n":
                while p < n and (src[p] in _WS or src[p] == "\n"):
                    p += 1
                return p, ""
            raise _err(src, pos + 2, "Unescaped '\\' in a string")
        if c == "u" or c == "U":
            size = 4 if c == "u" else 8
            h = src[pos + 2:pos + 2 + size]
            ok = len(h) == size
            for ch in h:
                if ch not in _HEX:
                    ok = False
            if not ok:
                raise _err(src, pos + 2, "Invalid hex value")
            code = int(h, 16)
            if code > 0x10FFFF or (0xD800 <= code <= 0xDFFF):
                raise _err(src, pos + 2, "Invalid hex value")
            return pos + 2 + size, chr(code)
        raise _err(src, pos + 2, "Unescaped '\\' in a string")

    def basic_string(self, pos):
        src = self.src
        pos += 1
        n = len(src)
        out = []
        while pos < n:
            c = src[pos]
            if c == '"':
                return pos + 1, "".join(out)
            if c == "\\":
                pos, t = self.escape(pos, False)
                out.append(t)
                continue
            if c == "\n" or (c < " " and c != "\t") or c == "\x7f":
                raise _err(src, pos, "Illegal character " + repr(c))
            out.append(c)
            pos += 1
        raise _err(src, pos, "Unterminated string")

    def multiline_basic(self, pos):
        src = self.src
        pos += 3
        if src.startswith("\n", pos):
            pos += 1
        n = len(src)
        out = []
        while True:
            if pos >= n:
                raise _err(src, pos, "Unterminated string")
            c = src[pos]
            if c == '"' and src.startswith('"""', pos):
                end = pos
                pos += 3
                while src.startswith('"', pos) and pos - end < 5:
                    pos += 1
                extra = pos - end - 3
                if extra > 2:
                    raise _err(src, end, "Unterminated string")
                out.append('"' * extra)
                return pos, "".join(out)
            if c == "\\":
                pos, t = self.escape(pos, True)
                out.append(t)
                continue
            if (c < " " and c != "\t" and c != "\n") or c == "\x7f":
                raise _err(src, pos, "Illegal character " + repr(c))
            out.append(c)
            pos += 1

    # ---- numbers and dates

    def parse_scalar(self, pos):
        src = self.src
        if self.digit_at(pos) and self.digit_at(pos + 1):
            if src[pos + 2:pos + 3] == ":":
                r = self.time_at(pos)
                if r is not None:
                    return r
            elif self.digit_at(pos + 2) and self.digit_at(pos + 3) and src[pos + 4:pos + 5] == "-":
                r = self.parse_datetime(pos)
                if r is not None:
                    return r
        end = self.match_number(pos)
        if end > pos:
            tok = src[pos:end].replace("_", "")
            if tok[:2] in ("0x", "0o", "0b"):
                return end, int(tok[2:], {"x": 16, "o": 8, "b": 2}[tok[1]])
            if "." in tok or "e" in tok or "E" in tok:
                return end, self.parse_float(tok)
            return end, int(tok)
        p = pos
        if src[p:p + 1] in ("+", "-"):
            p += 1
        w = src[p:p + 3]
        if w == "inf" or w == "nan":
            return p + 3, self.parse_float(src[pos:p + 3])
        raise _err(src, pos, "Invalid value")

    def run_digits(self, p, valid):
        """Digits with single underscores between; returns the end or p."""
        src = self.src
        n = len(src)
        q = p
        if q < n and src[q] in valid:
            q += 1
            while q < n:
                if src[q] in valid:
                    q += 1
                elif src[q] == "_" and q + 1 < n and src[q + 1] in valid:
                    q += 2
                else:
                    break
            return q
        return p

    def match_number(self, pos):
        src = self.src
        if src.startswith("0x", pos) or src.startswith("0o", pos) or src.startswith("0b", pos):
            valid = {"x": _HEX, "o": "01234567", "b": "01"}[src[pos + 1]]
            q = self.run_digits(pos + 2, valid)
            if q > pos + 2:
                return q
        p = pos
        if src[p:p + 1] in ("+", "-"):
            p += 1
        c = src[p:p + 1]
        if c == "0":
            p += 1
        elif c != "" and c in "123456789":
            p = self.run_digits(p, _DIGITS)
        else:
            return pos
        if src[p:p + 1] == ".":
            q = self.run_digits(p + 1, _DIGITS)
            if q > p + 1:
                p = q
        if src[p:p + 1] == "e" or src[p:p + 1] == "E":
            q = p + 1
            if src[q:q + 1] in ("+", "-"):
                q += 1
            r = self.run_digits(q, _DIGITS)
            if r > q:
                p = r
        return p

    def digit_at(self, pos):
        c = self.src[pos:pos + 1]
        return c != "" and c in _DIGITS

    def digits(self, pos, count):
        s = self.src[pos:pos + count]
        if len(s) != count:
            return None
        for ch in s:
            if ch not in _DIGITS:
                return None
        return int(s)

    def time_at(self, pos):
        src = self.src
        h = self.digits(pos, 2)
        if h is None or src[pos + 2:pos + 3] != ":":
            return None
        m = self.digits(pos + 3, 2)
        if m is None or src[pos + 5:pos + 6] != ":":
            return None
        s = self.digits(pos + 6, 2)
        if s is None:
            return None
        pos += 8
        micro = 0
        if src[pos:pos + 1] == ".":
            q = pos + 1
            while self.digit_at(q):
                q += 1
            frac = src[pos + 1:q]
            if frac == "":
                return None
            micro = int((frac + "000000")[:6])
            pos = q
        if h > 23 or m > 59 or s > 59:
            return None
        return pos, time(h, m, s, micro)

    def parse_datetime(self, pos):
        src = self.src
        start = pos
        y = self.digits(pos, 4)
        mo = self.digits(pos + 5, 2)
        d = self.digits(pos + 8, 2)
        if y is None or mo is None or d is None or src[pos + 7:pos + 8] != "-":
            return None
        if mo < 1 or mo > 12 or d < 1 or d > 31:
            return None
        pos += 10
        sep = src[pos:pos + 1]
        t = None
        if sep == "T" or sep == "t" or sep == " ":
            r = self.time_at(pos + 1)
            if r is not None:
                pos, t = r
        try:
            the_date = date(y, mo, d)
        except ValueError:
            raise _err(src, start, "Invalid date or datetime") from None
        if t is None:
            return pos, the_date
        tz = None
        c = src[pos:pos + 1]
        if c == "Z" or c == "z":
            tz = timezone.utc
            pos += 1
        elif c == "+" or c == "-":
            oh = self.digits(pos + 1, 2)
            om = self.digits(pos + 4, 2)
            if oh is not None and om is not None and src[pos + 3:pos + 4] == ":" and oh < 24 and om < 60:
                delta = timedelta(hours=oh, minutes=om)
                if c == "-":
                    delta = -delta
                tz = timezone(delta)
                pos += 6
        return pos, datetime(y, mo, d, t.hour, t.minute, t.second, t.microsecond, tz)
