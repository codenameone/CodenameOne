# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""lxml.etree: a pure-Python subset of lxml's ElementTree API.

Written from lxml's documented API and its observed output, over the
runtime's pure-Python expat. What it covers:

- Elements with lxml's tree model: getparent(), siblings, addnext() and
  addprevious(), tails that travel with their element, nsmap and prefix,
  namespace declarations that follow an element moved between trees the way
  lxml's do, and comments and processing instructions as tree nodes.
- ElementBase subclasses chosen per element by ElementNamespaceClassLookup
  or ElementDefaultClassLookup, for parsed and for created elements.
- XMLParser (remove_blank_text, remove_comments, remove_pis, recover,
  target= parsers fed incrementally), fromstring/XML/parse/iterparse.
- tostring() byte-compatible with libxml2's serializer: namespace
  declarations, empty elements, escaping, pretty_print, declarations.
- xpath() with XPath 1.0 location paths, predicates, unions and the core
  functions; find/findall/iterfind/findtext.
- A small HTMLParser over html.parser, for target parsers only in spirit:
  it does not repair documents the way libxml2's HTML parser does.

Not provided: XSLT, schemas, objectify, C14N, DTD validation, variables in
XPath, and LXML_VERSION (so that code probing for the C library keeps its
pure-Python fallback).
"""
import html.parser as _htmlparser
from xml.parsers import expat as _expat

from lxml import _xpath

__version__ = "5.3.0"

XML_NS = "http://www.w3.org/XML/1998/namespace"
_NOTFOUND = object()
_NOHINT = object()


class Error(Exception):
    pass


class LxmlError(Error):
    def __init__(self, message="", error_log=None):
        Error.__init__(self, message)
        self.msg = message
        self.error_log = error_log


class LxmlSyntaxError(LxmlError, SyntaxError):
    pass


class ParseError(LxmlSyntaxError):
    def __init__(self, message="", code=0, line=0, column=0, filename=None):
        LxmlSyntaxError.__init__(self, message)
        self.code = code
        self.position = (line, column)
        self.lineno = line
        self.offset = column
        self.filename = filename


class XMLSyntaxError(ParseError):
    pass


class ParserError(LxmlError):
    pass


class DocumentInvalid(LxmlError):
    pass


XPathError = _xpath.XPathError
XPathEvalError = _xpath.XPathEvalError
XPathSyntaxError = _xpath.XPathSyntaxError


def _split(tag):
    if tag[:1] == "{":
        j = tag.find("}")
        if j < 0:
            raise ValueError("Invalid tag name %r" % tag)
        return tag[1:j] or None, tag[j + 1:]
    return None, tag


def _tagstr(tag):
    if isinstance(tag, QName):
        return tag.text
    if isinstance(tag, bytes):
        tag = tag.decode("utf-8")
    if not isinstance(tag, str):
        raise TypeError("Invalid tag name %r" % (tag,))
    if tag[:2] == "{}":
        tag = tag[2:]
    _, local = _split(tag)
    if not local or ":" in local or " " in local:
        raise ValueError("Invalid tag name %r" % tag)
    return tag


def _valstr(v):
    if isinstance(v, QName):
        return v.text
    if isinstance(v, bytes):
        return v.decode("utf-8")
    if not isinstance(v, str):
        raise TypeError("Argument must be bytes or unicode, got '%s'" % type(v).__name__)
    return v


# ---------------------------------------------------------------- namespaces

def _resolve(el, prefix):
    n = el
    while n is not None:
        ns = n._xns
        if prefix in ns:
            return ns[prefix]
        n = n._xparent
    if prefix == "xml":
        return XML_NS
    return None


def _prefix_for(el, uri, is_attr):
    """The prefix under which `uri` is in scope at `el`, or _NOTFOUND."""
    if uri == XML_NS:
        return "xml"
    if not is_attr:
        hint = el._xpfx
        if hint is not _NOHINT and _resolve(el, hint) == uri:
            return hint
    seen = set()
    n = el
    while n is not None:
        ns = n._xns
        for k in ns:
            if k not in seen and ns[k] == uri and (k is not None or not is_attr):
                if is_attr or _resolve(el, k) == uri:
                    return k
        for k in ns:
            seen.add(k)
        n = n._xparent
    return _NOTFOUND


def _free_prefix(el):
    # lxml numbers generated prefixes with one counter per document
    cnt = el._xcnt
    while True:
        p = "ns%d" % cnt[0]
        cnt[0] += 1
        if _resolve(el, p) is None:
            return p


def _ensure_ns(el, uri, is_attr):
    if uri is None or uri == XML_NS:
        return
    if _prefix_for(el, uri, is_attr) is _NOTFOUND:
        el._xns[_free_prefix(el)] = uri


def _walk(el):
    yield el
    for c in el._xkids:
        if c._xkind == "e":
            for d in _walk(c):
                yield d


def _take_out(el):
    """Detach `el` from its parent, declaring on it what it used from above."""
    parent = el._xparent
    if parent is None:
        return
    if el._xkind == "e":
        declared = set()
        for n in _walk(el):
            for v in n._xns.values():
                declared.add(v)
        add = []
        added = set()
        for n in _walk(el):
            uri = _split(n._xtag)[0]
            if uri is not None and uri != XML_NS and uri not in declared and uri not in added:
                add.append((_prefix_for(n, uri, False), uri))
                added.add(uri)
            for k in n._xattrib:
                uri = _split(k)[0]
                if uri is not None and uri != XML_NS and uri not in declared and uri not in added:
                    add.append((_prefix_for(n, uri, True), uri))
                    added.add(uri)
    else:
        add = []
    parent._xkids.remove(el)
    el._xparent = None
    for p, uri in add:
        if p is None or p is _NOTFOUND:
            p = _free_prefix(el)
        el._xns[p] = uri


def _put_in(parent, el, index):
    if not isinstance(el, _Element):
        raise TypeError("Argument must be an element")
    if parent._xkind != "e":
        raise TypeError("Comments and processing instructions have no children")
    n = parent
    while n is not None:
        if n is el:
            raise ValueError("cannot append parent to itself")
        n = n._xparent
    if el._xparent is not None:
        if el._xparent is parent:
            old = parent._xkids.index(el)
            parent._xkids.pop(old)
            if index is not None and index > old:
                index -= 1
            el._xparent = None
        else:
            _take_out(el)
    if index is None:
        parent._xkids.append(el)
    else:
        parent._xkids.insert(index, el)
    el._xparent = parent
    for k in list(el._xns.keys()):
        v = el._xns[k]
        if _prefix_for(parent, v, False) is not _NOTFOUND:
            del el._xns[k]
    if el._xdoc is not None:
        el._xdoc = None


# ---------------------------------------------------------------- creation

def _class_for(lookup, kind, tag):
    if lookup is not None:
        cls = lookup._lookup_class(kind, tag)
        if cls is not None:
            return cls
    if kind == "c":
        return _Comment
    if kind == "p":
        return _ProcessingInstruction
    return _Element


def _blank(cls, kind, lookup):
    el = cls.__new__(cls)
    el._xkind = kind
    el._xtag = None
    el._xattrib = {}
    el._xns = {}
    el._xkids = []
    el._xparent = None
    el._xtext = None
    el._xtail = None
    el._xpfx = _NOHINT
    el._xlookup = lookup
    el._xdoc = None
    el._xtarget = None
    el._xcnt = [0]
    return el


def _call_init(el):
    init = getattr(el, "_init", None)
    if init is not None:
        init()


def _new(lookup, tag, attrib, nsmap, extra, parent=None):
    tag = _tagstr(tag)
    cls = _class_for(lookup, "e", tag)
    el = _blank(cls, "e", lookup)
    el._xtag = tag
    if nsmap:
        for k in nsmap:
            v = nsmap[k]
            if k == "":
                k = None
            if v is not None:
                el._xns[k] = _valstr(v)
    if parent is not None:
        el._xparent = parent
        el._xcnt = parent._xcnt
        parent._xkids.append(el)
    _ensure_ns(el, _split(tag)[0], False)
    if attrib:
        for k in attrib:
            el._set_attr(k, attrib[k])
    if extra:
        for k in extra:
            el._set_attr(k, extra[k])
    _call_init(el)
    return el


def Element(_tag, attrib=None, nsmap=None, **_extra):
    return _new(_default_lookup(), _tag, attrib, nsmap, _extra)


def SubElement(_parent, _tag, attrib=None, nsmap=None, **_extra):
    if _parent._xkind != "e":
        raise TypeError("Comments and processing instructions have no children")
    return _new(_parent._xlookup, _tag, attrib, nsmap, _extra, _parent)


def Comment(text=None):
    el = _blank(_class_for(_default_lookup(), "c", None), "c", None)
    if text is not None:
        text = _valstr(text)
        if "--" in text or text.endswith("-"):
            raise ValueError("Comment may not contain '--' or end with '-'")
    el._xtext = text
    _call_init(el)
    return el


def ProcessingInstruction(target, text=None):
    el = _blank(_class_for(_default_lookup(), "p", None), "p", None)
    el._xtarget = _valstr(target)
    el._xtext = None if text is None else _valstr(text)
    _call_init(el)
    return el


PI = ProcessingInstruction


def Entity(name):
    raise NotImplementedError("Entity nodes are not supported")


def iselement(element):
    return isinstance(element, _Element)


# ---------------------------------------------------------------- tag matching

def _matcher(tags):
    flat = []
    for t in tags:
        if t is None:
            continue
        if isinstance(t, (list, tuple)):
            flat.extend(t)
        else:
            flat.append(t)
    if not flat:
        return None
    tests = []
    for t in flat:
        if t is Comment:
            tests.append(("c",))
        elif t is ProcessingInstruction:
            tests.append(("p",))
        elif t is Element:
            tests.append(("e",))
        else:
            t = t.text if isinstance(t, QName) else t
            if t == "*":
                tests.append(("e",))
            elif t[:1] == "{":
                j = t.index("}")
                uri = t[1:j]
                tests.append(("n", None if uri == "" else uri, t[j + 1:], uri == "*"))
            else:
                tests.append(("n", None, t, False))

    def match(el):
        k = el._xkind
        for test in tests:
            if test[0] == "c" or test[0] == "p" or test[0] == "e":
                if k == test[0]:
                    return True
            elif k == "e":
                uri, local = _split(el._xtag)
                if (test[2] == "*" or test[2] == local) and (test[3] or uri == test[1]):
                    return True
        return False
    return match


# ---------------------------------------------------------------- elements

class _Element(object):
    """An element (and, as subclasses, a comment or processing instruction)."""

    def __init__(self, *args, **kwargs):
        raise TypeError("cannot create '_Element' instances directly; use Element()")

    # -- naming
    @property
    def tag(self):
        return self._xtag

    @tag.setter
    def tag(self, value):
        value = _tagstr(value)
        self._xtag = value
        self._xpfx = _NOHINT
        _ensure_ns(self, _split(value)[0], False)

    @property
    def prefix(self):
        uri = _split(self._xtag)[0]
        if uri is None:
            return None
        p = _prefix_for(self, uri, False)
        return None if p is _NOTFOUND else p

    @property
    def nsmap(self):
        out = {}
        chain = []
        n = self
        while n is not None:
            chain.append(n)
            n = n._xparent
        chain.reverse()
        for n in chain:
            for k in n._xns:
                out[k] = n._xns[k]
        return out

    @property
    def text(self):
        return self._xtext

    @text.setter
    def text(self, value):
        self._xtext = None if value is None else _valstr(value)

    @property
    def tail(self):
        return self._xtail

    @tail.setter
    def tail(self, value):
        self._xtail = None if value is None else _valstr(value)

    @property
    def attrib(self):
        return self._xattrib

    @property
    def sourceline(self):
        return None

    @property
    def base(self):
        return None

    # -- attributes
    def _set_attr(self, key, value):
        key = _tagstr(key)
        value = _valstr(value)
        _ensure_ns(self, _split(key)[0], True)
        self._xattrib[key] = value

    def get(self, key, default=None):
        if isinstance(key, QName):
            key = key.text
        return self._xattrib.get(key, default)

    def set(self, key, value):
        if self._xkind != "e":
            raise TypeError("Comments and processing instructions have no attributes")
        self._set_attr(key, value)

    def keys(self):
        return list(self._xattrib.keys())

    def values(self):
        return list(self._xattrib.values())

    def items(self):
        return list(self._xattrib.items())

    # -- children
    def __len__(self):
        return len(self._xkids)

    def __bool__(self):
        return len(self._xkids) > 0

    def __iter__(self):
        return iter(list(self._xkids))

    def __reversed__(self):
        return iter(list(reversed(self._xkids)))

    def __contains__(self, el):
        for c in self._xkids:
            if c is el:
                return True
        return False

    def __getitem__(self, index):
        if isinstance(index, slice):
            return self._xkids[index]
        return self._xkids[index]

    def __setitem__(self, index, value):
        if isinstance(index, slice):
            olds = self._xkids[index]
            start = index.start or 0
            if start < 0:
                start += len(self._xkids)
            for o in olds:
                _take_out(o)
            for v in list(value):
                _put_in(self, v, start)
                start += 1
            return
        old = self._xkids[index]
        if old is value:
            return
        pos = self._xkids.index(old)
        _take_out(old)
        _put_in(self, value, pos)

    def __delitem__(self, index):
        if isinstance(index, slice):
            for o in self._xkids[index]:
                _take_out(o)
        else:
            _take_out(self._xkids[index])

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        el = _copy_tree(self, None)
        if self._xparent is not None and self._xkind == "e":
            inherited = self.nsmap
            for k in inherited:
                if k not in el._xns:
                    el._xns[k] = inherited[k]
        return el

    def append(self, element):
        _put_in(self, element, None)

    def extend(self, elements):
        for e in list(elements):
            _put_in(self, e, None)

    def insert(self, index, element):
        n = len(self._xkids)
        if index < 0:
            index = max(0, n + index)
        elif index > n:
            index = n
        _put_in(self, element, index)

    def remove(self, element):
        if element._xparent is not self:
            raise ValueError("Element is not a child of this node.")
        _take_out(element)

    def replace(self, old_element, new_element):
        if old_element._xparent is not self:
            raise ValueError("Element is not a child of this node.")
        pos = self._xkids.index(old_element)
        _take_out(old_element)
        _put_in(self, new_element, pos)

    def clear(self, keep_tail=False):
        self._xattrib.clear()
        self._xtext = None
        if not keep_tail:
            self._xtail = None
        for c in list(self._xkids):
            _take_out(c)

    def index(self, child, start=None, stop=None):
        kids = self._xkids
        lo = 0 if start is None else start
        hi = len(kids) if stop is None else stop
        for i in range(lo, min(hi, len(kids))):
            if kids[i] is child:
                return i
        raise ValueError("Element is not a child of this node.")

    def getchildren(self):
        return list(self._xkids)

    def getparent(self):
        return self._xparent

    def getnext(self):
        p = self._xparent
        if p is None:
            return None
        i = p._xkids.index(self)
        return p._xkids[i + 1] if i + 1 < len(p._xkids) else None

    def getprevious(self):
        p = self._xparent
        if p is None:
            return None
        i = p._xkids.index(self)
        return p._xkids[i - 1] if i > 0 else None

    def addnext(self, element):
        p = self._xparent
        if p is None:
            raise TypeError("Only processing instructions and comments can be siblings of the root element")
        if element is self:
            return
        _take_out(element) if element._xparent is not None else None
        _put_in(p, element, p._xkids.index(self) + 1)

    def addprevious(self, element):
        p = self._xparent
        if p is None:
            raise TypeError("Only processing instructions and comments can be siblings of the root element")
        if element is self:
            return
        _take_out(element) if element._xparent is not None else None
        _put_in(p, element, p._xkids.index(self))

    def makeelement(self, _tag, attrib=None, nsmap=None, **_extra):
        return _new(self._xlookup, _tag, attrib, nsmap, _extra)

    def getroottree(self):
        n = self
        while n._xparent is not None:
            n = n._xparent
        return _ElementTree(n)

    # -- iteration
    def iterchildren(self, tag=None, *tags, **kw):
        m = _matcher((tag,) + tags)
        kids = list(self._xkids)
        if kw.get("reversed"):
            kids.reverse()
        return iter([c for c in kids if m is None or m(c)])

    def itersiblings(self, tag=None, *tags, **kw):
        m = _matcher((tag,) + tags)
        p = self._xparent
        if p is None:
            return iter([])
        i = p._xkids.index(self)
        sibs = list(reversed(p._xkids[:i])) if kw.get("preceding") else p._xkids[i + 1:]
        return iter([c for c in sibs if m is None or m(c)])

    def iterancestors(self, tag=None, *tags):
        m = _matcher((tag,) + tags)
        out = []
        n = self._xparent
        while n is not None:
            if m is None or m(n):
                out.append(n)
            n = n._xparent
        return iter(out)

    def iter(self, tag=None, *tags):
        m = _matcher((tag,) + tags)
        out = []
        _collect(self, m, out)
        return iter(out)

    def iterdescendants(self, tag=None, *tags):
        m = _matcher((tag,) + tags)
        out = []
        for c in self._xkids:
            _collect(c, m, out)
        return iter(out)

    def getiterator(self, tag=None, *tags):
        return self.iter(tag, *tags)

    def itertext(self, tag=None, *tags, **kw):
        with_tail = kw.get("with_tail", True)
        m = _matcher((tag,) + tags)
        out = []
        _itertext(self, m, with_tail, out, True)
        return iter(out)

    # -- searching
    def xpath(self, _path, namespaces=None, extensions=None, smart_strings=True, **_variables):
        if namespaces and None in namespaces:
            raise TypeError("empty namespace prefix is not supported in XPath")
        return _xpath_result(_xpath.evaluate(_xpath.compile_expr(_path), self, _xpath.Env(namespaces)),
                             smart_strings)

    def find(self, path, namespaces=None):
        for el in self.iterfind(path, namespaces):
            return el
        return None

    def findall(self, path, namespaces=None):
        return list(self.iterfind(path, namespaces))

    def iterfind(self, path, namespaces=None):
        if isinstance(path, QName):
            path = path.text
        if path[:1] == "/":
            raise SyntaxError("cannot use absolute path on element")
        ns = dict(namespaces) if namespaces else {}
        default = None
        if None in ns:
            default = ns.pop(None)
        if "" in ns:
            default = ns.pop("")
        res = _xpath.evaluate(_xpath.compile_expr(path), self, _xpath.Env(ns, default))
        if not isinstance(res, list):
            raise SyntaxError("invalid path")
        return iter([r for r in res if isinstance(r, _Element)])

    def findtext(self, path, default=None, namespaces=None):
        el = self.find(path, namespaces)
        if el is None:
            return default
        return el._xtext or ""

    def __repr__(self):
        return "<Element %s at 0x%x>" % (self._xtag, id(self))


class ElementBase(_Element):
    """Base class for custom element classes chosen by a class lookup."""

    def __init__(self, *children, **kw):
        attrib = kw.pop("attrib", None)
        nsmap = kw.pop("nsmap", None)
        tag = getattr(type(self), "TAG", None) or type(self).__name__
        tag = _tagstr(tag)
        self._xkind = "e"
        self._xtag = tag
        self._xattrib = {}
        self._xns = {}
        self._xkids = []
        self._xparent = None
        self._xtext = None
        self._xtail = None
        self._xpfx = _NOHINT
        self._xlookup = None
        self._xdoc = None
        self._xtarget = None
        self._xcnt = [0]
        if nsmap:
            for k in nsmap:
                self._xns[k] = nsmap[k]
        _ensure_ns(self, _split(tag)[0], False)
        if attrib:
            for k in attrib:
                self._set_attr(k, attrib[k])
        for k in kw:
            self._set_attr(k, kw[k])
        for c in children:
            if isinstance(c, dict):
                for k in c:
                    self._set_attr(k, c[k])
            elif isinstance(c, str):
                if self._xkids:
                    last = self._xkids[-1]
                    last._xtail = (last._xtail or "") + c
                else:
                    self._xtext = (self._xtext or "") + c
            else:
                self.append(c)
        _call_init(self)


class _Comment(_Element):
    @property
    def tag(self):
        return Comment

    def __repr__(self):
        return "<!--%s-->" % (self._xtext or "")


class _ProcessingInstruction(_Element):
    @property
    def tag(self):
        return ProcessingInstruction

    @property
    def target(self):
        return self._xtarget

    @target.setter
    def target(self, value):
        self._xtarget = _valstr(value)

    def __repr__(self):
        return "<?%s %s?>" % (self._xtarget, self._xtext or "")


class CommentBase(_Comment):
    pass


class PIBase(_ProcessingInstruction):
    pass


def _collect(el, m, out):
    if m is None or m(el):
        out.append(el)
    for c in el._xkids:
        _collect(c, m, out)


def _itertext(el, m, with_tail, out, top):
    if el._xkind == "e":
        if (m is None or m(el)) and el._xtext:
            out.append(el._xtext)
        for c in el._xkids:
            _itertext(c, m, with_tail, out, False)
    if not top and with_tail and el._xtail and (m is None or (el._xparent is not None and m(el._xparent))):
        out.append(el._xtail)


def _copy_tree(el, parent):
    c = _blank(type(el), el._xkind, el._xlookup)
    c._xtag = el._xtag
    c._xattrib = dict(el._xattrib)
    c._xns = dict(el._xns)
    c._xtext = el._xtext
    c._xtail = el._xtail
    c._xpfx = el._xpfx
    c._xtarget = el._xtarget
    if parent is not None:
        c._xparent = parent
        parent._xkids.append(c)
    for k in el._xkids:
        _copy_tree(k, c)
    _call_init(c)
    return c


class _ElementUnicodeResult(str):
    """An XPath string result that knows where it came from."""

    def getparent(self):
        return self._xowner


def _smart(value, owner, attrname, is_text, is_tail):
    # Results are plain str: the runtime does not yet take a str subclass
    # everywhere a str is accepted (int(), float(), ...), and the consumers
    # here never ask a result for its parent.
    return value


def _smart_result(value, owner, attrname, is_text, is_tail):
    s = _ElementUnicodeResult(value)
    s._xowner = owner
    s.attrname = attrname
    s.is_attribute = attrname is not None
    s.is_text = is_text
    s.is_tail = is_tail
    return s


def _xpath_result(res, smart):
    if not isinstance(res, list):
        return res
    out = []
    for n in res:
        k = n._xkind
        if k == "a":
            out.append(_smart(n.value, n.owner, n.name, False, False) if smart else n.value)
        elif k == "t":
            owner = n.holder._xparent if n.is_tail else n.holder
            out.append(_smart(n.value, owner, None, True, n.is_tail) if smart else n.value)
        elif k == "d":
            out.append(_ElementTree(n.root))
        else:
            out.append(n)
    return out


class XPath(object):
    def __init__(self, path, namespaces=None, extensions=None, regexp=True, smart_strings=True):
        self.path = path
        self._ast = _xpath.compile_expr(path)
        self._ns = namespaces
        self._smart = smart_strings

    def __call__(self, _etree_or_element, **_variables):
        node = _etree_or_element
        if isinstance(node, _ElementTree):
            node = _xpath._Doc(node.getroot())
        return _xpath_result(_xpath.evaluate(self._ast, node, _xpath.Env(self._ns)), self._smart)


class QName(object):
    def __init__(self, text_or_uri_or_element, tag=None):
        v = text_or_uri_or_element
        if isinstance(v, _Element):
            v = v._xtag
        elif isinstance(v, QName):
            v = v.text
        elif isinstance(v, bytes):
            v = v.decode("utf-8")
        if tag is not None:
            if isinstance(tag, bytes):
                tag = tag.decode("utf-8")
            v = "{%s}%s" % (v, tag) if v else tag
        self.text = v
        self.namespace, self.localname = _split(v)

    def __str__(self):
        return self.text

    def __repr__(self):
        return "QName(%r)" % self.text

    def __hash__(self):
        return hash(self.text)

    def __eq__(self, other):
        if isinstance(other, QName):
            return self.text == other.text
        return self.text == other

    def __ne__(self, other):
        return not self.__eq__(other)


# ---------------------------------------------------------------- class lookup

class ElementClassLookup(object):
    def _lookup_class(self, kind, tag):
        return None


class ElementDefaultClassLookup(ElementClassLookup):
    def __init__(self, element=None, comment=None, pi=None, entity=None):
        self.element_class = element
        self.comment_class = comment
        self.pi_class = pi

    def _lookup_class(self, kind, tag):
        if kind == "c":
            return self.comment_class
        if kind == "p":
            return self.pi_class
        return self.element_class


class FallbackElementClassLookup(ElementClassLookup):
    def __init__(self, fallback=None):
        self.fallback = fallback

    def set_fallback(self, lookup):
        self.fallback = lookup

    def _fall(self, kind, tag):
        if self.fallback is not None:
            return self.fallback._lookup_class(kind, tag)
        return None


class _NamespaceRegistry(dict):
    def __init__(self, uri):
        dict.__init__(self)
        self._uri = uri

    def __call__(self, obj):
        self[getattr(obj, "__name__", None)] = obj
        return obj


class ElementNamespaceClassLookup(FallbackElementClassLookup):
    def __init__(self, fallback=None):
        FallbackElementClassLookup.__init__(self, fallback)
        self._namespaces = {}

    def get_namespace(self, ns_uri):
        if isinstance(ns_uri, bytes):
            ns_uri = ns_uri.decode("utf-8")
        reg = self._namespaces.get(ns_uri)
        if reg is None:
            reg = _NamespaceRegistry(ns_uri)
            self._namespaces[ns_uri] = reg
        return reg

    def _lookup_class(self, kind, tag):
        if kind == "e":
            uri, local = _split(tag)
            reg = self._namespaces.get(uri)
            if reg is not None:
                cls = reg.get(local)
                if cls is None:
                    cls = reg.get(None)
                if cls is not None:
                    return cls
        return self._fall(kind, tag)


class CustomElementClassLookup(FallbackElementClassLookup):
    def lookup(self, type, doc, namespace, name):
        return None

    def _lookup_class(self, kind, tag):
        t = {"e": "element", "c": "comment", "p": "PI"}[kind]
        uri, local = (None, None) if tag is None else _split(tag)
        cls = self.lookup(t, None, uri, local)
        if cls is not None:
            return cls
        return self._fall(kind, tag)


_GLOBAL_LOOKUP = [None]


def set_element_class_lookup(lookup=None):
    _GLOBAL_LOOKUP[0] = lookup


def _default_lookup():
    return _GLOBAL_LOOKUP[0]


# ---------------------------------------------------------------- parsing

class _TreeSink(object):
    """Builds elements from parse events."""

    def __init__(self, lookup, events=None):
        self.lookup = lookup
        self.stack = []
        self.root = None
        self.pre = []
        self.post = []
        self.events = events
        self.cnt = [0]

    def start(self, tag, attrib, nsmap, hint):
        cls = _class_for(self.lookup, "e", tag)
        el = _blank(cls, "e", self.lookup)
        el._xtag = tag
        el._xpfx = hint
        el._xcnt = self.cnt
        for k in nsmap:
            el._xns[k] = nsmap[k]
        el._xattrib = attrib
        if self.stack:
            parent = self.stack[-1]
            el._xparent = parent
            parent._xkids.append(el)
        elif self.root is None:
            self.root = el
        self.stack.append(el)
        _call_init(el)
        if self.events is not None:
            self.events.append(("start", el))

    def end(self, tag):
        el = self.stack.pop()
        if self.events is not None:
            self.events.append(("end", el))
        return el

    def data(self, text):
        top = self.stack[-1]
        if top._xkids:
            last = top._xkids[-1]
            last._xtail = (last._xtail or "") + text
        else:
            top._xtext = (top._xtext or "") + text

    def _misc(self, el):
        if self.stack:
            el._xparent = self.stack[-1]
            self.stack[-1]._xkids.append(el)
        elif self.root is None:
            self.pre.append(el)
        else:
            self.post.append(el)
        _call_init(el)
        if self.events is not None:
            self.events.append(("comment" if el._xkind == "c" else "pi", el))

    def comment(self, text):
        el = _blank(_class_for(self.lookup, "c", None), "c", self.lookup)
        el._xtext = text
        self._misc(el)

    def pi(self, target, data):
        el = _blank(_class_for(self.lookup, "p", None), "p", self.lookup)
        el._xtarget = target
        el._xtext = data or None
        self._misc(el)

    def doctype(self, name, pubid, system):
        self.doctype_info = (name, pubid, system)

    def close(self):
        if self.root is None:
            raise XMLSyntaxError("Document is empty")
        if self.pre or self.post:
            self.root._xdoc = (self.pre, self.post)
        return self.root


class _TargetSink(object):
    """Hands parse events to a user's parser target."""

    def __init__(self, target):
        self.target = target
        self._start = getattr(target, "start", None)
        self._end = getattr(target, "end", None)
        self._data = getattr(target, "data", None)
        self._comment = getattr(target, "comment", None)
        self._pi = getattr(target, "pi", None)
        self._doctype = getattr(target, "doctype", None)
        self._close = getattr(target, "close", None)

    def start(self, tag, attrib, nsmap, hint):
        if self._start is not None:
            self._start(tag, attrib, nsmap)

    def end(self, tag):
        if self._end is not None:
            self._end(tag)

    def data(self, text):
        if self._data is not None:
            self._data(text)

    def comment(self, text):
        if self._comment is not None:
            self._comment(text)

    def pi(self, target, data):
        if self._pi is not None:
            self._pi(target, data)

    def doctype(self, name, pubid, system):
        if self._doctype is not None:
            self._doctype(name, pubid, system)

    def close(self):
        if self._close is not None:
            return self._close()
        return None


