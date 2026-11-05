# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""HTTP exceptions."""

_PHRASES = {
    200: "OK", 201: "Created", 202: "Accepted", 204: "No Content", 301: "Moved Permanently",
    302: "Found", 303: "See Other", 304: "Not Modified", 307: "Temporary Redirect",
    308: "Permanent Redirect", 400: "Bad Request", 401: "Unauthorized", 402: "Payment Required",
    403: "Forbidden", 404: "Not Found", 405: "Method Not Allowed", 406: "Not Acceptable",
    408: "Request Timeout", 409: "Conflict", 410: "Gone", 411: "Length Required",
    412: "Precondition Failed", 413: "Content Too Large", 415: "Unsupported Media Type",
    418: "I'm a Teapot", 422: "Unprocessable Content", 429: "Too Many Requests",
    500: "Internal Server Error", 501: "Not Implemented", 502: "Bad Gateway",
    503: "Service Unavailable", 504: "Gateway Timeout",
}


def phrase(code):
    return _PHRASES.get(code, "")


class HTTPException(Exception):
    def __init__(self, status_code, detail=None, headers=None):
        if detail is None:
            detail = phrase(status_code)
        self.status_code = status_code
        self.detail = detail
        self.headers = headers

    def __str__(self):
        return "%d: %s" % (self.status_code, self.detail)

    def __repr__(self):
        return "%s(status_code=%r, detail=%r)" % (type(self).__name__, self.status_code, self.detail)


class WebSocketException(Exception):
    def __init__(self, code, reason=None):
        self.code = code
        self.reason = reason or ""
