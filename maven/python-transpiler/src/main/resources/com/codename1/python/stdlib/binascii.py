# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Conversions between binary data and ASCII encodings."""

__all__ = ["Error", "Incomplete", "hexlify", "unhexlify", "b2a_hex", "a2b_hex", "b2a_base64",
           "a2b_base64", "crc32", "crc_hqx"]


class Error(ValueError):
    pass


class Incomplete(Exception):
    pass


Error.__module__ = "binascii"
Incomplete.__module__ = "binascii"

_B64 = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
_B64_REV = [-1] * 256
for _i in range(64):
    _B64_REV[_B64[_i]] = _i
_HEX = b"0123456789abcdef"


def _bytes(data, name):
    if isinstance(data, str):
        try:
            return data.encode("ascii")
        except UnicodeEncodeError:
            raise ValueError("string argument should contain only ASCII characters")
    if isinstance(data, (bytes, bytearray, memoryview)):
        return bytes(data)
    raise TypeError("a bytes-like object is required, not '%s'" % type(data).__name__)


def hexlify(data, sep=None, bytes_per_sep=1):
    if isinstance(data, str):
        raise TypeError("a bytes-like object is required, not 'str'")
    data = _bytes(data, "data")
    if sep is None:
        return data.hex().encode("ascii")
    if isinstance(sep, bytes):
        sep = sep.decode("ascii")
    return data.hex(sep, bytes_per_sep).encode("ascii")


b2a_hex = hexlify


def unhexlify(hexstr):
    hexstr = _bytes(hexstr, "hexstr")
    if len(hexstr) % 2:
        raise Error("Odd-length string")
    try:
        return bytes.fromhex(hexstr.decode("ascii").replace(" ", "!"))
    except ValueError:
        raise Error("Non-hexadecimal digit found")


a2b_hex = unhexlify


def b2a_base64(data, *, newline=True):
    if isinstance(data, str):
        raise TypeError("a bytes-like object is required, not 'str'")
    data = _bytes(data, "data")
    out = bytearray()
    n = len(data)
    i = 0
    t = _B64
    while i + 2 < n:
        v = (data[i] << 16) | (data[i + 1] << 8) | data[i + 2]
        out.append(t[v >> 18])
        out.append(t[(v >> 12) & 63])
        out.append(t[(v >> 6) & 63])
        out.append(t[v & 63])
        i += 3
    rest = n - i
    if rest == 1:
        v = data[i] << 16
        out += bytes((t[v >> 18], t[(v >> 12) & 63], 61, 61))
    elif rest == 2:
        v = (data[i] << 16) | (data[i + 1] << 8)
        out += bytes((t[v >> 18], t[(v >> 12) & 63], t[(v >> 6) & 63], 61))
    if newline:
        out.append(10)
    return bytes(out)


def a2b_base64(data, *, strict_mode=False):
    data = _bytes(data, "data")
    out = bytearray()
    rev = _B64_REV
    acc = 0
    bits = 0
    quad = 0
    pads = 0
    for i, c in enumerate(data):
        if c == 61:
            if strict_mode and quad == 0:
                raise Error("Excess padding not allowed" if i else "Leading padding not allowed")
            if quad >= 2:
                pads += 1
                if quad + pads >= 4:
                    if strict_mode and i != len(data) - 1:
                        raise Error("Excess data after padding")
                    acc = bits = quad = pads = 0
                    if not strict_mode:
                        continue
                    break
            elif strict_mode:
                raise Error("Discontinuous padding not allowed")
            continue
        v = rev[c]
        if v < 0:
            if strict_mode:
                raise Error("Only base64 data is allowed")
            continue
        if strict_mode and pads:
            raise Error("Discontinuous padding not allowed")
        pads = 0
        acc = (acc << 6) | v
        bits += 6
        quad = (quad + 1) % 4
        if bits >= 8:
            bits -= 8
            out.append((acc >> bits) & 255)
            acc &= (1 << bits) - 1
    if quad != 0:
        if quad == 1:
            raise Error("Invalid base64-encoded string: number of data characters (%d) cannot be "
                        "1 more than a multiple of 4" % (len([c for c in data if rev[c] >= 0])))
        raise Error("Incorrect padding")
    return bytes(out)


_CRC_TABLE = []
for _n in range(256):
    _c = _n
    for _k in range(8):
        _c = (0xEDB88320 ^ (_c >> 1)) if _c & 1 else _c >> 1
    _CRC_TABLE.append(_c)


def crc32(data, crc=0):
    data = _bytes(data, "data")
    crc = ~crc & 0xFFFFFFFF
    t = _CRC_TABLE
    for b in data:
        crc = t[(crc ^ b) & 0xFF] ^ (crc >> 8)
    return ~crc & 0xFFFFFFFF


def crc_hqx(data, value):
    data = _bytes(data, "data")
    crc = value & 0xFFFF
    for b in data:
        crc ^= b << 8
        for _ in range(8):
            crc = ((crc << 1) ^ 0x1021) & 0xFFFF if crc & 0x8000 else (crc << 1) & 0xFFFF
    return crc
