# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Base16, Base32, Base64, Base85 and Ascii85 data encodings."""

import binascii

__all__ = ["encode", "decode", "encodebytes", "decodebytes", "b64encode", "b64decode", "b32encode",
           "b32decode", "b32hexencode", "b32hexdecode", "b16encode", "b16decode", "b85encode",
           "b85decode", "a85encode", "a85decode", "z85encode", "z85decode", "standard_b64encode",
           "standard_b64decode", "urlsafe_b64encode", "urlsafe_b64decode"]

bytes_types = (bytes, bytearray)


def _bytes_from_decode_data(s):
    if isinstance(s, str):
        try:
            return s.encode("ascii")
        except UnicodeEncodeError:
            raise ValueError("string argument should contain only ASCII characters")
    if isinstance(s, (bytes, bytearray, memoryview)):
        return bytes(s)
    raise TypeError("argument should be a bytes-like object or ASCII string, not %r" % s.__class__.__name__)


def _input(s):
    if isinstance(s, (bytes, bytearray, memoryview)):
        return bytes(s)
    raise TypeError("a bytes-like object is required, not '%s'" % type(s).__name__)


def b64encode(s, altchars=None):
    encoded = binascii.b2a_base64(_input(s), newline=False)
    if altchars is not None:
        altchars = _input(altchars)
        if len(altchars) != 2:
            raise ValueError("invalid altchars: %r" % (altchars,))
        return encoded.translate(bytes.maketrans(b"+/", altchars))
    return encoded


def b64decode(s, altchars=None, validate=False):
    s = _bytes_from_decode_data(s)
    if altchars is not None:
        altchars = _bytes_from_decode_data(altchars)
        if len(altchars) != 2:
            raise ValueError("invalid altchars: %r" % (altchars,))
        s = s.translate(bytes.maketrans(altchars, b"+/"))
    return binascii.a2b_base64(s, strict_mode=validate)


def standard_b64encode(s):
    return b64encode(s)


def standard_b64decode(s):
    return b64decode(s)


def urlsafe_b64encode(s):
    return b64encode(s).translate(bytes.maketrans(b"+/", b"-_"))


def urlsafe_b64decode(s):
    s = _bytes_from_decode_data(s)
    s = s.translate(bytes.maketrans(b"-_", b"+/"))
    return b64decode(s)


_B32 = b"ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
_B32HEX = b"0123456789ABCDEFGHIJKLMNOPQRSTUV"


def _b32encode(alphabet, s):
    s = _input(s)
    leftover = len(s) % 5
    if leftover:
        s = s + b"\0" * (5 - leftover)
    out = bytearray()
    for i in range(0, len(s), 5):
        c = int.from_bytes(s[i:i + 5], "big")
        for shift in range(35, -1, -5):
            out.append(alphabet[(c >> shift) & 31])
    if leftover == 1:
        out[-6:] = b"======"
    elif leftover == 2:
        out[-4:] = b"===="
    elif leftover == 3:
        out[-3:] = b"==="
    elif leftover == 4:
        out[-1:] = b"="
    return bytes(out)


def _b32decode(alphabet, s, casefold=False, map01=None):
    s = _bytes_from_decode_data(s)
    if len(s) % 8:
        raise binascii.Error("Incorrect padding")
    if map01 is not None:
        map01 = _bytes_from_decode_data(map01)
        s = s.translate(bytes.maketrans(b"01", b"O" + map01))
    if casefold:
        s = s.upper()
    n = len(s)
    s = s.rstrip(b"=")
    padchars = n - len(s)
    rev = {c: i for i, c in enumerate(alphabet)}
    out = bytearray()
    for i in range(0, len(s), 8):
        quanta = s[i:i + 8]
        acc = 0
        for c in quanta:
            v = rev.get(c)
            if v is None:
                raise binascii.Error("Non-base32 digit found")
            acc = (acc << 5) | v
        acc <<= 5 * (8 - len(quanta))
        out += acc.to_bytes(5, "big")
    if n % 8 or padchars not in (0, 1, 3, 4, 6):
        raise binascii.Error("Incorrect padding")
    if padchars and out:
        leftover = (43 - 5 * padchars) // 8
        del out[leftover - 5:]
    return bytes(out)


def b32encode(s):
    return _b32encode(_B32, s)


def b32decode(s, casefold=False, map01=None):
    return _b32decode(_B32, s, casefold, map01)


def b32hexencode(s):
    return _b32encode(_B32HEX, s)


def b32hexdecode(s, casefold=False):
    return _b32decode(_B32HEX, s, casefold)


def b16encode(s):
    return _input(s).hex().upper().encode("ascii")


