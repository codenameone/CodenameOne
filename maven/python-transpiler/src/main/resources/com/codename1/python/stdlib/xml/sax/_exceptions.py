# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/sax/_exceptions.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""xml.sax._exceptions: the SAX exception classes."""


class SAXException(Exception):
    def __init__(self, msg, exception=None):
        self._msg = msg
        self._exception = exception
        Exception.__init__(self, msg)

    def getMessage(self):
        return self._msg

    def getException(self):
        return self._exception

    def __str__(self):
        return self._msg

    def __getitem__(self, ix):
        raise AttributeError("__getitem__")


class SAXParseException(SAXException):
    def __init__(self, msg, exception, locator):
        SAXException.__init__(self, msg, exception)
        self._locator = locator
        self._systemId = self._locator.getSystemId()
        self._colnum = self._locator.getColumnNumber()
        self._linenum = self._locator.getLineNumber()

    def getColumnNumber(self):
        return self._colnum

    def getLineNumber(self):
        return self._linenum

    def getPublicId(self):
        return self._locator.getPublicId()

    def getSystemId(self):
        return self._systemId

    def __str__(self):
        sysid = self.getSystemId()
        if sysid is None:
            sysid = "<unknown>"
        linenum = self.getLineNumber()
        if linenum is None:
            linenum = "?"
        colnum = self.getColumnNumber()
        if colnum is None:
            colnum = "?"
        return "%s:%s:%s: %s" % (sysid, linenum, colnum, self._msg)


class SAXNotRecognizedException(SAXException):
    pass


class SAXNotSupportedException(SAXException):
    pass


class SAXReaderNotAvailable(SAXNotSupportedException):
    pass
