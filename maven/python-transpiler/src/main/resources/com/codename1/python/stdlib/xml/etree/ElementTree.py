# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/xml/etree/ElementTree.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
#
#---------------------------------------------------------------------
# Licensed to PSF under a Contributor Agreement.
# See https://www.python.org/psf/license for licensing details.
#
# ElementTree
# Copyright (c) 1999-2008 by Fredrik Lundh.  All rights reserved.
#
# fredrik@pythonware.com
# http://www.pythonware.com
# --------------------------------------------------------------------
# The ElementTree toolkit is
#
# Copyright (c) 1999-2008 by Fredrik Lundh
#
# By obtaining, using, and/or copying this software and/or its
# associated documentation, you agree that you have read, understood,
# and will comply with the following terms and conditions:
#
# Permission to use, copy, modify, and distribute this software and
# its associated documentation for any purpose and without fee is
# hereby granted, provided that the above copyright notice appears in
# all copies, and that both that copyright notice and this permission
# notice appear in supporting documentation, and that the name of
# Secret Labs AB or the author not be used in advertising or publicity
# pertaining to distribution of the software without specific, written
# prior permission.
#
# SECRET LABS AB AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD
# TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANT-
# ABILITY AND FITNESS.  IN NO EVENT SHALL SECRET LABS AB OR THE AUTHOR
# BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY

"""xml.etree.ElementTree: the element tree, its parser and serializer.

The tree, TreeBuilder, XMLParser (over xml.parsers.expat), XMLPullParser,
iterparse and the "xml", "html" and "text" serializers, with CPython's
output byte for byte: namespace prefixes, attribute order and escaping.
canonicalize() and C14NWriterTarget (C14N 2.0) are not provided.
"""

import collections as _collections
import io as _io
import re as _re
import sys as _sys
import warnings as _warnings

from xml.etree import ElementPath
from xml.parsers import expat as _expat

import _cn1xml

_NOT_UTF8_STARTS = (b"\xff\xfe", b"\xfe\xff", b"<\x00", b"\x00<")

__all__ = [
    "Comment", "dump", "Element", "ElementTree", "fromstring", "fromstringlist", "indent", "iselement",
    "iterparse", "parse", "ParseError", "PI", "ProcessingInstruction", "QName", "SubElement", "tostring",
    "tostringlist", "TreeBuilder", "VERSION", "XML", "XMLID", "XMLParser", "XMLPullParser",
    "register_namespace",
]

VERSION = "1.3.0"


class ParseError(SyntaxError):
    pass


def iselement(element):
    return hasattr(element, "tag")


