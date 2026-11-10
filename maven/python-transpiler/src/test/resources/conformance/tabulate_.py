from tabulate import tabulate

table = [["Sun", 696000, 1989100000], ["Earth", 6371, 5973.6], ["Moon", 1737, 73.5], ["Mars", 3390, 641.85]]
hdr = ["Planet", "R (km)", "mass (x 10^29 kg)"]
for fmt in ("plain", "simple", "grid", "github", "pipe", "psql", "rst", "html", "orgtbl", "tsv"):
    print(fmt)
    print(tabulate(table, headers=hdr, tablefmt=fmt))
    print(tabulate(table, tablefmt=fmt))
    print()

print(tabulate(table, hdr, floatfmt=".2f"))
print(tabulate(table, hdr, floatfmt=".1e", numalign="right"))
print(tabulate(table, hdr, stralign="right", numalign="left"))
print(tabulate(table, hdr, stralign="center", tablefmt="grid"))
print(tabulate(table, hdr, showindex=True))
print(tabulate(table, hdr, showindex="always", tablefmt="psql"))
print(tabulate(table, hdr, showindex=["a", "b", "c", "d"]))
print(tabulate([[1, 2, 3], [4, 5, 6]]))
print(tabulate([[1, None, 3], ["x", "", "z"]], missingval="?"))
print(tabulate([{"a": 1, "b": "x"}, {"a": 22, "b": "yy"}], headers="keys"))
print(tabulate({"name": ["Al", "Bo"], "age": [3, 44]}, headers="keys", tablefmt="github"))
print(tabulate([["h1", "h2"], [1, 2.5], [10, 33.25]], headers="firstrow", tablefmt="pipe"))
print(tabulate([["a", "1.5"], ["b", "22"], ["c", "x"]], headers=["k", "v"]))
print(tabulate([["a", "1.5"], ["b", "22"]], headers=["k", "v"], disable_numparse=True))
print(tabulate([[True, 1], [False, 2]], headers=["flag", "n"]))
print(tabulate([[1.0, 2.55], [10.125, 3]], headers=["x", "y"]))
print(tabulate([["a<b", "&"]], headers=["h", "i"], tablefmt="html"))
print(tabulate([[1, 2], [3, 4]], headers=["only"]))
print(tabulate([], headers=["a", "b"]))
print(tabulate([[1, 2]], headers=["a", "b"], colalign=("right", "center"), tablefmt="pipe"))
print(tabulate([["x", 1]], headers=["col", "n"], colalign=["right", "left"], tablefmt="grid"))
print(tabulate([[1e10, 1.5e-5, 123456789.123]], floatfmt="g"))
print(tabulate([[1234, 5]], intfmt=","))
