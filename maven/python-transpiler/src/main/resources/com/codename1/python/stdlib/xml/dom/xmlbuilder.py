# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/dom/xmlbuilder.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""xml.dom.xmlbuilder: the DOM Level 3 load options and the DocumentLS
mixin minidom's Document carries."""

import xml.dom

__all__ = ["DOMBuilder", "DOMEntityResolver", "DOMInputSource"]


class Options:
    namespaces = 1
    namespace_declarations = True
    validation = False
    external_parameter_entities = True
    external_general_entities = True
    external_dtd_subset = True
    validate_if_schema = False
    validate = False
    datatype_normalization = False
    create_entity_ref_nodes = True
    entities = True
    whitespace_in_element_content = True
    cdata_sections = True
    comments = True
    charset_overrides_xml_encoding = True
    infoset = False
    supported_mediatypes_only = False
    errorHandler = None
    filter = None


class DOMInputSource:
    __slots__ = ("byteStream", "characterStream", "stringData", "encoding", "publicId", "systemId", "baseURI")

    def __init__(self):
        self.byteStream = None
        self.characterStream = None
        self.stringData = None
        self.encoding = None
        self.publicId = None
        self.systemId = None
        self.baseURI = None


class DOMEntityResolver:
    __slots__ = ()

    def resolveEntity(self, publicId, systemId):
        source = DOMInputSource()
        source.publicId = publicId
        source.systemId = systemId
        # Entities are resolved from local files only; nothing is fetched.
        source.byteStream = open(systemId, "rb")
        return source


class DOMBuilder:
    entityResolver = None
    errorHandler = None
    filter = None

    ACTION_REPLACE = 1
    ACTION_APPEND_AS_CHILDREN = 2
    ACTION_INSERT_AFTER = 3
    ACTION_INSERT_BEFORE = 4

    def __init__(self):
        self._options = Options()

    def parse(self, input):
        options = self._options
        options.filter = self.filter
        options.errorHandler = self.errorHandler
        fp = input.byteStream
        if fp is None and input.systemId:
            fp = open(input.systemId, "rb")
        from xml.dom import expatbuilder
        builder = expatbuilder.makeBuilder(options)
        return builder.parseFile(fp)

    def parseURI(self, uri):
        source = DOMInputSource()
        source.systemId = uri
        return self.parse(source)


class DocumentLS:
    async_ = False

    def _get_async(self):
        return False

    def _set_async(self, flag):
        if flag:
            raise xml.dom.NotSupportedErr("asynchronous document loading is not supported")

    def abort(self):
        raise xml.dom.InvalidStateErr("haven't decided what this should do")

    def load(self, uri):
        raise NotImplementedError("haven't written this yet")

    def loadXML(self, source):
        raise NotImplementedError("haven't written this yet")

    def saveXML(self, snode):
        if snode is None:
            snode = self
        elif snode.ownerDocument is not self:
            raise xml.dom.WrongDocumentErr()
        return snode.toxml()


class DOMImplementationLS:
    MODE_SYNCHRONOUS = 1
    MODE_ASYNCHRONOUS = 2

    def createDOMBuilder(self, mode, schemaType):
        if schemaType is not None:
            raise xml.dom.NotSupportedErr("schemaType not yet supported")
        if mode == self.MODE_SYNCHRONOUS:
            return DOMBuilder()
        if mode == self.MODE_ASYNCHRONOUS:
            raise xml.dom.NotSupportedErr("asynchronous builders are not supported")
        raise ValueError("unknown value for mode")

    def createDOMWriter(self):
        raise NotImplementedError("the writer interface hasn't been written yet!")

    def createDOMInputSource(self):
        return DOMInputSource()