class Element:
    tag = None
    attrib = None
    text = None
    tail = None

    def __init__(self, tag, attrib={}, **extra):
        if not isinstance(attrib, dict):
            raise TypeError("Element() argument 2 must be dict, not %s" % (attrib.__class__.__name__,))
        self.tag = tag
        self.attrib = dict(attrib)
        if extra:
            self.attrib.update(extra)
        self._children = []

    def __repr__(self):
        return "<%s %r at %#x>" % (self.__class__.__name__, self.tag, id(self))

    def makeelement(self, tag, attrib):
        return self.__class__(tag, attrib)

    def __copy__(self):
        elem = self.makeelement(self.tag, self.attrib)
        elem.text = self.text
        elem.tail = self.tail
        elem[:] = self
        return elem

    def copy(self):
        _warnings.warn("elem.copy() is deprecated. Use copy.copy(elem) instead.", DeprecationWarning, stacklevel=2)
        return self.__copy__()

    def __deepcopy__(self, memo):
        import copy as _copy
        elem = self.makeelement(self.tag, _copy.deepcopy(self.attrib, memo))
        elem.text = self.text
        elem.tail = self.tail
        for child in self._children:
            elem._children.append(_copy.deepcopy(child, memo))
        return elem

    def __len__(self):
        return len(self._children)

    def __bool__(self):
        _warnings.warn("Testing an element's truth value will always return True in future versions.  "
                       "Use specific 'len(elem)' or 'elem is not None' test instead.",
                       DeprecationWarning, stacklevel=2)
        return len(self._children) != 0

    def __getitem__(self, index):
        return self._children[index]

    def __setitem__(self, index, element):
        if isinstance(index, slice):
            for elt in element:
                self._assert_is_element(elt)
        else:
            self._assert_is_element(element)
        self._children[index] = element

    def __delitem__(self, index):
        del self._children[index]

    def __iter__(self):
        return iter(self._children)

    def append(self, subelement):
        if not isinstance(subelement, Element):
            raise TypeError("append() argument 1 must be xml.etree.ElementTree.Element, not %s"
                            % type(subelement).__name__)
        self._children.append(subelement)

    def extend(self, elements):
        for element in elements:
            self._assert_is_element(element)
            self._children.append(element)

    def insert(self, index, subelement):
        if not isinstance(subelement, Element):
            raise TypeError("insert() argument 2 must be xml.etree.ElementTree.Element, not %s"
                            % type(subelement).__name__)
        self._children.insert(index, subelement)

    def _assert_is_element(self, e):
        if not isinstance(e, Element):
            raise TypeError('expected an Element, not "%s"' % type(e).__name__)

    def remove(self, subelement):
        for i, c in enumerate(self._children):
            if c is subelement:
                del self._children[i]
                return
        raise ValueError("Element.remove(x): element not found")

    # A path that is one tag, as most are, matches children directly, in
    # Java, as the C accelerator does in C; _cn1xml answers NotImplemented
    # for anything else, which goes to ElementPath.
    def find(self, path, namespaces=None):
        if namespaces is None:
            r = _cn1xml.find(self, path)
            if r is not NotImplemented:
                return r
        return ElementPath.find(self, path, namespaces)

    def findtext(self, path, default=None, namespaces=None):
        if namespaces is None:
            r = _cn1xml.find(self, path)
            if r is not NotImplemented:
                return default if r is None else r.text or ""
        return ElementPath.findtext(self, path, default, namespaces)

    def findall(self, path, namespaces=None):
        if namespaces is None:
            r = _cn1xml.findall(self, path)
            if r is not NotImplemented:
                return r
        return ElementPath.findall(self, path, namespaces)

    def iterfind(self, path, namespaces=None):
        return ElementPath.iterfind(self, path, namespaces)

    def clear(self):
        self.attrib.clear()
        self._children = []
        self.text = self.tail = None

    def get(self, key, default=None):
        return self.attrib.get(key, default)

    def set(self, key, value):
        self.attrib[key] = value

    def keys(self):
        return list(self.attrib.keys())

    def items(self):
        return list(self.attrib.items())

    # A walk over a stack of (children, next index) in Java, as CPython's C
    # accelerator walks in C (_cn1xml.ElementIter).
    def iter(self, tag=None):
        if tag == "*":
            tag = None
        return _cn1xml.iter(self, tag)

    def itertext(self):
        tag = self.tag
        if not isinstance(tag, str) and tag is not None:
            return
        t = self.text
        if t:
            yield t
        for e in self:
            yield from e.itertext()
            t = e.tail
            if t:
                yield t


# Element's own __init__: the fast build below makes the elements of a class
# that still has it as that method would, without calling it.
_ELEMENT_INIT = Element.__init__


def SubElement(parent, tag, attrib={}, **extra):
    if not isinstance(parent, Element):
        raise TypeError("SubElement() argument 1 must be xml.etree.ElementTree.Element, not %s"
                        % type(parent).__name__)
    attrib = {**attrib, **extra}
    element = parent.makeelement(tag, attrib)
    parent.append(element)
    return element


def Comment(text=None):
    element = Element(Comment)
    element.text = text
    return element


def ProcessingInstruction(target, text=None):
    element = Element(ProcessingInstruction)
    element.text = target
    if text:
        element.text = element.text + " " + text
    return element


