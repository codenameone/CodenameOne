# XML: build a document as text, parse it with ElementTree and walk it, the
# work behind DOCX, PPTX and XLSX (all zipped XML).
import xml.etree.ElementTree as ET
from _harness import measure

NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"


def work(rows):
    parts = ['<worksheet xmlns="%s"><sheetData>' % NS]
    for r in range(rows):
        parts.append('<row r="%d">' % (r + 1))
        for c in range(6):
            parts.append('<c r="%s%d" t="n"><v>%d</v></c>' % ("ABCDEF"[c], r + 1, r * c))
        parts.append("</row>")
    parts.append("</sheetData></worksheet>")
    doc = "".join(parts)
    root = ET.fromstring(doc)
    total = 0
    cells = 0
    for row in root.iter("{%s}row" % NS):
        for c in row.findall("{%s}c" % NS):
            v = c.find("{%s}v" % NS)
            total += int(v.text)
            cells += 1
            if c.get("t") != "n":
                total -= 1
    return "%d %d %d" % (total, cells, len(doc))


measure("xml_etree", work, 6000, 5)
