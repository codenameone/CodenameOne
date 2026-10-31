# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""HTTP client: HTTPConnection over the platform's HTTP client.

A request is sent whole when getresponse() is called, through the same
transport as urllib.request and requests (Codename One's ConnectionRequest
in an application), so certificates and proxies follow the platform. The
response body has been read by then, which is what read() returns from.
"""

import _cn1http
import email.message as _email_message
from http import HTTPStatus

__all__ = ["HTTPResponse", "HTTPConnection", "HTTPSConnection", "HTTPException", "NotConnected",
           "InvalidURL", "UnknownProtocol", "IncompleteRead", "ImproperConnectionState",
           "CannotSendRequest", "CannotSendHeader", "ResponseNotReady", "BadStatusLine",
           "LineTooLong", "RemoteDisconnected", "error", "responses", "HTTPMessage",
           "HTTP_PORT", "HTTPS_PORT"]

HTTP_PORT = 80
HTTPS_PORT = 443

responses = {s.value: s.phrase for s in HTTPStatus}
CONTINUE = HTTPStatus.CONTINUE
SWITCHING_PROTOCOLS = HTTPStatus.SWITCHING_PROTOCOLS
PROCESSING = HTTPStatus.PROCESSING
EARLY_HINTS = HTTPStatus.EARLY_HINTS
OK = HTTPStatus.OK
CREATED = HTTPStatus.CREATED
ACCEPTED = HTTPStatus.ACCEPTED
NON_AUTHORITATIVE_INFORMATION = HTTPStatus.NON_AUTHORITATIVE_INFORMATION
NO_CONTENT = HTTPStatus.NO_CONTENT
RESET_CONTENT = HTTPStatus.RESET_CONTENT
PARTIAL_CONTENT = HTTPStatus.PARTIAL_CONTENT
MULTI_STATUS = HTTPStatus.MULTI_STATUS
ALREADY_REPORTED = HTTPStatus.ALREADY_REPORTED
IM_USED = HTTPStatus.IM_USED
MULTIPLE_CHOICES = HTTPStatus.MULTIPLE_CHOICES
MOVED_PERMANENTLY = HTTPStatus.MOVED_PERMANENTLY
FOUND = HTTPStatus.FOUND
SEE_OTHER = HTTPStatus.SEE_OTHER
NOT_MODIFIED = HTTPStatus.NOT_MODIFIED
USE_PROXY = HTTPStatus.USE_PROXY
TEMPORARY_REDIRECT = HTTPStatus.TEMPORARY_REDIRECT
PERMANENT_REDIRECT = HTTPStatus.PERMANENT_REDIRECT
BAD_REQUEST = HTTPStatus.BAD_REQUEST
UNAUTHORIZED = HTTPStatus.UNAUTHORIZED
PAYMENT_REQUIRED = HTTPStatus.PAYMENT_REQUIRED
FORBIDDEN = HTTPStatus.FORBIDDEN
NOT_FOUND = HTTPStatus.NOT_FOUND
METHOD_NOT_ALLOWED = HTTPStatus.METHOD_NOT_ALLOWED
NOT_ACCEPTABLE = HTTPStatus.NOT_ACCEPTABLE
PROXY_AUTHENTICATION_REQUIRED = HTTPStatus.PROXY_AUTHENTICATION_REQUIRED
REQUEST_TIMEOUT = HTTPStatus.REQUEST_TIMEOUT
CONFLICT = HTTPStatus.CONFLICT
GONE = HTTPStatus.GONE
LENGTH_REQUIRED = HTTPStatus.LENGTH_REQUIRED
PRECONDITION_FAILED = HTTPStatus.PRECONDITION_FAILED
CONTENT_TOO_LARGE = HTTPStatus.CONTENT_TOO_LARGE
URI_TOO_LONG = HTTPStatus.URI_TOO_LONG
UNSUPPORTED_MEDIA_TYPE = HTTPStatus.UNSUPPORTED_MEDIA_TYPE
RANGE_NOT_SATISFIABLE = HTTPStatus.RANGE_NOT_SATISFIABLE
EXPECTATION_FAILED = HTTPStatus.EXPECTATION_FAILED
IM_A_TEAPOT = HTTPStatus.IM_A_TEAPOT
MISDIRECTED_REQUEST = HTTPStatus.MISDIRECTED_REQUEST
UNPROCESSABLE_CONTENT = HTTPStatus.UNPROCESSABLE_CONTENT
LOCKED = HTTPStatus.LOCKED
FAILED_DEPENDENCY = HTTPStatus.FAILED_DEPENDENCY
TOO_EARLY = HTTPStatus.TOO_EARLY
UPGRADE_REQUIRED = HTTPStatus.UPGRADE_REQUIRED
PRECONDITION_REQUIRED = HTTPStatus.PRECONDITION_REQUIRED
TOO_MANY_REQUESTS = HTTPStatus.TOO_MANY_REQUESTS
REQUEST_HEADER_FIELDS_TOO_LARGE = HTTPStatus.REQUEST_HEADER_FIELDS_TOO_LARGE
UNAVAILABLE_FOR_LEGAL_REASONS = HTTPStatus.UNAVAILABLE_FOR_LEGAL_REASONS
INTERNAL_SERVER_ERROR = HTTPStatus.INTERNAL_SERVER_ERROR
NOT_IMPLEMENTED = HTTPStatus.NOT_IMPLEMENTED
BAD_GATEWAY = HTTPStatus.BAD_GATEWAY
SERVICE_UNAVAILABLE = HTTPStatus.SERVICE_UNAVAILABLE
GATEWAY_TIMEOUT = HTTPStatus.GATEWAY_TIMEOUT
HTTP_VERSION_NOT_SUPPORTED = HTTPStatus.HTTP_VERSION_NOT_SUPPORTED
VARIANT_ALSO_NEGOTIATES = HTTPStatus.VARIANT_ALSO_NEGOTIATES
INSUFFICIENT_STORAGE = HTTPStatus.INSUFFICIENT_STORAGE
LOOP_DETECTED = HTTPStatus.LOOP_DETECTED
NOT_EXTENDED = HTTPStatus.NOT_EXTENDED
NETWORK_AUTHENTICATION_REQUIRED = HTTPStatus.NETWORK_AUTHENTICATION_REQUIRED


class HTTPException(Exception):
    pass


error = HTTPException


class NotConnected(HTTPException):
    pass


class InvalidURL(HTTPException):
    pass


class UnknownProtocol(HTTPException):
    pass


class IncompleteRead(HTTPException):
    def __init__(self, partial, expected=None):
        HTTPException.__init__(self, partial)
        self.partial = partial
        self.expected = expected


class ImproperConnectionState(HTTPException):
    pass


class CannotSendRequest(ImproperConnectionState):
    pass


class CannotSendHeader(ImproperConnectionState):
    pass


class ResponseNotReady(ImproperConnectionState):
    pass


class BadStatusLine(HTTPException):
    pass


class LineTooLong(HTTPException):
    pass


class RemoteDisconnected(ConnectionResetError, BadStatusLine):
    pass


class HTTPMessage(_email_message.Message):
    def getallmatchingheaders(self, name):
        return [k + ": " + v for k, v in self.items() if k.lower() == name.lower()]


def _message(pairs):
    m = HTTPMessage()
    for k, v in pairs:
        m[k] = v
    return m


class HTTPResponse:
    def __init__(self, status, reason, url, pairs, body, method="GET"):
        self.status = status
        self.code = status
        self.reason = reason
        self.url = url
        self.headers = self.msg = _message(pairs)
        self.version = 11
        self._body = body if method != "HEAD" else b""
        self._pos = 0
        self.closed = False
        self.length = len(self._body) - self._pos
        self.chunked = False
        self.will_close = True

    def read(self, amt=None):
        if self.closed:
            return b""
        if amt is None or amt < 0:
            data = self._body[self._pos:]
            self._pos = len(self._body)
        else:
            data = self._body[self._pos:self._pos + amt]
            self._pos += len(data)
        self.length = len(self._body) - self._pos
        return data

    read1 = read

    def readinto(self, b):
        data = self.read(len(b))
        b[:len(data)] = data
        return len(data)

    def readline(self, limit=-1):
        end = self._body.find(b"\n", self._pos)
        end = len(self._body) if end < 0 else end + 1
        if limit is not None and limit >= 0:
            end = min(end, self._pos + limit)
        data = self._body[self._pos:end]
        self._pos = end
        self.length = len(self._body) - self._pos
        return data

    def readlines(self, hint=-1):
        lines = []
        while True:
            line = self.readline()
            if not line:
                return lines
            lines.append(line)

    def __iter__(self):
        return iter(self.readlines())

    def peek(self, n=0):
        return self._body[self._pos:]

    def getheader(self, name, default=None):
        values = self.headers.get_all(name)
        if not values:
            return default
        return ", ".join(values)

    def getheaders(self):
        return list(self.headers.items())

    def geturl(self):
        return self.url

    def info(self):
        return self.headers

    def getcode(self):
        return self.status

    def isclosed(self):
        return self.closed

    def readable(self):
        return True

    def fileno(self):
        raise OSError("no file descriptor")

    def close(self):
        self.closed = True

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()


class HTTPConnection:
    default_port = HTTP_PORT
    _scheme = "http"
    debuglevel = 0

    def __init__(self, host, port=None, timeout=None, source_address=None, blocksize=8192):
        if port is None:
            i = host.rfind(":")
            j = host.rfind("]")
            if i > j:
                try:
                    port = int(host[i + 1:])
                except ValueError:
                    if host[i + 1:] == "":
                        port = self.default_port
                    else:
                        raise InvalidURL("nonnumeric port: '%s'" % host[i + 1:])
                host = host[:i]
            else:
                port = self.default_port
        if host and host[0] == "[" and host[-1] == "]":
            host = host[1:-1]
        self.host = host
        self.port = port
        self.timeout = timeout
        self._method = None
        self._url = None
        self._headers = []
        self._body = []
        self._response = None
        self._tunnel_host = None

    def set_debuglevel(self, level):
        self.debuglevel = level

    def set_tunnel(self, host, port=None, headers=None):
        self._tunnel_host = host

    def connect(self):
        pass

    def close(self):
        self._method = None
        self._response = None

    def putrequest(self, method, url, skip_host=False, skip_accept_encoding=False):
        if self._method is not None:
            raise CannotSendRequest("Request-sent")
        self._method = method
        self._url = url or "/"
        self._headers = []
        self._body = []

    def putheader(self, header, *values):
        if self._method is None:
            raise CannotSendHeader()
        self._headers.append((str(header), "\r\n\t".join(str(v) for v in values)))

    def endheaders(self, message_body=None, *, encode_chunked=False):
        if message_body is not None:
            self.send(message_body)

    def send(self, data):
        if isinstance(data, str):
            data = data.encode("iso-8859-1")
        elif hasattr(data, "read"):
            data = data.read()
            if isinstance(data, str):
                data = data.encode("iso-8859-1")
        elif not isinstance(data, (bytes, bytearray, memoryview)):
            data = b"".join(data)
        self._body.append(bytes(data))

    def request(self, method, url, body=None, headers={}, *, encode_chunked=False):
        self.putrequest(method, url)
        names = {k.lower() for k in headers}
        for k, v in headers.items():
            self.putheader(k, v)
        if body is not None and isinstance(body, str):
            body = body.encode("iso-8859-1")
        if body is not None and "content-length" not in names and isinstance(body, (bytes, bytearray)):
            self.putheader("Content-Length", str(len(body)))
        self.endheaders(body)

    def _full_url(self):
        if self._url.startswith("http://") or self._url.startswith("https://"):
            return self._url
        host = self.host
        if ":" in host:
            host = "[" + host + "]"
        port = "" if self.port == self.default_port else ":%d" % self.port
        return "%s://%s%s%s" % (self._scheme, host, port, self._url)

    def getresponse(self):
        if self._method is None:
            raise ResponseNotReady("Idle")
        method = self._method
        body = b"".join(self._body) if self._body else None
        if body is None and method in ("POST", "PUT", "PATCH"):
            body = b""
        try:
            status, reason, url, pairs, content = _cn1http.request(
                method, self._full_url(), self._headers, body, self.timeout, False)
        except OSError as e:
            m = str(e).lower()
            if "timed out" in m or "timeout" in m:
                raise TimeoutError(str(e)) from None
            raise ConnectionError(str(e)) from None
        finally:
            self._method = None
        if not reason:
            reason = responses.get(status, "")
        self._response = HTTPResponse(status, reason, url, pairs, content, method)
        return self._response


class HTTPSConnection(HTTPConnection):
    default_port = HTTPS_PORT
    _scheme = "https"

    def __init__(self, host, port=None, *, timeout=None, source_address=None, context=None, blocksize=8192):
        HTTPConnection.__init__(self, host, port, timeout, source_address, blocksize)
        self.context = context
