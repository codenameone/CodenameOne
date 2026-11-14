# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/sax/xmlreader.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""xml.sax.xmlreader: XMLReader, IncrementalParser, InputSource and the
attribute collections."""

from xml.sax import handler
from xml.sax._exceptions import SAXNotRecognizedException, SAXNotSupportedException


class XMLReader:
    def __init__(self):
        self._cont_handler = handler.ContentHandler()
        self._dtd_handler = handler.DTDHandler()
        self._ent_handler = handler.EntityResolver()
        self._err_handler = handler.ErrorHandler()

    def parse(self, source):
        raise NotImplementedError("This method must be implemented!")

    def getContentHandler(self):
        return self._cont_handler

    def setContentHandler(self, handler):
        self._cont_handler = handler

    def getDTDHandler(self):
        return self._dtd_handler

    def setDTDHandler(self, handler):
        self._dtd_handler = handler

    def getEntityResolver(self):
        return self._ent_handler

    def setEntityResolver(self, resolver):
        self._ent_handler = resolver

    def getErrorHandler(self):
        return self._err_handler

    def setErrorHandler(self, handler):
        self._err_handler = handler

    def setLocale(self, locale):
        raise SAXNotSupportedException("Locale support not implemented")

    def getFeature(self, name):
        raise SAXNotRecognizedException("Feature '%s' not recognized" % name)

    def setFeature(self, name, state):
        raise SAXNotRecognizedException("Feature '%s' not recognized" % name)

    def getProperty(self, name):
        raise SAXNotRecognizedException("Property '%s' not recognized" % name)

    def setProperty(self, name, value):
        raise SAXNotRecognizedException("Property '%s' not recognized" % name)


class IncrementalParser(XMLReader):
    def __init__(self, bufsize=2 ** 16):
        self._bufsize = bufsize
        XMLReader.__init__(self)

    def parse(self, source):
        from xml.sax import saxutils
        source = saxutils.prepare_input_source(source)
        self.prepareParser(source)
        file = source.getCharacterStream()
        if file is None:
            file = source.getByteStream()
        buffer = file.read(self._bufsize)
        while buffer:
            self.feed(buffer)
            buffer = file.read(self._bufsize)
        self.close()

    def feed(self, data):
        raise NotImplementedError("This method must be implemented!")

    def prepareParser(self, source):
        raise NotImplementedError("prepareParser must be overridden!")

    def close(self):
        raise NotImplementedError("This method must be implemented!")

    def reset(self):
        raise NotImplementedError("This method must be implemented!")


class Locator:
    def getColumnNumber(self):
        return -1

    def getLineNumber(self):
        return -1

    def getPublicId(self):
        return None

    def getSystemId(self):
        return None


class InputSource:
    def __init__(self, system_id=None):
        self.__system_id = system_id
        self.__public_id = None
        self.__encoding = None
        self.__bytefile = None
        self.__charfile = None

    def setPublicId(self, public_id):
        self.__public_id = public_id

    def getPublicId(self):
        return self.__public_id

    def setSystemId(self, system_id):
        self.__system_id = system_id

    def getSystemId(self):
        return self.__system_id

    def setEncoding(self, encoding):
        self.__encoding = encoding

    def getEncoding(self):
        return self.__encoding

    def setByteStream(self, bytefile):
        self.__bytefile = bytefile

    def getByteStream(self):
        return self.__bytefile

    def setCharacterStream(self, charfile):
        self.__charfile = charfile

    def getCharacterStream(self):
        return self.__charfile


class AttributesImpl:
    def __init__(self, attrs):
        self._attrs = attrs

    def getLength(self):
        return len(self._attrs)

    def getType(self, name):
        return "CDATA"

    def getValue(self, name):
        return self._attrs[name]

    def getValueByQName(self, name):
        return self._attrs[name]

    def getNameByQName(self, name):
        if name not in self._attrs:
            raise KeyError(name)
        return name

    def getQNameByName(self, name):
        if name not in self._attrs:
            raise KeyError(name)
        return name

    def getNames(self):
        return list(self._attrs.keys())

    def getQNames(self):
        return list(self._attrs.keys())

    def __len__(self):
        return len(self._attrs)

    def __getitem__(self, name):
        return self._attrs[name]

    def keys(self):
        return list(self._attrs.keys())

    def __contains__(self, name):
        return name in self._attrs

    def get(self, name, alternative=None):
        return self._attrs.get(name, alternative)

    def copy(self):
        return self.__class__(self._attrs)

    def items(self):
        return list(self._attrs.items())

    def values(self):
        return list(self._attrs.values())


class AttributesNSImpl(AttributesImpl):
    def __init__(self, attrs, qnames):
        self._attrs = attrs
        self._qnames = qnames

    def getValueByQName(self, name):
        for nsname, qname in self._qnames.items():
            if qname == name:
                return self._attrs[nsname]
        raise KeyError(name)

    def getNameByQName(self, name):
        for nsname, qname in self._qnames.items():
            if qname == name:
                return nsname
        raise KeyError(name)

    def getQNameByName(self, name):
        return self._qnames[name]

    def getQNames(self):
        return list(self._qnames.values())

    def copy(self):
        return self.__class__(self._attrs, self._qnames)
