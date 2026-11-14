# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/html/parser.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""html.parser: a tolerant, event-driven HTML tokenizer.

Tags, attributes, comments, declarations and character references are read
the way the HTML5 tokenizer reads them; the text of script and style (and
the other raw-text elements) is reported verbatim up to its end tag."""

import re as _re
from html import unescape
import _markupbase

__all__ = ["HTMLParser"]

_WS = " \t\n\r\f"
_TAG_END = " \t\n\r\f/>"
_starttagopen = _re.compile(r"<[a-zA-Z]")
_charref = _re.compile(r"&#(?:[0-9]+|[xX][0-9a-fA-F]+)[^0-9a-fA-F]")
_entityref = _re.compile(r"&([a-zA-Z][-.a-zA-Z0-9]*)[^a-zA-Z0-9]")
_interesting_normal = _re.compile(r"[&<]")
_incomplete = _re.compile(r"&[a-zA-Z#]")
_amp_end = _re.compile(r"[\t\n\r\f ;]")
_entity_tail = _re.compile(r"&([a-zA-Z][-.a-zA-Z0-9]*)\Z")
_charref_tail = _re.compile(r"&#([0-9]+|[xX][0-9a-fA-F]+)\Z")

CDATA_CONTENT_ELEMENTS = ("script", "style")
RCDATA_CONTENT_ELEMENTS = ("textarea", "title")


class HTMLParser(_markupbase.ParserBase):
    CDATA_CONTENT_ELEMENTS = CDATA_CONTENT_ELEMENTS
    RCDATA_CONTENT_ELEMENTS = RCDATA_CONTENT_ELEMENTS

    def __init__(self, *, convert_charrefs=True):
        super().__init__()
        self.convert_charrefs = convert_charrefs
        self.reset()

    def reset(self):
        self.rawdata = ""
        self.lasttag = "???"
        self.interesting = _interesting_normal
        self.cdata_elem = None
        self._escapable = True
        self._support_cdata = True
        self.__starttag_text = None
        super().reset()

    def feed(self, data):
        self.rawdata = self.rawdata + data
        self.goahead(False)

    def close(self):
        self.goahead(True)

    __starttag_text = None

    def get_starttag_text(self):
        return self.__starttag_text

    def set_cdata_mode(self, elem, *, escapable=False):
        self.cdata_elem = elem.lower()
        self._escapable = escapable
        if escapable and not self.convert_charrefs:
            self.interesting = _re.compile(r"&|</%s(?=[\t\n\r\f />])" % self.cdata_elem, _re.IGNORECASE | _re.ASCII)
        else:
            self.interesting = _re.compile(r"</%s(?=[\t\n\r\f />])" % self.cdata_elem, _re.IGNORECASE | _re.ASCII)

    def clear_cdata_mode(self):
        self.interesting = _interesting_normal
        self.cdata_elem = None
        self._escapable = True

    def _set_support_cdata(self, flag=True):
        self._support_cdata = flag

    def _data(self, text):
        if self.convert_charrefs and (self.cdata_elem is None or self._escapable):
            text = unescape(text)
        self.handle_data(text)

    def goahead(self, end):
        rawdata = self.rawdata
        i = 0
        n = len(rawdata)
        while i < n:
            if self.convert_charrefs and not self.cdata_elem:
                j = rawdata.find("<", i)
                if j < 0:
                    amppos = rawdata.rfind("&", max(i, n - 34))
                    if amppos >= 0 and not _amp_end.search(rawdata, amppos) and not end:
                        break
                    j = n
            else:
                m = self.interesting.search(rawdata, i)
                if m:
                    j = m.start()
                elif self.cdata_elem is not None:
                    # Raw text runs to its end tag: wait for it.
                    if not end:
                        break
                    j = n
                else:
                    j = n
            if i < j:
                self._data(rawdata[i:j])
            i = self.updatepos(i, j)
            if i == n:
                break
            if rawdata.startswith("<", i):
                if _starttagopen.match(rawdata, i):
                    k = self.parse_starttag(i)
                elif rawdata.startswith("</", i):
                    k = self.parse_endtag(i)
                elif rawdata.startswith("<!--", i):
                    k = self.parse_comment(i)
                elif rawdata.startswith("<?", i):
                    k = self.parse_pi(i)
                elif rawdata.startswith("<!", i):
                    k = self.parse_html_declaration(i)
                elif i + 1 < n or end:
                    self.handle_data("<")
                    k = i + 1
                else:
                    break
                if k < 0:
                    if not end:
                        break
                    k = self._eof_construct(i)
                    if k < 0:
                        break
                i = self.updatepos(i, k)
            elif rawdata.startswith("&#", i):
                m = _charref.match(rawdata, i)
                if m:
                    name = m.group()[2:-1]
                    self.handle_charref(name)
                    k = m.end()
                    if not rawdata.startswith(";", k - 1):
                        k = k - 1
                    i = self.updatepos(i, k)
                    continue
                if ";" in rawdata[i:]:
                    self.handle_data(rawdata[i:i + 2])
                    i = self.updatepos(i, i + 2)
                break
            elif rawdata.startswith("&", i):
                m = _entityref.match(rawdata, i)
                if m:
                    name = m.group(1)
                    self.handle_entityref(name)
                    k = m.end()
                    if not rawdata.startswith(";", k - 1):
                        k = k - 1
                    i = self.updatepos(i, k)
                    continue
                m = _incomplete.match(rawdata, i)
                if m:
                    if end:
                        tail = _entity_tail.match(rawdata, i)
                        if tail:
                            self.handle_entityref(tail.group(1))
                            i = self.updatepos(i, tail.end())
                            continue
                        tail = _charref_tail.match(rawdata, i)
                        if tail:
                            self.handle_charref(tail.group(1))
                            i = self.updatepos(i, tail.end())
                            continue
                    break
                elif i + 1 < n:
                    self.handle_data("&")
                    i = self.updatepos(i, i + 1)
                else:
                    break
            else:
                raise AssertionError("we should not get here!")
        if end and i < n:
            if self.convert_charrefs and (self.cdata_elem is None or self._escapable):
                self.handle_data(unescape(rawdata[i:n]))
            else:
                self.handle_data(rawdata[i:n])
            i = self.updatepos(i, n)
        self.rawdata = rawdata[i:]

    def _eof_construct(self, i):
        """A construct still open at the end of the input: a comment or a
        declaration is closed there; an unfinished tag is dropped."""
        rawdata = self.rawdata
        n = len(rawdata)
        if rawdata.startswith("<!--", i):
            self.handle_comment(rawdata[i + 4:])
            return n
        if rawdata.startswith("<![CDATA[", i) and self._support_cdata:
            self.unknown_decl(rawdata[i + 3:])
            return n
        if rawdata.startswith("<!", i):
            if rawdata[i + 2:i + 9].lower() == "doctype":
                self.handle_decl(rawdata[i + 2:])
            else:
                self.handle_comment(rawdata[i + 2:])
            return n
        if rawdata.startswith("<?", i):
            self.handle_pi(rawdata[i + 2:])
            return n
        if rawdata.startswith("</", i):
            if i + 2 < n and rawdata[i + 2].isascii() and rawdata[i + 2].isalpha():
                return n
            self.handle_comment(rawdata[i + 2:])
            return n
        return n

    def parse_html_declaration(self, i):
        rawdata = self.rawdata
        if rawdata[i:i + 4] == "<!--":
            return self.parse_comment(i)
        if rawdata[i:i + 9] == "<![CDATA[" and self._support_cdata:
            j = rawdata.find("]]>", i + 9)
            if j < 0:
                return -1
            self.unknown_decl(rawdata[i + 3:j])
            return j + 3
        if rawdata[i:i + 9].lower() == "<!doctype":
            gtpos = rawdata.find(">", i + 9)
            if gtpos == -1:
                return -1
            self.handle_decl(rawdata[i + 2:gtpos])
            return gtpos + 1
        return self.parse_bogus_comment(i)

    def parse_comment(self, i, report=True):
        rawdata = self.rawdata
        if rawdata[i:i + 4] != "<!--":
            raise AssertionError("unexpected call to parse_comment()")
        j = i + 4
        if rawdata.startswith(">", j):
            if report:
                self.handle_comment("")
            return j + 1
        if rawdata.startswith("->", j):
            if report:
                self.handle_comment("")
            return j + 2
        k = j
        while True:
            k = rawdata.find("--", k)
            if k < 0:
                return -1
            if rawdata.startswith("-->", k):
                end = k
                close = k + 3
                break
            if rawdata.startswith("--!>", k):
                end = k
                close = k + 4
                break
            if k + 2 >= len(rawdata) or (rawdata[k + 2] == "!" and k + 3 >= len(rawdata)):
                return -1
            k += 1
        if report:
            self.handle_comment(rawdata[j:end])
        return close

    def parse_bogus_comment(self, i, report=True):
        rawdata = self.rawdata
        if rawdata[i:i + 2] not in ("<!", "</"):
            raise AssertionError("unexpected call to parse_bogus_comment()")
        pos = rawdata.find(">", i + 2)
        if pos == -1:
            return -1
        if report:
            self.handle_comment(rawdata[i + 2:pos])
        return pos + 1

    def parse_pi(self, i):
        rawdata = self.rawdata
        if rawdata[i:i + 2] != "<?":
            raise AssertionError("unexpected call to parse_pi()")
        j = rawdata.find(">", i + 2)
        if j < 0:
            return -1
        self.handle_pi(rawdata[i + 2:j])
        return j + 1

    def _scan_starttag(self, i):
        """Reads the start tag at i: (tag, attrs, end, selfclosing), or None
        when its closing '>' has not arrived yet."""
        rawdata = self.rawdata
        n = len(rawdata)
        k = i + 1
        while k < n and rawdata[k] not in _TAG_END:
            k += 1
        tag = rawdata[i + 1:k].lower()
        attrs = []
        selfclosing = False
        while True:
            while k < n and (rawdata[k] in _WS or rawdata[k] == "/"):
                if rawdata[k] == "/" and k + 1 < n and rawdata[k + 1] == ">":
                    selfclosing = True
                    k += 1
                    break
                k += 1
            if k >= n:
                return None
            if rawdata[k] == ">":
                return tag, attrs, k + 1, selfclosing
            start = k
            k += 1
            while k < n and rawdata[k] not in " \t\n\r\f/>=":
                k += 1
            name = rawdata[start:k].lower()
            p = k
            while p < n and rawdata[p] in _WS:
                p += 1
            if p < n and rawdata[p] == "=":
                p += 1
                while p < n and rawdata[p] in _WS:
                    p += 1
                if p >= n:
                    return None
                q = rawdata[p]
                if q == '"' or q == "'":
                    e = rawdata.find(q, p + 1)
                    if e < 0:
                        return None
                    value = rawdata[p + 1:e]
                    k = e + 1
                else:
                    e = p
                    while e < n and rawdata[e] not in _WS and rawdata[e] != ">":
                        e += 1
                    value = rawdata[p:e]
                    k = e
                attrs.append((name, unescape(value)))
            else:
                if p >= n:
                    return None
                attrs.append((name, None))
                k = p if p > k else k

    def parse_starttag(self, i):
        scanned = self._scan_starttag(i)
        if scanned is None:
            return -1
        tag, attrs, endpos, selfclosing = scanned
        self.__starttag_text = self.rawdata[i:endpos]
        self.lasttag = tag
        if selfclosing:
            self.handle_startendtag(tag, attrs)
        else:
            self.handle_starttag(tag, attrs)
            if tag in self.CDATA_CONTENT_ELEMENTS:
                self.set_cdata_mode(tag)
            elif tag in self.RCDATA_CONTENT_ELEMENTS:
                self.set_cdata_mode(tag, escapable=True)
        return endpos

    def check_for_whole_start_tag(self, i):
        scanned = self._scan_starttag(i)
        if scanned is None:
            return -1
        return scanned[2]

    def parse_endtag(self, i):
        rawdata = self.rawdata
        n = len(rawdata)
        if rawdata[i:i + 2] != "</":
            raise AssertionError("unexpected call to parse_endtag")
        if i + 2 >= n:
            return -1
        c = rawdata[i + 2]
        if c == ">":
            return i + 3
        if not (c.isascii() and c.isalpha()):
            if self.cdata_elem is not None:
                self.handle_data(rawdata[i:i + 2])
                return i + 2
            return self.parse_bogus_comment(i)
        k = i + 2
        while k < n and rawdata[k] not in _TAG_END:
            k += 1
        tag = rawdata[i + 2:k].lower()
        scanned = self._scan_endtag_rest(k)
        if scanned < 0:
            return -1
        if self.cdata_elem is not None and tag != self.cdata_elem:
            self.handle_data(rawdata[i:scanned])
            return scanned
        self.handle_endtag(tag)
        self.clear_cdata_mode()
        return scanned

    def _scan_endtag_rest(self, k):
        """Skips the attributes an end tag may (wrongly) carry, honouring
        quotes the way a start tag does; answers the index after '>'."""
        rawdata = self.rawdata
        n = len(rawdata)
        while k < n:
            c = rawdata[k]
            if c == ">":
                return k + 1
            if c == "=":
                p = k + 1
                while p < n and rawdata[p] in _WS:
                    p += 1
                if p < n and rawdata[p] in "\"'":
                    e = rawdata.find(rawdata[p], p + 1)
                    if e < 0:
                        return -1
                    k = e + 1
                    continue
                k = p
                continue
            k += 1
        return -1

    def handle_startendtag(self, tag, attrs):
        self.handle_starttag(tag, attrs)
        self.handle_endtag(tag)

    def handle_starttag(self, tag, attrs):
        pass

    def handle_endtag(self, tag):
        pass

    def handle_charref(self, name):
        pass

    def handle_entityref(self, name):
        pass

    def handle_data(self, data):
        pass

    def handle_comment(self, data):
        pass

    def handle_decl(self, decl):
        pass

    def handle_pi(self, data):
        pass

    def unknown_decl(self, data):
        pass
