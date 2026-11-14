# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/sax/expatreader.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""xml.sax.expatreader: the SAX driver over xml.parsers.expat."""

from xml.sax._exceptions import SAXException, SAXParseException, SAXNotRecognizedException, SAXNotSupportedException
from xml.sax.handler import (feature_namespaces, feature_namespace_prefixes, feature_validation,
                             feature_external_ges, feature_external_pes, feature_string_interning,
                             property_lexical_handler, property_xml_string, property_interning_dict)
from xml.sax import xmlreader, saxutils, handler
from xml.parsers import expat

AttributesImpl = xmlreader.AttributesImpl
AttributesNSImpl = xmlreader.AttributesNSImpl


class ExpatLocator(xmlreader.Locator):
    def __init__(self, parser):
        self._ref = parser

    def getColumnNumber(self):
        p = self._ref
        if p._parser is None:
            return None
        return p._parser.ErrorColumnNumber

    def getLineNumber(self):
        p = self._ref
        if p._parser is None:
            return 1
        return p._parser.ErrorLineNumber

    def getPublicId(self):
        p = self._ref
        if p is None:
            return None
        return p._source.getPublicId()

    def getSystemId(self):
        p = self._ref
        if p is None:
            return None
        return p._source.getSystemId()


class ExpatParser(xmlreader.IncrementalParser, xmlreader.Locator):
    def __init__(self, namespaceHandling=0, bufsize=2 ** 16 - 20):
        xmlreader.IncrementalParser.__init__(self, bufsize)
        self._source = xmlreader.InputSource()
        self._parser = None
        self._namespaces = namespaceHandling
        self._lex_handler_prop = None
        self._parsing = False
        self._entity_stack = []
        self._external_ges = 0
        self._interning = None
        self._namespace_prefixes = 0

    def parse(self, source):
        source = saxutils.prepare_input_source(source)
        self._source = source
        try:
            self.reset()
            self._cont_handler.setDocumentLocator(ExpatLocator(self))
            xmlreader.IncrementalParser.parse(self, source)
        except:
            self._close_source()
            raise

    def prepareParser(self, source):
        if source.getSystemId() is not None:
            self._parser.SetBase(source.getSystemId())

    def setContentHandler(self, handler):
        xmlreader.IncrementalParser.setContentHandler(self, handler)
        if self._parsing:
            self._reset_cont_handler()

    def getFeature(self, name):
        if name == feature_namespaces:
            return self._namespaces
        if name == feature_string_interning:
            return self._interning is not None
        if name in (feature_validation, feature_external_pes):
            return 0
        if name == feature_external_ges:
            return self._external_ges
        if name == feature_namespace_prefixes:
            return self._namespace_prefixes
        raise SAXNotRecognizedException("Feature '%s' not recognized" % name)

    def setFeature(self, name, state):
        if self._parsing:
            raise SAXNotSupportedException("Cannot set features while parsing")
        if name == feature_namespaces:
            self._namespaces = state
        elif name == feature_external_ges:
            self._external_ges = state
        elif name == feature_string_interning:
            if state:
                if self._interning is None:
                    self._interning = {}
            else:
                self._interning = None
        elif name == feature_validation:
            if state:
                raise SAXNotSupportedException("expat does not support validation")
        elif name == feature_external_pes:
            if state:
                raise SAXNotSupportedException("expat does not read external parameter entities")
        elif name == feature_namespace_prefixes:
            self._namespace_prefixes = state
        else:
            raise SAXNotRecognizedException("Feature '%s' not recognized" % name)

    def getProperty(self, name):
        if name == handler.property_lexical_handler:
            return self._lex_handler_prop
        if name == property_interning_dict:
            return self._interning
        if name == property_xml_string:
            if self._parser:
                return self._parser.GetInputContext()
            raise SAXNotRecognizedException("This property is only available during a parse")
        raise SAXNotRecognizedException("Property '%s' not recognized" % name)

    def setProperty(self, name, value):
        if name == handler.property_lexical_handler:
            self._lex_handler_prop = value
            if self._parsing:
                self._reset_lex_handler_prop()
        elif name == property_interning_dict:
            self._interning = value
        elif name == property_xml_string:
            raise SAXNotSupportedException("Property '%s' cannot be set" % name)
        else:
            raise SAXNotRecognizedException("Property '%s' not recognized" % name)

    def feed(self, data, isFinal=False):
        if not self._parsing:
            self.reset()
            self._parsing = True
            self._cont_handler.startDocument()
        try:
            self._parser.Parse(data, isFinal)
        except expat.error as e:
            exc = SAXParseException(expat.ErrorString(e.code), e, self)
            self._err_handler.fatalError(exc)

    def flush(self):
        if self._parser is None:
            return
        was = self._parser.GetReparseDeferralEnabled()
        try:
            self._parser.SetReparseDeferralEnabled(False)
            self._parser.Parse(b"", False)
        except expat.error as e:
            exc = SAXParseException(expat.ErrorString(e.code), e, self)
            self._err_handler.fatalError(exc)
        finally:
            self._parser.SetReparseDeferralEnabled(was)

    def _close_source(self):
        source = self._source
        try:
            file = source.getCharacterStream()
            if file is not None:
                file.close()
        finally:
            file = source.getByteStream()
            if file is not None:
                file.close()

    def close(self):
        if self._entity_stack or self._parser is None or isinstance(self._parser, _ClosedParser):
            return
        try:
            self.feed(b"", isFinal=True)
            self._cont_handler.endDocument()
            self._parsing = False
            self._parser = None
        finally:
            self._parsing = False
            if self._parser is not None:
                parser = _ClosedParser()
                parser.ErrorColumnNumber = self._parser.ErrorColumnNumber
                parser.ErrorLineNumber = self._parser.ErrorLineNumber
                self._parser = parser
            self._close_source()

    def _reset_cont_handler(self):
        self._parser.ProcessingInstructionHandler = self._cont_handler.processingInstruction
        self._parser.CharacterDataHandler = self._cont_handler.characters

    def _reset_lex_handler_prop(self):
        lex = self._lex_handler_prop
        parser = self._parser
        if lex is None:
            parser.CommentHandler = None
            parser.StartCdataSectionHandler = None
            parser.EndCdataSectionHandler = None
            parser.StartDoctypeDeclHandler = None
            parser.EndDoctypeDeclHandler = None
        else:
            parser.CommentHandler = lex.comment
            parser.StartCdataSectionHandler = lex.startCDATA
            parser.EndCdataSectionHandler = lex.endCDATA
            parser.StartDoctypeDeclHandler = self.start_doctype_decl
            parser.EndDoctypeDeclHandler = lex.endDTD

    def reset(self):
        if self._namespaces:
            self._parser = expat.ParserCreate(self._source.getEncoding(), " ", intern=self._interning)
            self._parser.namespace_prefixes = 1
            self._parser.StartElementHandler = self.start_element_ns
            self._parser.EndElementHandler = self.end_element_ns
        else:
            self._parser = expat.ParserCreate(self._source.getEncoding(), intern=self._interning)
            self._parser.StartElementHandler = self.start_element
            self._parser.EndElementHandler = self.end_element
        self._reset_cont_handler()
        self._parser.UnparsedEntityDeclHandler = self.unparsed_entity_decl
        self._parser.NotationDeclHandler = self.notation_decl
        self._parser.StartNamespaceDeclHandler = self.start_namespace_decl
        self._parser.EndNamespaceDeclHandler = self.end_namespace_decl
        self._decl_handler_prop = None
        if self._lex_handler_prop:
            self._reset_lex_handler_prop()
        self._parser.ExternalEntityRefHandler = self.external_entity_ref
        try:
            self._parser.SkippedEntityHandler = self.skipped_entity_handler
        except AttributeError:
            pass
        self._parser.SetParamEntityParsing(expat.XML_PARAM_ENTITY_PARSING_UNLESS_STANDALONE)
        self._parsing = False
        self._entity_stack = []

    def getColumnNumber(self):
        if self._parser is None:
            return None
        return self._parser.ErrorColumnNumber

    def getLineNumber(self):
        if self._parser is None:
            return 1
        return self._parser.ErrorLineNumber

    def getPublicId(self):
        return self._source.getPublicId()

    def getSystemId(self):
        return self._source.getSystemId()

    def start_element(self, name, attrs):
        self._cont_handler.startElement(name, AttributesImpl(attrs))

    def end_element(self, name):
        self._cont_handler.endElement(name)

    def start_element_ns(self, name, attrs):
        pair = name.split()
        if len(pair) == 1:
            pair = (None, name)
        elif len(pair) == 3:
            pair = pair[0], pair[1]
        else:
            pair = tuple(pair)
        newattrs = {}
        qnames = {}
        for (aname, value) in attrs.items():
            parts = aname.split()
            length = len(parts)
            if length == 1:
                qname = aname
                apair = (None, aname)
            elif length == 3:
                qname = "%s:%s" % (parts[2], parts[1])
                apair = parts[0], parts[1]
            else:
                qname = parts[1]
                apair = tuple(parts)
            newattrs[apair] = value
            qnames[apair] = qname
        self._cont_handler.startElementNS(pair, None, AttributesNSImpl(newattrs, qnames))

    def end_element_ns(self, name):
        pair = name.split()
        if len(pair) == 1:
            pair = (None, name)
        elif len(pair) == 3:
            pair = pair[0], pair[1]
        else:
            pair = tuple(pair)
        self._cont_handler.endElementNS(pair, None)

    def processing_instruction(self, target, data):
        self._cont_handler.processingInstruction(target, data)

    def character_data(self, data):
        self._cont_handler.characters(data)

    def start_namespace_decl(self, prefix, uri):
        self._cont_handler.startPrefixMapping(prefix, uri)

    def end_namespace_decl(self, prefix):
        self._cont_handler.endPrefixMapping(prefix)

    def start_doctype_decl(self, name, sysid, pubid, has_internal_subset):
        self._lex_handler_prop.startDTD(name, pubid, sysid)

    def unparsed_entity_decl(self, name, base, sysid, pubid, notation_name):
        self._dtd_handler.unparsedEntityDecl(name, pubid, sysid, notation_name)

    def notation_decl(self, name, base, sysid, pubid):
        self._dtd_handler.notationDecl(name, pubid, sysid)

    def external_entity_ref(self, context, base, sysid, pubid):
        # External general entities are never fetched; the reference is
        # reported as skipped, as expat does with the feature off.
        if not self._external_ges:
            return 1
        source = self._ent_handler.resolveEntity(pubid, sysid)
        source = saxutils.prepare_input_source(source, self._source.getSystemId() or "")
        self._entity_stack.append((self._parser, self._source))
        self._parser = self._parser.ExternalEntityParserCreate(context)
        self._source = source
        try:
            xmlreader.IncrementalParser.parse(self, source)
        except:
            return 0
        (self._parser, self._source) = self._entity_stack[-1]
        del self._entity_stack[-1]
        return 1

    def skipped_entity_handler(self, name, is_pe):
        if is_pe:
            name = "%" + name
        self._cont_handler.skippedEntity(name)


class _ClosedParser:
    pass


def create_parser(*args, **kwargs):
    return ExpatParser(*args, **kwargs)
