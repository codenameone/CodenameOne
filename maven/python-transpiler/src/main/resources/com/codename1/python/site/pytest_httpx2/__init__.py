# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""pytest_httpx2: the httpx2_mock (and httpx_mock) fixture, over the bundled
httpx.

Every request any client sends -- through the default transport, an
explicit HTTPTransport or AsyncHTTPTransport -- is answered from the
responses registered with add_response or add_callback instead of the
network. Matching is by url (str, httpx.URL or compiled pattern; query
parameters compare as a multiset), method, match_headers (a subset),
match_content, match_json and match_params. The first matching response
not yet used answers; once all are used, the last reusable one does. A
request nothing matches raises httpx.TimeoutException, as pytest_httpx's
does, and the fixture's teardown fails the test when a registered,
non-optional response was never requested.

A stream= body (IteratorStream or any iterable of bytes) is read whole
when the request arrives, so a client sees the same bytes, in one piece.
The options pytest_httpx reads from an httpx_mock marker are ignored.
"""

import json as _json
import re as _re
from urllib.parse import parse_qsl as _parse_qsl, urlsplit as _urlsplit

import httpx
import pytest

__all__ = ["HTTPXMock", "IteratorStream", "httpx_mock", "httpx2_mock"]


class IteratorStream(httpx.SyncByteStream, httpx.AsyncByteStream):
    """A response body produced chunk by chunk, sync or async."""

    def __init__(self, stream):
        self._chunks = stream

    def __iter__(self):
        yield from self._chunks

    async def __aiter__(self):
        for chunk in self._chunks:
            yield chunk


def _split_url(url):
    text = str(url)
    parts = _urlsplit(text)
    base = (parts.scheme.lower(), parts.netloc.lower(), parts.path or "/")
    return base, sorted(_parse_qsl(parts.query, keep_blank_values=True))


class _Matcher:
    def __init__(self, url=None, method=None, match_headers=None, match_content=None,
                 match_json=None, match_params=None, match_data=None, match_files=None,
                 match_extensions=None, proxy_url=None, is_optional=False, is_reusable=False):
        if match_content is not None and match_json is not None:
            raise ValueError("Only one way of matching against the body can be provided. "
                             "If you want to match against the JSON decoded representation, use match_json. "
                             "Otherwise, use match_content.")
        self.url = url
        self.method = method.upper() if method else None
        self.headers = match_headers
        self.content = match_content
        self.json = match_json
        self.params = match_params
        self.optional = is_optional
        self.reusable = is_reusable
        self.nb_calls = 0

    def match(self, request):
        if self.url is not None:
            if isinstance(self.url, _re.Pattern):
                if not self.url.match(str(request.url)):
                    return False
            else:
                want_base, want_query = _split_url(self.url)
                if self.params is not None:
                    want_query = sorted(want_query + [(k, str(v)) for k, vs in self.params.items()
                                                      for v in (vs if isinstance(vs, list) else [vs])])
                if (want_base, want_query) != _split_url(request.url):
                    return False
        if self.method is not None and request.method.upper() != self.method:
            return False
        if self.headers is not None:
            for k, v in self.headers.items():
                if request.headers.get(k) != v:
                    return False
        if self.content is not None and request.content != self.content:
            return False
        if self.json is not None:
            try:
                if _json.loads(request.content.decode("utf-8")) != self.json:
                    return False
            except ValueError:
                return False
        return True

    def describe(self):
        bits = []
        if self.method:
            bits.append(self.method)
        bits.append("request" if not self.url else "request on " + str(self.url))
        return "Match " + " ".join(bits)


class HTTPXMock:
    def __init__(self):
        self._callbacks = []
        self._requests = []

    def add_response(self, status_code=200, http_version="HTTP/1.1", headers=None, content=None,
                     text=None, html=None, stream=None, json=None, **matchers):
        import copy
        if json is not None:
            json = copy.deepcopy(json)

        def respond(request):
            body = content
            if stream is not None:
                body = b"".join(bytes(c) for c in stream)
            if html is not None:
                h = httpx.Headers(headers)
                h.setdefault("Content-Type", "text/html; charset=utf-8")
                return httpx.Response(status_code, headers=h, content=html.encode("utf-8"))
            return httpx.Response(status_code, headers=headers, content=body, text=text, json=json)
        self.add_callback(respond, **matchers)

    def add_callback(self, callback, **matchers):
        self._callbacks.append((_Matcher(**matchers), callback))

    def add_exception(self, exception, **matchers):
        def raise_it(request):
            if isinstance(exception, httpx.RequestError):
                exception.request = request
            raise exception
        self.add_callback(raise_it, **matchers)

    def _find(self, request):
        found = [(m, c) for m, c in self._callbacks if m.match(request)]
        if not found:
            return None
        for m, c in found:
            if not m.nb_calls:
                m.nb_calls += 1
                return c
        m, c = found[-1]
        if m.reusable:
            m.nb_calls += 1
            return c
        return None

    def _handle(self, request):
        self._requests.append(request)
        callback = self._find(request)
        if callback is None:
            raise httpx.TimeoutException("No response can be found for %s request on %s"
                                         % (request.method, request.url), request=request)
        response = callback(request)
        if response is None:
            raise TypeError("The callback did not return a response")
        response._request = request
        return response

    async def _handle_async(self, request):
        self._requests.append(request)
        callback = self._find(request)
        if callback is None:
            raise httpx.TimeoutException("No response can be found for %s request on %s"
                                         % (request.method, request.url), request=request)
        response = callback(request)
        if hasattr(response, "__await__"):
            response = await response
        if response is None:
            raise TypeError("The callback did not return a response")
        response._request = request
        return response

    def get_requests(self, **matchers):
        m = _Matcher(**matchers)
        return [r for r in self._requests if m.match(r)]

    def get_request(self, **matchers):
        found = self.get_requests(**matchers)
        assert len(found) <= 1, \
            "More than one request (%d) matched, use get_requests instead or refine your filters." % len(found)
        return found[0] if found else None

    def reset(self):
        self._callbacks.clear()
        self._requests.clear()

    def _unused(self):
        return [m for m, _ in self._callbacks if not m.nb_calls and not m.optional]


def _install(mock):
    """Routes every transport through the mock; returns the undo."""
    saved = (httpx._transport, httpx.HTTPTransport.handle_request,
             httpx.AsyncHTTPTransport.handle_async_request)

    def module_transport(request, timeout, follow):
        return mock._handle(request)

    def handle_request(self, request):
        return mock._handle(request)

    async def handle_async_request(self, request):
        return await mock._handle_async(request)
    httpx._transport = module_transport
    httpx.HTTPTransport.handle_request = handle_request
    httpx.AsyncHTTPTransport.handle_async_request = handle_async_request

    def undo():
        httpx._transport, httpx.HTTPTransport.handle_request, \
            httpx.AsyncHTTPTransport.handle_async_request = saved
    return undo


def _fixture():
    mock = HTTPXMock()
    undo = _install(mock)
    try:
        yield mock
    finally:
        undo()
    unused = mock._unused()
    if unused:
        raise AssertionError("The following responses are mocked but not requested:\n"
                             + "\n".join("  - " + m.describe() for m in unused))


@pytest.fixture
def httpx2_mock():
    yield from _fixture()


@pytest.fixture
def httpx_mock():
    yield from _fixture()
