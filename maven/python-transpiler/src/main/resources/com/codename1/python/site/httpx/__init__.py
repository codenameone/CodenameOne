# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The httpx API over the platform's HTTP client.

`httpx.get` and the other verbs, `Client` and `AsyncClient` (base URL,
default headers, params and timeout), `Response`, `Request`, `URL`,
`Headers` and httpx's exception hierarchy. As in httpx, redirects are not
followed unless `follow_redirects=True`. Transports follow httpx's
interface (`BaseTransport.handle_request`, `HTTPTransport`, `MockTransport`),
so a library that installs its own transport or wraps the default one works
unchanged. A response is read whole before it is returned: `stream=True`,
`Client.stream()` and the `iter_*` methods hand out the body that already
arrived, so a streamed reply (server-sent events) is delivered all at once
when the request completes. HTTP/2 and file uploads are not provided. The
asynchronous client runs each request to completion when it is awaited.
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


class StreamConsumed(StreamError):
    pass


class StreamClosed(StreamError):
    pass


class ResponseNotRead(StreamError):
    pass


class RequestNotRead(StreamError):
    pass


class LocalProtocolError(ProtocolError):
    pass


class RemoteProtocolError(ProtocolError):
    pass


class ProxyError(TransportError):
    pass


class WriteError(NetworkError):
    pass


class CloseError(NetworkError):
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

    def values(self):
        return [self[k] for k in self.keys()]

    def copy(self):
        return Headers(self.multi_items(), self.encoding)

    def setdefault(self, key, default=None):
        if key not in self:
            self[key] = default
        return self[key]

    def pop(self, key, *default):
        if key in self:
            v = self[key]
            del self[key]
            return v
        if default:
            return default[0]
        raise KeyError(key)

    def popitem(self):
        keys = self.keys()
        if not keys:
            raise KeyError("popitem(): headers are empty")
        k = keys[-1]
        return (k, self.pop(k))

    def clear(self):
        for k in list(self.keys()):
            del self[k]

    def __iter__(self):
        return iter(self.keys())

    def __len__(self):
        return len(self.keys())

    def __eq__(self, other):
        if isinstance(other, Headers):
            return sorted(self.multi_items()) == sorted(other.multi_items())
        try:
            return sorted(self.multi_items()) == sorted(Headers(other).multi_items())
        except Exception:
            return NotImplemented

    def __repr__(self):
        return f"Headers({dict(self.items())!r})"


def _qp_str(v):
    if v is True:
        return "true"
    if v is False:
        return "false"
    if v is None:
        return ""
    return str(v)


def _qp_pairs(params):
    if params is None:
        return []
    if isinstance(params, QueryParams):
        return params.multi_items()
    if isinstance(params, str):
        return _parse_qsl(params.lstrip("?"), keep_blank_values=True)
    items = params.items() if hasattr(params, "items") else params
    out = []
    for k, v in items:
        if isinstance(v, (list, tuple)):
            out.extend((str(k), _qp_str(x)) for x in v)
        else:
            out.append((str(k), _qp_str(v)))
    return out


