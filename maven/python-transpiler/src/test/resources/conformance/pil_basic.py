# Pillow Image basics: modes, pixels, conversion, geometry, paste, bands.
import hashlib

from PIL import Image


def h(im):
    return hashlib.sha256(im.tobytes()).hexdigest()[:16]


def pattern(w, h_):
    im = Image.new("RGBA", (w, h_))
    for y in range(h_):
        for x in range(w):
            im.putpixel((x, y), ((x * 37 + y * 11) % 256, (x * x + y * 3) % 256,
                                 (x * y * 5 + 7) % 256, (x * 13 + y * 29) % 256))
    return im


for mode, color in (("RGB", (10, 20, 30)), ("RGBA", (1, 2, 3, 4)), ("L", 77), ("RGB", "red"),
                    ("L", "orange"), ("RGBA", "#336699"), ("RGB", 0x123456), ("RGBA", (5, 6, 7))):
    im = Image.new(mode, (3, 2), color)
    print(mode, im.size, im.width, im.height, im.mode, im.getpixel((1, 1)), im.getpixel((-1, -1)))

src = pattern(11, 7)
rgb = src.convert("RGB")
gray = src.convert("L")
print("conv", h(src), h(rgb), h(gray), h(gray.convert("RGB")), h(gray.convert("RGBA")), h(rgb.convert("RGBA")))
print("px", rgb.getpixel((3, 4)), gray.getpixel((3, 4)), src.getpixel((10, 6)))
print("bands", src.getbands(), rgb.getbands(), gray.getbands())

c = rgb.crop((2, 1, 8, 5))
print("crop", c.size, h(c), h(rgb.crop((-2, -1, 4, 3))))
print("copy", rgb.copy().tobytes() == rgb.tobytes())

for t in (Image.FLIP_LEFT_RIGHT, Image.FLIP_TOP_BOTTOM, Image.ROTATE_90, Image.ROTATE_180,
          Image.ROTATE_270, Image.TRANSPOSE, Image.TRANSVERSE):
    r = src.transpose(t)
    print("transpose", t, r.size, h(r))

for angle, expand in ((90, True), (180, False), (270, True), (90, False), (30, False), (45, True), (-20, True)):
    r = rgb.rotate(angle, expand=expand)
    print("rotate", angle, expand, r.size, h(r))
print("rotate fill", h(gray.rotate(33, fillcolor=200)))

for size in ((5, 3), (22, 14), (11, 3), (4, 7), (17, 7)):
    print("nearest", size, h(rgb.resize(size, Image.NEAREST)), h(gray.resize(size, Image.Resampling.NEAREST)))
    print("bilinear", size, h(rgb.resize(size, Image.BILINEAR)), h(gray.resize(size, Image.BILINEAR)))
print("box", h(rgb.resize((4, 4), Image.BILINEAR, box=(1, 1, 9, 6))))

t = rgb.copy()
t.thumbnail((6, 6))
print("thumb", t.size)
t = Image.new("L", (300, 200))
t.thumbnail((128, 128))
print("thumb", t.size)

dst = Image.new("RGB", (8, 8), (9, 9, 9))
dst.paste(rgb.crop((0, 0, 4, 4)), (2, 3))
print("paste", h(dst))
dst.paste((200, 100, 50), (0, 0, 3, 2))
print("paste color", h(dst))
dst.paste(src.crop((0, 0, 5, 5)), (4, 4), src.crop((0, 0, 5, 5)))
print("paste mask", h(dst))
dst.paste(gray.crop((1, 1, 4, 4)), (-1, -1))
print("paste gray", h(dst), dst.getpixel((0, 0)))
mask = gray.crop((0, 0, 8, 8))
dst.paste((255, 255, 255), (0, 0, 8, 8), mask)
print("paste color mask", h(dst))

r, g, b, a = src.split()
print("split", r.mode, h(r), h(g), h(b), h(a))
m = Image.merge("RGB", (b, g, r))
print("merge", m.mode, h(m), m.getpixel((2, 2)))
print("channel", h(src.getchannel("A")), h(src.getchannel(1)))

raw = rgb.tobytes()
print("bytes", len(raw), h(Image.frombytes("RGB", rgb.size, raw)))

z = Image.new("RGB", (10, 10))
print("bbox empty", z.getbbox())
z.putpixel((3, 4), (0, 0, 1))
z.putpixel((7, 2), (5, 0, 0))
print("bbox", z.getbbox())
za = Image.new("RGBA", (10, 10), (50, 50, 50, 0))
za.putpixel((6, 6), (0, 0, 0, 9))
print("bbox alpha", za.getbbox(), za.getbbox(alpha_only=False))

print("point", h(gray.point(lambda v: 255 - v)), h(rgb.point(lambda v: v // 2)))
lut = [255 - i for i in range(256)] + list(range(256)) + [i // 3 for i in range(256)]
print("point lut", h(rgb.point(lut)))
print("data", list(gray.getdata())[:5], rgb.getdata()[3], gray.getextrema(), rgb.getextrema())
print("hist", sum(gray.histogram()), gray.histogram()[gray.getpixel((0, 0))])
print("eq", rgb == rgb.copy(), rgb == gray)
