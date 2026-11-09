# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""An XPath 1.0 evaluator over lxml.etree's pure-Python elements.

Location paths on every axis but namespace, following and preceding;
predicates (positional and boolean); unions; the operators; and the core
function library. Variables are not supported. It also evaluates
ElementPath expressions for find()/findall(), which are a subset of XPath
plus the "{uri}local" and "{*}local" name tests.

Nodes are the elements themselves (kind "e", comments "c", processing
instructions "p", told apart by their _xkind), plus three helper kinds made
on demand: _Doc (the document node above the root), _Attr and _Text.
"""
import math as _math
import re as _re


class XPathError(Exception):
    pass


class XPathEvalError(XPathError):
    pass


class XPathSyntaxError(XPathError, SyntaxError):
    pass


_TOKEN = _re.compile(
    r"\s*(?:(\d+(?:\.\d*)?|\.\d+)|(\"[^\"]*\"|'[^']*')|(//|::|\.\.|!=|<=|>=|[/()\[\]@,|.=<>+\-*$])"
    r"|(\{[^}]*\}(?:[A-Za-z_][\w.\-]*|\*)|[A-Za-z_][\w.\-]*(?::(?:[A-Za-z_][\w.\-]*|\*))?))")

_AXES = ("ancestor", "ancestor-or-self", "attribute", "child", "descendant", "descendant-or-self",
         "following-sibling", "parent", "preceding-sibling", "self")
_REVERSE = ("ancestor", "ancestor-or-self", "preceding-sibling", "parent")
_NODE_TYPES = ("node", "text", "comment", "processing-instruction")
_OPS_BEFORE = ("@", "::", "(", "[", ",", "/", "//", "|", "and", "or", "mod", "div", "*", "=", "!=",
               "<", "<=", ">", ">=", "+", "-")


def _tokenize(expr):
    toks = []
    pos = 0
    n = len(expr)
    while pos < n:
        m = _TOKEN.match(expr, pos)
        if m is None or m.end() == pos:
            if expr[pos:].strip() == "":
                break
            raise XPathSyntaxError("Invalid expression")
        pos = m.end()
        if m.group(1) is not None:
            toks.append(("num", float(m.group(1))))
        elif m.group(2) is not None:
            toks.append(("str", m.group(2)[1:-1]))
        elif m.group(3) is not None:
            op = m.group(3)
            if op == "*" and toks and not _is_op_ctx(toks[-1]):
                toks.append(("op", "mul"))
            else:
                toks.append(("op", op))
        else:
            name = m.group(4)
            if name in ("and", "or", "div", "mod") and toks and not _is_op_ctx(toks[-1]):
                toks.append(("op", name))
            else:
                toks.append(("name", name))
    toks.append(("end", None))
    return toks


def _is_op_ctx(tok):
    kind, val = tok
    return kind == "op" and val in _OPS_BEFORE


class _Parser:
    def __init__(self, expr):
        self.toks = _tokenize(expr)
        self.i = 0

    def peek(self, off=0):
        return self.toks[self.i + off]

    def next(self):
        t = self.toks[self.i]
        self.i += 1
        return t

    def accept(self, val):
        t = self.toks[self.i]
        if t[0] == "op" and t[1] == val:
            self.i += 1
            return True
        return False

    def expect(self, val):
        if not self.accept(val):
            raise XPathSyntaxError("Invalid expression")

    def parse(self):
        e = self.or_expr()
        if self.peek()[0] != "end":
            raise XPathSyntaxError("Invalid expression")
        return e

    def or_expr(self):
        e = self.and_expr()
        while self.accept("or"):
            e = ("or", e, self.and_expr())
        return e

    def and_expr(self):
        e = self.eq_expr()
        while self.accept("and"):
            e = ("and", e, self.eq_expr())
        return e

    def eq_expr(self):
        e = self.rel_expr()
        while True:
            t = self.peek()
            if t[0] == "op" and t[1] in ("=", "!="):
                self.i += 1
                e = ("cmp", t[1], e, self.rel_expr())
            else:
                return e

    def rel_expr(self):
        e = self.add_expr()
        while True:
            t = self.peek()
            if t[0] == "op" and t[1] in ("<", "<=", ">", ">="):
                self.i += 1
                e = ("cmp", t[1], e, self.add_expr())
            else:
                return e

    def add_expr(self):
        e = self.mul_expr()
        while True:
            t = self.peek()
            if t[0] == "op" and t[1] in ("+", "-"):
                self.i += 1
                e = ("arith", t[1], e, self.mul_expr())
            else:
                return e

    def mul_expr(self):
        e = self.unary()
        while True:
            t = self.peek()
            if t[0] == "op" and t[1] in ("mul", "div", "mod"):
                self.i += 1
                e = ("arith", t[1], e, self.unary())
            else:
                return e

    def unary(self):
        if self.accept("-"):
            return ("neg", self.unary())
        e = self.path_expr()
        while self.accept("|"):
            e = ("union", e, self.path_expr())
        return e

    def path_expr(self):
        t = self.peek()
        if t[0] == "op" and t[1] == "/":
            self.i += 1
            if self._step_start():
                return ("path", "/", None, self.rel_path([]))
            return ("path", "/", None, [])
        if t[0] == "op" and t[1] == "//":
            self.i += 1
            return ("path", "/", None, self.rel_path([("descendant-or-self", ("node",), [])]))
        if self._primary_start():
            prim = self.primary()
            preds = []
            while self.peek() == ("op", "["):
                preds.append(self.predicate())
            if preds:
                prim = ("filter", prim, preds)
            t = self.peek()
            if t[0] == "op" and t[1] in ("/", "//"):
                self.i += 1
                steps = [("descendant-or-self", ("node",), [])] if t[1] == "//" else []
                return ("path", None, prim, self.rel_path(steps))
            return prim
        return ("path", None, None, self.rel_path([]))

    def _step_start(self):
        t = self.peek()
        return t[0] == "name" or (t[0] == "op" and t[1] in ("@", ".", "..", "*"))

    def _primary_start(self):
        t = self.peek()
        if t[0] in ("num", "str"):
            return True
        if t[0] == "op" and t[1] in ("(", "$"):
            return True
        if t[0] == "name" and self.peek(1) == ("op", "(") and t[1] not in _NODE_TYPES:
            return True
        return False

    def primary(self):
        t = self.next()
        if t[0] == "num":
            return ("num", t[1])
        if t[0] == "str":
            return ("str", t[1])
        if t[1] == "(":
            e = self.or_expr()
            self.expect(")")
            return e
        if t[1] == "$":
            raise XPathEvalError("Undefined variable")
        name = t[1]
        self.expect("(")
        args = []
        if not self.accept(")"):
            args.append(self.or_expr())
            while self.accept(","):
                args.append(self.or_expr())
            self.expect(")")
        return ("call", name, args)

    def predicate(self):
        self.expect("[")
        e = self.or_expr()
        self.expect("]")
        return e

    def rel_path(self, steps):
        steps.append(self.step())
        while True:
            if self.accept("/"):
                steps.append(self.step())
            elif self.accept("//"):
                steps.append(("descendant-or-self", ("node",), []))
                steps.append(self.step())
            else:
                return steps

    def step(self):
        if self.accept("."):
            return ("self", ("node",), [])
        if self.accept(".."):
            return ("parent", ("node",), [])
        axis = "child"
        if self.accept("@"):
            axis = "attribute"
        else:
            t = self.peek()
            if t[0] == "name" and self.peek(1) == ("op", "::"):
                if t[1] not in _AXES:
                    raise XPathEvalError("Unsupported axis " + t[1])
                axis = t[1]
                self.i += 2
        t = self.next()
        if t == ("op", "*") or t == ("op", "mul"):
            test = ("name", None, "*", False)
        elif t[0] == "name":
            name = t[1]
            if name in _NODE_TYPES and self.peek() == ("op", "("):
                self.i += 1
                arg = None
                if self.peek()[0] == "str":
                    arg = self.next()[1]
                self.expect(")")
                test = ("pi", arg) if name == "processing-instruction" else (name,)
            elif name[:1] == "{":
                j = name.index("}")
                uri = name[1:j]
                if uri == "*":
                    test = ("name", None, name[j + 1:], True)
                else:
                    test = ("uri", uri or None, name[j + 1:])
            elif ":" in name:
                j = name.index(":")
                test = ("name", name[:j], name[j + 1:], False)
            else:
                test = ("name", None, name, False)
        else:
            raise XPathSyntaxError("Invalid expression")
        preds = []
        while self.peek() == ("op", "["):
            preds.append(self.predicate())
        return (axis, test, preds)


_CACHE = {}


def compile_expr(expr):
    ast = _CACHE.get(expr)
    if ast is None:
        ast = _optimize(_Parser(expr).parse())
        if len(_CACHE) > 500:
            _CACHE.clear()
        _CACHE[expr] = ast
    return ast


def _optimize(e):
    """Fold descendant-or-self::node()/child::x (no predicates) into descendant::x."""
    if isinstance(e, tuple) and e and e[0] == "path":
        steps = []
        for s in e[3]:
            if (steps and s[0] == "child" and not s[2] and steps[-1] == ("descendant-or-self", ("node",), [])):
                steps[-1] = ("descendant", s[1], [_optimize(p) for p in s[2]])
            else:
                steps.append((s[0], s[1], [_optimize(p) for p in s[2]]))
        return ("path", e[1], _optimize(e[2]) if e[2] is not None else None, steps)
    if isinstance(e, tuple) and e and e[0] in ("or", "and", "union"):
        return (e[0], _optimize(e[1]), _optimize(e[2]))
    if isinstance(e, tuple) and e and e[0] in ("cmp", "arith"):
        return (e[0], e[1], _optimize(e[2]), _optimize(e[3]))
    if isinstance(e, tuple) and e and e[0] == "call":
        return ("call", e[1], [_optimize(a) for a in e[2]])
    if isinstance(e, tuple) and e and e[0] == "filter":
        return ("filter", _optimize(e[1]), [_optimize(p) for p in e[2]])
    if isinstance(e, tuple) and e and e[0] == "neg":
        return ("neg", _optimize(e[1]))
    return e


class _Doc:
    _xkind = "d"

    def __init__(self, root):
        self.root = root


class _Attr:
    _xkind = "a"

    def __init__(self, owner, name, value, index):
        self.owner = owner
        self.name = name
        self.value = value
        self.index = index


class _Text:
    _xkind = "t"

    def __init__(self, holder, value, is_tail):
        self.holder = holder
        self.value = value
        self.is_tail = is_tail


def _top(el):
    while el._xparent is not None:
        el = el._xparent
    return el


def _node_root(n):
    k = n._xkind
    if k == "d":
        return n.root
    if k == "a":
        return _top(n.owner)
    if k == "t":
        return _top(n.holder)
    return _top(n)


def _same(a, b):
    if a is b:
        return True
    if a._xkind != b._xkind:
        return False
    k = a._xkind
    if k == "a":
        return a.owner is b.owner and a.name == b.name
    if k == "t":
        return a.holder is b.holder and a.is_tail == b.is_tail
    if k == "d":
        return a.root is b.root
    return False


def _ident(n):
    k = n._xkind
    if k == "a":
        return (id(n.owner), "a", n.name)
    if k == "t":
        return (id(n.holder), "t", n.is_tail)
    if k == "d":
        return (id(n.root), "d")
    return (id(n), "e")


def _children(el, with_text):
    if el._xkind == "d":
        return [el.root]
    if el._xkind != "e":
        return []
    if not with_text:
        return list(el._xkids)
    out = []
    if el._xtext:
        out.append(_Text(el, el._xtext, False))
    for c in el._xkids:
        out.append(c)
        if c._xtail:
            out.append(_Text(c, c._xtail, True))
    return out


def _descendants(el, with_text, out):
    for c in _children(el, with_text):
        out.append(c)
        if c._xkind == "e":
            _descendants(c, with_text, out)
    return out


def _parent(n):
    k = n._xkind
    if k == "a":
        return n.owner
    if k == "t":
        return n.holder._xparent if n.is_tail else n.holder
    if k == "d":
        return None
    p = n._xparent
    if p is None:
        return _Doc(n)
    return p


def _siblings(n, following, with_text):
    if n._xkind in ("a", "d"):
        return []
    p = _parent(n)
    if p is None:
        return []
    sibs = _children(p, with_text)
    idx = -1
    for i in range(len(sibs)):
        if _same(sibs[i], n):
            idx = i
            break
    if idx < 0:
        return []
    if following:
        return sibs[idx + 1:]
    r = sibs[:idx]
    r.reverse()
    return r


def _axis(n, axis, with_text):
    if axis == "child":
        return _children(n, with_text)
    if axis == "attribute":
        if n._xkind != "e":
            return []
        out = []
        i = 0
        for k, v in n._xattrib.items():
            out.append(_Attr(n, k, v, i))
            i += 1
        return out
    if axis == "self":
        return [n]
    if axis == "descendant":
        return _descendants(n, with_text, [])
    if axis == "descendant-or-self":
        return _descendants(n, with_text, [n])
    if axis == "parent":
        p = _parent(n)
        return [] if p is None else [p]
    if axis == "ancestor" or axis == "ancestor-or-self":
        out = [n] if axis == "ancestor-or-self" else []
        p = _parent(n)
        while p is not None:
            out.append(p)
            p = _parent(p)
        return out
    if axis == "following-sibling":
        return _siblings(n, True, with_text)
    return _siblings(n, False, with_text)


def _split(tag):
    if tag[:1] == "{":
        j = tag.index("}")
        return tag[1:j], tag[j + 1:]
    return None, tag


def _test(n, axis, test, env):
    k = n._xkind
    kind = test[0]
    if kind == "node":
        return True
    if kind == "text":
        return k == "t"
    if kind == "comment":
        return k == "c"
    if kind == "pi":
        return k == "p" and (test[1] is None or n._xtarget == test[1])
    if axis == "attribute":
        if k != "a":
            return False
        uri, local = _split(n.name)
    else:
        if k != "e":
            return False
        uri, local = _split(n._xtag)
    if kind == "uri":
        return uri == test[1] and (test[2] == "*" or local == test[2])
    prefix, lname, anyns = test[1], test[2], test[3]
    if prefix is not None and prefix != "xml" and prefix not in env.namespaces:
        raise XPathEvalError("Undefined namespace prefix")
    if lname != "*" and lname != local:
        return False
    if anyns:
        return True
    if prefix is None:
        if lname == "*":
            return True
        want = env.default_ns if axis != "attribute" else None
        return uri == want
    if prefix == "xml":
        want = "http://www.w3.org/XML/1998/namespace"
    else:
        want = env.namespaces.get(prefix)
        if want is None:
            raise XPathEvalError("Undefined namespace prefix")
    return uri == want


class Env:
    def __init__(self, namespaces, default_ns=None):
        self.namespaces = namespaces or {}
        self.default_ns = default_ns


def _doc_order(nodes):
    if len(nodes) < 2:
        return nodes
    roots = []
    for n in nodes:
        r = _node_root(n)
        found = False
        for x in roots:
            if x is r:
                found = True
                break
        if not found:
            roots.append(r)
    order = {}
    counter = [0]
    for r in roots:
        order[(id(r), "d")] = counter[0]
        counter[0] += 1
        _number(r, order, counter)
    nodes.sort(key=lambda x: order.get(_ident(x), 0))
    return nodes


def _number(el, order, counter):
    order[(id(el), "e")] = counter[0]
    counter[0] += 1
    if el._xkind != "e":
        return
    for k in el._xattrib:
        order[(id(el), "a", k)] = counter[0]
        counter[0] += 1
    order[(id(el), "t", False)] = counter[0]
    counter[0] += 1
    for c in el._xkids:
        _number(c, order, counter)
        order[(id(c), "t", True)] = counter[0]
        counter[0] += 1


def _dedupe(nodes):
    seen = set()
    out = []
    for n in nodes:
        key = _ident(n)
        if key not in seen:
            seen.add(key)
            out.append(n)
    return out


def evaluate(ast, node, env):
    return _eval(ast, node, 1, 1, env)


def _eval(e, node, pos, size, env):
    op = e[0]
    if op == "path":
        return _eval_path(e, node, pos, size, env)
    if op == "num":
        return e[1]
    if op == "str":
        return e[1]
    if op == "union":
        a = _eval(e[1], node, pos, size, env)
        b = _eval(e[2], node, pos, size, env)
        if not isinstance(a, list) or not isinstance(b, list):
            raise XPathEvalError("Invalid type")
        return _doc_order(_dedupe(a + b))
    if op == "or":
        return to_bool(_eval(e[1], node, pos, size, env)) or to_bool(_eval(e[2], node, pos, size, env))
    if op == "and":
        return to_bool(_eval(e[1], node, pos, size, env)) and to_bool(_eval(e[2], node, pos, size, env))
    if op == "cmp":
        return _compare(e[1], _eval(e[2], node, pos, size, env), _eval(e[3], node, pos, size, env))
    if op == "arith":
        a = to_num(_eval(e[2], node, pos, size, env))
        b = to_num(_eval(e[3], node, pos, size, env))
        o = e[1]
        if o == "+":
            return a + b
        if o == "-":
            return a - b
        if o == "mul":
            return a * b
        if o == "div":
            if b == 0:
                if a == 0 or a != a:
                    return float("nan")
                return float("inf") if (a > 0) == (_math.copysign(1.0, b) > 0) else float("-inf")
            return a / b
        if b == 0:
            return float("nan")
        return _math.fmod(a, b)
    if op == "neg":
        return -to_num(_eval(e[1], node, pos, size, env))
    if op == "filter":
        v = _eval(e[1], node, pos, size, env)
        if not isinstance(v, list):
            raise XPathEvalError("Invalid type")
        for p in e[2]:
            v = _apply_pred(v, p, env)
        return v
    if op == "call":
        return _call(e[1], e[2], node, pos, size, env)
    raise XPathEvalError("Invalid expression")


def _eval_path(e, node, pos, size, env):
    if e[1] == "/":
        nodes = [_Doc(_node_root(node))]
    elif e[2] is not None:
        nodes = _eval(e[2], node, pos, size, env)
        if not isinstance(nodes, list):
            raise XPathEvalError("Invalid type")
    else:
        nodes = [node]
    for axis, test, preds in e[3]:
        with_text = test[0] in ("text", "node")
        out = []
        for n in nodes:
            cand = [c for c in _axis(n, axis, with_text) if _test(c, axis, test, env)]
            for p in preds:
                cand = _apply_pred(cand, p, env)
            out.extend(cand)
        if len(nodes) > 1:
            out = _doc_order(_dedupe(out))
        elif axis in _REVERSE:
            out.reverse()
        nodes = out
    return nodes


def _apply_pred(cand, pred, env):
    size = len(cand)
    out = []
    for i in range(size):
        v = _eval(pred, cand[i], i + 1, size, env)
        if isinstance(v, float) and not isinstance(v, bool):
            if v == i + 1:
                out.append(cand[i])
        elif to_bool(v):
            out.append(cand[i])
    return out


def string_value(n):
    k = n._xkind
    if k == "a":
        return n.value
    if k == "t":
        return n.value
    if k == "d":
        return string_value(n.root)
    if k != "e":
        return n._xtext or ""
    parts = []
    _collect_text(n, parts)
    return "".join(parts)


def _collect_text(el, parts):
    if el._xtext:
        parts.append(el._xtext)
    for c in el._xkids:
        if c._xkind == "e":
            _collect_text(c, parts)
        if c._xtail:
            parts.append(c._xtail)


def to_bool(v):
    if isinstance(v, list):
        return len(v) > 0
    if isinstance(v, bool):
        return v
    if isinstance(v, float):
        return v == v and v != 0
    return len(v) > 0


def _str_to_num(s):
    s = s.strip(" \t\r\n")
    if not s or not _re.match(r"^-?(\d+(\.\d*)?|\.\d+)$", s):
        return float("nan")
    return float(s)


def to_num(v):
    if isinstance(v, list):
        return _str_to_num(to_str(v))
    if isinstance(v, bool):
        return 1.0 if v else 0.0
    if isinstance(v, float):
        return v
    if isinstance(v, int):
        return float(v)
    return _str_to_num(v)


def num_to_str(f):
    if f != f:
        return "NaN"
    if f == float("inf"):
        return "Infinity"
    if f == float("-inf"):
        return "-Infinity"
    if f == int(f):
        return str(int(f))
    return repr(f)


def to_str(v):
    if isinstance(v, list):
        return string_value(v[0]) if v else ""
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, float):
        return num_to_str(v)
    return v


def _cmp_atoms(op, a, b):
    if op in ("=", "!="):
        if isinstance(a, bool) or isinstance(b, bool):
            r = to_bool(a) == to_bool(b)
        elif isinstance(a, float) or isinstance(b, float):
            r = to_num(a) == to_num(b)
        else:
            r = to_str(a) == to_str(b)
        return r if op == "=" else not r
    x = to_num(a)
    y = to_num(b)
    if op == "<":
        return x < y
    if op == "<=":
        return x <= y
    if op == ">":
        return x > y
    return x >= y


def _compare(op, a, b):
    la = isinstance(a, list)
    lb = isinstance(b, list)
    if la and lb:
        sb = [string_value(y) for y in b]
        for x in a:
            sx = string_value(x)
            for y in sb:
                if _cmp_atoms(op, sx, y):
                    return True
        return False
    if la or lb:
        nodes, other, flip = (a, b, False) if la else (b, a, True)
        if isinstance(other, bool):
            return _cmp_atoms(op, other, to_bool(nodes)) if flip else _cmp_atoms(op, to_bool(nodes), other)
        for x in nodes:
            s = string_value(x)
            if isinstance(other, float):
                s = _str_to_num(s)
            if (_cmp_atoms(op, other, s) if flip else _cmp_atoms(op, s, other)):
                return True
        return False
    return _cmp_atoms(op, a, b)


def _local_name(n):
    k = n._xkind
    if k == "e":
        return _split(n._xtag)[1]
    if k == "a":
        return _split(n.name)[1]
    if k == "p":
        return n._xtarget
    return ""


def _ns_uri(n):
    k = n._xkind
    if k == "e":
        return _split(n._xtag)[0] or ""
    if k == "a":
        return _split(n.name)[0] or ""
    return ""


def _call(name, args, node, pos, size, env):
    vals = [_eval(a, node, pos, size, env) for a in args]
    if name == "count":
        return float(len(vals[0]))
    if name == "last":
        return float(size)
    if name == "position":
        return float(pos)
    if name == "string":
        return to_str(vals[0]) if vals else string_value(node)
    if name == "number":
        return to_num(vals[0]) if vals else _str_to_num(string_value(node))
    if name == "boolean":
        return to_bool(vals[0])
    if name == "not":
        return not to_bool(vals[0])
    if name == "true":
        return True
    if name == "false":
        return False
    if name == "local-name" or name == "name" or name == "namespace-uri":
        target = node
        if vals:
            if not vals[0]:
                return ""
            target = vals[0][0]
        if name == "namespace-uri":
            return _ns_uri(target)
        if name == "name" and target._xkind == "e":
            p = target.prefix
            ln = _local_name(target)
            return p + ":" + ln if p else ln
        return _local_name(target)
    if name == "concat":
        return "".join(to_str(v) for v in vals)
    if name == "contains":
        return to_str(vals[1]) in to_str(vals[0])
    if name == "starts-with":
        return to_str(vals[0]).startswith(to_str(vals[1]))
    if name == "ends-with":
        return to_str(vals[0]).endswith(to_str(vals[1]))
    if name == "string-length":
        return float(len(to_str(vals[0]) if vals else string_value(node)))
    if name == "normalize-space":
        s = to_str(vals[0]) if vals else string_value(node)
        return " ".join(s.split())
    if name == "sum":
        t = 0.0
        for n in vals[0]:
            t += _str_to_num(string_value(n))
        return t
    if name == "floor":
        return float(_math.floor(to_num(vals[0])))
    if name == "ceiling":
        return float(_math.ceil(to_num(vals[0])))
    if name == "round":
        f = to_num(vals[0])
        if f != f or f in (float("inf"), float("-inf")):
            return f
        return float(_math.floor(f + 0.5))
    if name == "substring-before":
        s = to_str(vals[0])
        t = to_str(vals[1])
        i = s.find(t)
        return s[:i] if i >= 0 else ""
    if name == "substring-after":
        s = to_str(vals[0])
        t = to_str(vals[1])
        i = s.find(t)
        return s[i + len(t):] if i >= 0 else ""
    if name == "substring":
        s = to_str(vals[0])
        start = to_num(vals[1])
        if start != start:
            return ""
        start = _math.floor(start + 0.5)
        if len(vals) > 2:
            ln = to_num(vals[2])
            if ln != ln:
                return ""
            end = start + _math.floor(ln + 0.5)
        else:
            end = float("inf")
        out = []
        for i in range(len(s)):
            p = i + 1
            if p >= start and p < end:
                out.append(s[i])
        return "".join(out)
    if name == "translate":
        s = to_str(vals[0])
        src = to_str(vals[1])
        dst = to_str(vals[2])
        out = []
        for ch in s:
            i = src.find(ch)
            if i < 0:
                out.append(ch)
            elif i < len(dst):
                out.append(dst[i])
        return "".join(out)
    raise XPathEvalError("Unregistered function")