class QueryParams:
    def __init__(self, pairs=(), **kwargs):
        if kwargs:
            pairs = kwargs
        self._pairs = _qp_pairs(pairs) if not isinstance(pairs, (list, tuple)) else \
            [(str(k), _qp_str(v)) for k, v in pairs]

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

    def values(self):
        return list(dict(self._pairs).values())

    def set(self, key, value=None):
        return QueryParams([(k, v) for k, v in self._pairs if k != key] + [(key, _qp_str(value))])

    def add(self, key, value=None):
        return QueryParams(self._pairs + [(key, _qp_str(value))])

    def remove(self, key):
        return QueryParams([(k, v) for k, v in self._pairs if k != key])

    def merge(self, params=None):
        other = params if isinstance(params, QueryParams) else QueryParams(_qp_pairs(params))
        keys = set(other.keys())
        return QueryParams([(k, v) for k, v in self._pairs if k not in keys] + other.multi_items())

    def update(self, params=None):
        raise RuntimeError("QueryParams are immutable since 0.18.0. Use q = q.merge(...) to create an updated copy.")

    def __eq__(self, other):
        if not isinstance(other, QueryParams):
            return False
        return sorted(self._pairs) == sorted(other._pairs)

    def __hash__(self):
        return hash(str(self))

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
    def raw_path(self):
        q = self._parts.query
        return ((self._parts.path or "/") + ("?" + q if q else "")).encode("ascii")

    @property
    def raw_host(self):
        return self.host.encode("ascii")

    @property
    def raw_scheme(self):
        return self.scheme.encode("ascii")

    @property
    def username(self):
        from urllib.parse import unquote
        return unquote(self._parts.username or "")

    @property
    def password(self):
        from urllib.parse import unquote
        return unquote(self._parts.password or "")

    @property
    def userinfo(self):
        n = self._parts.netloc
        return n.rpartition("@")[0].encode("ascii") if "@" in n else b""

    @property
    def raw(self):
        return (self.raw_scheme, self.raw_host, self.port, self.raw_path)

    def _with_params(self, params):
        q = str(params)
        return self.copy_with(query=q)

    def copy_set_param(self, key, value=None):
        return self._with_params(self.params.set(key, value))

    def copy_add_param(self, key, value=None):
        return self._with_params(self.params.add(key, value))

    def copy_remove_param(self, key):
        return self._with_params(self.params.remove(key))

    def copy_merge_params(self, params):
        return self._with_params(self.params.merge(params))

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
        netloc = kwargs.get("netloc", p.netloc)
        if isinstance(netloc, bytes):
            netloc = netloc.decode("ascii")
        if "host" in kwargs or "port" in kwargs:
            host = kwargs.get("host", p.hostname or "")
            port = kwargs.get("port", p.port)
            userinfo = netloc.rpartition("@")[0] + "@" if "@" in netloc else ""
            netloc = userinfo + host + (":" + str(port) if port is not None else "")
        query = kwargs.get("query", p.query)
        if isinstance(query, bytes):
            query = query.decode("ascii")
        if "params" in kwargs:
            query = str(QueryParams(kwargs["params"]))
        raw_path = kwargs.get("raw_path")
        path = kwargs.get("path", p.path)
        if raw_path is not None:
            rp = raw_path.decode("ascii") if isinstance(raw_path, bytes) else raw_path
            path, _, query = rp.partition("?")
        return URL(urlunsplit((kwargs.get("scheme", p.scheme), netloc, path, query,
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

    @property
    def cookies(self):
        c = Cookies()
        for v in self.headers.get_list("set-cookie"):
            pair = v.split(";", 1)[0]
            if "=" in pair:
                k, _, val = pair.partition("=")
                c.set(k.strip(), val.strip())
        return c

    @property
    def links(self):
        out = {}
        h = self.headers.get("link")
        if not h:
            return out
        for part in h.split(","):
            seg = [x.strip() for x in part.split(";")]
            if not seg[0].startswith("<"):
                continue
            link = {"url": seg[0].strip("<>")}
            for kv in seg[1:]:
                k, _, v = kv.partition("=")
                link[k.strip()] = v.strip().strip('"')
            out[link.get("rel") or link["url"]] = link
        return out

    async def aread(self):
        return self.content

    @property
    def stream(self):
        return ByteStream(self.content)

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
        self.elapsed = _datetime.timedelta(0)
        self.history = []
        self.next_request = None
        self.extensions = {}
        self.default_encoding = "utf-8"
        self.num_bytes_downloaded = len(self.content)

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

    def iter_raw(self, chunk_size=None):
        return self.iter_bytes(chunk_size)

    async def aiter_bytes(self, chunk_size=None):
        for chunk in self.iter_bytes(chunk_size):
            yield chunk

    async def aiter_raw(self, chunk_size=None):
        for chunk in self.iter_bytes(chunk_size):
            yield chunk

    async def aiter_text(self, chunk_size=None):
        for chunk in self.iter_text(chunk_size):
            yield chunk

    async def aiter_lines(self):
        for line in self.iter_lines():
            yield line

    @property
    def http_version(self):
        return "HTTP/1.1"

    @property
    def is_closed(self):
        return getattr(self, "_closed", False)

    @property
    def stream(self):
        # The body is read whole when the response arrives, so the raw
        # stream is the content, iterable both ways as httpx's is.
        return ByteStream(self.content)

    @property
    def is_stream_consumed(self):
        return True

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
        self._closed = True

    async def aclose(self):
        self._closed = True

    def __repr__(self):
        return f"<Response [{self.status_code} {self.reason_phrase}]>"


_UNSET_TIMEOUT = object()


def _timeout_seconds(timeout):
    if timeout is None:
        return None
    if isinstance(timeout, Timeout):
        return timeout.read
    if isinstance(timeout, tuple):
        return timeout[1] if len(timeout) > 1 else timeout[0]
    return timeout


class Timeout:
    def __init__(self, timeout=_UNSET_TIMEOUT, *, connect=_UNSET_TIMEOUT, read=_UNSET_TIMEOUT,
                 write=_UNSET_TIMEOUT, pool=_UNSET_TIMEOUT):
        if isinstance(timeout, Timeout):
            self.connect, self.read, self.write, self.pool = timeout.connect, timeout.read, timeout.write, timeout.pool
            return
        if isinstance(timeout, tuple):
            timeout = (tuple(timeout) + (None, None, None, None))[:4]
            self.connect, self.read, self.write, self.pool = timeout
            return
        if timeout is _UNSET_TIMEOUT:
            if _UNSET_TIMEOUT in (connect, read, write, pool):
                raise ValueError("httpx.Timeout must either include a default, or set all four parameters "
                                 "explicitly.")
            timeout = None
        self.connect = timeout if connect is _UNSET_TIMEOUT else connect
        self.read = timeout if read is _UNSET_TIMEOUT else read
        self.write = timeout if write is _UNSET_TIMEOUT else write
        self.pool = timeout if pool is _UNSET_TIMEOUT else pool

    def as_dict(self):
        return {"connect": self.connect, "read": self.read, "write": self.write, "pool": self.pool}

    def __eq__(self, other):
        return isinstance(other, Timeout) and self.as_dict() == other.as_dict()

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


class Limits:
    """Connection pool limits. The platform's HTTP client manages its own
    connections, so these are recorded and not enforced."""

    def __init__(self, *, max_connections=None, max_keepalive_connections=None, keepalive_expiry=5.0):
        self.max_connections = max_connections
        self.max_keepalive_connections = max_keepalive_connections
        self.keepalive_expiry = keepalive_expiry

    def __repr__(self):
        return (f"Limits(max_connections={self.max_connections}, "
                f"max_keepalive_connections={self.max_keepalive_connections}, "
                f"keepalive_expiry={self.keepalive_expiry})")


class Proxy:
    """A proxy setting. The platform's network stack applies the device's own
    proxy configuration; an explicit proxy is recorded and not used."""

    def __init__(self, url, *, ssl_context=None, auth=None, headers=None):
        self.url = URL(url)
        self.auth = auth
        self.headers = Headers(headers)


class Auth:
    """httpx's authentication flow: a generator that may modify the request
    and receive the response. The first request the flow yields is sent."""
    requires_request_body = False
    requires_response_body = False

    def auth_flow(self, request):
        yield request

    def sync_auth_flow(self, request):
        return self.auth_flow(request)

    def apply(self, request):
        flow = self.sync_auth_flow(request)
        try:
            next(flow)
        except StopIteration:
            pass


class FunctionAuth(Auth):
    def __init__(self, func):
        self._func = func

    def auth_flow(self, request):
        yield self._func(request)


DEFAULT_TIMEOUT_CONFIG = None
DEFAULT_LIMITS = Limits(max_connections=100, max_keepalive_connections=20)
DEFAULT_MAX_REDIRECTS = 20


class BaseTransport:
    def handle_request(self, request):
        raise NotImplementedError("The 'handle_request' method must be implemented.")

    def close(self):
        pass

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()


class AsyncBaseTransport:
    async def handle_async_request(self, request):
        raise NotImplementedError("The 'handle_async_request' method must be implemented.")

    async def aclose(self):
        pass


class HTTPTransport(BaseTransport):
    """The default transport: Codename One's HTTP client. TLS, proxies and
    connection reuse are the platform's."""

    def __init__(self, verify=True, cert=None, trust_env=True, http1=True, http2=False, limits=None,
                 proxy=None, uds=None, local_address=None, retries=0, socket_options=None, **kwargs):
        self.retries = retries

    def handle_request(self, request):
        ext = request.extensions or {}
        return _transport(request, ext.get("timeout"), False)


class AsyncHTTPTransport(AsyncBaseTransport):
    def __init__(self, *args, **kwargs):
        self._sync = HTTPTransport(*args, **kwargs)

    async def handle_async_request(self, request):
        return self._sync.handle_request(request)


class MockTransport(BaseTransport, AsyncBaseTransport):
    def __init__(self, handler):
        self.handler = handler

    def handle_request(self, request):
        r = self.handler(request)
        r._request = request
        return r

    async def handle_async_request(self, request):
        r = self.handler(request)
        if hasattr(r, "__await__"):
            r = await r
        r._request = request
        return r


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
                 follow_redirects=False, base_url="", transport=None, mounts=None, limits=None,
                 max_redirects=DEFAULT_MAX_REDIRECTS, event_hooks=None, trust_env=True, **kwargs):
        self._transport = transport
        self.max_redirects = max_redirects
        self.event_hooks = {"request": [], "response": []}
        if event_hooks:
            self.event_hooks.update({k: list(v) for k, v in event_hooks.items()})
        self.trust_env = trust_env
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

    def _prepare(self, request, auth, timeout):
        a = self.auth if auth is _UNSET else auth
        if isinstance(a, tuple):
            a = BasicAuth(a[0], a[1])
        elif a is not None and callable(a) and not hasattr(a, "apply"):
            a = FunctionAuth(a)
        if a is not None:
            a.apply(request)
        t = self.timeout if timeout is _UNSET else timeout
        if request.extensions is None:
            request.extensions = {}
        request.extensions.setdefault("timeout", t)
        for hook in self.event_hooks["request"]:
            hook(request)
        return t

    def send(self, request, *, stream=False, auth=_UNSET, follow_redirects=_UNSET, timeout=_UNSET):
        t = self._prepare(request, auth, timeout)
        follow = self.follow_redirects if follow_redirects is _UNSET else follow_redirects
        if self._transport is not None:
            r = self._transport.handle_request(request)
            r._request = request
        else:
            r = _transport(request, t, follow)
        for hook in self.event_hooks["response"]:
            hook(r)
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

    def stream(self, method, url, **kwargs):
        """The response, as a context manager. Its body has already been
        read (see the module documentation)."""
        return _StreamContext(self.request(method, url, **kwargs))

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


class _StreamContext:
    def __init__(self, response):
        self._response = response

    def __enter__(self):
        return self._response

    def __exit__(self, *args):
        self._response.close()

    async def __aenter__(self):
        return self._response

    async def __aexit__(self, *args):
        self._response.close()


class AsyncClient(Client):
    async def send(self, request, *, stream=False, auth=_UNSET, follow_redirects=_UNSET, timeout=_UNSET):
        if self._transport is not None and hasattr(self._transport, "handle_async_request"):
            self._prepare(request, auth, timeout)
            r = await self._transport.handle_async_request(request)
            r._request = request
            return r
        return Client.send(self, request, auth=auth, follow_redirects=follow_redirects, timeout=timeout)

    def stream(self, method, url, **kwargs):
        return _AsyncStreamContext(self, method, url, kwargs)

    async def request(self, method, url, *, content=None, data=None, files=None, json=None, params=None,
                      headers=None, cookies=None, auth=_UNSET, follow_redirects=_UNSET, timeout=_UNSET,
                      extensions=None):
        req = self.build_request(method, url, content=content, data=data, files=files, json=json, params=params,
                                 headers=headers, cookies=cookies, extensions=extensions)
        return await self.send(req, auth=auth, follow_redirects=follow_redirects, timeout=timeout)

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


class _AsyncStreamContext:
    def __init__(self, client, method, url, kwargs):
        self._client = client
        self._args = (method, url, kwargs)
        self._response = None

    async def __aenter__(self):
        method, url, kwargs = self._args
        self._response = await self._client.request(method, url, **kwargs)
        return self._response

    async def __aexit__(self, *args):
        self._response.close()


class Cookies:
    """A name to value jar, as a mapping; domains and paths are kept but not
    matched -- the platform's HTTP stack keeps the session's own cookies."""

    def __init__(self, cookies=None):
        self._jar = {}
        if isinstance(cookies, Cookies):
            self._jar = dict(cookies._jar)
        elif cookies:
            items = cookies.items() if hasattr(cookies, "items") else cookies
            for k, v in items:
                self.set(k, v)

    def set(self, name, value, domain="", path="/"):
        self._jar[name] = (value, domain, path)

    def get(self, name, default=None, domain=None, path=None):
        v = self._jar.get(name)
        return default if v is None else v[0]

    def delete(self, name, domain=None, path=None):
        self._jar.pop(name, None)

    def clear(self, domain=None, path=None):
        self._jar.clear()

    def update(self, cookies=None):
        for k, v in Cookies(cookies).items():
            self.set(k, v)

    def extract_cookies(self, response):
        self.update(response.cookies)

    def set_cookie_header(self, request):
        if self._jar:
            request.headers["Cookie"] = "; ".join(f"{k}={v}" for k, v in self.items())

    def items(self):
        return [(k, v[0]) for k, v in self._jar.items()]

    def keys(self):
        return list(self._jar)

    def values(self):
        return [v[0] for v in self._jar.values()]

    def __getitem__(self, name):
        return self._jar[name][0]

    def __setitem__(self, name, value):
        self.set(name, value)

    def __delitem__(self, name):
        del self._jar[name]

    def __contains__(self, name):
        return name in self._jar

    def __iter__(self):
        return iter(self._jar)

    def __len__(self):
        return len(self._jar)

    def __bool__(self):
        return bool(self._jar)

    def __repr__(self):
        return "<Cookies[%s]>" % ", ".join("<Cookie %s=%s>" % (k, v) for k, v in self.items())


class SyncByteStream:
    def __iter__(self):
        raise NotImplementedError("The '__iter__' method must be implemented.")
        yield b""

    def close(self):
        pass


class AsyncByteStream:
    async def __aiter__(self):
        raise NotImplementedError("The '__aiter__' method must be implemented.")
        yield b""

    async def aclose(self):
        pass


class ByteStream(SyncByteStream, AsyncByteStream):
    def __init__(self, stream=b""):
        self._stream = stream

    def __iter__(self):
        yield self._stream

    async def __aiter__(self):
        yield self._stream
