# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""A Pillow subset: images in "L", "RGB" and "RGBA" with PNG and JPEG built in.

Pixels live in the runtime's `_cn1image` module, which also does every loop
over them. Resampling and the PNG and JPEG codecs are Codename One's pure-Java
`com.codename1.image` package, so they work on servers as well as devices;
GIF, BMP and WEBP go through the platform's image codecs and need a running
display.
"""

__version__ = "11.0.0"


class UnidentifiedImageError(OSError):
    pass