PI = ProcessingInstruction


class QName:
    def __init__(self, text_or_uri, tag=None):
        if tag:
            text_or_uri = "{%s}%s" % (text_or_uri, tag)
        self.text = text_or_uri

    def __str__(self):
        return self.text

    def __repr__(self):
        return "<%s %r>" % (self.__class__.__name__, self.text)

    def __hash__(self):
        return hash(self.text)

    def _key(self, other):
        return other.text if isinstance(other, QName) else other

    def __le__(self, other):
        return self.text <= self._key(other)

    def __lt__(self, other):
        return self.text < self._key(other)

    def __ge__(self, other):
        return self.text >= self._key(other)

    def __gt__(self, other):
        return self.text > self._key(other)

    def __eq__(self, other):
        return self.text == self._key(other)


class ElementTree:
    def __init__(self, element=None, file=None):
        if element is not None and not iselement(element):
            raise TypeError("expected an Element, not %s" % type(element).__name__)
        self._root = element
        if file:
            self.parse(file)

    def getroot(self):
        return self._root

    def _setroot(self, element):
        if not iselement(element):
            raise TypeError("expected an Element, not %s" % type(element).__name__)
        self._root = element

    def parse(self, source, parser=None):
        close_source = False
        if not hasattr(source, "read"):
            source = open(source, "rb")
            close_source = True
        try:
            if parser is None:
                parser = XMLParser()
            while True:
                data = source.read(65536)
                if not data:
                    break
                parser.feed(data)
            self._root = parser.close()
            return self._root
        finally:
            if close_source:
                source.close()

    def iter(self, tag=None):
        return self._root.iter(tag)

    def _rel(self, path):
        if path[:1] == "/":
            path = "." + path
            _warnings.warn("This search is broken in 1.3 and earlier, and will be fixed in a future version.  "
                           "If you rely on the current behaviour, change it to %r" % path,
                           FutureWarning, stacklevel=3)
        return path

    def find(self, path, namespaces=None):
        return self._root.find(self._rel(path), namespaces)

    def findtext(self, path, default=None, namespaces=None):
        return self._root.findtext(self._rel(path), default, namespaces)

    def findall(self, path, namespaces=None):
        return self._root.findall(self._rel(path), namespaces)

    def iterfind(self, path, namespaces=None):
        return self._root.iterfind(self._rel(path), namespaces)

    def write(self, file_or_filename, encoding=None, xml_declaration=None, default_namespace=None, method=None, *,
              short_empty_elements=True):
        if not method:
            method = "xml"
        elif method not in _SERIALIZERS:
            raise ValueError("unknown method %r" % method)
        if not encoding:
            encoding = "utf-8" if method == "c14n" else "us-ascii"
        parts = []
        write = parts.append
        enc_lower = encoding.lower()
        if method == "xml" and (xml_declaration or (xml_declaration is None and encoding.lower() != "unicode"
                                                    and enc_lower not in ("utf-8", "us-ascii"))):
            declared = encoding
            if enc_lower == "unicode":
                declared = "UTF-8"
            write("<?xml version='1.0' encoding='%s'?>\n" % (declared,))
        if method == "text":
            _serialize_text(write, self._root)
        else:
            qnames, namespaces = _namespaces(self._root, default_namespace)
            _SERIALIZERS[method](write, self._root, qnames, namespaces, short_empty_elements=short_empty_elements)
        text = "".join(parts)
        if enc_lower == "unicode":
            data = text
        else:
            data = _encode(text, encoding)
        if hasattr(file_or_filename, "write"):
            file_or_filename.write(data)
        else:
            mode = "w" if enc_lower == "unicode" else "wb"
            with open(file_or_filename, mode) as f:
                f.write(data)

    def write_c14n(self, file):
        raise NotImplementedError("C14N serialization is not supported")


