# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""HTTP status codes and methods."""

from enum import IntEnum, StrEnum

__all__ = ["HTTPStatus", "HTTPMethod"]

_CODES = [
    (100, "CONTINUE", "Continue"), (101, "SWITCHING_PROTOCOLS", "Switching Protocols"),
    (102, "PROCESSING", "Processing"), (103, "EARLY_HINTS", "Early Hints"),
    (200, "OK", "OK"), (201, "CREATED", "Created"), (202, "ACCEPTED", "Accepted"),
    (203, "NON_AUTHORITATIVE_INFORMATION", "Non-Authoritative Information"),
    (204, "NO_CONTENT", "No Content"), (205, "RESET_CONTENT", "Reset Content"),
    (206, "PARTIAL_CONTENT", "Partial Content"), (207, "MULTI_STATUS", "Multi-Status"),
    (208, "ALREADY_REPORTED", "Already Reported"), (226, "IM_USED", "IM Used"),
    (300, "MULTIPLE_CHOICES", "Multiple Choices"), (301, "MOVED_PERMANENTLY", "Moved Permanently"),
    (302, "FOUND", "Found"), (303, "SEE_OTHER", "See Other"), (304, "NOT_MODIFIED", "Not Modified"),
    (305, "USE_PROXY", "Use Proxy"), (307, "TEMPORARY_REDIRECT", "Temporary Redirect"),
    (308, "PERMANENT_REDIRECT", "Permanent Redirect"),
    (400, "BAD_REQUEST", "Bad Request"), (401, "UNAUTHORIZED", "Unauthorized"),
    (402, "PAYMENT_REQUIRED", "Payment Required"), (403, "FORBIDDEN", "Forbidden"),
    (404, "NOT_FOUND", "Not Found"), (405, "METHOD_NOT_ALLOWED", "Method Not Allowed"),
    (406, "NOT_ACCEPTABLE", "Not Acceptable"),
    (407, "PROXY_AUTHENTICATION_REQUIRED", "Proxy Authentication Required"),
    (408, "REQUEST_TIMEOUT", "Request Timeout"), (409, "CONFLICT", "Conflict"), (410, "GONE", "Gone"),
    (411, "LENGTH_REQUIRED", "Length Required"), (412, "PRECONDITION_FAILED", "Precondition Failed"),
    (413, "CONTENT_TOO_LARGE", "Content Too Large"), (414, "URI_TOO_LONG", "URI Too Long"),
    (415, "UNSUPPORTED_MEDIA_TYPE", "Unsupported Media Type"),
    (416, "RANGE_NOT_SATISFIABLE", "Range Not Satisfiable"),
    (417, "EXPECTATION_FAILED", "Expectation Failed"), (418, "IM_A_TEAPOT", "I'm a Teapot"),
    (421, "MISDIRECTED_REQUEST", "Misdirected Request"),
    (422, "UNPROCESSABLE_CONTENT", "Unprocessable Content"), (423, "LOCKED", "Locked"),
    (424, "FAILED_DEPENDENCY", "Failed Dependency"), (425, "TOO_EARLY", "Too Early"),
    (426, "UPGRADE_REQUIRED", "Upgrade Required"), (428, "PRECONDITION_REQUIRED", "Precondition Required"),
    (429, "TOO_MANY_REQUESTS", "Too Many Requests"),
    (431, "REQUEST_HEADER_FIELDS_TOO_LARGE", "Request Header Fields Too Large"),
    (451, "UNAVAILABLE_FOR_LEGAL_REASONS", "Unavailable For Legal Reasons"),
    (500, "INTERNAL_SERVER_ERROR", "Internal Server Error"), (501, "NOT_IMPLEMENTED", "Not Implemented"),
    (502, "BAD_GATEWAY", "Bad Gateway"), (503, "SERVICE_UNAVAILABLE", "Service Unavailable"),
    (504, "GATEWAY_TIMEOUT", "Gateway Timeout"),
    (505, "HTTP_VERSION_NOT_SUPPORTED", "HTTP Version Not Supported"),
    (506, "VARIANT_ALSO_NEGOTIATES", "Variant Also Negotiates"),
    (507, "INSUFFICIENT_STORAGE", "Insufficient Storage"), (508, "LOOP_DETECTED", "Loop Detected"),
    (510, "NOT_EXTENDED", "Not Extended"),
    (511, "NETWORK_AUTHENTICATION_REQUIRED", "Network Authentication Required"),
]