def b16decode(s, casefold=False):
    s = _bytes_from_decode_data(s)
    if casefold:
        s = s.upper()
    for c in s:
        if c not in b"0123456789ABCDEF":
            raise binascii.Error("Non-base16 digit found")
    return binascii.unhexlify(s)


_B85 = (b"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        b"abcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~")
_Z85 = (b"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        b".-:+=^!/*?&<>()[]{}@%$#")


def _85encode(b, chars, pad=False, foldnuls=False, foldspaces=False):
    b = _input(b)
    padding = (-len(b)) % 4
    if padding:
        b = b + b"\0" * padding
    out = bytearray()
    for i in range(0, len(b), 4):
        word = int.from_bytes(b[i:i + 4], "big")
        if foldnuls and not word:
            out += b"z"
        elif foldspaces and word == 0x20202020:
            out += b"y"
        else:
            digits = bytearray(5)
            for k in range(4, -1, -1):
                word, digits[k] = divmod(word, 85)
            for d in digits:
                out.append(chars[d])
    if padding and not pad:
        if out[-1:] == b"z":
            out[-1:] = chars[0:1] * 5
        del out[len(out) - padding:]
    return bytes(out)


def b85encode(b, pad=False):
    return _85encode(b, _B85, pad)


def _85decode(b, chars, name):
    b = _bytes_from_decode_data(b)
    rev = {c: i for i, c in enumerate(chars)}
    padding = (-len(b)) % 5
    b = b + chars[84:85] * padding
    out = bytearray()
    for i in range(0, len(b), 5):
        acc = 0
        for j, c in enumerate(b[i:i + 5]):
            v = rev.get(c)
            if v is None:
                raise ValueError("bad %s character at position %d" % (name, i + j))
            acc = acc * 85 + v
        if acc > 0xFFFFFFFF:
            raise ValueError("%s overflow in hunk starting at byte %d" % (name, i))
        out += acc.to_bytes(4, "big")
    if padding:
        del out[len(out) - padding:]
    return bytes(out)


def b85decode(b):
    return _85decode(b, _B85, "base85")


def z85encode(s):
    return _85encode(s, _Z85, True)


def z85decode(s):
    return _85decode(s, _Z85, "z85")


_A85START = b"<~"
_A85END = b"~>"


def a85encode(b, *, foldspaces=False, wrapcol=0, pad=False, adobe=False):
    chars = bytes(range(33, 118))
    result = _85encode(b, chars, pad, True, foldspaces)
    if adobe:
        result = _A85START + result
    if wrapcol:
        wrapcol = max(2 if adobe else 1, wrapcol)
        chunks = [result[i:i + wrapcol] for i in range(0, len(result), wrapcol)]
        if adobe and len(chunks[-1]) + 2 > wrapcol:
            chunks.append(b"")
        result = b"\n".join(chunks)
    if adobe:
        result += _A85END
    return result


def a85decode(b, *, foldspaces=False, adobe=False, ignorechars=b" \t\n\r\x0b"):
    b = _bytes_from_decode_data(b)
    if adobe:
        if not b.endswith(_A85END):
            raise ValueError("Ascii85 encoded byte sequences must end with %r" % (_A85END,))
        b = b[2:-2] if b.startswith(_A85START) else b[:-2]
    out = bytearray()
    curr = []
    for x in b + b"u" * 4:
        if 33 <= x <= 117:
            curr.append(x)
            if len(curr) == 5:
                acc = 0
                for c in curr:
                    acc = 85 * acc + (c - 33)
                if acc > 0xFFFFFFFF:
                    raise ValueError("Ascii85 overflow")
                out += acc.to_bytes(4, "big")
                curr = []
        elif x == 122:
            if curr:
                raise ValueError("z inside Ascii85 5-tuple")
            out += b"\0\0\0\0"
        elif foldspaces and x == 121:
            if curr:
                raise ValueError("y inside Ascii85 5-tuple")
            out += b"\x20\x20\x20\x20"
        elif x in ignorechars:
            continue
        else:
            raise ValueError("Non-Ascii85 digit found: %c" % x)
    padding = 4 - len(curr)
    if padding:
        del out[len(out) - padding:]
    return bytes(out)


MAXLINESIZE = 76
MAXBINSIZE = (MAXLINESIZE // 4) * 3


def encodebytes(s):
    s = _input(s)
    pieces = []
    for i in range(0, len(s), MAXBINSIZE):
        pieces.append(binascii.b2a_base64(s[i:i + MAXBINSIZE]))
    return b"".join(pieces)


def decodebytes(s):
    return binascii.a2b_base64(_input(s))


def encode(input, output):
    while True:
        s = input.read(MAXBINSIZE)
        if not s:
            break
        output.write(binascii.b2a_base64(s))


def decode(input, output):
    output.write(binascii.a2b_base64(input.read()))
