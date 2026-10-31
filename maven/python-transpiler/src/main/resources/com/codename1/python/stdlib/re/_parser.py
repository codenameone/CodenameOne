# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Parses a regular expression and compiles it to a backtracking program."""

from re import _engine as E

F_IGNORECASE = 2
F_LOCALE = 4
F_MULTILINE = 8
F_DOTALL = 16
F_UNICODE = 32
F_VERBOSE = 64
F_ASCII = 256

_FLAG_LETTERS = {"a": F_ASCII, "i": F_IGNORECASE, "L": F_LOCALE, "m": F_MULTILINE, "s": F_DOTALL,
                 "u": F_UNICODE, "x": F_VERBOSE}
_DIGITS = "0123456789"
_OCT = "01234567"
_HEXD = "0123456789abcdefABCDEF"
_REPEAT = "*+?{"
_SIMPLE_ESC = {"a": "\a", "b": "\b", "f": "\f", "n": "\n", "r": "\r", "t": "\t", "v": "\v",
               "\\": "\\"}
_CATS = "dDsSwW"
_MAXREPEAT = 4294967295


class error(Exception):
    def __init__(self, msg, pattern=None, pos=None):
        self.msg = msg
        self.pattern = pattern
        self.pos = pos
        if pattern is not None and pos is not None:
            msg = "%s at position %d" % (msg, pos)
            if isinstance(pattern, str):
                newline = "\n"
            else:
                newline = b"\n"
            self.lineno = pattern.count(newline, 0, pos) + 1
            self.colno = pos - pattern.rfind(newline, 0, pos)
            if newline in pattern:
                msg = "%s (line %d, column %d)" % (msg, self.lineno, self.colno)
        else:
            self.lineno = self.colno = None
        Exception.__init__(self, msg)


error.__module__ = "re"
error.__qualname__ = "PatternError"
error.__name__ = "PatternError"


class Code:
    def __init__(self, prog, groups, groupindex, flags, prefix, anchored):
        self.prog = prog
        self.groups = groups
        self.groupindex = groupindex
        self.flags = flags
        self.prefix = prefix
        self.anchored = anchored


class _State:
    def __init__(self, text, flags, is_str):
        self.text = text
        self.i = 0
        self.flags = flags
        self.is_str = is_str
        self.groups = 0
        self.groupindex = {}
        self.open_groups = set()
        self.lookbehind_groups = None

    def err(self, msg, pos=None):
        return error(msg, self.text, self.i if pos is None else pos)

    def peek(self):
        return self.text[self.i] if self.i < len(self.text) else None

    def skip_ws(self, flags):
        if not flags & F_VERBOSE:
            return
        t = self.text
        while self.i < len(t):
            c = t[self.i]
            if c in " \t\n\r\v\f":
                self.i += 1
            elif c == "#":
                while self.i < len(t) and t[self.i] != "\n":
                    self.i += 1
            else:
                break


def compile(text, flags, is_str):
    if is_str:
        if flags & F_LOCALE:
            raise ValueError("cannot use LOCALE flag with a str pattern")
        if flags & F_ASCII and flags & F_UNICODE:
            raise ValueError("ASCII and UNICODE flags are incompatible")
    elif flags & F_UNICODE:
        raise ValueError("cannot use UNICODE flag with a bytes pattern")
    st = _State(text, flags, is_str)
    st.flags = _leading_flags(st, flags)
    node = _parse_alt(st, st.flags, 0)
    if st.i < len(text):
        if text[st.i] == ")":
            raise st.err("unbalanced parenthesis")
        raise st.err("unexpected end of pattern")
    final = st.flags
    if is_str and not final & F_ASCII:
        final |= F_UNICODE
    unicode = is_str and not final & F_ASCII
    prog = []
    c = _Compiler(prog, unicode)
    c.emit(node)
    prog.append((E.MATCH,))
    code = Code(prog, st.groups, st.groupindex, final, _prefix(node), _anchored(node))
    code.marks = c.marks
    return code


