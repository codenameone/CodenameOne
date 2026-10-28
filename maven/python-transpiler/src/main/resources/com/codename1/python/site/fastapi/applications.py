# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The FastAPI application: a pure dispatch core plus OpenAPI generation."""
import inspect
import re as _re
from urllib.parse import quote, unquote

from pydantic import BaseModel, TypeAdapter

from starlette.datastructures import State
from starlette.exceptions import HTTPException as StarletteHTTPException
from starlette.requests import Request
from starlette.responses import JSONResponse, PlainTextResponse, RedirectResponse, Response

from fastapi.exceptions import RequestValidationError, ResponseValidationError
from fastapi.params import Undefined
from fastapi.routing import (APIRouter, APIRoute, APIWebSocketRoute, run_coro, run_route, run_websocket,
                             _all_params, _all_body, _analyze)
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.websockets import WebSocket, WebSocketDisconnect, _Session


def _http_exception_handler(request, exc):
    headers = getattr(exc, "headers", None)
    if exc.status_code in (204, 304) or exc.status_code < 200:
        return Response(status_code=exc.status_code, headers=headers)
    return JSONResponse({"detail": exc.detail}, status_code=exc.status_code, headers=headers)


def _validation_handler(request, exc):
    return JSONResponse({"detail": exc.errors()}, status_code=422)