_PHRASES = {code: phrase for code, _, phrase in _CODES}

# Class statements, not the functional API: every member is defined once by
# the compiler and is a typed static of its class.
class HTTPStatus(IntEnum):
    CONTINUE = 100
    SWITCHING_PROTOCOLS = 101
    PROCESSING = 102
    EARLY_HINTS = 103
    OK = 200
    CREATED = 201
    ACCEPTED = 202
    NON_AUTHORITATIVE_INFORMATION = 203
    NO_CONTENT = 204
    RESET_CONTENT = 205
    PARTIAL_CONTENT = 206
    MULTI_STATUS = 207
    ALREADY_REPORTED = 208
    IM_USED = 226
    MULTIPLE_CHOICES = 300
    MOVED_PERMANENTLY = 301
    FOUND = 302
    SEE_OTHER = 303
    NOT_MODIFIED = 304
    USE_PROXY = 305
    TEMPORARY_REDIRECT = 307
    PERMANENT_REDIRECT = 308
    BAD_REQUEST = 400
    UNAUTHORIZED = 401
    PAYMENT_REQUIRED = 402
    FORBIDDEN = 403
    NOT_FOUND = 404
    METHOD_NOT_ALLOWED = 405
    NOT_ACCEPTABLE = 406
    PROXY_AUTHENTICATION_REQUIRED = 407
    REQUEST_TIMEOUT = 408
    CONFLICT = 409
    GONE = 410
    LENGTH_REQUIRED = 411
    PRECONDITION_FAILED = 412
    CONTENT_TOO_LARGE = 413
    URI_TOO_LONG = 414
    UNSUPPORTED_MEDIA_TYPE = 415
    RANGE_NOT_SATISFIABLE = 416
    EXPECTATION_FAILED = 417
    IM_A_TEAPOT = 418
    MISDIRECTED_REQUEST = 421
    UNPROCESSABLE_CONTENT = 422
    LOCKED = 423
    FAILED_DEPENDENCY = 424
    TOO_EARLY = 425
    UPGRADE_REQUIRED = 426
    PRECONDITION_REQUIRED = 428
    TOO_MANY_REQUESTS = 429
    REQUEST_HEADER_FIELDS_TOO_LARGE = 431
    UNAVAILABLE_FOR_LEGAL_REASONS = 451
    INTERNAL_SERVER_ERROR = 500
    NOT_IMPLEMENTED = 501
    BAD_GATEWAY = 502
    SERVICE_UNAVAILABLE = 503
    GATEWAY_TIMEOUT = 504
    HTTP_VERSION_NOT_SUPPORTED = 505
    VARIANT_ALSO_NEGOTIATES = 506
    INSUFFICIENT_STORAGE = 507
    LOOP_DETECTED = 508
    NOT_EXTENDED = 510
    NETWORK_AUTHENTICATION_REQUIRED = 511

    @property
    def phrase(self):
        return _PHRASES.get(self.value, "")

    @property
    def is_informational(self):
        return 100 <= self.value < 200

    @property
    def is_success(self):
        return 200 <= self.value < 300

    @property
    def is_redirection(self):
        return 300 <= self.value < 400

    @property
    def is_client_error(self):
        return 400 <= self.value < 500

    @property
    def is_server_error(self):
        return 500 <= self.value < 600


class HTTPMethod(StrEnum):
    CONNECT = "CONNECT"
    DELETE = "DELETE"
    GET = "GET"
    HEAD = "HEAD"
    OPTIONS = "OPTIONS"
    PATCH = "PATCH"
    POST = "POST"
    PUT = "PUT"
    TRACE = "TRACE"