def _leading_flags(st, flags):
    # Global inline flags must open the pattern: "(?i)abc".
    t = st.text
    while t.startswith("(?", st.i):
        j = st.i + 2
        add = 0
        while j < len(t) and t[j] in _FLAG_LETTERS:
            add |= _FLAG_LETTERS[t[j]]
            j += 1
        if j >= len(t) or t[j] != ")" or j == st.i + 2:
            break
        flags |= add
        st.i = j + 1
        st.skip_ws(flags)
    return flags


def _parse_alt(st, flags, depth):
    branches = [_parse_seq(st, flags, depth)]
    while st.peek() == "|":
        st.i += 1
        branches.append(_parse_seq(st, flags, depth))
    if len(branches) == 1:
        return branches[0]
    return ("alt", branches)


def _parse_seq(st, flags, depth):
    items = []
    t = st.text
    while True:
        st.skip_ws(flags)
        if st.i >= len(t):
            break
        c = t[st.i]
        if c == "|" or c == ")":
            break
        start = st.i
        atom = _parse_atom(st, flags, depth)
        if atom is None:
            continue
        st.skip_ws(flags)
        atom = _parse_repeat(st, flags, atom, start)
        items.append(atom)
    if len(items) == 1:
        return items[0]
    return ("seq", items)


def _parse_repeat(st, flags, atom, start):
    t = st.text
    repeated = False
    while st.i < len(t) and t[st.i] in _REPEAT:
        c = t[st.i]
        here = st.i
        if c == "{":
            r = _parse_braces(st)
            if r is None:
                break
            lo, hi = r
        else:
            st.i += 1
            if c == "*":
                lo, hi = 0, None
            elif c == "+":
                lo, hi = 1, None
            else:
                lo, hi = 0, 1
        if repeated:
            raise st.err("multiple repeat", here)
        if atom[0] in ("at", "empty") or atom[0] == "look":
            raise st.err("nothing to repeat", here)
        mode = "greedy"
        if st.i < len(t) and t[st.i] == "?":
            mode = "lazy"
            st.i += 1
        elif st.i < len(t) and t[st.i] == "+":
            mode = "possessive"
            st.i += 1
        atom = ("rep", atom, lo, hi, mode)
        repeated = True
        st.skip_ws(flags)
    return atom


def _parse_braces(st, probe=False):
    t = st.text
    j = st.i + 1
    if j < len(t) and t[j] == "}":
        return None
    a0 = j
    while j < len(t) and t[j] in _DIGITS:
        j += 1
    lo_s = t[a0:j]
    hi_s = lo_s
    if j < len(t) and t[j] == ",":
        j += 1
        b0 = j
        while j < len(t) and t[j] in _DIGITS:
            j += 1
        hi_s = t[b0:j]
    if j >= len(t) or t[j] != "}":
        return None
    if probe:
        return True
    lo = int(lo_s) if lo_s else 0
    hi = int(hi_s) if hi_s else None
    if lo >= _MAXREPEAT or (hi is not None and hi >= _MAXREPEAT):
        raise OverflowError("the repetition number is too large")
    if hi is not None and hi < lo:
        raise st.err("min repeat greater than max repeat", a0)
    st.i = j + 1
    return lo, hi


def _parse_atom(st, flags, depth):
    t = st.text
    c = t[st.i]
    if c == "(":
        return _parse_group(st, flags, depth)
    if c in "*+?":
        raise st.err("nothing to repeat")
    if c == "{" and _parse_braces(st, True):
        raise st.err("nothing to repeat")
    st.i += 1
    if c == ".":
        return ("any", bool(flags & F_DOTALL))
    if c == "^":
        return ("at", "bol" if flags & F_MULTILINE else "begin")
    if c == "$":
        return ("at", "eolm" if flags & F_MULTILINE else "eol")
    if c == "[":
        return _parse_class(st, flags)
    if c == "\\":
        return _parse_escape(st, flags, False)
    return ("lit", c, bool(flags & F_IGNORECASE))


