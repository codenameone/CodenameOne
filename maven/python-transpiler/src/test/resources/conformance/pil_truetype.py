# Pillow: scalable TrueType text -- ImageFont.load_default(size) (Aileron
# Regular), truetype() from a file object, FreeTypeFont metrics, and
# ImageDraw.text / multiline_text / textbbox / textlength with anchors.
#
# Tolerance. FreeType hints Aileron with its auto-hinter, which moves glyph
# edges and advances to whole pixels; Codename One's TrueTypeFont does not
# hint. So:
#   - exact: getmetrics(), getname(), errors, and getlength() at size 20,
#     where the hinted and unhinted advances agree;
#   - within 1 px per glyph: getlength() at other sizes;
#   - within 1 px per glyph: every box edge, for every anchor;
#   - pixels: the ink's coverage sum (sum of mask values / 255) within 10% of
#     what CPython+FreeType draws, and its centroid within 1 px.
# REF values below were measured with CPython 3.13 + Pillow 13.0 (basic
# layout); the program prints only whether each is within its tolerance.
import io

from PIL import Image, ImageDraw, ImageFont


def near(a, b, tol):
    return all(abs(x - y) <= tol for x, y in zip(a, b)) if isinstance(a, tuple) else abs(a - b) <= tol


f20 = ImageFont.load_default(20)
print(type(f20).__name__, f20.getname(), f20.size)
print(f20.getmetrics(), ImageFont.load_default().size, ImageFont.load_default().getmetrics())
REF20 = {"H": (0, 6, 15, 20), "AVA": (0, 6, 38, 20), "Hello World": (0, 5, 107, 20), "": (0, 0, 0, 0),
         "gy j": (0, 5, 31, 24)}
for s in ("H", "AVA", "Hello World", "", "gy j"):
    print(repr(s), f20.getlength(s), near(f20.getbbox(s), REF20[s], 1))
REFA = {"la": (0, 5, 19, 20), "lt": (0, 0, 19, 15), "ls": (0, -15, 19, 0), "lb": (0, -15, 19, 0),
        "ld": (0, -20, 19, -5), "ma": (-10, 5, 9, 20), "mm": (-10, -7, 9, 8), "ms": (-10, -15, 9, 0),
        "rs": (-19, -15, 0, 0), "rd": (-19, -20, 0, -5)}
for a in ("la", "lt", "ls", "lb", "ld", "ma", "mm", "ms", "rs", "rd"):
    print(a, near(f20.getbbox("Hi", anchor=a), REFA[a], 1))
try:
    f20.getbbox("x", anchor="xx")
except ValueError as e:
    print("ValueError", e)
try:
    f20.getlength("x", direction="rtl")
except KeyError:
    print("KeyError direction")

REF_LEN = {10: (8.0, 18.0, 55.0), 13: (9.0, 24.0, 70.0), 37: (27.0, 71.0, 197.0)}
REF_BOX = {10: (0, 2, 25, 10), 13: (0, 4, 31, 13), 37: (0, 9, 87, 37)}
for size in (10, 13, 37):
    f = ImageFont.load_default(size)
    lens = (f.getlength("H"), f.getlength("AVA"), f.getlength("Hello World"))
    print(size, f.getmetrics(), [near(x, r, n) for x, r, n in zip(lens, REF_LEN[size], (1, 3, 11))],
          near(f.getbbox("Hello"), REF_BOX[size], 5))

# truetype() from a file object and from bytes; font_variant
tt = ImageFont.truetype(io.BytesIO(ImageFont.load_default(10).path.getvalue()), 20,
                        layout_engine=ImageFont.Layout.BASIC)
print(tt.getname(), tt.getmetrics(), tt.getlength("Hello"), near(tt.getbbox("Hello"), (0, 5, 48, 20), 1))
v = tt.font_variant(size=40)
print(v.size, v.getmetrics(), near(v.getlength("Hello"), 94.0, 5))
try:
    ImageFont.truetype(io.BytesIO(b"not a font at all"), 12)
except OSError:
    print("OSError bad font")
try:
    ImageFont.truetype("/no/such/font.ttf", 12)
except OSError:
    print("OSError missing file")


def ink(im):
    """Coverage sum and centroid of a grayscale image's ink."""
    w, h = im.size
    raw = im.tobytes()
    total = sx = sy = 0
    for y in range(h):
        for x in range(w):
            v = raw[y * w + x]
            total += v
            sx += v * x
            sy += v * y
    return total / 255.0, (sx / total, sy / total) if total else (0, 0)


REF_INK = {"t1": (505.0, (63.4, 25.0)), "t2": (174.3, (48.6, 30.7)), "t3": (423.0, (53.2, 49.8))}


def check(name, im):
    cov, (cx, cy) = ink(im)
    rc, (rx, ry) = REF_INK[name]
    print(name, abs(cov - rc) <= 0.10 * rc, near((cx, cy), (rx, ry), 1.0))


im = Image.new("L", (130, 50), 0)
d = ImageDraw.Draw(im)
d.text((6, 12), "Hello, Pillow!", fill=255, font=ImageFont.load_default(20))
print(near(d.textbbox((6, 12), "Hello, Pillow!", font=ImageFont.load_default(20)), (6, 17, 121, 34), 2),
      near(d.textlength("Hello, Pillow!", font=ImageFont.load_default(20)), 115.0, 3))
check("t1", im)

im = Image.new("L", (100, 60), 0)
d = ImageDraw.Draw(im)
d.text((50, 30), "anchor", fill=255, font=ImageFont.load_default(16), anchor="mm")
print(near(d.textbbox((50, 30), "anchor", font=ImageFont.load_default(16), anchor="mm"), (24, 24, 76, 36), 2))
check("t2", im)

im = Image.new("L", (110, 80), 0)
d = ImageDraw.Draw(im)
d.multiline_text((55, 40), "two\nlines here", fill=255, font=ImageFont.load_default(18),
                 anchor="ms", align="center")
print(near(d.multiline_textbbox((55, 40), "two\nlines here", font=ImageFont.load_default(18),
                                anchor="ms", align="center"), (16.0, 28, 93.0, 62), 2))
check("t3", im)
try:
    d.multiline_text((0, 0), "a\nb", anchor="lt")
except ValueError as e:
    print("ValueError", e)

# Default font through ImageDraw, font_size, and fill colour on RGB
rgb = Image.new("RGB", (60, 20), "white")
d = ImageDraw.Draw(rgb)
print(near(d.textlength("abc"), 16.0, 3), near(d.textlength("abc", font_size=20), 34.0, 3),
      near(d.textbbox((0, 0), "abc"), (0, 2, 16, 10), 2))
d.text((2, 2), "abc", fill=(200, 0, 0), font_size=14)
raw = rgb.tobytes()
reds = sum(1 for i in range(0, len(raw), 3) if raw[i] > 150 and raw[i + 1] < 100)
print("red pixels", reds > 20)
