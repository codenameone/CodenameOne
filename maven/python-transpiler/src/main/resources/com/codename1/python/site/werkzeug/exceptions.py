# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Clean-room subset of werkzeug: HTTP exceptions with werkzeug's default pages."""


def _escape(s):
    """markupsafe-style escaping (single quote is the decimal reference)."""
    return (str(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace('"', "&#34;").replace("'", "&#39;"))

_DESCRIPTIONS = {
    301: ("Moved Permanently", "The resource has moved permanently."),
    302: ("Found", "The resource has moved temporarily."),
    304: ("Not Modified", ""),
    307: ("Temporary Redirect", "The resource has moved temporarily."),
    308: ("Permanent Redirect", "The resource has moved permanently."),
    400: ("Bad Request", "The browser (or proxy) sent a request that this server could not understand."),
    401: ("Unauthorized", "The server could not verify that you are authorized to access the URL requested. You either supplied the wrong credentials (e.g. a bad password), or your browser doesn't understand how to supply the credentials required."),
    403: ("Forbidden", "You don't have the permission to access the requested resource. It is either read-protected or not readable by the server."),
    404: ("Not Found", "The requested URL was not found on the server. If you entered the URL manually please check your spelling and try again."),
    405: ("Method Not Allowed", "The method is not allowed for the requested URL."),
    406: ("Not Acceptable", "The resource identified by the request is only capable of generating response entities which have content characteristics not acceptable according to the accept headers sent in the request."),
    408: ("Request Timeout", "The server closed the network connection because the browser didn't finish the request within the specified time."),
    409: ("Conflict", "A conflict happened while processing the request. The resource might have been modified while the request was being processed."),
    410: ("Gone", "The requested URL is no longer available on this server and there is no forwarding address. If you followed a link from a foreign page, please contact the author of this page."),
    411: ("Length Required", "A request with this method requires a valid Content-Length header."),
    412: ("Precondition Failed", "The precondition on the request for the URL failed positive evaluation."),
    413: ("Request Entity Too Large", "The data value transmitted exceeds the capacity limit."),
    415: ("Unsupported Media Type", "The server does not support the media type transmitted in the request."),
    418: ("I'm a teapot", "This server is a teapot, not a coffee machine"),
    422: ("Unprocessable Entity", "The request was well-formed but was unable to be followed due to semantic errors."),
    429: ("Too Many Requests", "This user has exceeded an allotted request count. Try again later."),
    500: ("Internal Server Error", "The server encountered an internal error and was unable to complete your request. Either the server is overloaded or there is an error in the application."),
    501: ("Not Implemented", "The server does not support the action requested by the browser."),
    502: ("Bad Gateway", "The proxy server received an invalid response from an upstream server."),
    503: ("Service Unavailable", "The server is temporarily unable to service your request due to maintenance downtime or capacity problems. Please try again later."),
    504: ("Gateway Timeout", "The connection to an upstream server timed out."),
}


class HTTPException(Exception):
    code = None
    description = None

    def __init__(self, description=None, response=None):
        Exception.__init__(self)
        if description is not None:
            self.description = description
        self.response = response

    @property
    def name(self):
        e = _DESCRIPTIONS.get(self.code)
        return e[0] if e else "Unknown Error"

    def _desc(self):
        d = self.description
        if d is None:
            e = _DESCRIPTIONS.get(self.code)
            d = e[1] if e else ""
        return d

    def get_description(self):
        return "<p>" + _escape(self._desc()).replace("\n", "<br>\n") + "</p>"

    def get_body(self):
        return ("<!doctype html>\n<html lang=en>\n<title>%d %s</title>\n<h1>%s</h1>\n%s\n"
                % (self.code, self.name, self.name, self.get_description()))

    def get_headers(self):
        return [("Content-Type", "text/html; charset=utf-8")]

    def get_response(self):
        if self.response is not None:
            return self.response
        from flask import Response
        return Response(self.get_body(), self.code, self.get_headers())

    def __str__(self):
        return "%d %s: %s" % (self.code, self.name, self._desc())

    def __repr__(self):
        return "<%s '%d: %s'>" % (type(self).__name__, self.code, self.name)


def _make(name, code):
    return type(name, (HTTPException,), {"code": code})


BadRequest = _make("BadRequest", 400)
Unauthorized = _make("Unauthorized", 401)
Forbidden = _make("Forbidden", 403)
NotFound = _make("NotFound", 404)
class MethodNotAllowed(HTTPException):
    code = 405
    valid_methods = None

    def __init__(self, valid_methods=None, description=None, response=None):
        HTTPException.__init__(self, description, response)
        self.valid_methods = valid_methods

    def get_headers(self):
        h = HTTPException.get_headers(self)
        if self.valid_methods:
            h.append(("Allow", ", ".join(self.valid_methods)))
        return h


NotAcceptable = _make("NotAcceptable", 406)
RequestTimeout = _make("RequestTimeout", 408)
Conflict = _make("Conflict", 409)
Gone = _make("Gone", 410)
LengthRequired = _make("LengthRequired", 411)
PreconditionFailed = _make("PreconditionFailed", 412)
RequestEntityTooLarge = _make("RequestEntityTooLarge", 413)
UnsupportedMediaType = _make("UnsupportedMediaType", 415)
ImATeapot = _make("ImATeapot", 418)
UnprocessableEntity = _make("UnprocessableEntity", 422)
TooManyRequests = _make("TooManyRequests", 429)
InternalServerError = _make("InternalServerError", 500)
NotImplemented = _make("NotImplemented", 501)
BadGateway = _make("BadGateway", 502)
ServiceUnavailable = _make("ServiceUnavailable", 503)
GatewayTimeout = _make("GatewayTimeout", 504)

default_exceptions = {}
for _c in (BadRequest, Unauthorized, Forbidden, NotFound, MethodNotAllowed, NotAcceptable, RequestTimeout,
           Conflict, Gone, LengthRequired, PreconditionFailed, RequestEntityTooLarge, UnsupportedMediaType,
           ImATeapot, UnprocessableEntity, TooManyRequests, InternalServerError, NotImplemented, BadGateway,
           ServiceUnavailable, GatewayTimeout):
    default_exceptions[_c.code] = _c


def abort(status, *args, **kwargs):
    if isinstance(status, Exception):
        raise status
    cls = default_exceptions.get(status)
    if cls is None:
        raise LookupError("no exception for %r" % (status,))
    raise cls(*args, **kwargs)
