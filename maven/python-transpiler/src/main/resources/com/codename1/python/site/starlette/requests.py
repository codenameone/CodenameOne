# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The request object handed to endpoints."""
import json as _json

from starlette.datastructures import Headers, QueryParams, URL, State


def _parse_cookies(header):
    out = {}
    if not header:
        return out
    for part in header.split(";"):
        if "=" in part:
            k, v = part.split("=", 1)
            k = k.strip()
            v = v.strip()
            if len(v) >= 2 and v[0] == '"' and v[-1] == '"':
                v = v[1:-1]
            if k and k not in out:
                out[k] = v
    return out


class HTTPConnection:
    def __init__(self, method, path, query, headers, body, app=None):
        self.method = method
        self._path = path
        self._query = query or ""
        self.headers = Headers(headers)
        self._body = bytes(body or b"")
        self.app = app
        self.path_params = {}
        self.state = State()
        self.scope = {"type": "http", "method": method, "path": path}
        self.client = ("testclient", 50000)

    @property
    def url(self):
        host = self.headers.get("host", "testserver")
        return URL("http://" + host + self._path + ("?" + self._query if self._query else ""))

    @property
    def base_url(self):
        return URL("http://" + self.headers.get("host", "testserver") + "/")

    @property
    def query_params(self):
        return QueryParams(self._query)

    @property
    def cookies(self):
        return _parse_cookies(self.headers.get("cookie"))


class Request(HTTPConnection):
    async def body(self):
        return self._body

    async def json(self):
        return _json.loads(self._body.decode("utf-8"))

    async def form(self):
        return QueryParams(self._body.decode("utf-8"))

    async def is_disconnected(self):
        return False
