from __future__ import annotations

from lxml import etree

from oxpkg.ns import NamespacePrefixedTag

element_class_lookup = etree.ElementNamespaceClassLookup()
oxml_parser = etree.XMLParser(remove_blank_text=True, resolve_entities=False)
oxml_parser.set_element_class_lookup(element_class_lookup)


def parse_xml(xml):
    return etree.fromstring(xml, oxml_parser)


def register_element_cls(nsptagname, cls):
    nsptag = NamespacePrefixedTag(nsptagname)
    namespace = element_class_lookup.get_namespace(nsptag.nsuri)
    namespace[nsptag.local_part] = cls


from oxpkg.shapes import CT_Ph, CT_NvPr, CT_Sp, CT_Ext  # noqa: E402

register_element_cls("p:ph", CT_Ph)
register_element_cls("p:nvPr", CT_NvPr)
register_element_cls("p:sp", CT_Sp)
register_element_cls("a:ext", CT_Ext)