def _qname(raw):
    parts = raw.split(" ")
    if len(parts) == 1:
        return raw, _NOHINT
    if len(parts) == 2:
        return "{%s}%s" % (parts[0], parts[1]), None
    return "{%s}%s" % (parts[0], parts[1]), parts[2]


class _XMLDriver(object):
    """Runs the pure-Python expat over a document and feeds a sink."""

    def __init__(self, opts, sink):
        self.opts = opts
        self.sink = sink
        self.text = []
        self.depth = 0
        self.state = []
        self.pending = []
        self.open_tags = []

    def flush(self, next_is_end):
        if not self.text:
            return
        text = "".join(self.text)
        self.text = []
        if self.depth == 0 or not text:
            return
        st = self.state[-1]
        if self.opts.remove_blank_text and text.strip(" \t\r\n") == "":
            if not st[0]:
                if not next_is_end:
                    return
            elif not st[1]:
                return
        if not st[0]:
            st[1] = True
        self.sink.data(text)

    def on_ns(self, prefix, uri):
        if uri:
            self.pending.append((prefix or None, uri))

    def on_start(self, name, attrs):
        self.flush(False)
        if self.state:
            self.state[-1][0] = True
        tag, hint = _qname(name)
        attrib = {}
        for i in range(0, len(attrs), 2):
            attrib[_qname(attrs[i])[0]] = attrs[i + 1]
        nsmap = {}
        for p, u in self.pending:
            nsmap[p] = u
        self.pending = []
        self.sink.start(tag, attrib, nsmap, hint)
        self.open_tags.append(tag)
        self.depth += 1
        self.state.append([False, False])

    def on_end(self, name):
        self.flush(True)
        self.depth -= 1
        self.state.pop()
        self.sink.end(self.open_tags.pop())

    def on_data(self, data):
        self.text.append(data)

    def on_comment(self, data):
        self.flush(False)
        if self.opts.remove_comments:
            return
        if self.state:
            self.state[-1][0] = True
        self.sink.comment(data)

    def on_pi(self, target, data):
        self.flush(False)
        if self.opts.remove_pis:
            return
        if self.state:
            self.state[-1][0] = True
        self.sink.pi(target, data)

    def on_doctype(self, name, sysid, pubid, has_internal):
        self.sink.doctype(name, pubid, sysid)

    def run(self, data):
        enc = self.opts.encoding if isinstance(data, bytes) else None
        p = _expat.ParserCreate(enc, " ")
        p.namespace_prefixes = True
        p.ordered_attributes = True
        p.StartNamespaceDeclHandler = self.on_ns
        p.StartElementHandler = self.on_start
        p.EndElementHandler = self.on_end
        p.CharacterDataHandler = self.on_data
        p.CommentHandler = self.on_comment
        p.ProcessingInstructionHandler = self.on_pi
        p.StartDoctypeDeclHandler = self.on_doctype
        try:
            p.Parse(data, True)
        except _expat.ExpatError as e:
            if not self.opts.recover:
                raise XMLSyntaxError(str(e), getattr(e, "code", 0), getattr(e, "lineno", 0),
                                     getattr(e, "offset", 0))
            self.flush(True)
            while self.open_tags:
                self.depth -= 1
                self.state.pop()
                self.sink.end(self.open_tags.pop())
        return self.sink.close()


