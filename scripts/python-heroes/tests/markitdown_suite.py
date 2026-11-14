# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""MarkItDown's own test suite, run transpiled: every module except the
command line's, the Azure services' (Document Intelligence, Content
Understanding) and the converters an application does not ship (images,
Outlook, YouTube transcripts). run_heroes.py runs the same modules
under CPython's pytest and compares."""
import pytest

from tests import (test_csv, test_docx, test_epub, test_html, test_module_misc, test_module_vectors, test_pdf,
                   test_pptx, test_rss, test_xlsx, test_zip)

MODULES = [test_module_vectors, test_module_misc, test_docx, test_pptx, test_xlsx, test_pdf, test_epub, test_csv,
           test_html, test_rss, test_zip]

for m in MODULES:
    print("## " + m.__name__)
    pytest.run_module(m)
