# csv reading, writing, dialects and the sniffer.
import csv
from io import StringIO

text = 'name,age,note\r\nAda,36,"likes ""math"", engines"\r\nAlan,41,"multi\nline"\r\n"",,\r\n'
print(list(csv.reader(StringIO(text, newline=""))))
print(list(csv.reader(['a;b;"c;d"', "e; f;g"], delimiter=";", skipinitialspace=True)))
print(list(csv.reader(['1,"2",x'], quoting=csv.QUOTE_NONNUMERIC)) if False else "", list(csv.reader(["1,2.5,3"], quoting=csv.QUOTE_NONNUMERIC)))
print(list(csv.reader(['a\\,b,c'], escapechar="\\")), list(csv.reader(['a,"b\\"c"'], escapechar="\\", doublequote=False)))
print(list(csv.reader(["x,y", "", "z"])), list(csv.reader(['"unterminated'])))
for bad in [['a,"b"c'], ["a\0b"]]:
    try:
        print(list(csv.reader(bad, strict=True)))
    except csv.Error as e:
        print("Error", e)
out = StringIO()
w = csv.writer(out)
w.writerow(["plain", "with,comma", 'with "quote"', "multi\nline", 3, 2.5, None, ""])
w.writerows([[1, 2], ["a b", " lead"]])
w.writerow([""])
print(repr(out.getvalue()))
for q in [csv.QUOTE_ALL, csv.QUOTE_NONNUMERIC, csv.QUOTE_STRINGS, csv.QUOTE_NOTNULL]:
    o = StringIO()
    csv.writer(o, quoting=q, lineterminator="\n").writerow(["s", 1, 2.0, None, ""])
    print(q, repr(o.getvalue()))
o = StringIO()
csv.writer(o, quoting=csv.QUOTE_NONE, escapechar="\\", lineterminator="\n").writerow(["a,b", 'q"', "c"])
print(repr(o.getvalue()))
try:
    csv.writer(StringIO(), quoting=csv.QUOTE_NONE).writerow(["a,b"])
except csv.Error as e:
    print("Error", e)
o = StringIO()
dw = csv.DictWriter(o, fieldnames=["id", "name"], lineterminator="\n")
dw.writeheader()
dw.writerow({"id": 1, "name": "x"})
dw.writerows([{"id": 2}, {"name": "only"}])
print(repr(o.getvalue()))
try:
    dw.writerow({"id": 1, "bogus": 2})
except ValueError as e:
    print(e)
dr = csv.DictReader(StringIO("id,name\n1,a\n2,b,extra\n3\n"), restkey="rest", restval="?")
print(dr.fieldnames, list(dr), dr.line_num)
csv.register_dialect("pipes", delimiter="|", quoting=csv.QUOTE_ALL)
print(sorted(csv.list_dialects()), csv.get_dialect("pipes").delimiter, list(csv.reader(['"a"|"b"'], "pipes")))
csv.unregister_dialect("pipes")
try:
    csv.get_dialect("pipes")
except csv.Error as e:
    print(e)
for kw in [{"delimiter": "ab"}, {"quotechar": 5}, {"quoting": 9}, {"delimiter": "\n"}]:
    try:
        csv.reader([], **kw)
    except (TypeError, ValueError) as e:
        print(type(e).__name__, e)
s = csv.Sniffer()
sample = "a;b;c\n1;2;3\n4;5;6\n"
d = s.sniff(sample)
print(d.delimiter, d.quotechar, s.has_header(sample), s.has_header("x,y\nname,1\nother,2\n"))
r = csv.reader(StringIO("a,b\nc,d\n"))
next(r)
print(r.line_num, r.dialect.delimiter, csv.field_size_limit(), csv.field_size_limit(10), csv.field_size_limit())
try:
    list(csv.reader(["0123456789abc"]))
except csv.Error as e:
    print(e)
