# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Routes, parameter resolution, dependencies and the pure dispatch core."""
import asyncio
import dataclasses
import inspect
import json as _json
import re as _re
import typing
from typing import Annotated, Any, Union, get_args, get_origin, get_type_hints
from urllib.parse import quote, unquote

from pydantic import BaseModel, Field, TypeAdapter, ValidationError

from starlette.background import BackgroundTasks
from starlette.datastructures import QueryParams
from starlette.exceptions import HTTPException as StarletteHTTPException
from starlette.requests import Request
from starlette.responses import JSONResponse, RedirectResponse, Response

from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import (HTTPException, RequestValidationError, ResponseValidationError)
from fastapi.params import Body, Cookie, Depends, Form, Header, Param, Path, Query, Undefined

_NONE = type(None)
_EMPTY = inspect.Parameter.empty
_SEQ = (list, set, tuple, frozenset)


def run_coro(c):
    return asyncio.run(c)


def _hints(call):
    target = call
    if isinstance(call, type):
        target = call.__init__
    try:
        return get_type_hints(target, include_extras=True)
    except Exception:
        return {}


def _unwrap_optional(tp):
    if get_origin(tp) is Union:
        args = [a for a in get_args(tp) if a is not _NONE]
        if len(args) == 1:
            return args[0], True
        return tp, len(args) != len(get_args(tp))
    return tp, False


def _is_scalar(tp):
    tp, _ = _unwrap_optional(tp)
    org = get_origin(tp)
    if org is Union:
        for a in get_args(tp):
            if a is not _NONE and not _is_scalar(a):
                return False
        return True
    if org in _SEQ or org is dict:
        return False
    if isinstance(tp, type):
        if issubclass(tp, BaseModel) or tp in _SEQ or tp is dict:
            return False
        if dataclasses.is_dataclass(tp):
            return False
    return True


def _is_seq(tp):
    tp, _ = _unwrap_optional(tp)
    return get_origin(tp) in _SEQ or tp in _SEQ


class _Dep:
    def __init__(self, call):
        self.call = call
        self.params = []      # path/query/header/cookie
        self.body = []
        self.deps = []        # (name, _Dep, use_cache)
        self.specials = {}    # name -> kind
        self.is_gen = inspect.isgeneratorfunction(call)
        self.single_body = False
        self.form = False


class _P:
    def __init__(self, name, tp, field, default, location):
        self.name = name
        self.tp = tp
        self.field = field
        self.default = default
        self.location = location
        self.is_seq = _is_seq(tp)
        key = field.alias if field is not None and field.alias else None
        if key is None:
            key = name
            if location == "header" and (field is None or getattr(field, "convert_underscores", True)):
                key = name.replace("_", "-")
        self.key = key
        self._adapter = None

    @property
    def required(self):
        return self.default is Undefined and (self.field is None or self.field.default_factory is None)

    def default_value(self):
        if self.field is not None and self.field.default_factory is not None:
            return self.field.default_factory()
        return self.default

    def adapter(self):
        if self._adapter is None:
            tp = self.tp
            cons = self.field.constraints() if self.field is not None else {}
            if cons:
                tp = Annotated[tp, Field(**cons)]
            self._adapter = TypeAdapter(tp)
        return self._adapter


