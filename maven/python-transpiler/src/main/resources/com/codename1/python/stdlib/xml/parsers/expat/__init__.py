# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""xml.parsers.expat: a non-validating XML parser with expat's interface.

Written in Python over the regular expression engine, so it runs wherever a
translated program runs. It follows expat where programs can see it: the
handlers and the order they are called in, namespace processing with a
separator, buffered character data, the internal DTD subset (general
entities, attribute defaults, notations), incremental Parse() calls, and
the error codes, messages and positions ElementTree and minidom report.

What it does not do:

- It never reads an external DTD or external entity on its own; an
  ExternalEntityRefHandler decides, as with expat's default settings.
- DefaultHandler and DefaultHandlerExpand are accepted and never called.
- Input encodings are what the codecs module decodes (UTF-8, UTF-16,
  ASCII and Latin-1). Any other declared encoding is "unknown encoding".
- Positions count characters, so CurrentByteIndex and ErrorByteIndex equal
  the character offset rather than the UTF-8 byte offset.
- Names are checked loosely: anything but markup delimiters and whitespace
  is accepted as a name character.
"""

import codecs as _codecs
import re as _re

import _cn1xml

from xml.parsers.expat import errors
from xml.parsers.expat import model

__all__ = ["ParserCreate", "ExpatError", "error", "ErrorString", "XMLParserType", "EXPAT_VERSION",
           "version_info", "native_encoding", "features", "errors", "model"]

EXPAT_VERSION = "expat_2.7.4"
version_info = (2, 7, 4)
native_encoding = "UTF-8"
features = [("sizeof(XML_Char)", 1), ("sizeof(XML_LChar)", 1), ("XML_DTD", 0), ("XML_CONTEXT_BYTES", 1024),
            ("XML_NS", 0)]

XML_PARAM_ENTITY_PARSING_NEVER = 0
XML_PARAM_ENTITY_PARSING_UNLESS_STANDALONE = 1
XML_PARAM_ENTITY_PARSING_ALWAYS = 2

_XML_NS = "http://www.w3.org/XML/1998/namespace"
_XMLNS_NS = "http://www.w3.org/2000/xmlns/"

_NAME = r"[^\s<>/=\"'!?&;%()\[\],|]+"
_Q = r"(?:\"[^\"]*\"|'[^']*')"
_START = _re.compile(r"<(" + _NAME + r")((?:\s+" + _NAME + r"\s*=\s*" + _Q + r")*)\s*(/?)>")
_ATTR = _re.compile(r"(" + _NAME + r")\s*=\s*(?:\"([^\"]*)\"|'([^']*)')")
_END = _re.compile(r"</(" + _NAME + r")\s*>")
_REF = _re.compile(r"&(?:#([0-9]+)|#x([0-9a-fA-F]+)|(" + _NAME + r"));")
_WS_CHARS = _re.compile(r"[\t\n\r]")
_DOCTYPE = _re.compile(r"<!DOCTYPE\s+(" + _NAME + r")(?:\s+(?:SYSTEM\s+(" + _Q + r")|PUBLIC\s+(" + _Q + r")\s+("
                       + _Q + r")))?\s*")
_ENTITY = _re.compile(r"<!ENTITY\s+(%\s+)?(" + _NAME + r")\s+(?:(" + _Q + r")|(?:SYSTEM\s+(" + _Q
                      + r")|PUBLIC\s+(" + _Q + r")\s+(" + _Q + r"))(?:\s+NDATA\s+(" + _NAME + r"))?)\s*>$")
_NOTATION = _re.compile(r"<!NOTATION\s+(" + _NAME + r")\s+(?:SYSTEM\s+(" + _Q + r")|PUBLIC\s+(" + _Q
                        + r")(?:\s+(" + _Q + r"))?)\s*>$")
_ATTDEF = _re.compile(r"\s+(" + _NAME + r")\s+(\([^)]*\)|NOTATION\s*\([^)]*\)|" + _NAME
                      + r")\s+(#REQUIRED|#IMPLIED|(?:#FIXED\s+)?" + _Q + r")")
_XMLDECL = _re.compile(r"<\?xml\s+version\s*=\s*(" + _Q + r")(?:\s+encoding\s*=\s*(" + _Q
                       + r"))?(?:\s+standalone\s*=\s*(" + _Q + r"))?\s*\?>")
_ENC_SNIFF = _re.compile(rb"^<\?xml[^>]*?encoding\s*=\s*[\"']([A-Za-z0-9._:-]+)[\"']")

_PREDEFINED = {"amp": "&", "lt": "<", "gt": ">", "quot": '"', "apos": "'"}

_HANDLER_NAMES = (
    "StartElementHandler", "EndElementHandler", "ProcessingInstructionHandler", "CharacterDataHandler",
    "UnparsedEntityDeclHandler", "NotationDeclHandler", "StartNamespaceDeclHandler",
    "EndNamespaceDeclHandler", "CommentHandler", "StartCdataSectionHandler", "EndCdataSectionHandler",
    "DefaultHandler", "DefaultHandlerExpand", "NotStandaloneHandler", "ExternalEntityRefHandler",
    "StartDoctypeDeclHandler", "EndDoctypeDeclHandler", "EntityDeclHandler", "XmlDeclHandler",
    "ElementDeclHandler", "AttlistDeclHandler", "SkippedEntityHandler",
)


class ExpatError(Exception):
    pass


error = ExpatError


def ErrorString(code):
    return errors.messages.get(code)


def _unquote(q):
    return None if q is None else q[1:-1]


class _Entity:
    def __init__(self, value, sysid, pubid, notation, base):
        self.value = value
        self.sysid = sysid
        self.pubid = pubid
        self.notation = notation
        self.base = base


class XMLParserType:
    """One parse: create with ParserCreate, set handlers, then Parse()."""

    def __init__(self, encoding=None, namespace_separator=None, intern=None):
        self.StartElementHandler = None
        self.EndElementHandler = None
        self.ProcessingInstructionHandler = None
        self.CharacterDataHandler = None
        self.UnparsedEntityDeclHandler = None
        self.NotationDeclHandler = None
        self.StartNamespaceDeclHandler = None
        self.EndNamespaceDeclHandler = None
        self.CommentHandler = None
        self.StartCdataSectionHandler = None
        self.EndCdataSectionHandler = None
        self.DefaultHandler = None
        self.DefaultHandlerExpand = None
        self.NotStandaloneHandler = None
        self.ExternalEntityRefHandler = None
        self.StartDoctypeDeclHandler = None
        self.EndDoctypeDeclHandler = None
        self.EntityDeclHandler = None
        self.XmlDeclHandler = None
        self.ElementDeclHandler = None
        self.AttlistDeclHandler = None
        self.SkippedEntityHandler = None
        self.buffer_text = False
        self.buffer_size = 8192
        self.ordered_attributes = False
        self.specified_attributes = False
        self.namespace_prefixes = False
        self.intern = {} if intern is None else intern
        self.ErrorCode = 0
        self.ErrorLineNumber = 1
        self.ErrorColumnNumber = 0
        self.ErrorByteIndex = -1
        self._encoding = encoding
        self._sep = namespace_separator
        self._raw = b""
        self._decoder = None
        self._buf = ""
        self._base_line = 1
        self._base_col = 0
        self._base_index = 0
        self._ev = 0
        self._started = False
        self._finished = False
        self._failed = False
        self._state = 0
        self._stack = []
        self._ns = {"xml": _XML_NS}
        self._text = []
        self._entities = {}
        self._pentities = {}
        self._expanding = []
        self._ref_pos = -1
        self._attdefaults = {}
        self._seen_doctype = False
        self._ext_subset = False
        self._standalone = -1
        self._foreign_dtd = False
        self._base = None
        self._held_cr = False
        self._outer_buf = ""

    # -- configuration -------------------------------------------------

    def SetBase(self, base):
        self._base = base

    def GetBase(self):
        return self._base

    def SetParamEntityParsing(self, flag):
        return 1

    def UseForeignDTD(self, flag=True):
        if self._started:
            self._raise(26, 0)
        self._foreign_dtd = bool(flag)

    def GetReparseDeferralEnabled(self):
        return False

    def SetReparseDeferralEnabled(self, enabled):
        pass

    def SetAllocTrackerActivationThreshold(self, threshold):
        pass

    def SetAllocTrackerMaximumAmplification(self, factor):
        pass

    def GetInputContext(self):
        if not self._started:
            return None
        i = max(self._ev, 0)
        return self._buf[i:i + 1024].encode("utf-8")

    def ExternalEntityParserCreate(self, context, encoding=None):
        p = XMLParserType(encoding, self._sep, self.intern)
        # Spelled out rather than setattr over _HANDLER_NAMES: a computed
        # attribute name keeps the whole module from being typed.
        p.StartElementHandler = self.StartElementHandler
        p.EndElementHandler = self.EndElementHandler
        p.ProcessingInstructionHandler = self.ProcessingInstructionHandler
        p.CharacterDataHandler = self.CharacterDataHandler
        p.UnparsedEntityDeclHandler = self.UnparsedEntityDeclHandler
        p.NotationDeclHandler = self.NotationDeclHandler
        p.StartNamespaceDeclHandler = self.StartNamespaceDeclHandler
        p.EndNamespaceDeclHandler = self.EndNamespaceDeclHandler
        p.CommentHandler = self.CommentHandler
        p.StartCdataSectionHandler = self.StartCdataSectionHandler
        p.EndCdataSectionHandler = self.EndCdataSectionHandler
        p.DefaultHandler = self.DefaultHandler
        p.DefaultHandlerExpand = self.DefaultHandlerExpand
        p.NotStandaloneHandler = self.NotStandaloneHandler
        p.ExternalEntityRefHandler = self.ExternalEntityRefHandler
        p.StartDoctypeDeclHandler = self.StartDoctypeDeclHandler
        p.EndDoctypeDeclHandler = self.EndDoctypeDeclHandler
        p.EntityDeclHandler = self.EntityDeclHandler
        p.XmlDeclHandler = self.XmlDeclHandler
        p.ElementDeclHandler = self.ElementDeclHandler
        p.AttlistDeclHandler = self.AttlistDeclHandler
        p.SkippedEntityHandler = self.SkippedEntityHandler
        p.buffer_text = self.buffer_text
        p.ordered_attributes = self.ordered_attributes
        p.namespace_prefixes = self.namespace_prefixes
        p._entities = self._entities
        p._attdefaults = self._attdefaults
        p._ns = dict(self._ns)
        p._base = self._base
        # An external entity is content, not a document: it may hold text
        # and several elements at its top level.
        p._state = 1
        p._fragment = True
        return p

    # -- positions -----------------------------------------------------

    def _position(self, i, buf=None):
        if buf is None:
            buf = self._buf
        if i > len(buf):
            i = len(buf)
        nl = buf.count("\n", 0, i)
        if nl == 0:
            return self._base_line, self._base_col + i, self._base_index + i
        return self._base_line + nl, i - buf.rfind("\n", 0, i) - 1, self._base_index + i

    @property
    def CurrentLineNumber(self):
        return self._position(self._ev)[0]

    @property
    def CurrentColumnNumber(self):
        return self._position(self._ev)[1]

    @property
    def CurrentByteIndex(self):
        if not self._started:
            return -1
        return self._position(self._ev)[2]

    @property
    def buffer_used(self):
        return sum(len(t) for t in self._text)

    def _raise(self, code, i):
        buf = None
        if self._ref_pos >= 0:
            i = self._ref_pos
            buf = self._outer_buf
        line, col, index = self._position(i, buf)
        self.ErrorCode = code
        self.ErrorLineNumber = line
        self.ErrorColumnNumber = col
        self.ErrorByteIndex = index
        self._failed = True
        e = ExpatError("%s: line %d, column %d" % (errors.messages[code], line, col))
        e.code = code
        e.lineno = line
        e.offset = col
        raise e

    # -- input ---------------------------------------------------------

    def Parse(self, data, isfinal=False):
        if self._finished:
            self._raise(36, len(self._buf))
        if self._failed:
            self._raise(self.ErrorCode, len(self._buf))
        self._started = True
        if isinstance(data, str):
            text = data
        else:
            text = self._decode(bytes(data), isfinal)
            if text is None:
                return 1
        if text:
            if self._held_cr:
                text = "\r" + text
                self._held_cr = False
            if not isfinal and text.endswith("\r"):
                text = text[:-1]
                self._held_cr = True
            if "\r" in text:
                text = text.replace("\r\n", "\n").replace("\r", "\n")
            if not self._buf and self._base_index == 0 and text.startswith("﻿"):
                text = text[1:]
            self._buf = self._buf + text
        elif isfinal and self._held_cr:
            self._buf = self._buf + "\n"
            self._held_cr = False
        pos = self._run(self._buf, 0, bool(isfinal))
        self._flush()
        if isfinal:
            self._finish(pos)
        else:
            self._compact(pos)
        return 1

    def ParseFile(self, file):
        read = file.read
        while True:
            chunk = read(self.buffer_size if self.buffer_size > 0 else 8192)
            if not chunk:
                break
            self.Parse(chunk, False)
        return self.Parse(b"", True)

    def _decode(self, data, final):
        if self._decoder is None:
            raw = self._raw + data
            if not final and len(raw) < 4:
                self._raw = raw
                return None
            if not final and raw.startswith(b"<?xml") and b"?>" not in raw[:1024] and len(raw) < 1024:
                self._raw = raw
                return None
            self._raw = b""
            enc = self._encoding
            if raw.startswith(b"\xef\xbb\xbf"):
                raw = raw[3:]
                enc = enc or "utf-8"
            elif raw.startswith(b"\xff\xfe") or raw.startswith(b"\xfe\xff"):
                enc = enc or "utf-16"
            elif raw.startswith(b"<\x00?\x00"):
                enc = enc or "utf-16-le"
            elif raw.startswith(b"\x00<\x00?"):
                enc = enc or "utf-16-be"
            if enc is None:
                m = _ENC_SNIFF.match(raw)
                enc = m.group(1).decode("ascii") if m else "utf-8"
            try:
                self._decoder = _codecs.getincrementaldecoder(enc)()
            except LookupError:
                self._started = True
                self._buf = ""
                self._raise(18, 0)
            data = raw
        try:
            return self._decoder.decode(data, final)
        except UnicodeDecodeError:
            self._raise(4, len(self._buf))

    def _compact(self, pos):
        if pos <= 0:
            return
        line, col, index = self._position(pos)
        self._base_line = line
        self._base_col = col
        self._base_index = index
        self._buf = self._buf[pos:]
        self._ev -= pos

    def _finish(self, pos):
        if pos < len(self._buf):
            self._raise(5, pos)
        if getattr(self, "_fragment", False):
            if len(self._stack) > 0:
                self._raise(3, len(self._buf))
        elif self._state == 0 or self._stack:
            self._raise(3, len(self._buf))
        self._finished = True

    # -- events --------------------------------------------------------

    def _flush(self):
        if self._text:
            data = "".join(self._text)
            self._text = []
            h = self.CharacterDataHandler
            if h is not None:
                h(data)

    def _chars(self, s):
        if not s:
            return
        if self.buffer_text:
            self._text.append(s)
            return
        h = self.CharacterDataHandler
        if h is None:
            return
        # Unbuffered, expat reports each line ending as a call of its own.
        start = 0
        while True:
            nl = s.find("\n", start)
            if nl < 0:
                if start < len(s):
                    h(s[start:])
                return
            if nl > start:
                h(s[start:nl])
            h("\n")
            start = nl + 1

    # -- the tokenizer -------------------------------------------------

    def _run(self, buf, pos, final):
        n = len(buf)
        while pos < n:
            lt = buf.find("<", pos)
            if lt != pos:
                end = n if lt < 0 else lt
                if lt < 0 and not final:
                    amp = buf.rfind("&", pos, end)
                    if amp >= 0 and buf.find(";", amp, end) < 0:
                        end = amp
                    if end == pos:
                        return pos
                self._ev = pos
                self._text_run(buf, pos, end)
                pos = end
                continue
            self._ev = pos
            c = buf[pos + 1] if pos + 1 < n else ""
            if c == "/":
                f = _cn1xml.scan_end(buf, pos)
                if f is not None:
                    self._end_tag(f[1], pos)
                    pos = f[0]
                    continue
                m = _END.match(buf, pos)
                if m is None:
                    if not final and buf.find(">", pos) < 0:
                        return pos
                    self._raise(4, pos + 2)
                self._end_tag(m.group(1), pos)
                pos = m.end()
                continue
            if c == "?":
                e = buf.find("?>", pos + 2)
                if e < 0:
                    if not final:
                        return pos
                    self._raise(5, pos)
                self._pi(buf, pos, e)
                pos = e + 2
                continue
            if c == "!":
                if buf.startswith("<!--", pos):
                    e = buf.find("-->", pos + 4)
                    if e < 0:
                        if not final:
                            return pos
                        self._raise(5, pos)
                    body = buf[pos + 4:e]
                    bad = body.find("--")
                    if bad >= 0 or body.endswith("-"):
                        self._raise(4, pos + 4 + (bad + 2 if bad >= 0 else len(body) + 1))
                    self._flush()
                    h = self.CommentHandler
                    if h is not None:
                        h(body)
                    pos = e + 3
                    continue
                if buf.startswith("<![CDATA[", pos):
                    e = buf.find("]]>", pos + 9)
                    if e < 0:
                        if not final:
                            return pos
                        self._raise(20, pos)
                    if self._state != 1:
                        self._raise(4, pos)
                    self._cdata(buf[pos + 9:e])
                    pos = e + 3
                    continue
                if buf.startswith("<!DOCTYPE", pos):
                    r = self._doctype(buf, pos, final)
                    if r < 0:
                        return pos
                    pos = r
                    continue
                if not final and n - pos < 9:
                    return pos
                self._raise(4 if self._state == 1 else 2, pos)
            f = _cn1xml.scan_start(buf, pos)
            if f is not None:
                fa = f[2]
                self._start_tail(f[1], fa[0::2], fa[1::2], pos, f[3])
                pos = f[0]
                continue
            m = _START.match(buf, pos)
            if m is None:
                if not final:
                    return pos
                if buf.find(">", pos) < 0:
                    self._raise(5, pos)
                self._raise(4, pos + 1)
            self._start_tag(m, pos)
            pos = m.end()
        return pos

    def _text_run(self, buf, s, e):
        if self._state != 1:
            i = s
            while i < e and buf[i] in " \t\n":
                i += 1
            if i < e:
                self._raise(2 if self._state == 0 else 9, i)
            return
        if "]]>" in buf[s:e]:
            self._raise(4, buf.find("]]>", s, e) + 2)
        amp = buf.find("&", s, e)
        if amp < 0:
            self._chars(buf[s:e])
            return
        p = s
        while amp >= 0:
            if amp > p:
                self._chars(buf[p:amp])
            m = _REF.match(buf, amp, e)
            if m is None:
                self._raise(4, amp + 1)
            self._content_ref(m, amp)
            p = m.end()
            amp = buf.find("&", p, e)
        if p < e:
            self._chars(buf[p:e])

    def _char_ref(self, m, at):
        if m.group(1) is not None:
            cp = int(m.group(1))
        else:
            cp = int(m.group(2), 16)
        if cp == 0 or cp > 0x10FFFF or 0xD800 <= cp <= 0xDFFF or cp in (0xFFFE, 0xFFFF) or (
                cp < 0x20 and cp not in (9, 10, 13)):
            self._raise(14, at)
        return chr(cp)

    def _content_ref(self, m, at):
        name = m.group(3)
        if name is None:
            self._chars(self._char_ref(m, at))
            return
        v = _PREDEFINED.get(name)
        if v is not None:
            self._chars(v)
            return
        ent = self._entities.get(name)
        if ent is None:
            if self._ext_subset or self._foreign_dtd:
                self._flush()
                h = self.SkippedEntityHandler
                if h is not None:
                    h(name, 0)
                return
            self._raise(11, at)
        if ent.notation is not None:
            self._raise(15, at + 1)
        if ent.value is None:
            h = self.ExternalEntityRefHandler
            self._flush()
            if h is not None:
                if not h(name, ent.base, ent.sysid, ent.pubid):
                    self._raise(21, at)
            else:
                h = self.SkippedEntityHandler
                if h is not None:
                    h(name, 0)
            return
        if name in self._expanding:
            self._raise(12, at + 1)
        value = ent.value
        if "<" not in value and "&" not in value:
            self._chars(value)
            return
        self._expand(name, value, at)

    def _expand(self, name, value, at):
        # The replacement text is parsed as content where the reference
        # stood; errors inside it are reported at the reference.
        saved_buf = self._buf
        saved_ref = self._ref_pos
        saved_ev = self._ev
        depth = len(self._stack)
        self._expanding.append(name)
        if self._ref_pos < 0:
            self._ref_pos = at
            self._outer_buf = saved_buf
        try:
            r = _EntityRun(self)
            r.run(value)
        finally:
            self._expanding.pop()
            self._ref_pos = saved_ref
            self._buf = saved_buf
            self._ev = saved_ev
        if len(self._stack) != depth:
            self._raise(13, at)

    def _attr_value(self, raw, at):
        lt = raw.find("<")
        if lt >= 0:
            self._raise(4, at + lt)
        if "&" not in raw:
            if "\t" in raw or "\n" in raw:
                return _WS_CHARS.sub(" ", raw)
            return raw
        out = []
        p = 0
        for m in _REF.finditer(raw):
            seg = raw[p:m.start()]
            if "&" in seg:
                self._raise(4, at)
            out.append(_WS_CHARS.sub(" ", seg))
            name = m.group(3)
            if name is None:
                out.append(self._char_ref(m, at))
            elif name in _PREDEFINED:
                out.append(_PREDEFINED[name])
            else:
                ent = self._entities.get(name)
                if ent is None:
                    if self._ext_subset or self._foreign_dtd:
                        p = m.end()
                        continue
                    self._raise(11, at)
                if ent.value is None:
                    self._raise(16 if ent.notation is None else 15, at)
                if name in self._expanding:
                    self._raise(12, at)
                self._expanding.append(name)
                try:
                    out.append(self._attr_value(ent.value, at))
                finally:
                    self._expanding.pop()
            p = m.end()
        seg = raw[p:]
        if "&" in seg:
            self._raise(4, at)
        out.append(_WS_CHARS.sub(" ", seg))
        return "".join(out)

    def _start_tag(self, m, pos):
        if self._state == 2:
            self._raise(9, pos)
        qname = m.group(1)
        raw_attrs = m.group(2)
        names = []
        values = []
        if raw_attrs:
            for a in _ATTR.finditer(raw_attrs):
                an = a.group(1)
                if an in names:
                    self._raise(8, pos + 1 + len(qname) + a.start())
                names.append(an)
                v = a.group(2)
                g = 2 if v is not None else 3
                values.append(self._attr_value(a.group(g), m.start(2) + a.start(g)))
        self._start_tail(qname, names, values, pos, m.group(3))

    # The fast scan (_cn1xml.scan_start) joins here with names and values
    # already checked and normalised.
    def _start_tail(self, qname, names, values, pos, self_closing):
        if self._state == 2:
            self._raise(9, pos)
        # specified_attributes leaves out the values only a DTD default gave.
        defaults = None if self.specified_attributes else self._attdefaults.get(qname)
        if defaults:
            for an, dv in defaults:
                if an not in names:
                    names.append(an)
                    values.append(dv)
        parent_ns = self._ns
        declared = []
        if self._sep is not None:
            ns = None
            keep_n = []
            keep_v = []
            for an, av in zip(names, values):
                if an == "xmlns" or an.startswith("xmlns:"):
                    prefix = an[6:] if len(an) > 5 else None
                    if prefix == "xmlns":
                        self._raise(39, pos)
                    if prefix == "xml" and av != _XML_NS:
                        self._raise(38, pos)
                    if prefix != "xml" and av in (_XML_NS, _XMLNS_NS):
                        self._raise(40, pos)
                    if prefix is not None and av == "":
                        self._raise(28, pos)
                    if ns is None:
                        ns = dict(parent_ns)
                    ns[prefix or ""] = av
                    declared.append((prefix, av if av != "" else None))
                else:
                    keep_n.append(an)
                    keep_v.append(av)
            if ns is not None:
                self._ns = ns
            names = keep_n
            values = keep_v
            name = self._expand_name(qname, True, pos)
            expanded = []
            for i in range(len(names)):
                en = self._expand_name(names[i], False, pos)
                if en in expanded:
                    self._raise(8, pos)
                expanded.append(en)
            names = expanded
        else:
            name = qname
        self._flush()
        h = self.StartNamespaceDeclHandler
        if h is not None:
            for prefix, uri in declared:
                h(prefix, uri)
        if self._state == 0:
            self._state = 1
        h = self.StartElementHandler
        if h is not None:
            if self.ordered_attributes:
                attrs = []
                for i in range(len(names)):
                    attrs.append(names[i])
                    attrs.append(values[i])
            else:
                attrs = {}
                for i in range(len(names)):
                    attrs[names[i]] = values[i]
            h(name, attrs)
        self._stack.append((qname, name, declared, parent_ns))
        if self_closing:
            self._ev = pos
            self._close_top()

    def _expand_name(self, qname, element, pos):
        colon = qname.find(":")
        if colon < 0:
            if not element:
                return qname
            uri = self._ns.get("")
            if not uri:
                return qname
            return uri + self._sep + qname
        prefix = qname[:colon]
        local = qname[colon + 1:]
        uri = self._ns.get(prefix)
        if uri is None:
            self._raise(27, pos)
        if self.namespace_prefixes:
            return uri + self._sep + local + self._sep + prefix
        return uri + self._sep + local

    def _end_tag(self, qname, pos):
        if not self._stack:
            self._raise(9 if self._state == 2 else 2, pos)
        if self._stack[-1][0] != qname:
            self._raise(7, pos + 2)
        self._close_top()

    def _close_top(self):
        qname, name, declared, parent_ns = self._stack.pop()
        self._flush()
        h = self.EndElementHandler
        if h is not None:
            h(name)
        h = self.EndNamespaceDeclHandler
        if h is not None:
            for prefix, uri in reversed(declared):
                h(prefix)
        self._ns = parent_ns
        if not self._stack and not self._expanding and not getattr(self, "_fragment", False):
            self._state = 2

    def _cdata(self, body):
        hs = self.StartCdataSectionHandler
        he = self.EndCdataSectionHandler
        if hs is not None or he is not None:
            self._flush()
            if hs is not None:
                hs()
            self._chars(body)
            self._flush()
            if he is not None:
                he()
        else:
            self._chars(body)

    def _pi(self, buf, pos, e):
        body = buf[pos + 2:e]
        i = 0
        while i < len(body) and body[i] not in " \t\n":
            i += 1
        target = body[:i]
        if not target:
            self._raise(4, pos + 2)
        if target.lower() == "xml":
            if target == "xml" and pos == 0 and self._base_index == 0 and not self._expanding:
                self._xml_decl(buf, pos, e)
                return
            self._raise(17, pos)
        data = body[i:].lstrip(" \t\n")
        self._flush()
        h = self.ProcessingInstructionHandler
        if h is not None:
            h(target, data)

    def _xml_decl(self, buf, pos, e):
        m = _XMLDECL.match(buf, pos)
        if m is None or m.end() != e + 2:
            self._raise(30, pos)
        version = _unquote(m.group(1))
        encoding = _unquote(m.group(2))
        sa = _unquote(m.group(3))
        if sa is None:
            standalone = -1
        elif sa == "yes":
            standalone = 1
        elif sa == "no":
            standalone = 0
        else:
            self._raise(30, pos)
        self._standalone = standalone
        h = self.XmlDeclHandler
        if h is not None:
            h(version, encoding, standalone)

    # -- the DTD -------------------------------------------------------

    def _doctype(self, buf, pos, final):
        if self._state != 0 or self._seen_doctype:
            self._raise(2, pos)
        if not final and buf.find(">", pos) < 0:
            return -1
        m = _DOCTYPE.match(buf, pos)
        if m is None:
            if not final:
                return -1
            self._raise(2, pos)
        n = len(buf)
        p = m.end()
        subset = None
        if p < n and buf[p] == "[":
            e = _subset_end(buf, p + 1)
            if e < 0:
                if not final:
                    return -1
                self._raise(5, pos)
            subset = buf[p + 1:e]
            p = e + 1
            while p < n and buf[p] in " \t\n":
                p += 1
        if p >= n:
            if not final:
                return -1
            self._raise(5, pos)
        if buf[p] != ">":
            self._raise(2, p)
        self._seen_doctype = True
        name = m.group(1)
        sysid = _unquote(m.group(2) if m.group(2) is not None else m.group(4))
        pubid = _unquote(m.group(3))
        if sysid is not None:
            self._ext_subset = True
        self._flush()
        h = self.StartDoctypeDeclHandler
        if h is not None:
            h(name, sysid, pubid, 1 if subset is not None else 0)
        if subset is not None:
            self._subset(subset, pos)
        h = self.EndDoctypeDeclHandler
        if h is not None:
            h()
        if self._ext_subset and self._standalone != 1:
            h = self.NotStandaloneHandler
            if h is not None and not h():
                self._raise(22, pos)
        return p + 1

    def _subset(self, s, at):
        n = len(s)
        i = 0
        while i < n:
            c = s[i]
            if c in " \t\n":
                i += 1
                continue
            if c == "%":
                e = s.find(";", i)
                if e < 0:
                    self._raise(2, at)
                # A parameter entity can bring in declarations this parser
                # never reads, so undeclared entities stop being errors.
                self._ext_subset = True
                i = e + 1
                continue
            if s.startswith("<!--", i):
                e = s.find("-->", i + 4)
                if e < 0:
                    self._raise(5, at)
                h = self.CommentHandler
                if h is not None:
                    h(s[i + 4:e])
                i = e + 3
                continue
            if s.startswith("<?", i):
                e = s.find("?>", i + 2)
                if e < 0:
                    self._raise(5, at)
                body = s[i + 2:e]
                parts = body.split(None, 1)
                h = self.ProcessingInstructionHandler
                if h is not None and parts:
                    h(parts[0], parts[1] if len(parts) > 1 else "")
                i = e + 2
                continue
            if s.startswith("<!", i):
                e = _decl_end(s, i)
                if e < 0:
                    self._raise(5, at)
                self._decl(s[i:e + 1], at)
                i = e + 1
                continue
            self._raise(2, at)

    def _decl(self, d, at):
        if d.startswith("<!ENTITY"):
            m = _ENTITY.match(d)
            if m is None:
                self._raise(2, at)
            is_pe = m.group(1) is not None
            name = m.group(2)
            value = _unquote(m.group(3))
            sysid = _unquote(m.group(4) if m.group(4) is not None else m.group(6))
            pubid = _unquote(m.group(5))
            notation = m.group(7)
            if value is not None:
                value = self._entity_value(value, at)
            table = self._pentities if is_pe else self._entities
            if name in table:
                return
            table[name] = _Entity(value, sysid, pubid, notation, self._base)
            h = self.EntityDeclHandler
            if h is not None:
                h(name, 1 if is_pe else 0, value, self._base, sysid, pubid, notation)
            elif notation is not None:
                h = self.UnparsedEntityDeclHandler
                if h is not None:
                    h(name, self._base, sysid, pubid, notation)
            return
        if d.startswith("<!ATTLIST"):
            rest = d[9:-1]
            m = _re.match(r"\s+(" + _NAME + r")", rest)
            if m is None:
                self._raise(2, at)
            el = m.group(1)
            p = m.end()
            while True:
                a = _ATTDEF.match(rest, p)
                if a is None:
                    break
                p = a.end()
                attname = a.group(1)
                atype = a.group(2)
                dflt = a.group(3)
                required = 1 if dflt == "#REQUIRED" else 0
                value = None
                if dflt not in ("#REQUIRED", "#IMPLIED"):
                    q = dflt[dflt.find(dflt[-1]):]
                    value = self._attr_value(q[1:-1], at)
                    known = self._attdefaults.setdefault(el, [])
                    if all(x[0] != attname for x in known):
                        known.append((attname, value))
                if atype.startswith("("):
                    atype = atype.replace(" ", "").replace("\t", "").replace("\n", "")
                h = self.AttlistDeclHandler
                if h is not None:
                    h(el, attname, atype, value, required)
            if rest[p:].strip(" \t\n"):
                self._raise(2, at)
            return
        if d.startswith("<!ELEMENT"):
            m = _re.match(r"<!ELEMENT\s+(" + _NAME + r")\s+(.*?)\s*>$", d, _re.S)
            if m is None:
                self._raise(2, at)
            h = self.ElementDeclHandler
            if h is not None:
                h(m.group(1), _content_model(m.group(2)))
            return
        if d.startswith("<!NOTATION"):
            m = _NOTATION.match(d)
            if m is None:
                self._raise(2, at)
            h = self.NotationDeclHandler
            if h is not None:
                sysid = _unquote(m.group(2) if m.group(2) is not None else m.group(4))
                h(m.group(1), self._base, sysid, _unquote(m.group(3)))
            return
        self._raise(2, at)

    def _entity_value(self, v, at):
        # Character and parameter-entity references are replaced when the
        # entity is declared; general entity references stay for later.
        if "&#" not in v and "%" not in v:
            return v
        out = []
        p = 0
        for m in _re.finditer(r"&#([0-9]+);|&#x([0-9a-fA-F]+);|%(" + _NAME + r");", v):
            out.append(v[p:m.start()])
            if m.group(3) is not None:
                pe = self._pentities.get(m.group(3))
                if pe is None or pe.value is None:
                    self._ext_subset = True
                else:
                    out.append(pe.value)
            else:
                out.append(self._char_ref(m, at))
            p = m.end()
        out.append(v[p:])
        return "".join(out)


class _EntityRun:
    """Parses one internal entity's replacement text as content of the
    parser it belongs to."""

    def __init__(self, parser):
        self.parser = parser

    def run(self, value):
        p = self.parser
        p._buf = value
        end = p._run(value, 0, True)
        if end < len(value):
            p._raise(5, end)


def _subset_end(buf, i):
    n = len(buf)
    while i < n:
        c = buf[i]
        if c == "]":
            return i
        if c == '"' or c == "'":
            e = buf.find(c, i + 1)
            if e < 0:
                return -1
            i = e + 1
            continue
        if buf.startswith("<!--", i):
            e = buf.find("-->", i + 4)
            if e < 0:
                return -1
            i = e + 3
            continue
        if buf.startswith("<?", i):
            e = buf.find("?>", i + 2)
            if e < 0:
                return -1
            i = e + 2
            continue
        i += 1
    return -1


def _decl_end(s, i):
    n = len(s)
    while i < n:
        c = s[i]
        if c == ">":
            return i
        if c == '"' or c == "'":
            e = s.find(c, i + 1)
            if e < 0:
                return -1
            i = e + 1
            continue
        i += 1
    return -1


def _content_model(spec):
    spec = spec.strip()
    if spec == "EMPTY":
        return (model.XML_CTYPE_EMPTY, model.XML_CQUANT_NONE, None, ())
    if spec == "ANY":
        return (model.XML_CTYPE_ANY, model.XML_CQUANT_NONE, None, ())
    toks = _re.findall(r"#PCDATA|[(),|?*+]|[^\s(),|?*+]+", spec)
    pos = [0]

    def quant():
        if pos[0] < len(toks) and toks[pos[0]] in ("?", "*", "+"):
            t = toks[pos[0]]
            pos[0] += 1
            return {"?": model.XML_CQUANT_OPT, "*": model.XML_CQUANT_REP, "+": model.XML_CQUANT_PLUS}[t]
        return model.XML_CQUANT_NONE

    def particle():
        t = toks[pos[0]]
        pos[0] += 1
        if t == "(":
            return group()
        return (model.XML_CTYPE_NAME, quant(), t, ())

    def group():
        items = []
        kind = model.XML_CTYPE_SEQ
        mixed = False
        while pos[0] < len(toks):
            t = toks[pos[0]]
            if t == "#PCDATA":
                mixed = True
                pos[0] += 1
                continue
            if t == ")":
                pos[0] += 1
                break
            if t == "|":
                kind = model.XML_CTYPE_CHOICE
                pos[0] += 1
                continue
            if t == ",":
                pos[0] += 1
                continue
            items.append(particle())
        q = quant()
        if mixed:
            return (model.XML_CTYPE_MIXED, q, None, tuple(items))
        return (kind, q, None, tuple(items))

    if toks and toks[0] == "(":
        pos[0] = 1
        return group()
    return (model.XML_CTYPE_ANY, model.XML_CQUANT_NONE, None, ())


def ParserCreate(encoding=None, namespace_separator=None, intern=None):
    if namespace_separator is not None:
        if not isinstance(namespace_separator, str):
            raise TypeError("ParserCreate() argument 'namespace_separator' must be str or None, not "
                            + type(namespace_separator).__name__)
        if len(namespace_separator) > 1:
            raise ValueError("namespace_separator must be at most one character, omitted, or None")
    if encoding is not None and not isinstance(encoding, str):
        raise TypeError("ParserCreate() argument 'encoding' must be str or None, not " + type(encoding).__name__)
    return XMLParserType(encoding, namespace_separator, intern)
