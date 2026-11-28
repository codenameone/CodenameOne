from lxml import etree

from oxpkg import parse_xml
from oxpkg.exc import InvalidXmlError
from oxpkg.ns import nsdecls

sp = parse_xml('<p:sp %s><p:nvPr><a:ext/></p:nvPr></p:sp>' % nsdecls("a", "p"))
nv = sp.nvPr
print(type(nv).__name__, nv.ph)
ph = nv.get_or_add_ph()
ph.type = "title"
ph.idx = 3
print(type(ph).__name__, ph.describe(), ph is nv.get_or_add_ph())
print([type(k).__name__ for k in nv])
e = nv._add_ext()
print(e.describe(), len(nv.ext_lst))
nv._remove_ph()
print(nv.ph, len(nv))
bare = parse_xml('<p:sp %s/>' % nsdecls("p"))
try:
    bare.nvPr
except InvalidXmlError as x:
    print("invalid", x)
print(etree.tostring(sp).count("ext"))
