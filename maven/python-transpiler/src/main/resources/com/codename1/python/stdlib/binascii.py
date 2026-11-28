# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Conversions between binary data and ASCII encodings."""

import _cn1binascii

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
    # In Java, as CPython runs it in C.
    return _cn1binascii.b2a_base64(data, newline)


def a2b_base64(data, *, strict_mode=False):
    data = _bytes(data, "data")
    if not strict_mode:
        # None when the input ends mid-quantum: the loop below reports it.
        r = _cn1binascii.a2b_base64(data)
        if r is not None:
            return r
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


def crc32(data, crc=0):
    data = _bytes(data, "data")
    return _cn1binascii.crc32(data, crc)


def crc_hqx(data, value):
    data = _bytes(data, "data")
    return _cn1binascii.crc_hqx(data, value)
