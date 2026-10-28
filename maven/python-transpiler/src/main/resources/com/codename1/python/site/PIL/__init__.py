# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""A Pillow subset: images in "L", "RGB" and "RGBA" with PNG built in.

Pixels live in the runtime's `_cn1image` module, which also does every loop
over them. PNG is read and written here over zlib, so it works on servers as
well as devices; JPEG and the other formats go through Codename One's own
image codecs and need a running display.
"""

__version__ = "11.0.0"


class UnidentifiedImageError(OSError):
    pass