def _charrefs(text, limit):
    out = []
    start = 0
    for i, ch in enumerate(text):
        if ord(ch) > limit:
            out.append(text[start:i])
            out.append("&#%d;" % ord(ch))
            start = i + 1
    if not out:
        return text
    out.append(text[start:])
    return "".join(out)


def _encode(text, encoding):
    e = encoding.lower().replace("_", "-")
    if e in ("us-ascii", "ascii"):
        return _charrefs(text, 0x7F).encode("ascii")
    if e in ("iso-8859-1", "latin-1", "latin1", "iso8859-1"):
        return _charrefs(text, 0xFF).encode("latin-1")
    return text.encode(encoding)


_namespace_map = {
    "http://www.w3.org/XML/1998/namespace": "xml",
    "http://www.w3.org/1999/xhtml": "html",
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#": "rdf",
    "http://schemas.xmlsoap.org/wsdl/": "wsdl",
    "http://www.w3.org/2001/XMLSchema": "xs",
    "http://www.w3.org/2001/XMLSchema-instance": "xsi",
    "http://purl.org/dc/elements/1.1/": "dc",
}


def register_namespace(prefix, uri):
    if _re.match(r"ns\d+$", prefix):
        raise ValueError("Prefix format reserved for internal use")
    for k, v in list(_namespace_map.items()):
        if k == uri or v == prefix:
            del _namespace_map[k]
    _namespace_map[uri] = prefix


def _raise_serialization_error(text):
    raise TypeError("cannot serialize %r (type %s)" % (text, type(text).__name__))


def _namespaces(elem, default_namespace=None):
    qnames = {None: None}
    namespaces = {}
    if default_namespace:
        namespaces[default_namespace] = ""

    def add_qname(qname):
        try:
            if qname[:1] == "{":
                uri, tag = qname[1:].rsplit("}", 1)
                prefix = namespaces.get(uri)
                if prefix is None:
                    prefix = _namespace_map.get(uri)
                    if prefix is None:
                        prefix = "ns%d" % len(namespaces)
                    if prefix != "xml":
                        namespaces[uri] = prefix
                if prefix:
                    qnames[qname] = "%s:%s" % (prefix, tag)
                else:
                    qnames[qname] = tag
            else:
                if default_namespace:
                    raise ValueError("cannot use non-qualified names with default_namespace option")
                qnames[qname] = qname
        except TypeError:
            _raise_serialization_error(qname)

    for e in elem.iter():
        tag = e.tag
        if isinstance(tag, QName):
            if tag.text not in qnames:
                add_qname(tag.text)
        elif isinstance(tag, str):
            if tag not in qnames:
                add_qname(tag)
        elif tag is not None and tag is not Comment and tag is not PI:
            _raise_serialization_error(tag)
        for key, value in e.items():
            if isinstance(key, QName):
                key = key.text
            if key not in qnames:
                add_qname(key)
            if isinstance(value, QName) and value.text not in qnames:
                add_qname(value.text)
        text = e.text
        if isinstance(text, QName) and text.text not in qnames:
            add_qname(text.text)
    return qnames, namespaces


def _serialize_xml(write, elem, qnames, namespaces, short_empty_elements, **kwargs):
    tag = elem.tag
    text = elem.text
    if tag is Comment:
        write("<!--%s-->" % text)
    elif tag is ProcessingInstruction:
        write("<?%s?>" % text)
    else:
        tag = qnames[tag.text if isinstance(tag, QName) else tag]
        if tag is None:
            if text:
                write(_escape_cdata(text))
            for e in elem:
                _serialize_xml(write, e, qnames, None, short_empty_elements=short_empty_elements)
        else:
            write("<" + tag)
            items = list(elem.items())
            if items or namespaces:
                if namespaces:
                    for v, k in sorted(namespaces.items(), key=lambda x: x[1]):
                        if k:
                            k = ":" + k
                        write(' xmlns%s="%s"' % (k, _escape_attrib(v)))
                for k, v in items:
                    if isinstance(k, QName):
                        k = k.text
                    if isinstance(v, QName):
                        v = qnames[v.text]
                    else:
                        v = _escape_attrib(v)
                    write(' %s="%s"' % (qnames[k], v))
            if text or len(elem) or not short_empty_elements:
                write(">")
                if text:
                    write(_escape_cdata(text))
                for e in elem:
                    _serialize_xml(write, e, qnames, None, short_empty_elements=short_empty_elements)
                write("</" + tag + ">")
            else:
                write(" />")
    if elem.tail:
        write(_escape_cdata(elem.tail))


