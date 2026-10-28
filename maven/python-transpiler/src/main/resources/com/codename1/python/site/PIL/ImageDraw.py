# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Pillow's ImageDraw: points, lines, rectangles, ellipses and polygons.

Points, rectangles and horizontal or vertical lines match Pillow pixel for
pixel. Diagonal lines are Bresenham's, and ellipses and polygons are filled
by pixel centre, so their edges can differ from Pillow's by a pixel. Nothing
is antialiased. Text is drawn with the built-in bitmap font (see ImageFont).
"""

import _cn1image
from PIL import Image as _Image


def _points(xy):
    xy = list(xy)
    if xy and not isinstance(xy[0], (tuple, list)):
        return [(xy[i], xy[i + 1]) for i in range(0, len(xy), 2)]
    return [tuple(p) for p in xy]


def _box(xy):
    p = _points(xy)
    (x0, y0), (x1, y1) = p[0], p[1]
    if x1 < x0:
        raise ValueError("x1 must be greater than or equal to x0")
    if y1 < y0:
        raise ValueError("y1 must be greater than or equal to y0")
    return int(x0), int(y0), int(x1), int(y1)


class ImageDraw:
    def __init__(self, im, mode=None):
        self.im = im
        self.mode = im.mode
        self.ink = _Image._color(im.mode, -1)
        self.fill = False
        self.font = None
        self.fontmode = "1" if im.mode in ("1", "P", "I", "F") else "L"

    def _ink(self, color):
        return None if color is None else _Image._color(self.mode, color)

    def _rect(self, x0, y0, x1, y1, ink):
        # inclusive corners
        _cn1image.fill(self.im._im, x0, y0, x1 + 1, y1 + 1, ink, None)

    def _inks(self, outline, fill):
        ink = self._ink(outline)
        fill_ink = self._ink(fill)
        if ink is None and fill_ink is None:
            if self.fill:
                fill_ink = self.ink
            else:
                ink = self.ink
        return ink, fill_ink

    def point(self, xy, fill=None):
        ink = self._ink(fill) if fill is not None else self.ink
        for x, y in _points(xy):
            x, y = int(x), int(y)
            self._rect(x, y, x, y, ink)

    def _segment(self, x0, y0, x1, y1, ink, width):
        if y0 == y1 or x0 == x1:
            if x1 < x0:
                x0, x1 = x1, x0
            if y1 < y0:
                y0, y1 = y1, y0
            lo = (width - 1) // 2
            hi = width - 1 - lo
            if y0 == y1:
                self._rect(x0, y0 - lo, x1, y0 + hi, ink)
            else:
                self._rect(x0 - lo, y0, x0 + hi, y1, ink)
            return
        dx = abs(x1 - x0)
        dy = -abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx + dy
        while True:
            self._rect(x0, y0, x0, y0, ink)
            if x0 == x1 and y0 == y1:
                return
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def line(self, xy, fill=None, width=0, joint=None):
        ink = self._ink(fill) if fill is not None else self.ink
        p = [(int(x), int(y)) for x, y in _points(xy)]
        for i in range(len(p) - 1):
            self._segment(p[i][0], p[i][1], p[i + 1][0], p[i + 1][1], ink, max(1, width))

    def rectangle(self, xy, fill=None, outline=None, width=1):
        x0, y0, x1, y1 = _box(xy)
        ink, fill_ink = self._inks(outline, fill)
        if fill_ink is not None:
            self._rect(x0, y0, x1, y1, fill_ink)
        if ink is not None and ink != fill_ink and width != 0:
            for i in range(width):
                if x0 + i > x1 - i or y0 + i > y1 - i:
                    break
                self._rect(x0 + i, y0 + i, x1 - i, y0 + i, ink)
                self._rect(x0 + i, y1 - i, x1 - i, y1 - i, ink)
                self._rect(x0 + i, y0 + i, x0 + i, y1 - i, ink)
                self._rect(x1 - i, y0 + i, x1 - i, y1 - i, ink)

    def _spans(self, x0, y0, x1, y1):
        cx = (x0 + x1 + 1) / 2.0
        cy = (y0 + y1 + 1) / 2.0
        rx = (x1 - x0 + 1) / 2.0
        ry = (y1 - y0 + 1) / 2.0
        for y in range(y0, y1 + 1):
            t = (y + 0.5 - cy) / ry
            if t * t > 1:
                continue
            half = rx * (1 - t * t) ** 0.5
            a = int(cx - half + 0.5)
            b = int(cx + half - 0.5)
            if b >= a:
                yield y, a, b

    def ellipse(self, xy, fill=None, outline=None, width=1):
        x0, y0, x1, y1 = _box(xy)
        ink, fill_ink = self._inks(outline, fill)
        spans = list(self._spans(x0, y0, x1, y1))
        if fill_ink is not None:
            for y, a, b in spans:
                self._rect(a, y, b, y, fill_ink)
        if ink is not None and ink != fill_ink and width != 0:
            inner = {}
            if x1 - x0 >= 2 * width and y1 - y0 >= 2 * width:
                for y, a, b in self._spans(x0 + width, y0 + width, x1 - width, y1 - width):
                    inner[y] = (a, b)
            for y, a, b in spans:
                if y in inner:
                    ia, ib = inner[y]
                    self._rect(a, y, ia - 1, y, ink)
                    self._rect(ib + 1, y, b, y, ink)
                else:
                    self._rect(a, y, b, y, ink)

    def polygon(self, xy, fill=None, outline=None, width=1):
        p = [(float(x), float(y)) for x, y in _points(xy)]
        ink, fill_ink = self._inks(outline, fill)
        if fill_ink is not None and len(p) > 2:
            ys = [q[1] for q in p]
            for y in range(int(min(ys)), int(max(ys)) + 1):
                yc = y + 0.5
                xs = []
                for i in range(len(p)):
                    (ax, ay), (bx, by) = p[i], p[(i + 1) % len(p)]
                    if (ay <= yc < by) or (by <= yc < ay):
                        xs.append(ax + (yc - ay) * (bx - ax) / (by - ay))
                xs.sort()
                for i in range(0, len(xs) - 1, 2):
                    a = int(xs[i] + 0.5)
                    b = int(xs[i + 1] - 0.5)
                    if b >= a:
                        self._rect(a, y, b, y, fill_ink)
        if ink is not None and ink != fill_ink:
            q = [(int(x), int(y)) for x, y in p]
            for i in range(len(q)):
                a, b = q[i], q[(i + 1) % len(q)]
                self._segment(a[0], a[1], b[0], b[1], ink, max(1, width))

    def getfont(self):
        if self.font is None:
            from PIL import ImageFont
            self.font = ImageFont.load_default()
        return self.font

    def _font(self, font, font_size=None):
        if font is not None:
            return font
        if font_size is not None:
            from PIL import ImageFont
            return ImageFont.load_default(font_size)
        return self.getfont()

    def _spacing(self, font, spacing, stroke_width=0):
        return self.textbbox((0, 0), "A", font, stroke_width=stroke_width)[3] + stroke_width + spacing

    @staticmethod
    def _scalable(font):
        return hasattr(font, "getmask2")

    def text(self, xy, text, fill=None, font=None, anchor=None, spacing=4, align="left",
             direction=None, features=None, language=None, stroke_width=0, stroke_fill=None,
             embedded_color=False, *args, **kwargs):
        """Draws `text`. A TrueType font's coverage mask is filled with the
        ink at the anchor's integer position, as Pillow does; the bitmap
        font's glyph mask likewise, at the top-left corner."""
        font = self._font(font, kwargs.get("font_size"))
        if isinstance(text, bytes):
            text = text.decode("latin-1")
        if "\n" in text:
            return self.multiline_text(xy, text, fill, font, anchor, spacing, align,
                                       direction, features, language, stroke_width)
        ink = self._ink(fill)
        if ink is None:
            ink = self.ink
        x, y = int(xy[0]), int(xy[1])
        if self._scalable(font):
            mask, offset = font.getmask2(text, self.fontmode, direction, features, language,
                                         anchor=anchor)
            x += offset[0]
            y += offset[1]
        else:
            mask = font.getmask(text, self.mode)
        if mask.width == 0 or mask.height == 0:
            return
        self.im.paste(ink, (x, y, x + mask.width, y + mask.height), mask)

    def _lines(self, xy, text, font, anchor, spacing, align, stroke_width=0):
        if anchor is None:
            anchor = "la"
        elif len(anchor) != 2:
            raise ValueError("anchor must be a 2 character string")
        elif anchor[1] in "tb":
            raise ValueError("anchor not supported for multiline text")
        lines = text.split("\n")
        line_spacing = self._spacing(font, spacing, stroke_width)
        widths = [self.textlength(line, font) for line in lines]
        widest = max(widths)
        top = xy[1]
        if anchor[1] == "m":
            top -= (len(lines) - 1) * line_spacing / 2.0
        elif anchor[1] == "d":
            top -= (len(lines) - 1) * line_spacing
        out = []
        for line, w in zip(lines, widths):
            left = xy[0]
            diff = widest - w
            if anchor[0] == "m":
                left -= diff / 2.0
            elif anchor[0] == "r":
                left -= diff
            if align == "center":
                left += diff / 2.0
            elif align == "right":
                left += diff
            elif align != "left":
                raise ValueError('align must be "left", "center" or "right"')
            out.append(((left, top), line))
            top += line_spacing
        return out, anchor

    def multiline_text(self, xy, text, fill=None, font=None, anchor=None, spacing=4, align="left",
                       direction=None, features=None, language=None, stroke_width=0, *args, **kwargs):
        font = self._font(font, kwargs.get("font_size"))
        lines, anchor = self._lines(xy, text, font, anchor, spacing, align, stroke_width)
        for pos, line in lines:
            self.text(pos, line, fill, font, anchor if self._scalable(font) else None)

    def textlength(self, text, font=None, direction=None, features=None, language=None,
                   embedded_color=False, *args, **kwargs):
        if "\n" in text:
            raise ValueError("can't measure length of multiline text")
        font = self._font(font, kwargs.get("font_size"))
        if self._scalable(font):
            return font.getlength(text, self.fontmode, direction, features, language)
        return font.getlength(text)

    def textbbox(self, xy, text, font=None, anchor=None, spacing=4, align="left", direction=None,
                 features=None, language=None, stroke_width=0, embedded_color=False, *args, **kwargs):
        font = self._font(font, kwargs.get("font_size"))
        if "\n" in text:
            return self.multiline_textbbox(xy, text, font, anchor, spacing, align, direction,
                                           features, language, stroke_width)
        if self._scalable(font):
            b = font.getbbox(text, self.fontmode, direction, features, language, stroke_width, anchor)
        else:
            b = font.getbbox(text)
        return (b[0] + xy[0], b[1] + xy[1], b[2] + xy[0], b[3] + xy[1])

    def multiline_textbbox(self, xy, text, font=None, anchor=None, spacing=4, align="left",
                           direction=None, features=None, language=None, stroke_width=0,
                           *args, **kwargs):
        font = self._font(font, kwargs.get("font_size"))
        lines, anchor = self._lines(xy, text, font, anchor, spacing, align, stroke_width)
        bbox = None
        for pos, line in lines:
            b = self.textbbox(pos, line, font, anchor if self._scalable(font) else None,
                              stroke_width=stroke_width)
            if bbox is None:
                bbox = list(b)
            else:
                bbox = [min(bbox[0], b[0]), min(bbox[1], b[1]), max(bbox[2], b[2]), max(bbox[3], b[3])]
        return tuple(bbox)


def Draw(im, mode=None):
    return ImageDraw(im, mode)
