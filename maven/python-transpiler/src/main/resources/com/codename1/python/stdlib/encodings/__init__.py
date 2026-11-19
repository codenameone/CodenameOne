# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/encodings/__init__.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
"""encodings: the codec package. Codename One supports UTF-8, ASCII and
Latin-1 only (see codecs), so the package holds those codecs' modules and an
alias table naming nothing else. Code that walks the table to try every
encoding -- charset_normalizer does -- therefore tries only these."""

import codecs

from encodings import aliases as _aliases_module


class CodecRegistryError(LookupError, SystemError):
    pass


def normalize_encoding(encoding):
    if isinstance(encoding, bytes):
        encoding = str(encoding, "ascii")
    chars = []
    punct = False
    for c in encoding:
        if c.isalnum() or c == ".":
            if punct and chars:
                chars.append("_")
            if c.isascii():
                chars.append(c)
            punct = False
        else:
            punct = True
    return "".join(chars)


def search_function(encoding):
    norm = normalize_encoding(encoding).lower()
    name = _aliases_module.aliases.get(norm, norm)
    try:
        return codecs.lookup(name)
    except LookupError:
        return None
