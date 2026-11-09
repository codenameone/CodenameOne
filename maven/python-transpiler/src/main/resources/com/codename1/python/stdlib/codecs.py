# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The codec registry, over the encodings str.encode and bytes.decode know.

The runtime supports UTF-8, ASCII and Latin-1 (and their aliases); lookup()
of anything else raises LookupError, as CPython does for an unknown name.
"""

BOM_UTF8 = b"\xef\xbb\xbf"
BOM_UTF16_LE = BOM_LE = b"\xff\xfe"
BOM_UTF16_BE = BOM_BE = b"\xfe\xff"
BOM_UTF32_LE = b"\xff\xfe\x00\x00"
BOM_UTF32_BE = b"\x00\x00\xfe\xff"
BOM = BOM_UTF16_LE
BOM_UTF16 = BOM_UTF16_LE
BOM_UTF32 = BOM_UTF32_LE

_ALIASES = {
    "utf-8": "utf-8", "utf8": "utf-8", "u8": "utf-8", "utf": "utf-8", "cp65001": "utf-8",
    "utf-8-sig": "utf-8-sig", "utf8-sig": "utf-8-sig",
    "ascii": "ascii", "us-ascii": "ascii", "646": "ascii", "us": "ascii",
    "latin-1": "iso8859-1", "latin1": "iso8859-1", "latin": "iso8859-1", "l1": "iso8859-1",
    "iso-8859-1": "iso8859-1", "iso8859-1": "iso8859-1", "8859": "iso8859-1", "cp819": "iso8859-1",
}


def _canonical(encoding):
    key = str(encoding).strip().lower().replace("_", "-").replace(" ", "-")
    name = _ALIASES.get(key)
    if name is None:
        raise LookupError(f"unknown encoding: {encoding}")
    return name


def _encode(input, errors="strict", name="utf-8"):
    if name == "utf-8-sig":
        return BOM_UTF8 + input.encode("utf-8", errors), len(input)
    return input.encode(name, errors), len(input)


def _decode(input, errors="strict", name="utf-8"):
    data = bytes(input)
    if name == "utf-8-sig":
        if data.startswith(BOM_UTF8):
            data = data[3:]
        return data.decode("utf-8", errors), len(input)
    return data.decode(name, errors), len(input)


class IncrementalEncoder:
    def __init__(self, errors="strict"):
        self.errors = errors

    def encode(self, input, final=False):
        return _encode(input, self.errors, self._name)[0]

    def reset(self):
        pass

    def getstate(self):
        return 0

    def setstate(self, state):
        pass


class IncrementalDecoder:
    def __init__(self, errors="strict"):
        self.errors = errors
        self._buffer = b""

    def decode(self, input, final=False):
        data = self._buffer + bytes(input)
        if self._name in ("utf-8", "utf-8-sig") and not final:
            # Hold back an incomplete trailing sequence for the next call.
            cut = len(data)
            i = len(data) - 1
            while i >= 0 and i >= len(data) - 4 and (data[i] & 0xC0) == 0x80:
                i -= 1
            if i >= 0 and data[i] >= 0xC0:
                need = 2 if data[i] < 0xE0 else 3 if data[i] < 0xF0 else 4
                if len(data) - i < need:
                    cut = i
            self._buffer = data[cut:]
            data = data[:cut]
        else:
            self._buffer = b""
        if self._name == "utf-8-sig":
            if not getattr(self, "_seen_bom", False) and len(data) >= 3:
                self._seen_bom = True
                if data.startswith(BOM_UTF8):
                    data = data[3:]
            return data.decode("utf-8", self.errors)
        return data.decode(self._name, self.errors)

    def reset(self):
        self._buffer = b""

    def getstate(self):
        return (self._buffer, 0)

    def setstate(self, state):
        self._buffer = state[0]


class CodecInfo(tuple):
    def __new__(cls, encode, decode, streamreader=None, streamwriter=None, incrementalencoder=None,
                incrementaldecoder=None, name=None):
        self = tuple.__new__(cls, (encode, decode, streamreader, streamwriter))
        self.name = name
        self.encode = encode
        self.decode = decode
        self.incrementalencoder = incrementalencoder
        self.incrementaldecoder = incrementaldecoder
        self.streamreader = streamreader
        self.streamwriter = streamwriter
        return self

    def __repr__(self):
        return f"<codecs.CodecInfo object for encoding {self.name}>"


def lookup(encoding):
    name = _canonical(encoding)
    enc = type("IncrementalEncoder", (IncrementalEncoder,), {"_name": name})
    dec = type("IncrementalDecoder", (IncrementalDecoder,), {"_name": name})
    return CodecInfo(lambda s, errors="strict": _encode(s, errors, name),
                     lambda b, errors="strict": _decode(b, errors, name),
                     incrementalencoder=enc, incrementaldecoder=dec, name=name)


def encode(obj, encoding="utf-8", errors="strict"):
    return lookup(encoding).encode(obj, errors)[0]


def decode(obj, encoding="utf-8", errors="strict"):
    return lookup(encoding).decode(obj, errors)[0]


def getencoder(encoding):
    return lookup(encoding).encode


def getdecoder(encoding):
    return lookup(encoding).decode


def getincrementalencoder(encoding):
    return lookup(encoding).incrementalencoder


def getincrementaldecoder(encoding):
    return lookup(encoding).incrementaldecoder


_ERRORS = {}


def register_error(name, handler):
    _ERRORS[name] = handler


def lookup_error(name):
    if name in _ERRORS:
        return _ERRORS[name]
    if name in ("strict", "ignore", "replace", "surrogateescape", "backslashreplace", "xmlcharrefreplace"):
        return name
    raise LookupError(f"unknown error handler name {name!r}")


def register(search_function):
    pass


def utf_8_decode(data, errors="strict", final=False):
    return bytes(data).decode("utf-8", errors), len(data)


def utf_8_encode(s, errors="strict"):
    return s.encode("utf-8", errors), len(s)
