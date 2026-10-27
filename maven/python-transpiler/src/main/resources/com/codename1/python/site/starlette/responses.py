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
        body = self.body
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
    def __init__(self, content, status_code=200, headers=None, media_type=None, background=None):
        if hasattr(content, "__iter__") and not isinstance(content, (str, bytes)):
            content = b"".join([c if isinstance(c, (bytes, bytearray)) else str(c).encode("utf-8")
                                for c in content])
        Response.__init__(self, content, status_code, headers, media_type, background)