def _parse_group(st, flags, depth):
    t = st.text
    start = st.i
    st.i += 1
    name = None
    capture = True
    local_flags = flags
    if t.startswith("?", st.i):
        st.i += 1
        if st.i >= len(t):
            raise st.err("unexpected end of pattern")
        c = t[st.i]
        if c == ":":
            st.i += 1
            capture = False
        elif c == "P":
            st.i += 1
            if t.startswith("<", st.i):
                st.i += 1
                end = t.find(">", st.i)
                if end < 0:
                    raise st.err("missing >, unterminated name")
                name = t[st.i:end]
                if not name:
                    raise st.err("missing group name")
                if not name.isidentifier():
                    raise st.err("bad character in group name %r" % name)
                if name in st.groupindex:
                    raise st.err("redefinition of group name %r as group %d; was group %d"
                                 % (name, st.groups + 1, st.groupindex[name]))
                st.i = end + 1
            elif t.startswith("=", st.i):
                st.i += 1
                end = t.find(")", st.i)
                if end < 0:
                    raise st.err("missing ), unterminated name")
                ref = t[st.i:end]
                if not ref:
                    raise st.err("missing group name")
                if ref not in st.groupindex:
                    raise st.err("unknown group name %r" % ref)
                idx = st.groupindex[ref]
                if idx in st.open_groups:
                    raise st.err("cannot refer to an open group", start)
                st.i = end + 1
                return ("backref", idx, bool(flags & F_IGNORECASE))
            else:
                raise st.err("unknown extension ?P" + (t[st.i] if st.i < len(t) else ""), start + 1)
        elif c == "#":
            end = t.find(")", st.i)
            if end < 0:
                raise st.err("missing ), unterminated comment", start)
            st.i = end + 1
            return None
        elif c in "=!":
            st.i += 1
            body = _parse_alt(st, flags, depth + 1)
            _close(st, start)
            return ("look", body, False, c == "!", 0)
        elif c == "<":
            st.i += 1
            if st.i < len(t) and t[st.i] in "=!":
                neg = t[st.i] == "!"
                st.i += 1
                body = _parse_alt(st, flags, depth + 1)
                _close(st, start)
                w = _width(body)
                if w[0] != w[1]:
                    raise error("look-behind requires fixed-width pattern")
                return ("look", body, True, neg, w[0])
            raise st.err("unknown extension ?<" + (t[st.i] if st.i < len(t) else ""), start + 1)
        elif c == ">":
            st.i += 1
            body = _parse_alt(st, flags, depth + 1)
            _close(st, start)
            return ("atomic", body)
        elif c == "(":
            st.i += 1
            end = t.find(")", st.i)
            if end < 0:
                raise st.err("missing ), unterminated name")
            ref = t[st.i:end]
            if ref.isdigit():
                idx = int(ref)
                if idx == 0:
                    raise st.err("bad group number", st.i)
            elif ref in st.groupindex:
                idx = st.groupindex[ref]
            elif ref.isidentifier():
                raise st.err("unknown group name %r" % ref, st.i)
            else:
                raise st.err("bad character in group name %r" % ref, st.i)
            st.i = end + 1
            yes = _parse_seq(st, flags, depth + 1)
            no = ("empty",)
            if st.peek() == "|":
                st.i += 1
                no = _parse_seq(st, flags, depth + 1)
                if st.peek() == "|":
                    raise st.err("conditional backref with more than two branches")
            _close(st, start)
            if idx > st.groups:
                raise st.err("invalid group reference %d" % idx, start + 3)
            return ("cond", idx, yes, no)
        elif c in _FLAG_LETTERS or c == "-":
            on = 0
            off = 0
            while st.i < len(t) and t[st.i] in _FLAG_LETTERS:
                on |= _FLAG_LETTERS[t[st.i]]
                st.i += 1
            if st.i < len(t) and t[st.i] == "-":
                st.i += 1
                while st.i < len(t) and t[st.i] in _FLAG_LETTERS:
                    off |= _FLAG_LETTERS[t[st.i]]
                    st.i += 1
            if st.i >= len(t):
                raise st.err("missing -, : or )")
            if t[st.i] == ")":
                raise st.err("global flags not at the start of the expression", start)
            if t[st.i] != ":":
                raise st.err("unknown flag" if t[st.i].isalpha() else "missing -, : or )")
            st.i += 1
            capture = False
            local_flags = (flags | on) & ~off
        else:
            raise st.err("unknown extension ?" + c, start + 1)
    idx = None
    if capture:
        st.groups += 1
        idx = st.groups
        if name is not None:
            st.groupindex[name] = idx
        st.open_groups.add(idx)
    body = _parse_alt(st, local_flags, depth + 1)
    _close(st, start)
    if idx is not None:
        st.open_groups.discard(idx)
        return ("group", idx, body)
    return body if body is not None else ("empty",)


