# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""pypdfium2: present so that importing it succeeds, with no renderer.

The real package is ctypes bindings to Google's PDFium, a native library a
translated program cannot load. Its users here (pdfplumber's `to_image`)
render a page to a bitmap; text, tables and layout come from pdfminer and
need none of it. Opening a document therefore raises PdfiumError, which
those callers already handle as "this PDF cannot be rendered".
"""

V_PYPDFIUM2 = "5.14.0"
V_LIBPDFIUM = "0"


class PdfiumError(RuntimeError):
    pass


class PdfDocument:
    def __init__(self, input, password=None, autoclose=False):
        raise PdfiumError("PDF page rendering is not available on this platform")


class PdfPage:
    def __init__(self, *args, **kwargs):
        raise PdfiumError("PDF page rendering is not available on this platform")


class PdfBitmap:
    def __init__(self, *args, **kwargs):
        raise PdfiumError("PDF page rendering is not available on this platform")