def _analyze(call, path_names):
    dep = _Dep(call)
    sig = inspect.signature(call)
    hints = _hints(call)
    for name, p in sig.parameters.items():
        if p.kind == inspect.Parameter.VAR_POSITIONAL or p.kind == inspect.Parameter.VAR_KEYWORD:
            continue
        tp = hints.get(name, p.annotation if p.annotation is not _EMPTY else Any)
        default = Undefined if p.default is _EMPTY else p.default
        field = None
        depends = None
        if get_origin(tp) is Annotated:
            args = get_args(tp)
            tp = args[0]
            for m in args[1:]:
                if isinstance(m, Depends):
                    depends = m
                elif isinstance(m, Param):
                    field = m
        if isinstance(default, Depends):
            depends = default
            default = Undefined
        elif isinstance(default, Param):
            field = default
            default = field.default
        if isinstance(tp, type) and issubclass(tp, Request):
            dep.specials[name] = "request"
            continue
        if isinstance(tp, type) and issubclass(tp, Response):
            dep.specials[name] = "response"
            continue
        if isinstance(tp, type) and issubclass(tp, BackgroundTasks):
            dep.specials[name] = "background"
            continue
        if depends is not None:
            sub = depends.dependency
            if sub is None:
                sub = tp
            dep.deps.append((name, _analyze(sub, path_names), depends.use_cache))
            continue
        if field is not None and field.location == "path" or (field is None and name in path_names):
            dep.params.append(_P(name, tp, field, Undefined, "path"))
        elif field is not None and field.location in ("query", "header", "cookie"):
            dep.params.append(_P(name, tp, field, default, field.location))
        elif field is not None and field.location == "body":
            dep.body.append(_P(name, tp, field, default, "body"))
            if isinstance(field, Form):
                dep.form = True
        elif name in path_names:
            dep.params.append(_P(name, tp, field, Undefined, "path"))
        elif _is_scalar(tp):
            dep.params.append(_P(name, tp, field, default, "query"))
        else:
            dep.body.append(_P(name, tp, field, default, "body"))
    return dep


def _all_body(dep):
    out = list(dep.body)
    for name, sub, uc in dep.deps:
        out.extend(_all_body(sub))
    return out


def _all_params(dep):
    out = list(dep.params)
    for name, sub, uc in dep.deps:
        out.extend(_all_params(sub))
    return out


_CONSTRAINT_TYPES = ("greater_than", "greater_than_equal", "less_than", "less_than_equal", "multiple_of")
_CONV = {"str": "[^/]+", "path": ".*", "int": "[0-9]+", "float": "[0-9]+(?:\\.[0-9]+)?",
         "uuid": "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"}


def _compile(path):
    pat = ""
    names = []
    pos = 0
    for m in _re.finditer("\\{([a-zA-Z_][a-zA-Z0-9_]*)(?::([a-z]+))?\\}", path):
        pat += _re.escape(path[pos:m.start()])
        conv = m.group(2) or "str"
        pat += "(" + _CONV[conv] + ")"
        names.append(m.group(1))
        pos = m.end()
    pat += _re.escape(path[pos:])
    return _re.compile("^" + pat + "$"), names


def _title(name):
    return name.replace("_", " ").title() if name else ""


class APIRoute:
    def __init__(self, path, endpoint, methods, **kw):
        self.path = path
        self.endpoint = endpoint
        self.methods = set(m.upper() for m in methods)
        self.name = kw.get("name") or endpoint.__name__
        self.regex, self.path_names = _compile(path)
        self.status_code = kw.get("status_code")
        self.response_model = kw.get("response_model", Undefined)
        self.response_class = kw.get("response_class")
        self.tags = list(kw.get("tags") or [])
        self.dependencies = list(kw.get("dependencies") or [])
        self.summary = kw.get("summary")
        self.description = kw.get("description")
        self.deprecated = kw.get("deprecated")
        self.operation_id = kw.get("operation_id")
        self.include_in_schema = kw.get("include_in_schema", True)
        self.responses = kw.get("responses") or {}
        self.exclude_unset = kw.get("response_model_exclude_unset", False)
        self.exclude_defaults = kw.get("response_model_exclude_defaults", False)
        self.exclude_none = kw.get("response_model_exclude_none", False)
        self.by_alias = kw.get("response_model_by_alias", True)
        self._dep = None
        self._pre = None
        self._adapter = None
        self._ready = False

    def _prepare(self):
        if self._ready:
            return
        self._dep = _analyze(self.endpoint, self.path_names)
        self._pre = [_analyze(d.dependency, self.path_names) for d in self.dependencies]
        rm = self.response_model
        if rm is Undefined:
            ann = _hints(self.endpoint).get("return", None)
            rm = None
            if ann is not None and ann is not _NONE and not (isinstance(ann, type) and issubclass(ann, Response)):
                rm = ann
            self.response_model = rm
        if rm is not None:
            self._adapter = TypeAdapter(rm)
        body = _all_body(self._dep)
        for p in self._pre:
            body.extend(_all_body(p))
        self.body_params = body
        self._ready = True

    def match(self, path):
        m = self.regex.match(path)
        if m is None:
            return None
        vals = {}
        for i in range(len(self.path_names)):
            vals[self.path_names[i]] = m.group(i + 1)
        return vals