def _close(st, start):
    if st.peek() != ")":
        raise st.err("missing ), unterminated subpattern", start)
    st.i += 1


def _parse_escape(st, flags, in_class):
    t = st.text
    if st.i >= len(t):
        raise st.err("bad escape (end of pattern)", st.i - 1)
    start = st.i - 1
    c = t[st.i]
    st.i += 1
    icase = bool(flags & F_IGNORECASE)
    if c in _SIMPLE_ESC and not (c == "b" and not in_class):
        return ("lit", _SIMPLE_ESC[c], icase)
    if c in _CATS:
        return ("cat", c)
    if not in_class:
        if c == "A":
            return ("at", "begin")
        if c == "Z" or c == "z":
            return ("at", "end")
        if c == "b":
            return ("at", "boundary")
        if c == "B":
            return ("at", "nonboundary")
    if c == "x":
        h = t[st.i:st.i + 2]
        if len(h) < 2 or h[0] not in _HEXD or h[1] not in _HEXD:
            raise st.err("incomplete escape \\x" + h, start)
        st.i += 2
        return ("lit", chr(int(h, 16)), icase)
    if c in "uU" and st.is_str:
        n = 4 if c == "u" else 8
        h = t[st.i:st.i + n]
        if len(h) < n or any(ch not in _HEXD for ch in h):
            raise st.err("incomplete escape \\" + c + h, start)
        st.i += n
        v = int(h, 16)
        if v > 0x10FFFF:
            raise st.err("bad escape \\" + c + h, start)
        return ("lit", chr(v), icase)
    if c == "0":
        v = 0
        n = 0
        while n < 2 and st.i < len(t) and t[st.i] in _OCT:
            v = v * 8 + int(t[st.i])
            st.i += 1
            n += 1
        return ("lit", chr(v), icase)
    if c in _DIGITS:
        j = st.i
        if c in _OCT and j + 1 < len(t) and t[j] in _OCT and t[j + 1] in _OCT:
            v = int(c + t[j:j + 2], 8)
            if v > 0o377:
                raise st.err("octal escape value \\%s outside of range 0-0o377" % (c + t[j:j + 2]), start)
            st.i = j + 2
            return ("lit", chr(v), icase)
        if in_class:
            raise st.err("bad escape \\" + c, start)
        digits = c
        if j < len(t) and t[j] in _DIGITS:
            digits += t[j]
            st.i += 1
        idx = int(digits)
        if idx > st.groups:
            raise st.err("invalid group reference %d" % idx, start + 1)
        if idx in st.open_groups:
            raise st.err("cannot refer to an open group", start)
        return ("backref", idx, icase)
    if c == "N" and st.is_str:
        raise st.err("bad escape \\N", start)
    if c.isascii() and c.isalnum():
        raise st.err("bad escape \\" + c, start)
    return ("lit", c, icase)


