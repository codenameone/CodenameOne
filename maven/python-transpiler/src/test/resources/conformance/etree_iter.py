import xml.etree.ElementTree as ET

root = ET.fromstring('<a><b><c/><d><c x="1"/></d></b><c/><e>t</e></a>')
print([e.tag for e in root.iter()])
print([e.tag for e in root.iter("c")])
print([e.tag for e in root.iter("*")])
print([e.tag for e in root.iter("a")])
print(len(list(root.iter("zz"))))
it = root.iter()
print(type(it) is not list, next(it).tag, next(it).tag)

# A child added under an element before the walk reaches it is seen.
seen = []
for e in root.iter():
    seen.append(e.tag)
    if e.tag == "b":
        ET.SubElement(e, "late")
print(seen)

leaf = ET.Element("solo")
print([e.tag for e in leaf.iter()], [e.tag for e in leaf.iter("x")])

ns = ET.fromstring('<r xmlns="urn:n"><i a="1"/><i a="2"/></r>')
print([(e.tag, e.get("a")) for e in ns.iter("{urn:n}i")])
print(ns[0].attrib is not ns[1].attrib, ns[0].attrib)

for s in ["12", "-7", "0", "007", " 5 ", "1_000", "+3", "123456789012345678", "1234567890123456789012"]:
    print(int(s))
for s in ["-", "", "1a", "--1"]:
    try:
        int(s)
    except ValueError as e:
        print("ValueError", e)
