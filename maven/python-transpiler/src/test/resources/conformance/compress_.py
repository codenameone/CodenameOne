import zlib, gzip, io, os, zipfile, struct
T = b'hello hello hello compression ' * 20
ZL=bytes.fromhex('789ccb48cdc9c957c8402293f3730b8a528b8b33f3f350c447654765899705005fe8e499')
RAW=bytes.fromhex('cb48cdc9c957c8402293f3730b8a528b8b33f3f350c44765476589970500')
GZ=bytes.fromhex('1f8b08000000000002ffcb48cdc9c957c8402293f3730b8a528b8b33f3f350c44765476589970500527e086f58020000')
DICT=bytes.fromhex('78bb3ca206e7cb000b209348920aa3b2a3b2e4c902005fe8e499')
ZIP=bytes.fromhex('504b030414000000000083182250527e086f580200005802000005000000612e74787468656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e2068656c6c6f2068656c6c6f2068656c6c6f20636f6d7072657373696f6e20504b03041400000000000539a652738c05290001000000010000090000006469722f622e62696e000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff504b0102140314000000000083182250527e086f5802000058020000050000000000000000000000800100000000612e747874504b010214031400000000000539a652738c0529000100000001000009000000000000000000000080017b0200006469722f622e62696e504b050600000000020002006a000000a20300000000')

# zlib: decompress what CPython's zlib produced, and round-trip our own
print(zlib.decompress(ZL) == T, zlib.decompress(RAW, -15) == T, zlib.decompress(GZ, 31) == T,
      zlib.decompress(GZ, 47) == T, zlib.decompress(ZL, 47) == T)
for level in (0, 1, 6, 9, -1):
    c = zlib.compress(T, level)
    print("level", level, zlib.decompress(c) == T, c[:1].hex(), len(c) < len(T) or level == 0)
print(hex(zlib.crc32(T)), hex(zlib.adler32(T)), zlib.crc32(b"abc", zlib.crc32(b"xyz")),
      zlib.adler32(b""), zlib.crc32(b""))
d = zlib.decompressobj(zdict=b"hello compression")
print("dict", d.decompress(DICT) == T, d.eof)

# streaming, max_length, unused data
c = zlib.compressobj(9, zlib.DEFLATED, -15)
parts = [c.compress(T[i:i + 50]) for i in range(0, len(T), 50)]
parts.append(c.flush(zlib.Z_SYNC_FLUSH))
parts.append(c.compress(b"tail"))
parts.append(c.flush())
stream = b"".join(parts)
d = zlib.decompressobj(-15)
out = b""
for i in range(0, len(stream), 7):
    out += d.decompress(stream[i:i + 7])
print("stream", out == T + b"tail", d.eof)
d = zlib.decompressobj()
first = d.decompress(ZL + b"extra", 100)
print("max_length", len(first), len(d.unconsumed_tail) > 0, d.eof)
rest = d.decompress(d.unconsumed_tail)
print("rest", first + rest == T, d.eof, d.unused_data)
try:
    zlib.decompress(b"not zlib data")
except zlib.error as e:
    print("error", e)
try:
    zlib.decompress(ZL[:20])
except zlib.error as e:
    print("error", e)
print(zlib.MAX_WBITS, zlib.Z_BEST_COMPRESSION, zlib.DEFLATED)

# gzip
print("gzip", gzip.decompress(GZ) == T, gzip.compress(T, mtime=0)[:10].hex(),
      gzip.compress(T, 1, mtime=5)[:10].hex(), gzip.decompress(gzip.compress(T)) == T)
print("members", gzip.decompress(GZ + gzip.compress(b"!", mtime=0)) == T + b"!")
os.makedirs("gz", exist_ok=True)
with gzip.open("gz/f.txt.gz", "wt", encoding="utf-8") as f:
    f.write("line one\nline two\n")
with gzip.open("gz/f.txt.gz", "rt", encoding="utf-8") as f:
    print("text", f.readlines())
with gzip.open("gz/f.txt.gz", "rb") as f:
    print("binary", f.read(4), f.read())
with open("gz/f.txt.gz", "rb") as f:
    raw = f.read()
print("header", raw[:4].hex(), raw[10:16])
buf = io.BytesIO()
with gzip.GzipFile(fileobj=buf, mode="wb", mtime=0) as g:
    g.write(b"abc")
    g.write(memoryview(b"def"))
print("fileobj", buf.getvalue()[:10].hex(), gzip.decompress(buf.getvalue()))
try:
    gzip.decompress(b"plain")
except gzip.BadGzipFile as e:
    print("bad", e)
try:
    gzip.decompress(GZ[:-3])
except EOFError as e:
    print("eof", e)

# zipfile: read CPython's archive
z = zipfile.ZipFile(io.BytesIO(ZIP))
print(z.namelist(), [i.date_time for i in z.infolist()], [i.compress_type for i in z.infolist()])
print(z.read("a.txt") == T, z.read("dir/b.bin") == bytes(range(256)), z.testzip())
info = z.getinfo("a.txt")
print(info.file_size, hex(info.CRC), info.is_dir())
with z.open("dir/b.bin") as f:
    print(f.read(3))
try:
    z.getinfo("missing")
except KeyError as e:
    print("KeyError", e)
z.close()

# write, append and extract
with zipfile.ZipFile("gz/out.zip", "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("x.txt", "text " * 50)
    z.writestr(zipfile.ZipInfo("stored.bin", (2022, 2, 3, 4, 5, 6)), b"\x00\x01", zipfile.ZIP_STORED)
    with z.open("w.txt", "w") as w:
        w.write(b"written ")
        w.write(b"through open")
    z.mkdir("empty")
    z.write("gz/f.txt.gz", "nested/f.gz")
    z.comment = b"a comment"
with zipfile.ZipFile("gz/out.zip", "a") as z:
    z.writestr("appended.txt", b"more")
print("is_zipfile", zipfile.is_zipfile("gz/out.zip"), zipfile.is_zipfile("gz/f.txt.gz"))
with zipfile.ZipFile("gz/out.zip") as z:
    print(z.namelist(), z.comment)
    print(z.read("x.txt") == b"text " * 50, z.read("w.txt"), z.read("appended.txt"), z.read("nested/f.gz") == raw)
    print([(i.filename, i.compress_type, i.is_dir()) for i in z.infolist()])
    print(z.getinfo("stored.bin").date_time, z.getinfo("stored.bin").compress_size)
    z.extractall("gz/ex")
    print(sorted(os.listdir("gz/ex")), sorted(os.listdir("gz/ex/nested")), os.path.isdir("gz/ex/empty"))
    p = zipfile.Path(z)
    print(sorted(c.name for c in p.iterdir()), (p / "w.txt").read_text(), (p / "nested").is_dir())
with open("gz/ex/w.txt") as f:
    print(f.read())
try:
    zipfile.ZipFile(io.BytesIO(b"nope"))
except zipfile.BadZipFile as e:
    print("BadZipFile", e)
try:
    zipfile.ZipFile(io.BytesIO(), "w", zipfile.ZIP_LZMA + 100)
except NotImplementedError as e:
    print("NotImplementedError", e)
