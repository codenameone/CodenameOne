# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""HTTP for humans: the requests API over the platform's HTTP client.

`requests.get(url, params=..., headers=..., json=..., timeout=...)` and the
other verbs, `Session`, `Response` with `.json()`, `.text` and
`.raise_for_status()`, and requests' exception hierarchy. Streaming, file
uploads, proxies and adapters are not provided; cookies are kept per
session.
"""

import datetime as _datetime
import json as _json

import _cn1_http
from requests import exceptions
from requests.exceptions import (RequestException, HTTPError, ConnectionError, Timeout, ConnectTimeout,
                                 ReadTimeout, URLRequired, TooManyRedirects, MissingSchema, InvalidSchema,
                                 InvalidURL, InvalidHeader, InvalidJSONError, JSONDecodeError)
from requests.structures import CaseInsensitiveDict

__version__ = "2.34.2"
__title__ = "requests"

_DEFAULT_HEADERS = (("User-Agent", "python-requests/" + __version__), ("Accept", "*/*"))


class _Codes:
    def __init__(self):
        table = {
            100: ("continue",), 200: ("ok", "okay", "all_ok", "all_good"), 201: ("created",),
            202: ("accepted",), 204: ("no_content",), 301: ("moved_permanently", "moved"), 302: ("found",),
            304: ("not_modified",), 307: ("temporary_redirect",), 308: ("permanent_redirect",),
            400: ("bad_request", "bad"), 401: ("unauthorized",), 403: ("forbidden",), 404: ("not_found",),
            405: ("method_not_allowed",), 409: ("conflict",), 410: ("gone",), 418: ("im_a_teapot", "teapot"),
            422: ("unprocessable_entity",), 429: ("too_many_requests",), 500: ("internal_server_error",),
            502: ("bad_gateway",), 503: ("service_unavailable", "unavailable"), 504: ("gateway_timeout",)}
        for code, names in table.items():
            for n in names:
                setattr(self, n, code)
                setattr(self, n.upper(), code)

    def __getitem__(self, name):
        return getattr(self, name, None)


codes = _Codes()


class PreparedRequest:
    def __init__(self, method, url, headers, body):
        self.method = method
        self.url = url
        self.headers = headers
        self.body = body

    def __repr__(self):
        return f"<PreparedRequest [{self.method}]>"


class Response:
    def __init__(self):
        self.status_code = None
        self.reason = None
        self.url = None
        self.headers = CaseInsensitiveDict()
        self._content = b""
        self.encoding = None
        self.history = []
        self.request = None
        self.elapsed = _datetime.timedelta(0)
        self.cookies = {}

    @property
    def content(self):
        return self._content

    @property
    def apparent_encoding(self):
        return "utf-8"

    @property
    def text(self):
        enc = self.encoding or "utf-8"
        try:
            return self._content.decode(enc, "replace")
        except LookupError:
            return self._content.decode("utf-8", "replace")

    def json(self, **kwargs):
        try:
            return _json.loads(self.text, **kwargs)
        except _json.JSONDecodeError as e:
            raise JSONDecodeError(e.msg, e.doc, e.pos, response=self, request=self.request) from None

    @property
    def ok(self):
        try:
            self.raise_for_status()
        except HTTPError:
            return False
        return True

    @property
    def is_redirect(self):
        return "location" in self.headers and self.status_code in (301, 302, 303, 307, 308)

    @property
    def is_permanent_redirect(self):
        return "location" in self.headers and self.status_code in (301, 308)

    @property
    def links(self):
        return {}

    def raise_for_status(self):
        reason = self.reason or ""
        msg = ""
        if 400 <= self.status_code < 500:
            msg = f"{self.status_code} Client Error: {reason} for url: {self.url}"
        elif 500 <= self.status_code < 600:
            msg = f"{self.status_code} Server Error: {reason} for url: {self.url}"
        if msg:
            raise HTTPError(msg, response=self)

    def iter_content(self, chunk_size=1, decode_unicode=False):
        data = self.text if decode_unicode else self._content
        if chunk_size is None:
            chunk_size = len(data) or 1
        for i in range(0, len(data), chunk_size):
            yield data[i:i + chunk_size]

    def iter_lines(self, chunk_size=512, decode_unicode=False, delimiter=None):
        data = self.text if decode_unicode else self._content
        lines = data.split(delimiter) if delimiter is not None else data.splitlines()
        for line in lines:
            yield line

    def close(self):
        pass

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()

    def __bool__(self):
        return self.ok

    def __iter__(self):
        return self.iter_content(128)

    def __repr__(self):
        return f"<Response [{self.status_code}]>"


def _encoding_of(content_type):
    cs = _cn1_http.charset(content_type)
    if cs:
        return cs
    if content_type is None:
        return None
    if "text" in content_type:
        return "ISO-8859-1"
    if "application/json" in content_type:
        return "utf-8"
    return None


class Session:
    def __init__(self):
        self.headers = CaseInsensitiveDict(dict(_DEFAULT_HEADERS))
        self.auth = None
        self.params = {}
        self.cookies = {}
        self.max_redirects = 30

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()

    def close(self):
        pass

    def request(self, method, url, params=None, data=None, headers=None, cookies=None, files=None, auth=None,
                timeout=None, allow_redirects=True, proxies=None, hooks=None, stream=None, verify=None,
                cert=None, json=None):
        if files:
            raise NotImplementedError("file uploads are not supported")
        if not isinstance(url, str):
            url = str(url)
        url = url.strip()
        if ":" not in url.split("/")[0] or url.startswith(":"):
            raise MissingSchema(f"Invalid URL '{url}': No scheme supplied. Perhaps you meant https://{url}?")
        scheme = url.split(":", 1)[0].lower()
        if scheme not in ("http", "https"):
            raise InvalidSchema(f"No connection adapters were found for '{url}'")
        merged = dict(self.params)
        if params:
            if hasattr(params, "items"):
                merged.update(params)
                full = _cn1_http.with_params(url, merged)
            else:
                full = _cn1_http.with_params(_cn1_http.with_params(url, merged), params)
        else:
            full = _cn1_http.with_params(url, merged)
        h = _cn1_http.Headers(self.headers.items())
        body = None
        if data is not None and data != {} and data != []:
            if isinstance(data, str):
                body = data.encode("utf-8")
            elif isinstance(data, (bytes, bytearray)):
                body = bytes(data)
            else:
                body = _cn1_http.form_body(data)
                h.setdefault("Content-Type", "application/x-www-form-urlencoded")
        elif json is not None:
            body = _cn1_http.json_bytes(json, False)
            h.setdefault("Content-Type", "application/json")
        if headers:
            for k, v in headers.items():
                if v is None:
                    if k in h:
                        del h[k]
                else:
                    h[k] = v
        jar = dict(self.cookies)
        if cookies:
            jar.update(cookies)
        if jar:
            h["Cookie"] = "; ".join(f"{k}={v}" for k, v in jar.items())
        a = auth if auth is not None else self.auth
        if a is not None:
            if isinstance(a, tuple):
                h["Authorization"] = _cn1_http.basic_auth(a[0], a[1])
            elif callable(a):
                prepared = PreparedRequest(method.upper(), full, h, body)
                prepared = a(prepared)
                h = prepared.headers
        t = timeout[1] if isinstance(timeout, tuple) else timeout
        prepared = PreparedRequest(method.upper(), full, CaseInsensitiveDict(h.items()), body)
        try:
            status, reason, final_url, pairs, content, seconds = _cn1_http.send(method.upper(), full, h, body, t,
                                                                                allow_redirects)
        except OSError as e:
            msg = str(e)
            if _cn1_http.is_timeout(msg):
                raise ReadTimeout(msg, request=prepared) from None
            raise ConnectionError(msg, request=prepared) from None
        r = Response()
        r.status_code = status
        r.reason = reason
        r.url = final_url
        hs = _cn1_http.Headers(pairs)
        r.headers = CaseInsensitiveDict(hs.items())
        r._content = content
        r.encoding = _encoding_of(r.headers.get("content-type"))
        r.request = prepared
        r.elapsed = _datetime.timedelta(seconds=seconds)
        for value in hs.get_list("set-cookie"):
            name, _, rest = value.partition("=")
            v = rest.split(";", 1)[0]
            r.cookies[name.strip()] = v
            self.cookies[name.strip()] = v
        return r

    def get(self, url, **kwargs):
        kwargs.setdefault("allow_redirects", True)
        return self.request("GET", url, **kwargs)

    def options(self, url, **kwargs):
        kwargs.setdefault("allow_redirects", True)
        return self.request("OPTIONS", url, **kwargs)

    def head(self, url, **kwargs):
        kwargs.setdefault("allow_redirects", False)
        return self.request("HEAD", url, **kwargs)

    def post(self, url, data=None, json=None, **kwargs):
        return self.request("POST", url, data=data, json=json, **kwargs)

    def put(self, url, data=None, **kwargs):
        return self.request("PUT", url, data=data, **kwargs)

    def patch(self, url, data=None, **kwargs):
        return self.request("PATCH", url, data=data, **kwargs)

    def delete(self, url, **kwargs):
        return self.request("DELETE", url, **kwargs)


def session():
    return Session()


def request(method, url, **kwargs):
    with Session() as s:
        return s.request(method, url, **kwargs)


def get(url, params=None, **kwargs):
    return request("GET", url, params=params, **kwargs)


def options(url, **kwargs):
    return request("OPTIONS", url, **kwargs)


def head(url, **kwargs):
    kwargs.setdefault("allow_redirects", False)
    return request("HEAD", url, **kwargs)


def post(url, data=None, json=None, **kwargs):
    return request("POST", url, data=data, json=json, **kwargs)


def put(url, data=None, **kwargs):
    return request("PUT", url, data=data, **kwargs)


def patch(url, data=None, **kwargs):
    return request("PATCH", url, data=data, **kwargs)


def delete(url, **kwargs):
    return request("DELETE", url, **kwargs)
