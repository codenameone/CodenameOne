# Scratch: expat event streams compared with CPython, then timed.
import os
import time
import xml.parsers.expat as E

DOCS = [
    '<a x="1" y=\'2\'>t&amp;x&#65;&#x42;&lt;<b/>tail\nline2<!-- c --><c z="a\tb"></c></a>',
    '<?xml version="1.0"?><r xmlns="urn:d" xmlns:p="urn:p"><p:e p:at="v" at="w">x</p:e><e/></r>',
    '<!DOCTYPE r [<!ENTITY e "ent">]><r>&e; &amp; <![CDATA[cd<>]]><?pi data?></r>',
    '<r><a>1</a><a>2</a>\n  <b k="&quot;q&apos;">\xe9中</b></r>',
]


def events(doc, sep, buffered, ordered):
    out = []
    p = E.ParserCreate(namespace_separator=sep)
    p.buffer_text = buffered
    p.ordered_attributes = ordered
    p.StartElementHandler = lambda n, a: out.append(("S", n, a if ordered else sorted(a.items())))
    p.EndElementHandler = lambda n: out.append(("E", n))
    p.CharacterDataHandler = lambda d: out.append(("C", d))
    p.CommentHandler = lambda d: out.append(("#", d))
    p.StartNamespaceDeclHandler = lambda a, b: out.append(("NS", a, b))
    p.EndNamespaceDeclHandler = lambda a: out.append(("NE", a))
    p.ProcessingInstructionHandler = lambda a, b: out.append(("PI", a, b))
    try:
        p.Parse(doc, True)
    except E.ExpatError as e:
        out.append(("ERR", str(e)))
    return out


def big():
    parts = ['<root xmlns:w="urn:w">']
    for i in range(2000):
        parts.append('<w:p w:id="%d"><w:r><w:t xml:space="preserve">Run %d &amp; more</w:t></w:r></w:p>\n' % (i, i))
    parts.append('</root>')
    return "".join(parts).encode("utf-8")


def main():
    for d in DOCS + ['<a><b></a>', '<a>&bogus;</a>', '<a x="1" x="2"/>']:
        for sep in (None, "}"):
            for bt in (False, True):
                for od in (False, True):
                    print(events(d, sep, bt, od))
    data = big()
    best = None
    for _ in range(int(os.environ.get("REP", "7"))):
        t0 = time.perf_counter()
        for _ in range(5):
            cnt = [0]
            p = E.ParserCreate(namespace_separator="}")
            p.buffer_text = True
            p.StartElementHandler = lambda n, a: None
            p.EndElementHandler = lambda n: None
            p.CharacterDataHandler = lambda d: None
            p.Parse(data, True)
        dt = time.perf_counter() - t0
        best = dt if best is None or dt < best else best
    print("PART parse", int(best * 1e6 / 5))


main()
