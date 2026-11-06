# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""urlopen and Request over the platform's HTTP client.

Requests go through the same transport as http.client and requests
(Codename One's ConnectionRequest in an application). Redirects are
followed for GET and HEAD, and for a POST answered with 301, 302 or 303,
which is re-sent as a GET, as CPython does. Handlers and openers are kept
for the code that builds them; the ones that change a request (headers,
basic authentication, cookies through http.cookiejar) are applied, while
proxy and FTP handlers are accepted and do nothing.
"""

import _cn1http
import base64
import os
from http.client import HTTPResponse, responses
from urllib.error import URLError, HTTPError
from urllib.parse import urlsplit, urljoin, quote, unquote, urlencode

__all__ = ["Request", "urlopen", "build_opener", "install_opener", "OpenerDirector",
           "BaseHandler", "HTTPHandler", "HTTPSHandler", "HTTPRedirectHandler",
           "HTTPErrorProcessor", "HTTPDefaultErrorHandler", "HTTPBasicAuthHandler",
           "HTTPPasswordMgr", "HTTPPasswordMgrWithDefaultRealm", "HTTPCookieProcessor",
           "ProxyHandler", "UnknownHandler", "urlretrieve", "pathname2url", "url2pathname",
           "getproxies", "quote", "unquote", "urlencode"]


class Request:
    def __init__(self, url, data=None, headers={}, origin_req_host=None, unverifiable=False, method=None):
        self.full_url = url
        self.headers = {}
        self.unredirected_hdrs = {}
        self.data = data
        for k, v in headers.items():
            self.add_header(k, v)
        self.origin_req_host = origin_req_host
        self.unverifiable = unverifiable
        if method:
            self.method = method
        self.timeout = None

    @property
    def full_url(self):
        if self.fragment:
            return "%s#%s" % (self._full_url, self.fragment)
        return self._full_url

    @full_url.setter
    def full_url(self, url):
        self._full_url, _, self.fragment = url.partition("#")
        parts = urlsplit(self._full_url)
        if not parts.scheme:
            raise ValueError("unknown url type: %r" % self._full_url)
        self.type = parts.scheme
        self.host = parts.netloc
        self.selector = parts.path + ("?" + parts.query if parts.query else "")
        if not self.selector:
            self.selector = "/"

    def get_method(self):
        default = "POST" if self.data is not None else "GET"
        return getattr(self, "method", default)

    def get_full_url(self):
        return self.full_url

    def add_header(self, key, val):
        self.headers[key.capitalize()] = val

    def add_unredirected_header(self, key, val):
        self.unredirected_hdrs[key.capitalize()] = val

    def has_header(self, header_name):
        return header_name in self.headers or header_name in self.unredirected_hdrs

    def get_header(self, header_name, default=None):
        return self.headers.get(header_name, self.unredirected_hdrs.get(header_name, default))

    def remove_header(self, header_name):
        self.headers.pop(header_name, None)
        self.unredirected_hdrs.pop(header_name, None)

    def header_items(self):
        hdrs = dict(self.unredirected_hdrs)
        hdrs.update(self.headers)
        return list(hdrs.items())

    def set_proxy(self, host, type):
        pass

    def has_proxy(self):
        return False


class BaseHandler:
    handler_order = 500

    def add_parent(self, parent):
        self.parent = parent

    def close(self):
        pass


class HTTPHandler(BaseHandler):
    pass


class HTTPSHandler(BaseHandler):
    def __init__(self, debuglevel=None, context=None, check_hostname=None):
        self.context = context


class HTTPRedirectHandler(BaseHandler):
    max_repeats = 4
    max_redirections = 10


class HTTPErrorProcessor(BaseHandler):
    pass


class HTTPDefaultErrorHandler(BaseHandler):
    pass


class UnknownHandler(BaseHandler):
    pass


class ProxyHandler(BaseHandler):
    def __init__(self, proxies=None):
        self.proxies = proxies


class HTTPPasswordMgr:
    def __init__(self):
        self.passwd = {}

    def add_password(self, realm, uri, user, passwd):
        if isinstance(uri, str):
            uri = [uri]
        for u in uri:
            self.passwd[(realm, self._reduce(u))] = (user, passwd)

    def _reduce(self, uri):
        parts = urlsplit(uri)
        return parts.netloc if parts.scheme else uri

    def find_user_password(self, realm, authuri):
        host = self._reduce(authuri)
        for (r, u), creds in self.passwd.items():
            if r == realm and u == host:
                return creds
        return None, None


class HTTPPasswordMgrWithDefaultRealm(HTTPPasswordMgr):
    def find_user_password(self, realm, authuri):
        user, password = HTTPPasswordMgr.find_user_password(self, realm, authuri)
        if user is not None:
            return user, password
        return HTTPPasswordMgr.find_user_password(self, None, authuri)


class HTTPBasicAuthHandler(BaseHandler):
    def __init__(self, password_mgr=None):
        self.passwd = password_mgr if password_mgr is not None else HTTPPasswordMgr()
        self.add_password = self.passwd.add_password

    def _authorize(self, req, realm):
        user, pw = self.passwd.find_user_password(realm, req.full_url)
        if user is None:
            return False
        raw = ("%s:%s" % (user, pw)).encode("utf-8")
        req.add_unredirected_header("Authorization", "Basic " + base64.b64encode(raw).decode("ascii"))
        return True


class HTTPCookieProcessor(BaseHandler):
    def __init__(self, cookiejar=None):
        self.cookiejar = cookiejar if cookiejar is not None else _SimpleCookieJar()


class _SimpleCookieJar:
    """Name=value cookies per host, for an HTTPCookieProcessor given none."""

    def __init__(self):
        self._cookies = {}

    def __iter__(self):
        return iter([(h, n, v) for h, d in self._cookies.items() for n, v in d.items()])

    def __len__(self):
        return sum(len(d) for d in self._cookies.values())

    def add_cookie_header(self, req):
        d = self._cookies.get(urlsplit(req.full_url).hostname)
        if d:
            req.add_unredirected_header("Cookie", "; ".join("%s=%s" % kv for kv in d.items()))

    def extract_cookies(self, response, req):
        host = urlsplit(req.full_url).hostname
        for value in response.headers.get_all("Set-Cookie") or []:
            pair = value.split(";", 1)[0]
            if "=" in pair:
                n, v = pair.split("=", 1)
                self._cookies.setdefault(host, {})[n.strip()] = v.strip()

    def clear(self):
        self._cookies = {}


def _realm(headers):
    value = headers.get("WWW-Authenticate") or ""
    if not value.lower().startswith("basic"):
        return None
    i = value.find("realm=")
    if i < 0:
        return ""
    r = value[i + 6:].split(",")[0].strip()
    return r[1:-1] if len(r) > 1 and r[0] == r[-1] == '"' else r


class OpenerDirector:
    def __init__(self):
        self.addheaders = [("User-agent", "Python-urllib/3.14")]
        self.handlers = []

    def add_handler(self, handler):
        handler.add_parent(self)
        self.handlers.append(handler)

    def _find(self, kind):
        for h in self.handlers:
            if isinstance(h, kind):
                return h
        return None

    def close(self):
        self.handlers = []

    def open(self, fullurl, data=None, timeout=None):
        req = fullurl if isinstance(fullurl, Request) else Request(fullurl, data)
        if isinstance(fullurl, Request) and data is not None:
            req.data = data
        req.timeout = timeout
        if req.type not in ("http", "https"):
            if req.type == "file":
                return _open_file(req)
            raise URLError("unknown url type: %s" % req.type)
        if isinstance(req.data, str):
            raise TypeError("POST data should be bytes, an iterable of bytes, or a file object. "
                            "It cannot be of type str.")
        cookies = self._find(HTTPCookieProcessor)
        auth = self._find(HTTPBasicAuthHandler)
        visited = 0
        tried_auth = False
        while True:
            headers = [(k, v) for k, v in self.addheaders if not req.has_header(k.capitalize())]
            if not req.has_header("Host"):
                headers.append(("Host", req.host))
            headers.extend(req.header_items())
            if req.data is not None and not req.has_header("Content-type"):
                headers.append(("Content-Type", "application/x-www-form-urlencoded"))
            if cookies is not None:
                cookies.cookiejar.add_cookie_header(req)
                c = req.unredirected_hdrs.get("Cookie")
                if c:
                    headers.append(("Cookie", c))
            body = req.data
            if body is not None and not isinstance(body, (bytes, bytearray)):
                body = body.read() if hasattr(body, "read") else b"".join(body)
            method = req.get_method()
            try:
                status, reason, url, pairs, content = _cn1http.request(
                    method, req.full_url, headers, body, timeout, False)
            except OSError as e:
                raise URLError(str(e)) from None
            if not reason:
                reason = responses.get(status, "")
            resp = HTTPResponse(status, reason, url, pairs, content, method)
            if cookies is not None:
                cookies.cookiejar.extract_cookies(resp, req)
            loc = resp.headers.get("Location") or resp.headers.get("URI")
            if status in (301, 302, 303, 307, 308) and loc:
                if visited >= HTTPRedirectHandler.max_redirections:
                    raise HTTPError(req.full_url, status,
                                    "The HTTP server returned a redirect error that would lead to an "
                                    "infinite loop.\nThe last 30x error message was:\n" + reason,
                                    resp.headers, resp)
                if not (method in ("GET", "HEAD") or (status in (301, 302, 303) and method == "POST")):
                    raise HTTPError(req.full_url, status, reason, resp.headers, resp)
                visited += 1
                newurl = urljoin(req.full_url, loc)
                kept = {k: v for k, v in req.headers.items() if k.lower() not in ("content-length", "content-type")}
                req = Request(newurl, headers=kept, origin_req_host=req.origin_req_host, unverifiable=True,
                              method="HEAD" if method == "HEAD" else "GET")
                continue
            if status == 401 and auth is not None and not tried_auth:
                tried_auth = True
                realm = _realm(resp.headers)
                if realm is not None and auth._authorize(req, realm):
                    continue
            if not 200 <= status < 300:
                raise HTTPError(req.full_url, status, reason, resp.headers, resp)
            return resp


def _open_file(req):
    path = url2pathname(urlsplit(req.full_url).path)
    try:
        with open(path, "rb") as f:
            data = f.read()
    except OSError as e:
        raise URLError(e) from None
    ext = path.rsplit(".", 1)[-1].lower() if "." in path else ""
    ctype = _TYPES.get(ext, "text/plain")
    pairs = [("Content-type", ctype), ("Content-length", str(len(data)))]
    return HTTPResponse(200, "OK", req.full_url, pairs, data)


_TYPES = {"html": "text/html", "htm": "text/html", "txt": "text/plain", "json": "application/json",
          "css": "text/css", "js": "text/javascript", "png": "image/png", "jpg": "image/jpeg",
          "jpeg": "image/jpeg", "gif": "image/gif", "svg": "image/svg+xml", "pdf": "application/pdf",
          "xml": "application/xml", "csv": "text/csv", "zip": "application/zip"}


def build_opener(*handlers):
    opener = OpenerDirector()
    for h in handlers:
        if isinstance(h, type):
            h = h()
        opener.add_handler(h)
    return opener


_opener = None


def install_opener(opener):
    global _opener
    _opener = opener


def urlopen(url, data=None, timeout=None, *, context=None):
    opener = _opener if _opener is not None else build_opener()
    return opener.open(url, data, timeout)


def urlretrieve(url, filename=None, reporthook=None, data=None):
    with urlopen(url, data) as resp:
        content = resp.read()
        headers = resp.info()
    if filename is None:
        filename = os.path.basename(urlsplit(resp.url).path) or "index.html"
    with open(filename, "wb") as f:
        f.write(content)
    if reporthook is not None:
        reporthook(1, len(content), len(content))
    return filename, headers


def pathname2url(pathname):
    return quote(pathname)


def url2pathname(url):
    return unquote(url)


def getproxies():
    return {}
