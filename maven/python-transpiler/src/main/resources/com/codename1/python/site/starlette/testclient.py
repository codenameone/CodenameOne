# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""A test client that calls the app's pure dispatch core."""
import gzip as _gzip
import json as _json
from urllib.parse import urlencode, urljoin

from starlette.datastructures import Headers, MutableHeaders, URL
from starlette.exceptions import phrase


class TestResponse:
    __test__ = False

    def __init__(self, status_code, headers, content, url):
        self.status_code = status_code
        self.headers = Headers(headers)
        self.content = content
        self.url = url
        self.history = []
        self.encoding = "utf-8"

    @property
    def text(self):
        return self.content.decode("utf-8", "replace")

    @property
    def reason_phrase(self):
        return phrase(self.status_code)

    @property
    def is_success(self):
        return 200 <= self.status_code < 300

    @property
    def is_redirect(self):
        return self.status_code in (301, 302, 303, 307, 308) and "location" in self.headers

    @property
    def is_error(self):
        return self.status_code >= 400

    def json(self, **kw):
        return _json.loads(self.content.decode("utf-8"))

    def raise_for_status(self):
        if self.status_code >= 400:
            raise RuntimeError("HTTP %d for url %s" % (self.status_code, self.url))
        return self

    def __repr__(self):
        return "<Response [%d %s]>" % (self.status_code, self.reason_phrase)