class FastAPI(APIRouter):
    def __init__(self, *, debug=False, title="FastAPI", summary=None, description="", version="0.1.0",
                 openapi_url="/openapi.json", docs_url="/docs", redoc_url="/redoc",
                 default_response_class=None, dependencies=None, exception_handlers=None,
                 lifespan=None, tags=None, root_path="", middleware=None, **kw):
        APIRouter.__init__(self, dependencies=dependencies, default_response_class=default_response_class)
        self.debug = debug
        self.title = title
        self.summary = summary
        self.description = description
        self.version = version
        self.openapi_url = openapi_url
        self.docs_url = docs_url
        self.redoc_url = redoc_url
        self.root_path = root_path
        self.router = self
        self.state = State()
        self.extra = kw
        self.dependency_overrides = {}
        self.exception_handlers = {}
        self.exception_handlers[StarletteHTTPException] = _http_exception_handler
        self.exception_handlers[RequestValidationError] = _validation_handler
        if exception_handlers:
            for k, v in exception_handlers.items():
                self.exception_handlers[k] = v
        self._startup_handlers = []
        self._shutdown_handlers = []
        self._lifespan = lifespan
        self._lifespan_cm = None
        self._openapi = None
        self.user_middleware = []
        self._stack = None
        for m in middleware or []:
            self.user_middleware.append(m)

    # -- registration
    def add_middleware(self, middleware_class, *args, **kwargs):
        if self._stack is not None:
            raise RuntimeError("Cannot add middleware after an application has started")
        from starlette.middleware import Middleware
        self.user_middleware.insert(0, Middleware(middleware_class, *args, **kwargs))

    def middleware(self, middleware_type):
        if middleware_type != "http":
            raise ValueError("Currently only middleware(\"http\") is supported.")

        def deco(f):
            self.add_middleware(BaseHTTPMiddleware, dispatch=f)
            return f
        return deco

    def _build_stack(self):
        if self._stack is None:
            app = self._core
            for m in reversed(self.user_middleware):
                cls, args, kwargs = m
                app = cls(app, *args, **kwargs)
            self._stack = app
        return self._stack

    # -- registration (handlers)
    def exception_handler(self, exc_class_or_status):
        def deco(f):
            self.exception_handlers[exc_class_or_status] = f
            return f
        return deco

    def add_exception_handler(self, exc_class_or_status, handler):
        self.exception_handlers[exc_class_or_status] = handler

    def on_event(self, kind):
        def deco(f):
            if kind == "startup":
                self._startup_handlers.append(f)
            else:
                self._shutdown_handlers.append(f)
            return f
        return deco

    def include_router(self, router, **kw):
        APIRouter.include_router(self, router, **kw)
        self._openapi = None

    def add_api_route(self, path, endpoint, **kw):
        APIRouter.add_api_route(self, path, endpoint, **kw)
        self._openapi = None

    def _startup(self):
        if self._lifespan is not None:
            self._lifespan_cm = self._lifespan(self)
            if hasattr(self._lifespan_cm, "__aenter__"):
                run_coro(self._lifespan_cm.__aenter__())
            else:
                self._lifespan_cm.__enter__()
        for f in self._startup_handlers:
            r = f()
            if inspect.iscoroutine(r):
                run_coro(r)

    def _shutdown(self):
        for f in self._shutdown_handlers:
            r = f()
            if inspect.iscoroutine(r):
                run_coro(r)
        if self._lifespan_cm is not None:
            cm = self._lifespan_cm
            self._lifespan_cm = None
            if hasattr(cm, "__aexit__"):
                run_coro(cm.__aexit__(None, None, None))
            else:
                cm.__exit__(None, None, None)

    # -- dispatch
    def _find_handler(self, exc):
        if isinstance(exc, StarletteHTTPException) and exc.status_code in self.exception_handlers:
            return self.exception_handlers[exc.status_code]
        for cls in type(exc).__mro__:
            if cls in self.exception_handlers:
                return self.exception_handlers[cls]
        return None

    async def _handle_exception(self, req, exc):
        h = self._find_handler(exc)
        if h is None:
            raise exc
        r = h(req, exc)
        if inspect.iscoroutine(r):
            r = await r
        return r

    def _route(self, req):
        path = req._path
        method = req.method
        partial = None
        for r in self.routes:
            if isinstance(r, APIWebSocketRoute):
                continue
            vals = r.match(path)
            if vals is None:
                continue
            if method in r.methods:
                req.path_params = vals
                return r
            if partial is None:
                partial = r
        if partial is not None:
            raise StarletteHTTPException(405, "Method Not Allowed",
                                         {"Allow": ", ".join(sorted(partial.methods))})
        if path.endswith("/") and len(path) > 1:
            alt = path.rstrip("/")
        else:
            alt = path + "/"
        for r in self.routes:
            if not isinstance(r, APIWebSocketRoute) and r.match(alt) is not None:
                host = req.headers.get("host", "testserver")
                loc = "http://" + host + quote(alt)
                if req._query:
                    loc += "?" + req._query
                return RedirectResponse(loc, status_code=307)
        raise StarletteHTTPException(404, "Not Found")

    async def _core(self, req):
        if req.scope.get("type") == "websocket":
            await self._run_ws(req._route, req)
            return None
        try:
            if self.openapi_url and req._path == self.openapi_url and req.method == "GET":
                resp = JSONResponse(self.openapi())
            else:
                route = self._route(req)
                if isinstance(route, Response):
                    resp = route
                else:
                    resp = await run_route(route, req, self.default_response_class or JSONResponse)
        except Exception as exc:
            resp = await self._handle_exception(req, exc)
        return resp

    def _handle(self, method, path, query, headers, body):
        req = Request(method.upper(), unquote(path), query, headers, body, self)
        req.app = self
        resp = run_coro(self._build_stack()(req))
        if resp.background is not None:
            resp.background()
        return resp

    def _dispatch_for_client(self, method, path, query, headers, body, raise_exceptions):
        try:
            resp = self._handle(method, path, query, headers, body)
        except Exception:
            if raise_exceptions:
                raise
            resp = PlainTextResponse("Internal Server Error", status_code=500)
        data = b"" if method.upper() == "HEAD" else resp.body
        return resp.status_code, resp.headers.items(), data

    def dispatch(self, method, path, query, headers, body):
        return self._dispatch_for_client(method, path, query, headers, body, False)

    # -- websockets
    def websocket_route(self, path):
        """The websocket route serving ``path`` and its path parameters, or (None, None)."""
        path = unquote(path)
        for r in self.routes:
            if isinstance(r, APIWebSocketRoute):
                vals = r.match(path)
                if vals is not None:
                    return r, vals
        return None, None

    def websocket_open(self, path, query, headers, sink, on_done=None):
        """Starts a websocket connection on the shared loop; returns its session.

        ``sink(message)`` receives the ASGI-shaped messages the endpoint sends
        (``websocket.accept``, ``websocket.send``, ``websocket.close``, and
        ``websocket.http.response`` when middleware refuses the handshake
        with a response), on the loop thread. The caller feeds client
        messages with ``session.deliver`` from any thread. The connection
        goes through the middleware stack first, as Starlette's does.
        """
        route, vals = self.websocket_route(path)
        session = _Session(sink, on_done)
        ws = WebSocket(unquote(path), query, headers, session, self)
        ws.app = self
        ws.path_params = vals or {}
        ws._route = route
        session.start(self._run_ws_stack(ws))
        return session

    async def _run_ws_stack(self, ws):
        from starlette.websockets import WebSocketState
        resp = await self._build_stack()(ws)
        if isinstance(resp, Response) and ws.application_state == WebSocketState.CONNECTING:
            # A middleware answered the handshake with an HTTP response
            # (TrustedHostMiddleware's 400): the upgrade is refused with it.
            ws.application_state = WebSocketState.DISCONNECTED
            ws._session.outgoing({"type": "websocket.http.response", "status": resp.status_code,
                                  "headers": list(resp.headers.items()), "body": resp.body})

    async def _run_ws(self, route, ws):
        from fastapi.exceptions import WebSocketException, WebSocketRequestValidationError
        if route is None:
            await ws.close(1000)
            return
        try:
            await run_websocket(route, ws)
        except WebSocketRequestValidationError:
            await ws.close(1008)
        except WebSocketException as e:
            await ws.close(e.code, e.reason)

    # -- OpenAPI
    def openapi(self):
        if self._openapi is None:
            self._openapi = self._build_openapi()
        return self._openapi

    def _build_openapi(self):
        info = {"title": self.title, "version": self.version}
        if self.summary:
            info["summary"] = self.summary
        if self.description:
            info["description"] = self.description
        paths = {}
        schemas = {}
        need_validation = False
        for r in self.routes:
            if isinstance(r, APIWebSocketRoute) or not r.include_in_schema:
                continue
            r._prepare()
            item = paths.setdefault(_re.sub(":[a-z]+\\}", "}", r.path), {})
            for m in sorted(r.methods):
                op = {}
                if r.tags:
                    op["tags"] = list(r.tags)
                op["summary"] = r.summary or r.name.replace("_", " ").title()
                if r.description:
                    op["description"] = r.description
                elif r.endpoint.__doc__:
                    op["description"] = inspect.cleandoc(r.endpoint.__doc__)
                op["operationId"] = r.operation_id or (r.name + r.path.replace("{", "_").replace("}", "_")
                                                       .replace("/", "_").replace(":", "_") + "_" + m.lower())
                if r.deprecated:
                    op["deprecated"] = True
                params = []
                allp = _all_params(r._dep)
                for pre in r._pre:
                    allp.extend(_all_params(pre))
                for p in allp:
                    if p.field is not None and not p.field.include_in_schema:
                        continue
                    s = self._schema(p.tp, schemas, p)
                    d = {"name": p.key, "in": p.location, "required": p.required, "schema": s}
                    if p.field is not None and p.field.description:
                        d["description"] = p.field.description
                    params.append(d)
                if params:
                    op["parameters"] = params
                body = _all_body(r._dep)
                if body:
                    need_validation = True
                    if len(body) == 1 and not (body[0].field is not None and getattr(body[0].field, "embed", None)):
                        bs = self._schema(body[0].tp, schemas, None)
                        required = body[0].required
                    else:
                        props = {}
                        req_names = []
                        for p in body:
                            props[p.key] = self._schema(p.tp, schemas, p)
                            if p.required:
                                req_names.append(p.key)
                        name = "Body_%s_%s_%s" % (r.name, r.path.replace("/", "_").replace("{", "").replace("}", "").strip("_"), m.lower())
                        sch = {"properties": props, "type": "object", "title": name.replace("_", " ")}
                        if req_names:
                            sch["required"] = req_names
                        schemas[name] = sch
                        bs = {"$ref": "#/components/schemas/" + name}
                        required = True
                    ct = "application/json"
                    if body[0].field is not None and getattr(body[0].field, "media_type", None):
                        ct = body[0].field.media_type
                    op["requestBody"] = {"content": {ct: {"schema": bs}}, "required": required}
                status = str(r.status_code or 200)
                if r._adapter is not None:
                    rs = self._schema(r.response_model, schemas, None)
                else:
                    rs = {}
                ct = "application/json"
                rc = r.response_class
                if rc is not None and getattr(rc, "media_type", None):
                    ct = rc.media_type
                    if rc is not JSONResponse and r._adapter is None:
                        rs = {"type": "string"}
                resp = {"description": "Successful Response", "content": {ct: {"schema": rs}}}
                responses = {status: resp}
                for code, extra in r.responses.items():
                    e = dict(extra) if isinstance(extra, dict) else {}
                    e.setdefault("description", "Additional Response")
                    responses[str(code)] = e
                if allp or body:
                    need_validation = True
                    responses["422"] = {"description": "Validation Error", "content": {"application/json": {
                        "schema": {"$ref": "#/components/schemas/HTTPValidationError"}}}}
                op["responses"] = responses
                item[m.lower()] = op
        if need_validation:
            schemas["HTTPValidationError"] = {"properties": {"detail": {"items": {"$ref": "#/components/schemas/ValidationError"},
                                                                         "type": "array", "title": "Detail"}},
                                              "type": "object", "title": "HTTPValidationError"}
            schemas["ValidationError"] = {"properties": {
                "loc": {"items": {"anyOf": [{"type": "string"}, {"type": "integer"}]}, "type": "array", "title": "Location"},
                "msg": {"type": "string", "title": "Message"},
                "type": {"type": "string", "title": "Error Type"},
                "input": {"title": "Input"},
                "ctx": {"type": "object", "title": "Context"}},
                "type": "object", "required": ["loc", "msg", "type"], "title": "ValidationError"}
        out = {"openapi": "3.1.0", "info": info, "paths": paths}
        if schemas:
            out["components"] = {"schemas": dict((k, schemas[k]) for k in sorted(schemas))}
        return out

    def _schema(self, tp, schemas, p):
        defs = {}
        try:
            s = dict(TypeAdapter(tp).json_schema())
        except Exception:
            s = {}
        defs = s.pop("$defs", {})
        for k, v in defs.items():
            schemas[k] = self._fix_refs(v)
        s = self._fix_refs(s)
        if p is not None and p.field is not None:
            if p.field.title:
                s["title"] = p.field.title
            for k in ("gt", "ge", "lt", "le", "min_length", "max_length", "pattern", "multiple_of"):
                v = getattr(p.field, k, None)
                if v is not None:
                    s[{"gt": "exclusiveMinimum", "ge": "minimum", "lt": "exclusiveMaximum", "le": "maximum",
                       "min_length": "minLength", "max_length": "maxLength", "pattern": "pattern",
                       "multiple_of": "multipleOf"}[k]] = v
            if not p.required and p.default is not Undefined:
                s["default"] = p.default
        if p is not None and "title" not in s and "$ref" not in s:
            s["title"] = p.key.replace("-", " ").replace("_", " ").title()
        return s

    def _fix_refs(self, v):
        if isinstance(v, dict):
            out = {}
            for k, x in v.items():
                if k == "$ref" and isinstance(x, str):
                    out[k] = x.replace("#/$defs/", "#/components/schemas/")
                else:
                    out[k] = self._fix_refs(x)
            return out
        if isinstance(v, list):
            return [self._fix_refs(x) for x in v]
        return v