def _parse_class(st, flags):
    t = st.text
    start = st.i - 1
    neg = False
    if st.i < len(t) and t[st.i] == "^":
        neg = True
        st.i += 1
    items = []
    first = True
    while True:
        if st.i >= len(t):
            raise st.err("unterminated character set", start)
        c = t[st.i]
        if c == "]" and not first:
            st.i += 1
            break
        first = False
        here = st.i
        if c == "\\":
            st.i += 1
            lo = _parse_escape(st, flags, True)
        else:
            st.i += 1
            lo = ("lit", c, False)
        if lo[0] == "lit" and st.i + 1 < len(t) and t[st.i] == "-" and t[st.i + 1] != "]":
            st.i += 1
            if t[st.i] == "\\":
                st.i += 1
                hi = _parse_escape(st, flags, True)
            else:
                hi = ("lit", t[st.i], False)
                st.i += 1
            if hi[0] != "lit":
                raise st.err("bad character range %s" % t[here:st.i], here)
            if ord(hi[1]) < ord(lo[1]):
                raise st.err("bad character range %s" % t[here:st.i], here)
            items.append(("range", lo[1], hi[1]))
        elif lo[0] == "lit":
            items.append(("ch", lo[1]))
        else:
            items.append(lo)
    return ("set", items, neg, bool(flags & F_IGNORECASE))


def _width(node):
    """(min, max) characters a node can match; max is None when unbounded."""
    k = node[0]
    if k in ("lit", "any", "set", "cat"):
        return (1, 1)
    if k in ("at", "look", "empty"):
        return (0, 0)
    if k == "group":
        return _width(node[2])
    if k == "atomic":
        return _width(node[1])
    if k == "seq":
        lo = 0
        hi = 0
        for n in node[1]:
            a, b = _width(n)
            lo += a
            hi = None if hi is None or b is None else hi + b
        return (lo, hi)
    if k == "alt":
        ws = [_width(n) for n in node[1]]
        lo = min(w[0] for w in ws)
        hi = None if any(w[1] is None for w in ws) else max(w[1] for w in ws)
        return (lo, hi)
    if k == "rep":
        a, b = _width(node[1])
        hi = None if node[3] is None or b is None else b * node[3]
        return (a * node[2], hi)
    if k == "cond":
        a = _width(node[2])
        b = _width(node[3])
        return (min(a[0], b[0]), None if a[1] is None or b[1] is None else max(a[1], b[1]))
    return (0, None)


def _prefix(node):
    """The literal text every match starts with, for a quick scan."""
    out = []
    items = node[1] if node[0] == "seq" else [node]
    for n in items:
        if n[0] == "lit" and not n[2]:
            out.append(n[1])
        elif n[0] == "group" and n[2][0] == "lit" and not n[2][2]:
            out.append(n[2][1])
            break
        else:
            break
    return "".join(out)


def _anchored(node):
    first = node[1][0] if node[0] == "seq" and node[1] else node
    return first[0] == "at" and first[1] == "begin"


