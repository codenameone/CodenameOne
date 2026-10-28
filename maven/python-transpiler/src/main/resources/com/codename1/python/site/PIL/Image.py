# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Pillow's Image module for "L", "RGB" and "RGBA" images.

The pixel loops follow Pillow's own fixed point code (luma conversion,
bilinear resampling, nearest neighbour affine transforms, mask blending), so
pixels come out byte identical. Bicubic, Lanczos, box and Hamming resampling
are done bilinearly. PNG decoding returns "P" and "LA" files as "RGB"/"RGBA".
"""

import builtins
import math
import os
import struct
import zlib

import _cn1image
from PIL import ImageColor, UnidentifiedImageError


class Resampling:
    NEAREST = 0
    LANCZOS = 1
    BILINEAR = 2
    BICUBIC = 3
    BOX = 4
    HAMMING = 5


class Transpose:
    FLIP_LEFT_RIGHT = 0
    FLIP_TOP_BOTTOM = 1
    ROTATE_90 = 2
    ROTATE_180 = 3
    ROTATE_270 = 4
    TRANSPOSE = 5
    TRANSVERSE = 6


NEAREST = Resampling.NEAREST
LANCZOS = ANTIALIAS = Resampling.LANCZOS
BILINEAR = Resampling.BILINEAR
BICUBIC = Resampling.BICUBIC
BOX = Resampling.BOX
HAMMING = Resampling.HAMMING
FLIP_LEFT_RIGHT = Transpose.FLIP_LEFT_RIGHT
FLIP_TOP_BOTTOM = Transpose.FLIP_TOP_BOTTOM
ROTATE_90 = Transpose.ROTATE_90
ROTATE_180 = Transpose.ROTATE_180
ROTATE_270 = Transpose.ROTATE_270
TRANSPOSE = Transpose.TRANSPOSE
TRANSVERSE = Transpose.TRANSVERSE

_BANDS = {"L": 1, "RGB": 3, "RGBA": 4}
_BAND_NAMES = {"L": ("L",), "RGB": ("R", "G", "B"), "RGBA": ("R", "G", "B", "A")}
_PNG_SIG = b"\x89PNG\r\n\x1a\n"
_EXT = {".png": "PNG", ".jpg": "JPEG", ".jpeg": "JPEG", ".gif": "GIF", ".bmp": "BMP", ".webp": "WEBP"}


def _bands(mode):
    if mode not in _BANDS:
        raise ValueError("unrecognized image mode: %s (only L, RGB and RGBA are supported)" % mode)
    return _BANDS[mode]


def _color(mode, color):
    """The pixel bytes of `color` in `mode`."""
    n = _bands(mode)
    if color is None:
        return bytes(n)
    if isinstance(color, str):
        color = ImageColor.getcolor(color, mode)
    if isinstance(color, float):
        color = int(color)
    if isinstance(color, int):
        if n == 1:
            return bytes([color & 255])
        return bytes([(color >> (8 * i)) & 255 for i in range(n)])
    c = [int(v) for v in color]
    if n == 1:
        return bytes([c[0] & 255])
    if len(c) == 3 and n == 4:
        c.append(255)
    return bytes([v & 255 for v in c[:n]])


def _wrap(mode, size, handle):
    im = Image()
    im.mode = mode
    im._size = (int(size[0]), int(size[1]))
    im._im = handle
    return im


class Image:
    format = None

    def __init__(self):
        self.mode = ""
        self._size = (0, 0)
        self._im = None
        self.info = {}

    @property
    def size(self):
        return self._size

    @property
    def width(self):
        return self._size[0]

    @property
    def height(self):
        return self._size[1]

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()

    def close(self):
        pass

    def load(self):
        return None

    def getbands(self):
        return _BAND_NAMES[self.mode]

    def _new(self, handle, size=None, mode=None):
        return _wrap(mode or self.mode, size or self._size, handle)

    def copy(self):
        return self._new(_cn1image.crop(self._im, 0, 0, self.width, self.height))

    def getpixel(self, xy):
        x, y = int(xy[0]), int(xy[1])
        if x < 0:
            x += self.width
        if y < 0:
            y += self.height
        t = _cn1image.getpixel(self._im, x, y)
        return t[0] if self.mode == "L" else t

    def putpixel(self, xy, value):
        x, y = int(xy[0]), int(xy[1])
        if x < 0:
            x += self.width
        if y < 0:
            y += self.height
        _cn1image.putpixel(self._im, x, y, _color(self.mode, value))

    def getdata(self, band=None):
        raw = self.tobytes()
        n = _bands(self.mode)
        if n == 1:
            return list(raw)
        if band is not None:
            return list(raw[band::n])
        return [tuple(raw[i:i + n]) for i in range(0, len(raw), n)]

    def histogram(self):
        raw = self.tobytes()
        n = _bands(self.mode)
        h = [0] * (256 * n)
        for i, v in enumerate(raw):
            h[(i % n) * 256 + v] += 1
        return h

    def getextrema(self):
        n = _bands(self.mode)
        raw = self.tobytes()
        ext = tuple((min(raw[b::n]), max(raw[b::n])) for b in range(n))
        return ext[0] if n == 1 else ext

    def tobytes(self, encoder_name="raw", *args):
        return _cn1image.tobytes(self._im)

    def frombytes(self, data):
        self._im = _cn1image.frombytes(self.width, self.height, _bands(self.mode), data)

    def crop(self, box=None):
        if box is None:
            return self.copy()
        x0, y0, x1, y1 = (int(round(v)) for v in box)
        if x1 < x0:
            raise ValueError("Coordinate 'right' is less than 'left'")
        if y1 < y0:
            raise ValueError("Coordinate 'lower' is less than 'upper'")
        return self._new(_cn1image.crop(self._im, x0, y0, x1, y1), (x1 - x0, y1 - y0))

    def resize(self, size, resample=None, box=None, reducing_gap=None):
        w, h = int(size[0]), int(size[1])
        if box is None:
            box = (0, 0) + self._size
        if resample is None:
            resample = BICUBIC
        if (w, h) == self._size and tuple(box) == (0, 0) + self._size:
            return self.copy()
        if resample == NEAREST:
            a = [(box[2] - box[0]) / w, 0, box[0], 0, (box[3] - box[1]) / h, box[1]]
            return self._new(_cn1image.affine(self._im, w, h, a, bytes(4)), (w, h))
        return self._new(_cn1image.resample(self._im, w, h, tuple(float(v) for v in box)), (w, h))

    def thumbnail(self, size, resample=BICUBIC, reducing_gap=2.0):
        x, y = math.floor(size[0]), math.floor(size[1])
        if x >= self.width and y >= self.height:
            return
        aspect = self.width / self.height

        def round_aspect(number, key):
            return max(min(math.floor(number), math.ceil(number), key=key), 1)

        if x / y >= aspect:
            x = round_aspect(y * aspect, key=lambda n: abs(aspect - n / y))
        else:
            y = round_aspect(x / aspect, key=lambda n: 0 if n == 0 else abs(aspect - x / n))
        im = self.resize((x, y), resample)
        self._im = im._im
        self._size = im._size

    def transpose(self, method):
        h = _cn1image.transpose(self._im, int(method))
        w, ht = self._size
        size = (ht, w) if method in (ROTATE_90, ROTATE_270, TRANSPOSE, TRANSVERSE) else (w, ht)
        return self._new(h, size)

    def transform(self, size, method, data=None, resample=NEAREST, fill=1, fillcolor=None):
        if method != 0:
            raise ValueError("only AFFINE transforms are supported")
        w, h = int(size[0]), int(size[1])
        return self._new(_cn1image.affine(self._im, w, h, [float(v) for v in data],
                                          _color(self.mode, fillcolor)), (w, h))

    def rotate(self, angle, resample=NEAREST, expand=0, center=None, translate=None, fillcolor=None):
        angle = angle % 360.0
        if not (center or translate):
            if angle == 0:
                return self.copy()
            if angle == 180:
                return self.transpose(ROTATE_180)
            if angle in (90, 270) and (expand or self.width == self.height):
                return self.transpose(ROTATE_90 if angle == 90 else ROTATE_270)
        w, h = self.size
        post_trans = (0, 0) if translate is None else translate
        if center is None:
            center = (w / 2, h / 2)
        angle = -math.radians(angle)
        matrix = [round(math.cos(angle), 15), round(math.sin(angle), 15), 0.0,
                  round(-math.sin(angle), 15), round(math.cos(angle), 15), 0.0]

        def transform(x, y, m):
            a, b, c, d, e, f = m
            return a * x + b * y + c, d * x + e * y + f

        matrix[2], matrix[5] = transform(-center[0] - post_trans[0], -center[1] - post_trans[1], matrix)
        matrix[2] += center[0]
        matrix[5] += center[1]
        if expand:
            xx = []
            yy = []
            for x, y in ((0, 0), (w, 0), (w, h), (0, h)):
                tx, ty = transform(x, y, matrix)
                xx.append(tx)
                yy.append(ty)
            nw = math.ceil(max(xx)) - math.floor(min(xx))
            nh = math.ceil(max(yy)) - math.floor(min(yy))
            matrix[2], matrix[5] = transform(-(nw - w) / 2.0, -(nh - h) / 2.0, matrix)
            w, h = nw, nh
        return self.transform((w, h), 0, matrix, resample, fillcolor=fillcolor)

    def convert(self, mode=None, matrix=None, dither=None, palette=None, colors=256):
        if mode is None or mode == self.mode:
            return self.copy()
        return self._new(_cn1image.convert(self._im, _bands(mode)), mode=mode)

    def paste(self, im, box=None, mask=None):
        if box is not None and len(box) == 2 and isinstance(im, Image):
            box = tuple(box) + (box[0] + im.width, box[1] + im.height)
        if box is None:
            box = (0, 0) + (im.size if isinstance(im, Image) else self._size)
        x0, y0 = int(box[0]), int(box[1])
        m = None if mask is None else mask._im
        if isinstance(im, Image):
            if len(box) == 4 and (box[2] - box[0], box[3] - box[1]) != im.size:
                raise ValueError("images do not match")
            if im.mode != self.mode:
                im = im.convert(self.mode)
            _cn1image.paste(self._im, im._im, x0, y0, m)
        else:
            x1, y1 = (int(box[2]), int(box[3])) if len(box) == 4 else (mask.width + x0, mask.height + y0)
            _cn1image.fill(self._im, x0, y0, x1, y1, _color(self.mode, im), m)

    def alpha_composite(self, im, dest=(0, 0), source=(0, 0)):
        raise NotImplementedError("alpha_composite is not supported")

    def split(self):
        if self.mode == "L":
            return (self.copy(),)
        return tuple(self._new(_cn1image.band(self._im, b), mode="L") for b in range(_bands(self.mode)))

    def getchannel(self, channel):
        if isinstance(channel, str):
            channel = self.getbands().index(channel)
        return self._new(_cn1image.band(self._im, channel), mode="L")

    def putalpha(self, alpha):
        if self.mode != "RGBA":
            rgba = self.convert("RGBA")
            self.mode, self._im = "RGBA", rgba._im
        if not isinstance(alpha, Image):
            alpha = new("L", self._size, alpha)
        r, g, b, _ = self.split()
        self._im = merge("RGBA", (r, g, b, alpha))._im

    def getbbox(self, alpha_only=True):
        return _cn1image.getbbox(self._im, alpha_only)

    def point(self, lut, mode=None):
        n = _bands(self.mode)
        if callable(lut):
            lut = [lut(i) for i in range(256)] * n
        else:
            lut = list(lut)
            if len(lut) == 256:
                lut = lut * n
            if len(lut) != 256 * n:
                raise ValueError("wrong number of lut entries")
        table = bytes(max(0, min(255, int(v))) for v in lut)
        return self._new(_cn1image.point(self._im, table))

    def filter(self, f):
        if isinstance(f, type):
            f = f()
        return f.filter(self)

    def save(self, fp, format=None, **params):
        filename = None
        if isinstance(fp, (str, os.PathLike)):
            filename = os.fspath(fp)
        if format is None:
            ext = os.path.splitext(filename or "")[1].lower()
            if ext not in _EXT:
                raise ValueError("unknown file extension: " + ext)
            format = _EXT[ext]
        format = format.upper()
        if format == "PNG":
            data = _png_encode(self)
        else:
            data = _native_encode(self, format, params.get("quality", 75))
        if filename is not None:
            with builtins.open(filename, "wb") as f:
                f.write(data)
        else:
            fp.write(data)

    def show(self, title=None):
        pass

    def __eq__(self, other):
        return (isinstance(other, Image) and self.mode == other.mode
                and self.size == other.size and self.tobytes() == other.tobytes())

    def __repr__(self):
        return "<PIL.Image.Image image mode=%s size=%dx%d>" % (self.mode, self.width, self.height)


def new(mode, size, color=0):
    w, h = int(size[0]), int(size[1])
    return _wrap(mode, (w, h), _cn1image.new(w, h, _bands(mode), _color(mode, color)))


def frombytes(mode, size, data, decoder_name="raw", *args):
    w, h = int(size[0]), int(size[1])
    return _wrap(mode, (w, h), _cn1image.frombytes(w, h, _bands(mode), data))


def merge(mode, bands):
    if len(bands) != _bands(mode):
        raise ValueError("wrong number of bands")
    for b in bands:
        if b.mode != "L":
            raise ValueError("mode mismatch")
    if mode == "L":
        return bands[0].copy()
    return _wrap(mode, bands[0].size, _cn1image.merge([b._im for b in bands]))


def open(fp, mode="r", formats=None):
    if isinstance(fp, (str, os.PathLike)):
        with builtins.open(os.fspath(fp), "rb") as f:
            data = f.read()
    else:
        data = fp.read()
    if data[:8] == _PNG_SIG:
        im = _png_decode(data)
        im.format = "PNG"
        return im
    kind = None
    if data[:3] == b"\xff\xd8\xff":
        kind = "JPEG"
    elif data[:4] == b"GIF8":
        kind = "GIF"
    elif data[:2] == b"BM":
        kind = "BMP"
    elif data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        kind = "WEBP"
    if kind is None:
        raise UnidentifiedImageError("cannot identify image file %r" % (fp if isinstance(fp, str) else fp,))
    if not _cn1image.native_available():
        raise OSError(kind + " needs a running Codename One display to decode; only PNG is built in")
    handle = _cn1image.native_decode(data)
    im = _wrap("RGBA", _cn1image.size(handle), handle)
    im.format = kind
    if kind in ("JPEG", "BMP"):
        im = im.convert("RGB")
        im.format = kind
    return im


def _native_encode(im, format, quality):
    if not _cn1image.native_available():
        raise OSError(format + " needs a running Codename One display to encode; only PNG is built in")
    if format == "JPEG" and im.mode == "RGBA":
        raise OSError("cannot write mode RGBA as JPEG")
    return _cn1image.native_encode(im._im, format.lower(), quality / 100.0)


def _chunk(kind, body):
    return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xffffffff)


def _png_encode(im):
    ctype = {"L": 0, "RGB": 2, "RGBA": 6}[im.mode]
    head = struct.pack(">IIBBBBB", im.width, im.height, 8, ctype, 0, 0, 0)
    body = zlib.compress(_cn1image.png_rows(im._im), 6)
    return _PNG_SIG + _chunk(b"IHDR", head) + _chunk(b"IDAT", body) + _chunk(b"IEND", b"")


def _png_decode(data):
    pos = 8
    idat = []
    plte = None
    trns = None
    head = None
    while pos + 8 <= len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            head = struct.unpack(">IIBBBBB", body)
        elif kind == b"PLTE":
            plte = bytes(body)
        elif kind == b"tRNS":
            trns = bytes(body)
        elif kind == b"IDAT":
            idat.append(body)
        elif kind == b"IEND":
            break
    if head is None:
        raise UnidentifiedImageError("cannot identify image file")
    w, h, depth, ctype, _, _, interlace = head
    if interlace:
        raise OSError("interlaced PNG files are not supported")
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    row = (w * channels * depth + 7) // 8
    bpp = max(1, channels * depth // 8)
    raw = _cn1image.png_unfilter(zlib.decompress(b"".join(idat)), row, h, bpp)
    if ctype != 3:
        trns = None
    handle = _cn1image.png_expand(raw, w, h, ctype, depth, plte, trns)
    mode = "L" if ctype == 0 else "RGB" if ctype == 2 or (ctype == 3 and trns is None) else "RGBA"
    return _wrap(mode, (w, h), handle)