HTML_EMPTY = {"area", "base", "basefont", "br", "col", "embed", "frame", "hr", "img", "input", "isindex", "link",
              "meta", "param", "source", "track", "wbr"}


def _serialize_html(write, elem, qnames, namespaces, **kwargs):
    tag = elem.tag
    text = elem.text
    if tag is Comment:
        write("<!--%s-->" % _escape_cdata(text))
    elif tag is ProcessingInstruction:
        write("<?%s?>" % _escape_cdata(text))
    else:
        tag = qnames[tag.text if isinstance(tag, QName) else tag]
        if tag is None:
            if text:
                write(_escape_cdata(text))
            for e in elem:
                _serialize_html(write, e, qnames, None)
        else:
            write("<" + tag)
            items = list(elem.items())
            if items or namespaces:
                if namespaces:
                    for v, k in sorted(namespaces.items(), key=lambda x: x[1]):
                        if k:
                            k = ":" + k
                        write(' xmlns%s="%s"' % (k, _escape_attrib_html(v)))
                for k, v in items:
                    if isinstance(k, QName):
                        k = k.text
                    if isinstance(v, QName):
                        v = qnames[v.text]
                    else:
                        v = _escape_attrib_html(v)
                    write(' %s="%s"' % (qnames[k], v))
            write(">")
            ltag = tag.lower()
            if text:
                if ltag == "script" or ltag == "style":
                    write(text)
                else:
                    write(_escape_cdata(text))
            for e in elem:
                _serialize_html(write, e, qnames, None)
            if ltag not in HTML_EMPTY:
                write("</" + tag + ">")
    if elem.tail:
        write(_escape_cdata(elem.tail))


def _serialize_text(write, elem):
    for part in elem.itertext():
        write(part)
    if elem.tail:
        write(elem.tail)


_SERIALIZERS = {"xml": _serialize_xml, "html": _serialize_html, "text": _serialize_text}


def _escape_cdata(text):
    try:
        if "&" in text:
            text = text.replace("&", "&amp;")
        if "<" in text:
            text = text.replace("<", "&lt;")
        if ">" in text:
            text = text.replace(">", "&gt;")
        return text
    except (TypeError, AttributeError):
        _raise_serialization_error(text)


def _escape_attrib(text):
    try:
        if "&" in text:
            text = text.replace("&", "&amp;")
        if "<" in text:
            text = text.replace("<", "&lt;")
        if ">" in text:
            text = text.replace(">", "&gt;")
        if '"' in text:
            text = text.replace('"', "&quot;")
        if "\r" in text:
            text = text.replace("\r", "&#13;")
        if "\n" in text:
            text = text.replace("\n", "&#10;")
        if "\t" in text:
            text = text.replace("\t", "&#09;")
        return text
    except (TypeError, AttributeError):
        _raise_serialization_error(text)


def _escape_attrib_html(text):
    try:
        if "&" in text:
            text = text.replace("&", "&amp;")
        if ">" in text:
            text = text.replace(">", "&gt;")
        if '"' in text:
            text = text.replace('"', "&quot;")
        return text
    except (TypeError, AttributeError):
        _raise_serialization_error(text)


def tostring(element, encoding=None, method=None, *, xml_declaration=None, default_namespace=None,
             short_empty_elements=True):
    stream = _io.StringIO() if encoding == "unicode" else _io.BytesIO()
    ElementTree(element).write(stream, encoding, xml_declaration=xml_declaration,
                               default_namespace=default_namespace, method=method,
                               short_empty_elements=short_empty_elements)
    return stream.getvalue()


