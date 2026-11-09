# Bytes in, text out: what every converter does to a file before parsing it.
# UTF-8 and Latin-1 decoding, encoding back, byte indexing and bytes slicing.
from _harness import measure


def work(n):
    chunk = ("naïve café — résumé <p class='x'>text</p>\n" * 8).encode("utf-8")
    total = 0
    checksum = 0
    for i in range(n):
        text = chunk.decode("utf-8")
        total += len(text)
        raw = text.encode("utf-8")
        checksum = (checksum + raw[i % len(raw)]) & 0xffff
        head = raw[: 16 + i % 32]
        checksum = (checksum + len(head.decode("latin-1"))) & 0xffff
    return "%d %d" % (total, checksum)


measure("bytes_codec", work, 40000, 5)