class _BaseParser(object):
    _html = False

    def __init__(self, encoding=None, remove_blank_text=False, remove_comments=False, remove_pis=False,
                 strip_cdata=True, recover=False, target=None, **kw):
        self.encoding = encoding
        self.remove_blank_text = remove_blank_text
        self.remove_comments = remove_comments
        self.remove_pis = remove_pis
        self.strip_cdata = strip_cdata
        self.recover = recover
        self.target = target
        self.resolve_entities = kw.get("resolve_entities", True)
        self._lookup = None
        self._feed = []
        self.error_log = []
        self.version = "libxml2 2.12.0"

    def set_element_class_lookup(self, lookup=None):
        self._lookup = lookup

    def makeelement(self, _tag, attrib=None, nsmap=None, **_extra):
        return _new(self._lookup if self._lookup is not None else _default_lookup(), _tag, attrib, nsmap,
                    _extra)

    def copy(self):
        p = self.__class__.__new__(self.__class__)
        p.__dict__.update(self.__dict__)
        p._feed = []
        return p

    def _sink(self, events=None):
        if self.target is not None:
            return _TargetSink(self.target)
        return _TreeSink(self._lookup if self._lookup is not None else _default_lookup(), events)

    def _parse(self, data, events=None):
        sink = self._sink(events)
        if self._html:
            return _HTMLDriver(self, sink).run(data)
        return _XMLDriver(self, sink).run(data)

    def feed(self, data):
        self._feed.append(data)

    def close(self):
        chunks = self._feed
        self._feed = []
        if not chunks:
            data = ""
        elif isinstance(chunks[0], bytes):
            data = b"".join(c if isinstance(c, bytes) else c.encode("utf-8") for c in chunks)
        else:
            data = "".join(c if isinstance(c, str) else c.decode(self.encoding or "utf-8") for c in chunks)
        return self._parse(data)


