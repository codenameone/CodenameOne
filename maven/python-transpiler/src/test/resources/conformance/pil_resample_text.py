# Pillow: BOX, BILINEAR, HAMMING, BICUBIC and LANCZOS resampling (two-pass
# convolution, premultiplied for LA and RGBA), text in the built-in bitmap
# font, and PNGs that open as P, LA and 1, interlaced (Adam7) ones included.
import hashlib
import io

from PIL import Image, ImageDraw, ImageFont

IL_RGB = '89504e470d0a1a0a0000000d494844520000000b000000070802000001260b109c000000ac49444154789c7d8d210e02311444a70ab7aa2488e69b35cd3adc26fc04b7ba62150255835bf90f51fd756f80e91110d59c804be010740d0143f2269964663200909b42130441962c0ff8d5a302c6cb18b0340086671778149e332f95934169cdf91bd8b5870824b414c672e7e13ec08a6399a3a424b7222f63cbf0b307c1d2c653c7b40de422f54243a27da6b1d0b1d264706a27db3f000aabbdd789f5123445bd8ade933eb3ee8a1eaa9edf9c22367ca460c6f60000000049454e44ae426082'
IL_PAL = '89504e470d0a1a0a0000000d49484452000000090000000a0403000001e32c591b0000000f504c5445000000ff000000ff000000ffc86432d7a3ae240000000374524e53ff80007f6d68780000003e49444154789c15c84b0d00200cc0d0f21140c0c0080818090246f0ef8971e83b94c4c210aed02964668316cd4327f1fc7194bac3303e05a72ace0627c803c53f065534c8b06a0000000049454e44ae426082'
IL_LA = '89504e470d0a1a0a0000000d494844520000000500000003080400000186383de50000002a49444154789c05c1410100300c02b19ba56ac20b0e6a884f5db104ca15152c9f996edf584026cae6f20123a71092c6c370780000000049454e44ae426082'


def _save(im):
    b = io.BytesIO()
    im.save(b, "PNG")
    return b.getvalue()


def digest(im):
    return hashlib.sha256(im.tobytes()).hexdigest()[:16]


def gradient(mode, w, h):
    im = Image.new(mode, (w, h))
    n = len(im.getbands())
    for y in range(h):
        for x in range(w):
            v = [(x * 37 + y * 11) & 255, (x * 5 + y * 29) & 255, (x * y * 3 + 17) & 255,
                 (255 - x * 13 - y * 7) & 255]
            if mode == "LA":
                v = [v[0], v[3]]
            im.putpixel((x, y), v[0] if n == 1 else tuple(v[:n]))
    return im


# Resampling: every filter, down and up, on L, RGB and RGBA (premultiplied)
FILTERS = [("NEAREST", Image.Resampling.NEAREST), ("BOX", Image.Resampling.BOX),
           ("BILINEAR", Image.Resampling.BILINEAR), ("HAMMING", Image.Resampling.HAMMING),
           ("BICUBIC", Image.Resampling.BICUBIC), ("LANCZOS", Image.Resampling.LANCZOS)]
for mode in ("L", "LA", "RGB", "RGBA"):
    src = gradient(mode, 23, 17)
    for name, f in FILTERS:
        for size in ((7, 5), (40, 31), (23, 9)):
            out = src.resize(size, f)
            print(mode, name, size, out.mode, out.size, digest(out))
small = gradient("RGB", 6, 4)
print(list(small.resize((3, 2), Image.LANCZOS).getdata()))
print(list(small.resize((9, 5), Image.BICUBIC).getdata())[:6])
print(small.resize((4, 3), Image.BICUBIC, box=(1, 0.5, 5, 3.5)).getpixel((1, 1)))
print(gradient("RGB", 40, 30).resize((8, 6)).getpixel((3, 3)))
t = gradient("RGB", 50, 20)
t.thumbnail((16, 16))
print(t.size, digest(t))

# Text with the built-in bitmap font
font = ImageFont.load_default_imagefont()
im = Image.new("RGB", (90, 40), "white")
d = ImageDraw.Draw(im)
print(d.textbbox((3, 4), "Hello, PIL!", font=font), d.textlength("Hello, PIL!", font=font))
d.text((3, 4), "Hello, PIL!", fill=(200, 0, 0), font=font)
d.text((-2, 20), "Wrap\nlines", fill="blue", font=font)
print(d.multiline_textbbox((-2, 20), "Wrap\nlines", font=font))
print(digest(im), im.getbbox(), im.getpixel((4, 7)))
g = Image.new("L", (30, 12))
ImageDraw.Draw(g).text((1, 0), "ab", fill=255, font=font)
for y in range(g.height):
    print("".join("#" if g.getpixel((x, y)) else "." for x in range(16)))
r = Image.new("RGBA", (30, 30), (0, 0, 0, 0))
ImageDraw.Draw(r).multiline_text((2, 2), "x\nyz", fill=(0, 255, 0, 128), font=font, align="right")
print(digest(r))

# Modes kept by Image.open, and interlaced files
for name in ("IL_RGB", "IL_PAL", "IL_LA"):
    im = Image.open(io.BytesIO(bytes.fromhex(globals()[name])))
    print(name, im.mode, im.size, im.info.get("transparency"), digest(im))
    print(" ", im.getpixel((0, 0)), im.getpixel((im.width - 1, im.height - 1)), im.getpalette())
    print(" ", digest(im.convert("RGBA")), digest(im.convert("RGB")), digest(im.convert("L")))
    back = Image.open(io.BytesIO(_save(im)))
    print(" ", back.mode, back.info.get("transparency"), digest(back) == digest(im))
p = Image.open(io.BytesIO(bytes.fromhex(IL_PAL)))
print(p.resize((4, 4), Image.BICUBIC).mode, digest(p.crop((1, 1, 5, 5))), p.crop((0, 0, 2, 2)).getpalette()[:6])
one = Image.open(io.BytesIO(_save(gradient("L", 12, 5).convert("1", dither=Image.Dither.NONE))))
print(one.mode, digest(one), one.getpixel((3, 1)), digest(one.convert("L")))
la = gradient("LA", 5, 4)
print(la.getpixel((2, 2)), digest(la.convert("RGBA")), digest(la.convert("L")), digest(gradient("RGBA", 4, 3).convert("LA")))
print(one.tobytes().hex(), Image.frombytes("1", one.size, one.tobytes()).tobytes() == one.tobytes())
