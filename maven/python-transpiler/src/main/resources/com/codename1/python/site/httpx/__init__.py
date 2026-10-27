# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The httpx API over the platform's HTTP client.

`httpx.get` and the other verbs, `Client` and `AsyncClient` (base URL,
default headers, params and timeout), `Response`, `Request`, `URL`,
`Headers` and httpx's exception hierarchy. As in httpx, redirects are not
followed unless `follow_redirects=True`. Streaming, HTTP/2, transports and
file uploads are not provided. The asynchronous client runs each request
to completion when it is awaited.
"""

import datetime as _datetime
import json as _json
from urllib.parse import urljoin as _urljoin, urlsplit as _urlsplit, parse_qsl as _parse_qsl

import _cn1_http

__version__ = "0.28.1"

_REASONS = {
    100: "Continue", 101: "Switching Protocols", 200: "OK", 201: "Created", 202: "Accepted",
    204: "No Content", 206: "Partial Content", 301: "Moved Permanently", 302: "Found", 303: "See Other",
    304: "Not Modified", 307: "Temporary Redirect", 308: "Permanent Redirect", 400: "Bad Request",
    401: "Unauthorized", 403: "Forbidden", 404: "Not Found", 405: "Method Not Allowed", 409: "Conflict",
    410: "Gone", 418: "I'm a teapot", 422: "Unprocessable Entity", 429: "Too Many Requests",
    500: "Internal Server Error", 501: "Not Implemented", 502: "Bad Gateway", 503: "Service Unavailable",
    504: "Gateway Timeout"}


class HTTPError(Exception):
    def __init__(self, message):
        super().__init__(message)
        self._request = None

    @property
    def request(self):
        if self._request is None:
            raise RuntimeError("The .request property has not been set.")
        return self._request

    @request.setter
    def request(self, request):
        self._request = request


class RequestError(HTTPError):
    def __init__(self, message, *, request=None):
        super().__init__(message)
        self._request = request


class TransportError(RequestError):
    pass


class TimeoutException(TransportError):
    pass


class ConnectTimeout(TimeoutException):
    pass


class ReadTimeout(TimeoutException):
    pass


class WriteTimeout(TimeoutException):
    pass


class PoolTimeout(TimeoutException):
    pass


class NetworkError(TransportError):
    pass


class ConnectError(NetworkError):
    pass


class ReadError(NetworkError):
    pass


class ProtocolError(TransportError):
    pass


class UnsupportedProtocol(TransportError):
    pass


class DecodingError(RequestError):
    pass


class TooManyRedirects(RequestError):
    pass


class HTTPStatusError(HTTPError):
    def __init__(self, message, *, request, response):
        super().__init__(message)
        self.request = request
        self.response = response


class InvalidURL(Exception):
    pass


class StreamError(RuntimeError):
    pass


class codes:
    CONTINUE = 100
    OK = 200
    CREATED = 201
    ACCEPTED = 202
    NO_CONTENT = 204
    MOVED_PERMANENTLY = 301
    FOUND = 302
    SEE_OTHER = 303
    NOT_MODIFIED = 304
    TEMPORARY_REDIRECT = 307
    PERMANENT_REDIRECT = 308
    BAD_REQUEST = 400
    UNAUTHORIZED = 401
    FORBIDDEN = 403
    NOT_FOUND = 404
    METHOD_NOT_ALLOWED = 405
    CONFLICT = 409
    IM_A_TEAPOT = 418
    UNPROCESSABLE_ENTITY = 422
    TOO_MANY_REQUESTS = 429
    INTERNAL_SERVER_ERROR = 500
    BAD_GATEWAY = 502
    SERVICE_UNAVAILABLE = 503
    GATEWAY_TIMEOUT = 504

    @staticmethod
    def get_reason_phrase(value):
        return _REASONS.get(value, "")


class Headers(_cn1_http.Headers):
    def __init__(self, headers=None, encoding=None):
        super().__init__(headers)
        self.encoding = encoding or "ascii"

    def keys(self):
        return [k.lower() for k in super().keys()]

    def items(self):
        return [(k, self[k]) for k in self.keys()]

    @property
    def raw(self):
        return [(k.lower().encode("ascii"), v.encode("ascii")) for k, v in self._items]

    def __repr__(self):
        return f"Headers({dict(self.items())!r})"


class QueryParams:
    def __init__(self, pairs=()):
        self._pairs = list(pairs)

    def get(self, key, default=None):
        for k, v in self._pairs:
            if k == key:
                return v
        return default

    def get_list(self, key):
        return [v for k, v in self._pairs if k == key]

    def __getitem__(self, key):
        for k, v in self._pairs:
            if k == key:
                return v
        raise KeyError(key)

    def __contains__(self, key):
        return any(k == key for k, _ in self._pairs)

    def keys(self):
        return list(dict(self._pairs).keys())

    def items(self):
        return list(dict(self._pairs).items())

    def multi_items(self):
        return list(self._pairs)

    def __len__(self):
        return len(self.keys())

    def __iter__(self):
        return iter(self.keys())

    def __str__(self):
        from urllib.parse import urlencode
        return urlencode(self._pairs)

    def __repr__(self):
        return f"QueryParams({str(self)!r})"


class URL:
    def __init__(self, url="", params=None):
        if isinstance(url, URL):
            url = str(url)
        if params:
            url = _cn1_http.with_params(url, params)
        self._url = url
        self._parts = _urlsplit(url)

    @property
    def scheme(self):
        return self._parts.scheme

    @property
    def host(self):
        return self._parts.hostname or ""

    @property
    def port(self):
        return self._parts.port

    @property
    def path(self):
        return self._parts.path or "/"

    @property
    def query(self):
        return self._parts.query.encode("ascii")

    @property
    def params(self):
        return QueryParams(_parse_qsl(self._parts.query, keep_blank_values=True))

    @property
    def fragment(self):
        return self._parts.fragment

    @property
    def netloc(self):
        return self._parts.netloc.encode("ascii")

    @property
    def is_absolute_url(self):
        return bool(self._parts.scheme and self._parts.netloc)

    @property
    def is_relative_url(self):
        return not self.is_absolute_url

    def join(self, url):
        return URL(_urljoin(self._url, str(url)))

    def copy_with(self, **kwargs):
        p = self._parts
        from urllib.parse import urlunsplit
        return URL(urlunsplit((kwargs.get("scheme", p.scheme), kwargs.get("netloc", p.netloc),
                               kwargs.get("path", p.path), kwargs.get("query", p.query),
                               kwargs.get("fragment", p.fragment))))

    def __str__(self):
        return self._url

    def __repr__(self):
        return f"URL({self._url!r})"

    def __eq__(self, other):
        if isinstance(other, (URL, str)):
            return str(self) == str(other)
        return NotImplemented

    def __hash__(self):
        return hash(self._url)


class Request:
    def __init__(self, method, url, *, params=None, headers=None, content=None, data=None, json=None,
                 files=None, cookies=None, extensions=None):
        if files:
            raise NotImplementedError("file uploads are not supported")
        self.method = method.upper()
        self.url = URL(url, params=params)
        self.headers = Headers(headers)
        body = b""
        if content is not None:
            body = content.encode("utf-8") if isinstance(content, str) else bytes(content)
        elif data is not None:
            body = _cn1_http.form_body(data)
            self.headers.setdefault("Content-Type", "application/x-www-form-urlencoded")
        elif json is not None:
            body = _cn1_http.json_bytes(json, True)
            self.headers.setdefault("Content-Type", "application/json")
        if cookies:
            self.headers["Cookie"] = "; ".join(f"{k}={v}" for k, v in cookies.items())
        self.content = body
        self.extensions = extensions or {}

    def read(self):
        return self.content

    def __repr__(self):
        return f"<Request({self.method!r}, {str(self.url)!r})>"


class Response:
    def __init__(self, status_code, *, headers=None, content=None, text=None, json=None, request=None):
        self.status_code = status_code
        self.headers = Headers(headers)
        if content is None and text is not None:
            content = text.encode("utf-8")
            self.headers.setdefault("Content-Type", "text/plain; charset=utf-8")
        elif content is None and json is not None:
            content = _cn1_http.json_bytes(json, True)
            self.headers.setdefault("Content-Type", "application/json")
        self.content = content if content is not None else b""
        self._request = request
        self.http_version = "HTTP/1.1"
        self.elapsed = _datetime.timedelta(0)
        self.history = []
        self.next_request = None
        self.extensions = {}
        self.default_encoding = "utf-8"

    @property
    def request(self):
        if self._request is None:
            raise RuntimeError("The request instance has not been set on this response.")
        return self._request

    @request.setter
    def request(self, value):
        self._request = value

    @property
    def url(self):
        return self.request.url

    @property
    def reason_phrase(self):
        return codes.get_reason_phrase(self.status_code)

    @property
    def encoding(self):
        return _cn1_http.charset(self.headers.get("content-type")) or self.default_encoding

    @property
    def charset_encoding(self):
        return _cn1_http.charset(self.headers.get("content-type"))

    @property
    def text(self):
        try:
            return self.content.decode(self.encoding, "replace")
        except LookupError:
            return self.content.decode("utf-8", "replace")

    def json(self, **kwargs):
        return _json.loads(self.content.decode(self.charset_encoding or "utf-8"), **kwargs)

    def read(self):
        return self.content

    async def aread(self):
        return self.content

    def iter_bytes(self, chunk_size=None):
        size = chunk_size or len(self.content) or 1
        for i in range(0, len(self.content), size):
            yield self.content[i:i + size]

    def iter_text(self, chunk_size=None):
        t = self.text
        size = chunk_size or len(t) or 1
        for i in range(0, len(t), size):
            yield t[i:i + size]

    def iter_lines(self):
        for line in self.text.splitlines():
            yield line

    @property
    def is_informational(self):
        return 100 <= self.status_code < 200

    @property
    def is_success(self):
        return 200 <= self.status_code < 300

    @property
    def is_redirect(self):
        return self.status_code in (301, 302, 303, 307, 308) and "location" in self.headers

    @property
    def is_client_error(self):
        return 400 <= self.status_code < 500

    @property
    def is_server_error(self):
        return 500 <= self.status_code < 600

    @property
    def is_error(self):
        return 400 <= self.status_code < 600

    @property
    def has_redirect_location(self):
        return self.is_redirect

    def raise_for_status(self):
        request = self._request
        if request is None:
            raise RuntimeError("Cannot call `raise_for_status` as the request instance has not been set on "
                               "this response.")
        if self.is_success:
            return self
        if self.has_redirect_location:
            message = ("{error_type} '{0.status_code} {0.reason_phrase}' for url '{0.url}'\n"
                       "Redirect location: '{0.headers[location]}'\n"
                       "For more information check: https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/"
                       "{0.status_code}")
        else:
            message = ("{error_type} '{0.status_code} {0.reason_phrase}' for url '{0.url}'\n"
                       "For more information check: https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/"
                       "{0.status_code}")
        kinds = {1: "Informational response", 3: "Redirect response", 4: "Client error", 5: "Server error"}
        error_type = kinds.get(self.status_code // 100, "Invalid status code")
        raise HTTPStatusError(message.format(self, error_type=error_type), request=request, response=self)

    def close(self):
        pass

    async def aclose(self):
        pass

    def __repr__(self):
        return f"<Response [{self.status_code} {self.reason_phrase}]>"


def _timeout_seconds(timeout):
    if timeout is None:
        return None
    if isinstance(timeout, Timeout):
        return timeout.read
    if isinstance(timeout, tuple):
        return timeout[1] if len(timeout) > 1 else timeout[0]
    return timeout


class Timeout:
    def __init__(self, timeout=5.0, *, connect=None, read=None, write=None, pool=None):
        self.connect = connect if connect is not None else timeout
        self.read = read if read is not None else timeout
        self.write = write if write is not None else timeout
        self.pool = pool if pool is not None else timeout

    def __repr__(self):
        if self.connect == self.read == self.write == self.pool:
            return f"Timeout(timeout={self.connect})"
        return (f"Timeout(connect={self.connect}, read={self.read}, write={self.write}, "
                f"pool={self.pool})")


class BasicAuth:
    def __init__(self, username, password):
        self._header = _cn1_http.basic_auth(username, password)

    def apply(self, request):
        request.headers["Authorization"] = self._header


_UNSET = object()


def _transport(request, timeout, follow_redirects):
    scheme = request.url.scheme
    if not scheme:
        raise UnsupportedProtocol("Request URL is missing an 'http://' or 'https://' protocol.")
    if scheme not in ("http", "https"):
        raise UnsupportedProtocol(f"Request URL has an unsupported protocol '{scheme}://'.")
    body = request.content if (request.content or request.method in ("POST", "PUT", "PATCH")) else None
    try:
        status, reason, final_url, pairs, content, seconds = _cn1_http.send(
            request.method, str(request.url), request.headers, body, _timeout_seconds(timeout), follow_redirects)
    except OSError as e:
        msg = str(e)
        if _cn1_http.is_timeout(msg):
            raise ReadTimeout(msg, request=request) from None
        raise ConnectError(msg, request=request) from None
    if final_url != str(request.url):
        request = Request(request.method, final_url, headers=request.headers.multi_items())
    r = Response(status, headers=pairs, content=content, request=request)
    r.elapsed = _datetime.timedelta(seconds=seconds)
    return r


class Client:
    def __init__(self, *, auth=None, params=None, headers=None, cookies=None, timeout=5.0,
                 follow_redirects=False, base_url="", **kwargs):
        self.base_url = URL(base_url) if base_url else URL("")
        self.headers = Headers([("Accept", "*/*"), ("User-Agent", "python-httpx/" + __version__)])
        if headers:
            self.headers.update(headers)
        self.params = QueryParams(list(params.items()) if hasattr(params, "items") else (params or []))
        self.cookies = dict(cookies or {})
        self.timeout = timeout if isinstance(timeout, Timeout) else Timeout(timeout)
        self.follow_redirects = follow_redirects
        self.auth = auth
        self.is_closed = False

    def _merge_url(self, url):
        u = URL(url)
        if u.is_relative_url and str(self.base_url):
            base = str(self.base_url)
            if not base.endswith("/"):
                base += "/"
            return URL(base + str(u).lstrip("/"))
        return u

    def build_request(self, method, url, *, content=None, data=None, files=None, json=None, params=None,
                      headers=None, cookies=None, timeout=_UNSET, extensions=None):
        merged = self.params.multi_items()
        if params:
            merged += list(params.items()) if hasattr(params, "items") else list(params)
        h = self.headers.copy()
        if headers:
            h.update(headers)
        jar = dict(self.cookies)
        if cookies:
            jar.update(cookies)
        return Request(method, self._merge_url(url), params=merged, headers=h, content=content, data=data,
                       json=json, files=files, cookies=jar or None, extensions=extensions)

    def send(self, request, *, auth=_UNSET, follow_redirects=_UNSET, timeout=_UNSET):
        a = self.auth if auth is _UNSET else auth
        if isinstance(a, tuple):
            a = BasicAuth(a[0], a[1])
        if a is not None:
            a.apply(request)
        follow = self.follow_redirects if follow_redirects is _UNSET else follow_redirects
        t = self.timeout if timeout is _UNSET else timeout
        r = _transport(request, t, follow)
        for value in r.headers.get_list("set-cookie"):
            name, _, rest = value.partition("=")
            self.cookies[name.strip()] = rest.split(";", 1)[0]
        return r

    def request(self, method, url, *, content=None, data=None, files=None, json=None, params=None,
                headers=None, cookies=None, auth=_UNSET, follow_redirects=_UNSET, timeout=_UNSET,
                extensions=None):
        req = self.build_request(method, url, content=content, data=data, files=files, json=json, params=params,
                                 headers=headers, cookies=cookies, extensions=extensions)
        return self.send(req, auth=auth, follow_redirects=follow_redirects, timeout=timeout)

    def get(self, url, **kwargs):
        return self.request("GET", url, **kwargs)

    def options(self, url, **kwargs):
        return self.request("OPTIONS", url, **kwargs)

    def head(self, url, **kwargs):
        return self.request("HEAD", url, **kwargs)

    def post(self, url, **kwargs):
        return self.request("POST", url, **kwargs)

    def put(self, url, **kwargs):
        return self.request("PUT", url, **kwargs)

    def patch(self, url, **kwargs):
        return self.request("PATCH", url, **kwargs)

    def delete(self, url, **kwargs):
        return self.request("DELETE", url, **kwargs)

    def close(self):
        self.is_closed = True

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()


class AsyncClient(Client):
    async def send(self, request, *, auth=_UNSET, follow_redirects=_UNSET, timeout=_UNSET):
        return Client.send(self, request, auth=auth, follow_redirects=follow_redirects, timeout=timeout)

    async def request(self, method, url, *, content=None, data=None, files=None, json=None, params=None,
                      headers=None, cookies=None, auth=_UNSET, follow_redirects=_UNSET, timeout=_UNSET,
                      extensions=None):
        req = self.build_request(method, url, content=content, data=data, files=files, json=json, params=params,
                                 headers=headers, cookies=cookies, extensions=extensions)
        return Client.send(self, req, auth=auth, follow_redirects=follow_redirects, timeout=timeout)

    async def get(self, url, **kwargs):
        return await self.request("GET", url, **kwargs)

    async def options(self, url, **kwargs):
        return await self.request("OPTIONS", url, **kwargs)

    async def head(self, url, **kwargs):
        return await self.request("HEAD", url, **kwargs)

    async def post(self, url, **kwargs):
        return await self.request("POST", url, **kwargs)

    async def put(self, url, **kwargs):
        return await self.request("PUT", url, **kwargs)

    async def patch(self, url, **kwargs):
        return await self.request("PATCH", url, **kwargs)

    async def delete(self, url, **kwargs):
        return await self.request("DELETE", url, **kwargs)

    async def aclose(self):
        self.close()

    async def __aenter__(self):
        return self

    async def __aexit__(self, *args):
        self.close()


def request(method, url, *, params=None, content=None, data=None, files=None, json=None, headers=None,
            cookies=None, auth=None, timeout=5.0, follow_redirects=False, **kwargs):
    with Client(cookies=cookies, timeout=timeout, follow_redirects=follow_redirects) as c:
        return c.request(method, url, content=content, data=data, files=files, json=json, params=params,
                         headers=headers, auth=auth)


def get(url, **kwargs):
    return request("GET", url, **kwargs)


def options(url, **kwargs):
    return request("OPTIONS", url, **kwargs)


def head(url, **kwargs):
    return request("HEAD", url, **kwargs)


def post(url, **kwargs):
    return request("POST", url, **kwargs)


def put(url, **kwargs):
    return request("PUT", url, **kwargs)


def patch(url, **kwargs):
    return request("PATCH", url, **kwargs)


def delete(url, **kwargs):
    return request("DELETE", url, **kwargs)
