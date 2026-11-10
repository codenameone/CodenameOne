# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The exceptions of requests, in its hierarchy."""

import json as _json


class RequestException(IOError):
    def __init__(self, *args, response=None, request=None):
        super().__init__(*args)
        self.response = response
        self.request = request
        if request is None and response is not None and hasattr(response, "request"):
            self.request = response.request


class InvalidJSONError(RequestException):
    pass


class JSONDecodeError(InvalidJSONError, _json.JSONDecodeError):
    def __init__(self, msg, doc, pos, response=None, request=None):
        _json.JSONDecodeError.__init__(self, msg, doc, pos)
        self.response = response
        self.request = request
        self.args = (_json.JSONDecodeError.__str__(self),)

    def __str__(self):
        return _json.JSONDecodeError.__str__(self)


class HTTPError(RequestException):
    pass


class ConnectionError(RequestException):
    pass


class ProxyError(ConnectionError):
    pass


class SSLError(ConnectionError):
    pass


class Timeout(RequestException):
    pass


class ConnectTimeout(ConnectionError, Timeout):
    pass


class ReadTimeout(Timeout):
    pass


class URLRequired(RequestException):
    pass


class TooManyRedirects(RequestException):
    pass


class MissingSchema(RequestException, ValueError):
    pass


class InvalidSchema(RequestException, ValueError):
    pass


class InvalidURL(RequestException, ValueError):
    pass


class InvalidHeader(RequestException, ValueError):
    pass


class ChunkedEncodingError(RequestException):
    pass


class ContentDecodingError(RequestException):
    pass


class StreamConsumedError(RequestException, TypeError):
    pass


class RetryError(RequestException):
    pass