def _call(call, values):
    r = call(**values)
    if inspect.iscoroutine(r):
        r = run_coro(r)
    return r


def _prep_content(v, route):
    if isinstance(v, BaseModel):
        return v.model_dump(by_alias=True, exclude_unset=route.exclude_unset,
                            exclude_defaults=route.exclude_defaults, exclude_none=route.exclude_none)
    if isinstance(v, (list, tuple, set, frozenset)):
        return [_prep_content(x, route) for x in v]
    if isinstance(v, dict):
        return dict((k, _prep_content(x, route)) for k, x in v.items())
    return v


class _Ctx:
    def __init__(self, route, req, body_obj):
        self.route = route
        self.req = req
        self.cache = {}
        self.cleanups = []
        self.errors = []
        self.body = body_obj      # parsed body (Undefined when absent)
        self.form = None
        self.sub = Response()
        self.sub._sub = True
        self.sub.status_code = None
        self.background = BackgroundTasks()


def _add_errors(ctx, exc, prefix, raw=None, is_body=False):
    for d in exc.errors(include_url=False):
        d2 = dict(d)
        d2["loc"] = tuple(prefix) + tuple(d["loc"])
        # pydantic here reports the coerced number for a constraint failing on a string, and
        # int_from_float for "5.5"; CPython's pydantic keeps the original string / int_parsing.
        if not is_body and len(d["loc"]) == 0 and isinstance(raw, str):
            if d["type"] in _CONSTRAINT_TYPES:
                d2["input"] = raw
        if d["type"] == "int_from_float" and isinstance(d["input"], str):
            d2["type"] = "int_parsing"
        if d["type"] == "model_type" and is_body:
            # FastAPI validates with from_attributes=True, which words this error differently
            d2["type"] = "model_attributes_type"
            d2["msg"] = "Input should be a valid dictionary or object to extract fields from"
            d2.pop("ctx", None)
        ctx.errors.append(d2)


def _missing(ctx, loc):
    ctx.errors.append({"type": "missing", "loc": tuple(loc), "msg": "Field required", "input": None})


def _run_dep(sub, subvals, ctx):
    r = sub.call(**subvals)
    # duck-typed: inspect.isgeneratorfunction is not reliable on the transpiler
    if sub.is_gen or (hasattr(r, "__next__") and hasattr(r, "throw") and hasattr(r, "send")):
        r2 = next(r)
        ctx.cleanups.append(r)
        return r2
    if inspect.iscoroutine(r):
        r = run_coro(r)
    return r


