# A raw deflate stream's last code may end a few bits into its final byte,
# short of the width the decoder's table is indexed with. The inflater used
# to wait for input that never comes and report the complete stream as
# truncated -- for roughly half of these inputs, depending on the mtime the
# gzip header carries (which is how zipfile lost an entry intermittently).
import zlib, gzip
ok = streamed = 0
for k in range(1780000000, 1780000000 + 2100, 7):
    data = gzip.compress(b"line one\nline two\n", mtime=k)
    c = zlib.compressobj(-1, zlib.DEFLATED, -15)
    comp = c.compress(data) + c.flush()
    if zlib.decompress(comp, -15) == data:
        ok += 1
    d = zlib.decompressobj(-15)
    if d.decompress(comp) == data and d.eof and d.unused_data == b"":
        streamed += 1
print("raw round trips", ok, streamed)
for size in (1, 2, 3, 7, 50, 300):
    data = bytes((i * 7 + size) & 0xff for i in range(size))
    comp = zlib.compress(data, 9, -15)
    print(size, zlib.decompress(comp, -15) == data, zlib.decompress(zlib.compress(data)) == data)
