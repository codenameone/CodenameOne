# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""FastAPI's exceptions."""
from starlette.exceptions import HTTPException as StarletteHTTPException


class HTTPException(StarletteHTTPException):
    pass


class ValidationException(Exception):
    def __init__(self, errors):
        Exception.__init__(self, errors)
        self._errors = errors

    def errors(self):
        return self._errors


class RequestValidationError(ValidationException):
    def __init__(self, errors, *, body=None):
        ValidationException.__init__(self, errors)
        self.body = body


class ResponseValidationError(ValidationException):
    def __init__(self, errors, *, body=None):
        ValidationException.__init__(self, errors)
        self.body = body

    def __str__(self):
        return "%d validation errors:\n%s" % (len(self._errors), "\n".join("  " + repr(e) for e in self._errors))


class FastAPIError(RuntimeError):
    pass


class WebSocketRequestValidationError(ValidationException):
    pass


class WebSocketException(Exception):
    def __init__(self, code, reason=None):
        Exception.__init__(self)
        self.code = code
        self.reason = reason or ""

    def __str__(self):
        return "%d: %s" % (self.code, self.reason)

    def __repr__(self):
        return "WebSocketException(code=%d, reason=%r)" % (self.code, self.reason)
