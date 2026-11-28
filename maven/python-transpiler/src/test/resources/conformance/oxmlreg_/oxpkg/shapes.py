from __future__ import annotations

from oxpkg.xmlchemy import (
    BaseOxmlElement,
    OneAndOnlyOne,
    OptionalAttribute,
    ZeroOrMore,
    ZeroOrOne,
)


class ST_Int(object):
    @classmethod
    def from_xml(cls, s):
        return int(s)

    @classmethod
    def to_xml(cls, v):
        return str(v)

    @classmethod
    def validate(cls, v):
        pass


class ST_Str(ST_Int):
    @classmethod
    def from_xml(cls, s):
        return s

    @classmethod
    def to_xml(cls, v):
        return v


class CT_Ph(BaseOxmlElement):
    type = OptionalAttribute("type", ST_Str, default="obj")
    idx = OptionalAttribute("idx", ST_Int, default=0)

    def describe(self):
        return "ph %s %d" % (self.type, self.idx)


class CT_Ext(BaseOxmlElement):
    def describe(self):
        return "ext"


class CT_NvPr(BaseOxmlElement):
    ph = ZeroOrOne("p:ph", successors=("a:ext", "p:extLst"))
    ext = ZeroOrMore("a:ext", successors=("p:extLst",))


class CT_Sp(BaseOxmlElement):
    nvPr = OneAndOnlyOne("p:nvPr")
