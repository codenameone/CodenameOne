# Pillow on the client engine (PyImageNative): Codename One's Image,
# ImageIO, Graphics and native font, here over the headless implementation
# of compat-testing. The platform decodes, scales and draws, so pixels are
# not Pillow's: the program prints only sizes, modes, round trips, colors
# within a tolerance and whether text left ink -- facts CPython answers the
# same way, so its output is the expected one.
import base64
import io

from PIL import Image, ImageDraw, ImageFont

PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAABgAAAAQCAIAAACDRijCAAAAMUlEQVR4nGM8IafBQA3ARBVTGAajQSzIHJ99IlgVbXF6Qz8XMY0aNATD"
    "iHE0rzEQAgBB5AU9DwB3PwAAAABJRU5ErkJggg==")
JPG = base64.b64decode(
    "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYU"
    "GBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wAARCAAQABgD"
    "ASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKB"
    "kaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZ"
    "mqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQF"
    "BgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5"
    "OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX"
    "2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDyzwT4J/4TH7b/AKb9j+zbP+WW/du3f7Qx939aPG3gn/hDvsX+m/bPtO//AJZbNu3b"
    "/tHP3v0o8E+Nv+EO+2/6F9s+07P+Wuzbt3f7Jz979KPG3jb/AITH7F/oX2P7Nv8A+Wu/du2/7Ix939a/MP8AaPrH9z5dvv3P27/jP/8AX/8A6kn/"
    "AHA/58f+Dv439chy9FFFd5+5H//Z")


def near(a, b, tol):
    return abs(a - b) <= tol


def rgb_near(px, ref, tol):
    return all(near(a, b, tol) for a, b in zip(px[:3], ref))


RED = (200, 30, 40)
GREEN = (20, 220, 60)

png = Image.open(io.BytesIO(PNG))
print("png", png.format, png.mode, png.size)
print("png red", rgb_near(png.getpixel((1, 1)), RED, 8))
print("png green", rgb_near(png.getpixel((7, 7)), GREEN, 8))

jpg = Image.open(io.BytesIO(JPG))
print("jpg", jpg.format, jpg.mode, jpg.size)
print("jpg red", rgb_near(jpg.getpixel((1, 1)), RED, 24))

for mode in ("L", "LA", "RGBA", "RGB"):
    c = png.convert(mode)
    print("convert", c.mode, c.size, c.getbands())
print("luma", near(png.convert("L").getpixel((1, 1)), 82, 4))

small = png.resize((12, 8))
print("resize", small.mode, small.size, rgb_near(small.getpixel((0, 0)), RED, 24))
big = jpg.convert("RGBA").resize((48, 32))
print("resize", big.mode, big.size, rgb_near(big.getpixel((2, 2)), RED, 32))
gray = png.convert("L").resize((6, 4))
print("resize", gray.mode, gray.size)
crop = png.resize((8, 8), box=(4, 4, 12, 12))
print("resize box", crop.size, rgb_near(crop.getpixel((4, 4)), GREEN, 24))
thumb = png.copy()
thumb.thumbnail((6, 6))
print("thumbnail", thumb.size)

for fmt in ("PNG", "JPEG"):
    out = io.BytesIO()
    png.save(out, fmt)
    data = out.getvalue()
    back = Image.open(io.BytesIO(data))
    print("save", fmt, len(data) > 0, back.format, back.mode, back.size)
    print("round trip", fmt, rgb_near(back.convert("RGB").getpixel((1, 1)), RED, 24))
out = io.BytesIO()
png.convert("RGBA").save(out, "PNG")
print("save RGBA", Image.open(io.BytesIO(out.getvalue())).mode)


def text_checks(name, f):
    w = f.getlength("Hello")
    print(name, "length", w > 0, f.getlength("Hello Hello") > w)
    x0, y0, x1, y1 = f.getbbox("Hello")
    print(name, "bbox", x1 > x0, y1 > y0)
    asc, desc = f.getmetrics()
    print(name, "metrics", asc > 0, desc >= 0)
    canvas = Image.new("RGB", (200, 60), "white")
    d = ImageDraw.Draw(canvas)
    bx0, by0, bx1, by1 = d.textbbox((4, 4), "Hello", font=f)
    print(name, "textbbox", bx1 > bx0, by1 > by0, bx0 >= 4)
    print(name, "textlength", d.textlength("Hello", font=f) == w)
    d.text((4, 4), "Hello", fill="black", font=f)
    dark = sum(1 for y in range(60) for x in range(200) if canvas.getpixel((x, y))[0] < 128)
    print(name, "ink", dark > 0)
    mask = f.getmask("Hi")
    print(name, "mask", mask.mode, mask.size[0] > 0, mask.size[1] > 0)


text_checks("default", ImageFont.load_default(18))
# CPython hands back the TrueType bytes it loaded the default from; this
# runtime keeps none, and the client engine draws its native font whatever
# bytes truetype() is given -- so either way this is a real truetype() call.
data = getattr(ImageFont.load_default(), "font_bytes", b"")
text_checks("truetype", ImageFont.truetype(io.BytesIO(data), 18))
