# Pillow JPEG: round trips through BytesIO and a file path, RGB and L, the
# quality setting, and the modes JPEG cannot hold. JPEG is lossy and codecs
# round differently, so pixels are compared within a tolerance.
import io
import os

from PIL import Image


def gradient(mode, w, h):
    im = Image.new(mode, (w, h))
    for y in range(h):
        for x in range(w):
            if mode == "L":
                im.putpixel((x, y), (x * 255 // (w - 1) + y * 2) % 256)
            else:
                im.putpixel((x, y), (x * 255 // (w - 1), y * 255 // (h - 1), (x + y) * 4 % 256))
    return im


def error(a, b):
    total = 0
    worst = 0
    n = 0
    for y in range(a.height):
        for x in range(a.width):
            p = a.getpixel((x, y))
            q = b.getpixel((x, y))
            if not isinstance(p, tuple):
                p, q = (p,), (q,)
            for u, v in zip(p, q):
                d = abs(u - v)
                total += d
                worst = max(worst, d)
                n += 1
    return total / n, worst


rgb = gradient("RGB", 40, 24)
buf = io.BytesIO()
rgb.save(buf, format="JPEG", quality=92)
data = buf.getvalue()
print(data[:3] == b"\xff\xd8\xff", data[-2:] == b"\xff\xd9")
back = Image.open(io.BytesIO(data))
print(back.format, back.mode, back.size)
mean, worst = error(rgb, back)
print("rgb close", mean < 4, worst < 60)

gray = gradient("L", 33, 17)
buf = io.BytesIO()
gray.save(buf, "JPEG")
g = Image.open(io.BytesIO(buf.getvalue()))
print(g.format, g.mode, g.size)
mean, worst = error(gray, g)
print("gray close", mean < 4, worst < 40)

low = io.BytesIO()
high = io.BytesIO()
rgb.save(low, "JPEG", quality=20)
rgb.save(high, "JPEG", quality=95)
print("quality orders size", len(low.getvalue()) < len(high.getvalue()))

path = "pil_jpeg_conformance_tmp.jpg"
rgb.save(path)
with Image.open(path) as f:
    print(f.format, f.mode, f.size)
os.remove(path)

again = io.BytesIO()
back.save(again, "JPEG")
print("re-encode", Image.open(io.BytesIO(again.getvalue())).size)

for mode in ("RGBA", "P"):
    try:
        Image.new(mode, (2, 2)).save(io.BytesIO(), "JPEG")
        print(mode, "saved")
    except OSError as e:
        print(mode, "OSError", e)
