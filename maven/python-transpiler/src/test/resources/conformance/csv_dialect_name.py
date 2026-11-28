import csv, io
print(list(csv.reader(io.StringIO("a;b\n1;2\n"))))
print(list(csv.reader(io.StringIO("a;b\n1;2\n"), delimiter=";")))
print(list(csv.reader(io.StringIO("a,b\n"), "excel")))
