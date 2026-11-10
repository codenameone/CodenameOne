# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Color names and `#rgb` strings, as Pillow's ImageColor reads them."""

colormap = {
    "black": (0, 0, 0), "white": (255, 255, 255), "red": (255, 0, 0),
    "lime": (0, 255, 0), "green": (0, 128, 0), "blue": (0, 0, 255),
    "yellow": (255, 255, 0), "cyan": (0, 255, 255), "aqua": (0, 255, 255),
    "magenta": (255, 0, 255), "fuchsia": (255, 0, 255), "gray": (128, 128, 128),
    "grey": (128, 128, 128), "silver": (192, 192, 192), "maroon": (128, 0, 0),
    "olive": (128, 128, 0), "navy": (0, 0, 128), "purple": (128, 0, 128),
    "teal": (0, 128, 128), "orange": (255, 165, 0), "pink": (255, 192, 203),
    "brown": (165, 42, 42), "gold": (255, 215, 0), "transparent": (0, 0, 0, 0),
}


def getrgb(color):
    c = color.lower()
    if c in colormap:
        return colormap[c]
    if c.startswith("#"):
        h = c[1:]
        if len(h) in (3, 4):
            return tuple(int(ch * 2, 16) for ch in h)
        if len(h) in (6, 8):
            return tuple(int(h[i:i + 2], 16) for i in range(0, len(h), 2))
    raise ValueError("unknown color specifier: %r" % color)


def getcolor(color, mode):
    rgb = getrgb(color)
    alpha = rgb[3] if len(rgb) == 4 else 255
    if mode == "L":
        return (rgb[0] * 299 + rgb[1] * 587 + rgb[2] * 114) // 1000
    if mode == "RGBA":
        return rgb[:3] + (alpha,)
    return rgb[:3]
