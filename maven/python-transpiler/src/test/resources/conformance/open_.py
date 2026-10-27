# open(): text and binary, modes, newlines, errors.
with open("a.txt", "w") as f:
    print(f.write("hello\nworld\n"))
    f.write("line three")
    print(f.mode, f.name, f.closed)
print(f.closed)
with open("a.txt") as f:
    print(repr(f.read()))
with open("a.txt") as f:
    for line in f:
        print(repr(line))
with open("a.txt") as f:
    print(f.readlines())
with open("a.txt") as f:
    print(repr(f.readline()), repr(f.read(3)), f.tell())
with open("a.txt", "a") as f:
    f.write("\nappended")
print(repr(open("a.txt").read()))
with open("b.bin", "wb") as f:
    f.write(b"\x00\x01\xff")
    f.write(bytearray(b"ab"))
with open("b.bin", "rb") as f:
    print(f.read(2), f.read())
    f.seek(1)
    print(f.read(1), f.tell())
with open("b.bin", "ab") as f:
    f.write(b"Z")
print(open("b.bin", "rb").read())
with open("u.txt", "w", encoding="utf-8") as f:
    f.write("café €\n")
print(open("u.txt", "rb").read())
print(open("u.txt", encoding="utf-8").read())
with open("n.txt", "wb") as f:
    f.write(b"a\r\nb\rc\n")
print(repr(open("n.txt").read()))
print(repr(open("n.txt", newline="").read()))
with open("r.txt", "w+") as f:
    f.write("xyz")
    f.seek(0)
    print(f.read())
with open("r.txt", "r+") as f:
    f.write("A")
print(open("r.txt").read())
f = open("w.txt", "w")
f.writelines(["1\n", "2\n"])
f.close()
print(open("w.txt").read().split())
try:
    open("missing.txt")
except FileNotFoundError as e:
    print(type(e).__name__, e.errno, e.strerror, e.filename)
    print(e)
try:
    open("w.txt", "x")
except FileExistsError as e:
    print(e)
for bad in ["rw", "q", "rbt"]:
    try:
        open("w.txt", bad)
    except ValueError as e:
        print(e)
try:
    open("w.txt", "rb", encoding="utf-8")
except ValueError as e:
    print(e)
f = open("w.txt")
f.close()
try:
    f.read()
except ValueError as e:
    print(e)
with open("w.txt") as f:
    try:
        f.write("x")
    except OSError as e:
        print(type(e).__name__)
with open("w.txt", "w") as f:
    try:
        f.read()
    except OSError as e:
        print(type(e).__name__)
try:
    with open("w.txt", "w") as f:
        f.write(5)
except TypeError as e:
    print(e)
