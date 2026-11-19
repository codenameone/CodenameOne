# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/sax/__init__.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""xml.sax: the SAX2 interface over the pure-Python expat parser."""

from xml.sax.xmlreader import InputSource
from xml.sax.handler import ContentHandler, ErrorHandler
from xml.sax._exceptions import (SAXException, SAXNotRecognizedException, SAXParseException,
                                 SAXNotSupportedException, SAXReaderNotAvailable)

default_parser_list = ["xml.sax.expatreader"]


def parse(source, handler, errorHandler=ErrorHandler()):
    parser = make_parser()
    parser.setContentHandler(handler)
    parser.setErrorHandler(errorHandler)
    parser.parse(source)


def parseString(string, handler, errorHandler=ErrorHandler()):
    import io
    if errorHandler is None:
        errorHandler = ErrorHandler()
    parser = make_parser()
    parser.setContentHandler(handler)
    parser.setErrorHandler(errorHandler)
    inpsrc = InputSource()
    if isinstance(string, str):
        inpsrc.setCharacterStream(io.StringIO(string))
    else:
        inpsrc.setByteStream(io.BytesIO(string))
    parser.parse(inpsrc)


def make_parser(parser_list=()):
    # Only the built-in reader exists; a module name is never imported
    # dynamically, so any other entry in parser_list is skipped.
    for name in list(parser_list) + default_parser_list:
        if name == "xml.sax.expatreader":
            from xml.sax import expatreader
            return expatreader.create_parser()
    raise SAXReaderNotAvailable("No parsers found", None)


__all__ = ["ContentHandler", "ErrorHandler", "InputSource", "SAXException", "SAXNotRecognizedException",
           "SAXNotSupportedException", "SAXParseException", "SAXReaderNotAvailable", "default_parser_list",
           "make_parser", "parse", "parseString"]
