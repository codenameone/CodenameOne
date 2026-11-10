b = bytearray(b"hello")
print(b, len(b), b[0], b[1:3], b[-1], list(b)[:2], bytearray(), bytearray(3), bytearray([65, 66]), bytearray("hi", "utf-8"))
b.append(33)
b.extend(b"??")
b.extend([1, 2])
b.insert(0, 62)
print(b, b.pop(), b.pop(0), b)
b[0] = 72
b[1:3] = b"EY"
del b[-1]
b += b"!"
print(b, b.upper(), b.find(b"l"), b.count(b"l"), b.replace(b"l", b"L"), b.split(b"l"), b.decode("ascii", "replace"))
print(b == b"HEYlo!??!", b"HEYlo!??!" == b, bytes(b), b"x" + b, b + b"x", b * 2, b"l" in b, 108 in b, b.hex())
b.reverse()
print(b, b.copy() is b, bytearray.fromhex("de ad be ef"), b.strip(b"!?"), b.startswith(b"!"))
b.remove(ord("Y"))
b.clear()
print(b, bool(b), bytearray(b"a") < bytearray(b"b"), bytes(bytearray(b"xyz")), b"-".join([bytearray(b"a"), b"b"]))
c = bytearray(b"abcdef")
del c[::2]
c[::2] = b"XY"
print(c, isinstance(c, bytearray), type(c).__name__)
m = memoryview(c)
print(len(m), m[0], m[1:].tobytes(), m.tolist(), m.readonly, m.nbytes, m.format, m.itemsize, m == b"XdYf", bytes(m))
m[0] = 90
m[1:3] = b"zz"
print(c, m.obj is c, list(m[::2]), m.hex())
with memoryview(b"abc") as v:
    print(v.tobytes(), v.readonly)
try:
    v.tobytes()
except ValueError as e:
    print("ValueError", e)
for bad in [lambda: hash(bytearray()), lambda: bytearray("x"), lambda: bytearray(b"a").append(256),
            lambda: bytearray().pop(), lambda: bytearray(b"a").remove(9), lambda: memoryview(b"a").__setitem__(0, 1),
            lambda: memoryview("s"), lambda: bytearray(b"a")[5]]:
    try:
        bad()
    except (TypeError, ValueError, IndexError) as e:
        print(type(e).__name__, e)