def tostringlist(element, encoding=None, method=None, *, xml_declaration=None, default_namespace=None,
                 short_empty_elements=True):
    return [tostring(element, encoding, method, xml_declaration=xml_declaration,
                     default_namespace=default_namespace, short_empty_elements=short_empty_elements)]


def dump(elem):
    if not isinstance(elem, ElementTree):
        elem = ElementTree(elem)
    elem.write(_sys.stdout, encoding="unicode")
    tail = elem.getroot().tail
    if not tail or tail[-1] != "\n":
        _sys.stdout.write("\n")


def indent(tree, space="  ", level=0):
    if isinstance(tree, ElementTree):
        tree = tree.getroot()
    if level < 0:
        raise ValueError(f"Initial indentation level must be >= 0, got {level}")
    if not len(tree):
        return
    indentations = ["\n" + level * space]

    def _indent_children(elem, level):
        child_level = level + 1
        if child_level < len(indentations):
            child_indentation = indentations[child_level]
        else:
            child_indentation = indentations[level] + space
            indentations.append(child_indentation)
        if not elem.text or not elem.text.strip():
            elem.text = child_indentation
        child = None
        for child in elem:
            if len(child):
                _indent_children(child, child_level)
            if not child.tail or not child.tail.strip():
                child.tail = child_indentation
        if not child.tail.strip():
            child.tail = indentations[level]

    _indent_children(tree, 0)


def parse(source, parser=None):
    tree = ElementTree()
    tree.parse(source, parser)
    return tree


class _IterParseIterator:
    def __init__(self, source, pullparser, close_source):
        self._source = source
        self._pull = pullparser
        self._close_source = close_source
        self._done = False
        self.root = None

    def __iter__(self):
        return self

    def __next__(self):
        while True:
            q = self._pull._events_queue
            if q:
                ev = q.popleft()
                if isinstance(ev, Exception):
                    self.close()
                    raise ev
                return ev
            if self._done:
                raise StopIteration
            try:
                data = self._source.read(16 * 1024)
                if data:
                    self._pull.feed(data)
                else:
                    self.root = self._pull._close_and_return_root()
                    self._done = True
                    self.close()
            except BaseException:
                self.close()
                raise

    def close(self):
        if self._close_source:
            self._close_source = False
            self._source.close()


def iterparse(source, events=None, parser=None):
    pullparser = XMLPullParser(events=events, _parser=parser)
    close_source = False
    if not hasattr(source, "read"):
        source = open(source, "rb")
        close_source = True
    return _IterParseIterator(source, pullparser, close_source)


class XMLPullParser:
    def __init__(self, events=None, *, _parser=None):
        self._events_queue = _collections.deque()
        self._parser = _parser or XMLParser(target=TreeBuilder())
        if events is None:
            events = ("end",)
        self._parser._setevents(self._events_queue, events)

    def feed(self, data):
        if self._parser is None:
            raise ValueError("feed() called after end of stream")
        if data:
            try:
                self._parser.feed(data)
            except SyntaxError as exc:
                self._events_queue.append(exc)

    def _close_and_return_root(self):
        root = self._parser.close()
        self._parser = None
        return root

    def close(self):
        self._close_and_return_root()

    def read_events(self):
        events = self._events_queue
        while events:
            event = events.popleft()
            if isinstance(event, Exception):
                raise event
            yield event

    def flush(self):
        if self._parser is None:
            raise ValueError("flush() called after end of stream")
        self._parser.flush()


def XML(text, parser=None):
    if not parser:
        # The default parser and builder in one Java pass, as CPython's C
        # accelerator does; None for any document it does not take, which
        # the parser below then handles (and reports) as before.
        src = None
        if isinstance(text, str):
            src = text
        elif isinstance(text, (bytes, bytearray)) and not text.startswith(_NOT_UTF8_STARTS):
            try:
                src = bytes(text).decode("utf-8")
            except UnicodeDecodeError:
                src = None
        if src is not None:
            root = _cn1xml.build(src, Element, not isinstance(text, str), _ELEMENT_INIT)
            if root is not None:
                return root
        parser = XMLParser(target=TreeBuilder())
    parser.feed(text)
    return parser.close()


