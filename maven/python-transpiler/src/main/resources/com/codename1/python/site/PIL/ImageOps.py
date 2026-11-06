# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""A few of Pillow's ImageOps helpers."""

from PIL import Image


def grayscale(image):
    return image.convert("L")


def flip(image):
    return image.transpose(Image.FLIP_TOP_BOTTOM)


def mirror(image):
    return image.transpose(Image.FLIP_LEFT_RIGHT)


def invert(image):
    return image.point(lambda v: 255 - v)


def expand(image, border=0, fill=0):
    if isinstance(border, int):
        border = (border,) * 4
    left, top, right, bottom = border
    out = Image.new(image.mode, (image.width + left + right, image.height + top + bottom), fill)
    out.paste(image, (left, top))
    return out