class _Compiler:
    def __init__(self, prog, unicode):
        self.prog = prog
        self.unicode = unicode
        self.marks = 0

    def emit(self, node):
        p = self.prog
        k = node[0]
        if k == "lit":
            if node[2]:
                p.append((E.CHARI, node[1].lower(), node[1].upper()))
            else:
                p.append((E.CHAR, node[1]))
        elif k == "any":
            p.append((E.ANYALL,) if node[1] else (E.ANY,))
        elif k == "set":
            p.append((E.SET,) + _compile_set(node[1], node[2], node[3], self.unicode))
        elif k == "cat":
            p.append((E.SET,) + _compile_set([node], False, False, self.unicode))
        elif k == "at":
            p.append((E.AT, node[1], self.unicode))
        elif k == "empty":
            pass
        elif k == "seq":
            for n in node[1]:
                self.emit(n)
        elif k == "group":
            p.append((E.SAVE, 2 * node[1]))
            self.emit(node[2])
            p.append((E.SAVE, 2 * node[1] + 1))
        elif k == "alt":
            jumps = []
            branches = node[1]
            for i in range(len(branches)):
                if i < len(branches) - 1:
                    split = len(p)
                    p.append(None)
                    self.emit(branches[i])
                    jumps.append(len(p))
                    p.append(None)
                    p[split] = (E.SPLIT, split + 1, len(p))
                else:
                    self.emit(branches[i])
            for j in jumps:
                p[j] = (E.JMP, len(p))
        elif k == "backref":
            p.append((E.BACKREF, node[1], node[2]))
        elif k == "look":
            sub = self._sub(node[1])
            p.append((E.LOOK, sub, node[2], node[3], node[4]))
        elif k == "atomic":
            p.append((E.ATOMIC, self._sub(node[1])))
        elif k == "cond":
            at = len(p)
            p.append(None)
            self.emit(node[2])
            jmp = len(p)
            p.append(None)
            p[at] = (E.GROUPREF_EXISTS, node[1], len(p))
            self.emit(node[3])
            p[jmp] = (E.JMP, len(p))
        elif k == "rep":
            self._rep(node)
        else:
            raise error("internal error: unknown node " + k)

    def _sub(self, node):
        prog = []
        c = _Compiler(prog, self.unicode)
        c.marks = self.marks
        c.emit(node)
        prog.append((E.MATCH,))
        self.marks = c.marks
        return prog

    def _rep(self, node):
        body, lo, hi, mode = node[1], node[2], node[3], node[4]
        if mode == "possessive":
            self.prog.append((E.ATOMIC, self._sub(("rep", body, lo, hi, "greedy"))))
            return
        p = self.prog
        greedy = mode == "greedy"
        single = body[0] in ("lit", "any", "set", "cat")
        if single and greedy and (hi is None or hi - lo > 1):
            # One character at a time: a counted loop with no per-step
            # choice point, backing off one character on failure.
            p.append((E.REPEAT_ONE, self._sub(body)[0], lo, hi))
            return
        if single and not greedy and (hi is None or hi - lo > 1):
            p.append((E.MIN_REPEAT_ONE, self._sub(body)[0], lo, hi))
            return
        for _ in range(lo):
            self.emit(body)
        can_be_empty = _width(body)[0] == 0
        if hi is None:
            loop = len(p)
            p.append(None)
            mark = None
            if can_be_empty:
                mark = self.marks
                self.marks += 1
                p.append((E.MARK, mark))
            self.emit(body)
            progress = None
            if mark is not None:
                progress = len(p)
                p.append(None)
            p.append((E.JMP, loop))
            out = len(p)
            if progress is not None:
                # An iteration that matched nothing ends the loop.
                p[progress] = (E.PROGRESS, mark, out)
            p[loop] = (E.SPLIT, loop + 1, out) if greedy else (E.SPLIT, out, loop + 1)
        else:
            holes = []
            for _ in range(hi - lo):
                at = len(p)
                p.append(None)
                holes.append(at)
                self.emit(body)
            out = len(p)
            for at in holes:
                p[at] = (E.SPLIT, at + 1, out) if greedy else (E.SPLIT, out, at + 1)


def _compile_set(items, neg, icase, unicode):
    chars = set()
    ranges = []
    cats = []
    for it in items:
        if it[0] == "ch":
            chars.add(it[1])
            if icase:
                chars.add(it[1].lower())
                chars.add(it[1].upper())
        elif it[0] == "range":
            ranges.append((ord(it[1]), ord(it[2])))
        elif it[0] == "cat":
            cats.append(it[1])
        elif it[0] == "lit":
            chars.add(it[1])
            if icase:
                chars.add(it[1].lower())
                chars.add(it[1].upper())
    return (frozenset(chars), tuple(ranges), "".join(cats), neg, icase and bool(ranges), unicode)