fromstring = XML


def XMLID(text, parser=None):
    if not parser:
        parser = XMLParser(target=TreeBuilder())
    parser.feed(text)
    tree = parser.close()
    ids = {}
    for elem in tree.iter():
        i = elem.get("id")
        if i:
            ids[i] = elem
    return tree, ids


def fromstringlist(sequence, parser=None):
    if not parser:
        parser = XMLParser(target=TreeBuilder())
    for text in sequence:
        parser.feed(text)
    return parser.close()


class TreeBuilder:
    def __init__(self, element_factory=None, *, comment_factory=None, pi_factory=None, insert_comments=False,
                 insert_pis=False):
        self._data = []
        self._elem = []
        self._last = None
        self._root = None
        self._tail = None
        if comment_factory is None:
            comment_factory = Comment
        self._comment_factory = comment_factory
        self.insert_comments = insert_comments
        if pi_factory is None:
            pi_factory = ProcessingInstruction
        self._pi_factory = pi_factory
        self.insert_pis = insert_pis
        if element_factory is None:
            element_factory = Element
        self._factory = element_factory

    def close(self):
        assert len(self._elem) == 0, "missing end tags"
        assert self._root is not None, "missing toplevel element"
        return self._root

    def _flush(self):
        if self._data:
            if self._last is not None:
                text = "".join(self._data)
                if self._tail:
                    assert self._last.tail is None, "internal error (tail)"
                    self._last.tail = text
                else:
                    assert self._last.text is None, "internal error (text)"
                    self._last.text = text
            self._data = []

    def data(self, data):
        self._data.append(data)

    def start(self, tag, attrs):
        self._flush()
        self._last = elem = self._factory(tag, attrs)
        if self._elem:
            self._elem[-1].append(elem)
        elif self._root is None:
            self._root = elem
        self._elem.append(elem)
        self._tail = 0
        return elem

    def end(self, tag):
        self._flush()
        self._last = self._elem.pop()
        assert self._last.tag == tag, "end tag mismatch (expected %s, got %s)" % (self._last.tag, tag)
        self._tail = 1
        return self._last

    def comment(self, text):
        return self._handle_single(self._comment_factory, self.insert_comments, text)

    def pi(self, target, text=None):
        return self._handle_single(self._pi_factory, self.insert_pis, target, text)

    def _handle_single(self, factory, insert, *args):
        elem = factory(*args)
        if insert:
            self._flush()
            self._last = elem
            if self._elem:
                self._elem[-1].append(elem)
            self._tail = 1
        return elem


