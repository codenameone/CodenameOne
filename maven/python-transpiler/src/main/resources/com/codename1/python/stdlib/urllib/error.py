# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Exceptions raised by urllib.request."""

import io
from http.client import HTTPResponse

__all__ = ["URLError", "HTTPError", "ContentTooShortError"]


class URLError(OSError):
    def __init__(self, reason, filename=None):
        self.args = (reason,)
        self.reason = reason
        if filename is not None:
            self.filename = filename

    def __str__(self):
        return "<urlopen error %s>" % self.reason


class HTTPError(URLError):
    """A response with an error status; also usable as the response."""

    def __init__(self, url, code, msg, hdrs, fp):
        self.code = code
        self.msg = msg
        self.hdrs = hdrs
        self.fp = fp
        self.filename = url
        self.args = (url, code, msg, hdrs, fp)

    def __str__(self):
        return "HTTP Error %s: %s" % (self.code, self.msg)

    def __repr__(self):
        return "<HTTPError %s: %r>" % (self.code, self.msg)

    @property
    def reason(self):
        return self.msg

    @property
    def headers(self):
        return self.hdrs

    @property
    def status(self):
        return self.code

    def getcode(self):
        return self.code

    def geturl(self):
        return self.filename

    def info(self):
        return self.hdrs

    def read(self, amt=None):
        return self.fp.read(amt) if self.fp is not None else b""

    def close(self):
        if self.fp is not None:
            self.fp.close()

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()


class ContentTooShortError(URLError):
    def __init__(self, message, content):
        URLError.__init__(self, message)
        self.content = content