class XMLParser(_BaseParser):
    pass


class ETCompatXMLParser(XMLParser):
    pass


class XMLPullParser(XMLParser):
    pass


_HTML_VOID = ("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source",
              "track", "wbr", "basefont", "frame", "isindex", "keygen")


class _HTMLDriver(_htmlparser.HTMLParser):
    """A plain HTML tokenizer feeding a sink; it does not repair markup as libxml2 does."""

    def __init__(self, opts, sink):
        _htmlparser.HTMLParser.__init__(self, convert_charrefs=True)
        self.opts = opts
        self.sink = sink
        self.open_tags = []

    def handle_starttag(self, tag, attrs):
        if self.open_tags == [] and getattr(self.sink, "root", None) is not None:
            return
        attrib = {}
        for k, v in attrs:
            attrib[k] = "" if v is None else v
        self.sink.start(tag, attrib, {}, _NOHINT)
        if tag in _HTML_VOID:
            self.sink.end(tag)
        else:
            self.open_tags.append(tag)

    def handle_startendtag(self, tag, attrs):
        self.handle_starttag(tag, attrs)
        if tag not in _HTML_VOID and self.open_tags and self.open_tags[-1] == tag:
            self.open_tags.pop()
            self.sink.end(tag)

    def handle_endtag(self, tag):
        if tag not in self.open_tags:
            return
        while self.open_tags:
            t = self.open_tags.pop()
            self.sink.end(t)
            if t == tag:
                break

    def handle_data(self, data):
        if self.open_tags:
            self.sink.data(data)

    def handle_comment(self, data):
        if not self.opts.remove_comments:
            self.sink.comment(data)

    def handle_decl(self, decl):
        parts = decl.split()
        if parts and parts[0].upper() == "DOCTYPE":
            self.sink.doctype(parts[1] if len(parts) > 1 else "html", None, None)

    def run(self, data):
        if isinstance(data, bytes):
            data = data.decode(self.opts.encoding or "utf-8", "replace")
        self.feed(data)
        _htmlparser.HTMLParser.close(self)
        while self.open_tags:
            self.sink.end(self.open_tags.pop())
        return self.sink.close()