def _solve(dep, ctx):
    req = ctx.req
    values = {}
    for name, sub, use_cache in dep.deps:
        key = sub.call
        if use_cache and key in ctx.cache:
            values[name] = ctx.cache[key]
            continue
        before = len(ctx.errors)
        subvals = _solve(sub, ctx)
        if len(ctx.errors) > before:
            continue
        r = _run_dep(sub, subvals, ctx)
        if use_cache:
            ctx.cache[key] = r
        values[name] = r
    for name, kind in dep.specials.items():
        if kind == "request":
            values[name] = req
        elif kind == "response":
            values[name] = ctx.sub
        else:
            values[name] = ctx.background
    qp = None
    for p in dep.params:
        loc = p.location
        raw = None
        present = False
        if loc == "path":
            raw = req.path_params.get(p.name)
            present = raw is not None
        elif loc == "query":
            if qp is None:
                qp = req.query_params
            if p.is_seq:
                raw = qp.getlist(p.key)
                present = len(raw) > 0
            elif p.key in qp:
                raw = qp.get(p.key)
                present = True
        elif loc == "header":
            if p.is_seq:
                raw = req.headers.getlist(p.key)
                present = len(raw) > 0
            elif p.key in req.headers:
                raw = req.headers.get(p.key)
                present = True
        else:
            ck = req.cookies
            if p.key in ck:
                raw = ck[p.key]
                present = True
        if not present:
            if p.required:
                _missing(ctx, (loc, p.key))
            else:
                values[p.name] = p.default_value()
            continue
        try:
            values[p.name] = p.adapter().validate_python(raw)
        except ValidationError as e:
            _add_errors(ctx, e, (loc, p.key), raw)
    body = dep.body
    if body:
        if dep.form:
            for p in body:
                form = ctx.form
                if form is not None and p.key in form:
                    try:
                        values[p.name] = p.adapter().validate_python(form.get(p.key))
                    except ValidationError as e:
                        _add_errors(ctx, e, ("body", p.key))
                elif p.required:
                    _missing(ctx, ("body", p.key))
                else:
                    values[p.name] = p.default_value()
        else:
            all_body = ctx.route.body_params
            embed = len(all_body) > 1
            for p in body:
                if p.field is not None and getattr(p.field, "embed", None):
                    embed = True
            for p in body:
                if not embed:
                    present = ctx.body is not Undefined and ctx.body is not None
                    val = ctx.body
                    loc = ("body",)
                else:
                    present = isinstance(ctx.body, dict) and p.key in ctx.body
                    val = ctx.body.get(p.key) if present else None
                    loc = ("body", p.key)
                if not present:
                    if p.required:
                        _missing(ctx, loc)
                    else:
                        values[p.name] = p.default_value()
                    continue
                try:
                    values[p.name] = p.adapter().validate_python(val, from_attributes=True)
                except ValidationError as e:
                    _add_errors(ctx, e, loc, val, True)
    return values


def _finish_cleanups(ctx, exc):
    for gen in reversed(ctx.cleanups):
        try:
            if exc is None:
                try:
                    next(gen)
                except StopIteration:
                    pass
            else:
                try:
                    gen.throw(exc)
                except StopIteration:
                    pass
        except Exception as e2:
            exc = e2
    ctx.cleanups = []
    if exc is not None:
        raise exc


def _read_body(route, req):
    """Parse the request body for the route's body parameters."""
    ctx_body = Undefined
    ctx_form = None
    if not route.body_params:
        return ctx_body, ctx_form, None
    raw = req._body
    ct = req.headers.get("content-type")
    if route._dep.form or any(isinstance(p.field, Form) for p in route.body_params):
        ctx_form = {}
        if raw:
            for k, v in QueryParams(raw.decode("utf-8", "replace")).items():
                ctx_form[k] = v
        return ctx_body, ctx_form, None
    if not ct or ct.split(";")[0].strip().lower() == "application/json" or ct.split(";")[0].strip().lower().endswith("+json"):
        if raw:
            try:
                ctx_body = _json.loads(raw.decode("utf-8"))
            except ValueError as e:
                pos = getattr(e, "pos", 0)
                msg = getattr(e, "msg", str(e))
                err = {"type": "json_invalid", "loc": ("body", pos), "msg": "JSON decode error",
                       "input": {}, "ctx": {"error": msg}}
                return ctx_body, ctx_form, err
    else:
        ctx_body = raw
    return ctx_body, ctx_form, None


def _serialize(route, raw):
    if route._adapter is not None:
        value = _prep_content(raw, route)
        try:
            validated = route._adapter.validate_python(value, from_attributes=True)
        except ValidationError as e:
            errs = []
            for d in e.errors(include_url=False):
                d2 = dict(d)
                d2["loc"] = ("response",) + tuple(d["loc"])
                errs.append(d2)
            raise ResponseValidationError(errs, body=raw)
        return route._adapter.dump_python(validated, mode="json", by_alias=route.by_alias,
                                          exclude_unset=route.exclude_unset,
                                          exclude_defaults=route.exclude_defaults,
                                          exclude_none=route.exclude_none)
    return jsonable_encoder(raw)


