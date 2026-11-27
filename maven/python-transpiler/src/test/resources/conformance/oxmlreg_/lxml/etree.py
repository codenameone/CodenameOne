"""The subset of lxml.etree the test uses, for the CPython reference run."""
import xml.etree.ElementTree as _ET


class _Element(object):
    def _setup(self, tag, nsmap):
        self.tag = tag
        self.nsmap = dict(nsmap or {})
        self._kids = []
        self._attrib = {}
        self._parent = None

    def get(self, key, default=None):
        return self._attrib.get(key, default)

    def set(self, key, value):
        self._attrib[key] = value

    def find(self, path, namespaces=None):
        for k in self._kids:
            if k.tag == path:
                return k
        return None

    def findall(self, path, namespaces=None):
        return [k for k in self._kids if k.tag == path]

    def append(self, child):
        if child._parent is not None:
            child._parent._kids.remove(child)
        child._parent = self
        self._kids.append(child)

    def remove(self, child):
        self._kids.remove(child)
        child._parent = None

    def addprevious(self, other):
        p = self._parent
        if other._parent is not None:
            other._parent._kids.remove(other)
        p._kids.insert(p._kids.index(self), other)
        other._parent = p

    def getparent(self):
        return self._parent

    def iterchildren(self):
        return iter(list(self._kids))

    def __iter__(self):
        return iter(list(self._kids))

    def __len__(self):
        return len(self._kids)


class ElementBase(_Element):
    pass


class _Plain(_Element):
    pass


class _Namespace(dict):
    pass


class ElementNamespaceClassLookup(object):
    def __init__(self):
        self._ns = {}

    def get_namespace(self, uri):
        n = self._ns.get(uri)
        if n is None:
            n = _Namespace()
            self._ns[uri] = n
        return n

    def _cls(self, tag):
        uri, local = tag[1:].split("}", 1)
        n = self._ns.get(uri, {})
        return n.get(local) or n.get(None) or _Plain


class XMLParser(object):
    def __init__(self, **kw):
        self._lookup = None

    def set_element_class_lookup(self, lookup):
        self._lookup = lookup

    def makeelement(self, tag, attrib=None, nsmap=None):
        cls = self._lookup._cls(tag) if self._lookup is not None else _Plain
        el = cls.__new__(cls)
        el._setup(tag, nsmap)
        return el


def fromstring(text, parser):
    def build(e):
        el = parser.makeelement(e.tag)
        for k, v in e.attrib.items():
            el.set(k, v)
        for c in e:
            el.append(build(c))
        return el
    return build(_ET.fromstring(text))


def tostring(el, encoding=None, pretty_print=False):
    a = "".join(' %s="%s"' % (k, v) for k, v in sorted(el._attrib.items()))
    return "<%s%s>%s</%s>" % (el.tag, a, "".join(tostring(k) for k in el._kids), el.tag)