class XMLParser:
    def __init__(self, *, target=None, encoding=None):
        parser = _expat.ParserCreate(encoding, "}")
        if target is None:
            target = TreeBuilder()
        self.parser = self._parser = parser
        self.target = self._target = target
        self._error = _expat.error
        self._names = {}
        parser.DefaultHandlerExpand = self._default
        if hasattr(target, "start"):
            parser.StartElementHandler = self._start
        if hasattr(target, "end"):
            parser.EndElementHandler = self._end
        if hasattr(target, "start_ns"):
            parser.StartNamespaceDeclHandler = self._start_ns
        if hasattr(target, "end_ns"):
            parser.EndNamespaceDeclHandler = self._end_ns
        if hasattr(target, "data"):
            parser.CharacterDataHandler = target.data
        if hasattr(target, "comment"):
            parser.CommentHandler = target.comment
        if hasattr(target, "pi"):
            parser.ProcessingInstructionHandler = target.pi
        parser.SkippedEntityHandler = self._skipped
        parser.buffer_text = 1
        parser.ordered_attributes = 1
        self._doctype = None
        self.entity = {}
        self.version = "Expat %d.%d.%d" % _expat.version_info

    def _setevents(self, events_queue, events_to_report):
        parser = self._parser
        append = events_queue.append
        for event_name in events_to_report:
            if event_name == "start":
                parser.ordered_attributes = 1

                def handler(tag, attrib_in, event=event_name, append=append, start=self._start):
                    append((event, start(tag, attrib_in)))
                parser.StartElementHandler = handler
            elif event_name == "end":
                def handler(tag, event=event_name, append=append, end=self._end):
                    append((event, end(tag)))
                parser.EndElementHandler = handler
            elif event_name == "start-ns":
                if hasattr(self.target, "start_ns"):
                    def handler(prefix, uri, event=event_name, append=append, start_ns=self._start_ns):
                        append((event, start_ns(prefix, uri)))
                else:
                    def handler(prefix, uri, event=event_name, append=append):
                        append((event, (prefix or "", uri or "")))
                parser.StartNamespaceDeclHandler = handler
            elif event_name == "end-ns":
                if hasattr(self.target, "end_ns"):
                    def handler(prefix, event=event_name, append=append, end_ns=self._end_ns):
                        append((event, end_ns(prefix)))
                else:
                    def handler(prefix, event=event_name, append=append):
                        append((event, None))
                parser.EndNamespaceDeclHandler = handler
            elif event_name == "comment":
                def handler(text, event=event_name, append=append, self=self):
                    append((event, self.target.comment(text)))
                parser.CommentHandler = handler
            elif event_name == "pi":
                def handler(pi_target, data, event=event_name, append=append, self=self):
                    append((event, self.target.pi(pi_target, data)))
                parser.ProcessingInstructionHandler = handler
            else:
                raise ValueError("unknown event %r" % event_name)

    def _raiseerror(self, value):
        err = ParseError(value)
        err.code = value.code
        err.position = value.lineno, value.offset
        raise err

    def _fixname(self, key):
        try:
            name = self._names[key]
        except KeyError:
            name = key
            if "}" in name:
                name = "{" + name
            self._names[key] = name
        return name

    def _start_ns(self, prefix, uri):
        return self.target.start_ns(prefix or "", uri or "")

    def _end_ns(self, prefix):
        return self.target.end_ns(prefix or "")

    def _start(self, tag, attr_list):
        fixname = self._fixname
        tag = fixname(tag)
        attrib = {}
        if attr_list:
            for i in range(0, len(attr_list), 2):
                attrib[fixname(attr_list[i])] = attr_list[i + 1]
        return self.target.start(tag, attrib)

    def _end(self, tag):
        return self.target.end(self._fixname(tag))

    def _default(self, text):
        pass

    def _skipped(self, name, is_parameter_entity):
        # An entity the document's DTD may declare but this parser cannot
        # read: answered from self.entity, as ElementTree's default handler
        # does.
        value = self.entity.get(name)
        if value is not None:
            if hasattr(self.target, "data"):
                self.target.data(value)
            return
        p = self.parser
        err = _expat.error("undefined entity &%s;: line %d, column %d"
                           % (name, p.CurrentLineNumber, p.CurrentColumnNumber))
        err.code = 11
        err.lineno = p.CurrentLineNumber
        err.offset = p.CurrentColumnNumber
        raise err

    def feed(self, data):
        try:
            self.parser.Parse(data, False)
        except self._error as v:
            self._raiseerror(v)

    def close(self):
        try:
            self.parser.Parse(b"", True)
        except self._error as v:
            self._raiseerror(v)
        try:
            close_handler = self.target.close
        except AttributeError:
            pass
        else:
            return close_handler()
        finally:
            # None rather than del: a closed layout keeps the module typed,
            # and a parser used after close still raises AttributeError.
            self.parser = self._parser = None
            self.target = self._target = None

    def flush(self):
        try:
            self.parser.Parse(b"", False)
        except self._error as v:
            self._raiseerror(v)