def run_route(route, req, default_class):
    route._prepare()
    body, form, err = _read_body(route, req)
    if err is not None:
        raise RequestValidationError([err], body=None)
    ctx = _Ctx(route, req, body)
    ctx.form = form
    try:
        for pre in route._pre:
            pv = _solve(pre, ctx)
            if not ctx.errors:
                _run_dep(pre, pv, ctx)
        values = _solve(route._dep, ctx)
        if ctx.errors:
            raise RequestValidationError(list(ctx.errors), body=body if body is not Undefined else None)
        raw = route.endpoint(**values)
        if inspect.iscoroutine(raw):
            raw = run_coro(raw)
        if isinstance(raw, Response):
            resp = raw
        else:
            status = route.status_code
            if ctx.sub.status_code is not None:
                status = ctx.sub.status_code
            if status is None:
                status = 200
            content = _serialize(route, raw)
            rc = route.response_class or default_class
            if status in (204, 304) or status < 200:
                resp = Response(status_code=status)
            else:
                resp = rc(content, status_code=status)
        for k, v in ctx.sub.headers.items():
            if k == "content-length":
                continue
            resp.headers.append(k, v)
        if ctx.background.tasks:
            resp.background = ctx.background
        _finish_cleanups(ctx, None)
    except Exception as exc:
        if ctx.cleanups:
            _finish_cleanups(ctx, exc)
        raise
    return resp


def _verb_decorator(self, methods, path, kw):
    def deco(f):
        self.add_api_route(path, f, methods=methods, **kw)
        return f
    return deco


class APIRouter:
    def __init__(self, *, prefix="", tags=None, dependencies=None, default_response_class=None,
                 responses=None, deprecated=None, include_in_schema=True, **kw):
        self.prefix = prefix
        self.tags = list(tags or [])
        self.dependencies = list(dependencies or [])
        self.default_response_class = default_response_class
        self.responses = responses or {}
        self.deprecated = deprecated
        self.routes = []

    def add_api_route(self, path, endpoint, *, methods=None, **kw):
        kw = dict(kw)
        kw["tags"] = list(self.tags) + list(kw.get("tags") or [])
        kw["dependencies"] = list(self.dependencies) + list(kw.get("dependencies") or [])
        if kw.get("response_class") is None:
            kw["response_class"] = self.default_response_class
        if kw.get("deprecated") is None:
            kw["deprecated"] = self.deprecated
        self.routes.append(APIRoute(self.prefix + path, endpoint, methods or ["GET"], **kw))

    def api_route(self, path, *, methods=None, **kw):
        return _verb_decorator(self, methods or ["GET"], path, kw)

    def get(self, path, **kw):
        return _verb_decorator(self, ["GET"], path, kw)

    def post(self, path, **kw):
        return _verb_decorator(self, ["POST"], path, kw)

    def put(self, path, **kw):
        return _verb_decorator(self, ["PUT"], path, kw)

    def delete(self, path, **kw):
        return _verb_decorator(self, ["DELETE"], path, kw)

    def patch(self, path, **kw):
        return _verb_decorator(self, ["PATCH"], path, kw)

    def head(self, path, **kw):
        return _verb_decorator(self, ["HEAD"], path, kw)

    def options(self, path, **kw):
        return _verb_decorator(self, ["OPTIONS"], path, kw)

    def include_router(self, router, *, prefix="", tags=None, dependencies=None,
                       default_response_class=None, responses=None, deprecated=None,
                       include_in_schema=True, **kw):
        for r in router.routes:
            n = APIRoute(prefix + r.path, r.endpoint, r.methods, name=r.name,
                         status_code=r.status_code, response_model=r.response_model,
                         response_class=r.response_class or default_response_class or self.default_response_class,
                         tags=list(tags or []) + list(r.tags),
                         dependencies=list(dependencies or []) + list(r.dependencies),
                         summary=r.summary, description=r.description,
                         deprecated=r.deprecated if r.deprecated is not None else deprecated,
                         operation_id=r.operation_id,
                         include_in_schema=r.include_in_schema and include_in_schema,
                         responses=r.responses,
                         response_model_exclude_unset=r.exclude_unset,
                         response_model_exclude_defaults=r.exclude_defaults,
                         response_model_exclude_none=r.exclude_none,
                         response_model_by_alias=r.by_alias)
            # a route registered before its response model was inferred keeps the sentinel
            self.routes.append(n)
