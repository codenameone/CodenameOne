# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""inspect: signatures and the is* predicates. There are no frames or
source to read, so what needs them (stack, getsource) is absent."""

import types as _types

_empty_marker = None


class _Empty:
    def __repr__(self):
        return "<class 'inspect._empty'>"


_empty = _Empty()


class _ParameterKind:
    def __init__(self, value, name, description):
        self.value = value
        self.name = name
        self.description = description

    def __repr__(self):
        return "<_ParameterKind.%s: %d>" % (self.name, self.value)

    def __str__(self):
        return self.name

    def __lt__(self, other):
        return self.value < other.value

    def __eq__(self, other):
        return isinstance(other, _ParameterKind) and other.value == self.value

    def __hash__(self):
        return self.value


POSITIONAL_ONLY = _ParameterKind(0, "POSITIONAL_ONLY", "positional-only")
POSITIONAL_OR_KEYWORD = _ParameterKind(1, "POSITIONAL_OR_KEYWORD", "positional or keyword")
VAR_POSITIONAL = _ParameterKind(2, "VAR_POSITIONAL", "variadic positional")
KEYWORD_ONLY = _ParameterKind(3, "KEYWORD_ONLY", "keyword-only")
VAR_KEYWORD = _ParameterKind(4, "VAR_KEYWORD", "variadic keyword")


def _formatannotation(a):
    if isinstance(a, type):
        if a.__module__ == "builtins":
            return a.__qualname__
        return a.__module__ + "." + a.__qualname__
    if isinstance(a, str):
        return repr(a)
    return repr(a).replace("typing.", "")


class Parameter:
    POSITIONAL_ONLY = POSITIONAL_ONLY
    POSITIONAL_OR_KEYWORD = POSITIONAL_OR_KEYWORD
    VAR_POSITIONAL = VAR_POSITIONAL
    KEYWORD_ONLY = KEYWORD_ONLY
    VAR_KEYWORD = VAR_KEYWORD
    empty = _empty

    def __init__(self, name, kind, *, default=_empty, annotation=_empty):
        self.name = name
        self.kind = kind
        self.default = default
        self.annotation = annotation

    def replace(self, **kw):
        return Parameter(kw.get("name", self.name), kw.get("kind", self.kind),
                         default=kw.get("default", self.default), annotation=kw.get("annotation", self.annotation))

    def __str__(self):
        s = self.name
        if self.annotation is not _empty:
            s += ": " + _formatannotation(self.annotation)
        if self.default is not _empty:
            s += (" = " if self.annotation is not _empty else "=") + repr(self.default)
        if self.kind == VAR_POSITIONAL:
            s = "*" + s
        elif self.kind == VAR_KEYWORD:
            s = "**" + s
        return s

    def __repr__(self):
        return '<Parameter "%s">' % str(self)

    def __eq__(self, other):
        return isinstance(other, Parameter) and (self.name, self.kind, self.default, self.annotation) == \
            (other.name, other.kind, other.default, other.annotation)

    def __hash__(self):
        return hash((self.name, self.kind))


class Signature:
    empty = _empty

    def __init__(self, parameters=None, *, return_annotation=_empty):
        self._params = {}
        for p in parameters or ():
            self._params[p.name] = p
        self.return_annotation = return_annotation

    @property
    def parameters(self):
        return _types.MappingProxyType(self._params) if hasattr(_types, "MappingProxyType") else dict(self._params)

    def replace(self, *, parameters=None, return_annotation=None):
        return Signature(list(self._params.values()) if parameters is None else parameters,
                         return_annotation=self.return_annotation if return_annotation is None else return_annotation)

    def bind(self, *args, **kwargs):
        return self._bind(args, kwargs, False)

    def bind_partial(self, *args, **kwargs):
        return self._bind(args, kwargs, True)

    def _bind(self, args, kwargs, partial):
        out = {}
        params = list(self._params.values())
        args = list(args)
        kwargs = dict(kwargs)
        for p in params:
            if p.kind in (POSITIONAL_ONLY, POSITIONAL_OR_KEYWORD):
                if args:
                    out[p.name] = args.pop(0)
                elif p.name in kwargs:
                    out[p.name] = kwargs.pop(p.name)
                elif p.default is _empty and not partial:
                    raise TypeError("missing a required argument: '%s'" % p.name)
            elif p.kind == VAR_POSITIONAL:
                if args:
                    out[p.name] = tuple(args)
                    args = []
            elif p.kind == KEYWORD_ONLY:
                if p.name in kwargs:
                    out[p.name] = kwargs.pop(p.name)
                elif p.default is _empty and not partial:
                    raise TypeError("missing a required argument: '%s'" % p.name)
            elif p.kind == VAR_KEYWORD:
                if kwargs:
                    out[p.name] = kwargs
                    kwargs = {}
        if args:
            raise TypeError("too many positional arguments")
        if kwargs:
            raise TypeError("got an unexpected keyword argument '%s'" % next(iter(kwargs)))
        return BoundArguments(self, out)

    def __str__(self):
        parts = []
        star = False
        for p in self._params.values():
            if p.kind == VAR_POSITIONAL:
                star = True
            elif p.kind == KEYWORD_ONLY and not star:
                parts.append("*")
                star = True
            parts.append(str(p))
        s = "(" + ", ".join(parts) + ")"
        if self.return_annotation is not _empty:
            s += " -> " + _formatannotation(self.return_annotation)
        return s

    def __repr__(self):
        return "<Signature %s>" % str(self)

    def __eq__(self, other):
        return isinstance(other, Signature) and list(self._params.values()) == list(other._params.values()) \
            and self.return_annotation == other.return_annotation


class BoundArguments:
    def __init__(self, signature, arguments):
        self.signature = signature
        self.arguments = arguments

    @property
    def args(self):
        out = []
        for p in self.signature._params.values():
            if p.name not in self.arguments:
                continue
            if p.kind == VAR_POSITIONAL:
                out.extend(self.arguments[p.name])
            elif p.kind in (POSITIONAL_ONLY, POSITIONAL_OR_KEYWORD):
                out.append(self.arguments[p.name])
        return tuple(out)

    @property
    def kwargs(self):
        out = {}
        for p in self.signature._params.values():
            if p.name not in self.arguments:
                continue
            if p.kind == VAR_KEYWORD:
                out.update(self.arguments[p.name])
            elif p.kind == KEYWORD_ONLY:
                out[p.name] = self.arguments[p.name]
        return out

    def apply_defaults(self):
        for p in self.signature._params.values():
            if p.name not in self.arguments:
                if p.kind == VAR_POSITIONAL:
                    self.arguments[p.name] = ()
                elif p.kind == VAR_KEYWORD:
                    self.arguments[p.name] = {}
                elif p.default is not _empty:
                    self.arguments[p.name] = p.default


def _function_signature(f, skip_first):
    code = f.__code__
    names = code.co_varnames
    npos = code.co_argcount
    nkw = code.co_kwonlyargcount
    defaults = f.__defaults__ or ()
    kwdefaults = f.__kwdefaults__ or {}
    ann = getattr(f, "__annotations__", {}) or {}
    params = []
    first_default = npos - len(defaults)
    for i in range(npos):
        n = names[i]
        params.append(Parameter(n, POSITIONAL_OR_KEYWORD,
                                default=defaults[i - first_default] if i >= first_default else _empty,
                                annotation=ann.get(n, _empty)))
    at = npos + nkw
    if code.co_flags & 0x04:
        n = names[at]
        params.append(Parameter(n, VAR_POSITIONAL, annotation=ann.get(n, _empty)))
        at += 1
    for i in range(npos, npos + nkw):
        n = names[i]
        params.append(Parameter(n, KEYWORD_ONLY, default=kwdefaults.get(n, _empty), annotation=ann.get(n, _empty)))
    if code.co_flags & 0x08:
        n = names[at]
        params.append(Parameter(n, VAR_KEYWORD, annotation=ann.get(n, _empty)))
    if skip_first and params and params[0].kind in (POSITIONAL_ONLY, POSITIONAL_OR_KEYWORD):
        params = params[1:]
    return Signature(params, return_annotation=ann.get("return", _empty))


def signature(obj, *, follow_wrapped=True):
    if follow_wrapped:
        while hasattr(obj, "__wrapped__"):
            obj = obj.__wrapped__
    sig = getattr(obj, "__signature__", None)
    if isinstance(sig, Signature):
        return sig
    if isfunction(obj):
        return _function_signature(obj, False)
    if ismethod(obj):
        return _function_signature(obj.__func__, True)
    if isinstance(obj, type):
        init = obj.__dict__.get("__init__") if "__init__" in obj.__dict__ else None
        for base in obj.__mro__:
            if "__init__" in base.__dict__ and isfunction(base.__dict__["__init__"]):
                return _function_signature(base.__dict__["__init__"], True)
        return Signature([])
    call = getattr(type(obj), "__call__", None)
    if call is not None and isfunction(call):
        return _function_signature(call, True)
    raise ValueError("no signature found for %r" % (obj,))


def isfunction(obj):
    return type(obj).__name__ == "function"


def ismethod(obj):
    return type(obj).__name__ == "method"


def isclass(obj):
    return isinstance(obj, type)


def ismodule(obj):
    return type(obj).__name__ == "module"


def isbuiltin(obj):
    return type(obj).__name__ == "builtin_function_or_method"


def isroutine(obj):
    return isfunction(obj) or ismethod(obj) or isbuiltin(obj)


def isgenerator(obj):
    return type(obj).__name__ == "generator"


def iscoroutine(obj):
    return type(obj).__name__ == "coroutine"


def iscoroutinefunction(obj):
    return bool(getattr(obj, "_is_coroutine", False)) or bool(getattr(obj, "__code__", None) and
                                                               obj.__code__.co_flags & 0x80)


def isgeneratorfunction(obj):
    return bool(getattr(obj, "__code__", None) and obj.__code__.co_flags & 0x20)


def isawaitable(obj):
    return hasattr(obj, "__await__")


def isabstract(obj):
    return bool(getattr(obj, "__abstractmethods__", False))


def getmembers(obj, predicate=None):
    out = []
    for name in dir(obj):
        try:
            v = getattr(obj, name)
        except AttributeError:
            continue
        if predicate is None or predicate(v):
            out.append((name, v))
    return sorted(out, key=lambda kv: kv[0])


def getmro(cls):
    return cls.__mro__


def cleandoc(doc):
    lines = doc.expandtabs().split("\n")
    margin = None
    for line in lines[1:]:
        content = len(line.lstrip())
        if content:
            indent = len(line) - content
            margin = indent if margin is None else min(margin, indent)
    if margin is not None:
        for i in range(1, len(lines)):
            lines[i] = lines[i][margin:]
    lines[0] = lines[0].lstrip()
    while lines and not lines[-1]:
        lines.pop()
    while lines and not lines[0]:
        lines.pop(0)
    return "\n".join(lines)


def getdoc(obj):
    doc = getattr(obj, "__doc__", None)
    if not isinstance(doc, str):
        return None
    return cleandoc(doc)


def unwrap(func):
    while hasattr(func, "__wrapped__"):
        func = func.__wrapped__
    return func