class HTMLParser(_BaseParser):
    _html = True

    def __init__(self, encoding=None, remove_blank_text=False, remove_comments=False, remove_pis=False,
                 strip_cdata=True, recover=True, target=None, **kw):
        _BaseParser.__init__(self, encoding, remove_blank_text, remove_comments, remove_pis, strip_cdata,
                             recover, target, **kw)


class HTMLPullParser(HTMLParser):
    pass


_DEFAULT_PARSER = [None]


def set_default_parser(parser=None):
    _DEFAULT_PARSER[0] = parser


def get_default_parser():
    p = _DEFAULT_PARSER[0]
    if p is None:
        p = XMLParser()
        _DEFAULT_PARSER[0] = p
    return p


def fromstring(text, parser=None, base_url=None):
    if parser is None:
        parser = get_default_parser()
    if isinstance(text, str) and text.lstrip().startswith("<?xml") and "encoding" in text.split("?>", 1)[0] \
            and not parser._html:
        raise ValueError("Unicode strings with encoding declaration are not supported. "
                         "Please use bytes input or XML fragments without declaration.")
    return parser._parse(text)


XML = fromstring


def HTML(text, parser=None, base_url=None):
    return fromstring(text, parser if parser is not None else HTMLParser())


def _read_source(source):
    if hasattr(source, "read"):
        return source.read()
    if hasattr(source, "__fspath__"):
        source = source.__fspath__()
    with open(source, "rb") as f:
        return f.read()


