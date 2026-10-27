# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Flask over a pure dispatch core: no sockets, no WSGI.

``app.dispatch(method, path, query, headers, body)`` maps one request to
``(status, headers, body)``; the test client and the native server both go
through it. Written from Flask's public documentation and behaviour.
"""
import json as _json
import re as _re
import uuid as _uuid
from urllib.parse import parse_qsl, quote, quote_plus, unquote, urlencode

from werkzeug.exceptions import (HTTPException, BadRequest, NotFound, MethodNotAllowed,
                                 UnsupportedMediaType, InternalServerError, abort,
                                 default_exceptions)

__all__ = ["Flask", "Blueprint", "Response", "request", "g", "jsonify", "make_response",
           "abort", "redirect", "url_for", "current_app"]

_STATUS = {
    100: "CONTINUE", 101: "SWITCHING PROTOCOLS", 200: "OK", 201: "CREATED", 202: "ACCEPTED",
    203: "NON AUTHORITATIVE INFORMATION", 204: "NO CONTENT", 205: "RESET CONTENT",
    206: "PARTIAL CONTENT", 300: "MULTIPLE CHOICES", 301: "MOVED PERMANENTLY", 302: "FOUND",
    303: "SEE OTHER", 304: "NOT MODIFIED", 307: "TEMPORARY REDIRECT", 308: "PERMANENT REDIRECT",
    400: "BAD REQUEST", 401: "UNAUTHORIZED", 403: "FORBIDDEN", 404: "NOT FOUND",
    405: "METHOD NOT ALLOWED", 406: "NOT ACCEPTABLE", 408: "REQUEST TIMEOUT", 409: "CONFLICT",
    410: "GONE", 411: "LENGTH REQUIRED", 412: "PRECONDITION FAILED", 413: "REQUEST ENTITY TOO LARGE",
    415: "UNSUPPORTED MEDIA TYPE", 418: "I'M A TEAPOT", 422: "UNPROCESSABLE ENTITY",
    429: "TOO MANY REQUESTS", 500: "INTERNAL SERVER ERROR", 501: "NOT IMPLEMENTED",
    502: "BAD GATEWAY", 503: "SERVICE UNAVAILABLE", 504: "GATEWAY TIMEOUT",
}


def _b(x):
    if isinstance(x, (bytes, bytearray)):
        return bytes(x)
    return str(x).encode("utf-8")


class Headers:
    """Case-insensitive, ordered, multi-valued header collection."""

    def __init__(self, items=None):
        self._l = []
        if items is not None:
            self.extend(items)

    def extend(self, items):
        if isinstance(items, Headers):
            items = items._l
        elif isinstance(items, dict):
            items = list(items.items())
        for k, v in items:
            self.add(k, v)

    def add(self, key, value):
        self._l.append((key, str(value)))

    def set(self, key, value):
        self.__setitem__(key, value)

    def __setitem__(self, key, value):
        lk = key.lower()
        out = []
        done = False
        for k, v in self._l:
            if k.lower() == lk:
                if not done:
                    out.append((key, str(value)))
                    done = True
            else:
                out.append((k, v))
        if not done:
            out.append((key, str(value)))
        self._l = out

    def __getitem__(self, key):
        lk = key.lower()
        for k, v in self._l:
            if k.lower() == lk:
                return v
        raise KeyError(key)

    def get(self, key, default=None, type=None):
        try:
            v = self[key]
        except KeyError:
            return default
        if type is not None:
            try:
                return type(v)
            except (ValueError, TypeError):
                return default
        return v

    def getlist(self, key):
        lk = key.lower()
        return [v for k, v in self._l if k.lower() == lk]

    def __contains__(self, key):
        return isinstance(key, str) and key.lower() in [k.lower() for k, v in self._l]

    def __delitem__(self, key):
        lk = key.lower()
        self._l = [(k, v) for k, v in self._l if k.lower() != lk]

    def remove(self, key):
        del self[key]

    def pop(self, key, default=None):
        v = self.get(key, default)
        del self[key]
        return v

    def setdefault(self, key, value):
        if key not in self:
            self[key] = value
        return self[key]

    def items(self):
        return list(self._l)

    def keys(self):
        return [k for k, v in self._l]

    def values(self):
        return [v for k, v in self._l]

    def __iter__(self):
        return iter(list(self._l))

    def __len__(self):
        return len(self._l)

    def to_wsgi_list(self):
        return list(self._l)

    def __repr__(self):
        return "Headers(%r)" % (self._l,)


class MultiDict(dict):
    """dict of first values that remembers every value per key."""

    def __init__(self, pairs=None):
        dict.__init__(self)
        self._all = {}
        if pairs:
            for k, v in pairs:
                self.add(k, v)

    def add(self, key, value):
        if key not in self._all:
            self._all[key] = []
            dict.__setitem__(self, key, value)
        self._all[key].append(value)

    def getlist(self, key, type=None):
        vals = self._all.get(key, [])
        if type is None:
            return list(vals)
        out = []
        for v in vals:
            try:
                out.append(type(v))
            except (ValueError, TypeError):
                pass
        return out

    def get(self, key, default=None, type=None):
        if key not in self:
            return default
        v = dict.__getitem__(self, key)
        if type is not None:
            try:
                return type(v)
            except (ValueError, TypeError):
                return default
        return v

    def __getitem__(self, key):
        if key not in self:
            raise BadRequest("The browser (or proxy) sent a request that this server could not understand.")
        return dict.__getitem__(self, key)

    def to_dict(self, flat=True):
        if flat:
            return dict(self.items())
        return dict((k, list(v)) for k, v in self._all.items())

    def lists(self):
        return [(k, list(v)) for k, v in self._all.items()]

    def __repr__(self):
        return "MultiDict(%r)" % ([(k, v) for k, vs in self._all.items() for v in vs],)


def _parse_qs(s):
    if isinstance(s, (bytes, bytearray)):
        s = bytes(s).decode("utf-8", "replace")
    return MultiDict(parse_qsl(s or "", keep_blank_values=True))


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


class Response:
    default_status = 200
    default_mimetype = "text/html"
    charset = "utf-8"

    def __init__(self, response=None, status=None, headers=None, mimetype=None, content_type=None):
        self.headers = Headers(headers) if headers is not None else Headers()
        if content_type is None:
            if mimetype is None and "Content-Type" not in self.headers:
                mimetype = self.default_mimetype
            if mimetype is not None:
                if mimetype.startswith("text/") or mimetype in ("application/xml", "application/javascript"):
                    mimetype = mimetype + "; charset=utf-8"
                content_type = mimetype
        if content_type is not None:
            self.headers["Content-Type"] = content_type
        self.status_code = self.default_status
        if status is not None:
            if isinstance(status, int):
                self.status_code = status
            else:
                self.status = status
        self._data = b""
        if response is not None:
            if isinstance(response, (str, bytes, bytearray)):
                self.set_data(response)
            else:
                self.set_data(b"".join([_b(c) for c in response]))
        self._cookies = []

    @property
    def status(self):
        s = getattr(self, "_status_text", None)
        if s is not None and s[0] == self.status_code:
            return s[1]
        return "%d %s" % (self.status_code, _STATUS.get(self.status_code, "UNKNOWN"))

    @status.setter
    def status(self, value):
        if isinstance(value, int):
            self.status_code = value
            self._status_text = None
        else:
            value = str(value)
            self.status_code = int(value.split(None, 1)[0])
            self._status_text = (self.status_code, value)

    def set_data(self, value):
        self._data = _b(value)
        self.headers["Content-Length"] = str(len(self._data))

    def get_data(self, as_text=False):
        if as_text:
            return self._data.decode("utf-8", "replace")
        return self._data

    data = property(get_data, set_data)

    @property
    def mimetype(self):
        ct = self.headers.get("Content-Type", "")
        return ct.split(";")[0].strip()

    @mimetype.setter
    def mimetype(self, v):
        if v.startswith("text/") or v in ("application/xml", "application/javascript"):
            v = v + "; charset=utf-8"
        self.headers["Content-Type"] = v

    @property
    def content_type(self):
        return self.headers.get("Content-Type")

    @content_type.setter
    def content_type(self, v):
        self.headers["Content-Type"] = v

    @property
    def content_length(self):
        return self.headers.get("Content-Length", type=int)

    @property
    def location(self):
        return self.headers.get("Location")

    @property
    def is_json(self):
        m = self.mimetype
        return m == "application/json" or (m.startswith("application/") and m.endswith("+json"))

    def get_json(self, force=False, silent=False):
        if not (force or self.is_json):
            return None
        try:
            return _json.loads(self._data.decode("utf-8"))
        except ValueError:
            if silent:
                return None
            raise BadRequest()

    @property
    def json(self):
        return self.get_json()

    def set_cookie(self, key, value="", max_age=None, expires=None, path="/", domain=None,
                   secure=False, httponly=False, samesite=None):
        s = "%s=%s" % (key, quote(str(value), safe="!#$%&'()*+/:<=>?@[]^`{|}~"))
        if max_age is not None:
            s += "; Max-Age=%d" % int(max_age)
        if domain:
            s += "; Domain=" + domain
        if path:
            s += "; Path=" + path
        if secure:
            s += "; Secure"
        if httponly:
            s += "; HttpOnly"
        if samesite:
            s += "; SameSite=" + samesite
        self.headers.add("Set-Cookie", s)

    def delete_cookie(self, key, path="/", domain=None):
        self.set_cookie(key, "", max_age=0, path=path, domain=domain)

    def __repr__(self):
        return "<Response %d bytes [%s]>" % (len(self._data), self.status)

    @property
    def is_redirect(self):
        return self.status_code in (301, 302, 303, 307, 308) and "Location" in self.headers


class _Ctx:
    def __init__(self, app, req):
        self.app = app
        self.request = req
        self.g = _AppGlobals()


class _AppGlobals:
    def get(self, name, default=None):
        return self.__dict__.get(name, default)

    def pop(self, name, default=None):
        return self.__dict__.pop(name, default)

    def setdefault(self, name, default=None):
        return self.__dict__.setdefault(name, default)

    def __contains__(self, name):
        return name in self.__dict__

    def __iter__(self):
        return iter(self.__dict__)


_stack = []


class _Proxy:
    def __init__(self, what):
        object.__setattr__(self, "_what", what)

    def _get(self):
        if not _stack:
            raise RuntimeError("Working outside of request context.")
        c = _stack[-1]
        w = object.__getattribute__(self, "_what")
        if w == "request":
            return c.request
        if w == "g":
            return c.g
        return c.app

    def __getattr__(self, name):
        return getattr(self._get(), name)

    def __setattr__(self, name, value):
        setattr(self._get(), name, value)

    def __delattr__(self, name):
        delattr(self._get(), name)

    def __contains__(self, name):
        return name in self._get()

    def __iter__(self):
        return iter(self._get())

    def __bool__(self):
        return bool(_stack)

    def __repr__(self):
        return repr(self._get())


class Request:
    def __init__(self, method, path, query, headers, body, app=None):
        self.method = method
        self.path = path
        self.query_string = _b(query or "")
        self.headers = Headers(headers)
        self._body = bytes(body or b"")
        self.view_args = None
        self.endpoint = None
        self.url_rule = None
        self.routing_exception = None
        self.scheme = "http"
        self.remote_addr = "127.0.0.1"
        self.environ = {}
        self._args = None
        self._form = None
        self._json = None
        self._json_done = False

    @property
    def blueprint(self):
        if self.endpoint and "." in self.endpoint:
            return self.endpoint.rsplit(".", 1)[0]
        return None

    @property
    def args(self):
        if self._args is None:
            self._args = _parse_qs(self.query_string)
        return self._args

    @property
    def form(self):
        if self._form is None:
            if self.mimetype == "application/x-www-form-urlencoded":
                self._form = _parse_qs(self._body)
            else:
                self._form = MultiDict()
        return self._form

    @property
    def values(self):
        m = MultiDict()
        for k, vs in self.args.lists():
            for v in vs:
                m.add(k, v)
        for k, vs in self.form.lists():
            for v in vs:
                m.add(k, v)
        return m

    @property
    def files(self):
        return MultiDict()

    @property
    def cookies(self):
        return _parse_cookies(self.headers.get("Cookie"))

    @property
    def content_type(self):
        return self.headers.get("Content-Type")

    @property
    def mimetype(self):
        ct = self.headers.get("Content-Type", "")
        return ct.split(";")[0].strip().lower()

    @property
    def content_length(self):
        return self.headers.get("Content-Length", type=int)

    @property
    def host(self):
        return self.headers.get("Host", "localhost")

    @property
    def host_url(self):
        return "http://" + self.host + "/"

    @property
    def base_url(self):
        return "http://" + self.host + quote(self.path)

    @property
    def url(self):
        q = self.query_string.decode("utf-8", "replace")
        return self.base_url + ("?" + q if q else "")

    @property
    def full_path(self):
        return self.path + "?" + self.query_string.decode("utf-8", "replace")

    @property
    def is_json(self):
        m = self.mimetype
        return m == "application/json" or (m.startswith("application/") and m.endswith("+json"))

    @property
    def is_secure(self):
        return False

    def get_data(self, cache=True, as_text=False, parse_form_data=False):
        if as_text:
            return self._body.decode("utf-8", "replace")
        return self._body

    @property
    def data(self):
        return self._body

    def get_json(self, force=False, silent=False, cache=True):
        if self._json_done:
            return self._json
        if not (force or self.is_json):
            if silent:
                return None
            raise UnsupportedMediaType("Did not attempt to load JSON data because the request"
                                       " Content-Type was not 'application/json'.")
        try:
            val = _json.loads(self._body.decode("utf-8"))
        except ValueError as e:
            if silent:
                return None
            raise BadRequest()
        self._json = val
        self._json_done = True
        return val

    @property
    def json(self):
        return self.get_json()

    def __repr__(self):
        return "<Request %r [%s]>" % (self.url, self.method)


request = _Proxy("request")
g = _Proxy("g")
current_app = _Proxy("app")


def _to_json(obj, compact=True, sort=True):
    def default(o):
        if hasattr(o, "isoformat"):
            return o.isoformat()
        if isinstance(o, _uuid.UUID):
            return str(o)
        if hasattr(o, "__html__"):
            return str(o.__html__())
        if hasattr(o, "model_dump"):
            return o.model_dump(mode="json")
        raise TypeError("Object of type %s is not JSON serializable" % type(o).__name__)
    if compact:
        return _json.dumps(obj, default=default, sort_keys=sort, separators=(",", ":"))
    return _json.dumps(obj, default=default, sort_keys=sort, indent=2)


def jsonify(*args, **kwargs):
    if args and kwargs:
        raise TypeError("jsonify() behavior undefined when passed both args and kwargs")
    if len(args) == 1:
        data = args[0]
    else:
        data = args or kwargs
        if isinstance(data, tuple):
            data = list(data)
    compact = not (_stack and _stack[-1].app.debug)
    return Response(_to_json(data, compact) + "\n", mimetype="application/json")


def make_response(*args):
    if not args:
        return Response()
    if len(args) == 1:
        args = args[0]
    app = _stack[-1].app if _stack else Flask.__new__(Flask)
    return _make_response(app, args)


def _make_response(app, rv):
    status = None
    headers = None
    if isinstance(rv, tuple):
        n = len(rv)
        if n == 3:
            rv, status, headers = rv
        elif n == 2:
            if isinstance(rv[1], (Headers, dict, tuple, list)):
                rv, headers = rv
            else:
                rv, status = rv
        else:
            raise TypeError("The view function did not return a valid response tuple."
                            " The tuple must have the form (body, status, headers),"
                            " (body, status), or (body, headers).")
    if rv is None:
        raise TypeError("The view function did not return a valid response. The function either"
                        " returned None or ended without a return statement.")
    if not isinstance(rv, Response):
        if isinstance(rv, (str, bytes, bytearray)):
            rv = Response(rv)
        elif isinstance(rv, (dict, list)):
            rv = jsonify(rv)
        elif isinstance(rv, HTTPException):
            rv = rv.get_response()
        elif hasattr(rv, "__iter__") or hasattr(rv, "__next__"):
            rv = Response(rv)
        else:
            raise TypeError("The view function did not return a valid response. The return type"
                            " must be a string, dict, list, tuple with headers or status, Response"
                            " instance, or WSGI callable, but it was a %s." % type(rv).__name__)
    if status is not None:
        if isinstance(status, int):
            rv.status_code = status
        else:
            rv.status = status
    if headers:
        if isinstance(headers, dict):
            headers = headers.items()
        for k, v in headers:
            rv.headers[k] = v
    return rv


def redirect(location, code=302, Response=Response):
    from werkzeug.exceptions import _escape as escape
    loc = str(location)
    body = ('<!doctype html>\n<html lang=en>\n<title>Redirecting...</title>\n<h1>Redirecting...</h1>\n'
            '<p>You should be redirected automatically to the target URL: <a href="%s">%s</a>. '
            'If not, click the link.\n' % (escape(loc), escape(loc)))
    r = Response(body, code, mimetype="text/html")
    r.headers["Location"] = loc
    return r


# ---- routing -------------------------------------------------------------

_UUID_RE = "[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"
_CONV = {
    "string": ("[^/]+", None),
    "int": ("[0-9]+", int),
    "float": ("[0-9]+\\.[0-9]+", float),
    "path": ("[^/].*?", None),
    "uuid": (_UUID_RE, _uuid.UUID),
    "any": ("[^/]+", None),
}


class Rule:
    def __init__(self, path, methods, endpoint, view, defaults=None, strict=True):
        self.rule = path
        self.methods = methods
        self.endpoint = endpoint
        self.view = view
        self.defaults = defaults or {}
        self.parts = []   # ("lit", text) | ("var", conv, name)
        pat = ""
        pos = 0
        weight = 0
        for m in _re.finditer("<(?:([a-zA-Z_][a-zA-Z0-9_]*)(?:\\(([^)]*)\\))?:)?([a-zA-Z_][a-zA-Z0-9_]*)>", path):
            lit = path[pos:m.start()]
            pat += _re.escape(lit)
            weight += len(lit)
            if lit:
                self.parts.append(("lit", lit))
            conv = m.group(1) or "string"
            if conv not in _CONV:
                raise LookupError("the converter %r does not exist" % conv)
            pat += "(" + _CONV[conv][0] + ")"
            self.parts.append(("var", conv, m.group(3)))
            pos = m.end()
        lit = path[pos:]
        pat += _re.escape(lit)
        weight += len(lit)
        if lit:
            self.parts.append(("lit", lit))
        self.regex = _re.compile("^" + pat + "$")
        self.slash = path.endswith("/") and strict
        self.names = [p[2] for p in self.parts if p[0] == "var"]
        self.convs = [p[1] for p in self.parts if p[0] == "var"]
        self.nvars = len(self.names)
        self.weight = weight

    def match(self, path):
        m = self.regex.match(path)
        if m is None:
            return None
        vals = {}
        for i in range(self.nvars):
            raw = m.group(i + 1)
            cv = _CONV[self.convs[i]][1]
            vals[self.names[i]] = cv(raw) if cv is not None else raw
        for k, v in self.defaults.items():
            vals.setdefault(k, v)
        return vals

    def build(self, values):
        out = ""
        used = set()
        for p in self.parts:
            if p[0] == "lit":
                out += p[1]
            else:
                if p[2] not in values:
                    return None
                v = values[p[2]]
                used.add(p[2])
                if p[1] == "path":
                    out += quote(str(v), safe="/")
                else:
                    out += quote(str(v), safe="")
        return out, used


class _Routes:
    """Decorators shared by Flask and Blueprint."""

    def route(self, rule, **options):
        def deco(f):
            self.add_url_rule(rule, options.pop("endpoint", None), f, **options)
            return f
        return deco

    def get(self, rule, **o):
        o["methods"] = ["GET"]
        return self.route(rule, **o)

    def post(self, rule, **o):
        o["methods"] = ["POST"]
        return self.route(rule, **o)

    def put(self, rule, **o):
        o["methods"] = ["PUT"]
        return self.route(rule, **o)

    def delete(self, rule, **o):
        o["methods"] = ["DELETE"]
        return self.route(rule, **o)

    def patch(self, rule, **o):
        o["methods"] = ["PATCH"]
        return self.route(rule, **o)


class Blueprint(_Routes):
    def __init__(self, name, import_name=None, url_prefix=None, **kw):
        self.name = name
        self.url_prefix = url_prefix
        self._rules = []
        self._before = []
        self._after = []
        self._errors = []
        self._blueprints = []

    def add_url_rule(self, rule, endpoint=None, view_func=None, **options):
        if endpoint is None:
            endpoint = view_func.__name__
        self._rules.append((rule, endpoint, view_func, options))

    def before_request(self, f):
        self._before.append(f)
        return f

    def after_request(self, f):
        self._after.append(f)
        return f

    def errorhandler(self, code_or_exc):
        def deco(f):
            self._errors.append((code_or_exc, f))
            return f
        return deco

    def register_blueprint(self, bp, **options):
        self._blueprints.append((bp, options))


def _join(prefix, rule):
    if not prefix:
        return rule
    return prefix.rstrip("/") + "/" + rule.lstrip("/") if rule else prefix


class Flask(_Routes):
    def __init__(self, import_name=None, static_folder=None, template_folder=None, **kw):
        self.import_name = import_name
        self.name = import_name
        self.config = {"DEBUG": False, "TESTING": False}
        self.debug = False
        self.url_map = []
        self.view_functions = {}
        self._before = []
        self._after = []
        self._teardown = []
        self._errors = {}      # code -> handler
        self._exc_errors = []  # (class, handler)
        self._bp_before = {}
        self._bp_after = {}
        self._bp_errors = {}
        self._sorted = None
        self.json = None

    # -- registration
    def add_url_rule(self, rule, endpoint=None, view_func=None, **options):
        if endpoint is None:
            endpoint = view_func.__name__
        methods = options.get("methods")
        if methods is None:
            methods = ["GET"]
        methods = set(m.upper() for m in methods)
        if "GET" in methods:
            methods.add("HEAD")
        explicit_options = "OPTIONS" in methods
        methods.add("OPTIONS")
        strict = options.get("strict_slashes")
        r = Rule(rule, methods, endpoint, view_func, options.get("defaults"),
                 True if strict is None else strict)
        r.auto_options = not explicit_options
        self.url_map.append(r)
        self.view_functions[endpoint] = view_func
        self._sorted = None

    def register_blueprint(self, bp, url_prefix=None, name=None, **options):
        self._register_bp(bp, url_prefix if url_prefix is not None else bp.url_prefix, name or bp.name)

    def _register_bp(self, bp, prefix, name):
        for rule, ep, view, opts in bp._rules:
            self.add_url_rule(_join(prefix, rule), name + "." + ep, view, **opts)
        self._bp_before[name] = list(bp._before)
        self._bp_after[name] = list(bp._after)
        self._bp_errors[name] = list(bp._errors)
        for sub, opts in bp._blueprints:
            sp = opts.get("url_prefix", sub.url_prefix)
            self._register_bp(sub, _join(prefix, sp or ""), name + "." + sub.name)

    def before_request(self, f):
        self._before.append(f)
        return f

    def after_request(self, f):
        self._after.append(f)
        return f

    def teardown_request(self, f):
        self._teardown.append(f)
        return f

    def errorhandler(self, code_or_exc):
        def deco(f):
            self.register_error_handler(code_or_exc, f)
            return f
        return deco

    def register_error_handler(self, code_or_exc, f):
        if isinstance(code_or_exc, int):
            self._errors[code_or_exc] = f
        else:
            self._exc_errors.append((code_or_exc, f))

    # -- helpers
    def make_response(self, rv):
        return _make_response(self, rv)

    def test_client(self, **kw):
        return FlaskClient(self)

    def test_request_context(self, path="/", method="GET", **kw):
        return _TestContext(self, path, method)

    def app_context(self):
        return _TestContext(self, "/", "GET")

    def url_for(self, endpoint, **values):
        return _url_for(self, endpoint, values)

    def run(self, host="127.0.0.1", port=5000, debug=None, **kw):
        import _cn1server
        if debug is not None:
            self.debug = bool(debug)
        _cn1server.serve(self.dispatch, host, port)

    # -- request handling
    def _rules_sorted(self):
        if self._sorted is None:
            self._sorted = sorted(self.url_map, key=lambda r: (r.nvars, -r.weight))
        return self._sorted

    def _route(self, req):
        path = req.path
        allowed = set()
        redirect_rule = None
        for r in self._rules_sorted():
            vals = r.match(path)
            if vals is not None:
                if req.method in r.methods:
                    return r, vals, None
                allowed |= r.methods
            elif r.slash and not path.endswith("/"):
                if r.match(path + "/") is not None and redirect_rule is None:
                    if req.method in r.methods:
                        redirect_rule = r
                    else:
                        allowed |= r.methods
        if redirect_rule is not None:
            return None, None, ("redirect", redirect_rule)
        if allowed:
            return None, None, ("405", sorted(allowed))
        return None, None, ("404", None)

    def _find_handler(self, exc, bp):
        names = []
        if bp:
            parts = bp.split(".")
            for i in range(len(parts), 0, -1):
                names.append(".".join(parts[:i]))
        for scope in names:
            h = self._scoped_handler(self._bp_errors.get(scope, []), exc)
            if h is not None:
                return h
        h = self._scoped_handler([(c, f) for c, f in self._errors.items()] + self._exc_errors, exc)
        return h

    def _scoped_handler(self, entries, exc):
        best = None
        for key, f in entries:
            if isinstance(key, int):
                if isinstance(exc, HTTPException) and exc.code == key:
                    return f
            elif isinstance(exc, key):
                if best is None or issubclass(key, best[0]):
                    best = (key, f)
        return best[1] if best else None

    def _handle_exception(self, exc, bp):
        if isinstance(exc, HTTPException):
            h = self._find_handler(exc, bp)
            if h is None:
                return exc.get_response()
            return _make_response(self, h(exc))
        h = self._find_handler(exc, bp)
        if h is not None:
            return _make_response(self, h(exc))
        if self.config.get("PROPAGATE_EXCEPTIONS") or self.config.get("TESTING"):
            raise exc
        ise = InternalServerError()
        ise.original_exception = exc
        h = self._find_handler(ise, bp)
        if h is not None:
            return _make_response(self, h(ise))
        return ise.get_response()

    def full_dispatch(self, req):
        bp = None
        try:
            rule, vals, fail = self._route(req)
            rv = None
            chain = []
            if fail is None:
                req.url_rule = rule
                req.endpoint = rule.endpoint
                req.view_args = vals
                bp = req.blueprint
                if bp:
                    parts = bp.split(".")
                    for i in range(1, len(parts) + 1):
                        chain.append(".".join(parts[:i]))
            for f in self._before:
                rv = f()
                if rv is not None:
                    break
            if rv is None:
                for sc in chain:
                    for f in self._bp_before.get(sc, []):
                        rv = f()
                        if rv is not None:
                            break
                    if rv is not None:
                        break
            if rv is None and fail is not None:
                kind, info = fail
                if kind == "redirect":
                    loc = "http://" + req.host + quote(req.path) + "/"
                    q = req.query_string.decode("utf-8", "replace")
                    if q:
                        loc += "?" + q
                    return self._finish(redirect(loc, 308), None, [])
                if kind == "405":
                    raise MethodNotAllowed(info)
                raise NotFound()
            if rv is None:
                if req.method == "OPTIONS" and rule.auto_options:
                    resp = Response()
                    resp.headers["Allow"] = ", ".join(sorted(self._allowed_for(req.path)))
                    resp.headers["Content-Length"] = "0"
                    return self._finish(resp, bp, chain)
                rv = rule.view(**vals)
            resp = _make_response(self, rv)
        except Exception as e:
            resp = self._handle_exception(e, bp)
            chain = []
        return self._finish(resp, bp, chain if bp else [])

    def _allowed_for(self, path):
        out = set()
        for r in self.url_map:
            if r.match(path) is not None:
                out |= r.methods
        return out

    def _finish(self, resp, bp, chain):
        try:
            for sc in reversed(chain):
                for f in reversed(self._bp_after.get(sc, [])):
                    r2 = f(resp)
                    if r2 is not None:
                        resp = r2
            for f in reversed(self._after):
                r2 = f(resp)
                if r2 is not None:
                    resp = r2
        except Exception as e:
            resp = self._handle_exception(e, bp)
        return resp

    def dispatch(self, method, path, query, headers, body):
        method = method.upper()
        path = unquote(path)
        req = Request(method, path, query, headers, body, self)
        ctx = _Ctx(self, req)
        _stack.append(ctx)
        try:
            real = "GET" if method == "HEAD" else method
            req.method = method
            if method == "HEAD":
                req.method = "HEAD"
            resp = self.full_dispatch(req)
            for f in self._teardown:
                try:
                    f(None)
                except Exception:
                    pass
        finally:
            _stack.pop()
        if method == "HEAD":
            hdrs = resp.headers.items()
            return resp.status_code, hdrs, b""
        if "Content-Length" not in resp.headers and resp.status_code not in (204, 304):
            resp.headers["Content-Length"] = str(len(resp._data))
        return resp.status_code, resp.headers.items(), resp._data


class _TestContext:
    def __init__(self, app, path, method):
        self.ctx = _Ctx(app, Request(method, path, "", [], b"", app))

    def __enter__(self):
        _stack.append(self.ctx)
        return self

    def __exit__(self, *a):
        _stack.pop()
        return False


def _url_for(app, endpoint, values):
    external = values.pop("_external", False)
    values.pop("_scheme", None)
    anchor = values.pop("_anchor", None)
    values.pop("_method", None)
    if endpoint.startswith("."):
        cur = _stack[-1].request.blueprint if _stack else None
        endpoint = (cur + endpoint) if cur else endpoint[1:]
    best = None
    for r in app.url_map:
        if r.endpoint != endpoint:
            continue
        b = r.build(values)
        if b is None:
            continue
        if best is None or len(b[1]) > len(best[1]):
            best = b
    if best is None:
        raise BuildError(endpoint, values)
    url, used = best
    rest = [(k, v) for k, v in values.items() if k not in used and v is not None]
    if rest:
        url += "?" + urlencode(rest)
    if anchor:
        url += "#" + quote(str(anchor))
    if external:
        host = _stack[-1].request.host if _stack else "localhost"
        url = "http://" + host + url
    return url


class BuildError(Exception):
    def __init__(self, endpoint, values):
        Exception.__init__(self, "Could not build url for endpoint %r with values %r." % (endpoint, sorted(values)))


def url_for(endpoint, **values):
    if not _stack:
        raise RuntimeError("Attempted to generate a URL without the application context being pushed.")
    return _url_for(_stack[-1].app, endpoint, values)


# ---- test client ---------------------------------------------------------

class FlaskClient:
    def __init__(self, app):
        self.application = app
        self._cookies = {}

    def get(self, *a, **k):
        return self.open(*a, method="GET", **k)

    def post(self, *a, **k):
        return self.open(*a, method="POST", **k)

    def put(self, *a, **k):
        return self.open(*a, method="PUT", **k)

    def delete(self, *a, **k):
        return self.open(*a, method="DELETE", **k)

    def patch(self, *a, **k):
        return self.open(*a, method="PATCH", **k)

    def head(self, *a, **k):
        return self.open(*a, method="HEAD", **k)

    def options(self, *a, **k):
        return self.open(*a, method="OPTIONS", **k)

    def set_cookie(self, key, value="", **kw):
        self._cookies[key] = value

    def open(self, path="/", method="GET", json=None, data=None, query_string=None, headers=None,
             content_type=None, follow_redirects=False, **kw):
        query = ""
        if "?" in path:
            path, query = path.split("?", 1)
        if "://" in path:
            path = "/" + path.split("://", 1)[1].split("/", 1)[-1] if "/" in path.split("://", 1)[1] else "/"
        if query_string is not None:
            if isinstance(query_string, dict):
                query_string = urlencode(query_string, doseq=True)
            query = query_string
        hdrs = Headers([("Host", "localhost")])
        if headers is not None:
            for k, v in (headers.items() if hasattr(headers, "items") else headers):
                hdrs[k] = v
        body = b""
        if json is not None:
            body = _to_json(json, True).encode("utf-8")
            content_type = content_type or "application/json"
        elif data is not None:
            if isinstance(data, dict):
                body = urlencode(data, doseq=True).encode("utf-8")
                content_type = content_type or "application/x-www-form-urlencoded"
            else:
                body = _b(data)
        if content_type:
            hdrs["Content-Type"] = content_type
        if body or method in ("POST", "PUT", "PATCH"):
            hdrs["Content-Length"] = str(len(body))
        if self._cookies and "Cookie" not in hdrs:
            hdrs["Cookie"] = "; ".join(k + "=" + v for k, v in self._cookies.items())
        status, rh, rb = self.application.dispatch(method, path, query, hdrs.items(), body)
        resp = TestResponse(rb, status, rh)
        for k, v in resp.headers.items():
            if k.lower() == "set-cookie":
                kv = v.split(";", 1)[0]
                if "=" in kv:
                    ck, cv = kv.split("=", 1)
                    if "max-age=0" in v.lower():
                        self._cookies.pop(ck, None)
                    else:
                        self._cookies[ck] = cv
        if follow_redirects and resp.status_code in (301, 302, 303, 307, 308):
            loc = resp.headers.get("Location")
            if "://" in loc:
                loc = "/" + loc.split("://", 1)[1].split("/", 1)[1] if "/" in loc.split("://", 1)[1] else "/"
            return self.open(loc, method="GET" if resp.status_code in (301, 302, 303) else method,
                             follow_redirects=True)
        return resp

    def __enter__(self):
        return self

    def __exit__(self, *a):
        return False


class TestResponse(Response):
    __test__ = False

    def __init__(self, body, status, headers):
        Response.__init__(self)
        self.headers = Headers(headers)
        self._data = bytes(body)
        self.status_code = status
