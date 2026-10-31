# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""CORSMiddleware, written from Starlette's documented behaviour."""
import re

from starlette.responses import PlainTextResponse

ALL_METHODS = ("DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT", "QUERY")
SAFELISTED_HEADERS = {"Accept", "Accept-Language", "Content-Language", "Content-Type"}


class CORSMiddleware:
    # Request based (see starlette.middleware); not wrapped as ASGI.
    _cn1_request_based = True

    def __init__(self, app, allow_origins=(), allow_methods=("GET",), allow_headers=(),
                 allow_credentials=False, allow_origin_regex=None, expose_headers=(), max_age=600,
                 allow_private_network=False):
        if "*" in allow_methods:
            allow_methods = ALL_METHODS
        self.app = app
        self.allow_origins = allow_origins
        self.allow_methods = allow_methods
        self.allow_all_origins = "*" in allow_origins
        self.allow_all_headers = "*" in allow_headers
        self.allow_origin_regex = re.compile(allow_origin_regex) if allow_origin_regex is not None else None
        self.allow_credentials = allow_credentials
        self.preflight_explicit_allow_origin = not self.allow_all_origins or allow_credentials
        simple = {}
        if self.allow_all_origins:
            simple["Access-Control-Allow-Origin"] = "*"
        if allow_credentials:
            simple["Access-Control-Allow-Credentials"] = "true"
        if expose_headers:
            simple["Access-Control-Expose-Headers"] = ", ".join(expose_headers)
        pre = {"Vary": "Origin, Access-Control-Request-Method, Access-Control-Request-Headers, "
                       "Access-Control-Request-Private-Network"}
        if not self.preflight_explicit_allow_origin:
            pre["Access-Control-Allow-Origin"] = "*"
        pre["Access-Control-Allow-Methods"] = ", ".join(allow_methods)
        pre["Access-Control-Max-Age"] = str(max_age)
        allowed = sorted(SAFELISTED_HEADERS | set(allow_headers))
        if allowed and not self.allow_all_headers:
            pre["Access-Control-Allow-Headers"] = ", ".join(allowed)
        if allow_credentials:
            pre["Access-Control-Allow-Credentials"] = "true"
        self.simple_headers = simple
        self.preflight_headers = pre
        self.allow_headers = [h.lower() for h in allowed]

    def is_allowed_origin(self, origin):
        if self.allow_all_origins:
            return True
        if self.allow_origin_regex is not None and self.allow_origin_regex.fullmatch(origin):
            return True
        return origin in self.allow_origins

    async def __call__(self, request):
        if request.scope.get("type") == "websocket":
            # Starlette's middleware passes a websocket scope straight through.
            return await self.app(request)
        headers = request.headers
        origin = headers.get("origin")
        if origin is None:
            resp = await self.app(request)
            resp.headers.add_vary_header("Origin")
            return resp
        if request.method == "OPTIONS" and "access-control-request-method" in headers:
            return self.preflight_response(headers)
        resp = await self.app(request)
        resp.headers.update(self.simple_headers)
        if self.allow_all_origins and self.allow_credentials:
            resp.headers["Access-Control-Allow-Origin"] = origin
        elif not self.allow_all_origins and self.is_allowed_origin(origin):
            resp.headers["Access-Control-Allow-Origin"] = origin
        resp.headers.add_vary_header("Origin")
        return resp

    def preflight_response(self, request_headers):
        origin = request_headers["origin"]
        method = request_headers["access-control-request-method"]
        requested = request_headers.get("access-control-request-headers")
        headers = dict(self.preflight_headers)
        failures = []
        if self.is_allowed_origin(origin):
            if self.preflight_explicit_allow_origin:
                headers["Access-Control-Allow-Origin"] = origin
        else:
            failures.append("origin")
        if method not in self.allow_methods:
            failures.append("method")
        if self.allow_all_headers and requested is not None:
            headers["Access-Control-Allow-Headers"] = requested
        elif requested is not None:
            for h in [h.lower() for h in requested.split(",")]:
                if h.strip() not in self.allow_headers:
                    failures.append("headers")
                    break
        if failures:
            return PlainTextResponse("Disallowed CORS " + ", ".join(failures), status_code=400, headers=headers)
        return PlainTextResponse("OK", status_code=200, headers=headers)
