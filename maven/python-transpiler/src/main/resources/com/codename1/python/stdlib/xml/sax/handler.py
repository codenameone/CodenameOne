# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/sax/handler.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""xml.sax.handler: the handler base classes and the feature and property names."""


class ErrorHandler:
    def error(self, exception):
        raise exception

    def fatalError(self, exception):
        raise exception

    def warning(self, exception):
        print(exception)


class ContentHandler:
    def __init__(self):
        self._locator = None

    def setDocumentLocator(self, locator):
        self._locator = locator

    def startDocument(self):
        pass

    def endDocument(self):
        pass

    def startPrefixMapping(self, prefix, uri):
        pass

    def endPrefixMapping(self, prefix):
        pass

    def startElement(self, name, attrs):
        pass

    def endElement(self, name):
        pass

    def startElementNS(self, name, qname, attrs):
        pass

    def endElementNS(self, name, qname):
        pass

    def characters(self, content):
        pass

    def ignorableWhitespace(self, whitespace):
        pass

    def processingInstruction(self, target, data):
        pass

    def skippedEntity(self, name):
        pass


class DTDHandler:
    def notationDecl(self, name, publicId, systemId):
        pass

    def unparsedEntityDecl(self, name, publicId, systemId, ndata):
        pass


class EntityResolver:
    def resolveEntity(self, publicId, systemId):
        return systemId


class LexicalHandler:
    def comment(self, content):
        pass

    def startDTD(self, name, public_id, system_id):
        pass

    def endDTD(self):
        pass

    def startCDATA(self):
        pass

    def endCDATA(self):
        pass


feature_namespaces = "http://xml.org/sax/features/namespaces"
feature_namespace_prefixes = "http://xml.org/sax/features/namespace-prefixes"
feature_string_interning = "http://xml.org/sax/features/string-interning"
feature_validation = "http://xml.org/sax/features/validation"
feature_external_ges = "http://xml.org/sax/features/external-general-entities"
feature_external_pes = "http://xml.org/sax/features/external-parameter-entities"
all_features = [feature_namespaces, feature_namespace_prefixes, feature_string_interning, feature_validation,
                feature_external_ges, feature_external_pes]

property_lexical_handler = "http://xml.org/sax/properties/lexical-handler"
property_declaration_handler = "http://xml.org/sax/properties/declaration-handler"
property_dom_node = "http://xml.org/sax/properties/dom-node"
property_xml_string = "http://xml.org/sax/properties/xml-string"
property_encoding = "http://www.python.org/sax/properties/encoding"
property_interning_dict = "http://www.python.org/sax/properties/interning-dict"
all_properties = [property_lexical_handler, property_dom_node, property_declaration_handler, property_xml_string,
                  property_encoding, property_interning_dict]
