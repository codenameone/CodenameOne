# Pillow ImageDraw, ImageOps and ImageFilter.BoxBlur.
import hashlib

from PIL import Image, ImageDraw, ImageFilter, ImageOps


def h(im):
    return hashlib.sha256(im.tobytes()).hexdigest()[:16]


im = Image.new("RGB", (20, 16), "white")
d = ImageDraw.Draw(im)
d.rectangle([2, 3, 9, 7], fill=(255, 0, 0))
print("rect fill", h(im), im.getpixel((2, 3)), im.getpixel((9, 7)), im.getpixel((10, 7)))
d.rectangle([(11, 1), (18, 12)], outline="blue")
print("rect outline", h(im), im.getpixel((11, 5)), im.getpixel((12, 5)))
d.rectangle((1, 9, 8, 14), fill=(0, 255, 0), outline=(0, 0, 0), width=2)
print("rect both", h(im))
d.point([(0, 0), (19, 15)], fill=(1, 2, 3))
d.point((5, 0, 6, 0), fill="black")
print("point", h(im), im.getpixel((19, 15)), im.getpixel((6, 0)))
d.line([(0, 15), (19, 15)], fill=(9, 9, 9))
d.line((10, 0, 10, 15), fill=(8, 8, 8))
d.line([(0, 8), (5, 8), (5, 2)], fill=(7, 7, 7))
print("line", h(im))

g = Image.new("L", (12, 12), 0)
dg = ImageDraw.Draw(g)
dg.rectangle([0, 0, 11, 11], outline=200)
dg.rectangle([3, 3, 4, 8], fill=90)
dg.line([(6, 2), (6, 9)], fill=255)
print("gray", h(g))

a = Image.new("RGBA", (10, 10), (0, 0, 0, 0))
da = ImageDraw.Draw(a)
da.rectangle([2, 2, 5, 5], fill=(10, 20, 30, 128))
da.rectangle([4, 4, 8, 8], fill=(255, 255, 255))
print("rgba", h(a), a.getpixel((3, 3)), a.getpixel((5, 5)))

e = Image.new("L", (21, 15), 0)
de = ImageDraw.Draw(e)
de.ellipse([2, 2, 18, 12], fill=255)
dp = ImageDraw.Draw(e)
dp.polygon([(0, 0), (6, 0), (0, 6)], fill=100)
print("shapes", e.getpixel((10, 7)), e.getpixel((0, 14)), e.getpixel((1, 1)), e.getpixel((20, 0)))

src = Image.new("RGB", (9, 7))
for y in range(7):
    for x in range(9):
        src.putpixel((x, y), (x * 28, y * 36, (x * y * 7) % 256))
print("ops", h(ImageOps.grayscale(src)), ImageOps.grayscale(src).mode)
print("flip", h(ImageOps.flip(src)), h(ImageOps.mirror(src)))
print("invert", h(ImageOps.invert(src)))
print("expand", ImageOps.expand(src, 2, (1, 2, 3)).size, h(ImageOps.expand(src, 2, (1, 2, 3))))
for r in (1, 2, 3):
    print("blur", r, h(src.filter(ImageFilter.BoxBlur(r))), h(src.convert("L").filter(ImageFilter.BoxBlur(r))))
print("blur0", src.filter(ImageFilter.BoxBlur(0)) == src)
