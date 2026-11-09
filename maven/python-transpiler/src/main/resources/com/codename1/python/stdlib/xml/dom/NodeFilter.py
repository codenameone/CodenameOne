# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/dom/NodeFilter.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""xml.dom.NodeFilter: the DOM Level 2 NodeFilter constants."""


class NodeFilter:
    FILTER_ACCEPT = 1
    FILTER_REJECT = 2
    FILTER_SKIP = 3

    SHOW_ALL = 0xFFFFFFFF
    SHOW_ELEMENT = 0x00000001
    SHOW_ATTRIBUTE = 0x00000002
    SHOW_TEXT = 0x00000004
    SHOW_CDATA_SECTION = 0x00000008
    SHOW_ENTITY_REFERENCE = 0x00000010
    SHOW_ENTITY = 0x00000020
    SHOW_PROCESSING_INSTRUCTION = 0x00000040
    SHOW_COMMENT = 0x00000080
    SHOW_DOCUMENT = 0x00000100
    SHOW_DOCUMENT_TYPE = 0x00000200
    SHOW_DOCUMENT_FRAGMENT = 0x00000400
    SHOW_NOTATION = 0x00000800

    def acceptNode(self, node):
        raise NotImplementedError
