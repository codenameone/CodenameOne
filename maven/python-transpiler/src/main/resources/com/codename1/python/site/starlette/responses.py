# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Response classes."""
import json as _json
from urllib.parse import quote

from starlette.datastructures import MutableHeaders


class Response:
    media_type = None
    charset = "utf-8"

    def __init__(self, content=None, status_code=200, headers=None, media_type=None, background=None):
        self.status_code = status_code
        if media_type is not None:
            self.media_type = media_type
        self.background = background
        self.body = self.render(content)
        self.headers = MutableHeaders()
        self._init_headers(headers)

    def render(self, content):
        if content is None:
            return b""
        if isinstance(content, (bytes, bytearray)):
            return bytes(content)
        return str(content).encode(self.charset)

    def _init_headers(self, headers):
        user = headers or {}
        items = user.items() if hasattr(user, "items") else user
        have_len = False
        have_type = False
        for k, v in items:
            self.headers.append(k, v)
            if k.lower() == "content-length":
                have_len = True
            if k.lower() == "content-type":
                have_type = True
        # A stream pending is not measured: it goes out chunked.
        body = None if getattr(self, "_iterator", None) is not None else self.body
        if body is not None and not have_len and not (self.status_code < 200 or self.status_code in (204, 304)):
            self.headers.append("content-length", str(len(body)))
        ct = self.media_type
        if ct is not None and not have_type:
            if ct.startswith("text/") and "charset=" not in ct.lower():
                ct += "; charset=" + self.charset
            self.headers.append("content-type", ct)

    def set_cookie(self, key, value="", max_age=None, expires=None, path="/", domain=None,
                   secure=False, httponly=False, samesite="lax"):
        s = "%s=%s" % (key, quote(str(value), safe="!#$%&'()*+/:<=>?@[]^`{|}~"))
        if max_age is not None:
            s += "; Max-Age=%d" % int(max_age)
        if path is not None:
            s += "; Path=" + path
        if domain is not None:
            s += "; Domain=" + domain
        if secure:
            s += "; Secure"
        if httponly:
            s += "; HttpOnly"
        if samesite is not None:
            s += "; SameSite=" + samesite
        self.headers.append("set-cookie", s)

    def delete_cookie(self, key, path="/", domain=None, secure=False, httponly=False, samesite="lax"):
        self.set_cookie(key, "", max_age=0, path=path, domain=domain, secure=secure,
                        httponly=httponly, samesite=samesite)


class HTMLResponse(Response):
    media_type = "text/html"


class PlainTextResponse(Response):
    media_type = "text/plain"


class JSONResponse(Response):
    media_type = "application/json"

    def render(self, content):
        return _json.dumps(content, ensure_ascii=False, allow_nan=False, indent=None,
                           separators=(",", ":")).encode("utf-8")


class RedirectResponse(Response):
    def __init__(self, url, status_code=307, headers=None, background=None):
        Response.__init__(self, content=b"", status_code=status_code, headers=headers,
                          background=background)
        self.headers["location"] = quote(str(url), safe=":/%#?=@[]!$&'()*+,;")


class StreamingResponse(Response):
    """A body produced as it is sent.

    Served, each chunk the iterator yields goes to the client as it is
    yielded (chunked). Read through ``body`` -- the test client, a
    middleware -- the iterator is drained once and kept.
    """

    def __init__(self, content, status_code=200, headers=None, media_type=None, background=None):
        self.status_code = status_code
        if media_type is not None:
            self.media_type = media_type
        self.background = background
        if isinstance(content, (str, bytes, bytearray)) or content is None:
            self._iterator = None
            self._body = self.render(content)
        else:
            self._iterator = _sync_chunks(content, self.charset)
            self._body = None
        self.headers = MutableHeaders()
        self._init_headers(headers)

    @property
    def body(self):
        if self._iterator is not None:
            it = self._iterator
            self._iterator = None
            self._body = b"".join(list(it))
        return self._body

    @body.setter
    def body(self, value):
        self._iterator = None
        self._body = value

    def take_stream(self):
        """The pending chunk iterator, once, or None when there is none."""
        it = self._iterator
        self._iterator = None
        return it


def _sync_chunks(content, charset):
    if hasattr(content, "__aiter__"):
        content = _drive_async(content.__aiter__())
    for c in content:
        if isinstance(c, (bytes, bytearray, memoryview)):
            yield bytes(c)
        else:
            # As Starlette: anything that is not bytes is encoded, so a str
            # works and an int fails with AttributeError here.
            yield c.encode(charset)


def _drive_async(it):
    from fastapi.routing import run_coro
    while True:
        try:
            item = run_coro(it.__anext__())
        except StopAsyncIteration:
            return
        yield item
