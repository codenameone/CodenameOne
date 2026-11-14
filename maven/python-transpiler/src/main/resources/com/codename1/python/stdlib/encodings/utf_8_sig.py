# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""encodings.utf_8_sig: the utf-8-sig codec."""

import codecs

_info = codecs.lookup("utf-8-sig")

IncrementalEncoder = _info.incrementalencoder
IncrementalDecoder = _info.incrementaldecoder


def encode(input, errors="strict"):
    return _info.encode(input, errors)


def decode(input, errors="strict"):
    return _info.decode(input, errors)


def getregentry():
    return _info