def parse(source, parser=None, base_url=None):
    if parser is None:
        parser = get_default_parser()
    root = parser._parse(_read_source(source))
    if parser.target is not None:
        return root
    return _ElementTree(root)


def iterparse(source, events=("end",), tag=None, html=False, recover=None, encoding=None,
              remove_blank_text=False, remove_comments=False, remove_pis=False, strip_cdata=True, **kw):
    parser = (HTMLParser if html else XMLParser)(encoding=encoding, remove_blank_text=remove_blank_text,
                                                 remove_comments=remove_comments, remove_pis=remove_pis,
                                                 recover=bool(recover))
    record = []
    parser._parse(_read_source(source), record)
    m = _matcher((tag,))
    out = []
    for ev, el in record:
        if ev in events and (m is None or m(el)):
            out.append((ev, el))
    return iter(out)


# ---------------------------------------------------------------- trees

class _ElementTree(object):
    def __init__(self, root=None):
        self._root = root

    def getroot(self):
        return self._root

    def _setroot(self, root):
        self._root = root

    @property
    def docinfo(self):
        return _DocInfo(self)

    def xpath(self, _path, namespaces=None, extensions=None, smart_strings=True, **_variables):
        node = _xpath._Doc(self._root)
        return _xpath_result(_xpath.evaluate(_xpath.compile_expr(_path), node, _xpath.Env(namespaces)),
                             smart_strings)

    def _fix(self, path):
        if path[:1] == "/":
            path = "." + path
        return path

    def find(self, path, namespaces=None):
        return self._root.find(self._fix(path), namespaces)

    def findall(self, path, namespaces=None):
        return self._root.findall(self._fix(path), namespaces)

    def iterfind(self, path, namespaces=None):
        return self._root.iterfind(self._fix(path), namespaces)

    def findtext(self, path, default=None, namespaces=None):
        return self._root.findtext(self._fix(path), default, namespaces)

    def iter(self, tag=None, *tags):
        return self._root.iter(tag, *tags)

    def getiterator(self, tag=None, *tags):
        return self._root.iter(tag, *tags)

    def getpath(self, element):
        parts = []
        n = element
        while n is not None:
            p = n._xparent
            name = n.prefix + ":" + _split(n._xtag)[1] if n.prefix else _split(n._xtag)[1]
            if p is not None:
                same = [c for c in p._xkids if c._xkind == "e" and c._xtag == n._xtag]
                if len(same) > 1:
                    name += "[%d]" % (same.index(n) + 1)
            parts.append(name)
            n = p
        parts.reverse()
        return "/" + "/".join(parts)

    def write(self, file, encoding=None, method="xml", pretty_print=False, xml_declaration=None,
              with_tail=True, standalone=None, doctype=None, compression=0, **kw):
        data = tostring(self, encoding=encoding, method=method, pretty_print=pretty_print,
                        xml_declaration=xml_declaration, with_tail=with_tail, standalone=standalone)
        if isinstance(data, str):
            data = data.encode("utf-8")
        if hasattr(file, "write"):
            file.write(data)
        else:
            if hasattr(file, "__fspath__"):
                file = file.__fspath__()
            with open(file, "wb") as f:
                f.write(data)


