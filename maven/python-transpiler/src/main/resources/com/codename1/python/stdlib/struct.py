# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Packing values into bytes as C structs lay them out.

Native mode ('@', the default) follows a 64-bit little-endian device:
`long` and pointers are 8 bytes and fields are aligned to their size.
"""

import math as _math

__all__ = ["calcsize", "pack", "pack_into", "unpack", "unpack_from", "iter_unpack", "Struct", "error"]


class error(Exception):
    pass


error.__module__ = "struct"

_STD = {"x": 1, "c": 1, "b": 1, "B": 1, "?": 1, "h": 2, "H": 2, "i": 4, "I": 4, "l": 4, "L": 4,
        "q": 8, "Q": 8, "e": 2, "f": 4, "d": 8, "s": 1, "p": 1}
_NATIVE = dict(_STD, l=8, L=8, n=8, N=8, P=8)
_SIGNED = "bhilqn"
_INTS = "bBhHiIlLqQnNP"


def _parse(fmt):
    if isinstance(fmt, bytes):
        fmt = fmt.decode("ascii")
    if not isinstance(fmt, str):
        raise TypeError("Struct() argument 1 must be a str or bytes object, not %s" % type(fmt).__name__)
    order = "@"
    i = 0
    if fmt[:1] in "@=<>!" and fmt:
        order = fmt[0]
        i = 1
    native = order == "@"
    little = order in "<@="
    sizes = _NATIVE if native else _STD
    items = []
    offset = 0
    n = len(fmt)
    while i < n:
        c = fmt[i]
        if c.isspace():
            i += 1
            continue
        count = None
        while i < n and fmt[i].isdigit():
            count = (count or 0) * 10 + int(fmt[i])
            i += 1
        if i >= n:
            raise error("repeat count given without format specifier")
        c = fmt[i]
        i += 1
        if c not in sizes:
            raise error("bad char in struct format")
        size = sizes[c]
        if native and c not in "xcsp?bB":
            offset = (offset + size - 1) // size * size
        if count is None:
            count = 1
        if c in "sp":
            items.append((c, offset, count))
            offset += count
        elif c == "x":
            offset += count
        else:
            for _ in range(count):
                items.append((c, offset, size))
                offset += size
    return items, offset, little


def _pack_float(x, size, little):
    ebits, mbits = {2: (5, 10), 4: (8, 23), 8: (11, 52)}[size]
    bias = (1 << (ebits - 1)) - 1
    sign = 0
    if _math.copysign(1.0, x) < 0:
        sign = 1
        x = -x
    if _math.isnan(x):
        e = (1 << ebits) - 1
        m = 1 << (mbits - 1)
    elif _math.isinf(x):
        e = (1 << ebits) - 1
        m = 0
    elif x == 0:
        e = m = 0
    else:
        f, ex = _math.frexp(x)
        e = ex + bias - 1
        if e <= 0:
            # Subnormal: the mantissa scaled to the fixed exponent.
            scaled = _math.ldexp(f, ex + bias - 1 + mbits)
            m = _round_even(scaled)
            e = 0
            if m >> mbits:
                e = 1
                m &= (1 << mbits) - 1
        else:
            m = _round_even(_math.ldexp(f, mbits + 1)) - (1 << mbits)
            if m >> mbits:
                m = 0
                e += 1
        if e >= (1 << ebits) - 1:
            raise OverflowError("float too large to pack with %s format" % {2: "e", 4: "f", 8: "d"}[size])
    bits = (sign << (size * 8 - 1)) | (e << mbits) | m
    return bits.to_bytes(size, "little" if little else "big")


def _round_even(v):
    fl = _math.floor(v)
    d = v - fl
    r = int(fl)
    if d > 0.5 or (d == 0.5 and r % 2 == 1):
        r += 1
    return r


def _unpack_float(b, little):
    size = len(b)
    ebits, mbits = {2: (5, 10), 4: (8, 23), 8: (11, 52)}[size]
    bits = int.from_bytes(b, "little" if little else "big")
    sign = bits >> (size * 8 - 1)
    e = (bits >> mbits) & ((1 << ebits) - 1)
    m = bits & ((1 << mbits) - 1)
    bias = (1 << (ebits - 1)) - 1
    if e == (1 << ebits) - 1:
        v = _math.nan if m else _math.inf
    elif e == 0:
        v = _math.ldexp(float(m), 1 - bias - mbits)
    else:
        v = _math.ldexp(float(m | (1 << mbits)), e - bias - mbits)
    return -v if sign else v


def _int_arg(v, c):
    if isinstance(v, bool):
        return int(v)
    if isinstance(v, int):
        return v
    idx = getattr(type(v), "__index__", None)
    if idx is None:
        raise error("required argument is not an integer")
    return idx(v)


def _pack_items(items, total, little, args):
    if len(args) != len(items):
        raise error("pack expected %d items for packing (got %d)" % (len(items), len(args)))
    out = bytearray(total)
    for (c, off, size), v in zip(items, args):
        if c in _INTS:
            v = _int_arg(v, c)
            signed = c in _SIGNED
            bits = size * 8
            lo = -(1 << (bits - 1)) if signed else 0
            hi = (1 << (bits - 1)) - 1 if signed else (1 << bits) - 1
            if not lo <= v <= hi:
                if signed:
                    raise error("'%s' format requires %d <= number <= %d" % (c, lo, hi))
                raise error("'%s' format requires 0 <= number <= %d" % (c, hi))
            out[off:off + size] = v.to_bytes(size, "little" if little else "big", signed=signed)
        elif c == "?":
            out[off] = 1 if v else 0
        elif c == "c":
            if not isinstance(v, (bytes, bytearray)) or len(v) != 1:
                raise error("char format requires a bytes object of length 1")
            out[off] = v[0]
        elif c in "efd":
            if not isinstance(v, (int, float)):
                try:
                    v = v.__float__()
                except AttributeError:
                    raise error("required argument is not a float")
            out[off:off + size] = _pack_float(float(v), size, little)
        elif c == "s":
            if not isinstance(v, (bytes, bytearray)):
                raise error("argument for 's' must be a bytes object")
            data = bytes(v[:size])
            out[off:off + len(data)] = data
        elif c == "p":
            if not isinstance(v, (bytes, bytearray)):
                raise error("argument for 'p' must be a bytes object")
            if size:
                data = bytes(v[:size - 1])
                out[off] = min(len(data), 255)
                out[off + 1:off + 1 + len(data)] = data
    return bytes(out)


def _unpack_items(items, little, buf, offset):
    out = []
    for c, off, size in items:
        o = offset + off
        if c in _INTS:
            out.append(int.from_bytes(buf[o:o + size], "little" if little else "big", signed=c in _SIGNED))
        elif c == "?":
            out.append(buf[o] != 0)
        elif c == "c":
            out.append(bytes(buf[o:o + 1]))
        elif c in "efd":
            out.append(_unpack_float(bytes(buf[o:o + size]), little))
        elif c == "s":
            out.append(bytes(buf[o:o + size]))
        elif c == "p":
            n = min(buf[o], size - 1) if size else 0
            out.append(bytes(buf[o + 1:o + 1 + n]))
    return tuple(out)


class Struct:
    def __init__(self, format):
        self._items, self.size, self._little = _parse(format)
        self.format = format.decode("ascii") if isinstance(format, bytes) else format

    def pack(self, *args):
        return _pack_items(self._items, self.size, self._little, args)

    def pack_into(self, buffer, offset, *args):
        data = self.pack(*args)
        if offset < 0:
            offset += len(buffer)
        if offset + len(data) > len(buffer):
            raise error("pack_into requires a buffer of at least %d bytes for packing %d bytes at offset %d "
                        "(actual buffer size is %d)" % (offset + len(data), len(data), offset, len(buffer)))
        buffer[offset:offset + len(data)] = data

    def unpack(self, buffer):
        if len(buffer) != self.size:
            raise error("unpack requires a buffer of %d bytes" % self.size)
        return _unpack_items(self._items, self._little, buffer, 0)

    def unpack_from(self, buffer, offset=0):
        if offset < 0:
            offset += len(buffer)
        if len(buffer) - offset < self.size:
            raise error("unpack_from requires a buffer of at least %d bytes for unpacking %d bytes at offset %d "
                        "(actual buffer size is %d)" % (self.size + offset, self.size, offset, len(buffer)))
        return _unpack_items(self._items, self._little, buffer, offset)

    def iter_unpack(self, buffer):
        if self.size == 0:
            raise error("cannot iteratively unpack with a struct of length 0")
        if len(buffer) % self.size:
            raise error("iterative unpacking requires a buffer of a multiple of %d bytes" % self.size)
        for i in range(0, len(buffer), self.size):
            yield _unpack_items(self._items, self._little, buffer, i)

    def __repr__(self):
        return "Struct(%r)" % (self.format,)


_cache = {}


def _struct(fmt):
    s = _cache.get(fmt)
    if s is None:
        s = Struct(fmt)
        if len(_cache) > 100:
            _cache.clear()
        _cache[fmt] = s
    return s


def calcsize(format):
    return _struct(format).size


def pack(format, *args):
    return _struct(format).pack(*args)


def pack_into(format, buffer, offset, *args):
    _struct(format).pack_into(buffer, offset, *args)


def unpack(format, buffer):
    return _struct(format).unpack(buffer)


def unpack_from(format, buffer, offset=0):
    return _struct(format).unpack_from(buffer, offset)


def iter_unpack(format, buffer):
    return _struct(format).iter_unpack(buffer)
