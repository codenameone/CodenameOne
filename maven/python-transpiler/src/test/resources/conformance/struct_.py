# struct.
import struct

print(struct.calcsize("<i"), struct.calcsize("@bi"), struct.calcsize("<bi"), struct.calcsize("@hq"), struct.calcsize("=l"), struct.calcsize("3s2x?"))
print(struct.pack("<hHiIqQ", -2, 65535, -1, 4000000000, -5, 2 ** 64 - 1))
print(struct.pack(">hHiI", -2, 65535, -1, 4000000000), struct.pack("!bB?c", -128, 255, True, b"z"))
print(struct.unpack("<hHiIqQ", struct.pack("<hHiIqQ", -2, 65535, -1, 4000000000, -5, 2 ** 64 - 1)))
for v in [0.0, -0.0, 1.0, -2.5, 0.1, 1e300, 5e-324, 3.4028234663852886e38, 1.401298464324817e-45, 65504.0, 6e-8, float("inf")]:
    print(repr(v), struct.pack("<d", v).hex(), struct.unpack("<d", struct.pack("<d", v))[0], struct.pack(">f", v).hex() if abs(v) < 3.5e38 or v != v else "-",
          struct.unpack(">f", struct.pack(">f", v))[0] if abs(v) < 3.5e38 else "-", struct.pack("<e", v).hex() if abs(v) <= 65504 or v == float("inf") else "-")
print(struct.unpack("<f", struct.pack("<f", 0.1))[0], struct.unpack("<e", struct.pack("<e", 0.1))[0], struct.unpack("<d", bytes.fromhex("000000000000f87f")))
print(struct.pack("5s", b"ab"), struct.pack("2s", b"abcd"), struct.pack("5p", b"abc"), struct.unpack("5p", b"\x03abcd"), struct.unpack("3s", b"xyz"))
print(struct.pack("@bhi", 1, 2, 3), struct.pack("@ci", b"a", 1), struct.pack("@d?", 1.0, False))
s = struct.Struct("<2h")
print(s.size, s.format, s.pack(1, 2), s.unpack(b"\x01\x00\x02\x00"), list(s.iter_unpack(b"\x01\x00\x02\x00\x03\x00\x04\x00")), repr(s))
buf = bytearray(8)
struct.pack_into("<i", buf, 2, 0x01020304)
print(buf, struct.unpack_from("<i", buf, 2), struct.unpack_from(">H", b"\x00\x01\x02", 1))
for args in [("<b", 200), ("<B", -1), ("<h", 40000), ("<I", -1), ("<i", 1.5), ("<i",), ("<c", b"ab"), ("<q", 2 ** 63)]:
    try:
        struct.pack(*args)
    except struct.error as e:
        print(e)
for f, b in [("<i", b"\x00"), ("<h", b"\x00\x00\x00")]:
    try:
        struct.unpack(f, b)
    except struct.error as e:
        print(e)
try:
    struct.calcsize("<y")
except struct.error as e:
    print(e)
try:
    struct.pack("<f", 1e39)
except OverflowError as e:
    print(e)
print(struct.pack("<?", []), struct.pack("<?", 5), struct.pack(b"<h", 7))
