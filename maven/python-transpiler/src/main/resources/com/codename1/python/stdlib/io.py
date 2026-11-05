# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Streams: in-memory text and bytes, and files through open().

A file is read whole when it is opened and written whole when it is
flushed or closed: the device file system (`FileSystemStorage`) has no
random access to offer, and application files are small. A file opened
for appending writes only what was added.
"""

import _cn1fs

SEEK_SET = 0
SEEK_CUR = 1
SEEK_END = 2
DEFAULT_BUFFER_SIZE = 8192

__all__ = ["open", "open_code", "IOBase", "RawIOBase", "BufferedIOBase", "TextIOBase", "StringIO",
           "BytesIO", "TextIOWrapper", "BufferedReader", "BufferedWriter", "BufferedRandom", "FileIO",
           "UnsupportedOperation", "SEEK_SET", "SEEK_CUR", "SEEK_END", "DEFAULT_BUFFER_SIZE",
           "text_encoding"]

_cwd = None


def _getcwd():
    global _cwd
    if _cwd is None:
        _cwd = _cn1fs.home()
    return _cwd


def _setcwd(path):
    global _cwd
    _cwd = path


def _abspath(path):
    """`path` made absolute against the working directory and normalised."""
    if not path.startswith("/"):
        base = _getcwd()
        path = base + "/" + path if base != "/" else "/" + path
    parts = []
    for p in path.split("/"):
        if p == "" or p == ".":
            continue
        if p == "..":
            if parts:
                parts.pop()
            continue
        parts.append(p)
    return "/" + "/".join(parts)


def _fspath(path):
    if isinstance(path, (str, bytes)):
        return path.decode("utf-8") if isinstance(path, bytes) else path
    fspath = getattr(type(path), "__fspath__", None)
    if fspath is None:
        raise TypeError("expected str, bytes or os.PathLike object, not " + type(path).__name__)
    return _fspath(fspath(path))


class UnsupportedOperation(OSError, ValueError):
    pass


UnsupportedOperation.__module__ = "io"


class IOBase:
    closed = False

    def close(self):
        if not self.closed:
            try:
                self.flush()
            finally:
                self.closed = True

    def flush(self):
        self._check()

    def readable(self):
        return False

    def writable(self):
        return False

    def seekable(self):
        return False

    def isatty(self):
        return False

    def fileno(self):
        raise UnsupportedOperation("fileno")

    def __enter__(self):
        self._check()
        return self

    def __exit__(self, *exc):
        self.close()

    def __iter__(self):
        self._check()
        return self

    def __next__(self):
        line = self.readline()
        if not line:
            raise StopIteration
        return line

    def readlines(self, hint=-1):
        out = []
        total = 0
        for line in self:
            out.append(line)
            total += len(line)
            if 0 < hint <= total:
                break
        return out

    def writelines(self, lines):
        self._check()
        for line in lines:
            self.write(line)

    def _check(self):
        if self.closed:
            raise ValueError("I/O operation on closed file.")

    def __del__(self):
        pass


class RawIOBase(IOBase):
    pass


class BufferedIOBase(IOBase):
    pass


class TextIOBase(IOBase):
    encoding = None
    errors = None
    newlines = None


class _Buffer:
    """A growable str or bytes with a cursor; appends are amortised."""

    def __init__(self, initial, empty):
        self._empty = empty
        self._buf = initial
        self._tail = []
        self._tail_len = 0
        self._pos = 0

    def _flat(self):
        if self._tail:
            self._buf = self._buf + self._empty.join(self._tail)
            self._tail = []
            self._tail_len = 0
        return self._buf

    def size(self):
        return len(self._buf) + self._tail_len

    def value(self):
        return self._flat()

    def write(self, s, pad):
        pos = self._pos
        n = len(s)
        if pos == self.size():
            self._tail.append(s)
            self._tail_len += n
        else:
            buf = self._flat()
            if pos > len(buf):
                buf = buf + pad(pos - len(buf))
            self._buf = buf[:pos] + s + buf[pos + n:]
        self._pos = pos + n
        return n

    def read(self, size):
        buf = self._flat()
        start = self._pos
        end = len(buf) if size is None or size < 0 else min(len(buf), start + size)
        self._pos = max(end, start)
        return buf[start:end]

    def readline(self, size, nl):
        buf = self._flat()
        start = self._pos
        i = buf.find(nl, start)
        end = len(buf) if i < 0 else i + len(nl)
        if size is not None and size >= 0:
            end = min(end, start + size)
        self._pos = max(end, start)
        return buf[start:end]

    def seek(self, pos, whence):
        if whence == 1:
            pos += self._pos
        elif whence == 2:
            pos += self.size()
        elif whence != 0:
            raise ValueError("invalid whence (%r, should be 0, 1 or 2)" % whence)
        if pos < 0:
            raise ValueError("negative seek value %d" % pos)
        self._pos = pos
        return pos

    def truncate(self, size):
        buf = self._flat()
        if size is None:
            size = self._pos
        if size < 0:
            raise ValueError("negative size value %d" % size)
        self._buf = buf[:size]
        return size


class StringIO(TextIOBase):
    def __init__(self, initial_value="", newline="\n"):
        if initial_value is not None and not isinstance(initial_value, str):
            raise TypeError("initial_value must be str or None, not " + type(initial_value).__name__)
        self._b = _Buffer(initial_value or "", "")

    def readable(self):
        self._check()
        return True

    def writable(self):
        self._check()
        return True

    def seekable(self):
        self._check()
        return True

    def getvalue(self):
        self._check()
        return self._b.value()

    def write(self, s):
        self._check()
        if not isinstance(s, str):
            raise TypeError("string argument expected, got '" + type(s).__name__ + "'")
        return self._b.write(s, lambda n: "\0" * n)

    def read(self, size=-1):
        self._check()
        return self._b.read(size)

    def readline(self, size=-1):
        self._check()
        return self._b.readline(size, "\n")

    def seek(self, pos, whence=0):
        self._check()
        return self._b.seek(pos, whence)

    def tell(self):
        self._check()
        return self._b._pos

    def truncate(self, size=None):
        self._check()
        return self._b.truncate(size)


class BytesIO(BufferedIOBase):
    def __init__(self, initial_bytes=b""):
        self._b = _Buffer(bytes(initial_bytes or b""), b"")

    def readable(self):
        self._check()
        return True

    def writable(self):
        self._check()
        return True

    def seekable(self):
        self._check()
        return True

    def getvalue(self):
        self._check()
        return self._b.value()

    def getbuffer(self):
        self._check()
        return memoryview(self._b.value())

    def write(self, s):
        self._check()
        if isinstance(s, (bytearray, memoryview)):
            s = bytes(s)
        if not isinstance(s, bytes):
            raise TypeError("a bytes-like object is required, not '" + type(s).__name__ + "'")
        return self._b.write(s, bytes)

    def read(self, size=-1):
        self._check()
        return self._b.read(size)

    read1 = read

    def readline(self, size=-1):
        self._check()
        return self._b.readline(size, b"\n")

    def seek(self, pos, whence=0):
        self._check()
        return self._b.seek(pos, whence)

    def tell(self):
        self._check()
        return self._b._pos

    def truncate(self, size=None):
        self._check()
        return self._b.truncate(size)


class FileIO(RawIOBase):
    """The bytes of one file, held in memory while it is open."""

    def __init__(self, file, mode="r", closefd=True, opener=None):
        name = _fspath(file)
        path = _abspath(name)
        self.name = file
        self._path = path
        kind = mode[0]
        self._reading = kind == "r" or "+" in mode
        self._writing = kind != "r" or "+" in mode
        self._append = kind == "a"
        self.mode = _raw_mode(mode)
        exists = _cn1fs.exists(path)
        if exists and _cn1fs.isdir(path):
            raise IsADirectoryError(21, "Is a directory", name)
        if kind == "x" and exists:
            raise FileExistsError(17, "File exists", name)
        if kind == "r" and not exists:
            raise FileNotFoundError(2, "No such file or directory", name)
        parent = path.rsplit("/", 1)[0] or "/"
        if kind != "r" and not _cn1fs.isdir(parent):
            raise FileNotFoundError(2, "No such file or directory", name)
        initial = b""
        if kind == "r" or (kind == "a" and "+" in mode):
            initial = _cn1fs.read(path)
        if kind in "wx":
            _cn1fs.write(path, b"", False)
        self._b = _Buffer(initial, b"")
        self._base = len(initial) if self._append else 0
        if self._append:
            self._b._pos = self._b.size()
        self._dirty = False

    def readable(self):
        self._check()
        return self._reading

    def writable(self):
        self._check()
        return self._writing

    def seekable(self):
        self._check()
        return True

    def read(self, size=-1):
        self._check()
        if not self._reading:
            raise UnsupportedOperation("File not open for reading")
        return self._b.read(size)

    def readall(self):
        return self.read()

    def readline(self, size=-1):
        self._check()
        if not self._reading:
            raise UnsupportedOperation("File not open for reading")
        return self._b.readline(size, b"\n")

    def write(self, data):
        self._check()
        if not self._writing:
            raise UnsupportedOperation("File not open for writing")
        if isinstance(data, (bytearray, memoryview)):
            data = bytes(data)
        if not isinstance(data, bytes):
            raise TypeError("a bytes-like object is required, not '" + type(data).__name__ + "'")
        if self._append:
            self._b._pos = self._b.size()
        self._dirty = True
        return self._b.write(data, bytes)

    def seek(self, pos, whence=0):
        self._check()
        return self._b.seek(pos, whence)

    def tell(self):
        self._check()
        return self._b._pos

    def truncate(self, size=None):
        self._check()
        if not self._writing:
            raise UnsupportedOperation("File not open for writing")
        self._dirty = True
        return self._b.truncate(size)

    def flush(self):
        self._check()
        if self._dirty:
            data = self._b.value()
            if self._append:
                _cn1fs.write(self._path, data[self._base:], True)
                self._base = len(data)
            else:
                _cn1fs.write(self._path, data, False)
            self._dirty = False

    def __repr__(self):
        return "<_io.FileIO name=%r mode=%r closefd=True>" % (self.name, self.mode)


def _raw_mode(mode):
    kind = mode[0]
    if kind == "r":
        return "rb+" if "+" in mode else "rb"
    if kind == "a":
        return "ab+" if "+" in mode else "ab"
    if kind == "x":
        return "xb+" if "+" in mode else "xb"
    return "rb+" if "+" in mode else "wb"


class _BufferedFile(BufferedIOBase):
    def __init__(self, raw):
        self.raw = raw

    @property
    def name(self):
        return self.raw.name

    @property
    def mode(self):
        return self.raw.mode

    @property
    def closed(self):
        return self.raw.closed

    def _check(self):
        self.raw._check()

    def close(self):
        self.raw.close()

    def flush(self):
        self.raw.flush()

    def readable(self):
        return self.raw.readable()

    def writable(self):
        return self.raw.writable()

    def seekable(self):
        return self.raw.seekable()

    def read(self, size=-1):
        return self.raw.read(size)

    read1 = read

    def peek(self, size=0):
        pos = self.raw.tell()
        data = self.raw.read(max(size, 1))
        self.raw.seek(pos)
        return data

    def readline(self, size=-1):
        return self.raw.readline(size)

    def write(self, data):
        return self.raw.write(data)

    def seek(self, pos, whence=0):
        return self.raw.seek(pos, whence)

    def tell(self):
        return self.raw.tell()

    def truncate(self, size=None):
        return self.raw.truncate(size)

    def detach(self):
        raw = self.raw
        self.raw = None
        return raw

    def __repr__(self):
        return "<_io.%s name=%r>" % (type(self).__name__, self.name)


class BufferedReader(_BufferedFile):
    pass


class BufferedWriter(_BufferedFile):
    pass


class BufferedRandom(_BufferedFile):
    pass


for _c in (BufferedReader, BufferedWriter, BufferedRandom, FileIO):
    _c.__module__ = "_io"


def text_encoding(encoding, stacklevel=2):
    return "utf-8" if encoding is None else encoding


class TextIOWrapper(TextIOBase):
    """Text over a binary stream: decoded whole on open, encoded on flush."""

    def __init__(self, buffer, encoding=None, errors=None, newline=None, line_buffering=False,
                 write_through=False):
        if newline not in (None, "", "\n", "\r", "\r\n"):
            raise ValueError("illegal newline value: %r" % (newline,))
        self.buffer = buffer
        self.encoding = "UTF-8" if encoding is None else encoding
        self.errors = "strict" if errors is None else errors
        self._newline = newline
        self.line_buffering = line_buffering
        text = ""
        if buffer.readable():
            pos = buffer.tell()
            buffer.seek(0)
            raw = buffer.read()
            buffer.seek(pos)
            text = raw.decode(self.encoding, self.errors)
            if newline is None:
                text = text.replace("\r\n", "\n").replace("\r", "\n")
        self._t = _Buffer(text, "")
        # Over this module's own files the text is written back through the
        # file's buffer; over any other binary stream (a GzipFile, a socket
        # file) only what was added since the last flush is written to it.
        self._native = isinstance(buffer, _BufferedFile)
        if self._native and getattr(buffer.raw, "_append", False):
            self._t._pos = self._t.size()
            self._base = self._t.size()
        else:
            self._base = 0
        self._dirty = False
        self._mode = None

    @property
    def name(self):
        return self.buffer.name

    @property
    def mode(self):
        if self._mode is not None:
            return self._mode
        return self.buffer.mode.replace("b", "")

    @property
    def closed(self):
        return self.buffer.closed

    def _check(self):
        self.buffer._check()

    def readable(self):
        return self.buffer.readable()

    def writable(self):
        return self.buffer.writable()

    def seekable(self):
        return self.buffer.seekable()

    def read(self, size=-1):
        self._check()
        if not self.buffer.readable():
            raise UnsupportedOperation("not readable")
        return self._t.read(size)

    def readline(self, size=-1):
        self._check()
        if not self.buffer.readable():
            raise UnsupportedOperation("not readable")
        return self._t.readline(size, "\n")

    def write(self, s):
        self._check()
        if not self.buffer.writable():
            raise UnsupportedOperation("not writable")
        if not isinstance(s, str):
            raise TypeError("write() argument must be str, not " + type(s).__name__)
        if not self._native or getattr(self.buffer.raw, "_append", False):
            self._t._pos = self._t.size()
        self._dirty = True
        n = self._t.write(s, lambda k: "\0" * k)
        if self.line_buffering and "\n" in s:
            self.flush()
        return n

    def seek(self, pos, whence=0):
        self._check()
        if whence == 1 and pos != 0:
            raise UnsupportedOperation("can't do nonzero cur-relative seeks")
        if whence == 2 and pos != 0:
            raise UnsupportedOperation("can't do nonzero end-relative seeks")
        return self._t.seek(pos, whence)

    def tell(self):
        self._check()
        return self._t._pos

    def truncate(self, size=None):
        self._check()
        self._dirty = True
        return self._t.truncate(size)

    def flush(self):
        self._check()
        if self._dirty:
            text = self._t.value()
            nl = self._newline
            if nl is not None and nl not in ("", "\n"):
                text = text.replace("\n", nl)
            if not self._native:
                self.buffer.write(text[self._base:].encode(self.encoding, self.errors))
                self._base = len(text)
                self._dirty = False
                return
            raw = self.buffer.raw
            if getattr(raw, "_append", False):
                data = text[self._base:].encode(self.encoding, self.errors)
                raw._b = _Buffer(data, b"")
                raw._base = 0
                raw._dirty = True
                raw.flush()
                raw._b = _Buffer(b"", b"")
                self._base = len(text)
            else:
                raw._b = _Buffer(text.encode(self.encoding, self.errors), b"")
                raw._dirty = True
                raw.flush()
            self._dirty = False

    def close(self):
        if not self.closed:
            try:
                self.flush()
            finally:
                self.buffer.close()

    def detach(self):
        self.flush()
        b = self.buffer
        self.buffer = None
        return b

    def reconfigure(self, *, encoding=None, errors=None, newline=None, line_buffering=None,
                    write_through=None):
        if encoding is not None:
            self.encoding = encoding
        if errors is not None:
            self.errors = errors
        if line_buffering is not None:
            self.line_buffering = line_buffering

    def __repr__(self):
        return "<_io.TextIOWrapper name=%r mode=%r encoding=%r>" % (self.name, self.mode, self.encoding)


TextIOWrapper.__module__ = "_io"
StringIO.__module__ = "_io"
BytesIO.__module__ = "_io"


def open(file, mode="r", buffering=-1, encoding=None, errors=None, newline=None, closefd=True,
         opener=None):
    if not isinstance(mode, str):
        raise TypeError("open() argument 'mode' must be str, not " + type(mode).__name__)
    modes = set(mode)
    if modes - set("axrwb+t") or len(mode) > len(modes):
        raise ValueError("invalid mode: %r" % mode)
    kinds = [c for c in mode if c in "rwax"]
    if len(kinds) != 1:
        raise ValueError("must have exactly one of create/read/write/append mode")
    binary = "b" in modes
    if binary and "t" in modes:
        raise ValueError("can't have text and binary mode at once")
    if binary and encoding is not None:
        raise ValueError("binary mode doesn't take an encoding argument")
    if binary and errors is not None:
        raise ValueError("binary mode doesn't take an errors argument")
    if binary and newline is not None:
        raise ValueError("binary mode doesn't take a newline argument")
    if isinstance(file, int):
        raise OSError("file descriptors are not supported")
    raw = FileIO(file, kinds[0] + ("+" if "+" in modes else ""))
    if buffering == 0:
        if not binary:
            raise ValueError("can't have unbuffered text I/O")
        return raw
    if "+" in modes:
        buf = BufferedRandom(raw)
    elif kinds[0] == "r":
        buf = BufferedReader(raw)
    else:
        buf = BufferedWriter(raw)
    if binary:
        return buf
    w = TextIOWrapper(buf, encoding, errors, newline, line_buffering=buffering == 1)
    w._mode = mode
    return w


def open_code(path):
    return open(path, "rb")
