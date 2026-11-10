# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""A Pillow subset: images in "L", "RGB" and "RGBA" with PNG and JPEG built in.

Pixels live in the runtime's `_cn1image` module, which also does every loop
over them. Codecs, resampling and text go through an engine the build picks:
on a server the backend's pure-Java `com.codename1.backend.image`, which
gives Pillow's exact pixels for resampling, PNG, JPEG and TrueType text; on
a device Codename One's `Image`, `ImageIO`, `Graphics` and `Font`, which
decode, encode, scale and draw text the platform's way, and also read GIF,
BMP and WEBP.
"""

__version__ = "11.0.0"


class UnidentifiedImageError(OSError):
    pass
