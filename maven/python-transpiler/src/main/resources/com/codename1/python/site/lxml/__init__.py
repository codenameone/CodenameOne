# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""lxml: a pure-Python subset of the lxml package, providing lxml.etree.

Only lxml.etree is provided (see that module for what it covers). The C
extensions of the real package -- objectify, html, isoschematron, XSLT -- are
not.
"""


def get_include():
    return []