class _DocInfo(object):
    def __init__(self, tree):
        self._tree = tree
        self.xml_version = "1.0"
        self.encoding = "UTF-8"
        self.standalone = None
        self.doctype = ""
        self.URL = None

    @property
    def root_name(self):
        r = self._tree.getroot()
        return None if r is None else _split(r._xtag)[1]


def ElementTree(element=None, file=None, parser=None):
    if file is not None:
        return parse(file, parser)
    return _ElementTree(element)


# ---------------------------------------------------------------- serializing

def _esc_text(s):
    if "&" in s:
        s = s.replace("&", "&amp;")
    if "<" in s:
        s = s.replace("<", "&lt;")
    if ">" in s:
        s = s.replace(">", "&gt;")
    if "\r" in s:
        s = s.replace("\r", "&#13;")
    return s


def _esc_attr(s):
    s = _esc_text(s)
    if '"' in s:
        s = s.replace('"', "&quot;")
    if "\n" in s:
        s = s.replace("\n", "&#10;")
    if "\t" in s:
        s = s.replace("\t", "&#9;")
    return s


def _name(el, tag, is_attr, extra_decls):
    uri, local = _split(tag)
    if uri is None:
        return local
    p = _prefix_for(el, uri, is_attr)
    if p is _NOTFOUND:
        for q, u in extra_decls:
            if u == uri and (q is not None or not is_attr):
                p = q
                break
        if p is _NOTFOUND:
            p = "ns%d" % len(extra_decls) if is_attr else None
            extra_decls.append((p, uri))
    return local if p is None else p + ":" + local


