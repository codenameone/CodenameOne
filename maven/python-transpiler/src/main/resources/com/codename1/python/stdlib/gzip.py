# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""gzip files and streams, framed here over zlib's raw deflate as CPython does.

A file opened for reading is decompressed whole when it is opened (every
member, as CPython concatenates them) and read from memory, so it can be
seeked freely. Writing compresses as it goes.
"""

import builtins
import io
import os
import struct
import time
import zlib

__all__ = ["BadGzipFile", "GzipFile", "open", "compress", "decompress"]

READ = "rb"
WRITE = "wb"
FTEXT, FHCRC, FEXTRA, FNAME, FCOMMENT = 1, 2, 4, 8, 16
_COMPRESS_LEVEL_FAST = 1
_COMPRESS_LEVEL_TRADEOFF = 6
_COMPRESS_LEVEL_BEST = 9


class BadGzipFile(OSError):
    pass


def open(filename, mode="rb", compresslevel=_COMPRESS_LEVEL_BEST, encoding=None, errors=None, newline=None):
    if "t" in mode:
        if "b" in mode:
            raise ValueError("Invalid mode: %r" % (mode,))
    else:
        if encoding is not None:
            raise ValueError("Argument 'encoding' not supported in binary mode")
        if errors is not None:
            raise ValueError("Argument 'errors' not supported in binary mode")
        if newline is not None:
            raise ValueError("Argument 'newline' not supported in binary mode")
    gz_mode = mode.replace("t", "")
    if isinstance(filename, (str, bytes, os.PathLike)):
        binary_file = GzipFile(filename, gz_mode, compresslevel)
    elif hasattr(filename, "read") or hasattr(filename, "write"):
        binary_file = GzipFile(None, gz_mode, compresslevel, filename)
    else:
        raise TypeError("filename must be a str or bytes object, or a file")
    if "t" in mode:
        encoding = io.text_encoding(encoding)
        return io.TextIOWrapper(binary_file, encoding, errors, newline)
    return binary_file


def _header(mtime, level, name=b""):
    flags = FNAME if name else 0
    if level == _COMPRESS_LEVEL_BEST:
        xfl = 2
    elif level == _COMPRESS_LEVEL_FAST:
        xfl = 4
    else:
        xfl = 0
    out = b"\037\213\010" + bytes([flags]) + struct.pack("<L", int(mtime) & 0xffffffff) + bytes([xfl]) + b"\377"
    if name:
        out += name + b"\000"
    return out


def _read_members(data):
    out = []
    pos = 0
    n = len(data)
    first = True
    while pos < n:
        if data[pos:pos + 2] != b"\037\213":
            if first:
                raise BadGzipFile("Not a gzipped file (%r)" % data[pos:pos + 2])
            if data[pos:].strip(b"\000") == b"":
                break
            raise BadGzipFile("Not a gzipped file (%r)" % data[pos:pos + 2])
        first = False
        if len(data) < pos + 10:
            raise EOFError("Compressed file ended before the end-of-stream marker was reached")
        method, flag = data[pos + 2], data[pos + 3]
        if method != 8:
            raise BadGzipFile("Unknown compression method")
        pos += 10
        if flag & FEXTRA:
            xlen = struct.unpack("<H", data[pos:pos + 2])[0]
            pos += 2 + xlen
        if flag & FNAME:
            pos = data.index(b"\000", pos) + 1
        if flag & FCOMMENT:
            pos = data.index(b"\000", pos) + 1
        if flag & FHCRC:
            pos += 2
        d = zlib.decompressobj(-zlib.MAX_WBITS)
        chunk = d.decompress(data[pos:])
        if not d.eof:
            raise EOFError("Compressed file ended before the end-of-stream marker was reached")
        rest = d.unused_data
        if len(rest) < 8:
            raise EOFError("Compressed file ended before the end-of-stream marker was reached")
        crc, isize = struct.unpack("<LL", rest[:8])
        if crc != zlib.crc32(chunk):
            raise BadGzipFile("CRC check failed %s != %s" % (hex(crc), hex(zlib.crc32(chunk))))
        if isize != (len(chunk) & 0xffffffff):
            raise BadGzipFile("Incorrect length of data produced")
        out.append(chunk)
        pos = n - len(rest) + 8
        while pos < n and data[pos] == 0:
            pos += 1
    return b"".join(out)


class GzipFile(io.BufferedIOBase):
    myfileobj = None

    def __init__(self, filename=None, mode=None, compresslevel=_COMPRESS_LEVEL_BEST, fileobj=None, mtime=None):
        if mode and ("t" in mode or "U" in mode):
            raise ValueError("Invalid mode: {!r}".format(mode))
        if mode and "b" not in mode:
            mode += "b"
        if fileobj is None:
            fileobj = self.myfileobj = builtins.open(filename, mode or "rb")
        if filename is None:
            filename = getattr(fileobj, "name", "")
            if not isinstance(filename, (str, bytes)):
                filename = ""
        else:
            filename = os.fspath(filename)
        if mode is None:
            mode = getattr(fileobj, "mode", "rb")
        self.name = filename
        self.fileobj = fileobj
        self.closed = False
        if mode.startswith("r"):
            self.mode = READ
            self._buffer = io.BytesIO(_read_members(fileobj.read()))
        elif mode.startswith(("w", "a", "x")):
            if not 0 <= compresslevel <= 9 and compresslevel != -1:
                raise ValueError("Invalid compression level")
            self.mode = WRITE
            self._crc = zlib.crc32(b"")
            self._size = 0
            self._compress = zlib.compressobj(compresslevel, zlib.DEFLATED, -zlib.MAX_WBITS,
                                              zlib.DEF_MEM_LEVEL, 0)
            if mtime is None:
                mtime = time.time()
            base = os.path.basename(filename)
            if isinstance(base, str):
                base = base.encode("latin-1", "replace")
            if base.endswith(b".gz"):
                base = base[:-3]
            fileobj.write(_header(mtime, compresslevel, base))
        else:
            raise ValueError("Invalid mode: {!r}".format(mode))

    @property
    def mtime(self):
        return None

    def __repr__(self):
        return "<gzip " + repr(self.fileobj) + " " + hex(id(self)) + ">"

    def _check_not_closed(self):
        if self.closed:
            raise ValueError("I/O operation on closed file.")

    def write(self, data):
        self._check_not_closed()
        if self.mode != WRITE:
            raise OSError(9, "write() on read-only GzipFile object")
        if isinstance(data, (bytes, bytearray)):
            length = len(data)
        else:
            data = memoryview(data)
            length = data.nbytes
        if length > 0:
            self.fileobj.write(self._compress.compress(data))
            self._size += length
            self._crc = zlib.crc32(data, self._crc)
        return length

    def _need_read(self):
        self._check_not_closed()
        if self.mode != READ:
            raise OSError(9, "read() on write-only GzipFile object")

    def read(self, size=-1):
        self._need_read()
        return self._buffer.read(size)

    def read1(self, size=-1):
        return self.read(size)

    def readinto(self, b):
        data = self.read(len(b))
        b[:len(data)] = data
        return len(data)

    def peek(self, n):
        self._need_read()
        pos = self._buffer.tell()
        data = self._buffer.read(max(n, 1))
        self._buffer.seek(pos)
        return data

    def readline(self, size=-1):
        self._need_read()
        return self._buffer.readline(size)

    def __next__(self):
        line = self.readline()
        if not line:
            raise StopIteration
        return line

    def __iter__(self):
        self._check_not_closed()
        return self

    def readlines(self, hint=-1):
        out = []
        for line in self:
            out.append(line)
        return out

    def close(self):
        if self.closed:
            return
        self.closed = True
        fileobj = self.fileobj
        if fileobj is None:
            return
        self.fileobj = None
        try:
            if self.mode == WRITE:
                fileobj.write(self._compress.flush())
                fileobj.write(struct.pack("<LL", self._crc, self._size & 0xffffffff))
        finally:
            myfileobj = self.myfileobj
            if myfileobj:
                self.myfileobj = None
                myfileobj.close()

    def flush(self, zlib_mode=zlib.Z_SYNC_FLUSH):
        self._check_not_closed()
        if self.mode == WRITE:
            self.fileobj.write(self._compress.flush(zlib_mode))
            self.fileobj.flush()

    def fileno(self):
        return self.fileobj.fileno()

    def rewind(self):
        self._need_read()
        self._buffer.seek(0)

    def readable(self):
        return self.mode == READ

    def writable(self):
        return self.mode == WRITE

    def seekable(self):
        return self.mode == READ

    def tell(self):
        if self.mode == WRITE:
            return self._size
        return self._buffer.tell()

    def seek(self, offset, whence=io.SEEK_SET):
        if self.mode != READ:
            raise OSError("Seek from end not supported")
        return self._buffer.seek(offset, whence)

    def __enter__(self):
        self._check_not_closed()
        return self

    def __exit__(self, *exc):
        self.close()


def compress(data, compresslevel=_COMPRESS_LEVEL_BEST, *, mtime=None):
    if mtime is None:
        mtime = time.time()
    c = zlib.compressobj(compresslevel, zlib.DEFLATED, -zlib.MAX_WBITS, zlib.DEF_MEM_LEVEL, 0)
    body = c.compress(data) + c.flush()
    return (_header(mtime, compresslevel) + body
            + struct.pack("<LL", zlib.crc32(data), len(data) & 0xffffffff))


def decompress(data):
    return _read_members(bytes(data))
