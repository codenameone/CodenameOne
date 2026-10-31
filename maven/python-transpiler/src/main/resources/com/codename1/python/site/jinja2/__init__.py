# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Clean-room subset of Jinja2, interpreted (no code generation, no eval).

Supported: ``{{ expr }}`` with filters and tests, ``{% if/elif/else %}``,
``{% for %}`` (tuple targets, ``loop.*``, ``else``, inline ``if``),
``{% set %}``, ``{% extends %}``/``{% block %}`` with ``super()``,
``{% include %}``, ``{% raw %}``, ``{# comments #}`` and ``-`` whitespace
control. Written from Jinja's public template documentation.
"""
import os as _os

from markupsafe import Markup, escape

__all__ = ["Environment", "Template", "DictLoader", "FileSystemLoader", "BaseLoader",
           "select_autoescape", "Markup", "escape", "Undefined", "TemplateNotFound",
           "TemplateSyntaxError", "UndefinedError", "TemplateError"]


class TemplateError(Exception):
    pass


class TemplateNotFound(TemplateError):
    def __init__(self, name):
        TemplateError.__init__(self, name)
        self.name = name


class TemplateSyntaxError(TemplateError):
    pass


class UndefinedError(TemplateError):
    pass


class Undefined:
    def __init__(self, hint=None, obj=None, name=None):
        self._name = name

    def _fail(self):
        raise UndefinedError("%r is undefined" % (self._name,))

    def __getattr__(self, name):
        if name.startswith("__"):
            raise AttributeError(name)
        self._fail()

    def __getitem__(self, key):
        self._fail()

    def __call__(self, *a, **k):
        self._fail()

    def __str__(self):
        return ""

    def __html__(self):
        return ""

    def __bool__(self):
        return False

    def __iter__(self):
        return iter(())

    def __len__(self):
        return 0

    def __eq__(self, other):
        return type(other) is type(self)

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return id(type(self))

    def __repr__(self):
        return "Undefined"


def select_autoescape(enabled_extensions=("html", "htm", "xml"), disabled_extensions=(),
                      default_for_string=True, default=False):
    en = tuple("." + e.lstrip(".") for e in enabled_extensions)
    dis = tuple("." + e.lstrip(".") for e in disabled_extensions)

    def autoescape(name):
        if name is None:
            return default_for_string
        low = name.lower()
        if low.endswith(en):
            return True
        if low.endswith(dis):
            return False
        return default
    return autoescape


class BaseLoader:
    def get_source(self, environment, template):
        raise TemplateNotFound(template)

    def load(self, environment, name, globals=None):
        src, filename, _ = self.get_source(environment, name)
        return environment._from_source(src, name, filename)


class DictLoader(BaseLoader):
    def __init__(self, mapping):
        self.mapping = mapping

    def get_source(self, environment, template):
        if template in self.mapping:
            return self.mapping[template], None, lambda: True
        raise TemplateNotFound(template)


class FileSystemLoader(BaseLoader):
    def __init__(self, searchpath, encoding="utf-8", followlinks=False):
        if isinstance(searchpath, str):
            searchpath = [searchpath]
        self.searchpath = [str(p) for p in searchpath]
        self.encoding = encoding

    def get_source(self, environment, template):
        parts = [p for p in template.split("/") if p and p != "."]
        if ".." in parts:
            raise TemplateNotFound(template)
        for base in self.searchpath:
            fn = _os.path.join(base, *parts)
            if _os.path.isfile(fn):
                with open(fn, "r", encoding=self.encoding) as f:
                    return f.read(), fn, lambda: True
        raise TemplateNotFound(template)


class ChoiceLoader(BaseLoader):
    def __init__(self, loaders):
        self.loaders = loaders

    def get_source(self, environment, template):
        for l in self.loaders:
            try:
                return l.get_source(environment, template)
            except TemplateNotFound:
                pass
        raise TemplateNotFound(template)


# ---- lexer ---------------------------------------------------------------

def _lex(src):
    """Splits a template into ("text", s), ("var", expr), ("block", stmt)."""
    out = []
    i = 0
    n = len(src)
    strip_next = False
    while i < n:
        j = -1
        k = i
        while True:
            k = src.find("{", k)
            if k < 0 or k + 1 >= n:
                k = -1
                break
            if src[k + 1] in "{%#":
                j = k
                break
            k += 1
        text = src[i:] if j < 0 else src[i:j]
        if strip_next:
            text = text.lstrip()
            strip_next = False
        if j < 0:
            if text:
                out.append(["text", text])
            break
        kind = src[j + 1]
        start = j + 2
        if start < n and src[start] == "-":
            text = text.rstrip()
            start += 1
        elif start < n and src[start] == "+":
            start += 1
        if text:
            out.append(["text", text])
        close = {"{": "}}", "%": "%}", "#": "#}"}[kind]
        e = _find_close(src, start, close, kind == "#")
        if e < 0:
            raise TemplateSyntaxError("unexpected end of template")
        inner = src[start:e]
        if inner.endswith("-"):
            inner = inner[:-1]
            strip_next = True
        elif inner.endswith("+"):
            inner = inner[:-1]
        i = e + 2
        if kind == "#":
            continue
        inner = inner.strip()
        if kind == "%" and inner == "raw":
            m = src.find("{%", i)
            while m >= 0:
                q = src.find("%}", m)
                body = src[m + 2:q].strip(" -+\t\r\n")
                if body == "endraw":
                    out.append(["text", src[i:m]])
                    i = q + 2
                    break
                m = src.find("{%", m + 2)
            continue
        out.append(["var" if kind == "{" else "block", inner])
    return out


def _find_close(src, start, close, comment):
    i = start
    q = None
    n = len(src)
    while i < n:
        c = src[i]
        if not comment:
            if q is not None:
                if c == "\\":
                    i += 2
                    continue
                if c == q:
                    q = None
                i += 1
                continue
            if c in "'\"":
                q = c
                i += 1
                continue
        if src.startswith(close, i):
            return i
        i += 1
    return -1


# ---- expressions -----------------------------------------------------------

_OPS2 = ("==", "!=", "<=", ">=", "//", "**")


def _tokenize(s):
    toks = []
    i = 0
    n = len(s)
    while i < n:
        c = s[i]
        if c.isspace():
            i += 1
        elif c.isdigit():
            j = i
            while j < n and (s[j].isdigit() or s[j] == "_"):
                j += 1
            if j + 1 < n and s[j] == "." and s[j + 1].isdigit():
                j += 1
                while j < n and s[j].isdigit():
                    j += 1
                toks.append(("num", float(s[i:j].replace("_", ""))))
            else:
                toks.append(("num", int(s[i:j].replace("_", ""))))
            i = j
        elif c.isalpha() or c == "_":
            j = i
            while j < n and (s[j].isalnum() or s[j] == "_"):
                j += 1
            toks.append(("name", s[i:j]))
            i = j
        elif c in "'\"":
            j = i + 1
            buf = []
            while j < n and s[j] != c:
                if s[j] == "\\" and j + 1 < n:
                    j += 1
                    buf.append({"n": "\n", "t": "\t", "r": "\r"}.get(s[j], s[j]))
                else:
                    buf.append(s[j])
                j += 1
            toks.append(("str", "".join(buf)))
            i = j + 1
        else:
            if s[i:i + 2] in _OPS2:
                toks.append(("op", s[i:i + 2]))
                i += 2
            else:
                toks.append(("op", c))
                i += 1
    toks.append(("end", None))
    return toks


class _P:
    def __init__(self, s):
        self.t = _tokenize(s)
        self.i = 0

    def peek(self, k=0):
        return self.t[self.i + k]

    def next(self):
        tok = self.t[self.i]
        self.i += 1
        return tok

    def at(self, kind, val=None):
        tok = self.t[self.i]
        return tok[0] == kind and (val is None or tok[1] == val)

    def accept(self, kind, val=None):
        if self.at(kind, val):
            return self.next()
        return None

    def expect(self, kind, val=None):
        tok = self.accept(kind, val)
        if tok is None:
            raise TemplateSyntaxError("expected %s %s, got %r" % (kind, val or "", self.peek()[1]))
        return tok

    def done(self):
        return self.at("end")

    # grammar
    def expr(self, with_cond=True):
        e = self.or_()
        if with_cond and self.accept("name", "if"):
            c = self.or_()
            other = None
            if self.accept("name", "else"):
                other = self.expr()
            return ("cond", c, e, other)
        return e

    def or_(self):
        e = self.and_()
        while self.accept("name", "or"):
            e = ("or", e, self.and_())
        return e

    def and_(self):
        e = self.not_()
        while self.accept("name", "and"):
            e = ("and", e, self.not_())
        return e

    def not_(self):
        if self.accept("name", "not"):
            return ("not", self.not_())
        return self.compare()

    def compare(self):
        e = self.concat()
        while True:
            tok = self.peek()
            if tok[0] == "op" and tok[1] in ("==", "!=", "<", ">", "<=", ">="):
                self.next()
                e = ("cmp", tok[1], e, self.concat())
            elif tok == ("name", "in"):
                self.next()
                e = ("cmp", "in", e, self.concat())
            elif tok == ("name", "not") and self.peek(1) == ("name", "in"):
                self.next()
                self.next()
                e = ("cmp", "notin", e, self.concat())
            else:
                return e

    def concat(self):
        e = self.add()
        while self.accept("op", "~"):
            e = ("concat", e, self.add())
        return e

    def add(self):
        e = self.mul()
        while self.at("op", "+") or self.at("op", "-"):
            op = self.next()[1]
            e = ("bin", op, e, self.mul())
        return e

    def mul(self):
        e = self.unary()
        while self.peek()[0] == "op" and self.peek()[1] in ("*", "/", "//", "%"):
            op = self.next()[1]
            e = ("bin", op, e, self.unary())
        return e

    def unary(self):
        if self.accept("op", "-"):
            return ("neg", self.unary())
        if self.accept("op", "+"):
            return self.unary()
        e = self.postfix(self.primary())
        if self.accept("op", "**"):
            e = ("bin", "**", e, self.unary())
        return e

    def args(self):
        args = []
        kw = []
        while not self.accept("op", ")"):
            if self.at("name") and self.peek(1) == ("op", "="):
                k = self.next()[1]
                self.next()
                kw.append((k, self.expr()))
            else:
                args.append(self.expr())
            if not self.accept("op", ","):
                self.expect("op", ")")
                break
        return args, kw

    def postfix(self, e):
        while True:
            if self.accept("op", "."):
                tok = self.next()
                e = ("getattr", e, str(tok[1]))
            elif self.accept("op", "["):
                k = self.expr()
                self.expect("op", "]")
                e = ("getitem", e, k)
            elif self.accept("op", "("):
                a, kw = self.args()
                e = ("call", e, a, kw)
            elif self.accept("op", "|"):
                name = self.expect("name")[1]
                a, kw = [], []
                if self.accept("op", "("):
                    a, kw = self.args()
                e = ("filter", name, e, a, kw)
            elif self.at("name", "is"):
                self.next()
                neg = bool(self.accept("name", "not"))
                name = self.expect("name")[1]
                a = []
                if self.accept("op", "("):
                    a, _ = self.args()
                elif self.peek()[0] in ("num", "str") or (self.at("name") and self.peek()[1] not in
                                                           ("and", "or", "else", "if", "in", "not")):
                    a = [self.primary()]
                e = ("test", name, e, a, neg)
            else:
                return e

    def primary(self):
        tok = self.next()
        k, v = tok
        if k in ("num", "str"):
            if k == "str":
                while self.at("str"):
                    v += self.next()[1]
            return ("const", v)
        if k == "name":
            if v in ("true", "True"):
                return ("const", True)
            if v in ("false", "False"):
                return ("const", False)
            if v in ("none", "None"):
                return ("const", None)
            return ("name", v)
        if k == "op" and v == "(":
            if self.accept("op", ")"):
                return ("tuple", [])
            e = self.expr()
            if self.accept("op", ","):
                items = [e]
                while not self.at("op", ")"):
                    items.append(self.expr())
                    if not self.accept("op", ","):
                        break
                self.expect("op", ")")
                return ("tuple", items)
            self.expect("op", ")")
            return e
        if k == "op" and v == "[":
            items = []
            while not self.accept("op", "]"):
                items.append(self.expr())
                if not self.accept("op", ","):
                    self.expect("op", "]")
                    break
            return ("list", items)
        if k == "op" and v == "{":
            items = []
            while not self.accept("op", "}"):
                key = self.expr()
                self.expect("op", ":")
                items.append((key, self.expr()))
                if not self.accept("op", ","):
                    self.expect("op", "}")
                    break
            return ("dict", items)
        raise TemplateSyntaxError("unexpected %r" % (v,))

    def targets(self):
        names = [self.expect("name")[1]]
        while self.accept("op", ","):
            names.append(self.expect("name")[1])
        return names


def _parse_expr(s):
    p = _P(s)
    e = p.expr()
    if not p.done():
        raise TemplateSyntaxError("unexpected %r" % (p.peek()[1],))
    return e


# ---- statements ------------------------------------------------------------

def _params(p):
    """Macro parameters after the opening parenthesis: names with optional defaults."""
    params = []
    while not p.accept("op", ")"):
        n = p.expect("name")[1]
        d = None
        if p.accept("op", "="):
            d = p.expr()
        params.append((n, d))
        if not p.accept("op", ","):
            p.expect("op", ")")
            break
    return params


def _parse(tokens):
    pos = [0]
    blocks = {}
    meta = {"extends": None}

    def body(ends):
        nodes = []
        while pos[0] < len(tokens):
            kind, val = tokens[pos[0]]
            pos[0] += 1
            if kind == "text":
                nodes.append(("text", val))
            elif kind == "var":
                nodes.append(("out", _parse_expr(val)))
            else:
                word = val.split(None, 1)[0] if val else ""
                if "(" in word:
                    word = word[:word.index("(")]
                rest = val[len(word):].strip()
                if word in ends:
                    return nodes, word, rest
                nodes.append(stmt(word, rest))
        if ends:
            raise TemplateSyntaxError("missing %s" % ends[-1])
        return nodes, None, None

    def stmt(word, rest):
        if word == "if":
            branches = []
            cond = _parse_expr(rest)
            other = []
            while True:
                b, end, r = body(("elif", "else", "endif"))
                branches.append((cond, b))
                if end == "elif":
                    cond = _parse_expr(r)
                    continue
                if end == "else":
                    other, _, _ = body(("endif",))
                break
            return ("if", branches, other)
        if word == "for":
            p = _P(rest)
            targets = p.targets()
            p.expect("name", "in")
            it = p.expr(False)
            filt = None
            if p.accept("name", "if"):
                filt = p.expr(False)
            b, end, _ = body(("else", "endfor"))
            other = []
            if end == "else":
                other, _, _ = body(("endfor",))
            return ("for", targets, it, filt, b, other)
        if word == "set":
            p = _P(rest)
            targets = p.targets()
            if p.accept("op", "="):
                e = p.expr()
                if p.accept("op", ","):
                    items = [e]
                    while not p.done():
                        items.append(p.expr())
                        if not p.accept("op", ","):
                            break
                    e = ("tuple", items)
                return ("set", targets, e)
            filt = None
            if p.accept("op", "|"):
                fp = _P("__cn1_body|" + rest.split("|", 1)[1])
                filt = fp.expr()
            b, _, _ = body(("endset",))
            return ("setblock", targets[0], b, filt)
        if word == "block":
            name = rest.split()[0]
            b, _, _ = body(("endblock",))
            blocks[name] = b
            return ("block", name)
        if word == "extends":
            meta["extends"] = _parse_expr(rest)
            return ("text", "")
        if word == "include":
            ignore = False
            if rest.endswith("ignore missing"):
                ignore = True
                rest = rest[:-len("ignore missing")]
            for suffix in ("with context", "without context"):
                if rest.endswith(suffix):
                    rest = rest[:-len(suffix)]
            return ("include", _parse_expr(rest.strip()), ignore)
        if word == "with":
            p = _P(rest)
            assigns = []
            while not p.done():
                n = p.expect("name")[1]
                p.expect("op", "=")
                assigns.append((n, p.expr()))
                p.accept("op", ",")
            b, _, _ = body(("endwith",))
            return ("with", assigns, b)
        if word == "macro":
            p = _P(rest)
            name = p.expect("name")[1]
            p.expect("op", "(")
            params = _params(p)
            b, _, _ = body(("endmacro",))
            return ("macro", name, params, b)
        if word == "call":
            p = _P(rest)
            params = []
            if p.accept("op", "("):
                params = _params(p)
            call = p.expr()
            if call[0] != "call":
                raise TemplateSyntaxError("expected call")
            b, _, _ = body(("endcall",))
            return ("callblock", params, call, b)
        if word == "filter":
            filt = _P("__cn1_body|" + rest).expr()
            b, _, _ = body(("endfilter",))
            return ("filterblock", filt, b)
        if word == "import":
            p = _P(rest)
            src_e = p.expr(False)
            p.expect("name", "as")
            return ("import", src_e, p.expect("name")[1])
        if word == "from":
            p = _P(rest)
            src_e = p.expr(False)
            p.expect("name", "import")
            names = []
            while p.at("name") and p.peek()[1] not in ("with", "without"):
                n = p.next()[1]
                alias = n
                if p.accept("name", "as"):
                    alias = p.expect("name")[1]
                names.append((n, alias))
                if not p.accept("op", ","):
                    break
            return ("fromimport", src_e, names)
        raise TemplateSyntaxError("Encountered unknown tag %r." % (word,))

    nodes, _, _ = body(())
    return nodes, blocks, meta["extends"]


# ---- runtime ---------------------------------------------------------------

class _TemplateModule:
    def __init__(self, name):
        self._name = name
        self._body = ""

    def __str__(self):
        return self._body

    def __html__(self):
        return Markup(self._body)


class _Macro:
    """A ``{% macro %}`` (or the ``caller`` of a ``{% call %}``) bound to the
    variables of the frame that defined it."""

    def __init__(self, tmpl, name, params, body, fr):
        self._tmpl = tmpl
        self._fr = fr
        self._params = params
        self._body = body
        self.name = name
        self.arguments = tuple(p[0] for p in params)
        self.catch_varargs = _uses(body, "varargs")
        self.catch_kwargs = _uses(body, "kwargs")
        self.caller = _uses(body, "caller")

    def __call__(self, *args, **kwargs):
        fr = self._fr
        vars = dict(fr.vars)
        n = len(self._params)
        caller = kwargs.pop("caller", None)
        for i in range(n):
            pname, dflt = self._params[i]
            if i < len(args):
                if pname in kwargs:
                    raise TypeError("macro %r got multiple values for argument %r"
                                    % (self.name, pname))
                vars[pname] = args[i]
            elif pname in kwargs:
                vars[pname] = kwargs.pop(pname)
            elif dflt is not None:
                vars[pname] = self._tmpl._eval(dflt, _Frame(fr.tmpl, vars, fr.blocks, []))
            else:
                vars[pname] = Undefined(hint="parameter %r was not provided" % (pname,),
                                        name=pname)
        extra = tuple(args[n:])
        if extra and not self.catch_varargs:
            raise TypeError("macro %r takes not more than %d argument(s)" % (self.name, n))
        if kwargs and not self.catch_kwargs:
            raise TypeError("macro %r takes no keyword argument %r"
                            % (self.name, next(iter(kwargs))))
        vars["varargs"] = extra
        vars["kwargs"] = kwargs
        if caller is not None:
            vars["caller"] = caller
        elif self.caller:
            vars["caller"] = Undefined(name="caller")
        out = []
        self._tmpl._exec_list(self._body, _Frame(fr.tmpl, vars, fr.blocks, out))
        s = "".join(out)
        return Markup(s) if self._tmpl.autoescape else s

    def __repr__(self):
        return "<Macro %r>" % (self.name,)


def _uses(node, name):
    """True when ``name`` is read anywhere inside a parsed body."""
    if isinstance(node, tuple):
        if len(node) == 2 and node[0] == "name" and node[1] == name:
            return True
        if node and node[0] == "macro":
            return False
        for x in node:
            if _uses(x, name):
                return True
    elif isinstance(node, list):
        for x in node:
            if _uses(x, name):
                return True
    return False


class Cycler:
    def __init__(self, *items):
        if not items:
            raise RuntimeError("at least one item has to be provided")
        self.items = items
        self.pos = 0

    def reset(self):
        self.pos = 0

    @property
    def current(self):
        return self.items[self.pos]

    def next(self):
        rv = self.items[self.pos]
        self.pos = (self.pos + 1) % len(self.items)
        return rv

    __next__ = next


_NO_CHANGE = object()


class _Loop:
    depth = 1
    depth0 = 0

    def __init__(self, items):
        self._items = items
        self.length = len(items)
        self.index0 = 0
        self._last_changed = _NO_CHANGE

    def changed(self, *value):
        if self._last_changed != value:
            self._last_changed = value
            return True
        return False

    index = property(lambda self: self.index0 + 1)
    revindex = property(lambda self: self.length - self.index0)
    revindex0 = property(lambda self: self.length - self.index0 - 1)
    first = property(lambda self: self.index0 == 0)
    last = property(lambda self: self.index0 == self.length - 1)
    previtem = property(lambda self: self._items[self.index0 - 1] if self.index0 > 0 else Undefined())
    nextitem = property(lambda self: self._items[self.index0 + 1]
                        if self.index0 + 1 < self.length else Undefined())

    def cycle(self, *args):
        return args[self.index0 % len(args)]


def _attr(obj, name):
    if isinstance(obj, Undefined):
        obj._fail()
    try:
        return getattr(obj, name)
    except AttributeError:
        pass
    try:
        return obj[name]
    except (TypeError, LookupError, AttributeError):
        return Undefined(name=name)


def _item(obj, key):
    if isinstance(obj, Undefined):
        obj._fail()
    try:
        return obj[key]
    except (TypeError, LookupError, AttributeError):
        pass
    if isinstance(key, str):
        try:
            return getattr(obj, key)
        except AttributeError:
            pass
    return Undefined(name=key)


def _do_default(v, default="", boolean=False):
    if isinstance(v, Undefined) or (boolean and not v):
        return default
    return v


def _do_join(env_ae, v, d="", attribute=None):
    items = list(v)
    if attribute is not None:
        items = [_attr(x, attribute) for x in items]
    if env_ae:
        return escape(d).join(items)
    return str(d).join(str(x) for x in items)


def _do_title(s):
    out = []
    up = True
    for ch in str(s):
        if ch.isalnum():
            out.append(ch.upper() if up else ch.lower())
            up = False
        else:
            out.append(ch)
            up = ch in " \t\n\r-([{"
    r = "".join(out)
    return Markup(r) if isinstance(s, Markup) else r


def _keep(s, r):
    return Markup(r) if isinstance(s, Markup) else r


_FILTERS = {
    "safe": lambda v: Markup(str(v)),
    "upper": lambda v: _keep(v, str(v).upper()),
    "lower": lambda v: _keep(v, str(v).lower()),
    "capitalize": lambda v: _keep(v, str(v).capitalize()),
    "title": _do_title,
    "trim": lambda v, chars=None: _keep(v, str(v).strip(chars)),
    "length": len,
    "count": len,
    "default": _do_default,
    "d": _do_default,
    "first": lambda v: next(iter(v), Undefined()),
    "last": lambda v: list(v)[-1] if len(list(v)) else Undefined(),
    "reverse": lambda v: v[::-1] if isinstance(v, str) else list(reversed(list(v))),
    "sort": lambda v, reverse=False: sorted(v, reverse=reverse),
    "list": list,
    "string": str,
    "int": lambda v, default=0: _to_int(v, default),
    "float": lambda v, default=0.0: _to_float(v, default),
    "abs": abs,
    "round": lambda v, precision=0: round(v, precision),
    "replace": lambda v, old, new: _keep(v, str(v).replace(old, new)),
    "sum": lambda v, start=0: sum(v, start),
    "min": min,
    "max": max,
    "unique": lambda v: list(dict.fromkeys(v)),
    "striptags": lambda v: Markup(escape(v) if not isinstance(v, Markup) else v).striptags(),
    "items": lambda v: list(v.items()),
    "dictsort": lambda v: sorted(v.items()),
    "wordcount": lambda v: len(str(v).split()),
    "center": lambda v, width=80: str(v).center(width),
    "truncate": lambda v, length=255, killwords=False, end="...": _truncate(str(v), length, killwords, end),
    "format": lambda v, *a, **k: str(v) % (a or k),
    "tojson": lambda v: Markup(_tojson(v)),
}


def _to_int(v, default):
    try:
        return int(v)
    except (TypeError, ValueError):
        try:
            return int(float(v))
        except (TypeError, ValueError):
            return default


def _to_float(v, default):
    try:
        return float(v)
    except (TypeError, ValueError):
        return default


def _truncate(s, length, killwords, end):
    if len(s) <= length + 5:
        return s
    if killwords:
        return s[:length - len(end)] + end
    r = s[:length - len(end)].rsplit(" ", 1)[0]
    return r + end


def _tojson(v):
    import json
    s = json.dumps(v, sort_keys=True)
    return (s.replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026")
            .replace("'", "\\u0027"))


def _test(name, v, args):
    if name == "defined":
        return not isinstance(v, Undefined)
    if name == "undefined":
        return isinstance(v, Undefined)
    if name == "none":
        return v is None
    if name in ("string",):
        return isinstance(v, str)
    if name == "number":
        return isinstance(v, (int, float)) and not isinstance(v, bool)
    if name in ("integer",):
        return isinstance(v, int) and not isinstance(v, bool)
    if name == "boolean":
        return isinstance(v, bool)
    if name == "even":
        return v % 2 == 0
    if name == "odd":
        return v % 2 == 1
    if name == "divisibleby":
        return v % args[0] == 0
    if name == "mapping":
        return isinstance(v, dict)
    if name == "iterable":
        try:
            iter(v)
            return True
        except TypeError:
            return False
    if name in ("sameas",):
        return v is args[0]
    if name in ("eq", "equalto", "=="):
        return v == args[0]
    if name in ("ne", "!="):
        return v != args[0]
    if name in ("lower",):
        return str(v).islower()
    if name in ("upper",):
        return str(v).isupper()
    if name == "in":
        return v in args[0]
    if name == "true":
        return v is True
    if name == "false":
        return v is False
    raise TemplateSyntaxError("No test named %r." % (name,))


class _Frame:
    def __init__(self, tmpl, vars, blocks, out):
        self.tmpl = tmpl
        self.vars = vars
        self.blocks = blocks
        self.out = out


class Template:
    def __init__(self, *args, **kwargs):
        pass

    def __new__(cls, source=None, autoescape=False, **kw):
        if source is None:
            return object.__new__(cls)
        env = Environment(autoescape=autoescape, **kw)
        return env.from_string(source)

    def _setup(self, env, source, name, filename):
        self.environment = env
        self.name = name
        self.filename = filename
        if not env.keep_trailing_newline and source.endswith("\n"):
            source = source[:-1]
            if source.endswith("\r"):
                source = source[:-1]
        self._nodes, self._blocks, self._extends = _parse(_lex(source))
        ae = env.autoescape
        self.autoescape = bool(ae(name)) if callable(ae) else bool(ae)

    def render(self, *args, **kwargs):
        vars = dict(self.environment.globals)
        if args:
            vars.update(dict(args[0]))
        vars.update(kwargs)
        out = []
        self._render_root(vars, {}, out)
        return "".join(out)

    def generate(self, *args, **kwargs):
        yield self.render(*args, **kwargs)

    def stream(self, *args, **kwargs):
        return self.generate(*args, **kwargs)

    def _render_root(self, vars, blocks, out):
        merged = dict(blocks)
        for k, b in self._blocks.items():
            merged.setdefault(k, [])
            merged[k] = merged[k] + [(self, b)]
        if self._extends is not None:
            fr = _Frame(self, vars, merged, [])
            for n in self._nodes:
                if n[0] in ("set", "macro", "import", "fromimport"):
                    self._exec(n, fr)
            parent = self._eval(self._extends, fr)
            if not isinstance(parent, Template):
                parent = self.environment.get_template(parent)
            parent._render_root(vars, merged, out)
            return
        self._exec_list(self._nodes, _Frame(self, vars, merged, out))

    # -- execution
    def _exec_list(self, nodes, fr):
        for n in nodes:
            self._exec(n, fr)

    def _exec(self, n, fr):
        k = n[0]
        if k == "text":
            fr.out.append(n[1])
        elif k == "out":
            fr.out.append(self._finalize(self._eval(n[1], fr)))
        elif k == "if":
            for cond, body in n[1]:
                if self._eval(cond, fr):
                    self._exec_list(body, fr)
                    return
            self._exec_list(n[2], fr)
        elif k == "for":
            self._for(n, fr)
        elif k == "set":
            v = self._eval(n[2], fr)
            self._assign(fr.vars, n[1], v)
        elif k == "setblock":
            sub = _Frame(fr.tmpl, fr.vars, fr.blocks, [])
            self._exec_list(n[2], sub)
            s = "".join(sub.out)
            s = Markup(s) if self.autoescape else s
            if n[3] is not None:
                tmp = dict(fr.vars)
                tmp["__cn1_body"] = s
                s = self._eval(n[3], _Frame(fr.tmpl, tmp, fr.blocks, []))
            fr.vars[n[1]] = s
        elif k == "macro":
            fr.vars[n[1]] = _Macro(self, n[1], n[2], n[3], fr)
        elif k == "callblock":
            call = n[2]
            caller = _Macro(self, "caller", n[1], n[3], fr)
            f = self._eval(call[1], fr)
            args = [self._eval(a, fr) for a in call[2]]
            kw = dict((kk, self._eval(v, fr)) for kk, v in call[3])
            kw["caller"] = caller
            fr.out.append(self._finalize(f(*args, **kw)))
        elif k == "filterblock":
            sub = _Frame(fr.tmpl, fr.vars, fr.blocks, [])
            self._exec_list(n[2], sub)
            s = "".join(sub.out)
            tmp = dict(fr.vars)
            tmp["__cn1_body"] = Markup(s) if self.autoescape else s
            fr.out.append(self._finalize(self._eval(n[1], _Frame(fr.tmpl, tmp, fr.blocks, []))))
        elif k == "import":
            fr.vars[n[2]] = self._module(self._eval(n[1], fr))
        elif k == "fromimport":
            mod = self._module(self._eval(n[1], fr))
            for name, alias in n[2]:
                v = getattr(mod, name, None)
                if v is None or name.startswith("_"):
                    v = Undefined(hint="the template %r does not export the requested name %r"
                                  % (mod._name, name), name=name)
                fr.vars[alias] = v
        elif k == "block":
            self._render_block(n[1], 0, fr)
        elif k == "include":
            name = self._eval(n[1], fr)
            try:
                if isinstance(name, (list, tuple)):
                    t = self.environment.select_template(name)
                else:
                    t = name if isinstance(name, Template) else self.environment.get_template(name)
            except TemplateNotFound:
                if n[2]:
                    return
                raise
            t._render_root(dict(fr.vars), {}, fr.out)
        elif k == "with":
            vars = dict(fr.vars)
            for name, e in n[1]:
                vars[name] = self._eval(e, fr)
            self._exec_list(n[2], _Frame(fr.tmpl, vars, fr.blocks, fr.out))

    def _module(self, t):
        """Run a template's top level and expose its macros and top-level
        assignments as attributes, as ``{% import %}`` does."""
        if not isinstance(t, Template):
            t = self.environment.get_template(t)
        vars = dict(self.environment.globals)
        out = []
        t._render_root(vars, {}, out)
        mod = _TemplateModule(t.name)
        for k, v in vars.items():
            if k not in self.environment.globals or vars[k] is not self.environment.globals[k]:
                if not k.startswith("_"):
                    setattr(mod, k, v)
        mod._body = "".join(out)
        return mod

    def _render_block(self, name, level, fr):
        chain = fr.blocks.get(name)
        if not chain:
            return
        owner, body = chain[level]
        vars = dict(fr.vars)
        if level + 1 < len(chain):
            def sup():
                sub = _Frame(fr.tmpl, fr.vars, fr.blocks, [])
                self._render_block(name, level + 1, sub)
                return Markup("".join(sub.out))
            vars["super"] = sup
        owner._exec_list(body, _Frame(owner, vars, fr.blocks, fr.out))

    def _assign(self, vars, targets, v):
        if len(targets) == 1:
            vars[targets[0]] = v
        else:
            vals = list(v)
            if len(vals) != len(targets):
                raise ValueError("not enough values to unpack")
            for t, x in zip(targets, vals):
                vars[t] = x

    def _for(self, n, fr):
        _, targets, it, filt, body, other = n
        seq = self._eval(it, fr)
        if isinstance(seq, dict):
            seq = list(seq.keys())
        items = list(seq)
        if filt is not None:
            kept = []
            for x in items:
                tmp = dict(fr.vars)
                self._assign(tmp, targets, x)
                if self._eval(filt, _Frame(fr.tmpl, tmp, fr.blocks, fr.out)):
                    kept.append(x)
            items = kept
        if not items:
            self._exec_list(other, fr)
            return
        loop = _Loop(items)
        for idx, x in enumerate(items):
            loop.index0 = idx
            vars = dict(fr.vars)
            vars["loop"] = loop
            self._assign(vars, targets, x)
            self._exec_list(body, _Frame(fr.tmpl, vars, fr.blocks, fr.out))

    def _finalize(self, v):
        if isinstance(v, Undefined):
            return ""
        if self.autoescape:
            return str(escape(v))
        if hasattr(v, "__html__") and not isinstance(v, str):
            return str(v.__html__())
        return str(v)

    # -- expressions
    def _eval(self, e, fr):
        k = e[0]
        if k == "const":
            return e[1]
        if k == "name":
            name = e[1]
            if name in fr.vars:
                return fr.vars[name]
            return Undefined(name=name)
        if k == "getattr":
            return _attr(self._eval(e[1], fr), e[2])
        if k == "getitem":
            return _item(self._eval(e[1], fr), self._eval(e[2], fr))
        if k == "call":
            f = self._eval(e[1], fr)
            args = [self._eval(a, fr) for a in e[2]]
            kw = dict((kk, self._eval(v, fr)) for kk, v in e[3])
            return f(*args, **kw)
        if k == "filter":
            name = e[1]
            v = self._eval(e[2], fr)
            args = [self._eval(a, fr) for a in e[3]]
            kw = dict((kk, self._eval(x, fr)) for kk, x in e[4])
            if name in self.environment.filters:
                return self.environment.filters[name](v, *args, **kw)
            if name in ("escape", "e", "forceescape"):
                return escape(str(v)) if name == "forceescape" else escape(v)
            if name == "join":
                return _do_join(self.autoescape, v, *args, **kw)
            f = _FILTERS.get(name)
            if f is None:
                raise TemplateSyntaxError("No filter named %r." % (name,))
            return f(v, *args, **kw)
        if k == "test":
            r = _test(e[1], self._eval(e[2], fr), [self._eval(a, fr) for a in e[3]])
            return (not r) if e[4] else r
        if k == "not":
            return not self._eval(e[1], fr)
        if k == "and":
            a = self._eval(e[1], fr)
            return self._eval(e[2], fr) if a else a
        if k == "or":
            a = self._eval(e[1], fr)
            return a if a else self._eval(e[2], fr)
        if k == "cond":
            if self._eval(e[1], fr):
                return self._eval(e[2], fr)
            return self._eval(e[3], fr) if e[3] is not None else Undefined()
        if k == "concat":
            a = self._eval(e[1], fr)
            b = self._eval(e[2], fr)
            if self.autoescape:
                return Markup(str(escape(a)) + str(escape(b)))
            return str(a) + str(b)
        if k == "neg":
            return -self._eval(e[1], fr)
        if k == "bin":
            a = self._eval(e[2], fr)
            b = self._eval(e[3], fr)
            op = e[1]
            if op == "+":
                return a + b
            if op == "-":
                return a - b
            if op == "*":
                return a * b
            if op == "/":
                return a / b
            if op == "//":
                return a // b
            if op == "%":
                return a % b
            return a ** b
        if k == "cmp":
            a = self._eval(e[2], fr)
            b = self._eval(e[3], fr)
            op = e[1]
            if op == "==":
                return a == b
            if op == "!=":
                return a != b
            if op == "<":
                return a < b
            if op == ">":
                return a > b
            if op == "<=":
                return a <= b
            if op == ">=":
                return a >= b
            if op == "in":
                return a in b
            return a not in b
        if k == "list":
            return [self._eval(x, fr) for x in e[1]]
        if k == "tuple":
            return tuple(self._eval(x, fr) for x in e[1])
        if k == "dict":
            return dict((self._eval(a, fr), self._eval(b, fr)) for a, b in e[1])
        raise TemplateSyntaxError("bad node %r" % (k,))


class Environment:
    def __init__(self, loader=None, autoescape=False, keep_trailing_newline=False,
                 undefined=Undefined, **kw):
        self.loader = loader
        self.autoescape = autoescape
        self.keep_trailing_newline = keep_trailing_newline
        self.undefined = undefined
        self.filters = {}
        self.tests = {}
        self.globals = {"range": range, "dict": dict, "lipsum": lambda *a, **k: "",
                        "cycler": Cycler, "namespace": _Namespace}
        self._cache = {}

    def from_string(self, source, globals=None, template_class=None):
        t = self._from_source(source, None, None)
        if globals:
            t.globals = globals
        return t

    def _from_source(self, source, name, filename):
        t = Template.__new__(Template)
        t._setup(self, source, name, filename)
        return t

    def get_template(self, name, parent=None, globals=None):
        if isinstance(name, Template):
            return name
        if name in self._cache:
            return self._cache[name]
        if self.loader is None:
            raise TypeError("no loader for this environment specified")
        t = self.loader.load(self, name, globals)
        self._cache[name] = t
        return t

    def select_template(self, names, parent=None, globals=None):
        for n in names:
            try:
                return self.get_template(n)
            except TemplateNotFound:
                pass
        raise TemplateNotFound(", ".join(str(n) for n in names))

    get_or_select_template = select_template

    def list_templates(self):
        m = getattr(self.loader, "mapping", None)
        return sorted(m) if m is not None else []


class _Namespace:
    def __init__(self, **kw):
        self.__dict__.update(kw)
