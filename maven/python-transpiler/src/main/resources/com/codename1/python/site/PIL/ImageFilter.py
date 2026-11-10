# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Pillow's ImageFilter: a box blur of a whole-number radius."""

import _cn1image


class Filter:
    pass


class BoxBlur(Filter):
    name = "BoxBlur"

    def __init__(self, radius):
        if radius < 0:
            raise ValueError("radius must be >= 0")
        if radius != int(radius):
            raise ValueError("only whole-number BoxBlur radii are supported")
        self.radius = int(radius)

    def filter(self, image):
        if self.radius == 0:
            return image.copy()
        return image._new(_cn1image.boxblur(image._im, self.radius))