class TestClient:
    __test__ = False

    def __init__(self, app, base_url="http://testserver", raise_server_exceptions=True,
                 follow_redirects=True, headers=None, cookies=None, **kw):
        self.app = app
        self.base_url = base_url
        self.raise_server_exceptions = raise_server_exceptions
        self.follow_redirects = follow_redirects
        self.headers = MutableHeaders({"accept": "*/*", "accept-encoding": "gzip, deflate",
                                       "connection": "keep-alive", "user-agent": "testclient"})
        if headers:
            self.headers.update(headers)
        self.cookies = dict(cookies or {})
        self._entered = False

    def __enter__(self):
        if hasattr(self.app, "_startup"):
            self.app._startup()
        self._entered = True
        return self

    def __exit__(self, *a):
        if hasattr(self.app, "_shutdown"):
            self.app._shutdown()
        return False

    def websocket_connect(self, url, subprotocols=None, headers=None, **kw):
        if "://" not in url:
            url = self.base_url.rstrip("/") + "/" + url.lstrip("/")
        u = URL(url)
        hdrs = MutableHeaders([("host", u.netloc), ("accept", "*/*"), ("accept-encoding", "gzip, deflate"),
                               ("user-agent", "testclient"), ("connection", "upgrade"),
                               ("sec-websocket-key", "testserver=="), ("sec-websocket-version", "13")])
        if subprotocols:
            hdrs["sec-websocket-protocol"] = ", ".join(subprotocols)
        if headers:
            hdrs.update(headers)
        return WebSocketTestSession(self.app, u.path, u.query, hdrs.items())

    def get(self, url, **kw):
        return self.request("GET", url, **kw)

    def head(self, url, **kw):
        return self.request("HEAD", url, **kw)

    def options(self, url, **kw):
        return self.request("OPTIONS", url, **kw)

    def post(self, url, **kw):
        return self.request("POST", url, **kw)

    def put(self, url, **kw):
        return self.request("PUT", url, **kw)

    def patch(self, url, **kw):
        return self.request("PATCH", url, **kw)

    def delete(self, url, **kw):
        return self.request("DELETE", url, **kw)

    def request(self, method, url, params=None, headers=None, json=None, data=None, content=None,
                cookies=None, follow_redirects=None, **kw):
        if follow_redirects is None:
            follow_redirects = self.follow_redirects
        hist = []
        for _ in range(21):
            resp = self._send(method, url, params, headers, json, data, content, cookies)
            if follow_redirects and resp.is_redirect:
                hist.append(resp)
                url = urljoin(str(resp.url), resp.headers["location"])
                if resp.status_code in (301, 302, 303) and method != "HEAD":
                    method = "GET"
                    json = data = content = None
                params = None
                continue
            resp.history = hist
            return resp
        raise RuntimeError("too many redirects")

    def _send(self, method, url, params, headers, json, data, content, cookies):
        if "://" not in url:
            url = urljoin(self.base_url.rstrip("/") + "/", url.lstrip("/") if not url.startswith("/") else url[1:])
        u = URL(url)
        query = u.query
        if params:
            extra = urlencode(params, doseq=True) if isinstance(params, dict) else str(params)
            query = extra if not query else query + "&" + extra
        hdrs = MutableHeaders(self.headers.items())
        hdrs["host"] = u.netloc
        if headers:
            hdrs.update(headers)
        body = b""
        if json is not None:
            body = _json.dumps(json, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
            hdrs["content-type"] = "application/json"
        elif data is not None:
            if isinstance(data, dict):
                body = urlencode(data, doseq=True).encode("utf-8")
                hdrs["content-type"] = "application/x-www-form-urlencoded"
            elif isinstance(data, (bytes, str)):
                body = data.encode("utf-8") if isinstance(data, str) else data
        elif content is not None:
            body = content.encode("utf-8") if isinstance(content, str) else bytes(content)
        if body or method in ("POST", "PUT", "PATCH"):
            hdrs["content-length"] = str(len(body))
        jar = dict(self.cookies)
        if cookies:
            jar.update(cookies)
        if jar and "cookie" not in hdrs:
            hdrs["cookie"] = "; ".join(k + "=" + v for k, v in jar.items())
        try:
            status, rh, rb = self.app._dispatch_for_client(method, u.path, query, hdrs.items(), body,
                                                           self.raise_server_exceptions)
        except Exception:
            raise
        for k, v in rh:
            if k.lower() == "content-encoding" and v.strip().lower() == "gzip":
                rb = _gzip.decompress(rb)
        resp = TestResponse(status, rh, rb, url if not query else url.split("?")[0] + "?" + query)
        for k, v in resp.headers.multi_items():
            if k == "set-cookie":
                kv = v.split(";", 1)[0]
                if "=" in kv:
                    ck, cv = kv.split("=", 1)
                    if "max-age=0" in v.lower():
                        self.cookies.pop(ck, None)
                    else:
                        self.cookies[ck] = cv
        return resp


class WebSocketTestSession:
    __test__ = False

    def __init__(self, app, path, query, headers):
        self.app = app
        self.path = path
        self.query = query
        self.headers = headers
        self.accepted_subprotocol = None
        self.extra_headers = None
        self._out = []
        self._session = None

    def _sink(self, message):
        self._out.append(message)

    def _raise_error(self):
        s = self._session
        if s is not None and s.error is not None:
            e = s.error
            s.error = None
            raise e

    def __enter__(self):
        self._session = self.app.websocket_open(self.path, self.query, self.headers, self._sink)
        self._raise_error()
        m = self.receive()
        if m["type"] == "websocket.close":
            from starlette.websockets import WebSocketDisconnect
            raise WebSocketDisconnect(m.get("code", 1000), m.get("reason", ""))
        self.accepted_subprotocol = m.get("subprotocol")
        self.extra_headers = m.get("headers")
        return self

    def __exit__(self, *a):
        s = self._session
        if not s.done:
            s.deliver({"type": "websocket.disconnect", "code": 1000})
        self._raise_error()
        return False

    def send(self, message):
        self._session.deliver(message)
        self._raise_error()

    def send_text(self, data):
        self.send({"type": "websocket.receive", "text": data})

    def send_bytes(self, data):
        self.send({"type": "websocket.receive", "bytes": bytes(data)})

    def send_json(self, data, mode="text"):
        text = _json.dumps(data, separators=(",", ":"), ensure_ascii=False)
        if mode == "text":
            self.send({"type": "websocket.receive", "text": text})
        else:
            self.send({"type": "websocket.receive", "bytes": text.encode("utf-8")})

    def close(self, code=1000, reason=None):
        self.send({"type": "websocket.disconnect", "code": code, "reason": reason})

    def receive(self):
        self._raise_error()
        if not self._out:
            raise RuntimeError("the websocket endpoint is waiting for a message and sent nothing")
        return self._out.pop(0)

    def _raise_on_close(self, m):
        if m["type"] == "websocket.close":
            from starlette.websockets import WebSocketDisconnect
            raise WebSocketDisconnect(m.get("code", 1000), m.get("reason", ""))

    def receive_text(self):
        m = self.receive()
        self._raise_on_close(m)
        return m["text"]

    def receive_bytes(self):
        m = self.receive()
        self._raise_on_close(m)
        return m["bytes"]

    def receive_json(self, mode="text"):
        m = self.receive()
        self._raise_on_close(m)
        if mode == "text":
            return _json.loads(m["text"])
        return _json.loads(m["bytes"].decode("utf-8"))