def _write(el, out, decls, pretty, level):
    k = el._xkind
    if k == "c":
        out.append("<!--" + (el._xtext or "") + "-->")
        return
    if k == "p":
        out.append("<?" + el._xtarget + (" " + el._xtext if el._xtext else "") + "?>")
        return
    extra = []
    qn = _name(el, el._xtag, False, extra)
    attrs = []
    for key in el._xattrib:
        attrs.append(" " + _name(el, key, True, extra) + '="' + _esc_attr(el._xattrib[key]) + '"')
    out.append("<" + qn)
    for p, u in list(el._xns.items()) + list(decls) + extra:
        out.append((' xmlns="' if p is None else " xmlns:" + p + '="') + _esc_attr(u) + '"')
    out.extend(attrs)
    kids = el._xkids
    if not kids and el._xtext is None:
        out.append("/>")
        return
    out.append(">")
    if el._xtext:
        out.append(_esc_text(el._xtext))
    fmt = pretty and kids and el._xtext is None
    if fmt:
        for c in kids:
            if c._xtail is not None:
                fmt = False
                break
    for c in kids:
        if fmt:
            out.append("\n" + "  " * (level + 1))
        _write(c, out, (), fmt, level + 1)
        if c._xtail:
            out.append(_esc_text(c._xtail))
    if fmt:
        out.append("\n" + "  " * level)
    out.append("</" + qn + ">")


def _inherited_decls(el):
    """In-scope declarations from above `el` that it does not redeclare, used ones first."""
    if el._xparent is None or el._xkind != "e":
        return []
    chain = []
    n = el._xparent
    while n is not None:
        chain.append(n)
        n = n._xparent
    chain.reverse()
    order = []
    final = {}
    for n in chain:
        for k in n._xns:
            if k in el._xns:
                continue
            if k not in final:
                order.append(k)
            final[k] = n._xns[k]
    found = [(k, final[k]) for k in order]
    used = []
    uri = _split(el._xtag)[0]
    if uri is not None:
        used.append(uri)
    for key in el._xattrib:
        u = _split(key)[0]
        if u is not None and u not in used:
            used.append(u)
    first = []
    rest = []
    for p, u in found:
        if u in used:
            first.append((p, u))
        else:
            rest.append((p, u))
    first.sort(key=lambda d: used.index(d[1]))
    return first + rest


def tostring(element_or_tree, encoding=None, method="xml", xml_declaration=None, pretty_print=False,
             with_tail=True, standalone=None, doctype=None, exclusive=False, inclusive_ns_prefixes=None,
             with_comments=True, strip_text=False):
    is_tree = isinstance(element_or_tree, _ElementTree)
    el = element_or_tree.getroot() if is_tree else element_or_tree
    if el is None:
        raise ValueError("Cannot serialize an empty tree")
    if not isinstance(el, _Element):
        raise TypeError("Type '%s' cannot be serialized." % type(element_or_tree).__name__)
    to_unicode = encoding is str or (isinstance(encoding, str) and encoding.lower() == "unicode")
    if method == "text":
        s = "".join(el.itertext())
        if with_tail and not is_tree and el._xtail:
            s += el._xtail
        return s if to_unicode else s.encode(encoding or "ascii", "xmlcharrefreplace")
    out = []
    if is_tree and el._xdoc is not None:
        for c in el._xdoc[0]:
            _write(c, out, (), pretty_print, 0)
            if pretty_print:
                out.append("\n")
    _write(el, out, () if is_tree else _inherited_decls(el), pretty_print, 0)
    if is_tree and el._xdoc is not None:
        for c in el._xdoc[1]:
            if pretty_print:
                out.append("\n")
            _write(c, out, (), pretty_print, 0)
    if pretty_print:
        out.append("\n")
    if with_tail and not is_tree and el._xtail:
        out.append(_esc_text(el._xtail))
    body = "".join(out)
    if to_unicode:
        if xml_declaration:
            raise ValueError("Serialisation to unicode must not request an XML declaration")
        return body
    enc = encoding or "ASCII"
    write_decl = xml_declaration
    if standalone is not None:
        write_decl = True
    if write_decl is None:
        write_decl = enc.upper().replace("_", "-") not in ("ASCII", "UTF-8", "UTF8", "US-ASCII")
    if write_decl:
        decl = "<?xml version='1.0' encoding='%s'" % enc
        if standalone is not None:
            decl += " standalone='%s'" % ("yes" if standalone else "no")
        body = decl + "?>\n" + body
    lower = enc.lower().replace("_", "-")
    if lower in ("ascii", "us-ascii"):
        return body.encode("ascii", "xmlcharrefreplace")
    return body.encode(enc, "xmlcharrefreplace")


def tounicode(element_or_tree, method="xml", pretty_print=False, with_tail=True, doctype=None):
    return tostring(element_or_tree, encoding="unicode", method=method, pretty_print=pretty_print,
                    with_tail=with_tail)


def tostringlist(element_or_tree, *args, **kwargs):
    return [tostring(element_or_tree, *args, **kwargs)]


def dump(elem, pretty_print=True, with_tail=True):
    import sys
    sys.stdout.write(tostring(elem, encoding="unicode", pretty_print=pretty_print, with_tail=with_tail))


def cleanup_namespaces(tree_or_element, top_nsmap=None, keep_ns_prefixes=None):
    root = tree_or_element.getroot() if isinstance(tree_or_element, _ElementTree) else tree_or_element
    used = set()
    for n in _walk(root):
        u = _split(n._xtag)[0]
        if u is not None:
            used.add(u)
        for k in n._xattrib:
            u = _split(k)[0]
            if u is not None:
                used.add(u)
    keep = set(keep_ns_prefixes or ())
    for n in _walk(root):
        for k in list(n._xns.keys()):
            if n._xns[k] not in used and k not in keep:
                del n._xns[k]


def strip_tags(tree_or_element, *tag_names):
    root = tree_or_element.getroot() if isinstance(tree_or_element, _ElementTree) else tree_or_element
    m = _matcher(tag_names)
    if m is None:
        return
    for el in list(root.iterdescendants()):
        p = el._xparent
        if p is None or not m(el):
            continue
        i = p._xkids.index(el)
        text = el._xtext if el._xkind == "e" else None
        kids = list(el._xkids)
        tail = el._xtail
        p._xkids.pop(i)
        el._xparent = None
        prev = p._xkids[i - 1] if i > 0 else None

        def add(s, prev=prev):
            if not s:
                return
            if prev is None:
                p._xtext = (p._xtext or "") + s
            else:
                prev._xtail = (prev._xtail or "") + s
        add(text)
        for c in kids:
            c._xparent = p
            p._xkids.insert(i, c)
            i += 1
            prev = c
        add(tail, prev)
