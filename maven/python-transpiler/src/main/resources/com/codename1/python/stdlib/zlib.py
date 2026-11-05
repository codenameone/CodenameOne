# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""zlib over Codename One's JZlib, a Java port of zlib.

Streams are interchangeable with CPython's: what one compresses the other
decompresses, in zlib, raw deflate and gzip framing.
"""

import _cn1zlib

__all__ = ["compress", "decompress", "compressobj", "decompressobj", "crc32", "adler32", "error",
           "MAX_WBITS", "DEFLATED", "DEF_MEM_LEVEL", "DEF_BUF_SIZE", "Z_NO_COMPRESSION", "Z_BEST_SPEED",
           "Z_BEST_COMPRESSION", "Z_DEFAULT_COMPRESSION", "Z_FILTERED", "Z_HUFFMAN_ONLY", "Z_RLE",
           "Z_FIXED", "Z_DEFAULT_STRATEGY", "Z_NO_FLUSH", "Z_PARTIAL_FLUSH", "Z_SYNC_FLUSH",
           "Z_FULL_FLUSH", "Z_FINISH", "Z_BLOCK", "Z_TREES", "ZLIB_VERSION", "ZLIB_RUNTIME_VERSION"]

MAX_WBITS = 15
DEFLATED = 8
DEF_MEM_LEVEL = 8
DEF_BUF_SIZE = 16384
Z_NO_COMPRESSION = 0
Z_BEST_SPEED = 1
Z_BEST_COMPRESSION = 9
Z_DEFAULT_COMPRESSION = -1
Z_FILTERED = 1
Z_HUFFMAN_ONLY = 2
Z_RLE = 3
Z_FIXED = 4
Z_DEFAULT_STRATEGY = 0
Z_NO_FLUSH = 0
Z_PARTIAL_FLUSH = 1
Z_SYNC_FLUSH = 2
Z_FULL_FLUSH = 3
Z_FINISH = 4
Z_BLOCK = 5
Z_TREES = 6
ZLIB_VERSION = "1.2.3"
ZLIB_RUNTIME_VERSION = "1.2.3"


class error(Exception):
    pass


def _raise(e, action):
    msg = str(e)
    if msg.startswith("Error -5"):
        raise error("Error -5 while %s data: incomplete or truncated stream" % action) from None
    raise error(msg) from None


def _check_level(level):
    if level < -1 or level > 9:
        raise error("Bad compression level")


def crc32(data, value=0):
    return _cn1zlib.crc32(data, value)


def adler32(data, value=1):
    return _cn1zlib.adler32(data, value)


class _Compress:
    def __init__(self, level, method, wbits, memLevel, strategy, zdict):
        _check_level(level)
        if method != DEFLATED:
            raise ValueError("Invalid initialization option")
        try:
            self._z = _cn1zlib.deflate_new(level, wbits, memLevel, strategy, zdict)
        except OSError as e:
            _raise(e, "compressing")
        self._done = False

    def compress(self, data):
        if self._done:
            raise error("Error -2 while compressing data: inconsistent stream state")
        try:
            return _cn1zlib.deflate(self._z, data, Z_NO_FLUSH)
        except OSError as e:
            _raise(e, "compressing")

    def flush(self, mode=Z_FINISH):
        if mode == Z_NO_FLUSH:
            return b""
        if self._done:
            raise error("Error -2 while flushing: inconsistent stream state")
        try:
            out = _cn1zlib.deflate(self._z, b"", mode)
        except OSError as e:
            _raise(e, "flushing")
        if mode == Z_FINISH:
            self._done = True
            _cn1zlib.end(self._z)
        return out

    def copy(self):
        raise ValueError("Inconsistent stream state")


class _Decompress:
    def __init__(self, wbits, zdict):
        try:
            self._z = _cn1zlib.inflate_new(wbits, zdict)
        except OSError as e:
            _raise(e, "decompressing")
        self.unused_data = b""
        self.unconsumed_tail = b""
        self.eof = False

    def decompress(self, data, max_length=0):
        if max_length < 0:
            raise ValueError("max_length must be non-negative")
        data = bytes(data)
        if self.eof:
            self.unused_data += data
            return b""
        try:
            out, used, eof = _cn1zlib.inflate(self._z, data, max_length, False)
        except OSError as e:
            _raise(e, "decompressing")
        if eof:
            self.eof = True
            self.unused_data += data[used:]
            self.unconsumed_tail = b""
        else:
            self.unconsumed_tail = data[used:]
        return out

    def flush(self, length=DEF_BUF_SIZE):
        if length <= 0:
            raise ValueError("length must be greater than zero")
        if self.eof or not self.unconsumed_tail:
            return b""
        data = self.unconsumed_tail
        out, used, eof = _cn1zlib.inflate(self._z, data, 0, True)
        if eof:
            self.eof = True
            self.unused_data += data[used:]
        self.unconsumed_tail = b""
        return out

    def copy(self):
        raise ValueError("Inconsistent stream state")


def compressobj(level=Z_DEFAULT_COMPRESSION, method=DEFLATED, wbits=MAX_WBITS, memLevel=DEF_MEM_LEVEL,
                strategy=Z_DEFAULT_STRATEGY, zdict=None):
    return _Compress(level, method, wbits, memLevel, strategy, zdict)


def decompressobj(wbits=MAX_WBITS, zdict=None):
    return _Decompress(wbits, zdict)


def compress(data, /, level=Z_DEFAULT_COMPRESSION, wbits=MAX_WBITS):
    c = _Compress(level, DEFLATED, wbits, DEF_MEM_LEVEL, Z_DEFAULT_STRATEGY, None)
    return c.compress(data) + c.flush()


def decompress(data, /, wbits=MAX_WBITS, bufsize=DEF_BUF_SIZE):
    d = _Decompress(wbits, None)
    try:
        out, used, eof = _cn1zlib.inflate(d._z, bytes(data), 0, True)
    except OSError as e:
        _raise(e, "decompressing")
    if not eof:
        raise error("Error -5 while decompressing data: incomplete or truncated stream")
    return out
